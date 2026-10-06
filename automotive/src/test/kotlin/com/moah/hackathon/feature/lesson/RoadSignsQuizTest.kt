package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.RoadSigns
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 점검 분류의 "도로 표시 읽기"(라운드 25 결정 5 H): 퀴즈만 받는 점검 과제, 문항마다 그림. */
@OptIn(ExperimentalCoroutinesApi::class)
class RoadSignsQuizTest {

    private val task = SeedCatalog.tasks.first { it.id == RoadSigns.TASK_ROAD_SIGNS }

    @Test
    fun aChecklistTaskCanBeQuizOnly() {
        assertEquals(TaskType.CHECKLIST, task.type)
        assertTrue(task.isReady && task.quizOnly)
        assertTrue(task.supports(LessonMode.QUIZ))
        listOf(LessonMode.GUIDE, LessonMode.HINT, LessonMode.EVALUATE).forEach { assertFalse("$it", task.supports(it)) }
        // 지식 과제는 그대로, 출발 전 점검은 퀴즈를 받지 않는다
        assertTrue(SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_KNOWLEDGE }.quizOnly)
        assertFalse(SeedCatalog.predriveTask.supports(LessonMode.QUIZ))
        assertTrue(SeedCatalog.scenariosFor(task).isEmpty())
        assertEquals(LessonMode.QUIZ, ModeAdvisor.suggest(task, ProgressStore()).mode)
        assertEquals("predrive stays first", SeedCatalog.TASK_PREDRIVE, SeedCatalog.tasks.first { it.type == TaskType.CHECKLIST }.id)
    }

    @Test
    fun everyItemHasAFigureAndNoDigits() {
        val items = SeedCatalog.quizFor(task)
        assertTrue(items.size >= 5)
        assertEquals(items.size, items.mapNotNull { it.figure }.toSet().size)   // 그림이 다 다르다
        items.forEach { q -> assertTrue(q.id, (q.question + q.why + q.choices.joinToString()).none(Char::isDigit)) }
    }

    @Test
    fun theQuizRunsFromTheChecklistCategory() = runTest {
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
        machine.begin(task.id, LessonMode.HINT)
        assertTrue("refused", machine.phase.value is LessonPhase.Setup)
        machine.begin(task.id, LessonMode.QUIZ)
        advanceUntilIdle()
        val quiz = machine.phase.value as LessonPhase.Quiz
        assertNotNull(quiz.item.figure)
        scope.cancel()
    }
}
