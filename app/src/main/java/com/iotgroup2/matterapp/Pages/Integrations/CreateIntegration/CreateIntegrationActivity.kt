package com.iotgroup2.matterapp.Pages.Integrations.CreateIntegration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.iotgroup2.matterapp.Pages.Home.Device.EditDevice.EditDeviceActivity
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActivity
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationViewModel
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityCreateIntegrationBinding
import com.iotgroup2.matterapp.databinding.ActivityEditIntegrationBinding

class CreateIntegrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateIntegrationBinding

    private lateinit var viewModel: CreateIntegrationViewModel

    private lateinit var nameFieldLayout : TextInputLayout
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateIntegrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nameFieldLayout = binding.nameFieldLayout
        cancelBtn = binding.cancelBtn
        saveBtn = binding.saveBtn

        viewModel = ViewModelProvider(this).get(CreateIntegrationViewModel::class.java)
        lifecycle.addObserver(viewModel)

        viewModel.finishedCreating.observe(this) {
            if (it) {
                val intent = Intent(this, EditIntegrationActivity::class.java)
                intent.putExtra("integrationId", viewModel.integrationId.value)
                startActivity(intent)
                goBack()
            }
        }

        cancelBtn.setOnClickListener {
            goBack()
        }

        saveBtn.setOnClickListener {
            val name = nameFieldLayout.editText?.text.toString().trim()
            viewModel.createIntegration(name)
        }
    }

    private fun goBack() {
        finish()
    }
}