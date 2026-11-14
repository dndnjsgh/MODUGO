package com.example.modugo.data

import com.example.modugo.domain.*
import kotlinx.coroutines.flow.StateFlow

/** 데이터 제공부는 조회 실패 시 오래된 값을 제거한다. */
interface MobilityRepository {
    val state: StateFlow<MobilitySnapshot>
    val restPlaces: List<RestPlace>
    val destinations: List<DestinationCandidate> get() = emptyList()
    fun start()
    fun stop()
    fun completeRest(): ActionResult
    suspend fun requestWaypoint(placeId: String): ActionResult
    suspend fun requestDestination(destinationId: String): ActionResult =
        ActionResult(false, "목적지 변경은 Mock에서만 지원해요. 실제 API 승인 범위 확인이 필요해요.")
    suspend fun requestRouteOption(option: RoutePreference): ActionResult =
        ActionResult(false, "경로 옵션 변경은 Mock에서만 지원해요. 실제 API 승인 범위 확인이 필요해요.")
}

interface VoiceCompanion {
    fun respond(command: VoiceCommand, snapshot: MobilitySnapshot, preferences: CarePreferences): VoiceReply
}
