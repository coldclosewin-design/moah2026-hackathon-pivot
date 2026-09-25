package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.AvailabilityBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParkingScorerTest {
    private val badge = AvailabilityBadge(0, 7, 1)
    private fun motion(segments: Int, seconds: Long) = MotionSummary(segments, seconds * 1000, seconds * 500, 4f, 1000L)
    private fun metrics(
        segments: Int = 2, seconds: Long = 30, harsh: Int = 0,
        steering: SteeringSummary? = SteeringSummary(1, 450f),
        gear: GearSummary? = GearSummary(0, endedInPark = true, everReversed = true),
        proximity: ProximitySummary? = ProximitySummary(0, 80f),
        belt: Boolean? = true,
    ) = ParkingMetrics(
        motion(segments, seconds),
        List(harsh) { HarshEvent(it * 2000L, HarshKind.BRAKING, -4f) },
        steering, gear, proximity, PreDriveSummary(belt, true),
    )

    @Test
    fun `ideal parking scores 100 on both axes`() {
        val s = ParkingScorer.score(metrics(), badge, listOf("door"))
        assertEquals(100, s.skill)
        assertEquals(100, s.safety)
        assertEquals(listOf("door"), s.missingSignals)
    }

    @Test
    fun `skill loses points for extra segments reversals shifts and time`() {
        val s = ParkingScorer.score(metrics(segments = 4, seconds = 70, steering = SteeringSummary(3, 450f), gear = GearSummary(2, true, true)), badge, emptyList())
        // 2 extra segments ×8 + 2 extra reversals ×6 + 2 shifts ×6 + 25 s over grace → 2 blocks ×2
        assertEquals(100 - 16 - 12 - 12 - 4, s.skill)
        assertEquals(100, s.safety)
    }

    @Test
    fun `safety loses points for harsh events proximity and belt - skill untouched`() {
        val s = ParkingScorer.score(metrics(harsh = 1, proximity = ProximitySummary(1, 35f), belt = false), badge, emptyList())
        assertEquals(100, s.skill)
        assertEquals(100 - 15 - 10 - 20, s.safety)
    }

    @Test
    fun `missing B-layer signals are simply not scored`() {
        val s = ParkingScorer.score(metrics(steering = null, gear = null, proximity = null, belt = null), badge, listOf("steering", "gear"))
        assertEquals(100, s.skill)
        assertEquals(100, s.safety)
    }

    @Test
    fun `scores are clamped to 0`() {
        val s = ParkingScorer.score(metrics(segments = 20, harsh = 10), badge, emptyList())
        assertEquals(0, s.skill)
        assertEquals(0, s.safety)
    }

    @Test
    fun `delta against the previous attempt - negative is better`() {
        val d = ParkingDelta.of(metrics(segments = 2, seconds = 30, steering = SteeringSummary(1, 450f)), metrics(segments = 4, seconds = 50, steering = SteeringSummary(3, 450f)))
        assertEquals(-2, d.segments)
        assertEquals(-20L, d.seconds)
        assertEquals(-2, d.reversals)
        assertNull(ParkingDelta.of(metrics(steering = null), metrics()).reversals)
    }
}
