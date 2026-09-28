package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun SetupScreen(profile: Profile, tasks: List<Task>, suggestedTask: Task, suggestedMode: LessonMode,
    reason: String, reservation: ReservationCard?, subtitle: String?, onBegin: (String, LessonMode) -> Unit,
    demo: (@Composable () -> Unit)? = null, cheer: String? = null) {
    var selectedTaskId by rememberSaveable(suggestedTask.id) { mutableStateOf(suggestedTask.id) }
    var categoryName by rememberSaveable(suggestedTask.id) { mutableStateOf(suggestedTask.type.name) }
    var modeName by rememberSaveable(suggestedMode) { mutableStateOf(suggestedMode.name) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    val expansion = rememberSaveable { mutableStateOf(false) }
    val task = tasks.firstOrNull { it.id == selectedTaskId && it.isReady } ?: suggestedTask
    // Browsing a planned category clears its selection but retains the last ready choice on return.
    val selectedTask = task.takeIf { it.isReady && it.type.name == categoryName }
    val mode = supportedMode(task, LessonMode.valueOf(modeName))
    val start = { if (task.isReady && (!sheet || selectedTask != null)) onBegin(task.id, mode) }
    PosterSurface {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(if (sheet) .30f else .53f).fillMaxHeight()) {
                    Image(painterResource(R.drawable.poster_car), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    BrandMark(Modifier.padding(start = 180.dp, top = 64.dp))
                }
                Row(Modifier.weight(if (sheet) .70f else .47f).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = if (sheet) 64.dp else 96.dp, bottom = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (sheet) {
                            TaskSheet(tasks, TaskType.valueOf(categoryName), selectedTask, mode, reservation,
                                onCategory = { category ->
                                    if (categoryName != category.name) {
                                        categoryName = category.name
                                        val firstReady = tasks.firstOrNull { it.type == category && it.isReady }
                                        firstReady?.let {
                                            selectedTaskId = it.id
                                            modeName = supportedMode(it, mode).name
                                        }
                                    }
                                },
                                onTask = { item ->
                                    selectedTaskId = item.id
                                    modeName = supportedMode(item, mode).name
                                },
                                onMode = { modeName = it.name },
                                onBack = { sheet = false }, onStart = start)
                        } else {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                if (cheer != null) {
                                    Eyebrow("동승자 · $cheer", color = CoachColors.Periwinkle)
                                    Spacer(Modifier.height(16.dp))
                                }
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
                                TextAction(stringResource(R.string.lesson_change_task_mode), {
                                    categoryName = task.type.name
                                    sheet = true
                                })
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
