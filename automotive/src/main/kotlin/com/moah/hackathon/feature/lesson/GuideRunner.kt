package com.moah.hackathon.feature.lesson

import com.moah.hackathon.vehicle.SignalRegistry

/**
 * 가이드 모드의 한 단계: 말하고 → 신호로 확인하고 → 다음. (§4.3 신호 확인형)
 *
 * @param signal 확인에 필요한 VSS 키. [SignalRegistry] 에서 MISSING 이면 확인 없이 읽고 넘어가며 [GuideRunner.unverified] 에 남는다.
 * @param check 스냅샷과 "이 단계에 들어온 뒤 움직였나"로 판정.
 */
data class GuideStep(
    val id: String,
    val say: String,
    val signal: String,
    val confirm: String,
    val check: (snapshot: VehicleSnapshot, movedSinceStep: Boolean) -> Boolean,
)

/**
 * 단계를 순서대로 진행한다. 안드로이드·코루틴 의존 없음. 말할 문장은 호출자(상태기계)가 TTS 로 넘긴다.
 */
class GuideRunner(private val steps: List<GuideStep>, private val registry: SignalRegistry) {
    var index: Int = 0
        private set
    private var movedSinceStep = false
    private val _unverified = ArrayList<GuideStep>()
    val unverified: List<GuideStep> get() = _unverified

    val current: GuideStep? get() = steps.getOrNull(index)
    val finished: Boolean get() = index >= steps.size
    val count: Int get() = steps.size

    /** 첫 단계를 읽는다. 첫 단계부터 신호가 없으면 있는 단계까지 연달아 읽는다. */
    fun start(): List<String> {
        index = 0
        movedSinceStep = false
        _unverified.clear()
        val out = ArrayList<String>()
        announce(out)
        return out
    }

    /**
     * 스냅샷이 바뀔 때마다. 확인되면 [GuideStep.confirm] + 다음 단계의 [GuideStep.say] 를 돌려준다.
     * 운전자가 앱보다 앞서 여러 단계를 이미 해 놓았으면(벨트·시동·R 을 한 번에) 같은 스냅샷으로 연달아 확인한다 —
     * 단, "움직인 뒤" 조건이 있는 단계는 새 스냅샷을 기다린다.
     */
    fun onSnapshot(snapshot: VehicleSnapshot): List<String> {
        val out = ArrayList<String>()
        if (snapshot.moving) movedSinceStep = true
        while (true) {
            val step = current ?: return out
            if (!step.check(snapshot, movedSinceStep)) return out
            out += step.confirm
            index++
            movedSinceStep = false
            announce(out)
        }
    }

    fun view(): GuideStepView? = current?.let {
        GuideStepView(index = index, count = steps.size, say = it.say, waitingFor = it.signal, unverified = !registry.isAvailable(it.signal))
    }

    /** 현재 단계를 읽되, 확인할 신호가 없는 단계는 읽고 바로 넘긴다(§3.2 "확인 없이 순서만"). */
    private fun announce(out: MutableList<String>) {
        while (true) {
            val step = current ?: return
            out += step.say
            if (registry.isAvailable(step.signal)) return
            _unverified += step
            index++
            movedSinceStep = false
        }
    }
}
