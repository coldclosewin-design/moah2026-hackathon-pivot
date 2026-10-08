package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun BriefingScreen(task: Task, mode: LessonMode, line: String, subtitle: String?,
    expectedMillis: Long = 6_000L, locked: Boolean, onSkip: () -> Unit = {}, wheelReference: Boolean = false) {
    val progress = remember(task.id, mode, line) { Animatable(0f) }
    LaunchedEffect(progress, expectedMillis) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(expectedMillis.coerceIn(1, Int.MAX_VALUE.toLong()).toInt(), easing = LinearEasing))
    }
    if (locked) {
        ResultLockedScreen(message = "정차하면 안내가 다시 보여요.")
        return
    }
    val sentences = remember(line) { briefingSentences(line) }
    val current = briefingSentenceIndex(sentences, progress.value)
    PosterSurface {
        Column(Modifier.fillMaxSize().padding(start = 120.dp, end = 120.dp, top = 64.dp, bottom = 48.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BrandMark()
                Column(Modifier.width(760.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Eyebrow("연습 준비")
                    LessonText("${taskTypeLabel(task.type)} › ${task.title}", 36)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        YellowModeIcon(Modifier.size(44.dp))
                        LessonText("${mode.label} 모드", 36)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Headline(coachDisplayText(briefingHeadline(task.watch)), Modifier.width(2180.dp), size = 150, weakTail = "볼게요.")
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(CoachColors.Platinum).testTag("briefing-progress")) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(CoachColors.Ink))
            }
            Spacer(Modifier.height(48.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                sentences.forEachIndexed { index, sentence ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LessonText((index + 1).toString().padStart(2, '0'), 32,
                            if (index == current) CoachColors.Ink else CoachColors.Muted)
                        LessonText(sentence, if (index == current) 48 else 40,
                            if (index == current) CoachColors.Ink else CoachColors.Muted,
                            modifier = Modifier.testTag(if (index == current) "briefing-current" else "briefing-sentence"))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                ArrowPill("건너뛰기 ›", "›", onSkip, Modifier.width(460.dp))
            }
        }
    }
}
