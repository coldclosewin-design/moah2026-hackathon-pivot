package com.moah.hackathon.ui.lesson

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/** Shared 100 x 250 silhouette, rear up. Callers set its size, palette and entry orientation. */
internal fun DrawScope.vehicleSilhouette(bodyColor: Color, panelColor: Color, glassColor: Color) {
    // One shaped body, two separate bonnet/trunk panels, four glass faces and mirrors.
    drawPath(silhouettePath(VehicleSilhouetteGeometry.body), bodyColor)
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
            drawPath(silhouettePath(VehicleSilhouetteGeometry.mirror), bodyColor)
        }
    }
}

private fun silhouettePath(segments: List<List<Float>>) = Path().apply {
    moveTo(segments.first()[0], segments.first()[1])
    for (p in segments.drop(1)) when (p.size) {
        2 -> lineTo(p[0], p[1])
        4 -> quadraticTo(p[0], p[1], p[2], p[3])
        6 -> cubicTo(p[0], p[1], p[2], p[3], p[4], p[5])
    }
    close()
}
