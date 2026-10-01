package com.moah.hackathon.ui.lesson

import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.vehicle.SignalAvailability
import mobis.vss.VssConstants as V

internal data class ChecklistIgnition(
    val value: String?, val satisfied: Boolean, val signal: SignalAvailability, val source: String,
)

/** Both current readings are required; ignition alone cannot confirm the brake. */
internal fun ManeuverDisplayState.checklistIgnition(): ChecklistIgnition {
    val brakeSource = brakeSignal.takeIf { brakePressed != null } ?: SignalAvailability.MISSING
    val ignitionSource = ignitionSignal.takeIf { ignitionOn != null } ?: SignalAvailability.MISSING
    val measured = brakeSource != SignalAvailability.MISSING && ignitionSource != SignalAvailability.MISSING
    val source = if (brakeSource == ignitionSource) signalLabel(brakeSource)
        else "브레이크 ${signalLabel(brakeSource)}\n시동 ${signalLabel(ignitionSource)}"
    return ChecklistIgnition(
        value = when {
            !measured -> null
            ignitionOn == false -> "꺼짐"
            brakePressed == true -> "밟음 → 켜짐"
            else -> "시동 켜짐"
        },
        satisfied = measured && brakePressed == true && ignitionOn == true,
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
    return listOf(
        ChecklistResult("도어", measured(V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN, pre.doorClosedBeforeIgnition), "시동 전 닫힘"),
        ChecklistResult("안전벨트", if (belt == true) pre.beltBeforeIgnition else belt,
            listOfNotNull(pre.beltOnMillis?.takeIf { belt != null }?.let { "벨트 ${it / 1000}초" },
                when (pre.beltBeforeIgnition.takeIf { belt != null }) {
                    true -> "벨트 먼저"; false -> "시동 먼저"; null -> "시동 전 착용"
                }).joinToString(" · ")),
        ChecklistResult("기어", measured(V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, score.metrics.gear?.endedInPark), "기어 P로 마침"),
        ChecklistResult("브레이크 / 시동", ignitionCheck,
            pre.ignitionOnMillis?.takeIf { ignition != null }?.let { "시동 ${it / 1000}초 · 브레이크 밟고 시동" }
                ?: "브레이크 밟고 시동"),
        ChecklistResult("좌 지시등", measured(V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, pre.leftIndicatorChecked), "켜 보기"),
        ChecklistResult("우 지시등", measured(V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, pre.rightIndicatorChecked), "켜 보기"),
        ChecklistResult("비상등", measured(V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING, pre.hazardChecked), "켜 보기"),
    )
}
