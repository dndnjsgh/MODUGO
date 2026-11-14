package com.example.modugo.domain

object CareEngine {
    fun evaluate(snapshot: MobilitySnapshot, preferences: CarePreferences): CareEvaluation {
        val settings = preferences.normalized()
        val journey = snapshot.journey
        val available = snapshot.connection != ConnectionStatus.UNAVAILABLE && journey != null
        val restIn = if (available && snapshot.tripTimeAvailable) (settings.restIntervalMinutes - snapshot.minutesSinceRest).coerceAtLeast(0) else null
        val restDue = settings.careEnabled && available && journey?.arrived == false && restIn == 0
        val arrivalDue = settings.careEnabled && available && journey?.arrived == false &&
            (journey.remainingMinutes <= 5 || journey.remainingDistanceKm <= 2.0)
        val journeyMessage = when {
            !available -> "이동 정보를 확인할 수 없어요. 연결 상태를 확인해 주세요."
            journey!!.arrived -> "목적지 도착 상황이에요. 안전하게 정차한 뒤 하차를 준비하세요."
            snapshot.vehicle?.motion == MotionState.PARKED && journey.elapsedMinutes == 0 -> "출발 전 목적지와 이동 케어 설정을 확인하세요."
            arrivalDue -> "약 ${journey.remainingMinutes}분 후 도착 예정이에요. 정차 후 하차 안내를 확인하세요."
            else -> "목적지까지 약 ${journey.remainingMinutes}분 남았어요. 현재 수신한 경로를 기준으로 안내해요."
        }
        val comfortMessage = when {
            !settings.careEnabled -> "이동 케어가 꺼져 있어요. 환경 정보만 표시해요."
            snapshot.connection == ConnectionStatus.UNAVAILABLE -> "차량 환경 정보를 확인할 수 없어요."
            settings.motionCareEnabled && snapshot.tripTimeAvailable && snapshot.minutesSinceRest >= 30 -> "오래 이동하고 있어요. 불편하다면 안전한 곳에 정차해 휴식과 환기를 고려하세요."
            snapshot.vehicle?.cabinTemperatureC == null -> "실내 온도 정보가 없어요. 선호 온도는 앱에만 저장돼요."
            snapshot.vehicle.cabinTemperatureC > settings.preferredTemperatureC + 2 -> "실내 온도가 선호 온도보다 높아요. 정차 후 쾌적한 환경을 확인하세요."
            settings.motionCareEnabled -> "Motion Care가 켜져 있어요. 이동 시간에 따라 예방 안내를 제공해요."
            else -> "선호하는 이동 환경을 확인했어요. 차량 장치는 제어하지 않아요."
        }
        val primary = when {
            snapshot.demo && snapshot.canEdit && settings.careEnabled && journey == null ->
                CareNotice("이용할 수 있는 목적지부터 찾아요", "가상 병원 후보의 출입구 조건과 이동 시간을 비교해 보세요.", CareFeature.ARRIVAL)
            !available -> CareNotice("이동 정보 연결을 확인해 주세요", "유효한 경로 정보가 없어요. 연결 상태와 내비게이션 목적지를 확인하세요.", CareFeature.JOURNEY)
            !settings.careEnabled -> CareNotice("이동 케어가 쉬고 있어요", "정차 중 설정에서 케어를 다시 켤 수 있어요.", CareFeature.COMFORT)
            journey!!.arrived -> CareNotice("도착했어요. 하차를 준비해요", "안전하게 정차한 뒤 소지품과 주변을 확인하세요.", CareFeature.ARRIVAL)
            arrivalDue -> CareNotice("곧 도착해요 · ${journey.destination}", "정차 후 선택한 목적지의 주차·출입구 정보를 확인하세요.", CareFeature.ARRIVAL)
            restDue -> CareNotice("잠시 쉬어 가는 건 어떨까요?", "마지막 휴식 후 ${snapshot.minutesSinceRest}분 이동했어요. 정차 후 휴식 장소를 살펴보세요.", CareFeature.REST)
            snapshot.demo && snapshot.canEdit && settings.needsAccessibleEntrance && snapshot.selectedDestination != null &&
                snapshot.selectedDestination.entrance != AccessibilityStatus.CONFIRMED ->
                CareNotice("선택한 목적지의 조건을 다시 확인해요", "출입구 ${snapshot.selectedDestination.entrance.label}. 설정에 맞는 다른 후보를 비교할 수 있어요.", CareFeature.ARRIVAL)
            snapshot.demo && snapshot.canEdit && snapshot.selectedDestination == null ->
                CareNotice("나에게 맞는 목적지를 비교해요", "거리보다 필요한 출입구 조건을 먼저 고려해요. 병원 A/B 대표 시연을 시작하세요.", CareFeature.ARRIVAL)
            snapshot.demo && snapshot.canEdit && settings.preferComfortableRoute && snapshot.routeOption != RoutePreference.COMFORTABLE ->
                CareNotice("편안한 경로를 선호하시나요?", "Comfort Care에서 변경 내용을 확인하고 적용하세요.", CareFeature.COMFORT)
            snapshot.vehicle?.motion == MotionState.PARKED -> CareNotice("나에게 맞는 이동을 준비해요", "목적지를 확인하고 편안한 여정을 시작하세요.", CareFeature.JOURNEY)
            !snapshot.tripTimeAvailable -> CareNotice("내비게이션과 함께하는 여정", "현재 주행 시간 응답이 없어 휴식 시간 추천을 일시 중지했어요.", CareFeature.JOURNEY)
            else -> CareNotice("편안한 여정을 함께해요", "다음 휴식 추천까지 약 ${restIn}분 남았어요.", CareFeature.JOURNEY)
        }
        return CareEvaluation(restDue, restIn, arrivalDue, journeyMessage, comfortMessage, primary)
    }
}
