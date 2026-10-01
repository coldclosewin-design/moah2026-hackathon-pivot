package com.moah.hackathon.vehicle

import com.moah.hackathon.scoring.ParkingRecorder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "실차" 대역. [initial] 의 키만 안다(get 에서 나오고, 구독으로 돌아온다). 모르는 키는 get 에서 빠지고 set 은 실패 목록으로.
 * [readOnly] 키는 알지만 **쓰기를 거부**한다(읽기 전용 sensor — 실패 목록으로 돌려준다).
 */
private class ScriptedRealPort(initial: Map<String, String>, private val readOnly: Set<String> = emptySet()) : VehiclePort {
    val store = initial.toMutableMap()
    private val changes = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 16)
    val setCalls = mutableListOf<Map<String, String>>()
    override suspend fun get(keys: List<String>) = keys.mapNotNull { k -> store[k]?.let { k to it } }.toMap()
    override suspend fun set(values: Map<String, String>): List<String> {
        setCalls += values
        val accepted = values.filterKeys { it in store && it !in readOnly }
        store.putAll(accepted); if (accepted.isNotEmpty()) changes.emit(accepted)
        return values.keys.filter { it !in store || it in readOnly }
    }
    override fun observe(keys: List<String>): Flow<Map<String, String>> = flow {
        emit(get(keys))
        changes.map { it.filterKeys { k -> k in keys } }.filter { it.isNotEmpty() }.collect { emit(it) }
    }
    suspend fun push(values: Map<String, String>) { store.putAll(values); changes.emit(values) }
    override fun dispose() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class HybridVehiclePortTest {
    private val speed = VssConstants.VEHICLE_SPEED
    private val door = VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN
    private val steering = VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE
    private val gear = VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR

    @Test
    fun `get fills only the keys the real port does not know and marks the rest live`() = runTest {
        val real = ScriptedRealPort(mapOf(speed to "12.0", door to "false"))
        val hybrid = HybridVehiclePort(real, FakeVehiclePort(simulate = false, dispatcher = StandardTestDispatcher(testScheduler)))
        val got = hybrid.get(listOf(speed, door, steering, gear))
        assertEquals("12.0", got[speed])
        assertEquals("0.0", got[steering])            // Fake DEFAULTS
        assertEquals(Gear.PARK.vss, got[gear])
        assertTrue(hybrid.isLive(speed)); assertTrue(hybrid.isLive(door))
        assertFalse(hybrid.isLive(steering)); assertFalse(hybrid.isLive(gear))
        hybrid.dispose()
    }

    @Test
    fun `fake operations go through the real port and come back as live values - fake deltas for live keys are dropped`() = runTest {
        // 9/30 사내 관찰: 회차 시작 get 에서 속도·조향·기어 등이 실신호라 시연 패널의 시나리오가 live 키에 먹지 않았다 → write-through
        val dispatcher = StandardTestDispatcher(testScheduler)
        val real = ScriptedRealPort(mapOf(speed to "0.0", door to "false"))
        val fake = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val hybrid = HybridVehiclePort(real, fake)
        val seen = mutableListOf<Map<String, String>>()
        val job = launch(dispatcher) { hybrid.observe(listOf(speed, steering)).collect { seen += it } }
        advanceUntilIdle()
        assertEquals(1, seen.size)                                    // 병합 스냅샷
        real.push(mapOf(speed to "5.5")); advanceUntilIdle()          // 실물 delta 통과
        fake.inject(mapOf(speed to "99.0")); advanceUntilIdle()       // Fake 조작 → 실물에 먼저 써지고(setCalls) → Real 구독으로 돌아온다
        assertEquals(listOf(mapOf(speed to "99.0")), real.setCalls)
        assertEquals("99.0", real.store[speed])
        fake.inject(mapOf(steering to "-450.0")); advanceUntilIdle()  // 실물이 모르는 키 → 실패 목록 → forced 가 아니라 그냥 Fake 통과(live 아님)
        assertEquals(listOf(mapOf(speed to "5.5"), mapOf(speed to "99.0"), mapOf(steering to "-450.0")), seen.drop(1))
        assertTrue(hybrid.isLive(speed)); assertFalse(hybrid.isLive(steering))
        job.cancel(); hybrid.dispose()
        assertEquals(null, fake.writeThrough)                         // dispose 가 훅을 푼다
    }

    @Test
    fun `a read-only sensor the real port rejects is forced - fake values reach the screen and later real values are ignored`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val real = ScriptedRealPort(mapOf(speed to "0.0", gear to "0"), readOnly = setOf(gear))
        val fake = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val hybrid = HybridVehiclePort(real, fake)
        val seen = mutableListOf<Map<String, String>>()
        val job = launch(dispatcher) { hybrid.observe(listOf(speed, gear)).collect { seen += it } }
        advanceUntilIdle()
        assertTrue(hybrid.isLive(gear))                               // 처음엔 실물 값(0=N)이 있어 live
        fake.inject(mapOf(gear to Gear.REVERSE.vss)); advanceUntilIdle()   // 실물이 거부 → forced → Fake 값이 화면까지
        assertEquals(mapOf(gear to Gear.REVERSE.vss), seen.last())
        assertFalse(hybrid.isLive(gear)); assertTrue(gear in hybrid.forcedKeys)
        real.push(mapOf(gear to "0")); advanceUntilIdle()             // 이후 실물 값은 무시
        assertEquals(mapOf(gear to Gear.REVERSE.vss), seen.last())
        assertEquals(Gear.REVERSE.vss, hybrid.get(listOf(gear))[gear])   // get 도 Fake 값
        assertFalse(hybrid.isLive(gear))                              // markLive 가 forced 키를 되살리지 않는다
        job.cancel(); hybrid.dispose()
    }

    @Test
    fun `set goes to the real port through the fake and keys it rejects stay in the fake`() = runTest {
        val real = ScriptedRealPort(mapOf(door to "false"))
        val fake = FakeVehiclePort(simulate = false, dispatcher = StandardTestDispatcher(testScheduler))
        val hybrid = HybridVehiclePort(real, fake)
        val failed = hybrid.set(mapOf(door to "true", gear to Gear.REVERSE.vss))
        assertTrue(failed.isEmpty())
        assertEquals("true", real.store[door])
        assertEquals(Gear.REVERSE.vss, fake.get(listOf(gear))[gear])
        hybrid.dispose()
    }

    @Test
    fun `registry built for a hybrid port splits the badge between live and simulated`() = runTest {
        val real = ScriptedRealPort(mapOf(speed to "0.0", door to "false"))
        val hybrid = HybridVehiclePort(real, FakeVehiclePort(simulate = false, dispatcher = StandardTestDispatcher(testScheduler)))
        val registry = SignalRegistry.forPort(ParkingRecorder.KEYS, hybrid)
        registry.onValues(hybrid.get(ParkingRecorder.KEYS.toList()))
        val badge = registry.badge()
        assertEquals(2, badge.live)
        assertEquals(ParkingRecorder.KEYS.size - 2, badge.simulated)
        assertEquals(0, badge.missing)
        assertEquals(SignalAvailability.LIVE, registry.availability(speed))
        assertEquals(SignalAvailability.SIMULATED, registry.availability(steering))
        hybrid.dispose()
    }

    @Test
    fun `forPort picks the right registry for fake and real`() {
        val fake = SignalRegistry.forPort(setOf(speed), FakeVehiclePort(simulate = false))
        fake.onValues(mapOf(speed to "1"))
        assertEquals(SignalAvailability.SIMULATED, fake.availability(speed))
        val real = SignalRegistry.forPort(setOf(speed), ScriptedRealPort(emptyMap()))
        real.onValues(mapOf(speed to "1"))
        assertEquals(SignalAvailability.LIVE, real.availability(speed))
    }
}
