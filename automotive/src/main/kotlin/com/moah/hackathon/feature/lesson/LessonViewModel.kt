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
import com.moah.hackathon.data.AdminPresets
import kotlinx.coroutines.flow.MutableStateFlow
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

    /**
     * 관리자 모드(라운드 22 결정 5, 10/5 — 준비실 D + 세션 중 띠 C). [demo] 가 있을 때만 존재한다 = Fake/Hybrid 시연 빌드.
     * **Real 빌드에서는 null** — 화면은 준비실·띠·워드마크 길게 누르기 핸들러를 아예 달지 않는다.
     */
    val admin: AdminControls? = demo?.let { AdminControls(it, vehicle is HybridVehiclePort) }

    inner class AdminControls(
        /** 세션 중 조작(시나리오·정차·문·AI) — 띠가 그대로 쓴다. */
        val demo: DemoControls,
        hybrid: Boolean,
    ) {
        val presets: List<AdminPreset> = AdminPresets.presets
        val profilePresets: List<ProfilePreset> = AdminPresets.profiles
        private val _profileId = MutableStateFlow<String?>(AdminPresets.PROFILE_RUSTY)
        /** 지금 고른 프로필 프리셋(준비실 칩 강조). 기록에서 나온 관측은 바꾸지 않는다. */
        val profileId: StateFlow<String?> = _profileId
        private val _bandVisible = MutableStateFlow(true)
        /** 세션 중 띠를 보일지 — 준비실의 "세션 중 패널(띠/숨김)". 시연 빌드 기본은 보임(도구가 띠의 라벨로 조작한다). */
        val bandVisible: StateFlow<Boolean> = _bandVisible
        /** 신호 출처 — 빌드에서 정해진다. 화면은 읽기만(전환은 빌드 플래그 `-PfillMissing`). */
        val signalSource: String = if (hybrid) "Hybrid · 실신호 + 시뮬레이션" else "Fake 전부 · 시뮬레이션"

        /** 프리셋 적용 — 기록·예약을 비우고 프로필을 바꾼 뒤 홈 제안을 프리셋 과제·모드로. 모르는 id 는 무시(로그). */
        fun applyPreset(id: String) {
            val preset = AdminPresets.preset(id) ?: run { android.util.Log.w(TAG, "admin: unknown preset $id"); return }
            val profile = AdminPresets.profile(preset.profileId) ?: return
            demo.stopScenario()
            demo.stopCar()
            machine.applyPreset(preset, profile.profile)
            _profileId.value = profile.id
        }
        fun setProfile(id: String) {
            val preset = AdminPresets.profile(id) ?: return
            machine.setProfile(preset.profile)
            _profileId.value = id
        }
        fun resetRecords() = machine.resetRecords()
        /** 프로필 초기화 — 저장을 지우고 다음 홈에서 첫 실행 질문이 다시 나온다. */
        fun resetProfile() { machine.resetProfile(); _profileId.value = AdminPresets.PROFILE_EMPTY }
        fun setBand(visible: Boolean) { _bandVisible.value = visible }

        private val _textInput = MutableStateFlow(false)
        /**
         * "시뮬레이션 음성 입력"(10/6) — 켜면 홈 코치 시트에 텍스트 입력이 생기고 AI(또는 Fake 키워드 규칙)가 답한다. 기본 꺼짐.
         * 화면의 입력 칸·배지·버튼 문구는 이 값이 아니라 `Setup.coachTextInput` 을 본다(같은 값).
         */
        val textInput: StateFlow<Boolean> = _textInput
        fun setTextInput(on: Boolean) { machine.setCoachTextInput(on); _textInput.value = on }
    }

    private fun currentTask(): Task? = when (val p = phase.value) {
        is LessonPhase.Briefing -> p.task
        is LessonPhase.Maneuver -> p.task
        is LessonPhase.Drive -> p.task
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

    /** 홈 코치 대화(라운드 22 결정 4) — `코치에게 말하기` · 답 칩 · `돌아가기`. 시트 요청은 시트를 연 뒤 소비한다. */
    fun openCoach() = machine.openCoach()
    fun chooseCoach(choice: CoachChoice) = machine.chooseCoach(choice)
    fun closeCoach() = machine.closeCoach()
    fun consumeSheetRequest() = machine.consumeSheetRequest()
    /** 텍스트 대화(10/6) — 입력 칸 `보내기`. 입력이 꺼져 있거나 답을 기다리는 중이면 무시. 프로필 시트 요청은 시트를 연 뒤 소비한다. */
    fun sendCoachText(text: String) = machine.sendCoachText(text)
    fun consumeProfileRequest() = machine.consumeProfileRequest()
    /** 예약 카드 `코스 연습`·`모의시험`. */
    fun chooseBooking(option: BookingOption) = machine.chooseBooking(option)

    /** 프로필(라운드 22 결정 7 = P2) — 칩 · 첫 실행 끝(시작하기·건너뛰기) · 리포트 끝 카드 `다음에요`. */
    fun answerProfile(field: ProfileField, chipId: String) = machine.answerProfile(field, chipId)
    fun finishOnboarding() = machine.finishOnboarding()
    fun skipAsk(field: ProfileField) = machine.skipAsk(field)

    companion object {
        private const val TAG = "MOAH/LessonViewModel"
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { LessonViewModel(container.lesson, container.tts, container.vehicle, container.scenarios, container.scenariosFor, container.copilot) }
        }
    }
}
