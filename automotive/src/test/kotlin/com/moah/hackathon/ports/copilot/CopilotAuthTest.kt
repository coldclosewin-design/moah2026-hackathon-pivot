package com.moah.hackathon.ports.copilot

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** HTTP 대본: URL 접두로 응답을 고른다. `access_token` 폴링은 호출마다 다음 응답. */
private class ScriptedHttp : HttpClient {
    val calls = mutableListOf<Triple<String, String, String?>>()
    var deviceCode = HttpResponse(200, """{"device_code":"dc1","user_code":"ABCD-1234","verification_uri":"https://github.com/login/device","interval":5,"expires_in":900}""")
    val polls = ArrayDeque<HttpResponse>()
    var session: (String?) -> HttpResponse = { HttpResponse(200, """{"token":"sess-1","expires_at":${2_000_000_000L}}""") }
    var chat: (Map<String, String>, String?) -> HttpResponse = { _, _ -> HttpResponse(200, """{"choices":[{"message":{"role":"assistant","content":"좋았어요! 다음엔 벨트부터요."}}]}""") }
    override suspend fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse {
        calls += Triple(method, url, body)
        return when {
            url == CopilotAuth.DEVICE_CODE_URL -> deviceCode
            url == CopilotAuth.ACCESS_TOKEN_URL -> polls.removeFirstOrNull() ?: HttpResponse(200, """{"error":"authorization_pending"}""")
            url == CopilotAuth.SESSION_TOKEN_URL -> session(headers["Authorization"])
            url.endsWith("/chat/completions") -> chat(headers, body)
            else -> HttpResponse(404, "")
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CopilotAuthTest {
    private val config = CopilotConfig(clientId = "cid")
    private val sleeps = mutableListOf<Long>()

    private fun TestScope.auth(store: TokenStore, http: HttpClient, cfg: CopilotConfig? = config, now: () -> Long = { 1_000_000L }) =
        CopilotAuth(cfg, store, http, this, clock = now, sleep = { sleeps += it })

    @Test
    fun `no config file means NoConfig and nothing is called`() = runTest {
        val http = ScriptedHttp()
        val a = auth(MemoryTokenStore(), http, cfg = null)
        assertEquals(CopilotAuth.State.NoConfig, a.state.value)
        a.connect(); a.prewarm(); advanceUntilIdle()
        assertNull(a.sessionToken())
        assertTrue(http.calls.isEmpty())
    }

    @Test
    fun `device code flow - pending then slow_down then token - ends Ready with the oauth stored and a session fetched`() = runTest(StandardTestDispatcher()) {
        val http = ScriptedHttp().apply {
            polls += HttpResponse(200, """{"error":"authorization_pending"}""")
            polls += HttpResponse(200, """{"error":"slow_down"}""")
            polls += HttpResponse(200, """{"access_token":"gho_abc","token_type":"bearer","scope":"copilot"}""")
        }
        val store = MemoryTokenStore()
        val a = auth(store, http)
        assertEquals(CopilotAuth.State.NeedsLogin, a.state.value)
        a.connect(); advanceUntilIdle()
        assertEquals(CopilotAuth.State.Ready, a.state.value)
        assertEquals("gho_abc", store.oauthToken())
        assertEquals(listOf(5_000L, 5_000L, 10_000L), sleeps)          // slow_down 뒤 간격 +5 s
        assertTrue(http.calls[0].third!!.contains("client_id=cid") && http.calls[0].third!!.contains("scope=copilot"))
        assertTrue(http.calls[1].third!!.contains("grant_type=urn:ietf:params:oauth:grant-type:device_code"))
        assertEquals("sess-1", a.sessionToken())
    }

    @Test
    fun `expired device code goes back to NeedsLogin and other errors surface as Error`() = runTest(StandardTestDispatcher()) {
        val http = ScriptedHttp().apply { polls += HttpResponse(200, """{"error":"expired_token"}""") }
        val a = auth(MemoryTokenStore(), http)
        a.connect(); advanceUntilIdle()
        assertEquals(CopilotAuth.State.NeedsLogin, a.state.value)

        val denied = ScriptedHttp().apply { polls += HttpResponse(200, """{"error":"access_denied","error_description":"The user has denied"}""") }
        val b = auth(MemoryTokenStore(), denied)
        b.connect(); advanceUntilIdle()
        assertTrue(b.state.value is CopilotAuth.State.Error)
        assertTrue((b.state.value as CopilotAuth.State.Error).message.contains("access_denied"))
    }

    @Test
    fun `the Code state carries what the user must type and where`() = runTest(StandardTestDispatcher()) {
        val http = ScriptedHttp()                                   // polls 비어 있음 → 계속 pending
        val a = auth(MemoryTokenStore(), http, now = object : () -> Long { var t = 0L; override fun invoke(): Long { t += 100_000L; return t } })
        a.connect(); advanceUntilIdle()
        // 만료(900 s)까지 pending 만 → NeedsLogin 으로 끝난다. 그 전에 Code 상태를 거쳤는지는 sleeps 로 안다
        assertTrue(sleeps.isNotEmpty())
        assertEquals(CopilotAuth.State.NeedsLogin, a.state.value)
    }

    @Test
    fun `session token is cached until 60 s before expiry and a 401 clears the oauth`() = runTest {
        var now = 1_000_000_000_000L
        val http = ScriptedHttp().apply { session = { HttpResponse(200, """{"token":"s1","expires_at":${now / 1000 + 120}}""") } }
        val store = MemoryTokenStore("gho_saved")
        val a = auth(store, http, now = { now })
        assertEquals(CopilotAuth.State.Ready, a.state.value)     // 저장된 OAuth 가 있으면 Ready 로 시작
        assertEquals("s1", a.sessionToken()); assertEquals("s1", a.sessionToken())
        assertEquals(1, http.calls.count { it.second == CopilotAuth.SESSION_TOKEN_URL })
        now += 61_000L                                           // 만료 60 s 전 → 갱신
        assertEquals("s1", a.sessionToken())
        assertEquals(2, http.calls.count { it.second == CopilotAuth.SESSION_TOKEN_URL })
        http.session = { HttpResponse(401, """{"message":"Bad credentials"}""") }
        a.invalidateSession()
        assertNull(a.sessionToken())
        assertNull(store.oauthToken())
        assertEquals(CopilotAuth.State.NeedsLogin, a.state.value)
    }

    @Test
    fun `transport posts system and user, reads the first choice, and retries once after a 401`() = runTest {
        val http = ScriptedHttp()
        val a = auth(MemoryTokenStore("gho"), http)
        val t = CopilotCoachTransport(a, config, http)
        assertEquals("좋았어요! 다음엔 벨트부터요.", t.complete("SYS \"q\"", "USER\nline"))
        val body = http.calls.last { it.second.endsWith("/chat/completions") }.third!!
        assertTrue(body, body.contains("\"model\":\"gpt-4o\"") && body.contains("SYS \\\"q\\\"") && body.contains("USER\\nline") && body.contains("\"max_tokens\":200"))
        val headers = http.calls.size
        var first = true
        http.chat = { h, _ -> if (first) { first = false; HttpResponse(401, "expired") } else { assertEquals("Bearer sess-1", h["Authorization"]); HttpResponse(200, """{"choices":[{"message":{"content":"두 번째."}}]}""") } }
        assertEquals("두 번째.", t.complete("s", "u"))
        assertEquals(2, http.calls.drop(headers).count { it.second.endsWith("/chat/completions") })
        http.chat = { _, _ -> HttpResponse(500, "boom") }
        try { t.complete("s", "u"); assertTrue(false) } catch (e: IOException) { assertTrue(e.message!!.contains("500")) }
    }

    @Test
    fun `sessionToken without oauth keeps the Code state while a login is in progress`() = runTest(StandardTestDispatcher()) {
        // 10/1 사내 검증 #2: 코드 표시 중에 회차가 끝나 총평이 호출되면 NeedsLogin 으로 덮여 패널의 코드가 사라졌다
        val gate = CompletableDeferred<Unit>()
        val http = ScriptedHttp()
        val store = MemoryTokenStore()
        val a = CopilotAuth(config, store, http, this, clock = { 1_000_000L }, sleep = { gate.await() })
        a.connect(); runCurrent()
        assertEquals(CopilotAuth.State.Code("ABCD-1234", "https://github.com/login/device"), a.state.value)
        assertNull(a.sessionToken())
        assertEquals(CopilotAuth.State.Code("ABCD-1234", "https://github.com/login/device"), a.state.value)   // 그대로
        http.polls += HttpResponse(200, """{"access_token":"gho_x","token_type":"bearer","scope":"copilot"}""")
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(CopilotAuth.State.Ready, a.state.value)
    }

    @Test
    fun `a transient session token failure returns null but keeps the state and the oauth`() = runTest {
        // 10/1 사내 검증 #2: 5xx 에 Error 로 가면 패널에 AI 연결이 떠서 멀쩡한 OAuth 로 기기 인증을 다시 시작하게 된다
        val http = ScriptedHttp().apply { session = { HttpResponse(503, "unavailable") } }
        val store = MemoryTokenStore("gho_saved")
        val a = auth(store, http)
        assertNull(a.sessionToken())
        assertEquals(CopilotAuth.State.Ready, a.state.value)
        assertEquals("gho_saved", store.oauthToken())
        http.session = { HttpResponse(200, """{"token":"s2","expires_at":${2_000_000_000L}}""") }
        assertEquals("s2", a.sessionToken())                       // 회복되면 그대로 이어진다
        http.session = { HttpResponse(200, """not json""") }
        a.invalidateSession()
        assertNull(a.sessionToken()); assertEquals(CopilotAuth.State.Ready, a.state.value)
    }

    @Test
    fun `session and chat requests carry the editor headers that passed in-house and the chat intent`() = runTest {
        val seen = mutableMapOf<String, Map<String, String>>()
        val http = object : HttpClient {
            val inner = ScriptedHttp()
            override suspend fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse {
                seen[url] = headers; return inner.request(method, url, headers, body)
            }
        }
        val a = auth(MemoryTokenStore("gho"), http)
        assertEquals("좋았어요! 다음엔 벨트부터요.", CopilotCoachTransport(a, config, http).complete("s", "u"))
        val session = seen.getValue(CopilotAuth.SESSION_TOKEN_URL)
        assertEquals("vscode/1.95.0", session["Editor-Version"])
        assertEquals("copilot-chat/0.25.2025010801", session["Editor-Plugin-Version"])
        val chat = seen.entries.single { it.key.endsWith("/chat/completions") }.value
        assertEquals("vscode/1.95.0", chat["Editor-Version"])
        assertEquals("conversation-panel", chat["Openai-Intent"])
        assertEquals("vscode-chat", chat["Copilot-Integration-Id"])
    }

    @Test
    fun `json lite reads strings numbers escapes and the first choice content`() {
        val j = """{"a":"x\ny \"q\" é","n":12.5,"choices":[{"index":0,"message":{"role":"assistant","content":"안녕.\n둘째."},"finish_reason":"stop"}]}"""
        assertEquals("x\ny \"q\" é", JsonLite.string(j, "a"))
        assertEquals(12.5, JsonLite.number(j, "n")!!, 0.0)
        assertEquals(12, JsonLite.int(j, "n"))
        assertEquals("안녕.\n둘째.", JsonLite.firstChoiceContent(j))
        assertNull(JsonLite.string(j, "missing"))
        assertEquals("\"a\\\"b\\\\c\\n\"", JsonLite.quote("a\"b\\c\n"))
        assertEquals(CopilotConfig("cid", "https://x.y", "m"), CopilotConfig.parse("""{"client_id":"cid","endpoint":"https://x.y/","model":"m"}"""))
        assertEquals(CopilotConfig("cid"), CopilotConfig.parse("""{"client_id":" cid "}"""))
        assertNull(CopilotConfig.parse("""{"endpoint":"x"}"""))
    }
}
