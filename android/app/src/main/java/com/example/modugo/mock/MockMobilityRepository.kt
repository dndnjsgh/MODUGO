package com.example.modugo.mock

import com.example.modugo.data.MobilityRepository
import com.example.modugo.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockMobilityRepository : MobilityRepository {
    private val mutableState = MutableStateFlow(MobilitySnapshot())
    override val state = mutableState.asStateFlow()
    override val restPlaces = listOf(
        RestPlace("rest-1", "모두쉼 휴게소", "휴게소", 3.2, "이동 중 잠시 쉬어 가는 공간을 가정한 데모 장소입니다.", listOf("무단차 출입구", "장애인 화장실", "실내 휴식 공간")),
        RestPlace("rest-2", "푸른뜰 쉼터", "공원 쉼터", 5.8, "가벼운 휴식 공간을 가정한 데모 장소입니다.", listOf("휴식 벤치", "평탄한 보행 구역", "화장실")),
        RestPlace("rest-3", "온유 라운지", "편의시설", 8.1, "실내 휴게 공간을 가정한 데모 장소입니다.", listOf("엘리베이터", "실내 좌석", "장애인 주차구역")),
    )

    override fun start() = Unit
    override fun stop() = Unit

    fun setScenario(scenario: DemoScenario, restIntervalMinutes: Int = 60) {
        val interval = restIntervalMinutes.coerceIn(30, 120)
        val journey = when (scenario) {
            DemoScenario.PARKED -> Journey()
            DemoScenario.STARTED -> Journey(elapsedMinutes = 5, remainingDistanceKm = 38.0, remainingMinutes = 45, distanceTravelledKm = 4.0)
            DemoScenario.LONG_DRIVE -> Journey(elapsedMinutes = 45, remainingDistanceKm = 25.0, remainingMinutes = 35, distanceTravelledKm = 44.0)
            DemoScenario.REST_DUE -> Journey(elapsedMinutes = interval, remainingDistanceKm = 16.0, remainingMinutes = 20, distanceTravelledKm = interval * 0.8)
            DemoScenario.APPROACHING -> Journey(elapsedMinutes = 76, remainingDistanceKm = 1.2, remainingMinutes = 4, distanceTravelledKm = 67.0)
            DemoScenario.ARRIVED -> Journey(elapsedMinutes = 80, remainingDistanceKm = 0.0, remainingMinutes = 0, distanceTravelledKm = 68.2, arrived = true)
            DemoScenario.CONNECTION_FAILED -> null
        }
        val failed = scenario == DemoScenario.CONNECTION_FAILED
        val parked = scenario == DemoScenario.PARKED || scenario == DemoScenario.ARRIVED
        mutableState.value = MobilitySnapshot(
            scenario = scenario,
            connection = if (failed) ConnectionStatus.UNAVAILABLE else ConnectionStatus.MOCK_READY,
            origin = if (failed) DataOrigin.UNAVAILABLE else DataOrigin.DEMO,
            journey = journey,
            vehicle = if (failed) null else VehicleState(motion = if (parked) MotionState.PARKED else MotionState.DRIVING),
            minutesSinceRest = journey?.elapsedMinutes ?: 0,
        )
    }

    fun advanceMinutes(minutes: Int) {
        if (minutes <= 0) return
        mutableState.update { current ->
            val journey = current.journey
            if (!current.isDriving || journey == null || journey.arrived) return@update current
            val added = minutes.coerceAtMost(journey.remainingMinutes)
            val remaining = (journey.remainingMinutes - added).coerceAtLeast(0)
            // shortcut: demo distance shrinks linearly; use validated route updates when a real source is added.
            val distance = if (journey.remainingMinutes > 0) journey.remainingDistanceKm * remaining / journey.remainingMinutes else 0.0
            current.copy(
                journey = journey.copy(
                    elapsedMinutes = journey.elapsedMinutes + added,
                    remainingMinutes = remaining,
                    remainingDistanceKm = distance,
                    distanceTravelledKm = journey.distanceTravelledKm + journey.remainingDistanceKm - distance,
                    arrived = remaining == 0,
                ),
                minutesSinceRest = current.minutesSinceRest + added,
            )
        }
    }

    fun park() {
        mutableState.update { current ->
            val vehicle = current.vehicle ?: return@update current
            current.copy(vehicle = vehicle.copy(motion = MotionState.PARKED))
        }
    }

    override fun completeRest(): ActionResult {
        if (!state.value.canEdit) return ActionResult(false, "정차 상황에서 휴식 완료를 기록해 주세요.")
        mutableState.update { it.copy(minutesSinceRest = 0) }
        return ActionResult(true, "휴식을 기록했어요. 다음 추천 시간을 다시 계산해요.")
    }

    override suspend fun requestWaypoint(placeId: String): ActionResult {
        if (!state.value.canEdit) return ActionResult(false, "정차 상황에서 샘플 경유지를 선택해 주세요.")
        val place = restPlaces.find { it.id == placeId } ?: return ActionResult(false, "샘플 장소를 찾을 수 없어요.")
        mutableState.update { it.copy(waypoint = place) }
        return ActionResult(true, "${place.name}을 데모 경유지로 선택했어요. 실제 내비게이션에는 전송하지 않았어요.")
    }
}
