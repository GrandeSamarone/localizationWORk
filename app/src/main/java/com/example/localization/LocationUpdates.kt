package com.example.localization

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.LocationManager
import android.util.Log

class LocationUpdates(context: Context) {

    private val mGpsLocationClient: LocationManager =
        context.getSystemService(LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun start(){
        Log.i("MYTAG", "start")

        try {
            mGpsLocationClient.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L ,
                3f ,
                locationListener)
        }catch (e: Exception){
            Log.i("MYTAG", "mGpsLocationClient $e")
        }
    }

    private val locationListener =
        android.location.LocationListener { location -> //handle location change
            Log.i("MYTAG", "UPDATE ${location.latitude} ${location.longitude}")
        }


}