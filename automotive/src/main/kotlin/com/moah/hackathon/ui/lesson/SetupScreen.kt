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
    onReserve: (String, String, String) -> Unit = { _, _, _ -> }, onCancelReservation: () -> Unit = {}) {
    var selectedTaskId by rememberSaveable(suggestedTask.id, booking) { mutableStateOf(suggestedTask.id) }
    var categoryName by rememberSaveable(suggestedTask.id, booking) { mutableStateOf(suggestedTask.type.name) }
    var modeName by rememberSaveable(suggestedMode, booking) { mutableStateOf(suggestedMode.name) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var venuesOpen by rememberSaveable { mutableStateOf(false) }
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
                AnimatedContent(sheet, Modifier.weight(1f - fraction).fillMaxHeight(),
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { slide }) togetherWith fadeOut(tween(300))
                    }, label = "setup-content") { showSheet ->
                    Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp,
                        top = if (showSheet) 64.dp else 96.dp, bottom = 52.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (showSheet) {
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
                                bookingDetails(booking, venues)?.let { details ->
                                    Eyebrow(details.badge, color = CoachColors.Periwinkle)
                                    Spacer(Modifier.height(20.dp))
                                }
                                Eyebrow(profileLine(profile), color = CoachColors.Muted)
                                Spacer(Modifier.height(32.dp))
                                Headline(setupProposal(task.type), size = 72)
                                Spacer(Modifier.height(32.dp))
                                LessonText("${task.title} · ${mode.label} 모드", 40)
                                Spacer(Modifier.height(20.dp))
                                LessonText(selectionReason(task, mode, suggestedTask, suggestedMode, reason), 40, CoachColors.Muted)
                                Spacer(Modifier.height(48.dp))
                                PrimaryPill(stringResource(R.string.lesson_start), start, Modifier.fillMaxWidth())
                                Spacer(Modifier.height(16.dp))
                                TextAction(stringResource(R.string.lesson_change_task_mode), {
                                    categoryName = task.type.name
                                    sheet = true
                                })
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
