package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun ReportDashboard(report: LessonReport, onRestart: () -> Unit, onDetails: () -> Unit,
    onCertificate: () -> Unit, onAnswer: (ProfileField, String) -> Unit, onSkip: (ProfileField) -> Unit) {
    PosterSurface {
        Column(Modifier.fillMaxSize().background(CoachColors.Lavender).padding(72.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Row(Modifier.weight(1.35f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                Column(Modifier.width(600.dp).fillMaxHeight().background(CoachColors.Ink, RoundedCornerShape(44.dp)).padding(56.dp),
                    verticalArrangement = Arrangement.Center) {
                    Eyebrow("연습한 회차", color = CoachColors.Platinum)
                    LessonText(report.attempts.size.toString().padStart(2, '0'), 250, CoachColors.Paper)
                    LessonText("회 · ${report.task.title}", 36, CoachColors.Platinum)
                }
                Column(Modifier.weight(1f).fillMaxHeight().background(CoachColors.Paper, RoundedCornerShape(44.dp)).padding(64.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BrandMark(); Eyebrow("오늘의 기록", color = CoachColors.Muted)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        CoachLines(driverReportSummary(report.summary), titleSize = 108, adviceSize = 52)
                    }
                    LessonText(taskModeLine(report.task, report.mode), 36,
                        modifier = Modifier.background(CoachColors.Lavender, RoundedCornerShape(100)).padding(horizontal = 24.dp, vertical = 12.dp))
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().background(CoachColors.Paper, RoundedCornerShape(44.dp)).padding(44.dp)) {
                    if (report.task.type == TaskType.PARKING) VerdictPanel(report.attempts.lastOrNull()?.verdict,
                        Modifier.fillMaxSize(), title = "마지막 회차의 판정", grid = true, dark = false, compact = true)
                    else {
                        Eyebrow(if (report.task.isCourse) "최고 회차의 코스" else "마지막 회차의 판정")
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val course = bestCourseAttempt(report.attempts)?.course
                            if (course != null) {
                                LessonText(courseVerdict(course), 56)
                                course.zones.forEach { zone ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        LessonText(zone.title, 36); LessonText(courseZoneStatus(course, zone), 36, CoachColors.Muted)
                                    }
                                }
                            } else report.attempts.lastOrNull()?.let { attempt ->
                                checklistResults(attempt.score).forEach {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        LessonText(it.label, 36); LessonText(it.mark, 36)
                                    }
                                }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().background(CoachColors.Paper, RoundedCornerShape(44.dp)).padding(horizontal = 44.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("신호 출처", color = CoachColors.Muted)
                    SourceBars(report)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        LessonText(reportDisclosure(report), 36, CoachColors.Muted)
                        ReportLimitations(report)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                ArrowPill("자세히 보기", "↗", onDetails)
                ArrowPill("진단서", "↗", onCertificate)
                Spacer(Modifier.weight(1f))
                MainPill(onRestart, Modifier.width(1100.dp))
            }
        }
        report.askOne?.let { question ->
            Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false,
                dismissOnBackPress = false, dismissOnClickOutside = false)) {
                Box(Modifier.fillMaxSize().padding(72.dp), contentAlignment = Alignment.BottomCenter) {
                    SheetCard(Modifier.fillMaxWidth().height(420.dp)) {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            ProfileAskCard(question, onAnswer, onSkip)
                        }
                    }
                }
            }
        }
    }
}

internal fun reportDisclosure(report: LessonReport): String = when {
    report.task.isCourse && bestCourseAttempt(report.attempts)?.course?.positionMeasured != true -> "시험장 위치를 받지 못해 구간은 확인 못 했어요."
    report.task.isCourse && report.best.badge.live == 0 && report.best.badge.simulated > 0 -> "구간과 위치·신호등은 시험장 신호(시뮬레이션)로 측정했어요."
    report.task.isCourse -> "구간과 위치·신호등은 시험장 신호로 측정했어요."
    report.task.type == TaskType.CHECKLIST -> "출발 전 점검을 돌아봤어요."
    else -> "주차 과정만 측정했어요."
}

@Composable
private fun SourceBars(report: LessonReport) {
    val badge = report.best.badge
    val values = listOf(badge.live, badge.simulated, badge.missing)
    val total = values.sum().coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().testTag("report-source-bars")
        .clearAndSetSemantics { text = AnnotatedString(badgeText(badge)) }, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("실신호", "시뮬레이션", "미측정").forEachIndexed { index, label ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(Modifier.width(340.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SignalShape(com.moah.hackathon.vehicle.SignalAvailability.entries[index], Modifier.size(36.dp))
                        LessonText(label, 36)
                    }
                    Canvas(Modifier.weight(1f).height(28.dp)) {
                        val radius = CornerRadius(size.height / 2)
                        drawRoundRect(CoachColors.Lavender, cornerRadius = radius)
                        if (index == 2) drawRoundRect(CoachColors.Muted, cornerRadius = radius,
                            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()))))
                        else {
                            val width = size.width * values[index] / total
                            drawRoundRect(CoachColors.Ink, size = Size(width, size.height), cornerRadius = radius)
                            if (index == 1) clipRect(right = width) {
                                var x = -size.height
                                while (x < width) {
                                    drawLine(CoachColors.Platinum, Offset(x, size.height), Offset(x + size.height, 0f), 3.dp.toPx()); x += 12.dp.toPx()
                                }
                            }
                        }
                    }
                    LessonText(values[index].toString(), 40, modifier = Modifier.width(60.dp))
                }
            }
        }
    }
}
