package com.moah.hackathon.ports

import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Profile
import com.moah.hackathon.feature.lesson.RemarkPool
import com.moah.hackathon.feature.lesson.RemarkTemplate
import com.moah.hackathon.feature.lesson.ScoreBand
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.scoring.ChecklistScorer
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingRubric
import com.moah.hackathon.scoring.ParkingScore

/**
 * 코치 — 정차했을 때만 말하는 AI 의 자리 (docs/topics/01_driving_coach.md §6).
 * 주행·주차 중 힌트는 규칙([com.moah.hackathon.feature.lesson.HintRules])이고 여기 오지 않는다.
 *
 * **운전자에게 들려주는 문장에는 숫자(횟수·초·점수)를 넣지 않는다**(2026-09-27 결정, `docs/design/03_round2_feedback.md` §0).
 * 회차 멘트는 **"서두. 조언."** 두 문장 — 서두는 밴드·프로필 기반 위트([RemarkPool]), 조언은 지표에서 고른 한 가지([AdviceRules]).
 * 줄바꿈(`\n`) 으로 문장을 나눠 화면이 문장 단위로 줄을 끊는다. 숫자는 리포트 "자세히 보기" 에만 있다.
 *
 * **구현체는 예외를 던지지 않는다.** 실패하면 시드 멘트 풀에서 골라 돌려준다 — 그래야 시연이 안 깨진다.
 */
interface CoachPort {
    /** 회차 멘트 — "장롱의 문 정도는 열었습니다. 좋은 출발이에요.\n핸들을 끝까지 꺾은 채 중립을 조금 늦게 잡아 보세요." */
    suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int): String

    /** 세션 총평 — 리포트 상단 두 문장(흐름 / 안전 한 가지). 숫자 없음. */
    suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String

}

/** 회차의 숫자 머리말 — **운전자 문장에는 쓰지 않는다.** 리포트 자세히 보기·AI 프롬프트 컨텍스트용. 주차 "N번 만에, N초." / 점검 "출발 준비 N초." */
fun attemptHead(task: Task, score: ParkingScore): String = when (task.type) {
    TaskType.CHECKLIST -> "출발 준비 ${ChecklistScorer.completionMillis(score.metrics) / 1000}초."
    else -> "${score.metrics.motion.movingSegments}번 만에, ${score.metrics.motion.totalMillis / 1000}초."
}

/**
 * 조언 한 문장 — 채점 지표에서 **가장 먼저 고칠 것 하나**를 고른다(안전 → 숙련 순). 숫자 없음.
 * 힌트 규칙과 같은 사실을 말하되, 회차가 끝난 뒤의 말투("다음엔")로.
 */
object AdviceRules {
    /** 고칠 것 하나 — 운전자에게 하는 말. */
    enum class Advice(val driver: String) {
        // 출발 전 점검
        BELT_BEFORE_IGNITION("다음엔 벨트가 먼저, 시동은 그다음이에요."),
        BELT_MISSING("벨트를 매고 시작하는 것부터 몸에 붙여요."),
        DOOR_OPEN_AT_IGNITION("문을 닫고 시동을 켜요."),
        NO_BRAKE_AT_IGNITION("시동은 브레이크를 밟은 채로요."),
        MOVED_DURING_CHECK("점검은 차를 세운 채로 해요. 움직이는 건 그다음."),
        CHECK_PARK("시동을 켤 땐 기어가 P 인지 한 번 더 봐요."),
        LIGHTS_SKIPPED("지시등과 비상등도 출발 전에 한 번씩 켜 봐요."),
        KEEP_ORDER("이 순서 그대로 몸에 남겨 두세요."),
        // 주차
        BELT_FIRST("다음엔 벨트를 먼저 매고 출발해요."),
        BRAKE("브레이크는 천천히, 멈추기 전에 미리 밟아 보세요."),
        PROXIMITY("뒤 거리를 조금 더 남겨 보세요."),
        STEERING("핸들을 끝까지 꺾은 채 중립을 조금 늦게 잡아 보세요."),
        SHIFT("전진으로 보정할 때는 핸들을 반대로 돌려 두세요."),
        SEGMENTS("한 번에 조금 더 깊이 들어가 봐요. 멈추는 횟수가 줄어요."),
        PARK("다 들어왔으면 기어 P 까지가 마무리예요."),
        KEEP("이 감각 그대로 한 번만 더 해 봐요."),
    }

    fun pick(task: Task, score: ParkingScore, rubric: ParkingRubric = ParkingRubric()): Advice {
        val m = score.metrics
        if (task.type == TaskType.CHECKLIST) {
            val pd = m.preDrive
            return when {
                pd.beltBeforeIgnition == false -> Advice.BELT_BEFORE_IGNITION
                pd.beltOnMillis == null && score.missingSignals.none { it.contains("IsBelted") } -> Advice.BELT_MISSING
                pd.doorClosedBeforeIgnition == false -> Advice.DOOR_OPEN_AT_IGNITION
                pd.brakeBeforeIgnition == false -> Advice.NO_BRAKE_AT_IGNITION
                m.motion.movingSegments > 0 -> Advice.MOVED_DURING_CHECK
                m.gear?.endedInPark == false -> Advice.CHECK_PARK
                pd.skippedLights > 0 -> Advice.LIGHTS_SKIPPED
                else -> Advice.KEEP_ORDER
            }
        }
        return when {
            m.preDrive.beltBeforeFirstMove == false -> Advice.BELT_FIRST
            m.harshEvents.isNotEmpty() -> Advice.BRAKE
            (m.proximity?.warnings ?: 0) > 0 -> Advice.PROXIMITY
            (m.steering?.reversals ?: 0) > rubric.idealReversals -> Advice.STEERING
            (m.gear?.reverseDriveShifts ?: 0) > rubric.idealShifts -> Advice.SHIFT
            m.motion.movingSegments > rubric.idealSegments -> Advice.SEGMENTS
            m.gear?.endedInPark == false -> Advice.PARK
            else -> Advice.KEEP
        }
    }

    fun advice(task: Task, score: ParkingScore, rubric: ParkingRubric = ParkingRubric()): String = pick(task, score, rubric).driver
}

/** 시드 멘트 풀에서 고르는 결정적 구현. 인터넷 불필요. */
class FakeCoachPort(
    private val pool: RemarkPool,
    private val rubric: ParkingRubric = ParkingRubric(),
) : CoachPort {

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
        val vars = mapOf("name" to profile.name, "years" to (profile.rustyYears ?: 0).toString())
        val opener = pool.pick(ScoreBand.of(score.skill), tags, vars, task.type)
        return "$opener\n${AdviceRules.advice(task, score, rubric)}"
    }

    override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String {
        if (attempts.isEmpty()) return "오늘은 움직이지 않았어요. 다음에 다시 해 봐요."
        val first = attempts.first().score
        val last = attempts.last().score
        val best = attempts.maxBy { it.score.skill }.score
        val trend = when {
            attempts.size == 1 -> "한 번 해 봤어요. 감각이 어땠는지 기억해 두세요."
            last.skill > first.skill -> "회차를 거듭할수록 좋아졌어요. 이게 연습의 맛이에요."
            last.skill < first.skill -> "뒤로 갈수록 힘이 빠졌지만 첫 회차가 좋았어요. 쉬었다 하면 돌아와요."
            else -> "회차마다 비슷했어요. 안정적이라는 뜻이에요."
        }
        val safety = when {
            best.safety >= 90 -> "안전 쪽은 걱정할 게 없어요."
            best.safety >= 70 -> "안전 쪽은 한두 가지만 챙기면 돼요."
            task.type == TaskType.CHECKLIST -> "안전 쪽을 먼저 봐야 해요. 문이나 벨트, 기어, 아니면 점검 중에 차가 움직였어요."
            else -> "안전 쪽을 먼저 봐야 해요. 급조작이나 근접이 있었어요."
        }
        // 과제·모드·회차 수 머리말은 붙이지 않는다 — 화면이 과제·모드를 따로 쓰고, 회차 수는 리포트 그래픽·자세히 보기에 있다(9/27 라운드 3 리뷰).
        return "$trend\n$safety"
    }
}
