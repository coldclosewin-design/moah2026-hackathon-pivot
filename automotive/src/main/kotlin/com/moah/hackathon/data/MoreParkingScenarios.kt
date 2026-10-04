package com.moah.hackathon.data

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.vehicle.VssValues
import com.moah.hackathon.vehicle.scenario
import mobis.vss.VssConstants as V

/**
 * 평행 주차·사선 주차 시연용 Fake 시나리오(10/4 전 범위 구현, `ParkingSpec.PARALLEL`·`ANGLE`). 후면 직각([ParkingScenarios])과 같은 형식.
 * 끝 방향은 자전거 모델 추정(PathReconstructor)으로 평행 ≈ 0°, 사선 ≈ 45° 가 되게 원호 길이를 맞췄다 — `MoreParkingScenariosTest` 가 고정한다.
 * 조향각은 COVESA 부호(양수 = 왼쪽). 시간·거리는 전부 가정이다.
 */
object MoreParkingScenarios {

    private const val RIGHT_FULL = "-450.0"
    private const val LEFT_FULL = "450.0"
    private const val RIGHT_HALF = "-300.0"
    private const val CENTER = "0.0"

    private fun com.moah.hackathon.vehicle.ScenarioBuilder.start(beltFirst: Boolean) {
        at(0.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "OFF", V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.FALSE,
            V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER,
            SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "250.0", V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.FALSE)
        speed(0.0, 0.0)
        at(2.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON")
        if (beltFirst) at(4.0, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.TRUE)
        at(6.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.REVERSE.vss)
    }

    /** 잘한 평행 주차: 우 끝 후진(약 45°) → 정지·좌 끝 → 후진(나란히) → 정지·중립 → P. 이동 2구간, 조향 되돌림 2(정석), 전환 0. 약 25초. */
    val parallelGood: Scenario = scenario("parallel-good", "잘한 평행") {
        start(beltFirst = true)
        at(8.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_FULL)
        speedRamp(10.0, 11.0, 0.0, 4.0)
        speedRamp(11.0, 13.5, 4.0, 4.0)
        at(12.0, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "180.0")
        speedRamp(13.5, 14.5, 4.0, 0.0)
        at(15.5, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to LEFT_FULL)
        speedRamp(17.0, 18.0, 0.0, 4.0)
        speedRamp(18.0, 20.5, 4.0, 4.0)
        at(19.5, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "90.0")
        speedRamp(20.5, 21.5, 4.0, 0.0)
        at(22.5, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER)
        at(24.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss)
        at(25.0, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "90.0")
    }

    /**
     * 못한 평행 주차: 벨트 없이 출발 → 우 끝 후진이 짧음 → 좌 끝 후진이 길어 반대로 비스듬 → 전진 보정 → 다시 후진, 근접 경고 + 급정지 → P.
     * 이동 4구간, 전환 2, 근접 1, 급정지 1. 약 40초.
     */
    val parallelBad: Scenario = scenario("parallel-bad", "못한 평행") {
        start(beltFirst = false)
        at(7.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_FULL)
        speedRamp(8.0, 9.0, 0.0, 3.0)
        at(9.5, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.TRUE)
        speedRamp(9.0, 10.5, 3.0, 3.0)
        speedRamp(10.5, 11.5, 3.0, 0.0)
        at(12.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to LEFT_FULL)
        speedRamp(13.0, 14.0, 0.0, 4.0)
        speedRamp(14.0, 18.0, 4.0, 4.0)
        speedRamp(18.0, 19.0, 4.0, 0.0)
        // 전진 보정
        at(20.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.DRIVE.vss)
        at(20.5, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_HALF)
        speedRamp(21.5, 22.5, 0.0, 3.0)
        speedRamp(23.5, 24.5, 3.0, 0.0)
        // 다시 후진 — 근접 경고 뒤 급정지
        at(25.5, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.REVERSE.vss)
        at(26.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER)
        speedRamp(27.0, 28.0, 0.0, 4.8)
        speedRamp(28.0, 30.0, 4.8, 4.8)
        at(29.5, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "35.0", V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.TRUE)
        speedRamp(30.0, 30.3, 4.8, 0.0, stepSeconds = 0.15)
        at(32.0, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "45.0", V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.FALSE)
        speedRamp(34.0, 35.0, 0.0, 2.0)
        speedRamp(36.0, 37.0, 2.0, 0.0)
        at(39.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss)
        at(40.0, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "60.0")
    }

    /** 잘한 사선 주차: 우 끝 후진(약 45°) → 정지·중립 → 곧게 후진 → P. 이동 2구간, 되돌림 1, 전환 0. 약 24초. */
    val angleGood: Scenario = scenario("angle-good", "잘한 사선") {
        start(beltFirst = true)
        at(8.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_FULL)
        speedRamp(10.0, 11.0, 0.0, 4.0)
        speedRamp(11.0, 13.8, 4.0, 4.0)
        at(12.5, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "190.0")
        speedRamp(13.8, 14.8, 4.0, 0.0)
        at(15.5, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER)
        speedRamp(16.5, 17.5, 0.0, 3.0)
        at(18.5, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "110.0")
        speedRamp(20.5, 21.5, 3.0, 0.0)
        at(22.5, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss)
        at(24.0, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "80.0")
    }

    /** 못한 사선 주차: 우 끝 후진이 길어 과회전 → 전진 보정(우 반) → 다시 후진, 근접 경고 + 급정지 → P. 이동 4구간, 전환 2. 약 38초. */
    val angleBad: Scenario = scenario("angle-bad", "못한 사선") {
        start(beltFirst = true)
        at(7.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_FULL)
        speedRamp(8.0, 9.0, 0.0, 4.0)
        speedRamp(9.0, 13.5, 4.0, 4.0)
        speedRamp(13.5, 14.5, 4.0, 0.0)
        at(15.5, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.DRIVE.vss)
        at(16.0, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to RIGHT_HALF)
        speedRamp(17.0, 18.0, 0.0, 3.0)
        speedRamp(19.0, 20.0, 3.0, 0.0)
        at(21.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.REVERSE.vss)
        at(21.5, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to CENTER)
        speedRamp(22.5, 23.5, 0.0, 4.8)
        speedRamp(23.5, 26.0, 4.8, 4.8)
        at(25.5, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "35.0", V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.TRUE)
        speedRamp(26.0, 26.3, 4.8, 0.0, stepSeconds = 0.15)
        at(28.0, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "45.0", V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING to VssValues.FALSE)
        speedRamp(30.0, 31.0, 0.0, 2.0)
        speedRamp(32.0, 33.0, 2.0, 0.0)
        at(35.0, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss)
        at(37.0, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM to "60.0")
    }

    val parallel: List<Scenario> = listOf(parallelGood, parallelBad)
    val angle: List<Scenario> = listOf(angleGood, angleBad)
}
