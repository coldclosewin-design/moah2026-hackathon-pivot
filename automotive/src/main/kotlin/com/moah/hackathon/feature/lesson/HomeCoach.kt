package com.moah.hackathon.feature.lesson

import com.moah.hackathon.ports.withObjectParticle

// ───────── 홈 코치 텍스트 대화 (사내 피드백 #4 기능 요청, 10/6 — docs/design/12_home_coach_dialog.md) ─────────
// 운전자가 글로 말을 걸면 코치가 **문장 하나 + 안내 의도 하나**로 답한다. 의도는 아래 고정 목록뿐이고,
// 앱이 가진 것(READY 과제·지원 모드·예약·지난 기록) 안에서만 받아들인다 — 화면 이동은 기존 진입점이 한다(AI 경계).

/**
 * 홈 코치 입력 방식 — 관리자 "시뮬레이션 음성 입력"(라운드 25 결정 7). [OFF] = 칩만 · [CARDS] = 말 카드만(키보드가 없는 사내 에뮬 — 입력 칸을 접는다) ·
 * [CARDS_AND_TEXT] = 말 카드 + 글 입력 칸. 어느 쪽이든 STT 자리를 대신하는 시뮬레이션이라 화면은 "음성 입력 · 시뮬레이션" 을 단다.
 */
enum class CoachInputMode { OFF, CARDS, CARDS_AND_TEXT;
    val on: Boolean get() = this != OFF
    val textField: Boolean get() = this == CARDS_AND_TEXT
}

/** 말 카드가 보이는 대화 단계 — 첫 화면(감정·인사) / 코치가 한 번 되물은 뒤(과제·상황). */
enum class CardStage { OPENING, FOLLOW_UP }

/** 카드가 보이려면 필요한 것 — 예약이 있어야 / 이어서 할 지난 기록이 있어야. */
enum class CardNeed { NONE, BOOKING, LAST }

/** 말 카드 한 장 — 누르면 [text] 가 운전자 말로 보내진다. 시드는 `data/SpeechCards.kt`. */
data class SpeechCard(val id: String, val text: String, val stage: CardStage, val needs: CardNeed = CardNeed.NONE)

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
    const val ASK_LINE = "어느 쪽이 궁금하세요?\n주차·주행·점검·지식 중에 골라 주세요."
    const val WORRY_LINE = "어디가 가장 걱정돼요?\n주차인지 도로인지 알려 주세요."
    const val MOVING_LINE = "세운 뒤에 이야기해요."
    const val NEAR_MISS_LINE = "많이 놀라셨죠.\n주차할 때였나요, 달릴 때였나요?"
    /** "가르치다 싸웠어요" — 이 앱의 이야기("화내지 않는 조수석") 그대로 받는다. */
    const val TEACH_LINE = "저는 화내지 않아요.\n어떤 게 제일 어려웠어요?"
    const val FIRST_LINE = "천천히 같이 가요.\n시동 켜는 순서부터 볼까요?"
    const val START_LINE = "점검부터 해요.\n가이드로 천천히 할까요?"
    const val SHAKY_LINE = "떨리는 게 당연해요.\n어떤 순간이 제일 떨려요?"
    const val SHORT_LINE = "좋아요, 짧게 해요.\n주차 하나만 해 볼까요?"
    const val KID_LINE = "주차부터 해 볼까요?\n아이를 태우려면 주차가 편해야 해요."
    const val RUSTY_LINE = "감은 금방 돌아와요.\n쉬운 것부터 해 볼까요?"
    const val HONK_LINE = "급할 것 없어요.\n어디서 그럴 때가 많아요?"
    const val PILLAR_LINE = "기둥은 다들 무서워요.\n앞으로 넣기, 뒤로 넣기 중 뭐가요?"

    /** 과제 제목 → 그 과제를 가리키는 말. 제목이 시드에 없으면 그 줄은 쓰이지 않는다. */
    private val TASK_WORDS: List<Pair<String, List<String>>> = listOf(
        "평행 주차" to listOf("평행"),
        "사선 주차" to listOf("사선"),
        "전면 직각 주차" to listOf("전면", "앞으로주차", "전진주차"),
        "후면 직각 주차" to listOf("후면", "후진", "뒤로주차", "직각주차"),
        "장내기능 모의시험" to listOf("장내", "기능시험", "모의시험"),
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

        if (has("프로필", "내정보", "나에대해", "연차", "목표")) return CoachReply("프로필을 열어 둘게요.\n바뀐 게 있으면 골라 주세요.", CoachIntent.OpenProfile)

        if (has("이어서", "지난번", "저번", "하던")) {
            val last = ctx.last
            if (last != null && ctx.allows(CoachIntent.ContinueLast)) return CoachReply("좋아요.\n지난번 ${last.title.withObjectParticle()} 이어서 올려 둘게요.", CoachIntent.ContinueLast)
        }

        if (ctx.bookingVenue != null) {
            val mock = CoachIntent.Booking(BookingOption.MOCK_EXAM)
            val practice = CoachIntent.Booking(BookingOption.COURSE_PRACTICE)
            if (has("모의시험", "시험보", "실전") && ctx.allows(mock)) return CoachReply("좋아요.\n예약한 코스로 모의시험을 준비할게요.", mock)
            if (has("코스연습", "예약한코스", "시험장연습") && ctx.allows(practice)) return CoachReply("좋아요.\n예약한 코스 연습을 올려 둘게요.", practice)
            if (has("예약", "시험장")) return CoachReply("예약을 띄워 둘게요.\n${ctx.bookingVenue} 카드를 확인해 주세요.", CoachIntent.ShowBooking)
        }

        val task = ctx.tasks.firstOrNull { it.title.replace(" ", "") in u }
            ?: TASK_WORDS.firstNotNullOfOrNull { (title, words) -> ctx.tasks.firstOrNull { it.title == title }?.takeIf { has(*words.toTypedArray()) } }
        if (task != null) {
            val asked = MODE_WORDS.firstOrNull { (_, words) -> has(*words.toTypedArray()) }?.first
            val mode = asked?.takeIf { it in task.modes } ?: task.suggested
            val pin = CoachIntent.PinTask(task.id, mode)
            if (ctx.allows(pin)) {
                val say = if (mode == LessonMode.QUIZ) "좋아요.\n${task.title.withObjectParticle()} 홈에 올려 둘게요."
                    else "좋아요.\n${task.title.withObjectParticle()} ${mode.label} 모드로 올려 둘게요."
                return CoachReply(say, pin)
            }
        }

        // 말 카드(라운드 25): "뭐부터" → 지금 추천하는 첫 주차 과제를 그 모드로
        if (has("뭐부터", "무엇부터", "추천해")) {
            val first = ctx.tasks.firstOrNull { it.type == TaskType.PARKING } ?: ctx.tasks.firstOrNull()
            if (first != null) {
                val pin = CoachIntent.PinTask(first.id, first.suggested)
                if (ctx.allows(pin)) return CoachReply("이것부터 해 봐요.\n${first.title.withObjectParticle()} ${first.suggested.label} 모드로 올려 둘게요.", pin)
            }
        }
        // 모드만 말했으면(과제 없음) 지금 추천 과제를 그 모드로 — "힌트만 주세요"
        MODE_WORDS.firstOrNull { (_, words) -> has(*words.toTypedArray()) }?.first?.let { mode ->
            val first = ctx.tasks.firstOrNull { mode in it.modes }
            val pin = first?.let { CoachIntent.PinTask(it.id, mode) }
            if (first != null && pin != null && !has("처음") && ctx.allows(pin))
                return CoachReply("좋아요.\n${first.title.withObjectParticle()} ${mode.label} 모드로 올려 둘게요.", pin)
        }
        if (has("싸웠", "화내", "혼났", "가르치")) return CoachReply(TEACH_LINE, CoachIntent.AskMore)
        if (has("처음")) return CoachReply(FIRST_LINE, CoachIntent.AskMore)
        if (has("떨려", "손이")) return CoachReply(SHAKY_LINE, CoachIntent.AskMore)
        if (has("시동", "헷갈")) return CoachReply(START_LINE, CoachIntent.AskMore)
        if (has("짧게", "조금만")) return CoachReply(SHORT_LINE, CoachIntent.AskMore)
        if (has("긁", "부딪", "뻔했")) return CoachReply(NEAR_MISS_LINE, CoachIntent.AskMore)
        if (has("등하원", "아이")) return CoachReply(KID_LINE, CoachIntent.AskMore)
        if (has("감을", "되찾")) return CoachReply(RUSTY_LINE, CoachIntent.AskMore)
        if (has("빵빵", "재촉", "당황")) return CoachReply(HONK_LINE, CoachIntent.AskMore)
        if (has("기둥")) return CoachReply(PILLAR_LINE, CoachIntent.AskMore)

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
            return CoachReply("과제를 펼쳐 둘게요.\n$name 중에 골라 주세요.", CoachIntent.OpenSheet(category))
        }

        if (has("무서", "걱정", "떨려", "긴장", "오랜만", "자신없", "모르겠")) return CoachReply(WORRY_LINE, CoachIntent.AskMore)
        return CoachReply(ASK_LINE, CoachIntent.AskMore)
    }
}
