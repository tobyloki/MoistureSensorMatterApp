package com.iotgroup2.matterapp.Pages.Home

import androidx.lifecycle.*
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.R
import kotlinx.coroutines.*
import org.json.JSONObject
import shared.Models.DeviceModel
import timber.log.Timber


class HomeViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _TAG = HomeViewModel::class.java.simpleName

    /** Encapsulated data **/
    private val _devices = MutableLiveData<List<DevicesListItem>>().apply {
        value = listOf()
    }

    private var devicesList: MutableList<DeviceModel> = mutableListOf()
    private var loadedList = false

    private val matterDevicesViewModelJob = Job()
    private var coroutineScope = CoroutineScope(matterDevicesViewModelJob + Dispatchers.Main)

    /** Public Accessors **/
    val devices : LiveData<List<DevicesListItem>> = _devices

    // Each Device List Entity
    class DevicesListItem {
        var id: String = ""
        var label: String = ""
        var state: Boolean = false
        var online: Boolean = false
        var image: Int = -1

        constructor(id: String, label: String, state: Boolean, online: Boolean, image: Int) {
            this.id = id
            this.label = label
            this.state = state
            this.online = online
            this.image = image
        }

        constructor()
    }

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

//        coroutineScope.launch {
//            try {
//                val awsHttpResponse = CatApi.retrofitService.getDevices().await()
//                devicesList = mutableListOf<DeviceModel>()
//
//                val data = JSONObject(awsHttpResponse).getJSONArray("devices")
//                for ( i in 0 until data.length() ) {
//                    try {
//                        devicesList.add(DeviceModel(data.getJSONObject(i)))
//                    } catch (e : Exception) {
//                        Timber.e("Parsing devices list error: %s", e)
//                    }
//                }
//
//                Timber.i("Devices: $data")
//
//                // Generate mutable DevicesListItem
//                val mutableDevicesList = mutableListOf<DevicesListItem>()
//                devicesList.forEach {
//                    // TODO: The structure of DeviceListItem might be wrong?
//                    mutableDevicesList.add(DevicesListItem(it.getDeviceId(), it.getDeviceName(), false, false, R.drawable.ic_baseline_outlet_24))
//                }
//
//                // Send event to update Devices View
//                _devices.postValue(mutableDevicesList)

//                Timber.i("Devices: ${devices.value}")

                loadedList = true
//            } catch (e : Exception) {
//                e.printStackTrace()
//                Log.e(_TAG, "Async getDevices failed: ${e.localizedMessage}")
//            }
//        }
    }

    fun updateDeviceStates(matterDevices: List<DeviceUiModel>) {
        coroutineScope.launch {
            while(!loadedList) {
                delay(1000)
            }

            for (matterDevice in matterDevices) {
                val device = _devices.value!!.find { it.id == matterDevice.device.deviceId.toString() }
                if (device != null) {
                    device.state = matterDevice.isOn
                    device.online = matterDevice.isOnline

//                    Timber.i("Device: ${matterDevice.device.deviceId}, State: ${matterDevice.isOn}, Online: ${matterDevice.isOnline}")
                }

                // update _devices
                _devices.postValue(_devices.value)
            }
        }
    }

    fun addDevice(matterDevices: List<DeviceUiModel>) {
        // check if a new device is added (find first missing device)
        coroutineScope.launch {
            while(!loadedList) {
                // sleep for 1 second
                withContext(Dispatchers.IO) {
                    Thread.sleep(1000)
                }
            }

            val mutableDevicesList = mutableListOf<DevicesListItem>()

            for (matterDevice in matterDevices) {
                    Timber.i("New device found id: ${matterDevice.device.deviceId}")
                    // create Device json object
                    val json = JSONObject()
                    json.put("id", matterDevice.device.deviceId.toString())
                    json.put("name", matterDevice.device.name)
                    json.put("active", true)
                    json.put("wifi", matterDevice.device.room)
                    json.put("deviceType", matterDevice.device.deviceTypeValue.toString())    // TODO: map to string name

                    // add to devicesList
                    val device = (DeviceModel(json))
                    devicesList.add(device)
                    mutableDevicesList.add(DevicesListItem(device.getDeviceId(), device.getDeviceName(), matterDevice.isOn, matterDevice.isOnline, R.drawable.ic_std_device))
            }

            // Send event to update Devices View
            _devices.postValue(mutableDevicesList)
        }
    }
}