package com.example.multistoprouter.net

import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApi {
    @GET("/maps/api/directions/json")
    suspend fun route(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String?,
        @Query("mode") mode: String,
        @Query("key") key: String
    ): DirectionsResponse
}
