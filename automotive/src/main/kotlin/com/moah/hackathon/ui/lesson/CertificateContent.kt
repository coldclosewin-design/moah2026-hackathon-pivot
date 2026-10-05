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

/** All three scopes remain above the fixed provenance and single action row. */
@Composable
internal fun CertificateContent(report: LessonReport, selected: ShareLevel?, onSelect: (ShareLevel) -> Unit,
    onExample: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("진단서")
            bestCourseAttempt(report.attempts)?.course?.let { result ->
                LessonText("${report.task.title} · ${courseVerdict(result)}", 32)
            }
        }
        LessonText("예시입니다 — 실제 전송·계약은 없습니다", 32, CoachColors.Periwinkle)
        Eyebrow("공유 범위 · 위로 갈수록 넓게")
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            report.shareLevels.reversed().forEach { level ->
                val chosen = level == selected
                val ink = if (chosen) CoachColors.Paper else CoachColors.Ink
                val muted = if (chosen) CoachColors.Paper.copy(alpha = .8f) else CoachColors.Muted
                Row(Modifier.fillMaxWidth().height(148.dp).testTag("share-${level.name}")
                    .surfaceTexture(if (chosen) CoachColors.Periwinkle else CoachColors.Lavender,
                        if (chosen) CoachTexture.SelectedCard else CoachTexture.Card)
                    .selectable(chosen, role = Role.RadioButton, onClick = { onSelect(level) })
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(.44f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LessonText(level.label, 40, ink)
                        LessonText(level.description, 32, muted)
                    }
                    Column(Modifier.weight(.56f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LessonText(level.benefit, 36, ink)
                        LessonText("${level.condition} · 예시", 28, muted)
                    }
                }
            }
        }
        LessonText("넓은 범위일수록 혜택이 늘어요 · 포함 항목은 공유 예시에서 확인", 28, CoachColors.Muted)
        TextAction("공유 예시 보기", onExample, size = 32)
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
                    LessonText(badgeText(report.best.badge), 28, CoachColors.Periwinkle)
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
