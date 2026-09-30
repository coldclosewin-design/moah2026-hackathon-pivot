package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.AvailabilityBadge
import mobis.vss.VssConstants

/**
 * 출발 전 점검 과제의 감점 규칙 (docs/topics/01_driving_coach.md §3.1 "하 · 출발 전 점검"). 값은 전부 가정이다.
 *  - 숙련 = 절차: 벨트·시동을 다 했나, **벨트가 시동보다 먼저**였나, [graceSeconds] 안에 끝냈나
 *  - 안전 = 차를 세운 채로 했나(움직임 0), 끝에 기어 P, 벨트를 채웠나
 * 신호가 MISSING 이면 그 항목은 감점하지 않는다(미측정).
 */
data class ChecklistRubric(
    val graceSeconds: Int = 30,
    val penaltyPerExtra10s: Int = 5,
    val penaltyNoBelt: Int = 30,
    val penaltyNoIgnition: Int = 40,
    val penaltyBeltAfterIgnition: Int = 40,
    val penaltyMoved: Int = 30,
    val penaltyNotInPark: Int = 30,
    val penaltyNoBeltSafety: Int = 40,
    // 7단계 확장(9/28) — 신호가 MISSING 이면 무감점
    /** 숙련: 브레이크를 밟지 않고 시동. */
    val penaltyNoBrakeAtIgnition: Int = 20,
    /** 숙련: 좌/우 지시등·비상등 확인을 건너뜀 — 등화당. */
    val penaltyPerSkippedLight: Int = 10,
    /** 안전: 운전석 도어가 열린 채 시동. */
    val penaltyDoorOpenAtIgnition: Int = 30,
)

/**
 * 출발 전 점검 채점. 주차와 같은 [ParkingMetrics] 를 받아(같은 [ParkingRecorder] 가 쌓는다) 같은 [ParkingScore] 를 낸다 —
 * 회차 기록·리포트·코치가 과제를 가리지 않고 한 타입으로 흐르게 하기 위해서다. 이동 구간·조향 지표는 이 과제에선 0 이거나 의미가 없다.
 */
object ChecklistScorer {
    fun score(
        metrics: ParkingMetrics,
        badge: AvailabilityBadge,
        missingSignals: List<String>,
        rubric: ChecklistRubric = ChecklistRubric(),
    ): ParkingScore {
        val pd = metrics.preDrive
        val beltMeasured = VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED !in missingSignals
        val ignitionMeasured = VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE !in missingSignals

        var skill = 100
        if (beltMeasured && pd.beltOnMillis == null) skill -= rubric.penaltyNoBelt
        if (ignitionMeasured && pd.ignitionOnMillis == null) skill -= rubric.penaltyNoIgnition
        if (pd.beltBeforeIgnition == false) skill -= rubric.penaltyBeltAfterIgnition
        if (pd.brakeBeforeIgnition == false) skill -= rubric.penaltyNoBrakeAtIgnition
        skill -= pd.skippedLights * rubric.penaltyPerSkippedLight
        val extraSeconds = maxOf(0L, completionMillis(metrics) / 1000 - rubric.graceSeconds)
        skill -= (extraSeconds / 10).toInt() * rubric.penaltyPerExtra10s

        var safety = 100
        if (metrics.motion.movingSegments > 0) safety -= rubric.penaltyMoved
        if (metrics.gear?.endedInPark == false) safety -= rubric.penaltyNotInPark
        if (beltMeasured && pd.beltOnMillis == null) safety -= rubric.penaltyNoBeltSafety
        if (pd.doorClosedBeforeIgnition == false) safety -= rubric.penaltyDoorOpenAtIgnition

        return ParkingScore(skill.coerceIn(0, 100), safety.coerceIn(0, 100), metrics, badge, missingSignals)
    }

    /** 점검이 끝난 시각 — 벨트·시동·등화 확인 중 늦은 쪽. 아무것도 없으면 회차 전체 시간. 리포트 자세히 보기("출발 준비 N초")의 근거. */
    fun completionMillis(metrics: ParkingMetrics): Long =
        listOfNotNull(metrics.preDrive.beltOnMillis, metrics.preDrive.ignitionOnMillis, metrics.preDrive.lightsDoneMillis).maxOrNull()
            ?: metrics.motion.totalMillis
}
