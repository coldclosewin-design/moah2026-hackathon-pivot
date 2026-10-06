package com.moah.hackathon.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Round 13 B: design-space surface effects, never applied to diagrams or driving locks. */
internal object CoachTexture {
    data class Surface(val shadow: Color, val y: Int, val blur: Int,
        val highlight: Float, val highlightHeight: Float, val inset: Float)
    val Button = Surface(CoachColors.Signal.copy(alpha = .28f), 10, 22, .42f, .46f, .18f)
    val Chip = Surface(CoachColors.Ink.copy(alpha = .10f), 3, 8, .42f, .46f, .06f)
    val SelectedChip = Chip.copy(shadow = CoachColors.Periwinkle.copy(alpha = .10f), highlight = .14f)
    val Card = Surface(CoachColors.Ink.copy(alpha = .10f), 6, 16, .42f, .38f, .06f)
    val SelectedCard = Card.copy(highlight = .14f)
    val Panel = Surface(CoachColors.Ink.copy(alpha = .22f), 12, 28, .12f, 0f, 0f)
    const val HighlightInset = .08f
    // Leave the central text on the original colour (D6). Only the edge carries the droplet.
    const val HighlightFade = .45f
    const val PressedShadow = .5f
    val InnerDepth = 8.dp
    val PanelHighlight = 2.dp
}

/** 세션의 모든 단계 화면이 같은 팔레트를 쓴다. 값은 Codex 가 화면을 만들며 바꿀 수 있다. */
internal object CoachColors {
    val Ink = Color(0xFF070827)
    val Paper = Color(0xFFFCFCFA)
    val Periwinkle = Color(0xFF5B60A1)
    val Lavender = Color(0xFFE5E6F0)
    val Signal = Color(0xFFF52D48)
    val Muted = Ink.copy(alpha = .60f)

    // Semantic road paint: confined to the parked RoadFigure illustration, never UI controls.
    val RoadMarkingYellow = Color(0xFFF4CC42)
    val RoadMarkingBlue = Color(0xFF398BEB)

}

/** Design-space sizes; DesignScale fixes the canvas at 2560 × 1268 dp. */
internal object CoachType {
    const val Eyebrow = 32
    const val Body = 40
    const val Headline = 72
    const val Value = 80
}

internal fun coachColorScheme() = lightColorScheme(
    primary = CoachColors.Periwinkle, onPrimary = CoachColors.Paper,
    background = CoachColors.Paper, onBackground = CoachColors.Ink,
    surface = CoachColors.Paper, onSurface = CoachColors.Ink,
    surfaceVariant = CoachColors.Lavender, onSurfaceVariant = CoachColors.Ink,
    outline = CoachColors.Muted,
)
