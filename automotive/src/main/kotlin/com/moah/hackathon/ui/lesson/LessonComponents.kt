package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
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
    DemoBrandMark(modifier, LocalDemoEscape.current, onInk = color == CoachColors.Paper)
}

@Composable
internal fun PlainBrandMark(modifier: Modifier = Modifier, color: Color = CoachColors.Ink) {
    Text("DRIVE COACH", modifier, color = color, fontSize = 32.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
internal fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = CoachColors.Ink) =
    LessonText(text, CoachType.Eyebrow, color, modifier = modifier
        .then(if (text == "코치" || text.startsWith("코치 ")) Modifier.padding(start = 4.dp) else Modifier))

@Composable
internal fun Headline(text: String, modifier: Modifier = Modifier, size: Int = CoachType.Headline,
    color: Color = CoachColors.Ink, weakTail: String? = null) {
    BoxWithConstraints(modifier) {
        val sizes = listOf(size, 64, 56).distinct().filter { it <= size }
        var step by remember(text, size, maxWidth, maxHeight) { mutableIntStateOf(0) }
        val fittedSize = sizes[step]
        val sentence = buildAnnotatedString {
            if (weakTail != null && text.endsWith(weakTail)) {
                append(text.removeSuffix(weakTail))
                withStyle(SpanStyle(color = CoachColors.Muted)) { append(weakTail) }
            } else append(text)
        }
        Text(sentence, color = color, fontSize = fittedSize.sp, lineHeight = (fittedSize * 1.18f).sp,
            maxLines = 3, onTextLayout = { result ->
                if (result.hasVisualOverflow && step < sizes.lastIndex) step++
            }, fontWeight = FontWeight.Medium, style = TextStyle(localeList = LocaleList("ko-KR"),
                lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)))
    }
}

/** Results can contain explicit line breaks and the full Cloud response; keep every line. */
@Composable
internal fun ResultHeadline(text: String, modifier: Modifier = Modifier) {
    CoachLines(text, modifier)
}

/** Keep one text node for speech/accessibility while drawing the title and advice at two sizes. */
@Composable
internal fun CoachLines(text: String, modifier: Modifier = Modifier, titleSize: Int = 88, adviceSize: Int = 48) {
    BoxWithConstraints(modifier) {
        val sizes = listOf(titleSize, 72, 64, 56, 48, 40, 32).distinct().filter { it <= titleSize }
        var step by remember(text, maxWidth, maxHeight) { mutableIntStateOf(0) }
        val size = sizes[step]
        val display = coachDisplayText(text).trim()
        val title = display.substringBefore('\n')
        val advice = display.substringAfter('\n', "")
        val styled = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(title) }
            if (advice.isNotEmpty()) {
                append('\n')
                withStyle(SpanStyle(color = CoachColors.Muted, fontWeight = FontWeight.Normal, fontSize = (adviceSize * size.toFloat() / titleSize).sp)) {
                    append(advice)
                }
            }
        }
        Text(styled, color = CoachColors.Ink, fontSize = size.sp, lineHeight = TextUnit.Unspecified,
            onTextLayout = { if (it.hasVisualOverflow && step < sizes.lastIndex) step++ },
            fontWeight = FontWeight.Normal, style = TextStyle(localeList = LocaleList("ko-KR"),
                lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)))
    }
}

/** Replace the result subtree so no score, scroll action or demo target survives the lock. */
@Composable
internal fun ResultLockedScreen(onDemoStop: (() -> Unit)? = null, message: String = "속도를 낮추면 결과가 다시 보여요.") {
    Column(Modifier.fillMaxSize().background(CoachColors.Ink)
        .padding(start = 120.dp, end = 100.dp, top = 64.dp, bottom = 52.dp)) {
        DemoBrandMark(onHold = onDemoStop ?: LocalDemoEscape.current, onInk = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            SymbolTile(CoachSymbol.Lock, Modifier.size(192.dp)
                .background(CoachColors.Periwinkle, RoundedCornerShape(56.dp)), animate = false, tile = false)
            Spacer(Modifier.height(32.dp))
            Headline("운전에 집중해 주세요", size = 120, color = CoachColors.Paper)
            Spacer(Modifier.height(32.dp))
            LessonText(message, 40, CoachColors.Paper.copy(alpha = .7f))
        }
        if (onDemoStop != null || LocalDemoEscape.current != null) LessonText("시연 · 막히면 워드마크를 길게",
            28, CoachColors.Paper.copy(alpha = .4f))
    }
}

@Composable
internal fun PrimaryPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    FillButton(label, onClick, modifier.width((2560 * .47f - 128).dp).height(140.dp), enabled)

/** Two thirds of the former result action, independent of the wider report column. */
@Composable
internal fun MainPill(onClick: () -> Unit, modifier: Modifier = Modifier) =
    PrimaryPill(androidx.compose.ui.res.stringResource(com.moah.hackathon.R.string.lesson_restart), onClick,
        modifier.width(((2560 * .47f - 128) * 2 / 3).dp))

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
    OutlinedButton(onClick, Modifier.height(112.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CoachColors.Ink),
        shape = RoundedCornerShape(100), interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(containerColor = CoachColors.Paper, contentColor = CoachColors.Ink),
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
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SymbolTile(when (availability) {
            SignalAvailability.LIVE -> CoachSymbol.Live
            SignalAvailability.SIMULATED -> CoachSymbol.Simulated
            SignalAvailability.MISSING -> CoachSymbol.Missing
        }, Modifier.size(40.dp), animate = false)
        Eyebrow(text, color = color)
    }
}

@Composable
internal fun PosterRule(modifier: Modifier = Modifier, color: Color = CoachColors.Platinum) =
    HorizontalDivider(modifier, thickness = 2.dp, color = color)

@Composable
internal fun SelectionChip(label: String, chosen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier.width(220.dp)) {
    Box(modifier.height(96.dp).surfaceTexture(if (chosen) CoachColors.Ink else CoachColors.Lavender,
        if (chosen) CoachTexture.SelectedChip else CoachTexture.Chip)
        .clickable(role = Role.RadioButton, onClick = onClick).semantics { selected = chosen }
        .padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        LessonText(label, 40, if (chosen) CoachColors.Paper else CoachColors.Periwinkle, maxLines = 1)
    }
}

@Composable
internal fun SpeechFooter(subtitle: String?) {
    if (!subtitle.isNullOrBlank()) {
        PosterRule()
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SymbolTile(CoachSymbol.Voice, Modifier.size(48.dp))
            LessonText(subtitle, 32, CoachColors.Muted)
        }
    }
}

/** Inline source shapes preserve the exact combined text used by accessibility and tools. */
@Composable
internal fun AvailabilitySummary(badge: com.moah.hackathon.vehicle.AvailabilityBadge, size: Int, onInk: Boolean = false) {
    val sources = listOf(CoachSymbol.Live to badge.live, CoachSymbol.Simulated to badge.simulated,
        CoachSymbol.Missing to badge.missing)
    val text = buildAnnotatedString {
        sources.forEachIndexed { index, (symbol, count) ->
            if (index > 0) append(" · ")
            appendInlineContent(symbol.name, symbol.label)
            append(" $count")
        }
    }
    val contents = sources.associate { (symbol, _) -> symbol.name to InlineTextContent(
        Placeholder(((symbol.label.length + 1.4f) * size).sp, (size * 1.3f).sp, PlaceholderVerticalAlign.Center)) {
        Row(Modifier.fillMaxSize().clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((size * .4f).dp)) {
            SignalShape(when (symbol) {
                CoachSymbol.Live -> SignalAvailability.LIVE
                CoachSymbol.Simulated -> SignalAvailability.SIMULATED
                else -> SignalAvailability.MISSING
            }, Modifier.size(size.dp), if (onInk) CoachColors.Platinum else CoachColors.Ink)
            LessonText(symbol.label, size, if (onInk) CoachColors.Platinum else CoachColors.Periwinkle, maxLines = 1)
        }
    } }
    Text(text, color = if (onInk) CoachColors.Platinum else CoachColors.Periwinkle, fontSize = size.sp, lineHeight = (size * 1.3f).sp,
        inlineContent = contents, style = TextStyle(localeList = LocaleList("ko-KR")))
}
