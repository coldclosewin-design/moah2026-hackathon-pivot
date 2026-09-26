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
) : ViewModel() {

    val phase: StateFlow<LessonPhase> = machine.phase
    val subtitle: StateFlow<String?> = tts.lastSpoken

    /** Fake 차량일 때만 존재하는 시연 조작. 사내(Real)에서는 null → 화면이 숨긴다. **버튼이 보이면 Real 이 아니다.** */
    val demo: DemoControls? = (vehicle as? FakeVehiclePort)?.let { DemoControls(it) }

    inner class DemoControls(private val fake: FakeVehiclePort) {
        val scenarios: List<Scenario> get() = this@LessonViewModel.scenarios
        val playback: StateFlow<ScenarioPlayback?> get() = fake.playback
        fun play(scenarioId: String) {
            scenarios.firstOrNull { it.id == scenarioId }?.let { fake.play(it, BuildConfig.DEMO_SPEED_FACTOR) }
        }
        fun stopScenario() = fake.stop()
        fun stopCar() { viewModelScope.launch { fake.holdSpeed(0f) } }
        fun resumeCar() { fake.speedOverrideKmh = null }
        fun setDoor(open: Boolean) {
            viewModelScope.launch { fake.set(mapOf(VssConstants.DOOR_DRIVER_ISOPEN to VssValues.ofBoolean(open))) }
        }
    }

    fun begin(taskId: String, mode: LessonMode) = machine.begin(taskId, mode)
    fun finishAttempt() = machine.finishAttempt()
    fun nextAttempt() = machine.nextAttempt()
    fun endSession() = machine.endSession()
    fun restart() = machine.reset()

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { LessonViewModel(container.lesson, container.tts, container.vehicle, container.scenarios) }
        }
    }
}
