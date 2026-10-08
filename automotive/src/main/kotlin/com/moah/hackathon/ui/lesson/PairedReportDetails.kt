package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.HarshKind
import com.moah.hackathon.ui.CoachColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun PairedReportDetails(report: LessonReport, onBack: () -> Unit) {
    var end by remember(report) { mutableIntStateOf(report.attempts.lastIndex) }
    val current = report.attempts.getOrNull(end)
    val previous = report.attempts.getOrNull(end - 1)
    Column(Modifier.fillMaxSize().background(CoachColors.Ink).padding(horizontal = 84.dp, vertical = 60.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BrandMark(color = CoachColors.Paper)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow("자세히 보기 · ${report.task.title}", color = CoachColors.Platinum)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (end > 1) ArrowPill("이전 회차", "←", { end-- }, dark = true)
                if (end < report.attempts.lastIndex) ArrowPill("다음 회차", "→", { end++ }, dark = true)
            }
        }
        if (current != null) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                if (report.task.isCourse && current.course != null) {
                    CourseScoreRuler(previous, current)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
                        listOfNotNull(previous, current).forEach { attempt ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                val course = attempt.course!!
                                LessonText("${attempt.index}회차 · 코스 ${course.score}점" + (course.passScore?.let { " · 합격선 ${it}점" } ?: ""), 40, CoachColors.Paper)
                                LessonText(courseVerdict(course), 48, CoachColors.Platinum)
                                if (course.deductions.isEmpty()) LessonText("감점 없음", 36, CoachColors.Platinum)
                                course.deductions.forEach {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                        LessonText(it.zoneTitle, 36, CoachColors.Platinum, modifier = Modifier.width(230.dp))
                                        LessonText(it.reason + if (it.disqualify) " · 실격" else "", 36, CoachColors.Paper, modifier = Modifier.weight(1f))
                                        LessonText("−${it.points}점", 40, CoachColors.Signal)
                                    }
                                }
                                course.zones.forEach { zone ->
                                    if (!course.positionMeasured || !zone.visited) LessonText("${zone.title} · 확인 못 함", 36, CoachColors.Platinum)
                                    else zone.unmeasured.forEach { LessonText("${zone.title} · $it · 확인 못 함", 36, CoachColors.Platinum) }
                                }
                            }
                        }
                    }
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(96.dp)) {
                    Column(Modifier.weight(1.65f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 160.dp), horizontalArrangement = Arrangement.SpaceAround) {
                            LessonText(previous?.let { "${it.index}회차" } ?: "지난 회차 없음", 36, CoachColors.Platinum.copy(alpha = .6f))
                            LessonText("${current.index}회차", 36, CoachColors.Platinum)
                        }
                        ScorePair("숙련", previous?.score?.skill, current.score.skill)
                        ScorePair("안전", previous?.score?.safety, current.score.safety)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (report.task.type == TaskType.PARKING) {
                            val now = current.score.metrics
                            val before = previous?.score?.metrics
                            PairedMetric("이동", before?.motion?.movingSegments, now.motion.movingSegments, "회")
                            PairedMetric("시간", before?.motion?.totalMillis?.div(1000), now.motion.totalMillis / 1000, "초")
                            PairedMetric("조향 왕복", before?.steering?.reversals, now.steering?.reversals)
                            PairedMetric("기어 전환", before?.gear?.reverseDriveShifts, now.gear?.reverseDriveShifts)
                            PairedMetric(if (report.task.parkingSpec.usesRearDistance) "근접" else "앞 근접", before?.proximity?.warnings, now.proximity?.warnings)
                            PairedMetric("급정지", before?.harshEvents?.count { it.kind == HarshKind.BRAKING }, now.harshEvents.count { it.kind == HarshKind.BRAKING })
                            if (now.harshEvents.any { it.kind == HarshKind.ACCELERATION } || before?.harshEvents?.any { it.kind == HarshKind.ACCELERATION } == true)
                                PairedMetric("급가속", before?.harshEvents?.count { it.kind == HarshKind.ACCELERATION }, now.harshEvents.count { it.kind == HarshKind.ACCELERATION })
                            PairedMetric("방향 편차", previous?.verdict?.headingErrorDeg?.roundToInt(), current.verdict?.headingErrorDeg?.roundToInt(), "°")
                        } else checklistResults(current.score).forEach { result ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                LessonText(result.label, 36, CoachColors.Platinum, modifier = Modifier.width(250.dp))
                                LessonText(result.mark, 40, if (result.passed == false) CoachColors.Signal else CoachColors.Paper)
                                LessonText(result.detail, 36, CoachColors.Platinum, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else Spacer(Modifier.weight(1f))
        PosterRule(color = CoachColors.Periwinkle)
        LessonText("다음엔 ${report.nextTask.title} · ${report.nextMode.label} — ${report.nextReason}", 40, CoachColors.Paper)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            AvailabilitySummary(report.best.badge, 40, onInk = true)
            LessonText(reportDisclosure(report), 40, CoachColors.Platinum, modifier = Modifier.weight(1f))
        }
        if (report.best.missingSignals.isNotEmpty() || report.unverifiedGuideSteps.isNotEmpty()) {
            Column(Modifier.heightIn(max = 82.dp).verticalScroll(rememberScrollState())) { ReportLimitations(report, onInk = true, compact = true) }
        }
        ArrowPill("돌아가기", "←", onBack, dark = true)
    }
}

@Composable
private fun ScorePair(label: String, previous: Int?, current: Int) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(label, Modifier.width(140.dp), color = CoachColors.Platinum)
            ScoreCapsule(previous?.toString() ?: "—", false, Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LessonText("→", 48, CoachColors.Platinum)
                if (previous != null) ComparisonDelta(current - previous)
            }
            ScoreCapsule(current.toString(), true, Modifier.weight(1f))
        }
}

@Composable
private fun ScoreCapsule(value: String, current: Boolean, modifier: Modifier) {
    Box(modifier.height(220.dp).background(if (current) CoachColors.Platinum else CoachColors.Ink, RoundedCornerShape(100))
        .border(2.dp, if (current) CoachColors.Platinum else CoachColors.Platinum.copy(alpha = .35f), RoundedCornerShape(100)),
        contentAlignment = Alignment.Center) {
        LessonText(value, 140, if (current) CoachColors.Ink else CoachColors.Platinum.copy(alpha = .5f), bold = true)
    }
}

@Composable
private fun ComparisonDelta(delta: Int, unit: String = "") {
    LessonText("${if (delta > 0) "+" else if (delta < 0) "−" else ""}${abs(delta)}$unit", 40, CoachColors.Platinum,
        modifier = Modifier.background(CoachColors.Periwinkle, RoundedCornerShape(100)).padding(horizontal = 18.dp, vertical = 6.dp))
}

@Composable
private fun PairedMetric(label: String, previous: Number?, current: Number?, unit: String = "") {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        LessonText(label, 40, CoachColors.Platinum, modifier = Modifier.weight(1f))
        LessonText(previous?.let { "$it$unit" } ?: "—", 40, CoachColors.Platinum.copy(alpha = .5f), modifier = Modifier.width(120.dp))
        LessonText("→", 36, CoachColors.Platinum.copy(alpha = .5f))
        LessonText(current?.let { "$it$unit" } ?: "미측정", 40, CoachColors.Paper, modifier = Modifier.width(140.dp))
    }
    PosterRule(color = CoachColors.Periwinkle)
}

@Composable
private fun CourseScoreRuler(previous: AttemptRecord?, current: AttemptRecord) {
    val result = current.course!!
    BoxWithConstraints(Modifier.fillMaxWidth().height(360.dp).testTag("course-score-ruler")) {
        val left = 180.dp
        val width = maxWidth - 360.dp
        val passScore = result.passScore
        Canvas(Modifier.fillMaxWidth().height(260.dp)) {
            val start = left.toPx(); val length = width.toPx(); val y = 240.dp.toPx()
            drawLine(CoachColors.Periwinkle, Offset(start, y), Offset(start + length, y), 12.dp.toPx())
            previous?.course?.score?.let { drawLine(CoachColors.Platinum.copy(alpha = .3f), Offset(start,y), Offset(start + length * it / 100, y), 12.dp.toPx()) }
            if (passScore != null) drawLine(CoachColors.Platinum, Offset(start + length * passScore / 100, 0f), Offset(start + length * passScore / 100, size.height), 3.dp.toPx())
        }
        previous?.course?.let { course -> ScoreCapsule(course.score.toString(), false,
            Modifier.offset(x = left + width * course.score / 100 - 160.dp, y = 40.dp).width(320.dp)) }
        ScoreCapsule(result.score.toString(), true, Modifier.offset(x = left + width * result.score / 100 - 160.dp, y = 40.dp).width(320.dp))
        LessonText("0", 40, CoachColors.Platinum, modifier = Modifier.offset(x = left, y = 280.dp))
        LessonText("100", 40, CoachColors.Platinum, modifier = Modifier.offset(x = left + width - 80.dp, y = 280.dp))
        if (passScore != null) LessonText("합격선 $passScore", 40, CoachColors.Platinum,
            modifier = Modifier.offset(x = left + width * passScore / 100 - 120.dp, y = 280.dp))
    }
}
