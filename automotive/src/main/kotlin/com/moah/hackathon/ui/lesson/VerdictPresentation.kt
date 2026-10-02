package com.moah.hackathon.ui.lesson

import com.moah.hackathon.scoring.ParkingVerdict
import kotlin.math.roundToInt

internal enum class VerdictMark(val symbol: String) { PASS("✓"), CAUTION("△"), FAIL("✗"), MISSING("—") }
internal data class VerdictLine(val text: String, val mark: VerdictMark, val note: String? = null)

/** Present the recorded verdict without reinterpreting scores or inferring missing signals. */
internal fun verdictLines(verdict: ParkingVerdict?): List<VerdictLine> {
    if (verdict == null) return listOf("진입은 알 수 없어요", "방향은 알 수 없어요", "마무리는 알 수 없어요", "안전은 알 수 없어요")
        .map { VerdictLine(it, VerdictMark.MISSING) }
    return listOf(
        when (verdict.entry) {
            ParkingVerdict.Entry.ONE_GO -> VerdictLine("한 번에 들어갔어요", VerdictMark.PASS)
            ParkingVerdict.Entry.ONE_FIX -> VerdictLine("한 번 다시 넣고 들어갔어요", VerdictMark.CAUTION)
            ParkingVerdict.Entry.MANY -> VerdictLine("여러 번 오가며 들어갔어요", VerdictMark.FAIL)
        },
        when (verdict.heading) {
            ParkingVerdict.Heading.ALIGNED -> VerdictLine("방향은 맞게 섰어요", VerdictMark.PASS, "신호로 추정")
            ParkingVerdict.Heading.SLIGHT -> VerdictLine("방향이 조금 틀어졌어요", VerdictMark.CAUTION, "신호로 추정")
            ParkingVerdict.Heading.OFF -> VerdictLine("방향이 많이 틀어졌어요", VerdictMark.FAIL, "신호로 추정")
            ParkingVerdict.Heading.UNKNOWN -> VerdictLine("방향은 알 수 없어요", VerdictMark.MISSING)
        },
        when (verdict.finish) {
            ParkingVerdict.Finish.CLEAN -> VerdictLine("마무리가 깔끔했어요", VerdictMark.PASS)
            ParkingVerdict.Finish.LOOSE -> VerdictLine("마무리를 더 살펴봐요", VerdictMark.CAUTION)
        },
        when (verdict.safety) {
            ParkingVerdict.Safety.SAFE -> VerdictLine("안전했어요", VerdictMark.PASS)
            ParkingVerdict.Safety.WATCH -> VerdictLine("주의할 게 있었어요", VerdictMark.CAUTION)
            ParkingVerdict.Safety.UNSAFE -> VerdictLine("위험한 순간이 있었어요", VerdictMark.FAIL)
        },
    )
}

internal fun headingDetailLine(verdict: ParkingVerdict?) =
    "방향 편차 ${verdict?.headingErrorDeg?.let { "${it.roundToInt()}°" } ?: "미측정"}"
