package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.SelectDeviceToAdd

import androidx.lifecycle.*
import com.iotgroup2.matterapp.Device
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTP
import timber.log.Timber
import java.io.Serializable

class SelectDeviceToAddViewModelFactory(private val deviceType: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectDeviceToAddViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectDeviceToAddViewModel(deviceType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SelectDeviceToAddViewModel(private val deviceType: Int) : ViewModel(), DefaultLifecycleObserver {
    /** Encapsulated data **/
    val deviceList = MutableLiveData<List<SelectDeviceItem>>().apply {
        value = listOf()
    }

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    // Each Integration List Entity
    class SelectDeviceItem : Serializable {
        var id: String = ""
        var label: String = ""
        var _version: Int = 0

        constructor(id: String, label: String, _version: Int) {
            this.id = id
            this.label = label
            this._version = _version
        }

        constructor()
    }

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        getList(deviceType)
    }

    private fun getList(deviceType: Int) {
        // add 2 integrations
//        deviceList.value = listOf(
//            SelectDeviceItem("1", "Node 1"),
//            SelectDeviceItem("2", "Node 2")
//        )

        coroutineScope.launch {
            try {
                var queryStr = ""
                if (deviceType == Device.DeviceType.TYPE_LIGHT_VALUE) {
                    queryStr = "listSensors"
                } else {
                    queryStr = "listActuators"
                }

                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "query MyQuery {\n" +
                        "  $queryStr {\n" +
                        "    items {\n" +
                        "      id\n" +
                        "      name\n" +
                        "      _version\n" +
                        "      integrationID\n" +
                        "    }\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTP.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                val items = JSONObject(httpResponse).getJSONObject("data").getJSONObject(queryStr).getJSONArray("items")

                deviceList.value = listOf()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item.getString("id")
                    val label = item.getString("name")
                    val _version = item.getInt("_version")

                    val integrationID = item.getString("integrationID")
                    // if item doesn't belong to an integration yet, add it here

                    if (integrationID == "null") {
                        deviceList.value = deviceList.value?.plus(SelectDeviceItem(id, label, _version))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}