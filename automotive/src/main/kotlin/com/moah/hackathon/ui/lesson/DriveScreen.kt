package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.LessonPhase
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.TrackSignal
import kotlin.math.roundToInt

/** O1: the map fits its own left-hand area; the static right sheet never covers the route. */
@Composable
internal fun DriveScreen(state: LessonPhase.Drive, subtitle: String?, onFinish: () -> Unit, demo: (@Composable () -> Unit)?) {
    val current = state.course.zone(state.progress.currentZoneId.orEmpty())
    val next = state.course.zone(state.progress.nextZoneId.orEmpty())
    val emergency = state.snapshot.emergency == true
    val foreground = if (emergency) CoachColors.Paper else CoachColors.Ink
    val secondary = if (emergency) CoachColors.Paper else CoachColors.Muted
    val signalText = when (state.snapshot.signal) {
        TrackSignal.RED -> "신호 · 빨간불"
        TrackSignal.YELLOW -> "신호 · 노란불"
        TrackSignal.GREEN -> "신호 · 초록불"
        TrackSignal.OFF -> null
        null -> "신호등 미측정"
    }
    val coach = coachDisplayText(if (emergency) "돌발 상황이에요.\n멈추세요."
        else state.lastHint ?: state.zoneLine ?: subtitle ?: "안내를 들으며 코스를 따라가요.").trim()
    // Locked screens never invoke the demo slot or create a finish/scroll action.
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(CoachColors.Lavender, CoachColors.Platinum)))
            .testTag("drive-map-screen").padding(72.dp), horizontalArrangement = Arrangement.spacedBy(52.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                BrandMark()
                Spacer(Modifier.height(48.dp))
                LessonText("${state.task.title} · ${state.attempt}회차", 36, CoachColors.Muted)
                CourseMap(state.course, Modifier.weight(1f).fillMaxWidth().testTag("drive-map"),
                    state.progress, state.progress.pose.takeIf { state.progress.positionMeasured }, state.snapshot.signal,
                    ink = CoachColors.Ink, lightRoad = true, labelSize = 32,
                    missingSignal = state.snapshot.signal == null)
                if (!state.progress.positionMeasured) LessonText("시험장 위치 미측정", 36, CoachColors.Muted)
                if (state.locked && LocalDemoEscape.current != null)
                    LessonText("시연 · 막히면 워드마크를 길게", 32, CoachColors.Muted)
            }
            SheetCard(Modifier.width(680.dp).fillMaxHeight().testTag("drive-information-card"),
                stacked = false, animate = false, color = if (emergency) CoachColors.Signal else CoachColors.Paper) {
                Column(Modifier.fillMaxSize().padding(52.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        LessonText(buildAnnotatedString {
                            append(state.snapshot.speedKmh.roundToInt().toString())
                            withStyle(SpanStyle(fontSize = 32.sp)) { append(" km/h") }
                        }, 72, foreground)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            YellowModeIcon(bars = when (state.mode) { LessonMode.GUIDE -> 3; LessonMode.HINT -> 2; else -> 1 })
                            LessonText(state.mode.label, 32, secondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LessonText("지금 · ${current?.title ?: "구간 사이"}", 34, CoachColors.Paper,
                        modifier = Modifier.background(if (emergency) CoachColors.Paper.copy(alpha = .18f) else CoachColors.Ink,
                            RoundedCornerShape(100)).padding(horizontal = 24.dp, vertical = 12.dp))
                    LessonText("다음 · ${next?.title ?: "마무리"}", 34, secondary,
                        modifier = Modifier.background(if (emergency) CoachColors.Paper.copy(alpha = .12f) else CoachColors.Lavender,
                            RoundedCornerShape(100)).padding(horizontal = 24.dp, vertical = 12.dp))
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Headline(coach.substringBefore('\n'), size = 72, color = foreground)
                        val advice = coach.substringAfter('\n', "")
                        if (advice.isNotEmpty()) LessonText(advice, 48, secondary)
                        signalText?.let { LessonText(it, 32,
                            if (!emergency && state.snapshot.signal == TrackSignal.RED) CoachColors.Signal else secondary) }
                    }
                    if (!state.locked && state.snapshot.stopped) FinishButton(state.askedDone, onFinish)
                    else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SymbolTile(CoachSymbol.Lock, Modifier.size(40.dp), animate = false)
                        LessonText("운전에 집중해 주세요", 32, secondary)
                    }
                }
            }
        }
        if (!state.locked) demo?.invoke()
    }
}
