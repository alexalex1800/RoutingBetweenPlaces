package com.example.multistoprouter.data

import android.util.Log
import com.example.multistoprouter.BuildConfig
import com.example.multistoprouter.data.cache.InMemoryLruCache
import com.example.multistoprouter.net.DirectionsApi
import com.example.multistoprouter.net.DirectionsResponse
import com.example.multistoprouter.net.PlacesTextSearchApi
import com.example.multistoprouter.net.PlacesTextSearchResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.FetchPlaceRequest
import com.google.android.libraries.places.api.model.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Locale

private const val TAG = "RoutesRepository"

class RoutesRepository(
    private val placesClient: PlacesClient,
    private val directionsApi: DirectionsApi,
    private val textSearchApi: PlacesTextSearchApi,
    private val suggestionCache: InMemoryLruCache<String, List<PlaceSuggestion>> = InMemoryLruCache(50),
    private val placeCache: InMemoryLruCache<String, PlaceLocation> = InMemoryLruCache(50),
    private val routeCache: InMemoryLruCache<String, RouteCandidate> = InMemoryLruCache(30),
) {

    private val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)

    suspend fun autocomplete(query: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        suggestionCache.get(query)?.let { return@withContext it }
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()
        return@withContext try {
            val response = placesClient.findAutocompletePredictions(request).await()
            val suggestions = response.autocompletePredictions.map {
                PlaceSuggestion(placeId = it.placeId, description = it.getFullText(null).toString())
            }
            suggestionCache.put(query, suggestions)
            suggestions
        } catch (t: Throwable) {
            Log.e(TAG, "Autocomplete failed", t)
            emptyList()
        }
    }

    suspend fun fetchPlace(placeId: String): PlaceLocation? = withContext(Dispatchers.IO) {
        if (placeId.isBlank()) return@withContext null
        placeCache.get(placeId)?.let { return@withContext it }
        val request = FetchPlaceRequest.builder(placeId, fields).build()
        return@withContext try {
            val response = placesClient.fetchPlace(request).await()
            val place = response.place
            val latLng = place.latLng ?: return@withContext null
            val location = PlaceLocation(
                placeId = place.id ?: placeId,
                name = place.name ?: place.address ?: "Unbenannter Ort",
                address = place.address,
                latLng = latLng
            )
            placeCache.put(placeId, location)
            location
        } catch (t: Throwable) {
            Log.e(TAG, "Fetch place failed", t)
            null
        }
    }

    suspend fun findCandidates(
        query: String,
        anchor: LatLng?,
        radiusMeters: Int = 10_000
    ): List<PlaceLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val locationString = anchor?.let { "${it.latitude},${it.longitude}" }
        return@withContext try {
            val response = textSearchApi.textSearch(
                query = query,
                location = locationString,
                radius = if (anchor != null) radiusMeters else null,
                key = BuildConfig.MAPS_API_KEY
            )
            response.results
                .take(10)
                .mapNotNull { it.toPlaceLocation() }
        } catch (http: HttpException) {
            Log.e(TAG, "Text search http error", http)
            emptyList()
        } catch (t: Throwable) {
            Log.e(TAG, "Text search failed", t)
            emptyList()
        }
    }

    private suspend fun requestRoute(
        start: PlaceLocation,
        waypoint: PlaceLocation,
        destination: PlaceLocation,
        travelMode: TravelMode
    ): RouteCandidate? = withContext(Dispatchers.IO) {
        val cacheKey = listOf(
            start.cacheKey(),
            waypoint.cacheKey(),
            destination.cacheKey(),
            travelMode.apiValue
        ).joinToString(separator = "|")
        routeCache.get(cacheKey)?.let { return@withContext it }
        val startString = start.latLng.toQueryParam()
        val destinationString = destination.latLng.toQueryParam()
        val waypointString = waypoint.latLng.toQueryParam()
        val response = try {
            directionsApi.route(
                origin = startString,
                destination = destinationString,
                waypoints = waypointString,
                mode = travelMode.apiValue,
                key = BuildConfig.MAPS_API_KEY
            )
        } catch (http: HttpException) {
            Log.e(TAG, "Directions http error", http)
            return@withContext null
        } catch (t: Throwable) {
            Log.e(TAG, "Directions error", t)
            return@withContext null
        }
        val candidate = response.toRouteCandidate()
        if (candidate != null) {
            routeCache.put(cacheKey, candidate)
        }
        candidate
    }

    suspend fun findBestRoute(
        start: PlaceLocation,
        stopoverQuery: String,
        destination: PlaceLocation,
        travelMode: TravelMode,
        anchor: LatLng?
    ): CandidateResult? {
        val candidates = findCandidates(stopoverQuery, anchor)
        if (candidates.isEmpty()) return null
        val evaluated = candidates.mapNotNull { candidate ->
            val route = requestRoute(start, candidate, destination, travelMode) ?: return@mapNotNull null
            CandidateResult(candidate, route)
        }
        return selectBestCandidate(evaluated)
    }
}

private fun PlaceLocation.cacheKey(): String = if (placeId.isNotBlank()) placeId else "${latLng.latitude},${latLng.longitude}"

private fun PlacesTextSearchResult.toPlaceLocation(): PlaceLocation? {
    val latLng = geometry?.location ?: return null
    val mapsLatLng = LatLng(latLng.lat, latLng.lng)
    return PlaceLocation(
        placeId = placeId,
        name = name,
        address = formattedAddress,
        latLng = mapsLatLng
    )
}

private fun LatLng.toQueryParam(): String = "${latitude},${longitude}"

private fun DirectionsResponse.toRouteCandidate(): RouteCandidate? {
    val firstRoute = routes.firstOrNull() ?: return null
    val legs = firstRoute.legs.mapNotNull { leg ->
        val distance = leg.distance ?: return@mapNotNull null
        val duration = leg.duration ?: return@mapNotNull null
        RouteLeg(
            start = LatLng(leg.startLocation.lat, leg.startLocation.lng),
            end = LatLng(leg.endLocation.lat, leg.endLocation.lng),
            distanceMeters = distance.value,
            distanceText = distance.text,
            durationSeconds = duration.value,
            durationText = duration.text
        )
    }
    if (legs.isEmpty()) return null
    val totalDistanceMeters = legs.sumOf { it.distanceMeters }
    val totalDurationSeconds = legs.sumOf { it.durationSeconds }
    val totalDistanceText = formatDistance(totalDistanceMeters)
    val totalDurationText = formatDuration(totalDurationSeconds)
    return RouteCandidate(
        overviewPolyline = firstRoute.overviewPolyline?.points,
        totalDistanceMeters = totalDistanceMeters,
        totalDistanceText = totalDistanceText,
        totalDurationSeconds = totalDurationSeconds,
        totalDurationText = totalDurationText,
        legs = legs
    )
}

fun selectBestCandidate(candidates: List<CandidateResult>): CandidateResult? {
    return candidates.minWithOrNull(
        compareBy<CandidateResult> { it.route.totalDurationSeconds }
            .thenBy { it.route.totalDistanceMeters }
    )
}

private fun formatDistance(meters: Long): String {
    if (meters >= 1000) {
        val km = meters / 1000.0
        return String.format(Locale.getDefault(), "%.1f km", km)
    }
    return "$meters m"
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%d h %02d min", hours, minutes)
        minutes > 0 -> String.format(Locale.getDefault(), "%d min", minutes)
        else -> String.format(Locale.getDefault(), "%d s", seconds)
    }
}
