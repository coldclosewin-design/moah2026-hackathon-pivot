package com.moah.hackathon.ui.lesson

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.LessonReport
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

private enum class ReportPage { SUMMARY, DETAILS, CERTIFICATE }

@Composable
internal fun ReportScreen(report: LessonReport, onRestart: () -> Unit, locked: Boolean = false) {
    var page by rememberSaveable(report) { mutableStateOf(ReportPage.SUMMARY) }
    if (locked) {
        ResultLockedScreen()
        return
    }
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(.38f).fillMaxHeight()) {
                val course = bestCourseAttempt(report.attempts)?.course
                val showVerdict = page == ReportPage.SUMMARY && report.task.type == TaskType.PARKING
                if (page == ReportPage.SUMMARY && course != null) CourseSummaryPanel(course, Modifier.fillMaxSize())
                else RecordGraphic(report.attempts.size, Modifier.fillMaxSize(), compact = showVerdict)
                if (showVerdict) VerdictPanel(report.attempts.lastOrNull()?.verdict,
                    Modifier.align(Alignment.BottomStart).fillMaxWidth(), title = "마지막 회차의 판정")
            }
            Column(Modifier.weight(.62f).fillMaxHeight().padding(start = 120.dp, end = 180.dp, top = 96.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)) {
                when (page) {
                    ReportPage.SUMMARY -> {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                            Eyebrow("오늘의 기록")
                            Spacer(Modifier.height(28.dp))
                            ResultHeadline(driverReportSummary(report.summary), Modifier.fillMaxWidth().heightIn(max = 520.dp))
                            Spacer(Modifier.height(32.dp))
                            LessonText("${report.task.title} · ${report.mode.label}", 40)
                            if (report.best.missingSignals.isNotEmpty() || report.unverifiedGuideSteps.isNotEmpty()) {
                                Spacer(Modifier.height(32.dp))
                                ReportLimitations(report)
                            }
                        }
                        PrimaryPill(stringResource(R.string.lesson_restart), onRestart)
                        Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                            TextAction("자세히 보기", { page = ReportPage.DETAILS })
                            TextAction("진단서", { page = ReportPage.CERTIFICATE })
                        }
                        ReportProvenance(report)
                    }
                    ReportPage.DETAILS -> {
                        Eyebrow("자세히 보기")
                        DetailsContent(report, Modifier.weight(1f))
                        BottomActions(secondary = { BackPill { page = ReportPage.SUMMARY } },
                            primary = { CompactProvenance(report) })
                    }
                    ReportPage.CERTIFICATE -> {
                        CertificateContent(report, Modifier.weight(1f))
                        BottomActions(secondary = { BackPill { page = ReportPage.SUMMARY } },
                            alignment = Alignment.Top, primary = {
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Box(Modifier.height(112.dp), contentAlignment = Alignment.CenterEnd) { CompactProvenance(report) }
                                    PrimaryPill(stringResource(R.string.lesson_restart), onRestart)
                                }
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordGraphic(attempts: Int, modifier: Modifier, compact: Boolean = false) {
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(CoachColors.Periwinkle, -90f, 180f, false,
                topLeft = Offset(-size.width * .67f, -size.height * .30f),
                size = Size(size.width * 1.7f, size.height * 1.6f), style = Stroke(size.width * .28f))
            drawPath(Path().apply {
                moveTo(0f, 0f); lineTo(size.width * .52f, 0f)
                lineTo(size.width * .52f, size.height * .19f)
                cubicTo(size.width * .98f, size.height * .31f, size.width * .95f, size.height * .61f,
                    size.width * .83f, size.height * .73f)
                quadraticTo(size.width * .4f, size.height * .82f, size.width * .16f, size.height)
                lineTo(0f, size.height); close()
            }, CoachColors.Ink)
            drawLine(CoachColors.Signal, Offset(size.width * .18f, size.height * .74f),
                Offset(size.width * .29f, size.height * .66f), strokeWidth = 14.dp.toPx())
        }
        BrandMark(Modifier.padding(start = 180.dp, top = 64.dp), CoachColors.Paper)
        Column(if (compact) Modifier.align(Alignment.TopStart).padding(start = 150.dp, top = 200.dp)
            else Modifier.align(Alignment.CenterStart).padding(start = 150.dp, bottom = 90.dp)) {
            Eyebrow("연습한 회차", color = CoachColors.Paper)
            val count = attempts.toString().padStart(2, '0')
            Headline(count, size = minOf(if (compact) 320 else 400, 820 / count.length), color = CoachColors.Paper)
            LessonText("회", 40, CoachColors.Paper.copy(alpha = .6f))
        }
    }
}

@Composable
private fun ReportProvenance(report: LessonReport) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PosterRule()
        Eyebrow("신호 출처", color = CoachColors.Muted)
        if (report.task.isCourse) {
            LessonText(badgeText(report.best.badge), 32, CoachColors.Periwinkle)
            LessonText(when {
                bestCourseAttempt(report.attempts)?.course?.positionMeasured != true -> "시험장 위치를 받지 못해 구간은 확인 못 했어요."
                report.best.badge.live == 0 && report.best.badge.simulated > 0 -> "구간과 위치·신호등은 시험장 신호(시뮬레이션)로 측정했어요."
                else -> "구간과 위치·신호등은 시험장 신호로 측정했어요."
            }, 32, CoachColors.Muted)
        } else Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            LessonText(badgeText(report.best.badge), 32, CoachColors.Periwinkle)
            LessonText(if (report.task.type == TaskType.CHECKLIST) "출발 전 점검을 돌아봤어요." else "주차 과정만 측정했어요.", 32, CoachColors.Muted)
        }
    }
}

/** Summary keeps its full disclosure; secondary pages share this compact footer. */
@Composable
private fun CompactProvenance(report: LessonReport) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            LessonText("신호 출처 · ", 32, CoachColors.Periwinkle, maxLines = 1)
            // Keep the exact badge text as a separate accessibility node used by the flow contract.
            LessonText(badgeText(report.best.badge), 32, CoachColors.Periwinkle, maxLines = 1)
        }
        LessonText(when {
            report.task.isCourse && bestCourseAttempt(report.attempts)?.course?.positionMeasured != true -> "시험장 위치를 받지 못해 구간은 미측정이에요."
            report.task.isCourse && report.best.badge.live == 0 && report.best.badge.simulated > 0 -> "구간·위치·신호등은 시험장 시뮬레이션이에요."
            report.task.isCourse -> "구간·위치·신호등은 시험장 신호로 측정했어요."
            report.task.type == TaskType.CHECKLIST -> "출발 전 점검을 돌아봤어요."
            else -> "주차 과정만 측정했어요."
        }, 28, CoachColors.Muted, maxLines = 1)
    }
}

/** Long missing-signal/guide lists share the body scroll, leaving actions and provenance fixed. */
@Composable
internal fun ReportLimitations(report: LessonReport) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (report.best.missingSignals.isNotEmpty()) {
            LessonText("이 신호는 이 차에서 받지 못했어요", 32, CoachColors.Muted)
            LessonText(report.best.missingSignals.map(::signalName).distinct().joinToString(" · "), 32, CoachColors.Muted)
        }
        if (report.unverifiedGuideSteps.isNotEmpty()) {
            LessonText("이 단계는 확인할 수 없었어요", 32, CoachColors.Muted)
            LessonText(report.unverifiedGuideSteps.joinToString(" · "), 32, CoachColors.Muted)
        }
    }
}

@Composable
private fun DetailsContent(report: LessonReport, modifier: Modifier) {
    if (report.task.isCourse) {
        CourseDetails(report, modifier)
        return
    }
    if (report.task.type == TaskType.PARKING) {
        ParkingDetails(report, modifier)
        return
    }
    Column(modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(if (report.task.type == TaskType.CHECKLIST) 16.dp else 32.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(160.dp)) {
            Column { Eyebrow("숙련"); LessonText(report.best.skill.toString(), 96, bold = true) }
            Column { Eyebrow("안전"); LessonText(report.best.safety.toString(), 96, bold = true) }
        }
        report.attempts.forEachIndexed { index, attempt ->
            Column(Modifier.fillMaxWidth().surfaceTexture(CoachColors.Paper, CoachTexture.Card)
                .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (report.task.type == TaskType.CHECKLIST) 16.dp else 32.dp)) {
                val previous = report.attempts.getOrNull(index - 1)?.score
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LessonText("${attempt.index}회차 ·", 40)
                    LessonText("숙련 ${attempt.score.skill}", 40, trendColor(attempt.score.skill, previous?.skill))
                    LessonText("· 안전 ${attempt.score.safety}", 40, trendColor(attempt.score.safety, previous?.safety))
                    if (report.task.type != TaskType.CHECKLIST) {
                        LessonText("· 이동 ${attempt.score.metrics.motion.movingSegments}회", 40,
                            trendColor(attempt.score.metrics.motion.movingSegments, previous?.metrics?.motion?.movingSegments, lowerBetter = true))
                        LessonText("· ${attempt.score.metrics.motion.totalMillis / 1000}초", 40,
                            trendColor(attempt.score.metrics.motion.totalMillis, previous?.metrics?.motion?.totalMillis, lowerBetter = true))
                    }
                }
                if (report.task.type == TaskType.PARKING) {
                    LessonText(parkingDetailLine(attempt.score.metrics, report.task.parkingSpec.usesRearDistance), 32, CoachColors.Muted)
                    LessonText(headingDetailLine(attempt.verdict), 32, CoachColors.Muted)
                } else if (report.task.type == TaskType.CHECKLIST) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        checklistResults(attempt.score).forEach { result ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                LessonText(result.label, 32, modifier = Modifier.width(280.dp))
                                LessonText(result.mark, 32, when (result.passed) {
                                    true -> CoachColors.Periwinkle; false -> CoachColors.Signal; null -> CoachColors.Muted
                                }, modifier = Modifier.width(120.dp))
                                LessonText(result.detail, 32, CoachColors.Muted, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        PosterRule()
        LessonText("다음엔 ${report.nextTask.title} · ${report.nextMode.label} — ${report.nextReason}", 40)
        ReportLimitations(report)
    }
}

private fun trendColor(value: Number, previous: Number?, lowerBetter: Boolean = false) = when {
    previous == null || value.toDouble() == previous.toDouble() -> CoachColors.Ink
    (value.toDouble() > previous.toDouble()) != lowerBetter -> CoachColors.Periwinkle
    else -> CoachColors.Muted
}

@Composable
private fun CertificateContent(report: LessonReport, modifier: Modifier) {
    var selectedName by rememberSaveable(report) { mutableStateOf(report.shareLevels.firstOrNull()?.name) }
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Eyebrow("진단서")
        bestCourseAttempt(report.attempts)?.course?.let { result ->
            LessonText("${report.task.title} · ${courseVerdict(result)}", 40)
        }
        LessonText("예시입니다 — 실제 전송·계약은 없습니다", 40, CoachColors.Periwinkle)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Eyebrow("공유 범위")
                report.shareLevels.forEach { level ->
                    PosterRule()
                    Row(Modifier.fillMaxWidth().selectable(selectedName == level.name, role = Role.RadioButton,
                        onClick = { selectedName = level.name }).padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                        RadioButton(selectedName == level.name, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = CoachColors.Periwinkle, unselectedColor = CoachColors.Muted))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LessonText(level.label, 40)
                            LessonText(level.description, 32, CoachColors.Muted)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Eyebrow("예상 혜택")
                report.benefits.forEach { benefit -> PosterRule(); LessonText(benefit, 40) }
            }
        }
        TextAction("공유 예시 보기", { Toast.makeText(context, "예시 화면입니다", Toast.LENGTH_SHORT).show() })
    }
}
