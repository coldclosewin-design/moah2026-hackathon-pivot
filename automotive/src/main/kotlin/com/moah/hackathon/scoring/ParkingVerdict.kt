package com.moah.hackathon.scoring

import kotlin.math.abs

/**
 * 운전자가 주차를 스스로 판단하는 네 가지 질문에 대한 답(docs/design/09_parking_verdict.md, 2026-10-02 결정 V1~V4 = 추천).
 * 전부 [ParkingMetrics]·추정 궤적·마지막 조향각에서 **파생**한다 — 점수([ParkingScorer])와 시나리오 고정값은 그대로다.
 * 운전자 화면(Done·Report 요약)은 점수 대신 이 넷을 보여 주고, 0~100 점수는 `자세히 보기`·진단서에만 남는다.
 */
data class ParkingVerdict(
    /** ① 한 번에 들어갔나 — 이동 구간·R↔D 전환(측정). */
    val entry: Entry,
    /** ② 방향이 맞게 섰나 — 궤적 끝 헤딩의 **추정**. 화면은 "신호로 추정" 꼬리표를 단다. 조향각이 없으면 [Heading.UNKNOWN]. */
    val heading: Heading,
    /** ②의 근거: 목표(직각 주차 90°) 대비 편차(도). 추정 불가면 null. `자세히 보기` 전용 — 운전자 문장에는 넣지 않는다. */
    val headingErrorDeg: Float?,
    /** ③ 깔끔하게 마무리했나 — 정차·주차 기어·핸들 중립. */
    val finish: Finish,
    /** ④ 안전했나 — 급조작·근접·벨트. 미측정 항목은 세지 않는다. */
    val safety: Safety,
) {
    enum class Entry { ONE_GO, ONE_FIX, MANY }
    enum class Heading { ALIGNED, SLIGHT, OFF, UNKNOWN }
    enum class Finish { CLEAN, LOOSE }
    enum class Safety { SAFE, WATCH, UNSAFE }
}

object ParkingVerdicts {
    /** 후면 직각 주차: 시작 방향 대비 차가 돌아야 하는 각. 평행 주차 등 다른 과제는 과제별 값을 넘긴다. */
    const val PERPENDICULAR_TARGET_DEG = 90f
    const val ALIGNED_DEG = 10f
    const val SLIGHT_DEG = 25f
    /** 핸들 중립로 보는 범위(마무리). 가이드 "중립" 판정과 같은 30°. */
    const val NEUTRAL_STEERING_DEG = 30f
    /** ① "한 번에" 경계(V3 = 가, 엄격): 전진 접근 + 후진 = 2구간, 전환 0. 3~4구간 또는 전환 1~2 = 한 번 다시. */
    const val ONE_GO_SEGMENTS = 2
    const val ONE_FIX_SEGMENTS = 4
    const val ONE_FIX_SHIFTS = 2

    fun of(
        metrics: ParkingMetrics,
        path: List<PathPoint>,
        lastSteeringDeg: Float?,
        stopped: Boolean,
        targetHeadingDeg: Float = PERPENDICULAR_TARGET_DEG,
    ): ParkingVerdict {
        val segments = metrics.motion.movingSegments
        val shifts = metrics.gear?.reverseDriveShifts ?: 0
        val entry = when {
            segments <= ONE_GO_SEGMENTS && shifts == 0 -> ParkingVerdict.Entry.ONE_GO
            segments <= ONE_FIX_SEGMENTS && shifts <= ONE_FIX_SHIFTS -> ParkingVerdict.Entry.ONE_FIX
            else -> ParkingVerdict.Entry.MANY
        }

        // 조향각이 한 번도 안 왔으면 궤적의 헤딩은 전부 0 → 추정이 아니라 미측정이다
        val headingError = if (metrics.steering == null || path.size < 2) null
        else abs(abs(path.last().headingDeg - path.first().headingDeg) - targetHeadingDeg)
        val heading = when {
            headingError == null -> ParkingVerdict.Heading.UNKNOWN
            headingError <= ALIGNED_DEG -> ParkingVerdict.Heading.ALIGNED
            headingError <= SLIGHT_DEG -> ParkingVerdict.Heading.SLIGHT
            else -> ParkingVerdict.Heading.OFF
        }

        val inPark = metrics.gear?.endedInPark ?: true                  // 기어 미측정이면 묻지 않는다
        val neutral = lastSteeringDeg?.let { abs(it) < NEUTRAL_STEERING_DEG } ?: true
        val finish = if (stopped && inPark && neutral) ParkingVerdict.Finish.CLEAN else ParkingVerdict.Finish.LOOSE

        val beltMissing = metrics.preDrive.beltBeforeFirstMove == false
        val incidents = metrics.harshEvents.size + (metrics.proximity?.warnings ?: 0)
        val safety = when {
            beltMissing || incidents >= 2 -> ParkingVerdict.Safety.UNSAFE
            incidents == 1 -> ParkingVerdict.Safety.WATCH
            else -> ParkingVerdict.Safety.SAFE
        }
        return ParkingVerdict(entry, heading, headingError, finish, safety)
    }
}
