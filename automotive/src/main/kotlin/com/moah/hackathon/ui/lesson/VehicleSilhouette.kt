package com.moah.hackathon.ui.lesson

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/** Shared 100 x 250 silhouette, rear up. Callers set its size, palette and entry orientation. */
internal fun DrawScope.vehicleSilhouette(bodyColor: Color, panelColor: Color, glassColor: Color) {
    // One shaped body, two separate bonnet/trunk panels, four glass faces and mirrors.
    drawPath(Path().apply {
        moveTo(25f, 1f); cubicTo(10f, 3f, 6f, 12f, 5f, 28f)
        lineTo(2f, 46f); lineTo(5f, 76f); lineTo(5f, 173f)
        cubicTo(-1f, 210f, 1f, 232f, 15f, 242f)
        cubicTo(30f, 253f, 70f, 253f, 85f, 242f)
        cubicTo(99f, 232f, 101f, 210f, 95f, 173f)
        lineTo(95f, 76f); lineTo(98f, 46f); lineTo(95f, 28f)
        cubicTo(94f, 12f, 90f, 3f, 75f, 1f)
        quadraticTo(50f, -2f, 25f, 1f); close()
    }, bodyColor)
    drawPath(Path().apply {
        moveTo(19f, 3f); quadraticTo(50f, -1f, 81f, 3f)
        quadraticTo(82f, 18f, 76f, 34f); lineTo(24f, 34f)
        quadraticTo(18f, 18f, 19f, 3f); close()
    }, panelColor)
    drawPath(Path().apply {
        moveTo(15f, 174f); quadraticTo(50f, 183f, 85f, 174f)
        quadraticTo(85f, 212f, 72f, 244f)
        quadraticTo(50f, 248f, 28f, 244f)
        quadraticTo(15f, 212f, 15f, 174f); close()
    }, panelColor)
    drawPath(Path().apply {
        moveTo(23f, 32f); quadraticTo(50f, 26f, 77f, 32f)
        lineTo(74f, 49f); quadraticTo(50f, 52f, 26f, 49f); close()
    }, glassColor)
    drawPath(Path().apply {
        moveTo(22f, 137f); quadraticTo(50f, 143f, 78f, 137f)
        lineTo(86f, 170f); quadraticTo(50f, 190f, 14f, 170f); close()
    }, glassColor)
    listOf(false, true).forEach { right ->
        withTransform({ if (right) { translate(100f, 0f); scale(-1f, 1f, Offset.Zero) } }) {
            drawPath(Path().apply {
                moveTo(17f, 52f); quadraticTo(24f, 90f, 19f, 126f)
                lineTo(11f, 162f); lineTo(11f, 93f); close()
            }, glassColor)
            drawLine(bodyColor, Offset(10f, 103f), Offset(23f, 92f), 2f)
            drawPath(Path().apply {
                moveTo(7f, 159f); lineTo(-2f, 154f)
                quadraticTo(-8f, 152f, -6f, 159f)
                quadraticTo(-4f, 162f, 7f, 165f); close()
            }, bodyColor)
        }
    }
}
