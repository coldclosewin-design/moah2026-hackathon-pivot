package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.SimOnlySignals

/**
 * 주차 과제별 **사양** — 채점기·판정·힌트가 "어느 방향으로 들어가는 주차인가" 를 알아야 하는 세 가지만 담는다(2026-10-02, 전면 직각 주차 추가).
 * 점수 규칙([ParkingRubric])·판정 경계([ParkingVerdicts])는 과제가 달라도 같다 — 바뀌는 것은 진입 기어, 목표 각, 구독 키뿐이다.
 *
 * @property entryGear 칸에 들어갈 때의 기어. 후면 직각 = R, 전면 직각 = D. 보정은 그 반대 기어로 한다(힌트·조언 문장이 여기서 갈린다).
 * @property targetHeadingDeg 시작 방향 대비 차가 돌아야 하는 각(판정 ② 방향의 기준). 직각 주차는 둘 다 90°.
 * @property keys 회차가 구독하는 키 = 배지의 분모. 전면 주차는 뒤 거리(Fake 전용)를 쓰지 않으므로 7개.
 * @property idealReversals 핸들 되돌림의 정석 횟수(10/4). 직각·사선 = 1(끝까지 감고 → 중립), 평행 = 2(우 끝 → 좌 끝 → 중립).
 *   채점([ParkingRubric.idealReversals])·힌트·조언이 이것을 기준으로 삼는다 — 평행 주차의 정석 동작이 감점되지 않게.
 */
data class ParkingSpec(
    val entryGear: Gear,
    val targetHeadingDeg: Float,
    val keys: Set<String>,
    val idealReversals: Int = 1,
) {
    /** 이 과제에 맞춘 채점표 — 되돌림 정석 횟수만 사양 값으로. */
    fun rubric(base: ParkingRubric = ParkingRubric()): ParkingRubric = base.copy(idealReversals = idealReversals)

    /** 보정할 때 쓰는 기어 — 진입 기어의 반대. */
    val fixGear: Gear get() = if (entryGear == Gear.REVERSE) Gear.DRIVE else Gear.REVERSE

    /** 뒤 거리(Fake 전용 `Rear.Distance`)를 화면·채점에 쓰는 과제인가. */
    val usesRearDistance: Boolean get() = SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM in keys

    companion object {
        /** 후면 직각 주차(시연 본편) — 9/26 부터의 기본값과 같다. */
        val REAR_PERPENDICULAR = ParkingSpec(
            entryGear = Gear.REVERSE,
            targetHeadingDeg = ParkingVerdicts.PERPENDICULAR_TARGET_DEG,
            keys = ParkingRecorder.KEYS,
        )

        /** 전면 직각 주차 — 앞으로 들어가므로 뒤 거리 없이 7키. 근접은 실물 `ObstacleDetection.IsWarning` 만. */
        val FRONT_PERPENDICULAR = ParkingSpec(
            entryGear = Gear.DRIVE,
            targetHeadingDeg = ParkingVerdicts.PERPENDICULAR_TARGET_DEG,
            keys = ParkingRecorder.KEYS - SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM,
        )

        /** 평행 주차(10/4) — 뒤로 들어가 시작 방향과 나란히 선다(목표 0°). 우 끝 → 좌 끝이 정석이라 되돌림 2. */
        val PARALLEL = ParkingSpec(
            entryGear = Gear.REVERSE,
            targetHeadingDeg = 0f,
            keys = ParkingRecorder.KEYS,
            idealReversals = 2,
        )

        const val ANGLE_TARGET_DEG = 45f

        /** 사선 주차(10/4) — 뒤로 들어가 45° 기울어진 칸에 맞춘다. */
        val ANGLE = ParkingSpec(
            entryGear = Gear.REVERSE,
            targetHeadingDeg = ANGLE_TARGET_DEG,
            keys = ParkingRecorder.KEYS,
        )
    }
}
