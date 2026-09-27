package com.moah.hackathon.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** 세션의 모든 단계 화면이 같은 팔레트를 쓴다. 값은 Codex 가 화면을 만들며 바꿀 수 있다. */
internal object CoachColors {
    val Ink = Color(0xFF070827)
    val Paper = Color(0xFFFCFCFA)
    val Periwinkle = Color(0xFF5B60A1)
    val Lavender = Color(0xFFE5E6F0)
    val Signal = Color(0xFFF52D48)
    val Muted = Ink.copy(alpha = .60f)

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
