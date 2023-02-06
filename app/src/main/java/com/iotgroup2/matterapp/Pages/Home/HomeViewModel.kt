package com.iotgroup2.matterapp.Pages.Home

import android.annotation.SuppressLint
import androidx.lifecycle.*
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Device.DeviceType
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTP
import timber.log.Timber


class HomeViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _TAG = HomeViewModel::class.java.simpleName

    /** Encapsulated data **/
    private val _devices = MutableLiveData<List<DevicesListItem>>().apply {
        value = listOf()
    }

    private var loadedList = false

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    /** Public Accessors **/
    val devices : LiveData<List<DevicesListItem>> = _devices

    // Each Device List Entity
    class DevicesListItem {
        var id: String = ""
        var label: String = ""
        var type: Int = DeviceType.TYPE_UNSPECIFIED_VALUE
        var state: Boolean = false
        var online: Boolean = false

        constructor(id: String, label: String, type: Int, state: Boolean, online: Boolean) {
            this.id = id
            this.label = label
            this.type = type
            this.state = state
            this.online = online
        }

        constructor()
    }

    class Item(var id: String, var name: String, var type: Int) {
    }

    private var itemList = mutableListOf<Item>()

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "query MyQuery {\n" +
                        "  listSensors {\n" +
                        "    items {\n" +
                        "      id\n" +
                        "      name\n" +
                        "      _deleted\n" +
                        "    }\n" +
                        "  }\n" +
                        "  listActuators {\n" +
                        "    items {\n" +
                        "      id\n" +
                        "      name\n" +
                        "      _deleted\n" +
                        "    }\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTP.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                val root = JSONObject(httpResponse).getJSONObject("data")
                val sensors = root.getJSONObject("listSensors").getJSONArray("items")
                val actuators = root.getJSONObject("listActuators").getJSONArray("items")

                itemList = mutableListOf<Item>()

                for (i in 0 until sensors.length()) {
                    val sensor = sensors.getJSONObject(i)
                    try {
                        val _deleted = sensor.getBoolean("_deleted")
                        if (!_deleted) {
                            itemList.add(Item(sensor.getString("id"), sensor.getString("name"), Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE))
                        }
                    } catch (e: Exception) {
//                        Timber.e(e)
                        // means _deleted is null, therefore it is false
                        itemList.add(Item(sensor.getString("id"), sensor.getString("name"), Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE))
                    }
                }
                for (i in 0 until actuators.length()) {
                    val actuator = actuators.getJSONObject(i)
                    try {
                    val _deleted = actuator.getBoolean("_deleted")
                        if (!_deleted) {
                            itemList.add(Item(actuator.getString("id"), actuator.getString("name"), Device.DeviceType.TYPE_UNKNOWN_VALUE))
                        }
                    } catch (e: Exception) {
//                        Timber.e(e)
                        // means _deleted is null, therefore it is false
                        itemList.add(Item(actuator.getString("id"), actuator.getString("name"), Device.DeviceType.TYPE_UNKNOWN_VALUE))
                    }
                }

                loadedList = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
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
            Timber.i("step 1")
            while(!loadedList) {
                // sleep for 1 second
                withContext(Dispatchers.IO) {
                    Thread.sleep(1000)
                }
            }
            Timber.i("step 2")

            val mutableDevicesList = mutableListOf<DevicesListItem>()

            for (matterDevice in matterDevices) {
                val id = matterDevice.device.deviceId.toString()
                val name = matterDevice.device.name
                var deviceType = matterDevice.device.deviceTypeValue
                Timber.i("asdf device type: $deviceType")

                var addedToBackend = false
                for (item in itemList) {
                    if (item.id == matterDevice.device.deviceId.toString()) {
                        addedToBackend = true
                        // typically, if device has already been added, the deviceType will always be 1
                        // so get the correct deviceType from itemList, which was determined from backend data
                        deviceType = item.type
                        break
                    }
                }

                Timber.i("Device: $id, Name: $name, Type: $deviceType, Added: $addedToBackend")
                if (!addedToBackend) {
                    Timber.i("New device found: ($id, $name, $deviceType). Adding to backend...")

                    // add to backend
                    addDeviceToBackend(id, name, deviceType)
                }

                // add to ui list
                mutableDevicesList.add(DevicesListItem(id, name, deviceType, matterDevice.isOn, matterDevice.isOnline))
            }

            // Send event to update Devices View
            _devices.postValue(mutableDevicesList)
        }
    }

    private fun addDeviceToBackend(id: String, name: String, deviceType: Int) {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                var query = ""
                if (deviceType == DeviceType.TYPE_HUMIDITY_SENSOR_VALUE) {
                    query = "createSensor"
                } else {
                    query = "createActuator"
                }

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  $query(input: {id: \"$id\", name: \"$name\"}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTP.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}