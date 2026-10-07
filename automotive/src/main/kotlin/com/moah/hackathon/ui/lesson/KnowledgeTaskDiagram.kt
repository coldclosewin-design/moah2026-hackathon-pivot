package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/** Six topic marks, drawn in the same single-ink line/face language as the parking bays. */
@Composable
internal fun KnowledgeTaskDiagram(id: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 240f, size.height / 176f, Offset.Zero) }) {
            val stroke = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                drawLine(color, Offset(x1, y1), Offset(x2, y2), 6f, cap = StrokeCap.Round)
            when (id) {
                "knowledge-hazard-weather" -> {
                    drawPath(Path().apply {
                        moveTo(120f, 22f); lineTo(196f, 154f); lineTo(44f, 154f); close()
                    }, color, style = stroke)
                    line(120f, 66f, 120f, 110f)
                    drawCircle(color, 4f, Offset(120f, 132f))
                }
                "knowledge-marking" -> {
                    line(42f, 28f, 42f, 148f); line(198f, 28f, 198f, 148f)
                    line(120f, 146f, 120f, 32f)
                    line(120f, 32f, 98f, 54f); line(120f, 32f, 142f, 54f)
                    line(120f, 96f, 78f, 96f)
                    line(78f, 96f, 94f, 80f); line(78f, 96f, 94f, 112f)
                }
                "knowledge-turn-signal" -> {
                    drawRoundRect(color, Offset(89f, 18f), Size(62f, 140f), CornerRadius(24f), style = stroke)
                    listOf(48f, 88f, 128f).forEachIndexed { index, y ->
                        drawCircle(color, 12f, Offset(120f, y), style = if (index == 2) androidx.compose.ui.graphics.drawscope.Fill else stroke)
                    }
                }
                "knowledge-priority" -> {
                    listOf(-1f, 1f).forEach { x -> listOf(-1f, 1f).forEach { y ->
                        drawPath(Path().apply {
                            moveTo(120f + x * 84f, 88f + y * 30f)
                            lineTo(120f + x * 32f, 88f + y * 30f)
                            lineTo(120f + x * 32f, 88f + y * 68f)
                        }, color, style = stroke)
                    } }
                    line(72f, 88f, 162f, 88f)
                    line(162f, 88f, 144f, 70f); line(162f, 88f, 144f, 106f)
                }
                "knowledge-manner" -> {
                    // Two merging lanes meet a single shared road.
                    drawPath(Path().apply {
                        moveTo(64f, 152f); lineTo(64f, 120f)
                        quadraticTo(64f, 88f, 120f, 76f); lineTo(120f, 24f)
                    }, color, style = stroke)
                    drawPath(Path().apply {
                        moveTo(176f, 152f); lineTo(176f, 120f)
                        quadraticTo(176f, 88f, 120f, 76f)
                    }, color, style = stroke)
                    line(120f, 24f, 98f, 46f); line(120f, 24f, 142f, 46f)
                }
                else -> {
                    // Confusable rules: a geometric check paired with a cross, without lettering.
                    drawCircle(color, 38f, Offset(67f, 88f), style = stroke)
                    drawPath(Path().apply {
                        moveTo(45f, 88f); lineTo(61f, 104f); lineTo(89f, 72f)
                    }, color, style = stroke)
                    drawCircle(color, 38f, Offset(173f, 88f), style = stroke)
                    line(157f, 72f, 189f, 104f); line(189f, 72f, 157f, 104f)
                }
            }
        }
    }
}
