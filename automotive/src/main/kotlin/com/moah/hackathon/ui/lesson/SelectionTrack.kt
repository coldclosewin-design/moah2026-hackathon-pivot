package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachColors

/** Screenshot fixtures snap; the explicit frame-clock fixture exercises the production motion. */
internal val LocalSelectionMotion = staticCompositionLocalOf { true }

@Composable
internal fun selectionMotionEnabled() = LocalSelectionMotion.current && !LocalInspectionMode.current

/** Shared category/mode/booking track; callers keep their own hierarchy and palette. */
@Composable
internal fun <T> SelectionTrack(items: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit,
    modifier: Modifier = Modifier, height: Dp = 88.dp, textSize: Int = 40,
    enabled: (T) -> Boolean = { true }, role: Role = Role.RadioButton,
    background: Color = CoachColors.Lavender, selectedBackground: Color = CoachColors.Ink,
    foreground: Color = CoachColors.Periwinkle, selectedForeground: Color = CoachColors.Paper,
    itemContent: (@Composable RowScope.(T, Color) -> Unit)? = null, smoothCategory: Boolean = false) {
    BoxWithConstraints(modifier.height(height)) {
        // Short tracks retain the cell width of a three-choice track, including its gutters.
        val layoutWidth = maxWidth
        val cells = items.size.coerceAtLeast(3)
        val cellWidth = (maxWidth - 20.dp - 12.dp * (cells - 1)) / cells
        val trackWidth = 20.dp + cellWidth * items.size + 12.dp * (items.size - 1).coerceAtLeast(0)
        // Content widths share the remaining space; the selected face follows each label's bay.
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val natural = items.map { with(density) {
            measurer.measure(AnnotatedString(label(it)), TextStyle(fontSize = textSize.sp), softWrap = false).size.width.toDp()
        } }
        val average = if (natural.isEmpty()) 0.dp else natural.reduce { a, b -> a + b } / natural.size
        val widths = natural.map { cellWidth + (it - average) }
        val starts = widths.indices.map { index -> 10.dp + widths.take(index).fold(0.dp) { a, b -> a + b } + 12.dp * index }
        val motion = selectionMotionEnabled()
        val duration = if (!motion) 0 else if (smoothCategory) 340 else CoachMotion.SelectMillis
        val easing = if (smoothCategory) CoachMotion.Category else CoachMotion.Stone
        val selectedIndex = items.indexOf(selected).takeIf { it >= 0 && enabled(items[it]) }
        // Reset only animation state on resize, keeping the selectable semantics nodes alive.
        val geometry = key(layoutWidth, height, items) {
            val transition = updateTransition(selectedIndex, label = "selection-track")
            val left by transition.animateDp({ tween(if (initialState == null) 0 else duration,
                easing = easing) }, label = "left") { starts.getOrElse(it ?: 0) { 10.dp } }
            val width by transition.animateDp({ tween(if (initialState == null) 0 else duration,
                easing = easing) }, label = "width") { widths.getOrElse(it ?: 0) { 0.dp } }
            left to width
        }
        val left = if (motion) geometry.first else starts.getOrElse(selectedIndex ?: 0) { 10.dp }
        val width = if (motion) geometry.second else widths.getOrElse(selectedIndex ?: 0) { 0.dp }
        Box(Modifier.width(trackWidth).fillMaxHeight().background(background, RoundedCornerShape(100))) {
            if (selectedIndex != null) Box(Modifier.offset(x = left, y = 10.dp).width(width).height(height - 20.dp)
                .shadow(2.dp, RoundedCornerShape(100)).background(selectedBackground, RoundedCornerShape(100)).testTag("selection-track-face"))
        }
        Row(Modifier.width(trackWidth).fillMaxHeight().selectableGroup()
            .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEachIndexed { index, item ->
                val chosen = item == selected
                val available = enabled(item)
                val targetColor = when { !available -> CoachColors.Ink.copy(alpha = .35f); chosen -> selectedForeground; else -> foreground }
                val color = key(layoutWidth, items) {
                    val animatedColor by animateColorAsState(targetColor,
                        tween(if (smoothCategory && motion) 140 else duration, easing = easing), label = "selection-label")
                    if (motion) animatedColor else targetColor
                }
                Row(Modifier.width(widths[index]).fillMaxHeight()
                    .clip(RoundedCornerShape(100))
                    .then(if (available) Modifier.clickable(role = role) { onSelect(item) } else Modifier.semantics { disabled() })
                    .semantics(mergeDescendants = true) { this.selected = chosen && available },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (!available) {
                        LessonText(AnnotatedString(label(item), SpanStyle(textDecoration = TextDecoration.LineThrough)), textSize, color, maxLines = 1)
                        Spacer(Modifier.width(10.dp))
                        LessonText("마감", 24, CoachColors.Muted, modifier = Modifier
                            .border(1.dp, CoachColors.Ink.copy(alpha = .2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp), maxLines = 1)
                    } else if (itemContent != null) itemContent(item, color)
                    else LessonText(label(item), textSize, color, maxLines = 1)
                }
            }
        }
    }
}
