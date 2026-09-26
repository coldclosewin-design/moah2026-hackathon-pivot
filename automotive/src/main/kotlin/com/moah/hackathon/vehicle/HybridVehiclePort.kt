package com.moah.hackathon.vehicle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

/**
 * 실차 포트에서 **오지 않는 키만** Fake 로 채운다 — "사내에 신호가 없어도 가상 신호로 진행"(2026-09-25 결정)의 구현체.
 *
 * 규칙은 하나: **Real 이 한 번이라도 값을 낸 키는 live** 다. live 키는 Real 만 믿고 Fake 의 delta 를 버린다.
 * 나머지 키는 Fake(시나리오 재생기)가 준다. 그래서 사내 시연의 배지가 "실신호 2 · 시뮬레이션 6 · 미측정 0" 처럼 섞여 나오고,
 * 조향각·기어가 실물에 없어도 도식·가이드가 살아 있다. [isLive] 로 [SignalRegistry] 가 키별 출처를 안다.
 *
 * 주의: live 키에는 시연 조작(정차·도어)이 먹지 않는다 — Real 값이 이긴다. 실차에서 도어는 실제로 열어야 한다.
 */
class HybridVehiclePort(
    private val real: VehiclePort,
    val fake: FakeVehiclePort,
) : VehiclePort {

    private val live: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun isLive(key: String): Boolean = key in live
    val liveKeys: Set<String> get() = live.toSet()

    override suspend fun get(keys: List<String>): Map<String, String> {
        val fromReal = runCatching { real.get(keys) }.getOrElse { emptyMap() }
        live.addAll(fromReal.keys)
        val missing = keys.filter { it !in fromReal }
        val fromFake = if (missing.isEmpty()) emptyMap() else fake.get(missing)
        return fromFake + fromReal   // 겹치면 Real
    }

    override suspend fun set(values: Map<String, String>): List<String> {
        if (values.isEmpty()) return emptyList()
        val failed = runCatching { real.set(values) }.getOrElse { values.keys.toList() }
        if (failed.isEmpty()) return emptyList()
        // Real 이 거부한(모르는) 키는 Fake 가 받는다 — 시연 조작이 live 아닌 키에는 통한다
        return fake.set(values.filterKeys { it in failed })
    }

    override fun observe(keys: List<String>): Flow<Map<String, String>> = flow {
        emit(get(keys))
        merge(
            real.observe(keys).drop(1).onEach { live.addAll(it.keys) },
            fake.observe(keys).drop(1).map { delta -> delta.filterKeys { it !in live } }.filter { it.isNotEmpty() },
        ).collect { emit(it) }
    }

    override fun dispose() {
        real.dispose()
        fake.dispose()
    }
}
