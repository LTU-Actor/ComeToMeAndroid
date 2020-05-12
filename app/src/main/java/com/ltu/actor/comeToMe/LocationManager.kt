package com.ltu.actor.comeToMe

import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

class LocationManager {
    private var mFusedLocationProviderClient: FusedLocationProviderClient
    private var mLocationCallback: LocationCallback

    private var mLastLocation: Location? = null

    constructor(provider: FusedLocationProviderClient) {
        mFusedLocationProviderClient = provider

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                //                            stopLocationUpdates()
                Log.i(TAG, "Location Received.")
                if (locationResult != null && locationResult.locations.isNotEmpty()) {
                    mLastLocation = locationResult.locations[0]
                    var locations = locationResult.locations
                    Log.i(TAG, "Setting Location: $mLastLocation")
                    Log.i(TAG, "Available Locations: $locations")
                } else {
                    //                                onRetrieval(null)
                    Log.i(TAG, "Location NOT GOOD: $locationResult")
                }
            }
        }
    }

    public fun startGettingLocation() {
        mFusedLocationProviderClient.requestLocationUpdates(LocationRequest(), mLocationCallback, Looper.getMainLooper())
    }


    // FusedLocationProviderClient
//    public fun lastLocationHandler(onRetrieval: (location: Location) -> Void) ->   {
//        return { task ->
//            Log.i(MapsActivity.TAG, "Received location")
//            var resultingLocation = task.result
//            Log.i(MapsActivity.TAG, "Result: $resultingLocation")
//
//            var locationNotUsable = resultingLocation == null || resultingLocation.accuracy > 100
//            when {
//                locationNotUsable -> {
//                    Log.i(MapsActivity.TAG, "Cached Location not usable, waiting for update...")
//                }
//                task.isSuccessful -> {
//                    Log.i(MapsActivity.TAG, "Pass along cached location.")
//                    resultingLocation?.let { onRetrieval(it) }
//                }
//                else -> {
//                    var exception = task.exception
//                    Log.w(MapsActivity.TAG, "getLastLocation:exception -> $exception")
//                }
//            }
//        }
//    }

    companion object {
        private const val TAG = "LocationProvider"
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }
}