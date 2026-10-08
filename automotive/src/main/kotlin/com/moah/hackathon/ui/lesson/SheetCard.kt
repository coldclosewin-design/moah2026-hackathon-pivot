package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachColors

/** C1: a second sheet at .965 scale, then the foreground rises in 420 ms. */
@Composable
internal fun SheetCard(modifier: Modifier = Modifier, dark: Boolean = false,
    content: @Composable BoxScope.() -> Unit) {
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(Unit) { arrival.animateTo(1f, tween(420, easing = CoachMotion.Fill)) }
    Box(modifier.graphicsLayer {
        translationY = (1f - arrival.value) * 180.dp.toPx()
        alpha = arrival.value
    }) {
        Box(Modifier.matchParentSize().graphicsLayer { scaleX = .965f; scaleY = .965f; translationY = -24.dp.toPx() }
            .background(CoachColors.Paper.copy(alpha = .6f), RoundedCornerShape(48.dp)))
        Box(Modifier.fillMaxSize().shadow(8.dp, RoundedCornerShape(48.dp))
            .background(if (dark) CoachColors.Ink else CoachColors.Paper, RoundedCornerShape(48.dp))
            .clip(RoundedCornerShape(48.dp)), content = content)
    }
}

@Composable
internal fun YellowModeIcon(modifier: Modifier = Modifier, bars: Int = 2, conversation: Boolean = false) {
    Canvas(modifier.size(64.dp).background(CoachColors.Accent, CircleShape).clearAndSetSemantics {}) {
        if (conversation) repeat(3) { i ->
            drawCircle(CoachColors.Ink, size.width * .055f, Offset(size.width * (.3f + i * .2f), size.height * .5f))
        } else repeat(3) { i ->
            val x = size.width * (.30f + i * .20f)
            drawLine(CoachColors.Ink.copy(alpha = if (i < bars) 1f else .25f),
                Offset(x, size.height * .70f), Offset(x, size.height * (.53f - i * .12f)), size.width * .085f)
        }
    }
}

@Composable
internal fun WordPill(label: String, description: String, size: Int, chosen: Boolean = false, placeholder: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.then(when (description) {
        "과제 바꾸기" -> Modifier.homeShared("home-task")
        "모드 바꾸기" -> Modifier.homeShared("home-mode")
        else -> Modifier
    }).heightIn(min = 112.dp).shadow(if (placeholder) 0.dp else 3.dp, RoundedCornerShape(30.dp))
        .background(if (chosen) CoachColors.Ink else CoachColors.Paper, RoundedCornerShape(30.dp))
        .drawWithContent {
            drawContent()
            if (placeholder) drawRoundRect(CoachColors.Muted, cornerRadius = CornerRadius(30.dp.toPx()),
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()))))
        }
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }
        .padding(horizontal = 32.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        LessonText(label, size, if (chosen) CoachColors.Paper else CoachColors.Ink, bold = true, maxLines = 1)
    }
}

@Composable
internal fun ArrowPill(label: String, arrow: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    dark: Boolean = false) {
    val foreground = if (dark) CoachColors.Paper else CoachColors.Ink
    Row(modifier.height(120.dp).border(1.5.dp, foreground.copy(alpha = .4f), CircleShape)
        .background(if (dark) CoachColors.Ink else CoachColors.Paper, CircleShape)
        .clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
        .padding(start = 12.dp, end = 40.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(Modifier.size(88.dp).background(if (dark) CoachColors.Platinum else CoachColors.Lavender, CircleShape),
            contentAlignment = Alignment.Center) { LessonText(arrow, 44, CoachColors.Ink) }
        LessonText(label, 40, foreground, maxLines = 1)
    }
}
