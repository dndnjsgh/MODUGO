package com.example.modugo.viewmodel

import com.example.modugo.domain.*
import com.example.modugo.mock.*
import com.example.modugo.settings.SettingsStore
import com.example.modugo.ui.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import androidx.lifecycle.ViewModelStore
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ModugoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = MockMobilityRepository()
    private val settings = object : SettingsStore {
        override val preferences = MutableStateFlow(CarePreferences())
        override fun update(preferences: CarePreferences) { this.preferences.value = preferences }
    }
    private lateinit var model: ModugoViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        model = ModugoViewModel(repository, settings, MockVoiceCompanion())
        store.put("test", model)
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun settingsRequireConfirmedParking() = runTest(dispatcher) {
        for (scenario in listOf(DemoScenario.STARTED, DemoScenario.CONNECTION_FAILED)) {
            model.actions.selectScenario(scenario)
            model.actions.updatePreferences(CarePreferences(careEnabled = false))
            assertTrue(settings.preferences.value.careEnabled)
        }
        model.actions.selectScenario(DemoScenario.PARKED)
        model.actions.updatePreferences(CarePreferences(careEnabled = false))
        assertFalse(settings.preferences.value.careEnabled)
    }

    @Test fun arrivalAcknowledgementAndChecklistResetForNewScenario() = runTest(dispatcher) {
        model.actions.selectScenario(DemoScenario.ARRIVED)
        model.actions.acknowledgeArrival()
        model.actions.toggleArrivalItem(0)
        runCurrent()
        assertTrue(model.state.value.arrivalAcknowledged)
        assertEquals(setOf(0), model.state.value.checkedArrivalItems)
        model.actions.selectScenario(DemoScenario.STARTED)
        runCurrent()
        assertFalse(model.state.value.arrivalAcknowledged)
        assertTrue(model.state.value.checkedArrivalItems.isEmpty())
    }

    @Test fun voiceCommandNavigatesAndReturnsText() = runTest(dispatcher) {
        model.actions.executeCommand(VoiceCommand.REST)
        runCurrent()
        assertEquals(Screen.REST, model.state.value.screen)
        assertTrue(model.state.value.voiceResponse.isNotBlank())
    }

    @Test fun completedRestResetsBothCareRecommendations() = runTest(dispatcher) {
        model.actions.updatePreferences(CarePreferences(motionCareEnabled = true))
        model.actions.selectScenario(DemoScenario.REST_DUE)
        runCurrent()
        assertTrue(model.state.value.evaluation.restDue)
        assertTrue(model.state.value.evaluation.comfortMessage.contains("휴식과 환기"))
        model.actions.park()
        model.actions.completeRest()
        runCurrent()
        assertFalse(model.state.value.evaluation.restDue)
        assertFalse(model.state.value.evaluation.comfortMessage.contains("휴식과 환기"))
    }
    @Test fun repositoryRouteLossClearsArrivalConfirmationWithoutDemoAction() = runTest(dispatcher) {
        repository.setScenario(DemoScenario.ARRIVED)
        runCurrent()
        model.actions.acknowledgeArrival()
        model.actions.toggleArrivalItem(0)
        runCurrent()
        assertTrue(model.state.value.arrivalAcknowledged)
        repository.setScenario(DemoScenario.CONNECTION_FAILED)
        runCurrent()
        assertFalse(model.state.value.arrivalAcknowledged)
        assertTrue(model.state.value.checkedArrivalItems.isEmpty())
    }
}
