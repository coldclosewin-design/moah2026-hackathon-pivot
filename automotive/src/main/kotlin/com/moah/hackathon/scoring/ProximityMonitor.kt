package com.moah.hackathon.scoring

/**
 * B층 안전 축: 후방 장애물에 몇 번, 얼마나 가까이 갔나.
 * 거리 신호가 있으면 [warnCm] 아래로 내려간 횟수(하강 에지)와 최소 거리, 거리 신호가 없고 경고 boolean 만 있으면 상승 에지 수.
 */
data class ProximitySummary(val warnings: Int, val minDistanceCm: Float?)

object ProximityMonitor {
    const val WARN_CM = 40f

    fun fromDistance(samples: List<DistanceSample>, warnCm: Float = WARN_CM): ProximitySummary? {
        if (samples.isEmpty()) return null
        var warnings = 0
        var below = false
        var min = Float.MAX_VALUE
        for (s in samples) {
            if (s.value < min) min = s.value
            val nowBelow = s.value < warnCm
            if (nowBelow && !below) warnings++
            below = nowBelow
        }
        return ProximitySummary(warnings, min)
    }

    fun fromWarningFlag(samples: List<FlagSample>): ProximitySummary? {
        if (samples.isEmpty()) return null
        var warnings = 0
        var on = false
        for (s in samples) {
            if (s.value && !on) warnings++
            on = s.value
        }
        return ProximitySummary(warnings, minDistanceCm = null)
    }
}
