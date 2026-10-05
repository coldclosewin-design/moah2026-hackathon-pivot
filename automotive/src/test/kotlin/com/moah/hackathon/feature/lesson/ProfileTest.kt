package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.AdminPresets
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeTtsPort
import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.vehicle.FakeVehiclePort
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import com.moah.hackathon.vehicle.VssValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** 프로필(라운드 22 결정 7 = P2 "한 장", 10/5): 칩 → 대표값 → 저장 → 다시 읽기, 첫 실행, 리포트 끝 카드. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileTest {

    // ───────── 칩 ─────────

    @Test
    fun everyChipRoundTripsToItself() {
        val year = 2026
        ProfileField.entries.forEach { field ->
            ProfileChips.chips(field).forEach { chip ->
                val s = ProfileChips.apply(ProfileStatement(), field, chip.id, year)!!
                assertEquals("$field/${chip.id}", chip, ProfileChips.answerOf(s, field, answered = true, thisYear = year))
                assertFalse(chip.label, chip.label.any { it.isDigit() })
            }
        }
        assertNull(ProfileChips.apply(ProfileStatement(), ProfileField.FEAR, "nope", year))
    }

    @Test
    fun unansweredRowHasNoChipAndDemoProfileReadsBack() {
        assertNull(ProfileChips.answerOf(ProfileStatement(), ProfileField.FEAR, answered = false, thisYear = 2026))
        val demo = SeedCatalog.demoProfile.statement
        assertEquals(ProfileField.entries.toSet(), ProfileChips.filledFields(demo))
        assertEquals("주차", ProfileChips.answerOf(demo, ProfileField.FEAR, true, 2026)!!.label)
        assertEquals("십 년 넘게", ProfileChips.answerOf(demo, ProfileField.LAST_DRIVE, true, 2026)!!.label)
        assertEquals("십 년쯤 전", ProfileChips.answerOf(demo, ProfileField.LICENSE, true, 2026)!!.label)
        assertEquals("중형 SUV", ProfileChips.answerOf(demo, ProfileField.CAR, true, 2026)!!.label)
    }

    @Test
    fun nextAskSkipsAnsweredAndTwiceDeferredRows() {
        assertEquals(ProfileField.FEAR, ProfileChips.nextAsk(emptySet(), emptyMap()))
        assertEquals(ProfileField.LAST_DRIVE, ProfileChips.nextAsk(setOf(ProfileField.FEAR), emptyMap()))
        assertEquals(ProfileField.GOAL, ProfileChips.nextAsk(setOf(ProfileField.FEAR), mapOf(ProfileField.LAST_DRIVE to 2)))
        assertNull(ProfileChips.nextAsk(ProfileField.entries.toSet(), emptyMap()))
    }

    @Test
    fun fileStoreRoundTripsAndToleratesGarbage() {
        val file = File.createTempFile("profile", ".properties").apply { deleteOnExit() }
        file.delete()
        val store = FileProfileStore(file)
        assertNull(store.load())
        val saved = StoredProfile(ProfileStatement(monthsSinceLastDrive = 36, fear = "주차", goal = "아이 등하원"),
            setOf(ProfileField.FEAR, ProfileField.LAST_DRIVE, ProfileField.GOAL), mapOf(ProfileField.CAR to 1), onboarded = true)
        store.save(saved)
        assertEquals(saved, FileProfileStore(file).load())
        store.clear()
        assertNull(store.load())
        file.writeBytes(byteArrayOf(0x00, 0x7f, 0x5c, 0x75))   // 깨진 파일 — 앱은 "저장 없음" 으로 돈다
        assertNull(FileProfileStore(file).load()?.takeIf { it.onboarded })
    }

    // ───────── 상태기계 ─────────

    private class Harness(val port: FakeVehiclePort, val machine: LessonStateMachine, val scope: CoroutineScope)

    private fun TestScope.harness(store: ProfileStore, start: Profile = SeedCatalog.firstRunProfile): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val port = FakeVehiclePort(simulate = false, dispatcher = dispatcher)
        val machine = LessonStateMachine(
            vehicle = port, tts = FakeTtsPort(), coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, Random(3))),
            registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS + CourseRecorder.KEYS, simulated = true), store = ProgressStore(),
            tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor, quizFor = SeedCatalog::quizFor, venues = SeedCatalog.venues,
            benefits = SeedCatalog.benefits, profile = start, scope = scope,
            clock = { testScheduler.currentTime }, briefingMillis = 0, profileStore = store,
        )
        return Harness(port, machine, scope)
    }

    private val Harness.setup: LessonPhase.Setup get() = machine.phase.value as LessonPhase.Setup

    private suspend fun TestScope.feed(h: Harness, scenario: Scenario) {
        var prev = 0.0
        for (step in scenario.steps) {
            val wait = Math.round((step.atSeconds - prev) * 1000)
            if (wait > 0) testScheduler.advanceTimeBy(wait)
            prev = step.atSeconds
            h.port.inject(step.values)
            testScheduler.runCurrent()
        }
    }

    @Test
    fun firstRunShowsOnboardingWithEmptyRowsAndSavesAnswers() = runTest {
        val store = MemoryProfileStore()
        val h = harness(store)
        assertEquals(ProfileField.ONBOARDING, h.setup.onboarding!!.fields)
        assertEquals(ProfileField.entries, h.setup.profileRows.map { it.field })
        assertTrue(h.setup.profileRows.all { it.answer == null && it.chips.isNotEmpty() })
        assertEquals(listOf("아직 함께한 연습이 없어요."), h.setup.observedLines)

        h.machine.answerProfile(ProfileField.FEAR, "lane")
        assertEquals("차선 바꾸기", h.setup.profileRows.first { it.field == ProfileField.FEAR }.answer!!.label)
        assertEquals("lane-change", h.setup.suggestedTask.id)   // 무서운 것 = 차선 변경 → 그 과제를 제안
        assertTrue(ProfileField.FEAR in store.load()!!.answered)
        assertFalse(store.load()!!.onboarded)

        h.machine.finishOnboarding()
        assertNull(h.setup.onboarding)
        assertTrue(store.load()!!.onboarded)
        h.scope.cancel()

        // 다시 켜면 저장된 진술로 시작하고 첫 실행 질문은 없다
        val again = harness(store)
        assertNull(again.setup.onboarding)
        assertEquals("차선 변경", again.setup.profile.statement.fear)
        again.scope.cancel()
    }

    @Test
    fun presetAndResetProfileDriveOnboardingAndStorage() = runTest {
        val store = MemoryProfileStore()
        val h = harness(store)
        val preset = AdminPresets.preset(AdminPresets.REAR_TWO)!!
        h.machine.applyPreset(preset, AdminPresets.profile(preset.profileId)!!.profile)
        assertNull("preset skips onboarding", h.setup.onboarding)
        assertEquals(ProfileField.entries.toSet(), store.load()!!.answered)

        h.machine.resetProfile()
        assertNull(store.load())
        assertNotNull(h.setup.onboarding)
        assertNull(h.setup.profile.statement.fear)
        h.scope.cancel()
    }

    @Test
    fun reportAsksOneUnansweredRowAndSkipOrAnswerClosesIt() = runTest {
        val store = MemoryProfileStore()
        val h = harness(store)
        h.machine.finishOnboarding()   // 아무것도 답하지 않고 건너뜀

        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt()
        advanceUntilIdle()
        h.port.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.TRUE))
        advanceUntilIdle()

        val report = (h.machine.phase.value as LessonPhase.Report).report
        assertEquals(ProfileField.FEAR, report.askOne!!.field)
        h.machine.skipAsk(ProfileField.FEAR)
        assertNull((h.machine.phase.value as LessonPhase.Report).report.askOne)
        assertEquals(1, store.load()!!.skips[ProfileField.FEAR])
        h.scope.cancel()
    }

    @Test
    fun seededDemoProfileHasNoReportCard() = runTest {
        val h = harness(MemoryProfileStore(), start = SeedCatalog.demoProfile)
        h.machine.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT)
        advanceUntilIdle()
        feed(h, ParkingScenarios.good)
        h.machine.finishAttempt()
        advanceUntilIdle()
        h.port.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.TRUE))
        advanceUntilIdle()
        assertNull((h.machine.phase.value as LessonPhase.Report).report.askOne)
        h.scope.cancel()
    }
}
