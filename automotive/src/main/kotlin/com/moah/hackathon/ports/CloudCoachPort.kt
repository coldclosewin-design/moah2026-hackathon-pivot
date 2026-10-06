package com.moah.hackathon.ports

import android.util.Log
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.BookingOption
import com.moah.hackathon.feature.lesson.CoachContext
import com.moah.hackathon.feature.lesson.CoachIntent
import com.moah.hackathon.feature.lesson.CoachReply
import com.moah.hackathon.feature.lesson.CoachTurn
import com.moah.hackathon.feature.lesson.ReplySource
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Profile
import com.moah.hackathon.feature.lesson.ScoreBand
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.ports.copilot.JsonLite
import com.moah.hackathon.scoring.CourseResult
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.scoring.ParkingVerdict
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LLM 호출의 전송 계층. 구현체는 `ports/copilot/CopilotCoachTransport`(device code 인증 → 세션 토큰 → chat, 10/1 사내 통과).
 * null 이면 [CloudCoachPort] 가 항상 시드 문장으로 폴백한다. 반환은 본문 텍스트 한 덩어리.
 */
interface CoachTransport {
    suspend fun complete(system: String, user: String): String

    /** 여러 턴(홈 코치 대화, 10/6). 기본은 이력을 한 덩어리 user 로 붙여 [complete] — Copilot 구현은 messages 배열로 보낸다. */
    suspend fun chat(system: String, messages: List<ChatMessage>): String =
        complete(system, messages.joinToString("\n") { "${it.role}: ${it.content}" })
}

/** chat 요청의 한 줄 — role 은 `user`(운전자) 또는 `assistant`(코치). */
data class ChatMessage(val role: String, val content: String)

/**
 * AI 코치 — 정차했을 때만 말한다. 프롬프트를 조립해 [CoachTransport] 에 보내고, **어떤 이유로든 못 쓰면 [fallback](시드 멘트 풀)으로**.
 * 못 쓰는 경우: 전송 계층 없음 · 예외 · [timeoutMillis] 초과 · 응답 검증 실패(빈 값, 너무 긺, 금지어). 예외를 밖으로 던지지 않는다(CoachPort 계약).
 *
 * 응답은 그대로 믿지 않는다 — 시연 중 화면에 나가는 문장이라 길이·금지어를 여기서 거른다.
 */
class CloudCoachPort(
    private val fallback: CoachPort,
    private val transport: CoachTransport?,
    private val timeoutMillis: Long = 5_000L,   // 4 → 5 s (9/30 사내 실측: Done 2~4 s)
    /** `copilot_config.json` 의 선택 키 `persona`(10/6). 없으면 기본 페르소나. 형식 규칙은 코드가 뒤에 붙여 덮어쓸 수 없다. */
    private val persona: String? = null,
) : CoachPort {

    override suspend fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int, verdict: ParkingVerdict?,
        course: CourseResult?): String {
        val safe = fallback.remark(task, score, delta, profile, attempt, verdict, course)
        val (system, user) = CoachPrompts.remark(task, score, delta, profile, attempt, seedLine = safe.replace('\n', ' '), course = course, persona = persona)
        // 숫자 머리말은 붙이지 않는다(운전자 문장 규칙). 두 문장을 줄바꿈으로 — 화면이 문장 단위로 줄을 끊는다
        return ask(system, user, maxChars = CoachPrompts.REMARK_MAX_CHARS)?.let { CoachPrompts.twoLines(it) } ?: safe
    }

    override suspend fun summarize(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile): String {
        val safe = fallback.summarize(task, mode, attempts, profile)
        if (attempts.isEmpty()) return safe
        val (system, user) = CoachPrompts.summary(task, mode, attempts, profile, seedLine = safe, persona = persona)
        return ask(system, user, maxChars = CoachPrompts.SUMMARY_MAX_CHARS) ?: safe
    }

    /**
     * 홈 코치 대화 — 이력 + 상황을 보내 JSON 한 줄(문장 + 의도)을 받는다. 형식이 틀리거나, 의도가 [context] 밖이거나, 문장이 규칙에 어긋나면
     * 키워드 규칙([fallback] 의 converse)으로 — 그때 출처는 `fallback`. 전송 계층이 없으면 처음부터 규칙(`rule`).
     */
    override suspend fun converse(history: List<CoachTurn>, utterance: String, context: CoachContext): CoachReply {
        if (transport == null) return fallback.converse(history, utterance, context)
        val raw = call { it.chat(CoachPrompts.dialogSystem(context, persona), CoachPrompts.dialogMessages(history, utterance)) }
        val parsed = raw?.let { CoachPrompts.parseReply(it, context) }
        if (raw != null && parsed == null) Log.w(TAG, "dialog response rejected → rules: ${raw.take(60)}")
        return parsed ?: fallback.converse(history, utterance, context).copy(source = ReplySource.FALLBACK)
    }

    /** 성공하면 검증된 문장, 아니면 null(→ 호출자가 폴백). */
    private suspend fun ask(system: String, user: String, maxChars: Int): String? {
        val raw = call { it.complete(system, user) } ?: return null
        return CoachPrompts.validate(raw, maxChars).also { if (it == null) Log.w(TAG, "response rejected → fallback: ${raw.take(40)}") }
    }

    /** 전송 한 번 — 전송 계층 없음·예외·시간 초과면 null. */
    private suspend fun call(block: suspend (CoachTransport) -> String): String? {
        val t = transport ?: run { Log.d(TAG, "no transport → fallback"); return null }
        val raw = try {
            withTimeoutOrNull(timeoutMillis) { block(t) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 안드로이드의 IOException·JSONException 은 checked 라 RuntimeException 만 잡으면 새어 나간다(9/30 사내) — 전부 폴백
            Log.w(TAG, "transport failed → fallback", e); return null
        }
        if (raw == null) Log.w(TAG, "transport timed out (${timeoutMillis} ms) → fallback")
        return raw
    }

    private companion object {
        const val TAG = "MOAH/CloudCoachPort"
    }
}

/** 프롬프트 조립과 응답 검증. 순수 함수 — 테스트로 고정한다. */
object CoachPrompts {
    const val REMARK_MAX_CHARS = 90
    const val SUMMARY_MAX_CHARS = 160

    /** 두려움을 줄이는 앱이 쓰지 않는 말(§3.4). 응답에 들어 있으면 버린다. */
    val BANNED: List<String> = listOf("하위", "실패", "못했", "형편없", "최악", "낙제", "불합격", "위험한 운전자")

    /** 홈 대화 말풍선 한 번의 최대 길이. */
    const val DIALOG_MAX_CHARS = 80
    /** AI 에 보내는 이력 줄 수(코치 첫 말 포함). */
    const val DIALOG_HISTORY = 8
    /** `persona` 키의 최대 길이 — 넘으면 자른다. */
    const val PERSONA_MAX_CHARS = 400

    const val DEFAULT_PERSONA = "당신은 초보 운전자의 조수석에 앉은, 화내지 않는 운전 코치입니다."

    /** 페르소나 뒤에 붙는 형식 규칙의 머리 — `persona` 키가 무엇이든 규칙이 우선한다. */
    private const val RULES_HEAD = "아래 형식 규칙은 위 설명보다 우선합니다."

    private const val REMARK_RULES = """규칙: 한국어 존댓말, 정확히 두 문장 — 첫 문장은 응원하는 서두, 둘째 문장은 고칠 것 하나. 위트 있게 북돋우되 사실만.
숫자(횟수·초·점수·등수)를 말하지 않습니다. 금지어: 하위, 실패, 못했, 최악, 낙제.
운전자의 프로필(장롱면허 햇수, 목표, 무서운 것)과 이번 회차의 과정 지표만 근거로 씁니다. 차가 칸에 반듯이 들어갔는지는 모릅니다 — 말하지 않습니다.
위 과정 지표에 없는 항목(예: 뒤쪽 시야 확보, 사이드미러)은 조언하지 않습니다."""

    /** 페르소나(설정 키 또는 기본) — 빈 값은 기본, 길면 자른다. */
    fun persona(custom: String?): String = custom?.trim()?.takeIf { it.isNotEmpty() }?.take(PERSONA_MAX_CHARS) ?: DEFAULT_PERSONA

    /** 회차 멘트·총평의 system — 페르소나 + 형식 규칙. */
    fun system(persona: String? = null): String = "${persona(persona)}\n$RULES_HEAD\n$REMARK_RULES"

    /** 응답을 문장 단위 두 줄로 — 첫 문장 끝(. ! ? 어느 것이든 — gpt-4o 가 ! 로 자주 끝낸다, 9/30) 뒤에서 한 번만 줄을 바꾼다. 문장이 하나면 그대로. */
    fun twoLines(text: String): String {
        val idx = listOf(". ", "! ", "? ").map { text.indexOf(it) }.filter { it > 0 }.minOrNull() ?: return text
        return if (idx < text.length - 2) text.substring(0, idx + 1) + "\n" + text.substring(idx + 2).trimStart() else text
    }

    fun remark(task: Task, score: ParkingScore, delta: ParkingDelta?, profile: Profile, attempt: Int, seedLine: String,
        course: CourseResult? = null, persona: String? = null): Pair<String, String> {
        val m = score.metrics
        val user = buildString {
            appendLine("운전자: ${profile.name}, 장롱면허 ${profile.rustyYears ?: "?"}년, 목표 ${profile.statement.goal ?: "-"}, 무서운 것 ${profile.statement.fear ?: "-"}.")
            appendLine("과제: ${task.title}, ${attempt}회차. 숙련 구간 ${ScoreBand.of(score.skill)}.")
            if (course != null) {
                // 코스 과제(10/4): 구간 결과만 근거로. 위치·신호등은 시뮬레이션이라는 것을 모델이 말하지 않게 사실만 준다
                val missed = course.deductions.joinToString(", ") { "${it.zoneTitle} ${it.reason}" }.ifEmpty { "없음" }
                val result = when (course.passed) { true -> "합격선 넘음"; false -> "합격선 못 미침"; null -> "연습 코스(합격 판정 없음)" }
                appendLine("코스 결과: $result${if (course.disqualified) ", 실격 사유 있음" else ""}. 놓친 것: $missed. 급조작 ${m.harshEvents.size}회.")
            } else if (task.type == TaskType.CHECKLIST) {
                val pd = m.preDrive
                fun at(ms: Long?) = ms?.let { "${it / 1000}초" } ?: "안 함/미측정"
                val order = when (pd.beltBeforeIgnition) { true -> "벨트 먼저(맞음)"; false -> "시동 먼저(순서 바뀜)"; null -> "모름" }
                appendLine("과정: 벨트 ${at(pd.beltOnMillis)}, 시동 ${at(pd.ignitionOnMillis)}, 순서 $order, 점검 중 움직임 ${m.motion.movingSegments}회, 끝 기어 ${if (m.gear?.endedInPark == true) "P" else m.gear?.let { "P 아님" } ?: "미측정"}.")
                delta?.let { appendLine("지난번 대비: ${signed(it.seconds.toInt())}초.") }
            } else {
                appendLine("과정: 이동 ${m.motion.movingSegments}회, ${m.motion.totalMillis / 1000}초, 조향 되돌림 ${m.steering?.reversals ?: "미측정"}회, 기어 전환 ${m.gear?.reverseDriveShifts ?: "미측정"}회, 급조작 ${m.harshEvents.size}회, 뒤 최소 ${m.proximity?.minDistanceCm?.let { "${it.toInt()} cm" } ?: "미측정"}.")
                delta?.let { appendLine("지난번 대비: 이동 ${signed(it.segments)}회, ${signed(it.seconds.toInt())}초, 조향 ${it.reversals?.let(::signed) ?: "-"}회.") }
            }
            appendLine("참고 문장(이 톤으로, 그대로 쓰지 말고 변주): $seedLine")
            append("두 문장(서두. 조언.), ${REMARK_MAX_CHARS}자 이내로 회차 멘트를 써 주세요. 위 과정 숫자(${attemptHead(task, score)})는 근거로만 쓰고 문장에는 넣지 마세요.")
        }
        return system(persona) to user
    }

    fun summary(task: Task, mode: LessonMode, attempts: List<AttemptRecord>, profile: Profile, seedLine: String, persona: String? = null): Pair<String, String> {
        val user = buildString {
            appendLine("운전자: ${profile.name}, 장롱면허 ${profile.rustyYears ?: "?"}년, 목표 ${profile.statement.goal ?: "-"}.")
            appendLine("과제 ${task.title}, ${mode.label} 모드, ${attempts.size}회차.")
            attempts.forEach { a ->
                appendLine("- ${a.index}회차: 숙련 ${a.score.skill}, 안전 ${a.score.safety}, 이동 ${a.score.metrics.motion.movingSegments}회, ${a.score.metrics.motion.totalMillis / 1000}초, 급조작 ${a.score.metrics.harshEvents.size}, 근접 ${a.score.metrics.proximity?.warnings ?: "미측정"}.")
            }
            appendLine("참고 문장(이 톤으로): ${seedLine.replace('\n', ' ')}")
            append("두 문장, ${SUMMARY_MAX_CHARS}자 이내로 오늘 세션 총평을 써 주세요. 첫 문장은 흐름(나아졌나), 둘째 문장은 안전 쪽 한 가지. 점수·횟수 숫자는 쓰지 마세요.")
        }
        return system(persona) to user
    }

    // ───────── 홈 코치 대화(10/6) ─────────

    /** 대화의 system — 페르소나 + 형식 규칙 + 의도 목록 + 지금 상황(앱이 가진 것만). */
    fun dialogSystem(ctx: CoachContext, persona: String? = null): String = buildString {
        appendLine(persona(persona))
        appendLine("지금은 차를 세운 홈 화면입니다. 운전자가 글로 말을 걸면 앱에 이미 있는 기능 중 하나로 안내합니다.")
        appendLine(RULES_HEAD)
        appendLine("""반드시 JSON 한 줄만 답합니다: {"say":"…","intent":"…"} — 필요하면 "category"·"task"·"mode"·"option" 키를 더합니다.""")
        appendLine("say: 한국어 존댓말 한두 문장, ${DIALOG_MAX_CHARS}자 이내. 숫자(횟수·초·점수·연차)를 쓰지 않습니다. 금지어: ${BANNED.joinToString(", ")}.")
        appendLine("intent 는 아래 중 하나입니다. 목록에 없는 과제·모드·분류를 지어내지 않습니다. 확실하지 않으면 ASK_MORE 로 한 가지만 되묻습니다.")
        appendLine("- OPEN_SHEET + category(${ctx.categories.joinToString("|")}): 그 분류의 과제 목록을 엽니다")
        appendLine("- PIN_TASK + task(아래 id) + mode(그 과제의 모드): 홈 제안을 그 과제·모드로 올립니다")
        if (ctx.bookingVenue != null) {
            appendLine("- BOOKING + option(${ctx.bookingOptions.joinToString("|")}): 예약한 코스로 연습하거나 모의시험을 봅니다")
            appendLine("- SHOW_BOOKING: 예약 카드를 표시합니다")
        }
        if (ctx.last != null) appendLine("- CONTINUE_LAST: 지난번 연습을 이어서 합니다")
        appendLine("- OPEN_PROFILE: 프로필(면허·목표·걱정)을 고치는 시트를 엽니다")
        appendLine("- ASK_MORE: 대화를 이어 갑니다")
        appendLine("분류: PARKING=주차, DRIVING=도로 주행, CHECKLIST=출발 전 점검, KNOWLEDGE=지식(정차 중 문제).")
        appendLine("모드: GUIDE=가이드(단계마다 안내), HINT=힌트(필요할 때만), EVALUATE=평가(조용히 채점), QUIZ=지식 테스트.")
        val st = ctx.profile.statement
        appendLine("운전자: 장롱면허 ${ctx.profile.rustyYears ?: "?"}년, 목표 ${st.goal ?: "-"}, 무서운 것 ${st.fear ?: "-"}.")
        appendLine("과제(id | 제목 | 분류 | 모드, 첫째가 지금 추천):")
        ctx.tasks.forEach { t ->
            val modes = (listOf(t.suggested) + (t.modes - t.suggested)).joinToString(",")
            appendLine("- ${t.id} | ${t.title} | ${t.type} | $modes")
        }
        appendLine("예약: ${ctx.bookingVenue ?: "없음"}.")
        append("지난 연습: ${ctx.last?.let { "${it.title} ${ctx.lastMode?.label ?: ""}".trim() } ?: "없음"}.")
    }

    /** 이력(코치 = assistant 의 JSON, 운전자 = user) + 새 글. 최근 [DIALOG_HISTORY] 줄만. */
    fun dialogMessages(history: List<CoachTurn>, utterance: String): List<ChatMessage> =
        history.takeLast(DIALOG_HISTORY).map { t ->
            if (t.fromDriver) ChatMessage("user", t.text)
            else ChatMessage("assistant", """{"say":${JsonLite.quote(t.text)},"intent":"ASK_MORE"}""")
        } + ChatMessage("user", utterance)

    /** 응답 JSON → 검증된 [CoachReply]. 형식·문장·의도 중 하나라도 틀리면 null(→ 규칙). 코드 펜스·앞뒤 말은 무시한다. */
    fun parseReply(raw: String, ctx: CoachContext): CoachReply? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val json = raw.substring(start, end + 1)
        val say = JsonLite.string(json, "say")?.let { validateSay(it) } ?: return null
        fun key(k: String) = JsonLite.string(json, k)?.trim()?.uppercase()
        val intent: CoachIntent = when (key("intent")) {
            "OPEN_SHEET" -> TaskType.entries.firstOrNull { it.name == key("category") }?.let { CoachIntent.OpenSheet(it) }
            "PIN_TASK" -> {
                val id = JsonLite.string(json, "task")?.trim()
                val mode = LessonMode.entries.firstOrNull { it.name == key("mode") }
                if (id != null && mode != null) CoachIntent.PinTask(id, mode) else null
            }
            "BOOKING" -> BookingOption.entries.firstOrNull { it.name == key("option") }?.let { CoachIntent.Booking(it) }
            "SHOW_BOOKING" -> CoachIntent.ShowBooking
            "CONTINUE_LAST" -> CoachIntent.ContinueLast
            "OPEN_PROFILE" -> CoachIntent.OpenProfile
            "ASK_MORE" -> CoachIntent.AskMore
            else -> null
        } ?: return null
        if (!ctx.allows(intent)) return null
        return CoachReply(say, intent, ReplySource.AI)
    }

    /** 말풍선 문장 — [validate] 에 더해 숫자도 버린다(운전자 문장 규칙). 줄바꿈은 공백으로. */
    fun validateSay(raw: String): String? =
        validate(raw, DIALOG_MAX_CHARS)?.replace('\n', ' ')?.takeIf { s -> s.none { it.isDigit() } }

    /** 빈 값·너무 긴 것·금지어·줄바꿈 여러 줄은 버린다. 앞뒤 따옴표·공백은 정리. */
    fun validate(raw: String, maxChars: Int): String? {
        val text = raw.trim().trim('"', '“', '”', '\'').trim()
        if (text.isBlank()) return null
        if (text.length > maxChars) return null
        if (text.lines().size > 2) return null
        if (BANNED.any { it in text }) return null
        return text
    }

    private fun signed(n: Int): String = if (n > 0) "+$n" else n.toString()
}
