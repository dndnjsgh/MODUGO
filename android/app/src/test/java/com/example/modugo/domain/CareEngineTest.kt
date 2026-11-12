package com.example.modugo.domain

import org.junit.Assert.*
import org.junit.Test

class CareEngineTest {
    private val preferences = CarePreferences(restIntervalMinutes = 60)
    private fun moving(minutes: Int) = MobilitySnapshot(
        journey = Journey(elapsedMinutes = minutes),
        vehicle = VehicleState(MotionState.DRIVING),
        minutesSinceRest = minutes,
    )

    @Test fun restRecommendationStartsAtTheConfiguredBoundary() {
        assertFalse(CareEngine.evaluate(moving(59), preferences).restDue)
        assertEquals(1, CareEngine.evaluate(moving(59), preferences).restInMinutes)
        assertTrue(CareEngine.evaluate(moving(60), preferences).restDue)
        assertEquals(0, CareEngine.evaluate(moving(85), preferences).restInMinutes)
    }

    @Test fun disabledCareSuppressesRestAndArrivalRecommendations() {
        val result = CareEngine.evaluate(moving(90).copy(journey = Journey(remainingMinutes = 3)), preferences.copy(careEnabled = false))
        assertFalse(result.restDue)
        assertFalse(result.arrivalDue)
        assertEquals(CareFeature.COMFORT, result.primary.feature)
    }

    @Test fun arrivalHasPriorityWhenRestIsAlsoDue() {
        val result = CareEngine.evaluate(moving(90).copy(journey = Journey(remainingMinutes = 4)), preferences)
        assertTrue(result.restDue)
        assertTrue(result.arrivalDue)
        assertEquals(CareFeature.ARRIVAL, result.primary.feature)
    }

    @Test fun distanceCanTriggerArrivalAndArrivalDoesNotTriggerRest() {
        assertTrue(CareEngine.evaluate(moving(10).copy(journey = Journey(remainingDistanceKm = 2.0)), preferences).arrivalDue)
        val result = CareEngine.evaluate(moving(90).copy(journey = Journey(arrived = true)), preferences)
        assertFalse(result.restDue)
        assertFalse(result.arrivalDue)
        assertEquals(CareFeature.ARRIVAL, result.primary.feature)
    }

    @Test fun connectionFailureNeverSuggestsTravelRecommendationsFromStaleValues() {
        val snapshot = moving(90).copy(connection = ConnectionStatus.UNAVAILABLE)
        val result = CareEngine.evaluate(snapshot, preferences)
        assertFalse(snapshot.canEdit)
        assertFalse(result.restDue)
        assertFalse(result.arrivalDue)
        assertNull(result.restInMinutes)
        assertTrue(result.journeyMessage.contains("확인할 수 없어요"))
    }

    @Test fun unknownVehicleStateBlocksEditing() {
        assertFalse(MobilitySnapshot(vehicle = null).canEdit)
        assertFalse(MobilitySnapshot(vehicle = VehicleState(MotionState.UNKNOWN)).canEdit)
        assertFalse(moving(1).canEdit)
        assertTrue(MobilitySnapshot().canEdit)
    }

    @Test fun missingTemperatureIsExplicitAndSettingsStayInRange() {
        val settings = CarePreferences(restIntervalMinutes = -1, preferredTemperatureC = 100).normalized()
        assertEquals(30, settings.restIntervalMinutes)
        assertEquals(28, settings.preferredTemperatureC)
        val snapshot = MobilitySnapshot(vehicle = VehicleState(cabinTemperatureC = null))
        assertTrue(CareEngine.evaluate(snapshot, settings).comfortMessage.contains("정보가 없어요"))
    }

    @Test fun motionCareUsesPreferenceAndTimeWithoutClaimingDetection() {
        val enabled = preferences.copy(motionCareEnabled = true)
        assertTrue(CareEngine.evaluate(moving(30), enabled).comfortMessage.contains("휴식과 환기"))
        assertFalse(CareEngine.evaluate(moving(29), enabled).comfortMessage.contains("휴식과 환기"))
        assertFalse(CareEngine.evaluate(moving(30), preferences).comfortMessage.contains("휴식과 환기"))
    }
}
