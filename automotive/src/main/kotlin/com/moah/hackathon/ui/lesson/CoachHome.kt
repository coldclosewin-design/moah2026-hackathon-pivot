package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
        .then(if (onClick == null) Modifier.alpha(.55f).background(CoachColors.Lavender, RoundedCornerShape(56.dp))
            else Modifier.background(CoachColors.Paper, RoundedCornerShape(56.dp))
                .border(1.5.dp, CoachColors.Ink, RoundedCornerShape(56.dp)))
        .clip(RoundedCornerShape(56.dp))
        .then(if (onClick == null) Modifier.semantics(mergeDescendants = true) { disabled(); role = Role.Button }
            else Modifier.clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick))
        .padding(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val words = if (label == "코치와 대화") Modifier.coachTextArrival() else Modifier
        SymbolTile(CoachSymbol.Listen, words.size(56.dp).testTag("coach-microphone").clearAndSetSemantics {}, animate = pressed)
        LessonText(label, 36, maxLines = 1, modifier = words)
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
        Spacer(Modifier.height(16.dp)) // The enclosing sheet starts at 64 dp; the eyebrow is at 168.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) { YellowModeIcon(conversation = true); Eyebrow("코치와 대화") }
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

/** B2: one white ticket beside the start circle; its radios stay available on the home. */
@Composable
internal fun HomeBookingCard(details: BookingDetails, tasks: List<Task>, options: List<BookingOption>,
    chosen: BookingOption?, onOpen: () -> Unit, onChoose: (BookingOption) -> Unit) {
    Row(Modifier.fillMaxWidth().height(348.dp).homeShared("booking-ticket")
        .shadow(16.dp, RoundedCornerShape(66.dp)).background(CoachColors.Paper, RoundedCornerShape(66.dp))
        .clickable(role = Role.Button, onClick = onOpen).semantics { contentDescription = "예약 카드 열기" }
        .padding(42.dp).testTag("home-booking-card"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        BookingMap(details, tasks, Modifier.size(264.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LessonText("예약 · ${details.venue.area} ${details.slot.start} · 예시", 36, CoachColors.Muted)
            LessonText("${details.venue.name} · ${details.course.title}", 54, maxLines = 1)
            BookingOptions(options, chosen, onChoose)
        }
        LessonText("›", 52, modifier = Modifier.semantics { contentDescription = "거기서 할 걸 골라요" })
    }
}

@Composable
private fun BookingMap(details: BookingDetails, tasks: List<Task>, modifier: Modifier) {
    val course = tasks.firstOrNull { it.id in details.course.taskIds && it.course != null }?.course ?: TrackCourses.exam
    CourseMap(course, modifier.background(CoachColors.Ink, RoundedCornerShape(42.dp)).padding(36.dp),
        thumbnail = true, ink = CoachColors.Paper, previewRoute = CoachColors.Paper)
}

@Composable
private fun BookingOptions(options: List<BookingOption>, chosen: BookingOption?, onChoose: (BookingOption) -> Unit) {
    Row(Modifier.semantics { contentDescription = "예약에서 할 일" }, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        options.forEach { option ->
            val selected = option == chosen
            Row(Modifier.height(100.dp).background(if (selected) CoachColors.Ink else CoachColors.Lavender, RoundedCornerShape(100))
                .clickable(role = Role.RadioButton) { onChoose(option) }.semantics { this.selected = selected }
                .padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (selected) LessonText("✓", 36, CoachColors.Paper)
                LessonText(option.label, 38, if (selected) CoachColors.Paper else CoachColors.Muted)
            }
        }
    }
}

@Composable
internal fun BookingTicketSheet(details: BookingDetails, tasks: List<Task>, options: List<BookingOption>,
    chosen: BookingOption?, onChoose: (BookingOption) -> Unit, onBack: () -> Unit, onEdit: () -> Unit, onSlot: (String) -> Unit,
    onCancel: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(CoachColors.Lavender).padding(80.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        BrandMark()
        Column(Modifier.weight(1f).fillMaxWidth().homeShared("booking-ticket")
            .shadow(16.dp, RoundedCornerShape(66.dp)).background(CoachColors.Paper, RoundedCornerShape(66.dp))
            .padding(48.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                BookingMap(details, tasks, Modifier.size(300.dp))
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Eyebrow("제휴 시험장 · 오늘", color = CoachColors.Muted)
                    Headline(details.venue.name, size = 88)
                    LessonText(details.course.title, 48, CoachColors.Muted)
                    LessonText(Reservation.EXAMPLE_NOTE, 32, CoachColors.Muted)
                }
            }
            SelectionTrack(details.venue.slots, details.slot, { it.label }, { onSlot(it.id) },
                Modifier.fillMaxWidth(), height = 104.dp, textSize = 40, enabled = { it.available })
            BookingOptions(options, chosen, onChoose)
            TextAction("시간·코스 바꾸기", onEdit)
        }
        BottomActions(secondary = {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) { BackPill(onBack); ArrowPill("예약 취소", "×", onCancel) }
        }, primary = { PrimaryPill("이 코스로", onBack) })
    }
}

/** H8/P3: booking never changes the title size or reserves a separate bottom row. */
@Composable
internal fun HomeGallery(profile: Profile, task: Task, mode: LessonMode, picked: Boolean, compact: Boolean,
    onAdmin: (() -> Unit)?, onProfile: () -> Unit, onCoach: () -> Unit, onTask: () -> Unit,
    onMode: (LessonMode) -> Unit, onStart: () -> Unit, hasBooking: Boolean, reason: String,
    showCoach: Boolean = true, booking: @Composable () -> Unit) {
    var wheelOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(start = 120.dp, end = 120.dp, top = 52.dp, bottom = 36.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SetupBrandMark(onAdmin = onAdmin)
            if (showCoach) CoachPill("코치와 대화", onCoach, Modifier.homeShared("coach-surface").alpha(if (wheelOpen) .18f else 1f))
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            ProfileTableEntry(profile, onProfile, Modifier.align(Alignment.TopEnd).alpha(if (wheelOpen) .18f else 1f))
            Column(Modifier.width(if (wheelOpen) 2160.dp else 1700.dp).align(Alignment.CenterStart).padding(top = 100.dp)) {
                HomeTaskTitle(task, mode, picked, compact, onTask, onMode, onWheelChanged = { wheelOpen = it })
                Spacer(Modifier.height(20.dp))
                LessonText(reason, 36, CoachColors.Muted, modifier = Modifier.padding(start = if (wheelOpen) 500.dp else 0.dp).testTag("home-reason"))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().height(if (compact) 280.dp else 348.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hasBooking) {
                Box(Modifier.weight(1f).alpha(if (wheelOpen) .18f else 1f)) { booking() }
                FillButton("시작", onStart, Modifier.width(610.dp).height(300.dp), picked, home = true)
            } else {
                Box(Modifier.weight(1f)) {
                    ProfileHomeVehicle(profile.statement.car, Modifier.alpha(if (wheelOpen) .18f else 1f).width(780.dp).height(if (compact) 250.dp else 300.dp).align(Alignment.CenterStart))
                }
                FillButton("시작", onStart, Modifier.alpha(if (wheelOpen) .18f else 1f).width(1410.dp).height(if (compact) 240.dp else 300.dp), picked, home = true)
            }
        }
        Spacer(Modifier.height(32.dp))
        PosterRule(color = CoachColors.Platinum)
    }
}

@Composable
private fun ProfileTableEntry(profile: Profile, onClick: () -> Unit, modifier: Modifier) {
    val parts = profileLine(profile).split(" · ")
    Row(modifier.heightIn(min = 140.dp).clickable(role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = "프로필 열기" }.testTag("profile-entry").padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("단계" to parts.getOrElse(0) { "연수생" },
            "장롱" to parts.getOrElse(1) { "아직" }.removePrefix("장롱 "),
            "목표" to parts.getOrElse(2) { "아직" }.removePrefix("목표: ")).forEachIndexed { index, (label, value) ->
            if (index > 0) Box(Modifier.width(2.dp).height(104.dp).background(CoachColors.Platinum))
            Column(Modifier.widthIn(min = 150.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LessonText(label, 32, CoachColors.Muted)
                LessonText(value, 47, bold = true)
            }
        }
        LessonText("›", 42)
    }
}
