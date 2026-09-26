package com.moah.hackathon.feature.lesson

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.SignalAvailability
import mobis.vss.VssConstants
import kotlin.math.roundToInt

/**
 * 주차 중 화면(도식)이 받는 값 전부. **점수·감점 누계·조향 왕복 수·기어 전환 수는 없다** — 운전자가 화면을 보게 만드는 값은
 * 타입에서부터 빼 둔다(AGENTS 규칙 10). 진행 상황(구간 수·경과 시간)과 지금 값(조향각·기어·거리)만 준다.
 */
internal data class ManeuverDisplayState(
    val speed: String,
    val steeringDeg: Float?,
    val gear: String?,
    val rearDistanceCm: Float?,
    val obstacleWarning: Boolean,
    val guideText: String?,
    val guideStep: String?,
    val hintText: String?,
    val attempt: Int,
    val movingSegments: Int,
    val elapsedSeconds: Long,
    val askedDone: Boolean,
    /** 도식 옆 칩: 조향·기어·거리 각각 "실신호 / 시뮬 / 미측정". */
    val steeringSignal: SignalAvailability,
    val gearSignal: SignalAvailability,
    val distanceSignal: SignalAvailability,
)

internal fun LessonPhase.Maneuver.toDisplayState() = ManeuverDisplayState(
    speed = if (snapshot.speedKmh.isFinite()) snapshot.speedKmh.coerceAtLeast(0f).roundToInt().toString() else "—",
    steeringDeg = snapshot.steeringDeg?.takeIf { it.isFinite() },
    gear = snapshot.gear?.label(),
    rearDistanceCm = snapshot.rearDistanceCm?.takeIf { it.isFinite() && it >= 0f },
    obstacleWarning = snapshot.obstacleWarning == true,
    guideText = guide?.say,
    guideStep = guide?.let { "${it.index + 1}/${it.count}" },
    hintText = lastHint?.takeIf { it.isNotBlank() },
    attempt = attempt,
    movingSegments = movingSegments,
    elapsedSeconds = elapsedMillis / 1000,
    askedDone = askedDone,
    steeringSignal = availability[VssConstants.STEERING_WHEEL_ANGLE] ?: SignalAvailability.MISSING,
    gearSignal = availability[VssConstants.TRANSMISSION_SELECTED_GEAR] ?: SignalAvailability.MISSING,
    distanceSignal = availability[VssConstants.OBSTACLE_REAR_DISTANCE_CM]
        ?: availability[VssConstants.OBSTACLE_IS_WARNING] ?: SignalAvailability.MISSING,
)

internal fun Gear.label(): String = when (this) {
    Gear.PARK -> "P"
    Gear.REVERSE -> "R"
    Gear.NEUTRAL -> "N"
    Gear.DRIVE -> "D"
}
