package com.example.multistoprouter.repository

import com.example.multistoprouter.cache.SimpleLruCache
import com.example.multistoprouter.model.PlaceDetails
import com.example.multistoprouter.model.RouteSummary
import com.example.multistoprouter.model.TravelMode
import com.example.multistoprouter.util.selectBestRoute
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoutePlanner(
    private val placesRepository: PlacesRepository,
    private val directionsRepository: DirectionsRepository,
    private val routesCache: SimpleLruCache<String, RouteSummary> = SimpleLruCache(32)
) {
    suspend fun findBestRoute(
        start: PlaceDetails,
        viaQuery: String,
        destination: PlaceDetails,
        travelMode: TravelMode,
        apiKey: String
    ): RouteSummary? = withContext(Dispatchers.IO) {
        if (viaQuery.isBlank()) {
            return@withContext directionsRepository.requestRoute(start, null, destination, travelMode, apiKey)
        }

        val candidates = placesRepository.findCandidates(
            query = viaQuery,
            origin = LatLng(start.latitude, start.longitude)
        )
        if (candidates.isEmpty()) return@withContext null

        val evaluations = candidates.mapNotNull { candidate ->
            val cacheKey = cacheKey(start, candidate, destination, travelMode)
            val summary = routesCache.get(cacheKey)
                ?: directionsRepository.requestRoute(start, candidate, destination, travelMode, apiKey)?.also {
                    routesCache.put(cacheKey, it)
                }
            summary?.let { com.example.multistoprouter.model.RouteCandidate(candidate, it) }
        }
        return@withContext selectBestRoute(evaluations)?.route
    }

    private fun cacheKey(
        start: PlaceDetails,
        via: PlaceDetails,
        destination: PlaceDetails,
        mode: TravelMode
    ): String = buildString {
        append(start.placeId.ifBlank { start.asLocationString() })
        append('|')
        append(via.placeId)
        append('|')
        append(destination.placeId.ifBlank { destination.asLocationString() })
        append('|')
        append(mode.name)
    }
}
