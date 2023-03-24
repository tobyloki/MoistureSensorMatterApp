package com.iotgroup2.matterapp.Pages.Units

import android.content.Context
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.davidmiguel.multistateswitch.MultiStateSwitch
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityDeviceEditBinding
import com.iotgroup2.matterapp.databinding.ActivityUnitsBinding
import com.iotgroup2.matterapp.shared.Utility.Utility
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.List

class UnitsActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityUnitsBinding

    private lateinit var unitSw : MultiStateSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityUnitsBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        unitSw = _binding.unitSwitch

        val unit = Utility.getUnit(this)
        Timber.i("Unit: $unit")

        unitSw.selectState(if (unit) 1 else 0)

        unitSw.addStatesFromStrings(List.of("Imperial (°F)", "Metric (°C)"))
        unitSw.addStateListener { stateIndex, (text, selectedText, disabledText) ->
            setUnit(stateIndex == 1)    // false is imperial, true is metric
        }
    }

    private fun setUnit(unit: Boolean) {
        Timber.i("Setting unit to $unit")
        // save to shared preferences
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putBoolean(getString(R.string.unit_key), unit)
            apply()
        }
    }
}