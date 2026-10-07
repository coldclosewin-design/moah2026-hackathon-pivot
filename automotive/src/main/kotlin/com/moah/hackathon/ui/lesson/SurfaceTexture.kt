package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachTexture
import com.moah.hackathon.ui.CoachColors

/** Flat Onyx faces, without gradients, highlights or inset shadows. */
internal fun Modifier.surfaceTexture(
    color: Color,
    texture: CoachTexture.Surface,
    pill: Boolean = false,
    shape: Shape? = null,
): Modifier {
    val face = shape ?: if (pill || texture === CoachTexture.Chip || texture === CoachTexture.SelectedChip) RoundedCornerShape(100) else RectangleShape
    val fill = if (texture === CoachTexture.Card && color == CoachColors.Paper) CoachColors.Lavender else color
    return this.shadow(texture.elevation.dp, face, clip = false).background(fill, face)
}
