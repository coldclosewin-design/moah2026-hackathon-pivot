package com.moah.hackathon.ports

import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.CoachContext
import com.moah.hackathon.feature.lesson.CoachReply
import com.moah.hackathon.feature.lesson.CoachTurn
import com.moah.hackathon.feature.lesson.IntentRules
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Profile
import com.moah.hackathon.feature.lesson.RemarkPool
import com.moah.hackathon.feature.lesson.RemarkTemplate
import com.moah.hackathon.feature.lesson.ScoreBand
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.scoring.ChecklistScorer
import com.moah.hackathon.scoring.CourseResult
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingRubric
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.scoring.ParkingVerdict

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
    suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int, verdict: ParkingVerdict? = null,
        course: CourseResult? = null): String

    /** 세션 총평 — 리포트 상단 두 문장(흐름 / 안전 한 가지). 숫자 없음. */
    suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String

    /**
     * 홈 코치 텍스트 대화 한 턴(사내 피드백 #4, 10/6) — [history] 는 이번 시트의 앞선 줄(코치 첫 말 포함), [utterance] 는 운전자의 새 글.
     * 답은 문장 하나 + 안내 의도 하나이고, 의도는 [context] 안의 것만. 기본은 키워드 규칙([IntentRules]) — Fake 가 이것으로 완결된다.
     */
    suspend fun converse(history: List<CoachTurn>, utterance: String, context: CoachContext): CoachReply =
        IntentRules.reply(utterance, context)
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
        MOVED_DURING_CHECK("점검을 마친 뒤에 움직여요."),
        CHECK_PARK("시동 전에 기어가 주차인지 봐요."),
        LIGHTS_SKIPPED("지시등과 비상등도 한 번씩 켜 봐요."),
        KEEP_ORDER("이 순서 그대로 몸에 남겨 두세요."),
        // 주차
        BELT_FIRST("다음엔 벨트를 먼저 매고 출발해요."),
        BRAKE("멈추기 전에 브레이크를 미리 밟아요."),
        PROXIMITY("뒤 거리를 조금 더 남겨 보세요."),
        STEERING("핸들 푸는 때를 조금 늦춰 봐요."),
        SHIFT("전진 보정 땐 핸들을 반대로 돌려요."),
        // 전면 직각 주차(10/2): 앞으로 들어가므로 보정은 후진, 가까운 쪽은 앞
        PROXIMITY_FRONT("앞 거리를 조금 더 남겨 보세요."),
        SHIFT_FRONT("후진 보정 땐 핸들을 반대로 돌려요."),
        SEGMENTS("한 번에 조금 더 깊이 들어가 봐요."),
        PARK("다 들어왔으면 주차 기어까지가 마무리예요."),
        KEEP("이 감각 그대로 한 번만 더 해 봐요."),
    }

    fun pick(task: Task, score: ParkingScore, base: ParkingRubric = ParkingRubric()): Advice {
        val rubric = task.parkingSpec.rubric(base)   // 평행 주차는 되돌림 정석 2(10/4)
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
        val front = task.parkingSpec.entryGear == com.moah.hackathon.vehicle.Gear.DRIVE
        return when {
            m.preDrive.beltBeforeFirstMove == false -> Advice.BELT_FIRST
            m.harshEvents.isNotEmpty() -> Advice.BRAKE
            (m.proximity?.warnings ?: 0) > 0 -> if (front) Advice.PROXIMITY_FRONT else Advice.PROXIMITY
            (m.steering?.reversals ?: 0) > rubric.idealReversals -> Advice.STEERING
            (m.gear?.reverseDriveShifts ?: 0) > rubric.idealShifts -> if (front) Advice.SHIFT_FRONT else Advice.SHIFT
            m.motion.movingSegments > rubric.idealSegments -> Advice.SEGMENTS
            m.gear?.endedInPark == false -> Advice.PARK
            else -> Advice.KEEP
        }
    }

    fun advice(task: Task, score: ParkingScore, rubric: ParkingRubric = ParkingRubric()): String = pick(task, score, rubric).driver
}

/**
 * 코스 과제(10/4)의 회차 멘트 — "서두.\n조언." 두 문장, 숫자 없음. 서두는 결과(합격·실격·깨끗함), 조언은 가장 큰 감점 하나를 "다음엔" 말투로.
 */
object CourseRemarks {
    fun opener(r: CourseResult): String = when {
        !r.positionMeasured -> "끝까지 달렸어요."
        r.disqualified -> "실격 사유가 있었어요."
        r.passed == true && r.deductions.isEmpty() -> "감점 없이 합격이에요."
        r.passed == true -> "합격선을 넘었어요."
        r.passed == false -> "합격선 조금 아래예요."
        r.deductions.isEmpty() -> "할 일을 다 챙겼어요."
        r.deductions.size == 1 -> "한 가지만 놓쳤어요."
        else -> "놓친 구간이 있었어요."
    }

    /** 가장 큰 감점(실격 먼저) 하나의 다음 행동. */
    fun advice(r: CourseResult): String {
        val d = r.deductions.sortedWith(compareByDescending<com.moah.hackathon.scoring.Deduction> { it.disqualify }.thenByDescending { it.points }).firstOrNull()
            ?: return "이 흐름 그대로 한 번 더 해 봐요."
        return when {
            d.reason == "신호 위반" -> "정지선 앞에서 신호를 기다려요."
            d.reason == "검지선 접촉" -> "${d.zoneTitle}에선 핸들을 늦게 돌려요."
            d.reason == "뒤로 밀림" -> "경사로에선 가속 뒤 브레이크를 떼요."
            d.reason == "출발 지연" -> "다음엔 멈춘 뒤 바로 출발 준비를 해요."
            d.reason == "방향지시등 미점등" -> "${d.zoneTitle} 전엔 지시등부터 켜요."
            d.reason == "가속 부족" -> "다음엔 가속 구간에서 속도를 충분히 올려요."
            d.reason == "속도 초과" -> "${d.zoneTitle}에선 속도부터 줄여요."
            d.reason == "돌발 정지 지연" -> "경보가 울리면 바로 멈춰요."
            d.reason == "비상등 미점등" -> "다음엔 돌발 정지 뒤 비상등까지 켜요."
            d.reason == "주차 칸 밖 정지" -> "다음엔 칸 끝까지 천천히 들어간 뒤 멈춰요."
            d.reason == "시간 초과" -> "다음엔 순서를 미리 떠올리고 들어가요."
            d.reason.endsWith("미조작") -> "다음엔 출발 전에 장치를 하나씩 켰다 꺼요."
            d.reason.endsWith("미정지") -> "${d.zoneTitle}에선 완전히 멈췄다 가요."
            else -> "다음엔 ${d.zoneTitle.withObjectParticle()} 한 번 더 연습해 봐요."
        }
    }

    fun remark(r: CourseResult): String = "${opener(r)}\n${advice(r)}"
}

/** 시드 멘트 풀에서 고르는 결정적 구현. 인터넷 불필요. */
class FakeCoachPort(
    private val pool: RemarkPool,
    private val rubric: ParkingRubric = ParkingRubric(),
) : CoachPort {

    override suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int, verdict: ParkingVerdict?,
        course: CourseResult?): String {
        if (course != null) return CourseRemarks.remark(course)
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
        // 판정 태그(one_go·one_fix·many·aligned)는 조건 — 사실이 아닌 서두는 뽑히지 않는다(docs/design/09, 라운드 12 ① C 절)
        val opener = pool.pick(ScoreBand.of(score.skill), tags + RemarkPool.verdictTags(verdict), vars, task.type)
        return "$opener\n${AdviceRules.advice(task, score, rubric)}"
    }

    override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String {
        if (attempts.isEmpty()) return "오늘은 쉬어 갔어요.\n다음에 다시 해 봐요."
        val first = attempts.first().score
        val last = attempts.last().score
        val best = attempts.maxBy { it.score.skill }.score
        val trend = when {
            attempts.size == 1 -> "한 번 해 봤어요."
            last.skill > first.skill -> "갈수록 좋아졌어요."
            last.skill < first.skill -> "첫 회차가 좋았어요."
            else -> "고르게 해냈어요."
        }
        val lastCourse = attempts.last().course
        val safety = when {
            // 코스 과제(10/5): 근접 센서를 쓰지 않으니 "근접" 이라고 하지 않는다 — 급조작과 놓친 구간으로
            lastCourse != null -> when {
                attempts.any { it.score.metrics.harshEvents.isNotEmpty() } -> "급하게 서고 출발한 순간을 줄여요."
                lastCourse.deductions.isNotEmpty() -> "놓친 구간만 천천히 다시 해 봐요."
                else -> "구간마다 할 일을 다 챙겼어요."
            }
            best.safety >= 90 -> "안전 쪽은 걱정할 게 없어요."
            best.safety >= 70 -> "안전은 한두 가지만 챙기면 돼요."
            task.type == TaskType.CHECKLIST -> "문·벨트·기어 순서부터 챙겨요."
            else -> "급조작과 근접부터 줄여 봐요."
        }
        // 과제·모드·회차 수 머리말은 붙이지 않는다 — 화면이 과제·모드를 따로 쓰고, 회차 수는 리포트 그래픽·자세히 보기에 있다(9/27 라운드 3 리뷰).
        return "$trend\n$safety"
    }
}
