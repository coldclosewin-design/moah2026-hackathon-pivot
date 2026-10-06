package com.moah.hackathon.feature.lesson

import com.moah.hackathon.ports.withObjectParticle

// ───────── 홈 코치 텍스트 대화 (사내 피드백 #4 기능 요청, 10/6 — docs/design/12_home_coach_dialog.md) ─────────
// 운전자가 글로 말을 걸면 코치가 **문장 하나 + 안내 의도 하나**로 답한다. 의도는 아래 고정 목록뿐이고,
// 앱이 가진 것(READY 과제·지원 모드·예약·지난 기록) 안에서만 받아들인다 — 화면 이동은 기존 진입점이 한다(AI 경계).

/** 대화 한 줄 — 운전자의 글 또는 코치의 말풍선. 시트를 닫으면 버린다(저장·반출 없음). */
data class CoachTurn(val fromDriver: Boolean, val text: String)

/** 코치 응답이 고를 수 있는 안내. 결과는 새 화면이 아니라 기존 홈·시트·카드다. */
sealed interface CoachIntent {
    /** 과제 시트를 그 분류로 연다(`sheetRequest`). */
    data class OpenSheet(val category: TaskType) : CoachIntent
    /** 홈 제안을 그 과제·모드로 고정한다(`pinned`). */
    data class PinTask(val taskId: String, val mode: LessonMode) : CoachIntent
    /** 예약 카드의 `코스 연습`·`모의시험`(`chooseBooking`). 예약이 있을 때만. */
    data class Booking(val option: BookingOption) : CoachIntent
    /** 예약 카드 강조(`highlightBooking`). 예약이 있을 때만. */
    data object ShowBooking : CoachIntent
    /** 지난 회차의 과제·모드로 고정. 기록이 있을 때만. */
    data object ContinueLast : CoachIntent
    /** 홈 눈썹의 프로필 시트를 연다(`profileRequest`). */
    data object OpenProfile : CoachIntent
    /** 아직 모르겠다 — 시트를 열어 둔 채 한 가지만 되묻는다. */
    data object AskMore : CoachIntent

    /** 로그·프롬프트용 이름 — `PIN_TASK(rear-parking,HINT)`. */
    val code: String get() = when (this) {
        is OpenSheet -> "OPEN_SHEET($category)"
        is PinTask -> "PIN_TASK($taskId,$mode)"
        is Booking -> "BOOKING($option)"
        ShowBooking -> "SHOW_BOOKING"
        ContinueLast -> "CONTINUE_LAST"
        OpenProfile -> "OPEN_PROFILE"
        AskMore -> "ASK_MORE"
    }
}

/** 응답이 어디서 왔나 — 로그 `home coach: intent=… (ai|rule|fallback)`. */
enum class ReplySource { AI, RULE, FALLBACK;
    val log: String get() = name.lowercase()
}

/** 코치 응답 — 말풍선·TTS 문장(숫자 없음) + 안내 의도. */
data class CoachReply(val say: String, val intent: CoachIntent, val source: ReplySource = ReplySource.RULE)

/** 대화가 고를 수 있는 과제 한 줄 — READY 만, 모드는 그 과제가 지원하는 것, [suggested] 는 지금 기록으로 본 추천. */
data class TaskOption(val id: String, val title: String, val type: TaskType, val modes: List<LessonMode>, val suggested: LessonMode)

/** 대화의 상황 — 프롬프트와 의도 검증이 같이 쓴다. */
data class CoachContext(
    val profile: Profile,
    val tasks: List<TaskOption>,
    /** 예약이 있고 예약 과제가 READY 일 때 시험장 이름. 아니면 null. */
    val bookingVenue: String? = null,
    val bookingOptions: List<BookingOption> = emptyList(),
    /** 지난 회차를 이어서 할 수 있을 때 그 과제. */
    val last: TaskOption? = null,
    val lastMode: LessonMode? = null,
) {
    val categories: List<TaskType> get() = TaskType.entries.filter { c -> tasks.any { it.type == c } }

    /** 앱이 가진 것 안의 의도인가. 아니면 버린다(AI 응답이든 규칙이든). */
    fun allows(intent: CoachIntent): Boolean = when (intent) {
        is CoachIntent.OpenSheet -> intent.category in categories
        is CoachIntent.PinTask -> tasks.any { it.id == intent.taskId && intent.mode in it.modes }
        is CoachIntent.Booking -> bookingVenue != null && intent.option in bookingOptions
        CoachIntent.ShowBooking -> bookingVenue != null
        CoachIntent.ContinueLast -> last != null && lastMode != null
        CoachIntent.OpenProfile, CoachIntent.AskMore -> true
    }
}

/**
 * Fake 코치의 대화 — **키워드 규칙**(인터넷 없이 시연이 끝나야 한다). AI 가 실패했을 때의 폴백도 이것이다.
 * 결과는 언제나 [CoachContext.allows] 를 통과한다. 문장에 숫자를 넣지 않는다.
 */
object IntentRules {
    const val ASK_LINE = "주차, 도로 주행, 출발 전 점검, 지식 중에 어느 쪽이 궁금하세요? 아래에서 바로 골라도 돼요."
    const val WORRY_LINE = "어떤 순간이 가장 걱정되세요? 주차인지 도로인지 말해 주시면 골라 드릴게요."
    const val MOVING_LINE = "차를 세운 뒤에 이야기해요."

    /** 과제 제목 → 그 과제를 가리키는 말. 제목이 시드에 없으면 그 줄은 쓰이지 않는다. */
    private val TASK_WORDS: List<Pair<String, List<String>>> = listOf(
        "평행 주차" to listOf("평행"),
        "사선 주차" to listOf("사선"),
        "전면 직각 주차" to listOf("전면", "앞으로주차", "전진주차"),
        "후면 직각 주차" to listOf("후면", "후진주차", "뒤로주차", "직각주차"),
        "장내기능 모의시험" to listOf("장내", "기능시험"),
        "회전교차로" to listOf("회전교차로", "로터리"),
        "차선 변경" to listOf("차선"),
        "좌회전 방향지시등" to listOf("좌회전", "깜빡이"),
        "출발 전 점검" to listOf("점검", "출발전"),
        "비상등·날씨별 행동" to listOf("비상등", "날씨"),
        "일반 도로 코스" to listOf("일반도로", "도로코스", "어린이보호구역"),
        "단순 전진 후 정지" to listOf("정지선", "멈추는"),
    )

    private val MODE_WORDS: List<Pair<LessonMode, List<String>>> = listOf(
        LessonMode.GUIDE to listOf("가이드", "차근차근", "알려", "처음부터"),
        LessonMode.HINT to listOf("힌트"),
        LessonMode.EVALUATE to listOf("평가", "시험처럼", "혼자", "조용히"),
    )

    fun reply(utterance: String, ctx: CoachContext): CoachReply {
        val u = utterance.replace(" ", "")
        fun has(vararg words: String) = words.any { it.replace(" ", "") in u }

        if (has("프로필", "내정보", "나에대해", "연차", "목표")) return CoachReply("프로필을 열어 둘게요. 바뀐 게 있으면 골라 주세요.", CoachIntent.OpenProfile)

        if (has("이어서", "지난번", "저번", "하던")) {
            val last = ctx.last
            if (last != null && ctx.allows(CoachIntent.ContinueLast)) return CoachReply("지난번 ${last.title.withObjectParticle()} 이어서 할 수 있게 올려 둘게요.", CoachIntent.ContinueLast)
        }

        if (ctx.bookingVenue != null) {
            val mock = CoachIntent.Booking(BookingOption.MOCK_EXAM)
            val practice = CoachIntent.Booking(BookingOption.COURSE_PRACTICE)
            if (has("모의시험", "시험보", "실전") && ctx.allows(mock)) return CoachReply("예약한 코스로 모의시험을 준비해 둘게요.", mock)
            if (has("코스연습", "예약한코스") && ctx.allows(practice)) return CoachReply("예약한 코스를 연습할 수 있게 올려 둘게요.", practice)
            if (has("예약", "시험장")) return CoachReply("예약한 ${ctx.bookingVenue} 카드를 표시해 둘게요.", CoachIntent.ShowBooking)
        }

        val task = ctx.tasks.firstOrNull { it.title.replace(" ", "") in u }
            ?: TASK_WORDS.firstNotNullOfOrNull { (title, words) -> ctx.tasks.firstOrNull { it.title == title }?.takeIf { has(*words.toTypedArray()) } }
        if (task != null) {
            val asked = MODE_WORDS.firstOrNull { (_, words) -> has(*words.toTypedArray()) }?.first
            val mode = asked?.takeIf { it in task.modes } ?: task.suggested
            val pin = CoachIntent.PinTask(task.id, mode)
            if (ctx.allows(pin)) {
                val say = if (mode == LessonMode.QUIZ) "${task.title.withObjectParticle()} 홈에 올려 둘게요. 정차 중에 풀어요."
                    else "${task.title.withObjectParticle()} ${mode.label} 모드로 홈에 올려 둘게요."
                return CoachReply(say, pin)
            }
        }

        val category = when {
            has("주차") -> TaskType.PARKING
            has("주행", "도로", "운전") -> TaskType.DRIVING
            has("지식", "문제", "퀴즈", "공부") -> TaskType.KNOWLEDGE
            else -> null
        }
        if (category != null && ctx.allows(CoachIntent.OpenSheet(category))) {
            val name = when (category) {
                TaskType.PARKING -> "주차"; TaskType.DRIVING -> "도로 주행"; TaskType.KNOWLEDGE -> "지식"; TaskType.CHECKLIST -> "점검"
            }
            return CoachReply("$name 과제를 펼쳐 둘게요. 마음에 드는 걸 골라 주세요.", CoachIntent.OpenSheet(category))
        }

        if (has("무서", "걱정", "떨려", "긴장", "오랜만", "자신없", "모르겠")) return CoachReply(WORRY_LINE, CoachIntent.AskMore)
        return CoachReply(ASK_LINE, CoachIntent.AskMore)
    }
}
