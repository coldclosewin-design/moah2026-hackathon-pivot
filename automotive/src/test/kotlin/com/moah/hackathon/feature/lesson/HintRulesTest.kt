package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.ports.SpeechPriority
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HintRulesTest {

    private fun run(scenario: com.moah.hackathon.vehicle.Scenario, cooldown: Long = 5_000L, checklist: Boolean = false): List<Hint> {
        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        val rules = HintRules(cooldown, checklist)
        var snap = VehicleSnapshot()
        val out = ArrayList<Hint>()
        for (step in scenario.steps) {
            val t = (step.atSeconds * 1000).toLong()
            snap = snap.apply(step.values)
            recorder.onDelta(t, step.values)
            out += rules.evaluate(t, recorder.metrics(), snap)
        }
        return out
    }

    @Test
    fun `good parking produces no hints`() {
        assertTrue(run(ParkingScenarios.good).isEmpty())
    }

    @Test
    fun `bad parking speaks belt proximity harsh steering and gear hints - safety ones urgent`() {
        val hints = run(ParkingScenarios.bad)
        val texts = hints.map { it.text }
        assertTrue(texts.toString(), "안전벨트가 아직이에요." in texts)
        assertTrue("뒤가 가까워요. 멈추세요." in texts)
        assertTrue("제동이 급했어요. 브레이크는 천천히 밟아요." in texts)
        assertTrue(texts.any { it.startsWith("핸들을 조금 더 유지") })
        assertTrue(texts.any { it.startsWith("전진으로 보정") })
        hints.filter { it.text.startsWith("뒤가") || it.text.startsWith("제동") || it.text.startsWith("안전벨트") }
            .forEach { assertEquals(SpeechPriority.URGENT, it.priority) }
        // 벨트 힌트는 첫 이동 뒤에 한 번만 — 출발 전에 외치지 않는다
        assertEquals(1, texts.count { it == "안전벨트가 아직이에요." })
    }

    @Test
    fun `checklist rules - ignition before belt is urgent and once, door and brake at ignition once, a creep is a calm reminder, a good check is silent`() {
        assertTrue(run(ChecklistScenarios.good, checklist = true).isEmpty())
        val hints = run(ChecklistScenarios.bad, checklist = true)
        assertEquals(listOf("시동보다 안전벨트가 먼저예요. 지금 매 주세요.", "문이 아직 열려 있어요. 닫고 시작해요.", "시동은 브레이크를 밟고 켜요.",
            "아직 출발 전이에요. 차는 세운 채로 점검만 해요."), hints.map { it.text })
        assertEquals(listOf(SpeechPriority.URGENT, SpeechPriority.NORMAL, SpeechPriority.NORMAL, SpeechPriority.NORMAL), hints.map { it.priority })
        // 점검 과제에서는 주차 규칙(벨트 없이 이동)을 따로 외치지 않는다 — 한 상황에 한 마디
        assertTrue(hints.none { it.text == "안전벨트가 아직이에요." })
    }

    @Test
    fun `cooldown collapses repeats of the same rule`() {
        val few = run(ParkingScenarios.bad, cooldown = 60_000L)
        assertEquals(1, few.count { it.text.startsWith("핸들을 조금 더 유지") })
        assertEquals(1, few.count { it.text.startsWith("전진으로 보정") })
    }
}
