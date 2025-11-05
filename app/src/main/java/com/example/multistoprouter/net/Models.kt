package com.example.multistoprouter.net

import com.squareup.moshi.Json

data class DirectionsResponse(
    @Json(name = "routes") val routes: List<Route> = emptyList(),
    @Json(name = "status") val status: String = ""
)

data class Route(
    @Json(name = "legs") val legs: List<Leg> = emptyList(),
    @Json(name = "overview_polyline") val overviewPolyline: Polyline? = null,
    @Json(name = "summary") val summary: String? = null
)

data class Leg(
    @Json(name = "distance") val distance: ValueText? = null,
    @Json(name = "duration") val duration: ValueText? = null,
    @Json(name = "start_address") val startAddress: String? = null,
    @Json(name = "end_address") val endAddress: String? = null,
    @Json(name = "start_location") val startLocation: LatLng? = null,
    @Json(name = "end_location") val endLocation: LatLng? = null
)

data class ValueText(
    @Json(name = "value") val value: Long = 0L,
    @Json(name = "text") val text: String = ""
)

data class Polyline(@Json(name = "points") val points: String = "")

data class LatLng(
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lng") val lng: Double = 0.0
)
