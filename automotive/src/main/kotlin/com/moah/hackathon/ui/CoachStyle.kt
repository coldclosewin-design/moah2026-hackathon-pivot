package com.moah.hackathon.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** P2: flat faces; only selected controls carry one shallow shadow. */
internal object CoachTexture {
    data class Surface(val elevation: Int)
    val Button = Surface(2)
    val Chip = Surface(0)
    val SelectedChip = Surface(2)
    val Card = Surface(0)
    val SelectedCard = Surface(2)
    val Panel = Surface(0)
}

/** 세션의 모든 단계 화면이 같은 팔레트를 쓴다. 값은 Codex 가 화면을 만들며 바꿀 수 있다. */
internal object CoachColors {
    val Ink = Color(0xFF222526)
    val Paper = Color(0xFFFFFFFF)
    val Periwinkle = Color(0xFF353A3E)
    val Lavender = Color(0xFFF2F2F2)
    val Signal = Color(0xFFF52D48)
    val Muted = Color(0xFF6E7071)
    val Platinum = Color(0xFFE0E0E0)
    val Jet = Color(0xFF1A1A1A)

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
