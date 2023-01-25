package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.SelectDeviceToAdd

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.databinding.ActivitySelectDeviceToAddBinding
import timber.log.Timber

class SelectDeviceToAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectDeviceToAddBinding

    private lateinit var selectDeviceRecyclerView: RecyclerView

    private lateinit var viewModel: SelectDeviceToAddViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectDeviceToAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Initialize Data */
        val extras: Bundle = intent.extras ?: return pageLoadFail("No extras passed to page")
        val integrationId = extras.getString("integrationId")
        val deviceType = extras.getInt("deviceType")
        Timber.i("deviceType: $deviceType")

        selectDeviceRecyclerView = binding.selectDeviceRecyclerView

        viewModel = ViewModelProvider(this, SelectDeviceToAddViewModelFactory(deviceType)).get(SelectDeviceToAddViewModel::class.java)
        lifecycle.addObserver(viewModel)

        selectDeviceRecyclerView.layoutManager = GridLayoutManager(this, 1)

        viewModel.deviceList.observe(this) {
            selectDeviceRecyclerView.adapter = SelectDeviceToAddAdapter(this, it, deviceType, integrationId!!)
        }
    }

    private fun pageLoadFail(msg: String) {
        Timber.e(msg)
        goBack()
    }

    private fun goBack() {
        finish()
    }
}