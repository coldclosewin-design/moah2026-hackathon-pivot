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

    /**
     * @param endMillis 회차가 끝난 시각(버튼·마지막 신호). 속도 샘플이 그보다 먼저 끊겼어도 총 시간은 여기까지다. null 이면 마지막 속도 샘플.
     */
    fun summarize(samples: List<SpeedSample>, movingKmh: Float = MOVING_KMH, endMillis: Long? = null): MotionSummary? {
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
        val end = maxOf(samples.last().tMillis, endMillis ?: Long.MIN_VALUE)
        if (moving) movingMillis += end - segmentStart
        return MotionSummary(
            movingSegments = segments,
            totalMillis = end - samples.first().tMillis,
            movingMillis = movingMillis,
            maxSpeedKmh = max,
            firstMoveMillis = firstMove,
        )
    }
}
