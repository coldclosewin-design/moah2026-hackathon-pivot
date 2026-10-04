package com.moah.hackathon.ui.lesson

import com.moah.hackathon.scoring.PathPoint
import com.moah.hackathon.scoring.PathReconstructor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal fun hasEstimatedPath(path: List<PathPoint>) = path.isNotEmpty() && PathReconstructor.travelled(path) >= .5f

/** Design dp; include the rotated cars in the fit without altering any path sample. */
internal data class PathViewport(val scale: Float, val centerX: Float, val centerY: Float, val width: Float, val height: Float) {
    fun x(meters: Float) = width / 2 + (meters - centerX) * scale
    fun y(meters: Float) = height / 2 - (meters - centerY) * scale
}

internal fun pathViewport(path: List<PathPoint>, width: Float, height: Float,
    frontEntry: Boolean = false, harshPoints: List<PathPoint> = emptyList(), targetHeading: Float? = null): PathViewport {
    require(path.isNotEmpty())
    val extent = path.map { it.x to it.y }.toMutableList()
    listOf(path.first(), path.last()).forEach { point ->
        val radians = Math.toRadians(point.headingDeg.toDouble())
        for (x in listOf(-.9f, .9f)) for (y in listOf(-2.25f, 2.25f)) {
            extent += (point.x + x * cos(radians) - y * sin(radians)).toFloat() to
                (point.y + x * sin(radians) + y * cos(radians)).toFloat()
        }
    }
    val left = extent.minOf { it.first }; val right = extent.maxOf { it.first }
    val bottom = extent.minOf { it.second }; val top = extent.maxOf { it.second }
    val scale = minOf((width - 240f) / (right - left).coerceAtLeast(.1f),
        (height - 240f) / (top - bottom).coerceAtLeast(.1f)).coerceAtLeast(36f)
    // Retain the horizontal fit. Centre the final painted scene, including fixed-dp strokes,
    // so the bay stays anchored while the measured path is replayed.
    val (paintBottom, paintTop) = pathVerticalBounds(path, scale, frontEntry, harshPoints, targetHeading)
    return PathViewport(scale, (left + right) / 2, (paintBottom + paintTop) / (2 * scale), width, height)
}

/** Painted vertical bounds in scaled world coordinates (+y up), before the rear-view rotation. */
internal fun pathVerticalBounds(path: List<PathPoint>, scale: Float, frontEntry: Boolean,
    harshPoints: List<PathPoint> = emptyList(), targetHeading: Float? = null): Pair<Float, Float> {
    var bottom = Float.POSITIVE_INFINITY
    var top = Float.NEGATIVE_INFINITY
    fun include(y: Float, radius: Float) {
        bottom = minOf(bottom, y - radius)
        top = maxOf(top, y + radius)
    }
    path.forEach { include(it.y * scale, 3f) } // Round 6 dp path strokes.
    harshPoints.forEach { include(it.y * scale, 10f) }
    val start = path.first()
    // Filled silhouettes share their exact body and mirror curves with the renderer.
    for (point in listOf(start, path.last())) {
        val (low, high) = VehicleSilhouetteGeometry.verticalBounds(point.headingDeg)
        include((point.y + low) * scale, 0f)
        include((point.y + high) * scale, 0f)
    }
    val end = path.last()
    val angle = Math.toRadians((targetHeading ?: end.headingDeg).toDouble())
    // All four bay corners are painted, including the two on its open edge.
    include(end.y * scale, (abs(sin(angle)) * 1.125f + abs(cos(angle)) * 2.5875f).toFloat() * scale + 2f)
    if (frontEntry) {
        fun chevron(point: PathPoint, heading: Float, tip: Float) {
            val a = Math.toRadians(heading.toDouble())
            for ((x, y) in listOf(0f to tip, -.648f to tip - .288f, .648f to tip - .288f)) {
                include(point.y * scale + (x * sin(a) + y * cos(a)).toFloat() * scale, 3f)
            }
        }
        chevron(end, end.headingDeg, 2.85f)
        initialTravelHeading(path)?.let { chevron(start, it, .8f) }
    }
    return bottom to top
}

internal data class PathLeg(val reversing: Boolean, val points: List<PathPoint>)

/** Each destination sample describes the gear used for the segment arriving there. */
internal fun pathLegs(path: List<PathPoint>): List<PathLeg> {
    if (path.size < 2) return emptyList()
    val legs = mutableListOf<PathLeg>()
    var reversing = path[1].reversing
    var points = mutableListOf(path.first())
    path.drop(1).forEach { point ->
        if (point.reversing != reversing) {
            legs += PathLeg(reversing, points)
            points = mutableListOf(points.last())
            reversing = point.reversing
        }
        points += point
    }
    legs += PathLeg(reversing, points)
    return legs
}

internal fun nearestPathPoint(path: List<PathPoint>, tMillis: Long) = path.minByOrNull { kotlin.math.abs(it.tMillis - tMillis) }

/** Reveal measured time, not sample index; the partial segment keeps its destination gear. */
internal fun pathThroughTime(path: List<PathPoint>, tMillis: Long): List<PathPoint> {
    if (path.isEmpty()) return emptyList()
    if (tMillis < path.first().tMillis) return listOf(path.first())
    val next = path.indexOfFirst { it.tMillis > tMillis }
    if (next == -1) return path
    val before = path[next - 1]
    if (before.tMillis == tMillis) return path.take(next)
    val after = path[next]
    val fraction = (tMillis - before.tMillis).toFloat() / (after.tMillis - before.tMillis)
    val headingDelta = ((after.headingDeg - before.headingDeg + 540f) % 360f) - 180f
    return path.take(next) + PathPoint(tMillis,
        before.x + (after.x - before.x) * fraction,
        before.y + (after.y - before.y) * fraction,
        before.headingDeg + headingDelta * fraction, after.reversing)
}

/** Initial travel direction, skipping stationary samples; independent of body heading and gear. */
internal fun initialTravelHeading(path: List<PathPoint>): Float? = path.zipWithNext()
    .firstOrNull { (a, b) -> kotlin.math.hypot(b.x - a.x, b.y - a.y) > .001f }
    ?.let { (a, b) -> Math.toDegrees(kotlin.math.atan2(-(b.x - a.x).toDouble(), (b.y - a.y).toDouble())).toFloat() }
