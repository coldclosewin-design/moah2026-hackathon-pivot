package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.moah.hackathon.data.TrackCourses
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

@Composable
internal fun CoachPill(label: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Row(modifier.height(112.dp)
        .then(if (onClick == null) Modifier.alpha(.55f).background(CoachColors.Lavender, RoundedCornerShape(100))
            else Modifier.background(CoachColors.Paper, RoundedCornerShape(100))
                .border(1.5.dp, CoachColors.Ink, RoundedCornerShape(100)))
        .clip(RoundedCornerShape(100))
        .then(if (onClick == null) Modifier.semantics(mergeDescendants = true) { disabled(); role = Role.Button }
            else Modifier.clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick))
        .padding(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        SymbolTile(CoachSymbol.Listen, Modifier.size(56.dp).testTag("coach-microphone").clearAndSetSemantics {}, animate = pressed)
        LessonText(label, 36, maxLines = 1)
    }
}

@Composable
internal fun CoachSheet(coach: CoachDialog, onChoose: (CoachChoice) -> Unit, onBack: () -> Unit,
    inputMode: CoachInputMode = CoachInputMode.OFF, onSend: (String) -> Unit = {}, onSendCard: (String) -> Unit = {}) {
    if (inputMode != CoachInputMode.OFF) {
        CoachTextSheet(coach, onChoose, onBack, onSend, inputMode, onSendCard)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(104.dp)) // The enclosing sheet starts at 64 dp; the eyebrow is at 168.
        Eyebrow("코치와 대화")
        Spacer(Modifier.height(27.dp))
        Column(Modifier.fillMaxWidth()
            .surfaceTexture(CoachColors.Lavender, CoachTexture.Card,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomEnd = 36.dp, bottomStart = 9.dp))
            .padding(horizontal = 36.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow("코치")
            CoachLines(coach.line, titleSize = 52, adviceSize = 44)
        }
        Spacer(Modifier.height(33.dp))
        CoachChoices(coach.choices, onChoose)
        Spacer(Modifier.height(18.dp))
        LessonText("고르면 바로 그 자리로 가요.", 32, CoachColors.Muted)
        Spacer(Modifier.height(57.dp))
        CoachPill("말로 답하기 — 준비 중", null)
        Spacer(Modifier.weight(1f))
        BottomActions(secondary = { BackPill(onBack) })
    }
}

@Composable
internal fun CoachChoices(choices: List<CoachChoice>, onChoose: (CoachChoice) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        choices.forEach { choice ->
            Box(Modifier.height(102.dp).surfaceTexture(CoachColors.Lavender, CoachTexture.Chip)
                .clickable(role = Role.Button) { onChoose(choice) }.padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center) { LessonText(choice.label, 36, CoachColors.Periwinkle, maxLines = 1) }
        }
    }
}

@Composable
internal fun HomeBookingCard(details: BookingDetails, tasks: List<Task>, options: List<BookingOption>,
    chosen: BookingOption?, highlighted: Boolean, onChoose: (BookingOption) -> Unit) {
    Row(Modifier.fillMaxWidth().height(198.dp).testTag("home-booking-card")
        .surfaceTexture(CoachColors.Paper, CoachTexture.Card)
        .then(if (highlighted) Modifier.border(6.dp, CoachColors.Ink) else Modifier)
        .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        val course = tasks.firstOrNull { it.id in details.course.taskIds && it.course != null }?.course ?: TrackCourses.exam
        CourseMap(course, Modifier.size(width = 162.dp, height = 144.dp).background(CoachColors.Ink).padding(12.dp),
            thumbnail = true, ink = CoachColors.Lavender, previewRoute = CoachColors.Periwinkle)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LessonText("예약 · ${details.venue.area} ${details.slot.start}", 28, CoachColors.Periwinkle)
            LessonText("예약한 ${details.venue.name} · ${details.course.title}", 32)
            LessonText("거기서 할 걸 골라요", 30, CoachColors.Muted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val selected = option == chosen
                Box(Modifier.width(200.dp).height(78.dp)
                    .surfaceTexture(if (selected) CoachColors.Ink else CoachColors.Lavender,
                        if (selected) CoachTexture.SelectedChip else CoachTexture.Chip)
                    .clickable(role = Role.RadioButton) { onChoose(option) }.semantics { this.selected = selected },
                    contentAlignment = Alignment.Center) {
                    LessonText(option.label, 32, if (selected) CoachColors.Paper else CoachColors.Periwinkle, maxLines = 1)
                }
            }
        }
    }
}

/** H8: the start circle is the action; the title and profile sit on one white wall. */
@Composable
internal fun HomeGallery(profile: Profile, task: Task, mode: LessonMode, picked: Boolean, compact: Boolean,
    onAdmin: (() -> Unit)?, onProfile: () -> Unit, onCoach: () -> Unit, onTask: () -> Unit,
    onMode: (LessonMode) -> Unit, onStart: () -> Unit, hasBooking: Boolean, reason: String, booking: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 120.dp, end = 120.dp, top = 52.dp, bottom = 36.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            SetupBrandMark(onAdmin = onAdmin)
            CoachPill("코치와 대화", onCoach)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(bottom = if (hasBooking) 32.dp else 0.dp)) {
            val readingHeight = maxHeight
            Column(Modifier.width(620.dp).align(Alignment.TopEnd).heightIn(min = 112.dp).testTag("profile-entry")
                .clickable(role = Role.Button, onClick = onProfile).padding(top = 12.dp)) {
                Eyebrow("프로필", color = CoachColors.Periwinkle)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LessonText(profileLine(profile), 36, modifier = Modifier.weight(1f))
                    LessonText("›", 36)
                }
            }
            Column(Modifier.width(1630.dp).align(Alignment.CenterStart)) {
                HomeTaskTitle(task, mode, picked, compact, onTask, onMode,
                    titleSize = when { readingHeight < 400.dp -> 64; readingHeight < 520.dp -> 80; else -> null })
                Spacer(Modifier.height(20.dp))
                LessonText(reason, 36, CoachColors.Muted, modifier = Modifier.testTag("home-reason"))
            }
        }
        booking()
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            FillButton("시작", onStart, Modifier.width(1410.dp).height(if (hasBooking) { if (compact) 160.dp else 240.dp } else if (compact) 240.dp else 300.dp), picked, home = true)
        }
        Spacer(Modifier.height(36.dp))
        PosterRule(color = CoachColors.Platinum)
    }
}
