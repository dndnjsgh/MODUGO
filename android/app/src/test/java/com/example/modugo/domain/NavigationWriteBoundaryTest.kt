package com.example.modugo.domain

import com.example.modugo.data.MobilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NavigationWriteBoundaryTest {
    @Test fun readOnlyAdapterRejectsNewNavigationWritesByDefault() = runBlocking {
        val adapter = object : MobilityRepository {
            override val state = MutableStateFlow(MobilitySnapshot(demo = false))
            override val restPlaces = emptyList<RestPlace>()
            override fun start() = Unit
            override fun stop() = Unit
            override fun completeRest() = ActionResult(false, "unsupported")
            override suspend fun requestWaypoint(placeId: String) = ActionResult(false, "unsupported")
        }
        val before = adapter.state.value
        assertTrue(adapter.destinations.isEmpty())
        assertFalse(adapter.requestDestination("hospital-b").success)
        assertFalse(adapter.requestRouteOption(RoutePreference.COMFORTABLE).success)
        assertEquals(before, adapter.state.value)
    }
}
