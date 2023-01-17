package com.iotgroup2.matterapp.Pages.Schedule

import androidx.lifecycle.*

class ScheduleViewModel : ViewModel(), DefaultLifecycleObserver {

    val schedules = MutableLiveData<List<ScheduleListItem>>().apply {
        value = listOf()
    }

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

        val schedules = mutableListOf<ScheduleListItem>()
        schedules.add(ScheduleListItem("12:00 AM", true))
        schedules.add(ScheduleListItem("1:00 AM", false))
        schedules.add(ScheduleListItem("2:00 AM", true))
        schedules.add(ScheduleListItem("3:00 AM", false))

        this.schedules.postValue(schedules)
    }

}