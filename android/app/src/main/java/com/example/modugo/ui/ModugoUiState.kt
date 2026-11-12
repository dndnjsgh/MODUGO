package com.example.modugo.ui

import com.example.modugo.domain.*

enum class Screen(val title: String, val subtitle: String) {
    HOME("나에게 맞는 이동", "가장 빠른 길보다, 나에게 맞는 길."),
    REST("잠시 쉬어 가요", "Smart Rest · 맞춤형 휴식"),
    JOURNEY("앞으로의 여정", "Predictive Journey · 이동 상황 안내"),
    VOICE("이동 동반자", "Voice Companion · 명령 버튼 데모"),
    ARRIVAL("도착까지 편안하게", "Arrival Care · 도착과 하차 준비"),
    COMFORT("편안한 이동 환경", "Comfort Care · 나의 쾌적한 이동"),
    SETTINGS("나의 이동 케어", "필요한 지원을 선택하세요.")
}

fun CareFeature.screen(): Screen = when (this) {
    CareFeature.REST -> Screen.REST
    CareFeature.JOURNEY -> Screen.JOURNEY
    CareFeature.VOICE -> Screen.VOICE
    CareFeature.ARRIVAL -> Screen.ARRIVAL
    CareFeature.COMFORT -> Screen.COMFORT
}

data class ModugoUiState(
    val snapshot: MobilitySnapshot,
    val preferences: CarePreferences,
    val evaluation: CareEvaluation,
    val restPlaces: List<RestPlace>,
    val screen: Screen = Screen.HOME,
    val selectedPlace: RestPlace? = null,
    val voiceResponse: String = "아래 명령 버튼을 선택하면 안내를 글로 보여드려요.",
    val checkedArrivalItems: Set<Int> = emptySet(),
    val arrivalAcknowledged: Boolean = false,
    val message: String? = null,
)

data class ModugoActions(
    val navigate: (Screen) -> Unit,
    val selectScenario: (DemoScenario) -> Unit,
    val advanceMinutes: () -> Unit,
    val park: () -> Unit,
    val completeRest: () -> Unit,
    val updatePreferences: (CarePreferences) -> Unit,
    val selectPlace: (RestPlace?) -> Unit,
    val addWaypoint: (RestPlace) -> Unit,
    val executeCommand: (VoiceCommand) -> Unit,
    val toggleArrivalItem: (Int) -> Unit,
    val acknowledgeArrival: () -> Unit,
    val dismissMessage: () -> Unit,
)
