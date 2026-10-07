package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.feature.lesson.QuizItem
import com.moah.hackathon.feature.lesson.QuizResult
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun QuizDoneScreen(task: Task, results: List<QuizResult>, items: List<QuizItem>, remark: String,
    onRestart: () -> Unit, locked: Boolean = false, onDemoStop: (() -> Unit)? = null) {
    if (locked) {
        ResultLockedScreen(onDemoStop)
        return
    }
    val byItem = results.associateBy { it.itemId }
    val correct = items.count { byItem[it.id]?.correct == true }
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            QuizNumber(correct.toString().padStart(2, '0'), "맞은 문제 / ${items.size}", Modifier.weight(.38f))
            Column(Modifier.weight(.62f).fillMaxHeight().padding(start = 100.dp, end = 120.dp, top = 64.dp, bottom = 52.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Eyebrow(taskModeLine(task, LessonMode.QUIZ))
                Headline(coachDisplayText(remark), size = 72)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    items.forEachIndexed { index, item ->
                        val result = byItem[item.id]
                        PosterRule()
                        LessonText("${index + 1}. ${item.question}", 40)
                        when {
                            result == null -> LessonText("안 풀었어요", 40, CoachColors.Muted)
                            result.correct -> LessonText("맞았어요", 40, CoachColors.Periwinkle)
                            else -> {
                                Text(buildAnnotatedString {
                                    withStyle(SpanStyle(color = CoachColors.Signal)) { append(item.choices[result.chosen]) }
                                    withStyle(SpanStyle(color = CoachColors.Muted)) { append(" → ") }
                                    withStyle(SpanStyle(color = CoachColors.Periwinkle)) { append(item.choices[item.answer]) }
                                }, fontSize = 40.sp, lineHeight = 52.sp)
                                QuizExplanation(item.why, 32)
                            }
                        }
                    }
                }
                BottomActions(primary = { MainPill(onRestart) })
            }
        }
    }
}
