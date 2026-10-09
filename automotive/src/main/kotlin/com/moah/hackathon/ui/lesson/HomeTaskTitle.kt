package com.moah.hackathon.ui.lesson

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.ports.withObjectParticle
import com.moah.hackathon.ui.CoachColors

internal fun nextHomeMode(task: Task, mode: LessonMode): LessonMode {
    val modes = LessonMode.entries.filter(task::supports)
    return modes.getOrNull((modes.indexOf(mode) + 1) % modes.size.coerceAtLeast(1)) ?: mode
}

@Composable
internal fun HomeTaskTitle(task: Task, mode: LessonMode, picked: Boolean, compact: Boolean,
    onTask: () -> Unit, onMode: (LessonMode) -> Unit, titleSize: Int? = null, singleLine: Boolean = false) {
    val taskWord = if (picked) task.title else "과제 고르기"
    val suffix = if (picked) task.title.withObjectParticle().removePrefix(task.title) else "부터"
    // Never measure just the current word: the following sentence must stay still.
    val longestMode = (LessonMode.entries.filter(task::supports).map { it.label } + "가이드").maxBy { it.length }
    if (!singleLine) {
        Eyebrow(if (picked) "오늘의 과제 · ${taskTypeLabel(task.type)}" else "처음 오셨네요", color = CoachColors.Periwinkle)
        Spacer(Modifier.height(16.dp))
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-task-title")) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val width = with(density) { maxWidth.toPx() }
        val parts = listOf("$taskWord ▼ $suffix", if (picked) "$longestMode ▸ 모드로 해 볼까요?" else "해 볼까요?")
        val lines = if (singleLine) listOf(parts.joinToString(" ")) else parts
        val size = listOf(titleSize ?: if (compact) 104 else 132, 112, 104, 88, 80, 72, 64, 56).distinct()
            .filter { it <= (titleSize ?: if (compact) 104 else 132) }.firstOrNull { candidate ->
                lines.all { measurer.measure(AnnotatedString(it), TextStyle(fontSize = candidate.sp, fontWeight = FontWeight.Medium), softWrap = false).size.width <= width }
            } ?: 56
        val wordWidth = with(density) { measurer.measure(AnnotatedString(longestMode),
            TextStyle(fontSize = size.sp, fontWeight = FontWeight.Medium), softWrap = false).size.width.toDp() }
        val taskTitle: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.homeShared("home-task").heightIn(min = 96.dp)
                    .clickable(role = Role.Button, onClick = onTask)
                    .semantics(mergeDescendants = true) { contentDescription = "과제·모드 바꾸기" }
                    .padding(end = 16.dp, bottom = 8.dp).titleUnderline(!picked),
                    verticalAlignment = Alignment.CenterVertically) {
                    LessonText(taskWord, size, bold = true)
                    HomeTitleArrow(size, right = false)
                }
                LessonText(suffix, size, bold = true)
            }
        }
        val modeTitle: @Composable () -> Unit = {
            if (picked) Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.homeShared("home-mode").heightIn(min = 96.dp)
                    .clickable(role = Role.Button) { onMode(nextHomeMode(task, mode)) }
                    .semantics(mergeDescendants = true) { contentDescription = "모드 바꾸기"; stateDescription = mode.label }
                    .testTag("home-mode-roller").padding(end = 16.dp, bottom = 8.dp).titleUnderline(),
                    verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(mode, Modifier.width(wordWidth).clipToBounds().clearAndSetSemantics { },
                        transitionSpec = {
                            slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } togetherWith
                                slideOutHorizontally(tween(360, easing = FastOutSlowInEasing)) { -it }
                        }, label = "home-mode-roll") { current ->
                        LessonText(current.label, size, bold = true, maxLines = 1, modifier = Modifier.width(wordWidth))
                    }
                    HomeTitleArrow(size, right = true)
                }
                LessonText("모드로 해 볼까요?", size, bold = true, modifier = Modifier.testTag("home-mode-tail"))
            } else LessonText("해 볼까요?", size, bold = true)
        }
        if (singleLine) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            taskTitle(); modeTitle()
        } else Column { taskTitle(); modeTitle() }
    }
}

/** Same glyph, size, colour and gap; only orientation differs between task and mode. */
@Composable
private fun HomeTitleArrow(size: Int, right: Boolean) {
    Spacer(Modifier.width((size * .18f).dp))
    LessonText("▼", (size * .55f).toInt(), CoachColors.Ink, bold = true,
        modifier = Modifier.rotate(if (right) -90f else 0f).clearAndSetSemantics { })
}

private fun Modifier.titleUnderline(dashed: Boolean = false) = drawBehind {
    drawLine(CoachColors.Periwinkle, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx(),
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null)
}
