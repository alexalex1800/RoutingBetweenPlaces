package com.example.multistoprouter.net

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OsrmResponse(
    @Json(name = "code") val code: String?,
    @Json(name = "routes") val routes: List<OsrmRoute> = emptyList(),
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    @Json(name = "distance") val distance: Double?,
    @Json(name = "duration") val duration: Double?,
    @Json(name = "geometry") val geometry: String?,
    @Json(name = "legs") val legs: List<OsrmLeg> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmLeg(
    @Json(name = "distance") val distance: Double?,
    @Json(name = "duration") val duration: Double?
)
