package com.moah.hackathon.feature.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moah.hackathon.AppContainer
import com.moah.hackathon.BuildConfig
import com.moah.hackathon.ports.TtsPort
import com.moah.hackathon.vehicle.FakeVehiclePort
import com.moah.hackathon.vehicle.HybridVehiclePort
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.ScenarioPlayback
import com.moah.hackathon.vehicle.VehiclePort
import com.moah.hackathon.vehicle.VssValues
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mobis.vss.VssConstants

/**
 * 화면 진입점. 화면은 [phase]·[subtitle] 을 보고 아래 진입점만 부른다.
 * 상태기계는 [AppContainer] 가 앱 수명으로 소유하므로 화면 재생성에도 세션이 이어진다.
 */
class LessonViewModel(
    private val machine: LessonStateMachine,
    tts: TtsPort,
    vehicle: VehiclePort? = null,
    private val scenarios: List<Scenario> = emptyList(),
    /** 과제별 시나리오 — 시연 패널이 지금 과제에 맞는 버튼만 보이게. 기본은 전부. */
    private val scenariosFor: (Task) -> List<Scenario> = { scenarios },
    /** AI 코치 인증(9/30). 없으면(플래그 off) null — 패널이 AI 줄을 숨긴다. */
    private val ai: com.moah.hackathon.ports.copilot.CopilotAuth? = null,
) : ViewModel() {

    val phase: StateFlow<LessonPhase> = machine.phase
    val subtitle: StateFlow<String?> = tts.lastSpoken

    /**
     * 시연 조작. Fake 가 섞여 있을 때만 존재한다 — 순수 Fake, 또는 Hybrid(실물에서 안 오는 키를 Fake 가 채움).
     * 순수 Real(`-PfillMissing=false`)에서는 null → 화면이 숨긴다. **패널이 보이면 Fake 가 섞여 있다.**
     * Hybrid 에서 live 키(실물이 주는 속도·도어)에는 정차·도어 버튼이 먹지 않는다 — 실차에서는 실제로 세우고 열어야 한다.
     */
    val demo: DemoControls? = when {
        !BuildConfig.SHOW_DEMO_PANEL -> null   // -PdemoPanel=false: 녹화·사내용, 패널 자체를 그리지 않는다
        vehicle is FakeVehiclePort -> DemoControls(vehicle)
        vehicle is HybridVehiclePort -> DemoControls(vehicle.fake)
        else -> null
    }

    inner class DemoControls(private val fake: FakeVehiclePort) {
        /** 진행 중인 과제의 시나리오만. Setup(과제 미정)에서는 전부. 화면은 phase 가 바뀔 때 다시 그리므로 그때 다시 읽힌다. */
        val scenarios: List<Scenario> get() = currentTask()?.let(scenariosFor) ?: this@LessonViewModel.scenarios
        val playback: StateFlow<ScenarioPlayback?> get() = fake.playback
        fun play(scenarioId: String) {
            scenarios.firstOrNull { it.id == scenarioId }?.let { fake.play(it, BuildConfig.DEMO_SPEED_FACTOR) }
        }
        fun stopScenario() = fake.stop()
        fun stopCar() { viewModelScope.launch { fake.holdSpeed(0f) } }
        fun resumeCar() { fake.speedOverrideKmh = null }
        fun setDoor(open: Boolean) {
            viewModelScope.launch { fake.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.ofBoolean(open))) }
        }
        /** AI 코치 상태(패널 맨 아래 `AI 코치 · <상태>`). 플래그 off 면 null. */
        val aiState: StateFlow<com.moah.hackathon.ports.copilot.CopilotAuth.State>? get() = ai?.state
        /** `AI 연결` 버튼 — NeedsLogin·Error 일 때만 보인다. device code 흐름 시작. */
        fun connectAi() { ai?.connect() }
    }

    private fun currentTask(): Task? = when (val p = phase.value) {
        is LessonPhase.Briefing -> p.task
        is LessonPhase.Maneuver -> p.task
        is LessonPhase.Done -> p.task
        is LessonPhase.Report -> p.report.task
        is LessonPhase.Quiz -> p.task
        is LessonPhase.QuizDone -> p.task
        is LessonPhase.Setup -> null
    }

    fun begin(taskId: String, mode: LessonMode) = machine.begin(taskId, mode)
    /** 지식 테스트 선택지(0-based). 정차 중에만 받는다. */
    fun answer(choice: Int) = machine.answer(choice)
    /** 지식 테스트 "다음 문제" / 마지막이면 "결과 보기". */
    fun nextQuestion() = machine.nextQuestion()
    fun finishAttempt() = machine.finishAttempt()
    fun nextAttempt() = machine.nextAttempt()
    fun endSession() = machine.endSession()
    fun restart() = machine.reset()

    /** 제휴 시험장 예약·취소 — Setup 에서만(D3). */
    fun reserve(venueId: String, slotId: String, courseId: String) = machine.reserve(venueId, slotId, courseId)
    fun cancelReservation() = machine.cancelReservation()

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { LessonViewModel(container.lesson, container.tts, container.vehicle, container.scenarios, container.scenariosFor, container.copilot) }
        }
    }
}
