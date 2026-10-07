package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.CubicBezierEasing

/** Motion from the selected A1 / B1 proposals, in milliseconds. */
internal object CoachMotion {
    const val PressMillis = 360
    const val ReleaseMillis = 260
    const val HomePressMillis = 280
    const val SelectMillis = 280
    val HomeFill = CubicBezierEasing(.4f, 0f, .2f, 1f)
    val Fill = CubicBezierEasing(.2f, .8f, .2f, 1f)
    val Release = CubicBezierEasing(.4f, 0f, .6f, 1f)
    val Stone = CubicBezierEasing(.3f, 1.25f, .5f, 1f)
}
