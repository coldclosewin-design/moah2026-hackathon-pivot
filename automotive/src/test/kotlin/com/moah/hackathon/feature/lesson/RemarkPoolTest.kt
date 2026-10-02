package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingVerdict
import com.moah.hackathon.vehicle.SignalRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RemarkPoolTest {

    @Test
    fun `same band three times in a row never repeats a line`() {
        val pool = RemarkPool(SeedCatalog.remarks, Random(7))
        val picks = (1..3).map { pool.pick(ScoreBand.GOOD, setOf("any"), emptyMap()) }
        assertEquals(3, picks.toSet().size)
    }

    @Test
    fun `situation tags are preferred over generic lines`() {
        val pool = RemarkPool(SeedCatalog.remarks, Random(1))
        val text = pool.pick(ScoreBand.GOOD, setOf("rusty", "first"), mapOf("years" to "10"))
        assertEquals("주차 과정을 대체로 잘 이어 갔어요.", text)
    }

    @Test
    fun `checklist picks never borrow a parking line and vice versa`() {
        val pool = RemarkPool(SeedCatalog.remarks, Random(2))
        val checklistTexts = SeedCatalog.remarks.filter { it.taskType == TaskType.CHECKLIST }.map { it.text }.toSet()
        ScoreBand.entries.forEach { band ->
            repeat(4) {
                val raw = pool.pick(band, setOf("rusty", "first"), emptyMap(), TaskType.CHECKLIST)
                assertTrue("$band: $raw", checklistTexts.contains(raw))
            }
        }
        val parking = pool.pick(ScoreBand.EXCELLENT, setOf("any"), emptyMap())
        assertFalse(parking, checklistTexts.contains(parking))
    }

    @Test
    fun `placeholders are filled`() {
        val pool = RemarkPool(listOf(RemarkTemplate(ScoreBand.OK, setOf("any"), "{segments}번, {years}년차 {name}")), Random(1))
        assertEquals("4번, 10년차 연수생", pool.pick(ScoreBand.OK, emptySet(), mapOf("segments" to "4", "years" to "10", "name" to "연수생")))
    }

    @Test
    fun `score bands`() {
        assertEquals(ScoreBand.EXCELLENT, ScoreBand.of(100))
        assertEquals(ScoreBand.EXCELLENT, ScoreBand.of(90))
        assertEquals(ScoreBand.GOOD, ScoreBand.of(89))
        assertEquals(ScoreBand.OK, ScoreBand.of(60))
        assertEquals(ScoreBand.ROUGH, ScoreBand.of(49))
    }

    @Test
    fun `seed pool has enough lines per band to avoid repeats`() {
        ScoreBand.entries.forEach { band ->
            val n = SeedCatalog.remarks.count { it.band == band }
            assertTrue("$band has $n", n >= 4)
        }
        assertFalse(SeedCatalog.remarks.any { it.text.contains("하위") })
    }

    @Test
    fun `every seed opener is one complete polite sentence without numbers or unguarded counts`() {
        val measuredEntryPhrases = mapOf("한 번에 들어갔어요." to "one_go", "한 번 다시 넣고 들어갔어요." to "one_fix")
        SeedCatalog.remarks.forEach { template ->
            val text = template.text
            assertEquals(text, 1, Regex("[.!?]").findAll(text).count())
            assertTrue(text, text.endsWith("요."))
            assertFalse(text, Regex("\\d|회차|[{}]").containsMatchIn(text))
            if (Regex("[한두세네] 번").containsMatchIn(text)) {
                assertEquals(text, TaskType.PARKING, template.taskType)
                assertTrue(text, measuredEntryPhrases[text] in template.tags)
            }
        }
    }

    @Test
    fun `good parking scenario prefers one_go then a fresh generic opener without aligned-only seeds`() = runBlocking {
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        ParkingScenarios.good.steps.forEach { recorder.onDelta((it.atSeconds * 1000).toLong(), it.values) }
        val score = recorder.score()!!
        val verdict = recorder.verdict()!!
        assertEquals(ParkingVerdict.Entry.ONE_GO, verdict.entry)
        val coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(7)))
        val openers = (1..2).map { attempt ->
            coach.remark(SeedCatalog.parkingTask, score, null, SeedCatalog.demoProfile, attempt, verdict).substringBefore('\n')
        }
        assertFalse(SeedCatalog.remarks.any { it.tags == setOf("aligned") })
        assertEquals("한 번에 들어갔어요.", openers.first())
        assertEquals(2, openers.toSet().size)
        assertTrue(SeedCatalog.remarks.any { it.text == openers.last() && it.tags == setOf("any") })
    }

    @Test
    fun `bad parking scenario picks one_fix rather than a many line from its OK band`() = runBlocking {
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        ParkingScenarios.bad.steps.forEach { recorder.onDelta((it.atSeconds * 1000).toLong(), it.values) }
        val score = recorder.score()!!
        val verdict = recorder.verdict()!!
        assertEquals(ScoreBand.OK, ScoreBand.of(score.skill))
        val coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(7)))
        assertEquals("한 번 다시 넣고 들어갔어요.",
            coach.remark(SeedCatalog.parkingTask, score, null, SeedCatalog.demoProfile, 1, verdict).substringBefore('\n'))
    }

    @Test
    fun `seed verdict claims remain truthful across every band and recent fallback`() {
        val required = mapOf("한 번에 들어갔어요." to "one_go", "한 번 다시 넣고 들어갔어요." to "one_fix",
            "여러 번 오가며 들어갔어요." to "many", "신호로 추정하면 방향도 맞게 섰어요." to "aligned")
        ScoreBand.entries.forEach { band ->
            val pool = RemarkPool(SeedCatalog.remarks, Random(1))
            listOf(emptySet(), setOf("one_go"), setOf("one_fix"), setOf("many"), setOf("one_go", "aligned")).forEach { tags ->
                repeat(12) {
                    val text = pool.pick(band, tags + setOf("rusty", "first"), emptyMap())
                    required[text]?.let { assertTrue("$band $tags: $text", it in tags) }
                }
            }
        }
        listOf(ScoreBand.OK, ScoreBand.ROUGH).forEach { band ->
            assertEquals("여러 번 오가며 들어갔어요.", RemarkPool(SeedCatalog.remarks).pick(band, setOf("many"), emptyMap()))
        }
    }

    @Test
    fun `verdict tags are conditions - a one_go line is never picked without the verdict, even in the fallback`() {
        // 라운드 12 ① C 절(Codex): 단순 태그 가점이면 best=0·폴백에서 사실이 아닌 문장이 뽑힐 수 있다
        val oneGo = RemarkTemplate(ScoreBand.EXCELLENT, setOf("one_go"), "한 번에 들어갔어요.")
        val aligned = RemarkTemplate(ScoreBand.EXCELLENT, setOf("one_go", "aligned"), "한 번에, 방향도 맞게 들어갔어요.")
        val plain = RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "주차 과정이 전반적으로 좋았어요.")
        val pool = RemarkPool(listOf(oneGo, aligned, plain), Random(1), recentSize = 0)
        repeat(10) { assertEquals(plain.text, pool.pick(ScoreBand.EXCELLENT, setOf("rusty"), emptyMap())) }          // 판정 없음
        repeat(10) { assertEquals(plain.text, pool.pick(ScoreBand.EXCELLENT, setOf("one_fix"), emptyMap())) }        // 다른 판정
        repeat(10) { assertEquals(oneGo.text, pool.pick(ScoreBand.EXCELLENT, setOf("one_go"), emptyMap())) }         // 맞는 판정 → 태그 겹침이 많아 우선
        repeat(10) { assertEquals(aligned.text, pool.pick(ScoreBand.EXCELLENT, setOf("one_go", "aligned"), emptyMap())) }
        // 다른 밴드로 폴백해도 조건은 지킨다
        val onlyVerdict = RemarkPool(listOf(oneGo, plain), Random(1), recentSize = 0)
        repeat(10) { assertEquals(plain.text, onlyVerdict.pick(ScoreBand.ROUGH, setOf("many"), emptyMap())) }
    }

    @Test
    fun `verdict tags come from the verdict - heading only when aligned, nothing without a verdict`() {
        val v = com.moah.hackathon.scoring.ParkingVerdict(
            com.moah.hackathon.scoring.ParkingVerdict.Entry.ONE_FIX, com.moah.hackathon.scoring.ParkingVerdict.Heading.SLIGHT, 18f,
            com.moah.hackathon.scoring.ParkingVerdict.Finish.CLEAN, com.moah.hackathon.scoring.ParkingVerdict.Safety.SAFE)
        assertEquals(setOf("one_fix"), RemarkPool.verdictTags(v))
        assertEquals(setOf("one_go", "aligned"), RemarkPool.verdictTags(v.copy(
            entry = com.moah.hackathon.scoring.ParkingVerdict.Entry.ONE_GO, heading = com.moah.hackathon.scoring.ParkingVerdict.Heading.ALIGNED)))
        assertEquals(emptySet<String>(), RemarkPool.verdictTags(null))
    }
}
