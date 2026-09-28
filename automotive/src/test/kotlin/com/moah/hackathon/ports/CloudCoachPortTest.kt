package com.moah.hackathon.ports

import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.RemarkPool
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.vehicle.SignalRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CloudCoachPortTest {
    private val profile = SeedCatalog.demoProfile
    private val fallback = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(5)))

    private fun score(): ParkingScore {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in ParkingScenarios.bad.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return r.score()!!
    }

    private fun transport(block: suspend (String, String) -> String) = object : CoachTransport {
        override suspend fun complete(system: String, user: String) = block(system, user)
    }

    @Test
    fun `companion note uses a two-line cloud answer and falls back otherwise`() = runTest {
        val attempts = listOf(AttemptRecord(1, SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT, score(), null, "서두.\n조언.", 0))
        var captured = ""
        val good = CloudCoachPort(fallback, transport { _, user -> captured = user; "오늘은 끝까지 해냈어요. 다음엔 출발 전에 벨트를 같이 봐 주세요." })
        val note = good.companionNote(SeedCatalog.parkingTask, attempts, profile)
        assertEquals("오늘은 끝까지 해냈어요.", note.praise)
        assertEquals("다음엔 출발 전에 벨트를 같이 봐 주세요.", note.help)
        assertTrue(captured, captured.contains("동승자") && captured.contains("벨트"))

        val oneLine = CloudCoachPort(fallback, transport { _, _ -> "한 문장뿐이에요" })
        assertEquals(fallback.companionNote(SeedCatalog.parkingTask, attempts, profile).help, oneLine.companionNote(SeedCatalog.parkingTask, attempts, profile).help)
        val banned = CloudCoachPort(fallback, transport { _, _ -> "실패했어요. 다음엔 잘해 주세요." })
        assertFalse(banned.companionNote(SeedCatalog.parkingTask, attempts, profile).text.contains("실패"))
        val none = CloudCoachPort(fallback, transport = null)
        assertTrue(none.companionNote(SeedCatalog.parkingTask, emptyList(), profile).praise.isNotBlank())
    }

    @Test
    fun `no transport falls back to the seed pool with the numeric head`() = runTest {
        val coach = CloudCoachPort(fallback, transport = null)
        val text = coach.remark(SeedCatalog.parkingTask, score(), null, profile, 1)
        assertTrue(text, !text.contains("4번") && text.contains("\n"))
    }

    @Test
    fun `a good cloud answer is used with the head prepended and the prompt carries profile and process`() = runTest {
        var captured = ""
        val coach = CloudCoachPort(fallback, transport { _, user -> captured = user; "문고리는 잡았어요. 다음엔 열어 봅시다." })
        val text = coach.remark(SeedCatalog.parkingTask, score(), null, profile, 1)
        assertEquals("문고리는 잡았어요.\n다음엔 열어 봅시다.", text)   // 머리말 없음, 문장 단위 줄바꿈
        assertTrue(captured, captured.contains("장롱면허 10년"))
        assertTrue(captured.contains("이동 4회"))
        assertTrue(captured.contains("조향 되돌림 3회"))
        assertTrue(captured.contains("참고 문장"))
    }

    @Test
    fun `exceptions timeouts and rejected answers all fall back`() = runTest {
        val s = score()
        val boom = CloudCoachPort(fallback, transport { _, _ -> throw IllegalStateException("401") })
        assertTrue(boom.remark(SeedCatalog.parkingTask, s, null, profile, 1).contains("\n"))

        val slow = CloudCoachPort(fallback, transport { _, _ -> delay(10_000); "늦은 답" }, timeoutMillis = 1_000)
        assertTrue(slow.remark(SeedCatalog.parkingTask, s, null, profile, 1).contains("\n"))
        assertFalse(slow.remark(SeedCatalog.parkingTask, s, null, profile, 1).contains("늦은 답"))

        val rude = CloudCoachPort(fallback, transport { _, _ -> "하위 20% 운전자입니다." })
        assertFalse(rude.remark(SeedCatalog.parkingTask, s, null, profile, 1).contains("하위"))

        val verbose = CloudCoachPort(fallback, transport { _, _ -> "아".repeat(200) })
        assertTrue(verbose.remark(SeedCatalog.parkingTask, s, null, profile, 1).length < 120)
    }

    @Test
    fun `checklist remarks get their own head, prompt and seed pool`() = runTest {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in ChecklistScenarios.bad.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        val s = r.scoreChecklist()!!
        var captured = ""
        val coach = CloudCoachPort(fallback, transport { _, user -> captured = user; "벨트가 먼저, 시동은 그다음이에요." })
        assertEquals("벨트가 먼저, 시동은 그다음이에요.", coach.remark(SeedCatalog.predriveTask, s, null, profile, 1))
        assertTrue(captured, captured.contains("과제: 출발 전 점검"))
        assertTrue(captured.contains("순서 시동 먼저(순서 바뀜)"))
        assertTrue(captured.contains("움직임 1회"))
        assertFalse(captured.contains("조향 되돌림"))
        val seed = CloudCoachPort(fallback, transport = null).remark(SeedCatalog.predriveTask, s, null, profile, 1)
        assertTrue(seed, !seed.contains("9초") && !seed.contains("들어갔") && seed.endsWith("다음엔 벨트가 먼저, 시동은 그다음이에요."))
    }

    @Test
    fun `validate trims quotes, rejects blanks, length, banned words and multi-line essays`() {
        assertEquals("좋아요.", CoachPrompts.validate("  \"좋아요.\"  ", 60))
        assertNull(CoachPrompts.validate("   ", 60))
        assertNull(CoachPrompts.validate("가".repeat(61), 60))
        assertNull(CoachPrompts.validate("실패했어요", 60))
        assertNull(CoachPrompts.validate("한 줄\n두 줄\n세 줄", 60))
        assertEquals("한 줄\n두 줄", CoachPrompts.validate("한 줄\n두 줄", 60))
    }

    @Test
    fun `summary uses the cloud when it answers and the fallback when attempts are empty`() = runTest {
        val s = score()
        val record = AttemptRecord(1, SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT, s, null, "…", 0L)
        val coach = CloudCoachPort(fallback, transport { _, user -> assertTrue(user.contains("1회차: 숙련 60")); "오늘은 여기까지 잘 왔어요. 뒤 거리만 조금 더." })
        assertEquals("오늘은 여기까지 잘 왔어요. 뒤 거리만 조금 더.", coach.summarize(SeedCatalog.parkingTask, LessonMode.HINT, listOf(record), profile))
        assertTrue(coach.summarize(SeedCatalog.parkingTask, LessonMode.HINT, emptyList(), profile).contains("움직이지 않았어요"))
    }
}
