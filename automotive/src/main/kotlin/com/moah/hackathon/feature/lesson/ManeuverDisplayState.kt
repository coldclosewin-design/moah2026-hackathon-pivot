package com.moah.hackathon.feature.lesson

import com.moah.hackathon.vehicle.SimOnlySignals
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
    /**
     * 주차 사양(10/2): 진입 기어 — 후면(R)이면 도식은 "뒤가 위", 전면(D)이면 "앞이 위" 로 그린다(화면 몫).
     * [rearDistanceApplies] 가 false 면 이 과제는 뒤 거리를 쓰지 않는다 — 칸을 "미측정" 으로 그리지 말고 **빼라**.
     */
    val entryGear: Gear = Gear.REVERSE,
    val rearDistanceApplies: Boolean = true,
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
    /** 모드(라운드 26 시안 4-3) — 화면은 점검 왼쪽 패널의 면 색·모드 사다리를 이것으로 고른다. */
    val mode: LessonMode = LessonMode.GUIDE,
    /** 점검 목록을 어디까지 보일지([ChecklistReveal]) — 모드가 정한다. 주차 도식에는 쓰지 않는다. */
    val checklistReveal: ChecklistReveal = ChecklistReveal.ALL,
)

/**
 * 출발 전 점검 왼쪽 목록의 공개 범위(라운드 26 시안 4-3, 사용자 10/7 "가이드/힌트/평가 차이가 없는 듯").
 * 세 모드 모두 **미측정 줄은 "미측정" 그대로**(정직성 — 가림과 섞지 않는다), 항목 이름은 늘 보인다.
 */
enum class ChecklistReveal {
    /** 가이드: 이름 · 지금 값 · 지금 줄 확대(지금처럼). */
    ALL,
    /** 힌트: 된 줄은 ✓, 아직은 "—", **틀린 줄**(예: 브레이크 없이 시동)만 값까지 진하게. */
    MISTAKES,
    /** 평가: 이름 + 빈 칸만 — 값·됨/안 됨을 끝(판정 화면)까지 보이지 않는다. */
    NAMES_ONLY,
}

internal fun LessonMode.checklistReveal(): ChecklistReveal = when (this) {
    LessonMode.GUIDE, LessonMode.QUIZ -> ChecklistReveal.ALL
    LessonMode.HINT -> ChecklistReveal.MISTAKES
    LessonMode.EVALUATE -> ChecklistReveal.NAMES_ONLY
}

internal fun LessonPhase.Maneuver.toDisplayState() = ManeuverDisplayState(
    speed = if (snapshot.speedKmh.isFinite()) snapshot.speedKmh.coerceAtLeast(0f).roundToInt().toString() else "—",
    steeringDeg = snapshot.steeringDeg?.takeIf { it.isFinite() },
    gear = snapshot.gear?.label(),
    rearDistanceCm = snapshot.rearDistanceCm?.takeIf { it.isFinite() && it >= 0f && task.parkingSpec.usesRearDistance },
    obstacleWarning = snapshot.obstacleWarning == true,
    guideText = guide?.say,
    guideStep = guide?.let { "${it.index + 1}/${it.count}" },
    hintText = lastHint?.takeIf { it.isNotBlank() },
    attempt = attempt,
    movingSegments = movingSegments,
    elapsedSeconds = elapsedMillis / 1000,
    askedDone = askedDone,
    steeringSignal = availability[VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE] ?: SignalAvailability.MISSING,
    gearSignal = availability[VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR] ?: SignalAvailability.MISSING,
    distanceSignal = availability[SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM]
        ?: availability[VssConstants.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING] ?: SignalAvailability.MISSING,
    taskType = task.type,
    entryGear = task.parkingSpec.entryGear,
    rearDistanceApplies = task.parkingSpec.usesRearDistance,
    belt = snapshot.belt,
    ignitionOn = snapshot.ignitionOn,
    beltSignal = availability[VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED] ?: SignalAvailability.MISSING,
    ignitionSignal = availability[VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE] ?: SignalAvailability.MISSING,
    doorOpen = snapshot.doorOpen.takeIf { availability[VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN] != SignalAvailability.MISSING },
    brakePressed = snapshot.brakePressed,
    indicatorLeft = snapshot.indicatorLeft,
    indicatorRight = snapshot.indicatorRight,
    hazard = snapshot.hazard,
    doorSignal = availability[VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN] ?: SignalAvailability.MISSING,
    brakeSignal = availability[VssConstants.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION] ?: SignalAvailability.MISSING,
    indicatorLeftSignal = availability[VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING] ?: SignalAvailability.MISSING,
    indicatorRightSignal = availability[VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING] ?: SignalAvailability.MISSING,
    hazardSignal = availability[VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING] ?: SignalAvailability.MISSING,
    leftIndicatorChecked = preDrive?.leftIndicatorChecked,
    rightIndicatorChecked = preDrive?.rightIndicatorChecked,
    hazardChecked = preDrive?.hazardChecked,
    brakeAtIgnition = preDrive?.brakeBeforeIgnition,
    mode = mode,
    checklistReveal = mode.checklistReveal(),
)

internal fun Gear.label(): String = when (this) {
    Gear.PARK -> "P"
    Gear.REVERSE -> "R"
    Gear.NEUTRAL -> "N"
    Gear.DRIVE -> "D"
}
