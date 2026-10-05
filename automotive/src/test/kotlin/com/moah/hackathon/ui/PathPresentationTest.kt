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
        assertTrue(view.y(2.25f) > 0f)
        assertTrue(view.y(-5f - .9f) < view.height)
        val short = pathViewport(listOf(point(0, 0f, 0f), point(1, 0f, -.5f)), 820f, 1000f)
        assertTrue(short.scale > view.scale)
        assertEquals(36f, pathViewport(listOf(point(0, 0f, 0f), point(1, 100f, 0f)), 820f, 1000f).scale, 0f)
    }

    @Test fun verticalFitBalancesTheFilledStartAndSettledFrontChevron() {
        val path = listOf(point(0, 0f, 0f, false), point(1, 0f, 6f, false))
        val view = pathViewport(path, 820f, 700f, frontEntry = true)
        val top = view.y(6f) - 2.85f * view.scale - 3f
        // Rear quadratic reaches source y=-.5, without an outline stroke.
        val bottom = view.height - (view.y(0f) + 2.259f * view.scale)
        assertEquals(top, bottom, .001f)
        assertTrue(top > 0)
        assertEquals(410f, view.x(0f), .001f)
    }

    @Test fun verticalFitIncludesHarshDotsAtAnIntermediateExtreme() {
        val path = listOf(point(0, 0f, 0f), point(1, 2f, 6f), point(2, 4f, 0f))
        val view = pathViewport(path, 820f, 1000f, harshPoints = listOf(path[1]))
        val top = view.y(6f) - 10f
        val bottom = view.height - (view.y(0f) + 2.5875f * view.scale + 2f)
        assertEquals(top, bottom, .001f)
        assertTrue(top > 0)
    }

    @Test fun sidewaysStartIncludesTheMirrorBeyondTheBody() {
        val start = point(0, 0f, 10f, heading = 90f)
        val path = listOf(start, point(1, 0f, 0f))
        val scale = 50f
        val bounds = pathVerticalBounds(path, scale, frontEntry = false)
        // Q(-2,154; -8,152; -6,159) reaches x=-6.5 at t=.75, outside the 100-wide body.
        val mirror = 56.5f / 100 * (4.5f * .43f)
        assertEquals((10 + mirror) * scale, bounds.second, .001f)
        assertEquals(-mirror, VehicleSilhouetteGeometry.verticalBounds(90f).first, .001f)
        // Reversing the pose swaps the asymmetric nose/tail extrema; no axis-aligned box padding.
        assertEquals(2.259f, VehicleSilhouetteGeometry.verticalBounds(180f).second, .001f)
        assertEquals(-2.2545f, VehicleSilhouetteGeometry.verticalBounds(180f).first, .001f)
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

    @Test fun adjacentBoundariesStayInsideTheViewportAtEveryParkingHeading() {
        for ((heading, parallel) in listOf(90f to false, 45f to false, 0f to true)) {
            val path = listOf(point(0, 0f, 0f), point(1, 3f, -6f, heading = heading))
            val view = pathViewport(path, 1280f, 730f, targetHeading = heading, neighborBays = true, parallel = parallel)
            val a = Math.toRadians(heading.toDouble())
            val ends = parkingNeighborLines(parallel).flatMap { listOf(it.first, it.second) }
            assertEquals(if (parallel) 4 else 8, ends.size)
            ends.forEach { (x, y) ->
                val px = view.x((3 + x * kotlin.math.cos(a) - y * kotlin.math.sin(a)).toFloat())
                val py = view.y((-6 + x * kotlin.math.sin(a) + y * kotlin.math.cos(a)).toFloat())
                assertTrue("Boundary clipped at $heading: $px,$py", px > 2 && px < 1278 && py > 2 && py < 728)
            }
            assertEquals(3 * view.scale, view.x(3f) - view.x(0f), .001f)
        }
    }
}
