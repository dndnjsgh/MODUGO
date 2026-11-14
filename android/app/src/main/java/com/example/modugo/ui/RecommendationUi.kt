package com.example.modugo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.modugo.domain.*

@Composable
internal fun DestinationComparison(state: ModugoUiState, actions: ModugoActions) {
    var expanded by remember(state.snapshot.selectedDestination?.id) { mutableStateOf(state.snapshot.selectedDestination == null) }
    CareCard {
        BadgeLabel("Arrival Care · 목적지 선택부터")
        Text("가상 병원 비교", style = MaterialTheme.typography.headlineSmall)
        MutedText("실제 병원 검색이 아닙니다. 모든 이름·거리·시설 상태는 시연용 설정입니다.")
        if (state.snapshot.selectedDestination != null) {
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 64.dp)) {
                Text(if (expanded) "후보 접기" else "다른 목적지 비교")
            }
        }
        if (expanded) {
            OutlinedTextField(value = state.destinationQuery, onValueChange = actions.searchDestinations,
                label = { Text("가상 후보 검색 · 병원 / 병원 A") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            SettingToggle("무단차 출입구 필수", "충족 후보를 먼저 비교하고 미확인 후보는 구분해요.",
                state.preferences.needsAccessibleEntrance, state.snapshot.canEdit) {
                actions.updatePreferences(state.preferences.copy(needsAccessibleEntrance = it))
            }
            if (state.destinations.isEmpty()) Text("일치하는 가상 후보가 없어요. ‘병원’으로 검색해 보세요.")
        }
    }
    if (!expanded) return
    state.destinations.forEachIndexed { index, recommendation ->
        val place = recommendation.place
        CareCard {
            BadgeLabel(when {
                index == 0 && recommendation.recommended -> "우선 추천 · 가상 예시"
                !recommendation.selectable -> "필수 조건 미충족"
                !recommendation.recommended -> "확인 필요 · 추천 보류"
                else -> "비교 후보"
            }, accent = index == 0 && recommendation.recommended)
            Text(place.name, style = MaterialTheme.typography.headlineSmall)
            Text("${km(place.distanceKm)} km · 예상 ${place.minutes}분", style = MaterialTheme.typography.titleLarge)
            Text("출입구 · ${place.entrance.label} (가상 설정)")
            Text(place.entranceInfo)
            Text("주차·하차 · ${place.parkingInfo}")
            MutedText(recommendation.reason)
            Button(onClick = { actions.selectDestination(place.id) }, enabled = recommendation.selectable,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(if (!recommendation.selectable) "필수 조건 미충족으로 선택 제외"
                    else if (!recommendation.recommended) "미확인 정보 유의 후 선택 검토" else "이 목적지 선택 검토")
            }
        }
    }
}

@Composable
internal fun RouteSelection(state: ModugoUiState, actions: ModugoActions) {
    CareCard {
        BadgeLabel("Comfort Care · 경로 선택")
        Text(state.snapshot.journey?.destination ?: "목적지를 먼저 선택해 주세요", style = MaterialTheme.typography.titleLarge)
        SettingToggle("편안한 경로 선호", "선호만 저장하며 경로는 확인 후 적용해요.",
            state.preferences.preferComfortableRoute, state.snapshot.canEdit) {
            actions.updatePreferences(state.preferences.copy(preferComfortableRoute = it))
        }
        if (!state.snapshot.demo) {
            Text("SDK 조회 모드에서는 경로 옵션 변경을 제공하지 않아요.")
        } else {
            val preferred = if (state.preferences.preferComfortableRoute) RoutePreference.COMFORTABLE else RoutePreference.RECOMMENDED
            Text("선호에 따른 제안 · ${preferred.label}")
            Text("현재 적용 · ${state.snapshot.routeOption.label}")
            RoutePreference.entries.forEach { option ->
                OutlinedButton(onClick = { actions.selectRouteOption(option) },
                    enabled = state.snapshot.canEdit && state.snapshot.journey?.arrived == false && state.snapshot.routeOption != option,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                    Text(if (state.snapshot.routeOption == option) "${option.label} · 적용 중" else "${option.label} 선택")
                }
            }
        }
        MutedText("COMFORTABLE 옵션을 가정한 Mock입니다. 도로 진동·급커브·승차감 분석이나 멀미 감소를 보장하지 않습니다.")
    }
}

@Composable
internal fun PreferenceSettings(state: ModugoUiState, actions: ModugoActions) {
    CareCard {
        Text("나에게 필요한 조건", style = MaterialTheme.typography.titleLarge)
        MutedText("유형별 프로필 대신 필요한 조건을 직접 조합하세요.")
        SettingToggle("무단차 출입구 필수", "목적지·휴식 장소의 필수 조건", state.preferences.needsAccessibleEntrance, state.snapshot.canEdit) {
            actions.updatePreferences(state.preferences.copy(needsAccessibleEntrance = it))
        }
        SettingToggle("휴식 장소 편의시설 고려", "장애인 화장실이 있는 가상 장소를 우선 비교", state.preferences.preferRestAmenities, state.snapshot.canEdit) {
            actions.updatePreferences(state.preferences.copy(preferRestAmenities = it))
        }
        SettingToggle("편안한 경로 선호", "변경 확인 전에는 적용 옵션을 유지", state.preferences.preferComfortableRoute, state.snapshot.canEdit) {
            actions.updatePreferences(state.preferences.copy(preferComfortableRoute = it))
        }
        SettingToggle("음성 중심 안내 선호", "현재는 글 안내와 명령 버튼으로 시연", state.preferences.preferVoiceGuidance, state.snapshot.canEdit) {
            actions.updatePreferences(state.preferences.copy(preferVoiceGuidance = it))
        }
    }
}

@Composable
internal fun ActiveCareSummary(state: ModugoUiState) {
    val active = if (!state.preferences.careEnabled) "이동 케어 꺼짐" else buildList {
        add("Predictive Journey")
        add(if (state.evaluation.restDue) "Smart Rest · 휴식 추천 중" else "Smart Rest · 대기")
        add(if (state.evaluation.arrivalDue || state.snapshot.journey?.arrived == true) "Arrival Care · 도착 안내" else "Arrival Care · 목적지 조건 비교")
        add("Comfort Care${if (state.preferences.motionCareEnabled) " · Motion Care 켜짐" else ""}")
        add("Voice Companion · ${if (state.preferences.preferVoiceGuidance) "글 안내 우선" else "명령 버튼"}")
    }.joinToString("\n")
    CareCard {
        Text("함께 연결된 케어", style = MaterialTheme.typography.titleLarge)
        if (state.snapshot.canEdit) Text(active)
        else Text(if (state.preferences.careEnabled) "이동 케어 활성 · ${state.evaluation.primary.feature.screen().subtitle}" else "이동 케어 꺼짐")
        if (state.preferences.preferVoiceGuidance && state.snapshot.canEdit) {
            Text(state.evaluation.primary.message)
            MutedText("음성 출력 없이 글로 안내 중")
        }
    }
}

@Composable
internal fun NavigationDialog(confirmation: NavigationConfirmation, actions: ModugoActions) {
    AlertDialog(onDismissRequest = actions.cancelNavigation,
        title = { Text(confirmation.title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BadgeLabel("사용자 확인 · Mock 변경")
            Text(confirmation.detail)
        } },
        confirmButton = { Button(onClick = actions.confirmNavigation, modifier = Modifier.heightIn(min = 64.dp)) { Text("확인 후 데모 적용") } },
        dismissButton = { TextButton(onClick = actions.cancelNavigation, modifier = Modifier.heightIn(min = 64.dp)) { Text("취소") } })
}
