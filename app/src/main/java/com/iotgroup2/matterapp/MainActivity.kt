package com.iotgroup2.matterapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.iotgroup2.matterapp.databinding.ActivityMainBinding
import com.iotgroup2.matterapp.shared.matter.VERSION_NAME
import com.iotgroup2.matterapp.shared.matter.displayPreferences
import com.iotgroup2.matterapp.shared.matter.setDeviceTypeStrings
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/** Main Activity for the "Google Home Sample App for Matter" (GHSAFM). */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var downloadModuleLauncher: ActivityResultLauncher<IntentSenderRequest>

    /** Kicks off the download module intent for the Home module via [ModuleInstallClient] */
    private fun downloadModule(downloadModuleLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        ModuleInstall.getClient(this)
            .getInstallModulesIntent(Matter.getCommissioningClient(this))
            .addOnSuccessListener { response ->
                if (response.pendingIntent != null) {
                    downloadModuleLauncher.launch(
                        IntentSenderRequest.Builder(response.pendingIntent!!.intentSender).build()
                    )
                } else {
                    Timber.i("Home Module Install module already installed")
                }
            }
            .addOnFailureListener { ex ->
                Timber.e(ex,"Home Module Install download failed")
            }
    }

    /**
     * Constants we access from Utils, but that depend on the Activity context to be set to their
     * values.
     */
    fun initContextDependentConstants() {
        // versionName is set in build.gradle.
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        VERSION_NAME = packageInfo.versionName

        // Strings associated with DeviceTypes
        setDeviceTypeStrings(
            unspecified = "unspecified",
            light = "light",
            outlet = "outlet",
            unknown = "unknown",
        colorTempLight = "colorTempLight",
        temperatureSensor = "temperatureSensor",
        humiditySensor = "humiditySensor",
            pressureSensor = "pressureSensor",
            soilMoistureSensor = "soilMoistureSensor",
            lightSensor = "lightSensor"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Useful to see which preferences are set under the hood by Matter libraries.
        displayPreferences(this)

        initContextDependentConstants()

        downloadModuleLauncher =
            registerForActivityResult<IntentSenderRequest, ActivityResult>(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                val resultCode = result.getResultCode()
                if (resultCode == RESULT_OK) {
                    Timber.i("Home Module Install download complete")
                } else if (resultCode == RESULT_CANCELED) {
                    Timber.e("Home Module Install download canceled")
                }
            }
        downloadModule(downloadModuleLauncher);

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_devices,
                R.id.navigation_integrations
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Firebase cloud messaging setup
        askNotificationPermission()
        // TODO: Firebase is crashing at this point now
//        FirebaseApp.initializeApp(this)
//        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Timber.e("Fetching FCM registration token failed", task.exception)
//                return@OnCompleteListener
//            }
//
//            // Get new FCM registration token
//            val token = task.result
//            Timber.i("FCM token: $token")
//
//            // make a toast
////            Toast.makeText(this, token, Toast.LENGTH_LONG).show()
//        })
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            Timber.i("Permission granted")
        } else {
            // TODO: Inform user that that your app will not show notifications.
            Timber.i("Permission denied")
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}