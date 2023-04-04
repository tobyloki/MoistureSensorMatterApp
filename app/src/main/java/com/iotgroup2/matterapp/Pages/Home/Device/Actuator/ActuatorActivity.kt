package com.iotgroup2.matterapp.Pages.Home.Device.Actuator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.davidmiguel.multistateswitch.MultiStateSwitch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iotgroup2.matterapp.Device.DeviceType
import com.iotgroup2.matterapp.Pages.Home.Device.DeviceViewModel
import com.iotgroup2.matterapp.Pages.Home.EditDevice.EditDeviceActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityActuatorBinding
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.shared.matter.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.List


@AndroidEntryPoint
class ActuatorActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityActuatorBinding

    private lateinit var deviceId: String
    private var deviceType: Int = DeviceType.TYPE_UNSPECIFIED_VALUE
    private lateinit var deviceName: String

    private lateinit var nameTxt: TextView
    private lateinit var onlineIcon: TextView
    private lateinit var onlineTxt: TextView
    private lateinit var multiStateSwitch: MultiStateSwitch

    private val viewModel: MatterActivityViewModel by viewModels()
    private var deviceUiModel: DeviceUiModel? = null

    private lateinit var deviceViewModel: DeviceViewModel

    // The ActivityResultLauncher that launches the "shareDevice" activity in Google Play Services.
    private lateinit var shareDeviceLauncher: ActivityResultLauncher<IntentSenderRequest>

    // Background work dialog.
    private lateinit var backgroundWorkAlertDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityActuatorBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        // Background Work AlertDialog.
        backgroundWorkAlertDialog = MaterialAlertDialogBuilder(this).create()

        nameTxt = _binding.nameTxt
        onlineIcon  = _binding.onlineIcon
        onlineTxt = _binding.onlineTxt
        multiStateSwitch = _binding.multiStateSwitch

        /* Initialize Data */
        val extras: Bundle? = intent.extras
        if(extras == null)
            return pageLoadFail("No extras passed to page")
        deviceId = extras.getString("deviceId").toString()
        deviceType = extras.getInt("deviceType")
        deviceName = extras.getString("deviceName").toString()
        val deviceOnline = extras.getBoolean("deviceOnline")

        Timber.i("Device ID: $deviceId")
        Timber.i("Device Type: $deviceType")
        Timber.i("Device Name: $deviceName")
        Timber.i("Device Online: $deviceOnline")

        nameTxt.text = deviceName
        onlineIcon.setTextColor(if (deviceOnline) ContextCompat.getColor(this, R.color.online) else ContextCompat.getColor(this, R.color.offline))
        onlineTxt.text = if (deviceOnline) "Online" else "Offline"

        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)
        lifecycle.addObserver(deviceViewModel)

        // matter device list
        viewModel.devicesUiModelLiveData.observe(this) { devicesUiModel: DevicesUiModel ->
            for (device in devicesUiModel.devices) {
                if (device.device.deviceId.toString() == deviceId) {
//                    if (deviceUiModel == null) {
                        deviceUiModel = device
                        updateUI()
//                    }
                }
            }
        }

        // Share Device Step 1, where an activity launcher is registered.
        // At step 2 of the "Share Device" flow, the user triggers the "Share Device"
        // action and the ViewModel calls the Google Play Services (GPS) API
        // (commissioningClient.shareDevice()).
        // This returns an  IntentSender that is then used in step 3 to call
        // shareDevicelauncher.launch().
        // CODELAB: shareDeviceLauncher definition
        shareDeviceLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                // Share Device Step 5.
                // The Share Device activity in GPS (step 4) has completed.
                val resultCode = result.resultCode
                if (resultCode == RESULT_OK) {
                    Timber.d("ShareDevice: Success")
                    deviceViewModel.shareDeviceSucceeded()
                } else {
                    deviceViewModel.shareDeviceFailed(resultCode)
                }
            }
        // CODELAB SECTION END

        // CODELAB FEATURED BEGIN
        // The current status of the share device action.
        deviceViewModel.shareDeviceStatus.observe(this) { status ->
//            val isButtonEnabled = status !is TaskStatus.InProgress
//            updateShareDeviceButton(isButtonEnabled)
            if (status is TaskStatus.Failed) {
                Timber.e("ShareDevice failed: ${status.message}, ${status.cause}")
                viewModel.startMonitoringStateChanges()
            }
        }
        // CODELAB FEATURED END

        // In the DeviceSharing flow step 2, the ViewModel calls the GPS shareDevice() API to get the
        // IntentSender to be used with the Android Activity Result API. Once the ViewModel has
        // the IntentSender, it posts it via LiveData so the Fragment can use that value to launch the
        // activity (step 3).
        // Note that when the IntentSender has been processed, it must be consumed to avoid a
        // configuration change that resends the observed values and re-triggers the device sharing.
        // CODELAB FEATURED BEGIN
        deviceViewModel.shareDeviceIntentSender.observe(this) { sender ->
            Timber.d("shareDeviceIntentSender.observe is called with [${intentSenderToString(sender)}]")
            if (sender != null) {
                // Share Device Step 4: Launch the activity described in the IntentSender that
                // was returned in Step 3 (where the viewModel calls the GPS API to share
                // the device).
                Timber.d("ShareDevice: Launch GPS activity to share device")
                shareDeviceLauncher.launch(IntentSenderRequest.Builder(sender).build())
                deviceViewModel.consumeShareDeviceIntentSender()

                // show alert
//                val builder = MaterialAlertDialogBuilder(this).apply {
//                    setTitle("Share Device")
//                    setMessage("Use pin code [$SETUP_PIN_CODE] and discriminator [$DISCRIMINATOR]. Device id is [$deviceId]. Click finish when done.")
//                    setPositiveButton("Finish") { dialog, which ->
//                        // Respond to positive button press
//                        deviceViewModel.shareDeviceSucceeded()
//                    }
//                    setCancelable(false)
//                }
//                builder.show()
            }
        }
        // CODELAB FEATURED END

        // Background work alert dialog actions.
        deviceViewModel.backgroundWorkAlertDialogAction.observe(this) { action ->
            if (action is BackgroundWorkAlertDialogAction.Show) {
                showBackgroundWorkAlertDialog(action.title, action.message)
            } else if (action is BackgroundWorkAlertDialogAction.Hide) {
                hideBackgroundWorkAlertDialog()
            }
        }

        multiStateSwitch.addStatesFromStrings(List.of("On", "Off"))
//        multiStateSwitch.setOnClickListener(object : View.OnClickListener {
//            override fun onClick(v: View?) {
//                if (deviceUiModel == null) {
//                    return
//                }
//                val deviceData = deviceUiModel!!
//
//                Timber.i("Hi there")
//
//                viewModel.updateDeviceStateOn(deviceData,
//                    multiStateSwitch.getCurrentState().text == "On"
//                )
//            }
//        })
        multiStateSwitch.addStateListener { stateIndex, (text, selectedText, disabledText) ->
            if (deviceUiModel == null) {
                return@addStateListener
            }
            val deviceData = deviceUiModel!!

            // this if statement prevents the app from constantly toggling between states b/c it gets confused after updating state and new device state received that is being read which causes it to trigger again
            // NOTE: this is addStateListener which listens to any state change, not just when I click it
            if ((stateIndex == 0) != deviceData.isOn) {
                Timber.i("State changed")
                viewModel.updateDeviceStateOn(deviceData, stateIndex == 0)
            }
        }
    }

    private fun showBackgroundWorkAlertDialog(title: String?, message: String?) {
        if (title != null) {
            backgroundWorkAlertDialog.setTitle(title)
        }
        if (message != null) {
            backgroundWorkAlertDialog.setMessage(message)
        }
//        backgroundWorkAlertDialog.setCancelable(false)
        backgroundWorkAlertDialog.show()
    }

    private fun hideBackgroundWorkAlertDialog() {
        backgroundWorkAlertDialog.hide()
    }

    override fun onResume() {
        super.onResume()
        if (PERIODIC_READ_INTERVAL_HOME_SCREEN_SECONDS != -1) {
            Timber.i("Starting periodic ping on devices")
            viewModel.startMonitoringStateChanges()
        }

        updateUI()
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause(): Stopping periodic ping on devices")
        viewModel.stopMonitoringStateChanges()
    }

    private fun updateUI() {
        if (deviceUiModel == null) {
            return
        }
        val deviceData = deviceUiModel!!
        val device = deviceData.device

        Timber.i("Device online: ${deviceData.isOnline}")

        nameTxt.text = device.name
        onlineIcon.setTextColor(if (deviceData.isOnline) ContextCompat.getColor(this, R.color.online) else ContextCompat.getColor(this, R.color.offline))
        onlineTxt.text = if (deviceData.isOnline) "Online" else "Offline"

        multiStateSwitch.selectState(if (deviceData.isOn) 0 else 1)

        Timber.i("Thing name: ${deviceData.thingName}")
    }

    private fun pageLoadFail(msg: String) {
        Timber.e(msg)
        goBack()
    }

    private fun goBack() {
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.actuator_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.editDevice -> {
                val intent = Intent(this, EditDeviceActivity::class.java)
                intent.putExtra("deviceId", deviceId)
                intent.putExtra("deviceType", deviceType)
                intent.putExtra("deviceName", deviceName)
                startActivityForResult(intent, 0)
                true
            }
            R.id.shareDevice -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Share Device")
                builder.setMessage("Use device id [$deviceId] when adding the device to the hub. Click continue to proceed.")
                builder.setPositiveButton("Continue") { dialog, which ->
                    viewModel.stopMonitoringStateChanges()
                    try {
                        deviceViewModel.shareDevice(this, deviceId.toLong())
                    } catch (e: Exception) {
                        Timber.e("Error: ${e.message}")

                        // show alert
                        val shareBuilder = MaterialAlertDialogBuilder(this).apply {
                            setTitle("Unable to share device")
                            setMessage("Please check your device is connected properly to the app.")
                            setPositiveButton("Close") { dialog, which ->
                            }
                            setCancelable(true)
                        }
                        shareBuilder.show()
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }
                builder.setCancelable(true)
                builder.setIcon(R.drawable.ic_baseline_share_24)
                // change the color of the positive button
                val dialog: AlertDialog = builder.create()
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.cancel))

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                val name = data?.getStringExtra("deviceName")
                if (name == null) {
                    Timber.e("Device name is null")
                    return
                }
                deviceName = name
                nameTxt.text = deviceName
            }
        }
    }
}