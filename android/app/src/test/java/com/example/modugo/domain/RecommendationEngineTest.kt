package com.example.modugo.domain

import com.example.modugo.mock.MockMobilityRepository
import org.junit.Assert.*
import org.junit.Test

class RecommendationEngineTest {
    private val repository = MockMobilityRepository()
    private val preferences = CarePreferences()

    @Test fun requiredEntranceOutranksShorterTravelTime() {
        val results = RecommendationEngine.destinations(repository.destinations, preferences)
        assertEquals("hospital-b", results.first().place.id)
        assertTrue(results.first().recommended)
        assertEquals(15, results.first().place.minutes)
        val unmet = results.first { it.place.id == "hospital-a" }
        assertFalse(unmet.selectable)
        assertFalse(unmet.recommended)
    }

    @Test fun unknownNeverMeansConfirmedOrUnusable() {
        val unknown = RecommendationEngine.destinations(repository.destinations, preferences).first { it.place.id == "hospital-c" }
        assertEquals(AccessibilityStatus.UNKNOWN, unknown.requiredStatus)
        assertFalse(unknown.recommended)
        assertTrue(unknown.selectable)
        assertTrue(unknown.reason.contains("별도 확인"))
        val onlyUnknown = RecommendationEngine.destinations(listOf(unknown.place), preferences)
        assertFalse(onlyUnknown.first().recommended)
    }

    @Test fun turningOffRequirementChangesRankingAndExplanationWithoutRelabelingAccess() {
        val result = RecommendationEngine.destinations(repository.destinations, preferences.copy(needsAccessibleEntrance = false)).first()
        assertEquals("hospital-c", result.place.id)
        assertNull(result.requiredStatus)
        assertEquals(AccessibilityStatus.UNKNOWN, result.place.entrance)
        assertTrue(result.reason.contains("접근성 확인을 뜻하지"))
    }

    @Test fun qualifiedDestinationsAreComparedByTimeThenDistance() {
        val base = repository.destinations.first { it.id == "hospital-b" }
        val results = RecommendationEngine.destinations(listOf(base, base.copy(id = "shorter", distanceKm = 5.5)), preferences)
        assertEquals("shorter", results.first().place.id)
    }

    @Test fun restFacilitiesOutrankProximityAndPreferenceChangesRecommendation() {
        val enabled = RecommendationEngine.restPlaces(repository.restPlaces, preferences)
        val disabled = RecommendationEngine.restPlaces(repository.restPlaces, preferences.copy(preferRestAmenities = false))
        assertEquals("rest-2", enabled.first().place.id)
        assertEquals("rest-4", disabled.first().place.id)
        assertNotEquals(enabled.first().reason, disabled.first().reason)
    }

    @Test fun restRankingUsesRouteAndAddedTravelAmongEquivalentFacilities() {
        val rest = repository.restPlaces.first { it.id == "rest-2" }
        val alternatives = listOf(rest.copy(id = "detour", extraMinutes = 9, distanceKm = 1.0, onRoute = false), rest)
        assertEquals(rest.id, RecommendationEngine.restPlaces(alternatives, preferences).first().place.id)
        assertTrue(RecommendationEngine.restPlaces(alternatives, preferences).last().reason.contains("우회 필요"))
    }

    @Test fun arrivalUsesSelectedDestinationAndNeverLeaksDemoFacilitiesIntoRealMode() {
        val b = repository.destinations.first { it.id == "hospital-b" }
        val snapshot = MobilitySnapshot(journey = Journey(destination = b.name), selectedDestination = b)
        assertTrue(RecommendationEngine.arrivalMessage(snapshot).contains("동쪽 무단차"))
        assertTrue(RecommendationEngine.arrivalMessage(snapshot).contains("지하 1층"))
        assertFalse(RecommendationEngine.arrivalMessage(snapshot.copy(demo = false)).contains("동쪽"))
        assertFalse(RecommendationEngine.arrivalMessage(snapshot.copy(journey = Journey(destination = "다른 목적지"))).contains("동쪽"))
    }
}
