package com.moah.hackathon.ui

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.lesson.*
import com.moah.hackathon.vehicle.*
import org.junit.Assert.*
import org.junit.Test

class LessonPresentationTest {
    private val metrics = ParkingMetrics(MotionSummary(4, 44_900, 28_000, 4f, 1_000), emptyList(),
        SteeringSummary(3, 450f), GearSummary(2, true, true), ProximitySummary(1, 35f), PreDriveSummary(false, true))

    @Test fun setupDoesNotInventMissingProfileFields() {
        assertEquals("연수생 · 장롱 10년차 · 목표 아이 등하원", profileLine(SeedCatalog.demoProfile))
        assertEquals("운전자", profileLine(Profile("운전자", ProfileStatement())))
    }

    @Test fun changedSelectionDoesNotKeepUnrelatedRecommendationReason() {
        val original = SeedCatalog.parkingTask
        val changed = SeedCatalog.tasks.first()
        assertEquals("첫 연습이에요", selectionReason(original, LessonMode.GUIDE, original, LessonMode.GUIDE, "첫 연습이에요"))
        assertEquals(changed.summary, selectionReason(changed, LessonMode.GUIDE, original, LessonMode.GUIDE, "첫 연습이에요"))
        assertEquals(original.summary, selectionReason(original, LessonMode.HINT, original, LessonMode.GUIDE, "첫 연습이에요"))
    }

    @Test fun doneFirstAttemptHasNoComparison() { assertTrue(deltaLines(null).isEmpty()) }

    @Test fun taskChangesCannotKeepAnUnsupportedMode() {
        val knowledge = SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }
        assertEquals(LessonMode.QUIZ, supportedMode(knowledge, LessonMode.HINT))
        assertEquals(LessonMode.GUIDE, supportedMode(SeedCatalog.predriveTask, LessonMode.QUIZ))
        assertEquals(LessonMode.HINT, supportedMode(SeedCatalog.parkingTask, LessonMode.HINT))
    }

    @Test fun briefingUsesTheCorrectKoreanParticlesAndOnlyTwoWatchItems() {
        assertEquals("핸들 방향과\n기어 전환을 볼게요.", briefingHeadline(SeedCatalog.parkingTask.watch))
        assertEquals("뒤 거리와\n시동을 볼게요.", briefingHeadline(listOf("뒤 거리", "시동", "벨트")))
        assertEquals("안전벨트를\n볼게요.", briefingHeadline(listOf("안전벨트")))
    }

    @Test fun checklistDoneOmitsParkingAndUnavailableTiming() {
        assertEquals("벨트 2초 · 시동 5초 · 벨트 먼저 · 움직임 4회", processLine(TaskType.CHECKLIST,
            metrics.copy(preDrive = PreDriveSummary(true, true, 2_000, 5_000))))
        assertEquals("움직임 4회", processLine(TaskType.CHECKLIST, metrics.copy(preDrive = PreDriveSummary(null, null))))
        assertEquals("시동 3초 · 움직임 4회", processLine(TaskType.CHECKLIST,
            metrics.copy(preDrive = PreDriveSummary(null, true, null, 3_000))))
    }

    @Test fun missingSignalsNeverCollapseAndChecklistUsesItsOwnSignals() {
        val missing = display().copy(steeringSignal = SignalAvailability.MISSING,
            gearSignal = SignalAvailability.MISSING, distanceSignal = SignalAvailability.MISSING)
        assertNull(missing.commonSignal())
        val checklist = missing.copy(taskType = TaskType.CHECKLIST, beltSignal = SignalAvailability.LIVE,
            gearSignal = SignalAvailability.LIVE, ignitionSignal = SignalAvailability.LIVE)
        assertEquals(SignalAvailability.LIVE, checklist.commonSignal())
        assertNull(checklist.copy(beltSignal = SignalAvailability.MISSING).commonSignal())
        assertEquals("시뮬레이션 신호", collapsedSignalLabel(SignalAvailability.SIMULATED))
    }

    @Test fun doneImprovementIsPositiveAndRegressionIsNeutral() {
        val lines = deltaLines(ParkingDelta(-2, 4, null, 0))
        assertEquals(3, lines.size)
        assertEquals(DeltaLine("지난번보다 이동 2회 ↓", true), lines[0])
        assertEquals(DeltaLine("지난번보다 시간 4초 ↑", false), lines[1])
        assertEquals(DeltaLine("지난번과 기어 전환 같아요", false), lines[2])
    }

    @Test fun doneOmitsUnmeasuredOptionalMetrics() {
        assertEquals(listOf("이동", "시간", "조향 왕복", "기어 전환", "뒤 최소 거리"), metricLines(metrics).map { it.label })
        assertEquals(listOf(MetricLine("이동", "4회"), MetricLine("시간", "44초")),
            metricLines(metrics.copy(steering = null, gear = null, proximity = null)))
        assertFalse(metricLines(metrics.copy(proximity = ProximitySummary(1, null))).any { it.label == "뒤 최소 거리" })
    }

    private fun display(distance: Float? = 35f, warning: Boolean = false) = ManeuverDisplayState("0", 450f, "R",
        distance, warning, null, null, null, 1, 4, 44, true,
        SignalAvailability.LIVE, SignalAvailability.SIMULATED, SignalAvailability.MISSING)

    @Test fun maneuverWarningsUseDistanceOrBooleanFallback() {
        assertTrue(display().proximityAlert())
        assertFalse(display(40f).proximityAlert())
        assertFalse(display(null).proximityAlert())
        assertTrue(display(null, true).proximityAlert())
    }

    @Test fun maneuverAccessibilityIncludesSourcesWithoutScores() {
        val description = display().diagramDescription()
        assertTrue(description.contains("뒤 35 cm"))
        assertTrue(description.contains("실신호"))
        assertTrue(description.contains("시뮬"))
        assertTrue(description.contains("미측정"))
        assertFalse(Regex("점수|감점|\\d+\\s*점").containsMatchIn(description))
    }

    @Test fun diagramClampsSensorRangeAndPreservesPositiveLeftSteering() {
        assertEquals(1f, distanceFraction(300f))
        assertEquals(0f, distanceFraction(null))
        assertEquals(0f, distanceFraction(-5f))
        assertEquals(30f, wheelRotation(450f))
        assertEquals(-30f, wheelRotation(-450f))
        assertEquals(38f, wheelRotation(900f))
    }

    @Test fun reportBadgePreservesAllThreeSignalCounts() {
        assertEquals("실신호 0 · 시뮬레이션 7 · 미측정 1", badgeText(AvailabilityBadge(0, 7, 1)))
        assertEquals("실신호 2 · 시뮬레이션 0 · 미측정 6", badgeText(AvailabilityBadge(2, 0, 6)))
    }

    @Test fun previousResultSpeechCannotLeakIntoNextManeuver() {
        assertNull(maneuverText("이동 4회로 마쳤어요."))
        assertNull(maneuverText("이번에는 44초 걸렸어요."))
        assertNull(maneuverText("지난번에는 60점이었어요."))
        assertNull(maneuverText("그게 오늘의 점수예요."))
        assertNull(maneuverText("이번 감점은 없었어요."))
        assertEquals("안전벨트 점검부터 해 볼까요?", maneuverText("안전벨트 점검부터 해 볼까요?"))
        assertEquals("뒤 35 cm, 멈추세요.", maneuverText("뒤 35 cm, 멈추세요."))
    }

    @Test fun reportSummaryDropsOnlyTheCoachMetadataCountAndPreservesSentenceBreaks() {
        assertEquals("후면 직각 주차, 힌트 모드.\n잘 마쳤어요.\n주변도 살펴요.",
            driverReportSummary("후면 직각 주차, 힌트 모드 2회.\n잘 마쳤어요.\n주변도 살펴요."))
        assertEquals("잘 마쳤어요.\n다음에도 천천히 해 봐요.", driverReportSummary("잘 마쳤어요.\n다음에도 천천히 해 봐요."))
    }

    @Test fun everySeedOpenerRespectsTheDoneCopyContractBeforeRandomSelection() {
        SeedCatalog.remarks.forEach { template ->
            assertFalse(template.text, Regex("\\d|\\{(?:years|segments|seconds|deltaSegments|deltaSeconds)\\}|지난번보다|cm")
                .containsMatchIn(template.text))
        }
    }
}
