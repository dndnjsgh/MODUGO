package com.example.modugo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.modugo.data.createMobilityRepository
import com.example.modugo.mock.MockVoiceCompanion
import com.example.modugo.settings.SharedPreferencesSettingsStore
import com.example.modugo.ui.ModugoApp
import com.example.modugo.ui.Screen
import com.example.modugo.ui.theme.MODUGOTheme
import com.example.modugo.viewmodel.ModugoViewModel

class MainActivity : ComponentActivity() {
    private val model by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ModugoViewModel(
                createMobilityRepository(applicationContext), SharedPreferencesSettingsStore(applicationContext), MockVoiceCompanion(),
            ) as T
        })[ModugoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            BackHandler(state.screen != Screen.HOME || state.selectedPlace != null) {
                if (state.selectedPlace != null) model.actions.selectPlace(null)
                else model.actions.navigate(Screen.HOME)
            }
            MODUGOTheme { ModugoApp(state, model.actions) }
        }
    }

    override fun onStart() { super.onStart(); model.start() }
    override fun onStop() { model.stop(); super.onStop() }
}
