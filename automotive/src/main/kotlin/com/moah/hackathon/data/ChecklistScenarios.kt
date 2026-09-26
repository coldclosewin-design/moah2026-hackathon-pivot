package com.moah.hackathon.data

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.VssValues
import com.moah.hackathon.vehicle.scenario
import mobis.vss.VssConstants as V

/**
 * 출발 전 점검 시연용 Fake 신호 시나리오 2벌. 차는 서 있고 벨트·시동·기어만 바뀐다. 시간은 전부 가정이다.
 * 채점 고정값은 `ChecklistScenarioTest` 가 지킨다.
 */
object ChecklistScenarios {

    /** 잘한 점검: 벨트(2 s) → 기어 P 그대로 → 시동(5 s). 움직임 없음. 8초. 숙련 100 · 안전 100. */
    val good: Scenario = scenario("predrive-good", "잘한 점검") {
        at(0.0, V.LOW_VOLTAGE_SYSTEM_STATE to "OFF", V.SEAT_DRIVER_ISBELTED to VssValues.FALSE,
            V.TRANSMISSION_SELECTED_GEAR to Gear.PARK.vss)
        speed(0.0, 0.0)
        at(2.0, V.SEAT_DRIVER_ISBELTED to VssValues.TRUE)
        at(5.0, V.LOW_VOLTAGE_SYSTEM_STATE to "ON")
        speed(8.0, 0.0)
    }

    /** 못한 점검: 시동 먼저(2 s, 벨트 없이) → 차가 살짝 움직임(4~7 s, 2 km/h) → 벨트는 9 s. 12초. 숙련 60(순서) · 안전 70(움직임). */
    val bad: Scenario = scenario("predrive-bad", "못한 점검") {
        at(0.0, V.LOW_VOLTAGE_SYSTEM_STATE to "OFF", V.SEAT_DRIVER_ISBELTED to VssValues.FALSE,
            V.TRANSMISSION_SELECTED_GEAR to Gear.PARK.vss)
        speed(0.0, 0.0)
        at(2.0, V.LOW_VOLTAGE_SYSTEM_STATE to "ON")
        speedRamp(4.0, 5.0, 0.0, 2.0)
        speedRamp(6.0, 7.0, 2.0, 0.0)
        at(9.0, V.SEAT_DRIVER_ISBELTED to VssValues.TRUE)
        speed(12.0, 0.0)
    }

    val all: List<Scenario> = listOf(good, bad)
}
