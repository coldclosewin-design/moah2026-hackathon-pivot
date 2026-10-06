package com.moah.hackathon.vehicle

/**
 * **실물에 없는(또는 사내 목록에서 아직 확인하지 못한)** 신호 경로 — Fake 시나리오가 만든다(2026-09-30, 10/4 확장).
 *
 * `mobis.vss.VssConstants` 에는 실물 jar 와 같은 이름·경로만 둔다(AGENTS 절대 규칙 4). 여기 있는 키는 Real 포트가 돌려주지 않으므로
 * Real 단독에서는 [SignalAvailability.MISSING], Hybrid(`FILL_MISSING_WITH_FAKE`) 에서는 Fake 가 채우고 배지에 "시뮬레이션" 으로 잡힌다.
 *
 * 10/4 전 범위 구현(docs/design/10_full_scope.md): 신호가 없다고 기능을 빼지 않는다. 필요한 신호를 여기에 정의하고 Fake 로 채우며,
 * 발표에서 "실제로는 ○○ 가 준다" 고 설명한다. 키 묶음:
 *  - **차량**: 뒤 거리 — 사내 목록에 같은 경로가 있으면 `VssConstants` 로 옮긴다(INTEGRATION B 10/4).
 *    전조등·와이퍼는 사내 jar 에 있어 2026-10-06(사내 피드백 #4) `VssConstants` 로 옮겼다 — [DeviceSignals].
 *  - **제휴 시험장(`Track.*`)**: 트랙 좌표계 위치·방향 · 신호등 · 돌발 경보 · 검지선 접촉 — 실제로는 시험장 전자채점 시스템·RTK-GPS 가
 *    차량에 내려 준다고 가정한다. VSS 트리 밖이라 `Vehicle.` 접두가 없다.
 */
object SimOnlySignals {
    /** 후방 장애물 거리(cm). 실물 ObstacleDetection 아래에는 IsEnabled/IsError/IsWarning 뿐이라 이 경로는 사내 목록에 없다. */
    const val OBSTACLE_REAR_DISTANCE_CM = "Vehicle.ADAS.ObstacleDetection.Rear.Distance"

    /** 트랙 좌표계 위치(m). 원점·축은 코스 도면([com.moah.hackathon.scoring.TrackMap]) 기준, +y = 도면 위. 실제로는 RTK-GPS(cm 급) 또는 코스 검지 센서. */
    const val TRACK_POSITION_X_M = "Track.Position.X"
    const val TRACK_POSITION_Y_M = "Track.Position.Y"

    /** 트랙 좌표계 진행 방향(°). 0 = +y(도면 위), 반시계(왼쪽) 양수 — [com.moah.hackathon.scoring.PathPoint] 와 같은 규약. */
    const val TRACK_HEADING_DEG = "Track.Heading"

    /** 차가 지금 마주한 신호등 — [TrackSignal] 이름 문자열. 신호등이 없는 곳은 "OFF". 실제로는 시험장 신호 제어기 API. */
    const val TRACK_SIGNAL_STATE = "Track.Signal.State"

    /** 돌발 경보(장내기능 돌발 구간 · 도로 과제의 갑작스런 보행자). true 동안 경보 중. 실제로는 시험장 전자채점 시스템. */
    const val TRACK_EVENT_EMERGENCY = "Track.Event.Emergency"

    /** 검지선(차로 경계·주차 칸 선) 접촉. true 동안 접촉 중. 실제로는 코스 매설 검지선 / 도로에서는 차량 ADAS 차선 신호. */
    const val TRACK_LINE_CONTACT = "Track.Line.Contact"

    /** 코스 과제가 구독하는 시험장 키 전부. */
    val TRACK_KEYS: Set<String> = setOf(
        TRACK_POSITION_X_M, TRACK_POSITION_Y_M, TRACK_HEADING_DEG,
        TRACK_SIGNAL_STATE, TRACK_EVENT_EMERGENCY, TRACK_LINE_CONTACT,
    )
}

/** 시험장 신호등 상태. 모르는 값은 null(파서). */
enum class TrackSignal { RED, YELLOW, GREEN, OFF }

fun String?.toTrackSignal(): TrackSignal? = this?.trim()?.uppercase()?.let { v -> TrackSignal.entries.firstOrNull { it.name == v } }

