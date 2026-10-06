package com.moah.hackathon.data

import com.moah.hackathon.feature.lesson.Difficulty
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.feature.lesson.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedCatalogTest {
    @Test
    fun `spoken guides and task descriptions use complete sentences without symbols or numbers`() {
        val lines = (SeedCatalog.parkingGuide + SeedCatalog.frontParkingGuide + SeedCatalog.predriveGuide)
            .flatMap { listOf(it.say, it.confirm) } + SeedCatalog.tasks.map { it.summary }
        lines.forEach { text ->
            assertTrue(text, text.endsWith("요."))
            assertTrue(text, !Regex("[0-9A-Z°→]").containsMatchIn(text))
        }
        SeedCatalog.quiz.forEach { item ->
            item.why.split(Regex("(?<=[.!?])\\s+")).forEach { assertTrue(it, it.endsWith("요.")) }
        }
        assertTrue(SeedCatalog.quiz.first { it.id == "rain-braking" }.why.contains("20%"))
        assertTrue(SeedCatalog.quiz.first { it.id == "following-distance" }.why.contains("60 m"))
    }

    @Test
    fun `parking variants are ordered catalogue entries - all four ready with their own spec`() {
        assertEquals(13, SeedCatalog.tasks.size)   // + 도로 표시 읽기(라운드 25 H)
        val parking = SeedCatalog.tasks.filter { it.type == TaskType.PARKING }
        assertEquals(listOf(SeedCatalog.TASK_PARKING_REAR, "parking-parallel", SeedCatalog.TASK_PARKING_FRONT, "parking-angle"), parking.map { it.id })
        assertEquals(listOf(Difficulty.MEDIUM, Difficulty.HARD), parking.takeLast(2).map { it.difficulty })
        // 10/4: 평행(목표 0°·되돌림 2)·사선(45°)도 READY — 가이드·시나리오 2벌씩
        parking.forEach {
            assertEquals(it.id, TaskStatus.READY, it.status)
            assertTrue(it.id, SeedCatalog.guideFor(it).isNotEmpty() && SeedCatalog.scenariosFor(it).size == 2)
        }
        assertEquals(com.moah.hackathon.scoring.ParkingSpec.PARALLEL, SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_PARKING_PARALLEL }.parking)
        assertEquals(com.moah.hackathon.scoring.ParkingSpec.ANGLE, SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_PARKING_ANGLE }.parking)
        assertEquals(SeedCatalog.tasks.size, SeedCatalog.tasks.count { it.isReady })   // 10/4: 준비 중 0 — 전 범위 구현
        // 10/2: 전면 직각 주차 — 앞으로 들어가는 사양, 뒤 거리 없이 7키. 후면은 사양을 명시해도 기본과 같다
        val front = SeedCatalog.frontParkingTask
        assertTrue(front.isReady)
        assertEquals(com.moah.hackathon.scoring.ParkingSpec.FRONT_PERPENDICULAR, front.parking)
        assertEquals(listOf("핸들 방향", "기어 전환"), front.watch)
        assertTrue(com.moah.hackathon.vehicle.SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM !in front.requiredSignals)
        assertEquals(com.moah.hackathon.scoring.ParkingSpec.REAR_PERPENDICULAR, SeedCatalog.parkingTask.parkingSpec)
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
        assertEquals(SeedCatalog.tasks.map { it.id }, ready.map { it.id })
        // 코스 과제는 전부 도면·시나리오 2벌을 갖고, 주행 모드 셋만 받는다
        ready.filter { it.type == TaskType.DRIVING }.forEach { t ->
            assertTrue(t.id, t.isCourse && SeedCatalog.scenariosFor(t).size == 2 && !t.supports(com.moah.hackathon.feature.lesson.LessonMode.QUIZ))
        }
        assertTrue(SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_TRACK_EXAM }.course!!.isExam)
        assertTrue(!SeedCatalog.predriveTask.requiresDriving)
        assertTrue(SeedCatalog.predriveTask.supports(com.moah.hackathon.feature.lesson.LessonMode.GUIDE) && !SeedCatalog.predriveTask.supports(com.moah.hackathon.feature.lesson.LessonMode.QUIZ))
        assertEquals(10, SeedCatalog.quizFor(SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_KNOWLEDGE }).size)
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
        // 전면 6단계: 같은 뼈대, 기어가 주행(D). 벨트·시동·주차 단계는 후면 것을 공유
        assertEquals(listOf("belt", "ignition", "drive", "steer-right", "center", "park"), SeedCatalog.guideFor(SeedCatalog.frontParkingTask).map { it.id })
        assertTrue(SeedCatalog.frontParkingGuide.none { it.say.contains("후진") || it.confirm.contains("후진") })
        assertEquals(listOf("door", "belt", "park-check", "ignition", "indicator-left", "indicator-right", "hazard"), SeedCatalog.guideFor(SeedCatalog.predriveTask).map { it.id })
        assertTrue(SeedCatalog.predriveGuide.last().confirm.contains("버튼을 눌러 주세요"))
    }

    @Test
    fun `scenarios follow the task type`() {
        assertEquals(listOf("parking-good", "parking-bad"), SeedCatalog.scenariosFor(SeedCatalog.parkingTask).map { it.id })
        assertEquals(listOf("parking-front-good", "parking-front-bad"), SeedCatalog.scenariosFor(SeedCatalog.frontParkingTask).map { it.id })
        // 시연 패널 버튼 라벨은 과제가 달라도 같다(emu_flow·inhouse_check 가 글자로 누른다)
        assertEquals(listOf("잘한 주차", "못한 주차"), SeedCatalog.scenariosFor(SeedCatalog.frontParkingTask).map { it.title })
        assertEquals(listOf("predrive-good", "predrive-bad"), SeedCatalog.scenariosFor(SeedCatalog.predriveTask).map { it.id })
        assertTrue(SeedCatalog.scenariosFor(SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_KNOWLEDGE }).isEmpty())
    }

    @Test
    fun `quiz answers are in range and profile has five questions`() {
        assertEquals(10, SeedCatalog.quiz.size)
        SeedCatalog.quiz.forEach {
            assertEquals(it.id, 3, it.choices.size)
            assertEquals(it.id, 3, it.choices.toSet().size)
            assertTrue(it.id, it.answer in it.choices.indices)
        }
        assertEquals(listOf("roundabout-priority", "hazard-when", "rain-braking", "night-highbeam", "following-distance"),
            SeedCatalog.quiz.take(5).map { it.id })
        SeedCatalog.quiz.drop(5).forEach { item ->
            assertTrue(item.id, !Regex("\\d").containsMatchIn(item.question + item.choices.joinToString() + item.why))
        }
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

    @Test
    fun `the knowledge quiz is an easy task - three choices while parked is not harder than rear parking`() {
        assertEquals(Difficulty.EASY, SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_KNOWLEDGE }.difficulty)
    }
}
