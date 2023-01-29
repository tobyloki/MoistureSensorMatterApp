package com.iotgroup2.matterapp.shared.MatterViewModel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.SystemClock
import androidx.activity.result.ActivityResult
import androidx.lifecycle.*
import com.google.android.gms.home.matter.commissioning.CommissioningResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.home.matter.commissioning.CommissioningRequest
import com.google.android.gms.home.matter.commissioning.DeviceInfo
import com.google.android.gms.home.matter.commissioning.SharedDeviceData
import com.iotgroup2.matterapp.*
import com.iotgroup2.matterapp.shared.matter.*
import com.iotgroup2.matterapp.shared.matter.chip.ClustersHelper
import com.iotgroup2.matterapp.shared.matter.commissioning.AppCommissioningService
import com.iotgroup2.matterapp.shared.matter.data.DevicesRepository
import com.iotgroup2.matterapp.shared.matter.data.DevicesStateRepository
import com.iotgroup2.matterapp.shared.matter.data.UserPreferencesRepository
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Encapsulates all of the information on a specific device. Note that the app currently only
 * supports Matter devices with server attribute "ON/OFF".
 */
data class DeviceUiModel(
    // Device information that is persisted in a Proto DataStore. See DevicesRepository.
    val device: Device,

    // Device state information that is retrieved dynamically.
    // Whether the device is online or offline.
    var isOnline: Boolean,
    // Whether the device is on or off.
    var isOn: Boolean,

    var temperature: Int,
    var pressure: Int,
    var humidity: Int,
    var thingName: Int,
    var battery: Int
)

/**
 * UI model that encapsulates the information about the devices to be displayed on the Home screen.
 */
data class DevicesUiModel(
    // The list of devices.
    val devices: List<DeviceUiModel>,
    // Making it so default is false, so that codelabinfo is not shown when we have not gotten
    // the userpreferences data yet.
    val showCodelabInfo: Boolean,
    // Whether offline devices should be shown.
    val showOfflineDevices: Boolean
)

@HiltViewModel
internal class MatterActivityViewModel
@Inject
constructor(
    private val devicesRepository: DevicesRepository,
    private val devicesStateRepository: DevicesStateRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clustersHelper: ClustersHelper,
): ViewModel() {
    // Controls whether a periodic ping to the devices is enabled or not.
    private var devicesPeriodicPingEnabled: Boolean = true

    // MARK: - Get list of devices stored in account

    private val devicesFlow = devicesRepository.devicesFlow
    private val devicesStateFlow = devicesStateRepository.devicesStateFlow
    private val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow

    // Every time the list of devices or user preferences are updated (emit is triggered),
    // we recreate the DevicesUiModel
    private val devicesUiModelFlow =
        combine(devicesFlow, devicesStateFlow, userPreferencesFlow) {
                devices: Devices,
                devicesStates: DevicesState,
                userPreferences: UserPreferences ->
            Timber.d("*** devicesUiModelFlow changed ***")
            return@combine DevicesUiModel(
                devices = processDevices(devices, devicesStates, userPreferences),
                showCodelabInfo = false,//!userPreferences.hideCodelabInfo,
                showOfflineDevices = true)//!userPreferences.hideOfflineDevices)
        }

    val devicesUiModelLiveData = devicesUiModelFlow.asLiveData()

    private fun processDevices(
        devices: Devices,
        devicesStates: DevicesState,
        userPreferences: UserPreferences
    ): List<DeviceUiModel> {
        val devicesUiModel = ArrayList<DeviceUiModel>()
        devices.devicesList.forEach { device ->
            Timber.d("processDevices() deviceId: [${device.deviceId}]}")
            val state = devicesStates.devicesStateList.find { it.deviceId == device.deviceId }
            if (userPreferences.hideOfflineDevices) {
                if (state?.online != true) return@forEach
            }
            if (state == null) {
                Timber.d("    deviceId setting default value for state")
                devicesUiModel.add(DeviceUiModel(device, isOnline = false, isOn = false, temperature = 0, pressure = 0, humidity = 0, thingName = 0, battery = 0))
            } else {
                Timber.d("    deviceId setting its own value for state")
                devicesUiModel.add(DeviceUiModel(device, state.online, state.on, state.temperature, state.pressure, state.humidity, state.thingName, state.battery))
            }
        }
        return devicesUiModel
    }

    // MARK: - Commissioning
    private val _commissionDeviceStatus = MutableLiveData<TaskStatus>(TaskStatus.NotStarted)
    val commissionDeviceStatus: LiveData<TaskStatus>
        get() = _commissionDeviceStatus

    private val _commissionDeviceIntentSender = MutableLiveData<IntentSender?>()
    val commissionDeviceIntentSender: LiveData<IntentSender?>
        get() = _commissionDeviceIntentSender

    // Called by the fragment in Step 5 of the Device Commissioning flow.
    fun commissionDeviceSucceeded(activityResult: ActivityResult, deviceName: String) {
        val result =
            CommissioningResult.fromIntentSenderResult(activityResult.resultCode, activityResult.data)
        Timber.i("Device commissioned successfully! deviceName [${result.deviceName}]")
        Timber.i("Device commissioned successfully! room [${result.room}]")
        Timber.i(
            "Device commissioned successfully! DeviceDescriptor of device:\n" +
                    "deviceType [${result.commissionedDeviceDescriptor.deviceType}]\n" +
                    "productId [${result.commissionedDeviceDescriptor.productId}]\n" +
                    "vendorId [${result.commissionedDeviceDescriptor.vendorId}]\n" +
                    "hashCode [${result.commissionedDeviceDescriptor.hashCode()}]")

        // Add the device to the devices repository.
        viewModelScope.launch {
            val deviceId = result.token?.toLong()!!
            try {
                Timber.d("Commissioning: Adding device to repository")
                devicesRepository.addDevice(
                    Device.newBuilder()
                        .setName(deviceName) // default name that can be overridden by user in next step
                        .setDeviceId(deviceId)
                        .setDateCommissioned(getTimestampForNow())
                        .setVendorId(result.commissionedDeviceDescriptor.vendorId.toString())
                        .setProductId(result.commissionedDeviceDescriptor.productId.toString())
                        // Note that deviceType is now deprecated. Need to get it by introspecting
                        // the device information. This is done below.
                        .setDeviceType(
                            convertToAppDeviceType(result.commissionedDeviceDescriptor.deviceType.toLong()))
                        .build())
                Timber.d("Commissioning: Adding device state to repository: isOnline:true isOn:false")
                devicesStateRepository.addDeviceState(deviceId, isOnline = true, isOn = false, temperature = null, pressure = null, humidity = null, thingName = null, battery = null)
                _commissionDeviceStatus.postValue(
                    TaskStatus.Completed("Device added: [${deviceId}] [${deviceName}]"))
            } catch (e: Exception) {
                Timber.e("Adding device [${deviceId}] [${deviceName}] to app's repository failed", e)
                _commissionDeviceStatus.postValue(
                    TaskStatus.Failed(
                        "Adding device [${deviceId}] [${deviceName}] to app's repository failed", e))
            }

            // Introspect the device and update its deviceType.
            val deviceMatterInfoList = clustersHelper.fetchDeviceMatterInfo(deviceId, 0)
            Timber.d("*** MATTER DEVICE INFO ***")
            deviceMatterInfoList.forEachIndexed { index, deviceMatterInfo ->
                Timber.d("Processing [[${index}] ${deviceMatterInfo}]")
                if (index == 3) {
                    if (deviceMatterInfo.types.size > 1) {
                        // TODO: Handle this properly
                        Timber.w("The device has more than one type. We're simply using the first one.")
                    }
                    devicesRepository.updateDeviceType(
                        deviceId, convertToAppDeviceType(deviceMatterInfo.types.first()))
                }
            }
        }
    }

    // Called by the fragment in Step 5 of the Device Commissioning flow when the GPS activity for
    // commissioning the device has failed.
    fun commissionDeviceFailed(resultCode: Int) {
        Timber.d("CommissionDevice: Failed [${resultCode}")
        _commissionDeviceStatus.postValue(
            TaskStatus.Failed("Commission device failed [${resultCode}]", null))
    }

    /**
     * Sample app has been invoked for multi-admin commissionning. TODO: Can we do it without going
     * through GMSCore? All we're missing is network location.
     */
    fun multiadminCommissioning(intent: Intent, context: Context) {
        Timber.d("multiadminCommissioning: starting")
        _commissionDeviceStatus.postValue(TaskStatus.InProgress)

        val sharedDeviceData = SharedDeviceData.fromIntent(intent)
        Timber.d("multiadminCommissioning: sharedDeviceData [${sharedDeviceData}]")
        Timber.d("multiadminCommissioning: manualPairingCode [${sharedDeviceData.manualPairingCode}]")

        val commissionRequestBuilder =
            CommissioningRequest.builder()
                .setCommissioningService(ComponentName(context, AppCommissioningService::class.java))

        // EXTRA_COMMISSIONING_WINDOW_EXPIRATION is a hint of how much time is remaining in the
        // commissioning window for multi-admin. It is based on the current system uptime.
        // If the user takes too long to select the target commissioning app, then there's not
        // enougj time to complete the multi-admin commissioning and we message it to the user.
        val commissioningWindowExpirationMillis =
            intent.getLongExtra(SharedDeviceData.EXTRA_COMMISSIONING_WINDOW_EXPIRATION, -1L)
        val currentUptimeMillis = SystemClock.elapsedRealtime()
        val timeLeftSeconds = (commissioningWindowExpirationMillis - currentUptimeMillis) / 1000
        Timber.d(
            "commissionDevice: TargetCommissioner for MultiAdmin. " +
                    "uptime [${currentUptimeMillis}] " +
                    "commissioningWindowExpiration [${commissioningWindowExpirationMillis}] " +
                    "-> expires in ${timeLeftSeconds} seconds")

        if (commissioningWindowExpirationMillis == -1L) {
            Timber.e(
                "EXTRA_COMMISSIONING_WINDOW_EXPIRATION not specified in multi-admin call. " +
                        "Still going ahead with the multi-admin though.")
        } else if (timeLeftSeconds < MIN_COMMISSIONING_WINDOW_EXPIRATION_SECONDS) {
//            _errorLiveData.value =
//                ErrorInfo(
//                    title = "Commissioning Window Expiration",
//                    message =
//                    "The commissioning window will " +
//                            "expire in ${timeLeftSeconds} seconds, not long enough to complete the commissioning.\n\n" +
//                            "In the future, please select the target commissioning application faster to avoid this situation.")
            Timber.e(
                "Commissioning window will expire in ${timeLeftSeconds} seconds, not long enough to complete the commissioning. In the future, please select the target commissioning application faster to avoid this situation.")
            return
        }

        val deviceName = intent.getStringExtra(SharedDeviceData.EXTRA_DEVICE_NAME)
        commissionRequestBuilder.setDeviceNameHint(deviceName)

        val vendorId = intent.getIntExtra(SharedDeviceData.EXTRA_VENDOR_ID, -1)
        val productId = intent.getIntExtra(SharedDeviceData.EXTRA_PRODUCT_ID, -1)
        val deviceType = intent.getIntExtra(SharedDeviceData.EXTRA_DEVICE_TYPE, -1)
        val deviceInfo = DeviceInfo.builder().setProductId(productId).setVendorId(vendorId).build()
        commissionRequestBuilder.setDeviceInfo(deviceInfo)

        val manualPairingCode = intent.getStringExtra(SharedDeviceData.EXTRA_MANUAL_PAIRING_CODE)
        commissionRequestBuilder.setOnboardingPayload(manualPairingCode)

        val commissioningRequest = commissionRequestBuilder.build()

        Timber.d(
            "multiadmin: commissioningRequest " +
                    "onboardingPayload [${commissioningRequest.onboardingPayload}] " +
                    "vendorId [${commissioningRequest.deviceInfo!!.vendorId}] " +
                    "productId [${commissioningRequest.deviceInfo!!.productId}]")

        Matter.getCommissioningClient(context)
            .commissionDevice(commissioningRequest)
            .addOnSuccessListener { result ->
                // Communication with fragment is via livedata
                _commissionDeviceStatus.postValue(TaskStatus.InProgress)
                _commissionDeviceIntentSender.postValue(result)
            }
            .addOnFailureListener { error ->
                Timber.e(error)
                _commissionDeviceStatus.postValue(
                    TaskStatus.Failed("Failed to to get the IntentSender.", error))
            }
    }

    /**
     * Consumes the value in [_commissionDeviceIntentSender] and sets it back to null. Needs to be
     * called to avoid re-processing the IntentSender after a configuration change (where the LiveData
     * is re-posted.
     */
    fun consumeCommissionDeviceIntentSender() {
        _commissionDeviceIntentSender.postValue(null)
    }

    fun commissionDevice(context: Context) {
        Timber.d("CommissionDevice: starting")
        _commissionDeviceStatus.postValue(TaskStatus.InProgress)

        val commissionDeviceRequest =
            CommissioningRequest.builder()
                .setCommissioningService(ComponentName(context, AppCommissioningService::class.java))
                .build()

        // The call to commissionDevice() creates the IntentSender that will eventually be launched
        // in the fragment to trigger the commissioning activity in GPS.
        Matter.getCommissioningClient(context)
            .commissionDevice(commissionDeviceRequest)
            .addOnSuccessListener { result ->
                Timber.d("ShareDevice: Success getting the IntentSender: result [${result}]")
                // Communication with fragment is via livedata
                _commissionDeviceIntentSender.postValue(result)
            }
            .addOnFailureListener { error ->
                Timber.e(error)
                _commissionDeviceStatus.postValue(
                    TaskStatus.Failed("Setting up the IntentSender failed", error))
            }
    }

    // Task that runs periodically to update the devices state.
    fun startDevicesPeriodicPing() {
        if (PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS == -1) {
            return
        }
        Timber.d(
            "${LocalDateTime.now()} startDevicesPeriodicPing every $PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS seconds")
        devicesPeriodicPingEnabled = true
        runDevicesPeriodicPing()
    }

    private fun runDevicesPeriodicPing() {
        viewModelScope.launch {
            while (devicesPeriodicPingEnabled) {
                // For each ne of the real devices
                val devicesList = devicesRepository.getAllDevices().devicesList
                devicesList.forEach { device ->
                    if (device.name.startsWith(DUMMY_DEVICE_NAME_PREFIX)) {
                        return@forEach
                    }
                    Timber.i("runDevicesPeriodicPing deviceId [${device.deviceId}]")

                    try {
                        var isOn = clustersHelper.getDeviceStateOnOffCluster(device.deviceId, 1)
                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [isOn: ${isOn}]")
                        var temperature = clustersHelper.getDeviceStateTemperatureMeasurementCluster(device.deviceId, 2)
                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [temperature: ${temperature}]")
                        var pressure = clustersHelper.getDeviceStatePressureMeasurementCluster(device.deviceId, 3)
                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [pressure: ${pressure}]")
                        var humidity = clustersHelper.getDeviceStateHumidityMeasurementCluster(device.deviceId, 4)
                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [humidity: ${humidity}]")
                        var thingName = clustersHelper.getDeviceStateHumidityMinMeasurementCluster(device.deviceId, 4)
                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [thingName: ${thingName}]")
                        var battery = clustersHelper.getDeviceStateHumidityMaxMeasurementCluster(device.deviceId, 4)
                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [battery: ${battery}]")
                        val isOnline: Boolean
                        if (isOn == null) {
                            Timber.e("runDevicesPeriodicUpdate: cannot get device on/off state -> OFFLINE")
                            isOn = false
                            isOnline = false
                        } else {
                            isOnline = true
                        }
                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [isOnline: ${isOnline}], [isOn: ${isOn}], [temperature: ${temperature}], [pressure: ${pressure}], [humidity: ${humidity}], [thingName: ${thingName}], [battery: ${battery}]")
                        // TODO: only need to do it if state has changed
                        devicesStateRepository.updateDeviceState(
                            device.deviceId, isOnline = isOnline, isOn = isOn, temperature = temperature, pressure = pressure, humidity = humidity, thingName = thingName, battery = battery
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
                delay(PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS * 1000L)
            }
        }
    }

    fun stopDevicesPeriodicPing() {
        devicesPeriodicPingEnabled = false
    }

    // MARK: - State control
    fun updateDeviceStateOn(deviceUiModel: DeviceUiModel, isOn: Boolean, temperature: Int?, pressure: Int?, humidity: Int?, thingName: Int?, battery: Int?) {
        Timber.d("updateDeviceStateOn: Device [${deviceUiModel}]  isOn [${isOn}]")
        viewModelScope.launch {
            if (isDummyDevice(deviceUiModel.device.name)) {
                Timber.d("Handling test device")
                devicesStateRepository.updateDeviceState(
                    deviceUiModel.device.deviceId,
                    true,
                    isOn,
                    temperature,
                    pressure,
                    humidity,
                    thingName,
                    battery
                )
            } else {
                Timber.d("Handling real device")
                clustersHelper.setOnOffDeviceStateOnOffCluster(deviceUiModel.device.deviceId, isOn, 1)
                devicesStateRepository.updateDeviceState(
                    deviceUiModel.device.deviceId,
                    true,
                    isOn,
                    temperature,
                    pressure,
                    humidity,
                    thingName,
                    battery
                )
            }
        }
    }

    fun updateDeviceName(deviceId: Long, name: String) {
        Timber.d("**************** update device name ****** [${deviceId}]")
        viewModelScope.launch {
            try {
                devicesRepository.updateDeviceName(deviceId, name)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun removeDevice(deviceId: Long) {
        Timber.d("**************** remove device ****** [${deviceId}]")
        viewModelScope.launch {
            try {
                devicesRepository.removeDevice(deviceId)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}