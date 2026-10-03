package com.moah.hackathon.ui.lesson

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Original rear-up outline: first point, then line/quadratic/cubic controls and endpoints. */
internal object VehicleSilhouetteGeometry {
    const val LENGTH = 4.5f
    const val WIDTH = LENGTH * .43f

    val body = listOf(
        listOf(25f, 1f), listOf(10f, 3f, 6f, 12f, 5f, 28f),
        listOf(2f, 46f), listOf(5f, 76f), listOf(5f, 173f),
        listOf(-1f, 210f, 1f, 232f, 15f, 242f),
        listOf(30f, 253f, 70f, 253f, 85f, 242f),
        listOf(99f, 232f, 101f, 210f, 95f, 173f),
        listOf(95f, 76f), listOf(98f, 46f), listOf(95f, 28f),
        listOf(94f, 12f, 90f, 3f, 75f, 1f), listOf(50f, -2f, 25f, 1f))
    val mirror = listOf(listOf(7f, 159f), listOf(-2f, 154f),
        listOf(-8f, 152f, -6f, 159f), listOf(-4f, 162f, 7f, 165f))

    /** Metres, +y towards the car front at heading zero. Includes both mirrors (controls x -8..108).
     * Project each Bezier before finding its extrema; a rotated bounding rectangle adds empty corners. */
    fun verticalBounds(heading: Float): Pair<Float, Float> {
        val angle = Math.toRadians(heading.toDouble())
        val sine = sin(angle)
        val cosine = cos(angle)
        var bottom = Double.POSITIVE_INFINITY
        var top = Double.NEGATIVE_INFINITY
        fun outline(segments: List<List<Float>>, reflected: Boolean = false) {
            fun project(x: Float, y: Float): Double {
                val sourceX = if (reflected) 100f - x else x
                return (50 - sourceX) * WIDTH / 100 * sine + (y - 125) * LENGTH / 250 * cosine
            }
            var previous = project(segments.first()[0], segments.first()[1])
            for (segment in segments.drop(1)) {
                val points = listOf(previous) + segment.chunked(2).map { project(it[0], it[1]) }
                val candidates = mutableListOf(0.0, 1.0)
                if (points.size == 3) {
                    val denominator = points[0] - 2 * points[1] + points[2]
                    if (abs(denominator) > 1e-10) candidates += (points[0] - points[1]) / denominator
                } else if (points.size == 4) {
                    val a = -points[0] + 3 * points[1] - 3 * points[2] + points[3]
                    val b = 2 * (points[0] - 2 * points[1] + points[2])
                    val c = points[1] - points[0]
                    if (abs(a) < 1e-10) {
                        if (abs(b) > 1e-10) candidates += -c / b
                    } else if (b * b - 4 * a * c >= 0) {
                        val root = sqrt(b * b - 4 * a * c)
                        candidates += (-b + root) / (2 * a)
                        candidates += (-b - root) / (2 * a)
                    }
                }
                candidates.filter { it in 0.0..1.0 }.forEach { t ->
                    var values = points
                    while (values.size > 1) values = values.zipWithNext { a, b -> a * (1 - t) + b * t }
                    bottom = minOf(bottom, values.single())
                    top = maxOf(top, values.single())
                }
                previous = points.last()
            }
        }
        outline(body)
        outline(mirror)
        outline(mirror, reflected = true)
        return bottom.toFloat() to top.toFloat()
    }
}
