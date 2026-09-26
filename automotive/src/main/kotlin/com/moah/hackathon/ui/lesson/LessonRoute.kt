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
        is LessonPhase.Briefing -> BriefingScreen(state.line, subtitle)
        is LessonPhase.Maneuver -> ManeuverScreen(state.toDisplayState(), state.snapshot.locked, state.snapshot.stopped,
            subtitle, vm::finishAttempt, demo, state.task.title)
        is LessonPhase.Done -> DoneScreen(state.task.title, state.attempt, state.record, subtitle, vm::nextAttempt, vm::endSession, demo)
        is LessonPhase.Report -> ReportScreen(state.report, vm::restart)
        // [cross] Claude: 지식 테스트 화면은 Codex 오더 대기(INTEGRATION C절 9/26). 그때까지는 음성으로 진행되고 자막만 보인다.
        is LessonPhase.Quiz, is LessonPhase.QuizDone -> BriefingScreen("지식 테스트 — 화면 준비 중. 음성과 자막으로 진행돼요.", subtitle)
    }
}
