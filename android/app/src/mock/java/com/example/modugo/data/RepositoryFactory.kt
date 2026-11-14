package com.example.modugo.data

import android.content.Context

fun createMobilityRepository(context: Context): MobilityRepository = com.example.modugo.mock.MockMobilityRepository().apply {
    setScenario(com.example.modugo.domain.DemoScenario.HOSPITAL_COMPARISON)
}
