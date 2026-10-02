package com.moah.hackathon.ui.lesson

import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.vehicle.SignalAvailability
import mobis.vss.VssConstants as V

internal data class ChecklistIgnition(
    val value: String?, val satisfied: Boolean, val signal: SignalAvailability, val source: String,
)

internal fun checklistLightValue(on: Boolean?, checked: Boolean?): String? = when {
    on == true -> "켜짐"
    checked == true -> "확인"
    on != null || checked != null -> "아직"
    else -> null
}

/** Once started, use the brake reading recorded at ignition, even after the pedal is released. */
internal fun ManeuverDisplayState.checklistIgnition(): ChecklistIgnition {
    val brake = if (ignitionOn == true) brakeAtIgnition else brakePressed
    val brakeSource = brakeSignal.takeIf { brake != null } ?: SignalAvailability.MISSING
    val ignitionSource = ignitionSignal.takeIf { ignitionOn != null } ?: SignalAvailability.MISSING
    val measured = brakeSource != SignalAvailability.MISSING && ignitionSource != SignalAvailability.MISSING
    val source = if (brakeSource == ignitionSource) signalLabel(brakeSource)
        else "브레이크 ${signalLabel(brakeSource)}\n시동 ${signalLabel(ignitionSource)}"
    return ChecklistIgnition(
        value = when {
            !measured -> null
            ignitionOn == false -> "꺼짐"
            brake == true -> "밟음 → 켜짐"
            else -> "브레이크 없이 켜짐"
        },
        satisfied = measured && brake == true && ignitionOn == true,
        signal = if (!measured) SignalAvailability.MISSING else brakeSource,
        source = source,
    )
}

internal data class ChecklistResult(val label: String, val passed: Boolean?, val detail: String) {
    val mark: String get() = when (passed) { true -> "✓"; false -> "✗"; null -> "미측정" }
}

/** Report values come from the recorded attempt, including lights that have since been turned off. */
internal fun checklistResults(score: ParkingScore): List<ChecklistResult> {
    val pre = score.metrics.preDrive
    fun measured(key: String, value: Boolean?): Boolean? = value.takeUnless { key in score.missingSignals }
    val belt = measured(V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED, pre.beltOnMillis != null)
    val ignition = measured(V.VEHICLE_LOWVOLTAGESYSTEMSTATE, pre.ignitionOnMillis != null)
    val brake = measured(V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION, pre.brakeBeforeIgnition)
    val ignitionCheck = when {
        ignition == false -> false
        ignition == null || brake == null -> null
        else -> brake
    }
    val doorCheck = measured(V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN, pre.doorClosedBeforeIgnition)
    val beltCheck = if (belt == true) pre.beltBeforeIgnition else belt
    val gearCheck = measured(V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, score.metrics.gear?.endedInPark)
    fun detail(passed: Boolean?, yes: String, no: String, unknown: String = "확인할 수 없어요") = when (passed) {
        true -> yes; false -> no; null -> unknown
    }
    fun light(label: String, key: String, checked: Boolean?): ChecklistResult {
        val passed = measured(key, checked)
        return ChecklistResult(label, passed, detail(passed, "켜짐 확인", "켜짐 확인 안 됨"))
    }
    return listOf(
        ChecklistResult("도어", doorCheck, detail(doorCheck, "시동 전 닫힘", "시동 때 문 열림", "순서를 확인할 수 없어요")),
        ChecklistResult("안전벨트", beltCheck,
            listOfNotNull(pre.beltOnMillis?.takeIf { belt != null }?.let { "벨트 ${it / 1000}초" },
                detail(beltCheck, "벨트 먼저", if (belt == false) "벨트 착용 확인 안 됨" else "시동 먼저",
                    "순서를 확인할 수 없어요")).joinToString(" · ")),
        ChecklistResult("기어", gearCheck, detail(gearCheck, "기어 P로 마침", "기어 P로 마치지 않음")),
        ChecklistResult("브레이크 / 시동", ignitionCheck,
            listOfNotNull(pre.ignitionOnMillis?.takeIf { ignition != null }?.let { "시동 ${it / 1000}초" },
                detail(ignitionCheck, "브레이크 밟고 시동", if (ignition == false) "시동 확인 안 됨" else "브레이크 확인 안 됨",
                    "순서를 확인할 수 없어요")).joinToString(" · ")),
        light("좌 지시등", V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, pre.leftIndicatorChecked),
        light("우 지시등", V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, pre.rightIndicatorChecked),
        light("비상등", V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING, pre.hazardChecked),
    )
}
