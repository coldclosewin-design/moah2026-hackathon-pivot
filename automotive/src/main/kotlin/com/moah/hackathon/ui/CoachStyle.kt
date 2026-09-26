package com.moah.hackathon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    val Background = Color(0xFF111916)
    val Panel = Color(0xFF1B2620)
    val Foreground = Color(0xFFF1F3E8)
    val Muted = Color(0xFFB2C0B7)
    val Accent = Color(0xFFD4EEAE)
    val Outline = Color(0xFF405148)
    val Warning = Color(0xFFFF887B)
    val Simulated = Color(0xFFE7C58C)
    val Ink = Color(0xFF172018)
}

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
