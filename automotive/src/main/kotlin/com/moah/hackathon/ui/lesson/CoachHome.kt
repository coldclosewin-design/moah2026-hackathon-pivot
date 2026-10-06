package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
    Row(modifier.height(112.dp)
        .then(if (onClick == null) Modifier.alpha(.55f).background(CoachColors.Lavender, RoundedCornerShape(100))
            else Modifier.surfaceTexture(CoachColors.Lavender, CoachTexture.Chip, pill = true))
        .clip(RoundedCornerShape(100))
        .then(if (onClick == null) Modifier.semantics(mergeDescendants = true) { disabled(); role = Role.Button }
            else Modifier.clickable(role = Role.Button, onClick = onClick))
        .padding(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(40.dp).testTag("coach-microphone")) {
            val ink = CoachColors.Periwinkle
            drawRoundRect(ink, Offset(size.width * .34f, 0f), Size(size.width * .32f, size.height * .6f), CornerRadius(size.width * .16f))
            drawArc(ink, 0f, 180f, false, Offset(size.width * .12f, size.height * .2f),
                Size(size.width * .76f, size.height * .6f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            drawLine(ink, Offset(size.width / 2, size.height * .8f), Offset(size.width / 2, size.height), 3.dp.toPx())
            drawLine(ink, Offset(size.width * .3f, size.height), Offset(size.width * .7f, size.height), 3.dp.toPx())
        }
        LessonText(label, 36, maxLines = 1)
    }
}

@Composable
internal fun ProfilePill(label: String, onClick: () -> Unit) {
    Row(Modifier.heightIn(min = 78.dp).testTag("profile-entry")
        .surfaceTexture(CoachColors.Lavender, CoachTexture.Chip, pill = true)
        .clip(RoundedCornerShape(100)).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(28.dp)) {
            drawCircle(CoachColors.Periwinkle, size.width * .17f, Offset(size.width / 2, size.height * .23f))
            drawArc(CoachColors.Periwinkle, 180f, 180f, true,
                Offset(size.width * .12f, size.height * .50f), Size(size.width * .76f, size.height * .78f))
        }
        LessonText(label, 32, CoachColors.Periwinkle, maxLines = 1)
        LessonText("›", 32, CoachColors.Periwinkle)
    }
}

@Composable
internal fun CoachSheet(coach: CoachDialog, onChoose: (CoachChoice) -> Unit, onBack: () -> Unit,
    textInput: Boolean = false, onSend: (String) -> Unit = {}) {
    if (textInput) {
        CoachTextSheet(coach, onChoose, onBack, onSend)
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
            LessonText(coach.line, 48)
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
                contentAlignment = Alignment.Center) { LessonText(choice.label, 36, maxLines = 1) }
        }
    }
}

@Composable
internal fun HomeBookingCard(details: BookingDetails, tasks: List<Task>, options: List<BookingOption>,
    chosen: BookingOption?, highlighted: Boolean, onChoose: (BookingOption) -> Unit) {
    Row(Modifier.fillMaxWidth().height(198.dp).testTag("home-booking-card")
        .surfaceTexture(CoachColors.Paper, CoachTexture.Card)
        .then(if (highlighted) Modifier.border(6.dp, CoachColors.Signal) else Modifier)
        .padding(horizontal = 24.dp, vertical = 12.dp),
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
                    .surfaceTexture(if (selected) CoachColors.Periwinkle else CoachColors.Lavender,
                        if (selected) CoachTexture.SelectedChip else CoachTexture.Chip)
                    .clickable(role = Role.RadioButton) { onChoose(option) }.semantics { this.selected = selected },
                    contentAlignment = Alignment.Center) {
                    LessonText(option.label, 32, if (selected) CoachColors.Paper else CoachColors.Ink, maxLines = 1)
                }
            }
        }
    }
}
