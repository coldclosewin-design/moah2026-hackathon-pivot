package com.moah.hackathon.vehicle

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.util.concurrent.ConcurrentHashMap

/**
 * 실차 포트에서 **오지 않는 키만** Fake 로 채운다 — "사내에 신호가 없어도 가상 신호로 진행"(2026-09-25 결정)의 구현체.
 *
 * 규칙 둘(2026-09-30 사내 이관 1차에서 보정):
 *  1. **Real 이 한 번이라도 값을 낸 키는 live** 다. live 키는 Real 만 믿고 Fake 의 delta 를 버린다. 나머지 키는 Fake(시나리오 재생기)가 준다.
 *     그래서 사내 시연의 배지가 "실신호 6 · 시뮬레이션 2 · 미측정 0" 처럼 섞여 나온다. [isLive] 로 [SignalRegistry] 가 키별 출처를 안다.
 *  2. **Fake 의 조작(시연 패널·시나리오)은 실물을 거쳐 돌아온다** — [FakeVehiclePort.writeThrough] 훅으로 Fake 가 값을 저장·emit 하기
 *     직전에 `real.set` 을 먼저 부른다. 실물이 받아 주면 Real 구독으로 같은 값이 돌아오고(live), Fake delta 는 버려진다.
 *     실물이 **실패 목록으로 돌려준 키 = [forced]**: live 에서 빼고 이후 `get`·Real 구독에서 그 키는 무시, Fake 값이 화면까지 간다(배지 "시뮬레이션").
 *     사내 관찰: 회차 시작 `get` 에서 주차 8키 중 6개가 실신호라 시연 패널의 시나리오가 live 키에 먹지 않아 시연이 멈췄다 → 이 훅으로 해결.
 *     실측: 속도·조향·기어·벨트·시동 모두 실물에 써졌고 실신호로 돌아와 가이드가 끝까지 진행.
 *
 * 주의: `setVSS` 는 실물에 없는 경로에도 실패 목록을 비워 준다 — 성공 판정은 반환값이 아니라 **구독으로 값이 돌아오는지**다.
 * 그런 키는 live 가 되지 않으므로 어차피 Fake delta 가 통과한다.
 */
class HybridVehiclePort(
    private val real: VehiclePort,
    val fake: FakeVehiclePort,
) : VehiclePort {

    private val live: MutableSet<String> = ConcurrentHashMap.newKeySet()
    /** 실물이 쓰기를 거부한(읽기 전용 sensor 등) 키 — 다시 live 가 되지 않고 Fake 값만 쓴다. */
    private val forced: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        fake.writeThrough = { values -> writeThrough(values) }
    }

    fun isLive(key: String): Boolean = key in live
    val liveKeys: Set<String> get() = live.toSet()
    val forcedKeys: Set<String> get() = forced.toSet()

    /** [forced] 키는 절대 live 로 올리지 않는다(`markLive` 규칙). */
    private fun markLive(keys: Collection<String>) {
        val added = keys.filter { it !in forced && live.add(it) }
        if (added.isNotEmpty()) Log.i(TAG, "live +$added → live=${live.size}")
    }

    private suspend fun writeThrough(values: Map<String, String>) {
        val failed = runCatching { real.set(values) }.getOrElse { values.keys.toList() }
        if (failed.isEmpty()) return
        Log.i(TAG, "setVSS rejected $failed → forced (Fake 값 사용)")
        forced.addAll(failed)
        live.removeAll(failed.toSet())
    }

    override suspend fun get(keys: List<String>): Map<String, String> {
        val fromReal = runCatching { real.get(keys) }.getOrElse { emptyMap() }.filterKeys { it !in forced }
        markLive(fromReal.keys)
        val missing = keys.filter { it !in fromReal }
        val fromFake = if (missing.isEmpty()) emptyMap() else fake.get(missing)
        Log.i(TAG, "get real=${fromReal.keys.map { it.substringAfterLast('.') }} fake=${fromFake.keys.map { it.substringAfterLast('.') }}")
        return fromFake + fromReal   // 겹치면 Real
    }

    /** 시연 조작은 Fake 로 — [FakeVehiclePort.writeThrough] 가 실물에 먼저 쓴다. */
    override suspend fun set(values: Map<String, String>): List<String> = fake.set(values)

    override fun observe(keys: List<String>): Flow<Map<String, String>> = flow {
        emit(get(keys))
        merge(
            real.observe(keys).drop(1)
                .map { delta -> delta.filterKeys { it !in forced } }
                .filter { it.isNotEmpty() }
                .map { delta -> markLive(delta.keys); delta },
            fake.observe(keys).drop(1).map { delta -> delta.filterKeys { it !in live } }.filter { it.isNotEmpty() },
        ).collect { emit(it) }
    }

    override fun dispose() {
        fake.writeThrough = null
        real.dispose()
        fake.dispose()
    }

    private companion object {
        const val TAG = "MOAH/HybridVehiclePort"
    }
}
