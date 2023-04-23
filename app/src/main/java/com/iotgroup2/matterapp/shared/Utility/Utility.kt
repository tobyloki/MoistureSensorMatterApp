package com.iotgroup2.matterapp.shared.Utility

import android.app.Activity
import android.content.Context
import com.iotgroup2.matterapp.R

class Utility {
    companion object {
        fun getUnit(activity: Activity): Boolean {
            val sharedPref = activity.getSharedPreferences("userPrefs", Context.MODE_PRIVATE) ?: return false
            return sharedPref.getBoolean(activity.getString(R.string.unit_key), false)
        }

        fun convertCelsiusToFahrenheit(celsius: Double): Double {
            return (celsius * 9 / 5) + 32
        }

        fun convert_inHg_to_kPA(inHg: Double): Double {
            return inHg * 3.38639
        }
    }
}