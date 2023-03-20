package com.iotgroup2.matterapp.Pages.Home.EditDevice

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.MainActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityDeviceEditBinding
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.shared.matter.PERIODIC_READ_INTERVAL_HOME_SCREEN_SECONDS
import dagger.hilt.android.AndroidEntryPoint
import shared.Models.DeviceModel
import timber.log.Timber

@AndroidEntryPoint
class EditDeviceActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityDeviceEditBinding

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding

    private val viewModel: MatterActivityViewModel by viewModels()

    private lateinit var deviceId: String
    private var deviceType: Int = Device.DeviceType.TYPE_UNSPECIFIED_VALUE
    private lateinit var devicesEditorViewModel: EditDeviceViewModel

    private var deviceUiModel: DeviceUiModel? = null

    private lateinit var nameFieldLayout: TextInputLayout
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityDeviceEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nameFieldLayout = binding.nameFieldLayout
        cancelBtn = binding.cancelBtn
        saveBtn = binding.saveBtn

        /* Initialize Data */
        val extras: Bundle? = intent.extras
        if(extras == null)
            return pageLoadFail("No extras passed to page")
        deviceId = extras.getString("deviceId").toString()
        deviceType = extras.getInt("deviceType")
        Timber.i("Device ID: $deviceId")
        Timber.i("Device Type: $deviceType")

        val deviceName = extras.getString("deviceName")

        if (deviceName != null) {
            nameFieldLayout.editText?.setText(deviceName)
        }

        devicesEditorViewModel = ViewModelProvider(this, EditDeviceViewModelFactory(deviceId, deviceType)).get(
            EditDeviceViewModel::class.java)
        lifecycle.addObserver(devicesEditorViewModel)

        devicesEditorViewModel.finishedSaving.observe(this) {
            if (it) {
                // send result name back to main activity
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("deviceName", nameFieldLayout.editText?.text.toString().trim())
                setResult(RESULT_OK, intent)

                goBack()
            }
        }
        devicesEditorViewModel.finishedDeleting.observe(this) {
            if (it) {
                try {
                    // remove from matter list
                    viewModel.removeDevice(deviceId.toLong())
                } catch (e: Exception) {
                    Timber.e("Error removing device from matter list: ${e.message}")
                }

                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }

        /* Cancel Button */
        cancelBtn.setOnClickListener {
            goBack()
        }

        /* Save Button */
        saveBtn.setOnClickListener() {
            val name = nameFieldLayout.editText?.text.toString().trim()
            devicesEditorViewModel.saveName(name)

            if (deviceUiModel != null) {
                viewModel.updateDeviceName(deviceId.toLong(), name)
            } else {
                Timber.e("Device UI Model is null")
                return@setOnClickListener
            }
        }

        // matter device list
        viewModel.devicesUiModelLiveData.observe(this) { devicesUiModel: DevicesUiModel ->
            for (device in devicesUiModel.devices) {
                if (device.device.deviceId.toString() == deviceId) {
                    if (deviceUiModel == null) {
                        deviceUiModel = device
                        // update ui name text
                        nameFieldLayout.editText?.setText(device.device.name)
                    }
                }
            }
        }

//        // on button
//        val onBtn: Button = binding.onBtn
//        onBtn.setOnClickListener() {
//            deviceUiModel?.let {
//                viewModel.updateDeviceStateOn(it, true, null, null, null, null, null)
//            }
//        }
//
//        // off button
//        val offBtn: Button = binding.offBtn
//        offBtn.setOnClickListener() {
//            deviceUiModel?.let {
//                viewModel.updateDeviceStateOn(it, false, null, null, null, null, null)
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        if (PERIODIC_READ_INTERVAL_HOME_SCREEN_SECONDS != -1) {
            Timber.i("Starting periodic ping on devices")
            viewModel.startMonitoringStateChanges()
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause(): Stopping periodic ping on devices")
        viewModel.stopMonitoringStateChanges()
    }

    private fun pageLoadFail(msg: String) {
        Timber.e(msg)
        goBack()
    }

    private fun goBack() {
        finish()
    }

    private fun readDeviceDetails(device: DeviceModel): DeviceModel {
        /* Name Attribute */
        val name: String = binding.nameFieldLayout.editText?.text.toString()
        device.setDeviceName(name)

        return device
    }

    private fun deleteDeviceConfirmation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Device")
        builder.setMessage("Are you sure you want to delete this device?")
        builder.setPositiveButton("Confirm") { dialog, which ->
            devicesEditorViewModel.deleteDevice()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        // change the color of the positive button
        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.warn))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.cancel))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit_device_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.deleteDevice -> {
                // ask for confirmation
                deleteDeviceConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}