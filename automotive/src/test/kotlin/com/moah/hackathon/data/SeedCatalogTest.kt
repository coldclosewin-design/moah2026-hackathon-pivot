package com.moah.hackathon.data

import com.moah.hackathon.feature.lesson.Difficulty
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.feature.lesson.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedCatalogTest {
    @Test
    fun `parking variants are ordered catalogue entries and remain planned`() {
        assertEquals(11, SeedCatalog.tasks.size)
        val parking = SeedCatalog.tasks.filter { it.type == TaskType.PARKING }
        assertEquals(listOf(SeedCatalog.TASK_PARKING_REAR, "parking-parallel", "parking-front", "parking-angle"), parking.map { it.id })
        assertEquals(listOf(Difficulty.MEDIUM, Difficulty.HARD), parking.takeLast(2).map { it.difficulty })
        parking.drop(1).forEach {
            assertEquals(TaskStatus.PLANNED, it.status)
            assertTrue(SeedCatalog.guideFor(it).isEmpty())
        }
        assertEquals(3, SeedCatalog.tasks.count { it.isReady })
    }

    @Test
    fun `task ids are unique and every difficulty is represented`() {
        assertEquals(SeedCatalog.tasks.size, SeedCatalog.tasks.map { it.id }.toSet().size)
        Difficulty.entries.forEach { d -> assertTrue("$d", SeedCatalog.tasks.any { it.difficulty == d }) }
        assertTrue(SeedCatalog.tasks.any { it.type == TaskType.KNOWLEDGE && !it.requiresDriving })
    }

    @Test
    fun `predrive parking and knowledge tasks are READY - parking supports the three driving modes but not quiz`() {
        val ready = SeedCatalog.tasks.filter { it.isReady }
        assertEquals(listOf(SeedCatalog.TASK_PREDRIVE, SeedCatalog.TASK_PARKING_REAR, SeedCatalog.TASK_KNOWLEDGE), ready.map { it.id })
        assertTrue(!SeedCatalog.predriveTask.requiresDriving)
        assertTrue(SeedCatalog.predriveTask.supports(com.moah.hackathon.feature.lesson.LessonMode.GUIDE) && !SeedCatalog.predriveTask.supports(com.moah.hackathon.feature.lesson.LessonMode.QUIZ))
        assertEquals(5, SeedCatalog.quizFor(SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_KNOWLEDGE }).size)
        assertTrue(SeedCatalog.quizFor(SeedCatalog.parkingTask).isEmpty())
        assertEquals(SeedCatalog.quiz.size, SeedCatalog.quiz.map { it.id }.toSet().size)
        val p = SeedCatalog.parkingTask
        assertTrue(p.supports(com.moah.hackathon.feature.lesson.LessonMode.GUIDE) && p.supports(com.moah.hackathon.feature.lesson.LessonMode.HINT) && p.supports(com.moah.hackathon.feature.lesson.LessonMode.EVALUATE))
        assertTrue(!p.supports(com.moah.hackathon.feature.lesson.LessonMode.QUIZ))
        assertTrue(SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }.supports(com.moah.hackathon.feature.lesson.LessonMode.QUIZ))
    }

    @Test
    fun `parking guide has six steps, predrive seven, and planned tasks have none`() {
        assertEquals(6, SeedCatalog.guideFor(SeedCatalog.parkingTask).size)
        assertTrue(SeedCatalog.guideFor(SeedCatalog.tasks.first { it.id == "road-course" }).isEmpty())
        assertEquals(listOf("belt", "ignition", "reverse", "steer-right", "center", "park"), SeedCatalog.parkingGuide.map { it.id })
        assertEquals(listOf("door", "belt", "park-check", "ignition", "indicator-left", "indicator-right", "hazard"), SeedCatalog.guideFor(SeedCatalog.predriveTask).map { it.id })
        assertTrue(SeedCatalog.predriveGuide.last().confirm.contains("버튼을 눌러 주세요"))
    }

    @Test
    fun `scenarios follow the task type`() {
        assertEquals(listOf("parking-good", "parking-bad"), SeedCatalog.scenariosFor(SeedCatalog.parkingTask).map { it.id })
        assertEquals(listOf("predrive-good", "predrive-bad"), SeedCatalog.scenariosFor(SeedCatalog.predriveTask).map { it.id })
        assertTrue(SeedCatalog.scenariosFor(SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_KNOWLEDGE }).isEmpty())
    }

    @Test
    fun `quiz answers are in range and profile has five questions`() {
        SeedCatalog.quiz.forEach { assertTrue(it.answer in it.choices.indices) }
        assertEquals(5, SeedCatalog.profileQuestions.size)
        assertEquals(10, SeedCatalog.demoProfile.rustyYears)
    }

    @Test
    fun `venues - three with courses whose task ids exist in the catalog, and one slot per venue is unavailable`() {
        assertEquals(3, SeedCatalog.venues.size)
        val ids = SeedCatalog.tasks.map { it.id }.toSet()
        for (v in SeedCatalog.venues) {
            assertTrue(v.name, v.courses.isNotEmpty() && v.slots.size == 3)
            assertEquals(v.name, 1, v.slots.count { !it.available })
            for (c in v.courses) for (id in c.taskIds) assertTrue("${c.id}: $id", id in ids)
        }
        // 주차 3종의 첫 과제가 READY(시연 본편) — 예약 제안이 실제로 뜨는 근거
        assertTrue(SeedCatalog.courses.first { it.id == SeedCatalog.COURSE_PARKING }.taskIds.first() == SeedCatalog.TASK_PARKING_REAR)
    }
}
