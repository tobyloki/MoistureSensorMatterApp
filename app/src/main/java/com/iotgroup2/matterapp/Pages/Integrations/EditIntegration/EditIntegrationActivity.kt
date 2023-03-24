package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.SelectDeviceToAdd.SelectDeviceToAddActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityEditIntegrationBinding
import timber.log.Timber

class EditIntegrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditIntegrationBinding

    private lateinit var viewModel: EditIntegrationViewModel

    private lateinit var nameFieldLayout : TextInputLayout
    private lateinit var ifList: RecyclerView
    private lateinit var thenList: RecyclerView
    private lateinit var ifAddBtn: ImageButton
    private lateinit var thenAddBtn: ImageButton
    private lateinit var doneBtn: Button
    private lateinit var spinner: SpinKitView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIntegrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get extra data
        val integrationId = intent.getStringExtra("integrationId")

        nameFieldLayout = binding.nameFieldLayout
        ifList = binding.recyclerView2
        thenList = binding.thenRecyclerView
        ifAddBtn = binding.ifAddBtn
        thenAddBtn = binding.thenAddBtn
        doneBtn = binding.doneBtn
        spinner = binding.spinKit

        ifList.layoutManager = GridLayoutManager(this, 1)
        thenList.layoutManager = GridLayoutManager(this, 1)

        viewModel = ViewModelProvider(this, EditIntegrationViewModelFactory(integrationId!!)).get(EditIntegrationViewModel::class.java)
        lifecycle.addObserver(viewModel)

        viewModel.name.observe(this) {
            nameFieldLayout.editText?.setText(it)
        }
        viewModel.ifList.observe(this) {
            ifList.adapter = EditIntegrationIfAdapter(this, it)
        }
        viewModel.thenList.observe(this) {
            thenList.adapter = EditIntegrationThenAdapter(this, it, integrationId)
        }
        viewModel.finishedSaving.observe(this) {
            if (it) {
                goBack()
            }
        }
        viewModel.finishedDeleting.observe(this) {
            if (it) {
                goBack()
            }
        }
        viewModel.loadedList.observe(this) { loaded ->
            if (loaded) {
                spinner.visibility = View.GONE
            } else {
                spinner.visibility = View.VISIBLE
            }
        }

        ifAddBtn.setOnClickListener {
            val intent = Intent(this, SelectDeviceToAddActivity::class.java)
            intent.putExtra("integrationId", integrationId)
            intent.putExtra("deviceType", Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE)
            startActivity(intent)
        }
        thenAddBtn.setOnClickListener {
            val intent = Intent(this, SelectDeviceToAddActivity::class.java)
            intent.putExtra("integrationId", integrationId)
            intent.putExtra("deviceType", Device.DeviceType.TYPE_UNSPECIFIED_VALUE)
            startActivity(intent)
        }

        doneBtn.setOnClickListener {
            val name = nameFieldLayout.editText?.text.toString().trim()
            viewModel.saveName(name)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
//        super.onBackPressed()
        // Handle the back button event
        Timber.i("intercepted back button")
        val name = nameFieldLayout.editText?.text.toString().trim()
        viewModel.saveName(name)
    }

    override fun onSupportNavigateUp(): Boolean {
//        super.onSupportNavigateUp()
        // Handle the back button event from the toolbar
        Timber.i("intercepted toolbar back button")
        val name = nameFieldLayout.editText?.text.toString().trim()
        viewModel.saveName(name)
        return false
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
            viewModel.deleteIntegration()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        builder.setIcon(R.drawable.baseline_warning_24)
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