package com.example.localization

import android.Manifest
import android.R.id
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.localization.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit


private lateinit var locationRequest: OneTimeWorkRequest
class MainActivity : AppCompatActivity(){

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val appLifecycleObserver: MyApplication.ObserverLife = MyApplication.ObserverLife()
    private val workManager = WorkManager.getInstance(application)
    private val outputWorkInfos: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG_MOTOBOY)
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
                    Log.d(TAGLOG, "active GPS")
                }
            }
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(appLifecycleObserver)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        createChannel()
        binding.btLoc.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startForResult.launch(intent)
            } else {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                       // Manifest.permission.POST_NOTIFICATIONS,
                    )
                )
            }

        }


        binding.btLocClosed.setOnClickListener {
            workManager.cancelUniqueWork(TAG_MOTOBOY)
        }

    }

    private fun startJob() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
        try {
            locationRequest = OneTimeWorkRequest.Builder(WorkServiceOnline::class.java)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        10,
                        TimeUnit.SECONDS)
                    .addTag(TAG_MOTOBOY)
                    .build()

            Log.d(TAGLOG, "MSG MSG")
            Log.d(TAGLOG, locationRequest.id.toString())


            workManager.enqueueUniqueWork(
                TAG_MOTOBOY,
                ExistingWorkPolicy.REPLACE,
                locationRequest)

            outputWorkInfos.observe(this,workInfosObserver())
            lifecycle.addObserver(appLifecycleObserver)
            Log.i(TAGLOG,"::::::::")
            Log.i(TAGLOG, outputWorkInfos.observe(this, workInfosObserver()).toString())

        } catch (e: Exception) {
            Log.i(TAGLOG, "erro Work:::::")
            Log.i(TAGLOG, e.toString())
        }
    }
//    private fun workInfosObserver(): Observer<List<WorkInfo>> {
//        return Observer { listOfWorkInfo ->
//            if (listOfWorkInfo.isEmpty()) {
//                return@Observer
//            }
//
//            val workInfo = listOfWorkInfo[0]
//            Log.i(TAGLOG, "${workInfo.state}")
//            binding.textobs.text = workInfo.state.toString()
//            binding.texterror.text = workInfo.outputData.getString("error")
//
//
//        }
//    }

    private fun checkLocation(): Boolean {
        val manager =
            this@MainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false
        }

        return true
    }
    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    // TODO: Step 1.0, Review Permissions: Handles permission result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAGLOG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    Log.d(TAGLOG, "User interaction was cancelled.")

                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    Log.d(TAGLOG, "PERMISSION_GRANTED")
                else -> {
                    Log.d(TAGLOG, "PERMISSION_NEGADA")
                }
            }
        }
    }

    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.description = DESCRIPTION
            val notificationManager =
                this@MainActivity.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

}