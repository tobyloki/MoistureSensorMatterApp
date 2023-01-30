package com.iotgroup2.matterapp.Pages.Home.Device.EditDevice

import android.view.InputDevice.getDevice
import androidx.lifecycle.*
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Pages.Home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTP
import timber.log.Timber

class EditDeviceViewModelFactory(private val deviceId: String, private val deviceType: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditDeviceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditDeviceViewModel(deviceId, deviceType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class EditDeviceViewModel(val deviceId: String, val deviceType: Int) : ViewModel(), DefaultLifecycleObserver {
    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val finishedSaving = MutableLiveData<Boolean>().apply {
        value = false
    }
    val finishedDeleting = MutableLiveData<Boolean>().apply {
        value = false
    }

    private var _version : Int? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        getDevice(deviceId, deviceType)
    }

    fun getDevice(id: String, deviceType: Int) {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")
                Timber.i("deviceType: $deviceType")

                var query = ""
                if (deviceType == Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE) {
                    query = "getSensor"
                } else {
                    query = "getActuator"
                }

                val json = JSONObject()
                json.put("query", "query MyQuery {\n" +
                        "  $query(id: \"$id\") {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    _version\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTP.retrofitService.query(body).await()
                Timber.i("data: $httpResponse"
                )
                val data = JSONObject(httpResponse).getJSONObject("data").getJSONObject(query)
                _version = data.getInt("_version")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun saveName(name: String) {
        if (_version == null) {
            Timber.e("Version is null, not saving")
            return
        }

        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                var query = ""
                if (deviceType == Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE) {
                    query = "updateSensor"
                } else {
                    query = "updateActuator"
                }

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  $query(input: {id: \"$deviceId\", name: \"$name\", _version: $_version}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTP.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                finishedSaving.value = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun deleteDevice() {
        if (_version == null) {
//            Timber.e("Version is null, not deleting")
//            return
            Timber.e("Version is null, but still proceeding")
        }

        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                var query = ""
                if (deviceType == Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE) {
                    query = "deleteSensor"
                } else {
                    query = "deleteActuator"
                }

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  $query(input: {id: \"$deviceId\", _version: $_version}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTP.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                finishedDeleting.value = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}