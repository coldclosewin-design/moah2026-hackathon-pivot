package com.moah.hackathon.scoring

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 출발 전 점검 시연 시나리오 2벌을 채점기에 그대로 흘려 숫자를 고정한다(`ChecklistScenarios` 주석과 같아야 한다). */
class ChecklistScenarioTest {

    private fun record(scenario: Scenario): ParkingRecorder {
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (step in scenario.steps) recorder.onDelta((step.atSeconds * 1000).toLong(), step.values)
        return recorder
    }

    @Test
    fun `good check - belt 2s ignition 5s no motion - 100 100`() {
        val score = record(ChecklistScenarios.good).scoreChecklist()!!
        val pd = score.metrics.preDrive
        assertEquals(2_000L, pd.beltOnMillis)
        assertEquals(5_000L, pd.ignitionOnMillis)
        assertEquals(true, pd.beltBeforeIgnition)
        assertEquals(0, score.metrics.motion.movingSegments)
        assertTrue(score.metrics.gear!!.endedInPark)
        assertEquals(100, score.skill)
        assertEquals(100, score.safety)
        assertEquals(5_000L, ChecklistScorer.completionMillis(score.metrics))
    }

    @Test
    fun `bad check - ignition 2s before belt 9s and a creep - 60 70`() {
        val score = record(ChecklistScenarios.bad).scoreChecklist()!!
        val pd = score.metrics.preDrive
        assertEquals(9_000L, pd.beltOnMillis)
        assertEquals(2_000L, pd.ignitionOnMillis)
        assertEquals(false, pd.beltBeforeIgnition)
        assertEquals(1, score.metrics.motion.movingSegments)
        assertTrue(score.metrics.harshEvents.isEmpty())
        assertEquals(60, score.skill)
        assertEquals(70, score.safety)
    }

    @Test
    fun `the parking scorer misses the order mistake - which is why the checklist has its own`() {
        // 같은 시계열을 주차 채점기에 넣으면 숙련 100 — "시동이 벨트보다 먼저" 를 볼 줄 모른다(벨트 없이 움직인 것만 -20).
        // 과제에 따라 채점기를 골라야 하고, 그 선택은 상태기계가 한다.
        val parking = record(ChecklistScenarios.bad).score()!!
        assertEquals(100, parking.skill)
        assertEquals(80, parking.safety)
    }
}
