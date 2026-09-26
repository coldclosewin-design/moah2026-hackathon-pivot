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
 * @param isLive 값이 온 키가 실신호인가. 순수 Fake 면 항상 false, 순수 Real 이면 항상 true,
 *   [HybridVehiclePort] 면 키별([HybridVehiclePort.isLive]) — 그래서 배지가 "실신호 2 · 시뮬레이션 6" 처럼 섞인다.
 */
class SignalRegistry(
    private val keys: Set<String>,
    private val isLive: (String) -> Boolean,
) {
    /** 하위 호환: 포트 전체가 Fake(true)거나 Real(false). */
    constructor(keys: Set<String>, simulated: Boolean) : this(keys, { !simulated })

    private val seen = LinkedHashSet<String>()

    /** 포트에서 delta 가 올 때마다 호출. 관심 없는 키는 무시. */
    fun onValues(values: Map<String, String>) {
        for (k in values.keys) if (k in keys) seen.add(k)
    }

    fun availability(key: String): SignalAvailability = when {
        key !in keys -> SignalAvailability.MISSING
        key !in seen -> SignalAvailability.MISSING
        isLive(key) -> SignalAvailability.LIVE
        else -> SignalAvailability.SIMULATED
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

    companion object {
        /** 포트 종류를 보고 알맞은 레지스트리를 만든다. 조립 지점(App.kt)에서 쓴다. */
        fun forPort(keys: Set<String>, port: VehiclePort): SignalRegistry = when (port) {
            is HybridVehiclePort -> SignalRegistry(keys) { port.isLive(it) }
            is FakeVehiclePort -> SignalRegistry(keys, simulated = true)
            else -> SignalRegistry(keys, simulated = false)
        }
    }
}
