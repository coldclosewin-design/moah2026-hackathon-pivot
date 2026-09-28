package com.moah.hackathon.ui.lesson

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.ui.CoachColors
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
internal fun LessonText(text: String, size: Int = 36, color: Color = CoachColors.Ink,
    bold: Boolean = false, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, textAlign: TextAlign? = null) {
    Text(text, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.3f).sp,
        maxLines = maxLines, overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal, textAlign = textAlign,
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
    BoxWithConstraints(modifier) {
        val sizes = listOf(size, 64, 56).distinct().filter { it <= size }
        var step by remember(text, size, maxWidth, maxHeight) { mutableIntStateOf(0) }
        val fittedSize = sizes[step]
        Text(text, color = color, fontSize = fittedSize.sp, lineHeight = (fittedSize * 1.18f).sp,
            maxLines = 3, onTextLayout = { result ->
                if (result.hasVisualOverflow && step < sizes.lastIndex) step++
            }, fontWeight = FontWeight.Normal, style = TextStyle(localeList = LocaleList("ko-KR"),
                lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)))
    }
}

@Composable
internal fun PrimaryPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, driver: Boolean = false) {
    val dimensions = if (driver) Modifier.widthIn(min = 720.dp).height(140.dp) else Modifier.heightIn(min = 120.dp)
    Button(onClick, modifier.then(dimensions), shape = RoundedCornerShape(100),
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
internal fun PosterRule(modifier: Modifier = Modifier, color: Color = CoachColors.Lavender) =
    HorizontalDivider(modifier, thickness = 2.dp, color = color)

@Composable
internal fun SelectionChip(label: String, chosen: Boolean, onClick: () -> Unit) {
    Box(Modifier.width(220.dp).height(96.dp).background(if (chosen) CoachColors.Periwinkle else CoachColors.Lavender)
        .clickable(role = Role.RadioButton, onClick = onClick).semantics { selected = chosen }
        .padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        LessonText(label, 40, if (chosen) CoachColors.Paper else CoachColors.Ink, maxLines = 1)
    }
}

@Composable
internal fun BoxScope.DemoRail(expansion: MutableState<Boolean>, demo: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDemoExpansion provides expansion) {
        Box(Modifier.align(Alignment.TopEnd).padding(end = 8.dp)
            .width(if (expansion.value) 420.dp else 88.dp)
            .background(if (expansion.value) CoachColors.Lavender else Color.Transparent)
            .padding(start = if (expansion.value) 24.dp else 0.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)) { demo() }
    }
}

@Composable
internal fun SpeechFooter(subtitle: String?) {
    if (!subtitle.isNullOrBlank()) {
        PosterRule()
        Spacer(Modifier.height(20.dp))
        LessonText(subtitle, 32, CoachColors.Muted)
    }
}
