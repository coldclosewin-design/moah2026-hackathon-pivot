package com.moah.hackathon.feature.lesson

/**
 * 세션 누적 — "지난번보다" 와 자동 제안의 근거 (§3.4). 지금은 인메모리(앱 수명). 파일 저장은 나중에.
 */
class ProgressStore {
    private val records = ArrayList<AttemptRecord>()
    private val quizzes = ArrayList<QuizRecord>()

    /** 제휴 시험장 예약 — 하나만(§3.3, D3). Setup 에서만 바뀐다. `reset` 이 지우지 않는다. */
    var reservation: Reservation? = null

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
        // 구간·조향 평균은 주차·점검 회차로만 — 코스 주행은 정차가 많아 구간 수가 뜻이 다르다(10/4)
        val maneuvers = records.filter { it.course == null }.ifEmpty { records }
        val segments = maneuvers.map { it.score.metrics.motion.movingSegments }
        val reversals = maneuvers.mapNotNull { it.score.metrics.steering?.reversals }
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
        // 운전자에게 보이는·들리는 문장이라 숫자(점수·횟수)를 넣지 않는다(2026-09-27 규칙). 임계값은 위 상수로만.
        val hintPasses = attempts.count { it.mode == LessonMode.HINT && it.score.skill >= HINT_PASS_SKILL }
        if (hintPasses >= PASSES_TO_EVALUATE) return Suggestion(LessonMode.EVALUATE, "힌트 없이도 잘 해냈어요. 이번엔 조용히 볼게요.")
        val guidePasses = attempts.count { it.mode == LessonMode.GUIDE && it.score.skill >= PASS_SKILL }
        if (guidePasses >= PASSES_TO_HINT) return Suggestion(LessonMode.HINT, "가이드는 충분히 익혔어요. 이번엔 틀린 순간에만 말할게요.")
        if (attempts.any { it.mode != LessonMode.GUIDE }) return Suggestion(LessonMode.HINT, "지난번처럼 힌트 모드로 가요. 틀린 순간에만 말할게요.")
        return Suggestion(LessonMode.GUIDE, "한 번 더 가이드로 해 봐요. 아직 순서가 손에 붙지 않았어요.")
    }

    /**
     * 과제 제안 — **시작 가능한(READY) 과제 중에서** 예약한 코스의 과제(D3, 9/28) → 무서운 것(진술) → 관측된 약한 과제 → 첫 쉬운 과제.
     * READY 가 없으면 카탈로그 첫 항목. 예약 코스에 READY 가 없으면 예약을 무시한다.
     */
    fun suggestTask(profile: Profile, tasks: List<Task>, reservation: Reservation? = null, venues: List<Venue> = emptyList()): Task {
        val ready = tasks.filter { it.isReady }.ifEmpty { tasks }
        reservedTask(ready, reservation, venues)?.let { return it }
        profile.observation.weakTaskId?.let { id -> ready.firstOrNull { it.id == id }?.let { return it } }
        val fear = profile.statement.fear
        if (fear != null) ready.firstOrNull { it.title.contains(fear) || fear.contains(it.type.koreanKey()) }?.let { return it }
        return ready.firstOrNull { it.difficulty == Difficulty.EASY } ?: ready.first()
    }

    /** 예약한 코스의 과제 중 첫 READY. 코스 순서대로. 없으면 null. */
    fun reservedTask(tasks: List<Task>, reservation: Reservation?, venues: List<Venue>): Task? {
        val course = reservation?.course(venues) ?: return null
        return course.taskIds.firstNotNullOfOrNull { id -> tasks.firstOrNull { it.id == id && it.isReady } }
    }

    /** 제안 이유 앞에 붙이는 한 문장(숫자 없음) — 과제가 예약 코스에서 왔을 때만. */
    const val RESERVED_REASON = "예약한 코스의 과제부터 해요."

    private fun TaskType.koreanKey(): String = when (this) {
        TaskType.PARKING -> "주차"
        TaskType.DRIVING -> "주행"
        TaskType.CHECKLIST -> "점검"
        TaskType.KNOWLEDGE -> "지식"
    }
}
