package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.vehicle.SignalAvailability
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManeuverDisplayStateTest {

    private fun phase(snapshot: VehicleSnapshot, availability: Map<String, SignalAvailability>) = LessonPhase.Maneuver(
        task = SeedCatalog.parkingTask, mode = LessonMode.HINT, attempt = 2, snapshot = snapshot,
        guide = GuideStepView(3, 6, "핸들을 오른쪽 끝까지 돌리세요.", VssConstants.STEERING_WHEEL_ANGLE, unverified = false),
        lastHint = "뒤가 가까워요. 멈추세요.", movingSegments = 3, elapsedMillis = 44_400, askedDone = false, availability = availability,
    )

    @Test
    fun `maps current values and progress - never a score`() {
        val snap = VehicleSnapshot().apply(mapOf(
            VssConstants.VEHICLE_SPEED to "2.6", VssConstants.STEERING_WHEEL_ANGLE to "-450",
            VssConstants.TRANSMISSION_SELECTED_GEAR to "-1", VssConstants.OBSTACLE_REAR_DISTANCE_CM to "35", VssConstants.OBSTACLE_IS_WARNING to "true",
        ))
        val s = phase(snap, mapOf(
            VssConstants.STEERING_WHEEL_ANGLE to SignalAvailability.SIMULATED,
            VssConstants.TRANSMISSION_SELECTED_GEAR to SignalAvailability.LIVE,
        )).toDisplayState()
        assertEquals("3", s.speed)
        assertEquals(-450f, s.steeringDeg)
        assertEquals("R", s.gear)
        assertEquals(35f, s.rearDistanceCm)
        assertTrue(s.obstacleWarning)
        assertEquals("핸들을 오른쪽 끝까지 돌리세요.", s.guideText)
        assertEquals("4/6", s.guideStep)
        assertEquals("뒤가 가까워요. 멈추세요.", s.hintText)
        assertEquals(2, s.attempt)
        assertEquals(3, s.movingSegments)
        assertEquals(44L, s.elapsedSeconds)
        assertEquals(SignalAvailability.SIMULATED, s.steeringSignal)
        assertEquals(SignalAvailability.LIVE, s.gearSignal)
        assertEquals(SignalAvailability.MISSING, s.distanceSignal)
        // 점수·감점·조향 왕복 수·기어 전환 수는 이 타입에 존재하지 않는다 (컴파일 타임 보장). 문서용 확인:
        val fields = ManeuverDisplayState::class.java.declaredFields.map { it.name }
        assertTrue(fields.none { it.contains("skill") || it.contains("safety") || it.contains("reversal") || it.contains("shift") || it.contains("score") })
    }

    @Test
    fun `garbage values are dropped`() {
        val snap = VehicleSnapshot(speedKmh = Float.NaN, steeringDeg = Float.NaN, rearDistanceCm = -1f)
        val s = phase(snap, emptyMap()).copy(guide = null, lastHint = "  ").toDisplayState()
        assertEquals("—", s.speed)
        assertNull(s.steeringDeg)
        assertNull(s.rearDistanceCm)
        assertNull(s.gear)
        assertNull(s.guideText)
        assertNull(s.hintText)
    }
}
