package com.example.multistoprouter.data

import android.util.Log
import com.example.multistoprouter.data.cache.InMemoryLruCache
import com.example.multistoprouter.net.OsrmApi
import com.example.multistoprouter.net.OsrmResponse
import com.example.multistoprouter.net.OverpassApi
import com.example.multistoprouter.net.PhotonApi
import com.example.multistoprouter.net.PhotonFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Locale

private const val TAG = "RoutesRepository"

class RoutesRepository(
    private val photonApi: PhotonApi,
    private val overpassApi: OverpassApi,
    private val osrmApi: OsrmApi,
    private val suggestionCache: InMemoryLruCache<String, List<PlaceSuggestion>> = InMemoryLruCache(50),
    private val stopoverCache: InMemoryLruCache<String, List<PlaceLocation>> = InMemoryLruCache(30),
    private val routeCache: InMemoryLruCache<String, RouteCandidate> = InMemoryLruCache(30),
) {

    suspend fun autocomplete(query: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        suggestionCache.get(query)?.let { return@withContext it }
        return@withContext try {
            val response = photonApi.search(query = query, limit = 8)
            val suggestions = response.features.mapNotNull { it.toSuggestion() }
            suggestionCache.put(query, suggestions)
            suggestions
        } catch (http: HttpException) {
            Log.e(TAG, "Photon http error", http)
            emptyList()
        } catch (t: Throwable) {
            Log.e(TAG, "Photon request failed", t)
            emptyList()
        }
    }

    suspend fun findBestRoute(
        start: PlaceLocation,
        stopoverQuery: String,
        destination: PlaceLocation,
        travelMode: TravelMode,
        anchor: LatLng?,
        radiusMeters: Int = 15_000
    ): CandidateResult? {
        val candidates = findStopovers(stopoverQuery, anchor, radiusMeters)
        if (candidates.isEmpty()) return null
        val evaluated = candidates.mapNotNull { candidate ->
            val route = requestRoute(start, candidate, destination, travelMode) ?: return@mapNotNull null
            CandidateResult(candidate, route)
        }
        return selectBestCandidate(evaluated)
    }

    private suspend fun findStopovers(
        query: String,
        anchor: LatLng?,
        radiusMeters: Int
    ): List<PlaceLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cacheKey = buildString {
            append(query.lowercase(Locale.getDefault()))
            anchor?.let { append("|${it.latitude},${it.longitude}") }
            append("|$radiusMeters")
        }
        stopoverCache.get(cacheKey)?.let { return@withContext it }
        val data = buildOverpassQuery(query, anchor, radiusMeters)
        return@withContext try {
            val response = overpassApi.query(data)
            val locations = response.elements
                .asSequence()
                .mapNotNull { element -> element.toPlaceLocation(query) }
                .distinctBy { it.id }
                .take(15)
                .toList()
            stopoverCache.put(cacheKey, locations)
            locations
        } catch (http: HttpException) {
            Log.e(TAG, "Overpass http error", http)
            emptyList()
        } catch (t: Throwable) {
            Log.e(TAG, "Overpass request failed", t)
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
            start.id,
            waypoint.id,
            destination.id,
            travelMode.osrmProfile
        ).joinToString(separator = "|")
        routeCache.get(cacheKey)?.let { return@withContext it }
        val coordinateString = listOf(start.latLng, waypoint.latLng, destination.latLng)
            .joinToString(separator = ";") { "${it.longitude},${it.latitude}" }
        val response = try {
            osrmApi.route(
                profile = travelMode.osrmProfile,
                coordinates = coordinateString,
                overview = "full",
                geometries = "polyline",
                steps = false
            )
        } catch (http: HttpException) {
            Log.e(TAG, "OSRM http error", http)
            return@withContext null
        } catch (t: Throwable) {
            Log.e(TAG, "OSRM request failed", t)
            return@withContext null
        }
        val candidate = response.toRouteCandidate(start, waypoint, destination)
        if (candidate != null) {
            routeCache.put(cacheKey, candidate)
        }
        candidate
    }
}

private fun PhotonFeature.toSuggestion(): PlaceSuggestion? {
    val properties = properties ?: return null
    val geometry = geometry ?: return null
    if (geometry.coordinates.size < 2) return null
    val name = properties.name ?: return null
    val latLng = LatLng(
        latitude = geometry.coordinates[1],
        longitude = geometry.coordinates[0]
    )
    val street = properties.street
    val city = properties.city
    val postcode = properties.postcode
    val suffixParts = listOfNotNull(street, city, postcode)
    val label = if (suffixParts.isEmpty()) name else "$name, ${suffixParts.joinToString(", ")}"
    val addressParts = listOfNotNull(properties.street, properties.city, properties.state, properties.country)
    val address = if (addressParts.isEmpty()) null else addressParts.joinToString(", ")
    val id = listOfNotNull(properties.osmType, properties.osmId?.toString()).joinToString(":")
    return PlaceSuggestion(
        id = id.ifEmpty { "${latLng.latitude},${latLng.longitude}" },
        description = label,
        address = address,
        latLng = latLng
    )
}

private fun buildOverpassQuery(query: String, anchor: LatLng?, radiusMeters: Int): String {
    val escaped = query.replace("\"", "\\\"")
    val filters = listOf("name", "shop", "amenity")
    val area = anchor?.let { "around:$radiusMeters,${it.latitude},${it.longitude}" }
    val builder = StringBuilder()
    builder.append("[out:json][timeout:25];(")
    for (key in filters) {
        if (area != null) {
            builder.append("node($area)[\"$key\"~\"$escaped\",i];")
            builder.append("way($area)[\"$key\"~\"$escaped\",i];")
            builder.append("relation($area)[\"$key\"~\"$escaped\",i];")
        } else {
            builder.append("node[\"$key\"~\"$escaped\",i];")
            builder.append("way[\"$key\"~\"$escaped\",i];")
            builder.append("relation[\"$key\"~\"$escaped\",i];")
        }
    }
    builder.append(");out center 20;")
    return builder.toString()
}

private fun OverpassElement.toPlaceLocation(fallbackQuery: String): PlaceLocation? {
    val latLon = when {
        lat != null && lon != null -> LatLng(lat, lon)
        center?.lat != null && center.lon != null -> LatLng(center.lat, center.lon)
        else -> null
    } ?: return null
    val name = tags["name"] ?: fallbackQuery
    val addressParts = listOfNotNull(
        tags["addr:street"],
        tags["addr:housenumber"],
        tags["addr:postcode"],
        tags["addr:city"],
        tags["addr:country"]
    )
    val address = if (addressParts.isEmpty()) null else addressParts.joinToString(", ")
    val identifier = buildString {
        type?.let { append(it) }
        append(":")
        append(id ?: latLon.hashCode())
    }
    return PlaceLocation(
        id = identifier,
        name = name,
        address = address,
        latLng = latLon
    )
}

private fun OsrmResponse.toRouteCandidate(
    start: PlaceLocation,
    waypoint: PlaceLocation,
    destination: PlaceLocation
): RouteCandidate? {
    if (code != "Ok") return null
    val firstRoute = routes.firstOrNull() ?: return null
    val legs = firstRoute.legs
    if (legs.size < 2) return null
    val legInfos = legs.take(2).mapIndexed { index, leg ->
        val distance = leg.distance ?: return null
        val duration = leg.duration ?: return null
        val endpoints = when (index) {
            0 -> start.latLng to waypoint.latLng
            else -> waypoint.latLng to destination.latLng
        }
        RouteLeg(
            start = endpoints.first,
            end = endpoints.second,
            distanceMeters = distance.toLong(),
            distanceText = formatDistance(distance.toLong()),
            durationSeconds = duration.toLong(),
            durationText = formatDuration(duration.toLong())
        )
    }
    val totalDistance = firstRoute.distance?.toLong() ?: legInfos.sumOf { it.distanceMeters }
    val totalDuration = firstRoute.duration?.toLong() ?: legInfos.sumOf { it.durationSeconds }
    return RouteCandidate(
        overviewPolyline = firstRoute.geometry,
        totalDistanceMeters = totalDistance,
        totalDistanceText = formatDistance(totalDistance),
        totalDurationSeconds = totalDuration,
        totalDurationText = formatDuration(totalDuration),
        legs = legInfos
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
