package com.example.multistoprouter.net

import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesTextSearchApi {
    @GET("/maps/api/place/textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("location") location: String?,
        @Query("radius") radius: Int?,
        @Query("key") key: String
    ): PlacesTextSearchResponse
}
