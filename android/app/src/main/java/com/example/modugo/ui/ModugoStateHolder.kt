package com.example.modugo.ui

import com.example.modugo.data.MobilityRepository
import com.example.modugo.data.VoiceCompanion
import com.example.modugo.domain.*
import com.example.modugo.mock.MockMobilityRepository
import com.example.modugo.settings.SettingsStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ModugoStateHolder(
    private val repository: MobilityRepository,
    private val settings: SettingsStore,
    private val voice: VoiceCompanion,
    private val scope: kotlinx.coroutines.CoroutineScope,
    initialScreen: Screen = Screen.HOME,
) {
    private data class Interaction(
        val screen: Screen = Screen.HOME,
        val selectedPlace: RestPlace? = null,
        val voiceResponse: String = "명령 버튼을 선택하면 안내를 글로 보여드려요.",
        val checkedItems: Set<Int> = emptySet(),
        val acknowledged: Boolean = false,
        val message: String? = null,
        val destinationQuery: String = "병원",
        val confirmation: NavigationConfirmation? = null,
    )

    private val interaction = MutableStateFlow(Interaction(screen = initialScreen))
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
            destinationQuery = local.destinationQuery,
            destinations = RecommendationEngine.destinations(repository.destinations.filter {
                it.name.contains(local.destinationQuery.trim(), ignoreCase = true)
            }, prefs),
            restRecommendations = RecommendationEngine.restPlaces(repository.restPlaces, prefs),
            confirmation = local.confirmation.takeIf { snapshot.canEdit },
        )

    val state: StateFlow<ModugoUiState> = combine(repository.state, settings.preferences, interaction, ::render)
        .stateIn(scope, SharingStarted.Eagerly,
            render(repository.state.value, settings.preferences.value, interaction.value))

    init {
        scope.launch {
            repository.state.map { it.canEdit to it.journey?.destination }.distinctUntilChanged().drop(1).collect {
                interaction.update { local -> local.copy(acknowledged = false, checkedItems = emptySet(), selectedPlace = null, confirmation = null) }
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
        searchDestinations = { query -> if (mayEdit()) interaction.update { it.copy(destinationQuery = query.take(80)) } },
        selectDestination = { requestNavigation(NavigationRequest.Destination(it)) },
        selectRouteOption = { requestNavigation(NavigationRequest.RouteOption(it)) },
        confirmNavigation = ::confirmNavigation,
        cancelNavigation = { interaction.update { it.copy(confirmation = null) } },
    )

    fun start() = repository.start()
    fun stop() = repository.stop()

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
            voiceResponse = "명령 버튼을 선택하면 현재 데모 상황을 안내해요.", message = null, confirmation = null,
            destinationQuery = "병원", screen = if (scenario == DemoScenario.HOSPITAL_COMPARISON) Screen.ARRIVAL else it.screen) }
    }

    private fun updatePreferences(prefs: CarePreferences) {
        if (mayEdit()) {
            interaction.update { it.copy(confirmation = null) }
            settings.update(prefs.normalized())
        }
    }

    private fun selectPlace(place: RestPlace?) {
        if (place == null || mayEdit()) {
            val knownPlace = place?.let { selected -> repository.restPlaces.find { it.id == selected.id } }
            interaction.update { it.copy(selectedPlace = knownPlace) }
        }
    }

    private fun addWaypoint(place: RestPlace) {
        requestNavigation(NavigationRequest.Waypoint(place.id))
    }

    private fun confirmationFor(request: NavigationRequest): NavigationConfirmation? {
        val snapshot = repository.state.value
        if (!snapshot.demo || !snapshot.canEdit) return null
        val prefs = settings.preferences.value
        val confirmation = when (request) {
            is NavigationRequest.Destination -> {
                val result = RecommendationEngine.destinations(repository.destinations, prefs)
                    .find { it.place.id == request.id && it.selectable } ?: return null
                NavigationConfirmation(request, "${result.place.name}을 목적지로 선택할까요?",
                    result.reason + "\n가상 병원이며 실제 시설 검증 결과가 아니에요. 기존 데모 경유지와 경로 옵션은 초기화돼요.")
            }
            is NavigationRequest.Waypoint -> {
                if (snapshot.journey == null || snapshot.journey.arrived) return null
                val result = RecommendationEngine.restPlaces(repository.restPlaces, prefs)
                    .find { it.place.id == request.id && it.selectable } ?: return null
                NavigationConfirmation(request, "${result.place.name}을 경유지로 추가할까요?",
                    result.reason + "\n현재 데모 경유지를 이 장소로 바꿔요. 실제 내비게이션에는 전송하지 않아요.")
            }
            is NavigationRequest.RouteOption -> {
                if (snapshot.journey == null || snapshot.journey.arrived || snapshot.routeOption == request.option) return null
                NavigationConfirmation(request, "${request.option.label}로 변경할까요?",
                    "${snapshot.journey.destination} · ${snapshot.routeOption.label} → ${request.option.label}\nMock 상태만 변경해요. 실제 도로 진동·급커브·승차감이나 멀미 감소 효과를 분석한 결과가 아니에요.")
            }
        }
        return confirmation.copy(routeContext = snapshot.journey, preferences = prefs)
    }

    private fun requestNavigation(request: NavigationRequest) {
        if (!mayEdit()) return
        val confirmation = confirmationFor(request)
        interaction.update { it.copy(confirmation = confirmation, selectedPlace = null,
            message = if (confirmation == null) "필수 조건과 이동할 목적지를 확인해 주세요. 실제 경로 변경은 지원하지 않아요." else null) }
    }

    private fun confirmNavigation() {
        val pending = interaction.value.confirmation ?: return
        interaction.update { it.copy(confirmation = null) }
        if (!mayEdit() || confirmationFor(pending.request) == null) return
        scope.launch {
            // 실행 대기 중 차량·경로 상태가 바뀌었으면 기존 확인을 재사용하지 않는다.
            if (confirmationFor(pending.request) != pending) {
                message("이동 상태나 설정이 바뀌었어요. 변경 내용을 다시 확인해 주세요.")
                return@launch
            }
            val result = when (val request = pending.request) {
                is NavigationRequest.Destination -> repository.requestDestination(request.id)
                is NavigationRequest.Waypoint -> repository.requestWaypoint(request.id)
                is NavigationRequest.RouteOption -> repository.requestRouteOption(request.option)
            }
            interaction.update { it.copy(message = result.message,
                screen = if (result.success && pending.request is NavigationRequest.Destination) Screen.COMFORT else it.screen) }
        }
    }

    private fun executeCommand(command: VoiceCommand) {
        val reply = voice.respond(command, repository.state.value, settings.preferences.value)
        interaction.update { it.copy(voiceResponse = reply.text,
            screen = reply.feature?.screen() ?: Screen.VOICE, selectedPlace = null, message = reply.text) }
        if (command == VoiceCommand.COMFORTABLE_ROUTE) requestNavigation(NavigationRequest.RouteOption(RoutePreference.COMFORTABLE))
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
