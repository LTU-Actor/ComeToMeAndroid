package com.ltu.actor.vehicleSummonSystem

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.checkSelfPermission
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    // Properties
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mDeviceLocationTextView: TextView
    private lateinit var mFollowLocationSwitch: SwitchCompat

    private var mLastLocation: Location? = null
    private var mShouldFollowLocationOnMap = false

    // Lifecycle
    @DelicateCoroutinesApi
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

        mDeviceLocationTextView = findViewById(R.id.device_location)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mFollowLocationSwitch = findViewById(R.id.follow_location_switch)
        mFollowLocationSwitch.setOnCheckedChangeListener { _, isChecked ->
            mShouldFollowLocationOnMap = isChecked
        }

        mLocationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult?) {
                //                            stopLocationUpdates()
                Log.i(TAG, "Location Received.")
                if (locationResult != null && locationResult.locations.isNotEmpty()) {
                    val locations = locationResult.locations
                    Log.i(TAG, "Available Locations: $locations")
                    mLastLocation = locationResult.locations[0]
                    Log.i(TAG, "Setting Location: $mLastLocation")

                    mLastLocation?.let { location ->
                        mDeviceLocationTextView.text = "(${location.latitude}, ${location.longitude})"


                        if (mShouldFollowLocationOnMap) {
                            mMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    ), 13f
                                )
                            )
                            val cameraPosition = CameraPosition.Builder()
                                .target(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    )
                                ).zoom(19f).bearing(90f).tilt(40f).build()
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                        }
                    }
                } else {
                    //                                onRetrieval(null)
                    Log.i(TAG, "Location NOT GOOD: $locationResult")
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability?) {
                if (p0 != null) {
                    super.onLocationAvailability(p0)
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
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 100
        locationRequest.priority = PRIORITY_HIGH_ACCURACY
        if (checkHaveLocationPermission()) {
            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    // RideServiceClient
    @DelicateCoroutinesApi
    private fun sendLocation() {
        if (!isNetworkConnected()) return

        Log.i(TAG, "Network or VPN  available")
        getLastLocation {
            Log.i(TAG, "Have a location")

            val client = RideServiceClient()
            Log.i(TAG, "Ride Service Client: $client")

            @Suppress("DeferredResultUnused")
            GlobalScope.async {
                Log.i(TAG, "Launched thread to make request")
//                showSnackBar(mainText = "${getString(R.string.sending_location)} $VEHICLE_IP")
                showSnackBar(mainText = "${getString(R.string.sending_location)} (${it.latitude}, ${it.longitude})")

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
        if (checkHaveLocationPermission()) {
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
        val permissionState = checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
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
                        if (checkHaveLocationPermission()) {
                            mMap.isMyLocationEnabled = true
                        }
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
        if (checkHaveLocationPermission()) {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun checkHaveLocationPermission(): Boolean {
        if (checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
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
