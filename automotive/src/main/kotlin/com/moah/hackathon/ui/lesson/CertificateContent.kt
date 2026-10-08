package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.HarshKind
import com.moah.hackathon.ui.CoachColors
import kotlin.math.roundToInt
import com.moah.hackathon.vehicle.SignalAvailability

/** A4: one recorded first/last table; nested brackets show exactly which rows leave the device. */
@Composable
internal fun CertificateContent(report: LessonReport, selected: ShareLevel?, onSelect: (ShareLevel) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CertificateHeader(report, "진단서")
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CertificateTable(report, selected, false, Modifier.weight(1.8f).fillMaxHeight())
            ScopeBrackets(report.shareLevels, selected, onSelect, Modifier.weight(1f).fillMaxHeight())
        }
        AvailabilitySummary(report.best.badge, 36, onInk = true)
        LessonText(reportDisclosure(report), 32, CoachColors.Platinum)
    }
}

@Composable
private fun CertificateHeader(report: LessonReport, title: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            BrandMark(color = CoachColors.Paper)
            LessonText("$title · ${report.task.title} · ${report.mode.label}", 38, CoachColors.Platinum, modifier = Modifier.testTag("certificate-task-row"))
        }
        Row(Modifier.background(CoachColors.Periwinkle, RoundedCornerShape(100)).padding(horizontal = 28.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            YellowContextIcon(ContextIcon.Document)
            LessonText("예시입니다 — 실제 전송·계약은 없습니다", 32, CoachColors.Paper)
        }
    }
}

/** Raw samples aren't retained by AttemptRecord; never turn a derived path into a raw series. */
internal fun certificateValue(attempt: AttemptRecord?, item: String): String {
    if (attempt == null) return "—"
    val metrics = attempt.score.metrics
    return when (item) {
        "숙련 점수" -> attempt.score.skill.toString()
        "안전 점수" -> attempt.score.safety.toString()
        "이동 구간 수" -> metrics.motion.movingSegments.toString()
        "조향 왕복" -> metrics.steering?.reversals?.toString() ?: "미측정"
        "기어 전환" -> metrics.gear?.reverseDriveShifts?.toString() ?: "미측정"
        "근접" -> metrics.proximity?.warnings?.toString() ?: "미측정"
        "급정지" -> metrics.harshEvents.count { it.kind == HarshKind.BRAKING }.toString()
        "방향 편차" -> attempt.verdict?.headingErrorDeg?.roundToInt()?.let { "$it°" } ?: "미측정"
        else -> "기록 없음"
    }
}

@Composable
private fun CertificateTable(report: LessonReport, selected: ShareLevel?, mask: Boolean, modifier: Modifier, compact: Boolean = false) {
    val before = report.attempts.firstOrNull()
    val after = report.attempts.lastOrNull()
    val size = if (compact) 23 else 36
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(if (compact) 64.dp else 72.dp), verticalAlignment = Alignment.CenterVertically) {
            LessonText("항목", size, CoachColors.Platinum, modifier = Modifier.weight(1.7f))
            LessonText(before?.let { "${it.index}회차" } ?: "—", size, CoachColors.Platinum, modifier = Modifier.weight(1f))
            LessonText(after?.let { "${it.index}회차" } ?: "—", size, CoachColors.Platinum, modifier = Modifier.weight(1f))
            if (!compact) LessonText("변화", size, CoachColors.Platinum, modifier = Modifier.weight(.8f))
            LessonText("출처", size, CoachColors.Platinum, modifier = Modifier.weight(.9f))
        }
        ShareLevel.RAW.includes.forEachIndexed { index, item ->
            val included = item in selected?.includes.orEmpty()
            val color = if (included) CoachColors.Platinum else CoachColors.Platinum.copy(alpha = .38f)
            PosterRule(color = CoachColors.Platinum.copy(alpha = .18f))
            Row(Modifier.weight(1f).fillMaxWidth().semantics(mergeDescendants = true) { stateDescription = if (included) "포함" else "범위 밖" }
                .then(if (!compact) Modifier.testTag("share-item-$item") else Modifier), verticalAlignment = Alignment.CenterVertically) {
                LessonText(item.removeSuffix(" 점수"), size, color, modifier = Modifier.weight(1.7f))
                if (mask && !included) {
                    Row(Modifier.weight(if (compact) 2.9f else 3.7f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.weight(1f).height(18.dp).background(CoachColors.Jet, RoundedCornerShape(4.dp)))
                        LessonText("범위 밖", if (compact) 20 else 28, CoachColors.Platinum.copy(alpha = .55f))
                    }
                } else {
                    val first = certificateValue(before, item)
                    val last = certificateValue(after, item)
                    listOf(first, last).forEachIndexed { column, value ->
                        Box(Modifier.weight(1f)) {
                            LessonText(value, size, if (index < 2 && column == 1) CoachColors.Ink else color,
                                modifier = if (index < 2) Modifier.border(1.dp, color.copy(alpha = .6f), RoundedCornerShape(100))
                                    .background(if (column == 1) color else CoachColors.Ink, RoundedCornerShape(100)).padding(horizontal = if (compact) 12.dp else 24.dp) else Modifier)
                        }
                    }
                    if (!compact) {
                        val delta = first.removeSuffix("°").toIntOrNull()?.let { a -> last.removeSuffix("°").toIntOrNull()?.minus(a) }
                        LessonText(delta?.let { if (it > 0) "+$it" else it.toString() } ?: "—", size, color, modifier = Modifier.weight(.8f))
                    }
                    val sources = if (last in listOf("미측정", "기록 없음", "—")) listOf(SignalAvailability.MISSING) else buildList {
                        if ((after?.score?.badge?.live ?: 0) > 0) add(SignalAvailability.LIVE)
                        if ((after?.score?.badge?.simulated ?: 0) > 0) add(SignalAvailability.SIMULATED)
                    }.ifEmpty { listOf(SignalAvailability.MISSING) }
                    Row(Modifier.weight(.9f).semantics { contentDescription = sources.joinToString(" · ") {
                        when (it) { SignalAvailability.LIVE -> "실신호"; SignalAvailability.SIMULATED -> "시뮬레이션"; SignalAvailability.MISSING -> "미측정" }
                    } }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sources.forEach { SignalShape(it, Modifier.size(size.dp), color) }
                    }
                }
            }
        }
        PosterRule(color = CoachColors.Platinum.copy(alpha = .18f))
        LessonText("출처는 회차 기준 · 원시 시계열은 저장하지 않음", if (compact) 20 else 28, CoachColors.Platinum.copy(alpha = .65f), modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun ScopeBrackets(levels: List<ShareLevel>, selected: ShareLevel?, onSelect: (ShareLevel) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize().padding(top = 72.dp, bottom = 40.dp)) {
            levels.forEachIndexed { index, level ->
                val x = (24 + index * 22).dp.toPx()
                val bottom = size.height * level.includes.size / ShareLevel.RAW.includes.size
                val color = CoachColors.Platinum.copy(alpha = if (level == selected) 1f else .35f)
                val stroke = if (level == selected) 6.dp.toPx() else 2.dp.toPx()
                drawLine(color, Offset(0f, 0f), Offset(x, 0f), stroke)
                drawLine(color, Offset(x, 0f), Offset(x, bottom), stroke)
                drawLine(color, Offset(0f, bottom), Offset(x, bottom), stroke)
                drawLine(color, Offset(x, bottom / 2), Offset(140.dp.toPx(), bottom / 2), stroke)
            }
        }
        Column(Modifier.fillMaxSize().padding(start = 140.dp, top = 64.dp), verticalArrangement = Arrangement.SpaceBetween) {
            levels.forEach { level ->
                val chosen = selected == level
                Column(Modifier.fillMaxWidth().border(1.5.dp, CoachColors.Platinum.copy(alpha = .55f), RoundedCornerShape(60.dp))
                    .background(if (chosen) CoachColors.Platinum else CoachColors.Ink, RoundedCornerShape(60.dp))
                    .clickable(role = Role.RadioButton) { onSelect(level) }.semantics(mergeDescendants = true) { this.selected = chosen }
                    .testTag("share-${level.name}").padding(horizontal = 38.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ink = if (chosen) CoachColors.Ink else CoachColors.Platinum
                    LessonText(level.label, 42, ink)
                    LessonText(level.benefit, 30, ink)
                    if (chosen) { LessonText(level.description, 28, ink); LessonText("${level.condition} · 예시", 28, ink) }
                }
            }
            LessonText("괄호 안 줄이 받는 쪽에 가요.\n넓은 범위일수록 혜택이 늘어요 · 예시", 28, CoachColors.Platinum)
        }
    }
}

/** A7 on the very same A4 table: excluded values have no drawn or accessible content. */
@Composable
internal fun ShareExampleContent(report: LessonReport, selected: ShareLevel?, modifier: Modifier) {
    var recipient by rememberSaveable { mutableStateOf("본인") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CertificateHeader(report, "공유 예시 · ${selected?.label.orEmpty()}")
        LessonText("공유 예시 · ${selected?.label.orEmpty()}", 36, CoachColors.Platinum)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(56.dp)) {
            CertificateTable(report, selected, true, Modifier.weight(1.65f).fillMaxHeight())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Eyebrow("받는 쪽 화면 · 예시", color = CoachColors.Platinum)
                SelectionTrack(listOf("본인", "동승자", "기관"), recipient, { it }, { recipient = it }, Modifier.fillMaxWidth(), height = 96.dp, textSize = 32,
                    background = CoachColors.Periwinkle, selectedBackground = CoachColors.Platinum, foreground = CoachColors.Platinum, selectedForeground = CoachColors.Ink)
                Column(Modifier.weight(1f).fillMaxWidth().border(8.dp, CoachColors.Jet, RoundedCornerShape(48.dp))
                    .background(CoachColors.Periwinkle, RoundedCornerShape(48.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LessonText("DRIVE COACH · $recipient", 26, CoachColors.Paper)
                    LessonText("예시 — 실제 전송 없음", 26, CoachColors.Platinum)
                    CertificateTable(report, selected, true, Modifier.weight(1f).fillMaxWidth(), compact = true)
                    LessonText("${report.task.title} · 발급일부터 석 달 · 예시", 24, CoachColors.Platinum)
                }
                LessonText(ShareLevel.EXCLUDED.joinToString(" · ") + " 없음", 30, CoachColors.Platinum)
            }
        }
        AvailabilitySummary(report.best.badge, 32, onInk = true)
    }
}
