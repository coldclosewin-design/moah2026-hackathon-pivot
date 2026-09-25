package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.AvailabilityBadge

/**
 * 후면 직각 주차 한 회차의 과정 지표. B층 항목은 신호가 [com.moah.hackathon.vehicle.SignalAvailability.MISSING] 이면 null.
 * 칸 안에 반듯이 들어갔는지는 **모른다** — 이 구조체에 그 값이 없는 것이 의도다(docs/topics/01_driving_coach.md §4.2).
 */
data class ParkingMetrics(
    val motion: MotionSummary,
    val harshEvents: List<HarshEvent>,
    val steering: SteeringSummary?,
    val gear: GearSummary?,
    val proximity: ProximitySummary?,
    val preDrive: PreDriveSummary,
)

/** 감점 규칙. 시드에서 바꿀 수 있게 값으로 둔다. 기준은 전부 가정이다. */
data class ParkingRubric(
    val idealSegments: Int = 2,
    val penaltyPerExtraSegment: Int = 8,
    val idealReversals: Int = 1,
    val penaltyPerExtraReversal: Int = 6,
    val idealShifts: Int = 0,
    val penaltyPerExtraShift: Int = 6,
    val graceSeconds: Int = 45,
    val penaltyPerExtra10s: Int = 2,
    val penaltyNotInPark: Int = 5,
    val penaltyPerHarshEvent: Int = 15,
    val penaltyPerProximityWarning: Int = 10,
    val penaltyNoBeltBeforeMove: Int = 20,
)

/** 숙련(느림·반복)과 안전(급조작·근접·벨트)을 섞지 않는다. 둘 다 0~100. */
data class ParkingScore(
    val skill: Int,
    val safety: Int,
    val metrics: ParkingMetrics,
    val badge: AvailabilityBadge,
    val missingSignals: List<String>,
)

object ParkingScorer {
    fun score(metrics: ParkingMetrics, badge: AvailabilityBadge, missingSignals: List<String>, rubric: ParkingRubric = ParkingRubric()): ParkingScore {
        var skill = 100
        skill -= maxOf(0, metrics.motion.movingSegments - rubric.idealSegments) * rubric.penaltyPerExtraSegment
        metrics.steering?.let { skill -= maxOf(0, it.reversals - rubric.idealReversals) * rubric.penaltyPerExtraReversal }
        metrics.gear?.let {
            skill -= maxOf(0, it.reverseDriveShifts - rubric.idealShifts) * rubric.penaltyPerExtraShift
            if (!it.endedInPark) skill -= rubric.penaltyNotInPark
        }
        val extraSeconds = maxOf(0L, metrics.motion.totalMillis / 1000 - rubric.graceSeconds)
        skill -= (extraSeconds / 10).toInt() * rubric.penaltyPerExtra10s

        var safety = 100
        safety -= metrics.harshEvents.size * rubric.penaltyPerHarshEvent
        metrics.proximity?.let { safety -= it.warnings * rubric.penaltyPerProximityWarning }
        if (metrics.preDrive.beltBeforeFirstMove == false) safety -= rubric.penaltyNoBeltBeforeMove

        return ParkingScore(skill.coerceIn(0, 100), safety.coerceIn(0, 100), metrics, badge, missingSignals)
    }
}

/** "지난번보다" — 회차 멘트의 근거. 음수 = 좋아짐. */
data class ParkingDelta(val segments: Int, val seconds: Long, val reversals: Int?, val shifts: Int?) {
    companion object {
        fun of(current: ParkingMetrics, previous: ParkingMetrics): ParkingDelta = ParkingDelta(
            segments = current.motion.movingSegments - previous.motion.movingSegments,
            seconds = (current.motion.totalMillis - previous.motion.totalMillis) / 1000,
            reversals = if (current.steering != null && previous.steering != null) current.steering.reversals - previous.steering.reversals else null,
            shifts = if (current.gear != null && previous.gear != null) current.gear.reverseDriveShifts - previous.gear.reverseDriveShifts else null,
        )
    }
}
