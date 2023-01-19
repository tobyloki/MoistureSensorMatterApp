package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration

import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.io.Serializable

class EditIntegrationViewModel : ViewModel(), DefaultLifecycleObserver {
    /** Encapsulated data **/
    val ifList = MutableLiveData<List<IfListItem>>().apply {
        value = listOf()
    }
    val thenList = MutableLiveData<List<ThenListItem>>().apply {
        value = listOf()
    }

    private val matterDevicesViewModelJob = Job()
    private var coroutineScope = CoroutineScope(matterDevicesViewModelJob + Dispatchers.Main)

    // Each Integration List Entity
    class IfListItem : Serializable {
        var id: String = ""
        var label: String = ""

        constructor(id: String, label: String) {
            this.id = id
            this.label = label
        }

        constructor()
    }

    class ThenListItem : Serializable {
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
        getIfList()
        getThenList()
    }

    private fun getIfList() {
        // add 2 integrations
        ifList.value = listOf(
            IfListItem("1", "Node 1"),
            IfListItem("2", "Node 2")
        )
    }

    private fun getThenList() {
        // add 2 integrations
        thenList.value = listOf(
            ThenListItem("1", "Actuator 1"),
            ThenListItem("2", "Actuator 2")
        )
    }
}