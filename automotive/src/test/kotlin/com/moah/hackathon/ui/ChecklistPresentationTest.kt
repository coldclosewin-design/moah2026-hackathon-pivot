package com.moah.hackathon.ui

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.ui.lesson.*
import com.moah.hackathon.vehicle.*
import mobis.vss.VssConstants as V
import org.junit.Assert.*
import org.junit.Test

class ChecklistPresentationTest {
    private fun score(scenario: Scenario, missing: Set<String> = emptySet()) =
        ParkingRecorder(SignalRegistry(ParkingRecorder.CHECKLIST_KEYS, simulated = true), ParkingRecorder.CHECKLIST_KEYS).apply {
            scenario.steps.forEach { onDelta((it.atSeconds * 1000).toLong(), it.values.filterKeys { key -> key !in missing }) }
        }.scoreChecklist()!!

    @Test fun goodScenarioKeepsAllSevenChecksAfterLightsAreOff() {
        val results = checklistResults(score(ChecklistScenarios.good))
        assertEquals(listOf("도어", "안전벨트", "기어", "브레이크 / 시동", "좌 지시등", "우 지시등", "비상등"), results.map { it.label })
        assertTrue(results.all { it.passed == true && it.mark == "✓" })
        assertTrue(results[1].detail.contains("벨트 3초 · 벨트 먼저"))
        assertTrue(results[3].detail.contains("시동 8초"))
        assertEquals(2, results.count { Regex("\\d").containsMatchIn(it.detail) })
    }

    @Test fun badScenarioShowsOrderDoorBrakeAndSkippedHazardFailures() {
        val results = checklistResults(score(ChecklistScenarios.bad))
        assertEquals(listOf(false, false, true, false, true, true, false), results.map { it.passed })
        assertTrue(results[1].detail.contains("시동 먼저"))
        assertEquals("✗", results.last().mark)
    }

    @Test fun missingNewSignalsAreUnmeasuredRatherThanFailed() {
        val results = checklistResults(score(ChecklistScenarios.good,
            setOf(V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN, V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING)))
        assertEquals(listOf(null, true, true, null, null, null, null), results.map { it.passed })
        assertEquals(5, results.count { it.mark == "미측정" })
    }

    @Test fun missingBeltIgnitionAndGearDoNotLookLikeUncheckedActions() {
        val results = checklistResults(score(ChecklistScenarios.good,
            setOf(V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED, V.VEHICLE_LOWVOLTAGESYSTEMSTATE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR)))
        assertTrue(results.take(4).all { it.passed == null })
        assertTrue(results.none { Regex("\\d").containsMatchIn(it.detail) })
    }

    private val state = ManeuverDisplayState("0", null, "P", null, false, null, null, null, 1, 0, 0, false,
        SignalAvailability.MISSING, SignalAvailability.LIVE, SignalAvailability.MISSING,
        taskType = TaskType.CHECKLIST, brakePressed = true, ignitionOn = true, brakeAtIgnition = true,
        brakeSignal = SignalAvailability.LIVE, ignitionSignal = SignalAvailability.SIMULATED)

    @Test fun combinedChipPreservesMixedSourcesAndRequiresBothReadings() {
        val combined = state.checklistIgnition()
        assertEquals("밟음 → 켜짐", combined.value)
        assertTrue(combined.satisfied)
        assertEquals("브레이크 실신호\n시동 시뮬레이션", combined.source)
        listOf(state.copy(brakeSignal = SignalAvailability.MISSING), state.copy(ignitionSignal = SignalAvailability.MISSING),
            state.copy(brakeAtIgnition = null), state.copy(ignitionOn = null)).forEach {
            assertNull(it.checklistIgnition().value)
            assertFalse(it.checklistIgnition().satisfied)
        }
    }

    @Test fun ignitionKeepsRecordedBrakeAfterReleaseAndRejectsLateBrake() {
        assertEquals("밟음 → 켜짐", state.copy(brakePressed = false).checklistIgnition().value)
        assertTrue(state.copy(brakePressed = false).checklistIgnition().satisfied)
        assertEquals("브레이크 없이 켜짐", state.copy(brakeAtIgnition = false).checklistIgnition().value)
        assertFalse(state.copy(brakeAtIgnition = false).checklistIgnition().satisfied)
        assertEquals("꺼짐", state.copy(ignitionOn = false).checklistIgnition().value)
    }

    @Test fun lightsDistinguishCurrentlyOnPreviouslyCheckedPendingAndMissing() {
        assertEquals("켜짐", checklistLightValue(true, true))
        assertEquals("켜짐", checklistLightValue(true, false))
        assertEquals("확인", checklistLightValue(false, true))
        assertEquals("아직", checklistLightValue(false, false))
        assertNull(checklistLightValue(null, null))
    }
}
