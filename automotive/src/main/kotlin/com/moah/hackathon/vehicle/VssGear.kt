package com.moah.hackathon.vehicle

/**
 * 기어 위치. VSS `Vehicle.Powertrain.Transmission.SelectedGear` 의 COVESA 인코딩(0=N, 양수=D, 음수=R, 126=P, 127=D)을
 * 앱이 쓰는 네 값으로 접는다. 실물 인코딩은 사내 확인 전까지 가정이므로 문자 "P/R/N/D" 도 받아들인다.
 */
enum class Gear(val vss: String) {
    PARK("126"), REVERSE("-1"), NEUTRAL("0"), DRIVE("127");

    companion object {
        fun parse(raw: String?): Gear? {
            val v = raw?.trim() ?: return null
            when (v.uppercase()) {
                "P", "PARK" -> return PARK
                "R", "REVERSE" -> return REVERSE
                "N", "NEUTRAL" -> return NEUTRAL
                "D", "DRIVE" -> return DRIVE
            }
            val n = v.toIntOrNull() ?: v.toFloatOrNull()?.toInt() ?: return null
            return when {
                n == 126 -> PARK
                n == 127 -> DRIVE
                n == 0 -> NEUTRAL
                n < 0 -> REVERSE
                else -> DRIVE
            }
        }
    }
}

fun String?.toVssGear(): Gear? = Gear.parse(this)

/** `Vehicle.LowVoltageSystemState` 가 시동 켜짐으로 볼 수 있는 값인가. 실물 값은 사내 확인 전까지 가정. */
fun String?.toVssIgnitionOn(): Boolean? = when (this?.trim()?.uppercase()) {
    "ON", "START" -> true
    "OFF", "ACC", "LOCK", "UNDEFINED" -> false
    "TRUE", "1" -> true
    "FALSE", "0" -> false
    else -> null
}
