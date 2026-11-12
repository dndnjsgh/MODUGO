package com.example.modugo.sdk

import com.example.modugo.domain.Journey

object RouteReading {
    /** Official navi-data 3.0.4 uses meters and seconds; zero alone is not an arrival event. */
    fun toJourney(name: String, meters: Int, seconds: Int): Journey? {
        if (name.isBlank() || meters < 0 || seconds < 0) return null
        return Journey(destination = name, remainingDistanceKm = meters / 1000.0,
            remainingMinutes = ((seconds.toLong() + 59) / 60).toInt(),
            elapsedMinutes = 0, distanceTravelledKm = 0.0, arrived = false)
    }
}
