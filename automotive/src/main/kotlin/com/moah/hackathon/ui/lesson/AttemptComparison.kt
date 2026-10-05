package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.LessonReport
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

/** Keep the latest pair together, with explicit access to every earlier attempt. */
@Composable
internal fun AttemptComparison(attempts: List<AttemptRecord>, modifier: Modifier, footer: @Composable () -> Unit = {},
    content: @Composable ColumnScope.(AttemptRecord, AttemptRecord?) -> Unit) {
    var end by remember(attempts) { mutableIntStateOf(attempts.lastIndex) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (attempts.size > 2) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (end > 1) TextAction("이전 회차", { end-- }, size = 32)
            if (end < attempts.lastIndex) TextAction("다음 회차", { end++ }, size = 32)
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            attempts.withIndex().filter { it.index in (end - 1).coerceAtLeast(0)..end }.forEachIndexed { position, (index, attempt) ->
                if (position > 0) LessonText("→", 40, CoachColors.Periwinkle)
                Column(Modifier.weight(1f).fillMaxHeight().surfaceTexture(CoachColors.Paper, CoachTexture.Card).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    content(attempt, attempts.getOrNull(index - 1).takeIf { position > 0 })
                }
            }
        }
        footer()
    }
}

@Composable
internal fun DeltaChip(delta: Int, unit: String = "", lowerBetter: Boolean = false) {
    LessonText("${if (delta > 0) "+" else if (delta < 0) "−" else ""}${kotlin.math.abs(delta)}$unit", 32,
        if (delta == 0 || (delta > 0) != lowerBetter) CoachColors.Periwinkle else CoachColors.Signal,
        modifier = Modifier.background(CoachColors.Lavender, RoundedCornerShape(100)).padding(horizontal = 20.dp, vertical = 4.dp))
}

@Composable
internal fun ParkingDetails(report: LessonReport, modifier: Modifier) {
    AttemptComparison(report.attempts, modifier, footer = {
        LessonText("다음엔 ${report.nextTask.title} · ${report.nextMode.label} — ${report.nextReason}", 36)
        ReportLimitations(report)
    }) { attempt, previous ->
        Eyebrow("${attempt.index}회차", color = CoachColors.Periwinkle)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f)) {
                Row(Modifier.height(56.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("숙련")
                    previous?.let { DeltaChip(attempt.score.skill - it.score.skill) }
                }
                LessonText(attempt.score.skill.toString(), 80, CoachColors.Periwinkle)
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.height(56.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("안전")
                    previous?.let { DeltaChip(attempt.score.safety - it.score.safety) }
                }
                LessonText(attempt.score.safety.toString(), 80, CoachColors.Periwinkle)
            }
        }
        PosterRule()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            LessonText("이동 ${attempt.score.metrics.motion.movingSegments}회", 36)
            previous?.let { DeltaChip(attempt.score.metrics.motion.movingSegments - it.score.metrics.motion.movingSegments, "회", true) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            LessonText("${attempt.score.metrics.motion.totalMillis / 1000}초", 36)
            previous?.let { DeltaChip((attempt.score.metrics.motion.totalMillis / 1000 - it.score.metrics.motion.totalMillis / 1000).toInt(), "초", true) }
        }
        LessonText(parkingDetailLine(attempt.score.metrics, report.task.parkingSpec.usesRearDistance), 32, CoachColors.Muted)
        LessonText(headingDetailLine(attempt.verdict), 32, CoachColors.Muted)
    }
}
