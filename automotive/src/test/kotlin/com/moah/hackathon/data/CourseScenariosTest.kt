package com.moah.hackathon.data

import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.CourseResult
import com.moah.hackathon.scoring.HarshEventDetector
import com.moah.hackathon.scoring.HarshKind
import com.moah.hackathon.scoring.Sample
import com.moah.hackathon.scoring.TrackCourse
import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.vehicle.toVssFloat
import com.moah.hackathon.vehicle.toVssGear
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 코스 시나리오 12벌을 채점기에 그대로 흘려 기대 감점을 고정한다(시연 대본의 근거). */
class CourseScenariosTest {

    private fun replay(course: TrackCourse, scenario: Scenario): CourseResult {
        val r = CourseRecorder(course)
        scenario.steps.forEach { r.onDelta((it.atSeconds * 1000).toLong(), it.values) }
        return r.result()
    }

    private fun harsh(scenario: Scenario): List<HarshKind> {
        val speed = scenario.steps.mapNotNull { s -> s.values[VssConstants.VEHICLE_SPEED]?.toVssFloat()?.let { Sample((s.atSeconds * 1000).toLong(), it) } }
        return HarshEventDetector.detect(speed).map { it.kind }
    }

    private fun harshDetail(scenario: Scenario): String {
        val speed = scenario.steps.mapNotNull { s -> s.values[VssConstants.VEHICLE_SPEED]?.toVssFloat()?.let { Sample((s.atSeconds * 1000).toLong(), it) } }
        return HarshEventDetector.detect(speed).joinToString { e ->
            "${e.kind}@${e.tMillis}(${e.accelMps2}) " + speed.filter { kotlin.math.abs(it.tMillis - e.tMillis) <= 800 }.joinToString(" ") { "${it.tMillis}:${it.value}" }
        }
    }

    private fun check(course: TrackCourse, scenario: Scenario, reasons: Set<String>, harshKinds: Set<HarshKind> = emptySet()): CourseResult {
        val res = replay(course, scenario)
        assertEquals(scenario.id, reasons, res.deductions.map { it.reason }.toSet())
        assertEquals(scenario.id, reasons.size, res.deductions.size)
        assertEquals("${scenario.id} harsh ${harshDetail(scenario)}", harshKinds.toSet(), harsh(scenario).toSet())
        // 끝은 정차 + P, 위치는 도면 안, 출발점에서 시작
        val last = scenario.steps.flatMap { it.values.entries }.associate { it.key to it.value }
        assertEquals(Gear.PARK, last[VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR].toVssGear())
        assertEquals(0f, last[VssConstants.VEHICLE_SPEED].toVssFloat()!!, 0.01f)
        val first = scenario.steps.first { SimOnlySignals.TRACK_POSITION_X_M in it.values }.values
        assertEquals(course.start.at.x, first[SimOnlySignals.TRACK_POSITION_X_M].toVssFloat()!!, 0.01f)
        assertEquals(course.start.at.y, first[SimOnlySignals.TRACK_POSITION_Y_M].toVssFloat()!!, 0.01f)
        res.trail.forEach { p ->
            assertTrue("${scenario.id} off map $p", p.x in 0f..course.map.widthM && p.y in 0f..course.map.heightM)
        }
        if (reasons.isEmpty()) assertTrue(scenario.id, res.zones.all { it.visited })
        println("${scenario.id}: ${"%.1f".format(scenario.durationSeconds)} s, score ${res.score}, ${res.deductions.map { it.reason }}")
        return res
    }

    @Test fun `exam good passes clean and exam bad fails on three lapses`() {
        val good = check(TrackCourses.exam, CourseScenarios.examGood, emptySet())
        assertEquals(100, good.score)
        assertEquals(true, good.passed)
        val bad = check(TrackCourses.exam, CourseScenarios.examBad, setOf("뒤로 밀림", "검지선 접촉", "비상등 미점등"))
        assertEquals(70, bad.score)
        assertEquals(false, bad.passed)
    }

    @Test fun `straight stop`() {
        check(TrackCourses.straightStop, CourseScenarios.straightGood, emptySet())
        check(TrackCourses.straightStop, CourseScenarios.straightBad, setOf("속도 초과", "정지선 미정지"), setOf(HarshKind.BRAKING))
    }

    @Test fun `left turn`() {
        check(TrackCourses.leftTurn, CourseScenarios.leftGood, emptySet())
        check(TrackCourses.leftTurn, CourseScenarios.leftBad, setOf("방향지시등 미점등", "검지선 접촉"))
    }

    @Test fun `lane change`() {
        check(TrackCourses.laneChange, CourseScenarios.laneGood, emptySet())
        check(TrackCourses.laneChange, CourseScenarios.laneBad, setOf("방향지시등 미점등", "속도 초과"))
    }

    @Test fun `roundabout`() {
        check(TrackCourses.roundabout, CourseScenarios.roundGood, emptySet())
        check(TrackCourses.roundabout, CourseScenarios.roundBad, setOf("속도 초과", "방향지시등 미점등"))
    }

    @Test fun `road course`() {
        check(TrackCourses.road, CourseScenarios.roadGood, emptySet())
        check(TrackCourses.road, CourseScenarios.roadBad, setOf("속도 초과", "방향지시등 미점등"))
    }

    @Test fun `practice courses have no pass line and every course has scenarios`() {
        TrackCourses.all.forEach { c ->
            assertEquals(c.id, 2, CourseScenarios.forCourse(c.id).size)
            assertEquals(c.id == TrackCourses.EXAM, c.isExam)
            c.zones.forEach { z -> assertTrue(z.id, z.guide.none(Char::isDigit) && z.announce.none(Char::isDigit)) }
        }
    }

    @Test fun `zones of a course do not overlap`() {
        TrackCourses.all.forEach { c ->
            c.zones.forEachIndexed { i, a ->
                c.zones.drop(i + 1).forEach { b ->
                    val overlap = a.area.minX < b.area.maxX && b.area.minX < a.area.maxX && a.area.minY < b.area.maxY && b.area.minY < a.area.maxY
                    assertTrue("${c.id}: ${a.id} overlaps ${b.id}", !overlap)
                }
            }
        }
    }
}
