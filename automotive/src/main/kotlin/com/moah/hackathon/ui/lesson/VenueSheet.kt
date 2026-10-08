package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.data.TrackCourses
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors

/** C1: rounded map cards and one white plate for both booking tracks. */
@Composable
internal fun VenueSheet(venues: List<Venue>, booking: Reservation?, onReserve: (String, String, String) -> Unit,
    onCancel: () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    var venueId by rememberSaveable { mutableStateOf<String?>(null) }
    var slotId by rememberSaveable(venueId) { mutableStateOf<String?>(null) }
    var courseId by rememberSaveable(venueId) { mutableStateOf<String?>(null) }
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
            BottomActions(secondary = { BackPill(onDone) }, primary = { TextAction("취소", { onCancel(); venueId = null }) })
        } else {
            Row(Modifier.weight(1f).fillMaxWidth().testTag("venue-choice-card"), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                venues.forEach { item -> VenueCard(item, item.id == venueId, Modifier.weight(1f).fillMaxHeight()) { venueId = item.id } }
            }
            Column(Modifier.fillMaxWidth().height(360.dp).shadow(8.dp, RoundedCornerShape(56.dp))
                .background(CoachColors.Paper, RoundedCornerShape(56.dp)).padding(40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (venue == null) LessonText("연습할 시험장을 골라 주세요.", 48, CoachColors.Muted)
                else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Eyebrow("시간", Modifier.width(120.dp), CoachColors.Muted)
                        SelectionTrack(venue.slots, slot, { it.label }, { slotId = it.id }, Modifier.weight(1f), height = 104.dp, textSize = 40, enabled = { it.available })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Eyebrow("코스", Modifier.width(120.dp), CoachColors.Muted)
                        SelectionTrack(venue.courses, course, { it.title }, { courseId = it.id }, Modifier.weight(1f), height = 104.dp, textSize = 40)
                    }
                    LessonText("${venue.name} · 오늘 · 장내 코스는 시험장 신호(시뮬레이션)로 채점해요.", 32, CoachColors.Muted, modifier = Modifier.padding(start = 152.dp))
                }
            }
            Row(Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                BackPill { if (venue != null) venueId = null else onBack() }
                LessonText(Reservation.EXAMPLE_NOTE, 32, CoachColors.Muted, modifier = Modifier.weight(1f))
                PrimaryPill("예약", { if (venue != null && slot != null && course != null) onReserve(venue.id, slot.id, course.id) }, Modifier.width(1000.dp), enabled = venue != null && slot != null && course != null)
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val foreground = if (chosen) CoachColors.Paper else CoachColors.Ink
    Column(modifier.shadow(if (chosen) 8.dp else 3.dp, RoundedCornerShape(44.dp))
        .background(if (chosen) CoachColors.Ink else CoachColors.Paper, RoundedCornerShape(44.dp))
        .clickable(role = Role.RadioButton, onClick = onClick).semantics(mergeDescendants = true) { selected = chosen }
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(CoachColors.Periwinkle)) {
            val course = SeedCatalog.tasks.firstOrNull { it.id == SeedCatalog.TASK_TRACK_EXAM && it.id in venue.courses.flatMap { it.taskIds } }?.course ?: TrackCourses.exam
            CourseMap(course, Modifier.fillMaxSize().padding(48.dp), thumbnail = true, ink = CoachColors.Paper, previewRoute = CoachColors.Paper)
            LessonText(venueAreaLine(venue), 30, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                .background(CoachColors.Paper, RoundedCornerShape(100)).padding(horizontal = 20.dp, vertical = 6.dp))
            if (chosen) LessonText("✓", 36, modifier = Modifier.padding(20.dp).background(CoachColors.Paper, RoundedCornerShape(100)).padding(horizontal = 12.dp))
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LessonText(venue.name, 48, foreground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                venue.courses.forEach { course ->
                    LessonText(course.title, 24, foreground, modifier = Modifier.clearAndSetSemantics {}.background(foreground.copy(alpha = .10f), RoundedCornerShape(100)).padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            LessonText(if (venue.slots.any { it.available }) "● 오늘 자리 있음" else "오늘 마감", 32, foreground)
        }
    }
}
