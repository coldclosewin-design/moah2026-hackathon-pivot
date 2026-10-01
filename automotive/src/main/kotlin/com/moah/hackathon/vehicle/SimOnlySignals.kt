package com.moah.hackathon.vehicle

/**
 * **실물에 없는** 신호 경로 — Fake 시나리오 전용(2026-09-30, 사내 이관 1차에서 확인).
 *
 * `mobis.vss.VssConstants` 에는 실물 jar 와 같은 이름·경로만 둔다(AGENTS 절대 규칙 4). 여기 있는 키는 Real 포트가 절대 돌려주지
 * 않으므로 Real 에서는 항상 [SignalAvailability.MISSING] — 근접 판정은 `VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING` 으로 폴백한다.
 * Hybrid(`FILL_MISSING_WITH_FAKE`) 에서는 Fake 가 채우고 배지에 "시뮬레이션" 으로 잡힌다.
 */
object SimOnlySignals {
    /** 후방 장애물 거리(cm). 실물 ObstacleDetection 아래에는 IsEnabled/IsError/IsWarning 뿐이라 이 경로는 사내 목록에 없다. */
    const val OBSTACLE_REAR_DISTANCE_CM = "Vehicle.ADAS.ObstacleDetection.Rear.Distance"
}
