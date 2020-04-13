package com.example.tolocation

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder.*

//   https://stackoverflow.com/questions/46177133/http-request-in-kotlin

class LocationPoster {
    fun sendPostRequest(userName:String, password:String) {

        val mURL = URL("10.0.1.33:8642")

        with(mURL.openConnection() as HttpURLConnection) {
            requestMethod = "POST"

            val wr = OutputStreamWriter(outputStream);
            wr.write(reqParam);
            wr.flush();

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                println("Response : $response")
            }
        }
    }

    fun sendGetRequest(userName:String, password:String) {

        var reqParam = encode("username", "UTF-8") + "=" + encode(userName, "UTF-8")
        reqParam += "&" + encode("password", "UTF-8") + "=" + encode(password, "UTF-8")

        val mURL = URL("<Yout API Link>?"+reqParam)

        with(mURL.openConnection() as HttpURLConnection) {
            // optional default is GET
            requestMethod = "GET"

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                it.close()
                println("Response : $response")
            }
        }
    }
}
