package com.iotgroup2.matterapp.Pages.Home.Device

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.home.matter.commissioning.SharedDeviceData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iotgroup2.matterapp.Device.DeviceType
import com.iotgroup2.matterapp.MainActivity
import com.iotgroup2.matterapp.Pages.Home.Device.EditDevice.EditDeviceActivity
import com.iotgroup2.matterapp.Pages.Units.UnitsActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityDeviceBinding
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.shared.Utility.Utility
import com.iotgroup2.matterapp.shared.matter.*
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import shared.Models.DeviceModel
import timber.log.Timber

@AndroidEntryPoint
class DeviceActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityDeviceBinding

    private lateinit var deviceId: String
    private var deviceType: Int = DeviceType.TYPE_UNSPECIFIED_VALUE

    private lateinit var nameTxt: TextView
    private lateinit var onlineIcon: TextView
    private lateinit var onlineTxt: TextView
    private lateinit var tempValueTxt: TextView
    private lateinit var tempUnitTxt: TextView
    private lateinit var moistureValueTxt: TextView
    private lateinit var airPressureValueTxt: TextView
    private lateinit var airPressureUnitTxt: TextView
    private lateinit var batteryValueTxt: TextView

    private val viewModel: MatterActivityViewModel by viewModels()
    private var deviceUiModel: DeviceUiModel? = null

    private lateinit var deviceViewModel: DeviceViewModel

    // The ActivityResultLauncher that launches the "shareDevice" activity in Google Play Services.
    private lateinit var shareDeviceLauncher: ActivityResultLauncher<IntentSenderRequest>

    // Background work dialog.
    private lateinit var backgroundWorkAlertDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        // Background Work AlertDialog.
        backgroundWorkAlertDialog = MaterialAlertDialogBuilder(this).create()

        nameTxt = _binding.nameTxt
        onlineIcon  = _binding.onlineIcon
        onlineTxt = _binding.onlineTxt
        tempValueTxt = _binding.tempValueTxt
        tempUnitTxt = _binding.tempUnitTxt
        moistureValueTxt = _binding.moistureValueTxt
        airPressureValueTxt = _binding.airPressureValueTxt
        airPressureUnitTxt = _binding.airPressureUnitTxt
        batteryValueTxt = _binding.batteryValueTxt

        /* Initialize Data */
        val extras: Bundle? = intent.extras
        if(extras == null)
            return pageLoadFail("No extras passed to page")
        deviceId = extras.getString("deviceId").toString()
        deviceType = extras.getInt("deviceType")
        Timber.i("Device ID: $deviceId")
        Timber.i("Device Type: $deviceType")

        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)

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
                viewModel.startDevicesPeriodicPing()
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
        if (PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS != -1) {
            Timber.i("Starting periodic ping on devices")
            viewModel.startDevicesPeriodicPing()
        }

        val unit = Utility.getUnit(this)
        Timber.i("Unit: $unit")
        if (!unit) {
            tempUnitTxt.text = "°C"
        } else {
            tempUnitTxt.text = "°F"
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause(): Stopping periodic ping on devices")
        viewModel.stopDevicesPeriodicPing()
    }

    private fun updateUI() {
        if (deviceUiModel == null) {
            return
        }
        val deviceData = deviceUiModel!!
        val device = deviceData.device

        nameTxt.text = device.name
        onlineIcon.setTextColor(if (deviceData.isOnline) ContextCompat.getColor(this, R.color.online) else ContextCompat.getColor(this, R.color.offline))
        onlineTxt.text = if (deviceData.isOnline) "Online" else "Offline"

        tempValueTxt.text = deviceData.temperature.toString()
        moistureValueTxt.text = deviceData.humidity.toString()
        airPressureValueTxt.text = deviceData.pressure.toString()
        batteryValueTxt.text = deviceData.battery.toString()

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
        inflater.inflate(R.menu.device_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.editDevice -> {
                val intent = Intent(this, EditDeviceActivity::class.java)
                intent.putExtra("deviceId", deviceId)
                intent.putExtra("deviceType", deviceType)
                startActivity(intent)
                true
            }
            R.id.changeUnits -> {
                val intent = Intent(this, UnitsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.shareDevice -> {
                viewModel.stopDevicesPeriodicPing()
                deviceViewModel.shareDevice(this, deviceId.toLong())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}