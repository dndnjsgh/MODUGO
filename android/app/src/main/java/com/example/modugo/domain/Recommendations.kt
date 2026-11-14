package com.example.modugo.domain

enum class AccessibilityStatus(val label: String) {
    CONFIRMED("확인됨"), UNMET("조건 미충족"), UNKNOWN("정보 미확인 · 갱신 필요")
}

enum class RoutePreference(val label: String) {
    RECOMMENDED("기본 추천 경로"), COMFORTABLE("편안한 경로")
}

data class DestinationCandidate(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val minutes: Int,
    val entrance: AccessibilityStatus,
    val entranceInfo: String,
    val parkingInfo: String,
)

data class PlaceRecommendation<T>(
    val place: T,
    val requiredStatus: AccessibilityStatus?,
    val reason: String,
) {
    val recommended: Boolean get() = requiredStatus == null || requiredStatus == AccessibilityStatus.CONFIRMED
    val selectable: Boolean get() = requiredStatus != AccessibilityStatus.UNMET
}

/** 목적지·경유지·경로 옵션 요청. 실제 SDK 어댑터는 변경 요청을 거절한다. */
sealed interface NavigationRequest {
    data class Destination(val id: String) : NavigationRequest
    data class Waypoint(val id: String) : NavigationRequest
    data class RouteOption(val option: RoutePreference) : NavigationRequest
}

data class NavigationConfirmation(
    val request: NavigationRequest,
    val title: String,
    val detail: String,
    val routeContext: Journey? = null,
    val preferences: CarePreferences = CarePreferences(),
)

object RecommendationEngine {
    private fun rank(status: AccessibilityStatus?) = when (status) {
        null, AccessibilityStatus.CONFIRMED -> 0
        AccessibilityStatus.UNKNOWN -> 1
        AccessibilityStatus.UNMET -> 2
    }

    private fun reason(status: AccessibilityStatus?) = when (status) {
        null -> "필수 출입구 조건을 선택하지 않아 이동 부담을 비교해요. 접근성 확인을 뜻하지 않아요."
        AccessibilityStatus.CONFIRMED -> "필수 무단차 출입구 조건을 충족한 것으로 설정된 가상 후보예요."
        AccessibilityStatus.UNMET -> "필수 출입구 조건 미충족으로 우선 추천과 선택에서 제외해요."
        AccessibilityStatus.UNKNOWN -> "필수 출입구 정보가 미확인이라 우선 추천하지 않아요. 이용 가능 여부는 별도 확인이 필요해요."
    }

    fun destinations(candidates: List<DestinationCandidate>, prefs: CarePreferences): List<PlaceRecommendation<DestinationCandidate>> =
        candidates.map { place ->
            val status = place.entrance.takeIf { prefs.needsAccessibleEntrance }
            PlaceRecommendation(place, status, reason(status) + " 같은 조건에서는 예상 시간·거리를 비교해요.")
        }.sortedWith(compareBy({ rank(it.requiredStatus) }, { it.place.minutes }, { it.place.distanceKm }, { it.place.id }))

    fun restPlaces(places: List<RestPlace>, prefs: CarePreferences): List<PlaceRecommendation<RestPlace>> =
        places.map { place ->
            val status = place.entrance.takeIf { prefs.needsAccessibleEntrance }
            val amenities = if (prefs.preferRestAmenities) " 편의시설 선호 반영 · 장애인 화장실 ${place.accessibleRestroom.label}."
                else " 편의시설 우선순위를 끄고 이동 부담을 비교해요."
            val route = if (place.onRoute) "현재 데모 경로 인근" else "현재 데모 경로에서 우회 필요"
            PlaceRecommendation(place, status, reason(status) + amenities + " $route · 추가 ${place.extraMinutes}분.")
        }.sortedWith(compareBy(
            { rank(it.requiredStatus) },
            { if (prefs.preferRestAmenities) rank(it.place.accessibleRestroom) else 0 },
            { it.place.extraMinutes }, { !it.place.onRoute }, { it.place.travelMinutes }, { it.place.distanceKm }, { it.place.id },
        ))

    fun arrivalMessage(snapshot: MobilitySnapshot): String {
        val destination = snapshot.selectedDestination?.takeIf { snapshot.demo && it.name == snapshot.journey?.destination }
            ?: return "현재 목적지의 출입구·주차·하차 시설 정보는 미확인이에요."
        return "가상 시설 안내 · ${destination.name}\n출입구 ${destination.entrance.label}: ${destination.entranceInfo}\n주차·하차: ${destination.parkingInfo}"
    }
}
