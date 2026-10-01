package com.moah.hackathon.ui

import com.moah.hackathon.ports.copilot.CopilotAuth.State
import com.moah.hackathon.ui.lesson.AiLine
import com.moah.hackathon.ui.lesson.aiLine
import org.junit.Assert.*
import org.junit.Test

class AiLineTest {
    @Test fun disabledAiHasNoLine() { assertNull(aiLine(null)) }

    @Test fun missingConfigExplainsTheSeedFallbackWithoutConnect() {
        assertEquals(AiLine("AI 코치 · 설정 없음", "copilot_config.json 없음 — 시드 문장으로 말해요", false),
            aiLine(State.NoConfig))
    }

    @Test fun loginRequiresGithubCodeAndAuthorizeWithConnect() {
        assertEquals(AiLine("AI 코치 · 로그인 필요", "GitHub 에서 코드를 넣고 Authorize 까지 눌러 주세요", true),
            aiLine(State.NeedsLogin))
    }

    @Test fun deviceCodePreservesTheExactUriAndCodeWithoutConnect() {
        val uri = "https://github.com/login/device?test=AbC"
        assertEquals(AiLine("AI 코치 · 코드 입력 중", "$uri  aB12-CD34", false),
            aiLine(State.Code("aB12-CD34", uri)))
    }

    @Test fun readyExplainsCopilotRemarksWithoutConnect() {
        assertEquals(AiLine("AI 코치 · 연결됨", "회차 멘트·총평을 Copilot 이 써요", false), aiLine(State.Ready))
    }

    @Test fun errorPreservesTheMessageAndAllowsReconnect() {
        val message = "device code HTTP 503 · 다시 연결해 주세요"
        assertEquals(AiLine("AI 코치 · 오류", message, true), aiLine(State.Error(message)))
    }

    @Test fun errorIsTruncatedOnlyAfterEightyCharactersWithoutAnEllipsis() {
        val prefix = "오류 내용 그대로 ".repeat(8)
        assertEquals(80, prefix.length)
        listOf("", prefix, prefix + "제외할 내용").forEach { message ->
            assertEquals(AiLine("AI 코치 · 오류", message.take(80), true), aiLine(State.Error(message)))
        }
    }
}
