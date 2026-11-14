package com.example.modugo.mock

import com.example.modugo.data.MobilityRepository
import com.example.modugo.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockMobilityRepository : MobilityRepository {
    private val mutableState = MutableStateFlow(MobilitySnapshot())
    override val state = mutableState.asStateFlow()
    override val destinations = listOf(
        DestinationCandidate("hospital-a", "병원 A", 5.0, 12, AccessibilityStatus.UNMET,
            "정문에 계단 · 무단차 출입구 없음", "일반 주차장 · 접근 가능한 하차 위치 미확인"),
        DestinationCandidate("hospital-b", "병원 B", 6.0, 15, AccessibilityStatus.CONFIRMED,
            "동쪽 무단차 출입구", "동쪽 출입구 앞 하차 구역 · 지하 1층 장애인 주차구역"),
        DestinationCandidate("hospital-c", "병원 C", 4.0, 10, AccessibilityStatus.UNKNOWN,
            "정보 갱신 필요 · 이용 가능 여부 미확인", "주차·하차 위치 정보 미확인"),
    )
    override val restPlaces = listOf(
        RestPlace("rest-1", "모두쉼 휴게소", "휴게소", 3.2, "모든 시설 상태는 가상 설정입니다.", listOf("입구 계단", "일반 화장실"),
            AccessibilityStatus.UNMET, AccessibilityStatus.UNMET, true, 4, 1),
        RestPlace("rest-2", "푸른뜰 쉼터", "공원 쉼터", 5.8, "모든 시설 상태는 가상 설정입니다.", listOf("무단차 출입구", "장애인 화장실", "휴식 벤치"),
            AccessibilityStatus.CONFIRMED, AccessibilityStatus.CONFIRMED, true, 7, 3),
        RestPlace("rest-3", "온유 라운지", "편의시설", 8.1, "출입구 및 화장실 정보가 미확인인 가상 장소입니다.", listOf("실내 좌석 예시 · 그 외 시설 미확인"),
            AccessibilityStatus.UNKNOWN, AccessibilityStatus.UNKNOWN, false, 12, 10),
        RestPlace("rest-4", "가까운 쉼터", "쉼터", 2.0, "무단차 출입구는 있으나 장애인 화장실은 없는 가상 장소입니다.", listOf("무단차 출입구", "휴식 벤치"),
            AccessibilityStatus.CONFIRMED, AccessibilityStatus.UNMET, true, 3, 1),
    )

    override fun start() = Unit
    override fun stop() = Unit

    fun setScenario(scenario: DemoScenario, restIntervalMinutes: Int = 60) {
        val interval = restIntervalMinutes.coerceIn(30, 120)
        val previous = state.value
        val reset = scenario == DemoScenario.PARKED || scenario == DemoScenario.HOSPITAL_COMPARISON || scenario == DemoScenario.CONNECTION_FAILED
        val selected = previous.selectedDestination.takeUnless { reset }
        val journey = when (scenario) {
            DemoScenario.HOSPITAL_COMPARISON -> null
            DemoScenario.PARKED -> Journey()
            DemoScenario.STARTED -> if (selected != null) Journey(selected.name, selected.distanceKm, selected.minutes)
                else Journey(elapsedMinutes = 5, remainingDistanceKm = 38.0, remainingMinutes = 45, distanceTravelledKm = 4.0)
            DemoScenario.LONG_DRIVE -> Journey(elapsedMinutes = 45, remainingDistanceKm = 25.0, remainingMinutes = 35, distanceTravelledKm = 44.0)
            DemoScenario.REST_DUE -> Journey(elapsedMinutes = interval, remainingDistanceKm = 16.0, remainingMinutes = 20, distanceTravelledKm = interval * 0.8)
            DemoScenario.APPROACHING -> Journey(elapsedMinutes = 76, remainingDistanceKm = 1.2, remainingMinutes = 4, distanceTravelledKm = 67.0)
            DemoScenario.ARRIVED -> Journey(elapsedMinutes = 80, remainingDistanceKm = 0.0, remainingMinutes = 0, distanceTravelledKm = 68.2, arrived = true)
            DemoScenario.CONNECTION_FAILED -> null
        }
        val failed = scenario == DemoScenario.CONNECTION_FAILED
        val parked = scenario == DemoScenario.PARKED || scenario == DemoScenario.ARRIVED || scenario == DemoScenario.HOSPITAL_COMPARISON
        mutableState.value = MobilitySnapshot(
            scenario = scenario,
            connection = if (failed) ConnectionStatus.UNAVAILABLE else ConnectionStatus.MOCK_READY,
            origin = if (failed) DataOrigin.UNAVAILABLE else DataOrigin.DEMO,
            journey = journey?.let { if (selected != null) it.copy(destination = selected.name) else it },
            vehicle = if (failed) null else VehicleState(motion = if (parked) MotionState.PARKED else MotionState.DRIVING),
            minutesSinceRest = journey?.elapsedMinutes ?: 0,
            selectedDestination = selected,
            routeOption = if (reset) RoutePreference.RECOMMENDED else previous.routeOption,
            waypoint = previous.waypoint.takeUnless { reset },
        )
    }

    fun advanceMinutes(minutes: Int) {
        if (minutes <= 0) return
        mutableState.update { current ->
            val journey = current.journey
            if (!current.isDriving || journey == null || journey.arrived) return@update current
            val added = minutes.coerceAtMost(journey.remainingMinutes)
            val remaining = (journey.remainingMinutes - added).coerceAtLeast(0)
            // 시연용 거리는 남은 시간에 비례해 감소시키며 실제 경로를 계산하지 않는다.
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
        if (state.value.journey == null || state.value.journey?.arrived == true) return ActionResult(false, "이동할 목적지를 먼저 선택해 주세요.")
        val place = restPlaces.find { it.id == placeId } ?: return ActionResult(false, "샘플 장소를 찾을 수 없어요.")
        mutableState.update { it.copy(waypoint = place) }
        return ActionResult(true, "${place.name}을 데모 경유지로 선택했어요. 실제 내비게이션에는 전송하지 않았어요.")
    }

    override suspend fun requestDestination(destinationId: String): ActionResult {
        if (!state.value.canEdit) return ActionResult(false, "정차 후 목적지를 선택해 주세요.")
        val destination = destinations.find { it.id == destinationId } ?: return ActionResult(false, "가상 목적지를 찾을 수 없어요.")
        mutableState.update { it.copy(
            journey = Journey(destination.name, destination.distanceKm, destination.minutes),
            selectedDestination = destination, waypoint = null, minutesSinceRest = 0,
            routeOption = RoutePreference.RECOMMENDED,
        ) }
        return ActionResult(true, "${destination.name}을 데모 목적지로 선택했어요. 실제 경로는 변경하지 않았어요.")
    }

    override suspend fun requestRouteOption(option: RoutePreference): ActionResult {
        if (!state.value.canEdit || state.value.journey == null || state.value.journey?.arrived == true) {
            return ActionResult(false, "정차 중 이동할 목적지가 있을 때만 경로 옵션을 선택할 수 있어요.")
        }
        mutableState.update { it.copy(routeOption = option) }
        return ActionResult(true, "${option.label}를 데모에 적용했어요. 실제 경로·예상 시간·승차감을 계산한 결과가 아니에요.")
    }
}
