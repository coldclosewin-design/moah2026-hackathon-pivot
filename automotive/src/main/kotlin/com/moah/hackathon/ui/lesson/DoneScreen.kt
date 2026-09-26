package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun DoneScreen(task: Task, attempt: Int, record: AttemptRecord, subtitle: String?,
    onAgain: () -> Unit, onEnd: () -> Unit, demo: (@Composable () -> Unit)? = null) {
    val expansion = rememberSaveable { mutableStateOf(true) }
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(.12f).background(CoachColors.Ink))
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 120.dp, end = 100.dp, top = 64.dp, bottom = 52.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)) {
                BrandMark()
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                        Eyebrow("${task.title} · ${attempt}회차")
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Headline(record.remark, size = 72)
                            LessonText(processLine(task.type, record.score.metrics), 40, CoachColors.Muted)
                            taskDeltaLines(task.type, record.delta).forEach { change ->
                                LessonText(change.text, 40, if (change.improved) CoachColors.Periwinkle else CoachColors.Muted)
                            }
                            if (task.type == TaskType.PARKING) LessonText(
                                "칸 안의 위치는 확인할 수 없어요. 오늘은 주차 과정을 돌아봤어요.", 32, CoachColors.Muted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                            PrimaryPill(stringResource(R.string.lesson_again), onAgain)
                            TextAction(stringResource(R.string.lesson_end), onEnd)
                        }
                    }
                    if (demo != null) DemoRail(expansion, demo)
                }
                SpeechFooter(subtitle)
            }
        }
    }
}
