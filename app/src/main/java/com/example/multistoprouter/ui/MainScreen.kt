package com.example.multistoprouter.ui

import android.graphics.Color
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.multistoprouter.R
import com.example.multistoprouter.data.PlaceSuggestion
import com.example.multistoprouter.data.RouteStatus
import com.example.multistoprouter.data.RouteSummary
import com.example.multistoprouter.data.RouteUiState
import com.example.multistoprouter.data.TravelMode
import com.example.multistoprouter.net.decodePolyline
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
    val context = LocalContext.current
    val mapViewState = remember { MapView(context).apply { setTileSource(TileSourceFactory.MAPNIK) } }
    val updatedState = rememberUpdatedState(uiState)

    DisposableEffect(mapViewState) {
        mapViewState.onResume()
        mapViewState.setMultiTouchControls(true)
        onDispose {
            mapViewState.onPause()
            mapViewState.onDetach()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        AndroidView(
            factory = { mapViewState },
            modifier = Modifier.matchParentSize()
        ) { mapView ->
            val state = updatedState.value
            mapView.overlays.clear()

            state.startSelection?.let { selection ->
                mapView.overlays.add(createMarker(mapView, selection.latLng, selection.name))
            }
            state.stopoverSelection?.let { selection ->
                mapView.overlays.add(createMarker(mapView, selection.latLng, selection.name))
            }
            state.destinationSelection?.let { selection ->
                mapView.overlays.add(createMarker(mapView, selection.latLng, selection.name))
            }

            state.routePolyline?.let { encoded ->
                val points = decodePolyline(encoded).map { GeoPoint(it.latitude, it.longitude) }
                if (points.isNotEmpty()) {
                    val line = Polyline().apply {
                        outlinePaint.color = Color.parseColor("#1B5E20")
                        outlinePaint.strokeWidth = 6f
                        setPoints(points)
                    }
                    mapView.overlays.add(line)
                }
            }

            val target = state.mapCenter ?: state.startSelection?.latLng ?: state.destinationSelection?.latLng
            target?.let {
                mapView.controller.setZoom(state.mapZoom)
                mapView.controller.setCenter(GeoPoint(it.latitude, it.longitude))
            }

            mapView.invalidate()
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

private fun createMarker(mapView: MapView, latLng: com.example.multistoprouter.data.LatLng, title: String): Marker {
    return Marker(mapView).apply {
        position = GeoPoint(latLng.latitude, latLng.longitude)
        this.title = title
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
                Text(text = stringResource(id = R.string.route_summary_duration, summary.durationText))
                Text(text = stringResource(id = R.string.route_summary_distance, summary.distanceText))
            }
        }
    }
}
