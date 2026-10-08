package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.HarshKind
import com.moah.hackathon.ui.CoachColors
import kotlin.math.roundToInt

/** C1: paired scores and two metric columns beside three counted scope pills. */
@Composable
internal fun CertificateContent(report: LessonReport, selected: ShareLevel?, onSelect: (ShareLevel) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(52.dp)) {
        CertificateHeader(report, "진단서")
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
            Column(Modifier.weight(1.05f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(52.dp)) {
                    Column(Modifier.width(450.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LessonText("연습한 회차", 32, CoachColors.Platinum)
                        Box(Modifier.fillMaxWidth().height(250.dp).border(2.dp, CoachColors.Platinum, RoundedCornerShape(100)), contentAlignment = Alignment.Center) {
                            LessonText(report.attempts.size.toString().padStart(2, '0'), 164, CoachColors.Platinum)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        ShareLevel.SCORE_ONLY.includes.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                LessonText(item.removeSuffix(" 점수"), 36, CoachColors.Platinum, modifier = Modifier.weight(1f))
                                LessonText(certificateValue(report.attempts.firstOrNull(), item), 34, CoachColors.Platinum,
                                    modifier = Modifier.border(1.dp, CoachColors.Muted, RoundedCornerShape(100)).padding(horizontal = 24.dp, vertical = 8.dp))
                                LessonText("→", 30, CoachColors.Muted)
                                LessonText(certificateValue(report.attempts.lastOrNull(), item), 34,
                                    modifier = Modifier.background(CoachColors.Platinum, RoundedCornerShape(100)).padding(horizontal = 24.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareLevel.RAW.includes.drop(2).chunked(2).forEach { pair ->
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                            pair.forEach { item ->
                                val included = item in selected?.includes.orEmpty()
                                val opacity by animateFloatAsState(if (included) 1f else .3f, tween(240), label = "scope-row")
                                Column(Modifier.weight(1f).graphicsLayer { alpha = opacity }
                                    .testTag("share-item-$item").semantics(mergeDescendants = true) { stateDescription = if (included) "포함" else "범위 밖" }) {
                                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        LessonText(item, 34, CoachColors.Platinum, modifier = Modifier.weight(1f))
                                        LessonText(if (item.endsWith("시계열")) "기록 없음" else
                                            "${certificateValue(report.attempts.firstOrNull(), item)} → ${certificateValue(report.attempts.lastOrNull(), item)}",
                                            32, CoachColors.Platinum)
                                    }
                                    PosterRule(color = CoachColors.Muted)
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LessonText(reportDisclosure(report), 27, CoachColors.Platinum)
                        LessonText("출처는 회차 기준 · 원시 시계열은 저장하지 않음", 24, CoachColors.Muted)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(36.dp)) {
                LessonText("공유 범위 · 큰 숫자 = 담기는 항목 수", 30, CoachColors.Platinum)
                report.shareLevels.sortedByDescending { it.includes.size }.forEach { level ->
                    ScopePill(level, selected == level, { onSelect(level) }, Modifier.weight(1f).fillMaxWidth())
                }
                LessonText("고른 범위의 줄이 받는 쪽에 가요. 넓은 범위일수록 혜택이 늘어요 · 예시", 28, CoachColors.Platinum,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ScopePill(level: ShareLevel, chosen: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val scale by animateFloatAsState(if (chosen) 1f else .965f, spring(.8f, 400f), label = "scope-scale")
    val fill by animateFloatAsState(if (chosen) 1f else 0f, tween(200), label = "scope-fill")
    val flip by animateFloatAsState(if (chosen) 180f else 0f, tween(200), label = "scope-number-flip")
    val ink = lerp(CoachColors.Platinum, CoachColors.Ink, fill)
    Row(modifier.homeShared("scope-${level.name}").graphicsLayer { scaleX = scale; scaleY = scale }
        .background(lerp(CoachColors.Ink, CoachColors.Platinum, fill), RoundedCornerShape(100))
        .border(2.dp, CoachColors.Muted, RoundedCornerShape(100)).clickable(role = Role.RadioButton, onClick = onSelect)
        .semantics(mergeDescendants = true) { selected = chosen }.testTag("share-${level.name}")
        .padding(horizontal = 28.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Box(Modifier.size(124.dp).graphicsLayer { rotationY = flip }.background(CoachColors.Ink, RoundedCornerShape(100))
            .border(2.dp, CoachColors.Muted, RoundedCornerShape(100)), contentAlignment = Alignment.Center) {
            LessonText(level.includes.size.toString(), 68, CoachColors.Platinum, modifier = Modifier.graphicsLayer { rotationY = -flip })
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LessonText(level.label, 46, ink)
            LessonText(level.benefit, 30, ink)
        }
        if (level == ShareLevel.RAW) LessonText("기록 없음 ${level.includes.count { it.endsWith("시계열") }}", 24, ink)
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

/** A2: the selected scope becomes the recipient column; absent raw samples stay absent. */
@Composable
internal fun ShareExampleContent(report: LessonReport, selected: ShareLevel?, modifier: Modifier) {
    var recipient by rememberSaveable { mutableStateOf("본인") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(48.dp)) {
        CertificateHeader(report, "공유 예시 · ${selected?.label.orEmpty()}")
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            Row(Modifier.weight(1.5f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.width(265.dp).fillMaxHeight()) {
                    Spacer(Modifier.height(150.dp))
                    ShareLevel.RAW.includes.forEach { item ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { LessonText(item, 30, CoachColors.Platinum) }
                    }
                }
                report.shareLevels.forEach { level ->
                    val chosen = level == selected
                    val ink = if (chosen) CoachColors.Ink else CoachColors.Platinum
                    Column(Modifier.weight(1f).fillMaxHeight().then(if (chosen) Modifier.homeShared("scope-${level.name}") else Modifier)
                        .background(if (chosen) CoachColors.Platinum else CoachColors.Ink, RoundedCornerShape(60.dp)).padding(horizontal = 20.dp)) {
                        Column(Modifier.height(150.dp).fillMaxWidth().sharedTextArrival(260, 220), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            LessonText(level.label, 32, ink); LessonText(level.includes.size.toString(), 50, ink)
                        }
                        ShareLevel.RAW.includes.forEach { item ->
                            Box(Modifier.weight(1f).fillMaxWidth().sharedTextArrival(260, 220).then(if (chosen) Modifier.testTag("share-item-$item")
                                .semantics(mergeDescendants = true) { stateDescription = if (item in level.includes) "포함" else "범위 밖" } else Modifier), contentAlignment = Alignment.Center) {
                                LessonText(when {
                                    item !in level.includes -> if (chosen) "범위 밖" else "—"
                                    item.endsWith("시계열") -> "기록 없음"
                                    chosen -> "${certificateValue(report.attempts.firstOrNull(), item)} → ${certificateValue(report.attempts.lastOrNull(), item)}"
                                    else -> "✓"
                                }, 30, ink)
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(.85f), verticalArrangement = Arrangement.spacedBy(30.dp)) {
                LessonText("발급 대상", 32, CoachColors.Platinum)
                SelectionTrack(listOf("본인", "동승자", "기관"), recipient, { it }, { recipient = it }, Modifier.fillMaxWidth(), height = 96.dp, textSize = 32,
                    background = CoachColors.Periwinkle, selectedBackground = CoachColors.Platinum, foreground = CoachColors.Platinum, selectedForeground = CoachColors.Ink)
                LessonText("기간   ${report.attempts.firstOrNull()?.index ?: "—"}회차 → ${report.attempts.lastOrNull()?.index ?: "—"}회차", 32, CoachColors.Platinum)
                LessonText("과제   ${report.task.title}", 32, CoachColors.Platinum)
                LessonText("유효   발급일부터 석 달 · 예시", 32, CoachColors.Platinum)
                LessonText("${ShareLevel.EXCLUDED.joinToString(" · ")} 없음", 30, CoachColors.Platinum)
                LessonText("채운 기둥 = 받는 쪽이 보는 값 · 시계열은 기록 없음", 26, CoachColors.Muted)
            }
            Column(Modifier.weight(.75f).fillMaxHeight().border(12.dp, CoachColors.Jet, RoundedCornerShape(72.dp))
                .background(CoachColors.Paper, RoundedCornerShape(72.dp)).padding(36.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LessonText("예시 — 실제로 보낸 진단서가 아니에요", 18)
                LessonText("DRIVE COACH · $recipient", 20)
                LessonText("진단서 · ${selected?.label.orEmpty()}", 30)
                Row(Modifier.fillMaxWidth().background(CoachColors.Ink,RoundedCornerShape(32.dp)).padding(12.dp),horizontalArrangement=Arrangement.SpaceAround) {
                    ShareLevel.SCORE_ONLY.includes.forEach { item ->
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            LessonText(item.removeSuffix(" 점수"),20,CoachColors.Platinum)
                            LessonText(certificateValue(report.attempts.lastOrNull(),item),32,CoachColors.Paper)
                        }
                    }
                }
                selected?.includes?.drop(2)?.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LessonText(item, 20, CoachColors.Muted)
                        LessonText(certificateValue(report.attempts.lastOrNull(), item), 22)
                    }
                }
                Spacer(Modifier.weight(1f))
                LessonText("보내지 않은 것: ${(ShareLevel.RAW.includes.filter { it !in selected?.includes.orEmpty() } + ShareLevel.EXCLUDED).joinToString(" · ")}", 18, CoachColors.Muted)
                LessonText("발급일부터 석 달 · 예시", 20, CoachColors.Muted)
            }
        }
    }
}
