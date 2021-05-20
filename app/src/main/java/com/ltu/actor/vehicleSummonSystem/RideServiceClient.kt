package com.ltu.actor.vehicleSummonSystem

import android.location.Location
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

enum class RequestType {
    REQUEST_POST_LOCATION
}

interface RideServiceClientCallback {
    fun completionHandler(success: Boolean?, type: RequestType?)
}

class RideServiceClient {

    companion object {
        private const val TAG = "Dev-RideServiceClient"

        private const val LAT = "lat"
        private const val LONG = "long"
        private const val COORD = "coord"

        const val VEHICLE_IP = "http://192.168.99.5:8642"
//        const val VEHICLE_IP = "http://192.168.60.100:8642" // non-VPN IP
    }

    suspend fun sendPostRequestForLocation(
        location: Location,
        callback: RideServiceClientCallback
    ) = withContext(Dispatchers.IO) {

        val locationData = JSONObject()
        locationData.put(LAT, location.latitude);
        locationData.put(LONG, location.longitude);

        val postData = JSONObject()
        postData.put(COORD, locationData);


        val url = URL(VEHICLE_IP)
        (url.openConnection() as? HttpURLConnection)?.run {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json")

            try {
                val writer = OutputStreamWriter(outputStream)
                writer.write(postData.toString())
                writer.flush()
            } catch (exception: Exception) {
                Log.i(TAG, "exception :$exception")
            }

            val stream = BufferedInputStream(inputStream)
            val bufferedReader = BufferedReader(InputStreamReader(stream))
            val stringBuilder = StringBuilder()
            bufferedReader.forEachLine { stringBuilder.append(it) }

//            val response = stringBuilder.toString()
            val statusCode = this.responseCode

            if (statusCode == 200) {
                Log.i(TAG, "was REQUEST_POST_LOCATION")
                callback.completionHandler(true, RequestType.REQUEST_POST_LOCATION)
            } else {
                callback.completionHandler(false, RequestType.REQUEST_POST_LOCATION)
            }
        }
    }
}