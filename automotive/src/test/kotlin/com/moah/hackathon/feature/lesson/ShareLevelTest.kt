package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.SeedCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 진단서 B(라운드 22 결정, 10/5): 범위마다 혜택과 조건이 짝이고, 범위가 넓을수록 포함 항목이 늘어난다. */
class ShareLevelTest {

    @Test
    fun everyLevelHasBenefitAndCondition() {
        ShareLevel.entries.forEach { level ->
            assertTrue("${level.name} benefit", level.benefit.isNotBlank())
            assertTrue("${level.name} condition", level.condition.isNotBlank())
            assertTrue("${level.name} description", level.description.isNotBlank())
        }
        assertEquals("benefits are distinct", ShareLevel.entries.size, ShareLevel.entries.map { it.benefit }.toSet().size)
    }

    @Test
    fun widerLevelIncludesEverythingOfNarrowerOne() {
        ShareLevel.entries.zipWithNext().forEach { (narrow, wide) ->
            assertTrue("${wide.name} ⊇ ${narrow.name}", wide.includes.containsAll(narrow.includes))
            assertTrue("${wide.name} adds something", wide.includes.size > narrow.includes.size)
        }
    }

    @Test
    fun excludedItemsAreNeverIncluded() {
        ShareLevel.entries.forEach { level ->
            ShareLevel.EXCLUDED.forEach { assertFalse("${level.name} includes $it", it in level.includes) }
        }
    }

    @Test
    fun benefitAndConditionHaveNoDigits() {
        // 진단서는 숫자 허용 화면이지만 혜택·조건 문장은 "실제로 받을 수 있는 상황" 을 말로 — 기간·횟수를 지어내지 않는다
        ShareLevel.entries.forEach { level ->
            assertFalse(level.benefit, level.benefit.any { it.isDigit() })
            assertFalse(level.condition, level.condition.any { it.isDigit() })
        }
    }

    @Test
    fun seedBenefitsFollowLevelOrder() {
        // 라운드 23 ① 전까지 지금 화면이 범위와 같은 줄 순서로 혜택을 그린다
        assertEquals(ShareLevel.entries.map { it.benefit }, SeedCatalog.benefits)
    }
}
