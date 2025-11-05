package com.example.multistoprouter.model

data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String?
) {
    val displayText: String = listOfNotNull(primaryText, secondaryText).joinToString(" · ")
}

data class PlaceDetails(
    val placeId: String,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double
) {
    fun asLocationString(): String = "$latitude,$longitude"
}

data class RouteCandidate(
    val candidate: PlaceDetails,
    val route: RouteSummary
)

data class RouteSummary(
    val durationSeconds: Long,
    val durationText: String,
    val distanceMeters: Long,
    val distanceText: String,
    val polyline: String?,
    val start: PlaceDetails?,
    val via: PlaceDetails?,
    val destination: PlaceDetails?
)
