package com.example.multistoprouter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.multistoprouter.R
import com.example.multistoprouter.data.PlaceSuggestion
import com.example.multistoprouter.data.RouteStatus
import com.example.multistoprouter.data.RouteSummary
import com.example.multistoprouter.data.RouteUiState
import com.example.multistoprouter.data.TravelMode
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MultiStopRouterApp(
    uiState: RouteUiState,
    onStartQueryChange: (String) -> Unit,
    onStopoverQueryChange: (String) -> Unit,
    onDestinationQueryChange: (String) -> Unit,
    onStartSuggestionSelected: (PlaceSuggestion) -> Unit,
    onStopoverSuggestionSelected: (PlaceSuggestion) -> Unit,
    onDestinationSuggestionSelected: (PlaceSuggestion) -> Unit,
    onTravelModeChange: (TravelMode) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.status) {
        if (uiState.status is RouteStatus.Error) {
            snackbarHostState.showSnackbar((uiState.status as RouteStatus.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AutoCompleteTextField(
                label = stringResource(id = R.string.start_label),
                value = uiState.startQuery,
                onValueChange = onStartQueryChange,
                suggestions = uiState.suggestionsStart,
                onSuggestionSelected = onStartSuggestionSelected
            )
            AutoCompleteTextField(
                label = stringResource(id = R.string.stopover_label),
                value = uiState.stopoverQuery,
                onValueChange = onStopoverQueryChange,
                suggestions = uiState.suggestionsStopover,
                onSuggestionSelected = onStopoverSuggestionSelected
            )
            AutoCompleteTextField(
                label = stringResource(id = R.string.destination_label),
                value = uiState.destinationQuery,
                onValueChange = onDestinationQueryChange,
                suggestions = uiState.suggestionsDestination,
                onSuggestionSelected = onDestinationSuggestionSelected
            )

            TravelModeSelector(
                current = uiState.travelMode,
                onModeSelected = onTravelModeChange
            )

            MapSection(uiState = uiState)

            RouteSummaryCard(uiState.routeSummary)
        }
    }
}

@Composable
private fun AutoCompleteTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<PlaceSuggestion>,
    onSuggestionSelected: (PlaceSuggestion) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = {
                expanded.value = true
                onValueChange(it)
            },
            label = { Text(label) }
        )
        DropdownMenu(
            expanded = expanded.value && suggestions.isNotEmpty(),
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.description) },
                    onClick = {
                        expanded.value = false
                        onSuggestionSelected(suggestion)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelModeSelector(
    current: TravelMode,
    onModeSelected: (TravelMode) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val modes = TravelMode.entries
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(id = R.string.travel_mode_label), fontWeight = FontWeight.Bold)
        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = { expanded.value = it }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                value = current.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.travel_mode_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) }
            )
            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                modes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.displayName) },
                        onClick = {
                            expanded.value = false
                            onModeSelected(mode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapSection(uiState: RouteUiState) {
    val defaultPosition = CameraPosition.fromLatLngZoom(LatLng(52.52, 13.4050), 10f)
    val cameraPositionState = rememberCameraPositionState {
        position = uiState.cameraPosition ?: defaultPosition
    }

    LaunchedEffect(uiState.cameraPosition) {
        uiState.cameraPosition?.let { position ->
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(position))
        }
    }

    val polylinePoints = remember(uiState.routePolyline) {
        uiState.routePolyline?.let { PolyUtil.decode(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState
        ) {
            uiState.startSelection?.let {
                Marker(state = rememberMarkerState(position = it.latLng), title = it.name)
            }
            uiState.stopoverSelection?.let {
                Marker(state = rememberMarkerState(position = it.latLng), title = it.name)
            }
            uiState.destinationSelection?.let {
                Marker(state = rememberMarkerState(position = it.latLng), title = it.name)
            }
            polylinePoints?.let { points ->
                Polyline(points = points)
            }
        }
        if (uiState.status is RouteStatus.Loading) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun RouteSummaryCard(summary: RouteSummary?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (summary == null) {
                Text(text = stringResource(id = R.string.route_summary_placeholder))
            } else {
                Text(text = summary.stopoverName, style = MaterialTheme.typography.titleMedium)
                summary.stopoverAddress?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Dauer: ${summary.durationText}")
                Text(text = "Distanz: ${summary.distanceText}")
            }
        }
    }
}
