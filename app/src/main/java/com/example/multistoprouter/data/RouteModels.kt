package com.example.multistoprouter.data

/** Represents an autocomplete suggestion from the search endpoint. */
data class PlaceSuggestion(
    val id: String,
    val description: String,
    val address: String?,
    val latLng: LatLng
)

/** Snapshot of a place location used for routing. */
data class PlaceLocation(
    val id: String,
    val name: String,
    val address: String?,
    val latLng: LatLng
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
    val start: LatLng,
    val end: LatLng,
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
