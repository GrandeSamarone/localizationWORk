package com.example.localization

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result.failure
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationWork (context: Context, params: WorkerParameters):CoroutineWorker(context,params) {

    private lateinit var locationRequest: LocationRequest
    private lateinit  var locationCallback: LocationCallback
    private var locationListener: LocationListener? = null
   private   var fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(context)

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        withContext(IO){
            getLocationUpdates()
        }
        try {
                for (i in 0..50) {
                    delay(1000)
                    Log.i("MYTAG", "Downloading $i")

                }
                Log.i("MYTAG", "Completed ${getCurrentDateTime()}")
                success()
            } catch (e: Exception) {
                Log.i("MYTAG", "Exception $e")
               failure()
            }

        return  success()
    }
    private fun getLocationUpdates() {
        setLocationRequest()
       setLocationCallback()
        startLocationUpdates()
         //setLocationListener()
    }
    private fun setLocationRequest() {

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,100L).apply {
            this.setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            this.setMinUpdateDistanceMeters(1F)
        }.build()
    }
    private fun setLocationCallback() {
        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {

                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
                        parseLocationWithGeocoder(location)
                    }

                    Log.i("MYTAG", "LatitudeCallback ${location.latitude} + ${location.longitude}")
                  //  Log.i("MYTAG", "accuracy ${location.accuracy}")
//                    Log.i("MYTAG", "bearing ${location.bearing}")
//                    Log.i("MYTAG", "speed ${location.speed}")
//                    Log.i("MYTAG", "time ${location.time}")
//                    Log.i("MYTAG", "speedAccuracyMetersPerSecond ${location.speedAccuracyMetersPerSecond}")
//                    Log.i("MYTAG", "hasAccuracy ${location.hasAccuracy()}")
//                    Log.i("MYTAG", "hasAltitude ${location.hasAltitude()}")
//                    Log.i("MYTAG", "hasBearing ${location.hasBearing()}")
//                    Log.i("MYTAG", "hasSpeed ${location.hasSpeed()}")
//                    Log.i("MYTAG", "hasSpeedAccuracy ${location.hasSpeedAccuracy()}")
//                    Log.i("MYTAG", "hasVerticalAccuracy ${location.hasVerticalAccuracy()}")
                }
            }
        }
    }

    private fun setLocationListener() {
        locationListener = LocationListener { location ->
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
                parseLocationWithGeocoder(location)
            }

            Log.i("MYTAG", "LatitudeListener ${location.latitude}")
            Log.i("MYTAG", "LongitudeListener ${location.longitude}")
        }

    }


    // region - GeoCoder Location Parsing
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun parseLocationWithGeocoder(location: Location) {
        val geoCoder = Geocoder(this.applicationContext, Locale.getDefault())
        geoCoder.getFromLocation(
            location.latitude,
            location.longitude,
            1,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {

                    if (addresses.isNotEmpty()) {
                        val address: String = addresses[0].getAddressLine(0)
                        val city: String = addresses[0].locality
                        val state: String = addresses[0].adminArea
                        val country: String = addresses[0].countryName
                        val postalCode: String = addresses[0].postalCode
                        val knownName: String = addresses[0].featureName
                        Log.i("MYTAG", "address: $address | city: $city | state: $state | country: $country | zipCode: $postalCode | knownName: $knownName")
                    } else {
                        Log.i("MYTAG", "Location Unavailable")
                    }
                }

                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)
                }
            })
    }

    private fun startLocationUpdates() {
        val locationRequest = locationRequest ?: return

        locationCallback?.let { callback ->
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

        }

        locationListener?.let { listener ->
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, listener, Looper.getMainLooper())
        }
    }


    override suspend fun getForegroundInfo(): ForegroundInfo {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val notification = NotificationCompat.Builder(applicationContext, "notification id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentText("Updating widget")
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(){
        // Create a Notification channel
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Download Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                    NotificationManager
        notificationManager.createNotificationChannel(serviceChannel)
    }


    private fun getCurrentDateTime(): String {
        val time = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        return time.format(Date())
    }
}