package com.moah.hackathon.scoring

/**
 * B층: 출발 전 점검. 첫 이동 시각보다 먼저 안전벨트가 채워졌나, 시동이 켜졌나.
 * 신호가 없으면 null(미측정). **아직 움직인 적이 없으면** 지금 켜져 있을 때만 true, 아니면 null(아직 위반이 아니다) —
 * 그래야 힌트 모드가 출발 전에 "안전벨트가 아직이에요"를 외치지 않는다.
 *
 * [beltOnMillis]·[ignitionOnMillis] 는 **출발 전 점검 과제**의 근거 — 마지막으로 켜진(false→true) 시각. 회차 시작 때 이미 켜져 있었으면 0.
 * 끝났을 때 꺼져 있거나 신호가 없으면 null.
 *
 * 7단계 확장(9/28, 결정 D1): 시동 순간의 **도어 닫힘·브레이크**, 세션 안의 **좌/우 지시등·비상등 확인**. 전부 신호가 없으면 null(미측정, 무감점).
 */
data class PreDriveSummary(
    val beltBeforeFirstMove: Boolean?,
    val ignitionOnBeforeFirstMove: Boolean?,
    val beltOnMillis: Long? = null,
    val ignitionOnMillis: Long? = null,
    /** 시동 순간 운전석 도어가 닫혀 있었나. 시동 없음/도어 신호 없음 → null. */
    val doorClosedBeforeIgnition: Boolean? = null,
    /** 시동 직전([BRAKE_WINDOW_MILLIS] 안)에 브레이크를 밟고 있었나. */
    val brakeBeforeIgnition: Boolean? = null,
    /** 세션 안에 한 번이라도 켜 봤나. 신호 없음 → null, 신호는 있는데 안 켬 → false. */
    val leftIndicatorChecked: Boolean? = null,
    val rightIndicatorChecked: Boolean? = null,
    val hazardChecked: Boolean? = null,
    /** 세 등화 확인 중 마지막 것이 처음 켜진 시각(점검 완료 시각의 근거). 하나라도 안 켰으면 null. */
    val lightsDoneMillis: Long? = null,
) {
    /** 출발 전 점검의 핵심 순서 — 벨트가 시동보다 먼저(또는 같이)였나. 둘 중 하나라도 모르면 null. */
    val beltBeforeIgnition: Boolean?
        get() = if (beltOnMillis != null && ignitionOnMillis != null) beltOnMillis <= ignitionOnMillis else null

    /** 등화 3종 중 신호가 있는데 건너뛴 개수(감점 단위). */
    val skippedLights: Int
        get() = listOf(leftIndicatorChecked, rightIndicatorChecked, hazardChecked).count { it == false }

    companion object {
        const val BRAKE_WINDOW_MILLIS = 3_000L
    }
}

object PreDriveChecklist {
    fun summarize(
        belt: List<FlagSample>,
        ignition: List<FlagSample>,
        firstMoveMillis: Long?,
        door: List<FlagSample> = emptyList(),
        brake: List<FlagSample> = emptyList(),
        indicatorLeft: List<FlagSample> = emptyList(),
        indicatorRight: List<FlagSample> = emptyList(),
        hazard: List<FlagSample> = emptyList(),
    ): PreDriveSummary {
        val ignitionOn = onMillis(ignition)
        val left = firstOnMillis(indicatorLeft)
        val right = firstOnMillis(indicatorRight)
        val haz = firstOnMillis(hazard)
        val lightsDone = listOf(left, right, haz).takeIf { list -> list.all { it != null } }?.let { list -> list.maxOf { it!! } }
        return PreDriveSummary(
            beltBeforeFirstMove = onBefore(belt, firstMoveMillis),
            ignitionOnBeforeFirstMove = onBefore(ignition, firstMoveMillis),
            beltOnMillis = onMillis(belt),
            ignitionOnMillis = ignitionOn,
            doorClosedBeforeIgnition = ignitionOn?.let { t -> door.lastOrNull { it.tMillis <= t }?.let { !it.value } },
            brakeBeforeIgnition = ignitionOn?.let { t ->
                if (brake.isEmpty()) null else brake.any { it.value && it.tMillis in (t - PreDriveSummary.BRAKE_WINDOW_MILLIS)..t }
            },
            leftIndicatorChecked = checked(indicatorLeft),
            rightIndicatorChecked = checked(indicatorRight),
            hazardChecked = checked(hazard),
            lightsDoneMillis = lightsDone,
        )
    }

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

    /** 처음 true 가 된 시각 — 등화 "켜 봤나" 는 켰다 껐어도 확인한 것이다. */
    private fun firstOnMillis(samples: List<FlagSample>): Long? = samples.firstOrNull { it.value }?.tMillis

    /** 신호 없음 → null / 한 번이라도 true → true / 신호는 있는데 계속 false → false. */
    private fun checked(samples: List<FlagSample>): Boolean? = if (samples.isEmpty()) null else samples.any { it.value }
}
