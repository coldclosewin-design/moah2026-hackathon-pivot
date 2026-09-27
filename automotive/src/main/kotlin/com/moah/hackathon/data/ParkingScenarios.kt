package com.moah.hackathon.data

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.VssValues
import com.moah.hackathon.vehicle.scenario
import mobis.vss.VssConstants as V

/**
 * 후면 직각 주차 시연용 Fake 신호 시나리오 2벌 (docs/topics/01_driving_coach.md §4.4).
 * 조향각은 COVESA 부호(양수 = 왼쪽). 후진은 기어로만 구분하고 속도는 크기만 쓴다.
 * 좌표·시간은 전부 가정이다 — 실차 값이 아니다.
 */
object ParkingScenarios {

    private const val RIGHT_FULL = "-450.0"
    private const val LEFT_HALF = "300.0"
    private const val CENTER = "0.0"

    /** 잘한 주차: 벨트 → R → 우 끝 → 후진 → 45°에서 정지·중립 → 곧게 후진 → P. 이동 2구간, 조향 1왕복, R↔D 전환 0, 근접 0. 약 26초. */
    val good: Scenario = scenario("parking-good", "잘한 주차") {
        at(0.0, V.LOW_VOLTAGE_SYSTEM_STATE to "OFF", V.SEAT_DRIVER_ISBELTED to VssValues.FALSE,
            V.TRANSMISSION_SELECTED_GEAR to Gear.PARK.vss, V.STEERING_WHEEL_ANGLE to CENTER,
            V.OBSTACLE_REAR_DISTANCE_CM to "250.0", V.OBSTACLE_IS_WARNING to VssValues.FALSE)
        speed(0.0, 0.0)
        at(2.0, V.LOW_VOLTAGE_SYSTEM_STATE to "ON")
        at(4.0, V.SEAT_DRIVER_ISBELTED to VssValues.TRUE)
        at(6.0, V.TRANSMISSION_SELECTED_GEAR to Gear.REVERSE.vss)
        at(8.0, V.STEERING_WHEEL_ANGLE to RIGHT_FULL)
        // 1구간: 핸들 끝까지 감은 채 천천히 후진
        speedRamp(10.0, 11.0, 0.0, 4.0)
        at(11.0, V.OBSTACLE_REAR_DISTANCE_CM to "200.0")
        speedRamp(13.0, 14.0, 4.0, 0.0)
        // 45° — 정지하고 핸들 중립
        at(14.5, V.STEERING_WHEEL_ANGLE to CENTER)
        // 2구간: 곧게 후진
        speedRamp(16.0, 17.0, 0.0, 3.0)
        at(18.0, V.OBSTACLE_REAR_DISTANCE_CM to "120.0")
        at(20.0, V.OBSTACLE_REAR_DISTANCE_CM to "80.0")
        speedRamp(21.0, 22.0, 3.0, 0.0)
        at(24.0, V.TRANSMISSION_SELECTED_GEAR to Gear.PARK.vss)
        at(26.0, V.OBSTACLE_REAR_DISTANCE_CM to "80.0")
    }

    /** 못한 주차: 벨트 없이 출발 → 너무 일찍 중립 → 전진 보정 → 다시 후진 → 근접 경고 + 급정지 → 다시 후진 → P. 이동 4구간, 조향 3왕복, R↔D 전환 2, 근접 1, 급정지 1. 약 60초. */
    val bad: Scenario = scenario("parking-bad", "못한 주차") {
        at(0.0, V.LOW_VOLTAGE_SYSTEM_STATE to "OFF", V.SEAT_DRIVER_ISBELTED to VssValues.FALSE,
            V.TRANSMISSION_SELECTED_GEAR to Gear.PARK.vss, V.STEERING_WHEEL_ANGLE to CENTER,
            V.OBSTACLE_REAR_DISTANCE_CM to "250.0", V.OBSTACLE_IS_WARNING to VssValues.FALSE)
        speed(0.0, 0.0)
        at(2.0, V.LOW_VOLTAGE_SYSTEM_STATE to "ON")
        at(4.0, V.TRANSMISSION_SELECTED_GEAR to Gear.REVERSE.vss)
        at(5.0, V.STEERING_WHEEL_ANGLE to RIGHT_FULL)
        // 1구간: 벨트 없이 출발 (출발 뒤에야 벨트)
        speedRamp(6.0, 7.0, 0.0, 3.0)
        at(8.0, V.SEAT_DRIVER_ISBELTED to VssValues.TRUE)
        at(10.0, V.STEERING_WHEEL_ANGLE to CENTER) // 너무 일찍 중립
        speedRamp(12.0, 13.0, 3.0, 0.0)
        // 2구간: 전진으로 보정 (핸들 반대)
        at(14.0, V.TRANSMISSION_SELECTED_GEAR to Gear.DRIVE.vss)
        at(15.0, V.STEERING_WHEEL_ANGLE to LEFT_HALF)
        speedRamp(16.0, 17.0, 0.0, 3.0)
        speedRamp(19.0, 20.0, 3.0, 0.0)
        // 3구간: 다시 후진, 근접 경고 뒤 급정지
        at(21.0, V.TRANSMISSION_SELECTED_GEAR to Gear.REVERSE.vss)
        at(22.0, V.STEERING_WHEEL_ANGLE to RIGHT_FULL)
        speedRamp(23.0, 24.0, 0.0, 4.8) // 잠금(> 5 km/h) 아래에서 최대한 — 급정지 Δv 를 벌린다
        at(26.0, V.OBSTACLE_REAR_DISTANCE_CM to "90.0")
        at(28.0, V.OBSTACLE_REAR_DISTANCE_CM to "35.0", V.OBSTACLE_IS_WARNING to VssValues.TRUE)
        // 4.8 km/h → 0 in 0.3 s ≈ -4.4 m/s². 임계 -3.0 까지 마지막 샘플이 144 ms 늦어도 잡힌다(4.0/-3.5 였을 땐 17 ms — 부하에서 빠짐)
        speedRamp(28.5, 28.8, 4.8, 0.0, stepSeconds = 0.15)
        at(30.0, V.OBSTACLE_REAR_DISTANCE_CM to "40.0", V.OBSTACLE_IS_WARNING to VssValues.FALSE)
        // 4구간: 핸들 중립, 곧게 마무리
        at(32.0, V.STEERING_WHEEL_ANGLE to CENTER)
        speedRamp(34.0, 35.0, 0.0, 2.0)
        at(37.0, V.OBSTACLE_REAR_DISTANCE_CM to "60.0")
        speedRamp(39.0, 40.0, 2.0, 0.0)
        at(42.0, V.TRANSMISSION_SELECTED_GEAR to Gear.PARK.vss)
        at(44.0, V.OBSTACLE_REAR_DISTANCE_CM to "60.0")
    }

    val all: List<Scenario> = listOf(good, bad)
}
