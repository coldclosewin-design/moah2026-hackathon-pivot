package com.moah.hackathon.ports

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.scoring.ChecklistRubric
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.RemarkPool
import kotlinx.coroutines.test.runTest

/** 조언 한 문장 — 가장 먼저 고칠 것 하나, 숫자 없음. (동승자 판은 9/28 제거 — `CompanionRulesTest` 에서 운전자 부분만 남김) */
class AdviceRulesTest {
    private val digits = Regex("\\d")

    private fun parking(scenario: Scenario): ParkingScore {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in scenario.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return r.score()!!
    }

    private fun checklist(scenario: Scenario): ParkingScore {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.CHECKLIST_KEYS, simulated = true), ParkingRecorder.CHECKLIST_KEYS)
        var last = 0L
        for (s in scenario.steps) { last = (s.atSeconds * 1000).toLong(); r.onDelta(last, s.values) }
        return r.scoreChecklist(ChecklistRubric(), last)!!
    }

    @Test
    fun `every advice has a driver line without digits`() {
        AdviceRules.Advice.entries.forEach {
            assertTrue(it.name, it.driver.isNotBlank())
            assertFalse(it.name, digits.containsMatchIn(it.driver))
        }
    }

    @Test
    fun `bad parking points at the belt and good parking only keeps`() {
        val bad = AdviceRules.pick(SeedCatalog.parkingTask, parking(ParkingScenarios.bad))
        assertEquals(AdviceRules.Advice.BELT_FIRST, bad)
        assertEquals(bad.driver, AdviceRules.advice(SeedCatalog.parkingTask, parking(ParkingScenarios.bad)))
        assertEquals(AdviceRules.Advice.KEEP, AdviceRules.pick(SeedCatalog.parkingTask, parking(ParkingScenarios.good)))
    }

    @Test
    fun `front parking spec - a reverse correction and a close call get the forward sentences`() {
        // 전면 직각 주차(10/2): 같은 지표, 문장의 방향만 — 보정은 후진, 가까운 쪽은 앞
        val front = SeedCatalog.parkingTask.copy(id = "parking-front", parking = com.moah.hackathon.scoring.ParkingSpec.FRONT_PERPENDICULAR)
        val gear = mobis.vss.VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR
        val belt = mobis.vss.VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED
        val warn = mobis.vss.VssConstants.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING
        val fix = com.moah.hackathon.vehicle.scenario("front-fix", "후진 보정 한 번") {
            at(0.0, belt to "true", gear to com.moah.hackathon.vehicle.Gear.DRIVE.vss, warn to "false"); speed(0.0, 0.0)
            speedRamp(2.0, 3.0, 0.0, 3.0); speedRamp(6.0, 7.0, 3.0, 0.0)
            at(8.0, gear to com.moah.hackathon.vehicle.Gear.REVERSE.vss)          // D → R → D = 전환 2
            speedRamp(9.0, 10.0, 0.0, 3.0); speedRamp(12.0, 13.0, 3.0, 0.0)
            at(14.0, gear to com.moah.hackathon.vehicle.Gear.DRIVE.vss)
            speedRamp(15.0, 16.0, 0.0, 3.0); speedRamp(18.0, 19.0, 3.0, 0.0)
            at(20.0, gear to com.moah.hackathon.vehicle.Gear.PARK.vss)
        }
        assertEquals(AdviceRules.Advice.SHIFT_FRONT, AdviceRules.pick(front, parking(fix)))
        assertEquals(AdviceRules.Advice.SHIFT, AdviceRules.pick(SeedCatalog.parkingTask, parking(fix)))

        val close = com.moah.hackathon.vehicle.scenario("front-close", "앞이 가까움") {
            at(0.0, belt to "true", gear to com.moah.hackathon.vehicle.Gear.DRIVE.vss, warn to "false"); speed(0.0, 0.0)
            speedRamp(2.0, 3.0, 0.0, 3.0); at(5.0, warn to "true"); at(6.0, warn to "false"); speedRamp(7.0, 8.0, 3.0, 0.0)
            at(9.0, gear to com.moah.hackathon.vehicle.Gear.PARK.vss)
        }
        assertEquals(AdviceRules.Advice.PROXIMITY_FRONT, AdviceRules.pick(front, parking(close)))
        assertEquals(AdviceRules.Advice.PROXIMITY, AdviceRules.pick(SeedCatalog.parkingTask, parking(close)))
    }

    @Test
    fun `bad predrive check maps to belt before ignition first - before door brake and lights`() {
        assertEquals(AdviceRules.Advice.BELT_BEFORE_IGNITION, AdviceRules.pick(SeedCatalog.predriveTask, checklist(ChecklistScenarios.bad)))
        assertEquals(AdviceRules.Advice.KEEP_ORDER, AdviceRules.pick(SeedCatalog.predriveTask, checklist(ChecklistScenarios.good)))
    }

    @Test
    fun `a rough predrive session names the door too in the safety sentence - seven steps since 9-28`() = runTest {
        val record = AttemptRecord(1, SeedCatalog.TASK_PREDRIVE, LessonMode.HINT, checklist(ChecklistScenarios.bad), null, "서두.\n조언.", 0)
        val summary = FakeCoachPort(RemarkPool(SeedCatalog.remarks)).summarize(SeedCatalog.predriveTask, LessonMode.HINT, listOf(record), SeedCatalog.demoProfile)
        assertTrue(summary, summary.lines().last().contains("문이나 벨트"))
        assertFalse(summary, digits.containsMatchIn(summary))
    }

    @Test
    fun `every advice is one polite sentence without digits or gear letters`() {
        // 라운드 11 ① C 절(Codex): 두 문장 조언(MOVED_DURING_CHECK·SEGMENTS)이 서두와 합쳐 세 문장이 됐고 CHECK_PARK·PARK 가 `P` 를 읽었다
        AdviceRules.Advice.entries.forEach { a ->
            assertEquals(a.driver, 1, Regex("[.!?]").findAll(a.driver).count())
            assertTrue(a.driver, a.driver.endsWith("요."))
            assertFalse(a.driver, digits.containsMatchIn(a.driver) || Regex("(^| )[PRND]( |$)").containsMatchIn(a.driver))
        }
    }
}
