package com.moah.hackathon.data

import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.MapShape
import com.moah.hackathon.scoring.Vec2
import org.junit.Test
import java.io.File

/**
 * 도면·시나리오 궤적을 `automotive/build/course-preview/<course>.json` 으로 쓴다 — 사람이 그림으로 확인하고(tools 없이 python 등),
 * Codex 가 지도 화면을 만들 때 참고한다. 검증은 하지 않는다(항상 통과).
 */
class CoursePreviewDump {
    private fun v(p: Vec2) = "[${p.x},${p.y}]"
    private fun q(s: String) = "\"" + s.replace("\"", "'") + "\""

    @Test fun dump() {
        val dir = File("build/course-preview").apply { mkdirs() }
        TrackCourses.all.forEach { c ->
            val shapes = c.map.shapes.joinToString(",") { s ->
                when (s) {
                    is MapShape.Road -> "{\"t\":\"road\",\"pts\":[${s.points.joinToString(",") { v(it) }}],\"w\":${s.widthM},\"lanes\":${s.lanes}}"
                    is MapShape.Ring -> "{\"t\":\"ring\",\"c\":${v(s.center)},\"r\":${s.radiusM},\"w\":${s.widthM}}"
                    is MapShape.Bay -> "{\"t\":\"bay\",\"c\":${v(s.center)},\"w\":${s.widthM},\"l\":${s.lengthM},\"h\":${s.headingDeg},\"target\":${s.target}}"
                    is MapShape.StopLine -> "{\"t\":\"stop\",\"a\":${v(s.a)},\"b\":${v(s.b)}}"
                    is MapShape.Crosswalk -> "{\"t\":\"cross\",\"a\":${v(s.a)},\"b\":${v(s.b)},\"w\":${s.widthM}}"
                    is MapShape.Light -> "{\"t\":\"light\",\"at\":${v(s.at)},\"f\":${s.facingDeg}}"
                    is MapShape.Ramp -> "{\"t\":\"ramp\",\"a\":[${s.area.minX},${s.area.minY},${s.area.maxX},${s.area.maxY}]}"
                    is MapShape.Label -> "{\"t\":\"label\",\"at\":${v(s.at)},\"text\":${q(s.text)}}"
                }
            }
            val zones = c.zones.joinToString(",") { z -> "{\"id\":${q(z.id)},\"title\":${q(z.title)},\"a\":[${z.area.minX},${z.area.minY},${z.area.maxX},${z.area.maxY}]}" }
            val runs = CourseScenarios.forCourse(c.id).joinToString(",") { sc ->
                val r = CourseRecorder(c)
                sc.steps.forEach { r.onDelta((it.atSeconds * 1000).toLong(), it.values) }
                val res = r.result()
                "{\"id\":${q(sc.id)},\"trail\":[${res.trail.joinToString(",") { "[${it.x},${it.y},${it.headingDeg}]" }}]," +
                    "\"ded\":[${res.deductions.joinToString(",") { d -> "{\"r\":${q(d.reason)},\"at\":${d.at?.let { v(it) } ?: "null"}}" }}]}"
            }
            File(dir, "${c.id}.json").writeText(
                "{\"id\":${q(c.id)},\"title\":${q(c.title)},\"w\":${c.map.widthM},\"h\":${c.map.heightM},\"shapes\":[$shapes],\"zones\":[$zones]," +
                    "\"route\":[${c.route.joinToString(",") { v(it) }}],\"runs\":[$runs]}")
        }
    }
}
