package com.iotgroup2.matterapp.Pages.Integrations

import androidx.lifecycle.*
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTP
import timber.log.Timber
import java.io.Serializable

class IntegrationsViewModel : ViewModel(), DefaultLifecycleObserver {
    /** Encapsulated data **/
    val integrations = MutableLiveData<List<IntegrationListItem>>().apply {
        value = listOf()
    }

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    // Each Integration List Entity
    class IntegrationListItem : Serializable {
        var id: String = ""
        var label: String = ""

        constructor(id: String, label: String) {
            this.id = id
            this.label = label
        }

        constructor()
    }

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Timber.i("Resuming IntegrationsViewModel")
        getIntegrations()
    }

    private fun getIntegrations() {
        // add 2 integrations
//        integrations.value = listOf(
//            IntegrationListItem("1", "Integration 1"),
//            IntegrationListItem("2", "Integration 2")
//        )

        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "query MyQuery {\n" +
                        "  listIntegrations {\n" +
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

                val data = JSONObject(httpResponse).getJSONObject("data").getJSONObject("listIntegrations").getJSONArray("items")
                val integrationList = mutableListOf<IntegrationListItem>()
                for (i in 0 until data.length()) {
                    val integration = data.getJSONObject(i)
                    try {
                        val _deleted = integration.getBoolean("_deleted")
                        if (!_deleted) {
                            integrationList.add(
                                IntegrationListItem(
                                    integration.getString("id"),
                                    integration.getString("name")
                                )
                            )
                        }
                    } catch (e: Exception) {
//                        Timber.e(e)
                        // means _deleted is null, therefore it is false
                        integrationList.add(
                            IntegrationListItem(
                                integration.getString("id"),
                                integration.getString("name")
                            )
                        )
                    }
                }
                integrations.value = integrationList
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}