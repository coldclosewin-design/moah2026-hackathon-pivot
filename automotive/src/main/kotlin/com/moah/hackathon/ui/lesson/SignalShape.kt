package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.SignalAvailability

/** Source marks occupy the text cap height, independent of font glyph coverage and tile padding. */
@Composable
internal fun SignalShape(source: SignalAvailability, modifier: Modifier, color: Color = CoachColors.Ink) {
    Canvas(modifier.testTag("signal-shape-${source.name}").clearAndSetSemantics {}) {
        val diameter = size.minDimension * .82f
        val radius = diameter / 2
        val top = center - Offset(radius, radius)
        val stroke = diameter * .09f
        when (source) {
            SignalAvailability.LIVE -> drawCircle(color, radius)
            SignalAvailability.SIMULATED -> {
                drawCircle(color, radius - stroke / 2, style = Stroke(stroke))
                drawArc(color, 90f, 180f, true, top, Size(diameter, diameter))
            }
            SignalAvailability.MISSING -> drawCircle(color, radius - stroke / 2,
                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(diameter * .16f, diameter * .12f))))
        }
    }
}
