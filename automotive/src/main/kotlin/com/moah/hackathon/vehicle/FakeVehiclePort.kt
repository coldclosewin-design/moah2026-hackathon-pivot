package com.moah.hackathon.vehicle

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mobis.vss.VssConstants
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

/**
 * 외부 PC에서 "실제로 도는" in-memory 구현. setVSS 흉내(store 쓰기 + emit)와 두 가지 신호 시뮬레이션으로 UI 를 검증한다.
 *
 *  - 기본 시뮬레이션: 속도가 45~95 km/h 사이를 천천히 오르내린다(도로 주행용, 16번 그대로).
 *  - **시나리오 재생** [play]: [Scenario] 타임라인을 [speedFactor] 배로 압축해 주입한다(주차 과제용). 재생 중에는 기본 시뮬레이션이 멈추고,
 *    끝나면 마지막 값을 유지한다.
 *
 * 이 포트가 만든 값은 전부 [SignalAvailability.SIMULATED] 다.
 *
 * @param simulate true 면 생성 직후 기본 시뮬레이션을 돌린다. 테스트에서는 false.
 * @param tickMillis 기본 시뮬레이션 주기.
 */
class FakeVehiclePort(
    private val simulate: Boolean = true,
    private val tickMillis: Long = 500L,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    initial: Map<String, String> = DEFAULTS,
) : VehiclePort {

    private val store = ConcurrentHashMap<String, String>(initial)
    private val changes = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 64)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var simulationJob: Job? = null

    /**
     * 값을 저장·emit 하기 **직전**에 부르는 훅(2026-09-30). [HybridVehiclePort] 가 여기에 "실물에 먼저 쓰기" 를 건다 —
     * 시연 패널·시나리오 조작이 실차의 live 키에도 먹게. Fake 단독이면 null.
     */
    @Volatile
    var writeThrough: (suspend (Map<String, String>) -> Unit)? = null

    private val _playback = MutableStateFlow<ScenarioPlayback?>(null)
    /** 재생 중인 시나리오. null = 기본 시뮬레이션 또는 정지. */
    val playback: StateFlow<ScenarioPlayback?> = _playback

    init {
        if (simulate) resumeDefaultSimulation()
    }

    override suspend fun get(keys: List<String>): Map<String, String> =
        keys.mapNotNull { k -> store[k]?.let { k to it } }.toMap()

    override suspend fun set(values: Map<String, String>): List<String> {
        if (values.isEmpty()) return emptyList()
        writeThrough?.invoke(values)
        store.putAll(values)
        changes.emit(values)
        return emptyList()
    }

    override fun observe(keys: List<String>): Flow<Map<String, String>> = flow {
        val keySet = keys.toSet()
        emit(get(keys))
        changes
            .map { delta -> delta.filterKeys { it in keySet } }
            .filter { it.isNotEmpty() }
            .collect { emit(it) }
    }

    override fun dispose() {
        scope.cancel()
    }

    /** 테스트/데모용: 외부에서 sensor 값을 주입한다 (실차라면 앱이 쓸 수 없는 신호). */
    suspend fun inject(values: Map<String, String>) {
        writeThrough?.invoke(values)
        store.putAll(values)
        changes.emit(values)
    }

    /**
     * 시연 조작: null 이면 시뮬레이션·시나리오가 정한 속도, 값이 있으면 그 속도로 고정 (예: 0 = 정차).
     * 다음 tick/스텝에 반영된다. 즉시 반영하려면 [holdSpeed].
     */
    @Volatile
    var speedOverrideKmh: Float? = null

    /** 속도를 고정하고 바로 주입한다. `null` 이면 고정 해제(시뮬레이션이 다음 tick 에 되돌린다). */
    suspend fun holdSpeed(kmh: Float?) {
        speedOverrideKmh = kmh
        if (kmh != null) inject(mapOf(VssConstants.VEHICLE_SPEED to ScenarioBuilder.formatSpeed(kmh.toDouble())))
    }

    /** 시나리오를 처음부터 재생한다. 진행 중이던 시뮬레이션·재생은 중단. */
    fun play(scenario: Scenario, speedFactor: Double = 1.0) {
        require(speedFactor > 0) { "speedFactor must be positive" }
        simulationJob?.cancel()
        _playback.value = ScenarioPlayback(scenario.id, -1, scenario.steps.size)
        simulationJob = scope.launch { runScenario(scenario, speedFactor) }
    }

    /** 재생을 멈추고 마지막 값을 유지한다(기본 시뮬레이션으로 돌아가지 않는다). */
    fun stop() {
        simulationJob?.cancel()
        simulationJob = null
        _playback.value = null
    }

    /** 도로 주행용 기본 시뮬레이션(45~95 km/h)으로 돌아간다. */
    fun resumeDefaultSimulation() {
        simulationJob?.cancel()
        _playback.value = null
        simulationJob = scope.launch { runSimulation() }
    }

    private suspend fun runScenario(scenario: Scenario, speedFactor: Double) {
        var previousAt = 0.0
        scenario.steps.forEachIndexed { index, step ->
            val waitMillis = ((step.atSeconds - previousAt) * 1000.0 / speedFactor).toLong()
            if (waitMillis > 0) delay(waitMillis)
            previousAt = step.atSeconds
            val values = step.values.toMutableMap()
            speedOverrideKmh?.let { held ->
                if (VssConstants.VEHICLE_SPEED in values) values[VssConstants.VEHICLE_SPEED] = ScenarioBuilder.formatSpeed(held.toDouble())
            }
            inject(values)
            _playback.value = ScenarioPlayback(scenario.id, index, scenario.steps.size)
        }
    }

    private suspend fun runSimulation() {
        var t = 0.0
        while (scope.isActive) {
            delay(tickMillis)
            t += tickMillis / 1000.0
            // 45 ~ 95 km/h 사이를 천천히 오르내리는 속도 (override 가 있으면 그 값).
            val speed = speedOverrideKmh?.toDouble() ?: (70 + 25 * sin(t / 12.0))
            inject(mapOf(VssConstants.VEHICLE_SPEED to ScenarioBuilder.formatSpeed(speed)))
        }
    }

    companion object {
        /** 시동 꺼진 채 주차된 차. 가이드 모드의 첫 단계("안전벨트를 매세요")가 여기서 시작한다. */
        val DEFAULTS: Map<String, String> = mapOf(
            VssConstants.VEHICLE_SPEED to "0.0",
            VssConstants.VEHICLE_ADAS_ABS_ISENABLED to VssValues.TRUE,
            VssConstants.VEHICLE_ADAS_CRUISECONTROL_SPEEDSET to "0.0",
            VssConstants.VEHICLE_BODY_HORN_ISACTIVE to VssValues.FALSE,
            VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.FALSE,
            VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "0.0",
            VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss,
            VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.FALSE,
            VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE to "OFF",
            VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to VssValues.FALSE,
            VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING to VssValues.FALSE,
            VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING to VssValues.FALSE,
            VssConstants.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION to "0",
            VssConstants.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.FALSE,
            SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "250.0",
        )
    }
}
