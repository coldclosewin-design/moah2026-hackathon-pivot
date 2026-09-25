package com.moah.hackathon.vehicle

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeVehiclePortTest {

    private fun newPort(dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
        FakeVehiclePort(simulate = false, dispatcher = dispatcher)

    @Test
    fun `get returns defaults and omits unknown keys`() = runTest {
        val port = newPort(StandardTestDispatcher(testScheduler))
        val result = port.get(listOf(VssConstants.VEHICLE_SPEED, "Vehicle.Does.Not.Exist"))
        assertEquals(mapOf(VssConstants.VEHICLE_SPEED to "0.0"), result)
        port.dispose()
    }

    @Test
    fun `set stores value and returns no failures`() = runTest {
        val port = newPort(StandardTestDispatcher(testScheduler))
        val failed = port.set(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.TRUE))
        assertTrue(failed.isEmpty())
        assertEquals(true, port.get(listOf(VssConstants.DOOR_DRIVER_ISOPEN))[VssConstants.DOOR_DRIVER_ISOPEN].toVssBoolean())
        port.dispose()
    }

    @Test
    fun `observe emits snapshot first then only subscribed deltas`() = runTest {
        val port = newPort(StandardTestDispatcher(testScheduler))
        val collected = mutableListOf<Map<String, String>>()
        val job = launch {
            port.observe(listOf(VssConstants.DOOR_DRIVER_ISOPEN)).take(2).toList(collected)
        }
        advanceUntilIdle()
        // 구독 키가 아닌 변경은 무시된다
        port.inject(mapOf(VssConstants.VEHICLE_SPEED to "42.0"))
        // 구독 키 변경은 전달된다
        port.set(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.TRUE))
        advanceUntilIdle()
        job.join()

        assertEquals(2, collected.size)
        assertEquals(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.FALSE), collected[0])
        assertEquals(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.TRUE), collected[1])
        port.dispose()
    }

    @Test
    fun `simulation ticks update speed`() = runTest {
        val port = FakeVehiclePort(simulate = true, tickMillis = 100, dispatcher = StandardTestDispatcher(testScheduler))
        val first = port.observe(listOf(VssConstants.VEHICLE_SPEED)).first()
        assertEquals("0.0", first[VssConstants.VEHICLE_SPEED])
        testScheduler.advanceTimeBy(350)
        testScheduler.runCurrent()
        val speed = port.get(listOf(VssConstants.VEHICLE_SPEED))[VssConstants.VEHICLE_SPEED].toVssFloat()
        assertTrue("speed should have changed, was $speed", speed != null && speed > 0f)
        port.dispose()
    }

    @Test
    fun `value parsers are lenient`() {
        assertEquals(12.5f, "12.5".toVssFloat())
        assertEquals(120, "120.0".toVssInt())
        assertEquals(true, "TRUE".toVssBoolean())
        assertEquals(false, "0".toVssBoolean())
        assertNull("abc".toVssBoolean())
        assertNull(null.toVssFloat())
        assertEquals("18446744073709551615", "18446744073709551615".toVssBigInteger().toString())
    }
}
