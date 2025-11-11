package com.example.multistoprouter.net

import retrofit2.http.POST
import retrofit2.http.Query

interface OverpassApi {
    @POST("/api/interpreter")
    suspend fun query(
        @Query("data") data: String
    ): OverpassResponse
}
