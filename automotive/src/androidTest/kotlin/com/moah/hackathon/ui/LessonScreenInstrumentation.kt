package com.moah.hackathon.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.moah.hackathon.App
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.concepts.DesignScale
import com.moah.hackathon.ui.lesson.*
import com.moah.hackathon.vehicle.*
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** Platform accessibility checks and screenshots; no additional Gradle dependency is needed. */
class LessonScreenInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    override fun onStart() {
        val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            val task = SeedCatalog.parkingTask
            captureSetupMorph(activity)
            val container = (activity.application as App).container
            var played: String? = null
            val setupDemo: @Composable () -> Unit = {
                DemoPanel(container.scenarios, null, { played = it }, {}, {}, {}, {})
            }
            val demo: @Composable () -> Unit = {
                DemoPanel(container.scenariosFor(task), null, {}, {}, {}, {}, {})
            }
            var started: Pair<String, LessonMode>? = null
            render(activity) {
                SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, task, LessonMode.GUIDE,
                    "처음은 제가 순서대로 함께할게요.", "오늘은 편안한 곳에서 주차부터 연습해 봐요.",
                    { id, mode -> started = id to mode }, setupDemo, venues = SeedCatalog.venues)
            }
            check(allText().containsAll(listOf(setupProposal(TaskType.PARKING), "시작", "시연")))
            check(allText().none { it.startsWith("동승자 · ") })
            assertDriverButton(activity, "시작")
            assertPanelCollapsed()
            capture("setup")
            val proposalBounds = textBounds(setupProposal(TaskType.PARKING))
            click("시연")
            check(texts().containsAll(listOf("잘한 주차", "못한 주차")))
            check(textBounds(setupProposal(TaskType.PARKING)) == proposalBounds) { "Demo rail resized the reading area" }
            capture("panel-open")
            click("잘한 주차")
            runOnMainSync { check(played == container.scenarios.first { it.title == "잘한 주차" }.id) }
            assertPanelCollapsed()
            click("과제·모드 바꾸기")
            uiAutomation.waitForIdle(100, 2_000)
            check(texts().contains("연습할 과제"))
            check(texts().containsAll(listOf("가이드", "힌트", "평가")))
            check(texts().none { it == "지식 테스트" })
            val parkingTasks = SeedCatalog.tasks.filter { it.type == TaskType.PARKING }
            check(texts().containsAll(parkingTasks.map { it.title })) { "Parking must show all four bays" }
            parkingTasks.filterNot { it.isReady }.forEach { assertPlannedTask(it.title) }
            assertSelected(task.title)
            assertSelected("주차")
            val categoryCenters = categoryOrder().map { textBounds(taskTypeLabel(it)).centerX() }
            check(categoryCenters == categoryCenters.sorted())
            check(allText().none { Regex("\\d").containsMatchIn(it) }) { "Sheet leaked task counts or numbers: ${allText()}" }
            check(texts().contains("준비 중"))
            assertDriverButton(activity, "시작")
            val sheetStartBounds = buttonBounds("시작")
            click("힌트")
            val selectedBounds = buttonBounds(task.title)
            val categoryTitleBounds = textBounds("주차")
            capture("setup-sheet") { bitmap ->
                val scale = designScale(activity)
                val checkCircle = colorBounds(bitmap, selectedBounds, CoachColors.Signal.toArgb())
                // Exact-color bounds exclude the antialiased boundary pixel on each side of a circle.
                check(kotlin.math.abs(checkCircle.width() - 64f * scale) <= 2f &&
                    kotlin.math.abs(checkCircle.height() - 64f * scale) <= 2f) { "Selected bay check circle: $checkCircle" }
                check(kotlin.math.abs(checkCircle.centerY() - (selectedBounds.bottom - 32 * scale)) <= 1f)
                check(bitmap.getPixel(checkCircle.centerX() - (4 * scale).toInt(),
                    checkCircle.centerY() + (11 * scale).toInt()) == CoachColors.Paper.toArgb()) { "Missing white check" }
                assertBayFill(bitmap, selectedBounds, CoachColors.Periwinkle.toArgb())
                check(!colorBounds(bitmap, textBounds(task.title), CoachColors.Paper.toArgb()).isEmpty)
                val diagram = Rect(selectedBounds.left, selectedBounds.top, selectedBounds.right,
                    selectedBounds.top + (184 * scale).toInt())
                check(!colorBounds(bitmap, diagram, CoachColors.Paper.toArgb()).isEmpty) { "Selected diagram must be Paper" }
                parkingTasks.filterNot { it.isReady }.forEach { planned ->
                    assertBayFill(bitmap, taskBounds(planned.title), CoachColors.Lavender.copy(alpha = .4f).compositeOver(CoachColors.Paper).toArgb())
                }
                check(!colorBounds(bitmap, categoryTitleBounds, CoachColors.Signal.toArgb()).isEmpty) { "Expanded category text must be Signal" }
                listOf(10, 26).forEach { belowTitle ->
                    check(bitmap.getPixel(categoryTitleBounds.centerX(), categoryTitleBounds.bottom + (belowTitle * scale).toInt()) == CoachColors.Signal.toArgb()) {
                        "Missing Signal underline or downward triangle"
                    }
                }
            }
            pass("Setup first render: filled Periwinkle/40% Lavender bays without outlines, Paper selection, modes/start, Signal category and 64 dp checked circle")
            click("제휴 시험장")
            check(texts().contains(Reservation.EXAMPLE_NOTE))
            check(texts().none { it in listOf("시작", "가이드", "힌트", "평가") })
            click("돌아가기")
            check(buttonBounds("시작") == sheetStartBounds) { "Returning from venues moved the footer" }
            assertSelected("힌트")
            click("시작")
            runOnMainSync { check(started == task.id to LessonMode.HINT) }
            click("주행")
            assertSelected("주행")
            check(texts().none { it in listOf("시작", "가이드", "힌트", "평가", "지식 테스트") })
            val driving = SeedCatalog.tasks.filter { it.type == TaskType.DRIVING }
            check(driving.size == 5)
            driving.take(4).forEach { assertPlannedTask(it.title) }
            capture("setup-sheet-driving")
            val taskRow = nodes().first { it.isScrollable }
            check(taskRow.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            Thread.sleep(350)
            assertPlannedTask(driving.last().title)
            capture("setup-sheet-driving-end")
            click("돌아가기")
            check(texts().contains(setupProposal(TaskType.PARKING)))
            click("과제·모드 바꾸기")
            assertSelected("주차")
            assertSelected(task.title)
            click("조작")
            assertSelected(SeedCatalog.predriveTask.title)
            check(texts().containsAll(listOf("가이드", "힌트", "평가", "시작")))
            click("시작")
            runOnMainSync { check(started == SeedCatalog.predriveTask.id to LessonMode.HINT) }
            val knowledge = SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }
            click("지식")
            assertSelected(knowledge.title)
            click(knowledge.title)
            check(texts().contains("지식 테스트")) { "Knowledge selection: ${texts()}" }
            check(texts().none { it in listOf("가이드", "힌트", "평가") })
            click("시작")
            runOnMainSync { check(started == knowledge.id to LessonMode.QUIZ) }
            click("돌아가기")
            check(texts().contains(setupProposal(TaskType.KNOWLEDGE)))
            click("과제·모드 바꾸기")
            assertSelected("지식")
            assertSelected(knowledge.title)
            check(buttonBounds("시작") == sheetStartBounds)
            capture("setup-knowledge")
            click("주행")
            check(texts().none { it == "시작" })
            click("돌아가기")
            check(texts().contains(setupProposal(TaskType.KNOWLEDGE))) { "Browsing planned tasks lost the last ready selection" }
            click("과제·모드 바꾸기")
            assertSelected("지식")
            assertSelected("지식 테스트")
            click("주차")
            assertSelected(task.title)
            assertSelected("가이드")
            pass("Setup categories: planned driving scroll/no start, checklist dispatch, knowledge QUIZ, reopen selection and mode filtering; fixed footer")
            // The live catalog has one READY task per category. Exercise an unselected READY bay too.
            val twoReady = SeedCatalog.tasks.map { if (it.id == "parking-parallel") it.copy(status = TaskStatus.READY) else it }
            render(activity) { SetupScreen(SeedCatalog.demoProfile, twoReady, task, LessonMode.GUIDE, "", null, { _, _ -> }) }
            click("과제·모드 바꾸기")
            val ready = twoReady.first { it.id == "parking-parallel" }
            val readyBounds = awaitSettledBay(ready.title, CoachColors.Lavender.toArgb())
            capture("setup-sheet-ready-contract") { bitmap ->
                assertBayFill(bitmap, readyBounds, CoachColors.Lavender.toArgb())
                check(!colorBounds(bitmap, textBounds(ready.title), CoachColors.Ink.toArgb()).isEmpty)
            }

            val line = "후면 직각 주차, 가이드 모드. 오늘은 핸들 방향과 기어 전환을 봅니다."
            render(activity) { BriefingScreen(task, LessonMode.GUIDE, line, line) }
            check(allText().contains(line))
            check(texts().none { it == line })
            check(nodes().none(::hasTouchAction))
            capture("briefing")
            pass("Briefing has its full sentence and no touch targets")

            val moving = ManeuverDisplayState("3", -450f, "R", 85f, false,
                "핸들을 오른쪽 끝까지 돌리세요.", "4/6", null, 1, 2, 18, false,
                SignalAvailability.SIMULATED, SignalAvailability.SIMULATED, SignalAvailability.SIMULATED)
            render(activity) { ManeuverScreen(moving, false, false, "다 돌렸어요. 이제 천천히 후진하세요.", {}, demo) }
            check(texts().none { it == "다 됐어요" })
            check(allText().any { "뒤 85 cm" in it })
            assertNoScores()
            check(texts().contains("${task.title} · 1회차"))
            check(allText().any { "후진 중" in it && "조향 방향 호" in it && "보조선" in it })
            check(texts().containsAll(listOf("오른쪽 450°", "오른쪽으로 한 바퀴", "코치")))
            assertPanelCollapsed()
            capture("maneuver")
            capture("maneuver-b")
            capture("maneuver-guides") { bitmap -> assertWheelGuidePixels(bitmap, -1) }
            capture("demo-toggle-pill")
            val pillBounds = buttonBounds("시연")
            // Compose expands the 64 x 32 drawing to a 64 x 48 accessibility touch target.
            check(pillBounds.width() == 64 && pillBounds.height() == 48) { "Pill touch bounds: $pillBounds" }
            check(textBounds("3 km/h").top - (pillBounds.bottom - 8) == 24) { "Pill/speed gap: $pillBounds / ${textBounds("3 km/h")}" }
            val gearBounds = textBounds("R")
            val distanceBounds = textBounds("85 cm")
            click("시연")
            check(texts().contains("잘한 주차"))
            click("잘한 주차")
            assertPanelCollapsed()
            check(texts().none { it == "잘한 주차" })
            check(texts().contains("시뮬레이션 신호"))
            capture("maneuver-collapsed")
            render(activity) { ManeuverScreen(moving.copy(speed = "0"), false, true, null, {}, demo) }
            check(textBounds("R") == gearBounds && textBounds("85 cm") == distanceBounds) { "Telemetry shifted when finish button appeared" }
            assertDriverButton(activity, "다 됐어요")
            assertNoScores()

            listOf(450f, 0f).forEach { steering ->
                render(activity) { ManeuverScreen(moving.copy(steeringDeg = steering), false, false, null, {}) }
                capture(if (steering == 0f) "maneuver-guides-straight" else "maneuver-guides-left") { bitmap ->
                    assertWheelGuidePixels(bitmap, if (steering == 0f) 0 else 1)
                }
            }
            pass("Steering guide pixels: concentric turns widen toward the bottom in both directions; neutral guides stay straight")

            render(activity) {
                ManeuverScreen(moving.copy(steeringSignal = SignalAvailability.LIVE,
                    distanceSignal = SignalAvailability.MISSING, rearDistanceCm = null), false, false, null, {})
            }
            check(texts().containsAll(listOf("실신호", "시뮬레이션", "미측정")))
            check(allText().any { "뒤 거리 미측정" in it })
            capture("mixed")

            // 5.1 rounds to the visible value "5", but the original snapshot MUST still lock all controls.
            listOf(5.1f, 20f).forEach { speed ->
                val snapshot = VehicleSnapshot(speedKmh = speed)
                render(activity) {
                    ManeuverScreen(moving.copy(speed = speed.toInt().toString()), snapshot.locked, snapshot.stopped,
                        "주변을 살피며 운전에 집중해 주세요.", {}, demo)
                }
                check(nodes().none(::hasTouchAction)) { "Locked Maneuver leaked a touch action at $speed" }
                assertNoScores()
            }
            capture("locked")
            render(activity) {
                ManeuverScreen(moving.copy(guideText = "점수 안내", hintText = "감점 안내"), false, false,
                    "지난 회차는 60점이었어요.", {}, demo)
            }
            assertNoScores()
            val longSubtitle = "첫 번째 안내 문장입니다.\n두 번째 안내도 끝까지 보여요.\n세 번째 줄도 잘리지 않아요.\n네 번째 줄까지 함께 확인해요."
            var finished = 0
            render(activity) {
                ManeuverScreen(moving.copy(speed = "0", rearDistanceCm = null, steeringDeg = null, gear = null,
                    guideText = null, guideStep = null, askedDone = true,
                    steeringSignal = SignalAvailability.MISSING, gearSignal = SignalAvailability.MISSING,
                    distanceSignal = SignalAvailability.MISSING), false, true, longSubtitle, { finished++ })
            }
            check(allText().any { it.contains("뒤 거리 미측정") })
            check(texts().count { it == "미측정" } >= 3)
            check(texts().contains(longSubtitle))
            check(allText().none { "조향 방향 호" in it || "보조선" in it || "후진 중" in it })
            check(texts().none { it == "중립" || it.startsWith("오른쪽으로") || it.startsWith("왼쪽으로") })
            capture("missing")
            click("다 됐어요")
            runOnMainSync { check(finished == 1) }
            assertNoScores()
            // A moving value displayed as "1" must not expose a finish action.
            render(activity) { ManeuverScreen(moving.copy(speed = "1"), false, false, null, {}) }
            check(texts().none { it == "다 됐어요" })
            render(activity) { ManeuverScreen(moving.copy(guideText = null, hintText = "잠깐 보이는 힌트"), false, false, "잠깐 보이는 힌트", {}) }
            check(texts().contains("잠깐 보이는 힌트"))
            Thread.sleep(4_200)
            check(texts().none { it == "잠깐 보이는 힌트" })
            pass("Maneuver: no scores, exact lock/stop boundaries, missing signals, four-line subtitle, hint expiry")

            val checklist = moving.copy(speed = "0", gear = "P", guideText = "브레이크를 밟고 시동을 켜 주세요.",
                guideStep = "4/7", taskType = TaskType.CHECKLIST, belt = true, ignitionOn = true,
                doorOpen = false, brakePressed = true, indicatorLeft = true, indicatorRight = false, hazard = false,
                beltSignal = SignalAvailability.SIMULATED, ignitionSignal = SignalAvailability.SIMULATED,
                doorSignal = SignalAvailability.SIMULATED, brakeSignal = SignalAvailability.SIMULATED,
                indicatorLeftSignal = SignalAvailability.SIMULATED, indicatorRightSignal = SignalAvailability.SIMULATED,
                hazardSignal = SignalAvailability.SIMULATED)
            val checklistDemo: @Composable () -> Unit = {
                DemoPanel(container.scenariosFor(SeedCatalog.predriveTask), null, {}, {}, {}, {}, {})
            }
            render(activity) { ManeuverScreen(checklist, false, true, null, {}, checklistDemo, SeedCatalog.predriveTask.title) }
            val checklistLabels = listOf("도어", "안전벨트", "기어", "브레이크 / 시동", "좌 지시등", "우 지시등", "비상등")
            check(texts().containsAll(checklistLabels + listOf("채움", "P", "닫힘", "밟음 → 켜짐", "확인", "아직", "다 됐어요")))
            check(allText().none { Regex("조향각|뒤 거리|cm|이동 \\d+회").containsMatchIn(it) })
            assertNoScores()
            assertDriverButton(activity, "다 됐어요")
            assertPanelCollapsed()
            val chipLabels = checklistLabels.map(::textBounds)
            check(chipLabels.take(4).map { it.left }.distinct().size == 1)
            check(chipLabels.drop(4).map { it.left }.distinct().size == 1)
            check(chipLabels[0].left < chipLabels[4].left && chipLabels[0].top == chipLabels[4].top)
            check(chipLabels.take(4).zipWithNext().all { (a, b) -> a.bottom < b.top })
            check(chipLabels.drop(4).zipWithNext().all { (a, b) -> a.bottom < b.top })
            fun chipColor(bitmap: Bitmap, label: String) = textBounds(label).let {
                bitmap.getPixel(it.left - (12 * designScale(activity)).toInt(), it.centerY())
            }
            capture("maneuver-checklist") { bitmap ->
                check(chipColor(bitmap, "도어") == CoachColors.Periwinkle.toArgb())
                check(chipColor(bitmap, "비상등") == CoachColors.Ink.copy(alpha = .6f).compositeOver(CoachColors.Lavender).toArgb())
            }
            render(activity) { ManeuverScreen(checklist.copy(doorOpen = null, brakePressed = null,
                indicatorLeft = null, indicatorRight = null, hazard = null,
                doorSignal = SignalAvailability.MISSING, brakeSignal = SignalAvailability.MISSING,
                indicatorLeftSignal = SignalAvailability.MISSING, indicatorRightSignal = SignalAvailability.MISSING,
                hazardSignal = SignalAvailability.MISSING), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().count { it == "미측정" } == 5)
            check(texts().containsAll(checklistLabels + listOf("P", "채움", "브레이크 미측정\n시동 시뮬레이션")))
            check(texts().none { it == "시뮬레이션 신호" })
            capture("maneuver-checklist-missing") { bitmap ->
                listOf("도어", "브레이크 / 시동", "좌 지시등", "우 지시등", "비상등").forEach {
                    check(chipColor(bitmap, it) == CoachColors.Lavender.toArgb())
                }
            }
            render(activity) { ManeuverScreen(checklist.copy(brakeSignal = SignalAvailability.LIVE), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().contains("브레이크 실신호\n시동 시뮬레이션"))
            check(texts().none { it == "시뮬레이션 신호" })
            render(activity) { ManeuverScreen(checklist.copy(belt = null, ignitionOn = null,
                beltSignal = SignalAvailability.MISSING, ignitionSignal = SignalAvailability.MISSING), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().count { it == "미측정" } == 2)
            check(texts().contains("P"))
            render(activity) { ManeuverScreen(checklist.copy(speed = "5"), true, false, null, {}, checklistDemo) }
            check(nodes().none(::hasTouchAction))
            assertNoScores()
            pass("Checklist: seven chips in 4+3 columns, complete/pending/missing colors, mixed brake/ignition sources, no parking telemetry, locked touch gate")

            var nextCount = 0
            var quitCount = 0
            SeedCatalog.quiz.forEachIndexed { index, item ->
                val chosen = mutableStateOf<Int?>(null)
                render(activity) { QuizScreen(knowledge, index, SeedCatalog.quiz.size, item, false,
                    chosen.value, index, { chosen.value = it }, { nextCount++ }, { quitCount++ }) }
                check(item.choices.all { it in texts() })
                check(nodes().count { it.isClickable } == item.choices.size + 1)
                check(texts().contains("그만하기"))
                check(texts().none { it in listOf("다음 문제", "결과 보기") })
                if (index == 0) capture("quiz")
                val selected = if (index == 0) (item.answer + 1) % item.choices.size else item.answer
                click(item.choices[selected])
                runOnMainSync { check(chosen.value == selected) }
                check(texts().contains(item.why))
                awaitClickableCount(2)
                val correctChoice = item.choices[item.answer]
                if (selected != item.answer) {
                    check(texts().containsAll(listOf("내 답", "정답")))
                    check(texts().none { it == "내 답 · 정답" })
                    check(textBounds("내 답").right < textBounds(item.choices[selected]).left)
                    check(textBounds("정답").right < textBounds(correctChoice).left)
                    capture("quiz-answered") { bitmap ->
                        val selectedCell = Rect().also { bounds ->
                            nodes().first { (it.isSelected || it.isChecked || it.stateDescription?.toString() in listOf("Selected", "선택됨")) && descendants(it).any { child ->
                                child.text?.toString() == item.choices[selected]
                            } }.getBoundsInScreen(bounds)
                        }
                        val scale = designScale(activity)
                        val outline = colorBounds(bitmap, selectedCell, CoachColors.Signal.toArgb())
                        check(outline == selectedCell) { "Wrong answer outline: $outline / $selectedCell" }
                        val y = selectedCell.top + (12 * scale).toInt()
                        check(bitmap.getPixel(selectedCell.left + (2 * scale).toInt(), y) == CoachColors.Signal.toArgb())
                        check(bitmap.getPixel(selectedCell.left + (6 * scale).toInt(), y) == CoachColors.Lavender.toArgb())
                        check(!colorBounds(bitmap, textBounds(item.choices[selected]), CoachColors.Ink.toArgb()).isEmpty)
                        check(!colorBounds(bitmap, textBounds("내 답"), CoachColors.Signal.toArgb()).isEmpty)
                        check(!colorBounds(bitmap, textBounds("정답"), CoachColors.Periwinkle.toArgb()).isEmpty)
                        check(!colorBounds(bitmap, textBounds(correctChoice), CoachColors.Paper.toArgb()).isEmpty)
                    }
                } else {
                    check(texts().contains("내 답 · 정답"))
                    check(texts().none { it == "내 답" || it == "정답" })
                }
                click(if (index == SeedCatalog.quiz.lastIndex) "결과 보기" else "다음 문제")
            }
            runOnMainSync { check(nextCount == SeedCatalog.quiz.size) }
            listOf<Int?>(null, 0).forEach { chosen ->
                render(activity) { QuizScreen(knowledge, 0, 5, SeedCatalog.quiz.first(), true, chosen, 0, {}, {}, { quitCount++ }) }
                check(texts().contains("정차 후 답해 주세요"))
                check(SeedCatalog.quiz.first().choices.none { it in texts() })
                check(texts().none { it == "그만하기" })
                check(nodes().none(::hasTouchAction))
            }
            capture("quiz-locked")
            runOnMainSync { check(quitCount == 0) }
            listOf<Int?>(null, 0).forEachIndexed { index, chosen ->
                val inQuiz = mutableStateOf(true)
                render(activity) {
                    if (inQuiz.value) QuizScreen(knowledge, 0, SeedCatalog.quiz.size, SeedCatalog.quiz.first(), false,
                        chosen, 0, {}, {}, { quitCount++; inQuiz.value = false })
                    else SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, task, LessonMode.GUIDE,
                        "처음은 제가 순서대로 함께할게요.", null, { _, _ -> })
                }
                click("그만하기")
                runOnMainSync { check(quitCount == index + 1) }
                check(texts().containsAll(listOf("시작", "과제·모드 바꾸기")))
            }
            val items = SeedCatalog.quiz.take(3)
            val wrongChoice = (items[0].answer + 1) % items[0].choices.size
            val results = listOf(QuizResult(items[1].id, items[1].answer, true), QuizResult(items[0].id, wrongChoice, false))
            var quizRestarted = 0
            render(activity) { QuizDoneScreen(knowledge, results, items, "이유까지 기억하면 충분해요.", { quizRestarted++ }) }
            val answerLine = "${items[0].choices[wrongChoice]} → ${items[0].choices[items[0].answer]}"
            check(texts().contains(answerLine))
            capture("quiz-done-results") { bitmap ->
                val bounds = textBounds(answerLine)
                check(bounds.height() / designScale(activity) < 60f) { "Answer comparison must fit on one line" }
                val mine = colorBounds(bitmap, bounds, CoachColors.Signal.toArgb())
                val correct = colorBounds(bitmap, bounds, CoachColors.Periwinkle.toArgb())
                check(!mine.isEmpty && !correct.isEmpty && mine.right < correct.left)
            }
            check(texts().contains(items[0].why))
            check(texts().none { it == items[1].why })
            check(texts().contains("맞았어요"))
            scrollTo("안 풀었어요")
            check(texts().contains("안 풀었어요"))
            click("다시 시작")
            runOnMainSync { check(quizRestarted == 1) }
            render(activity) { QuizDoneScreen(knowledge, SeedCatalog.quiz.mapIndexed { index, item ->
                QuizResult(item.id, if (index == 0) wrongChoice else item.answer, index != 0)
            }, SeedCatalog.quiz, "5문제 중 4개를 맞혔어요. 이유까지 기억하면 충분해요.", {}) }
            capture("quiz-done")
            pass("Quiz: wrong/correct answer eyebrows and outline, explanations, next/result, stopped quit once before/after answering, locked touch zero, colored keyed comparisons and restart")

            val metrics = ParkingMetrics(MotionSummary(4, 44_000, 26_000, 4f, 1_000), emptyList(),
                SteeringSummary(3, 450f), GearSummary(2, true, true), ProximitySummary(1, 35f), PreDriveSummary(false, true))
            val score = ParkingScore(60, 55, metrics, AvailabilityBadge(0, 7, 1), listOf(ParkingRecorder.KEYS.last()))
            val record = AttemptRecord(1, task.id, LessonMode.HINT, score, null,
                "장롱의 문 정도는 열었습니다.\n좋은 출발이에요.", 0)
            var again = 0
            var ended = 0
            render(activity) { DoneScreen(task, 1, record, record.remark, { again++ }, { ended++ }, demo) }
            check(texts().none { "지난번보다" in it })
            assertNoDoneMetrics()
            assertPanelCollapsed()
            check(allText().none { it == "추정 궤적" })
            check(texts().count { it == record.remark } == 1) { "Repeated remark in subtitle footer" }
            capture("done-no-path")
            assertDriverButton(activity, "한 번 더")
            click("한 번 더")
            click("오늘은 여기까지")
            runOnMainSync { check(again == 1 && ended == 1) }
            val path = listOf(PathPoint(0, 0f, 0f, 0f, false), PathPoint(1_000, -1f, -3f, -20f, true),
                PathPoint(2_000, -2f, -5f, -35f, true), PathPoint(3_000, -1f, -4f, -20f, false),
                PathPoint(4_000, -3f, -7f, -45f, true))
            val pathRecord = record.copy(path = path, delta = ParkingDelta(-2, -4, -1, -1),
                score = score.copy(metrics = metrics.copy(harshEvents = listOf(HarshEvent(1_100, HarshKind.BRAKING, -4f)))))
            val replayVisible = mutableStateOf(true)
            render(activity) {
                if (replayVisible.value) DoneScreen(task, 1, pathRecord, null, { replayVisible.value = false }, {}, demo)
                else LessonText("다음 회차")
            }
            click("한 번 더") // 700 ms render + 300 ms click: still inside the three-second replay.
            check(texts().contains("다음 회차")) { "Replay blocked the next attempt action" }
            render(activity) { DoneScreen(task, 1, pathRecord, null, {}, {}, demo) }
            capture("done-arrival-empty") { bitmap -> assertArrivalBay(activity, bitmap, path, settled = false) }
            Thread.sleep(3_000) // 500 ms initial delay + 3 s playback, including render's 700 ms.
            check(allText().contains("추정 궤적"))
            assertPanelCollapsed()
            check(texts().containsAll(listOf("신호로 추정한 궤적이에요.", "실제 위치와 다를 수 있어요.")))
            assertNoDoneMetrics()
            check(allText().none { "셰브론" in it })
            capture("done-path-contract") { bitmap ->
                assertArrivalBay(activity, bitmap, path, settled = true)
            }
            capture("done")
            pass("Done rear-up arrival bay: present before arrival, Periwinkle sides/rear and open front, settled car corners inside, 12 dp start dot without outline; no chevron or metrics")
            render(activity) { DoneScreen(task, 1, record.copy(path = listOf(path.first(), path.first().copy(y = .49f))), null, {}, {}) }
            check(allText().none { it == "추정 궤적" })
            val checklistRecord = record.copy(taskId = SeedCatalog.predriveTask.id, remark = "순서를 잘 익혔어요.\n다음에도 벨트부터 확인해요.",
                score = score.copy(metrics = metrics.copy(preDrive = PreDriveSummary(true, true, 2_000, 5_000),
                    motion = metrics.motion.copy(movingSegments = 0))))
            render(activity) { DoneScreen(SeedCatalog.predriveTask, 1, checklistRecord, null, {}, {}, checklistDemo) }
            check(texts().contains(checklistRecord.remark))
            assertNoDoneMetrics()
            check(allText().none { it == "추정 궤적" })
            check(texts().none { Regex("조향|뒤 최소|칸 안의 위치").containsMatchIn(it) })
            capture("done-checklist")

            val report = LessonReport(task, LessonMode.HINT, listOf(record), score,
                "후면 직각 주차, 힌트 모드 1회.\n다음에는 뒤 거리를 조금 더 남겨 볼까요?\n주변도 함께 살펴요.",
                task, LessonMode.GUIDE, "핸들 타이밍을 한 단계씩 함께 익혀요.", ShareLevel.entries,
                SeedCatalog.benefits, listOf("안전벨트 확인"))
            var restarted = 0
            render(activity) { ReportScreen(report, { restarted++ }) }
            check(texts().containsAll(listOf(badgeText(score.badge), "다시 시작", "진단서", "신호 출처", "이 신호는 이 차에서 받지 못했어요")))
            check(allText().none { "동승자" in it })
            val reportActions = listOf("자세히 보기", "진단서").map(::textBounds)
            check(reportActions.zipWithNext().all { (left, right) -> left.right < right.left && left.top == right.top })
            check(texts().contains(driverReportSummary(report.summary)))
            check(texts().none { Regex("\\d+회\\.").containsMatchIn(it) })
            capture("report")
            click("진단서")
            check(texts().containsAll(listOf("예시입니다 — 실제 전송·계약은 없습니다", "총점만", "항목별", "원시 신호")))
            click("항목별")
            capture("certificate")
            check(nodes().any { it.isChecked && descendants(it).any { child -> child.text?.toString() == "항목별" } })
            click("공유 예시 보기")
            click("돌아가기")
            click("자세히 보기")
            check(texts().containsAll(listOf("숙련", "안전", "60", "55")))
            check(texts().any { it.startsWith("다음엔 ") })
            capture("details")
            click("돌아가기")
            click("진단서")
            click("다시 시작")
            runOnMainSync { check(restarted == 1) }
            pass("Done actions and Report badge, certificate sharing choices, restart")

            render(activity) { ReportScreen(report.copy(attempts = List(100) { record.copy(index = it + 1) }), {}) }
            check(texts().contains("100"))
            check(texts().contains("다시 시작"))
            capture("report-100")

            val goodChecklistScore = score.copy(skill = 100, safety = 100, badge = AvailabilityBadge(0, 12, 0), missingSignals = emptyList(),
                metrics = metrics.copy(motion = metrics.motion.copy(movingSegments = 0, totalMillis = 16_000),
                    preDrive = PreDriveSummary(true, true, 3_000, 8_000, true, true, true, true, true, 14_000)))
            val checklistReport = report.copy(task = SeedCatalog.predriveTask,
                attempts = listOf(checklistRecord.copy(score = goodChecklistScore)), best = goodChecklistScore,
                unverifiedGuideSteps = emptyList())
            render(activity) { ReportScreen(checklistReport, {}) }
            check(texts().contains("출발 전 점검을 돌아봤어요."))
            check(texts().none { it == "주차 과정만 측정했어요." })
            click("자세히 보기")
            check(texts().containsAll(checklistLabels))
            check(texts().count { it == "✓" } == 7)
            check(texts().containsAll(listOf("벨트 3초 · 벨트 먼저", "시동 8초 · 브레이크 밟고 시동")))
            check(texts().none { Regex("이동 \\d+회|조향|뒤 거리|16초").containsMatchIn(it) })
            capture("report-checklist")
            val badChecklistScore = goodChecklistScore.copy(skill = 30, safety = 40,
                metrics = goodChecklistScore.metrics.copy(preDrive = PreDriveSummary(false, true, 9_000, 2_000,
                    false, false, true, true, false)))
            render(activity) { ReportScreen(checklistReport.copy(attempts = listOf(checklistRecord.copy(score = badChecklistScore)),
                best = badChecklistScore), {}) }
            click("자세히 보기")
            check(texts().count { it == "✗" } == 4 && texts().count { it == "✓" } == 3)
            capture("report-checklist-bad")
            val missingChecklistScore = goodChecklistScore.copy(badge = AvailabilityBadge(0, 8, 4),
                missingSignals = (ParkingRecorder.CHECKLIST_KEYS - ParkingRecorder.KEYS).toList(),
                metrics = goodChecklistScore.metrics.copy(preDrive = PreDriveSummary(true, true, 3_000, 8_000, true)))
            render(activity) { ReportScreen(checklistReport.copy(attempts = listOf(checklistRecord.copy(score = missingChecklistScore)),
                best = missingChecklistScore), {}) }
            click("자세히 보기")
            check(texts().count { it == "미측정" } == 4)
            check(texts().none { it == "✗" })
            capture("report-checklist-missing")
            pass("Checklist Report: seven recorded checks, only belt/ignition timing, good/bad/missing distinction; Done retains two sentences")
            reservationFlow(activity)
            recordMotionClips(activity, moving, pathRecord)
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Lesson contract passed\n") })
        } catch (failure: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", failure.stackTraceToString()) })
        }
    }

    private fun reservationFlow(activity: MainActivity) {
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        runOnMainSync {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            vm.restart()
            vm.cancelReservation()
        }
        var reserved: Triple<String, String, String>? = null
        var reserveCalls = 0
        var cancelCalls = 0
        render(activity) {
            val phase by vm.phase.collectAsStateWithLifecycle()
            val setup = phase as LessonPhase.Setup
            SetupScreen(setup.profile, setup.tasks, setup.suggestedTask, setup.suggestedMode, setup.reason, null, vm::begin,
                venues = setup.venues, booking = setup.booking,
                onReserve = { venue, slot, course -> reserveCalls++; reserved = Triple(venue, slot, course); vm.reserve(venue, slot, course) },
                onCancelReservation = { cancelCalls++; vm.cancelReservation() })
        }
        check(texts().none { it.startsWith("예약 · ") })
        val originalProfileBounds = textBounds(profileLine(SeedCatalog.demoProfile))
        click("과제·모드 바꾸기")
        // A manual choice must not hide the state machine's new recommendation after booking.
        click("지식")
        click("제휴 시험장")
        val seocho = SeedCatalog.venues[0]
        val gangnam = SeedCatalog.venues[1]
        val parking = seocho.courses.first()
        check(texts().containsAll(SeedCatalog.venues.map { it.name } + Reservation.EXAMPLE_NOTE))
        check(texts().none { it == "예약" })
        assertReservationNumbers()
        capture("venues") { bitmap ->
            SeedCatalog.venues.forEach { venue ->
                val bounds = buttonBounds(venue.name)
                check(kotlin.math.abs(bounds.height() / designScale(activity) - 220f) <= 1f)
                check(bitmap.getPixel(bounds.left + 2, bounds.top + 2) == CoachColors.Lavender.toArgb())
            }
        }
        click(seocho.name)
        check(texts().containsAll(seocho.slots.map { it.label }))
        assertPlannedTask(seocho.slots.single { !it.available }.label)
        click(parking.title)
        check(texts().none { it == "예약" }) { "Course alone exposed reservation" }
        click(seocho.slots.first().label)
        assertDriverButton(activity, "예약")
        click(gangnam.name)
        check(texts().none { it == "예약" }) { "Changing venue kept old slot/course selection" }
        assertPlannedTask(gangnam.slots.single { !it.available }.label)
        click(gangnam.slots.first { it.available }.label)
        check(texts().none { it == "예약" }) { "Slot alone exposed reservation" }
        click("돌아가기")
        check(texts().none { it == "시간" || it == "예약" })
        click(seocho.name)
        click(seocho.slots.first().label)
        check(texts().none { it == "예약" })
        click(parking.title)
        assertSelected(seocho.name)
        assertSelected(seocho.slots.first().label)
        assertSelected(parking.title)
        assertDriverButton(activity, "예약")
        assertReservationNumbers()
        capture("venue-slots") { bitmap ->
            val unavailable = textBounds(seocho.slots.single { !it.available }.label)
            check(!colorBounds(bitmap, unavailable, CoachColors.Muted.compositeOver(CoachColors.Lavender).toArgb(), tolerance = 1).isEmpty)
        }
        click("예약")
        runOnMainSync {
            check(reserveCalls == 1 && reserved == Triple(seocho.id, seocho.slots.first().id, parking.id))
            check(container.store.reservation?.courseId == parking.id)
        }
        check(texts().containsAll(listOf("예약 확인", "예약됨", "취소", Reservation.EXAMPLE_NOTE,
            "서초 · 오늘 14:00–15:00 · 주차 3종")))
        check(texts().none { it == "예약" })
        assertReservationNumbers()
        capture("reservation")
        click("돌아가기")
        val badge = "예약 · 서초 14:00 · 주차 3종"
        check(texts().contains(badge))
        check(textBounds(badge).bottom < textBounds(profileLine(SeedCatalog.demoProfile)).top)
        check(texts().contains("${SeedCatalog.parkingTask.title} · 가이드 모드"))
        check(texts().any { it.startsWith(ModeAdvisor.RESERVED_REASON) })
        capture("setup-reserved") { bitmap ->
            check(!colorBounds(bitmap, textBounds(badge), CoachColors.Periwinkle.toArgb()).isEmpty)
        }
        click("과제·모드 바꾸기")
        assertSelected("주차")
        assertSelected(SeedCatalog.parkingTask.title)
        check(texts().containsAll(listOf("가이드", "힌트", "평가", "시작")))
        click("제휴 시험장")
        check(texts().contains("예약됨"))
        capture("venues-booked") { bitmap ->
            val bounds = buttonBounds(seocho.name)
            check(bitmap.getPixel(bounds.left + 2, bounds.top + 2) == CoachColors.Periwinkle.toArgb())
        }
        click(seocho.name)
        check(texts().contains("예약 확인"))
        runOnMainSync { check(reserveCalls == 1) }
        click("취소")
        runOnMainSync { check(cancelCalls == 1 && container.store.reservation == null) }
        check(texts().containsAll(SeedCatalog.venues.map { it.name }))
        check(texts().none { it == "예약됨" || it == "예약 확인" })
        click("돌아가기")
        click("돌아가기")
        check(texts().none { it.startsWith("예약 · ") })
        check(textBounds(profileLine(SeedCatalog.demoProfile)) == originalProfileBounds) { "Cancelled badge left empty space" }
        pass("Reservation: three 220 dp cards, unavailable slot disabled, both choices required/reset per venue, reserve/cancel once, real ViewModel recommendation, confirmation/reopen, badge without leftover space")
    }

    private fun assertReservationNumbers() {
        allText().forEach { text ->
            val withoutAllowed = text.replace(Regex("\\d{2}:\\d{2}|\\d+(?:\\.\\d+)? km|주차 3종"), "")
            check(!Regex("\\d|점수|감점").containsMatchIn(withoutAllowed)) { "Reservation leaked other numbers: $text" }
        }
    }

    private fun render(activity: MainActivity, content: @Composable () -> Unit) {
        runOnMainSync { activity.setContent { MaterialTheme(colorScheme = coachColorScheme()) { DesignScale { content() } } } }
        Thread.sleep(700)
        runOnMainSync {}
    }
    @OptIn(ExperimentalComposeUiApi::class)
    private fun composeNodes(activity: MainActivity): List<SemanticsNode> {
        fun roots(view: View): List<RootForTest> = when (view) {
            is RootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return roots(activity.window.decorView).flatMap { descendants(it.semanticsOwner.rootSemanticsNode) }
    }
    private fun captureSetupMorph(activity: MainActivity) {
        // Drive Compose's real animation clock explicitly: cold layout/accessibility IPC must not
        // turn the requested 100 ms samples into several copies of the same physical frame.
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(AndroidUiDispatcher.Main + clock)
        val recomposer = Recomposer(scope.coroutineContext)
        lateinit var view: ComposeView
        runOnMainSync {
            view = ComposeView(activity).apply {
                setParentCompositionContext(recomposer)
                setContent { MaterialTheme(colorScheme = coachColorScheme()) { DesignScale {
                    SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, SeedCatalog.parkingTask,
                        LessonMode.GUIDE, "처음은 제가 순서대로 함께할게요.", null, { _, _ -> })
                } } }
            }
            activity.setContentView(view)
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
        }
        var frameMillis = 16L
        fun advance(millis: Long) {
            frameMillis += millis
            // Flush recomposition, layout and effect launches at the same animation timestamp.
            repeat(3) {
                runOnMainSync { clock.sendFrame(frameMillis * 1_000_000) }
                waitForIdleSync()
                Thread.sleep(40)
            }
        }
        fun posterWidth(): Float {
            var width = 0f
            runOnMainSync { width = composeNodes(activity).first {
                it.config.getOrNull(SemanticsProperties.TestTag) == "setup-poster"
            }.boundsInWindow.width }
            return width
        }
        fun activate(label: String) = runOnMainSync {
            val button = composeNodes(activity).first { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true &&
                    node.config.getOrNull(SemanticsActions.OnClick) != null
            }
            check(button.config[SemanticsActions.OnClick].action?.invoke() == true)
        }
        try {
            advance(0)
            advance(500) // Finish the independent PosterSurface entry animation.
            for (opening in listOf(true, false)) {
                val frames = mutableListOf<Bitmap>()
                val widths = mutableListOf<Float>()
                try {
                    activate(if (opening) "과제·모드 바꾸기" else "돌아가기")
                    for (index in 0..4) {
                        advance(if (index == 0) 0 else 100)
                        widths += posterWidth()
                        if (index == 0 && opening) runOnMainSync {
                            val labels = composeNodes(activity).flatMap {
                                it.config.getOrNull(SemanticsProperties.Text).orEmpty()
                            }.map { it.text }
                            check(labels.containsAll(listOf("가이드", "힌트", "평가", "시작"))) {
                                "First sheet frame must already contain modes and start"
                            }
                        }
                        frames += checkNotNull(uiAutomation.takeScreenshot())
                    }
                    val scale = designScale(activity)
                    check(abs(widths.first() - 2560 * scale * if (opening) .53f else .30f) <= 2)
                    check(abs(widths.last() - 2560 * scale * if (opening) .30f else .53f) <= 2)
                    check(widths.zipWithNext().all { (a, b) -> if (opening) a > b else a < b }) {
                        "Poster must continuously change width: $widths"
                    }
                    check(widths[2] > 2560 * scale * .30f + 4 && widths[2] < 2560 * scale * .53f - 4)
                    val frameWidth = 640
                    val frameHeight = (frames[0].height * frameWidth.toFloat() / frames[0].width).roundToInt()
                    val strip = Bitmap.createBitmap(frameWidth * 5, frameHeight + 36, Bitmap.Config.ARGB_8888)
                    try {
                        val canvas = android.graphics.Canvas(strip)
                        canvas.drawColor(CoachColors.Paper.toArgb())
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = CoachColors.Ink.toArgb(); textSize = 22f; isFilterBitmap = true
                        }
                        frames.forEachIndexed { index, frame ->
                            canvas.drawText("${index * 100} ms", (index * frameWidth + 12).toFloat(), 26f, paint)
                            canvas.drawBitmap(frame, null, Rect(index * frameWidth, 36, (index + 1) * frameWidth, frameHeight + 36), paint)
                        }
                        val name = if (opening) "setup-morph-strip" else "setup-morph-return-strip"
                        File(targetContext.filesDir, "lesson-$name.png").outputStream().use { strip.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally { strip.recycle() }
                    pass("Setup ${if (opening) "opening" else "return"} morph: 0/100/200/300/400 ms poster widths $widths; first-frame modes/start present")
                } finally { frames.forEach { it.recycle() } }
            }
        } finally {
            runOnMainSync { view.disposeComposition(); recomposer.cancel(); scope.cancel() }
        }
    }
    private fun awaitClickableCount(expected: Int) {
        val deadline = SystemClock.uptimeMillis() + 1_000
        do {
            if (nodes().count { it.isClickable } == expected) return
            Thread.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        check(nodes().count { it.isClickable } == expected) { "Clickable node count did not settle to $expected within 1 s" }
    }
    private fun taskBounds(label: String) = Rect().also { bounds ->
        nodes().first { node ->
            (node.isClickable || !node.isEnabled) && descendants(node).any { it.text?.toString() == label }
        }.getBoundsInScreen(bounds)
    }
    private fun awaitSettledBay(label: String, color: Int): Rect {
        val deadline = SystemClock.uptimeMillis() + 2_000
        var previous: Rect? = null
        var pixel: Int? = null
        while (SystemClock.uptimeMillis() < deadline) {
            val bounds = buttonBounds(label)
            if (bounds == previous) {
                val screenshot = checkNotNull(uiAutomation.takeScreenshot())
                try {
                    val sampled = screenshot.getPixel(bounds.left + 4, bounds.top + 4)
                    pixel = sampled
                    val matches = listOf(0, 8, 16).all {
                        abs(((sampled ushr it) and 255) - ((color ushr it) and 255)) <= 1
                    }
                    if (matches && SystemClock.uptimeMillis() <= deadline) return bounds
                } finally { screenshot.recycle() }
            }
            previous = bounds
            val remaining = deadline - SystemClock.uptimeMillis()
            if (remaining <= 0) break
            Thread.sleep(minOf(100L, remaining))
        }
        error("$label did not settle within 2 s: bounds=$previous, face pixel=$pixel / $color")
    }
    private fun assertBayFill(bitmap: Bitmap, bounds: Rect, color: Int) {
        // Sample the face and the former top/side outline positions, outside all illustration/text.
        listOf(4 to 4, bounds.width() / 2 to 2, 2 to bounds.height() / 2,
            bounds.width() - 3 to bounds.height() / 2).forEach { (x, y) ->
            val pixel = bitmap.getPixel(bounds.left + x, bounds.top + y)
            check(listOf(0, 8, 16).all { abs(((pixel ushr it) and 255) - ((color ushr it) and 255)) <= 1 }) {
                "Bay face/edge differs at ($x,$y) in $bounds: $pixel / $color"
            }
        }
    }
    private fun assertArrivalBay(activity: MainActivity, bitmap: Bitmap, path: List<PathPoint>, settled: Boolean) {
        val bounds = Rect().also { rect ->
            nodes().first { it.contentDescription?.toString() == "추정 궤적" }.getBoundsInScreen(rect)
        }
        val density = designScale(activity)
        val viewport = pathViewport(path, bounds.width() / density, bounds.height() / density)
        val scale = viewport.scale * density
        val arrival = path.last()
        val cx = bounds.right - viewport.x(arrival.x) * density
        val cy = bounds.bottom - viewport.y(arrival.y) * density
        val angle = Math.toRadians((180f - arrival.headingDeg).toDouble())
        val c = cos(angle).toFloat(); val s = sin(angle).toFloat()
        fun screen(x: Float, y: Float) = (cx + c * x - s * y).roundToInt() to (cy + s * x + c * y).roundToInt()
        fun pixelNear(x: Float, y: Float, color: Int): Boolean {
            val (px, py) = screen(x, y)
            return (-1..1).any { dx -> (-1..1).any { dy -> bitmap.getPixel(px + dx, py + dy) == color } }
        }
        val halfWidth = .9f * scale * 1.25f
        val halfDepth = 2.25f * scale * 1.15f
        for (side in listOf(-1, 1)) for (along in listOf(-.8f, 0f, .8f)) {
            check(pixelNear(side * halfWidth, along * halfDepth, CoachColors.Periwinkle.toArgb())) { "Arrival side missing" }
        }
        for (across in listOf(-.8f, 0f, .8f)) {
            check(pixelNear(across * halfWidth, halfDepth, CoachColors.Periwinkle.toArgb())) { "Arrival rear line missing" }
        }
        check(pixelNear(0f, -halfDepth, CoachColors.Paper.toArgb())) { "Arrival bay front must remain open" }
        if (!settled) {
            check(pixelNear(0f, 0f, CoachColors.Paper.toArgb())) { "Arrival bay must be empty during initial replay" }
            return
        }
        // Measure the drawn car in arrival-local axes, then check all four bounding corners.
        var left = Float.POSITIVE_INFINITY; var right = Float.NEGATIVE_INFINITY
        var top = Float.POSITIVE_INFINITY; var bottom = Float.NEGATIVE_INFINITY
        for (y in bounds.top until bounds.bottom) for (x in bounds.left until bounds.right) {
            if (bitmap.getPixel(x, y) == CoachColors.Ink.toArgb()) {
                val dx = x - cx; val dy = y - cy
                val localX = c * dx + s * dy; val localY = -s * dx + c * dy
                left = minOf(left, localX); right = maxOf(right, localX)
                top = minOf(top, localY); bottom = maxOf(bottom, localY)
            }
        }
        check(right > left && bottom > top) { "Missing arrival car" }
        check(abs((right - left) - 1.8f * scale) < 4 * density && abs((bottom - top) - 4.5f * scale) < 4 * density)
        for (x in listOf(left, right)) for (y in listOf(top, bottom)) {
            check(abs(x) < halfWidth - 2 * density && abs(y) < halfDepth - 2 * density) { "Arrival car corner outside bay: $x,$y" }
        }
        val start = path.first()
        val startX = bounds.right - viewport.x(start.x) * density
        val startY = bounds.bottom - viewport.y(start.y) * density
        val startRegion = Rect((startX - .95f * scale).toInt(), (startY - 2.3f * scale).toInt(),
            (startX + .95f * scale).toInt(), (startY + 2.3f * scale).toInt())
        val dot = colorBounds(bitmap, startRegion, CoachColors.Lavender.toArgb())
        check(!dot.isEmpty && dot.width() <= 12 * density + 1 && dot.height() <= 12 * density + 1) { "Start outline remains: $dot" }
        check(abs(dot.exactCenterX() - startX) <= 1 && abs(dot.exactCenterY() - startY) <= 1)
        check(cy < startY) { "Rear-up orientation changed" }
    }
    private fun recordMotionClips(activity: MainActivity, moving: ManeuverDisplayState, record: AttemptRecord) {
        // Recording is a review artifact; the animation still receives only the supplied signal/path data.
        val angle = mutableStateOf(0f)
        render(activity) { ManeuverScreen(moving.copy(steeringDeg = angle.value), false, false, null, {}) }
        recordClip("steering") {
            Thread.sleep(900)
            runOnMainSync { angle.value = -450f }
            Thread.sleep(1_500)
            runOnMainSync { angle.value = 0f }
        }
        recordClip("done") {
            render(activity) { DoneScreen(SeedCatalog.parkingTask, 1, record, null, {}, {}) }
        }
        pass("5-second screenrecord clips: /sdcard/lesson-round4-steering.mp4 and /sdcard/lesson-round4-done.mp4")
    }
    private fun recordClip(name: String, changes: () -> Unit) {
        val command = "screenrecord --display-id 4619827259835644672 --size 1280x720 --bit-rate 4M --time-limit 5 /sdcard/lesson-round4-$name.mp4"
        uiAutomation.executeShellCommand(command).use { output ->
            Thread.sleep(250)
            changes()
            val errors = FileInputStream(output.fileDescriptor).bufferedReader().readText()
            check(errors.isBlank()) { "screenrecord $name: $errors" }
        }
    }
    private fun hasTouchAction(node: AccessibilityNodeInfo) = node.isClickable || node.isLongClickable || node.isScrollable
    private fun nodes() = descendants(checkNotNull(uiAutomation.rootInActiveWindow))
    private fun texts() = nodes().mapNotNull { it.text?.toString() }
    private fun allText() = nodes().flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString(), it.stateDescription?.toString()) }
    private fun assertSelected(label: String) {
        // Selected tabs omit the click action; buttons and radio buttons expose other selection fields.
        // Switching categories replaces the subtree; accessibility can lag behind the drawn frame.
        repeat(10) {
            val matches = nodes().filter { descendants(it).any { child -> child.text?.toString() == label } }
            if (matches.any { it.isSelected || it.isChecked || it.stateDescription?.toString() in listOf("Selected", "선택됨") }) return
            Thread.sleep(100)
        }
        error("Missing selected semantics for $label")
    }
    private fun assertPlannedTask(label: String) {
        val ancestors = nodes().filter { descendants(it).any { child -> child.text?.toString() == label } }
        check(ancestors.isNotEmpty()) { "Planned task not visible: $label" }
        check(ancestors.none { it.isClickable || it.actionList.any { action -> action.id == AccessibilityNodeInfo.ACTION_CLICK } }) {
            "Planned task exposes a click action: $label"
        }
        check(ancestors.any { !it.isEnabled }) { "Planned task lacks disabled semantics: $label" }
    }
    private fun assertNoScores() { check(allText().none { Regex("점수|감점|\\d+\\s*점|이동 \\d+회|\\d+초").containsMatchIn(it) }) }
    private fun assertNoDoneMetrics() { check(allText().none { Regex("\\d+회(?!차)|\\d+초|지난번보다|cm").containsMatchIn(it) }) }
    private fun assertPanelCollapsed() {
        check(allText().contains("시연"))
        check(texts().none { it == "시연" }) { "Demo toggle must only expose its accessible label" }
        check(texts().none { it in listOf("잘한 주차", "못한 주차", "잘한 점검", "못한 점검", "문 열기") })
    }
    private fun textBounds(label: String) = Rect().also { rect ->
        nodes().first { it.text?.toString() == label }.getBoundsInScreen(rect)
    }
    private fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
        add(node)
        repeat(node.childCount) { node.getChild(it)?.let { child -> addAll(descendants(child)) } }
    }
    private fun click(label: String) {
        val button = buttonNode(label)
        check(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        Thread.sleep(300)
        // Read the updated semantics after Compose's accessibility events, not a cached subtree.
        uiAutomation.waitForIdle(100, 2_000)
    }
    private fun buttonNode(label: String) = nodes().first { it.isClickable && descendants(it).any { node ->
        node.text?.toString() == label || node.contentDescription?.toString() == label
    } }
    private fun buttonBounds(label: String) = Rect().also { buttonNode(label).getBoundsInScreen(it) }
    private fun assertDriverButton(activity: MainActivity, label: String) {
        val bounds = buttonBounds(label)
        val density = designScale(activity)
        check(kotlin.math.abs(bounds.height() / density - 140f) <= 1f && bounds.width() / density >= 719f) {
            "$label driver bounds: $bounds / $density"
        }
    }
    private fun designScale(activity: MainActivity): Float {
        var density = 1f
        runOnMainSync {
            val content = activity.findViewById<android.view.View>(android.R.id.content)
            density = checkNotNull(com.moah.hackathon.ui.concepts.designDensity(content.width, content.height))
        }
        return density
    }
    private fun scrollTo(label: String) {
        repeat(12) {
            if (texts().contains(label)) return
            val scroll = nodes().firstOrNull { it.isScrollable } ?: error("No scroll container for $label")
            check(scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            Thread.sleep(350)
        }
        error("Not visible after scrolling: $label")
    }
    private fun assertWheelGuidePixels(bitmap: Bitmap, direction: Int) {
        val diagram = Rect().also { bounds ->
            nodes().first { it.contentDescription?.contains("조향 방향 호") == true }.getBoundsInScreen(bounds)
        }
        val color = CoachColors.Periwinkle.copy(alpha = .4f).compositeOver(CoachColors.Ink).toArgb()
        // The renderer truncates premultiplied alpha channels; Color.toArgb rounds them.
        fun isGuide(pixel: Int) = listOf(0, 8, 16).all { shift ->
            kotlin.math.abs(((pixel ushr shift) and 255) - ((color ushr shift) and 255)) <= 1
        }
        // Below the body, only the translucent guides have this color. Scan shared dash rows
        // rather than relying on a particular dash phase or the centre solid arc.
        fun guidePair(from: Float, to: Float): Pair<Float, Float> {
            for (y in (diagram.top + diagram.height() * from).toInt() until (diagram.top + diagram.height() * to).toInt()) {
                val pixels = (diagram.left until diagram.right).filter { isGuide(bitmap.getPixel(it, y)) }
                val gap = pixels.zipWithNext().indexOfFirst { (left, right) -> right - left > 20 }
                if (gap < 0) continue
                val left = (pixels.first() + pixels[gap]) / 2f
                val right = (pixels[gap + 1] + pixels.last()) / 2f
                return left to right
            }
            error("Both guide dashes missing in $from..$to of $diagram")
        }
        val top = guidePair(.82f, .88f)
        val bottom = guidePair(.95f, .995f)
        val topGap = top.second - top.first
        val bottomGap = bottom.second - bottom.first
        if (direction == 0) {
            check(kotlin.math.abs(top.first - bottom.first) <= 1f && kotlin.math.abs(top.second - bottom.second) <= 1f)
        } else {
            check(bottomGap > topGap + 2f) { "Guides must have different radii: top=$top, bottom=$bottom" }
            check((bottom.first - top.first) * direction > 0f && (bottom.second - top.second) * direction > 0f) {
                "Guides turn away from the shared centre: top=$top, bottom=$bottom"
            }
        }
    }
    private fun colorBounds(bitmap: Bitmap, region: Rect, color: Int, tolerance: Int = 0): Rect {
        val bounds = Rect()
        for (y in region.top until region.bottom) for (x in region.left until region.right) {
            val pixel = bitmap.getPixel(x, y)
            val matches = if (tolerance == 0) pixel == color else listOf(0, 8, 16).all { shift ->
                kotlin.math.abs(((pixel ushr shift) and 255) - ((color ushr shift) and 255)) <= tolerance
            }
            if (matches) bounds.union(x, y, x + 1, y + 1)
        }
        return bounds
    }
    private fun capture(name: String, verify: (Bitmap) -> Unit = {}) {
        val screenshot = checkNotNull(uiAutomation.takeScreenshot())
        try {
            File(targetContext.filesDir, "lesson-$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            verify(screenshot)
        } finally {
            screenshot.recycle()
        }
    }
    private fun pass(message: String) { sendStatus(0, Bundle().apply { putString("stream", "PASS $message\n") }) }
}
