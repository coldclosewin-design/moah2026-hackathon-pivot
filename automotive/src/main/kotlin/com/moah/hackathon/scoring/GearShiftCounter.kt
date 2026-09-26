package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.Gear

/**
 * B층: 전·후진을 몇 번 바꿨나(R↔D). 중간의 N 은 통과 상태로 보고 세지 않는다. 마지막이 P 인지도 본다.
 * 후면 직각 주차의 이상형은 전환 0(R 로 한 번에).
 */
data class GearSummary(val reverseDriveShifts: Int, val endedInPark: Boolean, val everReversed: Boolean)

object GearShiftCounter {
    fun summarize(samples: List<GearSample>): GearSummary? {
        if (samples.isEmpty()) return null
        val driveOrReverse = samples.map { it.value }.filter { it == Gear.REVERSE || it == Gear.DRIVE }
        var shifts = 0
        for (i in 1 until driveOrReverse.size) if (driveOrReverse[i] != driveOrReverse[i - 1]) shifts++
        return GearSummary(
            reverseDriveShifts = shifts,
            endedInPark = samples.last().value == Gear.PARK,
            everReversed = driveOrReverse.any { it == Gear.REVERSE },
        )
    }
}
