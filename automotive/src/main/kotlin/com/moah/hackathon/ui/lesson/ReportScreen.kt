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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.LessonReport
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun ReportScreen(report: LessonReport, subtitle: String?, onRestart: () -> Unit,
    demo: (@Composable () -> Unit)? = null) {
    var certificate by rememberSaveable { mutableStateOf(false) }
    LessonFrame(subtitle, demo) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LessonText("오늘의 연습 기록", 56, bold = true, modifier = Modifier.weight(1f))
            LessonButton("오늘", { certificate = false }, primary = !certificate)
            LessonButton("진단서", { certificate = true }, primary = certificate)
        }
        if (certificate) CertificateContent(report, Modifier.weight(1f)) else TodayContent(report, Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LessonText("${report.nextTask.title} · ${report.nextMode.label} — ${report.nextReason}", 36,
                CoachColors.Accent, modifier = Modifier.weight(1f))
            LessonButton(stringResource(R.string.lesson_restart), onRestart, primary = true)
        }
    }
}

@Composable
private fun TodayContent(report: LessonReport, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LessonText(report.summary, 42)
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            LessonCard(Modifier.weight(1f)) {
                LessonText("가장 잘한 회차", 32, CoachColors.Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ScoreGauge("숙련", report.best.skill)
                    ScoreGauge("안전", report.best.safety)
                }
            }
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                LessonCard(Modifier.fillMaxWidth()) {
                    LessonText("회차별 기록", 36, bold = true)
                    ReportRow(listOf("회차", "숙련", "안전", "이동", "시간"), header = true)
                    report.attempts.forEach { attempt ->
                        ReportRow(listOf("${attempt.index}", "${attempt.score.skill}", "${attempt.score.safety}",
                            "${attempt.score.metrics.motion.movingSegments}회", "${attempt.score.metrics.motion.totalMillis / 1000}초"))
                    }
                }
                LessonText(badgeText(report.best.badge), 36, CoachColors.Simulated, bold = true)
                if (report.best.missingSignals.isNotEmpty()) {
                    LessonText("이 신호는 이 차에서 받지 못했어요", 34, bold = true)
                    LessonText(report.best.missingSignals.map(::signalName).distinct().joinToString(" · "), 32, CoachColors.Muted)
                }
                if (report.unverifiedGuideSteps.isNotEmpty()) {
                    LessonText("이 단계는 확인할 수 없었어요", 34, bold = true)
                    report.unverifiedGuideSteps.forEach { LessonText(it, 32, CoachColors.Muted) }
                }
            }
        }
        LessonText("카메라 없이 주차 과정을 측정했어요. 칸 안에 반듯이 들어갔는지는 알 수 없어요.", 32, CoachColors.Muted)
    }
}

@Composable
private fun ReportRow(values: List<String>, header: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        values.forEach { LessonText(it, 32, if (header) CoachColors.Muted else CoachColors.Foreground, modifier = Modifier.weight(1f)) }
    }
}

@Composable
private fun ScoreGauge(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(248.dp).semantics(mergeDescendants = true) { contentDescription = "$label ${score}점" },
            contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                drawArc(CoachColors.Outline, -90f, 360f, false, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
                drawArc(CoachColors.Accent, -90f, score.coerceIn(0, 100) * 3.6f, false,
                    style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            }
            LessonText(score.toString(), 76, bold = true)
        }
        LessonText(label, 36, CoachColors.Muted)
    }
}

@Composable
private fun CertificateContent(report: LessonReport, modifier: Modifier) {
    var selectedName by rememberSaveable { mutableStateOf(report.shareLevels.firstOrNull()?.name) }
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // This notice stays above the scrollable choices and benefits.
        LessonText("예시입니다 — 실제 전송·계약은 없습니다", 40, CoachColors.Simulated, bold = true)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                LessonText("공유 범위를 직접 골라요", 36, bold = true)
                report.shareLevels.forEach { level ->
                    LessonCard(Modifier.fillMaxWidth().selectable(selected = selectedName == level.name,
                        role = Role.RadioButton, onClick = { selectedName = level.name })) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            RadioButton(selectedName == level.name, onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = CoachColors.Accent, unselectedColor = CoachColors.Muted))
                            Column {
                                LessonText(level.label, 36, bold = true)
                                LessonText(level.description, 32, CoachColors.Muted)
                            }
                        }
                    }
                }
            }
            LessonCard(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                LessonText("예상 혜택", 36, bold = true)
                report.benefits.forEach { LessonText("· $it", 34) }
                LessonText("공유와 철회는 운전자가 선택하는 방식의 예시예요.", 32, CoachColors.Muted)
            }
        }
        LessonButton("공유 예시 보기", { Toast.makeText(context, "예시 화면입니다", Toast.LENGTH_SHORT).show() })
    }
}
