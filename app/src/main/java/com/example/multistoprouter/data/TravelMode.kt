package com.example.multistoprouter.data

enum class TravelMode(val apiValue: String, val displayName: String) {
    WALKING("walking", "Fuß"),
    DRIVING("driving", "Auto"),
    BICYCLING("bicycling", "Fahrrad"),
    TRANSIT("transit", "ÖPNV");

    companion object {
        fun fromIndex(index: Int): TravelMode = entries.getOrElse(index) { DRIVING }
    }
}
