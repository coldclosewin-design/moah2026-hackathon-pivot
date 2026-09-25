package com.moah.hackathon.ports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class RouteTest {
    /** 손으로 찍은 12점 경로(서초 → 운중동, 약 12 km)를 둥글린 것. 연수 코스 시드가 생기면 그쪽을 쓴다. */
    private val route = Route(listOf(
        LatLng(37.4837, 127.0324), LatLng(37.4760, 127.0345), LatLng(37.4680, 127.0395), LatLng(37.4560, 127.0470),
        LatLng(37.4440, 127.0580), LatLng(37.4320, 127.0720), LatLng(37.4200, 127.0850), LatLng(37.4105, 127.0940),
        LatLng(37.4020, 127.0990), LatLng(37.3975, 127.0900), LatLng(37.3950, 127.0780), LatLng(37.3940, 127.0680),
    )).smoothed()
    /** 도착점에서 주차 구역까지의 접근로 — 공개 구간에서만 화면에 전달된다. */
    private val approach = listOf(LatLng(37.3940, 127.0680), LatLng(37.3944, 127.0676), LatLng(37.3948, 127.0670))
    /** 경로 길이는 대원 거리, 화면 좌표는 평면 투영이라 길이가 0.1% 쯤 다르다. */
    private val PLANAR_TOLERANCE = 1.005
    private fun length(points: List<MapPoint>) = points.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }.sum()
    private fun distance(a: MapPoint, b: MapPoint) = hypot(b.x - a.x, b.y - a.y)

    @Test
    fun `hidden view never contains the destination and stops at the lookahead`() {
        val destination = route.view(1.0, hidden = false).destination!!
        for (step in 0..18) {
            val view = route.view(step / 20.0, hidden = true)
            assertNull(view.destination)
            assertTrue(view.approach.isEmpty())
            assertTrue("ahead is ${length(view.ahead)} m", length(view.ahead) <= Route.DEFAULT_LOOKAHEAD_METERS * PLANAR_TOLERANCE)
            assertTrue(
                "fog edge is ${distance(view.ahead.last(), destination)} m from the destination at step $step",
                distance(view.ahead.last(), destination) > 300.0,
            )
        }
    }

    @Test
    fun `announced view runs to the destination and carries the approach`() {
        val view = route.view(0.4, hidden = false, approach = approach)
        assertNotNull(view.destination)
        assertEquals(view.destination, view.ahead.last())
        assertEquals(approach.size, view.approach.size)
        assertEquals(route.lengthMeters, length(view.traveled) + length(view.ahead), route.lengthMeters * (PLANAR_TOLERANCE - 1))
    }

    @Test
    fun `car moves monotonically from origin to destination`() {
        val start = route.view(0.0, hidden = true)
        assertEquals(0.0, start.car.x, 0.001)
        assertEquals(0.0, start.car.y, 0.001)
        var previous = 0.0
        for (step in 1..20) {
            val traveled = length(route.view(step / 20.0, hidden = false).traveled)
            assertTrue(traveled > previous)
            previous = traveled
        }
        val end = route.view(1.0, hidden = false)
        assertEquals(0.0, distance(end.car, end.destination!!), 1.0)
    }

    @Test
    fun `heading follows the road - south-east out of Seocho`() {
        val heading = route.view(0.3, hidden = true).headingDegrees
        assertTrue("heading was $heading", heading in 100.0..180.0)
    }

    @Test
    fun `progress outside 0-1 and non-finite values are clamped`() {
        assertEquals(route.view(0.0, true).car, route.view(-3.0, true).car)
        assertEquals(route.view(1.0, false).car, route.view(7.0, false).car)
        assertEquals(route.view(0.0, true).car, route.view(Double.NaN, true).car)
    }

    @Test
    fun `smoothing keeps the endpoints and removes sharp corners`() {
        fun turns(r: Route): List<Double> = r.view(0.0, hidden = false).ahead.let { pts ->
            (1 until pts.size - 1).map { i ->
                val a = Math.atan2(pts[i].y - pts[i - 1].y, pts[i].x - pts[i - 1].x)
                val b = Math.atan2(pts[i + 1].y - pts[i].y, pts[i + 1].x - pts[i].x)
                Math.abs(Math.toDegrees(Math.atan2(Math.sin(b - a), Math.cos(b - a))))
            }
        }
        val raw = Route(listOf(LatLng(37.41, 127.09), LatLng(37.40, 127.10), LatLng(37.3975, 127.09), LatLng(37.394, 127.068)))
        val smooth = raw.smoothed()
        assertEquals(raw.points.first(), smooth.points.first())
        assertEquals(raw.points.last(), smooth.points.last())
        assertTrue("raw has a sharp corner", turns(raw).max() > 90.0)
        assertTrue("no corner sharper than 30 degrees, was ${turns(smooth).max()}", turns(smooth).max() < 30.0)
        assertTrue(smooth.lengthMeters <= raw.lengthMeters && smooth.lengthMeters > raw.lengthMeters * 0.9)
        // 픽스처 경로도 둥글린 것이어야 한다(지도에서 나들목이 직각으로 보이지 않게)
        assertTrue(turns(route).max() < 30.0)
        // 점 2개짜리는 그대로
        assertEquals(2, Route.straight(raw.points.first(), raw.points.last()).smoothed().points.size)
    }
}
