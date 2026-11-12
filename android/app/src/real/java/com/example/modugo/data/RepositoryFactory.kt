package com.example.modugo.data

import android.content.Context

fun createMobilityRepository(context: Context): MobilityRepository = com.example.modugo.sdk.PleosMobilityRepository(context)
