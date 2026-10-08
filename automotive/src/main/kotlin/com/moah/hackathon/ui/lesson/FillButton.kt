package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachColors

/** One press/release implementation for every primary action, including the H8 start. */
@Composable
internal fun FillButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, home: Boolean = false, inverse: Boolean = false,
    interactions: MutableInteractionSource = remember { MutableInteractionSource() }) {
    val pressed by interactions.collectIsPressedAsState()
    val progress by animateFloatAsState(if (pressed && enabled) 1f else 0f,
        tween(if (pressed) { if (home) CoachMotion.HomePressMillis else CoachMotion.PressMillis }
            else CoachMotion.ReleaseMillis, easing = if (!pressed) CoachMotion.Release else if (home) CoachMotion.HomeFill else CoachMotion.Fill), label = "fill-button")
    val shape = RoundedCornerShape(100)
    Box(modifier.alpha(if (enabled) 1f else .32f)
        .shadow(if (home || !enabled) 0.dp else 2.dp, shape).clip(shape)
        .background(if (home) CoachColors.Paper else if (inverse) CoachColors.Platinum else CoachColors.Ink)
        .clickable(interactionSource = interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .drawWithContent {
            if (!home) {
                val width = size.height + (size.width - size.height) * progress
                drawRoundRect(if (inverse) lerp(CoachColors.Paper, CoachColors.Platinum, progress) else lerp(CoachColors.Periwinkle, CoachColors.Jet, progress),
                    size = Size(width, size.height), cornerRadius = CornerRadius(size.height / 2))
            }
            drawContent()
            if (!enabled) drawRoundRect(CoachColors.Ink, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                cornerRadius = CornerRadius(size.height / 2), style = Stroke(2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx()))))
        }) {
        ButtonFace(label, home, progress, if (home || inverse) CoachColors.Ink else CoachColors.Paper)
        if (home) Box(Modifier.matchParentSize().clearAndSetSemantics {}.drawWithContent {
            val width = size.height + (size.width - size.height) * progress
            val radius = size.height / 2
            val reveal = Path().apply {
                addRoundRect(RoundRect(Rect(size.width - width, 0f, size.width, size.height), CornerRadius(radius)))
            }
            // First the circle blooms in place; then the same clipped face reaches the label.
            val bloom = (progress * 4f).coerceIn(0f, 1f)
            scale(bloom, bloom, Offset(size.width - radius, radius)) {
                clipPath(reveal) { drawRect(CoachColors.Ink); this@drawWithContent.drawContent() }
            }
        }) { ButtonFace(label, true, progress, CoachColors.Paper) }
    }
}

@Composable
private fun ButtonFace(label: String, home: Boolean, progress: Float, ink: Color) = BoxWithConstraints(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxSize().padding(start = if (home) 112.dp else 28.dp,
        end = if (home) 0.dp else if (maxWidth < 360.dp) 20.dp else 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (home) Arrangement.SpaceBetween else Arrangement.Start) {
        if (!home) {
            LessonText("→", 40, ink, modifier = Modifier.width(84.dp).clearAndSetSemantics {})
            Spacer(Modifier.weight(1f))
        }
        LessonText(label, if (home) 120 else 40, ink, bold = true, maxLines = 1)
        if (!home) Spacer(Modifier.weight(1f))
        if (home) Canvas(Modifier.fillMaxHeight().aspectRatio(1f).clearAndSetSemantics {}) {
            val stroke = 4.dp.toPx()
            if (ink == CoachColors.Ink) drawCircle(ink, size.minDimension / 2 - stroke, style = Stroke(stroke))
            rotate(45f * progress) {
                val s = size.minDimension
                drawPath(Path().apply {
                    moveTo(s * .3f, s * .7f); lineTo(s * .7f, s * .3f)
                    moveTo(s * .39f, s * .3f); lineTo(s * .7f, s * .3f); lineTo(s * .7f, s * .61f)
                }, ink, style = Stroke(12.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter))
            }
        }
    }
}
