package com.iotgroup2.matterapp.Pages.Home.Device.EditDevice

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.iotgroup2.matterapp.MainActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityDeviceEditBinding
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.shared.matter.PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS
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
    private lateinit var devicesEditorViewModel: EditDeviceViewModel

    private var deviceUiModel: DeviceUiModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityDeviceEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicesEditorViewModel = ViewModelProvider(this).get(EditDeviceViewModel::class.java)
        lifecycle.addObserver(devicesEditorViewModel)

        val textView = binding.nameFieldLayout

        devicesEditorViewModel.name.observe(this) {
            // set the text of the text view to the value of the text property
            textView.editText?.setText(it)
        }

        /* Initialize Data */
        val extras: Bundle? = intent.extras
        if(extras == null)
            return pageLoadFail("No extras passed to page")
        deviceId = extras.getString("deviceId").toString()
        Timber.i("Device ID: $deviceId")

        /* Cancel Button */
        val cancelBtn: Button = binding.cancelBtn
        cancelBtn.setOnClickListener {
            goBack()
        }

        /* Save Button */
        val saveDeviceBtn: Button = binding.saveBtn
        saveDeviceBtn.setOnClickListener() {
            val name = binding.nameFieldLayout.editText?.text.toString().trim()
            viewModel.updateDeviceName(deviceId.toLong(), name)
            goBack()
        }

        // matter device list
        viewModel.devicesUiModelLiveData.observe(this) { devicesUiModel: DevicesUiModel ->
            for (device in devicesUiModel.devices) {
                if (device.device.deviceId.toString() == deviceId) {
                    if (deviceUiModel == null) {
                        deviceUiModel = device
                        // update ui name text
                        devicesEditorViewModel.name.value = device.device.name
                    }
                }
            }
        }

        // on button
        val onBtn: Button = binding.onBtn
        onBtn.setOnClickListener() {
            deviceUiModel?.let {
                viewModel.updateDeviceStateOn(it, true)
            }
        }

        // off button
        val offBtn: Button = binding.offBtn
        offBtn.setOnClickListener() {
            deviceUiModel?.let {
                viewModel.updateDeviceStateOn(it, false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS != -1) {
            Timber.i("Starting periodic ping on devices")
            viewModel.startDevicesPeriodicPing()
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause(): Stopping periodic ping on devices")
        viewModel.stopDevicesPeriodicPing()
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
            viewModel.removeDevice(deviceId.toLong())
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
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