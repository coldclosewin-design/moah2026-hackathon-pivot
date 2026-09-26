package com.moah.hackathon.data

import com.moah.hackathon.feature.lesson.Difficulty
import com.moah.hackathon.feature.lesson.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedCatalogTest {
    @Test
    fun `task ids are unique and every difficulty is represented`() {
        assertEquals(SeedCatalog.tasks.size, SeedCatalog.tasks.map { it.id }.toSet().size)
        Difficulty.entries.forEach { d -> assertTrue("$d", SeedCatalog.tasks.any { it.difficulty == d }) }
        assertTrue(SeedCatalog.tasks.any { it.type == TaskType.KNOWLEDGE && !it.requiresDriving })
    }

    @Test
    fun `exactly the parking task is READY and it supports the three driving modes but not quiz`() {
        val ready = SeedCatalog.tasks.filter { it.isReady }
        assertEquals(listOf(SeedCatalog.TASK_PARKING_REAR), ready.map { it.id })
        val p = SeedCatalog.parkingTask
        assertTrue(p.supports(com.moah.hackathon.feature.lesson.LessonMode.GUIDE) && p.supports(com.moah.hackathon.feature.lesson.LessonMode.HINT) && p.supports(com.moah.hackathon.feature.lesson.LessonMode.EVALUATE))
        assertTrue(!p.supports(com.moah.hackathon.feature.lesson.LessonMode.QUIZ))
        assertTrue(SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }.supports(com.moah.hackathon.feature.lesson.LessonMode.QUIZ))
    }

    @Test
    fun `parking guide has six steps and only the parking task has a guide`() {
        assertEquals(6, SeedCatalog.guideFor(SeedCatalog.parkingTask).size)
        assertTrue(SeedCatalog.guideFor(SeedCatalog.tasks.first { it.id == "road-course" }).isEmpty())
        assertEquals(listOf("belt", "ignition", "reverse", "steer-right", "center", "park"), SeedCatalog.parkingGuide.map { it.id })
    }

    @Test
    fun `quiz answers are in range and profile has five questions`() {
        SeedCatalog.quiz.forEach { assertTrue(it.answer in it.choices.indices) }
        assertEquals(5, SeedCatalog.profileQuestions.size)
        assertEquals(10, SeedCatalog.demoProfile.rustyYears)
    }
}
