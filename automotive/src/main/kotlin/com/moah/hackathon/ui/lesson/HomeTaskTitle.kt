package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.ports.withObjectParticle
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun HomeTaskTitle(task: Task, mode: LessonMode, picked: Boolean, compact: Boolean,
    onTask: () -> Unit, onMode: (LessonMode) -> Unit) {
    var popup by remember(task.id, picked) { mutableStateOf(false) }
    val taskWord = if (picked) task.title else "과제 고르기"
    val suffix = if (picked) task.title.withObjectParticle().removePrefix(task.title) else "부터"
    Eyebrow(if (picked) "오늘의 과제 · ${taskTypeLabel(task.type)}" else "처음 오셨네요", color = CoachColors.Periwinkle)
    Spacer(Modifier.height(16.dp))
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-task-title")) {
        val measurer = rememberTextMeasurer()
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val lines = listOf("$taskWord ▼ $suffix", if (picked) "${mode.label} ▼ 모드로 해 볼까요?" else "해 볼까요?")
        val size = listOf(if (compact) 64 else 72, 64, 56).distinct().firstOrNull { candidate ->
            lines.all { measurer.measure(AnnotatedString(it), TextStyle(fontSize = candidate.sp), softWrap = false).size.width <= width }
        } ?: 56
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeTitleLink("$taskWord ▼", "과제·모드 바꾸기", size, !picked, onTask)
                LessonText(suffix, size)
            }
            if (picked) Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    HomeTitleLink("${mode.label} ▼", "모드 바꾸기", size, false, { popup = true })
                    DropdownMenu(popup, { popup = false }, Modifier.width(340.dp).background(CoachColors.Paper)) {
                        LessonMode.entries.filter(task::supports).forEach { option ->
                            Box(Modifier.fillMaxWidth().heightIn(min = 96.dp)
                                .background(if (option == mode) CoachColors.Lavender else CoachColors.Paper)
                                .clickable(role = Role.RadioButton) { popup = false; onMode(option) }
                                .semantics { selected = option == mode }.padding(horizontal = 32.dp, vertical = 20.dp)) {
                                LessonText(option.label, 40, CoachColors.Periwinkle)
                            }
                        }
                    }
                }
                LessonText("모드로 해 볼까요?", size)
            } else LessonText("해 볼까요?", size)
        }
    }
}

@Composable
private fun HomeTitleLink(text: String, description: String, size: Int, dashed: Boolean, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 96.dp).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }.padding(end = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterStart) {
        val label = buildAnnotatedString {
            append(text.removeSuffix("▼"))
            withStyle(SpanStyle(fontSize = (size * .55f).sp)) { append("▼") }
        }
        LessonText(label, size, CoachColors.Periwinkle, modifier = Modifier.drawBehind {
            drawLine(CoachColors.Periwinkle, Offset(0f, this.size.height), Offset(this.size.width, this.size.height),
                2.dp.toPx(), pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null)
        })
    }
}
