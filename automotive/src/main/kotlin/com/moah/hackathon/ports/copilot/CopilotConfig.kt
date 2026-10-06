package com.moah.hackathon.ports.copilot

import java.io.File

/**
 * 기기의 `/data/local/tmp/copilot_config.json` — `client_id`(필수), `endpoint`(기본 https://api.githubcopilot.com), `model`(기본 gpt-4o),
 * `persona`(선택, 10/6 — 코치 페르소나 문장. 사내에서 `adb push` 만으로 바꾼다. 형식 규칙은 코드에 남아 덮어쓰지 못한다).
 * 파일이 없으면 AI 코치를 끄고(시드 문장) 패널에 "설정 없음" 으로 보인다. 사내 세션 시작 때 `adb push` 로 넣는다(`docs/07_two_site_workflow.md`).
 * 저장소에는 넣지 않는다(client_id 는 GitHub OAuth App 의 것).
 */
data class CopilotConfig(
    val clientId: String,
    val endpoint: String = DEFAULT_ENDPOINT,
    val model: String = DEFAULT_MODEL,
    val persona: String? = null,
) {
    companion object {
        const val DEFAULT_PATH = "/data/local/tmp/copilot_config.json"
        const val DEFAULT_ENDPOINT = "https://api.githubcopilot.com"
        const val DEFAULT_MODEL = "gpt-4o"

        fun parse(json: String): CopilotConfig? {
            val clientId = JsonLite.string(json, "client_id")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return CopilotConfig(
                clientId = clientId,
                endpoint = JsonLite.string(json, "endpoint")?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: DEFAULT_ENDPOINT,
                model = JsonLite.string(json, "model")?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_MODEL,
                persona = JsonLite.string(json, "persona")?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        fun load(path: String = DEFAULT_PATH): CopilotConfig? =
            runCatching { File(path).takeIf { it.isFile }?.readText(Charsets.UTF_8)?.let(::parse) }.getOrNull()
    }
}
