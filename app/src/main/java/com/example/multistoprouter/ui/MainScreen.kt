package com.example.multistoprouter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.multistoprouter.R
import com.example.multistoprouter.model.MainUiState
import com.example.multistoprouter.model.PlaceSuggestion
import com.example.multistoprouter.model.TravelMode
import com.example.multistoprouter.util.decodePolyline
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onStartQueryChanged: (String) -> Unit,
    onStartSuggestionSelected: (PlaceSuggestion) -> Unit,
    onViaQueryChanged: (String) -> Unit,
    onDestinationQueryChanged: (String) -> Unit,
    onDestinationSuggestionSelected: (PlaceSuggestion) -> Unit,
    onModeSelected: (TravelMode) -> Unit,
    hasLocationPermission: Boolean
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, 11f)
    }

    val route = uiState.routeSummary
    val markers = buildList {
        uiState.startSelection?.let { add("Start" to LatLng(it.latitude, it.longitude)) }
        route?.via?.let { add(it.name to LatLng(it.latitude, it.longitude)) }
        uiState.destinationSelection?.let { add("Ziel" to LatLng(it.latitude, it.longitude)) }
    }

    LaunchedEffect(route) {
        val destination = uiState.destinationSelection
        if (destination != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(destination.latitude, destination.longitude),
                    12f
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RouteFields(
            uiState = uiState,
            onStartQueryChanged = onStartQueryChanged,
            onStartSuggestionSelected = onStartSuggestionSelected,
            onViaQueryChanged = onViaQueryChanged,
            onDestinationQueryChanged = onDestinationQueryChanged,
            onDestinationSuggestionSelected = onDestinationSuggestionSelected
        )

        ModeSelector(uiState.travelMode, onModeSelected)

        Surface(tonalElevation = 2.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(8.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    cameraPositionState = cameraPositionState
                ) {
                    markers.forEach { (title, position) ->
                        Marker(position = position, title = title)
                    }
                    val polyline = route?.polyline
                    if (!polyline.isNullOrBlank()) {
                        Polyline(points = decodePolyline(polyline))
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }

        route?.let {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = it.via?.name ?: stringResource(id = R.string.via_label), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.duration_format, it.durationText, it.distanceText))
                    it.via?.address?.let { address ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteFields(
    uiState: MainUiState,
    onStartQueryChanged: (String) -> Unit,
    onStartSuggestionSelected: (PlaceSuggestion) -> Unit,
    onViaQueryChanged: (String) -> Unit,
    onDestinationQueryChanged: (String) -> Unit,
    onDestinationSuggestionSelected: (PlaceSuggestion) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SuggestionField(
            label = stringResource(id = R.string.start_label),
            value = uiState.startQuery,
            suggestions = uiState.startSuggestions,
            onValueChanged = onStartQueryChanged,
            onSuggestionSelected = onStartSuggestionSelected
        )
        SuggestionField(
            label = stringResource(id = R.string.via_label),
            value = uiState.viaQuery,
            suggestions = uiState.viaSuggestions,
            onValueChanged = onViaQueryChanged,
            onSuggestionSelected = { onViaQueryChanged(it.displayText) }
        )
        SuggestionField(
            label = stringResource(id = R.string.destination_label),
            value = uiState.destinationQuery,
            suggestions = uiState.destinationSuggestions,
            onValueChanged = onDestinationQueryChanged,
            onSuggestionSelected = onDestinationSuggestionSelected
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionField(
    label: String,
    value: String,
    suggestions: List<PlaceSuggestion>,
    onValueChanged: (String) -> Unit,
    onSuggestionSelected: (PlaceSuggestion) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = value,
            onValueChange = {
                expanded = true
                onValueChanged(it)
            },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded && suggestions.isNotEmpty(), onDismissRequest = { expanded = false }) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.displayText) },
                    onClick = {
                        expanded = false
                        onSuggestionSelected(suggestion)
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: TravelMode, onModeSelected: (TravelMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TravelMode.entries.forEach { mode ->
            val selectedColor = if (mode == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            AssistChip(
                onClick = { onModeSelected(mode) },
                label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) },
                leadingIcon = {
                    Icon(
                        imageVector = when (mode) {
                            TravelMode.DRIVING -> Icons.Default.DirectionsCar
                            TravelMode.WALKING -> Icons.Default.DirectionsWalk
                            TravelMode.BICYCLING -> Icons.Default.DirectionsBike
                            TravelMode.TRANSIT -> Icons.Default.DirectionsBus
                        },
                        contentDescription = null
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = selectedColor)
            )
        }
    }
}

private val DEFAULT_LOCATION = LatLng(52.52, 13.405)
