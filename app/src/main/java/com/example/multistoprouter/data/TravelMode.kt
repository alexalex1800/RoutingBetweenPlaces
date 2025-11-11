package com.example.multistoprouter.data

enum class TravelMode(val osrmProfile: String, val displayName: String) {
    DRIVING("driving", "Auto"),
    CYCLING("cycling", "Fahrrad"),
    WALKING("walking", "Zu Fuß");

    companion object {
        fun fromIndex(index: Int): TravelMode = entries.getOrElse(index) { DRIVING }
    }
}
