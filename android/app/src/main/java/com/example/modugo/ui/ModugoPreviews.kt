package com.example.modugo.ui

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.modugo.domain.*
import com.example.modugo.mock.MockMobilityRepository
import com.example.modugo.mock.MockVoiceCompanion
import com.example.modugo.settings.SettingsStore
import com.example.modugo.ui.theme.MODUGOTheme
import kotlinx.coroutines.flow.MutableStateFlow

class CareScreenPreviews : PreviewParameterProvider<Screen> {
    override val values = Screen.entries.asSequence()
}

@Preview(name = "휴대폰 · 모든 화면", widthDp = 420, heightDp = 1000, showBackground = true)
@Preview(name = "차량 가로 · 모든 화면", widthDp = 1280, heightDp = 900, showBackground = true)
@Composable
fun ModugoScreensPreview(@PreviewParameter(CareScreenPreviews::class) screen: Screen) = MockPreview(screen)

@Preview(name = "큰 글씨 · 병원 비교", widthDp = 600, heightDp = 1200, fontScale = 1.5f)
@Composable
fun AccessibleComparisonPreview() = MockPreview(Screen.ARRIVAL)

/** 기기 저장소와 SDK 없이 인메모리 데이터로 화면과 조작을 확인한다. */
@Composable
private fun MockPreview(initialScreen: Screen) {
    val scope = rememberCoroutineScope()
    val model = remember(initialScreen) {
        val repository = MockMobilityRepository().apply {
            setScenario(when (initialScreen) {
                Screen.HOME, Screen.ARRIVAL -> DemoScenario.HOSPITAL_COMPARISON
                Screen.REST -> DemoScenario.REST_DUE
                Screen.JOURNEY -> DemoScenario.STARTED
                else -> DemoScenario.PARKED
            })
            if (initialScreen == Screen.REST) park()
        }
        val settings = object : SettingsStore {
            override val preferences = MutableStateFlow(CarePreferences())
            override fun update(preferences: CarePreferences) { this.preferences.value = preferences.normalized() }
        }
        ModugoStateHolder(repository, settings, MockVoiceCompanion(), scope, initialScreen)
    }
    val state by model.state.collectAsState()
    MODUGOTheme { ModugoApp(state, model.actions) }
}
