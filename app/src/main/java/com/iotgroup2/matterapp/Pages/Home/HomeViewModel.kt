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
import shared.Utility.HTTPGraphQL
import timber.log.Timber


class HomeViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _TAG = HomeViewModel::class.java.simpleName

    /** Encapsulated data **/
    private val _devices = MutableLiveData<List<DevicesListItem>>().apply {
        value = listOf()
    }

    var loadedList: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        value = false
    }

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    /** Public Accessors **/
    val devices : LiveData<List<DevicesListItem>> = _devices

    // Each Device List Entity
    class DevicesListItem {
        var id: String = ""
        var name: String = ""
        var type: Int = DeviceType.TYPE_UNSPECIFIED_VALUE
        var online: Boolean = false
        var thingName: String = ""

        constructor(id: String, label: String, type: Int, online: Boolean, thingName: String) {
            this.id = id
            this.name = label
            this.type = type
            this.online = online
            this.thingName = thingName
        }
    }

    class Item(var id: String, var thingName: String, var name: String, var type: Int, var _version: Int) {
    }

    private var itemList = mutableListOf<Item>()

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        getDevicesFromBackend()
    }

    private fun getDevicesFromBackend() {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "query MyQuery {\n" +
                        "  listSensors {\n" +
                        "    items {\n" +
                        "      id\n" +
                        "      thingName\n" +
                        "      name\n" +
                        "      _deleted\n" +
                        "      _version\n" +
                        "    }\n" +
                        "  }\n" +
                        "  listActuators {\n" +
                        "    items {\n" +
                        "      id\n" +
                        "      name\n" +
                        "      _deleted\n" +
                        "      _version\n" +
                        "    }\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                val root = JSONObject(httpResponse).getJSONObject("data")
                val sensors = root.getJSONObject("listSensors").getJSONArray("items")
                val actuators = root.getJSONObject("listActuators").getJSONArray("items")

                itemList = mutableListOf<Item>()

                for (i in 0 until sensors.length()) {
                    val sensor = sensors.getJSONObject(i)
                    val thingName = sensor.getString("thingName")
                    val _version = sensor.getInt("_version")
                    Timber.i("thingName: $thingName")
                    try {
                        val _deleted = sensor.getBoolean("_deleted")
                        if (!_deleted) {
                            itemList.add(Item(sensor.getString("id"), thingName, sensor.getString("name"), Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE, _version))
                        }
                    } catch (e: Exception) {
//                        Timber.e(e)
                        // means _deleted is null, therefore it is false
                        itemList.add(Item(sensor.getString("id"), thingName, sensor.getString("name"), Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE, _version))
                    }
                }
                for (i in 0 until actuators.length()) {
                    val actuator = actuators.getJSONObject(i)
                    val _version = actuator.getInt("_version")
                    try {
                        val _deleted = actuator.getBoolean("_deleted")
                        if (!_deleted) {
                            itemList.add(Item(actuator.getString("id"), "", actuator.getString("name"), Device.DeviceType.TYPE_UNKNOWN_VALUE, _version))
                        }
                    } catch (e: Exception) {
//                        Timber.e(e)
                        // means _deleted is null, therefore it is false
                        itemList.add(Item(actuator.getString("id"), "", actuator.getString("name"), Device.DeviceType.TYPE_UNKNOWN_VALUE, _version))
                    }
                }

                // show preliminary list
                val mutableDevicesList = mutableListOf<HomeViewModel.DevicesListItem>()

                for (item in itemList) {
                    // device is not added to ui list, so add it
                    Timber.i("Device ${item.id} is not added to ui list. Adding to ui list anyway so that it can still be manually deleted.")
                    mutableDevicesList.add(
                        HomeViewModel.DevicesListItem(
                            item.id,
                            item.name,
                            item.type,
                            false,
                            item.thingName
                        )
                    );
                }

                // Send event to update Devices View
                _devices.postValue(mutableDevicesList)

                loadedList.postValue(true)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun updateDeviceStates(matterDevices: List<DeviceUiModel>) {
        coroutineScope.launch {
            while(!loadedList.value!!) {
                delay(1000)
            }

//            for (matterDevice in matterDevices) {
//                val device = _devices.value!!.find { it.id == matterDevice.device.deviceId.toString() }
//                if (device != null) {
////                    device.state = matterDevice.isOn
//                    device.online = matterDevice.isOnline
//
//                    Timber.i("Device: ${matterDevice.device.deviceId}, State: ${matterDevice.isOn}, Online: ${matterDevice.isOnline}")
//                }

                // update _devices
                _devices.postValue(_devices.value)
//            }
        }
    }

    fun addDevice(matterDevices: List<DeviceUiModel>) {
        // check if a new device is added (find first missing device)
        coroutineScope.launch {
            Timber.i("step 1")
            while(!loadedList.value!!) {
                // sleep for 1 second
                withContext(Dispatchers.IO) {
                    Thread.sleep(1000)
                }
            }
            Timber.i("step 2")

            val mutableDevicesList = mutableListOf<HomeViewModel.DevicesListItem>()

            for (matterDevice in matterDevices) {
                val id = matterDevice.device.deviceId.toString()
                val thingName = matterDevice.thingName
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

                Timber.i("Device: $id, ThingName: $thingName, Name: $name, Type: $deviceType, Added: $addedToBackend")

                // if addedToBackend is true, check if thingName is has changed
                if (addedToBackend) {
                    val item = itemList.find { it.id == matterDevice.device.deviceId.toString() }
                    if (item != null) {
                        // store thingName in hex format (e.g. 0506)
                        val thingNameHex = String.format("%04x", thingName)

                        if (item.thingName != thingNameHex) {
                            if (deviceType == DeviceType.TYPE_TEMPERATURE_SENSOR_VALUE || deviceType == DeviceType.TYPE_PRESSURE_SENSOR_VALUE || deviceType == DeviceType.TYPE_HUMIDITY_SENSOR_VALUE || deviceType == DeviceType.TYPE_SOIL_MOISTURE_SENSOR_VALUE || deviceType == DeviceType.TYPE_LIGHT_SENSOR_VALUE) {
                                if(thingName != 0) {
                                    Timber.i("Device thingName has changed. Updating thingName from ${item.thingName} to $thingNameHex")

                                    updateThingName(id, thingNameHex, item._version);
                                }
                            }
                        }
                    }
                } else {
                    Timber.i("New device found: ($id, $thingName, $name, $deviceType). Adding to backend...")

                    // add to backend
                    addDeviceToBackend(id, thingName.toString(), name, deviceType)
                }

                // add to ui list
                mutableDevicesList.add(
                    HomeViewModel.DevicesListItem(
                        id,
                        name,
                        deviceType,
                        matterDevice.isOnline,
                        thingName.toString()
                    )
                );
            }

            for (item in itemList) {
                // check if device is added to ui list
                val device = mutableDevicesList.find { it.id == item.id }

                if (device == null) {
                    // device is not added to ui list, so add it
                    Timber.i("Device ${item.id} is not added to ui list. Adding to ui list anyway so that it can still be manually deleted.")
                    mutableDevicesList.add(
                        HomeViewModel.DevicesListItem(
                            item.id,
                            item.name,
                            item.type,
                            false,
                            item.thingName
                        )
                    );
                }
            }

            // Send event to update Devices View
            _devices.postValue(mutableDevicesList)
        }
    }

    private fun updateThingName(id: String, thingName: String, _version: Int) {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  updateSensor(input: {id: \"$id\", thingName: \"$thingName\", _version: $_version}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                Timber.i("json: $json")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun addDeviceToBackend(id: String, thingName: String, name: String, deviceType: Int) {
        if (deviceType == DeviceType.TYPE_UNSPECIFIED_VALUE || deviceType == DeviceType.TYPE_UNKNOWN_VALUE) {
            Timber.i("Device type is unspecified or unknown. Not adding to backend.")
            return
        }

        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                var query = ""
                var thingNameQuery = ""
                if (deviceType == DeviceType.TYPE_TEMPERATURE_SENSOR_VALUE || deviceType == DeviceType.TYPE_PRESSURE_SENSOR_VALUE || deviceType == DeviceType.TYPE_HUMIDITY_SENSOR_VALUE || deviceType == DeviceType.TYPE_SOIL_MOISTURE_SENSOR_VALUE || deviceType == DeviceType.TYPE_LIGHT_SENSOR_VALUE) {
                    query = "createSensor"
                    thingNameQuery = "thingName: \"$thingName\","
                } else {
                    query = "createActuator"
                }

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  $query(input: {id: \"$id\", $thingNameQuery name: \"$name\"}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                Timber.i("json: $json")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                // refresh devices list
                getDevicesFromBackend()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}