package com.moah.hackathon.ui.lesson

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.delay
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.moah.hackathon.ports.withObjectParticle

/** Open directly on the task cards so the existing text-driven tools can select a task. */
@Composable
internal fun TaskSheet(tasks: List<Task>, category: TaskType, task: Task?, mode: LessonMode,
    onCategory: (TaskType) -> Unit, onTask: (Task) -> Unit,
    onMode: (LessonMode) -> Unit, onBack: () -> Unit, onStart: () -> Unit, onVenues: () -> Unit) {
    var editor by remember { mutableStateOf<String?>(null) }
    // T2 ends on the sentence, then reveals the cards for the existing text-driven tools.
    LaunchedEffect(Unit) { Animatable(0f).animateTo(1f, tween(620)); editor = "task" }
    val sentenceSize by androidx.compose.animation.core.animateIntAsState(if (editor == null) 132 else 90,
        tween(420, easing = CoachMotion.Fill), label = "sentence-size")
    val groups = tasks.groupBy { it.type }
    Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(CoachColors.Lavender, CoachColors.Platinum))).padding(72.dp)) {
        BrandMark()
        Spacer(Modifier.height(if (editor == null) 120.dp else 36.dp))
        if (editor == null) {
            Eyebrow("연습할 과제")
            Spacer(Modifier.height(32.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LessonText("오늘은", sentenceSize, bold = true)
            WordPill(task?.title ?: "과제 고르기", "과제 바꾸기", sentenceSize, editor == "task") { editor = "task" }
            LessonText(task?.title?.let { it.withObjectParticle().removePrefix(it) } ?: "를", sentenceSize)
            if (editor != null) {
                YellowModeIcon()
                WordPill(mode.label, "모드 바꾸기", sentenceSize) { editor = "mode" }
                LessonText("로", sentenceSize)
                LessonText("연습해요.", sentenceSize, CoachColors.Muted)
            }
        }
        if (editor == null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                YellowModeIcon(Modifier.size(100.dp))
                WordPill(mode.label, "모드 바꾸기", sentenceSize) { editor = "mode" }
                LessonText("로", sentenceSize)
                LessonText("연습해요.", sentenceSize, CoachColors.Muted)
            }
            Spacer(Modifier.height(64.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("${taskTypeLabel(category)} › ${task?.title ?: "과제 고르기"}", modeDescription(mode)).forEach {
                    LessonText(it, 36, modifier = Modifier.background(CoachColors.Paper,
                        androidx.compose.foundation.shape.RoundedCornerShape(100)).padding(horizontal = 28.dp, vertical = 16.dp))
                }
            }
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.height(40.dp))
            key(editor) {
                SheetCard(Modifier.weight(1f).fillMaxWidth().homeShared("venue-paper")) {
                    Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (editor == "task") {
                            SelectionTrack(categoryOrder(), category, ::taskTypeLabel, onCategory,
                                Modifier.fillMaxWidth(.72f), height = 88.dp, textSize = 40, role = Role.Tab, smoothCategory = true)
                            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                                val width = (maxWidth - 72.dp - if (groups[category].orEmpty().size > 4) 80.dp else 0.dp) / 4
                                val density = LocalDensity.current
                                val categoryTransition = androidx.compose.animation.core.updateTransition(category, label = "category")
                                categoryTransition.AnimatedContent(Modifier.fillMaxSize(), transitionSpec = {
                                    val direction = if (categoryOrder().indexOf(targetState) > categoryOrder().indexOf(initialState)) 1 else -1
                                    EnterTransition.None togetherWith (fadeOut(tween(160)) + slideOutHorizontally(tween(160, easing = androidx.compose.animation.core.CubicBezierEasing(.4f, 0f, 1f, 1f))) {
                                        -with(density) { 48.dp.roundToPx() } * direction
                                    })
                                }) { shown ->
                                    LazyRow(Modifier.fillMaxSize().then(if (shown != category) Modifier.clearAndSetSemantics {} else Modifier),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                        itemsIndexed(groups[shown].orEmpty(), key = { _, item -> item.id }) { index, item ->
                                            val direction = if (categoryOrder().indexOf(categoryTransition.targetState) >= categoryOrder().indexOf(categoryTransition.currentState)) 1 else -1
                                            TaskBay(item, item.id == task?.id, Modifier.width(width).fillMaxHeight().animateEnterExit(
                                                enter = fadeIn(tween(280, delayMillis = 80 + index * 40)) + slideInHorizontally(tween(280, delayMillis = 80 + index * 40, easing = androidx.compose.animation.core.CubicBezierEasing(.22f, 1f, .36f, 1f))) {
                                                    with(density) { 64.dp.roundToPx() } * direction
                                                }, exit = ExitTransition.None)) { onTask(item) }
                                        }
                                    }
                                }
                            }
                        } else {
                            Eyebrow("모드 바꾸기")
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                LessonMode.entries.filter { task?.supports(it) != false }.forEach { option ->
                                    val chosen = option == mode
                                    Column(Modifier.weight(1f).fillMaxHeight()
                                        .background(if (chosen) CoachColors.Ink else CoachColors.Lavender,
                                            androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
                                        .clickable(role = Role.RadioButton) { onMode(option) }
                                        .semantics { selected = chosen }.padding(40.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                        SymbolTile(CoachSymbol.Voice, Modifier.size(88.dp), animate = false)
                                        LessonText(option.label, 72, if (chosen) CoachColors.Paper else CoachColors.Ink)
                                        LessonText(modeDescription(option), 40, if (chosen) CoachColors.Platinum else CoachColors.Muted)
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically) {
                            if (editor == "task" && task != null) SelectionTrack(LessonMode.entries.filter(task::supports), mode, { it.label }, onMode,
                                Modifier.width(980.dp), height = 96.dp, textSize = 38)
                            Spacer(Modifier.weight(1f))
                            ArrowPill("닫기", "↓", { editor = null })
                            Spacer(Modifier.width(32.dp))
                            PrimaryPill(if (editor == "task") "이 과제로" else "이 모드로", { editor = null }, Modifier.width(660.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            ArrowPill("돌아가기", "←", onBack, Modifier.homeShared("venue-back"))
            ArrowPill("제휴 시험장", "↗", onVenues, Modifier.homeShared("venue-title"))
            Spacer(Modifier.weight(1f))
            PrimaryPill(stringResource(R.string.lesson_start), onStart, Modifier.width(1125.dp).homeShared("venue-action"), enabled = task != null)
        }
    }
}

@Composable
private fun TaskBay(task: Task, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val foreground = when { chosen -> CoachColors.Paper; task.isReady -> CoachColors.Ink; else -> CoachColors.Muted }
    val background = when { chosen -> CoachColors.Ink; task.isReady -> CoachColors.Lavender; else -> CoachColors.Lavender.copy(alpha = .4f) }
    // Planned bays have disabled semantics and no click action, including through their children.
    val action = if (task.isReady) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics { disabled() }
    Box(modifier.background(background, androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
        .then(action).semantics(mergeDescendants = true) { selected = chosen }) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val artHeight = minOf(maxHeight, maxWidth / (240f / 176f))
                val art = Modifier.size(artHeight * (240f / 176f), artHeight).alpha(if (task.isReady) 1f else .55f)
                val ink = if (chosen) CoachColors.Paper else CoachColors.Ink
                when (task.type) {
                    TaskType.PARKING -> ParkingTaskDiagram(task.id, ink,
                        if (chosen) CoachColors.Lavender else CoachColors.Periwinkle,
                        if (chosen) CoachColors.Ink else CoachColors.Lavender, art)
                    TaskType.KNOWLEDGE -> KnowledgeTaskDiagram(task.id, ink, art)
                    TaskType.CHECKLIST -> if (task.quizOnly) RoadSignsTaskDiagram(ink, art) else ChecklistTaskDiagram(ink, art)
                    else -> if (task.course != null) CourseMap(task.course, art, thumbnail = true, ink = ink)
                        else CategoryTaskDiagram(task.type, ink, art)
                }
                if (task.course?.isExam == true) LessonText("모의시험", 28, foreground,
                    modifier = Modifier.align(Alignment.TopStart))
            }
            TaskTitleBand(task, foreground, if (chosen) CoachColors.Paper.copy(alpha = .7f) else CoachColors.Muted,
                Modifier.fillMaxWidth().height(64.dp))
        }
        if (chosen) SymbolTile(CoachSymbol.Check, Modifier.align(Alignment.TopEnd).padding(12.dp).size(48.dp), animate = false)
    }
}

/** Driving titles share one size; other categories retain their measured one-line titles. */
@Composable
private fun TaskTitleBand(task: Task, titleColor: Color, detailColor: Color, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val detail = if (task.isReady) task.difficulty.label else task.status.label
    val style = TextStyle(localeList = LocaleList("ko-KR"))
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val detailWidth = measurer.measure(AnnotatedString(detail), style.copy(fontSize = 32.sp), softWrap = false).size.width
        val titleWidth = with(density) { (maxWidth - 8.dp).toPx() } - detailWidth
        val titleSize = if (task.type == TaskType.DRIVING) 36 else (40 downTo 36).firstOrNull { size ->
            measurer.measure(AnnotatedString(task.title), style.copy(fontSize = size.sp), softWrap = false).size.width <= titleWidth
        } ?: 36
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LessonText(task.title, titleSize, titleColor, modifier = Modifier.weight(1f).alignByBaseline(), maxLines = 1)
            Spacer(Modifier.width(8.dp))
            LessonText(detail, 28, detailColor, modifier = Modifier.background(detailColor.copy(alpha = .12f), androidx.compose.foundation.shape.RoundedCornerShape(100)).padding(horizontal = 12.dp, vertical = 2.dp), maxLines = 1)
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
        val sx = size.width / 240.dp.toPx(); val sy = size.height / 176.dp.toPx()
        withTransform({ scale(sx, sy, Offset.Zero) }) {
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
}

/** Open-ended catalogue bays; the silhouette distinguishes front and rear entry. */
@Composable
private fun ParkingTaskDiagram(id: String, color: Color, panel: Color, glass: Color, modifier: Modifier) {
    Canvas(modifier) {
        val nominalHeight = size.height * .98f
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
