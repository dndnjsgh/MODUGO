package com.example.modugo.settings

import android.content.Context
import com.example.modugo.domain.CarePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesSettingsStore(context: Context) : SettingsStore {
    private val storage = context.applicationContext.getSharedPreferences("modugo_care", Context.MODE_PRIVATE)
    private val mutablePreferences = MutableStateFlow(read())
    override val preferences = mutablePreferences.asStateFlow()

    private fun read(): CarePreferences = try {
        CarePreferences(
            careEnabled = storage.getBoolean("care_enabled", true),
            motionCareEnabled = storage.getBoolean("motion_care", false),
            restIntervalMinutes = storage.getInt("rest_interval", 60),
            preferredTemperatureC = storage.getInt("preferred_temperature", 22),
        ).normalized()
    } catch (_: ClassCastException) {
        CarePreferences()
    }

    override fun update(preferences: CarePreferences) {
        val normalized = preferences.normalized()
        storage.edit()
            .putBoolean("care_enabled", normalized.careEnabled)
            .putBoolean("motion_care", normalized.motionCareEnabled)
            .putInt("rest_interval", normalized.restIntervalMinutes)
            .putInt("preferred_temperature", normalized.preferredTemperatureC)
            .apply()
        mutablePreferences.value = normalized
    }
}
