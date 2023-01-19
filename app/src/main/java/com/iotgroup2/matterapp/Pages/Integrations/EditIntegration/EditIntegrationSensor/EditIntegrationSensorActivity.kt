package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationSensor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityEditIntegrationSensorBinding

class EditIntegrationSensorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditIntegrationSensorBinding

    private lateinit var tempSpinner: Spinner
    private lateinit var tempEditTxt: EditText
    private lateinit var moistureSpinner: Spinner
    private lateinit var moistureValueSpinner: Spinner
    private lateinit var airPressureSpinner: Spinner
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIntegrationSensorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tempSpinner = binding.tempSpinner
        tempEditTxt = binding.tempEditTxt
        moistureSpinner = binding.moistureSpinner
        moistureValueSpinner = binding.moistureValueSpinner
        airPressureSpinner = binding.airPressureSpinner
        cancelBtn = binding.cancelBtn
        saveBtn = binding.saveBtn

        // populate tempSpinner with "Above", "Below", "Disabled"
        val tempSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Above", "Below", "Disabled"))
        tempSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tempSpinner.adapter = tempSpinnerAdapter

        // populate moistureSpinner with "Is", "Disabled"
        val moistureSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Is", "Disabled"))
        moistureSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        moistureSpinner.adapter = moistureSpinnerAdapter

        // populate moistureValueSpinner with "Wet", "Moist", "Dry", "Disabled"
        val moistureValueSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Wet", "Moist", "Dry", "Disabled"))
        moistureValueSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        moistureValueSpinner.adapter = moistureValueSpinnerAdapter

        // populate airPressureSpinner with "Above", "Below", "Disabled"
        val airPressureSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Above", "Below", "Disabled"))
        airPressureSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        airPressureSpinner.adapter = airPressureSpinnerAdapter

        cancelBtn.setOnClickListener {
            goBack()
        }
        saveBtn.setOnClickListener {
            goBack()
        }
    }

    private fun goBack() {
        finish()
    }
}