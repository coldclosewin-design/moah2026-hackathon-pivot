package com.moah.hackathon.vehicle

import mobis.vss.VssConstants

/**
 * Fake 신호 타임라인. [FakeVehiclePort.play] 가 [ScenarioStep.atSeconds] 순서로 값을 주입한다.
 * 실차라면 앱이 만들 수 없는 신호(속도·기어·조향각·센서)를 시연용으로 만든다 — 그래서 이 값은 전부
 * [SignalAvailability.SIMULATED] 로 표시된다.
 */
data class ScenarioStep(val atSeconds: Double, val values: Map<String, String>)

data class Scenario(val id: String, val title: String, val steps: List<ScenarioStep>) {
    val durationSeconds: Double get() = steps.lastOrNull()?.atSeconds ?: 0.0
}

/** 같은 시각의 값은 한 스텝으로 합치고 시각순으로 정렬한다. 속도 램프는 [speedRamp] 로 만든다. */
class ScenarioBuilder(private val id: String, private val title: String) {
    private val steps = LinkedHashMap<Double, MutableMap<String, String>>()

    fun at(seconds: Double, vararg values: Pair<String, String>): ScenarioBuilder {
        steps.getOrPut(seconds) { LinkedHashMap() }.putAll(values)
        return this
    }

    fun speed(seconds: Double, kmh: Double) = at(seconds, VssConstants.VEHICLE_SPEED to formatSpeed(kmh))

    /** [fromSeconds]~[toSeconds] 사이 속도를 [stepSeconds] 간격으로 선형 보간해 스텝을 만든다(양 끝 포함). */
    fun speedRamp(fromSeconds: Double, toSeconds: Double, fromKmh: Double, toKmh: Double, stepSeconds: Double = 0.5): ScenarioBuilder {
        require(toSeconds > fromSeconds) { "ramp must move forward in time" }
        require(stepSeconds > 0) { "stepSeconds must be positive" }
        // 누적 덧셈 대신 정수 배수로 시각을 만든다 — 0.15 를 세 번 더하면 28.8 이 28.799999… 가 되어 스텝이 겹친다
        var i = 0
        while (true) {
            val t = fromSeconds + i * stepSeconds
            if (t >= toSeconds - 1e-9) break
            val f = (t - fromSeconds) / (toSeconds - fromSeconds)
            speed(t, fromKmh + (toKmh - fromKmh) * f)
            i++
        }
        speed(toSeconds, toKmh)
        return this
    }

    fun build(): Scenario = Scenario(id, title, steps.entries.sortedBy { it.key }.map { ScenarioStep(it.key, it.value.toMap()) })

    companion object {
        fun formatSpeed(kmh: Double): String = String.format(java.util.Locale.US, "%.1f", kmh)
    }
}

fun scenario(id: String, title: String, block: ScenarioBuilder.() -> Unit): Scenario =
    ScenarioBuilder(id, title).apply(block).build()

/** 재생 상태. 화면의 시연 조작 패널이 본다. */
data class ScenarioPlayback(val id: String, val stepIndex: Int, val stepCount: Int) {
    val finished: Boolean get() = stepIndex >= stepCount - 1
}
