@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.moah.hackathon.ui.lesson

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    onChooseBooking: (BookingOption) -> Unit = {}, onConsumeSheetRequest: () -> Unit = {},
    onboarding: ProfileOnboarding? = null, profileRows: List<ProfileRow> = emptyList(),
    observedLines: List<String> = emptyList(), onAnswerProfile: (ProfileField, String) -> Unit = { _, _ -> },
    onFinishOnboarding: () -> Unit = {}, onAdmin: (() -> Unit)? = null,
    coachInput: CoachInputMode = CoachInputMode.OFF, onSendCoachText: (String) -> Unit = {},
    onSendCoachCard: (String) -> Unit = {},
    profileRequest: Boolean = false, onConsumeProfileRequest: () -> Unit = {},
    picked: Boolean = true, onChooseHomeMode: (LessonMode) -> Unit = {}) {
    if (onboarding != null) {
        ProfileOnboardingScreen(profileRows, onboarding, onAnswerProfile, onFinishOnboarding, onAdmin)
        return
    }
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(profileRequest) {
        if (profileRequest) {
            profileOpen = true
            onConsumeProfileRequest()
        }
    }
    var selectedTaskId by rememberSaveable(suggestedTask.id, booking) { mutableStateOf(suggestedTask.id) }
    var categoryName by rememberSaveable(suggestedTask.id, booking) { mutableStateOf(suggestedTask.type.name) }
    var modeName by rememberSaveable(suggestedMode, booking) { mutableStateOf(suggestedMode.name) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var bookingOpen by rememberSaveable { mutableStateOf(false) }
    var venuesOpen by rememberSaveable { mutableStateOf(false) }
    var awaitingRecommendation by remember { mutableStateOf(false) }
    var coachTextSubmitted by remember { mutableStateOf(false) }
    var lastCoach by remember { mutableStateOf(coach) }
    if (coach != null) lastCoach = coach
    LaunchedEffect(coach, profileRequest, sheetRequest) {
        if (coach == null && coachTextSubmitted) {
            // A text intent can pin the same model recommendation after manual browsing.
            // Restore that recommendation even when its task/mode keys did not change.
            awaitingRecommendation = !profileRequest && sheetRequest == null
            coachTextSubmitted = false
        }
    }
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
    val task = tasks.firstOrNull { it.id == selectedTaskId && it.isReady } ?: suggestedTask
    // Browsing a planned category clears its selection but retains the last ready choice on return.
    val selectedTask = task.takeIf { it.isReady && it.type.name == categoryName }
    val mode = supportedMode(task, LessonMode.valueOf(modeName))
    val start = { if (task.isReady && (if (sheet) selectedTask != null else picked)) onBegin(task.id, mode) }
    val slide = with(LocalDensity.current) { 40.dp.roundToPx() }
    val escape = LocalDemoEscape.current
    CompositionLocalProvider(LocalDemoEscape provides escape?.let { {
        profileOpen = false; bookingOpen = false; venuesOpen = false; sheet = false; onCloseCoach(); it()
    } }) {
    PosterSurface(band = demo.takeUnless { sheet || profileOpen || coach != null || bookingOpen }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // An expanded admin band needs room as well as a booking card. Keep the
            // A1 action/card gaps and full button heights; compact only the reading area.
            val compactHome = maxHeight < 1120.dp
            Row(Modifier.fillMaxSize().then(if (coachInput == CoachInputMode.CARDS_AND_TEXT && coach != null) Modifier.imePadding() else Modifier)) {
                val sheetColor by androidx.compose.animation.animateColorAsState(
                    if (sheet || bookingOpen) CoachColors.Lavender else CoachColors.Paper, tween(400), label = "home-paper")
                SharedTransitionLayout(Modifier.background(sheetColor)) {
                CompositionLocalProvider(LocalHomeShared provides this, LocalCoachOpening provides (coach != null)) {
                AnimatedContent(if (bookingOpen) "booking" else if (profileOpen) "profile" else if (coach != null) "coach" else if (sheet && venuesOpen) "venues" else if (sheet) "tasks" else "home", Modifier.weight(1f).fillMaxHeight(),
                    transitionSpec = {
                        if (initialState == "coach" || targetState == "coach" ||
                            setOf(initialState, targetState) == setOf("tasks", "venues")) {
                            (EnterTransition.None togetherWith ExitTransition.None).apply { targetContentZIndex = 1f }
                        } else fadeIn(tween(280, delayMillis = 280)) togetherWith fadeOut(tween(200))
                    }, label = "setup-content") { page ->
                    CompositionLocalProvider(LocalHomeVisibility provides this) {
                    val showSheet = page != "home"
                    if (page == "home") {
                        HomeGallery(profile, task, mode, picked, compactHome, onAdmin,
                            { profileOpen = true }, onOpenCoach,
                            { categoryName = task.type.name; sheet = true },
                            { modeName = it.name; onChooseHomeMode(it) }, start, bookingDetails(booking, venues) != null,
                            if (picked) selectionReason(task, mode, suggestedTask, suggestedMode, reason)
                            else "밑줄을 누르면 과제를 고를 수 있어요.") {
                            bookingDetails(booking, venues)?.let { details ->
                                HomeBookingCard(details, tasks, bookingOptions, bookingChoice, onOpen = { bookingOpen = true }) { option ->
                                    awaitingRecommendation = true
                                    onChooseBooking(option)
                                }
                            }
                        }
                    } else Column(Modifier.fillMaxSize().background(CoachColors.Paper).padding(if (page == "profile") 84.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(if (page == "profile") 0.dp else 24.dp)) {
                        if (page == "profile") {
                            BrandMark()
                            ProfileScreen(profileRows, onAnswerProfile, { profileOpen = false }, observedLines = observedLines, profile = profile)
                        } else if (page == "booking") {
                            bookingDetails(booking, venues)?.let { details ->
                                BookingTicketSheet(details, tasks, bookingOptions, bookingChoice, {
                                    awaitingRecommendation = true; onChooseBooking(it)
                                }, { bookingOpen = false }, { bookingOpen = false; venuesOpen = true; sheet = true },
                                    { slotId -> onReserve(details.venue.id, slotId, details.course.id) },
                                    { onCancelReservation(); bookingOpen = false })
                            }
                        } else if (page == "coach") {
                            Box(Modifier.fillMaxSize()) {
                            if (coach != null) Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                                CompositionLocalProvider(LocalHomeShared provides null) {
                                HomeGallery(profile, task, mode, picked, compactHome, null, {}, {}, {}, {}, {}, false, reason, showCoach = false) {}
                                }
                            }
                            if (coach != null) Box(Modifier.fillMaxSize().background(CoachColors.Ink.copy(alpha = .15f)))
                            Box(Modifier.fillMaxSize().padding(start = 72.dp, end = 72.dp, top = 180.dp, bottom = 36.dp)
                                .homeShared("coach-surface").background(CoachColors.Paper, RoundedCornerShape(56.dp))
                                .border(1.5.dp, CoachColors.Ink, RoundedCornerShape(56.dp)).clip(RoundedCornerShape(56.dp))) {
                            Box(Modifier.fillMaxSize().coachTextArrival().padding(52.dp)) {
                            (coach ?: lastCoach)?.let { dialog -> CoachSheet(dialog, { choice ->
                                coachTextSubmitted = false
                                awaitingRecommendation = choice == CoachChoice.CONTINUE_LAST
                                onChooseCoach(choice)
                            }, { coachTextSubmitted = false; onCloseCoach() }, coachInput, { text ->
                                coachTextSubmitted = true
                                onSendCoachText(text)
                            }, { id ->
                                coachTextSubmitted = true
                                onSendCoachCard(id)
                            }) }
                            }
                            }
                            }
                        } else if (showSheet) {
                            if (page == "venues") VenueSheet(venues, booking, onReserve, onCancelReservation,
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
                        }
                    }
                }
                }
                }
                }
            }
        }
    }
    }
}
