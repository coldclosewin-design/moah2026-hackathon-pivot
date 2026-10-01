package com.moah.hackathon.ports.copilot

import android.content.Context
import android.content.SharedPreferences

/**
 * OAuth 토큰을 device-protected 저장소에 둔다 — 사용자 잠금 해제 전에도 읽히고, `adb install -r`(재설치) 뒤에도 남는다(사내 확인 항목).
 * 앱 삭제(`uninstall`)에는 같이 지워진다.
 */
class PrefsTokenStore(context: Context) : TokenStore {
    private val prefs: SharedPreferences =
        context.createDeviceProtectedStorageContext().getSharedPreferences("copilot_auth", Context.MODE_PRIVATE)

    override fun oauthToken(): String? = prefs.getString(KEY_OAUTH, null)?.takeIf { it.isNotBlank() }

    override fun saveOauthToken(token: String?) {
        prefs.edit().apply { if (token == null) remove(KEY_OAUTH) else putString(KEY_OAUTH, token) }.apply()
    }

    private companion object {
        const val KEY_OAUTH = "oauth_token"
    }
}
