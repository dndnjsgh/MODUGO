package com.example.modugo.settings

import com.example.modugo.domain.CarePreferences
import kotlinx.coroutines.flow.StateFlow

interface SettingsStore {
    val preferences: StateFlow<CarePreferences>
    fun update(preferences: CarePreferences)
}
