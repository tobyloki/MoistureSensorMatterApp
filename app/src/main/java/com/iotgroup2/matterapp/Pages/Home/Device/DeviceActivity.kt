package com.iotgroup2.matterapp.Pages.Home.Device

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.iotgroup2.matterapp.Device.DeviceType
import com.iotgroup2.matterapp.Pages.Home.Device.EditDevice.EditDeviceActivity
import com.iotgroup2.matterapp.Pages.Units.UnitsActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.ActivityDeviceBinding
import com.iotgroup2.matterapp.shared.MatterViewModel.DeviceUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.shared.Utility.Utility
import com.iotgroup2.matterapp.shared.matter.PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import shared.Models.DeviceModel
import timber.log.Timber

@AndroidEntryPoint
class DeviceActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityDeviceBinding

    private lateinit var deviceId: String
    private var deviceType: Int = DeviceType.TYPE_UNSPECIFIED_VALUE

    private lateinit var nameTxt: TextView
    private lateinit var onlineIcon: TextView
    private lateinit var onlineTxt: TextView
    private lateinit var tempValueTxt: TextView
    private lateinit var tempUnitTxt: TextView

    private val viewModel: MatterActivityViewModel by viewModels()
    private var deviceUiModel: DeviceUiModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        nameTxt = _binding.nameTxt
        onlineIcon  = _binding.onlineIcon
        onlineTxt = _binding.onlineTxt
        tempValueTxt = _binding.tempValueTxt
        tempUnitTxt = _binding.tempUnitTxt

        /* Initialize Data */
        val extras: Bundle? = intent.extras
        if(extras == null)
            return pageLoadFail("No extras passed to page")
        deviceId = extras.getString("deviceId").toString()
        deviceType = extras.getInt("deviceType")
        Timber.i("Device ID: $deviceId")
        Timber.i("Device Type: $deviceType")

        // matter device list
        viewModel.devicesUiModelLiveData.observe(this) { devicesUiModel: DevicesUiModel ->
            for (device in devicesUiModel.devices) {
                if (device.device.deviceId.toString() == deviceId) {
//                    if (deviceUiModel == null) {
                        deviceUiModel = device
                        updateUI()
//                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PERIODIC_UPDATE_INTERVAL_HOME_SCREEN_SECONDS != -1) {
            Timber.i("Starting periodic ping on devices")
            viewModel.startDevicesPeriodicPing()
        }

        val unit = Utility.getUnit(this)
        Timber.i("Unit: $unit")
        if (!unit) {
            tempUnitTxt.text = "°C"
        } else {
            tempUnitTxt.text = "°F"
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause(): Stopping periodic ping on devices")
        viewModel.stopDevicesPeriodicPing()
    }

    private fun updateUI() {
        if (deviceUiModel == null) {
            return
        }
        val deviceData = deviceUiModel!!
        val device = deviceData.device

        nameTxt.text = device.name
        onlineIcon.setTextColor(if (deviceData.isOnline) ContextCompat.getColor(this, R.color.online) else ContextCompat.getColor(this, R.color.offline))
        onlineTxt.text = if (deviceData.isOnline) "Online" else "Offline"
    }

    private fun pageLoadFail(msg: String) {
        Timber.e(msg)
        goBack()
    }

    private fun goBack() {
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.device_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.editDevice -> {
                val intent = Intent(this, EditDeviceActivity::class.java)
                intent.putExtra("deviceId", deviceId)
                intent.putExtra("deviceType", deviceType)
                startActivity(intent)
                true
            }
            R.id.changeUnits -> {
                val intent = Intent(this, UnitsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.shareDevice -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}