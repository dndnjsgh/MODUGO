package com.example.modugo.mock

import com.example.modugo.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MockMobilityRepositoryTest {
    @Test fun allScenariosAreUsableAndFailureClearsReadings() = runBlocking {
        val repository = MockMobilityRepository()
        repository.requestWaypoint(repository.restPlaces.first().id)
        repository.setScenario(DemoScenario.CONNECTION_FAILED)
        val failed = repository.state.value
        assertNull(failed.journey)
        assertNull(failed.vehicle)
        assertNull(failed.waypoint)
        assertEquals(DataOrigin.UNAVAILABLE, failed.origin)
        assertFalse(failed.canEdit)
        for (scenario in DemoScenario.entries.filter { it != DemoScenario.CONNECTION_FAILED }) {
            repository.setScenario(scenario)
            assertNotNull(repository.state.value.journey)
            assertNotNull(repository.state.value.vehicle)
            assertEquals(DataOrigin.DEMO, repository.state.value.origin)
        }
    }

    @Test fun waypointIsLocalAndBlockedUntilParked() = runBlocking {
        val repository = MockMobilityRepository()
        val place = repository.restPlaces.first()
        repository.setScenario(DemoScenario.STARTED)
        assertFalse(repository.requestWaypoint(place.id).success)
        assertNull(repository.state.value.waypoint)
        repository.park()
        assertFalse(repository.requestWaypoint("unknown").success)
        val result = repository.requestWaypoint(place.id)
        assertTrue(result.success)
        assertTrue(result.message.contains("실제 내비게이션에는 전송하지 않았어요"))
        assertEquals(place, repository.state.value.waypoint)
    }

    @Test fun restResetRetainsTripAndRequiresParking() {
        val repository = MockMobilityRepository()
        repository.setScenario(DemoScenario.REST_DUE, 90)
        assertTrue(CareEngine.evaluate(repository.state.value, CarePreferences(restIntervalMinutes = 90)).restDue)
        assertFalse(repository.completeRest().success)
        repository.park()
        assertTrue(repository.completeRest().success)
        assertEquals(90, repository.state.value.journey?.elapsedMinutes)
        assertEquals(0, repository.state.value.minutesSinceRest)
        assertFalse(CareEngine.evaluate(repository.state.value, CarePreferences()).restDue)
    }

    @Test fun advancingToDestinationDoesNotPretendVehicleIsParked() {
        val repository = MockMobilityRepository()
        repository.setScenario(DemoScenario.APPROACHING)
        repository.advanceMinutes(15)
        val arrived = repository.state.value
        assertTrue(arrived.journey!!.arrived)
        assertEquals(0, arrived.journey.remainingMinutes)
        assertEquals(0.0, arrived.journey.remainingDistanceKm, 0.001)
        assertEquals(80, arrived.journey.elapsedMinutes)
        assertFalse(arrived.canEdit)
        repository.park()
        assertTrue(repository.state.value.canEdit)
    }

    @Test fun invalidTimeAndFailureCannotAdvance() {
        val repository = MockMobilityRepository()
        repository.setScenario(DemoScenario.STARTED)
        val before = repository.state.value
        repository.advanceMinutes(-3)
        assertEquals(before, repository.state.value)
        repository.setScenario(DemoScenario.CONNECTION_FAILED)
        repository.advanceMinutes(10)
        repository.park()
        assertNull(repository.state.value.journey)
        assertNull(repository.state.value.vehicle)
    }

    @Test fun newScenarioClearsPreviousTripSelection() = runBlocking {
        val repository = MockMobilityRepository()
        repository.requestWaypoint(repository.restPlaces.first().id)
        repository.setScenario(DemoScenario.PARKED)
        assertNull(repository.state.value.waypoint)
        assertEquals(0, repository.state.value.minutesSinceRest)
        repository.setScenario(DemoScenario.ARRIVED)
        assertTrue(repository.state.value.canEdit)
        assertTrue(repository.state.value.journey!!.arrived)
    }

    @Test fun everyVoiceButtonHasAResultAndAFeature() {
        val voice = MockVoiceCompanion()
        for (command in VoiceCommand.entries) {
            val reply = voice.respond(command, MobilitySnapshot(), CarePreferences())
            assertTrue(reply.text.isNotBlank())
            assertNotNull(reply.feature)
        }
        val unavailable = MobilitySnapshot(journey = null, vehicle = null, connection = ConnectionStatus.UNAVAILABLE)
        assertTrue(voice.respond(VoiceCommand.REMAINING_TIME, unavailable, CarePreferences()).text.contains("확인할 수 없어요"))
    }
}
