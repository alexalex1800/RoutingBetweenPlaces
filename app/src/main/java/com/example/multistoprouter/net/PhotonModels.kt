package com.example.multistoprouter.net

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PhotonFeatureCollection(
    @Json(name = "features") val features: List<PhotonFeature> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PhotonFeature(
    @Json(name = "properties") val properties: PhotonProperties?,
    @Json(name = "geometry") val geometry: PhotonGeometry?
)

@JsonClass(generateAdapter = true)
data class PhotonProperties(
    @Json(name = "name") val name: String?,
    @Json(name = "street") val street: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "postcode") val postcode: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "osm_id") val osmId: Long?,
    @Json(name = "osm_type") val osmType: String?,
    @Json(name = "state") val state: String?
)

@JsonClass(generateAdapter = true)
data class PhotonGeometry(
    @Json(name = "coordinates") val coordinates: List<Double> = emptyList()
)
