package com.iotgroup2.matterapp.Pages.Home.Device.EditDevice

import androidx.lifecycle.*
import com.iotgroup2.matterapp.Pages.Home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class EditDeviceViewModel : ViewModel(), DefaultLifecycleObserver {
    private val matterDevicesViewModelJob = Job()
    private var coroutineScope = CoroutineScope(matterDevicesViewModelJob + Dispatchers.Main)

    val name = MutableLiveData<String>().apply {
        value = "My Device"
    }
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }
}