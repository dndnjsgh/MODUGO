package com.example.modugo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.modugo.domain.*
import java.util.Locale

@Composable
fun ModugoApp(state: ModugoUiState, actions: ModugoActions) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    LaunchedEffect(state.screen) { scrollState.scrollTo(0) }
    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.safeDrawingPadding()) {
            AppHeader(state, actions)
            HorizontalDivider(color = colors.outline.copy(alpha = .25f))
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 840.dp
                Column(
                    Modifier.fillMaxSize().verticalScroll(scrollState)
                        .padding(horizontal = if (wide) 36.dp else 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (state.screen != Screen.HOME) {
                        TextButton(onClick = { actions.navigate(Screen.HOME) }, modifier = Modifier.heightIn(min = 56.dp)) {
                            Text("‹  홈으로")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(state.screen.title, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(6.dp))
                            MutedText(state.screen.subtitle)
                        }
                    }
                    if (state.snapshot.demo) DemoControls(state, actions)
                    else Notice("실제 SDK 조회 모드", state.snapshot.diagnostics)
                    if (state.snapshot.demo && state.snapshot.connection == ConnectionStatus.UNAVAILABLE) {
                        Notice("연결 실패 시뮬레이션", "경로와 차량 정보를 확인할 수 없어요. 시연 상황을 바꾸면 Mock 연결을 복구할 수 있어요.", error = true)
                    }
                    state.message?.let { message ->
                        CareCard {
                            Text(message, style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = actions.dismissMessage, modifier = Modifier.heightIn(min = 56.dp)) { Text("확인") }
                        }
                    }
                    if (!state.snapshot.canEdit && state.screen != Screen.HOME) {
                        JourneySummary(state.snapshot)
                        Notice("지금 필요한 안내", state.evaluation.primary.message)
                        LockedNotice()
                    } else when (state.screen) {
                        Screen.HOME -> HomeScreen(state, actions, wide)
                        Screen.REST -> RestScreen(state, actions, wide)
                        Screen.JOURNEY -> JourneyScreen(state)
                        Screen.VOICE -> VoiceScreen(state, actions)
                        Screen.ARRIVAL -> ArrivalScreen(state, actions)
                        Screen.COMFORT -> ComfortScreen(state, actions)
                        Screen.SETTINGS -> SettingsScreen(state, actions)
                    }
                    MutedText(if (state.snapshot.demo) "MOCK MODE · 경로·차량·시설 정보는 모두 데모 데이터입니다." else "SDK MODE · 수신 불가 값은 표시하지 않습니다. 음성·시설 정보는 미연동입니다.", small = true)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
    if (state.selectedPlace != null && state.snapshot.canEdit) {
        PlaceDialog(state.selectedPlace, state.snapshot.waypoint?.id == state.selectedPlace.id, actions)
    }
    if (state.confirmation != null && state.snapshot.canEdit) NavigationDialog(state.confirmation, actions)
}

@Composable
private fun AppHeader(state: ModugoUiState, actions: ModugoActions) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f).heightIn(min = 56.dp).clickable { actions.navigate(Screen.HOME) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandMark()
            Column {
                Text("모두고", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("MODU:GO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        BadgeLabel(if (state.snapshot.demo) "MOCK" else "SDK", accent = true)
        OutlinedButton(onClick = { actions.navigate(Screen.SETTINGS) }, modifier = Modifier.heightIn(min = 56.dp)) {
            Text("설정")
        }
    }
}

@Composable
private fun BrandMark() {
    val mint = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(44.dp)) {
        val s = size.width
        drawRoundRect(mint, topLeft = Offset(0f, s * .08f), size = Size(s, s * .84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .28f))
        val ink = Color(0xFF07372A)
        drawLine(ink, Offset(s*.22f,s*.65f), Offset(s*.33f,s*.35f), s*.08f, StrokeCap.Round)
        drawLine(ink, Offset(s*.33f,s*.35f), Offset(s*.49f,s*.60f), s*.08f, StrokeCap.Round)
        drawLine(ink, Offset(s*.49f,s*.60f), Offset(s*.67f,s*.35f), s*.08f, StrokeCap.Round)
        drawLine(ink, Offset(s*.67f,s*.35f), Offset(s*.78f,s*.65f), s*.08f, StrokeCap.Round)
    }
}

@Composable
private fun DemoControls(state: ModugoUiState, actions: ModugoActions) {
    var expanded by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text("데모 · ${state.snapshot.scenario.label}  ▾")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DemoScenario.entries.forEach { scenario ->
                            DropdownMenuItem(text = { Text(scenario.label) }, onClick = {
                                expanded = false
                                actions.selectScenario(scenario)
                            }, modifier = Modifier.heightIn(min = 64.dp))
                        }
                    }
                }
                TextButton(onClick = { showControls = !showControls }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(if (showControls) "접기" else "조작")
                }
            }
            if (showControls) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = actions.advanceMinutes, enabled = state.snapshot.isDriving,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp)) { Text("+10분 이동") }
                    OutlinedButton(onClick = actions.park, enabled = state.snapshot.isDriving,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp)) { Text("정차로 전환") }
                }
                MutedText("가상 상황을 수동으로 변경합니다. 실제 차량 상태·SDK 연결과 무관합니다.", small = true)
            }
        }
    }
}
@Composable
private fun HomeScreen(state: ModugoUiState, actions: ModugoActions, wide: Boolean) {
    val snapshot = state.snapshot
    val care = state.evaluation
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if (wide) 32.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            BadgeLabel(if (state.preferences.careEnabled) "이동 케어 활성" else "이동 케어 쉬는 중", accent = true)
            Text(care.primary.title, style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(care.primary.message, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Button(onClick = { actions.navigate(care.primary.feature.screen()) }, modifier = Modifier.heightIn(min = 64.dp)) {
                Text("케어 안내 보기  →")
            }
        }
    }
    JourneySummary(snapshot)
    ActiveCareSummary(state)
    if (!snapshot.canEdit) return
    if (snapshot.demo && snapshot.selectedDestination == null) {
        state.destinations.firstOrNull { it.recommended }?.let {
            Notice("목적지 추천 · ${it.place.name} (가상 예시)", "${km(it.place.distanceKm)} km · ${it.place.minutes}분\n${it.reason}")
        }
    }
    SectionTitle("함께하는 다섯 가지 케어", "지금 필요한 지원을 골라보세요.")
    val features = listOf(
        Feature("01", Screen.REST, "휴식 추천", "Smart Rest", if (care.restDue) "휴식할 시간이에요" else "나에게 맞는 휴식 간격"),
        Feature("02", Screen.JOURNEY, "여정 안내", "Predictive Journey", "남은 거리와 시간 한눈에"),
        Feature("03", Screen.VOICE, "이동 동반자", "Voice Companion", "명령 버튼으로 만나는 안내"),
        Feature("04", Screen.ARRIVAL, "도착·하차", "Arrival Care", "도착 이후까지 편안하게"),
        Feature("05", Screen.COMFORT, "쾌적한 이동", "Comfort Care", if (state.preferences.careEnabled && state.preferences.motionCareEnabled) "Motion Care 안내 활성" else "선호 환경과 예방 안내"),
    )
    if (wide) {
        features.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { FeatureCard(it, Modifier.weight(1f), actions.navigate) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    } else {
        features.forEach { FeatureCard(it, Modifier.fillMaxWidth(), actions.navigate) }
    }
}

private data class Feature(val number: String, val screen: Screen, val title: String, val english: String, val description: String)

@Composable
private fun FeatureCard(feature: Feature, modifier: Modifier, navigate: (Screen) -> Unit) {
    Card(onClick = { navigate(feature.screen) }, modifier = modifier,
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            FeatureIcon(feature.screen)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(feature.english, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(feature.title, style = MaterialTheme.typography.titleLarge)
                MutedText(feature.description)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FeatureIcon(screen: Screen) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(48.dp)) {
        val s = size.width
        val w = s*.055f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(accent, Offset(s*x1,s*y1), Offset(s*x2,s*y2),w,StrokeCap.Round)
        when (screen) {
            Screen.REST -> {
                drawRoundRect(accent, Offset(s*.18f,s*.38f), Size(s*.48f,s*.39f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s*.08f), style = Stroke(w))
                drawCircle(accent,s*.12f,Offset(s*.72f,s*.53f),style=Stroke(w))
                line(.3f,.14f,.3f,.25f); line(.5f,.14f,.5f,.25f); line(.14f,.88f,.76f,.88f)
            }
            Screen.JOURNEY -> {
                drawCircle(accent,s*.09f,Offset(s*.2f,s*.75f),style=Stroke(w))
                drawCircle(accent,s*.09f,Offset(s*.8f,s*.25f),style=Stroke(w))
                line(.3f,.75f,.55f,.75f); line(.55f,.75f,.45f,.25f); line(.45f,.25f,.7f,.25f)
            }
            Screen.VOICE -> {
                listOf(.25f,.43f,.6f,.4f,.2f).forEachIndexed { index, height ->
                    line(.15f+index*.17f,.5f-height/2,.15f+index*.17f,.5f+height/2)
                }
            }
            Screen.ARRIVAL -> {
                line(.25f,.15f,.25f,.87f); line(.25f,.18f,.8f,.18f)
                line(.8f,.18f,.67f,.4f); line(.67f,.4f,.25f,.4f); line(.1f,.87f,.42f,.87f)
            }
            else -> {
                drawOval(accent, Offset(s*.18f,s*.16f), Size(s*.58f,s*.68f), style=Stroke(w))
                line(.28f,.79f,.64f,.3f); line(.43f,.58f,.36f,.35f)
            }
        }
    }
}

@Composable
private fun JourneySummary(snapshot: MobilitySnapshot) {
    CareCard {
        Text(snapshot.journey?.destination ?: "목적지 정보 없음", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Metric("남은 시간", snapshot.journey?.let { "${it.remainingMinutes}분" } ?: "—", Modifier.weight(1f))
            Metric("남은 거리", snapshot.journey?.let { "${km(it.remainingDistanceKm)} km" } ?: "—", Modifier.weight(1f))
        }
        MutedText(if (!snapshot.demo) "실제 NaviHelper 수신 정보 · 미수신 값은 표시하지 않음" else if (snapshot.connection == ConnectionStatus.UNAVAILABLE) "Mock 연결 실패 · 현재 정보 없음" else "Mock 연결 정상 · 실제 SDK 미연동", small = true)
        if (snapshot.demo && snapshot.journey != null) Text("적용 옵션 · ${snapshot.routeOption.label}")
        snapshot.waypoint?.let { Text("휴식 경유지 · ${it.name} (가상)") }
    }
}

@Composable
private fun RestScreen(state: ModugoUiState, actions: ModugoActions, wide: Boolean) {
    if (!state.preferences.careEnabled) {
        Notice("휴식 안내가 쉬고 있어요", "이동 케어가 꺼져 있어요. 정차 중 설정에서 다시 켜면 휴식 안내를 받을 수 있어요.")
    } else {
        Notice(if (state.evaluation.restDue) "잠시 쉬어갈 시간이에요" else "내 이동 리듬에 맞춘 휴식",
            state.evaluation.restInMinutes?.let { if (it == 0) "설정한 ${state.preferences.restIntervalMinutes}분 휴식 간격에 도달했어요." else "다음 휴식 안내까지 약 ${it}분 남았어요." }
                ?: "이동 케어와 경로 상태를 확인해 주세요.")
    }
    CareCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric("이동 시간", state.snapshot.journey?.takeIf { state.snapshot.tripTimeAvailable }?.let { "${it.elapsedMinutes}분" } ?: "—", Modifier.weight(1f))
            Metric("주행 거리", state.snapshot.journey?.takeIf { state.snapshot.tripDistanceAvailable }?.let { "${km(it.distanceTravelledKm)} km" } ?: "—", Modifier.weight(1f))
        }
        Text("휴식 간격  ${state.preferences.restIntervalMinutes}분", style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = { actions.navigate(Screen.SETTINGS) }, modifier = Modifier.heightIn(min = 64.dp), enabled = state.snapshot.canEdit) { Text("휴식 간격 설정") }
        Button(onClick = actions.completeRest, enabled = state.snapshot.canEdit && state.snapshot.minutesSinceRest > 0,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("휴식 완료 · 휴식 시간 다시 시작") }
    }
    state.snapshot.waypoint?.let { Notice("데모 경유지에 추가됨", "${it.name} · 앱 내부에만 저장한 가상 경유지예요.") }
    SectionTitle("쉬어갈 장소", if (state.snapshot.demo) "위치·거리·시설은 실제 검색 결과가 아닌 샘플입니다." else "검증된 장소 데이터가 없어 실제 경유지 추가는 제공하지 않습니다.")
    if (!state.snapshot.canEdit) LockedNotice()
    state.restRecommendations.forEachIndexed { index, recommendation ->
        val place = recommendation.place
        Card(onClick = { actions.selectPlace(place) }, enabled = state.snapshot.canEdit && recommendation.selectable,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BadgeLabel(if (index == 0 && recommendation.recommended) "추천 · 가상 장소" else "가상 장소")
                Text(place.name, style = MaterialTheme.typography.titleLarge)
                Text("${place.type} · 예상 ${km(place.distanceKm)} km", style = MaterialTheme.typography.bodyLarge)
                MutedText(place.facilities.joinToString(" · "))
                Text("출입구 ${place.entrance.label} · 장애인 화장실 ${place.accessibleRestroom.label}")
                Text("이동 ${place.travelMinutes}분 · 추가 ${place.extraMinutes}분 · ${if (place.onRoute) "현재 데모 경로 인근" else "우회 필요"}")
                MutedText(recommendation.reason)
                Text(if (!recommendation.selectable) "필수 조건 미충족으로 선택 제외"
                    else if (state.snapshot.canEdit) "상세 정보 보기  →" else "정차 후 상세 정보 확인", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun JourneyScreen(state: ModugoUiState) {
    JourneySummary(state.snapshot)
    Notice("현재 여정 안내", state.evaluation.journeyMessage)
    CareCard {
        Text("이동의 시작부터 도착까지", style = MaterialTheme.typography.titleLarge)
        val arrived = state.snapshot.journey?.arrived == true
        val approaching = state.evaluation.arrivalDue
        listOf("출발 준비", "이동 케어", "도착·하차 준비").forEachIndexed { i, title ->
            val active = when { arrived || approaching -> i == 2; state.snapshot.isDriving || (!state.snapshot.demo && state.snapshot.journey != null) -> i == 1; else -> i == 0 }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BadgeLabel("0${i+1}", accent = active)
                Text(title, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium)
            }
        }
        state.snapshot.waypoint?.let { Text("데모 경유지 · ${it.name}") }
    }
    MutedText("시연 상황을 바꾸면 안내가 갱신됩니다. 교통 정체·위험 구간을 실제로 예측하지 않습니다.")
}

@Composable
private fun VoiceScreen(state: ModugoUiState, actions: ModugoActions) {
    Notice("명령 버튼으로 체험해 보세요", "음성 인식과 음성 합성은 연결되지 않았어요. 선택한 명령을 처리하고 결과를 글로 안내합니다.")
    VoiceCommand.entries.forEach { command ->
        OutlinedButton(onClick = { actions.executeCommand(command) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
            contentPadding = PaddingValues(20.dp)) { Text(command.label) }
    }
    CareCard {
        Text("모두고의 안내", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(state.voiceResponse, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ArrivalScreen(state: ModugoUiState, actions: ModugoActions) {
    if (state.snapshot.demo && state.snapshot.canEdit) DestinationComparison(state, actions)
    JourneySummary(state.snapshot)
    Notice(if (state.evaluation.arrivalDue) "도착을 준비해 주세요" else "도착 전에 미리 확인해요",
        if (state.snapshot.journey?.arrived == true) "목적지 도착 안내를 받았어요. 주변을 확인하고 안전하게 하차하세요."
        else if (state.evaluation.arrivalDue) "목적지에 가까워졌어요. 상세 확인과 하차 준비는 정차 후 진행하세요."
        else "목적지 접근 시 도착 안내를 표시합니다.")
    if (!state.snapshot.canEdit) LockedNotice()
    if (state.evaluation.arrivalDue || state.snapshot.journey?.arrived == true) CareCard {
        Text("하차 전 확인", style = MaterialTheme.typography.titleLarge)
        listOf("차량이 안전한 곳에 정차했는지 확인", "주변 차량과 보행자 확인", "소지품과 필요한 이동 보조기구 확인").forEachIndexed { index, text ->
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = index in state.checkedArrivalItems,
                    onCheckedChange = { actions.toggleArrivalItem(index) }, enabled = state.snapshot.canEdit,
                    modifier = Modifier.semantics { contentDescription = text })
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(onClick = actions.acknowledgeArrival,
            enabled = state.snapshot.canEdit && (state.evaluation.arrivalDue || state.snapshot.journey?.arrived == true) && !state.arrivalAcknowledged,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text(if (state.arrivalAcknowledged) "도착 안내 확인 완료" else "도착 안내 확인")
        }
    }
    Notice("선택한 목적지의 도착 안내", RecommendationEngine.arrivalMessage(state.snapshot))
}

@Composable
private fun ComfortScreen(state: ModugoUiState, actions: ModugoActions) {
    RouteSelection(state, actions)
    CareCard {
        BadgeLabel(if (state.snapshot.demo) "차량 환경 샘플" else "Vehicle State · 앞좌석 좌측 조회")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric("실내 온도", state.snapshot.vehicle?.cabinTemperatureC?.let { "$it°C" } ?: "—", Modifier.weight(1f))
            Metric("공조 상태", when (state.snapshot.vehicle?.hvacOn) { true -> "켜짐"; false -> "꺼짐"; null -> "—" }, Modifier.weight(1f))
        }
        MutedText(if (state.snapshot.demo) "표시값은 가상 차량 상태입니다. 차량 장치는 제어하지 않습니다." else "Capability 확인 후 조회한 값입니다. 차량 장치는 제어하지 않습니다.")
    }
    Notice("편안한 이동을 위한 안내", state.evaluation.comfortMessage)
    if (!state.snapshot.canEdit) LockedNotice()
    CareCard {
        SettingToggle("Motion Care", "이동 시간에 따른 휴식·환기 예방 안내", state.preferences.motionCareEnabled,
            state.snapshot.canEdit) { actions.updatePreferences(state.preferences.copy(motionCareEnabled = it)) }
        TemperatureSetting(state, actions)
        MutedText("멀미 감지나 치료 기능이 아닙니다. 선호 온도는 앱 내부 설정으로만 저장됩니다.")
    }
}

@Composable
private fun SettingsScreen(state: ModugoUiState, actions: ModugoActions) {
    if (!state.snapshot.canEdit) LockedNotice()
    PreferenceSettings(state, actions)
    CareCard {
        SettingToggle("이동 케어", "휴식·도착·쾌적한 이동 안내", state.preferences.careEnabled, state.snapshot.canEdit) {
            actions.updatePreferences(state.preferences.copy(careEnabled = it))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .25f))
        Text("휴식 추천 간격", style = MaterialTheme.typography.titleLarge)
        Stepper("${state.preferences.restIntervalMinutes}분", state.snapshot.canEdit,
            state.preferences.restIntervalMinutes > 30, state.preferences.restIntervalMinutes < 120,
            { actions.updatePreferences(state.preferences.copy(restIntervalMinutes = state.preferences.restIntervalMinutes - 15)) },
            { actions.updatePreferences(state.preferences.copy(restIntervalMinutes = state.preferences.restIntervalMinutes + 15)) }, "휴식 간격")
        MutedText("30~120분 · 마지막 휴식 이후의 가상 이동 시간 기준")
    }
    CareCard {
        SettingToggle("Motion Care", "장시간 이동 중 예방 안내", state.preferences.motionCareEnabled, state.snapshot.canEdit) {
            actions.updatePreferences(state.preferences.copy(motionCareEnabled = it))
        }
        TemperatureSetting(state, actions)
    }
    Notice("설정은 자동으로 저장됩니다", "필요한 조건과 이동 케어 설정을 유지합니다. 편안한 경로 선호를 켜도 적용 옵션은 확인 전까지 바뀌지 않습니다.")
    MutedText("음성 SDK는 미연동 상태입니다. Mock/Real은 Build Variant에서 선택합니다.")
}

@Composable
private fun TemperatureSetting(state: ModugoUiState, actions: ModugoActions) {
    Text("선호 온도", style = MaterialTheme.typography.titleLarge)
    Stepper("${state.preferences.preferredTemperatureC}°C", state.snapshot.canEdit,
        state.preferences.preferredTemperatureC > 18, state.preferences.preferredTemperatureC < 28,
        { actions.updatePreferences(state.preferences.copy(preferredTemperatureC = state.preferences.preferredTemperatureC - 1)) },
        { actions.updatePreferences(state.preferences.copy(preferredTemperatureC = state.preferences.preferredTemperatureC + 1)) }, "선호 온도")
}

@Composable
private fun Stepper(value: String, enabled: Boolean, canDecrease: Boolean, canIncrease: Boolean,
                    decrease: () -> Unit, increase: () -> Unit, name: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(decrease, enabled = enabled && canDecrease,
            modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 64.dp).semantics { contentDescription = "$name 낮추기" }) { Text("−") }
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(increase, enabled = enabled && canIncrease,
            modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 64.dp).semantics { contentDescription = "$name 높이기" }) { Text("+") }
    }
}

@Composable
internal fun SettingToggle(title: String, description: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            MutedText(description)
        }
        Switch(checked, onCheckedChange = onChange, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun PlaceDialog(place: RestPlace, alreadyAdded: Boolean, actions: ModugoActions) {
    AlertDialog(onDismissRequest = { actions.selectPlace(null) },
        title = { Text(place.name, style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BadgeLabel("샘플 장소 · 실제 검색 결과 아님")
                Text("${place.type} · 예상 ${km(place.distanceKm)} km")
                Text(place.description)
                Text(place.facilities.joinToString("\n") { "• $it" })
                MutedText("경유지 추가는 앱 내부 시뮬레이션입니다. 실제 내비게이션 경로는 변경하지 않습니다.")
            }
        },
        confirmButton = {
            Button(onClick = { actions.addWaypoint(place) }, enabled = !alreadyAdded,
                modifier = Modifier.heightIn(min = 64.dp)) { Text(if (alreadyAdded) "추가 완료" else "경유지 추가 검토") }
        },
        dismissButton = { TextButton({ actions.selectPlace(null) }, modifier = Modifier.heightIn(min = 64.dp)) { Text("닫기") } })
}

@Composable
internal fun CareCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
internal fun Notice(title: String, message: String, error: Boolean = false) {
    val color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    CareCard {
        Text(title, style = MaterialTheme.typography.titleLarge, color = color)
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LockedNotice() = Notice("정차 후 사용할 수 있어요", "주행 중이거나 차량 상태를 확인할 수 없으면 상세 조회와 설정을 잠급니다. Mock에서는 정차로 전환하고, SDK 모드에서는 차량의 조작 제한 해제를 기다리세요.")

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        MutedText(subtitle)
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MutedText(label)
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
internal fun BadgeLabel(text: String, accent: Boolean = false) {
    val color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(text, modifier = Modifier.border(1.dp, color.copy(alpha = .45f), RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 6.dp), color = color, style = MaterialTheme.typography.labelMedium)
}

@Composable
internal fun MutedText(text: String, small: Boolean = false) = Text(text,
    style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant)

internal fun km(value: Double) = String.format(Locale.KOREA, "%.1f", value)


