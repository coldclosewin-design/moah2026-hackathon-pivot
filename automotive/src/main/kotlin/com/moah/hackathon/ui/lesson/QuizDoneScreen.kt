package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
@OptIn(ExperimentalLayoutApi::class)
internal fun QuizDoneScreen(task: Task, results: List<QuizResult>, items: List<QuizItem>, remark: String,
    onRestart: () -> Unit, locked: Boolean = false, onDemoStop: (() -> Unit)? = null) {
    if (locked) {
        ResultLockedScreen(onDemoStop)
        return
    }
    val byItem = results.associateBy { it.itemId }
    val correct = items.count { byItem[it.id]?.correct == true }
    val compact = items.size >= 4
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(.25f).fillMaxHeight().background(CoachColors.Ink).padding(60.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                BrandMark(color = CoachColors.Paper)
                LessonText("맞은 문제 / ${items.size}", 36, CoachColors.Platinum)
                LessonText(correct.toString().padStart(2, '0'), 230, CoachColors.Platinum)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 5) {
                    items.forEachIndexed { index, item ->
                        val good = byItem[item.id]?.correct == true
                        LessonText("${index + 1} ${if (byItem[item.id] == null) "—" else if (good) "✓" else "✗"}", if(items.size>4) 26 else 32, if (good) CoachColors.Ink else CoachColors.Platinum,
                            modifier = Modifier.background(if (good) CoachColors.Platinum else CoachColors.Periwinkle, RoundedCornerShape(100)).padding(10.dp),maxLines=1)
                    }
                }
                LessonText(coachDisplayText(remark), 44, CoachColors.Platinum)
                Spacer(Modifier.weight(1f))
                FillButton("메인으로", onRestart, Modifier.fillMaxWidth().height(140.dp), inverse = true)
            }
            Column(Modifier.weight(.75f).fillMaxHeight().background(CoachColors.Lavender).padding(52.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Eyebrow(taskModeLine(task, LessonMode.QUIZ))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp)) {
                    items.forEachIndexed { index, item ->
                        val result = byItem[item.id]
                        Column(Modifier.fillMaxWidth().background(CoachColors.Paper, RoundedCornerShape(40.dp)).padding(if (compact) 22.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)) {
                            LessonText("${index + 1}   ${item.question}", 38)
                            if (result == null) LessonText("안 풀었어요", 34, CoachColors.Muted)
                            else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                if (!result.correct) {
                                    AnswerPill("내 답", false)
                                    LessonText(item.choices[result.chosen], 34, CoachColors.Signal, modifier = Modifier.weight(.85f))
                                }
                                AnswerPill(if (result.correct) "내 답 · 정답" else "정답", true)
                                LessonText(item.choices[item.answer], 34, modifier = Modifier.weight(1.2f))
                            }
                            Box(Modifier.fillMaxWidth().background(CoachColors.Lavender, RoundedCornerShape(18.dp)).padding(horizontal = 20.dp, vertical = if (compact) 8.dp else 12.dp)) {
                                LessonText("❝ ${item.why}", 30, CoachColors.Muted, maxLines = if (items.size >= 4 && result?.correct == true) 1 else 3)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerPill(label: String, correct: Boolean) {
    LessonText(label, 28, if (correct) CoachColors.Paper else CoachColors.Signal,
        modifier = Modifier.background(if (correct) CoachColors.Ink else CoachColors.Paper, RoundedCornerShape(100))
            .border(2.dp, if (correct) CoachColors.Ink else CoachColors.Signal, RoundedCornerShape(100)).padding(horizontal = 22.dp, vertical = 10.dp))
}
