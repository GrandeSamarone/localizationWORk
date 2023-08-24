package com.example.localization

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
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
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.TimeUnit

private const val TAG = "MYTAG"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    lateinit var workManager: WorkManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        createChannel()
        workManager = WorkManager.getInstance(this)
        binding.btLoc.setOnClickListener {
            requestForegroundPermissions()
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
                    .setInitialDelay(1, TimeUnit.SECONDS)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        10,
                        TimeUnit.SECONDS
                    )
                    .build()

            Log.d(TAG, locationRequest.toString())
            Log.d(TAG, locationRequest.workSpec.backoffDelayDuration.toString())


            workManager.enqueueUniqueWork(
                "motoboyON",
                ExistingWorkPolicy.REPLACE,
                locationRequest
            )

            observeProgress(locationRequest)
        } catch (e: Exception) {
            Log.i(TAG, e.toString())
            binding.textobs.text = e.toString()
        }


    }


    private fun observeProgress(oneTimeWorkRequest: OneTimeWorkRequest) {
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this, Observer<WorkInfo> {
                it?.let { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED ->
                            Log.d(TAG, "Worker ENQUEUED")

                        WorkInfo.State.RUNNING ->
                            Log.d(TAG, "Worker RUNNING")

                        WorkInfo.State.SUCCEEDED ->
                            Log.d(TAG, "Worker SUCCEEDED")

                        WorkInfo.State.FAILED ->
                            workManager.cancelAllWork()

                        WorkInfo.State.BLOCKED ->
                            Log.d(TAG, "Worker BLOCKED")

                        WorkInfo.State.CANCELLED ->
                            Log.d(TAG, "Worker CANCELLED")
                    }
                }
                binding.textobs.text = it.state.toString()
                binding.texterror.text = it.outputData.getString("error")
            })
    }

    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Log.d(TAG, "PERMISSION OK")
            if (checkLocation()) {
                startJob()
            } else {
                Log.d(TAG, "active GPS")
            }


            // LocationUpdates(applicationContext).start()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )

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

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
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