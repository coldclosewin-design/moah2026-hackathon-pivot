package com.moah.hackathon.ports

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.RemarkPool
import com.moah.hackathon.feature.lesson.ScoreBand
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.scoring.ChecklistRubric
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 동승자 두 문장 — 잘한 것 하나 · 도울 것 하나. 숫자 없음, 운전자 조언과 같은 사실을 옆자리 사람에게. */
class CompanionRulesTest {
    private val digits = Regex("\\d")

    private fun parking(scenario: Scenario): ParkingScore {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in scenario.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return r.score()!!
    }

    private fun checklist(scenario: Scenario): ParkingScore {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        var last = 0L
        for (s in scenario.steps) { last = (s.atSeconds * 1000).toLong(); r.onDelta(last, s.values) }
        return r.scoreChecklist(ChecklistRubric(), last)!!
    }

    private fun record(index: Int, taskId: String, score: ParkingScore) =
        AttemptRecord(index, taskId, LessonMode.HINT, score, null, "서두.\n조언.", 0)

    @Test
    fun `every advice has a driver line and a companion line without digits`() {
        AdviceRules.Advice.entries.forEach {
            assertTrue(it.name, it.driver.isNotBlank() && it.companion.isNotBlank())
            assertFalse(it.name, digits.containsMatchIn(it.driver) || digits.containsMatchIn(it.companion))
            assertTrue(it.name, it.companion.endsWith("주세요."))
        }
        CompanionRules.PRAISE.forEach { assertFalse(it.text, digits.containsMatchIn(it.text)) }
        // 주차·점검 모두 4밴드가 비지 않는다
        listOf(TaskType.PARKING, TaskType.CHECKLIST).forEach { type ->
            ScoreBand.entries.forEach { band ->
                assertTrue("$type $band", CompanionRules.PRAISE.any { it.taskType == type && it.band == band })
            }
        }
    }

    @Test
    fun `bad parking asks the companion to check the belt and good parking asks for nothing but praise`() {
        val bad = AdviceRules.pick(SeedCatalog.parkingTask, parking(ParkingScenarios.bad))
        assertEquals(AdviceRules.Advice.BELT_FIRST, bad)
        assertEquals(bad.driver, AdviceRules.advice(SeedCatalog.parkingTask, parking(ParkingScenarios.bad)))
        val good = AdviceRules.pick(SeedCatalog.parkingTask, parking(ParkingScenarios.good))
        assertEquals(AdviceRules.Advice.KEEP, good)
        assertEquals("오늘은 그냥 잘했다고 해 주세요.", good.companion)
    }

    @Test
    fun `bad predrive check maps to belt before ignition for the companion`() {
        val advice = AdviceRules.pick(SeedCatalog.predriveTask, checklist(ChecklistScenarios.bad))
        assertEquals(AdviceRules.Advice.BELT_BEFORE_IGNITION, advice)
    }

    @Test
    fun `note praises the best attempt and helps with the last one`() {
        val pool = RemarkPool(CompanionRules.PRAISE, Random(1))
        val task = SeedCatalog.parkingTask
        val attempts = listOf(record(1, task.id, parking(ParkingScenarios.bad)), record(2, task.id, parking(ParkingScenarios.good)))
        val note = CompanionRules.note(task, attempts, pool)
        // 칭찬은 최고 회차(잘한 주차, EXCELLENT), 도울 것은 마지막 회차(잘한 주차 → 고칠 것 없음)
        assertTrue(note.praise, CompanionRules.PRAISE.filter { it.band == ScoreBand.EXCELLENT && it.taskType == TaskType.PARKING }.any { it.text == note.praise })
        assertEquals(AdviceRules.Advice.KEEP.companion, note.help)
        assertFalse(note.text, digits.containsMatchIn(note.text))
        assertEquals("${note.praise}\n${note.help}", note.text)

        val reversed = CompanionRules.note(task, attempts.reversed(), pool)
        assertEquals(AdviceRules.Advice.BELT_FIRST.companion, reversed.help)   // 마지막 회차가 못한 주차
    }

    @Test
    fun `no attempts still gives the companion something to say`() = runTest {
        val note = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(2))).companionNote(SeedCatalog.parkingTask, emptyList(), SeedCatalog.demoProfile)
        assertTrue(note.praise.isNotBlank() && note.help.isNotBlank())
        assertFalse(digits.containsMatchIn(note.text))
    }
}
