package com.moah.hackathon.scoring

import kotlin.math.abs

/**
 * B층: 핸들을 몇 번 "돌렸다 되돌렸나". 각도 변화의 부호가 바뀐 횟수(데드밴드 이하의 잔떨림은 무시).
 * 후면 직각 주차의 이상형은 1왕복(끝까지 감고 → 중립).
 */
data class SteeringSummary(val reversals: Int, val maxAbsAngleDeg: Float)

object SteeringReversalCounter {
    const val DEADBAND_DEG = 10f

    fun summarize(samples: List<AngleSample>, deadbandDeg: Float = DEADBAND_DEG): SteeringSummary? {
        if (samples.isEmpty()) return null
        var reversals = 0
        var lastDirection = 0 // -1 / 0 / +1
        var reference = samples.first().value
        var maxAbs = abs(reference)
        for (s in samples.drop(1)) {
            maxAbs = maxOf(maxAbs, abs(s.value))
            val delta = s.value - reference
            if (abs(delta) < deadbandDeg) continue
            val direction = if (delta > 0) 1 else -1
            if (lastDirection != 0 && direction != lastDirection) reversals++
            lastDirection = direction
            reference = s.value
        }
        return SteeringSummary(reversals, maxAbs)
    }
}
