package com.example.localization

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.localization.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

private const val TAG = "MYTAG"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity() {

    private val binding by lazy{
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

    private fun startJob(){
        val constraints =  Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            //.setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()


        val locationRequest:OneTimeWorkRequest
            = OneTimeWorkRequest.Builder(WorkServiceOnline::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            //.setConstraints(constraints)
            .build()

        Log.d(TAG, locationRequest.toString())


        workManager.enqueueUniqueWork(
            "motoboyON",
            ExistingWorkPolicy.REPLACE,
            locationRequest)
     }

    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Log.d(TAG, "PERMISSION OK")
            startJob()

           // LocationUpdates(applicationContext).start()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,Manifest.permission.ACCESS_COARSE_LOCATION,),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )

        }
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



    private fun createChannel(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "mobbiexpresswork",
                "ForegroundWorker",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.description = "This is channel 1";
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
}