package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.AdminPresets
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

/** 관리자 모드(라운드 22 결정 5, 10/5): 프리셋 데이터가 시드와 맞고, 적용·프로필 전환·기록 초기화가 홈 제안을 바르게 바꾼다. */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminPresetTest {

    // ───────── 데이터 ─────────

    @Test
    fun presetsPointAtReadyTasksTheirModesAndTheirScenarios() {
        assertEquals(AdminPresets.REAR_TWO, AdminPresets.presets.first().id)
        assertEquals("ids are unique", AdminPresets.presets.size, AdminPresets.presets.map { it.id }.toSet().size)
        AdminPresets.presets.forEach { preset ->
            val task = SeedCatalog.tasks.firstOrNull { it.id == preset.taskId }
            assertNotNull("${preset.id} task", task)
            assertTrue("${preset.id} ready", task!!.isReady)
            assertTrue("${preset.id} mode ${preset.mode}", task.supports(preset.mode))
            assertNotNull("${preset.id} profile", AdminPresets.profile(preset.profileId))
            val ids = SeedCatalog.scenariosFor(task).map { it.id }
            preset.scenarioOrder.forEach { assertTrue("${preset.id} scenario $it in $ids", it in ids) }
        }
    }

    @Test
    fun presetReservationIsBookableInTheSeed() {
        AdminPresets.presets.mapNotNull { p -> p.reservation?.let { p to it } }.forEach { (preset, seed) ->
            val venue = SeedCatalog.venues.firstOrNull { it.id == seed.venueId }
            assertNotNull("${preset.id} venue", venue)
            assertTrue("${preset.id} slot open", venue!!.slots.any { it.id == seed.slotId && it.available })
            val course = venue.courses.firstOrNull { it.id == seed.courseId }
            assertNotNull("${preset.id} course", course)
            assertTrue("${preset.id} task in course", preset.taskId in course!!.taskIds)
        }
    }

    @Test
    fun profilePresetsStartWithTheDemoProfile() {
        assertEquals(SeedCatalog.demoProfile, AdminPresets.profiles.first().profile)
        assertEquals(AdminPresets.PROFILE_RUSTY, AdminPresets.profiles.first().id)
        assertEquals(AdminPresets.profiles.size, AdminPresets.profiles.map { it.id }.toSet().size)
    }

    @Test
    fun pinnedReasonsHaveNoDigits() {
        LessonMode.entries.forEach { mode ->
            val reason = ModeAdvisor.pinnedReason(mode)
            assertTrue(reason.isNotBlank())
            assertFalse(reason, reason.any { it.isDigit() })
        }
    }

    // ───────── 상태기계 ─────────

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

    @Test
    fun applyPresetPinsTaskAndModeAndClearsRecords() = runTest {
        val h = harness()
        h.machine.reserve("venue-gangnam", "slot-16", SeedCatalog.COURSE_ROAD_A)
        assertNotNull(h.store.reservation)

        val preset = AdminPresets.preset(AdminPresets.EXAM_FAIL_PASS)!!
        h.machine.applyPreset(preset, AdminPresets.profile(preset.profileId)!!.profile)

        assertNull("reservation cleared", h.store.reservation)
        assertTrue(h.store.all().isEmpty())
        assertEquals(SeedCatalog.TASK_TRACK_EXAM, h.setup.suggestedTask.id)
        assertEquals(LessonMode.EVALUATE, h.setup.suggestedMode)
        assertEquals(ModeAdvisor.pinnedReason(LessonMode.EVALUATE), h.setup.reason)
        h.scope.cancel()
    }

    @Test
    fun presetWithReservationShowsBookingAndReservedReason() = runTest {
        val h = harness()
        val preset = AdminPresets.preset(AdminPresets.RESERVED_EXAM)!!
        h.machine.applyPreset(preset, AdminPresets.profile(preset.profileId)!!.profile)

        val booking = h.setup.booking
        assertNotNull(booking)
        assertEquals(SeedCatalog.COURSE_EXAM, booking!!.courseId)
        assertEquals(SeedCatalog.TASK_TRACK_EXAM, h.setup.suggestedTask.id)
        assertTrue(h.setup.reason.startsWith(ModeAdvisor.RESERVED_REASON))
        h.scope.cancel()
    }

    @Test
    fun pinIsReleasedByAnotherChoiceOrByResettingRecords() = runTest {
        val h = harness()
        val preset = AdminPresets.preset(AdminPresets.KNOWLEDGE)!!
        h.machine.applyPreset(preset, AdminPresets.profile(preset.profileId)!!.profile)
        assertEquals(SeedCatalog.TASK_KNOWLEDGE, h.setup.suggestedTask.id)

        h.machine.resetRecords()
        // 기록이 비고 고정이 풀리면 시연 프로필의 무서운 것(주차)이 다시 제안된다
        assertEquals(SeedCatalog.TASK_PARKING_REAR, h.setup.suggestedTask.id)

        h.machine.applyPreset(preset, AdminPresets.profile(preset.profileId)!!.profile)
        h.machine.reserve("venue-seocho", "slot-14", SeedCatalog.COURSE_PARKING)
        assertEquals("reservation releases the pin", SeedCatalog.TASK_PARKING_REAR, h.setup.suggestedTask.id)
        h.scope.cancel()
    }

    @Test
    fun unknownOrUnsupportedPresetChangesNothing() = runTest {
        val h = harness()
        val before = h.setup
        h.machine.applyPreset(AdminPreset("bad", "x", "y", SeedCatalog.TASK_PARKING_REAR, LessonMode.QUIZ, AdminPresets.PROFILE_RUSTY),
            SeedCatalog.demoProfile)
        assertEquals(before, h.setup)
        h.scope.cancel()
    }

    @Test
    fun setProfileChangesStatementAndSuggestion() = runTest {
        val h = harness()
        h.machine.setProfile(AdminPresets.profile(AdminPresets.PROFILE_EMPTY)!!.profile)
        assertNull(h.setup.profile.statement.fear)
        // 무서운 것이 없으면 첫 쉬운 READY 과제
        assertEquals(SeedCatalog.tasks.first { it.isReady && it.difficulty == Difficulty.EASY }.id, h.setup.suggestedTask.id)
        h.machine.setProfile(AdminPresets.profile(AdminPresets.PROFILE_RUSTY)!!.profile)
        assertEquals(SeedCatalog.TASK_PARKING_REAR, h.setup.suggestedTask.id)
        h.scope.cancel()
    }
}
