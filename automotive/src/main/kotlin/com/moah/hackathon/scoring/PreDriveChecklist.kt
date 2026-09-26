package com.moah.hackathon.scoring

/**
 * B층: 출발 전 점검. 첫 이동 시각보다 먼저 안전벨트가 채워졌나, 시동이 켜졌나.
 * 신호가 없으면 null(미측정). **아직 움직인 적이 없으면** 지금 켜져 있을 때만 true, 아니면 null(아직 위반이 아니다) —
 * 그래야 힌트 모드가 출발 전에 "안전벨트가 아직이에요"를 외치지 않는다.
 */
data class PreDriveSummary(val beltBeforeFirstMove: Boolean?, val ignitionOnBeforeFirstMove: Boolean?)

object PreDriveChecklist {
    fun summarize(
        belt: List<FlagSample>,
        ignition: List<FlagSample>,
        firstMoveMillis: Long?,
    ): PreDriveSummary = PreDriveSummary(
        beltBeforeFirstMove = onBefore(belt, firstMoveMillis),
        ignitionOnBeforeFirstMove = onBefore(ignition, firstMoveMillis),
    )

    private fun onBefore(samples: List<FlagSample>, atMillis: Long?): Boolean? {
        if (samples.isEmpty()) return null
        if (atMillis == null) return if (samples.last().value) true else null
        val last = samples.lastOrNull { it.tMillis <= atMillis } ?: return false
        return last.value
    }
}
