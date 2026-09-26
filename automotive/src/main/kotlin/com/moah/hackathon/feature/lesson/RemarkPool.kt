package com.moah.hackathon.feature.lesson

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
 * 멘트 템플릿. [tags] 는 상황("rusty" 장롱면허, "first" 첫 회차, "improved" 나아짐, "regressed" 나빠짐, "any").
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

    fun pick(band: ScoreBand, tags: Set<String>, vars: Map<String, String>, taskType: TaskType = TaskType.PARKING): String {
        val forTask = templates.filter { it.taskType == taskType }.ifEmpty { templates }
        val inBand = forTask.filter { it.band == band }.ifEmpty { forTask }
        // 1순위: 상황 태그가 가장 많이 겹치는 것 + "any". 2순위: 같은 밴드 전부. 어느 쪽이든 최근에 쓴 문장은 피한다 —
        // 반복 금지가 태그 적합보다 우선이다("살아 있는 느낌").
        val scored = inBand.map { it to it.tags.count { t -> t in tags } }
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
}
