package com.example.localization

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.workDataOf
import com.example.localization.bubbleWork.BubbleWork
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException


class WorkServiceOnline(appcontext: Context, workerParams: WorkerParameters)
    : ListenableWorker(appcontext, workerParams) {

    private var context: Context = appcontext
    private lateinit  var locationCallback: LocationCallback
    private   var fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(context)

    override fun onStopped() {
        Log.d(TAGLOG, "onStopped()")
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
       // bublle.Start(context,false)
    }


    override fun startWork(): ListenableFuture<Result> {

        Log.d(TAGLOG, "startWork")
        return CallbackToFutureAdapter.getFuture { completer ->
            try{
                getLocationUpdates()
               // throw Exception("ExecutionException")

            } catch (ioError: IOException) {
               // Result.retry()
                 completer.set(Result.failure( workDataOf(
                    "error" to "$ioError IOException"
                )))
            } catch (otherError: Exception) {

                Log.d(TAGLOG, "Exception::::::::::$otherError")
                otherError.printStackTrace()
                Result.failure()
//
//                completer.set(Result.failure( workDataOf(
//                    "error" to "$otherError Exception1"
//                )))
//                onStopped()
            }catch (otherError: ExecutionException) {
                Log.d(TAGLOG, "localizedMessage:::::::::")
                Log.d(TAGLOG, "ExecutionException::::::::${otherError.localizedMessage}")
//                completer.set(Result.failure( workDataOf(
//                    "error" to "$otherError ExecutionException"
//                )))
                Result.retry()
            }
        }
    }

    private fun getLocationUpdates() {
        setLocationCallback()
        startLocationUpdates()
    }


    private fun setLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Toast.makeText(context, "OBSERVER ${location.latitude} + ${location.longitude}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,8000L).apply {
            this.setGranularity(Granularity.GRANULARITY_FINE)}.build()
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper())
    }
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


    private fun createForegroundInfo(): ForegroundInfo {
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Você está online")
            .setTicker("Mobbi Express")
              //  .setContentIntent(resultPendingIntent)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ForegroundInfo(
                NOTIFICATION_ID,
                builder.build(),
                FOREGROUND_SERVICE_TYPE_LOCATION
                        or FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }else{
            return ForegroundInfo(
                NOTIFICATION_ID,
                builder.build(),
            )
        }

    }

    @SuppressLint("RestrictedApi")
    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        Log.i(TAGLOG, "getForegroundInfoAsync")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            super.getForegroundInfoAsync();
        } else {
            val future = SettableFuture.create<ForegroundInfo>()
            future.set(createForegroundInfo())
            future
        }
    }
    }
