package com.moah.hackathon.scoring

/**
 * A층: 속도 하나로 "몇 번 움직였고 얼마나 걸렸나"를 잰다.
 * 이동 구간 = 속도가 [movingKmh] 를 넘은 시점부터 다시 그 아래로 내려올 때까지. 주차에서는 전·후진 왕복 횟수의 근사가 된다.
 */
data class MotionSummary(
    val movingSegments: Int,
    val totalMillis: Long,
    val movingMillis: Long,
    val maxSpeedKmh: Float,
    /** 첫 이동이 시작된 시각. 출발 전 점검(벨트)의 기준. 움직인 적 없으면 null. */
    val firstMoveMillis: Long?,
) {
    val idleMillis: Long get() = totalMillis - movingMillis
}

object MotionSegmenter {
    const val MOVING_KMH = 1.0f

    fun summarize(samples: List<SpeedSample>, movingKmh: Float = MOVING_KMH): MotionSummary? {
        if (samples.isEmpty()) return null
        var segments = 0
        var movingMillis = 0L
        var moving = false
        var segmentStart = 0L
        var firstMove: Long? = null
        var max = 0f
        for (s in samples) {
            if (s.value > max) max = s.value
            val nowMoving = s.value > movingKmh
            if (nowMoving && !moving) {
                segments++
                segmentStart = s.tMillis
                if (firstMove == null) firstMove = s.tMillis
            } else if (!nowMoving && moving) {
                movingMillis += s.tMillis - segmentStart
            }
            moving = nowMoving
        }
        if (moving) movingMillis += samples.last().tMillis - segmentStart
        return MotionSummary(
            movingSegments = segments,
            totalMillis = samples.last().tMillis - samples.first().tMillis,
            movingMillis = movingMillis,
            maxSpeedKmh = max,
            firstMoveMillis = firstMove,
        )
    }
}
