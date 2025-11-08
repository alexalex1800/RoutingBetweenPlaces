package com.example.multistoprouter.net

import retrofit2.http.GET
import retrofit2.http.Query

interface PhotonApi {
    @GET("/api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("lang") language: String? = null,
        @Query("lat") latitude: Double? = null,
        @Query("lon") longitude: Double? = null
    ): PhotonFeatureCollection
}
