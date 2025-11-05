package com.example.multistoprouter.repository

import com.example.multistoprouter.cache.SimpleLruCache
import com.example.multistoprouter.model.PlaceDetails
import com.example.multistoprouter.model.PlaceSuggestion
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PlacesRepository(
    private val placesClient: PlacesClient,
    private val suggestionCache: SimpleLruCache<String, List<PlaceSuggestion>> = SimpleLruCache(32),
    private val detailsCache: SimpleLruCache<String, PlaceDetails> = SimpleLruCache(32)
) {
    suspend fun autocomplete(
        query: String,
        token: AutocompleteSessionToken?,
        origin: LatLng? = null,
        radiusMeters: Int? = null
    ): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cacheKey = listOfNotNull("auto", query.trim(), origin?.toKey(), radiusMeters?.toString()).joinToString(":")
        suggestionCache.get(cacheKey)?.let { return@withContext it }

        val requestBuilder = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
        token?.let { requestBuilder.setSessionToken(it) }
        origin?.let { requestBuilder.setOrigin(it) }
        if (origin != null && radiusMeters != null) {
            requestBuilder.setLocationBias(createBounds(origin, radiusMeters))
        }

        val predictions = placesClient.findAutocompletePredictions(requestBuilder.build()).await().autocompletePredictions
        val suggestions = predictions.map {
            PlaceSuggestion(
                placeId = it.placeId,
                primaryText = it.getPrimaryText(null).toString(),
                secondaryText = it.getSecondaryText(null)?.toString()
            )
        }
        suggestionCache.put(cacheKey, suggestions)
        return@withContext suggestions
    }

    suspend fun fetchDetails(placeId: String): PlaceDetails? = withContext(Dispatchers.IO) {
        detailsCache.get(placeId)?.let { return@withContext it }
        val request = FetchPlaceRequest.newInstance(
            placeId,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        )
        val response = runCatching { placesClient.fetchPlace(request).await() }.getOrNull() ?: return@withContext null
        val place = response.place
        val latLng = place.latLng ?: return@withContext null
        return@withContext PlaceDetails(
            placeId = place.id.orEmpty(),
            name = place.name.orEmpty(),
            address = place.address,
            latitude = latLng.latitude,
            longitude = latLng.longitude
        ).also { detailsCache.put(placeId, it) }
    }

    suspend fun findCandidates(
        query: String,
        origin: LatLng? = null,
        radiusMeters: Int = DEFAULT_RADIUS_METERS,
        limit: Int = DEFAULT_CANDIDATE_LIMIT
    ): List<PlaceDetails> {
        val token = AutocompleteSessionToken.newInstance()
        val predictions = autocomplete(query, token, origin, radiusMeters)
        if (predictions.isEmpty()) return emptyList()
        val details = mutableListOf<PlaceDetails>()
        for (prediction in predictions.take(limit)) {
            fetchDetails(prediction.placeId)?.let { details.add(it) }
        }
        return details
    }

    private fun LatLng.toKey(): String = "${latitude},${longitude}"

    private fun createBounds(center: LatLng, radiusMeters: Int): RectangularBounds {
        val latDistance = radiusMeters / METERS_PER_DEGREE_LAT
        val lngDistance = radiusMeters / (METERS_PER_DEGREE_LAT * kotlin.math.cos(Math.toRadians(center.latitude)))
        val southWest = LatLng(center.latitude - latDistance, center.longitude - lngDistance)
        val northEast = LatLng(center.latitude + latDistance, center.longitude + lngDistance)
        return RectangularBounds.newInstance(southWest, northEast)
    }

    companion object {
        const val DEFAULT_RADIUS_METERS = 10_000
        const val DEFAULT_CANDIDATE_LIMIT = 10
        private const val METERS_PER_DEGREE_LAT = 111_320.0
    }
}
