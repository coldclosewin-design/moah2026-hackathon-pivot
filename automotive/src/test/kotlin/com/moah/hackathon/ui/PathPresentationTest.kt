package com.moah.hackathon.ui

import com.moah.hackathon.scoring.PathPoint
import com.moah.hackathon.ui.lesson.*
import org.junit.Assert.*
import org.junit.Test

class PathPresentationTest {
    private fun point(t: Long, x: Float, y: Float, reverse: Boolean = true, heading: Float = 0f) = PathPoint(t, x, y, heading, reverse)

    @Test fun initialDirectionSkipsStopsAndUsesTravelRatherThanTheCarHeading() {
        val start = point(0, 0f, 0f, false, 90f)
        assertEquals(0f, initialTravelHeading(listOf(start, start.copy(tMillis = 100), point(200, 0f, 1f, false)))!!, .001f)
        assertEquals(-90f, initialTravelHeading(listOf(start, point(1, 1f, 0f)))!!, .001f)
        assertEquals(180f, kotlin.math.abs(initialTravelHeading(listOf(start, point(1, 0f, -1f)))!!), .001f)
        assertNull(initialTravelHeading(emptyList()))
        assertNull(initialTravelHeading(listOf(start, start.copy(tMillis = 100))))
    }

    @Test fun replayUsesIrregularTimestampsAndDestinationGear() {
        val path = listOf(point(0, 0f, 0f, false), point(100, 2f, -2f, true, -20f),
            point(1_000, 8f, -8f, false, -80f))
        assertEquals(listOf(path.first()), pathThroughTime(path, 0))
        val frame = pathThroughTime(path, 400)
        assertEquals(3, frame.size)
        assertEquals(point(400, 4f, -4f, false, -40f), frame.last())
        assertEquals(listOf(true, false), pathLegs(frame).map { it.reversing })
        assertEquals(path.take(2), pathThroughTime(path, 100))
        assertEquals(path, pathThroughTime(path, 1_000))
        assertEquals(path, pathThroughTime(path, 2_000))
    }

    @Test fun replayHandlesEmptyDuplicateTimesAndWrappedHeading() {
        assertTrue(pathThroughTime(emptyList(), 0).isEmpty())
        val path = listOf(point(100, 0f, 0f, heading = 350f), point(100, 1f, 1f, heading = 350f),
            point(300, 3f, 3f, heading = 10f))
        assertEquals(listOf(path.first()), pathThroughTime(path, 0))
        assertEquals(path.take(2), pathThroughTime(path, 100))
        assertEquals(360f, pathThroughTime(path, 200).last().headingDeg, .001f)
        assertEquals(path, pathThroughTime(path, 300))
    }

    @Test fun stationaryAndShortTravelDoNotShowAPathButAnOutAndBackDoes() {
        assertFalse(hasEstimatedPath(emptyList()))
        assertFalse(hasEstimatedPath(listOf(point(0, 0f, 0f))))
        assertFalse(hasEstimatedPath(listOf(point(0, 0f, 0f), point(1, 0f, .49f))))
        assertTrue(hasEstimatedPath(listOf(point(0, 0f, 0f), point(1, 0f, .5f))))
        assertTrue(hasEstimatedPath(listOf(point(0, 0f, 0f), point(1, 1f, 0f), point(2, 0f, 0f))))
    }

    @Test fun fitPreservesMetersAndScreenUpAndIncludesTheRotatedEndCar() {
        val path = listOf(point(0, 0f, 0f), point(1, 3f, -5f, heading = 90f))
        val view = pathViewport(path, 820f, 1000f)
        assertEquals(view.scale * 3f, view.x(3f) - view.x(0f), .001f)
        assertTrue(view.y(1f) < view.y(0f))
        assertTrue(view.x(3f + 2.25f) <= 700.01f)
        assertTrue(view.x(-.9f) >= 119.99f)
        assertTrue(view.y(2.25f) >= 119.99f)
        assertTrue(view.y(-5f - .9f) <= 880.01f)
        val short = pathViewport(listOf(point(0, 0f, 0f), point(1, 0f, -.5f)), 820f, 1000f)
        assertTrue(short.scale > view.scale)
        assertEquals(36f, pathViewport(listOf(point(0, 0f, 0f), point(1, 100f, 0f)), 820f, 1000f).scale, 0f)
    }

    @Test fun reverseForwardTransitionsShareTheirActualEndpointWithoutGaps() {
        val path = listOf(point(0, 0f, 0f, false), point(1, 0f, -2f), point(2, 1f, -3f),
            point(3, 0f, -2f, false), point(4, 1f, -4f))
        val legs = pathLegs(path)
        assertEquals(listOf(true, false, true), legs.map { it.reversing })
        assertEquals(path.take(3), legs[0].points)
        assertEquals(path.subList(2, 4), legs[1].points)
        assertEquals(path.takeLast(2), legs[2].points)
        assertEquals(path.zipWithNext(), legs.flatMap { it.points.zipWithNext() })
    }

    @Test fun harshMarkerUsesNearestTimestamp() {
        val path = listOf(point(0, 0f, 0f), point(1_000, 1f, -1f), point(2_000, 2f, -2f))
        assertEquals(path[1], nearestPathPoint(path, 1_100))
        assertEquals(path[0], nearestPathPoint(path, -100))
        assertNull(nearestPathPoint(emptyList(), 10))
    }
}
