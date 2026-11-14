package com.example.modugo.viewmodel

import com.example.modugo.domain.*
import com.example.modugo.mock.*
import com.example.modugo.settings.SettingsStore
import com.example.modugo.ui.Screen
import com.example.modugo.ui.ModugoStateHolder
import com.example.modugo.data.MobilityRepository
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

    @Test fun destinationRequiresConfirmationAndUnmetConditionIsBlocked() = runTest(dispatcher) {
        model.actions.selectScenario(DemoScenario.HOSPITAL_COMPARISON)
        model.actions.selectDestination("hospital-a")
        runCurrent()
        assertNull(model.state.value.confirmation)
        model.actions.selectDestination("hospital-b")
        runCurrent()
        assertNotNull(model.state.value.confirmation)
        assertNull(repository.state.value.journey)
        model.actions.confirmNavigation()
        runCurrent()
        assertEquals("병원 B", repository.state.value.journey!!.destination)
        assertEquals(Screen.COMFORT, model.state.value.screen)
    }

    @Test fun routePreferenceDoesNotApplyUntilConfirmationAndCancelDoesNothing() = runTest(dispatcher) {
        model.actions.updatePreferences(CarePreferences(preferComfortableRoute = true))
        assertEquals(RoutePreference.RECOMMENDED, repository.state.value.routeOption)
        model.actions.selectRouteOption(RoutePreference.COMFORTABLE)
        model.actions.cancelNavigation()
        model.actions.confirmNavigation()
        runCurrent()
        assertEquals(RoutePreference.RECOMMENDED, repository.state.value.routeOption)
        model.actions.selectRouteOption(RoutePreference.COMFORTABLE)
        model.actions.confirmNavigation()
        runCurrent()
        assertEquals(RoutePreference.COMFORTABLE, repository.state.value.routeOption)
    }

    @Test fun movingOrChangedPreferencesRevokePendingConsent() = runTest(dispatcher) {
        runCurrent()
        model.actions.selectRouteOption(RoutePreference.COMFORTABLE)
        repository.setScenario(DemoScenario.STARTED)
        runCurrent()
        assertNull(model.state.value.confirmation)
        model.actions.confirmNavigation()
        assertEquals(RoutePreference.RECOMMENDED, repository.state.value.routeOption)
        repository.park()
        runCurrent()
        model.actions.selectRouteOption(RoutePreference.COMFORTABLE)
        model.actions.updatePreferences(CarePreferences(preferRestAmenities = false))
        model.actions.confirmNavigation()
        runCurrent()
        assertEquals(RoutePreference.RECOMMENDED, repository.state.value.routeOption)
    }

    @Test fun dispatchRevalidatesConsentAgainstRouteChanges() = runTest(dispatcher) {
        runCurrent()
        model.actions.selectRouteOption(RoutePreference.COMFORTABLE)
        model.actions.confirmNavigation()
        repository.setScenario(DemoScenario.APPROACHING)
        repository.park()
        runCurrent()
        assertEquals(RoutePreference.RECOMMENDED, repository.state.value.routeOption)
    }

    @Test fun unknownDestinationHasExplicitWarningAndRequiresConsent() = runTest(dispatcher) {
        model.actions.selectDestination("hospital-c")
        runCurrent()
        assertTrue(model.state.value.confirmation!!.detail.contains("미확인"))
        assertNull(repository.state.value.selectedDestination)
        model.actions.confirmNavigation()
        runCurrent()
        assertEquals(AccessibilityStatus.UNKNOWN, repository.state.value.selectedDestination!!.entrance)
    }

    @Test fun preferencesAndSearchUpdateCandidateLists() = runTest(dispatcher) {
        runCurrent()
        assertEquals("hospital-b", model.state.value.destinations.first().place.id)
        assertEquals("rest-2", model.state.value.restRecommendations.first().place.id)
        model.actions.updatePreferences(CarePreferences(needsAccessibleEntrance = false, preferRestAmenities = false))
        runCurrent()
        assertEquals("hospital-c", model.state.value.destinations.first().place.id)
        assertEquals("rest-4", model.state.value.restRecommendations.first().place.id)
        model.actions.searchDestinations("병원 A")
        runCurrent()
        assertEquals(listOf("hospital-a"), model.state.value.destinations.map { it.place.id })
        model.actions.searchDestinations("없는 후보")
        runCurrent()
        assertTrue(model.state.value.destinations.isEmpty())
    }

    @Test fun fiveCareFeaturesShareDestinationRouteRestAndArrivalWithoutSdk() = runTest(dispatcher) {
        model.actions.selectScenario(DemoScenario.HOSPITAL_COMPARISON)
        runCurrent()
        model.actions.selectDestination("hospital-b")
        model.actions.confirmNavigation()
        runCurrent()
        model.actions.executeCommand(VoiceCommand.COMFORTABLE_ROUTE)
        runCurrent()
        assertNotNull(model.state.value.confirmation)
        assertEquals(RoutePreference.RECOMMENDED, repository.state.value.routeOption)
        model.actions.confirmNavigation()
        runCurrent()
        model.actions.selectScenario(DemoScenario.STARTED)
        runCurrent()
        assertEquals("병원 B", model.state.value.snapshot.journey!!.destination)
        model.actions.selectScenario(DemoScenario.REST_DUE)
        runCurrent()
        assertTrue(model.state.value.evaluation.restDue)
        assertEquals(CareFeature.REST, model.state.value.evaluation.primary.feature)
        model.actions.park()
        runCurrent()
        model.actions.addWaypoint(repository.restPlaces.first { it.id == "rest-2" })
        assertNull(repository.state.value.waypoint)
        model.actions.confirmNavigation()
        runCurrent()
        assertEquals("rest-2", repository.state.value.waypoint!!.id)
        model.actions.selectScenario(DemoScenario.APPROACHING)
        runCurrent()
        assertTrue(model.state.value.evaluation.arrivalDue)
        assertEquals(CareFeature.ARRIVAL, model.state.value.evaluation.primary.feature)
        model.actions.park()
        model.actions.executeCommand(VoiceCommand.ARRIVAL)
        runCurrent()
        assertTrue(model.state.value.voiceResponse.contains("동쪽 무단차"))
        assertEquals(RoutePreference.COMFORTABLE, model.state.value.snapshot.routeOption)
    }

    @Test fun realModeRejectsAllNewNavigationFlowsEvenWhenUxAllowsInteraction() = runTest(dispatcher) {
        var writes = 0
        val adapter = object : MobilityRepository by repository {
            override val state = MutableStateFlow(MobilitySnapshot(demo = false, uxAllowsInteraction = true))
            override suspend fun requestDestination(destinationId: String): ActionResult { writes++; return ActionResult(true, "unexpected") }
            override suspend fun requestRouteOption(option: RoutePreference): ActionResult { writes++; return ActionResult(true, "unexpected") }
            override suspend fun requestWaypoint(placeId: String): ActionResult { writes++; return ActionResult(true, "unexpected") }
        }
        val holder = ModugoStateHolder(adapter, settings, MockVoiceCompanion(), backgroundScope)
        holder.actions.selectDestination("hospital-b")
        holder.actions.confirmNavigation()
        holder.actions.selectRouteOption(RoutePreference.COMFORTABLE)
        holder.actions.confirmNavigation()
        holder.actions.addWaypoint(repository.restPlaces.first())
        holder.actions.confirmNavigation()
        runCurrent()
        assertEquals(0, writes)
        assertNull(holder.state.value.confirmation)
    }

    @Test fun changingRequirementsWarnsAboutSelectedDestinationWithoutRerouting() = runTest(dispatcher) {
        model.actions.updatePreferences(CarePreferences(needsAccessibleEntrance = false))
        model.actions.selectDestination("hospital-a")
        model.actions.confirmNavigation()
        runCurrent()
        model.actions.updatePreferences(CarePreferences(needsAccessibleEntrance = true))
        runCurrent()
        assertEquals("병원 A", repository.state.value.journey!!.destination)
        assertEquals(CareFeature.ARRIVAL, model.state.value.evaluation.primary.feature)
        assertTrue(model.state.value.evaluation.primary.message.contains("조건 미충족"))
    }
}
