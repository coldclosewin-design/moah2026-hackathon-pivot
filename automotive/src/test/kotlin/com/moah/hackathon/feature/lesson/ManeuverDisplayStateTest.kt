package com.moah.hackathon.feature.lesson

import com.moah.hackathon.vehicle.SimOnlySignals
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
        guide = GuideStepView(3, 6, "핸들을 오른쪽 끝까지 돌리세요.", VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, unverified = false),
        lastHint = "뒤가 가까워요. 멈추세요.", movingSegments = 3, elapsedMillis = 44_400, askedDone = false, availability = availability,
    )

    @Test
    fun `maps current values and progress - never a score`() {
        val snap = VehicleSnapshot().apply(mapOf(
            VssConstants.VEHICLE_SPEED to "2.6", VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "-450",
            VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to "-1", SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "35", VssConstants.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to "true",
        ))
        val s = phase(snap, mapOf(
            VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to SignalAvailability.SIMULATED,
            VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to SignalAvailability.LIVE,
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
        assertEquals(TaskType.PARKING, s.taskType)
        assertNull(s.belt); assertNull(s.ignitionOn)
        assertEquals(SignalAvailability.MISSING, s.beltSignal)
        // 점수·감점·조향 왕복 수·기어 전환 수는 이 타입에 존재하지 않는다 (컴파일 타임 보장). 문서용 확인:
        val fields = ManeuverDisplayState::class.java.declaredFields.map { it.name }
        assertTrue(fields.none { it.contains("skill") || it.contains("safety") || it.contains("reversal") || it.contains("shift") || it.contains("score") })
    }

    @Test
    fun `checklist task carries belt and ignition for the three chips`() {
        val snap = VehicleSnapshot().apply(mapOf(
            VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to "true", VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE to "OFF",
            VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to "126",
        ))
        val s = phase(snap, mapOf(
            VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to SignalAvailability.LIVE,
            VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE to SignalAvailability.SIMULATED,
        )).copy(task = SeedCatalog.predriveTask, guide = null).toDisplayState()
        assertEquals(TaskType.CHECKLIST, s.taskType)
        assertEquals(true, s.belt)
        assertEquals(false, s.ignitionOn)
        assertEquals("P", s.gear)
        assertEquals(SignalAvailability.LIVE, s.beltSignal)
        assertEquals(SignalAvailability.SIMULATED, s.ignitionSignal)
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
