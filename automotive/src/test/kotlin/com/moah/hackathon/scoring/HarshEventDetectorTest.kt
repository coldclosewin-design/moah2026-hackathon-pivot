package com.moah.hackathon.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HarshEventDetectorTest {
    private fun s(vararg pairs: Pair<Long, Float>) = pairs.map { Sample(it.first, it.second) }

    @Test
    fun `gentle parking speeds produce no events`() {
        // 0 → 4 km/h over 1 s = 1.1 m/s²
        val events = HarshEventDetector.detect(s(0L to 0f, 500L to 2f, 1000L to 4f, 3000L to 4f, 3500L to 2f, 4000L to 0f))
        assertTrue(events.toString(), events.isEmpty())
    }

    @Test
    fun `stopping from 4 kmh in 300 ms is harsh braking`() {
        val events = HarshEventDetector.detect(s(0L to 4f, 4000L to 4f, 4150L to 2f, 4300L to 0f, 4450L to 0f))
        assertEquals(1, events.size)
        assertEquals(HarshKind.BRAKING, events[0].kind)
        assertTrue("accel was ${events[0].accelMps2}", events[0].accelMps2 <= -3.5f)
    }

    @Test
    fun `launching 0 to 40 kmh in 3 s is harsh acceleration`() {
        val events = HarshEventDetector.detect(s(0L to 0f, 1000L to 13f, 2000L to 27f, 3000L to 40f, 4000L to 40f))
        assertEquals(listOf(HarshKind.ACCELERATION), events.map { it.kind }.distinct())
    }

    @Test
    fun `one event per second per kind - debounced`() {
        // 두 번 연속 급제동 (0.3 s 간격) → 1건
        val events = HarshEventDetector.detect(s(0L to 20f, 100L to 12f, 200L to 4f, 300L to 0f, 400L to 0f))
        assertEquals(1, events.count { it.kind == HarshKind.BRAKING })
    }

    @Test
    fun `sparse 500 ms ticks fall back to neighbour differences`() {
        // 20 → 0 km/h in one 500 ms tick = -11 m/s²
        val events = HarshEventDetector.detect(s(0L to 20f, 500L to 20f, 1000L to 0f, 1500L to 0f))
        assertEquals(1, events.size)
        assertEquals(1000L, events[0].tMillis)
    }

    @Test
    fun `max abs acceleration reports the steepest window`() {
        // 0→3.6 km/h over 1 s = 1 m/s² ; 3.6→7.2 over 0.3 s = 3.33 m/s²
        val max = HarshEventDetector.maxAbsAccel(s(0L to 0f, 1000L to 3.6f, 1300L to 7.2f))
        assertTrue("max was $max", max > 3.2f && max < 3.5f)
    }
}
