package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

/** Shared category/mode/booking track; callers keep their own hierarchy and palette. */
@Composable
internal fun <T> SelectionTrack(items: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit,
    modifier: Modifier = Modifier, height: Dp = 88.dp, textSize: Int = 40,
    enabled: (T) -> Boolean = { true }, role: Role = Role.RadioButton,
    background: Color = CoachColors.Lavender, selectedBackground: Color = CoachColors.Periwinkle,
    foreground: Color = CoachColors.Periwinkle, selectedForeground: Color = CoachColors.Paper,
    itemContent: (@Composable RowScope.(T, Color) -> Unit)? = null) {
    BoxWithConstraints(modifier.height(height)) {
        // Short tracks retain the cell width of a three-choice track, including its gutters.
        val cells = items.size.coerceAtLeast(3)
        val cellWidth = (maxWidth - 20.dp - 12.dp * (cells - 1)) / cells
        val trackWidth = 20.dp + cellWidth * items.size + 12.dp * (items.size - 1).coerceAtLeast(0)
        Row(Modifier.width(trackWidth).fillMaxHeight().background(background, RoundedCornerShape(100)).selectableGroup()
            .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                val chosen = item == selected
                val available = enabled(item)
                val color = when { !available -> CoachColors.Ink.copy(alpha = .35f); chosen -> selectedForeground; else -> foreground }
                Row(Modifier.weight(1f).fillMaxHeight()
                    .then(if (chosen && available) Modifier.surfaceTexture(selectedBackground, CoachTexture.SelectedChip, pill = true) else Modifier)
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
