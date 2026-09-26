package com.moah.hackathon.feature.lesson

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
            registry = SignalRegistry(ParkingRecorder.KEYS, simulated = true), store = store,
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, reservation = SeedCatalog.reservation, benefits = SeedCatalog.benefits,
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
        assertTrue(done.record.remark, done.record.remark.startsWith("2번 만에, 26초."))
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
        assertTrue(report.summary, report.summary.contains("60점에서 100점까지"))
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
            override suspend fun remark(score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int): String = throw IllegalStateException("no network")
            override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String = throw IllegalStateException("no network")
        }
        val h = harness(angry)
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.EVALUATE)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt(); advanceUntilIdle()
        val done = h.machine.phase.value as LessonPhase.Done
        assertTrue(done.record.remark, done.record.remark.startsWith("2번 만에, 26초."))
        h.machine.endSession(); advanceUntilIdle()
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertTrue(report.summary.contains("가장 좋은 회차 100점"))
        assertFalse(h.tts.spoken.isEmpty())
        h.scope.cancel()
    }
}
