package com.iotgroup2.matterapp.Pages.Home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import chip.devicecontroller.AttestationInfo
import chip.devicecontroller.DeviceAttestationDelegate
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Pages.Units.UnitsActivity
import com.iotgroup2.matterapp.shared.MatterViewModel.DevicesUiModel
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.databinding.FragmentHomeBinding
import com.iotgroup2.matterapp.databinding.FragmentNewDeviceBinding
import com.iotgroup2.matterapp.shared.matter.PERIODIC_READ_INTERVAL_DEVICE_SCREEN_SECONDS
import com.iotgroup2.matterapp.shared.matter.TaskStatus
import com.iotgroup2.matterapp.shared.matter.chip.ChipClient
import com.iotgroup2.matterapp.shared.matter.data.DevicesRepository
import com.iotgroup2.matterapp.shared.matter.data.DevicesStateRepository
import com.iotgroup2.matterapp.shared.matter.data.UserPreferencesRepository
import com.iotgroup2.matterapp.shared.matter.isMultiAdminCommissioning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    @Inject
    internal lateinit var devicesRepository: DevicesRepository
    @Inject
    internal lateinit var devicesStateRepository: DevicesStateRepository
    @Inject
    internal lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject
    internal lateinit var chipClient: ChipClient

    private lateinit var _binding: FragmentHomeBinding

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding

    // The ActivityResult launcher that launches the "commissionDevice" activity in Google Play services.
    private lateinit var commissionDeviceLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val viewModel: MatterActivityViewModel by viewModels()

    // New device information dialog
    private lateinit var newDeviceAlertDialogBinding: FragmentNewDeviceBinding

    private lateinit var sensorFilterBtn: Button
    private lateinit var actuatorFilterBtn: Button
    private lateinit var spinner: SpinKitView

    private var isSensorFilter = true

    // Tells whether a device attestation failure was ignored.
    // This is used in the "Device information" screen to warn the user about that fact.
    // We're doing it this way as we cannot ask permission to the user while the
    // decision has to be made because UI is fully controlled by GPS at that point.
    private var deviceAttestationFailureIgnored = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        val HomeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        lifecycle.addObserver(HomeViewModel)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // We need our own device attestation delegate as we currently only support attestation
        // of test Matter devices. This DeviceAttestationDelegate makes it possible to ignore device
        // attestation failures, which happen if commissioning production devices.
        // TODO: Look into supporting different Root CAs.
        setDeviceAttestationDelegate()

        sensorFilterBtn = _binding.sensorFilterBtn
        actuatorFilterBtn = _binding.actuatorFilterBtn
        spinner = _binding.spinKit

        if (isSensorFilter) {
            sensorFilterBtn.setBackgroundResource(R.drawable.filter_btn_checked)
            actuatorFilterBtn.setBackgroundResource(R.drawable.filter_btn_unchecked)
        } else {
            sensorFilterBtn.setBackgroundResource(R.drawable.filter_btn_unchecked)
            actuatorFilterBtn.setBackgroundResource(R.drawable.filter_btn_checked)
        }

        /** Devices List Recycle View **/
        val devicesListRecyclerView = binding.devicesList

        // Setup recycler view for the devices list
        devicesListRecyclerView.layoutManager = GridLayoutManager(context, 2)
        devicesListRecyclerView.adapter = HomeAdapter(requireContext(), listOf())

        HomeViewModel.devices.observe(viewLifecycleOwner) { devices ->
//            for (device in devices) {
//                Timber.d("Device: ${device.label} is ${device.state}, online: ${device.online}")
//            }
            if (isSensorFilter) {
                devicesListRecyclerView.adapter = HomeAdapter(requireContext(), devices.filter { it.type == Device.DeviceType.TYPE_TEMPERATURE_SENSOR_VALUE || it.type == Device.DeviceType.TYPE_PRESSURE_SENSOR_VALUE || it.type == Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE })
            } else {
                devicesListRecyclerView.adapter = HomeAdapter(requireContext(), devices.filter { it.type != Device.DeviceType.TYPE_TEMPERATURE_SENSOR_VALUE && it.type != Device.DeviceType.TYPE_PRESSURE_SENSOR_VALUE && it.type != Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE && it.type != Device.DeviceType.TYPE_UNKNOWN_VALUE && it.type != Device.DeviceType.TYPE_UNSPECIFIED_VALUE })
            }
//            devicesListRecyclerView.adapter = HomeAdapter(requireContext(), devices)
        }
        HomeViewModel.loadedList.observe(viewLifecycleOwner) { loaded ->
            if (loaded) {
                spinner.visibility = View.GONE
            } else {
                spinner.visibility = View.VISIBLE
            }
        }

        /** Dynamic set UI elements **/
        val addNewDeviceButton: ImageButton = binding.addDevicesBtn

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

            HomeViewModel.updateDeviceStates(devicesUiModel.devices)
            HomeViewModel.addDevice(devicesUiModel.devices)
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

        sensorFilterBtn.setOnClickListener {
            isSensorFilter = true
            sensorFilterBtn.setBackgroundResource(R.drawable.filter_btn_checked)
            actuatorFilterBtn.setBackgroundResource(R.drawable.filter_btn_unchecked)

            if (HomeViewModel.devices.value == null) {
                return@setOnClickListener
            }

            devicesListRecyclerView.adapter = HomeAdapter(requireContext(), HomeViewModel.devices.value!!.filter { it.type == Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE })
        }

        actuatorFilterBtn.setOnClickListener {
            isSensorFilter = false
            sensorFilterBtn.setBackgroundResource(R.drawable.filter_btn_unchecked)
            actuatorFilterBtn.setBackgroundResource(R.drawable.filter_btn_checked)

            if (HomeViewModel.devices.value == null) {
                return@setOnClickListener
            }

            devicesListRecyclerView.adapter = HomeAdapter(requireContext(), HomeViewModel.devices.value!!.filter { it.type != Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE })
        }

        // Setup Interactions Within Devices View
        addNewDeviceButton.setOnClickListener {
            Timber.d("addDeviceButton.setOnClickListener")
            deviceAttestationFailureIgnored = false
            viewModel.stopMonitoringStateChanges()
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
            val dialog = MaterialAlertDialogBuilder(requireContext())
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

            if (deviceAttestationFailureIgnored) {
                dialog.setMessage(
                    Html.fromHtml(getString(R.string.device_attestation_warning),
                        Html.FROM_HTML_MODE_LEGACY
                    ))
            }
            dialog.show()
            // Make the hyperlink clickable. Must be set after show().
            val msgTextView: TextView? = dialog.findViewById(android.R.id.message)
            msgTextView?.movementMethod = LinkMovementMethod.getInstance()
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
            } else {
                Timber.d("TaskStatus is *not* NotStarted: $viewModel.commissionDeviceStatus.value")
            }
        } else {
            Timber.d("Invocation: Main")
            Timber.d(
                "Starting periodic ping on device with interval [$PERIODIC_READ_INTERVAL_DEVICE_SCREEN_SECONDS] seconds")
            viewModel.startMonitoringStateChanges()
        }
    }

    override fun onPause() {
        super.onPause()

        Timber.d("onPause(): Stopping periodic ping on devices")
        viewModel.stopMonitoringStateChanges()
    }

    override fun onDestroy() {
        super.onDestroy()
        chipClient.chipDeviceController.setDeviceAttestationDelegate(0, EmptyAttestationDelegate())
    }

    // ---------------------------------------------------------------------------
    // Device Attestation Delegate

    private class EmptyAttestationDelegate : DeviceAttestationDelegate {
        override fun onDeviceAttestationCompleted(
            devicePtr: Long,
            attestationInfo: AttestationInfo,
            errorCode: Int
        ) {}
    }

    private fun setDeviceAttestationDelegate() {
        chipClient.chipDeviceController.setDeviceAttestationDelegate(
            DEVICE_ATTESTATION_FAILED_TIMEOUT_SECONDS) { devicePtr, attestationInfo, errorCode ->
            Timber.d(
                "Device attestation errorCode: $errorCode, " +
                        "Look at 'src/credentials/attestation_verifier/DeviceAttestationVerifier.h' " +
                        "AttestationVerificationResult enum to understand the errors")

            if (errorCode == STATUS_PAIRING_SUCCESS) {
                Timber.d("DeviceAttestationDelegate: Success on device attestation.")
                lifecycleScope.launch {
                    chipClient.chipDeviceController.continueCommissioning(devicePtr, true)
                }
            } else {
                Timber.d("DeviceAttestationDelegate: Error on device attestation [$errorCode].")
                // Ideally, we'd want to show a Dialog and ask the user whether the attestation
                // failure should be ignored or not.
                // Unfortunately, the GPS commissioning API is in control at this point, and the
                // Dialog will only show up after GPS gives us back control.
                // So, we simply ignore the attestation failure for now.
                // TODO: Add a new setting to control that behavior.
                deviceAttestationFailureIgnored = true
                Timber.w("Ignoring attestation failure.")
                lifecycleScope.launch {
                    chipClient.chipDeviceController.continueCommissioning(devicePtr, true)
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Companion object

    companion object {
        private const val STATUS_PAIRING_SUCCESS = 0

        /** Set for the fail-safe timer before onDeviceAttestationFailed is invoked. */
        private const val DEVICE_ATTESTATION_FAILED_TIMEOUT_SECONDS = 60
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