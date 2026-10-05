package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.Reservation
import com.moah.hackathon.feature.lesson.Venue
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

@Composable
internal fun VenueSheet(venues: List<Venue>, booking: Reservation?, onReserve: (String, String, String) -> Unit,
    onCancel: () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    var venueId by rememberSaveable { mutableStateOf<String?>(null) }
    var slotId by rememberSaveable(venueId) { mutableStateOf<String?>(null) }
    var courseId by rememberSaveable(venueId) { mutableStateOf<String?>(null) }
    val venue = venues.firstOrNull { it.id == venueId }
    val confirmed = bookingDetails(booking, venues)
    val confirmation = confirmed?.takeIf { it.venue.id == venueId }
    val slot = venue?.slots?.firstOrNull { it.id == slotId && it.available }
    val course = venue?.courses?.firstOrNull { it.id == courseId }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Eyebrow(if (confirmation != null) "예약 확인" else "제휴 시험장")
        LessonText(Reservation.EXAMPLE_NOTE, 40, CoachColors.Periwinkle)
        if (confirmation != null) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                Column(Modifier.fillMaxWidth().surfaceTexture(CoachColors.Lavender, CoachTexture.Card).padding(48.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Eyebrow("예약됨", color = CoachColors.Periwinkle)
                    Headline(confirmation.summary, size = 64)
                    LessonText(confirmation.venue.name, 40, CoachColors.Muted)
                }
            }
            Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                TextAction(stringResource(R.string.lesson_back), onDone)
                TextAction(stringResource(R.string.lesson_cancel), { onCancel(); venueId = null })
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                venues.forEach { item ->
                    VenueCard(item, item.id == venueId, confirmed?.venue?.id == item.id, Modifier.weight(1f)) { venueId = item.id }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
                if (venue != null) {
                    Eyebrow("시간")
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        venue.slots.forEach { item ->
                            if (item.available) SelectionChip(item.label, item.id == slotId, { slotId = item.id }, Modifier.width(360.dp))
                            else Box(Modifier.width(360.dp).height(96.dp).surfaceTexture(CoachColors.Lavender, CoachTexture.Chip)
                                .semantics(mergeDescendants = true) { disabled() }, contentAlignment = Alignment.Center) {
                                LessonText(item.label, 40, CoachColors.Muted)
                            }
                        }
                    }
                    Eyebrow("코스")
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        venue.courses.forEach { item ->
                            SelectionChip(item.title, item.id == courseId, { courseId = item.id }, Modifier.width(360.dp))
                        }
                    }
                } else LessonText(when {
                    venues.isEmpty() -> "이용 가능한 시험장이 없어요."
                    booking != null -> "예약한 시험장을 누르면 확인할 수 있어요."
                    else -> "연습할 시험장을 골라 주세요."
                }, 48)
            }
            Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                TextAction(stringResource(R.string.lesson_back), { if (venue != null) venueId = null else onBack() })
                if (venue != null && slot != null && course != null) {
                    PrimaryPill(stringResource(R.string.lesson_reserve), { onReserve(venue.id, slot.id, course.id) })
                }
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, chosen: Boolean, booked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val foreground = if (booked) CoachColors.Paper else CoachColors.Ink
    val secondary = if (booked) CoachColors.Paper else CoachColors.Muted
    Column(modifier.height(264.dp).surfaceTexture(if (booked) CoachColors.Periwinkle else CoachColors.Lavender,
        if (booked) CoachTexture.SelectedCard else CoachTexture.Card)
        .then(if (chosen) Modifier.border(4.dp, CoachColors.Periwinkle) else Modifier)
        .clickable(role = Role.Button, onClick = onClick).semantics(mergeDescendants = true) { selected = chosen }
        .padding(horizontal = 24.dp, vertical = 4.dp)) {
        LessonText(venue.name, 40, foreground)
        LessonText(venueAreaLine(venue), 32, secondary)
        LessonText(venue.courses.joinToString(" · ") { it.title }, 32, secondary)
        LessonText(if (booked) "예약됨" else if (venue.slots.any { it.available }) "오늘 자리 있음" else "오늘 자리 없음", 32, foreground)
    }
}
