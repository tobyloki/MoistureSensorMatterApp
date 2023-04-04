package com.iotgroup2.matterapp.Pages.Integrations.CreateIntegration

import androidx.lifecycle.*
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTPGraphQL
import timber.log.Timber

class CreateIntegrationViewModel : ViewModel(), DefaultLifecycleObserver {
    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val integrationId = MutableLiveData<String>()
    val finishedCreating = MutableLiveData<Boolean>().apply {
        value = false
    }

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
    }

    fun createIntegration(name: String) {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  createIntegration(input: {name: \"$name\"}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                Timber.i("Sending: $json")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                val data = JSONObject(httpResponse).getJSONObject("data").getJSONObject("createIntegration")
                integrationId.value = data.getString("id")
                finishedCreating.value = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}