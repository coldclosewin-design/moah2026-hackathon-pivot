package com.moah.hackathon.ports

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.IntentRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 코치 문장 = "제목. 한 마디."(라운드 26 시안 5-B, 10/7) — 규칙 문장이 길이 규칙을 지키고, AI 응답은 같은 규칙으로 걸러진다. */
class ShortCoachLinesTest {

    @Test
    fun `every seed opener fits the title and every advice fits the line`() {
        SeedCatalog.remarks.forEach { assertTrue("title too long: ${it.text}", it.text.length <= CoachPrompts.TITLE_MAX) }
        AdviceRules.Advice.entries.forEach { assertTrue("line too long: ${it.driver}", it.driver.length <= CoachPrompts.LINE_MAX) }
    }

    @Test
    fun `dialog rule lines are a title and a line`() {
        listOf(IntentRules.ASK_LINE, IntentRules.WORRY_LINE, IntentRules.NEAR_MISS_LINE, IntentRules.KID_LINE, IntentRules.MOVING_LINE,
            IntentRules.TEACH_LINE, IntentRules.FIRST_LINE, IntentRules.START_LINE, IntentRules.SHAKY_LINE, IntentRules.SHORT_LINE).forEach {
            assertEquals(it, CoachPrompts.titleAndLine(it, CoachPrompts.DIALOG_LINE_MAX, allowOne = true))
        }
    }

    @Test
    fun `ai text is cut into a title and a line or rejected`() {
        assertEquals("감각이 돌아왔어요.\n다음엔 후진할 때 속도를 고르게 해 봐요.",
            CoachPrompts.titleAndLine("감각이 돌아왔어요. 다음엔 후진할 때 속도를 고르게 해 봐요.", CoachPrompts.LINE_MAX))
        assertNull("one sentence is not a remark", CoachPrompts.titleAndLine("감각이 돌아왔어요.", CoachPrompts.LINE_MAX))
        assertNull("three sentences", CoachPrompts.titleAndLine("좋아요. 잘했어요. 또 해요.", CoachPrompts.LINE_MAX))
        assertNull("long title", CoachPrompts.titleAndLine("오늘 첫 주행치고 훌륭했어요, 감각이 꽤 살아나고 있네요. 다음엔 천천히요.", CoachPrompts.LINE_MAX))
        assertNull("long line", CoachPrompts.titleAndLine("좋았어요. 다음엔 후진 중 갑작스러운 속도 변화에 조금 더 신경 써 보세요.", CoachPrompts.LINE_MAX))
        assertEquals("한 문장 대화도 돼요.", CoachPrompts.titleAndLine("한 문장 대화도 돼요.", CoachPrompts.DIALOG_LINE_MAX, allowOne = true))
    }
}
