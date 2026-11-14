package com.example.modugo.domain

enum class CareFeature { REST, JOURNEY, VOICE, ARRIVAL, COMFORT }

enum class DemoScenario(val label: String) {
    HOSPITAL_COMPARISON("병원 A/B 비교 · 대표 시연"),
    PARKED("정차 중"), STARTED("주행 시작"), LONG_DRIVE("장시간 이동"),
    REST_DUE("휴식 추천"), APPROACHING("목적지 접근"), ARRIVED("목적지 도착"),
    CONNECTION_FAILED("SDK 연결 실패")
}

enum class MotionState { PARKED, DRIVING, UNKNOWN }
enum class ConnectionStatus { MOCK_READY, REAL_CONNECTED, UNAVAILABLE }
enum class DataOrigin(val label: String) { DEMO("데모 데이터"), REAL("실제 SDK 데이터"), UNAVAILABLE("정보 없음") }

data class Journey(
    val destination: String = "모두누리 문화센터",
    val remainingDistanceKm: Double = 42.0,
    val remainingMinutes: Int = 50,
    val elapsedMinutes: Int = 0,
    val distanceTravelledKm: Double = 0.0,
    val arrived: Boolean = false,
)

data class VehicleState(
    val motion: MotionState = MotionState.PARKED,
    val cabinTemperatureC: Int? = 24,
    val hvacOn: Boolean? = true,
)

data class RestPlace(
    val id: String,
    val name: String,
    val type: String,
    val distanceKm: Double,
    val description: String,
    val facilities: List<String>,
    val entrance: AccessibilityStatus = AccessibilityStatus.UNKNOWN,
    val accessibleRestroom: AccessibilityStatus = AccessibilityStatus.UNKNOWN,
    val onRoute: Boolean = false,
    val travelMinutes: Int = 0,
    val extraMinutes: Int = 0,
)

data class MobilitySnapshot(
    val demo: Boolean = true,
    val uxAllowsInteraction: Boolean = false,
    val tripTimeAvailable: Boolean = demo,
    val tripDistanceAvailable: Boolean = demo,
    val diagnostics: String = "",
    val scenario: DemoScenario = DemoScenario.PARKED,
    val connection: ConnectionStatus = ConnectionStatus.MOCK_READY,
    val origin: DataOrigin = DataOrigin.DEMO,
    val journey: Journey? = Journey(),
    val vehicle: VehicleState? = VehicleState(),
    val waypoint: RestPlace? = null,
    val minutesSinceRest: Int = 0,
    val selectedDestination: DestinationCandidate? = null,
    val routeOption: RoutePreference = RoutePreference.RECOMMENDED,
) {
    val canEdit: Boolean get() = if (demo) connection != ConnectionStatus.UNAVAILABLE && vehicle?.motion == MotionState.PARKED else uxAllowsInteraction
    val isDriving: Boolean get() = vehicle?.motion == MotionState.DRIVING
}

data class CarePreferences(
    val careEnabled: Boolean = true,
    val motionCareEnabled: Boolean = false,
    val restIntervalMinutes: Int = 60,
    val preferredTemperatureC: Int = 22,
    val needsAccessibleEntrance: Boolean = true,
    val preferRestAmenities: Boolean = true,
    val preferComfortableRoute: Boolean = false,
    val preferVoiceGuidance: Boolean = false,
) {
    fun normalized() = copy(
        restIntervalMinutes = restIntervalMinutes.coerceIn(30, 120),
        preferredTemperatureC = preferredTemperatureC.coerceIn(18, 28),
    )
}

data class CareNotice(val title: String, val message: String, val feature: CareFeature)

data class CareEvaluation(
    val restDue: Boolean,
    val restInMinutes: Int?,
    val arrivalDue: Boolean,
    val journeyMessage: String,
    val comfortMessage: String,
    val primary: CareNotice,
)

data class ActionResult(val success: Boolean, val message: String)

enum class VoiceCommand(val label: String) {
    DESTINATIONS("병원 후보 비교해 줘"), COMFORTABLE_ROUTE("편안한 경로로 바꿔 줘"),
    REST("휴식 장소 추천해 줘"), REMAINING_TIME("남은 이동 시간 알려줘"),
    ARRIVAL("도착 정보 보여줘"), CARE_STATUS("이동 케어 상태 알려줘")
}

data class VoiceReply(val text: String, val feature: CareFeature? = null)
