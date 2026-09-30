package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 출발 전 점검 7단계 시연 시나리오 2벌을 채점기에 그대로 흘려 숫자를 고정한다(`ChecklistScenarios` 주석과 같아야 한다). */
class ChecklistScenarioTest {

    private val BASELINE = mapOf(
        mobis.vss.VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "0", SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "200",
        mobis.vss.VssConstants.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to com.moah.hackathon.vehicle.VssValues.FALSE,
    )

    private fun record(scenario: Scenario, keys: Set<String> = ParkingRecorder.CHECKLIST_KEYS): ParkingRecorder {
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.CHECKLIST_KEYS, simulated = true), keys)
        recorder.onDelta(0L, BASELINE)   // 앱에서는 회차 시작 때 `VehiclePort.get` 이 주는 현재값 — 조향·거리·경고는 시나리오가 건드리지 않는다
        for (step in scenario.steps) recorder.onDelta((step.atSeconds * 1000).toLong(), step.values)
        return recorder
    }

    @Test
    fun `good check - door belt brake ignition three lights no motion - 100 100 in 14s`() {
        val score = record(ChecklistScenarios.good).scoreChecklist()!!
        val pd = score.metrics.preDrive
        assertEquals(3_000L, pd.beltOnMillis)
        assertEquals(8_000L, pd.ignitionOnMillis)
        assertEquals(true, pd.beltBeforeIgnition)
        assertEquals(true, pd.doorClosedBeforeIgnition)
        assertEquals(true, pd.brakeBeforeIgnition)
        assertEquals(listOf(true, true, true), listOf(pd.leftIndicatorChecked, pd.rightIndicatorChecked, pd.hazardChecked))
        assertEquals(14_000L, pd.lightsDoneMillis)
        assertEquals(0, pd.skippedLights)
        assertEquals(0, score.metrics.motion.movingSegments)
        assertTrue(score.metrics.gear!!.endedInPark)
        assertEquals(100, score.skill)
        assertEquals(100, score.safety)
        assertEquals(14_000L, ChecklistScorer.completionMillis(score.metrics))
        assertEquals(12, score.badge.simulated)
    }

    @Test
    fun `bad check - door open no brake no belt at ignition, a creep, hazard skipped - 30 40`() {
        val score = record(ChecklistScenarios.bad).scoreChecklist()!!
        val pd = score.metrics.preDrive
        assertEquals(9_000L, pd.beltOnMillis)
        assertEquals(2_000L, pd.ignitionOnMillis)
        assertEquals(false, pd.beltBeforeIgnition)
        assertEquals(false, pd.doorClosedBeforeIgnition)
        assertEquals(false, pd.brakeBeforeIgnition)
        assertEquals(listOf(true, true, false), listOf(pd.leftIndicatorChecked, pd.rightIndicatorChecked, pd.hazardChecked))
        assertNull(pd.lightsDoneMillis)
        assertEquals(1, pd.skippedLights)
        assertEquals(1, score.metrics.motion.movingSegments)
        assertTrue(score.metrics.harshEvents.isEmpty())
        assertEquals(30, score.skill)     // 순서 -40 · 브레이크 -20 · 비상등 -10
        assertEquals(40, score.safety)    // 문 열린 채 시동 -30 · 움직임 -30
    }

    @Test
    fun `missing light and brake signals are unmeasured - no penalty and the badge counts them missing`() {
        // 등화·브레이크 신호가 아예 안 오는 차(사내 실물 가능성): 나머지만으로 채점, 새 항목은 전부 null
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.CHECKLIST_KEYS, simulated = true), ParkingRecorder.CHECKLIST_KEYS)
        recorder.onDelta(0L, BASELINE)
        val drop = setOf(mobis.vss.VssConstants.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION, mobis.vss.VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING,
            mobis.vss.VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, mobis.vss.VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING)
        for (step in ChecklistScenarios.good.steps) recorder.onDelta((step.atSeconds * 1000).toLong(), step.values.filterKeys { it !in drop })
        val score = recorder.scoreChecklist()!!
        val pd = score.metrics.preDrive
        assertNull(pd.brakeBeforeIgnition)
        assertNull(pd.leftIndicatorChecked); assertNull(pd.hazardChecked); assertNull(pd.lightsDoneMillis)
        assertEquals(0, pd.skippedLights)
        assertEquals(100, score.skill)
        assertEquals(100, score.safety)
        assertEquals(4, score.badge.missing)
        assertEquals(8_000L, ChecklistScorer.completionMillis(score.metrics))
    }

    @Test
    fun `the parking scorer misses the order mistake - which is why the checklist has its own`() {
        // 같은 시계열을 주차 채점기에 넣으면 숙련 100 — "시동이 벨트보다 먼저" 를 볼 줄 모른다(벨트 없이 움직인 것만 -20).
        // 과제에 따라 채점기를 골라야 하고, 그 선택은 상태기계가 한다.
        val parking = record(ChecklistScenarios.bad, keys = ParkingRecorder.KEYS).score()!!
        assertEquals(100, parking.skill)
        assertEquals(80, parking.safety)
        assertEquals(8, parking.badge.simulated)   // 주차 키로 자르면 배지 분모는 8 그대로
    }
}
