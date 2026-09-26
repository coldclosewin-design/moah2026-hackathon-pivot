package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun BriefingScreen(line: String, subtitle: String?) {
    LessonFrame(subtitle) {
        Column(Modifier.weight(1f).padding(horizontal = 80.dp), verticalArrangement = Arrangement.Center) {
            LessonText("연습을 시작할게요", 40, CoachColors.Accent)
            Spacer(Modifier.height(40.dp))
            LessonText(line, 88, bold = true)
            Spacer(Modifier.height(48.dp))
            LessonText("서두르지 않아도 괜찮아요.", 40, CoachColors.Muted)
        }
    }
}
