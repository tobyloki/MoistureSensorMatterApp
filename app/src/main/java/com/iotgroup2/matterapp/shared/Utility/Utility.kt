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
    }
}