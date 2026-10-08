package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.LessonReport
import com.moah.hackathon.feature.lesson.ShareLevel
import com.moah.hackathon.scoring.HarshKind
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture
import kotlin.math.roundToInt

/** C1: one actual task table and a separate scope track; no synthetic cross-task totals. */
@Composable
internal fun CertificateContent(report: LessonReport, selected: ShareLevel?, onSelect: (ShareLevel) -> Unit,
    modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(40.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { BrandMark(); Spacer(Modifier.height(24.dp)); Headline("진단서", size = 88) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                YellowContextIcon(ContextIcon.Document)
                LessonText("예시입니다 — 실제 전송·계약은 없습니다", 36)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LessonText("기간 ${report.attempts.firstOrNull()?.index ?: 0}회차–${report.attempts.lastOrNull()?.index ?: 0}회차", 36)
            LessonText(report.task.title, 36, CoachColors.Muted)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            SheetCard(Modifier.weight(.58f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(52.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    Eyebrow("과제별")
                    Row(Modifier.fillMaxWidth()) {
                        LessonText("과제", 32, CoachColors.Muted, modifier = Modifier.weight(1.8f))
                        LessonText("회차", 32, CoachColors.Muted, modifier = Modifier.weight(.6f))
                        LessonText("숙련", 32, CoachColors.Muted, modifier = Modifier.weight(1f))
                        LessonText("안전 · 판정", 32, CoachColors.Muted, modifier = Modifier.weight(1.2f))
                    }
                    PosterRule()
                    Row(Modifier.fillMaxWidth().testTag("certificate-task-row"), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1.8f)) { LessonText(report.task.title, 42); LessonText(report.mode.label, 32, CoachColors.Muted) }
                        LessonText(report.attempts.size.toString(), 44, modifier = Modifier.weight(.6f))
                        LessonText("${report.attempts.firstOrNull()?.score?.skill ?: report.best.skill} → ${report.attempts.lastOrNull()?.score?.skill ?: report.best.skill}", 44, modifier = Modifier.weight(1f))
                        LessonText(bestCourseAttempt(report.attempts)?.course?.let(::courseVerdict)
                            ?: "${report.attempts.firstOrNull()?.score?.safety ?: report.best.safety} → ${report.attempts.lastOrNull()?.score?.safety ?: report.best.safety}", 44, modifier = Modifier.weight(1.2f))
                    }
                    PosterRule()
                    Spacer(Modifier.weight(1f))
                    LessonText("화살표 왼쪽 = 첫 회차 · 오른쪽 = 마지막 회차", 32, CoachColors.Muted)
                    AvailabilitySummary(report.best.badge, 36)
                    LessonText(reportDisclosure(report), 32, CoachColors.Muted)
                }
            }
            SheetCard(Modifier.weight(.42f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    Eyebrow("공유 범위 · 오른쪽으로 갈수록 넓게")
                    SelectionTrack(report.shareLevels, selected, { it.label }, onSelect, Modifier.fillMaxWidth(), height = 112.dp, textSize = 36)
                    selected?.let { level ->
                        LessonText(level.benefit, 44)
                        LessonText("${level.condition} · 예시", 36, CoachColors.Muted)
                        LessonText(level.description, 36)
                    }
                    Spacer(Modifier.weight(1f))
                    LessonText("넓은 범위일수록 혜택이 늘어요 · 포함 항목은 공유 예시에서 확인", 32, CoachColors.Muted)
                }
            }
        }
    }
}

@Composable
internal fun ShareExampleContent(report: LessonReport, selected: ShareLevel?, modifier: Modifier) {
    var recipient by rememberSaveable { mutableStateOf("본인") }
    val metrics = report.best.metrics
    val verdict = report.attempts.firstOrNull { it.score == report.best }?.verdict
    fun value(item: String): String? = when (item) {
        "숙련 점수" -> report.best.skill.toString()
        "안전 점수" -> report.best.safety.toString()
        "이동 구간 수" -> metrics.motion.movingSegments.toString()
        "조향 왕복" -> metrics.steering?.reversals?.toString() ?: "미측정"
        "기어 전환" -> metrics.gear?.reverseDriveShifts?.toString() ?: "미측정"
        "근접" -> metrics.proximity?.warnings?.toString() ?: "미측정"
        "급정지" -> metrics.harshEvents.count { it.kind == HarshKind.BRAKING }.toString()
        "방향 편차" -> verdict?.headingErrorDeg?.roundToInt()?.let { "$it°" } ?: "미측정"
        else -> null // A scope preview must not invent samples that the report does not store.
    }
    Box(modifier) {
        LessonText("예시 — 실제 전송 없음", 64, CoachColors.Periwinkle.copy(alpha = .12f),
            modifier = Modifier.align(Alignment.Center).rotate(-18f))
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Eyebrow("공유 예시 · ${selected?.label.orEmpty()}")
            LessonText("예시 — 실제 전송 없음", 32, CoachColors.Periwinkle)
            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                Column(Modifier.weight(.42f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Eyebrow("발급 대상")
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("본인", "동승자", "기관").forEach { target ->
                            SelectionChip(target, recipient == target, { recipient = target }, Modifier.weight(1f))
                        }
                    }
                    Eyebrow("기간")
                    LessonText(report.attempts.let { "${it.firstOrNull()?.index ?: 0}회차–${it.lastOrNull()?.index ?: 0}회차" }, 36)
                    Eyebrow("과제")
                    LessonText(report.task.title, 36)
                    Eyebrow("유효")
                    LessonText("발급일부터 석 달 · 예시", 32, CoachColors.Muted)
                    Eyebrow("제외")
                    LessonText(ShareLevel.EXCLUDED.joinToString(" · ") + " 없음", 32, CoachColors.Muted)
                    AvailabilitySummary(report.best.badge, 28)
                }
                Column(Modifier.weight(.58f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("포함 항목")
                    ShareLevel.RAW.includes.forEach { item ->
                        val included = item in selected?.includes.orEmpty()
                        val color = if (included) CoachColors.Ink else CoachColors.Muted.copy(alpha = .55f)
                        Row(Modifier.fillMaxWidth().semantics { stateDescription = if (included) "포함" else "범위 밖" }
                            .testTag("share-item-$item"), horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            LessonText(if (included) "✓" else "—", 32, color)
                            LessonText(item, 32, color, modifier = Modifier.weight(1f))
                            LessonText(if (included) value(item).orEmpty() else "범위 밖", 28, color)
                        }
                    }
                }
            }
        }
    }
}
