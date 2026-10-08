package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
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
    onAgain: () -> Unit, onEnd: () -> Unit, demo: (@Composable () -> Unit)? = null, locked: Boolean = false,
    onDemoStop: (() -> Unit)? = null) {
    if (locked) {
        ResultLockedScreen(onDemoStop)
        return
    }
    val showPath = remember(record.path) { hasEstimatedPath(record.path) }
    PosterSurface(band = demo) {
        Column(Modifier.fillMaxSize().background(CoachColors.Lavender).padding(start = 60.dp, end = 60.dp, top = 60.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                BrandMark()
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LessonText(taskModeLine(task, record.mode), 36)
                    LessonText("${attempt}회차", 32, CoachColors.Paper, modifier = Modifier.background(CoachColors.Ink,
                        androidx.compose.foundation.shape.RoundedCornerShape(100)).padding(horizontal = 24.dp, vertical = 8.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
            CoachLines(record.remark, Modifier.fillMaxWidth().heightIn(max = 290.dp).padding(horizontal = 24.dp), titleSize = 120, adviceSize = 56)
            Spacer(Modifier.height(44.dp))
            SheetCard(Modifier.fillMaxWidth().weight(1f), dark = true) {
                Row(Modifier.fillMaxSize().padding(52.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (task.course != null && record.course != null) {
                            CourseDonePanel(task.course, record.course, Modifier.fillMaxSize(), compact = true)
                        } else if (task.type == TaskType.PARKING && showPath) {
                            EstimatedPath(record, Modifier.fillMaxSize(), task.parkingSpec.entryGear,
                                targetHeading = task.parkingSpec.targetHeadingDeg, parallel = task.id == "parking-parallel")
                        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            SymbolTile(CoachSymbol.Check, Modifier.size(220.dp), animate = false)
                        }
                    }
                    Column(Modifier.weight(1.12f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Eyebrow("판정", color = CoachColors.Platinum)
                        if (task.type == TaskType.PARKING) VerdictPanel(record.verdict, Modifier.weight(1f), compact = true, stagger = true)
                        else Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            if (task.type == TaskType.CHECKLIST) checklistResults(record.score).forEach { result ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    LessonText(result.label, 40, CoachColors.Paper)
                                    LessonText(result.mark, 40, if (result.passed == false) CoachColors.Signal else CoachColors.Platinum)
                                }
                            } else record.course?.let { course ->
                                course.zones.forEach { zone ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        LessonText(zone.title, 36, CoachColors.Paper)
                                        LessonText(courseZoneStatus(course, zone), 36, CoachColors.Platinum)
                                    }
                                }
                            }
                        }
                        AvailabilitySummary(record.score.badge, 32, onInk = true)
                    }
                    Column(Modifier.weight(.9f).padding(top = 60.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        FillButton(stringResource(R.string.lesson_again), onAgain, Modifier.fillMaxWidth().height(140.dp), inverse = true)
                        ArrowPill(stringResource(R.string.lesson_end), "→", onEnd, Modifier.fillMaxWidth(), dark = true)
                    }
                }
            }
        }
    }
}
