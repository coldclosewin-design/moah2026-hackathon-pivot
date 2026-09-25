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
    fun `non hangul endings fall back to 가`() {
        assertEquals("Kim가", "Kim".withSubjectParticle())
        assertEquals("", "".withSubjectParticle())
    }
}
