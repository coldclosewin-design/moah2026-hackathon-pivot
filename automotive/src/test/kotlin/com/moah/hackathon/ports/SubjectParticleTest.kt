package com.moah.hackathon.ports

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectParticleTest {
    @Test
    fun `final consonant takes 이 and open syllable takes 가`() {
        assertEquals("지훈이", "지훈".withSubjectParticle())
        assertEquals("엄마가", "엄마".withSubjectParticle())
        assertEquals("아버지가", "아버지".withSubjectParticle())
        assertEquals("여울이가", "여울이".withSubjectParticle())
        assertEquals("오늘의 드라이브가", "오늘의 드라이브".withSubjectParticle())
        assertEquals("민영이", "민영".withSubjectParticle())
    }

    @Test
    fun `object topic and conjunction particles follow the final consonant`() {
        assertEquals("뒤 거리를", "뒤 거리".withObjectParticle())
        assertEquals("기어 전환을", "기어 전환".withObjectParticle())
        assertEquals("후면 직각 주차는", "후면 직각 주차".withTopicParticle())
        assertEquals("출발 전 점검은", "출발 전 점검".withTopicParticle())
        assertEquals("핸들 방향과", "핸들 방향".withAndParticle())
        assertEquals("뒤 거리와", "뒤 거리".withAndParticle())
        assertEquals("핸들 방향과 기어 전환과 뒤 거리를", listOf("핸들 방향", "기어 전환", "뒤 거리").joinAsObjects())
        assertEquals("안전벨트와 기어 P와 시동을", listOf("안전벨트", "기어 P", "시동").joinAsObjects())
        assertEquals("속도를", listOf("속도").joinAsObjects())
        assertEquals("", emptyList<String>().joinAsObjects())
    }

    @Test
    fun `non hangul endings fall back to 가`() {
        assertEquals("Kim가", "Kim".withSubjectParticle())
        assertEquals("", "".withSubjectParticle())
    }
}
