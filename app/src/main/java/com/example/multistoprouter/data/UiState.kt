package com.example.multistoprouter.data

sealed class RouteStatus {
    data object Idle : RouteStatus()
    data object Loading : RouteStatus()
    data class Error(val message: String) : RouteStatus()
}

data class RouteUiState(
    val startQuery: String = "",
    val startSelection: PlaceLocation? = null,
    val stopoverQuery: String = "",
    val destinationQuery: String = "",
    val stopoverSelection: PlaceLocation? = null,
    val destinationSelection: PlaceLocation? = null,
    val travelMode: TravelMode = TravelMode.DRIVING,
    val suggestionsStart: List<PlaceSuggestion> = emptyList(),
    val suggestionsStopover: List<PlaceSuggestion> = emptyList(),
    val suggestionsDestination: List<PlaceSuggestion> = emptyList(),
    val routePolyline: String? = null,
    val mapCenter: LatLng? = null,
    val mapZoom: Double = 10.0,
    val routeSummary: RouteSummary? = null,
    val status: RouteStatus = RouteStatus.Idle
)

fun RouteUiState.hasCompleteSelection(): Boolean =
    startSelection != null && destinationSelection != null && stopoverQuery.isNotBlank()

data class LatLngBoundsData(val southwest: LatLng, val northeast: LatLng)
