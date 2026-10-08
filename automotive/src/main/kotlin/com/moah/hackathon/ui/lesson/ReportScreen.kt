@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.moah.hackathon.ui.lesson

import androidx.compose.animation.*
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.LessonReport
import com.moah.hackathon.feature.lesson.ProfileField
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
        SharedTransitionLayout(Modifier.background(CoachColors.Ink)) {
        CompositionLocalProvider(LocalHomeShared provides this) {
        AnimatedContent(page, transitionSpec = {
            (fadeIn(tween(220, delayMillis = 260)) + slideInHorizontally(tween(480, easing = CoachMotion.Shared)) { it / 8 }) togetherWith
                (fadeOut(tween(220)) + slideOutHorizontally(tween(480, easing = CoachMotion.Shared)) { -it / 5 })
        }, label = "certificate-share") { shown ->
        CompositionLocalProvider(LocalHomeVisibility provides this) {
        Column(Modifier.fillMaxSize().background(CoachColors.Ink).padding(72.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            if (shown == ReportPage.CERTIFICATE) {
                CertificateContent(report, shareLevel, { shareName = it.name }, Modifier.weight(1f))
                BottomActions(secondary = {
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        ArrowPill("돌아가기", "←", { page = ReportPage.SUMMARY }, dark = true)
                        ArrowPill("공유 예시 보기", "↗", { page = ReportPage.SHARE_EXAMPLE }, dark = true)
                    }
                }, primary = { FillButton("메인으로", onRestart, Modifier.width(720.dp).height(140.dp), inverse = true) })
            } else {
                ShareExampleContent(report, shareLevel, Modifier.weight(1f))
                BottomActions(secondary = { ArrowPill("돌아가기", "←", { page = ReportPage.CERTIFICATE }, dark = true) })
            }
        }
        }
        }
        }
        }
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
