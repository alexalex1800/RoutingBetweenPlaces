package com.example.multistoprouter.net

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    @Json(name = "elements") val elements: List<OverpassElement> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    @Json(name = "type") val type: String?,
    @Json(name = "id") val id: Long?,
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?,
    @Json(name = "center") val center: OverpassCenter?,
    @Json(name = "tags") val tags: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class OverpassCenter(
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?
)
