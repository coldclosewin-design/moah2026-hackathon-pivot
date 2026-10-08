package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

/** V1: words select the raised card; the booking action always stays below that card. */
@Composable
internal fun VenueSheet(venues: List<Venue>, booking: Reservation?, onReserve: (String, String, String) -> Unit,
    onCancel: () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    var venueId by rememberSaveable { mutableStateOf<String?>(null) }
    var slotId by rememberSaveable(venueId) { mutableStateOf<String?>(null) }
    var courseId by rememberSaveable(venueId) { mutableStateOf<String?>(null) }
    var editor by rememberSaveable { mutableStateOf<String?>("venue") }
    val venue = venues.firstOrNull { it.id == venueId }
    val confirmation = bookingDetails(booking, venues)?.takeIf { it.venue.id == venueId }
    val slot = venue?.slots?.firstOrNull { it.id == slotId && it.available }
    val course = venue?.courses?.firstOrNull { it.id == courseId }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CoachColors.Lavender, CoachColors.Platinum)))
        .padding(72.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        BrandMark()
        Eyebrow(if (confirmation != null) "예약 확인" else "제휴 시험장")
        if (confirmation != null) {
            SheetCard(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().padding(64.dp), verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically)) {
                    Eyebrow("예약됨")
                    Headline(confirmation.summary, size = 88)
                    LessonText(confirmation.venue.name, 48, CoachColors.Muted)
                    LessonText(Reservation.EXAMPLE_NOTE, 36, CoachColors.Muted)
                }
            }
            BottomActions(secondary = { BackPill(onDone) }, primary = { TextAction("취소", { onCancel(); venueId = null; editor = "venue" }) })
        } else {
            val size = if (editor == null) 96 else 80
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                YellowContextIcon(ContextIcon.Pin)
                WordPill(venue?.name ?: "시험장", "시험장 바꾸기", size, editor == "venue", placeholder = venue == null) { editor = "venue" }
                LessonText("에서", size)
                WordPill(slot?.label ?: "시간", "시간 바꾸기", size, editor == "slot", placeholder = slot == null) { editor = if (venue == null) "venue" else "slot" }
                LessonText("에", size)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                WordPill(course?.title ?: "코스", "코스 바꾸기", size, editor == "course", placeholder = course == null) { editor = if (venue == null) "venue" else "course" }
                LessonText("를", size)
                LessonText("연습해요.", size, CoachColors.Muted)
            }
            if (editor != null) key(editor, venueId) {
                SheetCard(Modifier.weight(1f).fillMaxWidth().testTag("venue-choice-card")) {
                    Column(Modifier.fillMaxSize().padding(44.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Eyebrow(when (editor) { "slot" -> "시간 · 오늘 · ${venue?.name.orEmpty()}"; "course" -> "코스 · ${venue?.name.orEmpty()}"; else -> "연습할 시험장" })
                            TextAction("닫기", { editor = null }, size = 32)
                        }
                        when (editor) {
                            "venue" -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                venues.forEach { item ->
                                    VenueChoice(item.name, venueAreaLine(item), item.id == venueId, true, Modifier.weight(1f).fillMaxHeight()) {
                                        venueId = item.id; editor = "slot"
                                    }
                                }
                            }
                            "slot" -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                venue?.slots?.forEach { item ->
                                    VenueChoice(item.label, if (item.available) "자리 있음" else "마감", item.id == slotId, item.available, Modifier.weight(1f).fillMaxHeight()) {
                                        slotId = item.id; editor = "course"
                                    }
                                }
                            }
                            "course" -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                venue?.courses?.forEach { item ->
                                    VenueChoice(item.title, "연습 코스", item.id == courseId, true, Modifier.weight(1f).fillMaxHeight()) {
                                        courseId = item.id; editor = null
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                venue?.let { LessonText(venueAreaLine(it), 36, CoachColors.Muted) }
                Spacer(Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                BackPill { if (venue != null) { venueId = null; editor = "venue" } else onBack() }
                LessonText(Reservation.EXAMPLE_NOTE, 32, CoachColors.Muted, modifier = Modifier.weight(1f))
                PrimaryPill("예약", { if (venue != null && slot != null && course != null) onReserve(venue.id, slot.id, course.id) },
                    Modifier.width(760.dp), enabled = venue != null && slot != null && course != null)
            }
        }
    }
}

@Composable
private fun VenueChoice(label: String, detail: String, chosen: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.shadow(if (enabled) 3.dp else 0.dp, RoundedCornerShape(40.dp)).background(if (chosen) CoachColors.Ink else if (enabled) CoachColors.Paper else CoachColors.Lavender, RoundedCornerShape(40.dp))
        .then(if (enabled) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier.semantics { disabled() })
        .semantics(mergeDescendants = true) { selected = chosen && enabled }.padding(36.dp), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
        LessonText(AnnotatedString(label, SpanStyle(textDecoration = if (enabled) TextDecoration.None else TextDecoration.LineThrough)), 56, if (chosen) CoachColors.Paper else if (enabled) CoachColors.Ink else CoachColors.Muted)
        LessonText(detail, 36, if (chosen) CoachColors.Platinum else CoachColors.Muted)
        if (!enabled) LessonText("누를 수 없음", 32, CoachColors.Muted)
        if (chosen) LessonText("✓", 36, CoachColors.Paper)
    }
}
