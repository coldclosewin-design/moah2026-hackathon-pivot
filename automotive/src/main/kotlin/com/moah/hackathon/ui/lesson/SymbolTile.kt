package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import com.moah.hackathon.ui.CoachColors
import kotlin.math.PI
import kotlin.math.cos

internal enum class CoachSymbol(val label: String, val millis: Int) {
    Voice("음성 안내 중", 1000), Thinking("코치 생각 중", 1200), Loading("불러오는 중", 1000),
    Live("실신호", 1600), Simulated("시뮬레이션", 6000), Missing("미측정", 2400),
    Check("체크 완료", 700), Listen("듣는 중", 1200), Lock("잠금", 0),
    Proximity("가까워요", 900), Turn("방향지시등", 800), Hazard("비상등", 800), Power("시동", 2000)
}

/** Shape carries meaning even with motion disabled. Locks never start an animation clock. */
@Composable
internal fun SymbolTile(symbol: CoachSymbol, modifier: Modifier = Modifier, animate: Boolean = true,
    locked: Boolean = false, active: Boolean = true, tile: Boolean = true, onLight: Boolean = false) {
    val moving = animate && active && symbol.millis > 0 && (!locked || symbol == CoachSymbol.Voice) && selectionMotionEnabled()
    val once = symbol == CoachSymbol.Check || symbol == CoachSymbol.Power
    val progress = if (moving && once) {
        val value = remember(symbol) { Animatable(0f) }
        LaunchedEffect(value) { value.animateTo(1f, tween(symbol.millis, easing = LinearEasing)) }
        value.value
    } else if (moving) {
        val transition = rememberInfiniteTransition(label = "symbol-${symbol.name}")
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(symbol.millis, easing = LinearEasing)),
            label = "symbol-phase")
        value
    } else 1f
    Canvas(modifier.alpha(if (active) 1f else .4f).semantics { contentDescription = symbol.label }) {
        if (tile) drawRoundRect(if (symbol == CoachSymbol.Proximity) CoachColors.Signal else CoachColors.Ink,
            cornerRadius = CornerRadius(size.minDimension * .28f))
        val unit = size.minDimension / 64f
        withTransform({ translate(size.width / 2 - 20 * unit, size.height / 2 - 20 * unit); scale(unit, unit, Offset.Zero) }) {
            drawSymbol(symbol, progress, moving, if (onLight) CoachColors.Ink else CoachColors.Paper)
        }
    }
}

private fun DrawScope.drawSymbol(symbol: CoachSymbol, t: Float, moving: Boolean, white: Color) {
    val stroke = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun wave(offset: Float = 0f) = if (!moving) 1f else ((1 - cos(2 * PI * ((t - offset + 1) % 1))) / 2).toFloat()
    fun line(x: Float, y: Float, x2: Float, y2: Float, alpha: Float = 1f) =
        drawLine(white.copy(alpha = alpha), Offset(x, y), Offset(x2, y2), 2.6f, StrokeCap.Round)
    fun path(vararg points: Pair<Float, Float>, alpha: Float = 1f, fill: Boolean = false) {
        drawPath(Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second) } },
            white.copy(alpha = alpha), style = if (fill) Fill else stroke)
    }
    when (symbol) {
        CoachSymbol.Voice -> repeat(5) { i ->
            val h = (if (i % 2 == 0) 17f else 27f) * (.3f + .7f * wave(i * .12f))
            drawRoundRect(white, Offset(5f + i * 7f, 20f - h / 2), Size(4f, h), CornerRadius(2f))
        }
        CoachSymbol.Thinking -> repeat(3) { i ->
            val phase = (t - i * .125f + 1) % 1
            val bounce = if (moving && phase < .6f) kotlin.math.sin(phase / .6f * PI).toFloat() * 5 else 0f
            drawCircle(white, 3f, Offset(10f + i * 10, 22f - bounce))
        }
        CoachSymbol.Loading -> rotate(t * 360, Offset(20f, 20f)) {
            drawArc(white, -80f, 270f, false, Offset(7f, 7f), Size(26f, 26f), style = stroke)
        }
        CoachSymbol.Live -> {
            drawCircle(white, 4.5f, Offset(20f, 20f))
            if (moving) repeat(2) { i ->
                val phase = (t + i * .5f) % 1
                drawCircle(white.copy(alpha = .9f * (1 - phase)), 4.5f + 10.5f * CoachMotion.Fill.transform(phase),
                    Offset(20f, 20f), style = stroke)
            }
        }
        CoachSymbol.Simulated -> {
            drawCircle(white, 7f, Offset(20f, 20f), style = stroke)
            drawArc(white, 90f, 180f, true, Offset(13f, 13f), Size(14f, 14f))
            rotate(t * 360, Offset(20f, 20f)) {
                drawCircle(white, 13.5f, Offset(20f, 20f), style = Stroke(1.7f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4.2f))))
            }
        }
        CoachSymbol.Missing -> {
            val alpha = if (moving) .35f + .55f * wave() else 1f
            drawCircle(white.copy(alpha = alpha), 9f, Offset(20f, 20f), style = Stroke(2.6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3.2f))))
            line(16f, 20f, 24f, 20f, alpha)
        }
        CoachSymbol.Check -> {
            drawArc(white, -90f, 360f * (t * 700 / 400).coerceIn(0f, 1f), false, Offset(7f, 7f), Size(26f, 26f), style = stroke)
            val check = Path().apply { moveTo(13.5f, 20.5f); lineTo(18f, 25f); lineTo(26.5f, 16f) }
            val measure = PathMeasure().apply { setPath(check, false) }
            val segment = Path()
            measure.getSegment(0f, measure.length * ((t * 700 - 400) / 300).coerceIn(0f, 1f), segment)
            drawPath(segment, white, style = stroke)
        }
        CoachSymbol.Listen -> {
            drawRoundRect(white, Offset(16f, 7f), Size(8f, 15f), CornerRadius(4f), style = stroke)
            drawArc(white, 0f, 180f, false, Offset(12f, 10f), Size(16f, 16f), style = stroke)
            line(20f, 26f, 20f, 31f)
            repeat(2) { i ->
                val alpha = if (moving) 1 - (t + i * .25f) % 1 else .7f
                drawArc(white.copy(alpha = alpha), -40f, 80f, false, Offset(8f - i * 5, 8f - i * 5),
                    Size(24f + i * 10, 24f + i * 10), style = stroke)
                drawArc(white.copy(alpha = alpha), 140f, 80f, false, Offset(8f - i * 5, 8f - i * 5),
                    Size(24f + i * 10, 24f + i * 10), style = stroke)
            }
        }
        CoachSymbol.Lock -> {
            drawRoundRect(white, Offset(14f, 8f), Size(12f, 19f), CornerRadius(6f), style = stroke)
            drawRoundRect(white, Offset(10.5f, 19f), Size(19f, 13f), CornerRadius(3.5f))
            drawCircle(CoachColors.Ink, 2f, Offset(20f, 25.5f))
        }
        CoachSymbol.Proximity -> {
            line(9f, 7f, 31f, 7f)
            repeat(3) { i ->
                val alpha = if (moving) 1 - (t + i / 6f) % 1 else 1f
                drawArc(white.copy(alpha = alpha), 220f, 100f, false, Offset(11.5f + i, 19f - i * 5),
                    Size(17f - i * 2, 9f), style = stroke)
            }
            drawRoundRect(white, Offset(12f, 26f), Size(16f, 8f), CornerRadius(3f))
        }
        CoachSymbol.Turn -> path(21f to 10f, 10f to 20f, 21f to 30f, 21f to 24f, 30f to 24f,
            30f to 16f, 21f to 16f, 21f to 10f, fill = true, alpha = if (moving && t >= .5f) .2f else 1f)
        CoachSymbol.Hazard -> {
            val alpha = if (moving && t >= .5f) .2f else 1f
            path(20f to 7f, 34f to 31f, 6f to 31f, 20f to 7f, alpha = alpha)
            path(20f to 15.5f, 27f to 27.5f, 13f to 27.5f, 20f to 15.5f, alpha = alpha)
        }
        CoachSymbol.Power -> {
            drawArc(white, -50f, 280f * (t * 4).coerceAtMost(1f), false, Offset(10f, 10f), Size(20f, 20f), style = stroke)
            line(20f, 8.5f, 20f, 8.5f + 10f * (t * 4).coerceAtMost(1f))
            if (moving && t in .25f.. .6f) drawCircle(white.copy(alpha = (1 - (t - .25f) / .35f).coerceIn(0f, 1f)),
                16f, Offset(20f, 20f), style = stroke)
        }
    }
}
