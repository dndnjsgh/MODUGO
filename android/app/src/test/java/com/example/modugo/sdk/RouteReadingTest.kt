package com.example.modugo.sdk
import org.junit.Test
import org.junit.Assert.*
import com.example.modugo.domain.*

class RouteReadingTest {
    @Test fun unitsAreConvertedAndPartialMinutesRoundUp() {
        val journey = RouteReading.toJourney("목적지", 1250, 61)!!
        assertEquals(1.25, journey.remainingDistanceKm, 0.001)
        assertEquals(2, journey.remainingMinutes)
    }
    @Test fun invalidReadingsNeverBecomeArrival() {
        assertNull(RouteReading.toJourney("", 0, 0))
        assertNull(RouteReading.toJourney("목적지", -1, 3))
        assertNull(RouteReading.toJourney("목적지", 3, -1))
        assertFalse(RouteReading.toJourney("목적지", 0, 0)!!.arrived)
        assertTrue(RouteReading.toJourney("목적지", 1, Int.MAX_VALUE)!!.remainingMinutes > 0)
    }
    @Test fun realModeNeverFabricatesRestTimerOrParking() {
        val state = MobilitySnapshot(demo = false, origin = DataOrigin.REAL,
            connection = ConnectionStatus.REAL_CONNECTED,
            journey = RouteReading.toJourney("목적지", 3000, 900),
            vehicle = VehicleState(MotionState.UNKNOWN, null, null), minutesSinceRest = 90)
        assertFalse(state.canEdit)
        val care = CareEngine.evaluate(state, CarePreferences())
        assertNull(care.restInMinutes)
        assertFalse(care.restDue)
    }
    @Test fun realTripTimeEnablesRestOnlyWithValidReadingAndUxDoesNotInventMotion() {
        val state = MobilitySnapshot(demo = false, tripTimeAvailable = true,
            connection = ConnectionStatus.REAL_CONNECTED, origin = DataOrigin.REAL,
            journey = RouteReading.toJourney("목적지", 3000, 900), minutesSinceRest = 60,
            vehicle = VehicleState(MotionState.UNKNOWN, null, null))
        assertTrue(CareEngine.evaluate(state, CarePreferences()).restDue)
        assertFalse(CareEngine.evaluate(state.copy(tripTimeAvailable = false), CarePreferences()).restDue)
        assertFalse(state.canEdit)
        assertTrue(state.copy(uxAllowsInteraction = true).canEdit)
        assertFalse(state.copy(uxAllowsInteraction = true).isDriving)
    }
}
