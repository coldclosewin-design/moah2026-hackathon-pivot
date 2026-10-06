package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.data.SpeechCards
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 말 카드(라운드 25 결정 7, 사내 #5): 키보드 없이 한국어 문장으로 대화 — 카드마다 Fake 규칙이 같은 의도로 간다, 단계·조건에 맞는 카드만. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechCardsTest {

    private class Harness(val store: ProgressStore, val machine: LessonStateMachine, val scope: CoroutineScope)

    private fun TestScope.harness(): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val store = ProgressStore()
        val machine = LessonStateMachine(
            vehicle = FakeVehiclePort(simulate = false, dispatcher = dispatcher), tts = FakeTtsPort(),
            coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = store,
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues,
            benefits = SeedCatalog.benefits, profile = SeedCatalog.demoProfile, scope = scope,
            clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        return Harness(store, machine, scope)
    }

    private val Harness.setup: LessonPhase.Setup get() = machine.phase.value as LessonPhase.Setup

    private fun record(task: Task, mode: LessonMode): AttemptRecord {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in ParkingScenarios.good.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return AttemptRecord(1, task.id, mode, r.score()!!, null, "서두.\n조언.", 0)
    }

    /** 예약·지난 기록이 다 있는 상황 — 모든 카드가 보인다. */
    private val full: CoachContext by lazy {
        val tasks = SeedCatalog.tasks.filter { it.isReady }.map { t ->
            val modes = LessonMode.entries.filter { t.supports(it) }
            TaskOption(t.id, t.title, t.type, modes, modes.first())
        }
        val rear = tasks.first { it.id == SeedCatalog.TASK_PARKING_REAR }
        CoachContext(SeedCatalog.demoProfile, tasks, bookingVenue = "서초 시험장", bookingOptions = BookingOption.entries,
            last = rear, lastMode = LessonMode.HINT)
    }

    @Test
    fun everyCardReachesItsIntentWithTheKeywordRules() {
        val expected: Map<String, (CoachIntent) -> Boolean> = mapOf(
            "long-time" to { it == CoachIntent.AskMore },
            "near-miss" to { it == CoachIntent.AskMore },
            "what-first" to { it is CoachIntent.PinTask && it.taskId == SeedCatalog.TASK_PARKING_REAR },
            "kid-school" to { it == CoachIntent.AskMore },
            "parallel-hint" to { it == CoachIntent.PinTask(SeedCatalog.TASK_PARKING_PARALLEL, LessonMode.HINT) },
            "rear-again" to { it is CoachIntent.PinTask && it.taskId == SeedCatalog.TASK_PARKING_REAR },
            "mock-exam" to { it == CoachIntent.Booking(BookingOption.MOCK_EXAM) },
            "venue-practice" to { it == CoachIntent.Booking(BookingOption.COURSE_PRACTICE) },
            "continue-last" to { it == CoachIntent.ContinueLast },
            "road" to { it == CoachIntent.OpenSheet(TaskType.DRIVING) },
            "profile" to { it == CoachIntent.OpenProfile },
        )
        assertEquals(expected.keys, SpeechCards.all.map { it.id }.toSet())
        for (card in SpeechCards.all) {
            val reply = IntentRules.reply(card.text, full)
            assertTrue("${card.id} → ${reply.intent.code}", expected.getValue(card.id)(reply.intent))
            assertTrue(card.text, card.text.none { it.isDigit() } && '\n' !in card.text)
            assertTrue(reply.say, reply.say.none { it.isDigit() })
        }
        // 예약이 없으면 "모의시험 볼래요" 는 장내기능 모의시험 과제로
        val noBooking = IntentRules.reply("모의시험 볼래요", full.copy(bookingVenue = null, bookingOptions = emptyList())).intent
        assertTrue(noBooking.code, noBooking is CoachIntent.PinTask && noBooking.taskId == SeedCatalog.TASK_TRACK_EXAM)
        // 감정 카드의 되물음은 서로 다르다(같은 줄 반복이 아니게)
        assertEquals(3, listOf("long-time", "near-miss", "kid-school").map { id -> IntentRules.reply(SpeechCards.all.first { it.id == id }.text, full).say }.toSet().size)
    }

    @Test
    fun cardsAppearOnlyWithInputOnAndFollowTheDialogStage() = runTest {
        val h = harness()
        h.machine.openCoach()
        assertTrue("input off → no cards", h.setup.coach!!.cards.isEmpty())
        h.machine.setCoachInput(CoachInputMode.CARDS)
        assertEquals(CoachInputMode.CARDS, h.setup.coachInput)
        assertTrue(h.setup.coachTextInput)
        assertEquals(SpeechCards.all.filter { it.stage == CardStage.OPENING }.map { it.id }, h.setup.coach!!.cards.map { it.id })

        h.machine.sendCoachCard("kid-school"); advanceUntilIdle()
        val coach = h.setup.coach!!
        assertEquals(listOf(CoachTurn(true, "아이 등하원 때문에 배워요"), CoachTurn(false, IntentRules.KID_LINE)), coach.turns)
        val follow = coach.cards.map { it.id }
        assertTrue(follow.toString(), "parallel-hint" in follow && "long-time" !in follow)
        assertFalse("no booking → no venue card", "venue-practice" in follow)
        assertFalse("no records → no continue card", "continue-last" in follow)

        h.machine.sendCoachCard("parallel-hint"); advanceUntilIdle()
        assertNull("sheet closes", h.setup.coach)
        assertEquals(SeedCatalog.TASK_PARKING_PARALLEL, h.setup.suggestedTask.id)
        assertEquals(LessonMode.HINT, h.setup.suggestedMode)
        h.scope.cancel()
    }

    @Test
    fun bookingAndRecordsUnlockTheirCardsAndUnknownCardsAreIgnored() = runTest {
        val h = harness()
        h.machine.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_EXAM)
        h.store.add(record(SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_PARKING_REAR }, LessonMode.HINT))
        h.machine.setCoachInput(CoachInputMode.CARDS_AND_TEXT)
        h.machine.openCoach()
        h.machine.sendCoachCard("parallel-hint"); advanceUntilIdle()   // 첫 화면에 없는 카드
        assertTrue("not offered → ignored", h.setup.coach!!.turns.isEmpty())
        h.machine.sendCoachCard("long-time"); advanceUntilIdle()
        val ids = h.setup.coach!!.cards.map { it.id }
        assertTrue(ids.toString(), "venue-practice" in ids && "continue-last" in ids)
        h.machine.sendCoachCard("venue-practice"); advanceUntilIdle()
        assertNull(h.setup.coach)
        assertEquals(BookingOption.COURSE_PRACTICE, h.setup.bookingChoice)

        h.machine.setCoachInput(CoachInputMode.OFF)
        h.machine.openCoach()
        assertTrue(h.setup.coach!!.cards.isEmpty())
        assertFalse(h.setup.coachTextInput)
        h.scope.cancel()
    }
}
