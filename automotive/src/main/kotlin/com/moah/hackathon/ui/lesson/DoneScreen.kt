package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

@Composable
internal fun DoneScreen(task: Task, attempt: Int, record: AttemptRecord, subtitle: String?,
    onAgain: () -> Unit, onEnd: () -> Unit, demo: (@Composable () -> Unit)? = null, locked: Boolean = false) {
    if (locked) {
        ResultLockedScreen()
        return
    }
    val showPath = remember(record.path) { hasEstimatedPath(record.path) }
    PosterSurface(band = demo) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                if (task.course != null && record.course != null) {
                    CourseDonePanel(task.course, record.course, Modifier.fillMaxHeight().fillMaxWidth(.38f))
                } else if (task.type == TaskType.CHECKLIST) {
                    Column(Modifier.fillMaxHeight().fillMaxWidth(.38f).surfaceTexture(CoachColors.Ink, CoachTexture.Panel)
                        .padding(start = 120.dp, end = 72.dp, top = 96.dp, bottom = 52.dp)) {
                        Eyebrow("출발 전 점검", color = CoachColors.Paper)
                        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)) {
                            checklistResults(record.score).forEach { result ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    LessonText(result.label, 40, CoachColors.Paper, modifier = Modifier.weight(1f))
                                    LessonText(result.mark, 40, when (result.passed) {
                                        true -> CoachColors.Periwinkle
                                        false -> CoachColors.Signal
                                        // Muted uses translucent Paper on the Ink panel, as in Maneuver.
                                        null -> CoachColors.Paper.copy(alpha = .6f)
                                    })
                                }
                            }
                        }
                    }
                } else if (task.type == TaskType.PARKING) {
                    Column(Modifier.fillMaxHeight().fillMaxWidth(.5f).background(CoachColors.Ink)) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            if (showPath) EstimatedPath(record, Modifier.fillMaxSize(), task.parkingSpec.entryGear,
                                targetHeading = task.parkingSpec.targetHeadingDeg, parallel = task.id == "parking-parallel")
                        }
                        VerdictPanel(record.verdict, Modifier.fillMaxWidth(), grid = true)
                    }
                } else if (showPath) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(.38f)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(.06f / .38f).background(CoachColors.Ink))
                        EstimatedPath(record, Modifier.fillMaxSize().padding(start = (2560 * .06f).dp), task.parkingSpec.entryGear)
                    }
                } else Box(Modifier.fillMaxHeight().fillMaxWidth(.12f).background(CoachColors.Ink))
                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 120.dp, end = 100.dp, top = 96.dp, bottom = 52.dp),
                    verticalArrangement = Arrangement.spacedBy(48.dp)) {
                    BrandMark()
                    Eyebrow("${task.title} · ${attempt}회차")
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        ResultHeadline(record.remark)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                        PrimaryPill(stringResource(R.string.lesson_again), onAgain, Modifier.weight(1f, fill = false))
                        TextAction(stringResource(R.string.lesson_end), onEnd)
                    }
                    // The remark is already the Done speech. Never show the previous phase's subtitle:
                    // removing that footer after TTS starts used to move the action row vertically.
                }
            }
        }
    }
}
