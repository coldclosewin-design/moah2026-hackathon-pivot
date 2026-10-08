package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ParkingRingScreen(state: ManeuverDisplayState, stopped: Boolean, subtitle: String?,
    onFinish: () -> Unit, demo: (@Composable () -> Unit)?, taskTitle: String) {
    var hint by remember(state.hintText) { mutableStateOf(maneuverText(state.hintText)) }
    LaunchedEffect(state.hintText) { if (hint != null) { delay(4_000); hint = null } }
    val source = state.commonSignal()
    val ring = remember { Animatable(state.steeringDeg ?: 0f) }
    LaunchedEffect(state.steeringDeg) {
        if (state.steeringDeg == null) ring.snapTo(0f)
        else ring.animateTo(state.steeringDeg, spring(dampingRatio = .88f, stiffness = 110f))
    }
    val angle = ring.value
    val stretch by animateFloatAsState(1f + .8f * (abs(ring.velocity) / 900f).coerceIn(0f, 1f),
        spring(dampingRatio = .35f, stiffness = 420f), label = "ring-droplet")
    val ripple = remember { Animatable(1f) }
    var previousStep by remember(state.attempt) { mutableStateOf(state.guideStep) }
    LaunchedEffect(state.mode, state.guideStep) {
        val before = previousStep?.substringBefore('/')?.toIntOrNull()
        val after = state.guideStep?.substringBefore('/')?.toIntOrNull()
        previousStep = state.guideStep
        if (state.mode == LessonMode.GUIDE && before != null && after != null && after > before) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }
    }
    PosterSurface(band = demo) {
        Row(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CoachColors.Lavender, CoachColors.Platinum)))
            .padding(42.dp), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().background(CoachColors.Paper, RoundedCornerShape(48.dp))
                .testTag("parking-ring-card")) {
                BrandMark(Modifier.padding(60.dp))
                Box(Modifier.align(Alignment.Center).size(900.dp).testTag("steering-ring"), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension
                        val magnitude = abs(angle)
                        val direction = if (angle < 0f) -1 else 1
                        val stroke = 28.dp.toPx()
                        for ((radius, sweep) in listOf(.36f to magnitude.coerceAtMost(360f), .45f to (magnitude - 360).coerceIn(0f, 360f))) {
                            val p = Offset(r * (.5f - radius), r * (.5f - radius))
                            val bounds = Size(r * radius * 2, r * radius * 2)
                            drawArc(CoachColors.Lavender, -90f, 360f, false, p, bounds, style = Stroke(stroke))
                            if (state.steeringDeg != null) drawArc(CoachColors.Ink, -90f, sweep * direction, false, p, bounds,
                                style = Stroke(stroke, cap = StrokeCap.Butt))
                        }
                        if (state.steeringDeg != null) {
                            val radius = if (magnitude > 360f) .45f else .36f
                            val degrees = -90f + (if (magnitude > 360f) magnitude - 360f else magnitude) * direction
                            val rad = Math.toRadians(degrees.toDouble())
                            val head = Offset(r * .5f + r * radius * cos(rad).toFloat(), r * .5f + r * radius * sin(rad).toFloat())
                            val diameter = stroke * 1.4f
                            val length = diameter * stretch
                            // The leading edge stays at the ring endpoint; only the trailing edge stretches.
                            rotate(degrees + direction * 90f, head) {
                                drawOval(CoachColors.Ink, Offset(head.x + diameter / 2 - length, head.y - diameter / 2), Size(length, diameter))
                            }
                        }
                        if (ripple.value < 1f) drawCircle(CoachColors.Ink.copy(alpha = .6f * (1f - ripple.value)),
                            r * .45f * (1f + .075f * ripple.value), style = Stroke(4.dp.toPx()))
                        drawLine(CoachColors.Muted, Offset(r / 2, r * .9f), Offset(r / 2, r), 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 8.dp.toPx())))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Eyebrow("조향각", color = CoachColors.Muted)
                        LessonText(state.steeringDeg?.let {
                            steeringControlLabel(it)
                        } ?: "미측정", 84, bold = true)
                        steeringTurnsLabel(state.steeringDeg?.let { -it })?.let { LessonText(it, 36, CoachColors.Muted) }
                        if (source == null && state.steeringDeg != null) StateLabel(signalLabel(state.steeringSignal), state.steeringSignal)
                    }
                }
                Eyebrow("조향 방향 도식", Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), CoachColors.Muted)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(top = 28.dp, bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LessonText("$taskTitle · ${state.attempt}회차", 40, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        LessonText("${state.speed} km/h", 64, bold = true)
                        source?.let { StateLabel(collapsedSignalLabel(it), it) }
                    }
                }
                Row(Modifier.background(CoachColors.Paper, RoundedCornerShape(100)).padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    YellowModeIcon(Modifier.size(44.dp), bars = when(state.mode) { LessonMode.GUIDE -> 3; LessonMode.HINT -> 2; else -> 1 })
                    LessonText("${state.mode.label} 모드", 36)
                }
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    val guide = maneuverText(state.guideText)?.takeIf { it.isNotBlank() }
                    val spoken = maneuverText(subtitle)?.takeIf { it.isNotBlank() && it != state.hintText }
                    val main = guide ?: hint ?: spoken?.takeIf { it.count { c -> c == '\n' } < 3 }
                    if (main != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow("코치")
                            if (guide != null) state.guideStep?.let {
                                Eyebrow(it, Modifier.background(CoachColors.Ink, RoundedCornerShape(100))
                                    .padding(horizontal = 20.dp, vertical = 8.dp), CoachColors.Paper)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            Headline(coachDisplayText(main), size = 86, modifier = Modifier.weight(1f),
                                color = if (hint != null && guide == null) CoachColors.Signal else CoachColors.Ink)
                            if (guide != null && state.mode == LessonMode.GUIDE && state.guideStep != null)
                                ParkingStageTile(state.guideStep, state.entryGear == com.moah.hackathon.vehicle.Gear.DRIVE)
                        }
                    }
                    if (spoken != null && spoken != main) {
                        Spacer(Modifier.height(32.dp)); LessonText(coachDisplayText(spoken), 40, CoachColors.Muted)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(42.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.size(240.dp).background(CoachColors.Ink, RoundedCornerShape(100)),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Eyebrow("기어", color = CoachColors.Platinum)
                        LessonText(state.gear ?: "미측정", if (state.gear == null) 46 else 112, CoachColors.Paper)
                    }
                    if (state.rearDistanceApplies) Row(Modifier.weight(1f).height(240.dp)
                        .background(CoachColors.Paper, RoundedCornerShape(100)).padding(42.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow(if (state.proximityAlert()) "가까워요" else "뒤 거리", color = CoachColors.Muted)
                            LessonText(state.rearDistanceCm?.let { "${it.roundToInt()} cm" } ?: "미측정", 64,
                                if (state.proximityAlert()) CoachColors.Signal else CoachColors.Ink)
                        }
                        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.fillMaxWidth().height(28.dp).background(CoachColors.Lavender, RoundedCornerShape(100))) {
                                state.rearDistanceCm?.let { distance ->
                                    Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth((1f - distance / 150f).coerceIn(0f, 1f)).fillMaxHeight()
                                        .background(if (state.proximityAlert()) CoachColors.Signal else CoachColors.Ink, RoundedCornerShape(100)))
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LessonText("150", 24, CoachColors.Muted); LessonText("벽 0", 24, CoachColors.Muted)
                            }
                        }
                    }
                }
                if (source == null) Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    if (state.gear != null) StateLabel(signalLabel(state.gearSignal), state.gearSignal)
                    if (state.rearDistanceApplies && state.rearDistanceCm != null) StateLabel(signalLabel(state.distanceSignal), state.distanceSignal)
                }
                Spacer(Modifier.height(36.dp))
                if (stopped) FinishButton(state.askedDone, onFinish) else Spacer(Modifier.height(140.dp))
            }
        }
    }
}

/** B5 is the current guide's illustrative pose, never a vehicle position reading. */
@Composable
private fun ParkingStageTile(step: String, front: Boolean) {
    val index = step.substringBefore('/').toIntOrNull() ?: 1
    Column(Modifier.width(216.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.size(200.dp).background(CoachColors.Ink, RoundedCornerShape(48.dp))
            .semantics { contentDescription = "가이드 $step 단계 자세 · 위치 측정 아님" }) {
            val w = size.width
            drawPath(Path().apply { moveTo(w * .18f, w * .43f); lineTo(w * .18f, w * .87f); lineTo(w * .83f, w * .87f); lineTo(w * .83f, w * .43f) },
                CoachColors.Muted, style = Stroke(3.dp.toPx()))
            rotate((if (front) 180f else 0f) + when (index) { 1, 2 -> 90f; 3, 4 -> 35f; else -> 0f }) {
                withTransform({ translate(w * .39f, w * .16f); scale(w * .24f / 100f, w * .6f / 250f, Offset.Zero) }) {
                    vehicleSilhouette(CoachColors.Paper, CoachColors.Ink, CoachColors.Ink)
                }
            }
        }
        LessonText("위치 측정 아님", 27, CoachColors.Muted)
    }
}
