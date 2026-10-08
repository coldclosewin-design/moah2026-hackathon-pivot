package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.LessonPhase
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.TrackSignal
import kotlin.math.roundToInt

/** B4: the moving map and its floating card are information only. */
@Composable
internal fun DriveScreen(state: LessonPhase.Drive, subtitle: String?, onFinish: () -> Unit, demo: (@Composable () -> Unit)?) {
    val current = state.course.zone(state.progress.currentZoneId.orEmpty())
    val next = state.course.zone(state.progress.nextZoneId.orEmpty())
    val emergency = state.snapshot.emergency == true
    val signalText = when (state.snapshot.signal) {
        TrackSignal.RED -> "신호 · 빨간불"
        TrackSignal.YELLOW -> "신호 · 노란불"
        TrackSignal.GREEN -> "신호 · 초록불"
        TrackSignal.OFF -> null
        null -> "신호등 미측정"
    }
    // Do not invoke the demo slot, FinishButton, a scroll container or decorative animation when locked.
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().background(CoachColors.Lavender).testTag("drive-map-screen")) {
            CourseMap(state.course, Modifier.fillMaxSize().padding(horizontal = 160.dp, vertical = 70.dp).testTag("drive-map"),
                state.progress, state.progress.pose.takeIf { state.progress.positionMeasured }, state.snapshot.signal,
                ink = CoachColors.Ink, showLabels = !state.locked, lightRoad = true)
            Column(Modifier.align(Alignment.TopStart).padding(84.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                BrandMark()
                LessonText("${state.task.title} · ${state.attempt}회차", 36, CoachColors.Muted)
            }
            Row(Modifier.align(Alignment.TopEnd).padding(84.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                signalText?.let { LessonText(it, 40, if (state.snapshot.signal == TrackSignal.RED) CoachColors.Signal else CoachColors.Ink) }
                LessonText("${state.snapshot.speedKmh.roundToInt()} km/h", 64,
                    modifier = Modifier.background(CoachColors.Paper, RoundedCornerShape(100)).padding(horizontal = 32.dp, vertical = 10.dp))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(84.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!state.progress.positionMeasured) LessonText("시험장 위치 미측정", 36, CoachColors.Muted)
                if (state.locked && LocalDemoEscape.current != null) LessonText("시연 · 막히면 워드마크를 길게", 28, CoachColors.Muted.copy(alpha = .6f))
            }
            Column(Modifier.align(Alignment.BottomEnd).padding(80.dp).width(1060.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                if (state.locked) LessonText("운전에 집중해 주세요", 32, CoachColors.Muted, modifier = Modifier.align(Alignment.End))
                // Static while moving: the only changes are actual incoming route/signal information.
                Column(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(64.dp)).background(if (emergency) CoachColors.Signal else CoachColors.Paper, RoundedCornerShape(64.dp))
                    .padding(52.dp).testTag("drive-information-card"), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        LessonText("지금 ${current?.title ?: "구간 사이"} · 다음 ${next?.title ?: "마무리"}", 34,
                            if (emergency) CoachColors.Paper else CoachColors.Ink, modifier = Modifier.weight(1f))
                        YellowModeIcon()
                        LessonText(state.mode.label, 32, if (emergency) CoachColors.Paper else CoachColors.Muted)
                    }
                    Headline(coachDisplayText(if (emergency) "돌발 상황이에요.\n멈추세요." else state.lastHint ?: state.zoneLine ?: subtitle ?: "안내를 들으며 코스를 따라가요."),
                        size = 68, color = if (emergency) CoachColors.Paper else if (state.snapshot.signal == TrackSignal.RED) CoachColors.Signal else CoachColors.Ink)
                    if (!state.locked && state.snapshot.stopped) FinishButton(state.askedDone, onFinish)
                }
            }
        }
        if (!state.locked) demo?.invoke()
    }
}
