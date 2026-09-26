package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.SeedCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RemarkPoolTest {

    @Test
    fun `same band three times in a row never repeats a line`() {
        val pool = RemarkPool(SeedCatalog.remarks, Random(7))
        val picks = (1..3).map { pool.pick(ScoreBand.GOOD, setOf("any"), emptyMap()) }
        assertEquals(3, picks.toSet().size)
    }

    @Test
    fun `situation tags are preferred over generic lines`() {
        val pool = RemarkPool(SeedCatalog.remarks, Random(1))
        val text = pool.pick(ScoreBand.GOOD, setOf("rusty", "first"), mapOf("years" to "10"))
        assertEquals("장롱의 문 정도는 열었습니다. 좋은 출발이에요.", text)
    }

    @Test
    fun `placeholders are filled`() {
        val pool = RemarkPool(listOf(RemarkTemplate(ScoreBand.OK, setOf("any"), "{segments}번, {years}년차 {name}")), Random(1))
        assertEquals("4번, 10년차 연수생", pool.pick(ScoreBand.OK, emptySet(), mapOf("segments" to "4", "years" to "10", "name" to "연수생")))
    }

    @Test
    fun `score bands`() {
        assertEquals(ScoreBand.EXCELLENT, ScoreBand.of(100))
        assertEquals(ScoreBand.EXCELLENT, ScoreBand.of(90))
        assertEquals(ScoreBand.GOOD, ScoreBand.of(89))
        assertEquals(ScoreBand.OK, ScoreBand.of(60))
        assertEquals(ScoreBand.ROUGH, ScoreBand.of(49))
    }

    @Test
    fun `seed pool has enough lines per band to avoid repeats`() {
        ScoreBand.entries.forEach { band ->
            val n = SeedCatalog.remarks.count { it.band == band }
            assertTrue("$band has $n", n >= 4)
        }
        assertFalse(SeedCatalog.remarks.any { it.text.contains("하위") })
    }
}
