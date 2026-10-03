package com.moah.hackathon.ui.lesson

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/** One front-up silhouette for the catalogue, replay car and unfilled starting pose. */
internal fun DrawScope.smallCarMark(center: Offset, width: Float, height: Float,
    body: Color, window: Color, hood: Color, outline: Boolean = false) {
    val left = center.x - width / 2
    val top = center.y - height / 2
    val shape = Path().apply {
        addRoundRect(RoundRect(left, top, left + width, top + height,
            topLeftCornerRadius = CornerRadius(width * SmallCarGeometry.FRONT_RADIUS),
            topRightCornerRadius = CornerRadius(width * SmallCarGeometry.FRONT_RADIUS),
            bottomRightCornerRadius = CornerRadius(width * SmallCarGeometry.REAR_RADIUS),
            bottomLeftCornerRadius = CornerRadius(width * SmallCarGeometry.REAR_RADIUS)))
    }
    if (outline) drawPath(shape, body, style = Stroke(3.dp.toPx())) else {
        drawPath(shape, body)
        clipPath(shape) { drawRect(hood, Offset(left, top), Size(width, height / 3)) }
        // Keep the bright bonnet legible even on a light catalogue face.
        clipPath(shape) { drawPath(shape, body, style = Stroke(4.dp.toPx())) }
    }
    fun glass(fraction: Float, y: Float, depth: Float): Path = Path().apply {
        val half = width * fraction / 2
        moveTo(center.x - half, top + height * y)
        lineTo(center.x + half, top + height * y)
        lineTo(center.x + half * .8f, top + height * (y + depth))
        lineTo(center.x - half * .8f, top + height * (y + depth))
        close()
    }
    val front = glass(SmallCarGeometry.FRONT_WINDOW, .36f, .12f)
    val rear = glass(SmallCarGeometry.REAR_WINDOW, .73f, .10f)
    if (outline) {
        drawPath(front, body, style = Stroke(2.dp.toPx()))
        drawPath(rear, body, style = Stroke(2.dp.toPx()))
    } else {
        drawPath(front, window)
        drawPath(rear, window)
    }
}
