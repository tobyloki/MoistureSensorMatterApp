package com.iotgroup2.matterapp.shared.MatterViewModel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.SystemClock
import androidx.activity.result.ActivityResult
import androidx.lifecycle.*
import chip.devicecontroller.model.NodeState
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
import com.iotgroup2.matterapp.shared.matter.chip.ChipClient
import com.iotgroup2.matterapp.shared.matter.chip.ClustersHelper
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.HumidityMaxMeasurementAttribute
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.HumidityMeasurementAttribute
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.HumidityMinMeasurementAttribute
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.LightMeasurementAttribute
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.OnOffAttribute
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.PressureMeasurementAttribute
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.SoilMoistureMeasurementAttribute
import com.iotgroup2.matterapp.shared.matter.chip.MatterConstants.TemperatureMeasurementAttribute
import com.iotgroup2.matterapp.shared.matter.chip.SubscriptionHelper
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
    var humidity: Int,
    var pressure: Int,
    var soilMoisture: Int,
    var light: Int,

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
    private val chipClient: ChipClient,
    private val subscriptionHelper: SubscriptionHelper,
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
                devicesUiModel.add(DeviceUiModel(device, isOnline = false, isOn = false, temperature = 0, humidity = 0, pressure = 0, soilMoisture = 0, light = 0, thingName = 0, battery = 0))
            } else {
                Timber.d("    deviceId setting its own value for state")
                devicesUiModel.add(DeviceUiModel(device, state.online, state.on, state.temperature, state.humidity, state.pressure, state.soilMoisture, state.light, state.thingName, state.battery))
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
                devicesStateRepository.addDeviceState(deviceId, isOnline = true, isOn = false, temperature = null, humidity = null, pressure = null, soilMoisture = null, light = null, thingName = null, battery = null)
                _commissionDeviceStatus.postValue(
                    TaskStatus.Completed("Device added: [${deviceId}] [${deviceName}]"))
            } catch (e: Exception) {
                Timber.e("Adding device [${deviceId}] [${deviceName}] to app's repository failed", e)
                _commissionDeviceStatus.postValue(
                    TaskStatus.Failed(
                        "Adding device [${deviceId}] [${deviceName}] to app's repository failed", e))
            }

            // Introspect the device and update its deviceType.
            // TODO: Need to get capabilities information and store that in the devices repository.
            // (e.g on/off on which endpoint).
            val deviceMatterInfoList = clustersHelper.fetchDeviceMatterInfo(deviceId)
            Timber.d("*** MATTER DEVICE INFO ***")
            var gotDeviceType = false
            deviceMatterInfoList.forEach { deviceMatterInfo ->
                Timber.d("Processing endpoint [$deviceMatterInfo.endpoint]")
                // Endpoint 0 is the Root Node, so we disregard it.
                if (deviceMatterInfo.endpoint != 0) {
                    if (gotDeviceType) {
                        // TODO: Handle this properly once we have specific examples to learn from.
                        Timber.w(
                            "The device has more than one endpoint. We're simply using the first one to define the device type.")
                        return@forEach
                    }
                    if (deviceMatterInfo.types.size > 1) {
                        // TODO: Handle this properly once we have specific examples to learn from.
                        Timber.w(
                            "The endpoint has more than one type. We're simply using the first one to define the device type.")
                    }
                    devicesRepository.updateDeviceType(
                        deviceId, convertToAppDeviceType(deviceMatterInfo.types.first()))
                    gotDeviceType = true
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
        if (PERIODIC_READ_INTERVAL_HOME_SCREEN_SECONDS == -1) {
            return
        }
        Timber.d(
            "${LocalDateTime.now()} startDevicesPeriodicPing every $PERIODIC_READ_INTERVAL_HOME_SCREEN_SECONDS seconds")
        devicesPeriodicPingEnabled = true
        runDevicesPeriodicPing()
    }

    private fun runDevicesPeriodicPing() {
        viewModelScope.launch {
            while (devicesPeriodicPingEnabled) {
                // For each ne of the real devices
                val devicesList = devicesRepository.getAllDevices().devicesList
                devicesList.forEach { device ->
                    Timber.i("runDevicesPeriodicPing deviceId [${device.deviceId}]")

                    try {
                        var isOn: Boolean? = null
                        try {
                            isOn = clustersHelper.getDeviceStateOnOffCluster(device.deviceId, 1)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [isOn: ${isOn}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        var temperature: Int? = null
                        try {
                            temperature = clustersHelper.getDeviceStateTemperatureMeasurementCluster(device.deviceId, 1)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [temperature: ${temperature}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        var humidity: Int? = null
                        try {
                            humidity = clustersHelper.getDeviceStateHumidityMeasurementCluster(device.deviceId, 2)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [humidity: ${humidity}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        var pressure: Int? = null
                        try {
                            pressure = clustersHelper.getDeviceStatePressureMeasurementCluster(device.deviceId, 3)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [pressure: ${pressure}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        var soilMoisture: Int? = null
                        try {
                            soilMoisture = clustersHelper.getDeviceStateSoilMoistureMeasurementCluster(device.deviceId, 4)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [soilMoisture: ${soilMoisture}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        var light: Int? = null
                        try {
                            light = clustersHelper.getDeviceStateLightMeasurementCluster(device.deviceId, 5)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [light: ${light}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        var thingName: Int? = null
                        try {
                            thingName = clustersHelper.getDeviceStateHumidityMinMeasurementCluster(device.deviceId, 2)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [thingName: ${thingName}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        var battery: Int? = null
                        try {
                            battery = clustersHelper.getDeviceStateHumidityMaxMeasurementCluster(device.deviceId, 2)
                            Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [battery: ${battery}]")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        val isOnline: Boolean
//                        if (thingName == null && isOn == null) {
//                            Timber.e("runDevicesPeriodicUpdate: cannot get device thingName or isOn -> OFFLINE")
//                            isOnline = false
//                        } else {
//                            isOnline = true
//                        }
                        isOnline = true

                        Timber.i("runDevicesPeriodicPing [deviceId: ${device.deviceId}], [isOnline: ${isOnline}], [isOn: ${isOn}], [temperature: ${temperature}], [humidity: ${humidity}], [pressure: ${pressure}], [soilMoisture: ${soilMoisture}], [light: ${light}], [thingName: ${thingName}], [battery: ${battery}]")
                        // TODO: only need to do it if state has changed
                        devicesStateRepository.updateDeviceState(
                            device.deviceId, isOnline = isOnline, isOn = isOn, temperature = temperature, humidity = humidity, pressure = pressure, soilMoisture = soilMoisture, light = light, thingName = thingName, battery = battery
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
                delay(PERIODIC_READ_INTERVAL_HOME_SCREEN_SECONDS * 1000L)
            }
        }
    }

    fun stopDevicesPeriodicPing() {
        devicesPeriodicPingEnabled = false
    }

    // MARK: - State control
    fun updateDeviceStateOn(deviceUiModel: DeviceUiModel, isOn: Boolean) {
        Timber.d("updateDeviceStateOn: isOn [${isOn}]")
        val deviceId = deviceUiModel.device.deviceId
        viewModelScope.launch {
            // CODELAB: toggle
            Timber.d("Handling real device")
            try {
                clustersHelper.setOnOffDeviceStateOnOffCluster(deviceId, isOn, 1)
                devicesStateRepository.updateDeviceState(
                    deviceId,
                    null,
                    isOn,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            } catch (e: Throwable) {
                Timber.e("Failed setting on/off state")
            }
            // CODELAB SECTION END
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

    // -----------------------------------------------------------------------------------------------
    // State Changes Monitoring

    /**
     * The way we monitor state changes is defined by constant [StateChangesMonitoringMode].
     * [StateChangesMonitoringMode.Subscription] is the preferred mode.
     * [StateChangesMonitoringMode.PeriodicRead] was used initially because of issues with
     * subscriptions. We left its associated code as it could be useful to some developers.
     */
    fun startMonitoringStateChanges() {
        when (STATE_CHANGES_MONITORING_MODE) {
            StateChangesMonitoringMode.Subscription -> subscribeToDevicesPeriodicUpdates()
            StateChangesMonitoringMode.PeriodicRead -> startDevicesPeriodicPing()
        }
    }

    fun stopMonitoringStateChanges() {
        when (STATE_CHANGES_MONITORING_MODE) {
            StateChangesMonitoringMode.Subscription -> unsubscribeToDevicesPeriodicUpdates()
            StateChangesMonitoringMode.PeriodicRead -> stopDevicesPeriodicPing()
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Subscription to periodic device updates.
    // See:
    //   - Spec section "8.5 Subscribe Interaction"
    //   - Matter primer:
    //
    // https://developers.home.google.com/matter/primer/interaction-model-reading#subscription_transaction

    private fun subscribeToDevicesPeriodicUpdates() {
        Timber.d("subscribeToDevicesPeriodicUpdates()")
        viewModelScope.launch {
            // For each one of the real devices
            val devicesList = devicesRepository.getAllDevices().devicesList
            devicesList.forEach { device ->
                val reportCallback =
                    object : SubscriptionHelper.ReportCallbackForDevice(device.deviceId) {
                        override fun onReport(nodeState: NodeState) {
                            super.onReport(nodeState)
                            // TODO: See HomeViewModel:CommissionDeviceSucceeded for device capabilities
//                            val onOffState =
//                                subscriptionHelper.extractAttribute(nodeState, 1, OnOffAttribute) as Boolean?
//                            Timber.d("onOffState [${onOffState}]")
//                            if (onOffState == null) {
//                                Timber.e("onReport(): WARNING -> onOffState is NULL. Ignoring.")
//                                return
//                            }
//                            viewModelScope.launch {
//                                devicesStateRepository.updateDeviceState(
//                                    device.deviceId, isOnline = true, isOn = onOffState)
//                            }

                            viewModelScope.launch {
                                try {
                                    var isOn: Boolean? = null
                                    try {
//                                    isOn = clustersHelper.getDeviceStateOnOffCluster(device.deviceId, 1)
                                        isOn = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            1,
                                            OnOffAttribute
                                        ) as Boolean?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [isOn: ${isOn}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    var temperature: Int? = null
                                    try {
//                                        temperature =
//                                            clustersHelper.getDeviceStateTemperatureMeasurementCluster(
//                                                device.deviceId,
//                                                1
//                                            )
                                        temperature = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            1,
                                            TemperatureMeasurementAttribute
                                        ) as Int?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [temperature: ${temperature}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    var humidity: Int? = null
                                    try {
//                                        humidity =
//                                            clustersHelper.getDeviceStateHumidityMeasurementCluster(
//                                                device.deviceId,
//                                                3
//                                            )
                                        humidity = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            2,
                                            HumidityMeasurementAttribute
                                        ) as Int?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [humidity: ${humidity}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    var pressure: Int? = null
                                    try {
//                                        pressure =
//                                            clustersHelper.getDeviceStatePressureMeasurementCluster(
//                                                device.deviceId,
//                                                2
//                                            )
                                        pressure = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            3,
                                            PressureMeasurementAttribute
                                        ) as Int?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [pressure: ${pressure}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    var soilMoisture: Int? = null
                                    try {
                                        soilMoisture = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            4,
                                            SoilMoistureMeasurementAttribute
                                        ) as Int?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [soilMoisture: ${soilMoisture}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    var light: Int? = null
                                    try {
                                        light = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            5,
                                            LightMeasurementAttribute
                                        ) as Int?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [light: ${light}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    var thingName: Int? = null
                                    try {
                                        thingName = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            2,
                                            HumidityMinMeasurementAttribute
                                        ) as Int?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [thingName: ${thingName}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    var battery: Int? = null
                                    try {
                                        battery = subscriptionHelper.extractAttribute(
                                            nodeState,
                                            2,
                                            HumidityMaxMeasurementAttribute
                                        ) as Int?
                                        Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [battery: ${battery}]")
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                    val isOnline: Boolean
//                                    if (thingName == null && isOn == null) {
//                                        Timber.e("subscribeToDevicesPeriodicUpdates: cannot get device thingName or isOn -> OFFLINE")
//                                        isOnline = false
//                                    } else {
//                                        isOnline = true
//                                    }
                                    isOnline = true

                                    Timber.i("subscribeToDevicesPeriodicUpdates [deviceId: ${device.deviceId}], [isOnline: ${isOnline}], [isOn: ${isOn}], [temperature: ${temperature}], [humidity: ${humidity}], [pressure: ${pressure}], [soilMoisture: ${soilMoisture}], [light: ${light}], [thingName: ${thingName}], [battery: ${battery}]")
                                    // TODO: only need to do it if state has changed
                                    devicesStateRepository.updateDeviceState(
                                        device.deviceId,
                                        isOnline = isOnline,
                                        isOn = isOn,
                                        temperature = temperature,
                                        humidity = humidity,
                                        pressure = pressure,
                                        soilMoisture = soilMoisture,
                                        light = light,
                                        thingName = thingName,
                                        battery = battery
                                    )
                                } catch (e: Exception) {
                                    Timber.e(e)
                                }
                            }
                        }
                    }

                try {
                    val connectedDevicePointer = chipClient.getConnectedDevicePointer(device.deviceId)
                    subscriptionHelper.awaitSubscribeToPeriodicUpdates(
                        connectedDevicePointer,
                        SubscriptionHelper.SubscriptionEstablishedCallbackForDevice(device.deviceId),
                        SubscriptionHelper.ResubscriptionAttemptCallbackForDevice(device.deviceId),
                        reportCallback)
                } catch (e: IllegalStateException) {
                    Timber.e("Can't get connectedDevicePointer for ${device.deviceId}.")
                    return@forEach
                }
            }
        }
    }

    private fun unsubscribeToDevicesPeriodicUpdates() {
        Timber.d("unsubscribeToPeriodicUpdates()")
        viewModelScope.launch {
            // For each one of the real devices
            val devicesList = devicesRepository.getAllDevices().devicesList
            devicesList.forEach { device ->
                try {
                    val connectedDevicePtr =
                        chipClient.getConnectedDevicePointer(device.deviceId)
                    subscriptionHelper.awaitUnsubscribeToPeriodicUpdates(connectedDevicePtr)
                } catch (e: IllegalStateException) {
                    Timber.e("Can't get connectedDevicePointer for ${device.deviceId}.")
                    return@forEach
                }
            }
        }
    }
}