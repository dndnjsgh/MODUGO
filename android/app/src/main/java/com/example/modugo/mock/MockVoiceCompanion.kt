package com.example.modugo.mock

import com.example.modugo.data.VoiceCompanion
import com.example.modugo.domain.*

class MockVoiceCompanion : VoiceCompanion {
    override fun respond(command: VoiceCommand, snapshot: MobilitySnapshot, preferences: CarePreferences): VoiceReply {
        val evaluation = CareEngine.evaluate(snapshot, preferences)
        return when (command) {
            VoiceCommand.DESTINATIONS -> VoiceReply(if (snapshot.demo) "가상 병원 후보를 비교해요. 출입구 필수 조건을 충족한 후보부터 확인하세요."
                else "실제 목적지 후보 검색은 아직 제공하지 않아요.", CareFeature.ARRIVAL)
            VoiceCommand.COMFORTABLE_ROUTE -> VoiceReply("편안한 경로 변경은 정차 중 확인을 거쳐 데모에만 적용해요.", CareFeature.COMFORT)
            VoiceCommand.REST -> VoiceReply(
                if (snapshot.canEdit) "샘플 휴식 장소를 준비했어요. 아래에서 휴식 화면을 열어 확인해 주세요."
                else "휴식 안내를 준비했어요. 장소 상세와 경유지 선택은 정차 후 이용해 주세요.", CareFeature.REST)
            VoiceCommand.REMAINING_TIME -> VoiceReply(evaluation.journeyMessage, CareFeature.JOURNEY)
            VoiceCommand.ARRIVAL -> VoiceReply(
                if (snapshot.journey == null) "도착 정보를 확인할 수 없어요."
                else if (!snapshot.canEdit) "${snapshot.journey.destination} 도착 정보는 정차 후 확인하세요."
                else RecommendationEngine.arrivalMessage(snapshot), CareFeature.ARRIVAL)
            VoiceCommand.CARE_STATUS -> VoiceReply(
                "이동 케어 ${if (preferences.careEnabled) "켜짐" else "꺼짐"} · Motion Care ${if (preferences.motionCareEnabled) "켜짐" else "꺼짐"}. 실제 음성 인식·음성 출력 없이 버튼으로 실행한 데모예요.", CareFeature.COMFORT)
        }
    }
}
