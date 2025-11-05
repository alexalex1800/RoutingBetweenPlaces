package com.example.multistoprouter

import com.example.multistoprouter.model.PlaceDetails
import com.example.multistoprouter.model.RouteCandidate
import com.example.multistoprouter.model.RouteSummary
import com.example.multistoprouter.util.selectBestRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteSelectionTest {

    @Test
    fun `selectBestRoute chooses lowest duration`() {
        val candidates = listOf(
            RouteCandidate(dummyPlace("1"), dummyRoute(duration = 600, distance = 4000)),
            RouteCandidate(dummyPlace("2"), dummyRoute(duration = 300, distance = 8000)),
            RouteCandidate(dummyPlace("3"), dummyRoute(duration = 900, distance = 2000))
        )

        val best = selectBestRoute(candidates)

        assertEquals("2", best?.candidate?.placeId)
    }

    @Test
    fun `selectBestRoute falls back to distance`() {
        val candidates = listOf(
            RouteCandidate(dummyPlace("1"), dummyRoute(duration = 600, distance = 5000)),
            RouteCandidate(dummyPlace("2"), dummyRoute(duration = 600, distance = 3000))
        )

        val best = selectBestRoute(candidates)

        assertEquals("2", best?.candidate?.placeId)
    }

    @Test
    fun `selectBestRoute handles empty list`() {
        assertNull(selectBestRoute(emptyList()))
    }

    private fun dummyPlace(id: String) = PlaceDetails(
        placeId = id,
        name = "Place $id",
        address = "",
        latitude = 0.0,
        longitude = 0.0
    )

    private fun dummyRoute(duration: Long, distance: Long) = RouteSummary(
        durationSeconds = duration,
        durationText = "$duration s",
        distanceMeters = distance,
        distanceText = "$distance m",
        polyline = null,
        start = null,
        via = null,
        destination = null
    )
}
