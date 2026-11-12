package com.example.modugo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modugo.data.MobilityRepository
import com.example.modugo.data.VoiceCompanion
import com.example.modugo.domain.*
import com.example.modugo.mock.MockMobilityRepository
import com.example.modugo.settings.SettingsStore
import com.example.modugo.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ModugoViewModel(
    private val repository: MobilityRepository,
    private val settings: SettingsStore,
    private val voice: VoiceCompanion,
) : ViewModel() {
    private data class Interaction(
        val screen: Screen = Screen.HOME,
        val selectedPlace: RestPlace? = null,
        val voiceResponse: String = "명령 버튼을 선택하면 안내를 글로 보여드려요.",
        val checkedItems: Set<Int> = emptySet(),
        val acknowledged: Boolean = false,
        val message: String? = null,
    )

    private val interaction = MutableStateFlow(Interaction())
    private fun render(snapshot: MobilitySnapshot, prefs: CarePreferences, local: Interaction) =
        ModugoUiState(
            snapshot = snapshot,
            preferences = prefs,
            evaluation = CareEngine.evaluate(snapshot, prefs),
            restPlaces = repository.restPlaces,
            screen = local.screen,
            selectedPlace = local.selectedPlace.takeIf { snapshot.canEdit },
            voiceResponse = local.voiceResponse,
            checkedArrivalItems = local.checkedItems,
            arrivalAcknowledged = local.acknowledged,
            message = local.message,
        )

    val state: StateFlow<ModugoUiState> = combine(repository.state, settings.preferences, interaction, ::render)
        .stateIn(viewModelScope, SharingStarted.Eagerly,
            render(repository.state.value, settings.preferences.value, interaction.value))

    init {
        viewModelScope.launch {
            repository.state.map { it.journey?.destination }.distinctUntilChanged().drop(1).collect {
                interaction.update { local -> local.copy(acknowledged = false, checkedItems = emptySet(), selectedPlace = null) }
            }
        }
    }

    val actions = ModugoActions(
        navigate = ::navigate,
        selectScenario = ::selectScenario,
        updatePreferences = ::updatePreferences,
        selectPlace = ::selectPlace,
        addWaypoint = ::addWaypoint,
        executeCommand = ::executeCommand,
        toggleArrivalItem = ::toggleArrivalItem,
        acknowledgeArrival = ::acknowledgeArrival,
        dismissMessage = { interaction.update { it.copy(message = null) } },
        advanceMinutes = { mock()?.advanceMinutes(10) },
        park = { mock()?.park() },
        completeRest = { if (mayEdit()) message(repository.completeRest().message) },
    )

    fun start() = repository.start()
    fun stop() = repository.stop()
    override fun onCleared() { repository.stop() }

    private fun mock(): MockMobilityRepository? = repository as? MockMobilityRepository
    private fun message(text: String) { interaction.update { it.copy(message = text) } }
    private fun mayEdit(): Boolean {
        if (repository.state.value.canEdit) return true
        message("정차 상태가 확인된 뒤 이용할 수 있어요.")
        return false
    }

    private fun navigate(screen: Screen) {
        interaction.update { it.copy(screen = screen, selectedPlace = null, message = null) }
    }

    private fun selectScenario(scenario: DemoScenario) {
        mock()?.setScenario(scenario, settings.preferences.value.restIntervalMinutes)
        interaction.update { it.copy(selectedPlace = null, checkedItems = emptySet(), acknowledged = false,
            voiceResponse = "명령 버튼을 선택하면 현재 데모 상황을 안내해요.", message = null) }
    }

    private fun updatePreferences(prefs: CarePreferences) {
        if (mayEdit()) settings.update(prefs.normalized())
    }

    private fun selectPlace(place: RestPlace?) {
        if (place == null || mayEdit()) {
            val knownPlace = place?.let { selected -> repository.restPlaces.find { it.id == selected.id } }
            interaction.update { it.copy(selectedPlace = knownPlace) }
        }
    }

    private fun addWaypoint(place: RestPlace) {
        if (!mayEdit()) return
        viewModelScope.launch {
            val result = repository.requestWaypoint(place.id)
            interaction.update { it.copy(message = result.message, selectedPlace = null) }
        }
    }

    private fun executeCommand(command: VoiceCommand) {
        val reply = voice.respond(command, repository.state.value, settings.preferences.value)
        interaction.update { it.copy(voiceResponse = reply.text,
            screen = reply.feature?.screen() ?: Screen.VOICE, selectedPlace = null, message = reply.text) }
    }

    private fun toggleArrivalItem(index: Int) {
        if (index !in 0..2 || !mayEdit()) return
        interaction.update { it.copy(checkedItems = if (index in it.checkedItems) it.checkedItems - index else it.checkedItems + index) }
    }

    private fun acknowledgeArrival() {
        if (!mayEdit()) return
        val snapshot = repository.state.value
        if (!CareEngine.evaluate(snapshot, settings.preferences.value).arrivalDue && snapshot.journey?.arrived != true) {
            message("목적지에 가까워지면 도착 안내를 확인할 수 있어요.")
            return
        }
        interaction.update { it.copy(acknowledged = true, message = "도착 안내를 확인했어요. 주변을 살핀 뒤 안전하게 하차하세요.") }
    }
}
