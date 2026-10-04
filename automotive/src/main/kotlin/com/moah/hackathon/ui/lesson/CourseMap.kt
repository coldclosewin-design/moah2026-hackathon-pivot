package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.TrackSignal
import kotlin.math.min

/** Stateless map: Drive supplies progress only; result overlays are supplied only by Done. */
@Composable
internal fun CourseMap(course: TrackCourse, modifier: Modifier, progress: CourseProgress? = null,
    pose: Pose? = null, signal: TrackSignal? = null, trail: List<TrackPoint> = emptyList(),
    markers: List<Vec2> = emptyList(), thumbnail: Boolean = false, ink: Color = CoachColors.Paper) {
    val measurer = rememberTextMeasurer()
    val labels = remember(course, thumbnail, ink, measurer) {
        if (thumbnail) emptyList() else course.map.shapes.filterIsInstance<MapShape.Label>().map {
            it to measurer.measure(AnnotatedString(it.text), TextStyle(fontSize = 26.sp, color = ink))
        }
    }
    Canvas(modifier.clipToBounds().semantics { contentDescription = "${course.title} 코스 도면" }) {
        val map = course.map
        val margin = if (thumbnail) 4.dp.toPx() else 28.dp.toPx()
        val scale = min((size.width - margin * 2) / map.widthM, (size.height - margin * 2) / map.heightM)
        val ox = (size.width - map.widthM * scale) / 2
        val oy = (size.height - map.heightM * scale) / 2
        fun p(v: Vec2) = Offset(ox + v.x * scale, oy + (map.heightM - v.y) * scale)
        fun area(a: Area, color: Color) = drawRect(color, p(Vec2(a.minX, a.maxY)), Size(a.width * scale, a.height * scale))
        val road = if (thumbnail) ink else ink.copy(alpha = .28f)
        val line = (if (thumbnail) 1.5f else 3f).dp.toPx()
        val dashed = PathEffect.dashPathEffect(floatArrayOf(2f * scale, 2f * scale))
        course.zones.forEach { zone ->
            when {
                zone.id == progress?.currentZoneId -> area(zone.area, CoachColors.Periwinkle.copy(alpha = .25f))
                zone.id in progress?.passedZoneIds.orEmpty() -> area(zone.area, CoachColors.Periwinkle.copy(alpha = .07f))
            }
        }
        map.shapes.forEach { shape -> when (shape) {
            is MapShape.Road -> {
                coursePolyline(shape.points.map(::p), road, shape.widthM * scale)
                if (!thumbnail && shape.lanes > 1) coursePolyline(shape.points.map(::p), ink.copy(alpha = .6f), line, dashed)
            }
            is MapShape.Ring -> drawCircle(road, shape.radiusM * scale, p(shape.center), style = Stroke(shape.widthM * scale))
            is MapShape.Bay -> {
                val c = p(shape.center)
                val w = shape.widthM * scale / 2; val h = shape.lengthM * scale / 2f
                rotate(-shape.headingDeg, c) {
                    drawPath(Path().apply {
                        moveTo(c.x - w, c.y + h); lineTo(c.x - w, c.y - h)
                        lineTo(c.x + w, c.y - h); lineTo(c.x + w, c.y + h)
                    }, if (shape.target && !thumbnail) CoachColors.Periwinkle else ink.copy(alpha = .6f), style = Stroke(line))
                }
            }
            is MapShape.StopLine -> drawLine(ink, p(shape.a), p(shape.b), line * 1.5f)
            is MapShape.Crosswalk -> if (!thumbnail) {
                val a = p(shape.a); val b = p(shape.b)
                val delta = b - a; val length = delta.getDistance()
                val normal = Offset(-delta.y, delta.x) / length * shape.widthM * scale / 2f
                val stripes = (length / (1.2f * scale)).toInt().coerceAtLeast(2)
                repeat(stripes) { i ->
                    val c = a + delta * ((i + .5f) / stripes)
                    drawLine(ink.copy(alpha = .7f), c - normal, c + normal, length / stripes * .5f)
                }
            }
            is MapShape.Ramp -> if (!thumbnail) {
                area(shape.area, CoachColors.Periwinkle.copy(alpha = .35f))
                val c = p(shape.area.center)
                rotate(-shape.upHeadingDeg, c) {
                    repeat(3) { i ->
                        val y = c.y + (i - 1) * 2 * scale
                        coursePolyline(listOf(Offset(c.x - scale, y + scale), Offset(c.x, y), Offset(c.x + scale, y + scale)), ink.copy(alpha = .6f), line)
                    }
                }
            }
            is MapShape.Light -> if (!thumbnail) {
                val c = p(shape.at)
                drawCircle(CoachColors.Ink, 15.dp.toPx(), c)
                drawCircle(when (signal) {
                    TrackSignal.RED, TrackSignal.YELLOW -> CoachColors.Signal
                    TrackSignal.GREEN -> CoachColors.Periwinkle
                    else -> CoachColors.Muted
                }, 10.dp.toPx(), c)
                drawCircle(ink.copy(alpha = .6f), 15.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            }
            is MapShape.Label -> Unit
        } }
        if (!thumbnail) {
            coursePolyline(course.route.map(::p), ink.copy(alpha = .22f), 2.dp.toPx(), dashed)
            coursePolyline(trail.map { p(Vec2(it.x, it.y)) }, CoachColors.Periwinkle, 5.dp.toPx())
            labels.forEach { (label, text) ->
                val at = p(label.at) - Offset(text.size.width / 2f, text.size.height / 2f)
                drawText(text, topLeft = Offset(at.x.coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f)),
                    at.y.coerceIn(0f, (size.height - text.size.height).coerceAtLeast(0f))))
            }
            pose?.let { car ->
                val c = p(car.at)
                val w = VehicleSilhouetteGeometry.WIDTH * scale
                val h = VehicleSilhouetteGeometry.LENGTH * scale
                rotate(180f - car.headingDeg, c) {
                    withTransform({ translate(c.x - w / 2, c.y - h / 2); scale(w / 100f, h / 250f, Offset.Zero) }) {
                        vehicleSilhouette(CoachColors.Paper, CoachColors.Periwinkle, CoachColors.Ink)
                    }
                }
            }
            markers.forEach { drawCircle(CoachColors.Signal, 7.dp.toPx(), p(it)) }
        }
    }
}

private fun DrawScope.coursePolyline(points: List<Offset>, color: Color, width: Float, effect: PathEffect? = null) {
    if (points.size < 2) return
    drawPath(Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))
}
