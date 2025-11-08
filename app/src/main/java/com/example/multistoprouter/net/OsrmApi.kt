package com.example.multistoprouter.net

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OsrmApi {
    @GET("/route/v1/{profile}/{coordinates}")
    suspend fun route(
        @Path("profile") profile: String,
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("steps") steps: Boolean = false
    ): OsrmResponse
}
