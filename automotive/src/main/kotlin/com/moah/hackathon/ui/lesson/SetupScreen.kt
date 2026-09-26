package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun SetupScreen(profile: Profile, tasks: List<Task>, suggestedTask: Task, suggestedMode: LessonMode,
    reason: String, reservation: ReservationCard?, subtitle: String?, onBegin: (String, LessonMode) -> Unit,
    demo: (@Composable () -> Unit)? = null) {
    var selectedTaskId by rememberSaveable(suggestedTask.id) { mutableStateOf(suggestedTask.id) }
    var modeName by rememberSaveable(suggestedMode) { mutableStateOf(suggestedMode.name) }
    val task = tasks.firstOrNull { it.id == selectedTaskId } ?: suggestedTask
    val mode = LessonMode.valueOf(modeName)
    val aside: (@Composable () -> Unit)? = if (demo == null) null else {
        {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                demo()
                reservation?.let { ReservationInfo(it, compact = true) }
            }
        }
    }
    LessonFrame(subtitle, aside) {
        LessonText("오늘은 뭘 해볼까요?", 60, bold = true)
        LessonText(profileLine(profile), 32, CoachColors.Muted)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            LessonCard(Modifier.fillMaxWidth(), padding = 20, spacing = 8) {
                LessonText("오늘의 제안", 32, CoachColors.Accent)
                LessonText("${task.title} · ${mode.label} 모드", 46, bold = true)
                LessonText(selectionReason(task, mode, suggestedTask, suggestedMode, reason), 36)
            }
            LessonText("연습할 과제", 32, CoachColors.Muted)
            tasks.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { item ->
                        val chosen = item.id == task.id
                        Surface(onClick = { selectedTaskId = item.id },
                            modifier = Modifier.weight(1f).semantics { selected = chosen },
                            color = if (chosen) CoachColors.Accent else CoachColors.Panel,
                            shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, CoachColors.Outline)) {
                            Column(Modifier.padding(14.dp)) {
                                LessonText(item.title, 34, if (chosen) CoachColors.Ink else CoachColors.Foreground, bold = true)
                                LessonText("${taskTypeLabel(item.type)} · 난이도 ${item.difficulty.label}", 32,
                                    if (chosen) CoachColors.Ink else CoachColors.Muted)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (demo == null) reservation?.let { ReservationInfo(it, compact = false) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LessonMode.entries.forEach { item ->
                LessonButton(item.label, { modeName = item.name }, Modifier.semantics { selected = item == mode }, primary = item == mode)
            }
            Spacer(Modifier.weight(1f))
            LessonButton(stringResource(R.string.lesson_start), { onBegin(task.id, mode) }, primary = true)
        }
    }
}

@Composable
private fun ReservationInfo(card: ReservationCard, compact: Boolean) {
    LessonCard(Modifier.fillMaxWidth(), padding = 20, spacing = 8) {
        LessonText(card.venue, 32, CoachColors.Accent, bold = true)
        LessonText(if (compact) "${card.slot}\n${card.course}" else "${card.slot} · ${card.course}", 32)
        LessonText(if (card.note.contains("실제 예약 연계 없음")) card.note else "${card.note} · 실제 예약 연계 없음", 32, CoachColors.Muted)
    }
}
