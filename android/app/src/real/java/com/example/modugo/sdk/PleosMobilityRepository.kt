package com.example.modugo.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import com.example.modugo.data.MobilityRepository
import com.example.modugo.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** SDK 자원을 활성 화면 세션에 묶고 종료된 세션의 지연 콜백을 무시한다. */
class PleosMobilityRepository(context: Context) : MobilityRepository {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0
    private var active = false
    private var ux: PlatformUx? = null
    private var uxAllowed = false
    private var navi: NaviHelper? = null
    private var vehicle: Vehicle? = null
    private var listener: NaviHelperEventListener? = null
    private var navConnected = false
    private var journey: Journey? = null
    private var temperature: Int? = null
    private var power: Boolean? = null
    private var tripMinutes: Int? = null
    private var tripKm: Double? = null
    private var lastRestMinutes = 0
    private var tripTime = 0L
    private var distanceTime = 0L
    private var navTime = 0L
    private var temperatureTime = 0L
    private var powerTime = 0L
    private var navMessage = "연결 대기"
    private var temperatureMessage = "조회 대기"
    private var powerMessage = "조회 대기"
    private val area = SeatArea(SeatValue.ROW_1_LEFT)
    private val mutableState = MutableStateFlow(emptyState())
    override val state = mutableState.asStateFlow()
    override val restPlaces = emptyList<RestPlace>()

    private fun emptyState() = MobilitySnapshot(demo = false, origin = DataOrigin.UNAVAILABLE,
        connection = ConnectionStatus.UNAVAILABLE, journey = null, vehicle = null,
        diagnostics = "SDK 연결 대기 · 정차 확인과 실제 주행 규제 연동 전에는 상세 조작이 잠깁니다.")

    private fun publish() {
        val received = journey != null || temperature != null || power != null
        mutableState.value = emptyState().copy(
            origin = if (received) DataOrigin.REAL else DataOrigin.UNAVAILABLE,
            connection = if (received) ConnectionStatus.REAL_CONNECTED else ConnectionStatus.UNAVAILABLE,
            journey = journey?.copy(elapsedMinutes = tripMinutes ?: 0, distanceTravelledKm = tripKm ?: 0.0),
            tripTimeAvailable = tripMinutes != null,
            tripDistanceAvailable = tripKm != null,
            minutesSinceRest = ((tripMinutes ?: 0) - lastRestMinutes).coerceAtLeast(0),
            uxAllowsInteraction = uxAllowed,
            vehicle = VehicleState(MotionState.UNKNOWN, temperature, power),
            diagnostics = "내비: $navMessage\n온도: $temperatureMessage · 공조: $powerMessage\n주행 시간: ${tripMinutes?.let { "${it}분 수신" } ?: "미수신/권한·지원 확인 필요"}\n주행 규제: ${if (uxAllowed) "조작 허용" else "제한 또는 미확인"}. 재연결하려면 앱을 닫고 다시 여세요.")
    }

    private fun accept(session: Int, block: () -> Unit) {
        handler.post {
            if (active && generation == session) {
                block()
                publish()
            }
        }
    }

    override fun start() {
        if (active) return
        active = true
        val session = ++generation
        try { ux = PlatformUx(context) { allowed -> accept(session) { uxAllowed = allowed } }.also { it.start() } }
        catch (_: Exception) { uxAllowed = false }
        catch (_: LinkageError) { uxAllowed = false }
        navMessage = "연결 중"
        temperatureMessage = "조회 대기"
        powerMessage = "조회 대기"
        publish()
        try {
            val client = NaviHelper(context)
            navi = client
            val events = object : NaviHelperEventListener {
                override fun onDestinationInfo(destinationInfo: DestinationInfo) = accept(session) {
                    val arrived = journey?.arrived == true && journey?.destination == destinationInfo.name
                    journey = RouteReading.toJourney(destinationInfo.name, destinationInfo.remainDistance, destinationInfo.remainTime)?.let { if (arrived) it.copy(arrived = true, remainingDistanceKm = 0.0, remainingMinutes = 0) else it }
                    navTime = SystemClock.elapsedRealtime()
                    navMessage = if (journey == null) "유효한 목적지 정보 없음" else "경로 수신"
                }
                override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = accept(session) {
                    journey = journey?.copy(remainingDistanceKm = 0.0, remainingMinutes = 0, arrived = true)
                    navTime = SystemClock.elapsedRealtime()
                }
                override fun onDestinationChanged() = accept(session) { journey = null; navMessage = "목적지 변경 · 갱신 대기" }
                override fun onRouteCancelled() = accept(session) { journey = null; navMessage = "경로 취소" }
                override fun onNavigationStatus(status: NavigationStatus) = accept(session) {
                    if (status != NavigationStatus.ROUTE_DRIVE) { journey = null; navMessage = "안내 중인 경로 없음" }
                }
                override fun onError(errorCode: NaviErrorCode) = accept(session) { journey = null; navMessage = "오류: $errorCode" }
            }
            listener = events
            client.initialize(onConnected = { accept(session) {
                try { client.addListener(events); navConnected = true; navMessage = "연결됨 · 경로 대기" }
                catch (e: Exception) { navMessage = "리스너 등록 실패: ${e.javaClass.simpleName}" }
            } }, onFailed = { error -> accept(session) { navConnected = false; journey = null; navMessage = "연결 실패: ${error.javaClass.simpleName}" } })
        } catch (e: Exception) { navMessage = "초기화 실패: ${e.javaClass.simpleName}" }
        try { vehicle = Vehicle(context).also { it.initialize() } }
        catch (e: Exception) { temperatureMessage = "초기화 실패: ${e.javaClass.simpleName}"; powerMessage = temperatureMessage }
        publish()
        handler.post(object : Runnable {
            override fun run() {
                if (!active || session != generation) return
                refresh(session)
                handler.postDelayed(this, 5_000)
            }
        })
    }

    private fun refresh(session: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - tripTime > 15_000) tripMinutes = null
        if (now - distanceTime > 15_000) tripKm = null
        if (now - navTime > 15_000) { journey = null; if (navConnected) navMessage = "경로 응답 대기 / 유효 시간 만료" }
        if (now - temperatureTime > 15_000) temperature = null
        if (now - powerTime > 15_000) power = null
        try { if (navConnected) navi?.getDestinationInfo() }
        catch (e: Exception) { journey = null; navMessage = "조회 실패: ${e.javaClass.simpleName}" }
        val odometer = try { vehicle?.getOdometer() } catch (_: Exception) { null }
        if (odometer != null) {
            try {
                odometer.checkTripCurrentDrivingTimeCapabilities(onComplete = { config -> accept(session) {
                    if (config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE)) {
                        try { odometer.getTripCurrentDrivingTime(onComplete = { value -> accept(session) {
                            val valid = value?.takeIf { it >= 0 }
                            if (valid != null && (valid < (tripMinutes ?: 0) || valid < lastRestMinutes)) lastRestMinutes = 0
                            tripMinutes = valid; tripTime = SystemClock.elapsedRealtime()
                        } }, onFailed = { accept(session) { tripMinutes = null } }) }
                        catch (_: Exception) { tripMinutes = null }
                    } else tripMinutes = null
                } }, onFailed = { accept(session) { tripMinutes = null } })
            } catch (_: Exception) { tripMinutes = null }
            try {
                odometer.checkTripCurrentDrivingDistanceCapabilities(onComplete = { config -> accept(session) {
                    if (config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE)) {
                        try { odometer.getTripCurrentDrivingDistance(onComplete = { value -> accept(session) {
                            tripKm = value?.takeIf { it.isFinite() && it >= 0 }?.toDouble()?.div(1000)
                            distanceTime = SystemClock.elapsedRealtime()
                        } }, onFailed = { accept(session) { tripKm = null } }) }
                        catch (_: Exception) { tripKm = null }
                    } else tripKm = null
                } }, onFailed = { accept(session) { tripKm = null } })
            } catch (_: Exception) { tripKm = null }
        }
        val hvac = try { vehicle?.getHVAC() } catch (_: Exception) { null }
        if (hvac != null) {
            try {
                hvac.checkHvacTemperatureCurrentCapabilities(onComplete = { config, areas -> accept(session) {
                    if (config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE) && area in areas) {
                        try { hvac.getHvacTemperatureCurrent(area, TemperatureUnit.CELSIUS,
                            onComplete = { value -> accept(session) { temperature = value?.takeIf { it.isFinite() }?.roundToInt(); temperatureTime = SystemClock.elapsedRealtime(); temperatureMessage = if (temperature == null) "값 없음" else "수신" } },
                            onFailed = { error -> accept(session) { temperature = null; temperatureMessage = "조회 실패: ${error.javaClass.simpleName}" } }) }
                        catch (e: Exception) { temperature = null; temperatureMessage = "조회 실패: ${e.javaClass.simpleName}" }
                    } else { temperature = null; temperatureMessage = "읽기 권한/영역 미지원" }
                } }, onFailed = { error -> accept(session) { temperature = null; temperatureMessage = "지원 확인 실패: ${error.javaClass.simpleName}" } })
            } catch (e: Exception) { temperature = null; temperatureMessage = "지원 확인 실패: ${e.javaClass.simpleName}" }
            try {
                hvac.checkHvacPowerCapabilities(onComplete = { config, areas -> accept(session) {
                    if (config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE) && area in areas) {
                        try { hvac.getHvacPowerState(area,
                            onComplete = { value -> accept(session) { power = value; powerTime = SystemClock.elapsedRealtime(); powerMessage = if (value == null) "값 없음" else "수신" } },
                            onFailed = { error -> accept(session) { power = null; powerMessage = "조회 실패: ${error.javaClass.simpleName}" } }) }
                        catch (e: Exception) { power = null; powerMessage = "조회 실패: ${e.javaClass.simpleName}" }
                    } else { power = null; powerMessage = "읽기 권한/영역 미지원" }
                } }, onFailed = { error -> accept(session) { power = null; powerMessage = "지원 확인 실패: ${error.javaClass.simpleName}" } })
            } catch (e: Exception) { power = null; powerMessage = "지원 확인 실패: ${e.javaClass.simpleName}" }
        }
        publish()
    }

    override fun stop() {
        active = false
        generation++
        handler.removeCallbacksAndMessages(null)
        ux?.stop(); ux = null; uxAllowed = false
        try { listener?.let { navi?.removeListener(it) } } catch (_: Exception) { }
        try { navi?.release() } catch (_: Exception) { }
        try { vehicle?.release() } catch (_: Exception) { }
        navi = null; vehicle = null; listener = null; navConnected = false
        journey = null; temperature = null; power = null; tripMinutes = null; tripKm = null
        mutableState.value = emptyState()
    }

    override fun completeRest(): ActionResult {
        val current = tripMinutes
        if (!uxAllowed || current == null) return ActionResult(false, "주행 규제 또는 주행 시간 미수신으로 기록할 수 없어요.")
        lastRestMinutes = current
        publish()
        return ActionResult(true, "휴식을 기록했어요. 실제 주행 시간을 기준으로 다음 휴식을 계산해요.")
    }

    override suspend fun requestWaypoint(placeId: String) = ActionResult(false,
        "실제 위치가 검증된 장소 데이터가 없습니다. 샘플 장소를 내비게이션에 전송하지 않습니다.")
}
