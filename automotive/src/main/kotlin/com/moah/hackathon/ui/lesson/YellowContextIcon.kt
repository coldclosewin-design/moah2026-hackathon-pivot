package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachColors

internal val LocalContextAccent = staticCompositionLocalOf { true }

internal enum class ContextIcon { Eye, Pin, Document, Calendar }

internal fun modeIconBars(mode: com.moah.hackathon.feature.lesson.LessonMode): Int = when (mode) {
    com.moah.hackathon.feature.lesson.LessonMode.GUIDE -> 3
    com.moah.hackathon.feature.lesson.LessonMode.HINT -> 2
    else -> 1
}

@Composable
internal fun YellowContextIcon(kind: ContextIcon, modifier: Modifier = Modifier) {
    Canvas(modifier.size(64.dp).background(if (LocalContextAccent.current) CoachColors.Accent else CoachColors.Lavender, CircleShape).clearAndSetSemantics {}) {
        withTransform({ scale(size.width / 64f, size.height / 64f, Offset.Zero) }) {
            val ink = CoachColors.Ink
            val line = Stroke(3.5f)
            when (kind) {
                ContextIcon.Eye -> {
                    drawPath(Path().apply {
                        moveTo(12f, 32f); quadraticTo(32f, 7f, 52f, 32f)
                        quadraticTo(32f, 57f, 12f, 32f); close()
                    }, ink, style = line)
                    drawCircle(ink, 7f, Offset(32f, 32f))
                }
                ContextIcon.Pin -> {
                    drawPath(Path().apply {
                        moveTo(32f, 51f); cubicTo(5f, 24f, 19f, 12f, 32f, 12f)
                        cubicTo(45f, 12f, 59f, 24f, 32f, 51f); close()
                    }, ink, style = line)
                    drawCircle(ink, 5f, Offset(32f, 27f), style = line)
                }
                ContextIcon.Document, ContextIcon.Calendar -> {
                    drawRoundRect(ink, Offset(19f, 15f), Size(27f, 35f), CornerRadius(4f), style = line)
                    for (y in listOf(25f, 32f, 39f)) drawLine(ink, Offset(25f, y), Offset(40f, y), 3f)
                    if (kind == ContextIcon.Calendar) for (x in listOf(25f, 39f))
                        drawLine(ink, Offset(x, 10f), Offset(x, 21f), 3f)
                }
            }
        }
    }
}
