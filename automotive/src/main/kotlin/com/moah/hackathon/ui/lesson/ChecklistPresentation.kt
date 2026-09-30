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
    val belt = measured(V.SEAT_DRIVER_ISBELTED, pre.beltOnMillis != null)
    val ignition = measured(V.LOW_VOLTAGE_SYSTEM_STATE, pre.ignitionOnMillis != null)
    val brake = measured(V.BRAKE_PEDAL_POSITION, pre.brakeBeforeIgnition)
    val ignitionCheck = when {
        ignition == false -> false
        ignition == null || brake == null -> null
        else -> brake
    }
    return listOf(
        ChecklistResult("도어", measured(V.DOOR_DRIVER_ISOPEN, pre.doorClosedBeforeIgnition), "시동 전 닫힘"),
        ChecklistResult("안전벨트", if (belt == true) pre.beltBeforeIgnition else belt,
            listOfNotNull(pre.beltOnMillis?.takeIf { belt != null }?.let { "벨트 ${it / 1000}초" },
                when (pre.beltBeforeIgnition.takeIf { belt != null }) {
                    true -> "벨트 먼저"; false -> "시동 먼저"; null -> "시동 전 착용"
                }).joinToString(" · ")),
        ChecklistResult("기어", measured(V.TRANSMISSION_SELECTED_GEAR, score.metrics.gear?.endedInPark), "기어 P로 마침"),
        ChecklistResult("브레이크 / 시동", ignitionCheck,
            pre.ignitionOnMillis?.takeIf { ignition != null }?.let { "시동 ${it / 1000}초 · 브레이크 밟고 시동" }
                ?: "브레이크 밟고 시동"),
        ChecklistResult("좌 지시등", measured(V.LIGHT_INDICATOR_LEFT, pre.leftIndicatorChecked), "켜 보기"),
        ChecklistResult("우 지시등", measured(V.LIGHT_INDICATOR_RIGHT, pre.rightIndicatorChecked), "켜 보기"),
        ChecklistResult("비상등", measured(V.LIGHT_HAZARD, pre.hazardChecked), "켜 보기"),
    )
}
