package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration

import androidx.lifecycle.*
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import shared.Utility.HTTPGraphQL
import timber.log.Timber
import java.io.Serializable

class EditIntegrationViewModelFactory(private val integrationId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditIntegrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditIntegrationViewModel(integrationId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class EditIntegrationViewModel(private val integrationId: String) : ViewModel(), DefaultLifecycleObserver {
    /** Encapsulated data **/
    val ifList = MutableLiveData<List<IfListItem>>().apply {
        value = listOf()
    }
    val thenList = MutableLiveData<List<ThenListItem>>().apply {
        value = listOf()
    }

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private var originalName = ""
    val name = MutableLiveData<String>().apply {
        value = ""
    }
    private var _version = 0

    // Each Integration List Entity
    class IfListItem : Serializable {
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

    class ThenListItem : Serializable {
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

    val finishedSaving = MutableLiveData<Boolean>().apply {
        value = false
    }
    val finishedDeleting = MutableLiveData<Boolean>().apply {
        value = false
    }

    var loadedList: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        value = false
    }

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        getIntegrationData()
    }

    private fun getIntegrationData() {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "query MyQuery {\n" +
                        "  getIntegration(id: \"$integrationId\") {\n" +
                        "    name\n" +
                        "    _version\n" +
                        "    Actuators {\n" +
                        "      items {\n" +
                        "        id\n" +
                        "        name\n" +
                        "        _version\n" +
                        "      }\n" +
                        "    }\n" +
                        "    Sensors {\n" +
                        "      items {\n" +
                        "        id\n" +
                        "        name\n" +
                        "        _version\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                val root = JSONObject(httpResponse).getJSONObject("data").getJSONObject("getIntegration")
                originalName = root.getString("name")
                name.value = root.getString("name")
                _version = root.getInt("_version")
                val actuators = root.getJSONObject("Actuators").getJSONArray("items")
                val sensors = root.getJSONObject("Sensors").getJSONArray("items")

                getIfList(sensors)
                getThenList(actuators)

                loadedList.value = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun getIfList(sensors: JSONArray) {
        // add 2 integrations
//        ifList.value = listOf(
//            IfListItem("1", "Node 1"),
//            IfListItem("2", "Node 2")
//        )

        ifList.value = listOf()
        for (i in 0 until sensors.length()) {
            val sensor = sensors.getJSONObject(i)
            val id = sensor.getString("id")
            val label = sensor.getString("name")
            val _version = sensor.getInt("_version")
            val ifListItem = IfListItem(id, label, _version)
            ifList.value = ifList.value?.plus(ifListItem)
        }
    }

    private fun getThenList(actuators: JSONArray) {
        // add 2 integrations
//        thenList.value = listOf(
//            ThenListItem("1", "Actuator 1"),
//            ThenListItem("2", "Actuator 2")
//        )

        thenList.value = listOf()
        for (i in 0 until actuators.length()) {
            val actuator = actuators.getJSONObject(i)
            val id = actuator.getString("id")
            val label = actuator.getString("name")
            val _version = actuator.getInt("_version")
            val thenListItem = ThenListItem(id, label, _version)
            thenList.value = thenList.value?.plus(thenListItem)
        }
    }

    fun saveName(name: String) {
        if (name != originalName) {
            coroutineScope.launch {
                try {
                    Timber.i("Sending http request to graphql endpoint...")

                    val json = JSONObject()
                    json.put("query", "mutation MyMutation {\n" +
                            "  updateIntegration(input: {id: \"$integrationId\", name: \"$name\", _version: $_version}) {\n" +
                            "    id\n" +
                            "  }\n" +
                            "}")
                    Timber.i("Sending: $json")
                    val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                    val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                    Timber.i("data: $httpResponse")

                    finishedSaving.value = true
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        } else {
            finishedSaving.value = true
        }
    }

    fun deleteIntegration() {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  deleteIntegration(input: {id: \"$integrationId\", _version: $_version}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                finishedDeleting.value = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}