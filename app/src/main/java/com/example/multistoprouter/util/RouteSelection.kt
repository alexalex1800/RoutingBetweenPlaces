package com.example.multistoprouter.util

import com.example.multistoprouter.model.RouteCandidate

fun selectBestRoute(candidates: List<RouteCandidate>): RouteCandidate? {
    return candidates.minWithOrNull { a, b ->
        when {
            a.route.durationSeconds != b.route.durationSeconds ->
                a.route.durationSeconds.compareTo(b.route.durationSeconds)
            else -> a.route.distanceMeters.compareTo(b.route.distanceMeters)
        }
    }
}
