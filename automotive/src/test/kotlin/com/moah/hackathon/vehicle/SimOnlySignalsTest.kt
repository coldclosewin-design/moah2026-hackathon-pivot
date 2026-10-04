package com.moah.hackathon.vehicle

import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimOnlySignalsTest {
    @Test fun `track signal parser is lenient and rejects unknown`() {
        assertEquals(TrackSignal.RED, "red".toTrackSignal())
        assertEquals(TrackSignal.GREEN, " GREEN ".toTrackSignal())
        assertNull("BLUE".toTrackSignal())
        assertNull((null as String?).toTrackSignal())
    }

    @Test fun `wiper mode maps OFF to false and running modes to true`() {
        assertEquals(false, "OFF".toWiperOn())
        assertEquals(true, "interval".toWiperOn())
        assertNull("??".toWiperOn())
    }

    @Test fun `fake defaults cover every sim only key so badges start simulated`() {
        val all = SimOnlySignals.TRACK_KEYS + SimOnlySignals.DEVICE_KEYS + SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM
        assertTrue(all.all { it in FakeVehiclePort.DEFAULTS })
    }

    @Test fun `sim only keys never collide with in-house constants`() {
        val real = VssConstants::class.java.fields.mapNotNull { it.get(null) as? String }.toSet()
        val sim = SimOnlySignals.TRACK_KEYS + SimOnlySignals.DEVICE_KEYS + SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM
        assertFalse(sim.any { it in real })
        assertTrue(SimOnlySignals.TRACK_KEYS.all { it.startsWith("Track.") })
    }
}
