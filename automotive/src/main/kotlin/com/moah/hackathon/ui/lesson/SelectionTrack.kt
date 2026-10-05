package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
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
    Row(modifier.height(height).background(background, RoundedCornerShape(100)).selectableGroup()
        .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            val chosen = item == selected
            val available = enabled(item)
            val color = when { !available -> CoachColors.Muted; chosen -> selectedForeground; else -> foreground }
            Row(Modifier.weight(1f).fillMaxHeight()
                .then(if (chosen) Modifier.surfaceTexture(selectedBackground, CoachTexture.SelectedChip, pill = true) else Modifier)
                .clip(RoundedCornerShape(100))
                .then(if (available) Modifier.clickable(role = role) { onSelect(item) } else Modifier.semantics { disabled() })
                .semantics(mergeDescendants = true) { this.selected = chosen },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (itemContent != null) itemContent(item, color)
                else LessonText(label(item), textSize, color, maxLines = 1)
            }
        }
    }
}
