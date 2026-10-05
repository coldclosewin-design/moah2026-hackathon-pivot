package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.moah.hackathon.ui.CoachTexture

@Composable
internal fun QuizScreen(task: Task, index: Int, total: Int, item: QuizItem, locked: Boolean,
    chosen: Int?, correctSoFar: Int, onAnswer: (Int) -> Unit, onNext: () -> Unit, onQuit: () -> Unit) {
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            QuizNumber((index + 1).toString().padStart(2, '0'), "${index + 1} / $total", Modifier.weight(.38f),
                correct = chosen?.let { it == item.answer }.takeUnless { locked })
            Column(Modifier.weight(.62f).fillMaxHeight().padding(start = 100.dp, end = 120.dp, top = 64.dp, bottom = 52.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Eyebrow("지식 테스트 · ${task.title}")
                Headline(item.question, size = 72)
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
                    if (locked) {
                        LessonText(stringResource(R.string.lesson_quiz_locked), 40, CoachColors.Muted)
                    } else {
                        item.choices.forEachIndexed { choiceIndex, choice ->
                            val correct = chosen != null && choiceIndex == item.answer
                            val mine = choiceIndex == chosen
                            val badge = when {
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
                            val action = if (chosen == null) Modifier.clickable(role = Role.Button) { onAnswer(choiceIndex) } else Modifier
                            Row(Modifier.fillMaxWidth().heightIn(min = 112.dp)
                                .surfaceTexture(if (correct) CoachColors.Periwinkle else CoachColors.Lavender,
                                    if (correct) CoachTexture.SelectedCard else CoachTexture.Card)
                                .then(if (mine && !correct) Modifier.border(4.dp, CoachColors.Signal) else Modifier)
                                .then(action).semantics { selected = mine }
                                .padding(horizontal = 28.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                LessonText("${choiceIndex + 1}", 32, foreground)
                                LessonText(choice, 42, foreground, modifier = Modifier.weight(1f))
                                if (badge != null) LessonText(badge, 32,
                                    if (correct) CoachColors.Periwinkle else CoachColors.Paper,
                                    modifier = Modifier.background(if (correct) CoachColors.Paper else CoachColors.Signal,
                                        RoundedCornerShape(100)).padding(horizontal = 20.dp, vertical = 8.dp))
                            }
                        }
                        if (chosen != null) {
                            Spacer(Modifier.height(8.dp))
                            QuizExplanation(item.why)
                        }
                    }
                }
                if (!locked) {
                    LessonText("맞은 문제 $correctSoFar", 32, CoachColors.Muted)
                    Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextAction(stringResource(R.string.lesson_quit), onQuit)
                        if (chosen != null) PrimaryPill(stringResource(if (index >= total - 1) R.string.lesson_quiz_result
                            else R.string.lesson_next_question), onNext)
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuizNumber(number: String, label: String, modifier: Modifier, correct: Boolean? = null) {
    Box(modifier.fillMaxHeight().background(CoachColors.Ink).clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(CoachColors.Periwinkle, 160f, 190f, false,
                Offset(-size.width * .5f, size.height * .38f), Size(size.width * 1.5f, size.width * 1.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(110.dp.toPx()))
        }
        BrandMark(Modifier.padding(start = 180.dp, top = 64.dp), CoachColors.Paper)
        Column(Modifier.align(Alignment.Center).padding(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow(label, color = CoachColors.Paper)
            if (correct == null) Headline(number, size = 320, color = CoachColors.Paper)
            else {
                Spacer(Modifier.height(72.dp))
                Canvas(Modifier.size(250.dp)) {
                    val path = Path().apply {
                        if (correct) {
                            moveTo(size.width * .12f, size.height * .52f)
                            lineTo(size.width * .40f, size.height * .80f)
                            lineTo(size.width * .90f, size.height * .20f)
                        } else {
                            moveTo(size.width * .18f, size.height * .18f); lineTo(size.width * .82f, size.height * .82f)
                            moveTo(size.width * .82f, size.height * .18f); lineTo(size.width * .18f, size.height * .82f)
                        }
                    }
                    drawPath(path, if (correct) CoachColors.Paper else CoachColors.Signal,
                        style = Stroke(28.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                Spacer(Modifier.height(64.dp))
                LessonText(if (correct) "맞았어요" else "아쉬워요", 56, CoachColors.Paper)
            }
        }
    }
}

@Composable
internal fun QuizExplanation(text: String, size: Int = 40) {
    Row(Modifier.fillMaxWidth().surfaceTexture(CoachColors.Lavender, CoachTexture.Card).padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
        LessonText("“", 88, CoachColors.Periwinkle, modifier = Modifier.width(56.dp))
        LessonText(text, size, modifier = Modifier.weight(1f))
    }
}
