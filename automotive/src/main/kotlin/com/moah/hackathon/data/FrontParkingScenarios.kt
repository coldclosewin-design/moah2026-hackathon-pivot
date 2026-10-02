package com.moah.hackathon.data

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.VssValues
import com.moah.hackathon.vehicle.scenario
import mobis.vss.VssConstants as V

/**
 * 전면 직각 주차 시연용 Fake 신호 시나리오 2벌 (2026-10-02, `ParkingSpec.FRONT_PERPENDICULAR`).
 * 후면 시나리오([ParkingScenarios])를 거울처럼 — 진입 기어가 D 이고 보정이 R 이며, 뒤 거리(Fake 전용)는 쓰지 않는다.
 * 근접은 실물에도 있는 `ObstacleDetection.IsWarning` 하나로만 — 전면 주차는 앞 센서 경고라고 **가정**한다(INTEGRATION B 10/2).
 * 조향각은 COVESA 부호(양수 = 왼쪽). 좌표·시간은 전부 가정이다 — 실차 값이 아니다.
 */
object FrontParkingScenarios {

    private const val RIGHT_FULL = "-450.0"
    private const val LEFT_HALF = "300.0"
    private const val CENTER = "0.0"

    /** 잘한 주차: 벨트 → D → 우 끝 → 전진 원호(약 90°) → 정지·중립 → 곧게 전진 → P. 이동 2구간, 조향 1왕복, D↔R 전환 0, 근접 0. 약 28초. */
    val good: Scenario = scenario("parking-front-good", "잘한 주차") {
        at(0.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "OFF", V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.FALSE,
            V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER,
            V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.FALSE)
        speed(0.0, 0.0)
        at(2.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON")
        at(4.0, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.TRUE)
        at(6.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.DRIVE.vss)
        at(8.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_FULL)
        // 1구간: 핸들 끝까지 감은 채 천천히 전진 — 후면 good 과 같은 원호 길이라 끝 방향 약 88°
        speedRamp(10.0, 11.0, 0.0, 4.0)
        speedRamp(11.0, 16.5, 4.0, 4.0)
        speedRamp(16.5, 17.5, 4.0, 0.0)
        // 칸과 나란해지면 정지하고 핸들 중립
        at(18.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER)
        // 2구간: 곧게 전진
        speedRamp(19.5, 20.5, 0.0, 3.0)
        speedRamp(24.5, 25.5, 3.0, 0.0)
        at(27.5, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss)
    }

    /** 못한 주차: 벨트 없이 출발 → 너무 일찍 중립 → 후진(R)으로 보정 → 다시 전진 → 앞 근접 경고 + 급정지 → 곧게 마무리 → P. 이동 4구간, 조향 3왕복, D↔R 전환 2, 근접 1, 급정지 1. 약 44초. */
    val bad: Scenario = scenario("parking-front-bad", "못한 주차") {
        at(0.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "OFF", V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.FALSE,
            V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER,
            V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.FALSE)
        speed(0.0, 0.0)
        at(2.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON")
        at(4.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.DRIVE.vss)
        at(5.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_FULL)
        // 1구간: 벨트 없이 출발 (출발 뒤에야 벨트)
        speedRamp(6.0, 7.0, 0.0, 3.0)
        at(8.0, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.TRUE)
        at(10.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER) // 너무 일찍 중립
        speedRamp(12.0, 13.0, 3.0, 0.0)
        // 2구간: 후진으로 보정 (핸들 반대)
        at(14.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.REVERSE.vss)
        at(15.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to LEFT_HALF)
        speedRamp(16.0, 17.0, 0.0, 3.0)
        speedRamp(19.0, 20.0, 3.0, 0.0)
        // 3구간: 다시 전진, 앞 근접 경고 뒤 급정지
        at(21.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.DRIVE.vss)
        at(22.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_FULL)
        speedRamp(23.0, 24.0, 0.0, 4.8) // 잠금(> 5 km/h) 아래에서 최대한 — 급정지 Δv 를 벌린다
        speedRamp(24.0, 28.5, 4.8, 4.8)
        at(27.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER) // 직각보다 덜 돈 채 중립
        at(28.0, V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.TRUE)
        // 4.8 km/h → 0 in 0.3 s ≈ -4.4 m/s² (후면 bad 와 같은 여유)
        speedRamp(28.5, 28.8, 4.8, 0.0, stepSeconds = 0.15)
        at(30.0, V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.FALSE)
        // 4구간: 중립을 유지한 채 곧게 마무리
        speedRamp(34.0, 35.0, 0.0, 2.0)
        speedRamp(39.0, 40.0, 2.0, 0.0)
        at(42.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss)
    }

    val all: List<Scenario> = listOf(good, bad)
}
