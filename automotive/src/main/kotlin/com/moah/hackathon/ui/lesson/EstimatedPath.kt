package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.scoring.PathPoint
import com.moah.hackathon.ui.CoachColors
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
internal fun EstimatedPath(record: AttemptRecord, modifier: Modifier) {
    val time = remember(record) { Animatable(0f) }
    LaunchedEffect(record) {
        delay(500)
        time.animateTo(record.path.last().tMillis.toFloat(), tween(3_000, easing = LinearEasing))
    }
    Column(modifier.padding(top = 64.dp, bottom = 52.dp)) {
        Canvas(Modifier.weight(1f).fillMaxWidth().clipToBounds().semantics { contentDescription = "추정 궤적" }) {
            val viewport = pathViewport(record.path, size.width / density, size.height / density)
            val elapsed = time.value.roundToLong()
            val revealed = pathThroughTime(record.path, elapsed)
            fun position(point: PathPoint) = Offset(viewport.x(point.x).dp.toPx(), viewport.y(point.y).dp.toPx())
            // Match Maneuver's rear-up convention without changing recorded coordinates or time.
            rotate(180f, pivot = center) {
                val arrival = record.path.last()
                arrivalBay(position(arrival), arrival.headingDeg, viewport.scale.dp.toPx())
                pathLegs(revealed).forEach { leg ->
                    drawPath(Path().apply {
                        leg.points.forEachIndexed { index, point ->
                            val p = position(point)
                            if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                    }, if (leg.reversing) CoachColors.Periwinkle else CoachColors.Lavender,
                        style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawCircle(CoachColors.Lavender, 6.dp.toPx(), position(record.path.first()))
                val current = revealed.last()
                pathCar(position(current), current.headingDeg, viewport.scale.dp.toPx(), CoachColors.Ink,
                    reversing = time.isRunning && elapsed < record.path.last().tMillis && current.reversing)
                record.score.metrics.harshEvents.filter { it.tMillis <= elapsed }.forEach { event ->
                    nearestPathPoint(record.path, event.tMillis)?.let { drawCircle(CoachColors.Signal, 10.dp.toPx(), position(it)) }
                }
            }
        }
        Column(Modifier.padding(start = 100.dp, end = 32.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LessonText("신호로 추정한 궤적이에요.", 32, CoachColors.Muted)
            LessonText("실제 위치와 다를 수 있어요.", 32, CoachColors.Muted)
        }
    }
}

/** The arrival pose defines this illustrative bay; its open edge faces the car's front. */
private fun DrawScope.arrivalBay(center: Offset, heading: Float, scale: Float) {
    val halfWidth = 1.8f * scale * 1.25f / 2
    val halfDepth = 4.5f * scale * 1.15f / 2
    rotate(-heading, center) {
        drawPath(Path().apply {
            moveTo(center.x - halfWidth, center.y - halfDepth)
            lineTo(center.x - halfWidth, center.y + halfDepth)
            lineTo(center.x + halfWidth, center.y + halfDepth)
            lineTo(center.x + halfWidth, center.y - halfDepth)
        }, CoachColors.Periwinkle, style = Stroke(4.dp.toPx()))
    }
}

private fun DrawScope.pathCar(center: Offset, heading: Float, scale: Float, color: Color,
    reversing: Boolean = false) {
    val width = 1.8f * scale
    val height = 4.5f * scale
    // Compose rotates clockwise; PathPoint heading is counterclockwise from screen-up.
    rotate(-heading, center) {
        val left = center.x - width / 2
        val top = center.y - height / 2
        drawRoundRect(color, Offset(left, top), Size(width, height), CornerRadius(width * .28f))
        drawPath(Path().apply {
            moveTo(left + width * .12f, top + height * .24f)
            quadraticTo(center.x, top + height * .14f, left + width * .88f, top + height * .24f)
            lineTo(left + width * .78f, top + height * .38f)
            quadraticTo(center.x, top + height * .34f, left + width * .22f, top + height * .38f); close()
        }, CoachColors.Paper)
        if (reversing) {
            val rear = center.y + height / 2 + .6f * scale
            drawPath(Path().apply {
                moveTo(center.x - width * .36f, rear - width * .16f)
                lineTo(center.x, rear)
                lineTo(center.x + width * .36f, rear - width * .16f)
            }, CoachColors.Signal, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawPath(Path().apply {
            moveTo(left + width * .22f, top + height * .72f)
            quadraticTo(center.x, top + height * .76f, left + width * .78f, top + height * .72f)
            lineTo(left + width * .87f, top + height * .86f)
            quadraticTo(center.x, top + height * .91f, left + width * .13f, top + height * .86f); close()
        }, CoachColors.Paper)
    }
}
