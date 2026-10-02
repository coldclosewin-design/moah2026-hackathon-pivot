package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moah.hackathon.scoring.ParkingVerdict
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun VerdictPanel(verdict: ParkingVerdict?, modifier: Modifier = Modifier, title: String? = null) {
    Column(modifier.background(CoachColors.Ink).padding(start = 120.dp, end = 72.dp, top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (title != null) Eyebrow(title, color = CoachColors.Paper.copy(alpha = .6f))
        verdictLines(verdict).forEach { line ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    LessonText(line.text, 40, CoachColors.Paper)
                    line.note?.let { LessonText(it, 32, CoachColors.Paper.copy(alpha = .6f)) }
                }
                // Muted is Ink at 60%; a light backing keeps an unmeasured mark legible on Ink.
                Box(Modifier.size(56.dp).then(if (line.mark == VerdictMark.MISSING)
                    Modifier.background(CoachColors.Lavender) else Modifier), contentAlignment = Alignment.Center) {
                    LessonText(line.mark.symbol, 40, when (line.mark) {
                        VerdictMark.PASS -> CoachColors.Periwinkle
                        VerdictMark.CAUTION -> CoachColors.Paper.copy(alpha = .6f)
                        VerdictMark.FAIL -> CoachColors.Signal
                        VerdictMark.MISSING -> CoachColors.Muted
                    })
                }
            }
        }
    }
}
