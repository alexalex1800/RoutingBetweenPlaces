package com.example.multistoprouter.data

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

fun LatLng.midpoint(other: LatLng): LatLng = LatLng(
    latitude = (latitude + other.latitude) / 2.0,
    longitude = (longitude + other.longitude) / 2.0
)
