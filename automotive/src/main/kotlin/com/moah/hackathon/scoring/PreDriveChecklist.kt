package com.moah.hackathon.scoring

/**
 * B층: 출발 전 점검. 첫 이동 시각보다 먼저 안전벨트가 채워졌나, 시동이 켜졌나.
 * 신호가 없으면 null(미측정). **아직 움직인 적이 없으면** 지금 켜져 있을 때만 true, 아니면 null(아직 위반이 아니다) —
 * 그래야 힌트 모드가 출발 전에 "안전벨트가 아직이에요"를 외치지 않는다.
 *
 * [beltOnMillis]·[ignitionOnMillis] 는 **출발 전 점검 과제**의 근거 — 마지막으로 켜진(false→true) 시각. 회차 시작 때 이미 켜져 있었으면 0.
 * 끝났을 때 꺼져 있거나 신호가 없으면 null.
 */
data class PreDriveSummary(
    val beltBeforeFirstMove: Boolean?,
    val ignitionOnBeforeFirstMove: Boolean?,
    val beltOnMillis: Long? = null,
    val ignitionOnMillis: Long? = null,
) {
    /** 출발 전 점검의 핵심 순서 — 벨트가 시동보다 먼저(또는 같이)였나. 둘 중 하나라도 모르면 null. */
    val beltBeforeIgnition: Boolean?
        get() = if (beltOnMillis != null && ignitionOnMillis != null) beltOnMillis <= ignitionOnMillis else null
}

object PreDriveChecklist {
    fun summarize(
        belt: List<FlagSample>,
        ignition: List<FlagSample>,
        firstMoveMillis: Long?,
    ): PreDriveSummary = PreDriveSummary(
        beltBeforeFirstMove = onBefore(belt, firstMoveMillis),
        ignitionOnBeforeFirstMove = onBefore(ignition, firstMoveMillis),
        beltOnMillis = onMillis(belt),
        ignitionOnMillis = onMillis(ignition),
    )

    private fun onBefore(samples: List<FlagSample>, atMillis: Long?): Boolean? {
        if (samples.isEmpty()) return null
        if (atMillis == null) return if (samples.last().value) true else null
        val last = samples.lastOrNull { it.tMillis <= atMillis } ?: return false
        return last.value
    }

    /**
     * 마지막 false→true 전이 시각(첫 샘플이 true 면 그 시각). 마지막 값이 false 면 null.
     * Fake 기본값(OFF)을 회차 시작에 한 번 읽고, 시나리오가 다시 OFF→ON 을 주는 흐름에서 **시나리오의 시각**이 잡히도록 "마지막" 전이를 쓴다.
     */
    fun onMillis(samples: List<FlagSample>): Long? {
        var result: Long? = null
        var prev: Boolean? = null
        for (s in samples) {
            if (s.value && prev != true) result = s.tMillis
            prev = s.value
        }
        return if (prev == true) result else null
    }
}
