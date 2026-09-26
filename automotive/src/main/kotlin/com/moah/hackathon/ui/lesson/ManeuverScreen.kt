package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.SignalAvailability
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ManeuverScreen(state: ManeuverDisplayState, locked: Boolean, stopped: Boolean, subtitle: String?,
    onFinish: () -> Unit, demo: (@Composable () -> Unit)? = null) {
    var visibleHint by remember(state.hintText) { mutableStateOf(maneuverText(state.hintText)) }
    LaunchedEffect(state.hintText) { if (state.hintText != null) { delay(4_000); visibleHint = null } }
    // Gate the whole slot here so even an expanded Fake control panel cannot leak touch targets.
    LessonFrame(maneuverText(subtitle), if (locked) null else demo) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LessonText("${state.attempt}회차 · 이동 ${state.movingSegments}회 · ${state.elapsedSeconds}초", 40, bold = true)
            LessonText("${state.speed} km/h", 40, CoachColors.Muted)
        }
        if (locked) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                LessonText("운전에 집중해 주세요", 88, bold = true)
                Spacer(Modifier.height(36.dp))
                LessonText("속도를 낮추면 주차 도식이 다시 보여요.", 42, CoachColors.Muted)
            }
        } else {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    LessonText("차량 뒤쪽 ↑", 32, CoachColors.Muted)
                    VehicleDiagram(state, Modifier.weight(1f).fillMaxWidth())
                    LessonText("조향 방향을 보여 주는 도식이에요.", 32, CoachColors.Muted)
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    LessonCard(Modifier.fillMaxWidth()) {
                        SignalHeading("기어", state.gearSignal)
                        LessonText(state.gear ?: "—", 112, CoachColors.Accent, bold = true)
                    }
                    LessonCard(Modifier.fillMaxWidth()) {
                        SignalHeading("조향각", state.steeringSignal)
                        val angle = state.steeringDeg
                        LessonText(angle?.let { "${if (it > 0) "왼쪽" else if (it < 0) "오른쪽" else "중립"} ${abs(it).roundToInt()}°" } ?: "—", 48, bold = true)
                    }
                    LessonCard(Modifier.fillMaxWidth()) {
                        SignalHeading("뒤 거리", state.distanceSignal)
                        LessonText(state.rearDistanceCm?.let { "${it.roundToInt()} cm" } ?: "—", 56,
                            if (state.proximityAlert()) CoachColors.Warning else CoachColors.Foreground, bold = true)
                        if (state.proximityAlert()) LessonText("뒤가 가까워요", 36, CoachColors.Warning)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 96.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                maneuverText(state.guideText)?.let { LessonText("${state.guideStep.orEmpty()}  $it", 42, CoachColors.Accent) }
                visibleHint?.let { LessonText(it, 42, CoachColors.Simulated) }
                if (!locked && state.askedDone) LessonText("다 되셨나요?", 36, CoachColors.Accent)
            }
            if (!locked && stopped) FinishButton(state.askedDone, onFinish)
        }
    }
}

@Composable
private fun FinishButton(emphasized: Boolean, onFinish: () -> Unit) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(emphasized) {
        pulse.snapTo(1f)
        if (emphasized) repeat(2) {
            pulse.animateTo(1.04f, tween(450))
            pulse.animateTo(1f, tween(450))
        }
    }
    LessonButton(stringResource(R.string.lesson_finish), onFinish,
        Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }, primary = true)
}

@Composable
private fun SignalHeading(title: String, signal: SignalAvailability) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LessonText(title, 32, CoachColors.Muted)
        LessonText(signalLabel(signal), 32, when (signal) {
            SignalAvailability.LIVE -> CoachColors.Accent
            SignalAvailability.SIMULATED -> CoachColors.Simulated
            SignalAvailability.MISSING -> CoachColors.Muted
        })
    }
}

@Composable
private fun VehicleDiagram(state: ManeuverDisplayState, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = state.diagramDescription() }) {
        val cx = size.width / 2
        val top = size.height * .29f
        val carWidth = size.width * .34f
        val carHeight = size.height * .59f
        val left = cx - carWidth / 2
        val stroke = 4.dp.toPx()
        val rearColor = if (state.proximityAlert()) CoachColors.Warning else CoachColors.Accent
        // Rear sensor: full width represents 250 cm; unavailable values are an unfilled outline.
        val barWidth = size.width * .70f
        val barTop = size.height * .07f
        drawRoundRect(CoachColors.Outline, Offset(cx - barWidth / 2, barTop), Size(barWidth, 22.dp.toPx()), CornerRadius(11.dp.toPx()))
        if (state.rearDistanceCm != null) drawRoundRect(rearColor, Offset(cx - barWidth / 2, barTop),
            Size(barWidth * distanceFraction(state.rearDistanceCm), 22.dp.toPx()), CornerRadius(11.dp.toPx()))
        if (state.gear == "R") {
            val arrowBottom = top - 12.dp.toPx()
            val arrowTop = barTop + 45.dp.toPx()
            drawLine(CoachColors.Accent, Offset(cx, arrowBottom), Offset(cx, arrowTop), stroke)
            drawPath(Path().apply {
                moveTo(cx - 18.dp.toPx(), arrowTop + 20.dp.toPx()); lineTo(cx, arrowTop)
                lineTo(cx + 18.dp.toPx(), arrowTop + 20.dp.toPx())
            }, CoachColors.Accent, style = Stroke(stroke))
        }
        drawRoundRect(CoachColors.Panel, Offset(left, top), Size(carWidth, carHeight), CornerRadius(45.dp.toPx()))
        drawRoundRect(CoachColors.Accent, Offset(left, top), Size(carWidth, carHeight), CornerRadius(45.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(CoachColors.Outline, Offset(left + carWidth * .14f, top + carHeight * .20f),
            Size(carWidth * .72f, carHeight * .46f), CornerRadius(22.dp.toPx()))
        drawLine(CoachColors.Muted, Offset(left + carWidth * .17f, top + carHeight * .70f),
            Offset(left + carWidth * .83f, top + carHeight * .70f), stroke)
        listOf(left - 8.dp.toPx(), left + carWidth + 8.dp.toPx()).forEach { x ->
            listOf(.18f, .80f).forEach { fraction ->
                val pivot = Offset(x, top + carHeight * fraction)
                // Front faces downward; positive clockwise rotation therefore points the wheels left on screen.
                rotate(if (fraction > .5f) wheelRotation(state.steeringDeg) else 0f, pivot) {
                    drawRoundRect(CoachColors.Foreground, Offset(x - 14.dp.toPx(), pivot.y - 36.dp.toPx()),
                        Size(28.dp.toPx(), 72.dp.toPx()), CornerRadius(8.dp.toPx()))
                }
            }
        }
    }
}
