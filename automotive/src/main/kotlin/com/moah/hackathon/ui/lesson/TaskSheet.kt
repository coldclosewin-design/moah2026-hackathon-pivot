package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

/** Category navigation and the footer stay fixed; the reservation action opens a separate sheet layer. */
@Composable
internal fun TaskSheet(tasks: List<Task>, category: TaskType, task: Task?, mode: LessonMode,
    onCategory: (TaskType) -> Unit, onTask: (Task) -> Unit,
    onMode: (LessonMode) -> Unit, onBack: () -> Unit, onStart: () -> Unit, onVenues: () -> Unit) {
    val groups = tasks.groupBy { it.type }
    Column(Modifier.fillMaxSize()) {
        Eyebrow("연습할 과제", color = CoachColors.Periwinkle)
        Spacer(Modifier.height(40.dp))
        // Reclaim unused space below the menu so the venue link and fixed footer still fit.
        Row(Modifier.fillMaxWidth().height(128.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            categoryOrder().forEach { type ->
                CategoryChoice(type, type == category, groups[type].orEmpty().any { it.isReady },
                    Modifier.weight(1f).fillMaxHeight()) { onCategory(type) }
            }
        }
        Eyebrow("${taskTypeLabel(category)} 세부 과제", color = CoachColors.Periwinkle)
        Spacer(Modifier.height(24.dp))
        // The face is 288 dp art + 144 dp band; reserve the existing 32 dp check overhang.
        BoxWithConstraints(Modifier.fillMaxWidth().height(464.dp)) {
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
        Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp)) {
            PosterRule()
            TextAction("제휴 시험장", onVenues, size = 32)
        }
        // Keep the footer at the same position even when a category has no ready task.
        Column(Modifier.height(146.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (task != null) {
                Eyebrow("모드", color = CoachColors.Periwinkle)
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
            if (task != null) PrimaryPill(stringResource(R.string.lesson_start), onStart)
        }
    }
}

@Composable
private fun CategoryChoice(type: TaskType, expanded: Boolean, ready: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = when {
        expanded -> CoachColors.Signal
        ready -> CoachColors.Periwinkle
        else -> CoachColors.Muted
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
    val foreground = when { chosen -> CoachColors.Paper; task.isReady -> CoachColors.Ink; else -> CoachColors.Muted }
    val background = when { chosen -> CoachColors.Periwinkle; task.isReady -> CoachColors.Lavender; else -> CoachColors.Lavender.copy(alpha = .4f) }
    // Planned bays have disabled semantics and no click action, including through their children.
    val action = if (task.isReady) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics { disabled() }
    Box(modifier.then(action).semantics(mergeDescendants = true) { selected = chosen }) {
        Box(Modifier.matchParentSize().padding(bottom = 32.dp).then(
            if (task.isReady) Modifier.surfaceTexture(background, if (chosen) CoachTexture.SelectedCard else CoachTexture.Card)
            else Modifier.background(background)))
        Canvas(Modifier.fillMaxSize()) {
            val bottom = size.height - 32.dp.toPx()
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
        Column(Modifier.fillMaxSize().padding(bottom = 32.dp)) {
            Box(Modifier.fillMaxWidth().height(288.dp), contentAlignment = Alignment.Center) {
                val art = Modifier.size(240.dp, 176.dp).alpha(if (task.isReady) 1f else .55f)
                val ink = if (chosen) CoachColors.Paper else CoachColors.Ink
                when (task.type) {
                    TaskType.PARKING -> ParkingTaskDiagram(task.id, ink,
                        if (chosen) CoachColors.Lavender else CoachColors.Periwinkle,
                        if (chosen) CoachColors.Periwinkle else CoachColors.Lavender, art)
                    TaskType.CHECKLIST -> ChecklistTaskDiagram(ink, art)
                    else -> if (task.course != null) CourseMap(task.course, art, thumbnail = true, ink = ink)
                        else CategoryTaskDiagram(task.type, ink, art)
                }
                if (task.course?.isExam == true) LessonText("모의시험", 28, foreground,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 16.dp))
            }
            Box(Modifier.fillMaxWidth().height(144.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(
                    if (chosen) CoachColors.Paper.copy(alpha = .18f) else CoachColors.Ink.copy(alpha = .08f)))
                TaskTitleBand(task, foreground, if (chosen) CoachColors.Paper.copy(alpha = .7f) else CoachColors.Muted,
                    Modifier.fillMaxSize().padding(horizontal = 28.dp))
            }
        }
    }
}

/** Measure before drawing so each title stays on one line and never drops below 36 sp. */
@Composable
private fun TaskTitleBand(task: Task, titleColor: Color, detailColor: Color, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val detail = if (task.isReady) task.difficulty.label else task.status.label
    val style = TextStyle(localeList = LocaleList("ko-KR"))
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val detailWidth = measurer.measure(AnnotatedString(detail), style.copy(fontSize = 32.sp), softWrap = false).size.width
        val titleWidth = with(density) { (maxWidth - 8.dp).toPx() } - detailWidth
        val titleSize = (40 downTo 36).firstOrNull { size ->
            measurer.measure(AnnotatedString(task.title), style.copy(fontSize = size.sp), softWrap = false).size.width <= titleWidth
        } ?: 36
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LessonText(task.title, titleSize, titleColor, modifier = Modifier.weight(1f).alignByBaseline(), maxLines = 1)
            Spacer(Modifier.width(8.dp))
            LessonText(detail, 32, detailColor, modifier = Modifier.alignByBaseline(), maxLines = 1)
        }
    }
}

/** Monochrome category marks fill previously empty catalogue art areas without resources or letters. */
@Composable
private fun CategoryTaskDiagram(type: TaskType, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (type == TaskType.DRIVING) {
            for (side in listOf(-1f, 1f)) drawPath(Path().apply {
                moveTo(size.width / 2 + side * 64.dp.toPx(), 20.dp.toPx())
                lineTo(size.width / 2 + side * 84.dp.toPx(), size.height - 20.dp.toPx())
            }, color, style = stroke)
            drawLine(color, Offset(size.width / 2, 24.dp.toPx()), Offset(size.width / 2, size.height - 24.dp.toPx()),
                6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20.dp.toPx(), 16.dp.toPx())))
        } else {
            val cx = size.width / 2
            val top = 24.dp.toPx()
            val bottom = size.height - 24.dp.toPx()
            for (side in listOf(-1f, 1f)) drawPath(Path().apply {
                moveTo(cx, top + 12.dp.toPx())
                quadraticTo(cx + side * 40.dp.toPx(), top - 8.dp.toPx(), cx + side * 80.dp.toPx(), top)
                lineTo(cx + side * 80.dp.toPx(), bottom - 12.dp.toPx())
                quadraticTo(cx + side * 40.dp.toPx(), bottom - 20.dp.toPx(), cx, bottom)
                close()
            }, color, style = stroke)
        }
    }
}

@Composable
private fun ChecklistTaskDiagram(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        repeat(3) { row ->
            val y = (40 + row * 48).dp.toPx()
            drawPath(Path().apply {
                moveTo(36.dp.toPx(), y)
                lineTo(46.dp.toPx(), y + 10.dp.toPx())
                lineTo(64.dp.toPx(), y - 12.dp.toPx())
            }, color, style = Stroke(stroke))
            drawLine(color, Offset(88.dp.toPx(), y), Offset(204.dp.toPx(), y), stroke)
        }
    }
}

/** Open-ended catalogue bays; the silhouette distinguishes front and rear entry. */
@Composable
private fun ParkingTaskDiagram(id: String, color: Color, panel: Color, glass: Color, modifier: Modifier) {
    Canvas(modifier) {
        val nominalHeight = 160.dp.toPx()
        // The diagonal bay also fits the 176 dp art box, including its rotated line ends.
        val fit = if (id == "parking-angle") minOf(1f,
            (size.height - 6.dp.toPx()) / (nominalHeight * .8660254f + (nominalHeight * .43f + 40.dp.toPx()) * .5f)) else 1f
        val carHeight = nominalHeight * fit
        val carWidth = carHeight * .43f
        val center = Offset(size.width / 2, size.height / 2)
        val halfWidth = carWidth / 2 + 20.dp.toPx() * fit
        val halfHeight = carHeight / 2
        val stroke = 4.dp.toPx()
        rotate(if (id == "parking-angle") 30f else 0f) {
            if (id == "parking-parallel") {
                drawLine(color, center + Offset(halfWidth, -halfHeight), center + Offset(halfWidth, halfHeight), stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 12.dp.toPx())))
            } else {
                for (side in listOf(-1f, 1f)) drawLine(color,
                    center + Offset(side * halfWidth, -halfHeight), center + Offset(side * halfWidth, halfHeight), stroke)
            }
            rotate(if (id == "parking-front") 180f else 0f, center) {
                withTransform({
                    translate(center.x - carWidth / 2, center.y - carHeight / 2)
                    scale(carWidth / 100f, carHeight / 250f, Offset.Zero)
                }) {
                    vehicleSilhouette(color, panel, glass)
                }
            }
        }
    }
}
