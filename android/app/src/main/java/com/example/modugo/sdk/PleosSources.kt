package com.example.modugo.sdk

import com.example.modugo.domain.*
import kotlinx.coroutines.flow.StateFlow

enum class SourceConnection { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

/** 내비게이션 연결부가 리스너 등록과 해제를 담당한다. */
interface NavigationSource {
    val connection: StateFlow<SourceConnection>
    val journey: StateFlow<Journey?>
    fun start()
    fun stop()
    suspend fun requestWaypoint(place: RestPlace): ActionResult
}

/** 조회 미지원 또는 권한이 없는 항목은 null로 유지한다. */
interface VehicleStateSource {
    val connection: StateFlow<SourceConnection>
    val vehicle: StateFlow<VehicleState?>
    fun start()
    fun stop()
}
