package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.FrontParkingScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.ports.CoachPort
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeTtsPort
import com.moah.hackathon.ports.withObjectParticle
import com.moah.hackathon.ports.SpeechPriority
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.vehicle.FakeVehiclePort
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import com.moah.hackathon.vehicle.VssValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LessonStateMachineTest {

    private class Harness(val port: FakeVehiclePort, val tts: FakeTtsPort, val store: ProgressStore, val machine: LessonStateMachine, val scope: CoroutineScope)

    /** 카탈로그가 전부 READY 가 된 뒤(10/4)에도 거절 경로를 시험하려고 넣는 "준비 중" 과제. */
    private val plannedTask = Task("planned-night", "야간 주차", TaskType.PARKING, Difficulty.HARD, "예시", listOf("핸들"), emptySet(),
        requiresDriving = true, status = TaskStatus.PLANNED)

    private fun TestScope.harness(coach: CoachPort? = null, tasks: List<Task> = SeedCatalog.tasks): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val port = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val tts = FakeTtsPort()
        val store = ProgressStore()
        val machine = LessonStateMachine(
            vehicle = port, tts = tts, coach = coach ?: FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + com.moah.hackathon.scoring.CourseRecorder.KEYS, simulated = true), store = store,
            tasks = tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues, benefits = SeedCatalog.benefits,
            profile = SeedCatalog.demoProfile, scope = scope, clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        return Harness(port, tts, store, machine, scope)
    }

    /** 시나리오를 시각대로 주입한다(재생기 대신 직접 — 테스트 시계로). */
    private suspend fun TestScope.feed(h: Harness, scenario: Scenario) {
        var prev = 0.0
        for (step in scenario.steps) {
            val wait = Math.round((step.atSeconds - prev) * 1000)
            if (wait > 0) testScheduler.advanceTimeBy(wait)
            prev = step.atSeconds
            h.port.inject(step.values)
            testScheduler.runCurrent()
        }
    }

    private suspend fun TestScope.openDoor(h: Harness) {
        h.port.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.TRUE))
        advanceUntilIdle()
    }

    @Test
    fun `setup suggests the feared task in guide mode for a first timer`() = runTest {
        val h = harness()
        val setup = h.machine.phase.value as LessonPhase.Setup
        assertEquals(SeedCatalog.TASK_PARKING_REAR, setup.suggestedTask.id)
        assertEquals(LessonMode.GUIDE, setup.suggestedMode)
        h.scope.cancel()
    }

    @Test
    fun `guide mode - good parking is confirmed step by step and scores 100 then door opens the report`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.GUIDE)
        assertTrue(h.machine.phase.value is LessonPhase.Briefing)
        advanceUntilIdle()
        val m0 = h.machine.phase.value as LessonPhase.Maneuver
        assertEquals(1, m0.attempt)
        assertEquals("안전벨트를 매 주세요.", m0.guide!!.say)
        assertTrue(h.tts.spoken.any { it.startsWith("후면 직각 주차, 가이드 모드") })

        feed(h, ParkingScenarios.good)
        val confirms = SeedCatalog.parkingGuide.map { it.confirm }
        assertEquals(confirms, h.tts.spoken.filter { it in confirms })
        val m1 = h.machine.phase.value as LessonPhase.Maneuver
        assertTrue(m1.askedDone)
        assertEquals(2, m1.movingSegments)

        h.machine.finishAttempt()
        advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertEquals(100, done.record.score.skill)
        assertEquals(100, done.record.score.safety)
        // 운전자 문장에 숫자 없음(9/27) — "서두\n조언" 두 문장
        assertEquals(done.record.remark, 2, Regex("[.!?]").findAll(done.record.remark).count())
        assertTrue(done.record.remark, !done.record.remark.contains(Regex("[0-9]+번|[0-9]+초")) && done.record.remark.contains("\n"))
        assertEquals(SpeechPriority.URGENT, h.tts.priorities.last())
        assertEquals(1, h.store.all().size)

        openDoor(h)
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertEquals(1, report.attempts.size)
        assertEquals(100, report.best.skill)
        assertTrue(report.unverifiedGuideSteps.isEmpty())
        assertEquals(LessonMode.GUIDE, report.nextMode) // 아직 가이드 통과 1회
        assertEquals(ShareLevel.entries.size, report.shareLevels.size)
        assertTrue(h.tts.spoken.last().contains("다음엔"))
        h.scope.cancel()
    }

    @Test
    fun `hint mode - bad parking gets urgent hints and scores 60 55`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        advanceUntilIdle()
        feed(h, ParkingScenarios.bad)
        assertTrue(h.tts.spoken.toString(), "안전벨트가 아직이에요." in h.tts.spoken)
        assertTrue("뒤가 가까워요. 멈추세요." in h.tts.spoken)
        assertTrue("제동이 급했어요. 브레이크는 천천히 밟아요." in h.tts.spoken)
        val m = h.machine.phase.value as LessonPhase.Maneuver
        assertTrue(m.askedDone)  // 기어 P + 정차 → "다 되셨나요?"
        assertTrue(h.tts.spoken.any { it.startsWith("다 되셨나요?") })
        assertTrue(m.lastHint != null)

        h.machine.finishAttempt()
        advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertEquals(60, done.record.score.skill)
        assertEquals(55, done.record.score.safety)
        assertEquals(4, done.record.score.metrics.motion.movingSegments)
        h.scope.cancel()
    }

    @Test
    fun `front parking hint mode - forward hints, seven key badge, same fixed scores and a one fix verdict`() = runTest {
        // 전면 직각 주차(10/2): 상태기계는 과제 사양으로 키·힌트·판정을 고른다. 시연 본편(후면)은 그대로
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_FRONT, LessonMode.HINT)
        advanceUntilIdle()
        assertTrue(h.tts.spoken.any { it.startsWith("전면 직각 주차, 힌트 모드") && it.contains("핸들 방향과 기어 전환을") })
        feed(h, FrontParkingScenarios.bad)
        assertTrue(h.tts.spoken.toString(), "안전벨트가 아직이에요." in h.tts.spoken)
        assertTrue(h.tts.spoken.toString(), "앞이 가까워요. 멈추세요." in h.tts.spoken)
        assertTrue(h.tts.spoken.any { it.startsWith("후진으로 보정") })
        assertTrue(h.tts.spoken.none { it.startsWith("뒤가") || it.startsWith("전진으로 보정") })
        val m = h.machine.phase.value as LessonPhase.Maneuver
        assertTrue(m.askedDone)
        assertEquals(7, m.availability.size)   // 뒤 거리 없는 7키
        assertTrue(com.moah.hackathon.vehicle.SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM !in m.availability)

        h.machine.finishAttempt()
        advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertEquals(60, done.record.score.skill)
        assertEquals(55, done.record.score.safety)
        assertEquals(7, done.record.score.badge.simulated)
        assertEquals(com.moah.hackathon.scoring.ParkingVerdict.Entry.ONE_FIX, done.record.verdict!!.entry)
        assertEquals(com.moah.hackathon.scoring.ParkingVerdict.Heading.SLIGHT, done.record.verdict!!.heading)
        assertTrue(done.record.remark, done.record.remark.lines().last() == "다음엔 벨트를 먼저 매고 출발해요.")
        h.scope.cancel()
    }

    @Test
    fun `planned tasks and unsupported modes are refused and the session stays in Setup`() = runTest {
        val h = harness(tasks = SeedCatalog.tasks + plannedTask)
        h.machine.begin(plannedTask.id, LessonMode.HINT)         // 카탈로그에만 있는 과제
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Setup)
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().contains("준비 중"))
        assertTrue(h.tts.spoken.last().contains("후면 직각 주차"))

        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.QUIZ)  // 주차에 지식 테스트
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Setup)
        assertTrue(h.tts.spoken.last().contains("지식 테스트 모드로는"))

        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        h.scope.cancel()
    }

    @Test
    fun `quiz - ten questions, verdict and reason spoken, done with the score, moving locks answers`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_KNOWLEDGE, LessonMode.QUIZ)
        advanceUntilIdle()
        var q = h.machine.phase.value as LessonPhase.Quiz
        assertEquals(0, q.index); assertEquals(10, q.total); assertFalse(q.answered); assertFalse(q.locked)
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().startsWith("1번. 회전교차로"))
        assertTrue(h.tts.spoken.last().contains("둘째, 돌고 있는 차"))

        h.machine.nextQuestion()                       // 답하기 전엔 무시
        assertEquals(0, (h.machine.phase.value as LessonPhase.Quiz).index)

        h.machine.answer(1)                            // 정답
        q = h.machine.phase.value as LessonPhase.Quiz
        assertEquals(1, q.chosen); assertEquals(1, q.correctSoFar)
        assertTrue(h.tts.spoken.last().startsWith("맞아요."))
        assertEquals(SpeechPriority.URGENT, h.tts.priorities.last())
        h.machine.answer(2)                            // 이미 답한 문제는 무시
        assertEquals(1, (h.machine.phase.value as LessonPhase.Quiz).chosen)

        h.machine.nextQuestion()
        q = h.machine.phase.value as LessonPhase.Quiz
        assertEquals(1, q.index); assertFalse(q.answered)

        // 움직이면 잠금 — 답이 무시된다
        h.port.inject(mapOf(VssConstants.VEHICLE_SPEED to "12.0")); advanceUntilIdle()
        assertTrue((h.machine.phase.value as LessonPhase.Quiz).locked)
        h.machine.answer(1)
        assertFalse((h.machine.phase.value as LessonPhase.Quiz).answered)
        h.port.inject(mapOf(VssConstants.VEHICLE_SPEED to "0.0")); advanceUntilIdle()
        assertFalse((h.machine.phase.value as LessonPhase.Quiz).locked)

        h.machine.answer(0)                            // 오답
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().startsWith("아쉬워요. 정답은 갑자기 서거나"))
        h.machine.nextQuestion()
        h.machine.answer(2); h.machine.nextQuestion()  // 3번 정답
        h.machine.answer(1); h.machine.nextQuestion()  // 4번 정답
        h.machine.answer(0)                            // 5번 정답, 아직 마지막이 아니다
        assertFalse((h.machine.phase.value as LessonPhase.Quiz).isLast)
        SeedCatalog.quiz.drop(5).forEachIndexed { index, item ->
            h.machine.nextQuestion()
            h.machine.answer(item.answer)
            val current = h.machine.phase.value as LessonPhase.Quiz
            assertEquals(index + 5, current.index)
            assertEquals(index == 4, current.isLast)
        }
        assertTrue((h.machine.phase.value as LessonPhase.Quiz).isLast)
        h.machine.nextQuestion()                       // 결과 보기
        val done = h.machine.phase.value as LessonPhase.QuizDone
        assertEquals(9, done.correct); assertEquals(10, done.total)
        assertEquals(listOf(true, false) + List(8) { true }, done.results.map { it.correct })
        assertTrue(done.remark, done.remark.contains("10문제 중 9개"))
        assertEquals(1, h.store.quizzes().size)
        h.machine.reset()
        assertEquals(null, h.tts.lastSpoken.value)     // 9/30: 그만하기·다시 시작 뒤 Setup 에 지난 해설 자막이 남지 않는다
        assertTrue(h.machine.phase.value is LessonPhase.Setup)
        h.scope.cancel()
    }

    @Test
    fun `quiz - ending early keeps the answered ones and a task without items goes back to setup`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_KNOWLEDGE, LessonMode.QUIZ)
        advanceUntilIdle()
        h.machine.answer(1); h.machine.nextQuestion(); h.machine.answer(0)
        h.machine.endSession()
        val done = h.machine.phase.value as LessonPhase.QuizDone
        assertEquals(2, done.results.size); assertEquals(1, done.correct); assertEquals(10, done.total)
        h.scope.cancel()
    }

    @Test
    fun `predrive check - guide confirms door belt P ignition and three lights without moving, scores 100 and the door opens the report`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PREDRIVE, LessonMode.GUIDE)
        advanceUntilIdle()
        val m0 = h.machine.phase.value as LessonPhase.Maneuver
        assertEquals("안전벨트를 매 주세요.", m0.guide!!.say)   // 7단계(9/28): 1단계 "문" 은 이미 닫혀 있어 시작과 함께 확인된다
        assertTrue(h.tts.spoken.toString(), "운전석 문을 닫아 주세요." in h.tts.spoken && "닫혔어요." in h.tts.spoken)
        assertTrue(h.tts.spoken.first(), h.tts.spoken.first().startsWith("출발 전 점검, 가이드 모드"))

        feed(h, ChecklistScenarios.good)
        val confirms = SeedCatalog.predriveGuide.map { it.confirm }
        assertEquals(confirms, h.tts.spoken.filter { it in confirms })
        val m1 = h.machine.phase.value as LessonPhase.Maneuver
        assertTrue(m1.askedDone)                       // 가이드가 끝났다
        assertEquals(0, m1.movingSegments)
        // 9/30: 등화는 켰다 끄므로 지금 값은 꺼짐이지만, 기록된 확인은 남는다(칩 완료 색의 근거). 시동 순간 브레이크도
        val d1 = m1.toDisplayState()
        assertEquals(false, d1.indicatorLeft); assertEquals(false, d1.hazard)
        assertEquals(listOf(true, true, true, true), listOf(d1.leftIndicatorChecked, d1.rightIndicatorChecked, d1.hazardChecked, d1.brakeAtIgnition))
        assertTrue(h.tts.spoken.none { it.startsWith("다 되셨나요?") })   // 가이드 모드는 마지막 단계 확인 문장이 그 역할

        h.machine.finishAttempt()
        advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertEquals(100, done.record.score.skill)
        assertEquals(100, done.record.score.safety)
        assertTrue(done.record.remark, !done.record.remark.contains("초.") && done.record.remark.endsWith("이 순서 그대로 몸에 남겨 두세요."))
        assertFalse(done.record.remark.contains("들어갔"))   // 주차 멘트가 섞이지 않는다
        assertEquals(done.record.remark, 2, Regex("[.!?]").findAll(done.record.remark).count())

        openDoor(h)
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertEquals(SeedCatalog.TASK_PREDRIVE, report.task.id)
        assertEquals(100, report.best.skill)
        assertTrue(report.unverifiedGuideSteps.isEmpty())
        h.scope.cancel()
    }

    @Test
    fun `predrive check - hint mode speaks order door brake hints when the engine starts wrong and scores 30 40`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PREDRIVE, LessonMode.HINT)
        advanceUntilIdle()
        feed(h, ChecklistScenarios.bad)
        assertTrue(h.tts.spoken.toString(), "시동보다 안전벨트가 먼저예요. 지금 매 주세요." in h.tts.spoken)
        assertTrue(h.tts.spoken.any { it.startsWith("아직 출발 전이에요") })
        assertTrue(h.tts.spoken.toString(), "문이 아직 열려 있어요. 닫고 시작해요." in h.tts.spoken && "시동은 브레이크를 밟고 켜요." in h.tts.spoken)
        val m = h.machine.phase.value as LessonPhase.Maneuver
        assertTrue(!m.askedDone)                       // 비상등을 건너뛰어 아직 "다 되셨나요?" 를 묻지 않는다(7단계: 신호 있는 등화까지 봐야 끝) — 버튼으로 끝낸다
        assertTrue(h.tts.spoken.none { it.startsWith("다 되셨나요?") })

        h.machine.finishAttempt()
        advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertEquals(30, done.record.score.skill)
        assertEquals(40, done.record.score.safety)
        assertTrue(done.record.remark, !done.record.remark.contains("초.") && done.record.remark.endsWith("다음엔 벨트가 먼저, 시동은 그다음이에요."))
        assertEquals(done.record.remark, 2, Regex("[.!?]").findAll(done.record.remark).count())
        h.machine.endSession(); advanceUntilIdle()
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertTrue(report.summary, report.summary.startsWith("한 번 해 봤어요.") && report.summary.lines().size == 2
            && !Regex("\\d").containsMatchIn(report.summary))
        h.scope.cancel()
    }

    @Test
    fun `a door already open at attempt start does not end the attempt - closing then opening does`() = runTest {
        val h = harness()
        h.port.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.TRUE))   // 타는 중 — 문이 열린 채 시작
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        feed(h, ParkingScenarios.good)                 // 정차 구간이 여러 번 있지만 문이 계속 열려 있어도 끝나지 않는다
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        assertTrue(h.store.all().isEmpty())
        h.port.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.FALSE)); advanceUntilIdle()   // 닫음 → 무장
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        openDoor(h)                                    // 다시 열림 → 회차 종료 + 리포트
        assertTrue(h.machine.phase.value is LessonPhase.Report)
        assertEquals(1, h.store.all().size)
        h.scope.cancel()
    }

    @Test
    fun `spoken lines carry the right particles`() = runTest {
        val h = harness(tasks = SeedCatalog.tasks + plannedTask)
        h.machine.begin(plannedTask.id, LessonMode.HINT)
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().startsWith("야간 주차는 아직 준비 중이에요. 지금은 출발 전 점검·"))
        // 끝은 마지막 READY 과제 + 목적격 조사 — 과제가 늘어도 조사 규칙을 본다(라운드 25 에서 과제가 늘었다)
        val lastReady = SeedCatalog.tasks.last { it.isReady }.title
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().endsWith("${lastReady.withObjectParticle()} 할 수 있어요."))
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.QUIZ)
        assertTrue(h.tts.spoken.last().startsWith("후면 직각 주차는 지식 테스트 모드로는"))
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.GUIDE)
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().endsWith("오늘은 핸들 방향과 기어 전환과 뒤 거리를 봅니다."))
        h.machine.reset()
        h.machine.begin(SeedCatalog.TASK_PREDRIVE, LessonMode.HINT)
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().endsWith("오늘은 문과 안전벨트와 기어 P와 시동과 지시등을 봅니다."))
        h.scope.cancel()
    }

    @Test
    fun `door opening while moving is harmless`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        h.port.inject(mapOf(VssConstants.VEHICLE_SPEED to "3.0", VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.TRUE))
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        h.scope.cancel()
    }

    @Test
    fun `second attempt carries a delta against the first and the report summarises both`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        feed(h, ParkingScenarios.bad)
        h.machine.finishAttempt(); advanceUntilIdle()
        h.machine.nextAttempt(); advanceUntilIdle()
        assertEquals(2, (h.machine.phase.value as LessonPhase.Maneuver).attempt)
        // 회차 시작 한 마디는 번호 없이(운전자 문장 숫자 금지 — 감사 08 A1-02)
        assertTrue(h.tts.spoken.toString(), "다시 시작해요. 조용히 볼게요." in h.tts.spoken)
        assertTrue(h.tts.spoken.none { it.contains("회차") && it.any(Char::isDigit) })
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt(); advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        val delta: ParkingDelta = done.record.delta!!
        assertEquals(-2, delta.segments)
        assertEquals(-2, delta.reversals)
        h.machine.endSession(); advanceUntilIdle()
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertEquals(2, report.attempts.size)
        assertEquals(100, report.best.skill)
        assertTrue(report.summary, report.summary.contains("좋아졌어요") && !report.summary.contains("점"))
        h.machine.reset()
        assertTrue(h.machine.phase.value is LessonPhase.Setup)
        h.scope.cancel()
    }

    @Test
    fun `finish button and door together record the attempt only once`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt()
        h.machine.finishAttempt()
        openDoor(h)
        assertTrue(h.machine.phase.value is LessonPhase.Report)
        assertEquals(1, h.store.all().size)
        h.scope.cancel()
    }

    @Test
    fun `a failing coach falls back to a rule sentence and the session continues`() = runTest {
        val angry = object : CoachPort {
            override suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int, verdict: com.moah.hackathon.scoring.ParkingVerdict?,
                course: com.moah.hackathon.scoring.CourseResult?): String = throw IllegalStateException("no network")
            override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String = throw IllegalStateException("no network")
        }
        val h = harness(angry)
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt(); advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertTrue(done.record.remark, done.record.remark.startsWith("수고했어요.\n"))
        h.machine.endSession(); advanceUntilIdle()
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertTrue(report.summary, report.summary.contains("수고했어요") && !report.summary.contains("점"))
        assertFalse(h.tts.spoken.isEmpty())
        h.scope.cancel()
    }

    @Test
    fun `reservation - a booked course puts its READY task first, cancel restores, outside Setup and unavailable slots are ignored`() = runTest {
        val h = harness()
        val noFear = Profile("x", ProfileStatement())   // 공포 없음 → 원래 제안은 첫 쉬운 과제(출발 전 점검)
        val m = LessonStateMachine(
            vehicle = h.port, tts = h.tts, coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.KEYS, simulated = true), store = h.store,
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor,
            venues = SeedCatalog.venues, benefits = SeedCatalog.benefits,
            profile = noFear, scope = h.scope, clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        val before = m.phase.value as LessonPhase.Setup
        assertEquals(SeedCatalog.TASK_PREDRIVE, before.suggestedTask.id)
        assertEquals(null, before.booking)
        assertEquals(3, before.venues.size)

        // 자리 없는 시간대·모르는 코스는 무시
        m.reserve("venue-seocho", "slot-16", SeedCatalog.COURSE_PARKING)
        assertEquals(null, h.store.reservation)
        m.reserve("venue-seocho", "slot-14", "course-none")
        assertEquals(null, h.store.reservation)

        m.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_PARKING)
        val booked = m.phase.value as LessonPhase.Setup
        assertEquals("venue-seocho", h.store.reservation?.venueId)
        assertEquals(SeedCatalog.TASK_PARKING_REAR, booked.suggestedTask.id)        // 주차 3종의 첫 READY = 후면 직각
        assertTrue(booked.reason, booked.reason.startsWith(ModeAdvisor.RESERVED_REASON) && !Regex("\\d").containsMatchIn(booked.reason))

        // Setup 밖에서는 예약·취소를 받지 않는다
        m.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT); advanceUntilIdle()
        m.cancelReservation()
        assertEquals("venue-seocho", h.store.reservation?.venueId)
        m.reset()
        assertEquals(SeedCatalog.TASK_PARKING_REAR, (m.phase.value as LessonPhase.Setup).suggestedTask.id)   // reset 이 예약을 지우지 않는다

        m.cancelReservation()
        val after = m.phase.value as LessonPhase.Setup
        assertEquals(null, h.store.reservation)
        assertEquals(SeedCatalog.TASK_PREDRIVE, after.suggestedTask.id)
        assertTrue(!after.reason.startsWith(ModeAdvisor.RESERVED_REASON))
        h.scope.cancel()
    }

    @Test
    fun `result screens lock while the car moves and unlock when it stops - door exit still works`() = runTest {
        // 감사 08 A1-01: 정차 전용 결과 화면(Done·Report)에서 다시 움직이면 터치 타깃을 숨겨야 한다(절대 규칙 10)
        val speed = mobis.vss.VssConstants.VEHICLE_SPEED
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt(); advanceUntilIdle()
        assertFalse((h.machine.phase.value as LessonPhase.Done).locked)
        h.port.inject(mapOf(speed to "20.0")); testScheduler.runCurrent()
        assertTrue((h.machine.phase.value as LessonPhase.Done).locked)
        h.port.inject(mapOf(speed to "0.0")); testScheduler.runCurrent()
        assertFalse((h.machine.phase.value as LessonPhase.Done).locked)
        h.machine.endSession(); advanceUntilIdle()
        assertFalse((h.machine.phase.value as LessonPhase.Report).locked)
        h.port.inject(mapOf(speed to "20.0")); testScheduler.runCurrent()
        assertTrue((h.machine.phase.value as LessonPhase.Report).locked)   // 리포트도 구독을 끊지 않는다
        h.port.inject(mapOf(speed to "0.0")); testScheduler.runCurrent()
        assertFalse((h.machine.phase.value as LessonPhase.Report).locked)
        h.scope.cancel()
    }
}
