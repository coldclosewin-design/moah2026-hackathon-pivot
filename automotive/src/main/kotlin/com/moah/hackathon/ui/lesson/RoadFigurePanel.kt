package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.RoadFigure
import com.moah.hackathon.ui.CoachColors

/** The diagram remains visible after answering, but the parent replaces it entirely while locked. */
@Composable
internal fun RoadFigurePanel(figure: RoadFigure, progress: String, modifier: Modifier) {
    Column(modifier.fillMaxHeight().background(CoachColors.Ink)
        .padding(start = 100.dp, end = 80.dp, top = 64.dp, bottom = 64.dp)) {
        BrandMark(Modifier.padding(start = 80.dp), CoachColors.Paper)
        Spacer(Modifier.height(64.dp))
        Eyebrow("도로 표시", color = CoachColors.Paper.copy(alpha = .7f))
        Spacer(Modifier.height(32.dp))
        RoadFigureDiagram(figure, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        LessonText(progress, 40, CoachColors.Paper, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
internal fun RoadFigureDiagram(figure: RoadFigure, modifier: Modifier) {
    val description = when (figure) {
        RoadFigure.LEFT_ARROW -> "차로 바닥의 왼쪽으로 꺾인 화살표"
        RoadFigure.STRAIGHT_LEFT_ARROW -> "차로 바닥의 위쪽과 왼쪽으로 갈라지는 화살표"
        RoadFigure.WHITE_SOLID -> "같은 방향의 두 차로 사이 흰 실선"
        RoadFigure.YELLOW_SOLID_CENTER -> "서로 마주 오는 두 차로 사이 노란 실선"
        RoadFigure.ZIGZAG -> "차로 양옆의 흰 지그재그 선"
        RoadFigure.BLUE_BUS_LANE -> "나란한 두 차로 사이 파란 실선"
    }
    Canvas(modifier.clipToBounds().testTag("road-figure-${figure.name}")
        .semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        val paint = CoachColors.Paper
        val line = w * .013f
        drawRect(paint.copy(alpha = .07f), p(.06f, 0f), Size(w * .88f, h))
        for (x in listOf(.12f, .88f)) drawLine(paint.copy(alpha = .4f), p(x, 0f), p(x, 1f), line)
        fun direction(x: Float, down: Boolean = false) {
            val start = if (down) .23f else .77f
            val end = if (down) .40f else .60f
            drawLine(paint.copy(alpha = .55f), p(x, start), p(x, end), line * 1.5f)
            val back = if (down) end - .045f else end + .045f
            drawPath(Path().apply {
                moveTo(x * w, end * h)
                lineTo((x - .045f) * w, back * h)
                lineTo((x + .045f) * w, back * h)
                close()
            }, paint.copy(alpha = .55f))
        }
        when (figure) {
            RoadFigure.LEFT_ARROW, RoadFigure.STRAIGHT_LEFT_ARROW -> {
                drawPath(Path().apply {
                    moveTo(w * .56f, h * .78f); lineTo(w * .56f, h * .48f)
                    quadraticTo(w * .56f, h * .39f, w * .44f, h * .39f)
                    lineTo(w * .30f, h * .39f)
                }, paint, style = Stroke(w * .055f, cap = StrokeCap.Butt, join = StrokeJoin.Round))
                drawPath(Path().apply {
                    moveTo(w * .19f, h * .39f); lineTo(w * .37f, h * .29f)
                    lineTo(w * .37f, h * .49f); close()
                }, paint)
                if (figure == RoadFigure.STRAIGHT_LEFT_ARROW) {
                    drawLine(paint, p(.56f, .54f), p(.56f, .25f), w * .055f)
                    drawPath(Path().apply {
                        moveTo(w * .56f, h * .14f); lineTo(w * .43f, h * .31f)
                        lineTo(w * .69f, h * .31f); close()
                    }, paint)
                }
            }
            RoadFigure.WHITE_SOLID, RoadFigure.YELLOW_SOLID_CENTER, RoadFigure.BLUE_BUS_LANE -> {
                val color = when (figure) {
                    RoadFigure.YELLOW_SOLID_CENTER -> CoachColors.RoadMarkingYellow
                    RoadFigure.BLUE_BUS_LANE -> CoachColors.RoadMarkingBlue
                    else -> paint
                }
                drawLine(color, p(.5f, 0f), p(.5f, 1f), line * 1.5f)
                direction(.3f, down = figure == RoadFigure.YELLOW_SOLID_CENTER)
                direction(.7f)
            }
            RoadFigure.ZIGZAG -> {
                for (x in listOf(.26f, .74f)) drawPath(Path().apply {
                    moveTo(x * w, h * .06f)
                    repeat(8) { i -> lineTo((x + if (i % 2 == 0) .06f else 0f) * w, (.16f + i * .1f) * h) }
                }, paint, style = Stroke(line * 1.5f, join = StrokeJoin.Miter))
                direction(.5f)
            }
        }
    }
}

/** Monochrome catalogue mark; the two semantic paint colors are reserved for quiz diagrams. */
@Composable
internal fun RoadSignsTaskDiagram(ink: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        for (x in listOf(.20f, .8f)) drawLine(ink, Offset(w * x, h * .1f), Offset(w * x, h * .9f), 4.dp.toPx())
        drawLine(ink.copy(alpha = .6f), Offset(w * .5f, h * .1f), Offset(w * .5f, h * .9f), 4.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx())))
        drawPath(Path().apply {
            moveTo(w * .66f, h * .79f); lineTo(w * .66f, h * .3f)
            moveTo(w * .57f, h * .43f); lineTo(w * .66f, h * .27f); lineTo(w * .75f, h * .43f)
        }, ink, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
