package com.example.modugo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modugo.data.MobilityRepository
import com.example.modugo.data.VoiceCompanion
import com.example.modugo.settings.SettingsStore
import com.example.modugo.ui.ModugoStateHolder

class ModugoViewModel(repository: MobilityRepository, settings: SettingsStore, voice: VoiceCompanion) : ViewModel() {
    private val holder = ModugoStateHolder(repository, settings, voice, viewModelScope)
    val state = holder.state
    val actions = holder.actions
    fun start() = holder.start()
    fun stop() = holder.stop()
    override fun onCleared() = holder.stop()
}
