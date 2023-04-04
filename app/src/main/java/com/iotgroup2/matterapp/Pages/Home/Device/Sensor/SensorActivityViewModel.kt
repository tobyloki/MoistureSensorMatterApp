package com.iotgroup2.matterapp.Pages.Home.Device.Sensor

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import shared.Utility.HTTPRest
import timber.log.Timber
import java.util.*

class SensorActivityViewModelFactory(private val deviceId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorActivityViewModel(deviceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SensorActivityViewModel(private val deviceId: String) : ViewModel(), DefaultLifecycleObserver {
    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val temperature = MutableLiveData<Int>().apply {
        value = 0
    }
    val pressure = MutableLiveData<Int>().apply {
        value = 0
    }
    val moisture = MutableLiveData<Int>().apply {
        value = 0
    }

    private var timer = Timer()

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        startTimer()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        cancelTimer()
    }

    private fun getSensorData() {
        // do something in background
        coroutineScope.launch {
            try {
                Timber.i("Sending fetch sensor data request")

                val httpResponse = HTTPRest.retrofitService.fetchSensorData(deviceId).await()
                Timber.i("data: $httpResponse")

                val data = JSONObject(httpResponse)
                temperature.value = data.getInt("temperature")
                pressure.value = data.getInt("pressure")
                moisture.value = data.getInt("moisture")

                val temperatureTime = data.getString("temperatureTime")
                val pressureTime = data.getString("pressureTime")
                val moistureTime = data.getString("moistureTime")

                Timber.i("temperatureTime: $temperatureTime")
                Timber.i("pressureTime: $pressureTime")
                Timber.i("moistureTime: $moistureTime")
            } catch (e: Exception) {
                Timber.e("Error: ${e.message}")
            }
        }
    }

    fun startTimer() {
        cancelTimer()

        // start timer to fetch sensor data every 5 seconds
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                getSensorData()
            }
        }, 0, 5000)
    }

    private fun cancelTimer() {
        try {
            timer.cancel()
        } catch (_: Exception) {}
    }
}