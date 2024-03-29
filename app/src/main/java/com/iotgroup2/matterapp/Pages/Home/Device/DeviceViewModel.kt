package com.iotgroup2.matterapp.Pages.Home.Device

import android.app.Activity
import android.content.IntentSender
import android.os.SystemClock
import androidx.lifecycle.*
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.home.matter.commissioning.CommissioningWindow
import com.google.android.gms.home.matter.commissioning.ShareDeviceRequest
import com.google.android.gms.home.matter.common.DeviceDescriptor
import com.google.android.gms.home.matter.common.Discriminator
import com.iotgroup2.matterapp.shared.matter.*
import com.iotgroup2.matterapp.shared.matter.chip.ChipClient
import com.iotgroup2.matterapp.shared.matter.chip.ClustersHelper
import com.iotgroup2.matterapp.shared.matter.data.DevicesRepository
import com.iotgroup2.matterapp.shared.matter.data.DevicesStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DeviceViewModel
@Inject
constructor(
    private val devicesRepository: DevicesRepository,
    private val devicesStateRepository: DevicesStateRepository,
    private val chipClient: ChipClient,
    private val clustersHelper: ClustersHelper
) : ViewModel(), DefaultLifecycleObserver {

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    /**
     * The current status of the share device task. The enum it is based on is used by the Fragment to
     * properly react to the processing happening with the share device task.
     */
    private val _shareDeviceStatus = MutableLiveData<TaskStatus>(TaskStatus.NotStarted)
    val shareDeviceStatus: LiveData<TaskStatus>
        get() = _shareDeviceStatus

    /**
     * Actions that drive showing/hiding a "background work" alert dialog. The enum it is based on is
     * used by the Fragment to properly react on the management of that dialog.
     */
    private val _backgroundWorkAlertDialogAction =
        MutableLiveData<BackgroundWorkAlertDialogAction>(BackgroundWorkAlertDialogAction.Hide)
    val backgroundWorkAlertDialogAction: LiveData<BackgroundWorkAlertDialogAction>
        get() = _backgroundWorkAlertDialogAction

    /** IntentSender LiveData triggered by [shareDevice]. */
    private val _shareDeviceIntentSender = MutableLiveData<IntentSender?>()
    val shareDeviceIntentSender: LiveData<IntentSender?>
        get() = _shareDeviceIntentSender

    private var timer = Timer()
    val deviceOnline = MutableLiveData<Boolean>().apply {
        value = false
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        startTimer()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        cancelTimer()
    }

    fun setOnline(online: Boolean) {
        deviceOnline.postValue(online)
    }

    fun startTimer() {
        cancelTimer()

        // start timer to fetch sensor data every 5 seconds
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                setOnline(false)
                startTimer()
            }
        }, 5000)
    }

    private fun cancelTimer() {
        try {
            timer.cancel()
            Timber.i("Timer cancelled")
        } catch (_: Exception) {}
    }

    // -----------------------------------------------------------------------------------------------
    // Device Sharing (aka Multi-Admin)
    //
    // See "docs/Google Home Mobile SDK.pdf" for a good overview of all the artifacts needed
    // to transfer control from the sample app's UI to the GPS ShareDevice UI, and get a result back.

    /**
     * Share Device Step 2 (part 2). Triggered by the "Share Device" button in the fragment. Initiates
     * a share device task. The success callback of the commissioningClient.shareDevice() API provides
     * the IntentSender to be used to launch the "Share Device" activity in Google Play Services. This
     * viewModel provides two LiveData objects to report on the result of this API call that can then
     * be used by the Fragment who's observing them:
     * 1. [shareDeviceStatus] updates the fragment's UI according to the TaskStatus
     * 2. [shareDeviceIntentSender] is the IntentSender to be used in the Fragment to launch the
     *    Google Play Services "Share Device" activity (step 3).
     *
     * See [consumeShareDeviceIntentSender()] for proper management of the IntentSender in the face of
     * configuration changes that repost LiveData.
     */
    fun shareDevice(activity: Activity, deviceId: Long) {
        Timber.d("ShareDevice: starting")
//        stopDevicePeriodicPing()
        _shareDeviceStatus.postValue(TaskStatus.InProgress)
        _backgroundWorkAlertDialogAction.postValue(
            BackgroundWorkAlertDialogAction.Show(
                "Opening Pairing Window", "This may take a few seconds."))

        viewModelScope.launch {
            // First we need to open a commissioning window.
            try {
                when (OPEN_COMMISSIONING_WINDOW_API) {
                    OpenCommissioningWindowApi.ChipDeviceController ->
                        openCommissioningWindowUsingOpenPairingWindowWithPin(deviceId)
                    OpenCommissioningWindowApi.AdministratorCommissioningCluster ->
                        openCommissioningWindowWithAdministratorCommissioningCluster(deviceId)
                }
            } catch (e: Throwable) {
                val msg = "Failed to open the commissioning window"
                Timber.d("ShareDevice: ${msg} [${e}]")
//                _backgroundWorkAlertDialogAction.postValue(BackgroundWorkAlertDialogAction.Hide)
//                _shareDeviceStatus.postValue(TaskStatus.Failed(msg, e))
//                return@launch
            }

            // Second, we get the IntentSender and post it as LiveData for the fragment to pick it up
            // and trigger the GPS ShareDevice activity.
            // CODELAB: shareDevice
            Timber.d("ShareDevice: Setting up the IntentSender")
            val shareDeviceRequest =
                ShareDeviceRequest.builder()
                    .setDeviceDescriptor(DeviceDescriptor.builder().build())
                    .setDeviceName("temp device name")
                    .setCommissioningWindow(
                        CommissioningWindow.builder()
                            .setDiscriminator(Discriminator.forLongValue(DISCRIMINATOR))
                            .setPasscode(SETUP_PIN_CODE)
                            .setWindowOpenMillis(SystemClock.elapsedRealtime())
                            .setDurationSeconds(OPEN_COMMISSIONING_WINDOW_DURATION_SECONDS.toLong())
                            .build())
                    .build()

            Timber.i(
                "ShareDevice: shareDeviceRequest " +
                        "onboardingPayload [${shareDeviceRequest.commissioningWindow.passcode}] " +
                        "discriminator [${shareDeviceRequest.commissioningWindow.discriminator}]")

            // The call to shareDevice() creates the IntentSender that will eventually be launched
            // in the fragment to trigger the multi-admin activity in GPS (step 3).
            Matter.getCommissioningClient(activity)
                .shareDevice(shareDeviceRequest)
                .addOnSuccessListener { result ->
                    Timber.d("ShareDevice: Success getting the IntentSender: result [${result}]")
                    // Communication with fragment is via livedata
                    _backgroundWorkAlertDialogAction.postValue(BackgroundWorkAlertDialogAction.Hide)
                    _shareDeviceIntentSender.postValue(result)
                }
                .addOnFailureListener { error ->
                    Timber.e(error)
                    _backgroundWorkAlertDialogAction.postValue(BackgroundWorkAlertDialogAction.Hide)
                    _shareDeviceStatus.postValue(
                        TaskStatus.Failed("Setting up the IntentSender failed", error))
                }
            // CODELAB SECTION END
        }
    }

    // CODELAB FEATURED BEGIN
    /**
     * Consumes the value in [_shareDeviceIntentSender] and sets it back to null. Needs to be called
     * to avoid re-processing an IntentSender after a configuration change where the LiveData is
     * re-posted.
     */
    fun consumeShareDeviceIntentSender() {
        _shareDeviceIntentSender.postValue(null)
    }
    // CODELAB FEATURED END

    // Called by the fragment in Step 5 of the Device Sharing flow when the GPS activity for
    // Device Sharing has succeeded.
    fun shareDeviceSucceeded() {
        _shareDeviceStatus.postValue(TaskStatus.Completed("Device sharing completed successfully"))
//        startDevicePeriodicPing(deviceUiModel)
    }

    // Called by the fragment in Step 5 of the Device Sharing flow when the GPS activity for
    // Device Sharing has failed.
    fun shareDeviceFailed(resultCode: Int) {
        Timber.d("ShareDevice: Failed with errorCode [${resultCode}]")
        _shareDeviceStatus.postValue(TaskStatus.Failed("Device sharing failed [${resultCode}]", null))
//        startDevicePeriodicPing(deviceUiModel)
    }

    // Called after we dismiss an error dialog. If we don't consume, a config change redisplays the
    // alert dialog.
    fun consumeShareDeviceStatus() {
        _shareDeviceStatus.postValue(TaskStatus.NotStarted)
    }

    // -----------------------------------------------------------------------------------------------
    // Open commissioning window

    private suspend fun openCommissioningWindowUsingOpenPairingWindowWithPin(deviceId: Long) {
        // TODO: Should generate random 64 bit value for SETUP_PIN_CODE (taking into account
        // spec constraints)
        Timber.d("ShareDevice: chipClient.awaitGetConnectedDevicePointer(${deviceId})")
        val connectedDevicePointer = chipClient.awaitGetConnectedDevicePointer(deviceId)

        try {
            // Check if there is a commission window that's already open.
            // See [CommissioningWindowStatus] for complete details.
            val isOpen = clustersHelper.isCommissioningWindowOpen(connectedDevicePointer)
            Timber.d("ShareDevice: isCommissioningWindowOpen [$isOpen]")
            if (isOpen) {
                // close commission window
                clustersHelper.closeCommissioningWindow(connectedDevicePointer)
            }
        } catch (ex: Exception) {
            val errorMsg = "Failed to setup Administrator Commissioning Cluster"
            Timber.d("$errorMsg. Cause: ${ex.localizedMessage}")
            // ToDo() decide whether to terminate the OCW task if we fail to configure the window status.
        }

        val duration = OPEN_COMMISSIONING_WINDOW_DURATION_SECONDS
        Timber.d(
            "ShareDevice: chipClient.chipClient.awaitOpenPairingWindowWithPIN " +
                    "duration [${duration}] iteration [${ITERATION}] discriminator [${DISCRIMINATOR}] " +
                    "setupPinCode [${SETUP_PIN_CODE}]")
        chipClient.awaitOpenPairingWindowWithPIN(
            connectedDevicePointer, duration, ITERATION, DISCRIMINATOR, SETUP_PIN_CODE)
        Timber.d("ShareDevice: After chipClient.awaitOpenPairingWindowWithPIN")
    }

    // TODO: Was not working when tested. Use openCommissioningWindowUsingOpenPairingWindowWithPin
    // for now.
    private suspend fun openCommissioningWindowWithAdministratorCommissioningCluster(deviceId: Long) {
        Timber.d(
            "ShareDevice: openCommissioningWindowWithAdministratorCommissioningCluster [${deviceId}]")
        val salt = Random.nextBytes(32)
        val timedInvokeTimeoutMs = 10000
        val devicePtr = chipClient.awaitGetConnectedDevicePointer(deviceId)
        val verifier = chipClient.computePaseVerifier(devicePtr, SETUP_PIN_CODE, ITERATION, salt)
        clustersHelper.openCommissioningWindowAdministratorCommissioningCluster(
            deviceId,
            0,
            180,
            verifier.pakeVerifier,
            DISCRIMINATOR,
            ITERATION,
            salt,
            timedInvokeTimeoutMs)
    }
}