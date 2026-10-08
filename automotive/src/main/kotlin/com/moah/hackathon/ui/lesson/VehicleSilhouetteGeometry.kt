package com.moah.hackathon.ui.lesson

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Exact painted bounds of V4's rounded body, including its 8-unit outline. */
internal object VehicleSilhouetteGeometry {
    const val LENGTH = 4.5f
    const val WIDTH = LENGTH * .43f

    fun verticalBounds(heading: Float): Pair<Float, Float> {
        val radians = Math.toRadians(heading.toDouble())
        val x = sin(radians) * WIDTH / 100
        val y = cos(radians) * LENGTH / 250
        // Body: x=5..95, y=0..250, corner radius=40, stroke radius=4.
        val radius = (5 * abs(x) + 85 * abs(y) + 44 * sqrt(x * x + y * y)).toFloat()
        return -radius to radius
    }
}
