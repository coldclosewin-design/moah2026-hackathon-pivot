package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun DoneScreen(taskTitle: String, attempt: Int, record: AttemptRecord, subtitle: String?,
    onAgain: () -> Unit, onEnd: () -> Unit, demo: (@Composable () -> Unit)? = null) {
    LessonFrame(subtitle, demo) {
        LessonText("$taskTitle · ${attempt}회차 완료", 36, CoachColors.Accent)
        BoxWithConstraints(Modifier.weight(1f)) {
            val availableHeight = maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = availableHeight),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)) {
                LessonText(record.remark, 92, bold = true)
                LessonCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        metricLines(record.score.metrics).forEach { metric ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                LessonText(metric.label, 32, CoachColors.Muted)
                                LessonText(metric.value, 48, bold = true)
                            }
                        }
                    }
                }
                deltaLines(record.delta).forEach { line ->
                    LessonText(line.text, 36, if (line.improved) CoachColors.Accent else CoachColors.Muted)
                }
                LessonText("칸 안의 위치는 확인할 수 없어요. 오늘은 주차 과정을 돌아봤어요.", 32, CoachColors.Muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LessonButton(stringResource(R.string.lesson_again), onAgain, primary = true)
            LessonButton(stringResource(R.string.lesson_end), onEnd)
        }
    }
}
