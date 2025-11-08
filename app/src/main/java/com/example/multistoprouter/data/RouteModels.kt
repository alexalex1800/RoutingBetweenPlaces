package com.example.multistoprouter.data

/** Simple latitude/longitude coordinate used across the app. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/** Represents an autocomplete suggestion from Photon (OpenStreetMap search). */
data class PlaceSuggestion(
    val id: String,
    val description: String,
    val address: String?,
    val coordinate: GeoPoint
) {
    fun toPlaceLocation(): PlaceLocation = PlaceLocation(
        placeId = id,
        name = description,
        address = address,
        point = coordinate
    )
}

/** Snapshot of a place location used for routing. */
data class PlaceLocation(
    val placeId: String,
    val name: String,
    val address: String?,
    val point: GeoPoint
)

data class CandidateResult(
    val candidate: PlaceLocation,
    val route: RouteCandidate
)

data class RouteCandidate(
    val overviewPolyline: String?,
    val totalDistanceMeters: Long,
    val totalDistanceText: String,
    val totalDurationSeconds: Long,
    val totalDurationText: String,
    val legs: List<RouteLeg>
)

data class RouteLeg(
    val start: GeoPoint,
    val end: GeoPoint,
    val distanceMeters: Long,
    val distanceText: String,
    val durationSeconds: Long,
    val durationText: String
)

/** UI summary of the selected route. */
data class RouteSummary(
    val stopoverName: String,
    val stopoverAddress: String?,
    val distanceText: String,
    val durationText: String
)
