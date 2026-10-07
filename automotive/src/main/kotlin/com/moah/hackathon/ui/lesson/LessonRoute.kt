package com.moah.hackathon.ui.lesson

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moah.hackathon.feature.lesson.*

@Composable
internal fun LessonRoute(vm: LessonViewModel) {
    val phase by vm.phase.collectAsStateWithLifecycle()
    val subtitle by vm.subtitle.collectAsStateWithLifecycle()
    val admin = vm.admin
    val controls = admin?.demo
    val stopDemo: (() -> Unit)? = remember(controls) {
        controls?.let { { it.stopScenario(); it.stopCar() } }
    }
    val bandVisible = admin?.bandVisible?.collectAsStateWithLifecycle()?.value == true
    var adminOpen by rememberSaveable { mutableStateOf(false) }
    // The inherited Fake port defaults to road speeds. Park it for the lesson setup;
    // Real has no DemoControls and receives no synthetic commands.
    LaunchedEffect(controls, phase is LessonPhase.Setup) {
        if (phase is LessonPhase.Setup) controls?.stopCar()
    }
    val demo: (@Composable () -> Unit)? = if (controls == null || !bandVisible) null else {
        {
            val playback by controls.playback.collectAsStateWithLifecycle()
            AdminBand(controls.scenarios, playback, { id ->
                controls.stopScenario()
                controls.resumeCar()
                controls.play(id)
            }, controls::stopScenario,
                controls::stopCar, controls::resumeCar, controls::setDoor,
                aiState = controls.aiState, onConnectAi = controls::connectAi,
                onResetRecords = admin::resetRecords, onHide = { admin.setBand(false) }, signalSource = admin.signalSource)
        }
    }
    when (val state = phase) {
        is LessonPhase.Setup -> if (adminOpen && admin != null) AdminHome(admin, state, { adminOpen = false })
        else SetupScreen(state.profile, state.tasks, state.suggestedTask, state.suggestedMode,
            state.reason, subtitle, vm::begin, demo, state.venues, state.booking, vm::reserve, vm::cancelReservation,
            state.coach, state.bookingOptions, state.bookingChoice, state.highlightBooking, state.sheetRequest,
            vm::openCoach, vm::chooseCoach, vm::closeCoach, vm::chooseBooking, vm::consumeSheetRequest,
            state.onboarding, state.profileRows, state.observedLines, vm::answerProfile, vm::finishOnboarding,
            if (admin != null) ({ adminOpen = true }) else null,
            coachInput = if (admin != null) state.coachInput else CoachInputMode.OFF,
            onSendCoachText = vm::sendCoachText, onSendCoachCard = vm::sendCoachCard,
            profileRequest = state.profileRequest, onConsumeProfileRequest = vm::consumeProfileRequest,
            picked = state.picked, onChooseHomeMode = vm::chooseHomeMode)
        is LessonPhase.Briefing -> BriefingScreen(state.task, state.mode, state.line, subtitle,
            expectedMillis = state.expectedMillis, locked = state.locked, onSkip = vm::skipBriefing)
        is LessonPhase.Maneuver -> ManeuverScreen(state.toDisplayState(), state.snapshot.locked, state.snapshot.stopped,
            subtitle, vm::finishAttempt, demo, state.task.title)
        is LessonPhase.Drive -> DriveScreen(state, subtitle, vm::finishAttempt, demo)
        is LessonPhase.Done -> DoneScreen(state.task, state.attempt, state.record, subtitle, vm::nextAttempt, vm::endSession, demo, state.locked, stopDemo)
        is LessonPhase.Report -> ReportScreen(state.report, vm::restart, state.locked, vm::answerProfile, vm::skipAsk, stopDemo)
        is LessonPhase.Quiz -> QuizScreen(state.task, state.index, state.total, state.item, state.locked,
            state.chosen, state.correctSoFar, vm::answer, vm::nextQuestion, vm::endSession, stopDemo)
        is LessonPhase.QuizDone -> QuizDoneScreen(state.task, state.results, state.items, state.remark, vm::restart, state.locked, stopDemo)
    }
}
