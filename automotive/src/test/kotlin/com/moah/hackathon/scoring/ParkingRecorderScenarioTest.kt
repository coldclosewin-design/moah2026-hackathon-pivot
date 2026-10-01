package com.moah.hackathon.scoring

import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.vehicle.AvailabilityBadge
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 시연 시나리오 2벌을 채점기에 그대로 흘려 문서(01_driving_coach.md §4.4)에 적은 숫자가 실제로 나오는지 고정한다.
 * 시나리오·채점기 어느 쪽을 바꿔도 여기서 드러난다.
 */
class ParkingRecorderScenarioTest {

    private fun record(scenario: Scenario, jitterSeconds: (Double) -> Double = { 0.0 }): ParkingRecorder {
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (step in scenario.steps) recorder.onDelta(((step.atSeconds + jitterSeconds(step.atSeconds)) * 1000).toLong(), step.values)
        return recorder
    }

    @Test
    fun `bad parking - the harsh stop survives a late last sample (emulator load)`() {
        // 9/28 녹화·덤프 부하에서 급정지 힌트가 두 번 빠졌다. 마지막 정지 샘플(28.8 s)이 120 ms 늦게 와도 급제동 1건이어야 한다.
        val late = record(ParkingScenarios.bad) { at -> if (at == 28.8) 0.12 else 0.0 }.score()!!
        assertEquals(listOf(HarshKind.BRAKING), late.metrics.harshEvents.map { it.kind })
        assertEquals(100 - 15 - 10 - 20, late.safety)
    }

    @Test
    fun `good parking - 2 segments 1 reversal 0 shifts no safety events`() {
        val score = record(ParkingScenarios.good).score()!!
        val m = score.metrics
        assertEquals(2, m.motion.movingSegments)
        assertEquals(1, m.steering!!.reversals)
        assertEquals(0, m.gear!!.reverseDriveShifts)
        assertTrue(m.gear!!.endedInPark)
        assertTrue(m.harshEvents.isEmpty())
        assertEquals(0, m.proximity!!.warnings)
        assertEquals(80f, m.proximity!!.minDistanceCm)
        assertEquals(true, m.preDrive.beltBeforeFirstMove)
        assertEquals(true, m.preDrive.ignitionOnBeforeFirstMove)
        assertEquals(100, score.skill)
        assertEquals(100, score.safety)
    }

    @Test
    fun `bad parking - 4 segments 3 reversals 2 shifts 1 harsh brake 1 proximity and no belt`() {
        val score = record(ParkingScenarios.bad).score()!!
        val m = score.metrics
        assertEquals(4, m.motion.movingSegments)
        assertEquals(3, m.steering!!.reversals)
        assertEquals(2, m.gear!!.reverseDriveShifts)
        assertTrue(m.gear!!.endedInPark)
        assertEquals(listOf(HarshKind.BRAKING), m.harshEvents.map { it.kind })
        assertEquals(1, m.proximity!!.warnings)
        assertEquals(35f, m.proximity!!.minDistanceCm)
        assertEquals(false, m.preDrive.beltBeforeFirstMove)
        assertEquals(100 - 16 - 12 - 12, score.skill)
        assertEquals(100 - 15 - 10 - 20, score.safety)
    }

    @Test
    fun `badge - everything from the fake is SIMULATED and the door never arrives`() {
        val score = record(ParkingScenarios.good).score()!!
        assertEquals(AvailabilityBadge(live = 0, simulated = 7, missing = 1), score.badge)
        assertEquals(listOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN), score.missingSignals)
    }

    @Test
    fun `A layer alone still yields a verdict when every B signal is missing`() {
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = false))
        for (step in ParkingScenarios.bad.steps) {
            val speedOnly = step.values.filterKeys { it == VssConstants.VEHICLE_SPEED }
            if (speedOnly.isNotEmpty()) recorder.onDelta((step.atSeconds * 1000).toLong(), speedOnly)
        }
        val score = recorder.score()!!
        assertEquals(4, score.metrics.motion.movingSegments)
        assertEquals(null, score.metrics.steering)
        assertEquals(null, score.metrics.gear)
        assertEquals(null, score.metrics.proximity)
        assertEquals(null, score.metrics.preDrive.beltBeforeFirstMove)
        assertEquals(100 - 16, score.skill)          // 구간 초과만
        assertEquals(100 - 15, score.safety)         // 급정지만
        assertEquals(AvailabilityBadge(live = 1, simulated = 0, missing = 7), score.badge)
    }

    @Test
    fun `delta between the two attempts reads as improvement`() {
        val bad = record(ParkingScenarios.bad).metrics()!!
        val good = record(ParkingScenarios.good).metrics()!!
        val d = ParkingDelta.of(good, bad)
        assertEquals(-2, d.segments)
        assertEquals(-2, d.reversals)
        assertEquals(-2, d.shifts)
        assertTrue(d.seconds < 0)
    }
}
