package com.ltu.actor.comeToMe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ltu.actor.comeToMe.RideServiceClient.Companion.VEHICLE_IP
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    // Properties
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback

    private var mLastLocation: Location? = null
        set(value) {
            updateMapLocation(value)
            field = value
        }

    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        val button: Button = findViewById(R.id.button_id)
        button.setOnClickListener {
            Log.i(TAG, "Button pressed.")
            sendLocation()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                //                            stopLocationUpdates()
                Log.i(TAG, "Location Received.")
                if (locationResult != null && locationResult.locations.isNotEmpty()) {
                    val locations = locationResult.locations
                    Log.i(TAG, "Available Locations: $locations")
                    mLastLocation = locationResult.locations[0]
                    Log.i(TAG, "Setting Location: $mLastLocation")
                } else {
                    //                                onRetrieval(null)
                    Log.i(TAG, "Location NOT GOOD: $locationResult")
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability?) {
                super.onLocationAvailability(p0)
                if (p0 != null) {
                    when {
                        p0.isLocationAvailable -> {
                            Log.i(TAG, "Location Available.")
                        }
                        else -> {
                            Log.i(TAG, "Location Not Available.")
                        }
                    }
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()

        Log.i(TAG, "Checking Permissions")
        if (!checkPermissions()) {
            Log.i(TAG, "Requesting Permissions")
            requestPermissions()
        } else {
            Log.i(TAG, "Setting Up Location Updates")
            startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }


    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        mFusedLocationProviderClient.requestLocationUpdates(LocationRequest(), mLocationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    // RideServiceClient
    private fun sendLocation() {
        if (!isNetworkConnected()) return

        Log.i(TAG, "Network or VPN  available")
        getLastLocation {
            Log.i(TAG, "Have a location")

            val client = RideServiceClient()
            Log.i(TAG, "Ride Service Client: $client")

            GlobalScope.async {
                Log.i(TAG, "Launched thread to make request")
                showSnackBar(mainText = "${getString(R.string.sending_location)} $VEHICLE_IP")
                client.sendPostRequestForLocation(it, object : RideServiceClientCallback {
                    override fun completionHandler(success: Boolean?, type: RequestType?) {
                        Log.i(TAG, "completionHandler - success $success")
                        showSnackBar(R.string.vehicle_received_location)
                    }
                })
            }
        }
    }

    // FusedLocationProviderClient
    private fun getLastLocation(onRetrieval: (location: Location) -> Unit) {
        mFusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                Log.i(TAG, "Received location")
                val resultingLocation = task.result
                Log.i(TAG, "Result: $resultingLocation")

                val locationNotUsable = resultingLocation == null || resultingLocation.accuracy > 100
                when {
                    locationNotUsable -> {
                        Log.i(TAG, "Cached Location not usable, waiting for update...")
                        resultingLocation?.let { onRetrieval(it) }
                    }
                    task.isSuccessful -> {
                        Log.i(TAG, "Pass along cached location.")
                        resultingLocation?.let { onRetrieval(it) }
                    }
                    else -> {
                        val exception = task.exception
                        Log.w(TAG, "getLastLocation:exception -> $exception")
                    }
                }
            }
    }

    // Messages to User
    private fun showSnackBar(mainTextStringId: Int = 0, mainText: String = "") {
        if ((mainTextStringId == 0) && (mainText == ""))  { return }

        runOnUiThread {
            when {
                mainTextStringId != 0 -> {
                    Toast.makeText(this, getString(mainTextStringId), Toast.LENGTH_SHORT).show()
                }
                mainText != "" -> {
                    Toast.makeText(this, mainText, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.w(TAG, "Unable to display Toast with: $mainText and id: $mainTextStringId")
                }
            }
        }
    }

    // Location Permissions
    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        Log.i(TAG, "Permission State: $permissionState")
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSIONS_REQUEST_CODE)
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackBar(R.string.permission_rationale)
        } else {
            Log.i(TAG, "Requesting permission")
            startLocationPermissionRequest()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    Log.i(TAG, "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    Log.i(TAG, "PERMISSION_GRANTED")
                    getLastLocation {
                        mLastLocation = it
                    }
                }
                else -> {
                    Log.i(TAG, "PERMISSION_DENIED")
                    showSnackBar(R.string.permission_denied_explanation)
                }
            }
        }
    }

    // Maps
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateMapLatLng(mLastLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0))
    }

    private fun updateMapLatLng(location: LatLng) {
        mMap.addMarker(MarkerOptions().position(location).title("Device Location"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 17.0f))
    }

    private fun updateMapLocation(location: Location?) {
        val latLong =  location?.let { LatLng(it.latitude, it.longitude) } ?: return
        mMap.addMarker(MarkerOptions().position(latLong).title("Device Location"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, 17.0f))
    }

    // Networking
    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null &&
                (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
    }

    companion object {
        private const val TAG = "Dev-MapsActivity"
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }
}
