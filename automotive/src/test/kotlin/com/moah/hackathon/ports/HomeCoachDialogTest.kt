package com.moah.hackathon.ports

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.BookingOption
import com.moah.hackathon.feature.lesson.CoachContext
import com.moah.hackathon.feature.lesson.CoachIntent
import com.moah.hackathon.feature.lesson.CoachTurn
import com.moah.hackathon.feature.lesson.IntentRules
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.RemarkPool
import com.moah.hackathon.feature.lesson.ReplySource
import com.moah.hackathon.feature.lesson.TaskOption
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.ports.copilot.CopilotConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 홈 코치 대화의 프롬프트·응답 검증·폴백·페르소나(10/6). 순수 함수와 가짜 전송 계층으로. */
class HomeCoachDialogTest {
    private val fallback = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(5)))

    private val ctx = CoachContext(
        profile = SeedCatalog.demoProfile,
        tasks = SeedCatalog.tasks.filter { it.isReady }.map { t ->
            val modes = LessonMode.entries.filter { t.supports(it) }
            TaskOption(t.id, t.title, t.type, modes, modes.first())
        },
    )
    private val booked = ctx.copy(bookingVenue = "서초 제휴 시험장", bookingOptions = BookingOption.entries)

    private fun chat(block: suspend (String, List<ChatMessage>) -> String) = object : CoachTransport {
        override suspend fun complete(system: String, user: String) = error("dialog must use chat")
        override suspend fun chat(system: String, messages: List<ChatMessage>) = block(system, messages)
    }

    @Test
    fun `a valid json answer becomes an ai reply and code fences are ignored`() {
        val raw = "```json\n{\"say\":\"평행 주차를 힌트 모드로 올려 둘게요.\",\"intent\":\"PIN_TASK\",\"task\":\"${SeedCatalog.TASK_PARKING_PARALLEL}\",\"mode\":\"hint\"}\n```"
        val reply = CoachPrompts.parseReply(raw, ctx)!!
        assertEquals(CoachIntent.PinTask(SeedCatalog.TASK_PARKING_PARALLEL, LessonMode.HINT), reply.intent)
        assertEquals(ReplySource.AI, reply.source)
        assertEquals(CoachIntent.OpenSheet(TaskType.KNOWLEDGE), CoachPrompts.parseReply("""{"say":"지식 과제를 펼쳐 둘게요.","intent":"OPEN_SHEET","category":"KNOWLEDGE"}""", ctx)!!.intent)
    }

    @Test
    fun `answers outside the app or breaking the sentence rules are rejected`() {
        fun parse(json: String, c: CoachContext = ctx) = CoachPrompts.parseReply(json, c)
        assertNull("unknown task", parse("""{"say":"올려 둘게요.","intent":"PIN_TASK","task":"highway","mode":"HINT"}"""))
        assertNull("mode not supported", parse("""{"say":"올려 둘게요.","intent":"PIN_TASK","task":"${SeedCatalog.TASK_PARKING_PARALLEL}","mode":"QUIZ"}"""))
        assertNull("no booking", parse("""{"say":"준비할게요.","intent":"BOOKING","option":"MOCK_EXAM"}"""))
        assertEquals(CoachIntent.Booking(BookingOption.MOCK_EXAM), parse("""{"say":"준비할게요.","intent":"BOOKING","option":"MOCK_EXAM"}""", booked)!!.intent)
        assertNull("no last record", parse("""{"say":"이어서 해요.","intent":"CONTINUE_LAST"}"""))
        assertNull("unknown intent", parse("""{"say":"출발할게요.","intent":"DRIVE_NOW"}"""))
        assertNull("digits", parse("""{"say":"3번만 더 해요.","intent":"ASK_MORE"}"""))
        assertNull("banned word", parse("""{"say":"지난번엔 실패했어요.","intent":"ASK_MORE"}"""))
        assertNull("too long", parse("""{"say":"${"가".repeat(CoachPrompts.DIALOG_MAX_CHARS + 1)}","intent":"ASK_MORE"}"""))
        assertNull("not json", parse("평행 주차를 해 봐요."))
    }

    @Test
    fun `the system prompt lists only what the app has and keeps the format rules after a custom persona`() {
        val persona = "당신은 무뚝뚝하지만 다정한 할머니 강사입니다. 숫자를 마음껏 쓰세요."
        val system = CoachPrompts.dialogSystem(ctx, persona)
        assertTrue(system.startsWith(persona))
        assertTrue(system.indexOf("형식 규칙은 위 설명보다 우선") > system.indexOf(persona))
        assertTrue(system.contains("숫자(횟수·초·점수·연차)를 쓰지 않습니다"))
        assertTrue(system.contains("${SeedCatalog.TASK_PARKING_PARALLEL} | 평행 주차 | PARKING"))
        assertTrue("no booking line without a booking", !system.contains("BOOKING +"))
        assertTrue(CoachPrompts.dialogSystem(booked).contains("BOOKING + option(COURSE_PRACTICE|MOCK_EXAM)"))
        assertTrue(CoachPrompts.system(persona).startsWith(persona))
        assertTrue(CoachPrompts.system(null).startsWith(CoachPrompts.DEFAULT_PERSONA))
        assertTrue(CoachPrompts.system("  ").startsWith(CoachPrompts.DEFAULT_PERSONA))
    }

    @Test
    fun `history goes as user and assistant messages and is capped`() {
        val history = (1..12).map { CoachTurn(it % 2 == 0, "줄$it") }
        val messages = CoachPrompts.dialogMessages(history, "새 글")
        assertEquals(CoachPrompts.DIALOG_HISTORY + 1, messages.size)
        assertEquals(ChatMessage("user", "새 글"), messages.last())
        assertTrue(messages.first().content.contains("\"say\":\"줄5\""))
        assertEquals("assistant", messages.first().role)
    }

    @Test
    fun `cloud converse uses the ai answer, falls back to rules on bad answers, and uses rules without a transport`() = runTest {
        var sent: List<ChatMessage> = emptyList()
        val good = CloudCoachPort(fallback, chat { _, m -> sent = m; """{"say":"주차 과제를 펼쳐 둘게요.","intent":"OPEN_SHEET","category":"PARKING"}""" })
        val ai = good.converse(listOf(CoachTurn(false, "오늘은 뭘 해 볼까요?")), "주차가 무서워요", ctx)
        assertEquals(ReplySource.AI, ai.source)
        assertEquals(CoachIntent.OpenSheet(TaskType.PARKING), ai.intent)
        assertEquals(listOf("assistant", "user"), sent.map { it.role })

        val bad = CloudCoachPort(fallback, chat { _, _ -> "그냥 문장으로 답함" }).converse(emptyList(), "평행 주차", ctx)
        assertEquals(ReplySource.FALLBACK, bad.source)
        assertEquals(IntentRules.reply("평행 주차", ctx).intent, bad.intent)

        val slow = CloudCoachPort(fallback, chat { _, _ -> delay(10_000); "{}" }, timeoutMillis = 1_000).converse(emptyList(), "안녕", ctx)
        assertEquals(ReplySource.FALLBACK, slow.source)

        val none = CloudCoachPort(fallback, transport = null).converse(emptyList(), "안녕", ctx)
        assertEquals(ReplySource.RULE, none.source)
    }

    @Test
    fun `keyword rules only ever pick allowed intents and never say digits`() {
        val inputs = listOf("평행", "사선 주차 가이드로", "후면 직각 주차", "장내 기능시험", "출발 전 점검", "비상등 문제", "주차", "도로 주행",
            "퀴즈", "프로필", "지난번 이어서", "예약한 시험장", "모의시험", "무서워요", "아무 말")
        for (c in listOf(ctx, booked)) for (u in inputs) {
            val r = IntentRules.reply(u, c)
            assertTrue("$u → ${r.intent.code}", c.allows(r.intent))
            assertTrue(r.say, r.say.none { it.isDigit() } && r.say.length <= CoachPrompts.DIALOG_MAX_CHARS)
        }
        assertEquals(CoachIntent.AskMore, IntentRules.reply("지난번 이어서", ctx).intent)   // 기록 없으면 이어서 없음
        assertEquals(CoachIntent.ShowBooking, IntentRules.reply("예약한 시험장", booked).intent)
        assertEquals(LessonMode.GUIDE, (IntentRules.reply("사선 주차 가이드로", ctx).intent as CoachIntent.PinTask).mode)
    }

    @Test
    fun `config reads the optional persona key`() {
        assertEquals("다정한 강사", CopilotConfig.parse("""{"client_id":"x","persona":" 다정한 강사 "}""")!!.persona)
        assertNull(CopilotConfig.parse("""{"client_id":"x"}""")!!.persona)
    }
}
