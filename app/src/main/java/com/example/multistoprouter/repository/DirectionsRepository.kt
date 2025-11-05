package com.example.multistoprouter.repository

import com.example.multistoprouter.model.PlaceDetails
import com.example.multistoprouter.model.RouteSummary
import com.example.multistoprouter.model.TravelMode
import com.example.multistoprouter.net.DirectionsApi
import com.example.multistoprouter.net.DirectionsResponse
import com.example.multistoprouter.net.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DirectionsRepository(private val api: DirectionsApi) {
    suspend fun requestRoute(
        start: PlaceDetails,
        via: PlaceDetails?,
        destination: PlaceDetails,
        travelMode: TravelMode,
        apiKey: String
    ): RouteSummary? = withContext(Dispatchers.IO) {
        val waypoints = via?.asLocationString()
        val response = runCatching {
            api.route(
                origin = start.asLocationString(),
                destination = destination.asLocationString(),
                waypoints = waypoints,
                mode = travelMode.directionsValue,
                key = apiKey
            )
        }.getOrNull() ?: return@withContext null
        parseRoute(response, start, via, destination)
    }

    private fun parseRoute(
        response: DirectionsResponse,
        start: PlaceDetails,
        via: PlaceDetails?,
        destination: PlaceDetails
    ): RouteSummary? {
        val route: Route = response.routes.firstOrNull() ?: return null
        val legs = route.legs
        if (legs.isEmpty()) return null
        val distanceMeters = legs.sumOf { it.distance?.value ?: 0L }
        val durationSeconds = legs.sumOf { it.duration?.value ?: 0L }
        val distanceText = legs.joinToString(" + ") { it.distance?.text.orEmpty() }
        val durationText = legs.joinToString(" + ") { it.duration?.text.orEmpty() }
        return RouteSummary(
            durationSeconds = durationSeconds,
            durationText = durationText,
            distanceMeters = distanceMeters,
            distanceText = distanceText,
            polyline = route.overviewPolyline?.points,
            start = start,
            via = via,
            destination = destination
        )
    }
}
