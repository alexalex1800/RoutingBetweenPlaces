package com.example.multistoprouter

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multistoprouter.model.MainUiState
import com.example.multistoprouter.model.PlaceDetails
import com.example.multistoprouter.model.PlaceSuggestion
import com.example.multistoprouter.model.TravelMode
import com.example.multistoprouter.repository.PlacesRepository
import com.example.multistoprouter.repository.RoutePlanner
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val placesRepository: PlacesRepository,
    private val routePlanner: RoutePlanner,
    private val mapsApiKey: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val startToken = AutocompleteSessionToken.newInstance()
    private val destinationToken = AutocompleteSessionToken.newInstance()
    private val viaToken = AutocompleteSessionToken.newInstance()

    private var startSearchJob: Job? = null
    private var destinationSearchJob: Job? = null
    private var viaSearchJob: Job? = null

    fun setCurrentLocation(location: Location) {
        val place = PlaceDetails(
            placeId = "",
            name = "Aktueller Standort",
            address = null,
            latitude = location.latitude,
            longitude = location.longitude
        )
        _uiState.value = _uiState.value.copy(
            startSelection = place,
            startQuery = "Aktueller Standort",
            useCurrentLocation = true
        )
        triggerRouteUpdate()
    }

    fun onStartQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(startQuery = query, useCurrentLocation = false)
        startSearchJob?.cancel()
        startSearchJob = viewModelScope.launch {
            delay(AUTOCOMPLETE_DEBOUNCE_MS)
            val origin = _uiState.value.startSelection?.let { LatLng(it.latitude, it.longitude) }
            val suggestions = runCatching {
                placesRepository.autocomplete(query, startToken, origin)
            }.getOrElse {
                postError(it)
                emptyList()
            }
            updateStartSuggestions(suggestions)
        }
    }

    fun selectStartSuggestion(suggestion: PlaceSuggestion) {
        viewModelScope.launch {
            val details = placesRepository.fetchDetails(suggestion.placeId)
            if (details != null) {
                _uiState.value = _uiState.value.copy(
                    startSelection = details,
                    startQuery = suggestion.displayText,
                    useCurrentLocation = false
                )
                triggerRouteUpdate()
            }
        }
    }

    fun onDestinationQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(destinationQuery = query)
        destinationSearchJob?.cancel()
        destinationSearchJob = viewModelScope.launch {
            delay(AUTOCOMPLETE_DEBOUNCE_MS)
            val suggestions = runCatching {
                placesRepository.autocomplete(query, destinationToken)
            }.getOrElse {
                postError(it)
                emptyList()
            }
            updateDestinationSuggestions(suggestions)
        }
    }

    fun selectDestinationSuggestion(suggestion: PlaceSuggestion) {
        viewModelScope.launch {
            val details = placesRepository.fetchDetails(suggestion.placeId)
            if (details != null) {
                _uiState.value = _uiState.value.copy(
                    destinationSelection = details,
                    destinationQuery = suggestion.displayText
                )
                triggerRouteUpdate()
            }
        }
    }

    fun onViaQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(viaQuery = query)
        viaSearchJob?.cancel()
        viaSearchJob = viewModelScope.launch {
            delay(AUTOCOMPLETE_DEBOUNCE_MS)
            val start = _uiState.value.startSelection
            val origin = start?.let { LatLng(it.latitude, it.longitude) }
            val suggestions = runCatching {
                placesRepository.autocomplete(query, viaToken, origin)
            }.getOrElse {
                postError(it)
                emptyList()
            }
            updateViaSuggestions(suggestions)
            triggerRouteUpdate()
        }
    }

    fun onTravelModeSelected(mode: TravelMode) {
        if (_uiState.value.travelMode == mode) return
        _uiState.value = _uiState.value.copy(travelMode = mode)
        triggerRouteUpdate()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun updateStartSuggestions(suggestions: List<PlaceSuggestion>) {
        _uiState.value = _uiState.value.copy(startSuggestions = suggestions)
    }

    private fun updateDestinationSuggestions(suggestions: List<PlaceSuggestion>) {
        _uiState.value = _uiState.value.copy(destinationSuggestions = suggestions)
    }

    private fun updateViaSuggestions(suggestions: List<PlaceSuggestion>) {
        _uiState.value = _uiState.value.copy(viaSuggestions = suggestions)
    }

    private fun triggerRouteUpdate() {
        val state = _uiState.value
        val start = state.startSelection
        val destination = state.destinationSelection
        if (start == null || destination == null) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val summary = runCatching {
                routePlanner.findBestRoute(
                    start = start,
                    viaQuery = state.viaQuery,
                    destination = destination,
                    travelMode = state.travelMode,
                    apiKey = mapsApiKey
                )
            }.getOrElse {
                postError(it)
                null
            }
            if (summary == null && state.viaQuery.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    routeSummary = null,
                    isLoading = false,
                    errorMessage = NO_CANDIDATE_MESSAGE
                )
            } else {
                _uiState.value = _uiState.value.copy(routeSummary = summary, isLoading = false)
            }
        }
    }

    private fun postError(throwable: Throwable) {
        _uiState.value = _uiState.value.copy(errorMessage = throwable.message ?: GENERIC_ERROR)
    }

    companion object {
        private const val AUTOCOMPLETE_DEBOUNCE_MS = 250L
        private const val GENERIC_ERROR = "Routing fehlgeschlagen"
        private const val NO_CANDIDATE_MESSAGE = "Kein passendes Zwischenziel gefunden."
    }
}

class MainViewModelFactory(
    private val placesRepository: PlacesRepository,
    private val routePlanner: RoutePlanner,
    private val mapsApiKey: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(placesRepository, routePlanner, mapsApiKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
