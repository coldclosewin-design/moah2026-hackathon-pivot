package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.CourseScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.ports.CourseRemarks
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeTtsPort
import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.vehicle.FakeVehiclePort
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalAvailability
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 코스 과제(10/4) — 상태기계가 Drive 단계로 진행하고, 구간 안내·감점 발화·결과를 모드대로 내는지. */
@OptIn(ExperimentalCoroutinesApi::class)
class CourseLessonTest {

    private class H(val port: FakeVehiclePort, val tts: FakeTtsPort, val store: ProgressStore, val machine: LessonStateMachine, val scope: CoroutineScope)

    private fun TestScope.harness(): H {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val port = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val tts = FakeTtsPort()
        val store = ProgressStore()
        val machine = LessonStateMachine(
            vehicle = port, tts = tts, coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = store,
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues, benefits = SeedCatalog.benefits,
            profile = SeedCatalog.demoProfile, scope = scope, clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        return H(port, tts, store, machine, scope)
    }

    private val drives = ArrayList<LessonPhase.Drive>()

    private suspend fun TestScope.feed(h: H, scenario: Scenario) {
        var prev = 0.0
        for (step in scenario.steps) {
            val wait = Math.round((step.atSeconds - prev) * 1000)
            if (wait > 0) testScheduler.advanceTimeBy(wait)
            prev = step.atSeconds
            h.port.inject(step.values)
            testScheduler.runCurrent()
            (h.machine.phase.value as? LessonPhase.Drive)?.let { drives += it }
        }
    }

    private suspend fun TestScope.run(taskId: String, mode: LessonMode, scenario: Scenario): Pair<H, LessonPhase.Done> {
        val h = harness()
        h.machine.begin(taskId, mode)
        advanceUntilIdle()
        assertTrue(h.machine.phase.value is LessonPhase.Drive)
        feed(h, scenario)
        h.machine.finishAttempt()
        advanceUntilIdle()
        return h to (h.machine.phase.value as LessonPhase.Done)
    }

    @Test fun `exam good in evaluate mode - announcements only, passes with full score`() = runTest {
        val (h, done) = run(SeedCatalog.TASK_TRACK_EXAM, LessonMode.EVALUATE, CourseScenarios.examGood)
        val course = assertNotNull(done.record.course).let { done.record.course!! }
        assertEquals(true, course.passed)
        assertEquals(100, done.record.score.skill)
        assertNull(done.record.verdict)
        assertTrue(done.record.path.isEmpty())
        val spoken = h.tts.spoken
        assertTrue(spoken.any { it.contains("시험 모드") })
        assertTrue("시험을 시작합니다." in spoken)
        assertTrue("장치 조작을 시작합니다." in spoken)
        assertTrue("경사로 구간입니다." in spoken && "종료 구간입니다." in spoken)
        assertTrue(spoken.none { it.endsWith("감점입니다.") || it.contains("실격") })
        assertTrue("다 되셨나요? 다 됐으면 버튼을 눌러 주세요." in spoken)
        assertEquals(CourseRemarks.remark(course), done.record.remark)
        assertTrue(done.record.remark.startsWith("감점 없이 합격선을 넘었어요."))
        // 배지 분모 17 — 전부 시뮬레이션
        assertEquals(17, done.record.score.badge.total)
        assertEquals(17, done.record.score.badge.simulated)
        h.scope.cancel()
    }

    @Test fun `exam bad in evaluate mode - deductions announced without numbers, fails`() = runTest {
        val (h, done) = run(SeedCatalog.TASK_TRACK_EXAM, LessonMode.EVALUATE, CourseScenarios.examBad)
        val course = done.record.course!!
        assertEquals(false, course.passed)
        assertEquals(70, done.record.score.skill)
        val spoken = h.tts.spoken
        assertTrue("뒤로 밀림, 감점입니다." in spoken)
        assertTrue("검지선 접촉, 감점입니다." in spoken)
        assertTrue("비상등 미점등, 감점입니다." in spoken)
        assertTrue(done.record.remark.startsWith("이번엔 합격선에 조금 못 미쳤어요."))
        (spoken + done.record.remark).forEach { assertFalse(it, it.any(Char::isDigit)) }
        h.scope.cancel()
    }

    @Test fun `the screen line is always the latest spoken one - deductions show, then the next zone replaces them`() = runTest {
        drives.clear()
        val (h, _) = run(SeedCatalog.TASK_TRACK_EXAM, LessonMode.EVALUATE, CourseScenarios.examBad)
        fun headline(d: LessonPhase.Drive) = d.lastHint ?: d.zoneLine
        val lines = drives.map(::headline).fold(ArrayList<String?>()) { acc, l -> if (acc.lastOrNull() != l) acc += l; acc }
        val rollback = lines.indexOf("뒤로 밀림, 감점입니다.")
        assertTrue(lines.toString(), rollback > 0)
        assertEquals("우회전 구간입니다.", lines.drop(rollback + 1).firstOrNull())   // 다음 구간이 감점 문장을 덮는다
        assertTrue(lines.toString(), "검지선 접촉, 감점입니다." in lines && "비상등 미점등, 감점입니다." in lines)
        h.scope.cancel()
    }

    @Test fun `guide mode reads each zone and explains a deduction at the moment`() = runTest {
        val (h, done) = run("left-turn-signal", LessonMode.GUIDE, CourseScenarios.leftBad)
        val spoken = h.tts.spoken
        assertTrue("구간마다 할 일을 말할게요." in spoken)
        assertTrue(spoken.any { it.startsWith("교차로가 가까워지면") })
        assertTrue(spoken.any { it.startsWith("선에 닿았어요.") })
        assertTrue(spoken.any { it.startsWith("왼쪽 방향지시등을 켜지 않았어요.") })
        assertNull(done.record.course!!.passed)   // 연습 코스 — 합격 판정 없음
        assertTrue(done.record.remark.startsWith("놓친 구간이 몇 군데 있었어요."))
        h.scope.cancel()
    }

    @Test fun `hint mode warns before the red light and at the speeding moment`() = runTest {
        val (h, _) = run("road-course", LessonMode.HINT, CourseScenarios.roadBad)
        val spoken = h.tts.spoken
        assertTrue("속도가 빨라요. 천천히 가요." in spoken)
        assertTrue("빨간불이에요. 정지선 앞에서 멈춰요." in spoken)
        assertTrue("오른쪽 방향지시등을 켜요." in spoken)
        h.scope.cancel()
    }

    @Test fun `hint mode shouts stop at an emergency`() = runTest {
        val (h, _) = run(SeedCatalog.TASK_TRACK_EXAM, LessonMode.HINT, CourseScenarios.examGood)
        assertTrue("돌발 상황이에요. 멈추세요." in h.tts.spoken)
        h.scope.cancel()
    }

    @Test fun `drive phase carries the map and progress but never a score, and locks while moving`() = runTest {
        drives.clear()
        val (h, _) = run(SeedCatalog.TASK_TRACK_EXAM, LessonMode.HINT, CourseScenarios.examGood)
        assertTrue(drives.size > 100)
        assertTrue(drives.any { it.locked } && drives.any { !it.locked })
        assertTrue(drives.all { it.progress.positionMeasured })
        // 지나간 구간이 앞으로만 쌓인다
        val passed = drives.map { it.progress.passedZoneIds.size }
        assertEquals(passed, passed.sorted())
        assertEquals(SignalAvailability.SIMULATED, drives.last().availability[com.moah.hackathon.vehicle.SimOnlySignals.TRACK_POSITION_X_M])
        val fields = LessonPhase.Drive::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fields.toString(), fields.none { it.contains("score") || it.contains("deduction") || it.contains("result") })
        h.scope.cancel()
    }

    @Test fun `second attempt starts from the previous finish and is still judged zone by zone`() = runTest {
        val (h, first) = run(SeedCatalog.TASK_TRACK_EXAM, LessonMode.EVALUATE, CourseScenarios.examGood)
        assertEquals(true, first.record.course!!.passed)
        h.machine.nextAttempt()
        advanceUntilIdle()
        val asked = h.tts.spoken.count { it.startsWith("다 되셨나요") }
        feed(h, CourseScenarios.examGood)
        assertEquals(asked + 1, h.tts.spoken.count { it.startsWith("다 되셨나요") })
        h.machine.finishAttempt()
        advanceUntilIdle()
        val second = (h.machine.phase.value as LessonPhase.Done).record.course!!
        assertTrue(second.zones.all { it.visited })
        assertEquals(true, second.passed)
        h.scope.cancel()
    }

    @Test fun `overrunning the last zone still asks done`() = runTest {
        val (h, _) = run("straight-stop", LessonMode.HINT, CourseScenarios.straightBad)
        assertTrue("다 되셨나요? 다 됐으면 버튼을 눌러 주세요." in h.tts.spoken)
        h.scope.cancel()
    }

    @Test fun `door opening at the end goes straight to the report`() = runTest {
        val h = harness()
        h.machine.begin("straight-stop", LessonMode.EVALUATE)
        advanceUntilIdle()
        feed(h, CourseScenarios.straightGood)
        h.port.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.TRUE))
        advanceUntilIdle()
        val r = h.machine.phase.value as LessonPhase.Report
        assertEquals(1, r.report.attempts.size)
        assertNotNull(r.report.attempts.single().course)
        h.scope.cancel()
    }

    @Test fun `course attempts do not skew the observed segment average`() = runTest {
        val (h, _) = run("straight-stop", LessonMode.EVALUATE, CourseScenarios.straightGood)
        val obs = h.store.observation()
        assertEquals(1, obs.attempts)
        assertEquals(h.store.all().single().score.metrics.motion.movingSegments.toFloat(), obs.meanSegments!!, 0.01f)
        h.scope.cancel()
    }
}
