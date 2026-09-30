package com.moah.hackathon.feature.lesson

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.SignalAvailability
import mobis.vss.VssConstants
import kotlin.math.roundToInt

/**
 * 주차 중 화면(도식)이 받는 값 전부. **점수·감점 누계·조향 왕복 수·기어 전환 수는 없다** — 운전자가 화면을 보게 만드는 값은
 * 타입에서부터 빼 둔다(AGENTS 규칙 10). 진행 상황(구간 수·경과 시간)과 지금 값(조향각·기어·거리)만 준다.
 *
 * [taskType] 이 [TaskType.CHECKLIST] 면 화면은 도식 대신 벨트·기어·시동 세 칩을 그린다([belt]·[gear]·[ignitionOn]).
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
    /** 과제 유형 — 주차는 도식, 출발 전 점검은 칩 3개. */
    val taskType: TaskType = TaskType.PARKING,
    /** 출발 전 점검용 현재 값. 신호가 없으면 null(미측정). */
    val belt: Boolean? = null,
    val ignitionOn: Boolean? = null,
    val beltSignal: SignalAvailability = SignalAvailability.MISSING,
    val ignitionSignal: SignalAvailability = SignalAvailability.MISSING,
    /** 7단계 확장(9/28): 문·브레이크·좌/우 지시등·비상등. 신호가 없으면 null(미측정). */
    val doorOpen: Boolean? = null,
    val brakePressed: Boolean? = null,
    val indicatorLeft: Boolean? = null,
    val indicatorRight: Boolean? = null,
    val hazard: Boolean? = null,
    val doorSignal: SignalAvailability = SignalAvailability.MISSING,
    val brakeSignal: SignalAvailability = SignalAvailability.MISSING,
    val indicatorLeftSignal: SignalAvailability = SignalAvailability.MISSING,
    val indicatorRightSignal: SignalAvailability = SignalAvailability.MISSING,
    val hazardSignal: SignalAvailability = SignalAvailability.MISSING,
    /**
     * 출발 전 점검 **기록**(9/30): 회차 중 한 번이라도 켜 봤나 / 시동 순간 브레이크를 밟았나. 켰다 끈 뒤에도 true 로 남는다.
     * 신호 없음·아직 시동 전 → null. 칩의 완료 색은 이것으로(현재 값 [indicatorLeft] 등은 "지금 켜짐" 표시용).
     */
    val leftIndicatorChecked: Boolean? = null,
    val rightIndicatorChecked: Boolean? = null,
    val hazardChecked: Boolean? = null,
    val brakeAtIgnition: Boolean? = null,
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
    taskType = task.type,
    belt = snapshot.belt,
    ignitionOn = snapshot.ignitionOn,
    beltSignal = availability[VssConstants.SEAT_DRIVER_ISBELTED] ?: SignalAvailability.MISSING,
    ignitionSignal = availability[VssConstants.LOW_VOLTAGE_SYSTEM_STATE] ?: SignalAvailability.MISSING,
    doorOpen = snapshot.doorOpen.takeIf { availability[VssConstants.DOOR_DRIVER_ISOPEN] != SignalAvailability.MISSING },
    brakePressed = snapshot.brakePressed,
    indicatorLeft = snapshot.indicatorLeft,
    indicatorRight = snapshot.indicatorRight,
    hazard = snapshot.hazard,
    doorSignal = availability[VssConstants.DOOR_DRIVER_ISOPEN] ?: SignalAvailability.MISSING,
    brakeSignal = availability[VssConstants.BRAKE_PEDAL_POSITION] ?: SignalAvailability.MISSING,
    indicatorLeftSignal = availability[VssConstants.LIGHT_INDICATOR_LEFT] ?: SignalAvailability.MISSING,
    indicatorRightSignal = availability[VssConstants.LIGHT_INDICATOR_RIGHT] ?: SignalAvailability.MISSING,
    hazardSignal = availability[VssConstants.LIGHT_HAZARD] ?: SignalAvailability.MISSING,
    leftIndicatorChecked = preDrive?.leftIndicatorChecked,
    rightIndicatorChecked = preDrive?.rightIndicatorChecked,
    hazardChecked = preDrive?.hazardChecked,
    brakeAtIgnition = preDrive?.brakeBeforeIgnition,
)

internal fun Gear.label(): String = when (this) {
    Gear.PARK -> "P"
    Gear.REVERSE -> "R"
    Gear.NEUTRAL -> "N"
    Gear.DRIVE -> "D"
}
