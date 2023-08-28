package com.example.localization

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.localization.databinding.ActivityMainBinding
import java.util.UUID
import java.util.concurrent.TimeUnit


private const val TAG = "MYTAG"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    lateinit var workManager: WorkManager

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.e("DEBUG", "${it.key} = ${it.value}")

            if (it.value) {
                if (checkLocation()) {
                    startJob()

                } else {
                    val intent1 = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startForResult.launch(intent1)
                    Log.d(TAG, "active GPS")
                }
            }
        }
    }

    ///ir para confirugação do GPS
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        createChannel()
        workManager = WorkManager.getInstance(this)

        binding.btLoc.setOnClickListener {

            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startForResult.launch(intent)
            } else {
                requestMultiplePermissions.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                )
            }

        }

        binding.btLocClosed.setOnClickListener {
            workManager.cancelUniqueWork("motoboyON")
        }

    }

    private fun startJob() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
        try {
            val locationRequest: OneTimeWorkRequest =
                OneTimeWorkRequest.Builder(WorkServiceOnline::class.java)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        10,
                        TimeUnit.SECONDS
                    )
                    .build()

            Log.d(TAG, "MSG MSG")
            Log.d(TAG, locationRequest.id.toString())


            workManager.enqueueUniqueWork(
                "motoboyON",
                ExistingWorkPolicy.REPLACE,
                locationRequest)


          observeWork(locationRequest)

        } catch (e: Exception) {
            Log.i(TAG, e.toString())
           // binding.textobs.text = e.toString()
        }


    }

    private fun observeWork(oneTimeWorkRequest: OneTimeWorkRequest) {
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null) {
                    Log.i(TAG, "${workInfo.state}")
                    binding.textobs.text = workInfo.state.toString()
                    binding.texterror.text = workInfo.outputData.getString("error")
                }
            }
    }



    private fun checkLocation(): Boolean {
        val manager =
            this@MainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false
        }

        return true
    }

    // TODO: Step 1.0, Review Permissions: Handles permission result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")

                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    // foregroundOnlyLocationService?.subscribeToLocationUpdates()
                    Log.d(TAG, "PERMISSION_GRANTED")

                else -> {
                    Log.d(TAG, "PERMISSION_NEGADA")
                }
            }
        }
    }

    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "mobbiexpresswork",
                "ForegroundWorker",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.description = "This is channel 1";
            val notificationManager =
                this@MainActivity.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

}