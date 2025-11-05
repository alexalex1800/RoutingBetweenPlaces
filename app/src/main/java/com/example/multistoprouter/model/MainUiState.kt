package com.example.multistoprouter.model

data class MainUiState(
    val isLoading: Boolean = false,
    val travelMode: TravelMode = TravelMode.default,
    val startQuery: String = "",
    val startSuggestions: List<PlaceSuggestion> = emptyList(),
    val startSelection: PlaceDetails? = null,
    val useCurrentLocation: Boolean = true,
    val viaQuery: String = "",
    val viaSuggestions: List<PlaceSuggestion> = emptyList(),
    val destinationQuery: String = "",
    val destinationSuggestions: List<PlaceSuggestion> = emptyList(),
    val destinationSelection: PlaceDetails? = null,
    val routeSummary: RouteSummary? = null,
    val errorMessage: String? = null
)
