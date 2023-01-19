package com.iotgroup2.matterapp.Pages.Integrations

import androidx.lifecycle.*
import kotlinx.coroutines.*
import shared.Models.DeviceModel
import java.io.Serializable

class IntegrationsViewModel : ViewModel(), DefaultLifecycleObserver {
    /** Encapsulated data **/
    val integrations = MutableLiveData<List<IntegrationListItem>>().apply {
        value = listOf()
    }

    private val matterDevicesViewModelJob = Job()
    private var coroutineScope = CoroutineScope(matterDevicesViewModelJob + Dispatchers.Main)

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
        getIntegrations()
    }

    private fun getIntegrations() {
        // add 2 integrations
        integrations.value = listOf(
            IntegrationListItem("1", "Integration 1"),
            IntegrationListItem("2", "Integration 2")
        )
    }
}