package com.moah.hackathon.vehicle

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mobis.vss.VssConstants
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

/**
 * 외부 PC에서 "실제로 도는" in-memory 구현. setVSS 흉내(store 쓰기 + emit)와
 * 신호 시뮬레이션 티커로 UI 를 검증한다.
 *
 * @param simulate true 면 속도 등 sensor 신호를 주기적으로 변화시킨다. 테스트에서는 false.
 * @param tickMillis 시뮬레이션 주기.
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

    init {
        if (simulate) scope.launch { runSimulation() }
    }

    override suspend fun get(keys: List<String>): Map<String, String> =
        keys.mapNotNull { k -> store[k]?.let { k to it } }.toMap()

    override suspend fun set(values: Map<String, String>): List<String> {
        if (values.isEmpty()) return emptyList()
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
        store.putAll(values)
        changes.emit(values)
    }

    /**
     * 시연 조작: null 이면 자동 시뮬레이션, 값이 있으면 그 속도로 고정 (예: 0 = 정차).
     * 시뮬레이션 티커가 다음 tick 에 반영한다.
     */
    @Volatile
    var speedOverrideKmh: Float? = null

    private suspend fun runSimulation() {
        var t = 0.0
        while (scope.isActive) {
            delay(tickMillis)
            t += tickMillis / 1000.0
            // 45 ~ 95 km/h 사이를 천천히 오르내리는 속도 (override 가 있으면 그 값).
            // 0 근처까지 내려가면 주행 중에 화면 잠금이 풀리고, 구간 소요 시간이 앱 시작 시점에 따라 크게 달라진다.
            val speed = speedOverrideKmh?.toDouble() ?: (70 + 25 * sin(t / 12.0))
            inject(mapOf(VssConstants.VEHICLE_SPEED to "%.1f".format(speed)))
        }
    }

    companion object {
        val DEFAULTS: Map<String, String> = mapOf(
            VssConstants.VEHICLE_SPEED to "0.0",
            VssConstants.VEHICLE_ADAS_ABS_ISENABLED to VssValues.TRUE,
            VssConstants.VEHICLE_ADAS_CRUISECONTROL_SPEEDSET to "0.0",
            VssConstants.VEHICLE_BODY_HORN_ISACTIVE to VssValues.FALSE,
            VssConstants.DOOR_DRIVER_ISOPEN to VssValues.FALSE,
        )
    }
}
