package com.example.multistoprouter.net

import com.example.multistoprouter.data.LatLng

fun decodePolyline(polyline: String): List<LatLng> {
    val coordinates = mutableListOf<LatLng>()
    var index = 0
    val length = polyline.length
    var lat = 0
    var lng = 0

    while (index < length) {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = polyline[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20 && index < length)
        val deltaLat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lat += deltaLat

        result = 0
        shift = 0
        do {
            b = polyline[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20 && index < length)
        val deltaLng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lng += deltaLng

        coordinates += LatLng(lat / 1E5, lng / 1E5)
    }

    return coordinates
}
