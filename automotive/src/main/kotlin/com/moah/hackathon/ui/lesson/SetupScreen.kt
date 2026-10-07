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
    var venuesOpen by rememberSaveable { mutableStateOf(false) }
    var awaitingRecommendation by remember { mutableStateOf(false) }
    var coachTextSubmitted by remember { mutableStateOf(false) }
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
    val fraction by animateFloatAsState(when { profileOpen -> .30f; sheet || coach != null -> .21f; else -> 0f },
        tween(400, easing = FastOutSlowInEasing), label = "poster")
    val slide = with(LocalDensity.current) { 40.dp.roundToPx() }
    PosterSurface(band = demo.takeUnless { sheet || profileOpen || coach != null }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // An expanded admin band needs room as well as a booking card. Keep the
            // A1 action/card gaps and full button heights; compact only the reading area.
            val compactHome = maxHeight < 1120.dp
            Row(Modifier.fillMaxSize().then(if (coachInput == CoachInputMode.CARDS_AND_TEXT && coach != null) Modifier.imePadding() else Modifier)) {
                if (fraction > .001f) Box(Modifier.weight(fraction).fillMaxHeight().clipToBounds().testTag("setup-poster")) {
                    Image(painterResource(R.drawable.poster_car), null, Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1f + (.53f - fraction) * .35f
                        scaleY = scaleX
                        translationX = -(.53f - fraction) * 220.dp.toPx()
                    }, contentScale = ContentScale.Crop, alignment = Alignment.CenterStart,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(CoachColors.Periwinkle, androidx.compose.ui.graphics.BlendMode.Color))
                    SetupBrandMark(Modifier.padding(start = 180.dp, top = 64.dp), onAdmin)
                }
                AnimatedContent(if (profileOpen) "profile" else if (coach != null) "coach" else if (sheet) "tasks" else "home", Modifier.weight(1f - fraction).fillMaxHeight(),
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { slide }) togetherWith fadeOut(tween(300))
                    }, label = "setup-content") { page ->
                    val showSheet = page != "home"
                    if (page == "home") {
                        HomeGallery(profile, task, mode, picked, compactHome, onAdmin,
                            { profileOpen = true }, onOpenCoach,
                            { categoryName = task.type.name; sheet = true },
                            { modeName = it.name; onChooseHomeMode(it) }, start, bookingDetails(booking, venues) != null,
                            if (picked) selectionReason(task, mode, suggestedTask, suggestedMode, reason)
                            else "밑줄을 누르면 과제를 고를 수 있어요.") {
                            bookingDetails(booking, venues)?.let { details ->
                                HomeBookingCard(details, tasks, bookingOptions, bookingChoice, highlightBooking) { option ->
                                    awaitingRecommendation = true
                                    onChooseBooking(option)
                                }
                            }
                        }
                    } else Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp,
                        top = if (showSheet) 64.dp else if (compactHome) 48.dp else 96.dp,
                        bottom = if (compactHome) 24.dp else 52.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (page == "profile") {
                            ProfileScreen(profileRows, onAnswerProfile, { profileOpen = false }, observedLines = observedLines)
                        } else if (page == "coach") {
                            coach?.let { dialog -> CoachSheet(dialog, { choice ->
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
                        }
                    }
                }
            }
        }
    }
}
