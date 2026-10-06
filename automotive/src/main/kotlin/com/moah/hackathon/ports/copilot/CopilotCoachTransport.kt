package com.moah.hackathon.ports.copilot

import android.util.Log
import com.moah.hackathon.ports.ChatMessage
import com.moah.hackathon.ports.CoachTransport
import java.io.IOException

/**
 * [CoachTransport] 의 Copilot 구현 — `POST {endpoint}/chat/completions`(Bearer 세션 토큰, `Copilot-Integration-Id: vscode-chat`,
 * `Openai-Intent: conversation-panel`, 에디터 헤더는 [CopilotAuth.EDITOR_HEADERS] — 10/1 사내에서 통과한 값),
 * max_tokens 200, temperature 0.7 → `choices[0].message.content`. 홈 코치 대화(10/6)는 [chat] 으로 이력을 messages 배열 그대로 보낸다. 401 이면 세션을 무효화하고 **한 번** 재시도.
 * 실패는 예외로 — [com.moah.hackathon.ports.CloudCoachPort] 가 잡아 시드 문장으로 폴백한다.
 */
class CopilotCoachTransport(
    private val auth: CopilotAuth,
    private val config: CopilotConfig,
    private val http: HttpClient,
) : CoachTransport {

    override suspend fun complete(system: String, user: String): String = chat(system, listOf(ChatMessage("user", user)))

    override suspend fun chat(system: String, messages: List<ChatMessage>): String {
        val token = auth.sessionToken() ?: throw IOException("copilot: no session (needs login)")
        val res = post(token, system, messages)
        val body = if (res.code == 401) {
            Log.w(TAG, "401 → session invalidated, retry once")
            auth.invalidateSession()
            val again = auth.sessionToken() ?: throw IOException("copilot: no session after 401")
            post(again, system, messages)
        } else res
        if (!body.ok) throw IOException("copilot: HTTP ${body.code} ${body.body.take(80)}")
        return JsonLite.firstChoiceContent(body.body) ?: throw IOException("copilot: no content in response")
    }

    private suspend fun post(token: String, system: String, messages: List<ChatMessage>): HttpResponse {
        val lines = (listOf(ChatMessage("system", system)) + messages)
            .joinToString(",") { """{"role":${JsonLite.quote(it.role)},"content":${JsonLite.quote(it.content)}}""" }
        val payload = """{"model":${JsonLite.quote(config.model)},"messages":[$lines],"max_tokens":200,"temperature":0.7}"""
        return http.request("POST", "${config.endpoint}/chat/completions", mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json",
            "Copilot-Integration-Id" to "vscode-chat",
            "Openai-Intent" to "conversation-panel",
        ) + CopilotAuth.EDITOR_HEADERS, payload)
    }

    private companion object {
        const val TAG = "MOAH/CopilotCoachTransport"
    }
}
