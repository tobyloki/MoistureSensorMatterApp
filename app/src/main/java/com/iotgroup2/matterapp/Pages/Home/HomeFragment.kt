package com.iotgroup2.matterapp.Pages.Home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.iotgroup2.matterapp.Pages.Units.UnitsActivity
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.FragmentHomeBinding
import com.iotgroup2.matterapp.databinding.FragmentNewDeviceBinding
import com.iotgroup2.matterapp.shared.matter.PERIODIC_UPDATE_INTERVAL_DEVICE_SCREEN_SECONDS
import com.iotgroup2.matterapp.shared.matter.TaskStatus
import com.iotgroup2.matterapp.shared.matter.isMultiAdminCommissioning
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var _binding: FragmentHomeBinding

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding

    // The ActivityResult launcher that launches the "commissionDevice" activity in Google Play services.
    private lateinit var commissionDeviceLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val viewModel: MatterActivityViewModel by viewModels()

    // New device information dialog
    private lateinit var newDeviceAlertDialogBinding: FragmentNewDeviceBinding

    private lateinit var spinner : Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        val matterDeviceViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        lifecycle.addObserver(matterDeviceViewModel)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        spinner = _binding.spinner

        // populate spinner with "Garden Node" and "Controller"
        val spinnerArray = arrayOf("Garden Node", "Controller")
        val adapter = context?.let { ArrayAdapter(it, android.R.layout.simple_spinner_item, spinnerArray) }
        adapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        /** Devices List Recycle View **/
        val devicesListRecyclerView = binding.devicesList

        // Setup recycler view for the devices list
        devicesListRecyclerView.layoutManager = GridLayoutManager(context, 2)
        devicesListRecyclerView.adapter = HomeAdapter(requireContext(), listOf())

        matterDeviceViewModel.devices.observe(viewLifecycleOwner) { devices ->
//            for (device in devices) {
//                Timber.d("Device: ${device.label} is ${device.state}, online: ${device.online}")
//            }
            devicesListRecyclerView.adapter = HomeAdapter(requireContext(), devices)
        }

        /** Dynamic set UI elements **/
        val addNewDeviceButton: FloatingActionButton = binding.addDevicesBtn


        // Matter
        // Observe the devicesLiveData.
        viewModel.devicesUiModelLiveData.observe(viewLifecycleOwner) { devicesUiModel: DevicesUiModel ->
            // done: Andrew - Grab one of the devices from devicesUiModel and save in variable called deviceUiModel (done)
//            localDevicesUiModel = devicesUiModel

//            if (devicesUiModel.devices.isNotEmpty()) {
//                Timber.i("devicesUiModel.devices is not empty: ${devicesUiModel.devices.count()}")
////                deviceUiModel = devicesUiModel.devices[0]
////                var deviceUiModel = devicesUiModel.devices[0]
////                deviceUiModel.device.deviceId
//            } else {
//                Timber.i("devicesUiModel.devices is empty")
//            }

            matterDeviceViewModel.updateDeviceStates(devicesUiModel.devices)
            matterDeviceViewModel.addDevice(devicesUiModel.devices)
        }
        viewModel.commissionDeviceStatus.observe(viewLifecycleOwner) { status ->
            Timber.d("commissionDeviceStatus.observe: status [${status}]")
        }
        viewModel.commissionDeviceIntentSender.observe(viewLifecycleOwner) { sender ->
            Timber.d("commissionDeviceIntentSender.observe is called with sender [${sender}]")
            if (sender != null) {
                // Commission Device Step 4: Launch the activity described in the IntentSender that
                // was returned in Step 3 where the viewModel calls the GPS API to commission
                // the device.
                Timber.d("*** Calling commissionDeviceLauncher.launch")
                commissionDeviceLauncher.launch(IntentSenderRequest.Builder(sender).build())
                viewModel.consumeCommissionDeviceIntentSender()
            }
        }

        // commission to development fabric
        commissionDeviceLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                // Commission Device Step 5.
                // The Commission Device activity in GPS (step 4) has completed.
                val resultCode = result.resultCode
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("CommissionDevice: Success")
                    // We now need to capture the device information for the app's fabric.
                    // Once this completes, a call is made to the viewModel to persist the information
                    // about that device in the app.
                    showNewDeviceAlertDialog(result)
                } else {
                    viewModel.commissionDeviceFailed(resultCode)
                }
            }

        // Setup Interactions Within Devices View
        addNewDeviceButton.setOnClickListener {
            Timber.d("addDeviceButton.setOnClickListener")
            viewModel.stopDevicesPeriodicPing()
            viewModel.commissionDevice(requireContext())
        }

        // Binding to the NewDevice UI, which is part of the dialog where we
        // capture new device information.
        newDeviceAlertDialogBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_new_device, container, false)

        return binding.root
    }

    private fun showNewDeviceAlertDialog(activityResult: ActivityResult?) {
        try {
            MaterialAlertDialogBuilder(requireContext())
                .setView(newDeviceAlertDialogBinding.root)
                .setTitle("New device information")
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    // Extract the info entered by user and process it.
                    val nameTextView: TextInputEditText = newDeviceAlertDialogBinding.nameTextView
                    val deviceName = nameTextView.text.toString().trim()
                    viewModel.commissionDeviceSucceeded(activityResult!!, deviceName)
                }
                .setCancelable(false)
                .create()
                .show()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onResume() {
        super.onResume()

        val intent = requireActivity().intent
        if (isMultiAdminCommissioning(intent)) {
            Timber.d("Invocation: MultiAdminCommissioning")
            if (viewModel.commissionDeviceStatus.value == TaskStatus.NotStarted) {
                Timber.d("TaskStatus.NotStarted so starting commissioning")
                viewModel.multiadminCommissioning(intent, requireContext())
//                viewModel.commissionDevice(requireContext())
            } else {
                Timber.d("TaskStatus is *not* NotStarted: $viewModel.commissionDeviceStatus.value")
            }
        } else {
            Timber.d("Invocation: Main")
            Timber.d(
                "Starting periodic ping on device with interval [$PERIODIC_UPDATE_INTERVAL_DEVICE_SCREEN_SECONDS] seconds")
            viewModel.startDevicesPeriodicPing()
        }
    }

    override fun onPause() {
        super.onPause()

        Timber.d("onPause(): Stopping periodic ping on devices")
        viewModel.stopDevicesPeriodicPing()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_integrations_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.changeUnits -> {
                val intent = Intent(requireActivity(), UnitsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}