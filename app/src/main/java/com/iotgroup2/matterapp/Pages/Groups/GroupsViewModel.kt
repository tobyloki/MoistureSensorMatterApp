package com.iotgroup2.matterapp.Pages.Groups

import android.util.Log
import androidx.lifecycle.*
import com.iotgroup2.matterapp.Pages.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.R
import kotlinx.coroutines.*
import org.json.JSONObject
import shared.Models.DeviceModel
import shared.Utility.CatApi
import timber.log.Timber
import java.io.Serializable

class GroupsViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _TAG = GroupsViewModel::class.java.simpleName

    /** Encapsulated data **/
    private val _devices = MutableLiveData<List<DevicesListItem>>().apply {
        value = listOf()
    }

    val groups = MutableLiveData<List<String>>().apply {
        value = listOf()
    }
    val selectedGroup = MutableLiveData<Int>().apply {
        value = 0
    }

    var devicesList: MutableList<DeviceModel> = mutableListOf()
    private var loadedList = false

    private val matterDevicesViewModelJob = Job()
    private var coroutineScope = CoroutineScope(matterDevicesViewModelJob + Dispatchers.Main)

    /** Public Accessors **/
    val devices : LiveData<List<DevicesListItem>> = _devices

    // Each Device List Entity
    class DevicesListItem : Serializable {
        var id: String = ""
        var label: String = ""
        var state: Boolean = false
        var online: Boolean = false
        var image: Int = -1

        constructor(id: String, label: String, state: Boolean, online: Boolean, image: Int) {
            this.id = id
            this.label = label
            this.state = state
            this.online = online
            this.image = image
        }

        constructor()
    }

    /** Lifecycle Handlers **/
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }
}