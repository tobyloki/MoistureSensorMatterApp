package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.iotgroup2.matterapp.MainActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityEditIntegrationActuatorBinding

class EditIntegrationActuatorActivity : AppCompatActivity() {
    private lateinit var binding : ActivityEditIntegrationActuatorBinding

    private lateinit var zone1Spinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIntegrationActuatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        zone1Spinner = binding.zone1Spinner
        timeSpinner = binding.timeSpinner
        cancelBtn = binding.cancelBtn
        saveBtn = binding.saveBtn

        // populate zone1Spinner with "On" and "Off"
        val zone1SpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("On", "Off"))
        zone1SpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        zone1Spinner.adapter = zone1SpinnerAdapter

        // populate timeSpinner with "Minutes", "Hours", "Days"
        val timeSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Minutes", "Hours", "Days"))
        timeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSpinner.adapter = timeSpinnerAdapter

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