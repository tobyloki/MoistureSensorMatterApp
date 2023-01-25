package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.iotgroup2.matterapp.databinding.ActivityEditIntegrationActuatorBinding
import timber.log.Timber

class EditIntegrationActuatorActivity : AppCompatActivity() {
    private lateinit var binding : ActivityEditIntegrationActuatorBinding

    private lateinit var viewModel: EditIntegrationViewModel

    private lateinit var nameTxt: TextView
    private lateinit var tempEditTxt: EditText
    private lateinit var timeSpinner: Spinner
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIntegrationActuatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get integrationId, deviceId from intent
        val method = intent.getStringExtra("method")
        val integrationId = intent.getStringExtra("integrationId")
        val deviceId = intent.getStringExtra("deviceId")
        val name = intent.getStringExtra("name")
        var _version = intent.getIntExtra("_version", 0)

        Timber.i("name: $name")

        nameTxt = binding.nameTxt
        tempEditTxt = binding.tempEditTxt
        timeSpinner = binding.timeSpinner
        cancelBtn = binding.cancelBtn
        saveBtn = binding.saveBtn

        viewModel = ViewModelProvider(this, EditIntegrationViewModelFactory(method!!, integrationId!!, deviceId!!)).get(EditIntegrationViewModel::class.java)
        lifecycle.addObserver(viewModel)

        viewModel.finishedSaving.observe(this) {
            if (it) {
                goBack()
            }
        }
        viewModel.name.observe(this) {
            nameTxt.text = it
        }
        viewModel.expirationValue.observe(this) {
            tempEditTxt.setText(it.toString())
        }
        viewModel.expirationGranularity.observe(this) {
            // get selected position based on value
            val position = when (it) {
                Granularity.MINUTES -> 0
                Granularity.HOURS -> 1
                Granularity.DAYS -> 2
                else -> 0
            }
            timeSpinner.setSelection(position)
        }
        viewModel._version.observe(this) {
            _version = it
        }

        nameTxt.text = name

        // populate timeSpinner with "Minutes", "Hours", "Days"
        val timeSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf(Granularity.MINUTES, Granularity.HOURS, Granularity.DAYS))
        timeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSpinner.adapter = timeSpinnerAdapter

        cancelBtn.setOnClickListener {
            goBack()
        }
        saveBtn.setOnClickListener {
            // this means we are adding the actuator
            val expirationValue = tempEditTxt.text.toString().toInt()
            val expirationGranularity = timeSpinner.selectedItem.toString()
            val action = Action.OFF

            Timber.i("expirationValue: $expirationValue")
            Timber.i("expirationGranularity: $expirationGranularity")
            Timber.i("action: $action")

            viewModel.addActuatorToIntegration(
                _version,
                expirationValue,
                expirationGranularity,
                action
            )
        }
    }

    private fun goBack() {
        finish()
    }
}