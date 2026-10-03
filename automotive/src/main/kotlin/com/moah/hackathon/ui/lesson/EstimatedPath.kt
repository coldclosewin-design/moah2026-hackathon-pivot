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
import androidx.compose.ui.graphics.PathEffect
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
import com.moah.hackathon.vehicle.Gear
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
internal fun EstimatedPath(record: AttemptRecord, modifier: Modifier, entryGear: Gear = Gear.REVERSE) {
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
            // Preserve measured coordinates and time; front entry already points screen-up.
            rotate(if (entryGear == Gear.DRIVE) 0f else 180f, pivot = center) {
                val arrival = record.path.last()
                arrivalBay(position(arrival), arrival.headingDeg, viewport.scale.dp.toPx(), frontEntry = entryGear == Gear.DRIVE)
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
                val settled = elapsed >= record.path.last().tMillis
                if (entryGear == Gear.DRIVE && settled) initialTravelHeading(record.path)?.let { heading ->
                    val start = position(record.path.first())
                    rotate(-heading, start) {
                        pathChevron(start - Offset(0f, .8f * viewport.scale.dp.toPx()), 1.8f * viewport.scale.dp.toPx(), -1f)
                    }
                }
                val current = revealed.last()
                pathCar(position(current), current.headingDeg, viewport.scale.dp.toPx(), CoachColors.Ink,
                    reversing = time.isRunning && elapsed < record.path.last().tMillis && current.reversing,
                    forward = entryGear == Gear.DRIVE && (settled || (time.isRunning && !current.reversing)))
                record.score.metrics.harshEvents.filter { it.tMillis <= elapsed }.forEach { event ->
                    nearestPathPoint(record.path, event.tMillis)?.let { drawCircle(CoachColors.Signal, 10.dp.toPx(), position(it)) }
                }
            }
        }
        Column(Modifier.padding(start = 100.dp, end = 32.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (entryGear == Gear.DRIVE) LessonText("앞으로 들어간 주차예요.", 32, CoachColors.Muted)
            LessonText("신호로 추정한 궤적이에요.", 32, CoachColors.Muted)
            LessonText("실제 위치와 다를 수 있어요.", 32, CoachColors.Muted)
        }
    }
}

/** The bay opens toward the approach: car front for rear entry, car rear for front entry. */
private fun DrawScope.arrivalBay(center: Offset, heading: Float, scale: Float, frontEntry: Boolean) {
    val halfWidth = 1.8f * scale * 1.25f / 2
    val halfDepth = 4.5f * scale * 1.15f / 2
    rotate(-heading + if (frontEntry) 180f else 0f, center) {
        drawPath(Path().apply {
            moveTo(center.x - halfWidth, center.y - halfDepth)
            lineTo(center.x - halfWidth, center.y + halfDepth)
            lineTo(center.x + halfWidth, center.y + halfDepth)
            lineTo(center.x + halfWidth, center.y - halfDepth)
        }, CoachColors.Periwinkle, style = Stroke(4.dp.toPx()))
        if (frontEntry) {
            val dashed = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()))
            for (side in listOf(-1f, 1f)) drawLine(CoachColors.Periwinkle.copy(alpha = .4f),
                Offset(center.x + side * halfWidth, center.y - halfDepth),
                Offset(center.x + side * halfWidth / 2, center.y - halfDepth), 4.dp.toPx(), pathEffect = dashed)
        }
    }
}

private fun DrawScope.pathCar(center: Offset, heading: Float, scale: Float, color: Color,
    reversing: Boolean = false, forward: Boolean = false) {
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
        if (reversing || forward) {
            val direction = if (forward) -1f else 1f
            val tip = center.y + (height / 2 + .6f * scale) * direction
            pathChevron(Offset(center.x, tip), width, direction)
        }
        drawPath(Path().apply {
            moveTo(left + width * .22f, top + height * .72f)
            quadraticTo(center.x, top + height * .76f, left + width * .78f, top + height * .72f)
            lineTo(left + width * .87f, top + height * .86f)
            quadraticTo(center.x, top + height * .91f, left + width * .13f, top + height * .86f); close()
        }, CoachColors.Paper)
    }
}

/** The same small Signal chevron marks the replay car, front arrival and initial travel direction. */
private fun DrawScope.pathChevron(tip: Offset, width: Float, direction: Float) {
    drawPath(Path().apply {
        moveTo(tip.x - width * .36f, tip.y - width * .16f * direction)
        lineTo(tip.x, tip.y)
        lineTo(tip.x + width * .36f, tip.y - width * .16f * direction)
    }, CoachColors.Signal, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}
