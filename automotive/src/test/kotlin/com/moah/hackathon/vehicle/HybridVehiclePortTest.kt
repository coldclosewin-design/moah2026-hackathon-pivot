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

/** 속도·도어만 아는 "실차". 조향각·기어 등 나머지 키는 모른다(get 에서 빠지고, set 은 실패 목록으로). */
private class ScriptedRealPort(initial: Map<String, String>) : VehiclePort {
    val store = initial.toMutableMap()
    private val changes = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 16)
    val setCalls = mutableListOf<Map<String, String>>()
    override suspend fun get(keys: List<String>) = keys.mapNotNull { k -> store[k]?.let { k to it } }.toMap()
    override suspend fun set(values: Map<String, String>): List<String> {
        setCalls += values
        val known = values.filterKeys { it in store }
        store.putAll(known); if (known.isNotEmpty()) changes.emit(known)
        return values.keys.filter { it !in store }
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
    private val door = VssConstants.DOOR_DRIVER_ISOPEN
    private val steering = VssConstants.STEERING_WHEEL_ANGLE
    private val gear = VssConstants.TRANSMISSION_SELECTED_GEAR

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
    fun `observe passes real deltas, drops fake deltas for live keys, keeps fake deltas for the rest`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val real = ScriptedRealPort(mapOf(speed to "0.0", door to "false"))
        val fake = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val hybrid = HybridVehiclePort(real, fake)
        val seen = mutableListOf<Map<String, String>>()
        val job = launch(dispatcher) { hybrid.observe(listOf(speed, steering)).collect { seen += it } }
        advanceUntilIdle()
        assertEquals(1, seen.size)                                    // 병합 스냅샷
        real.push(mapOf(speed to "5.5")); advanceUntilIdle()
        fake.inject(mapOf(speed to "99.0")); advanceUntilIdle()       // live 키 → 버림
        fake.inject(mapOf(steering to "-450.0")); advanceUntilIdle()  // Fake 만 아는 키 → 통과
        assertEquals(listOf(mapOf(speed to "5.5"), mapOf(steering to "-450.0")), seen.drop(1))
        job.cancel(); hybrid.dispose()
    }

    @Test
    fun `set goes to the real port and keys it rejects fall through to the fake`() = runTest {
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
