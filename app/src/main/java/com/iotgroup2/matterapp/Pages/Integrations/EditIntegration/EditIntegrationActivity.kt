package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator.EditIntegrationActuatorActivity
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationSensor.EditIntegrationSensorActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityEditIntegrationBinding
import com.iotgroup2.matterapp.databinding.FragmentIntegrationsBinding

class EditIntegrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditIntegrationBinding

    private lateinit var ifList: RecyclerView
    private lateinit var thenList: RecyclerView
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var ifAddBtn: FloatingActionButton
    private lateinit var thenAddBtn: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIntegrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ifList = binding.recyclerView2
        thenList = binding.thenRecyclerView
        cancelBtn = binding.cancelBtn
        saveBtn = binding.saveBtn
        ifAddBtn = binding.ifAddBtn
        thenAddBtn = binding.thenAddBtn

        ifList.layoutManager = GridLayoutManager(this, 1)
        thenList.layoutManager = GridLayoutManager(this, 1)

        val viewModel = ViewModelProvider(this).get(EditIntegrationViewModel::class.java)
        lifecycle.addObserver(viewModel)

        viewModel.ifList.observe(this) {
            ifList.adapter = EditIntegrationIfAdapter(this, it)
        }
        viewModel.thenList.observe(this) {
            thenList.adapter = EditIntegrationThenAdapter(this, it)
        }

        ifAddBtn.setOnClickListener {
            val intent = Intent(this, EditIntegrationSensorActivity::class.java)
            intent.putExtra("deviceType", Device.DeviceType.TYPE_LIGHT_VALUE)
            startActivity(intent)
        }
        thenAddBtn.setOnClickListener {
            val intent = Intent(this, EditIntegrationActuatorActivity::class.java)
            intent.putExtra("deviceType", Device.DeviceType.TYPE_UNKNOWN)
            startActivity(intent)
        }

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit_integration_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun deleteIntegrationConfirmation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Integration")
        builder.setMessage("Are you sure you want to delete this integration?")
        builder.setPositiveButton("Confirm") { dialog, which ->
            goBack()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        // change the color of the positive button
        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.warn))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.cancel))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.deleteIntegration -> {
                // ask for confirmation
                deleteIntegrationConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}