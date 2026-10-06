package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
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
import com.moah.hackathon.ui.CoachTexture
import com.moah.hackathon.vehicle.SignalAvailability

@Composable
internal fun PosterSurface(band: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    val offset = remember { Animatable(180f) }
    val density = LocalDensity.current.density
    LaunchedEffect(Unit) { offset.animateTo(0f, tween(260)) }
    // Only the incoming composition moves. Never retain outgoing score text or touch targets.
    Box(Modifier.fillMaxSize().background(CoachColors.Paper).clipToBounds()) {
        Column(Modifier.fillMaxSize()) {
            Surface(Modifier.weight(1f).fillMaxWidth().graphicsLayer { translationX = offset.value * density },
                color = CoachColors.Paper, content = content)
            band?.invoke()
        }
    }
}

@Composable
internal fun LessonText(text: String, size: Int = 36, color: Color = CoachColors.Ink,
    bold: Boolean = false, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, textAlign: TextAlign? = null) =
    LessonText(AnnotatedString(text), size, color, bold, modifier, maxLines, textAlign)

@Composable
internal fun LessonText(text: AnnotatedString, size: Int = 36, color: Color = CoachColors.Ink,
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

/** Results can contain explicit line breaks and the full Cloud response; keep every line. */
@Composable
internal fun ResultHeadline(text: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val sizes = listOf(72, 64, 56, 48, 40, 32)
        var step by remember(text, maxWidth, maxHeight) { mutableIntStateOf(0) }
        val size = sizes[step]
        Text(text, color = CoachColors.Ink, fontSize = size.sp, lineHeight = (size * 1.18f).sp,
            onTextLayout = { if (it.hasVisualOverflow && step < sizes.lastIndex) step++ },
            fontWeight = FontWeight.Normal, style = TextStyle(localeList = LocaleList("ko-KR"),
                lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)))
    }
}

/** Replace the result subtree so no score, scroll action or demo target survives the lock. */
@Composable
internal fun ResultLockedScreen() {
    Column(Modifier.fillMaxSize().background(CoachColors.Ink)
        .padding(start = 180.dp, end = 100.dp, top = 64.dp, bottom = 52.dp)) {
        BrandMark(color = CoachColors.Paper)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Headline("운전에 집중해 주세요", color = CoachColors.Paper)
            Spacer(Modifier.height(32.dp))
            LessonText("속도를 낮추면 결과가 다시 보여요.", 40, CoachColors.Paper.copy(alpha = .7f))
        }
    }
}

@Composable
internal fun PrimaryPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Setup summary: 47% of the 2560 dp canvas, minus its two 64 dp margins.
    val dimensions = Modifier.widthIn(min = (2560 * .47f - 128).dp).height(140.dp)
    val interactions = remember { MutableInteractionSource() }
    val pressed = interactions.collectIsPressedAsState()
    Button(onClick, modifier.then(dimensions).surfaceTexture(CoachColors.Signal, CoachTexture.Button,
        pill = true, pressed = { pressed.value }), shape = RoundedCornerShape(100), interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CoachColors.Paper),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp)) {
        LessonText(label, 40, CoachColors.Paper, bold = true)
        Spacer(Modifier.width(32.dp))
        LessonText("→", 40, CoachColors.Paper)
    }
}

/** Measure the secondary first; a constrained primary yields its preferred minimum to the gap. */
@Composable
internal fun BottomActions(secondary: (@Composable () -> Unit)? = null, primary: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier, alignment: Alignment.Vertical = Alignment.CenterVertically) {
    Row(modifier.fillMaxWidth().heightIn(min = 140.dp), verticalAlignment = alignment) {
        if (secondary != null) {
            Box { secondary() }
            if (primary != null) Spacer(Modifier.width(64.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { primary?.invoke() }
    }
}

@Composable
internal fun BackPill(onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Button(onClick, Modifier.height(112.dp).surfaceTexture(CoachColors.Lavender, CoachTexture.Chip,
        pill = true, pressed = { pressed }, insetWhenPressed = true),
        shape = RoundedCornerShape(100), interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CoachColors.Ink),
        contentPadding = PaddingValues(horizontal = 48.dp)) {
        LessonText("←", 40)
        Spacer(Modifier.width(24.dp))
        LessonText("돌아가기", 40)
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
internal fun SelectionChip(label: String, chosen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier.width(220.dp)) {
    Box(modifier.height(96.dp).surfaceTexture(if (chosen) CoachColors.Periwinkle else CoachColors.Lavender,
        if (chosen) CoachTexture.SelectedChip else CoachTexture.Chip)
        .clickable(role = Role.RadioButton, onClick = onClick).semantics { selected = chosen }
        .padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        LessonText(label, 40, if (chosen) CoachColors.Paper else CoachColors.Ink, maxLines = 1)
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
