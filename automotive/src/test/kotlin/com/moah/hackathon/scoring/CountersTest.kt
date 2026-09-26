package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountersTest {
    private fun <T> s(vararg pairs: Pair<Long, T>) = pairs.map { Sample(it.first, it.second) }

    // ── SteeringReversalCounter ──

    @Test
    fun `full right then back to center is one reversal`() {
        val st = SteeringReversalCounter.summarize(s(0L to 0f, 1L to -450f, 2L to 0f))!!
        assertEquals(1, st.reversals)
        assertEquals(450f, st.maxAbsAngleDeg)
    }

    @Test
    fun `right center left right center is three reversals and jitter is ignored`() {
        val st = SteeringReversalCounter.summarize(s(0L to 0f, 1L to -450f, 2L to -445f, 3L to 0f, 4L to 300f, 5L to -450f, 6L to 0f))!!
        assertEquals(3, st.reversals)
    }

    @Test
    fun `holding the wheel is zero reversals`() {
        assertEquals(0, SteeringReversalCounter.summarize(s(0L to -450f, 1L to -448f, 2L to -452f))!!.reversals)
        assertNull(SteeringReversalCounter.summarize(emptyList()))
    }

    // ── GearShiftCounter ──

    @Test
    fun `reverse in one go counts zero shifts and ends in park`() {
        val g = GearShiftCounter.summarize(s(0L to Gear.PARK, 1L to Gear.REVERSE, 2L to Gear.PARK))!!
        assertEquals(0, g.reverseDriveShifts)
        assertTrue(g.endedInPark)
        assertTrue(g.everReversed)
    }

    @Test
    fun `R to D to R through neutral is two shifts`() {
        val g = GearShiftCounter.summarize(s(0L to Gear.PARK, 1L to Gear.REVERSE, 2L to Gear.NEUTRAL, 3L to Gear.DRIVE, 4L to Gear.NEUTRAL, 5L to Gear.REVERSE))!!
        assertEquals(2, g.reverseDriveShifts)
        assertFalse(g.endedInPark)
    }

    // ── ProximityMonitor ──

    @Test
    fun `distance crossings below 40 cm are counted once per approach`() {
        val p = ProximityMonitor.fromDistance(s(0L to 250f, 1L to 90f, 2L to 35f, 3L to 30f, 4L to 45f, 5L to 38f))!!
        assertEquals(2, p.warnings)
        assertEquals(30f, p.minDistanceCm)
    }

    @Test
    fun `warning flag rising edges when no distance signal`() {
        val p = ProximityMonitor.fromWarningFlag(s(0L to false, 1L to true, 2L to true, 3L to false, 4L to true))!!
        assertEquals(2, p.warnings)
        assertNull(p.minDistanceCm)
    }

    // ── PreDriveChecklist ──

    @Test
    fun `belt fastened before the first move passes and after it fails`() {
        val pass = PreDriveChecklist.summarize(s(0L to false, 4000L to true), s(0L to true), firstMoveMillis = 10_000L)
        assertEquals(true, pass.beltBeforeFirstMove)
        val fail = PreDriveChecklist.summarize(s(0L to false, 8000L to true), s(0L to true), firstMoveMillis = 6_500L)
        assertEquals(false, fail.beltBeforeFirstMove)
    }

    @Test
    fun `no belt signal is unmeasured - before moving belted is true and unbelted is not yet a verdict`() {
        assertNull(PreDriveChecklist.summarize(emptyList(), emptyList(), 1000L).beltBeforeFirstMove)
        assertEquals(true, PreDriveChecklist.summarize(s(0L to false, 3L to true), emptyList(), firstMoveMillis = null).beltBeforeFirstMove)
        assertNull(PreDriveChecklist.summarize(s(0L to false), emptyList(), firstMoveMillis = null).beltBeforeFirstMove)
    }
}
