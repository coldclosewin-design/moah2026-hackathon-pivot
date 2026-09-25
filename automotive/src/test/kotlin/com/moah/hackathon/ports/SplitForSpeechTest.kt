package com.moah.hackathon.ports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitForSpeechTest {
    /** 상태기계가 실제로 만드는 최종 도착 멘트(JourneyStateMachineTest 가 전문을 고정한다). 줄 = 주제(메모 / 가는 길 / 예약 / 산책 / 마지막 메시지). */
    private val topics = listOf(
        "아버지의 메모예요. 여울이가 어릴 때 가장 좋아했던 피자집.",
        "가는 길은 화면에 있어요. E3 엘리베이터가 제일 빨라요. 피자집 창가 자리까지 약 2분.",
        "예약해 뒀어요. 5층 피자집 창가 자리, 오늘 12:30, 2명.",
        "옥상정원 한 바퀴, 15분이면 돼요. 다 먹고 옥상 올라가 봐라. 세 번째 벤치, 거기서 보는 판교가 제일 좋더라.",
        "아버지의 마지막 메시지예요. 아들, 아빠랑 피자 치즈 쭉 늘리면서 대결했던 거 기억나니? 나중에 또 아빠랑 대결하자. 맛있게 먹으렴.",
    )
    private val finalArrival = topics.joinToString("\n")

    @Test
    fun `long arrival line becomes caption sized chunks and loses nothing`() {
        val chunks = splitForSpeech(finalArrival)
        assertEquals(finalArrival.asCaption(), chunks.joinToString(" "))
        assertTrue("several captions, was ${chunks.size}", chunks.size in 5..9)
        chunks.forEach { assertTrue("fits one caption: $it", it.length <= 56) }
        // 머리말과 내용이 한 자막에 같이 나온다
        assertEquals(topics[0], chunks.first())
        assertTrue(chunks.any { it.startsWith("아버지의 마지막 메시지예요. 아들, 아빠랑") })
        assertTrue(chunks.last().endsWith("맛있게 먹으렴."))
    }

    @Test
    fun `chunks never join two topics`() {
        val chunks = splitForSpeech(finalArrival)
        // 짧은 주제는 통째로 한 자막: 화면이 카드마다 음성에 맞춰 주목시킨다
        assertEquals(topics[0], chunks[0])
        assertEquals(topics[1], chunks[1])
        assertEquals(topics[2], chunks[2])
        // 모든 토막은 정확히 한 주제 안에 있다(예전에는 "…약 2분. 예약해 뒀어요." 처럼 섞였다)
        chunks.forEach { chunk -> assertEquals("chunk spans topics: $chunk", 1, topics.count { chunk in it }) }
        // 공백으로 이으면(줄바꿈 없음) 예전처럼 주제를 넘어 합친다 — 경계는 줄바꿈만이 만든다
        assertTrue(splitForSpeech(finalArrival.asCaption()).any { "약 2분. 예약해 뒀어요." in it })
        assertEquals("가 나.", "가\n\n  나.  ".asCaption())
    }

    @Test
    fun `dots without a following space are not sentence ends and short lines stay whole`() {
        assertEquals(listOf("남은 거리는 3.2 km, 도착은 12:30 쯤이에요."), splitForSpeech("남은 거리는 3.2 km, 도착은 12:30 쯤이에요."))
        assertEquals(listOf("다음은 아버지가 준비한 현대백화점 판교점."), splitForSpeech("  다음은 아버지가 준비한 현대백화점 판교점.  "))
        assertEquals(emptyList<String>(), splitForSpeech("  "))
    }

    @Test
    fun `a sentence longer than the limit is never cut in the middle`() {
        val long = "가".repeat(80) + "."
        assertEquals(listOf(long, "끝."), splitForSpeech("$long 끝."))
    }
}
