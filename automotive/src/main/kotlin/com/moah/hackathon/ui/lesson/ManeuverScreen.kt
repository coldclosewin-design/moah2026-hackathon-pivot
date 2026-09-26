package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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
    onFinish: () -> Unit, demo: (@Composable () -> Unit)? = null, taskTitle: String = "후면 직각 주차") {
    var visibleHint by remember(state.hintText) { mutableStateOf(maneuverText(state.hintText)) }
    val hintAlpha = remember { Animatable(1f) }
    LaunchedEffect(state.hintText) {
        if (visibleHint != null) {
            hintAlpha.snapTo(0f)
            hintAlpha.animateTo(1f, tween(180))
            delay(3_620)
            hintAlpha.animateTo(0f, tween(200))
            visibleHint = null
        }
    }
    val expansion = rememberSaveable { mutableStateOf(true) }
    val commonSignal = state.commonSignal()
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(.53f).fillMaxHeight().background(CoachColors.Ink)
                .padding(start = 180.dp, end = 100.dp, top = 64.dp, bottom = 52.dp)) {
                BrandMark(color = CoachColors.Paper)
                if (locked) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Headline("운전에 집중해 주세요", color = CoachColors.Paper)
                        Spacer(Modifier.height(32.dp))
                        LessonText("속도를 낮추면 주차 도식이 다시 보여요.", 40, CoachColors.Paper.copy(alpha = .7f))
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        VehicleDiagram(state, Modifier.fillMaxSize())
                        if (state.gear == "R") Eyebrow("차량 뒤쪽 ↑", Modifier.align(Alignment.TopCenter)
                            .padding(top = 24.dp), CoachColors.Paper)
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow("조향각", color = CoachColors.Paper)
                        LessonText(state.steeringDeg?.let {
                            "${if (it > 0) "왼쪽" else if (it < 0) "오른쪽" else "중립"} ${abs(it).roundToInt()}°"
                        } ?: "미측정", 80, if (state.steeringDeg == null) CoachColors.Paper.copy(alpha = .6f)
                            else CoachColors.Paper, bold = true)
                        if (commonSignal == null) StateLabel(signalLabel(state.steeringSignal), state.steeringSignal, onInk = true)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Eyebrow("조향 방향 도식", color = CoachColors.Paper.copy(alpha = .7f))
            }
            Row(Modifier.weight(.47f).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 64.dp, bottom = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        LessonText(taskTitle, 40, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            LessonText("${state.speed} km/h", 56, bold = true)
                            commonSignal?.let { StateLabel(signalLabel(it), it) }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                        val guide = maneuverText(state.guideText)?.takeIf { it.isNotBlank() }
                        val spoken = maneuverText(subtitle)?.takeIf { it.isNotBlank() && it != state.hintText }
                        val main = guide ?: visibleHint ?: spoken
                        if (main != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Eyebrow("조수석")
                                if (guide != null) state.guideStep?.let { Eyebrow(it, color = CoachColors.Periwinkle) }
                            }
                            Spacer(Modifier.height(24.dp))
                            Headline(main, Modifier.graphicsLayer {
                                alpha = if (guide == null && visibleHint != null) hintAlpha.value else 1f
                            }, size = 72)
                            if (guide != null && spoken != null && spoken != guide) {
                                Spacer(Modifier.height(24.dp))
                                LessonText(spoken, 40)
                            }
                        }
                    }
                    if (!locked) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            SignalValue("기어", state.gear ?: "미측정", state.gearSignal, commonSignal == null,
                                Modifier.weight(.8f))
                            Box(Modifier.width(2.dp).height(140.dp).background(CoachColors.Lavender))
                            SignalValue(if (state.proximityAlert()) "가까워요" else "뒤 거리",
                                state.rearDistanceCm?.let { "${it.roundToInt()} cm" } ?: "미측정",
                                state.distanceSignal, commonSignal == null, Modifier.weight(1.4f))
                        }
                        Spacer(Modifier.height(40.dp))
                        if (stopped) {
                            Eyebrow("${state.attempt}회차 · 이동 ${state.movingSegments}회 · ${state.elapsedSeconds}초", color = CoachColors.Muted)
                            Spacer(Modifier.height(16.dp))
                            FinishButton(state.askedDone, onFinish)
                        }
                    }
                }
                // No panel, toggle, scroll or clickable node survives the original snapshot lock.
                if (!locked && demo != null) CompositionLocalProvider(LocalDemoExpansion provides expansion) {
                    Box(Modifier.width(if (expansion.value) 420.dp else 80.dp)) { demo() }
                }
            }
        }
    }
}

@Composable
private fun SignalValue(label: String, value: String, signal: SignalAvailability, showSource: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(label)
        LessonText(value, if (value == "미측정") 56 else 80,
            if (value == "미측정") CoachColors.Muted else CoachColors.Ink, bold = true)
        if (showSource) StateLabel(signalLabel(signal), signal)
    }
}

@Composable
private fun FinishButton(emphasized: Boolean, onFinish: () -> Unit) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(emphasized) {
        pulse.snapTo(1f)
        if (emphasized) repeat(2) {
            pulse.animateTo(1.025f, tween(450))
            pulse.animateTo(1f, tween(450))
        }
    }
    PrimaryPill(stringResource(R.string.lesson_finish), onFinish,
        Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value })
}

@Composable
private fun VehicleDiagram(state: ManeuverDisplayState, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = state.diagramDescription() }) {
        val cx = size.width / 2
        val top = size.height * .20f
        val carHeight = size.height * .73f
        val carWidth = carHeight * .43f
        val left = cx - carWidth / 2
        if (state.gear == "R") {
            val arrowY = size.height * .105f
            drawPath(Path().apply {
                moveTo(cx, arrowY); lineTo(cx + 36.dp.toPx(), arrowY + 36.dp.toPx())
                lineTo(cx + 13.dp.toPx(), arrowY + 36.dp.toPx()); lineTo(cx + 13.dp.toPx(), arrowY + 62.dp.toPx())
                lineTo(cx - 13.dp.toPx(), arrowY + 62.dp.toPx()); lineTo(cx - 13.dp.toPx(), arrowY + 36.dp.toPx())
                lineTo(cx - 36.dp.toPx(), arrowY + 36.dp.toPx()); close()
            }, CoachColors.Signal)
        }
        drawRoundRect(CoachColors.Paper, Offset(left, top), Size(carWidth, carHeight), CornerRadius(carWidth * .29f))
        // Rear glass is at the top, windshield and steerable front wheels at the bottom.
        drawPath(Path().apply {
            moveTo(left + carWidth * .13f, top + carHeight * .13f)
            quadraticTo(cx, top + carHeight * .06f, left + carWidth * .87f, top + carHeight * .13f)
            lineTo(left + carWidth * .80f, top + carHeight * .28f)
            quadraticTo(cx, top + carHeight * .24f, left + carWidth * .20f, top + carHeight * .28f)
            close()
        }, CoachColors.Periwinkle)
        drawPath(Path().apply {
            moveTo(left + carWidth * .20f, top + carHeight * .60f)
            quadraticTo(cx, top + carHeight * .64f, left + carWidth * .80f, top + carHeight * .60f)
            lineTo(left + carWidth * .90f, top + carHeight * .79f)
            quadraticTo(cx, top + carHeight * .88f, left + carWidth * .10f, top + carHeight * .79f)
            close()
        }, CoachColors.Periwinkle)
        listOf(left + carWidth * .05f, left + carWidth * .90f).forEach { x ->
            drawRoundRect(CoachColors.Periwinkle, Offset(x, top + carHeight * .32f),
                Size(carWidth * .05f, carHeight * .25f), CornerRadius(6.dp.toPx()))
        }
        listOf(left - 26.dp.toPx(), left + carWidth + 26.dp.toPx()).forEach { x ->
            listOf(.18f, .82f).forEach { fraction ->
                val pivot = Offset(x, top + carHeight * fraction)
                // The nose faces down. A right steering input draws the front wheels as /.
                rotate(if (fraction > .5f) -wheelRotation(state.steeringDeg) else 0f, pivot) {
                    drawRoundRect(CoachColors.Periwinkle, Offset(x - 17.dp.toPx(), pivot.y - 59.dp.toPx()),
                        Size(34.dp.toPx(), 118.dp.toPx()), CornerRadius(8.dp.toPx()))
                }
            }
        }
    }
}
