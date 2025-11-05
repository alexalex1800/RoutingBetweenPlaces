package com.example.multistoprouter.net

import com.squareup.moshi.Json

data class DirectionsResponse(
    @Json(name = "routes") val routes: List<Route> = emptyList(),
    @Json(name = "status") val status: String = "",
    @Json(name = "error_message") val errorMessage: String? = null
)

data class Route(
    @Json(name = "legs") val legs: List<Leg> = emptyList(),
    @Json(name = "overview_polyline") val overviewPolyline: Polyline? = null
)

data class Leg(
    @Json(name = "distance") val distance: ValueText?,
    @Json(name = "duration") val duration: ValueText?,
    @Json(name = "start_location") val startLocation: LatLng,
    @Json(name = "end_location") val endLocation: LatLng
)

data class ValueText(
    @Json(name = "value") val value: Long = 0L,
    @Json(name = "text") val text: String = ""
)

data class Polyline(
    @Json(name = "points") val points: String = ""
)

data class LatLng(
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lng") val lng: Double = 0.0
)

data class PlacesTextSearchResponse(
    @Json(name = "results") val results: List<PlacesTextSearchResult> = emptyList(),
    @Json(name = "status") val status: String = "",
    @Json(name = "error_message") val errorMessage: String? = null
)

data class PlacesTextSearchResult(
    @Json(name = "place_id") val placeId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "formatted_address") val formattedAddress: String? = null,
    @Json(name = "geometry") val geometry: Geometry? = null
)

data class Geometry(
    @Json(name = "location") val location: LatLng? = null
)
