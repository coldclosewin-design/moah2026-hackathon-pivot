package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
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
    var sheet by rememberSaveable { mutableStateOf(false) }
    val expansion = rememberSaveable { mutableStateOf(false) }
    val task = tasks.firstOrNull { it.id == selectedTaskId } ?: suggestedTask
    val mode = supportedMode(task, LessonMode.valueOf(modeName))
    val start = { if (task.isReady) onBegin(task.id, mode) }
    PosterSurface {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(if (sheet) .30f else .53f).fillMaxHeight()) {
                    Image(painterResource(R.drawable.poster_car), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    BrandMark(Modifier.padding(start = 180.dp, top = 64.dp))
                }
                Row(Modifier.weight(if (sheet) .70f else .47f).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 96.dp, bottom = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (sheet) {
                            Eyebrow("연습할 과제")
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                tasks.chunked(3).forEach { row ->
                                    Row(Modifier.height(220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        row.forEach { item ->
                                            TaskChoice(item, item.id == task.id, Modifier.weight(1f).fillMaxHeight()) {
                                                selectedTaskId = item.id
                                                modeName = supportedMode(item, mode).name
                                            }
                                        }
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                                reservation?.let { ReservationInfo(it) }
                            }
                            // Keep mode selection and start reachable while the task/reservation list scrolls.
                            Eyebrow("모드")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LessonMode.entries.filter(task::supports).forEach { item ->
                                    SelectionChip(item.label, item == mode, { modeName = item.name })
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TextAction(stringResource(R.string.lesson_back), { sheet = false })
                                PrimaryPill(stringResource(R.string.lesson_start), start, driver = true)
                            }
                        } else {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                Eyebrow(profileLine(profile), color = CoachColors.Muted)
                                Spacer(Modifier.height(32.dp))
                                Headline(setupProposal(task.type), size = 72)
                                Spacer(Modifier.height(32.dp))
                                LessonText("${task.title} · ${mode.label} 모드", 40)
                                Spacer(Modifier.height(20.dp))
                                LessonText(selectionReason(task, mode, suggestedTask, suggestedMode, reason), 40, CoachColors.Muted)
                                Spacer(Modifier.height(48.dp))
                                PrimaryPill(stringResource(R.string.lesson_start), start, Modifier.fillMaxWidth(), driver = true)
                                Spacer(Modifier.height(16.dp))
                                TextAction(stringResource(R.string.lesson_change_task_mode), { sheet = true })
                            }
                            SpeechFooter(subtitle)
                        }
                    }
                }
            }
            if (demo != null) DemoRail(expansion, demo)
        }
    }
}

@Composable
private fun TaskChoice(task: Task, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val background = if (chosen && task.isReady) CoachColors.Periwinkle else CoachColors.Lavender
    val foreground = when { !task.isReady -> CoachColors.Muted; chosen -> CoachColors.Paper; else -> CoachColors.Ink }
    // Disabled tasks deliberately have no onClick semantics (even a disabled clickable action would leak).
    val action = if (task.isReady) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics { disabled() }
    val stripe = when (task.type) {
        TaskType.PARKING -> if (chosen && task.isReady) CoachColors.Paper else CoachColors.Periwinkle
        TaskType.DRIVING -> CoachColors.Ink
        TaskType.CHECKLIST -> CoachColors.Signal
        TaskType.KNOWLEDGE -> CoachColors.Periwinkle.copy(alpha = .4f)
    }
    Box(modifier.background(background).then(action).semantics { selected = chosen }) {
        Box(Modifier.width(12.dp).fillMaxHeight().background(stripe))
        Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 20.dp, top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            LessonText(task.title, 40, foreground, modifier = Modifier.padding(end = if (task.isReady) 0.dp else 116.dp), maxLines = 2)
            LessonText("${taskTypeLabel(task.type)} · ${task.difficulty.label}", 32, foreground, maxLines = 1)
        }
        if (!task.isReady) LessonText(task.status.label, 32, CoachColors.Muted, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
    }
}

@Composable
private fun ReservationInfo(card: ReservationCard) {
    PosterRule()
    Eyebrow("제휴 시험장 예시", color = CoachColors.Periwinkle)
    LessonText("${card.venue}\n${card.slot}\n${card.course}", 40)
    LessonText(if (card.note.contains("실제 예약 연계 없음")) card.note else "${card.note} · 실제 예약 연계 없음", 32, CoachColors.Muted)
}
