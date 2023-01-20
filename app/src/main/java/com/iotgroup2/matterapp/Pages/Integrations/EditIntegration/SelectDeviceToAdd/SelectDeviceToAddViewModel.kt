package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.SelectDeviceToAdd

import androidx.lifecycle.*
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationViewModel
import kotlinx.coroutines.*
import java.io.Serializable

class SelectDeviceToAddViewModel : ViewModel(), DefaultLifecycleObserver {
    /** Encapsulated data **/
    val deviceList = MutableLiveData<List<SelectDeviceItem>>().apply {
        value = listOf()
    }

    private val matterDevicesViewModelJob = Job()
    private var coroutineScope = CoroutineScope(matterDevicesViewModelJob + Dispatchers.Main)

    // Each Integration List Entity
    class SelectDeviceItem : Serializable {
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
        getDeviceList()
    }

    private fun getDeviceList() {
        // add 2 integrations
        deviceList.value = listOf(
            SelectDeviceItem("1", "Node 1"),
            SelectDeviceItem("2", "Node 2")
        )
    }
}