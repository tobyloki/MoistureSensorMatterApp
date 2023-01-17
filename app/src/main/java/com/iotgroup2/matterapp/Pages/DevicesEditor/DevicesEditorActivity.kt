package com.iotgroup2.matterapp.Pages.DevicesEditor

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.iotgroup2.matterapp.Pages.Devices.MatterDeviceViewModel
import com.iotgroup2.matterapp.Pages.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.Pages.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.Pages.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.FragmentDevicesEditorBinding
import com.iotgroup2.matterapp.shared.matter.PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import shared.Models.DeviceModel
import timber.log.Timber


@AndroidEntryPoint
class DevicesEditorActivity : AppCompatActivity() {
    private val _TAG = MatterDeviceViewModel::class.java.simpleName

    private lateinit var _binding: FragmentDevicesEditorBinding

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding

    private val viewModel: MatterActivityViewModel by viewModels()

    private lateinit var deviceId: String
    private lateinit var devicesEditorViewModel: DevicesEditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = FragmentDevicesEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicesEditorViewModel = ViewModelProvider(this).get(DevicesEditorViewModel::class.java)
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
        if(deviceId == null)
            return pageLoadFail("No deviceId extra found")

        var initDevice: DeviceModel? = null
//        devicesEditorViewModel.getDevice(deviceId) { device ->
//            initDevice = device
//            if(initDevice == null)
//                pageLoadFail("Failed to initialize device with id $deviceId")
//
//            initDevice?.let { displayDeviceDetails(it) }
//        } //this param should be passed to this Fragment on page call

        /* Cancel Button */
        val cancelBtn: Button = binding.cancelBtn
        cancelBtn.setOnClickListener {
            goToPreviousPage()
        }

        /* Save Button */
        val saveDeviceBtn: Button = binding.saveBtn
        saveDeviceBtn.setOnClickListener() {
            val updatedDevice: DeviceModel = readDeviceDetails(initDevice!!)
//            devicesEditorViewModel.updateDevice(updatedDevice)

            viewModel.updateDeviceName(updatedDevice.getDeviceId().toLong(), updatedDevice.getDeviceName())

            goToPreviousPage()
        }

        /* Delete Button */
//        val deleteDeviceBtn: Button = binding.deleteDeviceBtn
//        deleteDeviceBtn.setOnClickListener() {
//            devicesEditorViewModel.deleteDevice(deviceId)
//            goToPreviousPage()
//        }

        var deviceUiModel: DeviceUiModel? = null

        // matter device list
        viewModel.devicesUiModelLiveData.observe(this) { devicesUiModel: DevicesUiModel ->
            for (device in devicesUiModel.devices) {
                if (device.device.deviceId.toString() == deviceId) {
                    if (deviceUiModel == null) {
                        deviceUiModel = device

                        val json = JSONObject()
                        json.put("id", device.device.deviceId.toString())
                        json.put("name", device.device.name)
                        json.put("active", true)
                        json.put("wifi", device.device.room)
                        json.put("deviceType", device.device.deviceTypeValue.toString())    // TODO: map to string name

                        // add to devicesList
                        initDevice = (DeviceModel(json))

                        // update ui name text
                        devicesEditorViewModel.name.value = initDevice?.getDeviceName()
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

        Timber.d("*** Main ***")
        if (PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS != -1) {
            Timber.d("Starting periodic ping on devices")
            viewModel.startDevicesPeriodicPing()
        }
    }

    override fun onPause() {
        super.onPause()

        Timber.d("onPause(): Stopping periodic ping on devices")
        viewModel.stopDevicesPeriodicPing()
    }

    private fun pageLoadFail(msg: String) {
        Log.e(_TAG, msg)
        goToPreviousPage()
    }

    private fun goToPreviousPage() {
//        val intent = Intent(this, MatterDeviceFragment::class.java)
//        startActivity(intent)
        finish()
    }

    private fun displayDeviceDetails(device: DeviceModel) {
        /* Name Attribute */
        val name: String = device.getDeviceName()
        binding.nameFieldLayout.editText?.setText(name)

        /* Groups Attribute */
//        val groups: MutableList<String> = device.getDeviceGroups()
//        val groupAdapter: ArrayAdapter<String> = ArrayAdapter(
//            this,
//            android.R.layout.simple_list_item_1,
//            groups
//        )
//        binding.groupDropDownBtn.adapter = groupAdapter


    }

    private fun readDeviceDetails(device: DeviceModel): DeviceModel {
        /* Name Attribute */
        val name: String = binding.nameFieldLayout.editText?.text.toString()
        device.setDeviceName(name)

        /* Groups Attribute */
//        val groupAdapter = binding.groupDropDownBtn.adapter
//        val count = groupAdapter.count
//        val groups: ArrayList<String> = ArrayList()
//        for (i in 0 until count) {
//            groups.add(groupAdapter.getItem(i) as String)
//        }

        return device
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.devices_editor_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.delete_device -> {
                viewModel.removeDevice(deviceId.toLong())
//                devicesEditorViewModel.deleteDevice(deviceId)
                goToPreviousPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}