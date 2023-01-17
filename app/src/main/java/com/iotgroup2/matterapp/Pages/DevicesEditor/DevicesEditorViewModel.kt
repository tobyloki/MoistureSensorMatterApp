package com.iotgroup2.matterapp.Pages.DevicesEditor

import android.util.Log
import androidx.lifecycle.*
import com.iotgroup2.matterapp.Pages.Devices.MatterDeviceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Models.DeviceModel
import shared.Utility.CatApi

class DevicesEditorViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _TAG = MatterDeviceViewModel::class.java.simpleName
    private val matterDevicesViewModelJob = Job()
    private var coroutineScope = CoroutineScope(matterDevicesViewModelJob + Dispatchers.Main)

    public val name = MutableLiveData<String>().apply {
        value = "My Device"
    }

//    var groups = MutableLiveData<List<String>>()
//    var schedules = MutableLiveData<List<ScheduleListItem>>()

    // Each Device List Entity
    class ScheduleListItem {
        var time: String = ""
        var enabled: Boolean = false

        constructor(time: String, enabled: Boolean) {
            this.time = time
            this.enabled = enabled
        }

        constructor()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        // initialize schedule list with dummy data
//        val schedules = mutableListOf<ScheduleListItem>()
//        schedules.add(ScheduleListItem("12:00 AM", true))
//        schedules.add(ScheduleListItem("1:00 AM", false))
//        this.schedules.postValue(schedules)
    }

//    fun getDevice(deviceId: String, completion: (DeviceModel?) -> Unit) {
//        var device: DeviceModel? = null
//        coroutineScope.launch {
//            try {
//                val responseJson = CatApi.retrofitService.getDeviceById(deviceId).await()
//                Log.i(_TAG, "Async getDeviceById success: $responseJson")
//                val deviceJson = JSONObject(responseJson).getJSONObject("device")
//                device = DeviceModel(deviceJson)
//
//                // update groups
////                groups.postValue(device?.getDeviceGroups())
//
//                completion(device)
//            } catch (e : Exception) {
//                e.printStackTrace()
//                Log.e(_TAG, "Async getDeviceById failed: ${e.localizedMessage}")
//                completion(null)
//            }
//        }
//    }

//    fun updateDevice(device: DeviceModel) {
//        coroutineScope.launch {
//            try {
//                val deviceId = device.getDeviceId()
//                val deviceJson = device.deviceToJson()
//                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), deviceJson)
//                val awsHttpResponse = CatApi.retrofitService.updateDeviceById(deviceId, body).await()
//                Log.e(_TAG, "Async updateDeviceById success: $awsHttpResponse")
//            } catch (e : Exception) {
//                e.printStackTrace()
//                Log.e(_TAG, "Async updateDeviceById failed: ${e.localizedMessage}")
//            }
//        }
//    }

//    fun deleteDevice(deviceId: String) {
//        coroutineScope.launch {
//            try {
//                val awsHttpResponse = CatApi.retrofitService.deleteDeviceById(deviceId).await()
//                Log.e(_TAG, "Async deleteDeviceById success: $awsHttpResponse")
//            } catch (e : Exception) {
//                e.printStackTrace()
//                Log.e(_TAG, "Async deleteDeviceById failed: ${e.localizedMessage}")
//            }
//        }
//    }
}