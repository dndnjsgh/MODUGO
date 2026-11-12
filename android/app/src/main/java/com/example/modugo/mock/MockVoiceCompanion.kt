package com.example.modugo.mock

import com.example.modugo.data.VoiceCompanion
import com.example.modugo.domain.*

class MockVoiceCompanion : VoiceCompanion {
    override fun respond(command: VoiceCommand, snapshot: MobilitySnapshot, preferences: CarePreferences): VoiceReply {
        val evaluation = CareEngine.evaluate(snapshot, preferences)
        return when (command) {
            VoiceCommand.REST -> VoiceReply(
                if (snapshot.canEdit) "샘플 휴식 장소를 준비했어요. 아래에서 휴식 화면을 열어 확인해 주세요."
                else "휴식 안내를 준비했어요. 장소 상세와 경유지 선택은 정차 후 이용해 주세요.", CareFeature.REST)
            VoiceCommand.REMAINING_TIME -> VoiceReply(evaluation.journeyMessage, CareFeature.JOURNEY)
            VoiceCommand.ARRIVAL -> VoiceReply(
                if (snapshot.journey == null) "도착 정보를 확인할 수 없어요."
                else "${snapshot.journey.destination} 도착 정보를 준비했어요. 시설 정보는 샘플이며 하차 전에는 안전한 정차를 확인하세요.", CareFeature.ARRIVAL)
            VoiceCommand.CARE_STATUS -> VoiceReply(
                "이동 케어 ${if (preferences.careEnabled) "켜짐" else "꺼짐"} · Motion Care ${if (preferences.motionCareEnabled) "켜짐" else "꺼짐"}. 실제 음성 인식·음성 출력 없이 버튼으로 실행한 데모예요.", CareFeature.COMFORT)
        }
    }
}
