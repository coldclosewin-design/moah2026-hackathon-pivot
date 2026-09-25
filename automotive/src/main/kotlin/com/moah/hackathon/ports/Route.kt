package com.moah.hackathon.ports

import kotlin.math.atan2
import kotlin.math.cos

/**
 * 한 구간(출발 → 정거장)의 경로 좌표. 화면은 이것을 직접 받지 않는다 — 끝점이 곧 목적지라서
 * 숨긴 구간에서 그대로 그리면 어디로 가는지 드러난다. 화면에는 [view] 가 만든 [RouteView] 만 간다.
 */
data class Route(val points: List<LatLng>) {
    init { require(points.size >= 2) { "a route needs at least two points" } }

    private val cumulative: DoubleArray = DoubleArray(points.size).also { acc ->
        for (i in 1 until points.size) acc[i] = acc[i - 1] + points[i - 1].distanceTo(points[i])
    }

    val lengthMeters: Double get() = cumulative.last()

    /**
     * @param progress 구간 진행률 0~1 (연수 세션의 구간 진행률). 경로 길이에 대한 비율로 쓴다
     *   — 남은 거리는 직선 기준이고 경로는 굽어 있어 실제 위치와 조금 다르지만, 출발에서 0·도착에서 1 이고 단조 증가한다.
     * @param hidden 숨긴 구간이면 목적지와, 차 앞 [lookaheadMeters] 너머의 경로를 주지 않는다.
     * @param approach 정거장에서 주차/픽업 존까지의 접근로. 공개된 구간에서만 전달된다.
     */
    fun view(
        progress: Double,
        hidden: Boolean,
        lookaheadMeters: Double = DEFAULT_LOOKAHEAD_METERS,
        approach: List<LatLng> = emptyList(),
    ): RouteView {
        val p = if (progress.isFinite()) progress.coerceIn(0.0, 1.0) else 0.0
        val at = p * lengthMeters
        val carIndex = segmentAt(at)
        val car = interpolate(carIndex, at)
        val traveledLl = points.subList(0, carIndex + 1) + car

        val aheadEnd = if (hidden) (at + lookaheadMeters).coerceAtMost(lengthMeters) else lengthMeters
        val endIndex = segmentAt(aheadEnd)
        val aheadLl = buildList {
            add(car)
            for (i in carIndex + 1..endIndex) add(points[i])
            add(interpolate(endIndex, aheadEnd))
        }.distinctConsecutive()

        val origin = points.first()
        val next = aheadLl.getOrNull(1) ?: traveledLl.getOrNull(traveledLl.size - 2)?.let { mirror(it, car) } ?: car
        return RouteView(
            traveled = traveledLl.distinctConsecutive().map { it.toMapPoint(origin) },
            ahead = aheadLl.map { it.toMapPoint(origin) },
            car = car.toMapPoint(origin),
            headingDegrees = heading(car.toMapPoint(origin), next.toMapPoint(origin)),
            destination = if (hidden) null else points.last().toMapPoint(origin),
            approach = if (hidden) emptyList() else approach.map { it.toMapPoint(origin) },
            hidden = hidden,
        )
    }

    /**
     * 모서리를 둥글린 경로(Chaikin 모서리 깎기, 양 끝점은 그대로). 손으로 찍은 꺾은선은 점 간격이 1 km 안팎이라
     * 지도에서 나들목이 직각으로 꺾인 직선처럼 보인다 — 점을 늘려 곡선으로 만든다. 길이는 조금 짧아지지만
     * 화면은 진행률(비율)로만 쓰므로 출발 0·도착 1 은 그대로다.
     */
    fun smoothed(iterations: Int = 3): Route {
        var current = points
        repeat(iterations.coerceAtLeast(0)) {
            if (current.size < 3) return@repeat
            val next = ArrayList<LatLng>(current.size * 2)
            next.add(current.first())
            for (i in 0 until current.size - 1) {
                val a = current[i]
                val b = current[i + 1]
                if (i > 0) next.add(LatLng(a.lat * 0.75 + b.lat * 0.25, a.lng * 0.75 + b.lng * 0.25))
                if (i < current.size - 2) next.add(LatLng(a.lat * 0.25 + b.lat * 0.75, a.lng * 0.25 + b.lng * 0.75))
            }
            next.add(current.last())
            current = next
        }
        return Route(current)
    }

    private fun segmentAt(meters: Double): Int {
        var i = 0
        while (i < points.size - 2 && cumulative[i + 1] <= meters) i++
        return i
    }

    private fun interpolate(segment: Int, meters: Double): LatLng {
        val start = cumulative[segment]
        val length = cumulative[segment + 1] - start
        val t = if (length <= 0) 0.0 else ((meters - start) / length).coerceIn(0.0, 1.0)
        val a = points[segment]
        val b = points[segment + 1]
        return LatLng(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
    }

    companion object {
        /** 숨긴 구간에서 차 앞으로 보여줄 거리. 방향은 알려 주되 어디로 가는지는 알 수 없는 길이. */
        const val DEFAULT_LOOKAHEAD_METERS = 900.0

        fun straight(from: LatLng, to: LatLng) = Route(listOf(from, to))
    }
}

/** 화면용 평면 좌표(m). 구간 출발점이 원점, x = 동쪽, y = 북쪽. */
data class MapPoint(val x: Double, val y: Double)

/**
 * 화면이 그려도 되는 것만 담은 경로. 숨긴 구간에서는 [destination] 이 null 이고 [ahead] 는 차 앞 일부에서 끊긴다
 * (그 끝이 "안개"가 시작되는 곳이다).
 */
data class RouteView(
    /** 출발점 ~ 차. */
    val traveled: List<MapPoint>,
    /** 차 ~ (공개: 목적지 / 숨김: 차 앞 lookahead 지점). 첫 점은 [car]. */
    val ahead: List<MapPoint>,
    val car: MapPoint,
    /** 진행 방향. 0 = 북, 90 = 동 (시계 방향). */
    val headingDegrees: Double,
    val destination: MapPoint?,
    /** 목적지 ~ 주차/픽업 존. 공개 단계에서만. */
    val approach: List<MapPoint>,
    val hidden: Boolean,
)

private fun LatLng.toMapPoint(origin: LatLng): MapPoint {
    val metersPerDegLat = 111_320.0
    val metersPerDegLng = metersPerDegLat * cos(Math.toRadians(origin.lat))
    return MapPoint((lng - origin.lng) * metersPerDegLng, (lat - origin.lat) * metersPerDegLat)
}

private fun heading(from: MapPoint, to: MapPoint): Double {
    if (from == to) return 0.0
    val degrees = Math.toDegrees(atan2(to.x - from.x, to.y - from.y))
    return (degrees + 360.0) % 360.0
}

private fun mirror(previous: LatLng, car: LatLng) = LatLng(2 * car.lat - previous.lat, 2 * car.lng - previous.lng)

private fun List<LatLng>.distinctConsecutive(): List<LatLng> =
    filterIndexed { index, point -> index == 0 || point != this[index - 1] }
