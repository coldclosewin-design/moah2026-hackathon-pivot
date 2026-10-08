package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.ports.withObjectParticle
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun HomeTaskTitle(task: Task, mode: LessonMode, picked: Boolean, compact: Boolean,
    onTask: () -> Unit, onMode: (LessonMode) -> Unit, titleSize: Int? = null, onWheelChanged: (Boolean) -> Unit = {}) {
    var popup by remember(task.id, picked) { mutableStateOf(false) }
    LaunchedEffect(popup) { onWheelChanged(popup) }
    val taskWord = if (picked) task.title else "과제 고르기"
    val suffix = if (picked) task.title.withObjectParticle().removePrefix(task.title) else "부터"
    Eyebrow(if (picked) "오늘의 과제 · ${taskTypeLabel(task.type)}" else "처음 오셨네요", color = CoachColors.Periwinkle)
    Spacer(Modifier.height(16.dp))
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-task-title")) {
        val measurer = rememberTextMeasurer()
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val lines = listOf("$taskWord ▼ $suffix", if (picked) "${mode.label} ▼ 모드로 해 볼까요?" else "해 볼까요?")
        val size = listOf(titleSize ?: if (compact) 104 else 126, 112, 104, 88, 80, 72, 64, 56).distinct().filter { it <= (titleSize ?: if (compact) 104 else 126) }.firstOrNull { candidate ->
            lines.all { measurer.measure(AnnotatedString(it), TextStyle(fontSize = candidate.sp), softWrap = false).size.width <= width }
        } ?: 56
        Column {
            Row(Modifier.alpha(if (popup) .16f else 1f), verticalAlignment = Alignment.CenterVertically) {
                HomeTitleLink("$taskWord ▼", "과제·모드 바꾸기", size, !picked, onTask)
                LessonText(suffix, size, if (popup) CoachColors.Platinum else CoachColors.Ink, bold = true)
            }
            if (picked) Row(verticalAlignment = Alignment.CenterVertically) {
                Box(if (popup) Modifier.width(500.dp) else Modifier) {
                    Box(Modifier.alpha(if (popup) 0f else 1f)) { HomeTitleLink("${mode.label} ▼", "모드 바꾸기", size, false, { popup = true }) }
                    if (popup) Popup(onDismissRequest = { popup = false },
                        offset = IntOffset(0, with(LocalDensity.current) { -150.dp.roundToPx() }),
                        properties = PopupProperties(focusable = true)) {
                        val options = LessonMode.entries.filter(task::supports)
                        Box(Modifier.width(500.dp).height(464.dp).shadow(12.dp, RoundedCornerShape(52.dp))
                            .background(CoachColors.Paper, RoundedCornerShape(52.dp)).padding(16.dp)
                            .semantics { contentDescription = "모드 바꾸기" }.testTag("home-mode-wheel")) {
                            options.forEachIndexed { index, option ->
                                val selectedIndex = options.indexOf(mode)
                                val position = (index - selectedIndex + 1 + options.size) % options.size
                                val y by animateDpAsState((position * 144).dp,
                                    tween(280, easing = CoachMotion.Stone), label = "mode-wheel-position")
                                val chosen = option == mode
                                Row(Modifier.offset(y = y).fillMaxWidth().height(144.dp)
                                    .background(if (chosen) CoachColors.Ink else CoachColors.Paper, RoundedCornerShape(36.dp))
                                    .clickable(role = Role.RadioButton) { if (chosen) popup = false else onMode(option) }
                                    .semantics { selected = chosen }.padding(horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    if (chosen) YellowModeIcon(bars = 3 - index.coerceAtMost(2))
                                    else SymbolTile(CoachSymbol.Voice, Modifier.size(64.dp).alpha(.35f), animate = false)
                                    LessonText(option.label, 72, if (chosen) CoachColors.Paper else CoachColors.Muted)
                                }
                            }
                        }
                    }
                }
                LessonText("모드로 해 볼까요?", size, bold = true)
            } else LessonText("해 볼까요?", size, bold = true)
        }
    }
}

@Composable
private fun HomeTitleLink(text: String, description: String, size: Int, dashed: Boolean, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 96.dp).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }.padding(end = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterStart) {
        val label = buildAnnotatedString {
            append(text.removeSuffix("▼"))
            withStyle(SpanStyle(fontSize = (size * .55f).sp)) { append("▼") }
        }
        LessonText(label, size, CoachColors.Ink, bold = true, modifier = Modifier.drawBehind {
            drawLine(CoachColors.Periwinkle, Offset(0f, this.size.height), Offset(this.size.width, this.size.height),
                2.dp.toPx(), pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null)
        })
    }
}
