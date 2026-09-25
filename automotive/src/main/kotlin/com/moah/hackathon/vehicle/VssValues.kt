package com.moah.hackathon.vehicle

import java.math.BigInteger

/**
 * VSS 값(String) → 타입 변환. docs/02_vss_api_contract.md 변환표 기준.
 * 실제 값 포맷은 사내에서만 확인 가능하므로 전부 관대한(nullable) 파서로 둔다.
 */
object VssValues {
    const val TRUE = "true"
    const val FALSE = "false"

    fun ofBoolean(v: Boolean): String = if (v) TRUE else FALSE
    fun ofFloat(v: Float): String = v.toString()
    fun ofInt(v: Int): String = v.toString()
}

/** boolean */
fun String?.toVssBoolean(): Boolean? = when (this?.trim()?.lowercase()) {
    "true", "1" -> true
    "false", "0" -> false
    else -> null
}

/** uint8/int8/uint16/int16/int32 */
fun String?.toVssInt(): Int? = this?.trim()?.toIntOrNull() ?: this?.trim()?.toFloatOrNull()?.toInt()

/** uint32/int64 */
fun String?.toVssLong(): Long? = this?.trim()?.toLongOrNull() ?: this?.trim()?.toDoubleOrNull()?.toLong()

/** uint64 */
fun String?.toVssBigInteger(): BigInteger? = this?.trim()?.let { runCatching { BigInteger(it) }.getOrNull() }

/** float */
fun String?.toVssFloat(): Float? = this?.trim()?.toFloatOrNull()

/** double */
fun String?.toVssDouble(): Double? = this?.trim()?.toDoubleOrNull()
