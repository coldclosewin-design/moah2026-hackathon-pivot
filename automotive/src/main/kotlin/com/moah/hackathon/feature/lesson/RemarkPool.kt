package com.moah.hackathon.feature.lesson

import com.moah.hackathon.scoring.ParkingVerdict
import kotlin.random.Random

/** 숙련 점수 구간. 멘트 풀의 첫 번째 키. */
enum class ScoreBand {
    EXCELLENT, GOOD, OK, ROUGH;

    companion object {
        fun of(skill: Int): ScoreBand = when {
            skill >= 90 -> EXCELLENT
            skill >= 70 -> GOOD
            skill >= 50 -> OK
            else -> ROUGH
        }
    }
}

/**
 * 멘트 템플릿. [tags] 는 상황("rusty" 장롱면허, "first" 첫 회차, "improved" 나아짐, "regressed" 나빠짐, "any")
 * 과 **판정 태그**([RemarkPool.VERDICT_TAGS]: "one_go"·"one_fix"·"many"·"aligned" — docs/design/09). 판정 태그가 붙은 문장은
 * 그 판정이 **실제로 났을 때만** 뽑힌다(필수 필터, 가점이 아니다) — "한 번에 들어갔어요" 가 세 구간 회차에 나오지 않게.
 * 본문의 `{segments}` `{seconds}` `{years}` `{deltaSegments}` `{deltaSeconds}` `{name}` 은 [RemarkPool.pick] 이 채운다.
 * [taskType] 은 이 멘트가 어울리는 과제 — 주차 멘트("한 번에 들어갔어요")가 출발 전 점검에 나오지 않게 한다.
 */
data class RemarkTemplate(val band: ScoreBand, val tags: Set<String>, val text: String, val taskType: TaskType = TaskType.PARKING)

/**
 * 거부감 있는 말 대신 프로필 기반 위트로 북돋운다 (§3.4). **같은 점수에 같은 말을 반복하지 않는다** — 최근 [recentSize] 개는 피한다.
 * AI(`CoachPort`)가 있으면 이 풀을 변주하고, 없으면 여기서 그대로 고른다.
 */
class RemarkPool(
    private val templates: List<RemarkTemplate>,
    private val random: Random = Random.Default,
    private val recentSize: Int = 3,
) {
    private val recent = ArrayDeque<String>()

    /**
     * @param tags 상황 태그 — 많이 겹칠수록 우선. 판정 태그([VERDICT_TAGS])가 들어 있으면 그것은 **조건**이다:
     *   판정 태그를 가진 템플릿은 그 태그가 [tags] 에 있을 때만 후보가 되고, 최근 문장 회피의 폴백(같은 밴드 전부)에서도 조건을 지킨다.
     *   판정이 없으면(점검 과제·속도 없음) 판정 태그 템플릿은 전부 제외된다.
     */
    fun pick(band: ScoreBand, tags: Set<String>, vars: Map<String, String>, taskType: TaskType = TaskType.PARKING): String {
        val forTask = templates.filter { it.taskType == taskType }.ifEmpty { templates }
        // 판정 필터는 밴드·폴백보다 먼저 — 사실이 아닌 문장은 어느 단계에서도 뽑히지 않는다
        val truthful = forTask.filter { t -> t.tags.all { it !in VERDICT_TAGS || it in tags } }.ifEmpty { forTask.filter { it.tags.none { t -> t in VERDICT_TAGS } } }
        val inBand = truthful.filter { it.band == band }.ifEmpty { truthful }
        // 1순위: 상황 태그가 가장 많이 겹치는 것 + "any". 2순위: 같은 밴드 전부. 어느 쪽이든 최근에 쓴 문장은 피한다 —
        // 반복 금지가 태그 적합보다 우선이다("살아 있는 느낌").
        // 판정이 맞는 문장이 있으면 그것만 1순위 — "any" 보다 사실에 맞는 말이 먼저다
        val verdictHit = inBand.filter { t -> t.tags.any { it in VERDICT_TAGS && it in tags } }
        val scored = verdictHit.ifEmpty { inBand }.map { it to it.tags.count { t -> t in tags } }
        val best = scored.maxOfOrNull { it.second } ?: 0
        val tier1 = scored.filter { it.second == best || "any" in it.first.tags }.map { it.first }
        val candidates = tier1.filter { it.text !in recent }.ifEmpty { inBand.filter { it.text !in recent } }.ifEmpty { tier1 }
        val chosen = candidates[random.nextInt(candidates.size)]
        recent.addLast(chosen.text)
        while (recent.size > recentSize) recent.removeFirst()
        return fill(chosen.text, vars)
    }

    private fun fill(text: String, vars: Map<String, String>): String =
        vars.entries.fold(text) { acc, (k, v) -> acc.replace("{$k}", v) }

    companion object {
        /** 판정 태그 — 템플릿에 붙으면 조건이 된다(docs/design/09, 라운드 12 ① C 절). */
        val VERDICT_TAGS: Set<String> = setOf("one_go", "one_fix", "many", "aligned")

        /** 판정 → 태그. 판정이 없으면 비어 있다(판정 태그 템플릿은 전부 제외). 방향은 `ALIGNED` 일 때만 태그가 된다(추정이라 긍정만 말한다). */
        fun verdictTags(verdict: ParkingVerdict?): Set<String> = buildSet {
            verdict ?: return@buildSet
            add(when (verdict.entry) {
                ParkingVerdict.Entry.ONE_GO -> "one_go"
                ParkingVerdict.Entry.ONE_FIX -> "one_fix"
                ParkingVerdict.Entry.MANY -> "many"
            })
            if (verdict.heading == ParkingVerdict.Heading.ALIGNED) add("aligned")
        }
    }
}
