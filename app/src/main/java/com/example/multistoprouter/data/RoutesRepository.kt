package com.example.multistoprouter.data

import android.util.Log
import com.example.multistoprouter.data.cache.InMemoryLruCache
import com.example.multistoprouter.net.OsrmApi
import com.example.multistoprouter.net.OsrmResponse
import com.example.multistoprouter.net.PhotonApi
import com.example.multistoprouter.net.PhotonFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Locale

private const val TAG = "RoutesRepository"

class RoutesRepository(
    private val photonApi: PhotonApi,
    private val osrmApi: OsrmApi,
    private val suggestionCache: InMemoryLruCache<String, List<PlaceSuggestion>> = InMemoryLruCache(50),
    private val routeCache: InMemoryLruCache<String, RouteCandidate> = InMemoryLruCache(30)
) {

    suspend fun autocomplete(query: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        suggestionCache.get(query)?.let { return@withContext it }
        val language = Locale.getDefault().language
        return@withContext try {
            val response = photonApi.search(query = query, limit = 8, language = language)
            val suggestions = response.features.mapNotNull { it.toSuggestion() }
            suggestionCache.put(query, suggestions)
            suggestions
        } catch (http: HttpException) {
            Log.e(TAG, "Photon autocomplete HTTP error", http)
            emptyList()
        } catch (t: Throwable) {
            Log.e(TAG, "Photon autocomplete failed", t)
            emptyList()
        }
    }

    suspend fun findCandidates(
        query: String,
        anchor: GeoPoint?,
        limit: Int = 10
    ): List<PlaceLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val language = Locale.getDefault().language
        return@withContext try {
            val response = photonApi.search(
                query = query,
                limit = limit,
                language = language,
                latitude = anchor?.latitude,
                longitude = anchor?.longitude
            )
            response.features.mapNotNull { feature ->
                feature.toSuggestion()?.toPlaceLocation()
            }
        } catch (http: HttpException) {
            Log.e(TAG, "Photon candidate HTTP error", http)
            emptyList()
        } catch (t: Throwable) {
            Log.e(TAG, "Photon candidate search failed", t)
            emptyList()
        }
    }

    private suspend fun requestRoute(
        start: PlaceLocation,
        waypoint: PlaceLocation,
        destination: PlaceLocation,
        travelMode: TravelMode
    ): RouteCandidate? = withContext(Dispatchers.IO) {
        val profile = travelMode.osrmProfile ?: return@withContext null
        val cacheKey = listOf(
            start.cacheKey(),
            waypoint.cacheKey(),
            destination.cacheKey(),
            profile
        ).joinToString(separator = "|")
        routeCache.get(cacheKey)?.let { return@withContext it }
        val coordinates = listOf(start.point, waypoint.point, destination.point)
            .joinToString(separator = ";") { "${it.longitude},${it.latitude}" }
        val response = try {
            osrmApi.route(profile = profile, coordinates = coordinates)
        } catch (http: HttpException) {
            Log.e(TAG, "OSRM route HTTP error", http)
            return@withContext null
        } catch (t: Throwable) {
            Log.e(TAG, "OSRM route failed", t)
            return@withContext null
        }
        val candidate = response.toRouteCandidate(start, waypoint, destination)
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
        anchor: GeoPoint?
    ): CandidateResult? {
        if (!travelMode.isSupported) return null
        val candidates = findCandidates(stopoverQuery, anchor)
        if (candidates.isEmpty()) return null
        val evaluated = candidates.mapNotNull { candidate ->
            val route = requestRoute(start, candidate, destination, travelMode) ?: return@mapNotNull null
            CandidateResult(candidate, route)
        }
        return selectBestCandidate(evaluated)
    }
}

private fun PhotonFeature.toSuggestion(): PlaceSuggestion? {
    val geometry = geometry ?: return null
    if (geometry.coordinates.size < 2) return null
    val lon = geometry.coordinates[0]
    val lat = geometry.coordinates[1]
    val name = properties?.name ?: return null
    val id = listOfNotNull(properties?.osmType, properties?.osmId?.toString()).joinToString(":")
    val addressParts = listOfNotNull(
        properties?.street,
        properties?.postcode,
        properties?.city,
        properties?.country
    )
    val address = if (addressParts.isEmpty()) null else addressParts.joinToString(", ")
    return PlaceSuggestion(
        id = if (id.isNotBlank()) id else "$lon,$lat",
        description = name,
        address = address,
        coordinate = GeoPoint(latitude = lat, longitude = lon)
    )
}

private fun PlaceLocation.cacheKey(): String =
    if (placeId.isNotBlank()) placeId else "${point.latitude},${point.longitude}"

private fun OsrmResponse.toRouteCandidate(
    start: PlaceLocation,
    waypoint: PlaceLocation,
    destination: PlaceLocation
): RouteCandidate? {
    if (!code.equals("Ok", ignoreCase = true)) {
        Log.w(TAG, "OSRM response not OK: $code $message")
        return null
    }
    val osrmRoute = routes.firstOrNull() ?: return null
    val routeLegs = osrmRoute.legs
    val orderedPoints = listOf(start.point, waypoint.point, destination.point)
    if (routeLegs.size != orderedPoints.size - 1) return null
    val legs = routeLegs.zip(orderedPoints.zipWithNext()).map { (leg, points) ->
        val (startPoint, endPoint) = points
        RouteLeg(
            start = startPoint,
            end = endPoint,
            distanceMeters = leg.distance?.toLong() ?: return null,
            distanceText = formatDistance(leg.distance ?: return null),
            durationSeconds = leg.duration?.toLong() ?: return null,
            durationText = formatDuration(leg.duration ?: return null)
        )
    }
    val totalDistanceMeters = osrmRoute.distance?.toLong() ?: legs.sumOf { it.distanceMeters }
    val totalDurationSeconds = osrmRoute.duration?.toLong() ?: legs.sumOf { it.durationSeconds }
    return RouteCandidate(
        overviewPolyline = osrmRoute.geometry,
        totalDistanceMeters = totalDistanceMeters,
        totalDistanceText = formatDistance(totalDistanceMeters.toDouble()),
        totalDurationSeconds = totalDurationSeconds,
        totalDurationText = formatDuration(totalDurationSeconds.toDouble()),
        legs = legs
    )
}

fun selectBestCandidate(candidates: List<CandidateResult>): CandidateResult? {
    return candidates.minWithOrNull(
        compareBy<CandidateResult> { it.route.totalDurationSeconds }
            .thenBy { it.route.totalDistanceMeters }
    )
}

private fun formatDistance(meters: Double): String {
    if (meters >= 1000) {
        val km = meters / 1000.0
        return String.format(Locale.getDefault(), "%.1f km", km)
    }
    return String.format(Locale.getDefault(), "%.0f m", meters)
}

private fun formatDuration(seconds: Double): String {
    val totalSeconds = seconds.toLong()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%d h %02d min", hours, minutes)
        minutes > 0 -> String.format(Locale.getDefault(), "%d min", minutes)
        else -> String.format(Locale.getDefault(), "%d s", totalSeconds)
    }
}
