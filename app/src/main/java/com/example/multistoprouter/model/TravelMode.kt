package com.example.multistoprouter.model

enum class TravelMode(val directionsValue: String) {
    DRIVING("driving"),
    WALKING("walking"),
    BICYCLING("bicycling"),
    TRANSIT("transit");

    companion object {
        val default = DRIVING
    }
}
