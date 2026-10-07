package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.LessonReport
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture
import kotlinx.coroutines.delay

@Composable
internal fun CourseDonePanel(course: TrackCourse, result: CourseResult, modifier: Modifier) {
    val time = remember(result) { Animatable(0f) }
    LaunchedEffect(result) {
        delay(500)
        time.animateTo((result.trail.lastOrNull()?.tMillis ?: 0L).toFloat(), tween(3_000, easing = LinearEasing))
    }
    val trail = courseTrailThroughTime(result.trail, time.value.toLong())
    Column(modifier.surfaceTexture(CoachColors.Ink, CoachTexture.Panel).padding(start = 56.dp, end = 56.dp, top = 64.dp, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Eyebrow("지나간 자리", color = CoachColors.Paper.copy(alpha = .6f))
        CourseMap(course, Modifier.weight(1f).fillMaxWidth(),
            pose = trail.lastOrNull()?.let { Pose(Vec2(it.x, it.y), it.headingDeg) }.takeIf { result.positionMeasured },
            trail = trail, markers = result.deductions.filter { it.tMillis <= time.value }.mapNotNull { it.at })
        LessonText("시험장 위치·신호로 기록했어요.", 28, CoachColors.Paper.copy(alpha = .6f))
        LessonText(courseVerdict(result), 56, CoachColors.Paper)
        result.deductions.distinctBy { it.zoneTitle to it.reason }.take(3).forEach {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                LessonText("●", 28, CoachColors.Signal)
                Column {
                    LessonText(it.reason, 36, CoachColors.Paper)
                    LessonText(it.zoneTitle, 28, CoachColors.Paper.copy(alpha = .6f))
                }
            }
        }
        if (result.positionMeasured && result.zones.any { it.unmeasured.isNotEmpty() || !it.visited })
            LessonText("확인하지 못한 구간은 자세히 보기에서 확인해요.", 28, CoachColors.Paper.copy(alpha = .6f))
    }
}

@Composable
internal fun CourseSummaryPanel(result: CourseResult, modifier: Modifier) {
    Column(modifier.surfaceTexture(CoachColors.Ink, CoachTexture.Panel).padding(start = 80.dp, end = 64.dp, top = 64.dp, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        BrandMark(color = CoachColors.Paper)
        Eyebrow("최고 회차의 코스", color = CoachColors.Paper.copy(alpha = .6f))
        LessonText(courseVerdict(result), 56, CoachColors.Paper)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            result.zones.forEach { zone ->
                val status = courseZoneStatus(result, zone)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LessonText(zone.title, 36, CoachColors.Paper, modifier = Modifier.weight(1f))
                    LessonText(status, 36, when (status) {
                        "지남" -> CoachColors.Paper
                        "놓침" -> CoachColors.Signal
                        else -> CoachColors.Paper.copy(alpha = .6f)
                    })
                }
            }
        }
    }
}

@Composable
internal fun CourseDetails(report: LessonReport, modifier: Modifier) {
    AttemptComparison(report.attempts, modifier) { attempt, previous ->
        attempt.course?.let { result ->
            LessonText("${attempt.index}회차 · 코스 ${result.score}점" +
                (result.passScore?.let { " · 합격선 ${it}점" } ?: ""), 36)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                LessonText(courseVerdict(result), 40, CoachColors.Periwinkle)
                previous?.course?.let { DeltaChip(result.score - it.score, "점") }
            }
            PosterRule()
            if (result.deductions.isEmpty()) {
                LessonText("감점 없음", 32, CoachColors.Muted)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LessonText("구간", 32, CoachColors.Muted, modifier = Modifier.width(150.dp))
                    LessonText("사유", 32, CoachColors.Muted, modifier = Modifier.weight(1f))
                    LessonText("감점", 32, CoachColors.Muted, modifier = Modifier.width(90.dp))
                }
                result.deductions.forEach { deduction ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LessonText(deduction.zoneTitle, 32, modifier = Modifier.width(150.dp))
                        LessonText(deduction.reason + if (deduction.disqualify) " · 실격" else "", 32, modifier = Modifier.weight(1f))
                        LessonText("−${deduction.points}점", 32, CoachColors.Signal, modifier = Modifier.width(90.dp))
                    }
                }
            }
            result.zones.forEach { zone ->
                if (!result.positionMeasured || !zone.visited) LessonText("${zone.title} · 확인 못 함", 32, CoachColors.Muted)
                else zone.unmeasured.forEach { LessonText("${zone.title} · $it · 확인 못 함", 32, CoachColors.Muted) }
            }
        }
    }
}
