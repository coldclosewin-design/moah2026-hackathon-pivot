package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.LessonCanvas
import com.moah.hackathon.ui.CoachType
import com.moah.hackathon.vehicle.SignalAvailability

@Composable
internal fun PosterSurface(content: @Composable () -> Unit) {
    val offset = remember { Animatable(180f) }
    val density = LocalDensity.current.density
    LaunchedEffect(Unit) { offset.animateTo(0f, tween(260)) }
    // Only the incoming composition moves. Never retain outgoing score text or touch targets.
    Box(Modifier.fillMaxSize().background(CoachColors.Paper).clipToBounds()) {
        Surface(Modifier.fillMaxSize().graphicsLayer { translationX = offset.value * density },
            color = CoachColors.Paper, content = content)
    }
}

@Composable
internal fun LessonText(text: String, size: Int = 36, color: Color = CoachColors.Foreground,
    bold: Boolean = false, modifier: Modifier = Modifier) {
    Text(text, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.3f).sp,
        fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
        style = TextStyle(localeList = LocaleList("ko-KR"),
            lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)))
}

@Composable
internal fun BrandMark(modifier: Modifier = Modifier, color: Color = CoachColors.Ink) {
    Text("DRIVE COACH", modifier, color = color, fontSize = 32.sp,
        letterSpacing = 6.sp, fontWeight = FontWeight.Normal)
}

@Composable
internal fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = CoachColors.Ink) =
    LessonText(text, CoachType.Eyebrow, color, modifier = modifier)

@Composable
internal fun Headline(text: String, modifier: Modifier = Modifier, size: Int = CoachType.Headline,
    color: Color = CoachColors.Ink) {
    // Never ellipsize speech. Explicit four-line subtitles remain complete in the accessibility tree.
    Text(text, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.18f).sp,
        fontWeight = FontWeight.Normal, style = TextStyle(localeList = LocaleList("ko-KR"),
            lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)))
}

@Composable
internal fun PrimaryPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick, modifier.heightIn(min = 120.dp), shape = RoundedCornerShape(100),
        colors = ButtonDefaults.buttonColors(containerColor = CoachColors.Signal, contentColor = CoachColors.Paper),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp)) {
        LessonText(label, 40, CoachColors.Paper, bold = true)
        Spacer(Modifier.width(32.dp))
        LessonText("→", 40, CoachColors.Paper)
    }
}

@Composable
internal fun TextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Int = 40) {
    Text(label, modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 16.dp),
        color = CoachColors.Ink, fontSize = size.sp, fontWeight = FontWeight.Normal,
        textDecoration = TextDecoration.Underline)
}

@Composable
internal fun StateLabel(text: String, availability: SignalAvailability, modifier: Modifier = Modifier,
    onInk: Boolean = false) {
    val color = when (availability) {
        SignalAvailability.LIVE -> if (onInk) CoachColors.Paper else CoachColors.Ink
        SignalAvailability.SIMULATED -> if (onInk) CoachColors.Lavender else CoachColors.Periwinkle
        SignalAvailability.MISSING -> if (onInk) CoachColors.Paper.copy(alpha = .60f) else CoachColors.Muted
    }
    Eyebrow(text, modifier, color)
}

@Composable
internal fun PosterRule(modifier: Modifier = Modifier) =
    HorizontalDivider(modifier, thickness = 2.dp, color = CoachColors.Lavender)

@Composable
internal fun LessonFrame(subtitle: String?, demo: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    LessonCanvas {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
            if (demo != null) Box(Modifier.width(490.dp)) { demo() }
        }
        Surface(color = CoachColors.Panel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 32.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                LessonText("조수석", 32, CoachColors.Accent, modifier = Modifier.width(130.dp).padding(top = 8.dp))
                // No ellipsis or line cap: the entire spoken sentence remains accessible (including four lines).
                LessonText(subtitle?.takeIf { it.isNotBlank() } ?: "화내지 않는 조수석, 함께 연습해요.", 42,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun LessonCard(modifier: Modifier = Modifier, padding: Int = 28, spacing: Int = 16,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = CoachColors.Panel, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CoachColors.Outline)) {
        Column(Modifier.padding(padding.dp), verticalArrangement = Arrangement.spacedBy(spacing.dp), content = content)
    }
}

@Composable
internal fun LessonButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    Button(onClick, modifier.heightIn(min = 80.dp), shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) CoachColors.Accent else CoachColors.Panel,
            contentColor = if (primary) CoachColors.Ink else CoachColors.Foreground),
        border = if (primary) null else BorderStroke(2.dp, CoachColors.Outline),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp)) {
        LessonText(label, 34, if (primary) CoachColors.Ink else CoachColors.Foreground, bold = true)
    }
}
