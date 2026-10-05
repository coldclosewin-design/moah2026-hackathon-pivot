package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.moah.hackathon.data.TrackCourses
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
                BackPill(onDone)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow("시간", Modifier.width(100.dp))
                        venue.slots.forEach { item ->
                            if (item.available) SelectionChip(item.label, item.id == slotId, { slotId = item.id }, Modifier.width(360.dp))
                            else Box(Modifier.width(360.dp).height(96.dp).surfaceTexture(CoachColors.Lavender, CoachTexture.Chip)
                                .semantics(mergeDescendants = true) { disabled() }, contentAlignment = Alignment.Center) {
                                LessonText(item.label, 40, CoachColors.Muted)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow("코스", Modifier.width(100.dp))
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
                BackPill { if (venue != null) venueId = null else onBack() }
                if (venue != null && slot != null && course != null) {
                    PrimaryPill(stringResource(R.string.lesson_reserve), { onReserve(venue.id, slot.id, course.id) })
                }
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, chosen: Boolean, booked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(512.dp).surfaceTexture(CoachColors.Paper, CoachTexture.Card)
        .then(if (chosen) Modifier.border(6.dp, CoachColors.Signal) else Modifier)
        .clickable(role = Role.Button, onClick = onClick).semantics(mergeDescendants = true) { selected = chosen }
        .padding(6.dp)) {
        Box(Modifier.fillMaxWidth().height(180.dp).background(CoachColors.Ink)) {
            CourseMap(TrackCourses.exam, Modifier.fillMaxSize().padding(16.dp), thumbnail = true,
                ink = CoachColors.Lavender, previewRoute = if (chosen) CoachColors.Signal else CoachColors.Periwinkle)
            LessonText(venueAreaLine(venue), 36, CoachColors.Ink,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .background(CoachColors.Paper.copy(alpha = .92f), RoundedCornerShape(100)).padding(horizontal = 20.dp, vertical = 4.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LessonText(venue.name, 48)
            LessonText(venue.courses.joinToString(" · ") { it.title }, 34, CoachColors.Muted)
            LessonText(if (booked) "예약됨" else if (venue.slots.any { it.available }) "오늘 자리 있음" else "오늘 자리 없음", 36,
                if (booked) CoachColors.Signal else CoachColors.Periwinkle)
        }
    }
}
