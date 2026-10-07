package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeTtsPort
import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.vehicle.FakeVehiclePort
import com.moah.hackathon.vehicle.SignalRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 브리핑이 음성이 끝날 때까지 머문다(라운드 26 시안 2-B, 10/7): 최소 3 s → 음성 끝 + 1 s → 최대 12 s · `건너뛰기`. */
@OptIn(ExperimentalCoroutinesApi::class)
class BriefingWaitTest {

    private class Harness(val tts: FakeTtsPort, val machine: LessonStateMachine, val scope: CoroutineScope)

    private fun TestScope.harness(): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val tts = FakeTtsPort()
        val machine = LessonStateMachine(
            vehicle = FakeVehiclePort(simulate = false, dispatcher = dispatcher), tts = tts,
            coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = ProgressStore(),
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues,
            benefits = SeedCatalog.benefits, profile = SeedCatalog.demoProfile, scope = scope,
            clock = { testScheduler.currentTime }, briefingMillis = 3_000, briefingMaxMillis = 12_000,
        )
        return Harness(tts, machine, scope)
    }

    private fun Harness.phase() = machine.phase.value

    @Test
    fun staysUntilTheSpeechEndsThenOneMoreSecond() = runTest {
        val h = harness()
        h.tts.speakingFlow.value = true
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        val b = h.phase() as LessonPhase.Briefing
        assertTrue("expected bar length", b.expectedMillis in 3_000..12_000)
        testScheduler.advanceTimeBy(5_000); testScheduler.runCurrent()
        assertTrue("still speaking → stays", h.phase() is LessonPhase.Briefing)
        h.tts.speakingFlow.value = false
        testScheduler.advanceTimeBy(900); testScheduler.runCurrent()
        assertTrue("one beat after the speech", h.phase() is LessonPhase.Briefing)
        testScheduler.advanceTimeBy(200); testScheduler.runCurrent()
        assertTrue("then the attempt", h.phase() is LessonPhase.Maneuver)
        h.scope.cancel()
    }

    @Test
    fun noSpeechMeansTheMinimumAndAStuckEngineMeansTheMaximum() = runTest {
        val quiet = harness()
        quiet.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        testScheduler.advanceTimeBy(2_900); testScheduler.runCurrent()
        assertTrue(quiet.phase() is LessonPhase.Briefing)
        testScheduler.advanceTimeBy(1_200); testScheduler.runCurrent()
        assertTrue("minimum + one beat", quiet.phase() is LessonPhase.Maneuver)
        quiet.scope.cancel()

        val stuck = harness()
        stuck.tts.speakingFlow.value = true
        stuck.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        testScheduler.advanceTimeBy(11_900); testScheduler.runCurrent()
        assertTrue(stuck.phase() is LessonPhase.Briefing)
        testScheduler.advanceTimeBy(200); testScheduler.runCurrent()
        assertTrue("cap even if the engine never says done", stuck.phase() is LessonPhase.Maneuver)
        stuck.scope.cancel()
    }

    @Test
    fun skipLeavesAtOnceAndOnlyFromTheBriefing() = runTest {
        val h = harness()
        h.tts.speakingFlow.value = true
        h.machine.skipBriefing()
        assertTrue("ignored outside the briefing", h.phase() is LessonPhase.Setup)
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        testScheduler.advanceTimeBy(500); testScheduler.runCurrent()
        h.machine.skipBriefing()
        testScheduler.runCurrent()
        assertTrue(h.phase() is LessonPhase.Maneuver)
        testScheduler.advanceTimeBy(15_000); testScheduler.runCurrent()
        assertTrue("the cancelled wait does not start a second attempt", (h.phase() as LessonPhase.Maneuver).attempt == 1)
        h.scope.cancel()
    }
}
