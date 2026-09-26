package com.moah.hackathon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.R

/** 세션의 모든 단계 화면이 같은 팔레트를 쓴다. 값은 Codex 가 화면을 만들며 바꿀 수 있다. */
internal object CoachColors {
    val Ink = Color(0xFF070827)
    val Paper = Color(0xFFFCFCFA)
    val Periwinkle = Color(0xFF5B60A1)
    val Lavender = Color(0xFFE5E6F0)
    val Signal = Color(0xFFF52D48)
    val Muted = Ink.copy(alpha = .60f)

    // Compatibility roles for the screens whose composition changes in round 2.
    val Background = Paper
    val Panel = Lavender
    val Foreground = Ink
    val Accent = Periwinkle
    val Outline = Lavender
    val Warning = Muted
    val Simulated = Periwinkle
}

/** Design-space sizes; DesignScale fixes the canvas at 2560 × 1268 dp. */
internal object CoachType {
    const val Eyebrow = 32
    const val Body = 40
    const val Headline = 80
    const val Value = 80
}

internal fun coachColorScheme() = lightColorScheme(
    primary = CoachColors.Periwinkle, onPrimary = CoachColors.Paper,
    background = CoachColors.Paper, onBackground = CoachColors.Ink,
    surface = CoachColors.Paper, onSurface = CoachColors.Ink,
    surfaceVariant = CoachColors.Lavender, onSurfaceVariant = CoachColors.Ink,
    outline = CoachColors.Muted,
)

@Composable
internal fun LessonCanvas(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CoachColors.Background,
        contentColor = CoachColors.Foreground,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                stringResource(R.string.brand),
                color = CoachColors.Accent,
                fontSize = 32.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}
