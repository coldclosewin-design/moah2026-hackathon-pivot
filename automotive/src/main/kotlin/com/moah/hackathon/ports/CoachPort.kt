package com.moah.hackathon.ports

import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Profile
import com.moah.hackathon.feature.lesson.RemarkPool
import com.moah.hackathon.feature.lesson.ScoreBand
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.scoring.ChecklistScorer
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingScore

/**
 * 코치 — 정차했을 때만 말하는 AI 의 자리 (docs/topics/01_driving_coach.md §6).
 * 주행·주차 중 힌트는 규칙([com.moah.hackathon.feature.lesson.HintRules])이고 여기 오지 않는다.
 *
 * **구현체는 예외를 던지지 않는다.** 실패하면 시드 멘트 풀에서 골라 돌려준다 — 그래야 시연이 안 깨진다.
 * 외부·기본은 [FakeCoachPort]. Cloud Copilot 구현체는 사내 인증 확인 후 같은 인터페이스로.
 */
interface CoachPort {
    /** 회차 멘트 — "4번 만에, 44초. 장롱 10년차 첫 주차치고 몸이 기억하네요." 과제에 따라 머리말과 멘트 풀이 다르다([attemptHead]). */
    suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int): String

    /** 세션 총평 — 리포트 상단 한두 문장. */
    suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String
}

/** 회차 멘트의 숫자 머리말. 앱이 붙이고 AI 는 뒤 문장만 쓴다. 주차: "N번 만에, N초." / 출발 전 점검: "출발 준비 N초." */
fun attemptHead(task: Task, score: ParkingScore): String = when (task.type) {
    TaskType.CHECKLIST -> "출발 준비 ${ChecklistScorer.completionMillis(score.metrics) / 1000}초."
    else -> "${score.metrics.motion.movingSegments}번 만에, ${score.metrics.motion.totalMillis / 1000}초."
}

/** 시드 멘트 풀에서 고르는 결정적 구현. 인터넷 불필요. */
class FakeCoachPort(private val pool: RemarkPool) : CoachPort {

    override suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int): String {
        val checklist = task.type == TaskType.CHECKLIST
        // 나아짐의 기준: 주차는 이동 구간 수, 점검은 걸린 시간
        val better = delta != null && if (checklist) delta.seconds < 0 else delta.segments < 0
        val worse = delta != null && if (checklist) delta.seconds > 0 else delta.segments > 0
        val tags = buildSet {
            if ((profile.rustyYears ?: 0) >= 3) add("rusty")
            if (attempt == 1) add("first")
            if (better) add("improved")
            if (worse) add("regressed")
        }
        val m = score.metrics
        val vars = mapOf(
            "name" to profile.name,
            "segments" to m.motion.movingSegments.toString(),
            "seconds" to ((if (checklist) ChecklistScorer.completionMillis(m) else m.motion.totalMillis) / 1000).toString(),
            "years" to (profile.rustyYears ?: 0).toString(),
            "deltaSegments" to kotlin.math.abs(delta?.segments ?: 0).toString(),
            "deltaSeconds" to kotlin.math.abs(delta?.seconds ?: 0L).toString(),
        )
        return "${attemptHead(task, score)} ${pool.pick(ScoreBand.of(score.skill), tags, vars, task.type)}"
    }

    override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String {
        if (attempts.isEmpty()) return "오늘은 움직이지 않았어요. 다음에 다시 해 봐요."
        val first = attempts.first().score
        val last = attempts.last().score
        val best = attempts.maxBy { it.score.skill }.score
        val trend = when {
            attempts.size == 1 -> "한 번 해 봤어요."
            last.skill > first.skill -> "첫 회차 ${first.skill}점에서 ${last.skill}점까지 올라왔어요."
            last.skill < first.skill -> "뒤로 갈수록 힘이 빠졌네요. 첫 회차가 ${first.skill}점으로 가장 좋았어요."
            else -> "회차마다 비슷했어요. 안정적이라는 뜻이에요."
        }
        val safety = when {
            best.safety >= 90 -> "안전 쪽은 걱정할 게 없어요."
            best.safety >= 70 -> "안전 쪽은 한두 가지만 챙기면 돼요."
            task.type == TaskType.CHECKLIST -> "안전 쪽을 먼저 봐야 해요 — 벨트나 기어, 아니면 점검 중에 차가 움직였어요."
            else -> "안전 쪽을 먼저 봐야 해요 — 급조작이나 근접이 있었어요."
        }
        return "${task.title}, ${mode.label} 모드 ${attempts.size}회. $trend $safety"
    }
}
