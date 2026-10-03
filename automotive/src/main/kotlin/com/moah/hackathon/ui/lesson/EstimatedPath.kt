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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
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
            val harshPoints = record.score.metrics.harshEvents.mapNotNull { nearestPathPoint(record.path, it.tMillis) }
            val viewport = pathViewport(record.path, size.width / density, size.height / density,
                frontEntry = entryGear == Gear.DRIVE, harshPoints = harshPoints)
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
                val first = record.path.first()
                rotate(-first.headingDeg, position(first)) {
                    pathSilhouette(position(first), viewport.scale.dp.toPx(),
                        CoachColors.Lavender, CoachColors.Paper, CoachColors.Paper)
                }
                val settled = elapsed >= record.path.last().tMillis
                if (entryGear == Gear.DRIVE && settled) initialTravelHeading(record.path)?.let { heading ->
                    val start = position(record.path.first())
                    rotate(-heading, start) {
                        pathChevron(start - Offset(0f, .8f * viewport.scale.dp.toPx()), 1.8f * viewport.scale.dp.toPx(), -1f)
                    }
                }
                val current = revealed.last()
                pathCar(position(current), current.headingDeg, viewport.scale.dp.toPx(),
                    reversing = time.isRunning && elapsed < record.path.last().tMillis && current.reversing,
                    forward = entryGear == Gear.DRIVE && (settled || (time.isRunning && !current.reversing)))
                record.score.metrics.harshEvents.filter { it.tMillis <= elapsed }.forEach { event ->
                    nearestPathPoint(record.path, event.tMillis)?.let { drawCircle(CoachColors.Signal, 10.dp.toPx(), position(it)) }
                }
            }
        }
        Spacer(Modifier.height(64.dp))
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

private fun DrawScope.pathSilhouette(center: Offset, scale: Float, body: Color, panel: Color, glass: Color) {
    val width = VehicleSilhouetteGeometry.WIDTH * scale
    val height = VehicleSilhouetteGeometry.LENGTH * scale
    // Shared source is rear-up; a heading-zero PathPoint faces screen-up.
    rotate(180f, center) {
        withTransform({
            translate(center.x - width / 2, center.y - height / 2)
            scale(width / 100f, height / 250f, Offset.Zero)
        }) { vehicleSilhouette(body, panel, glass) }
    }
}

private fun DrawScope.pathCar(center: Offset, heading: Float, scale: Float,
    reversing: Boolean = false, forward: Boolean = false) {
    val height = VehicleSilhouetteGeometry.LENGTH * scale
    // Compose rotates clockwise; PathPoint heading is counterclockwise from screen-up.
    rotate(-heading, center) {
        pathSilhouette(center, scale, CoachColors.Ink, CoachColors.Periwinkle, CoachColors.Lavender)
        if (reversing || forward) {
            val direction = if (forward) -1f else 1f
            val tip = center.y + (height / 2 + .6f * scale) * direction
            pathChevron(Offset(center.x, tip), 1.8f * scale, direction)
        }
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
