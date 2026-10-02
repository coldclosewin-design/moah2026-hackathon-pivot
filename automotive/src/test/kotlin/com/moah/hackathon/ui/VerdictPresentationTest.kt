package com.moah.hackathon.ui

import com.moah.hackathon.scoring.ParkingVerdict
import com.moah.hackathon.scoring.ParkingVerdict.*
import com.moah.hackathon.ui.lesson.*
import org.junit.Assert.*
import org.junit.Test

class VerdictPresentationTest {
    @Test fun allSeventyTwoVerdictsKeepTheirFourIndependentFactsAndNoNumbers() {
        val entry = listOf("한 번에 들어갔어요" to VerdictMark.PASS, "한 번 다시 넣고 들어갔어요" to VerdictMark.CAUTION,
            "여러 번 오가며 들어갔어요" to VerdictMark.FAIL)
        val heading = listOf("방향은 맞게 섰어요" to VerdictMark.PASS, "방향이 조금 틀어졌어요" to VerdictMark.CAUTION,
            "방향이 많이 틀어졌어요" to VerdictMark.FAIL, "방향은 알 수 없어요" to VerdictMark.MISSING)
        val finish = listOf("마무리가 깔끔했어요" to VerdictMark.PASS, "마무리를 더 살펴봐요" to VerdictMark.CAUTION)
        val safety = listOf("안전했어요" to VerdictMark.PASS, "주의할 게 있었어요" to VerdictMark.CAUTION,
            "위험한 순간이 있었어요" to VerdictMark.FAIL)
        var count = 0
        for (e in Entry.entries) for (h in Heading.entries) for (f in Finish.entries) for (s in Safety.entries) {
            val result = verdictLines(ParkingVerdict(e, h, 16.579f, f, s))
            assertEquals(listOf(entry[e.ordinal], heading[h.ordinal], finish[f.ordinal], safety[s.ordinal]),
                result.map { it.text to it.mark })
            assertEquals(listOf(null, if (h == Heading.UNKNOWN) null else "신호로 추정", null, null), result.map { it.note })
            assertTrue(result.all { !Regex("\\d|°|점수").containsMatchIn(it.text + it.note) })
            count++
        }
        assertEquals(72, count)
    }

    @Test fun missingVerdictNeverClaimsAnyMeasuredSuccess() {
        val lines = verdictLines(null)
        assertEquals(listOf("진입은 알 수 없어요", "방향은 알 수 없어요", "마무리는 알 수 없어요", "안전은 알 수 없어요"), lines.map { it.text })
        assertTrue(lines.all { it.mark == VerdictMark.MISSING && it.note == null })
        assertEquals(listOf("✓", "△", "✗", "—"), VerdictMark.entries.map { it.symbol })
    }

    @Test fun detailsRoundOnlyRecordedAngleAndKeepNullUnmeasured() {
        val good = ParkingVerdict(Entry.ONE_GO, Heading.ALIGNED, 1.515f, Finish.CLEAN, Safety.SAFE)
        assertEquals("방향 편차 2°", headingDetailLine(good))
        assertEquals("방향 편차 17°", headingDetailLine(good.copy(headingErrorDeg = 16.579f)))
        assertEquals("방향 편차 0°", headingDetailLine(good.copy(headingErrorDeg = 0f)))
        assertEquals("방향 편차 미측정", headingDetailLine(good.copy(headingErrorDeg = null)))
        assertEquals("방향 편차 미측정", headingDetailLine(null))
    }
}
