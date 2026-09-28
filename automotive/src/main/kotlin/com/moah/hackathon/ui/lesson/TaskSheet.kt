package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors

/** Category navigation and the footer stay fixed; only the task row and reservation can scroll. */
@Composable
internal fun TaskSheet(tasks: List<Task>, category: TaskType, task: Task?, mode: LessonMode,
    reservation: ReservationCard?, onCategory: (TaskType) -> Unit, onTask: (Task) -> Unit,
    onMode: (LessonMode) -> Unit, onBack: () -> Unit, onStart: () -> Unit) {
    val groups = tasks.groupBy { it.type }
    Column(Modifier.fillMaxSize()) {
        Eyebrow("연습할 과제", color = CoachColors.Periwinkle)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            categoryOrder().forEach { type ->
                CategoryChoice(type, type == category, groups[type].orEmpty().any { it.isReady },
                    Modifier.weight(1f).fillMaxHeight()) { onCategory(type) }
            }
        }
        Spacer(Modifier.height(16.dp))
        LessonText("${taskTypeLabel(category)} 세부 과제", 40)
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(432.dp)) {
            val items = groups[category].orEmpty()
            // Four bays fit. Longer categories leave a visible preview of the next bay.
            val width = (maxWidth - 72.dp - if (items.size > 4) 80.dp else 0.dp) / 4
            key(category) {
                LazyRow(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    items(items, key = { it.id }) { item ->
                        TaskBay(item, item.id == task?.id, Modifier.width(width).fillMaxHeight()) { onTask(item) }
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)) {
            reservation?.let { ReservationInfo(it, compact = maxHeight < 300.dp) }
        }
        // Keep the footer at the same position even when a category has no ready task.
        Column(Modifier.height(146.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (task != null) {
                Eyebrow("모드")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LessonMode.entries.filter(task::supports).forEach { item ->
                        SelectionChip(item.label, item == mode, { onMode(item) })
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            TextAction(stringResource(R.string.lesson_back), onBack)
            if (task != null) PrimaryPill(stringResource(R.string.lesson_start), onStart, driver = true)
        }
    }
}

@Composable
private fun CategoryChoice(type: TaskType, expanded: Boolean, ready: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = when {
        expanded -> CoachColors.Signal
        ready -> CoachColors.Periwinkle
        else -> CoachColors.Periwinkle.copy(alpha = .6f)
    }
    Column(modifier.clickable(role = Role.Tab, onClick = onClick).semantics { selected = expanded },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
            LessonText(taskTypeLabel(type), 56, color)
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).background(CoachColors.Signal))
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Canvas(Modifier.size(32.dp, 16.dp)) {
                drawPath(Path().apply {
                    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
                }, CoachColors.Signal)
            }
        }
        if (!ready) {
            LessonText(TaskStatus.PLANNED.label, 32, color)
        }
    }
}

@Composable
private fun TaskBay(task: Task, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val foreground = if (task.isReady) CoachColors.Ink else CoachColors.Periwinkle.copy(alpha = .4f)
    val outline = when { chosen -> CoachColors.Ink; task.isReady -> CoachColors.Periwinkle; else -> CoachColors.Lavender }
    // Planned bays have disabled semantics and no click action, including through their children.
    val action = if (task.isReady) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics { disabled() }
    Box(modifier.then(action).semantics(mergeDescendants = true) { selected = chosen }) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = (if (chosen) 6.dp else 4.dp).toPx()
            val edge = stroke / 2
            val bottom = size.height - 32.dp.toPx()
            drawPath(Path().apply {
                moveTo(edge, 0f); lineTo(edge, bottom)
                lineTo(size.width - edge, bottom); lineTo(size.width - edge, 0f)
            }, outline, style = Stroke(stroke))
            if (chosen) {
                val center = Offset(size.width / 2, bottom)
                drawCircle(CoachColors.Paper, 40.dp.toPx(), center)
                drawCircle(CoachColors.Signal, 32.dp.toPx(), center)
                drawPath(Path().apply {
                    moveTo(center.x - 15.dp.toPx(), center.y)
                    lineTo(center.x - 4.dp.toPx(), center.y + 11.dp.toPx())
                    lineTo(center.x + 17.dp.toPx(), center.y - 12.dp.toPx())
                }, CoachColors.Paper, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(176.dp), contentAlignment = Alignment.Center) {
                if (task.type == TaskType.PARKING) {
                    ParkingTaskDiagram(task.id, if (chosen) CoachColors.Ink else CoachColors.Periwinkle.copy(alpha = if (task.isReady) 1f else .4f),
                        Modifier.size(240.dp, 176.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            // Include Korean font metrics as well as both 52 sp line boxes; 104 dp ellipsizes a line.
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                LessonText(task.title, 40, foreground, modifier = Modifier.widthIn(max = if (task.type == TaskType.PARKING) 220.dp else 252.dp),
                    maxLines = 2, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(4.dp))
            LessonText(if (task.isReady) task.difficulty.label else task.status.label, 32, foreground, maxLines = 1)
        }
    }
}

/** Static catalogue illustration: body and windows only, with a 160 dp car; no live vehicle data. */
@Composable
private fun ParkingTaskDiagram(id: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val carHeight = 160.dp.toPx()
        val carWidth = carHeight * .43f
        val cx = size.width / 2
        val top = (size.height - carHeight) / 2
        rotate(if (id == "parking-angle") 30f else 0f) {
            val left = cx - carWidth / 2 - 20.dp.toPx()
            val right = cx + carWidth / 2 + 20.dp.toPx()
            val stroke = 4.dp.toPx()
            when (id) {
                "parking-parallel" -> drawLine(color, Offset(right, top), Offset(right, top + carHeight), stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 12.dp.toPx())))
                else -> {
                    drawLine(color, Offset(left, top), Offset(left, top + carHeight), stroke)
                    drawLine(color, Offset(right, top), Offset(right, top + carHeight), stroke)
                    if (id == "parking-front") drawLine(color, Offset(left, top), Offset(right, top), stroke)
                }
            }
            withTransform({
                translate(cx - carWidth / 2, top)
                scale(carWidth / 100f, carHeight / 250f, Offset.Zero)
            }) {
                drawPath(Path().apply {
                    moveTo(25f, 1f); cubicTo(10f, 3f, 6f, 12f, 5f, 28f)
                    lineTo(5f, 173f); cubicTo(-1f, 210f, 1f, 232f, 15f, 242f)
                    cubicTo(30f, 253f, 70f, 253f, 85f, 242f)
                    cubicTo(99f, 232f, 101f, 210f, 95f, 173f); lineTo(95f, 28f)
                    cubicTo(94f, 12f, 90f, 3f, 75f, 1f); quadraticTo(50f, -2f, 25f, 1f); close()
                }, color)
                drawPath(Path().apply {
                    moveTo(23f, 32f); quadraticTo(50f, 26f, 77f, 32f)
                    lineTo(74f, 55f); quadraticTo(50f, 60f, 26f, 55f); close()
                }, CoachColors.Paper)
                drawPath(Path().apply {
                    moveTo(22f, 137f); quadraticTo(50f, 143f, 78f, 137f)
                    lineTo(86f, 170f); quadraticTo(50f, 190f, 14f, 170f); close()
                }, CoachColors.Paper)
                listOf(false, true).forEach { rightWindow ->
                    withTransform({ if (rightWindow) { translate(100f, 0f); scale(-1f, 1f, Offset.Zero) } }) {
                        drawPath(Path().apply {
                            moveTo(17f, 62f); quadraticTo(24f, 90f, 19f, 126f)
                            lineTo(11f, 154f); lineTo(11f, 93f); close()
                        }, CoachColors.Paper)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationInfo(card: ReservationCard, compact: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PosterRule()
        if (compact) {
            // The toggle remains reachable while the expanded details scroll in the remaining space.
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TextAction("제휴 시험장 예시", { expanded = !expanded }, size = 32)
                if (expanded) Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) { ReservationDetails(card) }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("제휴 시험장 예시", color = CoachColors.Periwinkle)
                ReservationDetails(card)
            }
        }
    }
}

@Composable
private fun ReservationDetails(card: ReservationCard) {
    LessonText("${card.venue}\n${card.slot}\n${card.course}", 40)
    LessonText(if (card.note.contains("실제 예약 연계 없음")) card.note else "${card.note} · 실제 예약 연계 없음", 32, CoachColors.Muted)
}
