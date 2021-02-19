package com.ltu.actor.vehicleSummonSystem

import android.location.Location
import android.os.AsyncTask
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

enum class RequestType {
    REQUEST_GET, REQUEST_POST_LOCATION
}

interface RideServiceClientCallback {
    fun completionHandler(success: Boolean?, type: RequestType?)
}

sealed class Result<out Success, out Failure>

typealias LocationData = MutableMap<String, Double>

class RideServiceClient {

    companion object {
        private const val TAG = "Dev-RideServiceClient"

        private const val LAT = "lat"
        private const val LONG = "long"
        private const val COORD = "coord"

        const val VEHICLE_IP = "http://192.168.99.5:8642"
//        const val VEHICLE_IP = "http://192.168.60.100:8642" // non-VPN IP
    }

    suspend fun sendPostRequestForLocation(location: Location, callback: RideServiceClientCallback): AsyncTask<String?, Void?, Void?> {
        Log.i(TAG, "Starting Post Request")
        return GlobalScope.async {
            Log.i(TAG, "Async now")

            val locationData: LocationData = HashMap()
            locationData[LAT] = location.latitude
            locationData[LONG] = location.longitude

            val postData: MutableMap<String, LocationData> = HashMap()
            postData[COORD] = locationData

            val task = HttpPostAsyncTask(postData, RequestType.REQUEST_POST_LOCATION, callback)

            Log.i(TAG, "Posting location to $VEHICLE_IP")
            task.execute(VEHICLE_IP)
        }.await()
    }
}

class HttpPostAsyncTask(postData: MutableMap<String, LocationData>?)
    : AsyncTask<String, Void?, Void?>() {

    companion object {
        private const val TAG = "HttpPostAsyncTask"
    }

    private var postData: JSONObject? = null
    var requestType: RequestType? = null
    var callback: RideServiceClientCallback? = null

    constructor(postData: MutableMap<String, LocationData>, type: RequestType, callback: RideServiceClientCallback) : this(postData) {
        Log.i(TAG, "constructing for type: $type")
        Log.i(TAG, "             callback: $callback")
        this.requestType = type;
        this.callback = callback;
    }

    override fun doInBackground(vararg params: String?): Void? {
        try {
            Log.i(TAG, "in background")
            val url = URL(params[0])
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.doInput = true
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.requestMethod = "POST"

            if (postData != null) {
                val writer = OutputStreamWriter(urlConnection.outputStream)
                writer.write(postData.toString())
                writer.flush()
            }
            val statusCode = urlConnection.responseCode
            Log.i(TAG, "received status code: $statusCode")
            if (statusCode == 200) {
                val inputStream: InputStream = BufferedInputStream(urlConnection.inputStream)
                val response: String = convertInputStreamToString(inputStream)

                Log.i(TAG, "when this type: $this.requestType")
                when (this.requestType) {
                    RequestType.REQUEST_POST_LOCATION -> {
                        Log.i(TAG, "was REQUEST_POST_LOCATION")
                        callback?.completionHandler(true, requestType)
                    }
                    RequestType.REQUEST_GET -> {
                        Log.i(TAG, "was REQUEST_GET")
                        print(response)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            Log.d(TAG, e.localizedMessage)
        }
        return null
    }

    private fun convertInputStreamToString(inputStream: InputStream?): String {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line: String?
        try {
            while (bufferedReader.readLine().also{ line = it } != null) {
                sb.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    init {
        if (postData != null) {
            this.postData = JSONObject(postData as Map<*, *>)
        }
    }
}