package com.moah.hackathon.scoring

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PathReconstructorTest {

    private fun path(scenario: Scenario): List<PathPoint> {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in scenario.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return r.path()
    }

    @Test
    fun `good parking - reversing with the wheel right sends the tail back and to the right, nose swings left`() {
        val p = path(ParkingScenarios.good)
        assertTrue(p.size > 10)
        assertEquals(0f, p.first().x); assertEquals(0f, p.first().y)
        val end = p.last()
        assertTrue("y=${end.y}", end.y < -3f)                 // 뒤로 갔다
        assertTrue("x=${end.x}", end.x > 0.5f)                // 오른쪽으로 꺾어 들어갔다
        assertTrue("heading=${end.headingDeg}", end.headingDeg in 15f..120f)   // 차 앞이 왼쪽으로 돌았다(후진 우회전)
        val travelled = PathReconstructor.travelled(p)
        assertTrue("travelled=$travelled", travelled in 4f..12f)   // 약 7~8 m
        assertTrue(p.drop(1).all { it.reversing })              // 전진 구간 없음
    }

    @Test
    fun `bad parking - has a forward correction and moves further`() {
        val p = path(ParkingScenarios.bad)
        assertTrue(p.any { !it.reversing && it.tMillis in 14_000L..20_000L })
        assertTrue(PathReconstructor.travelled(p) > PathReconstructor.travelled(path(ParkingScenarios.good)))
    }

    @Test
    fun `no motion yields a path that stays at the origin`() {
        val p = path(ChecklistScenarios.good)
        assertTrue(p.all { abs(it.x) < 1e-3f && abs(it.y) < 1e-3f })
    }

    @Test
    fun `straight reverse then straight forward returns near the start`() {
        val speed = listOf(Sample(0L, 0f), Sample(1000L, 3.6f), Sample(3000L, 3.6f), Sample(4000L, 0f),
            Sample(5000L, 0f), Sample(6000L, 3.6f), Sample(8000L, 3.6f), Sample(9000L, 0f))
        val gear = listOf(Sample(0L, com.moah.hackathon.vehicle.Gear.REVERSE), Sample(4500L, com.moah.hackathon.vehicle.Gear.DRIVE))
        val p = PathReconstructor.reconstruct(speed, emptyList(), gear)
        assertTrue(abs(p.last().x) < 0.01f)
        assertTrue("y=${p.last().y}", abs(p.last().y) < 0.05f)
        assertEquals(0f, p.last().headingDeg)
    }
}
