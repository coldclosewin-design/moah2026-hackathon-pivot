package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.ports.CoachPort
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeTtsPort
import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.CourseResult
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.scoring.ParkingVerdict
import com.moah.hackathon.vehicle.FakeVehiclePort
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import com.moah.hackathon.vehicle.VssValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 사내 피드백 #4(10/6): 회차 시작 직후 문 열기 → 화면이 "1회차" Done 에 멈춤.
 * 원인 = 사내 Cloud 코치 지연(1.4~3.3 s)에 리포트가 1 s 만 기다리고 Setup 으로 되돌린 뒤, 늦게 온 채점이 Done 을 덮어씀.
 * 이 테스트는 3 s 걸리는 코치로 같은 상황을 만든다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DoorExitTest {

    private class SlowCoach(private val millis: Long) : CoachPort {
        private val inner = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3)))
        override suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int, verdict: ParkingVerdict?,
            course: CourseResult?): String { delay(millis); return inner.remark(task, score, delta, profile, attempt, verdict, course) }
        override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String {
            delay(millis); return inner.summarize(task, mode, attempts, profile)
        }
    }

    private class Harness(val port: FakeVehiclePort, val machine: LessonStateMachine, val scope: CoroutineScope)

    private fun TestScope.harness(coachMillis: Long = 3_000): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val port = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val machine = LessonStateMachine(
            vehicle = port, tts = FakeTtsPort(), coach = SlowCoach(coachMillis),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = ProgressStore(),
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues,
            benefits = SeedCatalog.benefits, profile = SeedCatalog.demoProfile, scope = scope,
            clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        return Harness(port, machine, scope)
    }

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
    fun doorBeforeMovingEndsWithoutAScoredAttempt() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Maneuver)
        openDoor(h)
        // 빈 회차를 채점하지 않으니 지난 회차도 없음 → Setup. 늦게 덮이는 Done 도 없다
        assertTrue("was ${h.machine.phase.value}", h.machine.phase.value is LessonPhase.Setup)
        h.scope.cancel()
    }

    @Test
    fun doorAfterMovingWaitsForTheSlowCoachAndReports() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        openDoor(h)
        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertEquals(1, report.attempts.size)
        h.scope.cancel()
    }

    @Test
    fun aResultFinishingAfterTheSessionEndedIsDropped() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt()          // 코치가 3 s 걸리는 동안
        h.machine.reset()                  // 세션을 끝내면
        advanceUntilIdle()
        assertTrue("was ${h.machine.phase.value}", h.machine.phase.value is LessonPhase.Setup)   // Done 이 덮이지 않는다
        h.scope.cancel()
    }
}
