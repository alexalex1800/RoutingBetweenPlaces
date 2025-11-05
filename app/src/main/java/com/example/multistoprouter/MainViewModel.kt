package com.example.multistoprouter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multistoprouter.data.CandidateResult
import com.example.multistoprouter.data.PlaceLocation
import com.example.multistoprouter.data.PlaceSuggestion
import com.example.multistoprouter.data.RouteStatus
import com.example.multistoprouter.data.RouteSummary
import com.example.multistoprouter.data.RouteUiState
import com.example.multistoprouter.data.TravelMode
import com.example.multistoprouter.data.hasCompleteSelection
import com.example.multistoprouter.data.RoutesRepository
import com.example.multistoprouter.location.LocationRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainViewModel(
    private val routesRepository: RoutesRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private val startQuery = MutableStateFlow("")
    private val stopoverQuery = MutableStateFlow("")
    private val destinationQuery = MutableStateFlow("")

    private var routeJob: Job? = null

    init {
        observeQueries()
    }

    private fun observeQueries() {
        viewModelScope.launch {
            startQuery
                .debounce(250)
                .distinctUntilChanged()
                .collect { query -> fetchSuggestions(query, FieldType.START) }
        }
        viewModelScope.launch {
            stopoverQuery
                .debounce(250)
                .distinctUntilChanged()
                .collect { query -> fetchSuggestions(query, FieldType.STOPOVER) }
        }
        viewModelScope.launch {
            destinationQuery
                .debounce(250)
                .distinctUntilChanged()
                .collect { query -> fetchSuggestions(query, FieldType.DESTINATION) }
        }
    }

    private fun fetchSuggestions(query: String, type: FieldType) {
        viewModelScope.launch {
            val suggestions = routesRepository.autocomplete(query)
            _uiState.value = when (type) {
                FieldType.START -> _uiState.value.copy(suggestionsStart = suggestions)
                FieldType.STOPOVER -> _uiState.value.copy(suggestionsStopover = suggestions)
                FieldType.DESTINATION -> _uiState.value.copy(suggestionsDestination = suggestions)
            }
        }
    }

    fun onStartQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(startQuery = value, startSelection = null)
        startQuery.value = value
    }

    fun onStopoverQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(stopoverQuery = value)
        stopoverQuery.value = value
        triggerRouteRefresh()
    }

    fun onDestinationQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(destinationQuery = value, destinationSelection = null)
        destinationQuery.value = value
    }

    fun onStartSuggestionSelected(suggestion: PlaceSuggestion) {
        _uiState.value = _uiState.value.copy(startQuery = suggestion.description)
        viewModelScope.launch {
            routesRepository.fetchPlace(suggestion.placeId)?.let { place ->
                _uiState.value = _uiState.value.copy(startSelection = place)
                triggerRouteRefresh()
            }
        }
    }

    fun onStopoverSuggestionSelected(suggestion: PlaceSuggestion) {
        val text = suggestion.description
        _uiState.value = _uiState.value.copy(stopoverQuery = text)
        stopoverQuery.value = text
        triggerRouteRefresh()
    }

    fun onDestinationSuggestionSelected(suggestion: PlaceSuggestion) {
        _uiState.value = _uiState.value.copy(destinationQuery = suggestion.description)
        viewModelScope.launch {
            routesRepository.fetchPlace(suggestion.placeId)?.let { place ->
                _uiState.value = _uiState.value.copy(destinationSelection = place)
                triggerRouteRefresh()
            }
        }
    }

    fun onTravelModeChange(mode: TravelMode) {
        _uiState.value = _uiState.value.copy(travelMode = mode)
        triggerRouteRefresh()
    }

    fun setCurrentLocation(latLng: LatLng) {
        val current = PlaceLocation(
            placeId = "",
            name = "Aktueller Standort",
            address = null,
            latLng = latLng
        )
        _uiState.value = _uiState.value.copy(
            startSelection = current,
            startQuery = current.name
        )
        triggerRouteRefresh()
    }

    fun refreshCurrentLocation() {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                setCurrentLocation(location)
            }
        }
    }

    private fun triggerRouteRefresh() {
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            delay(200)
            val state = _uiState.value
            if (!state.hasCompleteSelection()) return@launch
            val start = state.startSelection ?: return@launch
            val destination = state.destinationSelection ?: return@launch
            val stopoverQuery = state.stopoverQuery
            _uiState.value = state.copy(status = RouteStatus.Loading)
            val anchor = computeAnchor(start.latLng, destination.latLng)
            val result = routesRepository.findBestRoute(
                start = start,
                stopoverQuery = stopoverQuery,
                destination = destination,
                travelMode = state.travelMode,
                anchor = anchor
            )
            if (result == null) {
                _uiState.value = _uiState.value.copy(
                    status = RouteStatus.Error("Keine Route gefunden (Kandidaten oder Netzfehler)"),
                    routeSummary = null,
                    routePolyline = null,
                    stopoverSelection = null
                )
            } else {
                applyRouteResult(result)
            }
        }
    }

    private fun applyRouteResult(result: CandidateResult) {
        val legs = result.route.legs
        val lastLeg = legs.lastOrNull()
        val camera = lastLeg?.let {
            val mid = midpoint(legs.first().start, legs.last().end)
            CameraPosition.Builder()
                .target(mid)
                .zoom(11f)
                .build()
        }
        val summary = RouteSummary(
            stopoverName = result.candidate.name,
            stopoverAddress = result.candidate.address,
            distanceText = result.route.totalDistanceText,
            durationText = result.route.totalDurationText
        )
        _uiState.value = _uiState.value.copy(
            stopoverSelection = result.candidate,
            routePolyline = result.route.overviewPolyline,
            routeSummary = summary,
            cameraPosition = camera,
            status = RouteStatus.Idle
        )
    }

    private fun computeAnchor(start: LatLng, destination: LatLng): LatLng {
        return LatLng(
            (start.latitude + destination.latitude) / 2.0,
            (start.longitude + destination.longitude) / 2.0
        )
    }

    private fun midpoint(start: LatLng, end: LatLng): LatLng {
        return LatLng(
            (start.latitude + end.latitude) / 2.0,
            (start.longitude + end.longitude) / 2.0
        )
    }

    enum class FieldType { START, STOPOVER, DESTINATION }
}
