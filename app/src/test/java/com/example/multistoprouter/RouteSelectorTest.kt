package com.example.multistoprouter

import com.example.multistoprouter.data.CandidateResult
import com.example.multistoprouter.data.PlaceLocation
import com.example.multistoprouter.data.RouteCandidate
import com.example.multistoprouter.data.RouteLeg
import com.example.multistoprouter.data.selectBestCandidate
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteSelectorTest {

    @Test
    fun `selectBestCandidate returns null for empty list`() {
        assertNull(selectBestCandidate(emptyList()))
    }

    @Test
    fun `selectBestCandidate prefers shorter duration`() {
        val candidate1 = candidate(durationSeconds = 1200, distanceMeters = 5000)
        val candidate2 = candidate(durationSeconds = 900, distanceMeters = 8000)
        val result = selectBestCandidate(listOf(candidate1, candidate2))
        assertEquals(candidate2, result)
    }

    @Test
    fun `selectBestCandidate breaks tie with distance`() {
        val candidate1 = candidate(durationSeconds = 900, distanceMeters = 6000)
        val candidate2 = candidate(durationSeconds = 900, distanceMeters = 5000)
        val result = selectBestCandidate(listOf(candidate1, candidate2))
        assertEquals(candidate2, result)
    }

    private fun candidate(durationSeconds: Long, distanceMeters: Long): CandidateResult {
        val location = PlaceLocation(
            placeId = "test",
            name = "Test",
            address = null,
            latLng = LatLng(0.0, 0.0)
        )
        val route = RouteCandidate(
            overviewPolyline = null,
            totalDistanceMeters = distanceMeters,
            totalDistanceText = "$distanceMeters m",
            totalDurationSeconds = durationSeconds,
            totalDurationText = "$durationSeconds s",
            legs = listOf(
                RouteLeg(
                    start = LatLng(0.0, 0.0),
                    end = LatLng(1.0, 1.0),
                    distanceMeters = distanceMeters,
                    distanceText = "$distanceMeters m",
                    durationSeconds = durationSeconds,
                    durationText = "$durationSeconds s"
                )
            )
        )
        return CandidateResult(location, route)
    }
}
