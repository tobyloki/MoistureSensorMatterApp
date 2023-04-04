package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator

import androidx.lifecycle.*
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.SelectDeviceToAdd.SelectDeviceToAddViewModel
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTPGraphQL
import timber.log.Timber

class Method {
    companion object {
        const val ADD = "ADD"
        const val EDIT = "EDIT"
    }
}

class Action {
    companion object {
        const val ON = "ON"
        const val OFF = "OFF"
    }
}

class Granularity {
    companion object {
        const val MINUTES = "MINUTES"
        const val HOURS = "HOURS"
        const val DAYS = "DAYS"
    }
}

class EditIntegrationViewModelFactory(private val method: String, private val integrationId: String, private val deviceId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditIntegrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditIntegrationViewModel(method, integrationId, deviceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class EditIntegrationViewModel(private val method: String, private val integrationId: String, private val deviceId: String) : ViewModel(), DefaultLifecycleObserver {
    /** Encapsulated data **/
    val deviceList = MutableLiveData<List<SelectDeviceToAddViewModel.SelectDeviceItem>>().apply {
        value = listOf()
    }

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val finishedSaving = MutableLiveData<Boolean>().apply {
        value = false
    }

    val name = MutableLiveData<String>()
    val expirationValue = MutableLiveData<Int>().apply {
        value = 1
    }

    val expirationGranularity = MutableLiveData<String>().apply {
        value = Granularity.DAYS
    }

    val _version = MutableLiveData<Int>().apply {
        value = 0
    }

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        getActuatorData()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
    }

    fun getActuatorData() {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "query MyQuery {\n" +
                        "  getActuator(id: \"$deviceId\") {\n" +
                        "    name\n" +
                        "    expirationValue\n" +
                        "    expirationGranularity\n" +
                        "    _version\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                val root = JSONObject(httpResponse).getJSONObject("data").getJSONObject("getActuator")
                name.value = root.getString("name")

                // don't update these fields if we are adding a new actuator, leave the default options for the user
                if (method == "EDIT") {
                    expirationValue.value = root.getInt("expirationValue")
                    expirationGranularity.value = root.getString("expirationGranularity")
                }

                _version.value = root.getInt("_version")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun addActuatorToIntegration(_version: Int, expirationValue: Int, expirationGranularity: String, action: String = Action.OFF) {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  updateActuator(input: {id: \"$deviceId\", integrationID: \"$integrationId\", expirationValue: $expirationValue, expirationGranularity: $expirationGranularity, action: $action, _version: $_version}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                Timber.i("json: $json")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                finishedSaving.value = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}