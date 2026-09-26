package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.LessonCanvas

@Composable
internal fun LessonText(text: String, size: Int = 36, color: Color = CoachColors.Foreground,
    bold: Boolean = false, modifier: Modifier = Modifier) {
    Text(text, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.3f).sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        style = TextStyle(localeList = LocaleList("ko-KR"),
            lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)))
}

@Composable
internal fun LessonFrame(subtitle: String?, demo: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    LessonCanvas {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
            if (demo != null) Box(Modifier.width(490.dp)) { demo() }
        }
        Surface(color = CoachColors.Panel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 32.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                LessonText("조수석", 32, CoachColors.Accent, modifier = Modifier.width(130.dp).padding(top = 8.dp))
                // No ellipsis or line cap: the entire spoken sentence remains accessible (including four lines).
                LessonText(subtitle?.takeIf { it.isNotBlank() } ?: "화내지 않는 조수석, 함께 연습해요.", 42,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun LessonCard(modifier: Modifier = Modifier, padding: Int = 28, spacing: Int = 16,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = CoachColors.Panel, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CoachColors.Outline)) {
        Column(Modifier.padding(padding.dp), verticalArrangement = Arrangement.spacedBy(spacing.dp), content = content)
    }
}

@Composable
internal fun LessonButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    Button(onClick, modifier.heightIn(min = 80.dp), shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) CoachColors.Accent else CoachColors.Panel,
            contentColor = if (primary) CoachColors.Ink else CoachColors.Foreground),
        border = if (primary) null else BorderStroke(2.dp, CoachColors.Outline),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp)) {
        LessonText(label, 34, if (primary) CoachColors.Ink else CoachColors.Foreground, bold = true)
    }
}
