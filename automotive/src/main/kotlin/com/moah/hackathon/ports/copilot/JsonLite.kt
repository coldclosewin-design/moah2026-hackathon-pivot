package com.moah.hackathon.ports.copilot

/**
 * 아주 작은 JSON 도우미 — 의존성 없이(사내 Gradle 이 외부 저장소에 못 닿을 수 있다) 평평한 응답에서 문자열·숫자 하나를 꺼내고,
 * 요청 본문용 문자열을 이스케이프한다. 전체 파서가 아니다: GitHub 인증 응답과 `choices[0].message.content` 만 다룬다.
 */
object JsonLite {
    private val ESCAPES = mapOf('"' to "\\\"", '\\' to "\\\\", '\n' to "\\n", '\r' to "\\r", '\t' to "\\t")

    fun escape(s: String): String = buildString(s.length + 8) {
        for (c in s) append(ESCAPES[c] ?: if (c < ' ') String.format("\\u%04x", c.code) else c)
    }

    fun quote(s: String): String = "\"${escape(s)}\""

    /** `"key": "value"` — 처음 나오는 것. 없거나 문자열이 아니면 null. 이스케이프(\n, \", \\, \uXXXX)를 푼다. */
    fun string(json: String, key: String): String? {
        val start = valueStart(json, key) ?: return null
        if (json[start] != '"') return null
        return readString(json, start).first
    }

    /** `"key": 123` — 정수·실수 문자열. 없거나 숫자가 아니면 null. */
    fun number(json: String, key: String): Double? {
        val start = valueStart(json, key) ?: return null
        val end = (start until json.length).firstOrNull { json[it] !in "-+.0123456789eE" } ?: json.length
        return json.substring(start, end).toDoubleOrNull()
    }

    fun int(json: String, key: String): Int? = number(json, key)?.toInt()

    /** OpenAI 호환 응답의 `choices[0].message.content`. `"content"` 키가 여럿이면 `"message"` 뒤 첫 것. */
    fun firstChoiceContent(json: String): String? {
        val msg = json.indexOf("\"message\"")
        val from = if (msg >= 0) msg else 0
        val key = json.indexOf("\"content\"", from)
        if (key < 0) return null
        val colon = json.indexOf(':', key + 9)
        if (colon < 0) return null
        var i = colon + 1
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != '"') return null
        return readString(json, i).first
    }

    private fun valueStart(json: String, key: String): Int? {
        val k = json.indexOf("\"$key\"")
        if (k < 0) return null
        val colon = json.indexOf(':', k + key.length + 2)
        if (colon < 0) return null
        var i = colon + 1
        while (i < json.length && json[i].isWhitespace()) i++
        return if (i < json.length) i else null
    }

    /** [start] 는 여는 따옴표. (값, 닫는 따옴표 다음 인덱스) */
    private fun readString(json: String, start: Int): Pair<String, Int> {
        val out = StringBuilder()
        var i = start + 1
        while (i < json.length) {
            val c = json[i]
            when {
                c == '"' -> return out.toString() to i + 1
                c == '\\' && i + 1 < json.length -> {
                    when (val e = json[i + 1]) {
                        'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t')
                        'b' -> out.append('\b'); 'f' -> out.append('\u000C')
                        'u' -> if (i + 5 < json.length) { out.append(json.substring(i + 2, i + 6).toInt(16).toChar()); i += 4 }
                        else -> out.append(e)
                    }
                    i += 2
                }
                else -> { out.append(c); i++ }
            }
        }
        return out.toString() to i
    }
}
