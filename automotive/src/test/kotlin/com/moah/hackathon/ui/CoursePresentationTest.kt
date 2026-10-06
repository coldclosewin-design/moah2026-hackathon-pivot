package com.moah.hackathon.ui

import mobis.vss.VssConstants
import com.moah.hackathon.data.CourseScenarios
import com.moah.hackathon.data.TrackCourses
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.lesson.*
import com.moah.hackathon.vehicle.SignalAvailability
import com.moah.hackathon.vehicle.SimOnlySignals
import org.junit.Assert.*
import org.junit.Test

class CoursePresentationTest {
    private fun exam(good: Boolean): CourseResult = CourseRecorder(TrackCourses.exam).apply {
        (if (good) CourseScenarios.examGood else CourseScenarios.examBad).steps.forEach {
            onDelta((it.atSeconds * 1000).toLong(), it.values)
        }
    }.result()

    @Test fun examVerdictsAndZoneStatusUseRecordedEvidenceWithoutNumbers() {
        val bad = exam(false)
        val good = exam(true)
        assertEquals("불합격", courseVerdict(bad))
        assertEquals("합격", courseVerdict(good))
        assertEquals("실격 사유 있음", courseVerdict(good.copy(disqualified = true)))
        assertEquals("시험장 위치 미측정", courseVerdict(good.copy(positionMeasured = false)))
        bad.zones.forEach { zone ->
            assertEquals(if (zone.deductions.isEmpty()) "지남" else "놓침", courseZoneStatus(bad, zone))
        }
        val zone = good.zones.first()
        assertEquals("미측정", courseZoneStatus(good, zone.copy(unmeasured = listOf("전조등"))))
        assertEquals("놓침", courseZoneStatus(good, zone.copy(visited = false)))
        assertEquals("미측정", courseZoneStatus(good.copy(positionMeasured = false), zone))
    }

    @Test fun provenanceIncludesMissingTrackKeysAndDoesNotBorrowVehicleSources() {
        assertEquals("미측정", trackSource(emptyMap()))
        val fake = SimOnlySignals.TRACK_KEYS.associateWith { SignalAvailability.SIMULATED }
        assertEquals("시뮬레이션", trackSource(fake))
        assertEquals("실신호 · 시뮬레이션", trackSource(fake + (SimOnlySignals.TRACK_POSITION_X_M to SignalAvailability.LIVE)))
        assertEquals("시뮬레이션 · 미측정", trackSource(fake - SimOnlySignals.TRACK_HEADING_DEG))
        assertEquals("미측정", trackSource(mapOf(VssConstants.VEHICLE_BODY_WINDSHIELD_FRONT_WIPING_MODE to SignalAvailability.LIVE)))
    }

    @Test fun trailReplayUsesTimeAndShortestHeadingArc() {
        val trail = listOf(TrackPoint(100, 0f, 0f, 350f), TrackPoint(1100, 10f, 20f, 10f), TrackPoint(5100, 30f, 40f, 90f))
        val half = courseTrailThroughTime(trail, 600).last()
        assertEquals(5f, half.x, .001f)
        assertEquals(10f, half.y, .001f)
        assertEquals(360f, half.headingDeg, .001f)
        assertEquals(trail, courseTrailThroughTime(trail, 5100))
        assertTrue(courseTrailThroughTime(emptyList(), 0).isEmpty())
    }

    @Test fun arrivalBayBoundsFollowTargetEvenWhenMeasuredHeadingDiffers() {
        val path = listOf(PathPoint(0, 0f, 0f, 90f, false), PathPoint(1000, 0f, 0f, 90f, true))
        val (bottom, top) = pathVerticalBounds(path, 100f, false, targetHeading = 0f)
        assertEquals(-260.75f, bottom, .01f)
        assertEquals(260.75f, top, .01f)
        val angle = pathVerticalBounds(path, 100f, false, targetHeading = 45f)
        assertTrue(angle.first < -260f && angle.second > 260f)
    }
}
