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
import com.moah.hackathon.scoring.HarshKind
import kotlin.math.roundToInt

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
        val metrics = attempt.score.metrics
        val before = previous?.score?.metrics
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailMetric("이동", metrics.motion.movingSegments, before?.motion?.movingSegments, "회")
            DetailMetric("시간", metrics.motion.totalMillis / 1000, before?.motion?.totalMillis?.div(1000), "초")
            DetailMetric("조향 왕복", metrics.steering?.reversals, before?.steering?.reversals)
            DetailMetric("기어 전환", metrics.gear?.reverseDriveShifts, before?.gear?.reverseDriveShifts)
            DetailMetric(if (report.task.parkingSpec.usesRearDistance) "근접" else "앞 근접",
                metrics.proximity?.warnings, before?.proximity?.warnings)
            DetailMetric("급정지", metrics.harshEvents.count { it.kind == HarshKind.BRAKING },
                before?.harshEvents?.count { it.kind == HarshKind.BRAKING })
            if (metrics.harshEvents.any { it.kind == HarshKind.ACCELERATION } ||
                before?.harshEvents?.any { it.kind == HarshKind.ACCELERATION } == true) {
                DetailMetric("급가속", metrics.harshEvents.count { it.kind == HarshKind.ACCELERATION },
                    before?.harshEvents?.count { it.kind == HarshKind.ACCELERATION })
            }
            DetailMetric("방향 편차", attempt.verdict?.headingErrorDeg?.roundToInt(),
                previous?.verdict?.headingErrorDeg?.roundToInt(), "°")
        }
    }
}

/** A value owns its width; labels and deltas cannot push a final digit onto another line. */
@Composable
private fun DetailMetric(label: String, value: Number?, previous: Number?, unit: String = "") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        LessonText(label, 32, CoachColors.Muted, modifier = Modifier.weight(1f), maxLines = 1)
        if (value != null && previous != null) DeltaChip((value.toLong() - previous.toLong()).toInt(), unit, true)
        LessonText(value?.let { "$it$unit" } ?: "미측정", 36, CoachColors.Ink, maxLines = 1)
    }
}
