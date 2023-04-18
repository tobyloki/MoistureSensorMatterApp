package com.iotgroup2.matterapp.Pages.Home.Device.Sensor

import androidx.lifecycle.*
import com.google.ar.core.dependencies.e
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
    val humidity = MutableLiveData<Int>().apply {
        value = 0
    }
    val pressure = MutableLiveData<Int>().apply {
        value = 0
    }
    val soilMoisture = MutableLiveData<Int>().apply {
        value = 0
    }
    val light = MutableLiveData<Int>().apply {
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

                var temperatureTime: String? = null
                var humidityTime: String? = null
                var pressureTime: String? = null
                var soilMoistureTime: String? = null
                var lightTime: String? = null

                // run on main thread
                launch(Dispatchers.Main) {
                    val data = JSONObject(httpResponse)
                    try {
                        temperature.value = data.getInt("temperature")
                        temperatureTime = data.getString("temperatureTime")
                    } catch (_: Exception) {
                    }
                    try {
                        humidity.value = data.getInt("humidity")
                        humidityTime = data.getString("humidityTime")
                    } catch (_: Exception) {
                    }
                    try {
                        pressure.value = data.getInt("pressure")
                        pressureTime = data.getString("pressureTime")
                    } catch (_: Exception) {
                    }
                    try {
                        soilMoisture.value = data.getInt("soilMoisture")
                        soilMoistureTime = data.getString("soilMoistureTime")
                    } catch (_: Exception) {
                    }
                    try {
                        light.value = data.getInt("light")
                        lightTime = data.getString("lightTime")
                    } catch (_: Exception) {
                    }

                    Timber.i("temperature: ${temperature.value}")
                    Timber.i("humidity: ${humidity.value}")
                    Timber.i("pressure: ${pressure.value}")
                    Timber.i("soilMoisture: ${soilMoisture.value}")
                    Timber.i("light: ${light.value}")

                    Timber.i("temperatureTime: $temperatureTime")
                    Timber.i("humidityTime: $humidityTime")
                    Timber.i("pressureTime: $pressureTime")
                    Timber.i("soilMoistureTime: $soilMoistureTime")
                    Timber.i("lightTime: $lightTime")
                }
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
                startTimer()
            }
        }, 10000)
    }

    private fun cancelTimer() {
        try {
            timer.cancel()
            Timber.i("Timer cancelled")
        } catch (_: Exception) {}
    }
}