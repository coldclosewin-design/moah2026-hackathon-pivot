package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.scoring.HarshEvent
import com.moah.hackathon.scoring.MotionSummary
import com.moah.hackathon.scoring.ParkingMetrics
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.scoring.PreDriveSummary
import com.moah.hackathon.scoring.SteeringSummary
import com.moah.hackathon.vehicle.AvailabilityBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressStoreTest {
    private val task = SeedCatalog.parkingTask

    private fun record(i: Int, mode: LessonMode, skill: Int, segments: Int = 2, reversals: Int = 1, harsh: Int = 0) = AttemptRecord(
        i, task.id, mode,
        ParkingScore(skill, 100,
            ParkingMetrics(MotionSummary(segments, 30_000, 15_000, 4f, 1000L), List(harsh) { HarshEvent(0, com.moah.hackathon.scoring.HarshKind.BRAKING, -4f) },
                SteeringSummary(reversals, 450f), null, null, PreDriveSummary(true, true)),
            AvailabilityBadge(0, 7, 1), emptyList()),
        null, "…", i * 1000L,
    )

    @Test
    fun `advisor starts with guide then moves to hint then evaluate`() {
        val store = ProgressStore()
        assertEquals(LessonMode.GUIDE, ModeAdvisor.suggest(task, store).mode)
        store.add(record(1, LessonMode.GUIDE, 80))
        assertEquals(LessonMode.GUIDE, ModeAdvisor.suggest(task, store).mode)   // 1 pass — not yet
        store.add(record(2, LessonMode.GUIDE, 75))
        assertEquals(LessonMode.HINT, ModeAdvisor.suggest(task, store).mode)    // 2 passes
        store.add(record(3, LessonMode.HINT, 85))
        assertEquals(LessonMode.HINT, ModeAdvisor.suggest(task, store).mode)
        store.add(record(4, LessonMode.HINT, 90))
        assertEquals(LessonMode.EVALUATE, ModeAdvisor.suggest(task, store).mode)
    }

    @Test
    fun `a failed guide does not count as a pass`() {
        val store = ProgressStore()
        store.add(record(1, LessonMode.GUIDE, 40))
        store.add(record(2, LessonMode.GUIDE, 60))
        assertEquals(LessonMode.GUIDE, ModeAdvisor.suggest(task, store).mode)
    }

    @Test
    fun `previous best and observation`() {
        val store = ProgressStore()
        assertNull(store.previous(task.id))
        store.add(record(1, LessonMode.GUIDE, 60, segments = 4, reversals = 3, harsh = 1))
        store.add(record(2, LessonMode.GUIDE, 100, segments = 2, reversals = 1))
        assertEquals(2, store.previous(task.id)!!.index)
        assertEquals(100, store.best(task.id)!!.score.skill)
        val obs = store.observation()
        assertEquals(2, obs.attempts)
        assertEquals(3f, obs.meanSegments)
        assertEquals(2f, obs.meanReversals)
        assertEquals(1, obs.harshEvents)
        assertEquals(task.id, obs.weakTaskId)
    }

    @Test
    fun `task suggestion follows fear then falls back to the first easy task - among READY tasks only`() {
        assertEquals(SeedCatalog.TASK_PARKING_REAR, ModeAdvisor.suggestTask(SeedCatalog.demoProfile, SeedCatalog.tasks).id)
        // 시드에는 READY 가 주차 하나뿐 → 공포가 없어도 계획 과제(출발 전 점검 등)를 제안하지 않는다
        val noFear = Profile("x", ProfileStatement())
        assertEquals(SeedCatalog.TASK_PARKING_REAR, ModeAdvisor.suggestTask(noFear, SeedCatalog.tasks).id)
        // READY 가 여럿이면 쉬운 것부터
        val ready = SeedCatalog.tasks.map { it.copy(status = TaskStatus.READY) }
        assertEquals(Difficulty.EASY, ModeAdvisor.suggestTask(noFear, ready).difficulty)
    }
}
