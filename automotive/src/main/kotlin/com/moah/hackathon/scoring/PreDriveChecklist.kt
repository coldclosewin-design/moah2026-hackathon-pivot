package com.moah.hackathon.scoring

/**
 * B층: 출발 전 점검. 첫 이동 시각보다 먼저 안전벨트가 채워졌나, 시동이 켜졌나.
 * 신호가 없으면 null(미측정). 움직인 적이 없으면 마지막 상태로 판정한다.
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
        val cutoff = atMillis ?: Long.MAX_VALUE
        val last = samples.lastOrNull { it.tMillis <= cutoff } ?: return false
        return last.value
    }
}
