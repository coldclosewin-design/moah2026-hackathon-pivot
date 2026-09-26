package com.moah.hackathon.feature.lesson

/**
 * 세션 누적 — "지난번보다" 와 자동 제안의 근거 (§3.4). 지금은 인메모리(앱 수명). 파일 저장은 나중에.
 */
class ProgressStore {
    private val records = ArrayList<AttemptRecord>()
    private val quizzes = ArrayList<QuizRecord>()

    fun add(record: AttemptRecord) { records += record }
    fun addQuiz(record: QuizRecord) { quizzes += record }
    fun quizzes(): List<QuizRecord> = quizzes.toList()
    fun all(): List<AttemptRecord> = records.toList()
    fun forTask(taskId: String): List<AttemptRecord> = records.filter { it.taskId == taskId }
    fun previous(taskId: String): AttemptRecord? = forTask(taskId).lastOrNull()
    fun best(taskId: String): AttemptRecord? = forTask(taskId).maxByOrNull { it.score.skill }

    /** 관측 프로필 — 진술과 어긋나면 앱이 제안한다. */
    fun observation(): ProfileObservation {
        if (records.isEmpty()) return ProfileObservation()
        val segments = records.map { it.score.metrics.motion.movingSegments }
        val reversals = records.mapNotNull { it.score.metrics.steering?.reversals }
        val weak = records.groupBy { it.taskId }.minByOrNull { (_, rs) -> rs.map { it.score.skill }.average() }?.key
        return ProfileObservation(
            attempts = records.size,
            meanSegments = segments.average().toFloat(),
            meanReversals = reversals.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            harshEvents = records.sumOf { it.score.metrics.harshEvents.size },
            weakTaskId = weak,
        )
    }
}

/**
 * 앱이 실력을 보고 모드를 제안한다 (§3.2). 운전자는 언제든 다른 모드를 고를 수 있다.
 *  - 이 과제를 해 본 적 없음 → 가이드
 *  - 가이드 [PASS_SKILL] 점 이상 [PASSES_TO_HINT] 회 → 힌트
 *  - 힌트 [HINT_PASS_SKILL] 점 이상 [PASSES_TO_EVALUATE] 회 → 평가
 */
object ModeAdvisor {
    const val PASS_SKILL = 70
    const val PASSES_TO_HINT = 2
    const val HINT_PASS_SKILL = 80
    const val PASSES_TO_EVALUATE = 2

    data class Suggestion(val mode: LessonMode, val reason: String)

    fun suggest(task: Task, store: ProgressStore): Suggestion {
        val attempts = store.forTask(task.id)
        if (attempts.isEmpty()) return Suggestion(LessonMode.GUIDE, "처음 하는 과제라 가이드부터 해요. 제가 단계마다 확인할게요.")
        val hintPasses = attempts.count { it.mode == LessonMode.HINT && it.score.skill >= HINT_PASS_SKILL }
        if (hintPasses >= PASSES_TO_EVALUATE) return Suggestion(LessonMode.EVALUATE, "힌트 없이도 ${HINT_PASS_SKILL}점을 ${hintPasses}번 넘었어요. 이번엔 조용히 볼게요.")
        val guidePasses = attempts.count { it.mode == LessonMode.GUIDE && it.score.skill >= PASS_SKILL }
        if (guidePasses >= PASSES_TO_HINT || attempts.any { it.mode != LessonMode.GUIDE }) {
            return Suggestion(LessonMode.HINT, "가이드를 ${guidePasses}번 통과했어요. 이번엔 틀린 순간에만 말할게요.")
        }
        return Suggestion(LessonMode.GUIDE, "한 번 더 가이드로 해 봐요. 아직 ${PASS_SKILL}점을 ${PASSES_TO_HINT}번 넘지 않았어요.")
    }

    /** 과제 제안 — **시작 가능한(READY) 과제 중에서** 무서운 것(진술) → 관측된 약한 과제 → 첫 쉬운 과제. READY 가 없으면 카탈로그 첫 항목. */
    fun suggestTask(profile: Profile, tasks: List<Task>): Task {
        val ready = tasks.filter { it.isReady }.ifEmpty { tasks }
        profile.observation.weakTaskId?.let { id -> ready.firstOrNull { it.id == id }?.let { return it } }
        val fear = profile.statement.fear
        if (fear != null) ready.firstOrNull { it.title.contains(fear) || fear.contains(it.type.koreanKey()) }?.let { return it }
        return ready.firstOrNull { it.difficulty == Difficulty.EASY } ?: ready.first()
    }

    private fun TaskType.koreanKey(): String = when (this) {
        TaskType.PARKING -> "주차"
        TaskType.DRIVING -> "주행"
        TaskType.CHECKLIST -> "점검"
        TaskType.KNOWLEDGE -> "지식"
    }
}
