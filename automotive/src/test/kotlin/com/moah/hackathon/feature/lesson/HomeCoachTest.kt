package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 홈 코치 대화 + 예약 카드(라운드 22 결정 4 = A + D, 간격 A1, 10/5): 탭 대화의 결과가 기존 홈·시트로 떨어진다. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeCoachTest {

    private class Harness(val tts: FakeTtsPort, val store: ProgressStore, val machine: LessonStateMachine, val scope: CoroutineScope)

    private fun TestScope.harness(): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val tts = FakeTtsPort()
        val store = ProgressStore()
        val machine = LessonStateMachine(
            vehicle = FakeVehiclePort(simulate = false, dispatcher = dispatcher), tts = tts,
            coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = store,
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues,
            benefits = SeedCatalog.benefits, profile = SeedCatalog.demoProfile, scope = scope,
            clock = { testScheduler.currentTime }, briefingMillis = 0,
        )
        return Harness(tts, store, machine, scope)
    }

    private val Harness.setup: LessonPhase.Setup get() = machine.phase.value as LessonPhase.Setup

    private fun parkingRecord(task: Task, mode: LessonMode): AttemptRecord {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in ParkingScenarios.good.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return AttemptRecord(1, task.id, mode, r.score()!!, null, "서두.\n조언.", 0)
    }

    @Test
    fun coachWithoutReservationOrRecordsOffersParkingOnlyAndSpeaksTheLine() = runTest {
        val h = harness()
        assertNull(h.setup.coach)
        h.machine.openCoach()
        val coach = assertNotNull(h.setup.coach).let { h.setup.coach!! }
        assertEquals(listOf(CoachChoice.PARKING_PRACTICE), coach.choices)
        assertEquals(coach.line, h.tts.lastSpoken.value)
        assertFalse(coach.line, coach.line.any { it.isDigit() })
        h.scope.cancel()
    }

    @Test
    fun parkingPracticeRequestsTheParkingSheetOnce() = runTest {
        val h = harness()
        h.machine.openCoach()
        h.machine.chooseCoach(CoachChoice.PARKING_PRACTICE)
        assertNull("sheet closes", h.setup.coach)
        assertEquals(TaskType.PARKING, h.setup.sheetRequest)
        h.machine.consumeSheetRequest()
        assertNull(h.setup.sheetRequest)
        h.scope.cancel()
    }

    @Test
    fun reservedVenueHighlightsTheBookingCard() = runTest {
        val h = harness()
        h.machine.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_EXAM)
        assertEquals(listOf(BookingOption.COURSE_PRACTICE, BookingOption.MOCK_EXAM), h.setup.bookingOptions)
        h.machine.openCoach()
        assertEquals(CoachChoice.RESERVED_VENUE, h.setup.coach!!.choices.first())
        assertTrue(h.setup.coach!!.line.contains("서초 시험장"))
        h.machine.chooseCoach(CoachChoice.RESERVED_VENUE)
        assertTrue(h.setup.highlightBooking)
        assertNull(h.setup.coach)
        h.scope.cancel()
    }

    @Test
    fun bookingOptionsPinReservedTaskWithMode() = runTest {
        val h = harness()
        h.machine.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_PARKING)
        h.machine.chooseBooking(BookingOption.MOCK_EXAM)
        assertEquals(SeedCatalog.TASK_PARKING_REAR, h.setup.suggestedTask.id)
        assertEquals(LessonMode.EVALUATE, h.setup.suggestedMode)
        assertEquals(BookingOption.MOCK_EXAM, h.setup.bookingChoice)
        assertTrue(h.setup.reason.endsWith("시험장 코스 그대로, 제가 채점만 할게요."))

        h.machine.chooseBooking(BookingOption.COURSE_PRACTICE)
        assertEquals(LessonMode.GUIDE, h.setup.suggestedMode)   // 처음 하는 과제 → 가이드
        assertEquals(BookingOption.COURSE_PRACTICE, h.setup.bookingChoice)
        h.scope.cancel()
    }

    @Test
    fun noBookingNoOptionsAndBookingChoiceIgnored() = runTest {
        val h = harness()
        assertTrue(h.setup.bookingOptions.isEmpty())
        val before = h.setup
        h.machine.chooseBooking(BookingOption.MOCK_EXAM)
        assertEquals(before, h.setup)
        h.machine.openCoach()
        h.machine.chooseCoach(CoachChoice.RESERVED_VENUE)   // 시트에 없는 칩
        assertNotNull("still open", h.setup.coach)
        h.scope.cancel()
    }

    @Test
    fun continueLastPinsLastTaskAndMode() = runTest {
        val h = harness()
        val front = SeedCatalog.tasks.first { it.id == SeedCatalog.TASK_PARKING_FRONT }
        h.store.add(parkingRecord(front, LessonMode.EVALUATE))
        h.machine.reset()
        h.machine.openCoach()
        assertEquals(listOf(CoachChoice.PARKING_PRACTICE, CoachChoice.CONTINUE_LAST), h.setup.coach!!.choices)
        h.machine.chooseCoach(CoachChoice.CONTINUE_LAST)
        assertEquals(front.id, h.setup.suggestedTask.id)
        assertEquals(LessonMode.EVALUATE, h.setup.suggestedMode)
        h.scope.cancel()
    }

    @Test
    fun resetAndNewReservationClearHomeState() = runTest {
        val h = harness()
        h.machine.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_PARKING)
        h.machine.chooseBooking(BookingOption.MOCK_EXAM)
        h.machine.openCoach()
        h.machine.reset()
        assertNull(h.setup.coach)
        assertNull(h.setup.bookingChoice)
        h.machine.chooseBooking(BookingOption.MOCK_EXAM)
        h.machine.cancelReservation()
        assertNull(h.setup.bookingChoice)
        assertTrue(h.setup.bookingOptions.isEmpty())
        h.scope.cancel()
    }
}
