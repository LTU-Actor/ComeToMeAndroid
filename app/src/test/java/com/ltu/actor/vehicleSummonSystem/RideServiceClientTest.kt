package com.ltu.actor.vehicleSummonSystem

import android.location.Location
import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test

class RideServiceClientTest {

    @Test
    fun testPostRequest() {
        // Given
        var subject: RideServiceClient = RideServiceClient()
        var answer: Result<String, Exception>? = null
        var location: Location = Location("TestProvider")

        // When
        runBlocking {
            answer = subject.sendPostRequestForLocation(location).await()
        }

        // Then
        assertEquals("Success(value=Hello)", answer.toString())
    }
}