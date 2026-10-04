package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.LessonPhase
import com.moah.hackathon.scoring.MapShape
import com.moah.hackathon.scoring.Vec2
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.TrackSignal
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * **임시 화면(Claude, 10/4)** — 코스 과제의 [LessonPhase.Drive] 가 컴파일·시연되게 하는 최소 구현. 디자인·문구·배치는 Codex 라운드 18 에서 교체한다
 * (docs/handoffs/2026-10-04_codex_ui_round18.md). 규칙은 지킨다: 점수·감점 없음, 잠금(> 5 km/h) 중 터치 타깃 없음, 지도는 도식.
 */
@Composable
internal fun DriveInterimScreen(state: LessonPhase.Drive, subtitle: String?, onFinish: () -> Unit, demo: (@Composable () -> Unit)?) {
    val expansion = rememberSaveable { mutableStateOf(false) }
    val locked = state.locked
    LaunchedEffect(locked) { if (locked) expansion.value = false }
    val zones = state.course.zones
    val current = zones.firstOrNull { it.id == state.progress.currentZoneId }
    val next = zones.firstOrNull { it.id == state.progress.nextZoneId }
    PosterSurface {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(.53f).fillMaxHeight().background(CoachColors.Ink).padding(48.dp)) {
                    CourseMapCanvas(state, Modifier.fillMaxSize())
                }
                Column(Modifier.weight(.47f).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 96.dp, bottom = 52.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LessonText("${state.task.title} · ${state.attempt}회차", 40, modifier = Modifier.weight(1f))
                        LessonText("${state.snapshot.speedKmh.roundToInt()} km/h", 56, bold = true)
                    }
                    Eyebrow(listOfNotNull(current?.title?.let { "지금 $it" }, next?.title?.let { "다음 $it" }).joinToString(" · ").ifEmpty { "코스" })
                    state.snapshot.signal?.takeIf { it != TrackSignal.OFF }?.let { s ->
                        LessonText("신호 ${when (s) { TrackSignal.RED -> "빨간불"; TrackSignal.YELLOW -> "노란불"; TrackSignal.GREEN -> "초록불"; else -> "" }}", 48,
                            if (s == TrackSignal.RED) CoachColors.Signal else CoachColors.Ink, bold = true)
                    }
                    if (state.snapshot.emergency == true) LessonText("돌발 상황", 56, CoachColors.Signal, bold = true)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        (state.lastHint ?: state.zoneLine ?: subtitle)?.let { Headline(it, size = 64) }
                    }
                    if (!state.progress.positionMeasured) LessonText("시험장 위치 미측정", 32, CoachColors.Muted)
                    else LessonText("시험장 위치 · 시뮬레이션", 32, CoachColors.Muted)
                    if (!locked && state.snapshot.stopped) {
                        PrimaryPill(stringResource(R.string.lesson_finish), onFinish, Modifier.fillMaxWidth(), driver = true)
                    } else Spacer(Modifier.height(140.dp))
                }
            }
            if (!locked && demo != null) DemoRail(expansion, demo)
        }
    }
}

@Composable
private fun CourseMapCanvas(state: LessonPhase.Drive, modifier: Modifier) {
    val map = state.course.map
    val road = CoachColors.Paper.copy(alpha = .28f)
    Canvas(modifier) {
        val scale = min(size.width / map.widthM, size.height / map.heightM)
        val ox = (size.width - map.widthM * scale) / 2f
        val oy = (size.height - map.heightM * scale) / 2f
        fun p(v: Vec2) = Offset(ox + v.x * scale, oy + (map.heightM - v.y) * scale)
        for (s in map.shapes) when (s) {
            is MapShape.Road -> drawPolyline(s.points.map(::p), road, s.widthM * scale)
            is MapShape.Ring -> drawCircle(road, s.radiusM * scale, p(s.center), style = Stroke(s.widthM * scale))
            is MapShape.Bay -> {
                val c = p(s.center)
                drawRect(if (s.target) CoachColors.Periwinkle else CoachColors.Paper.copy(alpha = .5f),
                    Offset(c.x - s.widthM * scale / 2, c.y - s.lengthM * scale / 2), Size(s.widthM * scale, s.lengthM * scale), style = Stroke(3f))
            }
            is MapShape.StopLine -> drawLine(CoachColors.Paper, p(s.a), p(s.b), 6f)
            is MapShape.Crosswalk -> drawLine(CoachColors.Paper.copy(alpha = .6f), p(s.a), p(s.b), s.widthM * scale)
            is MapShape.Light -> drawCircle(if (state.snapshot.signal == TrackSignal.RED) CoachColors.Signal else CoachColors.Periwinkle, 12f, p(s.at))
            is MapShape.Ramp -> drawRect(CoachColors.Periwinkle.copy(alpha = .35f), p(Vec2(s.area.minX, s.area.maxY)), Size(s.area.width * scale, s.area.height * scale))
            is MapShape.Label -> {}
        }
        // 지금 구간 강조(점수 아님)
        state.course.zones.firstOrNull { it.id == state.progress.currentZoneId }?.let { z ->
            drawRect(CoachColors.Periwinkle.copy(alpha = .25f), p(Vec2(z.area.minX, z.area.maxY)), Size(z.area.width * scale, z.area.height * scale))
        }
        drawPolyline(state.course.route.map(::p), CoachColors.Paper.copy(alpha = .35f), 3f)
        state.progress.pose?.let { pose ->
            val c = p(pose.at)
            rotate(-pose.headingDeg, c) {
                val w = 1.9f * scale; val l = 4.6f * scale
                drawRoundRect(CoachColors.Paper, Offset(c.x - w / 2, c.y - l / 2), Size(w, l), androidx.compose.ui.geometry.CornerRadius(w / 4))
                drawRect(CoachColors.Ink, Offset(c.x - w * .35f, c.y - l * .32f), Size(w * .7f, l * .16f))
            }
        }
    }
}

private fun DrawScope.drawPolyline(points: List<Offset>, color: androidx.compose.ui.graphics.Color, width: Float) {
    if (points.size < 2) return
    val path = Path().apply { moveTo(points[0].x, points[0].y); points.drop(1).forEach { lineTo(it.x, it.y) } }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Butt, join = StrokeJoin.Round))
}
