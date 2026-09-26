package com.moah.hackathon.vehicle

/**
 * 신호 하나가 이번 세션에서 어디서 왔는가. 리포트 배지 "실신호 N · 시뮬레이션 N · 미측정 N" 의 근거.
 *
 * - [LIVE]      실제 VSS 에서 값이 한 번이라도 왔다.
 * - [SIMULATED] Fake(시나리오)가 만든 값이다. 채점에는 쓰되 화면·리포트에 "시뮬" 표시.
 * - [MISSING]   구독했지만 값이 온 적이 없다 — 경로가 실물과 다르거나 그 차에 없는 신호. **채점에서 뺀다.**
 *
 * VSS 는 경로 오타를 예외 없이 무시하므로(docs/02_vss_api_contract.md 함정 6) "값이 안 온다"가 유일한 단서다.
 */
enum class SignalAvailability { LIVE, SIMULATED, MISSING }

data class AvailabilityBadge(val live: Int, val simulated: Int, val missing: Int) {
    val total: Int get() = live + simulated + missing
}

/**
 * 세션 동안 어떤 키에 값이 왔는지 기록한다. 코루틴·안드로이드 의존 없음.
 *
 * @param keys 이 세션이 관심 있는 키 전부(배지의 분모).
 * @param simulated 포트가 Fake 인가. true 면 값이 온 키는 전부 [SignalAvailability.SIMULATED].
 */
class SignalRegistry(
    private val keys: Set<String>,
    private val simulated: Boolean,
) {
    private val seen = LinkedHashSet<String>()

    /** 포트에서 delta 가 올 때마다 호출. 관심 없는 키는 무시. */
    fun onValues(values: Map<String, String>) {
        for (k in values.keys) if (k in keys) seen.add(k)
    }

    fun availability(key: String): SignalAvailability = when {
        key !in keys -> SignalAvailability.MISSING
        key !in seen -> SignalAvailability.MISSING
        simulated -> SignalAvailability.SIMULATED
        else -> SignalAvailability.LIVE
    }

    fun isAvailable(key: String): Boolean = availability(key) != SignalAvailability.MISSING

    fun snapshot(): Map<String, SignalAvailability> = keys.associateWith { availability(it) }

    fun badge(): AvailabilityBadge {
        val s = snapshot().values
        return AvailabilityBadge(
            live = s.count { it == SignalAvailability.LIVE },
            simulated = s.count { it == SignalAvailability.SIMULATED },
            missing = s.count { it == SignalAvailability.MISSING },
        )
    }

    fun missingKeys(): List<String> = keys.filter { it !in seen }
}
