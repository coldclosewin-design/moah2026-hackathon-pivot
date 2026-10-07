package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
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
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(.57f).fillMaxHeight().padding(start = 180.dp, end = 100.dp, top = 64.dp, bottom = 100.dp)) {
                BrandMark()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Eyebrow("연습 준비", color = CoachColors.Periwinkle)
                    Spacer(Modifier.height(32.dp))
                    Headline(coachDisplayText(briefingHeadline(task.watch)))
                    Spacer(Modifier.height(40.dp))
                    LessonText(taskModeLine(task, mode), 40)
                    Spacer(Modifier.height(64.dp))
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(Modifier.width(4.dp).fillMaxHeight().background(CoachColors.Periwinkle))
                        Column(Modifier.padding(start = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            sentences.forEachIndexed { index, sentence ->
                                LessonText(sentence, 40, if (index == current) CoachColors.Ink else CoachColors.Muted,
                                    modifier = Modifier.testTag(if (index == current) "briefing-current" else "briefing-sentence"))
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextAction("건너뛰기 ›", onSkip, Modifier.heightIn(min = 96.dp), size = 32)
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(CoachColors.Lavender).testTag("briefing-progress")) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(CoachColors.Periwinkle))
                }
            }
            if (wheelReference) Image(painterResource(R.drawable.poster_wheel_b), null,
                Modifier.weight(.43f).fillMaxHeight(), contentScale = ContentScale.Crop)
            else Box(Modifier.weight(.43f).fillMaxHeight().background(CoachColors.Ink), contentAlignment = Alignment.Center) {
                SymbolTile(CoachSymbol.Voice, Modifier.size(420.dp))
            }
        }
    }
}
