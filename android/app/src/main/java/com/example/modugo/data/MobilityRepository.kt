package com.example.modugo.data

import com.example.modugo.domain.*
import kotlinx.coroutines.flow.StateFlow

/** App-owned contract; implementations must clear unavailable readings instead of reusing stale data. */
interface MobilityRepository {
    val state: StateFlow<MobilitySnapshot>
    val restPlaces: List<RestPlace>
    fun start()
    fun stop()
    fun completeRest(): ActionResult
    suspend fun requestWaypoint(placeId: String): ActionResult
}

interface VoiceCompanion {
    fun respond(command: VoiceCommand, snapshot: MobilitySnapshot, preferences: CarePreferences): VoiceReply
}
