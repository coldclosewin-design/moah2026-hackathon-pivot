package com.moah.hackathon.ui.lesson

import kotlin.math.*

/** Diagram units, with width 100 and the nose down. This is illustrative, not vehicle calibration. */
internal data class SteeringPoint(val x: Double, val y: Double)
internal class SteeringGeometry(steeringDeg: Float?) {
    val frontY = HEIGHT * 213 / 250
    val rearY = HEIGHT * 42 / 250
    val wheelbase = frontY - rearY
    val frontLeftX = 3.6 // Body edge at the axle is x≈2.27; 1/3 of the 8-unit tyre sits outside.
    val frontRightX = 100 - frontLeftX
    private val halfTrack = 50 - frontLeftX
    private val centralAngle = Math.toRadians(wheelRotation(steeringDeg).toDouble())
    // Limiting the common radius (rather than either wheel independently) preserves Ackermann.
    val centerX: Double? = if (centralAngle == 0.0) null else
        50 + sign(centralAngle) * max(wheelbase / tan(abs(centralAngle)), wheelbase + halfTrack)

    fun frontAngle(x: Double): Float = centerX?.let {
        Math.toDegrees(atan(wheelbase / (it - x))).toFloat()
    } ?: 0f

    /** Signed travel along the wheel circle; negative travel draws the rearward parking guides. */
    fun point(x: Double, y: Double, travel: Double): SteeringPoint {
        val cx = centerX ?: return SteeringPoint(x, y + travel)
        val dx = x - cx
        val dy = y - rearY
        val sweep = -sign(cx - 50) * travel / hypot(dx, dy)
        // Compute displacement from the start to avoid subtracting two huge radii near neutral.
        val cosMinusOne = -2 * sin(sweep / 2).pow(2)
        return SteeringPoint(x + dx * cosMinusOne - dy * sin(sweep),
            y + dx * sin(sweep) + dy * cosMinusOne)
    }

    companion object { const val HEIGHT = 100 / .43 }
}

/** Driver-side left/right, mirrored by the rear-up display convention. */
internal fun wheelAngles(steeringDeg: Float?): Pair<Float, Float> = SteeringGeometry(steeringDeg).let {
    it.frontAngle(it.frontRightX) to it.frontAngle(it.frontLeftX)
}
