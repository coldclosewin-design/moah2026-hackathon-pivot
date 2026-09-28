package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.ports.CoachPort
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeTtsPort
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

    private fun TestScope.harness(coach: CoachPort? = null): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val port = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val tts = FakeTtsPort()
        val store = ProgressStore()
        val machine = LessonStateMachine(
            vehicle = port, tts = tts, coach = coach ?: FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS, simulated = true), store = store,
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, reservation = SeedCatalog.reservation, venues = SeedCatalog.venues, benefits = SeedCatalog.benefits,
            cheers = SeedCatalog.cheers,
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
        h.port.set(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.TRUE))
        advanceUntilIdle()
    }

    @Test
    fun `setup suggests the feared task in guide mode for a first timer`() = runTest {
        val h = harness()
        val setup = h.machine.phase.value as LessonPhase.Setup
        assertEquals(SeedCatalog.TASK_PARKING_REAR, setup.suggestedTask.id)
        assertEquals(LessonMode.GUIDE, setup.suggestedMode)
        assertNotNull(setup.reservation)
        assertEquals(null, setup.cheer)
        // 리포트 밖에서는 동승자 동작을 받지 않는다
        h.machine.cheer("오늘도 천천히 가요"); h.machine.shareWithCompanion(CompanionShareLevel.FULL)
        assertEquals(null, h.store.cheer); assertEquals(null, h.store.companionShare)
        h.scope.cancel()
    }

    @Test
    fun `report carries the companion note and the cheer survives reset into the next setup`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT); advanceUntilIdle()
        feed(h, ParkingScenarios.bad)
        h.machine.finishAttempt(); advanceUntilIdle()
        openDoor(h)
        val report = (h.machine.phase.value as LessonPhase.Report).report
        val note = report.companion
        assertTrue(note.text, note.praise.isNotBlank() && !Regex("\\d").containsMatchIn(note.text))
        assertEquals("출발 전에 벨트 같이 확인해 주세요.", note.help)   // 못한 주차 = 벨트 늦음
        assertEquals(SeedCatalog.cheers, report.cheers)
        assertEquals(CompanionShareLevel.entries.toList(), report.companionShareLevels)

        h.machine.shareWithCompanion(CompanionShareLevel.PROCESS)
        assertEquals(CompanionShareLevel.PROCESS, h.store.companionShare)
        h.machine.cheer(" ${SeedCatalog.cheers[0]} ")
        h.machine.reset()
        val setup = h.machine.phase.value as LessonPhase.Setup
        assertEquals(SeedCatalog.cheers[0], setup.cheer)   // 다음 세션 첫 줄. reset 이 지우지 않는다
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
    fun `planned tasks and unsupported modes are refused and the session stays in Setup`() = runTest {
        val h = harness()
        h.machine.begin("road-course", LessonMode.HINT)          // 카탈로그에만 있는 과제
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
    fun `quiz - five questions, verdict and reason spoken, done with the score, moving locks answers`() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_KNOWLEDGE, LessonMode.QUIZ)
        advanceUntilIdle()
        var q = h.machine.phase.value as LessonPhase.Quiz
        assertEquals(0, q.index); assertEquals(5, q.total); assertFalse(q.answered); assertFalse(q.locked)
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
        h.machine.answer(0)                            // 5번 정답 (마지막)
        assertTrue((h.machine.phase.value as LessonPhase.Quiz).isLast)
        h.machine.nextQuestion()                       // 결과 보기
        val done = h.machine.phase.value as LessonPhase.QuizDone
        assertEquals(4, done.correct); assertEquals(5, done.total)
        assertEquals(listOf(true, false, true, true, true), done.results.map { it.correct })
        assertTrue(done.remark, done.remark.contains("5문제 중 4개"))
        assertEquals(1, h.store.quizzes().size)
        h.machine.reset()
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
        assertEquals(2, done.results.size); assertEquals(1, done.correct); assertEquals(5, done.total)
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
        assertTrue(h.tts.spoken.none { it.startsWith("다 되셨나요?") })   // 가이드 모드는 마지막 단계 확인 문장이 그 역할

        h.machine.finishAttempt()
        advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertEquals(100, done.record.score.skill)
        assertEquals(100, done.record.score.safety)
        assertTrue(done.record.remark, !done.record.remark.contains("초.") && done.record.remark.endsWith("이 순서 그대로 몸에 남겨 두세요."))
        assertFalse(done.record.remark.contains("들어갔"))   // 주차 멘트가 섞이지 않는다

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
        h.machine.endSession(); advanceUntilIdle()
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertTrue(report.summary, report.summary.startsWith("한 번 해 봤어요.") && report.summary.lines().size == 2
            && !Regex("\\d").containsMatchIn(report.summary))
        h.scope.cancel()
    }

    @Test
    fun `a door already open at attempt start does not end the attempt - closing then opening does`() = runTest {
        val h = harness()
        h.port.set(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.TRUE))   // 타는 중 — 문이 열린 채 시작
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        feed(h, ParkingScenarios.good)                 // 정차 구간이 여러 번 있지만 문이 계속 열려 있어도 끝나지 않는다
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        assertTrue(h.store.all().isEmpty())
        h.port.set(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.FALSE)); advanceUntilIdle()   // 닫음 → 무장
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        openDoor(h)                                    // 다시 열림 → 회차 종료 + 리포트
        assertTrue(h.machine.phase.value is LessonPhase.Report)
        assertEquals(1, h.store.all().size)
        h.scope.cancel()
    }

    @Test
    fun `spoken lines carry the right particles`() = runTest {
        val h = harness()
        h.machine.begin("road-course", LessonMode.HINT)
        assertTrue(h.tts.spoken.last(), h.tts.spoken.last().startsWith("일반 도로 코스는 아직 준비 중이에요. 지금은 출발 전 점검·후면 직각 주차·비상등·날씨별 행동을 할 수 있어요."))
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
        h.port.inject(mapOf(VssConstants.VEHICLE_SPEED to "3.0", VssConstants.DOOR_DRIVER_ISOPEN to VssValues.TRUE))
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
            override suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int): String = throw IllegalStateException("no network")
            override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String = throw IllegalStateException("no network")
            override suspend fun companionNote(task: Task, attempts: List<AttemptRecord>, profile: Profile): CompanionNote = throw IllegalStateException("no network")
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
            reservation = SeedCatalog.reservation, venues = SeedCatalog.venues, benefits = SeedCatalog.benefits,
            profile = noFear, scope = h.scope, clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        val before = m.phase.value as LessonPhase.Setup
        assertEquals(SeedCatalog.TASK_PREDRIVE, before.suggestedTask.id)
        assertEquals(null, before.booking)
        assertEquals(3, before.venues.size)
        assertEquals(SeedCatalog.reservation, before.reservation)   // 예약 없음 → 옛 카드는 시드 예시

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
        assertEquals("제휴 도로주행시험장 서초", booked.reservation?.venue)        // 옛 카드는 예약을 비춘다
        assertEquals("오늘 14:00–15:00", booked.reservation?.slot)
        assertEquals(Reservation.EXAMPLE_NOTE, booked.reservation?.note)

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
}
