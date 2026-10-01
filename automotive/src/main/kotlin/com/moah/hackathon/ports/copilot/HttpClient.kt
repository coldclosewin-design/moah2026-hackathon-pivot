package com.moah.hackathon.ports.copilot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class HttpResponse(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299
}

/** 테스트에서 대본으로 바꿔 끼우는 HTTP 한 줄. 실패는 예외(IOException 포함 — 호출자가 잡는다). */
interface HttpClient {
    suspend fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse
}

/**
 * `HttpURLConnection` 구현. 연결·읽기 타임아웃 3.5 s(코치 전체 예산 5 s 안에 한 번 재시도할 여유) — [runInterruptible] 로 코루틴 취소가
 * 블로킹 I/O 를 끊는다. 의존성 없음(사내 Gradle 저장소 사정).
 */
class UrlHttpClient(
    private val connectTimeoutMs: Int = 3_500,
    private val readTimeoutMs: Int = 3_500,
) : HttpClient {
    override suspend fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse =
        runInterruptible(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) { doOutput = true }
            }
            try {
                if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = try { stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "" } catch (e: IOException) { "" }
                HttpResponse(code, text)
            } finally {
                conn.disconnect()
            }
        }
}
