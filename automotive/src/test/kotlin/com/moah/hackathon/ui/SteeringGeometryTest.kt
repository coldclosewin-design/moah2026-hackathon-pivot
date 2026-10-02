package com.moah.hackathon.ui

import com.moah.hackathon.ui.lesson.*
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SteeringGeometryTest {
    @Test fun fivePathsShareTheRearAxleCentreAndHaveDifferentRadii() {
        for (angle in listOf(-900f, -450f, -.01f, .01f, 450f, 900f)) {
            val g = SteeringGeometry(angle)
            val cx = g.centerX!!
            val starts = listOf(g.frontLeftX to g.frontY, 50.0 to g.frontY, g.frontRightX to g.frontY,
                7.0 to g.rearY, 93.0 to g.rearY)
            starts.forEach { (x, y) ->
                val radius = hypot(x - cx, y - g.rearY)
                listOf(-100.0, 0.0, 100.0, 220.0).forEach { travel ->
                    val p = g.point(x, y, travel)
                    assertEquals(radius, hypot(p.x - cx, p.y - g.rearY), .00001)
                }
            }
            val radii = starts.take(3).map { (x, y) -> hypot(x - cx, y - g.rearY) }
            assertTrue(radii[1] > min(radii[0], radii[2]) && radii[1] < max(radii[0], radii[2]))
            assertTrue(if (angle > 0) radii[2] < radii[0] else radii[0] < radii[2])
        }
    }

    @Test fun frontWheelAnglesAreTangentToTheGuideAndRearWheelsStartStraight() {
        for (angle in listOf(-900f, -450f, 450f, 900f)) {
            val g = SteeringGeometry(angle)
            for (x in listOf(g.frontLeftX, g.frontRightX)) {
                val next = g.point(x, g.frontY, .0001)
                val tangent = Math.toDegrees(atan2(next.x - x, next.y - g.frontY))
                assertEquals(g.frontAngle(x).toDouble(), tangent, .001)
            }
            for (x in listOf(7.0, 93.0)) {
                val next = g.point(x, g.rearY, -.0001)
                assertEquals(x, next.x, .00001)
                assertTrue(next.y < g.rearY)
            }
        }
    }

    @Test fun neutralMissingAndNearNeutralRemainContinuousAndMirrorLeftRight() {
        for (angle in listOf<Float?>(null, 0f, .00001f, -.00001f)) {
            val g = SteeringGeometry(angle)
            val p = g.point(g.frontLeftX, g.frontY, 150.0)
            assertEquals(g.frontLeftX, p.x, .0001)
            assertEquals(g.frontY + 150, p.y, .0001)
        }
        val left = SteeringGeometry(450f); val right = SteeringGeometry(-450f)
        for (travel in listOf(-100.0, 50.0, 220.0)) {
            val p = left.point(left.frontLeftX, left.frontY, travel)
            val q = right.point(right.frontRightX, right.frontY, travel)
            assertEquals(100 - p.x, q.x, .00001)
            assertEquals(p.y, q.y, .00001)
        }
    }
}
