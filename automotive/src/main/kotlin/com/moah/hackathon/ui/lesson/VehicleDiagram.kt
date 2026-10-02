package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.Gear

/** Entry direction controls orientation; the rear task keeps its rear-up convention. One measured angle drives the front wheels, direction arc and wheel guides. */
@Composable
internal fun VehicleDiagram(state: ManeuverDisplayState, modifier: Modifier) {
    val angle by animateFloatAsState(state.steeringDeg ?: 0f, tween(350, easing = FastOutSlowInEasing), label = "steering")
    Canvas(modifier.semantics { contentDescription = state.diagramDescription() }) {
        val carHeight = size.height * .66f
        val carWidth = carHeight * .43f
        val cx = size.width / 2
        val top = size.height * .15f
        clipRect {
            rotate(if (state.entryGear == Gear.DRIVE) 180f else 0f, Offset(cx, top + carHeight / 2)) {
                val geometry = SteeringGeometry(angle.takeIf { state.steeringDeg != null })
                fun position(x: Double, y: Double) = Offset(cx - carWidth / 2 + (x * carWidth / 100).toFloat(),
                    top + (y * carWidth / 100).toFloat())
                if (state.steeringDeg != null) clipRect {
                    fun track(x: Double, y: Double, distance: Double) = Path().apply {
                        repeat(121) { step ->
                            val point = geometry.point(x, y, distance * step / 120)
                            val at = position(point.x, point.y)
                            if (step == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
                        }
                    }
                    val dashed = Stroke(4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 12.dp.toPx())))
                    // All five paths and wheel tangents share the ICR on the rear axle.
                    listOf(geometry.frontLeftX, geometry.frontRightX).forEach { x ->
                        drawPath(track(x, geometry.frontY, 220.0), CoachColors.Periwinkle.copy(alpha = .4f), style = dashed)
                    }
                    drawPath(track(50.0, geometry.frontY, 220.0), CoachColors.Periwinkle, style = Stroke(6.dp.toPx()))
                    listOf(7.0, 93.0).forEach { x ->
                        drawPath(track(x, geometry.rearY, -100.0), CoachColors.Lavender.copy(alpha = .4f), style = dashed)
                    }
                }
                if (state.gear == "R" || (state.entryGear == Gear.DRIVE && state.gear == "D")) {
                    val forward = state.gear == "D"
                    val y = if (forward) top + carHeight + 38.dp.toPx() else top - 38.dp.toPx()
                    drawPath(Path().apply {
                        moveTo(cx - carWidth * .36f, y)
                        lineTo(cx, y + carWidth * .16f * if (forward) 1f else -1f)
                        lineTo(cx + carWidth * .36f, y)
                    }, CoachColors.Signal, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                withTransform({
                    translate(cx - carWidth / 2, top)
                    scale(carWidth / 100f, carHeight / 250f, Offset.Zero)
                }) {
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
                    }, CoachColors.Paper)
                    drawPath(Path().apply {
                        moveTo(19f, 3f); quadraticTo(50f, -1f, 81f, 3f)
                        quadraticTo(82f, 18f, 76f, 34f); lineTo(24f, 34f)
                        quadraticTo(18f, 18f, 19f, 3f); close()
                    }, CoachColors.Lavender)
                    drawPath(Path().apply {
                        moveTo(15f, 174f); quadraticTo(50f, 183f, 85f, 174f)
                        quadraticTo(85f, 212f, 72f, 244f)
                        quadraticTo(50f, 248f, 28f, 244f)
                        quadraticTo(15f, 212f, 15f, 174f); close()
                    }, CoachColors.Lavender)
                    drawPath(Path().apply {
                        moveTo(23f, 32f); quadraticTo(50f, 26f, 77f, 32f)
                        lineTo(74f, 49f); quadraticTo(50f, 52f, 26f, 49f); close()
                    }, CoachColors.Periwinkle)
                    drawPath(Path().apply {
                        moveTo(22f, 137f); quadraticTo(50f, 143f, 78f, 137f)
                        lineTo(86f, 170f); quadraticTo(50f, 190f, 14f, 170f); close()
                    }, CoachColors.Periwinkle)
                    listOf(false, true).forEach { right ->
                        withTransform({ if (right) { translate(100f, 0f); scale(-1f, 1f, Offset.Zero) } }) {
                            drawPath(Path().apply {
                                moveTo(17f, 52f); quadraticTo(24f, 90f, 19f, 126f)
                                lineTo(11f, 162f); lineTo(11f, 93f); close()
                            }, CoachColors.Periwinkle)
                            drawLine(CoachColors.Paper, Offset(10f, 103f), Offset(23f, 92f), 2f)
                            drawPath(Path().apply {
                                moveTo(7f, 159f); lineTo(-2f, 154f)
                                quadraticTo(-8f, 152f, -6f, 159f)
                                quadraticTo(-4f, 162f, 7f, 165f); close()
                            }, CoachColors.Paper)
                        }
                    }
                    listOf(7f, 93f).forEach { x ->
                        drawRoundRect(CoachColors.Periwinkle, Offset(x - 4f, 27f), Size(8f, 30f), CornerRadius(3f))
                    }
                }
                // Rotate in screen space so the body's unequal x/y scaling cannot skew wheel tangency.
                listOf(geometry.frontLeftX, geometry.frontRightX).forEach { x ->
                    val pivot = position(x, geometry.frontY)
                    val width = carWidth * .08f
                    val height = carHeight * (30f / 250f)
                    rotate(-geometry.frontAngle(x), pivot) {
                        drawRoundRect(CoachColors.Periwinkle, pivot - Offset(width / 2, height / 2),
                            Size(width, height), CornerRadius(carWidth * .03f))
                    }
                }
            }
        }
    }
}
