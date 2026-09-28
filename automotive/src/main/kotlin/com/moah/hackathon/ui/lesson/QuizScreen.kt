package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.QuizItem
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun QuizScreen(task: Task, index: Int, total: Int, item: QuizItem, locked: Boolean,
    chosen: Int?, correctSoFar: Int, onAnswer: (Int) -> Unit, onNext: () -> Unit, onRestart: () -> Unit) {
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            QuizNumber((index + 1).toString().padStart(2, '0'), "${index + 1} / $total", Modifier.weight(.38f))
            Column(Modifier.weight(.62f).fillMaxHeight().padding(start = 100.dp, end = 120.dp, top = 64.dp, bottom = 52.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Eyebrow("지식 테스트 · ${task.title}")
                Headline(item.question, size = 72)
                Spacer(Modifier.weight(1f))
                if (locked) {
                    // Omit choices and next entirely: moving screens expose no click or scroll actions.
                    LessonText(stringResource(R.string.lesson_quiz_locked), 40, CoachColors.Muted)
                } else {
                    item.choices.forEachIndexed { choiceIndex, choice ->
                        val correct = chosen != null && choiceIndex == item.answer
                        val mine = choiceIndex == chosen
                        val eyebrow = when {
                            correct && mine -> "내 답 · 정답"
                            correct -> "정답"
                            mine -> "내 답"
                            else -> null
                        }
                        val foreground = when {
                            correct -> CoachColors.Paper
                            chosen == null || mine -> CoachColors.Ink
                            else -> CoachColors.Muted
                        }
                        val action = if (chosen == null) Modifier.clickable(role = Role.Button) { onAnswer(choiceIndex) }
                            else Modifier
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(220.dp)) {
                                if (eyebrow != null) Eyebrow(eyebrow,
                                    color = if (correct) CoachColors.Periwinkle else CoachColors.Signal)
                            }
                            Row(Modifier.weight(1f).heightIn(min = 120.dp)
                                .background(if (correct) CoachColors.Periwinkle else CoachColors.Lavender)
                                .then(if (mine && !correct) Modifier.border(4.dp, CoachColors.Signal) else Modifier)
                                .then(action).semantics { selected = mine }
                                .padding(horizontal = 32.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                LessonText("${choiceIndex + 1}", 32, foreground)
                                LessonText(choice, 48, foreground)
                            }
                        }
                    }
                    if (chosen != null) {
                        LessonText(item.why, 40, CoachColors.Muted)
                        PrimaryPill(stringResource(if (index >= total - 1) R.string.lesson_quiz_result
                            else R.string.lesson_next_question), onNext)
                    }
                }
                Spacer(Modifier.weight(1f))
                PosterRule()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (!locked) TextAction(stringResource(R.string.lesson_quit), onRestart)
                    LessonText("맞은 문제 $correctSoFar", 32, CoachColors.Muted)
                }
            }
        }
    }
}

@Composable
internal fun QuizNumber(number: String, label: String, modifier: Modifier) {
    Box(modifier.fillMaxHeight().background(CoachColors.Ink).clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(CoachColors.Periwinkle, 160f, 190f, false,
                Offset(-size.width * .5f, size.height * .38f), Size(size.width * 1.5f, size.width * 1.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(110.dp.toPx()))
            drawRect(CoachColors.Lavender, Offset(size.width * .85f, size.height * .85f),
                Size(size.width * .15f, size.height * .15f))
        }
        BrandMark(Modifier.padding(start = 180.dp, top = 64.dp), CoachColors.Paper)
        Column(Modifier.align(Alignment.Center).padding(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow(label, color = CoachColors.Paper)
            Headline(number, size = 320, color = CoachColors.Paper)
        }
    }
}
