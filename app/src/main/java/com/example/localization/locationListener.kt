package com.example.localization
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.android.gms.location.LocationCallback
import com.google.common.util.concurrent.ListenableFuture


class WorkServiceOnline(context: Context, workerParams: WorkerParameters)
    : ListenableWorker(context, workerParams) {
    companion object {
        const val TAG = "ForegroundWorker"
        const val NOTIFICATION_ID = 42
        const val CHANNEL_ID = "mobbiexpresswork"
    }

    private val mGpsLocationClient: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var locationListener: LocationListener? = null


    override fun onStopped() {
        Log.e("MYTAG", "onStopped()")
        locationListener?.let { mGpsLocationClient.removeUpdates(it) }
    }


    override fun startWork(): ListenableFuture<Result> {
        Log.i("MYTAG", "startWork")
        //setForegroundAsync(createForegroundInfo())

        return CallbackToFutureAdapter.getFuture { completer ->
            Log.i("MYTAG", "CallbackToFutureAdapter")
            try{
                if(foregroundPermissionApproved()){
                    startListenerLocalization()
                }else{
                    completer.set(Result.failure())
                }
            }catch (e:Exception){
                Log.i("MYTAG", "Exception $e")
                completer.set(Result.failure())
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun startListenerLocalization(){
        Log.i("MYTAG", "start")
         locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.i("MYTAG", "UPDATE ${location.latitude} ${location.longitude}")
            }
            override fun onProviderEnabled(provider: String) {
                Log.i("MYTAG", "onProviderEnabled $provider")
            }
            override fun onProviderDisabled(provider: String) {
                Log.i("MYTAG", "onProviderDisabled $provider")
            }
        }
        try {
            mGpsLocationClient.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0 ,
                1f ,
                locationListener as LocationListener
            )
        }catch (e: Exception){
            Log.i("MYTAG", "mGpsLocationClient $e")
        }
    }

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    private fun createForegroundInfo(): ForegroundInfo {
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Você está online") // .setContentTitle("Mobbi Express")
            .setTicker("Mobbi Express")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
        return ForegroundInfo(NOTIFICATION_ID, builder.build())
    }

    @SuppressLint("RestrictedApi")
    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        val future = SettableFuture.create<ForegroundInfo>()
        future.set(createForegroundInfo())
        return future
    }
}