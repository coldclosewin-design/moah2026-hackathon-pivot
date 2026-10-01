package com.moah.hackathon.ports.copilot

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** OAuth 토큰 보관 — 사내 앱은 device-protected SharedPreferences(재설치 `install -r` 뒤에도 남는다), 테스트는 메모리. */
interface TokenStore {
    fun oauthToken(): String?
    fun saveOauthToken(token: String?)
}

class MemoryTokenStore(private var token: String? = null) : TokenStore {
    override fun oauthToken() = token
    override fun saveOauthToken(token: String?) { this.token = token }
}

/**
 * GitHub Copilot 인증 — 공개된 GitHub 방식(device code → OAuth 토큰 → Copilot 세션 토큰). 템플릿 코드를 옮기지 않고 새로 썼다.
 *
 * 상태([state]): [State.NoConfig](설정 파일 없음) / [State.NeedsLogin] / [State.Code](사용자가 `uri` 에서 `userCode` 를 넣고 Authorize 까지 눌러야 한다,
 * 15분 만료) / [State.Ready] / [State.Error](사용자 조치가 필요한 것만 — 일시 오류는 상태를 바꾸지 않고 폴백만 한다).
 *
 * - [connect]: `POST https://github.com/login/device/code {client_id, scope}` → Code 상태 → `interval` 마다
 *   `POST https://github.com/login/oauth/access_token {client_id, device_code, grant_type=…:device_code}`.
 *   `authorization_pending` 계속, `slow_down` 은 간격 +5 s, `expired_token` 은 종료(NeedsLogin), 그 밖의 error 는 즉시 Error. poll error 가 바뀔 때마다 로그.
 * - [sessionToken]: `GET https://api.github.com/copilot_internal/v2/token`(Authorization: token <oauth>, [EDITOR_HEADERS]) → `token`/`expires_at`
 *   캐시(만료 60 s 전 갱신). 401 이면 OAuth 를 지우고 NeedsLogin. 5xx·형식 오류 같은 **일시 오류는 null 만 돌려주고 상태를 지킨다**
 *   (Error 로 바꾸면 패널에 `AI 연결` 이 떠서 멀쩡한 OAuth 로 기기 인증을 다시 시작하게 된다 — 10/1 사내 검증 #2).
 *   OAuth 가 없을 때도 **Code 상태(로그인 진행 중)는 덮지 않는다** — 회차가 끝나 총평이 호출돼도 패널의 코드가 사라지지 않는다.
 * - 앱 시작 때 [prewarm] — 저장된 OAuth 가 있으면 세션 토큰을 미리 받아 Ready 로. 시작 상태는 로그로 남긴다(사내 `inhouse_check.sh` 의 `-- Copilot:` 줄).
 */
class CopilotAuth(
    val config: CopilotConfig?,
    private val store: TokenStore,
    private val http: HttpClient,
    private val scope: CoroutineScope,
    private val scopeName: String = "copilot",
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    sealed class State {
        object NoConfig : State()
        object NeedsLogin : State()
        data class Code(val userCode: String, val uri: String) : State()
        object Ready : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(if (config == null) State.NoConfig else if (store.oauthToken() == null) State.NeedsLogin else State.Ready)
    val state: StateFlow<State> = _state
    val hasConfig: Boolean get() = config != null

    private val mutex = Mutex()
    @Volatile private var session: String? = null
    @Volatile private var sessionExpiresAtMillis: Long = 0L
    private var connectJob: Job? = null

    init {
        Log.i(TAG, "state=${_state.value.javaClass.simpleName}" + if (config == null) " (copilot_config.json 없음)" else "")
    }

    /** 앱 시작 때: 저장된 OAuth 로 세션 토큰을 미리 받아 둔다. 실패해도 상태만 바꾸고 예외는 삼킨다. */
    fun prewarm() {
        if (config == null || store.oauthToken() == null) return
        scope.launch { runCatching { sessionToken() }.onFailure { Log.w(TAG, "prewarm failed", it) } }
    }

    /** Device code 흐름 시작. 이미 진행 중이면 무시. */
    fun connect() {
        val cfg = config ?: run { _state.value = State.NoConfig; return }
        if (connectJob?.isActive == true) return
        connectJob = scope.launch {
            try {
                val res = http.request("POST", DEVICE_CODE_URL, FORM_HEADERS,
                    "client_id=${cfg.clientId}&scope=$scopeName")
                if (!res.ok) { fail("device code HTTP ${res.code}"); return@launch }
                val deviceCode = JsonLite.string(res.body, "device_code")
                val userCode = JsonLite.string(res.body, "user_code")
                val uri = JsonLite.string(res.body, "verification_uri") ?: "https://github.com/login/device"
                var intervalSec = (JsonLite.int(res.body, "interval") ?: 5).coerceAtLeast(1)
                val expiresIn = JsonLite.int(res.body, "expires_in") ?: 900
                if (deviceCode == null || userCode == null) { fail("device code response malformed"); return@launch }
                _state.value = State.Code(userCode, uri)
                Log.i(TAG, "device code issued: user_code=$userCode uri=$uri interval=${intervalSec}s")
                val deadline = clock() + expiresIn * 1000L
                var lastError: String? = null
                while (clock() < deadline) {
                    sleep(intervalSec * 1000L)
                    val poll = http.request("POST", ACCESS_TOKEN_URL, FORM_HEADERS,
                        "client_id=${cfg.clientId}&device_code=$deviceCode&grant_type=urn:ietf:params:oauth:grant-type:device_code")
                    val token = JsonLite.string(poll.body, "access_token")
                    if (token != null) {
                        store.saveOauthToken(token)
                        session = null
                        _state.value = State.Ready
                        Log.i(TAG, "oauth token stored → state=Ready")
                        runCatching { sessionToken() }.onFailure { Log.w(TAG, "session after login failed", it) }
                        return@launch
                    }
                    val error = JsonLite.string(poll.body, "error") ?: "HTTP ${poll.code}"
                    if (error != lastError) { Log.i(TAG, "poll: $error"); lastError = error }
                    when (error) {
                        "authorization_pending" -> Unit
                        "slow_down" -> intervalSec += 5
                        "expired_token" -> { _state.value = State.NeedsLogin; Log.w(TAG, "device code expired"); return@launch }
                        else -> { fail("login: $error"); return@launch }
                    }
                }
                _state.value = State.NeedsLogin
                Log.w(TAG, "device code timed out")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("connect: ${e.javaClass.simpleName} ${e.message ?: ""}".trim())
            }
        }
    }

    /**
     * Copilot 세션 토큰(만료 60 s 전이면 갱신). OAuth 가 없으면 null(Code 상태가 아니면 NeedsLogin 으로). 401 이면 OAuth 를 지우고 NeedsLogin 으로.
     * 그 밖의 HTTP 오류·형식 오류는 null 만(상태 유지 → 호출자 = 전송 계층 → CloudCoachPort 가 폴백). 네트워크 예외는 그대로 던진다(같은 폴백).
     */
    suspend fun sessionToken(): String? {
        config ?: return null
        val oauth = store.oauthToken() ?: run {
            if (_state.value !is State.Code) _state.value = State.NeedsLogin
            return null
        }
        mutex.withLock {
            session?.takeIf { clock() < sessionExpiresAtMillis - REFRESH_MARGIN_MS }?.let { return it }
            val res = http.request("GET", SESSION_TOKEN_URL, mapOf("Authorization" to "token $oauth") + EDITOR_HEADERS, null)
            if (res.code == 401) {
                store.saveOauthToken(null); session = null
                _state.value = State.NeedsLogin
                Log.w(TAG, "session token 401 → oauth cleared, state=NeedsLogin")
                return null
            }
            if (!res.ok) { Log.w(TAG, "session token HTTP ${res.code} → 폴백(상태 유지 ${_state.value.javaClass.simpleName})"); return null }
            val token = JsonLite.string(res.body, "token") ?: run { Log.w(TAG, "session token malformed → 폴백(상태 유지)"); return null }
            val expiresAt = JsonLite.number(res.body, "expires_at")?.toLong()
            session = token
            sessionExpiresAtMillis = expiresAt?.let { it * 1000L } ?: (clock() + DEFAULT_SESSION_TTL_MS)
            _state.value = State.Ready
            return token
        }
    }

    /** 전송 계층이 401 을 받았을 때: 캐시를 버려 다음 호출이 새 세션을 받게. */
    fun invalidateSession() { session = null; sessionExpiresAtMillis = 0L }

    /** 사용자 조치가 필요한 실패만 — 패널에 `AI 연결` 이 뜬다. */
    private fun fail(message: String) {
        Log.w(TAG, message)
        _state.value = State.Error(message)
    }

    companion object {
        const val TAG = "MOAH/CopilotAuth"
        const val DEVICE_CODE_URL = "https://github.com/login/device/code"
        const val ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token"
        const val SESSION_TOKEN_URL = "https://api.github.com/copilot_internal/v2/token"
        const val REFRESH_MARGIN_MS = 60_000L
        const val DEFAULT_SESSION_TTL_MS = 25 * 60_000L
        val FORM_HEADERS = mapOf("Accept" to "application/json", "Content-Type" to "application/x-www-form-urlencoded")
        /** GitHub 이 Copilot 세션 토큰·chat 을 내주는 에디터 헤더 — 10/1 사내에서 통과한 값(구버전 `vscode/1.85.0`·`copilot-chat/0.12.0` 은 거부 위험). */
        val EDITOR_HEADERS = mapOf(
            "Accept" to "application/json",
            "Editor-Version" to "vscode/1.95.0",
            "Editor-Plugin-Version" to "copilot-chat/0.25.2025010801",
            "User-Agent" to "GitHubCopilotChat/0.25.2025010801",
        )
    }
}
