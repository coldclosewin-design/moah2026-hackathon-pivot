package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.AvailabilityBadge
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistScorerTest {
    private val badge = AvailabilityBadge(0, 7, 1)

    private fun metrics(
        beltOn: Long? = 2_000L,
        ignitionOn: Long? = 5_000L,
        segments: Int = 0,
        endedInPark: Boolean? = true,
        totalMillis: Long = 8_000L,
    ) = ParkingMetrics(
        MotionSummary(segments, totalMillis, if (segments > 0) 1_000L else 0L, if (segments > 0) 2f else 0f, if (segments > 0) 4_000L else null),
        emptyList(), null,
        endedInPark?.let { GearSummary(0, it, everReversed = false) }, null,
        PreDriveSummary(beltOn != null, ignitionOn != null, beltOn, ignitionOn),
    )

    @Test
    fun `belt then ignition without moving scores 100 on both axes and completion is the later of the two`() {
        val s = ChecklistScorer.score(metrics(), badge, emptyList())
        assertEquals(100, s.skill)
        assertEquals(100, s.safety)
        assertEquals(5_000L, ChecklistScorer.completionMillis(s.metrics))
    }

    @Test
    fun `ignition before belt costs skill - the order is the whole point of the task`() {
        val s = ChecklistScorer.score(metrics(beltOn = 9_000L, ignitionOn = 2_000L), badge, emptyList())
        assertEquals(60, s.skill)
        assertEquals(100, s.safety)
        assertEquals(9_000L, ChecklistScorer.completionMillis(s.metrics))
    }

    @Test
    fun `never belted costs skill and safety, never started costs skill only`() {
        val noBelt = ChecklistScorer.score(metrics(beltOn = null), badge, emptyList())
        assertEquals(70, noBelt.skill)
        assertEquals(60, noBelt.safety)
        val noStart = ChecklistScorer.score(metrics(ignitionOn = null), badge, emptyList())
        assertEquals(60, noStart.skill)
        assertEquals(100, noStart.safety)
    }

    @Test
    fun `a MISSING signal is unmeasured - no penalty for what the car did not report`() {
        val missing = listOf(VssConstants.SEAT_DRIVER_ISBELTED, VssConstants.LOW_VOLTAGE_SYSTEM_STATE)
        val s = ChecklistScorer.score(metrics(beltOn = null, ignitionOn = null), badge, missing)
        assertEquals(100, s.skill)
        assertEquals(100, s.safety)
        assertEquals(missing, s.missingSignals)
    }

    @Test
    fun `moving the car and not ending in P are safety penalties`() {
        val moved = ChecklistScorer.score(metrics(segments = 1), badge, emptyList())
        assertEquals(100, moved.skill)
        assertEquals(70, moved.safety)
        val notPark = ChecklistScorer.score(metrics(endedInPark = false), badge, emptyList())
        assertEquals(70, notPark.safety)
        // 기어 신호가 없으면(null) 모른다 → 감점 없음
        assertEquals(100, ChecklistScorer.score(metrics(endedInPark = null), badge, emptyList()).safety)
    }

    @Test
    fun `slow completion is charged per 10 seconds past the grace period`() {
        val s = ChecklistScorer.score(metrics(beltOn = 10_000L, ignitionOn = 52_000L, totalMillis = 60_000L), badge, emptyList())
        assertEquals(100 - 2 * 5, s.skill)   // 52 s - 30 s = 22 s → 2 × 10 s
    }
}
