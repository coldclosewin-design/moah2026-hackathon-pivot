package com.moah.hackathon.ui.lesson

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moah.hackathon.feature.lesson.*

@Composable
internal fun LessonRoute(vm: LessonViewModel) {
    val phase by vm.phase.collectAsStateWithLifecycle()
    val subtitle by vm.subtitle.collectAsStateWithLifecycle()
    val controls = vm.demo
    // The inherited Fake port defaults to road speeds. Park it for the lesson setup;
    // Real has no DemoControls and receives no synthetic commands.
    LaunchedEffect(controls, phase is LessonPhase.Setup) {
        if (phase is LessonPhase.Setup) controls?.stopCar()
    }
    val demo: (@Composable () -> Unit)? = if (controls == null) null else {
        {
            val playback by controls.playback.collectAsStateWithLifecycle()
            DemoPanel(controls.scenarios, playback, { id ->
                controls.stopScenario()
                controls.resumeCar()
                controls.play(id)
            }, controls::stopScenario,
                controls::stopCar, controls::resumeCar, controls::setDoor)
        }
    }
    when (val state = phase) {
        is LessonPhase.Setup -> SetupScreen(state.profile, state.tasks, state.suggestedTask, state.suggestedMode,
            state.reason, state.reservation, subtitle, vm::begin, demo)
        is LessonPhase.Briefing -> BriefingScreen(state.task, state.mode, state.line, subtitle)
        is LessonPhase.Maneuver -> ManeuverScreen(state.toDisplayState(), state.snapshot.locked, state.snapshot.stopped,
            subtitle, vm::finishAttempt, demo, state.task.title)
        is LessonPhase.Done -> DoneScreen(state.task, state.attempt, state.record, subtitle, vm::nextAttempt, vm::endSession, demo)
        is LessonPhase.Report -> ReportScreen(state.report, vm::restart)
        is LessonPhase.Quiz -> QuizScreen(state.task, state.index, state.total, state.item, state.locked,
            state.chosen, state.correctSoFar, vm::answer, vm::nextQuestion)
        is LessonPhase.QuizDone -> QuizDoneScreen(state.task, state.results, state.items, state.remark, vm::restart)
    }
}
