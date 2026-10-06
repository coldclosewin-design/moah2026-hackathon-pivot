package com.moah.hackathon.vehicle

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioPlaybackTest {

    private val tiny = scenario("tiny", "테스트") {
        at(0.0, VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.REVERSE.vss)
        speedRamp(1.0, 2.0, 0.0, 4.0)          // 1.0 → 0, 1.5 → 2, 2.0 → 4
        at(3.0, VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss)
    }

    @Test
    fun `builder merges same-time values and sorts by time`() {
        val s = scenario("b", "b") {
            at(2.0, "k1" to "a")
            at(1.0, "k2" to "b")
            at(2.0, "k3" to "c")
        }
        assertEquals(listOf(1.0, 2.0), s.steps.map { it.atSeconds })
        assertEquals(mapOf("k1" to "a", "k3" to "c"), s.steps[1].values)
        assertEquals(2.0, s.durationSeconds, 0.0)
    }

    @Test
    fun `speed ramp uses integer multiples so fractional steps do not drift`() {
        val s = scenario("r", "r") { speedRamp(28.5, 28.8, 4.0, 0.0, stepSeconds = 0.15) }
        assertEquals(listOf(28.5, 28.65, 28.8), s.steps.map { Math.round(it.atSeconds * 100) / 100.0 })
        assertEquals("0.0", s.steps.last().values[VssConstants.VEHICLE_SPEED])
    }

    @Test
    fun `play injects steps on schedule compressed by speedFactor and coasts to a stop at the end`() = runTest {
        val port = FakeVehiclePort(simulate = false, dispatcher = StandardTestDispatcher(testScheduler))
        port.play(tiny, speedFactor = 2.0) // 3 s 시나리오 → 1.5 s
        testScheduler.runCurrent()
        assertEquals(Gear.REVERSE.vss, port.get(listOf(VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR)).values.first())
        assertEquals("0.0", port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first())

        testScheduler.advanceTimeBy(760); testScheduler.runCurrent()   // t = 1.5 s 시나리오 시각
        assertEquals("2.0", port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first())
        assertFalse(port.playback.value!!.finished)

        testScheduler.advanceTimeBy(1000); testScheduler.runCurrent()  // 끝
        assertEquals(Gear.PARK.vss, port.get(listOf(VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR)).values.first())
        // 마지막 스텝(4 km/h) 뒤 0.26 s 가 지나 감속이 막 시작됐다 — 한 번에 0 이 아니라 완만하게(라운드 25 결정 6 C)
        val coasting = port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first().toVssFloat()!!
        assertTrue("$coasting", coasting > 2f && coasting < 4f)
        assertTrue(port.playback.value!!.finished)

        // 끝난 뒤: 기본 시뮬레이션으로 돌아가지 않고 0 에 멈춘다 — 기어 등 나머지는 그대로
        testScheduler.advanceTimeBy(5000); testScheduler.runCurrent()
        assertEquals("0.0", port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first())
        assertEquals(Gear.PARK.vss, port.get(listOf(VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR)).values.first())
        port.dispose()
    }

    @Test
    fun `stopping mid-scenario coasts the car to zero instead of leaving it moving`() = runTest {
        val port = FakeVehiclePort(simulate = false, dispatcher = StandardTestDispatcher(testScheduler))
        val fast = scenario("fast", "fast") { speedRamp(0.0, 4.0, 0.0, 20.0); at(30.0, VssConstants.VEHICLE_SPEED to "20.0") }
        port.play(fast)
        testScheduler.advanceTimeBy(4100); testScheduler.runCurrent()
        assertEquals("20.0", port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first())
        port.stop()
        assertNull(port.playback.value)
        testScheduler.advanceTimeBy(1000); testScheduler.runCurrent()
        val mid = port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first().toVssFloat()!!
        assertTrue("gentle, not instant: $mid", mid in 14f..16f)
        testScheduler.advanceTimeBy(5000); testScheduler.runCurrent()
        assertEquals("0.0", port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first())
        port.dispose()
    }

    @Test
    fun `speed override wins over scenario speed and stop clears playback`() = runTest {
        val port = FakeVehiclePort(simulate = false, dispatcher = StandardTestDispatcher(testScheduler))
        port.speedOverrideKmh = 0f
        port.play(tiny)
        testScheduler.advanceTimeBy(2100); testScheduler.runCurrent()
        assertEquals("0.0", port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first())
        port.stop()
        assertNull(port.playback.value)
        port.dispose()
    }

    @Test
    fun `play replaces the default simulation`() = runTest {
        val port = FakeVehiclePort(simulate = true, tickMillis = 100, dispatcher = StandardTestDispatcher(testScheduler))
        testScheduler.advanceTimeBy(350); testScheduler.runCurrent()
        val simulated = port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first().toVssFloat()!!
        assertTrue(simulated > 40f)
        port.play(tiny)
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(1000); testScheduler.runCurrent()
        assertEquals("0.0", port.get(listOf(VssConstants.VEHICLE_SPEED)).values.first())
        port.dispose()
    }
}
