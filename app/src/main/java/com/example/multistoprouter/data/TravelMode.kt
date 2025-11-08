package com.example.multistoprouter.data

enum class TravelMode(val displayName: String, val osrmProfile: String?) {
    WALKING("Fuß", "foot"),
    DRIVING("Auto", "driving"),
    BICYCLING("Fahrrad", "cycling"),
    TRANSIT("ÖPNV", null);

    val isSupported: Boolean
        get() = osrmProfile != null

    companion object {
        fun fromIndex(index: Int): TravelMode = entries.getOrElse(index) { DRIVING }
    }
}
