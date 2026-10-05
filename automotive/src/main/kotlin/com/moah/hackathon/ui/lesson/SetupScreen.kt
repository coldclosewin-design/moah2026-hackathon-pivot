package com.moah.hackathon.ui.lesson

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun SetupScreen(profile: Profile, tasks: List<Task>, suggestedTask: Task, suggestedMode: LessonMode,
    reason: String, subtitle: String?, onBegin: (String, LessonMode) -> Unit,
    demo: (@Composable () -> Unit)? = null, venues: List<Venue> = emptyList(), booking: Reservation? = null,
    onReserve: (String, String, String) -> Unit = { _, _, _ -> }, onCancelReservation: () -> Unit = {},
    coach: CoachDialog? = null, bookingOptions: List<BookingOption> = emptyList(), bookingChoice: BookingOption? = null,
    highlightBooking: Boolean = false, sheetRequest: TaskType? = null, onOpenCoach: () -> Unit = {},
    onChooseCoach: (CoachChoice) -> Unit = {}, onCloseCoach: () -> Unit = {},
    onChooseBooking: (BookingOption) -> Unit = {}, onConsumeSheetRequest: () -> Unit = {}) {
    var selectedTaskId by rememberSaveable(suggestedTask.id, booking) { mutableStateOf(suggestedTask.id) }
    var categoryName by rememberSaveable(suggestedTask.id, booking) { mutableStateOf(suggestedTask.type.name) }
    var modeName by rememberSaveable(suggestedMode, booking) { mutableStateOf(suggestedMode.name) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var venuesOpen by rememberSaveable { mutableStateOf(false) }
    var awaitingRecommendation by remember { mutableStateOf(false) }
    LaunchedEffect(awaitingRecommendation) {
        if (awaitingRecommendation) {
            selectedTaskId = suggestedTask.id
            categoryName = suggestedTask.type.name
            modeName = suggestedMode.name
            awaitingRecommendation = false
        }
    }
    LaunchedEffect(sheetRequest) {
        sheetRequest?.let { requested ->
            categoryName = requested.name
            tasks.firstOrNull { it.type == requested && it.isReady }?.let {
                selectedTaskId = it.id
                modeName = supportedMode(it, LessonMode.valueOf(modeName)).name
            }
            venuesOpen = false
            sheet = true
            onConsumeSheetRequest()
        }
    }
    val expansion = rememberSaveable { mutableStateOf(false) }
    val task = tasks.firstOrNull { it.id == selectedTaskId && it.isReady } ?: suggestedTask
    // Browsing a planned category clears its selection but retains the last ready choice on return.
    val selectedTask = task.takeIf { it.isReady && it.type.name == categoryName }
    val mode = supportedMode(task, LessonMode.valueOf(modeName))
    val start = { if (task.isReady && (!sheet || selectedTask != null)) onBegin(task.id, mode) }
    val fraction by animateFloatAsState(if (sheet) .30f else .53f,
        tween(400, easing = FastOutSlowInEasing), label = "poster")
    val slide = with(LocalDensity.current) { 40.dp.roundToPx() }
    PosterSurface {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(fraction).fillMaxHeight().clipToBounds().testTag("setup-poster")) {
                    Image(painterResource(R.drawable.poster_car), null, Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1f + (.53f - fraction) * .35f
                        scaleY = scaleX
                        translationX = -(.53f - fraction) * 220.dp.toPx()
                    }, contentScale = ContentScale.Crop, alignment = Alignment.CenterStart)
                    BrandMark(Modifier.padding(start = 180.dp, top = 64.dp))
                }
                AnimatedContent(if (coach != null) "coach" else if (sheet) "tasks" else "home", Modifier.weight(1f - fraction).fillMaxHeight(),
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { slide }) togetherWith fadeOut(tween(300))
                    }, label = "setup-content") { page ->
                    val showSheet = page != "home"
                    Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp,
                        top = if (showSheet) 64.dp else 96.dp, bottom = 52.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (page == "coach") {
                            coach?.let { dialog -> CoachSheet(dialog, { choice ->
                                awaitingRecommendation = choice == CoachChoice.CONTINUE_LAST
                                onChooseCoach(choice)
                            }, onCloseCoach) }
                        } else if (showSheet) {
                            if (venuesOpen) VenueSheet(venues, booking, onReserve, onCancelReservation,
                                onBack = { venuesOpen = false }, onDone = { venuesOpen = false; sheet = false })
                            else TaskSheet(tasks, TaskType.valueOf(categoryName), selectedTask, mode,
                                onCategory = { category ->
                                    if (categoryName != category.name) {
                                        categoryName = category.name
                                        val firstReady = tasks.firstOrNull { it.type == category && it.isReady }
                                        firstReady?.let {
                                            selectedTaskId = it.id
                                            modeName = supportedMode(it, mode).name
                                        }
                                    }
                                },
                                onTask = { item ->
                                    selectedTaskId = item.id
                                    modeName = supportedMode(item, mode).name
                                },
                                onMode = { modeName = it.name },
                                onBack = { sheet = false }, onStart = start, onVenues = { venuesOpen = true })
                        } else {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                Eyebrow(profileLine(profile), color = CoachColors.Muted)
                                Spacer(Modifier.height(27.dp))
                                Headline(setupProposal(task.type), size = 72)
                                Spacer(Modifier.height(36.dp))
                                LessonText("${task.title} · ${mode.label} 모드", 40)
                                Spacer(Modifier.height(12.dp))
                                LessonText(selectionReason(task, mode, suggestedTask, suggestedMode, reason), 36, CoachColors.Muted,
                                    modifier = Modifier.testTag("home-reason"))
                                bookingDetails(booking, venues)?.let { details ->
                                    Spacer(Modifier.height(33.dp))
                                    HomeBookingCard(details, tasks, bookingOptions, bookingChoice, highlightBooking) { option ->
                                        awaitingRecommendation = true
                                        onChooseBooking(option)
                                    }
                                }
                                Spacer(Modifier.height(33.dp))
                                PrimaryPill(stringResource(R.string.lesson_start), start, Modifier.fillMaxWidth())
                                Spacer(Modifier.height(57.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(66.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextAction(stringResource(R.string.lesson_change_task_mode), {
                                        categoryName = task.type.name
                                        sheet = true
                                    })
                                    CoachPill("코치에게 말하기", onOpenCoach)
                                }
                            }
                            SpeechFooter(subtitle)
                        }
                    }
                }
            }
            if (demo != null) DemoRail(expansion, demo)
        }
    }
}
