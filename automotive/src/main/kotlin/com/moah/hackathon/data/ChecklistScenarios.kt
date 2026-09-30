package com.moah.hackathon.data

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.VssValues
import com.moah.hackathon.vehicle.scenario
import mobis.vss.VssConstants as V

/**
 * 출발 전 점검 7단계(9/28, D1) 시연용 Fake 신호 시나리오 2벌. 차는 서 있고 문·벨트·브레이크·시동·등화만 바뀐다. 시간은 전부 가정이다.
 * 채점 고정값은 `ChecklistScenarioTest` 가 지킨다.
 */
object ChecklistScenarios {

    private val start = arrayOf(
        V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "OFF", V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.FALSE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to Gear.PARK.vss,
        V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.TRUE, V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION to "0",
        V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to VssValues.FALSE, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING to VssValues.FALSE, V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING to VssValues.FALSE,
    )

    /** 잘한 점검: 문 닫음(1 s) → 벨트(3 s) → P 그대로 → 브레이크(7 s) 밟고 시동(8 s) → 좌(10 s)·우(12 s) 지시등 → 비상등(14 s). 움직임 없음. 16초. 100 · 100. */
    val good: Scenario = scenario("predrive-good", "잘한 점검") {
        at(0.0, *start)
        speed(0.0, 0.0)
        at(1.0, V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.FALSE)
        at(3.0, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.TRUE)
        at(7.0, V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION to "40")
        at(8.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON")
        at(9.0, V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION to "0")
        at(10.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to VssValues.TRUE)
        at(11.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to VssValues.FALSE)
        at(12.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING to VssValues.TRUE)
        at(13.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING to VssValues.FALSE)
        at(14.0, V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING to VssValues.TRUE)
        at(15.0, V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING to VssValues.FALSE)
        speed(16.0, 0.0)
    }

    /**
     * 못한 점검: 문 열린 채·브레이크 없이·벨트 없이 시동(2 s) → 차가 살짝 움직임(4~7 s, 2 km/h) → 벨트 9 s → 문 10 s → 좌(12 s)·우(14 s) 지시등 → 비상등 건너뜀. 20초.
     * 숙련 30(순서 -40 · 브레이크 -20 · 비상등 -10) · 안전 40(문 열린 채 시동 -30 · 움직임 -30).
     */
    val bad: Scenario = scenario("predrive-bad", "못한 점검") {
        at(0.0, *start)
        speed(0.0, 0.0)
        at(2.0, V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON")
        speedRamp(4.0, 5.0, 0.0, 2.0)
        speedRamp(6.0, 7.0, 2.0, 0.0)
        at(9.0, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to VssValues.TRUE)
        at(10.0, V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.FALSE)
        at(12.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to VssValues.TRUE)
        at(13.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to VssValues.FALSE)
        at(14.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING to VssValues.TRUE)
        at(15.0, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING to VssValues.FALSE)
        speed(20.0, 0.0)
    }

    val all: List<Scenario> = listOf(good, bad)
}
