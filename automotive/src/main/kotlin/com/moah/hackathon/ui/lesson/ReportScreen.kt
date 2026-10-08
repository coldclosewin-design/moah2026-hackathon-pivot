package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.LessonReport
import com.moah.hackathon.feature.lesson.ProfileField
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.ui.CoachColors

private enum class ReportPage { SUMMARY, DETAILS, CERTIFICATE, SHARE_EXAMPLE }

@Composable
internal fun ReportScreen(report: LessonReport, onRestart: () -> Unit, locked: Boolean = false,
    onAnswerProfile: (ProfileField, String) -> Unit = { _, _ -> }, onSkipAsk: (ProfileField) -> Unit = {},
    onDemoStop: (() -> Unit)? = null) {
    var page by rememberSaveable(report) { mutableStateOf(ReportPage.SUMMARY) }
    var shareName by rememberSaveable(report) { mutableStateOf(report.shareLevels.firstOrNull()?.name) }
    val shareLevel = report.shareLevels.firstOrNull { it.name == shareName } ?: report.shareLevels.firstOrNull()
    if (locked) {
        ResultLockedScreen(onDemoStop)
        return
    }
    if (page == ReportPage.SUMMARY) {
        ReportDashboard(report, onRestart, { page = ReportPage.DETAILS }, { page = ReportPage.CERTIFICATE }, onAnswerProfile, onSkipAsk)
        return
    }
    if (page == ReportPage.DETAILS) {
        PairedReportDetails(report) { page = ReportPage.SUMMARY }
        return
    }
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(.38f).fillMaxHeight()) {
                RecordGraphic(report.attempts.size, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(.62f).fillMaxHeight().padding(start = 120.dp, end = 180.dp, top = 96.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)) {
                when (page) {
                    ReportPage.SUMMARY, ReportPage.DETAILS -> Unit
                    ReportPage.CERTIFICATE -> {
                        CertificateContent(report, shareLevel, { shareName = it.name },
                            { page = ReportPage.SHARE_EXAMPLE }, Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { CompactProvenance(report) }
                        BottomActions(secondary = { BackPill { page = ReportPage.SUMMARY } },
                            primary = { MainPill(onRestart) })
                    }
                    ReportPage.SHARE_EXAMPLE -> {
                        ShareExampleContent(report, shareLevel, Modifier.weight(1f))
                        BottomActions(secondary = { BackPill { page = ReportPage.CERTIFICATE } })
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
            drawLine(CoachColors.Platinum, Offset(size.width * .18f, size.height * .74f),
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

/** Summary keeps its full disclosure; secondary pages share this compact footer. */
@Composable
private fun CompactProvenance(report: LessonReport) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            LessonText("신호 출처 · ", 32, CoachColors.Periwinkle, maxLines = 1)
            // Keep the exact badge text as a separate accessibility node used by the flow contract.
            AvailabilitySummary(report.best.badge, 32)
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
internal fun ReportLimitations(report: LessonReport, onInk: Boolean = false, compact: Boolean = false) {
    val ink = if (onInk) CoachColors.Platinum else CoachColors.Muted
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (report.best.missingSignals.isNotEmpty()) {
            val missing = report.best.missingSignals.map(::signalName).distinct().joinToString(" · ")
            if (compact) LessonText("이 신호는 이 차에서 받지 못했어요 · $missing", 36, ink)
            else {
                LessonText("이 신호는 이 차에서 받지 못했어요", 32, ink)
                LessonText(missing, 32, ink)
            }
        }
        if (report.unverifiedGuideSteps.isNotEmpty()) {
            LessonText("이 단계는 확인할 수 없었어요", 32, ink)
            LessonText(report.unverifiedGuideSteps.joinToString(" · "), 32, ink)
        }
    }
}
