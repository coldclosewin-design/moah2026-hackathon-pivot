package com.moah.hackathon.ui.lesson

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
import androidx.compose.ui.graphics.drawscope.withTransform
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
    PosterSurface(band = demo) {
        Row(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CoachColors.Lavender, CoachColors.Platinum)))
            .padding(42.dp), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().background(CoachColors.Paper, RoundedCornerShape(48.dp))
                .testTag("parking-ring-card")) {
                BrandMark(Modifier.padding(60.dp))
                Box(Modifier.align(Alignment.TopEnd).padding(32.dp).size(272.dp, 216.dp)
                    .background(CoachColors.Lavender, RoundedCornerShape(32.dp))) {
                    Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                        drawPath(Path().apply {
                            moveTo(size.width * .2f, size.height * .15f)
                            lineTo(size.width * .2f, size.height * .9f)
                            lineTo(size.width * .8f, size.height * .9f)
                            lineTo(size.width * .8f, size.height * .15f)
                        }, CoachColors.Muted, style = Stroke(3.dp.toPx()))
                        withTransform({
                            translate(size.width * .36f, size.height * .18f)
                            scale(size.width * .28f / 100f, size.height * .6f / 250f, Offset.Zero)
                        }) { vehicleSilhouette(CoachColors.Ink, CoachColors.Lavender, CoachColors.Lavender) }
                    }
                }
                Box(Modifier.align(Alignment.Center).size(900.dp).testTag("steering-ring"), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension
                        val angle = abs(state.steeringDeg ?: 0f)
                        val direction = if ((state.steeringDeg ?: 0f) > 0) -1 else 1
                        for ((radius, sweep) in listOf(.36f to angle.coerceAtMost(360f), .45f to (angle - 360).coerceIn(0f,360f))) {
                            val p = Offset(r * (.5f - radius), r * (.5f - radius))
                            val bounds = Size(r * radius * 2, r * radius * 2)
                            drawArc(CoachColors.Lavender, -90f, 360f, false, p, bounds, style = Stroke(28.dp.toPx()))
                            if (state.steeringDeg != null) drawArc(CoachColors.Ink, -90f, sweep * direction, false, p, bounds,
                                style = Stroke(28.dp.toPx(), cap = StrokeCap.Round))
                        }
                        drawLine(CoachColors.Muted, Offset(r / 2, r * .9f), Offset(r / 2, r), 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 8.dp.toPx())))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Eyebrow("조향각", color = CoachColors.Muted)
                        LessonText(state.steeringDeg?.let {
                            "${if (it > 0) "왼쪽" else if (it < 0) "오른쪽" else "중립"} ${abs(it).roundToInt()}°"
                        } ?: "미측정", 84, bold = true)
                        steeringTurnsLabel(state.steeringDeg)?.let { LessonText(it, 36, CoachColors.Muted) }
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
                        Headline(coachDisplayText(main), size = 90,
                            color = if (hint != null && guide == null) CoachColors.Signal else CoachColors.Ink)
                    }
                    if (spoken != null && spoken != main) {
                        Spacer(Modifier.height(32.dp)); LessonText(coachDisplayText(spoken), 40, CoachColors.Muted)
                    }
                }
                Row(Modifier.fillMaxWidth().background(CoachColors.Paper, RoundedCornerShape(100)).padding(28.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow("기어", color = CoachColors.Muted)
                        LessonText(state.gear ?: "미측정", 76)
                        if (source == null && state.gear != null) StateLabel(signalLabel(state.gearSignal), state.gearSignal)
                    }
                    if (state.rearDistanceApplies) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow(if (state.proximityAlert()) "가까워요" else "뒤 거리", color = CoachColors.Muted)
                        LessonText(state.rearDistanceCm?.let { "${it.roundToInt()} cm" } ?: "미측정", 76,
                            if (state.proximityAlert()) CoachColors.Signal else CoachColors.Ink)
                        if (source == null && state.rearDistanceCm != null) StateLabel(signalLabel(state.distanceSignal), state.distanceSignal)
                    }
                }
                Spacer(Modifier.height(36.dp))
                if (stopped) FinishButton(state.askedDone, onFinish) else Spacer(Modifier.height(140.dp))
            }
        }
    }
}
