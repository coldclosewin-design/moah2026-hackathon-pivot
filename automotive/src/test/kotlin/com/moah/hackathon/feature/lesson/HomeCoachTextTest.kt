package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.ports.CoachPort
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeTtsPort
import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.vehicle.FakeVehiclePort
import com.moah.hackathon.vehicle.SignalRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 홈 코치 텍스트 대화(사내 피드백 #4 기능 요청, 10/6): 글 → 문장 + 의도 → 기존 홈·시트·카드. 입력은 관리자가 켤 때만, 정차일 때만. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeCoachTextTest {

    private class Harness(val tts: FakeTtsPort, val vehicle: FakeVehiclePort, val store: ProgressStore, val machine: LessonStateMachine, val scope: CoroutineScope)

    /** 받은 이력·글을 남기고, [reply] 가 있으면 그것으로 답하는 코치. 없으면 키워드 규칙. */
    private class ScriptedCoach(private val base: CoachPort, var reply: CoachReply? = null, var delayMillis: Long = 0) : CoachPort by base {
        val heard = ArrayList<Pair<List<CoachTurn>, String>>()
        override suspend fun converse(history: List<CoachTurn>, utterance: String, context: CoachContext): CoachReply {
            heard += history to utterance
            if (delayMillis > 0) delay(delayMillis)
            return reply ?: base.converse(history, utterance, context)
        }
    }

    private fun TestScope.harness(coach: CoachPort = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3)))): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val tts = FakeTtsPort()
        val store = ProgressStore()
        val vehicle = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val machine = LessonStateMachine(
            vehicle = vehicle, tts = tts, coach = coach,
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = store,
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues,
            benefits = SeedCatalog.benefits, profile = SeedCatalog.demoProfile, scope = scope,
            clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        return Harness(tts, vehicle, store, machine, scope)
    }

    private val Harness.setup: LessonPhase.Setup get() = machine.phase.value as LessonPhase.Setup

    private fun TestScope.say(h: Harness, text: String) { h.machine.sendCoachText(text); advanceUntilIdle() }

    @Test
    fun inputIsOffByDefaultAndIgnored() = runTest {
        val h = harness()
        assertFalse(h.setup.coachTextInput)
        h.machine.openCoach()
        say(h, "평행 주차 하고 싶어요")
        assertTrue("dialog untouched", h.setup.coach!!.turns.isEmpty())
        assertNull(h.setup.sheetRequest)
        h.scope.cancel()
    }

    @Test
    fun unclearTextAsksMoreAndKeepsTheSheetOpenWithTheChips() = runTest {
        val h = harness()
        h.machine.setCoachTextInput(true)
        assertTrue(h.setup.coachTextInput)
        h.machine.openCoach()
        say(h, "안녕하세요")
        val coach = h.setup.coach!!
        assertEquals(listOf(CoachTurn(true, "안녕하세요"), CoachTurn(false, IntentRules.ASK_LINE)), coach.turns)
        assertFalse(coach.waiting)
        assertTrue("chips stay", coach.choices.isNotEmpty())
        assertEquals(IntentRules.ASK_LINE, h.tts.lastSpoken.value)
        h.scope.cancel()
    }

    @Test
    fun aTaskByNamePinsTheHomeSuggestionAndClosesTheSheet() = runTest {
        val h = harness()
        h.machine.setCoachTextInput(true)
        h.machine.openCoach()
        say(h, "오늘은 평행 주차 힌트로 해 볼래요")
        assertNull("sheet closes", h.setup.coach)
        assertEquals(SeedCatalog.TASK_PARKING_PARALLEL, h.setup.suggestedTask.id)
        assertEquals(LessonMode.HINT, h.setup.suggestedMode)
        val spoken = h.tts.lastSpoken.value!!
        assertTrue(spoken, spoken.startsWith("평행 주차를") && spoken.none { it.isDigit() })
        h.scope.cancel()
    }

    @Test
    fun categoryProfileAndBookingLandOnTheExistingEntryPoints() = runTest {
        val h = harness()
        h.machine.setCoachTextInput(true)
        h.machine.openCoach()
        say(h, "주차가 제일 걱정이에요")
        assertEquals(TaskType.PARKING, h.setup.sheetRequest)

        h.machine.consumeSheetRequest()
        h.machine.openCoach()
        say(h, "내 프로필 고칠래요")
        assertTrue(h.setup.profileRequest)
        h.machine.consumeProfileRequest()
        assertFalse(h.setup.profileRequest)

        h.machine.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_EXAM)
        h.machine.openCoach()
        say(h, "모의시험 보고 싶어요")
        assertEquals(BookingOption.MOCK_EXAM, h.setup.bookingChoice)
        assertEquals(LessonMode.EVALUATE, h.setup.suggestedMode)
        h.scope.cancel()
    }

    @Test
    fun theCoachGetsTheOpeningLineAndEarlierTurnsAsHistory() = runTest {
        val coach = ScriptedCoach(FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))))
        val h = harness(coach)
        h.machine.setCoachTextInput(true)
        h.machine.openCoach()
        val opening = h.setup.coach!!.line
        say(h, "음")
        say(h, "주차요")
        assertEquals(2, coach.heard.size)
        assertEquals(listOf(CoachTurn(false, opening)), coach.heard[0].first)
        assertEquals(listOf(CoachTurn(false, opening), CoachTurn(true, "음"), CoachTurn(false, IntentRules.ASK_LINE)), coach.heard[1].first)
        assertEquals("주차요", coach.heard[1].second)
        h.scope.cancel()
    }

    @Test
    fun anIntentOutsideTheAppIsReplacedByAskMore() = runTest {
        val coach = ScriptedCoach(FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            reply = CoachReply("고속도로 연습을 올려 둘게요.", CoachIntent.PinTask("highway", LessonMode.HINT), ReplySource.AI))
        val h = harness(coach)
        h.machine.setCoachTextInput(true)
        h.machine.openCoach()
        say(h, "고속도로")
        assertEquals(IntentRules.ASK_LINE, h.setup.coach!!.turns.last().text)
        assertFalse(h.setup.suggestedTask.id == "highway")
        h.scope.cancel()
    }

    @Test
    fun movingCarGetsNoAnswer() = runTest {
        val coach = ScriptedCoach(FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))))
        val h = harness(coach)
        h.vehicle.set(mapOf(VssConstants.VEHICLE_SPEED to "20"))
        h.machine.setCoachTextInput(true)
        h.machine.openCoach()
        say(h, "평행 주차")
        assertTrue("coach not asked", coach.heard.isEmpty())
        assertEquals(IntentRules.MOVING_LINE, h.setup.coach!!.turns.last().text)
        assertFalse(h.setup.suggestedTask.id == SeedCatalog.TASK_PARKING_PARALLEL)
        h.scope.cancel()
    }

    @Test
    fun aReplyAfterTheSheetClosedIsDroppedAndSendIsBlockedWhileWaiting() = runTest {
        val coach = ScriptedCoach(FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))), delayMillis = 3_000)
        val h = harness(coach)
        h.machine.setCoachTextInput(true)
        h.machine.openCoach()
        h.machine.sendCoachText("평행 주차")
        testScheduler.advanceTimeBy(100)
        assertTrue(h.setup.coach!!.waiting)
        h.machine.sendCoachText("또 보냄")
        testScheduler.advanceTimeBy(100)
        assertEquals("second send ignored", 1, h.setup.coach!!.turns.size)
        h.machine.closeCoach()
        advanceUntilIdle()
        assertNull(h.setup.coach)
        assertFalse("late reply did not pin", h.setup.suggestedTask.id == SeedCatalog.TASK_PARKING_PARALLEL)
        h.scope.cancel()
    }

    @Test
    fun turningInputOffDropsTheTalkButKeepsTheSheet() = runTest {
        val h = harness()
        h.machine.setCoachTextInput(true)
        h.machine.openCoach()
        say(h, "안녕")
        h.machine.setCoachTextInput(false)
        assertFalse(h.setup.coachTextInput)
        assertTrue(h.setup.coach!!.turns.isEmpty())
        h.scope.cancel()
    }
}
