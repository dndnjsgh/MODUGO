package com.example.modugo.sdk

import com.example.modugo.domain.*
import kotlinx.coroutines.flow.StateFlow

enum class SourceConnection { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

/** App-owned port, not a Pleos SDK type. Real adapters will own listener registration and release. */
interface NavigationSource {
    val connection: StateFlow<SourceConnection>
    val journey: StateFlow<Journey?>
    fun start()
    fun stop()
    suspend fun requestWaypoint(place: RestPlace): ActionResult
}

/** Read only: unavailable or unauthorized values must remain null; there are no vehicle controls. */
interface VehicleStateSource {
    val connection: StateFlow<SourceConnection>
    val vehicle: StateFlow<VehicleState?>
    fun start()
    fun stop()
}
