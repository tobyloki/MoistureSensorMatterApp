package com.iotgroup2.matterapp.Pages.Home.Device.Sensor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iotgroup2.matterapp.Device.DeviceType
import com.iotgroup2.matterapp.Pages.Home.Device.DeviceViewModel
import com.iotgroup2.matterapp.Pages.Home.EditDevice.EditDeviceActivity
import com.iotgroup2.matterapp.Pages.Units.UnitsActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivitySensorBinding
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.shared.Utility.Utility
import com.iotgroup2.matterapp.shared.matter.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class SensorActivity : AppCompatActivity() {
    private lateinit var _binding: ActivitySensorBinding

    private lateinit var deviceId: String
    private var deviceType: Int = DeviceType.TYPE_UNSPECIFIED_VALUE
    private lateinit var deviceName: String

    private lateinit var nameTxt: TextView
    private lateinit var onlineIcon: TextView
    private lateinit var onlineTxt: TextView

    private lateinit var tempValueTxt: TextView
    private lateinit var tempUnitTxt: TextView
    private lateinit var humidityValueTxt: TextView
    private lateinit var humidityUnitTxt: TextView
    private lateinit var pressureValueTxt: TextView
    private lateinit var pressureUnitTxt: TextView
    private lateinit var soilMoistureValueTxt: TextView
    private lateinit var soilMoistureUnitTxt: TextView
    private lateinit var lightValueTxt: TextView
    private lateinit var lightUnitTxt: TextView

    private lateinit var batteryValueTxt: TextView

    private lateinit var tempInfoBtn: ImageButton
    private lateinit var humidityInfoBtn: ImageButton
    private lateinit var pressureInfoBtn: ImageButton
    private lateinit var soilMoistureInfoBtn: ImageButton
    private lateinit var lightInfoBtn: ImageButton

    private val viewModel: MatterActivityViewModel by viewModels()
    private var deviceUiModel: DeviceUiModel? = null

    private lateinit var deviceViewModel: DeviceViewModel

    // The ActivityResultLauncher that launches the "shareDevice" activity in Google Play Services.
    private lateinit var shareDeviceLauncher: ActivityResultLauncher<IntentSenderRequest>

    // Background work dialog.
    private lateinit var backgroundWorkAlertDialog: AlertDialog

    private lateinit var sensorActivityViewModel: SensorActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySensorBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        // Background Work AlertDialog.
        backgroundWorkAlertDialog = MaterialAlertDialogBuilder(this).create()

        nameTxt = _binding.nameTxt
        onlineIcon  = _binding.onlineIcon
        onlineTxt = _binding.onlineTxt
        tempValueTxt = _binding.tempValueTxt
        tempUnitTxt = _binding.tempUnitTxt
        humidityValueTxt = _binding.humidityValueTxt
        humidityUnitTxt = _binding.humidityUnitTxt
        pressureValueTxt = _binding.pressureValueTxt
        pressureUnitTxt = _binding.pressureUnitTxt
        soilMoistureValueTxt = _binding.soilMoistureValueTxt
        soilMoistureUnitTxt = _binding.soilMoistureUnitTxt
        lightValueTxt = _binding.lightValueTxt
        lightUnitTxt = _binding.lightUnitTxt

        batteryValueTxt = _binding.batteryValueTxt

        tempInfoBtn = _binding.tempInfoBtn
        humidityInfoBtn = _binding.humidityInfoBtn
        pressureInfoBtn = _binding.pressureInfoBtn
        soilMoistureInfoBtn = _binding.soilMoistureInfoBtn
        lightInfoBtn = _binding.lightInfoBtn

        /* Initialize Data */
        val extras: Bundle? = intent.extras
        if(extras == null)
            return pageLoadFail("No extras passed to page")
        deviceId = extras.getString("deviceId").toString()
        deviceType = extras.getInt("deviceType")
        Timber.i("Device ID: $deviceId")
        Timber.i("Device Type: $deviceType")

        deviceName = extras.getString("deviceName").toString()
        val deviceOnline = extras.getBoolean("deviceOnline")
        Timber.i("ONline: $deviceOnline")

        nameTxt.text = deviceName
        onlineIcon.setTextColor(if (deviceOnline) ContextCompat.getColor(this, R.color.online) else ContextCompat.getColor(this, R.color.offline))
        onlineTxt.text = if (deviceOnline) "Online" else "Offline"

        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)
        lifecycle.addObserver(deviceViewModel)

        deviceViewModel.setOnline(deviceOnline)
        deviceViewModel.deviceOnline.observe(this) { online ->
            onlineIcon.setTextColor(if (online) ContextCompat.getColor(this, R.color.online) else ContextCompat.getColor(this, R.color.offline))
            onlineTxt.text = if (online) "Online" else "Offline"
        }

        sensorActivityViewModel = ViewModelProvider(this, SensorActivityViewModelFactory(deviceId)).get(
            SensorActivityViewModel::class.java)
        lifecycle.addObserver(sensorActivityViewModel)

        // TODO: temporarily disabled until backend and hub updated to handle all 5 sensors
        sensorActivityViewModel.temperature.observe(this) {
            if (it != null) {
                Timber.i("Temperature: $it")

                val unit = Utility.getUnit(this)

                if(unit) {
                    tempValueTxt.text = String.format("%.1f", it.toDouble())
                } else {
                    tempValueTxt.text = String.format("%.1f", Utility.convertCelsiusToFahrenheit(it.toDouble()))
                }

                Timber.i("Temperature 3 : ${tempValueTxt.text}")
            }
        }
        sensorActivityViewModel.humidity.observe(this) {
            if (it != null) {
                Timber.i("Humidity: $it")
                humidityValueTxt.text = String.format("%.1f", it.toDouble())
            }
        }
        sensorActivityViewModel.pressure.observe(this) {
            if (it != null) {
                Timber.i("Pressure: $it")

                val unit = Utility.getUnit(this)

                if (unit) {
                    pressureValueTxt.text = String.format("%.1f", Utility.convert_inHg_to_kPA(it.toDouble()))
                } else {
                    pressureValueTxt.text = String.format("%.1f", it.toDouble())
                }
            }
        }
        sensorActivityViewModel.soilMoisture.observe(this) {
            if (it != null) {
                Timber.i("Soil Moisture: $it")
                soilMoistureValueTxt.text = it.toString()
            }
        }
        sensorActivityViewModel.light.observe(this) {
            if (it != null) {
                Timber.i("Light: $it")
                lightValueTxt.text = String.format("%.1f", it.toDouble())
            }
        }

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

        tempInfoBtn.setOnClickListener {
            // show alert with info
            AlertDialog.Builder(this)
                .setTitle("Temperature")
                .setMessage("This metric reports the temperature of the air.")
                .setPositiveButton("Close") { dialog, which -> }
                .setIcon(R.drawable.ic_baseline_info_24)
                .setCancelable(true)
                .show()
        }

        humidityInfoBtn.setOnClickListener {
            // show alert with info
            AlertDialog.Builder(this)
                .setTitle("Moisture")
                .setMessage("This metric reports the humidity of the air.")
                .setPositiveButton("Close") { dialog, which -> }
                .setIcon(R.drawable.ic_baseline_info_24)
                .setCancelable(true)
                .show()
        }

        pressureInfoBtn.setOnClickListener {
            // show alert with info
            AlertDialog.Builder(this)
                .setTitle("Air Pressure")
                .setMessage("This metric reports the pressure of the air.")
                .setPositiveButton("Close") { dialog, which -> }
                .setIcon(R.drawable.ic_baseline_info_24)
                .setCancelable(true)
                .show()
        }

        soilMoistureInfoBtn.setOnClickListener {
            // show alert with info
            AlertDialog.Builder(this)
                .setTitle("Soil Moisture")
                .setMessage("This metric reports the moisture of the soil.")
                .setPositiveButton("Close") { dialog, which -> }
                .setIcon(R.drawable.ic_baseline_info_24)
                .setCancelable(true)
                .show()
        }

        lightInfoBtn.setOnClickListener {
            // show alert with info
            AlertDialog.Builder(this)
                .setTitle("Light")
                .setMessage("This metric reports the light intensity.")
                .setPositiveButton("Close") { dialog, which -> }
                .setIcon(R.drawable.ic_baseline_info_24)
                .setCancelable(true)
                .show()
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

    private var lastTemp: Double = 0.0;
    private var lastPressure: Double = 0.0;

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        deviceViewModel.startTimer()

        // start the timer that'll fetch sensor data from timestream
        sensorActivityViewModel.startTimer()

        if (deviceUiModel == null) {
            return
        }
        val deviceData = deviceUiModel!!
        val device = deviceData.device

        nameTxt.text = device.name
        onlineIcon.setTextColor(if (deviceData.isOnline) ContextCompat.getColor(this, R.color.online) else ContextCompat.getColor(this, R.color.offline))
        onlineTxt.text = if (deviceData.isOnline) "Online" else "Offline"

        Timber.i("ONline f: ${deviceData.isOnline}")
        deviceViewModel.setOnline(deviceData.isOnline)


        val unit = Utility.getUnit(this)
        Timber.i("Unit: $unit")
        if (unit) {
            tempUnitTxt.text = "°C"
            pressureUnitTxt.text = "kPa"

        } else {
            tempUnitTxt.text = "°F"
            pressureUnitTxt.text = "inHg"
        }

        // TODO: if not all data is reported at the same time, then default values are automatically 0 for some reason
        if (deviceData.temperature != 0) {
            lastTemp = deviceData.temperature.toDouble()

            if(unit) {
                tempValueTxt.text = String.format("%.1f", deviceData.temperature.toDouble() / 100)
            } else {
                tempValueTxt.text = String.format("%.1f", Utility.convertCelsiusToFahrenheit(deviceData.temperature.toDouble() / 100))
            }
        } else {
            if(unit) {
                tempValueTxt.text = String.format("%.1f", lastTemp / 100)
            } else {
                tempValueTxt.text = String.format("%.1f", Utility.convertCelsiusToFahrenheit(lastTemp / 100))
            }
        }
        if (deviceData.humidity != 0) {
            humidityValueTxt.text = String.format("%.1f", deviceData.humidity.toDouble() / 100)
        }
        if (deviceData.pressure != 0) {
            lastPressure = deviceData.pressure.toDouble()

            if (unit) {
                pressureValueTxt.text = String.format("%.1f", Utility.convert_inHg_to_kPA(deviceData.pressure.toDouble() / 10))
            } else {
                pressureValueTxt.text = String.format("%.1f", deviceData.pressure.toDouble() / 10)
            }
        } else {
            if (unit) {
                pressureValueTxt.text = String.format("%.1f", Utility.convert_inHg_to_kPA(lastPressure.toDouble() / 10))
            } else {
                pressureValueTxt.text = String.format("%.1f", lastPressure / 10)
            }
        }
        if (deviceData.soilMoisture != 0) {
            soilMoistureValueTxt.text = (deviceData.soilMoisture / 10).toString()
        }
        if (deviceData.light != 0) {
            val value = ( kotlin.math.exp((deviceData.light - 1).toDouble() / 10000) )
            // format to 1 decimal place
            lightValueTxt.text = String.format("%.1f", value)
        }

        if (deviceData.battery != 0) {
            batteryValueTxt.text = deviceData.battery.toString()
        }

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
                intent.putExtra("deviceName", deviceName)
                startActivityForResult(intent, 0)
                true
            }
            R.id.changeUnits -> {
                val intent = Intent(this, UnitsActivity::class.java)
                startActivity(intent)
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