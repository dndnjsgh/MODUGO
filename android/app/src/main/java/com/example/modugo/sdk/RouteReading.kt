package com.example.modugo.sdk

import com.example.modugo.domain.Journey

object RouteReading {
    /** navi-data 3.0.4의 미터·초 단위를 변환하며, 0만으로 도착을 판정하지 않는다. */
    fun toJourney(name: String, meters: Int, seconds: Int): Journey? {
        if (name.isBlank() || meters < 0 || seconds < 0) return null
        return Journey(destination = name, remainingDistanceKm = meters / 1000.0,
            remainingMinutes = ((seconds.toLong() + 59) / 60).toInt(),
            elapsedMinutes = 0, distanceTravelledKm = 0.0, arrived = false)
    }
}
