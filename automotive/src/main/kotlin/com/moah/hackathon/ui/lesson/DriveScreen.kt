package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.LessonPhase
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.TrackSignal
import kotlin.math.roundToInt

@Composable
internal fun DriveScreen(state: LessonPhase.Drive, subtitle: String?, onFinish: () -> Unit, demo: (@Composable () -> Unit)?) {
    val current = state.course.zone(state.progress.currentZoneId.orEmpty())
    val next = state.course.zone(state.progress.nextZoneId.orEmpty())
    PosterSurface(band = demo.takeUnless { state.locked }) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(.53f).fillMaxHeight().background(CoachColors.Ink).padding(48.dp)) {
                    Eyebrow("코스 · ${state.course.map.title}", color = CoachColors.Paper.copy(alpha = .6f))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        CourseMap(state.course, Modifier.fillMaxSize(), state.progress,
                            state.progress.pose.takeIf { state.progress.positionMeasured }, state.snapshot.signal)
                        if (state.snapshot.emergency == true) Box(Modifier.fillMaxWidth().background(CoachColors.Signal).padding(16.dp),
                            contentAlignment = Alignment.Center) { LessonText("돌발 상황", 40, CoachColors.Paper) }
                    }
                    if (!state.progress.positionMeasured) LessonText("시험장 위치 미측정", 32, CoachColors.Paper)
                    LessonText("시험장 위치·신호 · ${trackSource(state.availability)}", 32, CoachColors.Paper.copy(alpha = .6f))
                }
                Column(Modifier.weight(.47f).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 96.dp, bottom = 52.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            LessonText("${state.task.title} · ${state.attempt}회차", 40)
                            if (state.mode == LessonMode.EVALUATE && state.course.isExam)
                                Box(Modifier.background(CoachColors.Lavender).padding(horizontal = 20.dp, vertical = 8.dp)) { LessonText("모의시험", 32) }
                        }
                        LessonText("${state.snapshot.speedKmh.roundToInt()} km/h", 56)
                    }
                    Eyebrow("지금 ${current?.title ?: "구간 사이"} · 다음 ${next?.title ?: "마무리"}")
                    val signalText = when (state.snapshot.signal) {
                        TrackSignal.RED -> "신호 · 빨간불"
                        TrackSignal.YELLOW -> "신호 · 노란불"
                        TrackSignal.GREEN -> "신호 · 초록불"
                        TrackSignal.OFF -> null
                        null -> "신호등 미측정"
                    }
                    signalText?.let { LessonText(it, 40,
                        if (state.snapshot.signal == TrackSignal.RED) CoachColors.Signal else CoachColors.Muted) }
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Headline(state.lastHint ?: state.zoneLine ?: subtitle ?: "안내를 들으며 코스를 따라가요.", size = 64)
                    }
                    if (!state.locked && state.snapshot.stopped) FinishButton(state.askedDone, onFinish)
                    else Spacer(Modifier.height(140.dp))
                }
            }
        }
    }
}
