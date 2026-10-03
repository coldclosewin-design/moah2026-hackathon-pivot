package com.moah.hackathon.ui.lesson

import com.moah.hackathon.scoring.PathPoint
import com.moah.hackathon.scoring.PathReconstructor
import kotlin.math.cos
import kotlin.math.sin

internal fun hasEstimatedPath(path: List<PathPoint>) = path.isNotEmpty() && PathReconstructor.travelled(path) >= .5f

/** Design dp; include the rotated cars in the fit without altering any path sample. */
internal data class PathViewport(val scale: Float, val centerX: Float, val centerY: Float, val width: Float, val height: Float) {
    fun x(meters: Float) = width / 2 + (meters - centerX) * scale
    fun y(meters: Float) = height / 2 - (meters - centerY) * scale
}

internal fun pathViewport(path: List<PathPoint>, width: Float, height: Float): PathViewport {
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
    return PathViewport(scale, (left + right) / 2, (top + bottom) / 2, width, height)
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
