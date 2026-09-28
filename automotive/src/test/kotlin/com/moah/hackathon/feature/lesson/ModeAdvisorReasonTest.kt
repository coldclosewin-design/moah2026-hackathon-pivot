package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** 모드 제안 이유는 Setup 화면과 리포트 TTS 에 그대로 나간다 — 운전자 문장이라 숫자 없음(9/28 동승자 화면 리뷰에서 "가이드를 0번 통과했어요" 발견). */
class ModeAdvisorReasonTest {
    private val digits = Regex("\\d")
    private val task = SeedCatalog.parkingTask

    private fun score(scenario: Scenario): ParkingScore {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in scenario.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return r.score()!!
    }

    private fun record(index: Int, mode: LessonMode, scenario: Scenario) =
        AttemptRecord(index, task.id, mode, score(scenario), null, "서두.\n조언.", 0)

    @Test
    fun `every suggestion path reads without digits`() {
        val store = ProgressStore()
        val reasons = ArrayList<String>()
        reasons += ModeAdvisor.suggest(task, store).reason                       // 처음 → 가이드
        store.add(record(1, LessonMode.GUIDE, ParkingScenarios.bad))
        reasons += ModeAdvisor.suggest(task, store).reason                       // 가이드 미통과 → 가이드 다시
        store.add(record(2, LessonMode.HINT, ParkingScenarios.bad))
        val afterHint = ModeAdvisor.suggest(task, store)
        assertEquals(LessonMode.HINT, afterHint.mode)                           // 힌트로 해 봤으면 힌트 유지 — "0번 통과" 가 나오던 자리
        reasons += afterHint.reason
        store.add(record(3, LessonMode.GUIDE, ParkingScenarios.good)); store.add(record(4, LessonMode.GUIDE, ParkingScenarios.good))
        reasons += ModeAdvisor.suggest(task, store).reason                       // 가이드 통과 2회 → 힌트
        store.add(record(5, LessonMode.HINT, ParkingScenarios.good)); store.add(record(6, LessonMode.HINT, ParkingScenarios.good))
        val evaluate = ModeAdvisor.suggest(task, store)
        assertEquals(LessonMode.EVALUATE, evaluate.mode)
        reasons += evaluate.reason
        reasons.forEach { assertFalse(it, digits.containsMatchIn(it)) }
        assertEquals(5, reasons.toSet().size)
    }
}
