package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.AdminPresets
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 홈이 처음엔 과제를 임의로 정하지 않는다 · 홈에서 모드만 바꾼다(라운드 26 시안 1-6, 10/7). */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeUnpickedTest {

    private class Harness(val machine: LessonStateMachine, val scope: CoroutineScope)

    private fun TestScope.harness(): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val machine = LessonStateMachine(
            vehicle = FakeVehiclePort(simulate = false, dispatcher = dispatcher), tts = FakeTtsPort(),
            coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = ProgressStore(),
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues,
            benefits = SeedCatalog.benefits, profile = SeedCatalog.demoProfile, scope = scope,
            clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        return Harness(machine, scope)
    }

    private val Harness.setup: LessonPhase.Setup get() = machine.phase.value as LessonPhase.Setup

    @Test
    fun aFreshHomeHasNotPickedATaskAndIgnoresAModeChange() = runTest {
        val h = harness()
        assertFalse("nothing practised, nothing pinned or booked", h.setup.picked)
        val before = h.setup.suggestedMode
        h.machine.chooseHomeMode(if (before == LessonMode.HINT) LessonMode.GUIDE else LessonMode.HINT)
        assertEquals("no mode change before a task is picked", before, h.setup.suggestedMode)
        h.scope.cancel()
    }

    @Test
    fun aPresetPicksAndTheHomeModeChangeStaysOnThatTask() = runTest {
        val h = harness()
        val preset = AdminPresets.presets.first { it.id == "rear-two" }
        h.machine.applyPreset(preset, SeedCatalog.demoProfile)
        assertTrue(h.setup.picked)
        assertEquals(SeedCatalog.TASK_PARKING_REAR, h.setup.suggestedTask.id)
        h.machine.chooseHomeMode(LessonMode.EVALUATE)
        assertEquals(SeedCatalog.TASK_PARKING_REAR, h.setup.suggestedTask.id)
        assertEquals(LessonMode.EVALUATE, h.setup.suggestedMode)
        h.machine.chooseHomeMode(LessonMode.QUIZ)
        assertEquals("unsupported mode ignored", LessonMode.EVALUATE, h.setup.suggestedMode)
        h.scope.cancel()
    }

    @Test
    fun anUnansweredQuizDoesNotPickButABookingDoes() = runTest {
        val h = harness()
        h.machine.begin(SeedCatalog.TASK_KNOWLEDGE, LessonMode.QUIZ)
        advanceUntilIdle()
        h.machine.reset()
        assertFalse("a started-but-unanswered quiz leaves no record", h.setup.picked)
        h.machine.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_EXAM)
        assertTrue("a booking picks the booked course", h.setup.picked)
        h.scope.cancel()
    }
}
