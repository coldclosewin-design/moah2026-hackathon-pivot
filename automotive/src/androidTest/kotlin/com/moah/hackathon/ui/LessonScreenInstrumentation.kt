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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import com.moah.hackathon.App
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.data.ChecklistScenarios
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.FrontParkingScenarios
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ports.copilot.CopilotAuth
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.concepts.DesignScale
import com.moah.hackathon.ui.lesson.*
import com.moah.hackathon.vehicle.*
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** Platform accessibility checks and screenshots; no additional Gradle dependency is needed. */
class LessonScreenInstrumentation : Instrumentation() {
    private var seedSpeech = false
    private var frontOnly = false
    private var textureOnly = false
    private var round14Only = false
    private var round18Only = false
    private var reservationOnly = false
    private var round22Only = false
    private var round23aOnly = false
    private var round23bOnly = false
    private var round24Only = false
    private var round25aOnly = false
    private var round25bOnly = false
    private var round26aOnly = false
    private var round26bOnly = false
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        seedSpeech = arguments?.getString("seedSpeech") == "true"
        frontOnly = arguments?.getString("frontOnly") == "true"
        round14Only = arguments?.getString("round14Only") == "true"
        round18Only = arguments?.getString("round18Only") == "true"
        round22Only = arguments?.getString("round22Only") == "true"
        round23aOnly = arguments?.getString("round23aOnly") == "true"
        reservationOnly = arguments?.getString("reservationOnly") == "true"
        textureOnly = arguments?.getString("textureOnly") == "true"
        round23bOnly = arguments?.getString("round23bOnly") == "true"
        round24Only = arguments?.getString("round24Only") == "true"
        round25aOnly = arguments?.getString("round25aOnly") == "true"
        round26aOnly = arguments?.getString("round26aOnly") == "true"
        round26bOnly = arguments?.getString("round26bOnly") == "true"
        round25bOnly = arguments?.getString("round25bOnly") == "true"
        start()
    }

    override fun onStart() {
        val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            if (round26bOnly) {
                round26bContract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round26b contract passed\n") })
                return
            }
            if (round26aOnly) {
                round26aContract(activity)
                captureSelectionMorph(activity)
                textureContract(activity)
                cardLayoutContract(activity)
                reservationFlow(activity)
                round25bContract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round26a contract passed\n") })
                return
            }
            if (round25bOnly) {
                round25bContract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round25b contract passed\n") })
                return
            }
            if (round25aOnly) {
                cardLayoutContract(activity)
                round23aContract(activity)
                round23bContract(activity)
                round24Contract(activity)
                round25Locks(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round25a contract passed\n") })
                return
            }
            if (round24Only) {
                round24Contract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round24 contract passed\n") })
                return
            }
            if (round23bOnly) {
                round23bContract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round23b contract passed\n") })
                return
            }
            if (round23aOnly) {
                round23aContract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round23a contract passed\n") })
                return
            }
            if (round22Only) {
                round22Contract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round22 contract passed\n") })
                return
            }
            if (reservationOnly) {
                reservationFlow(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Reservation contract passed\n") })
                return
            }
            if (round18Only) {
                courseContract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round18 contract passed\n") })
                return
            }
            if (round14Only) {
                cardLayoutContract(activity)
                frontParking(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Round14 contract passed\n") })
                return
            }
            if (frontOnly) {
                frontParking(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Front contract passed\n") })
                return
            }
            if (textureOnly) {
                textureContract(activity)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Texture contract passed\n") })
                return
            }
            if (seedSpeech) {
                verifySeedSpeech()
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Seed guide speech passed\n") })
                return
            }
            val task = SeedCatalog.parkingTask
            round26bContract(activity)
            round26aContract(activity)
            captureSelectionMorph(activity)
            captureSetupMorph(activity)
            val container = (activity.application as App).container
            var played: String? = null
            val panelAi = mutableStateOf<StateFlow<CopilotAuth.State>?>(null)
            var aiConnections = 0
            val setupDemo: @Composable () -> Unit = {
                AdminBand(container.scenarios, null, { played = it }, {}, {}, {}, {},
                    aiState = panelAi.value, onConnectAi = { aiConnections++ })
            }
            val demo: @Composable () -> Unit = {
                AdminBand(container.scenariosFor(task), null, {}, {}, {}, {}, {})
            }
            var started: Pair<String, LessonMode>? = null
            render(activity) {
                SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, task, LessonMode.GUIDE,
                    "처음은 제가 순서대로 함께할게요.", "오늘은 편안한 곳에서 주차부터 연습해 봐요.",
                    { id, mode -> started = id to mode }, setupDemo, venues = SeedCatalog.venues)
            }
            check(allText().containsAll(listOf("후면 직각 주차 ▼", "시작", "관리자")))
            check(allText().none { it.startsWith("동승자 · ") })
            assertDriverButton(activity, "시작")
            assertAdminBand()
            capture("setup")
            val setupPillWidth = buttonBounds("시작").width()
            val proposalBounds = textBounds("후면 직각 주차 ▼")
            check(texts().containsAll(listOf("잘한 주차", "못한 주차")))
            check(textBounds("후면 직각 주차 ▼") == proposalBounds) { "Demo rail resized the reading area" }
            click("더 보기 ▴")
            checkAiPanel(activity, panelAi)
            assertBandBelow(activity, "시작")
            click("접기 ▾")
            runOnMainSync { check(aiConnections == 2) { "AI connect/reconnect must dispatch exactly once per click" } }
            click("잘한 주차")
            runOnMainSync { check(played == container.scenarios.first { it.title == "잘한 주차" }.id) }
            assertAdminBand()
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
            sheetHeadingContract(activity)
            val categoryCenters = categoryOrder().map { textBounds(taskTypeLabel(it)).centerX() }
            check(categoryCenters == categoryCenters.sorted())
            check(allText().none { Regex("\\d").containsMatchIn(it) }) { "Sheet leaked task counts or numbers: ${allText()}" }
            check(SeedCatalog.tasks.size == 18 && SeedCatalog.tasks.all { it.isReady })
            check("준비 중" !in texts())
            assertDriverButton(activity, "시작")
            val sheetStartBounds = buttonBounds("시작")
            click("힌트")
            assertSelected("힌트")
            assertActionGap(activity, "시작")
            val selectedBounds = buttonBounds(task.title)
            val categoryTitleBounds = textBounds("주차")
            capture("setup-sheet") { bitmap ->
                assertParkingCardWindows(activity, bitmap, frontSelected = false)
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
                check(!colorBounds(bitmap, categoryTitleBounds, CoachColors.Paper.toArgb()).isEmpty) { "Selected category text must be Paper" }
                val categoryBackground = CoachColors.Lavender.copy(alpha = .5f).compositeOver(CoachColors.Paper)
                check(!colorBounds(bitmap, textBounds("주행"), CoachColors.Ink.copy(alpha = .6f).compositeOver(categoryBackground).toArgb(), tolerance = 1).isEmpty) {
                    "Unselected category must use Ink 60%"
                }
                check(!colorBounds(bitmap, Rect(categoryTitleBounds).apply { inset(-40, -6) }, CoachColors.Ink.toArgb()).isEmpty)
                runOnMainSync {
                    val tabs = composeNodes(activity).filter { it.config.getOrNull(SemanticsProperties.Role) == androidx.compose.ui.semantics.Role.Tab }
                    check(tabs.size == 4)
                    check(tabs.count { it.config.getOrNull(SemanticsProperties.Selected) == true } == 1)
                    val widths = tabs.map { it.boundsInWindow.width }
                    check(widths.all { it >= 400 * scale }) { "Category pills must retain full touch bays: $widths" }
                    check(tabs.sortedBy { it.boundsInWindow.left }.zipWithNext().all { (a, b) ->
                        abs(b.boundsInWindow.left - a.boundsInWindow.right - 12 * scale) <= 1
                    }) { "Category pill gutters changed" }
                }
            }
            pass("Setup first render: filled Periwinkle/40% Lavender bays without outlines, Paper selection, modes/start, content-sized pill tabs and 64 dp checked circle")
            click("제휴 시험장")
            afterUiSettles("venue sheet transition") {
                check(texts().contains(Reservation.EXAMPLE_NOTE))
                check(texts().none { it in listOf("시작", "가이드", "힌트", "평가") })
            }
            click("돌아가기")
            afterUiSettles("venue return footer") { check(buttonBounds("시작") == sheetStartBounds) { "Returning from venues moved the footer" } }
            assertSelected("힌트")
            click("시작")
            runOnMainSync { check(started == task.id to LessonMode.HINT) }
            click("주행")
            assertSelected("주행")
            check(texts().containsAll(listOf("시작", "가이드", "힌트", "평가")))
            val driving = SeedCatalog.tasks.filter { it.type == TaskType.DRIVING }
            check(driving.size == 6 && driving.all { it.isReady })
            driving.take(4).forEach { check(buttonBounds(it.title).width() > 0) }
            capture("setup-sheet-driving")
            val taskRow = nodes().first { it.isScrollable }
            check(taskRow.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            Thread.sleep(350)
            check(buttonBounds(driving.last().title).width() > 0)
            click(driving.last().title)
            click("시작")
            runOnMainSync { check(started == driving.last().id to LessonMode.HINT) }
            capture("setup-sheet-driving-end")
            click("주차")
            click("돌아가기")
            check(texts().contains("후면 직각 주차 ▼"))
            click("과제·모드 바꾸기")
            assertSelected("주차")
            assertSelected(task.title)
            click("점검")
            assertSelected(SeedCatalog.predriveTask.title)
            check(texts().containsAll(listOf("가이드", "힌트", "평가", "시작")))
            check(texts().contains("점검 세부 과제"))
            capture("setup-sheet-checklist")
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
            check(texts().contains("${knowledge.title} ▼"))
            click("과제·모드 바꾸기")
            assertSelected("지식")
            assertSelected(knowledge.title)
            afterUiSettles("reopened sheet footer") { check(buttonBounds("시작") == sheetStartBounds) }
            capture("setup-knowledge")
            click("주행")
            check("시작" in texts())
            click("지식")
            click("돌아가기")
            check(texts().contains("${knowledge.title} ▼")) { "Returning to knowledge lost its selection" }
            click("과제·모드 바꾸기")
            assertSelected("지식")
            assertSelected("지식 테스트")
            click("주차")
            assertSelected(task.title)
            assertSelected("가이드")
            pass("Setup categories: six ready driving cards, scroll and start dispatch, checklist dispatch, knowledge QUIZ, reopen selection and mode filtering; fixed footer")
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

            val line = "후면 직각 주차, 가이드 모드. 오늘은 핸들 방향과 기어 전환과 뒤 거리를 봅니다."
            render(activity) { BriefingScreen(task, LessonMode.GUIDE, line, line, locked = false) }
            check(texts().containsAll(briefingSentences(line)))
            check(nodes().count(::hasTouchAction) == 1)
            check(texts().contains("핸들 방향, 기어 전환과\n뒤 거리를 볼게요."))
            capture("briefing")
            pass("Briefing has sentence captions, progress and a stopped-only skip action")

            listOf(SeedCatalog.predriveTask to "checklist", knowledge to "knowledge").forEach { (briefTask, name) ->
                render(activity) { BriefingScreen(briefTask, if (name == "knowledge") LessonMode.QUIZ else LessonMode.GUIDE, briefTask.summary, null, locked = false) }
                check(nodes().count(::hasTouchAction) == 1)
                capture("briefing-$name")
            }

            val moving = ManeuverDisplayState("3", -450f, "R", 85f, false,
                SeedCatalog.parkingGuide[3].say, "4/6", null, 1, 2, 18, false,
                SignalAvailability.SIMULATED, SignalAvailability.SIMULATED, SignalAvailability.SIMULATED)
            render(activity) { ManeuverScreen(moving, false, false, SeedCatalog.parkingGuide[3].confirm, {}, demo) }
            check(texts().none { it == "다 됐어요" })
            check(allText().any { "뒤 85 cm" in it })
            assertNoScores()
            check(texts().contains("${task.title} · 1회차"))
            check(allText().any { "후진 중" in it && "조향 방향 호" in it && "보조선" in it })
            check(texts().containsAll(listOf("오른쪽 450°", "오른쪽으로 한 바퀴", "코치")))
            assertAdminBand()
            capture("maneuver")
            capture("maneuver-b")
            capture("maneuver-guides") { bitmap -> assertWheelGuidePixels(bitmap, -1) }
            capture("admin-band")
            val bandTop = textBounds("관리자").top
            check(textBounds("85 cm").bottom < bandTop)
            val gearBounds = textBounds("R")
            val distanceBounds = textBounds("85 cm")
            check(texts().contains("잘한 주차"))
            click("잘한 주차")
            assertAdminBand()
            check(texts().contains("잘한 주차"))
            check(texts().contains("시뮬레이션 신호"))
            capture("maneuver-collapsed")
            listOf("hint" to "뒤가 가까워요. 잠깐 멈추고 확인해 주세요.", "evaluate" to null).forEach { (name, hint) ->
                render(activity) { ManeuverScreen(moving.copy(guideText = null, guideStep = null, hintText = hint), false, false, hint, {}, demo) }
                capture("maneuver-$name")
            }
            render(activity) { ManeuverScreen(moving.copy(speed = "0"), false, true, null, {}, demo) }
            check(textBounds("R") == gearBounds && textBounds("85 cm") == distanceBounds) { "Telemetry shifted when finish button appeared" }
            assertDriverButton(activity, "다 됐어요")
            assertBandBelow(activity, "다 됐어요")
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
                check(texts().none { it == "조향 방향 도식" })
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
            check(texts().count { it == "미측정" } == 3) { "Each telemetry cell must show missing only once" }
            listOf("조향각", "기어", "뒤 거리").forEach { label ->
                val title = textBounds(label)
                val missingValues = nodes().filter { it.text?.toString() == "미측정" }.map { Rect().also(it::getBoundsInScreen) }
                check(missingValues.count { it.top >= title.bottom && it.top - title.bottom < 32 * designScale(activity) &&
                    abs(it.centerX() - title.centerX()) < 200 * designScale(activity) } == 1) { "Duplicate or missing value under $label" }
            }
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

            val checklist = moving.copy(speed = "0", gear = "P", guideText = SeedCatalog.predriveGuide[3].say,
                guideStep = "4/7", taskType = TaskType.CHECKLIST, belt = true, ignitionOn = true,
                doorOpen = false, brakePressed = true, brakeAtIgnition = true,
                indicatorLeft = true, indicatorRight = false, hazard = false,
                beltSignal = SignalAvailability.SIMULATED, ignitionSignal = SignalAvailability.SIMULATED,
                doorSignal = SignalAvailability.SIMULATED, brakeSignal = SignalAvailability.SIMULATED,
                indicatorLeftSignal = SignalAvailability.SIMULATED, indicatorRightSignal = SignalAvailability.SIMULATED,
                hazardSignal = SignalAvailability.SIMULATED)
            val checklistDemo: @Composable () -> Unit = {
                AdminBand(container.scenariosFor(SeedCatalog.predriveTask), null, {}, {}, {}, {}, {})
            }
            render(activity) { ManeuverScreen(checklist, false, true, null, {}, checklistDemo, SeedCatalog.predriveTask.title) }
            val checklistLabels = listOf("도어", "안전벨트", "기어", "브레이크 / 시동", "좌 지시등", "우 지시등", "비상등")
            guideBaselineContract(activity, "4/7")
            check(texts().containsAll(checklistLabels + listOf("채움", "P", "닫힘", "밟음 → 켜짐", "켜짐", "미수행", "다 됐어요")))
            check(allText().none { Regex("조향각|뒤 거리|cm|이동 \\d+회").containsMatchIn(it) })
            assertNoScores()
            assertDriverButton(activity, "다 됐어요")
            assertAdminBand()
            val chipLabels = checklistLabels.map(::textBounds)
            check(chipLabels.map { it.left }.distinct().size == 1)
            check(chipLabels.zipWithNext().all { (a, b) -> a.bottom < b.top })
            fun chipColor(bitmap: Bitmap, label: String) = textBounds(label).let {
                bitmap.getPixel(it.left - (12 * designScale(activity)).toInt(), it.centerY())
            }
            capture("maneuver-checklist-pending")
            onMainChecked {
                val wheel = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == "checklist-wheel" }.boundsInWindow
                val active = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == "checklist-step-3" }.boundsInWindow
                check(abs(wheel.center.y - active.center.y) < 2)
            }
            val (goodChecklistScore, completedChecklist) = replayChecklist(ChecklistScenarios.good)
            val (badChecklistScore, badChecklist) = replayChecklist(ChecklistScenarios.bad)
            check(completedChecklist.brakePressed == false && completedChecklist.indicatorLeft == false &&
                completedChecklist.indicatorRight == false && completedChecklist.hazard == false)
            render(activity) { ManeuverScreen(completedChecklist, false, true, null, {}, checklistDemo, SeedCatalog.predriveTask.title) }
            check(texts().count { it == "확인" } == 3)
            check(texts().contains("밟음 → 켜짐"))
            capture("maneuver-checklist") { bitmap ->
                checklistLabels.forEach { check(chipColor(bitmap, it) == CoachColors.Ink.toArgb()) { "$it must be flat once complete" } }
            }
            render(activity) { ManeuverScreen(badChecklist, false, true, null, {}, taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().contains("브레이크 없이 켜짐"))
            capture("maneuver-checklist-bad") { bitmap ->
                check(chipColor(bitmap, "브레이크 / 시동") == CoachColors.Ink.toArgb())
            }
            render(activity) { ManeuverScreen(checklist.copy(doorOpen = null, brakePressed = null, brakeAtIgnition = null,
                indicatorLeft = null, indicatorRight = null, hazard = null,
                doorSignal = SignalAvailability.MISSING, brakeSignal = SignalAvailability.MISSING,
                indicatorLeftSignal = SignalAvailability.MISSING, indicatorRightSignal = SignalAvailability.MISSING,
                hazardSignal = SignalAvailability.MISSING), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().count { it == "미측정" } == 5)
            check(texts().containsAll(checklistLabels + listOf("P", "채움", "브레이크 미측정 시동 시뮬레이션")))
            check(texts().none { it == "시뮬레이션 신호" })
            capture("maneuver-checklist-missing") { bitmap ->
                listOf("도어", "브레이크 / 시동", "좌 지시등", "우 지시등", "비상등").forEach {
                    check(chipColor(bitmap, it) == CoachColors.Ink.toArgb())
                }
            }
            render(activity) { ManeuverScreen(checklist.copy(brakeSignal = SignalAvailability.LIVE), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().contains("브레이크 실신호 시동 시뮬레이션"))
            capture("maneuver-checklist-mixed")
            check(texts().none { it == "시뮬레이션 신호" })
            render(activity) { ManeuverScreen(checklist.copy(belt = null, ignitionOn = null,
                beltSignal = SignalAvailability.MISSING, ignitionSignal = SignalAvailability.MISSING), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().count { it == "미측정" } == 2)
            check(texts().contains("P"))
            render(activity) { ManeuverScreen(checklist.copy(speed = "6"), true, false, null, {}, checklistDemo,
                taskTitle = SeedCatalog.predriveTask.title) }
            check(nodes().none(::hasTouchAction))
            assertNoScores()
            capture("maneuver-checklist-locked")
            pass("Checklist: centered guide wheel, ordered values, current-row provenance, mixed and missing signals, no parking telemetry, locked touch gate")

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
                val selected = if (index <= 1) (item.answer + 1) % item.choices.size else item.answer
                click(item.choices[selected])
                runOnMainSync { check(chosen.value == selected) }
                check(texts().contains(item.why))
                val nextLabel = if (index == SeedCatalog.quiz.lastIndex) "결과 보기" else "다음 문제"
                assertDriverButton(activity, nextLabel)
                check(abs(buttonBounds(nextLabel).width() - setupPillWidth) <= 1) { "Quiz primary width differs from Setup" }
                awaitClickableCount(2)
                val correctChoice = item.choices[item.answer]
                if (selected != item.answer) {
                    check(texts().containsAll(listOf("내 답", "정답")))
                    check(texts().none { it == "내 답 · 정답" })
                    check("아쉬워요" in texts())
                    check(textBounds("내 답").left > textBounds(item.choices[selected]).right)
                    check(textBounds("정답").left > textBounds(correctChoice).right)
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
                        check(!colorBounds(bitmap, textBounds("내 답"), CoachColors.Paper.toArgb()).isEmpty)
                        check(!colorBounds(bitmap, textBounds("정답"), CoachColors.Periwinkle.toArgb()).isEmpty)
                        check(!colorBounds(bitmap, textBounds(correctChoice), CoachColors.Paper.toArgb()).isEmpty)
                    }
                } else {
                    check(texts().contains("내 답 · 정답"))
                    check("맞았어요" in texts())
                    if (index == 2) capture("quiz-correct")
                    check(texts().none { it == "내 답" || it == "정답" })
                }
                if (index >= 5) {
                    assertFullText(activity, item.why, if (index == SeedCatalog.quiz.lastIndex) "결과 보기" else "다음 문제")
                    check(texts().contains(item.question))
                    capture("quiz-round13-${index + 1}")
                }
                click(if (index == SeedCatalog.quiz.lastIndex) "결과 보기" else "다음 문제")
            }
            runOnMainSync { check(nextCount == SeedCatalog.quiz.size) }
            listOf<Int?>(null, 0).forEach { chosen ->
                render(activity) { QuizScreen(knowledge, 0, SeedCatalog.quiz.size, SeedCatalog.quiz.first(), true, chosen, 0, {}, {}, { quitCount++ }) }
                check(texts().contains("운전에 집중해 주세요"))
                check(SeedCatalog.quiz.first().choices.none { it in texts() })
                check(texts().none { it == "그만하기" })
                check(nodes().none(::hasTouchAction))
                check(allText().none { Regex("\\d").containsMatchIn(it) }) { "Locked quiz leaked numbers" }
                check(allText().none { it.startsWith("맞은 문제") })
            }
            capture("quiz-locked")
            runOnMainSync { check(quitCount == 0) }
            listOf<Int?>(null, 0).forEachIndexed { index, chosen ->
                val inQuiz = mutableStateOf(true)
                render(activity) {
                    if (inQuiz.value) QuizScreen(knowledge, 0, SeedCatalog.quiz.size, SeedCatalog.quiz.first(), false,
                        chosen, 0, {}, {}, { quitCount++; inQuiz.value = false })
                    else QuizDoneScreen(knowledge, emptyList(), SeedCatalog.quiz, "이유까지 기억하면 충분해요.", {})
                }
                click("그만하기")
                runOnMainSync { check(quitCount == index + 1) }
                check(texts().contains("메인으로") && texts().none { it == "시작" })
            }
            quizQuitFlow(activity)
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
            click("메인으로")
            runOnMainSync { check(quizRestarted == 1) }
            render(activity) { QuizDoneScreen(knowledge, SeedCatalog.quiz.mapIndexed { index, item ->
                QuizResult(item.id, if (index == 0) wrongChoice else item.answer, index != 0)
            }, SeedCatalog.quiz, "10문제 중 9개를 맞혔어요. 이유까지 기억하면 충분해요.", {}) }
            assertDriverButton(activity, "메인으로")
            capture("quiz-done")
            pass("Quiz: wrong/correct answer eyebrows and outline, explanations, next/result, stopped quit once before/after answering, locked touch zero, colored keyed comparisons and restart")

            val badRecorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true)).apply {
                ParkingScenarios.bad.steps.forEach { onDelta((it.atSeconds * 1000).toLong(), it.values) }
            }
            val metrics = badRecorder.metrics()!!
            val score = ParkingScore(60, 55, metrics, AvailabilityBadge(0, 7, 1), listOf(ParkingRecorder.KEYS.last()))
            val seedCoach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, kotlin.random.Random(3)))
            val record = AttemptRecord(1, task.id, LessonMode.HINT, score, null,
                runBlocking { seedCoach.remark(task, score, null, SeedCatalog.demoProfile, 1, badRecorder.verdict()) }, 0)
            check(record.remark.startsWith("한 번 고쳐 넣었어요.\n"))
            var again = 0
            var ended = 0
            render(activity) { DoneScreen(task, 1, record, record.remark, { again++ }, { ended++ }, demo) }
            check(texts().none { "지난번보다" in it })
            assertNoDoneMetrics()
            assertAdminBand()
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
            assertAdminBand()
            check(texts().containsAll(listOf("신호로 추정한 궤적이에요.", "실제 위치와 다를 수 있어요.")))
            assertNoDoneMetrics()
            check(allText().none { "셰브론" in it })
            capture("done-path-contract") { bitmap ->
                assertArrivalBay(activity, bitmap, path, settled = true)
                assertPathMargins(activity, bitmap)
            }
            capture("done")
            pass("Done rear-up arrival bay: fixed target heading, Lavender sides/rear and open front, faint neighbor strokes without plane, measured car heading and dim starting silhouette; balanced painted margins; no settled rear chevron or metrics")
            render(activity) { DoneScreen(task, 1, record.copy(path = listOf(path.first(), path.first().copy(y = .49f))), null, {}, {}) }
            check(allText().none { it == "추정 궤적" })
            val checklistRecord = record.copy(taskId = SeedCatalog.predriveTask.id,
                remark = runBlocking { seedCoach.remark(SeedCatalog.predriveTask, goodChecklistScore, null, SeedCatalog.demoProfile, 1) },
                score = goodChecklistScore)
            render(activity) { DoneScreen(SeedCatalog.predriveTask, 1, checklistRecord, null, {}, {}, checklistDemo) }
            check(texts().contains(checklistRecord.remark))
            assertNoDoneMetrics()
            check(allText().none { it == "추정 궤적" })
            check(texts().none { Regex("조향|뒤 최소|칸 안의 위치").containsMatchIn(it) })
            check(texts().containsAll(checklistLabels))
            check(texts().count { it == "✓" } == 7)
            capture("done-checklist")
            render(activity) { DoneScreen(SeedCatalog.predriveTask, 1, checklistRecord.copy(score = badChecklistScore,
                remark = "서두르지 않아도 괜찮아요.\n문과 브레이크부터 확인해요."), null, {}, {}, checklistDemo) }
            check(texts().count { it == "✗" } == 4 && texts().count { it == "✓" } == 3)
            assertNoDoneMetrics()
            capture("done-checklist-bad")

            val report = LessonReport(task, LessonMode.HINT, listOf(record), score,
                "다음에는 뒤 거리를 조금 더 남겨 볼까요?\n주변도 함께 살펴요.",
                task, LessonMode.GUIDE, "핸들 타이밍을 한 단계씩 함께 익혀요.", ShareLevel.entries,
                SeedCatalog.benefits, listOf("안전벨트 확인"))
            val ask = ProfileRow(ProfileField.GOAL, null, ProfileChips.chips(ProfileField.GOAL))
            val askedReport = mutableStateOf(report.copy(askOne = ask))
            var profileAnswer: Pair<ProfileField, String>? = null
            var skipped: ProfileField? = null
            render(activity) { ReportScreen(askedReport.value, {}, onAnswerProfile = { field, id ->
                profileAnswer = field to id; askedReport.value = report
            }, onSkipAsk = { field -> skipped = field; askedReport.value = report }) }
            check(textBounds("하나만 물어볼게요").top < buttonBounds("메인으로").top)
            check(textBounds(taskModeLine(report.task, report.mode)).bottom < textBounds("하나만 물어볼게요").top)
            assertFullText(activity, ask.field.question)
            capture("report-ask-one")
            click("아이 등하원")
            afterUiSettles("Answered report question closes") { check("하나만 물어볼게요" !in texts()) }
            check(profileAnswer == ProfileField.GOAL to "school-run")
            runOnMainSync { askedReport.value = report.copy(askOne = ask) }
            afterUiSettles("Report skip available") { check("다음에요" in texts()) }
            click("다음에요")
            afterUiSettles("Skipped report question closes") { check("하나만 물어볼게요" !in texts()) }
            check(skipped == ProfileField.GOAL)
            var restarted = 0
            render(activity) { ReportScreen(report, { restarted++ }) }
            check(texts().containsAll(listOf(badgeText(score.badge), "메인으로", "진단서", "신호 출처", "이 신호는 이 차에서 받지 못했어요")))
            check(allText().none { "동승자" in it })
            val reportActions = listOf("자세히 보기", "진단서").map(::textBounds)
            check(reportActions.zipWithNext().all { (left, right) -> left.right < right.left && left.top == right.top })
            check(texts().contains(coachDisplayText(driverReportSummary(report.summary))))
            check(texts().containsAll(listOf("연습한 회차", "회")))
            check(texts().none { Regex("\\d+회\\.").containsMatchIn(it) })
            assertDriverButton(activity, "메인으로")
            capture("report")
            click("진단서")
            check(texts().containsAll(listOf("예시입니다 — 실제 전송·계약은 없습니다", "총점만", "항목별", "원시 신호")))
            click("항목별")
            assertDriverButton(activity, "메인으로")
            assertActionGap(activity, "메인으로")
            assertCertificateLayout(activity, report)
            capture("certificate")
            check(nodes().any { it.isChecked && descendants(it).any { child -> child.text?.toString() == "항목별" } })
            click("공유 예시 보기")
            assertShareExample(activity, ShareLevel.PER_ITEM)
            capture("certificate-share-example")
            click("돌아가기")
            click("돌아가기")
            click("자세히 보기")
            check(texts().containsAll(listOf("숙련", "안전", "60", "55")))
            assertMetricRow(activity, "조향 왕복", "3")
            assertMetricRow(activity, "기어 전환", "2")
            assertMetricRow(activity, "근접", "1")
            assertMetricRow(activity, "급정지", "1")
            check(texts().any { it.startsWith("다음엔 ") })
            capture("details")
            assertCompactProvenance(activity, report)
            click("돌아가기")
            click("진단서")
            click("메인으로")
            runOnMainSync { check(restarted == 1) }
            pass("Done actions and Report badge, certificate sharing choices, restart")

            val unmeasuredRecord = record.copy(index = 2, score = score.copy(metrics = metrics.copy(
                steering = null, gear = null, proximity = null, harshEvents = emptyList())))
            render(activity) { ReportScreen(report.copy(attempts = listOf(record, unmeasuredRecord)), {}) }
            check(texts().none { it.startsWith("조향 왕복") })
            click("자세히 보기")
            assertMetricRow(activity, "조향 왕복", "3")
            listOf("조향 왕복", "기어 전환", "근접").forEach { assertMetricRow(activity, it, "미측정") }
            assertMetricRow(activity, "급정지", "0")
            capture("details-multiple-missing")

            render(activity) { ReportScreen(report.copy(attempts = List(100) { record.copy(index = it + 1) }), {}) }
            check(texts().contains("100"))
            check(texts().contains("메인으로"))
            capture("report-100")

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
            render(activity) { ReportScreen(checklistReport.copy(attempts = listOf(checklistRecord.copy(score = badChecklistScore)),
                best = badChecklistScore), {}) }
            click("자세히 보기")
            check(texts().count { it == "✗" } == 4 && texts().count { it == "✓" } == 3)
            capture("report-checklist-bad")
            val missingChecklistScore = goodChecklistScore.copy(badge = AvailabilityBadge(0, 8, 4),
                missingSignals = (ParkingRecorder.CHECKLIST_KEYS - ParkingRecorder.KEYS).toList(),
                metrics = goodChecklistScore.metrics.copy(preDrive = PreDriveSummary(true, true, 3_000, 8_000, true)))
            render(activity) { DoneScreen(SeedCatalog.predriveTask, 1, checklistRecord.copy(score = missingChecklistScore), null, {}, {}) }
            check(texts().count { it == "미측정" } == 4)
            assertNoDoneMetrics()
            capture("done-checklist-missing") { bitmap ->
                val mutedOnInk = CoachColors.Paper.copy(alpha = .6f).compositeOver(CoachColors.Ink).toArgb()
                nodes().filter { it.text?.toString() == "미측정" }.forEach { node ->
                    check(!colorBounds(bitmap, Rect().also(node::getBoundsInScreen), mutedOnInk, tolerance = 1).isEmpty)
                }
            }
            render(activity) { ReportScreen(checklistReport.copy(attempts = listOf(checklistRecord.copy(score = missingChecklistScore)),
                best = missingChecklistScore), {}) }
            click("자세히 보기")
            check(texts().count { it == "미측정" } == 4)
            check(texts().none { it == "✗" })
            capture("report-checklist-missing")
            pass("Checklist Report: seven recorded checks, only belt/ignition timing, good/bad/missing distinction; Done retains two sentences")
            round11Results(activity, record, pathRecord, checklistRecord, report, demo)
            round12Verdicts(activity, report)
            frontParking(activity)
            seedVerdictOpeners(activity)
            reservationFlow(activity)
            recordMotionClips(activity, moving, pathRecord)
            textureContract(activity)
            cardLayoutContract(activity)
            courseContract(activity, full = false)
            round23aContract(activity)
            round23bContract(activity)
            round24Contract(activity)
            round25Locks(activity)
            round25bContract(activity)
            captureDoneSettle(activity, task, pathRecord)
            captureDoneSettle(activity, SeedCatalog.predriveTask, checklistRecord, "done-checklist-settle-strip")
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Lesson contract passed\n") })
        } catch (failure: Throwable) {
            println("Failure screen: ${allText()}")
            runCatching { capture("failure") }
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", failure.stackTraceToString()) })
        }
    }

    /** Actual good seed plus an explicit five-segment fixture: guarded openers and recent fallback, existing Done layout. */
    private fun seedVerdictOpeners(activity: MainActivity) {
        fun replay(scenario: Scenario) = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true)).apply {
            scenario.steps.forEach { onDelta((it.atSeconds * 1000).toLong(), it.values) }
        }
        val task = SeedCatalog.parkingTask
        val good = replay(ParkingScenarios.good)
        val score = good.score()!!
        val verdict = good.verdict()!!
        val coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, kotlin.random.Random(7)))
        val seen = mutableSetOf<String>()
        repeat(2) { index ->
            val remark = runBlocking { coach.remark(task, score, null, SeedCatalog.demoProfile, index + 1, verdict) }
            val opener = remark.substringBefore('\n')
            seen += opener
            val record = AttemptRecord(index + 1, task.id, LessonMode.HINT, score, null, remark, 0,
                path = good.path(), verdict = verdict)
            render(activity) { DoneScreen(task, index + 1, record, null, {}, {}) }
            Thread.sleep(3_000)
            assertFullText(activity, remark, "한 번 더")
            capture(if (opener == "한 번에 들어갔어요.") "done-seed-one-go" else "done-seed-repeat")
        }
        check(seen.size == 2 && "한 번에 들어갔어요." in seen)
        check("신호로 추정하면 방향도 맞게 섰어요." !in seen)

        // This is a layout fixture, not a third driving scenario. Derive score/verdict from five measured segments.
        val bad = replay(ParkingScenarios.bad)
        val badScore = bad.score()!!
        val manyMetrics = badScore.metrics.copy(motion = badScore.metrics.motion.copy(movingSegments = 5))
        val manyScore = ParkingScorer.score(manyMetrics, badScore.badge, badScore.missingSignals)
        val manyVerdict = ParkingVerdicts.of(manyMetrics, bad.path(), 0f, true)
        val manyRemark = runBlocking { coach.remark(task, manyScore, null, SeedCatalog.demoProfile, 1, manyVerdict) }
        check(manyRemark.startsWith("여러 번 오갔어요.\n"))
        val many = AttemptRecord(1, task.id, LessonMode.HINT, manyScore, null, manyRemark, 0, verdict = manyVerdict)
        render(activity) { DoneScreen(task, 1, many, null, {}, {}) }
        assertFullText(activity, manyRemark, "한 번 더")
        capture("done-seed-many-fixture")
        pass("Seed verdict openers: one_go/one_fix/many and recent fallback come from the seed without aligned-only openers and fit the existing Done layout")
    }

    /** External layout fixtures only: these do not represent measurements in a real vehicle. */
    private fun round11Results(activity: MainActivity, record: AttemptRecord, pathRecord: AttemptRecord,
        checklistRecord: AttemptRecord, report: LessonReport, demo: @Composable () -> Unit) {
        val task = report.task
        val knowledge = SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }
        val locked = mutableStateOf(false)
        var actions = 0
        fun setLock(value: Boolean) {
            runOnMainSync { locked.value = value }
            Thread.sleep(350)
            uiAutomation.waitForIdle(100, 2_000)
        }
        fun assertLocked(name: String) {
            check(texts().containsAll(listOf("운전에 집중해 주세요", "속도를 낮추면 결과가 다시 보여요.")))
            check(nodes().none(::hasTouchAction)) { "$name leaked a touch/scroll action" }
            check(allText().none { Regex("\\d|점수|감점|시연").containsMatchIn(it) }) { "$name leaked result content: ${allText()}" }
            capture(name)
        }
        listOf(task to pathRecord, SeedCatalog.predriveTask to checklistRecord).forEach { (resultTask, attempt) ->
            render(activity) { DoneScreen(resultTask, 1, attempt, null, { actions++ }, { actions++ }, demo, locked.value) }
            setLock(true)
            assertLocked(if (resultTask == task) "done-locked" else "done-checklist-locked")
            setLock(false)
            afterUiSettles("Done remark after unlocking") { check(attempt.remark in texts()) }
            assertAdminBand()
            click("한 번 더")
        }
        render(activity) { ReportScreen(report, { actions++ }, locked.value) }
        listOf<String?>(null, "자세히 보기", "진단서").forEach { page ->
            if (page != null) click(page)
            setLock(true)
            assertLocked("report-locked" + when (page) { "자세히 보기" -> "-details"; "진단서" -> "-certificate"; else -> "" })
            setLock(false)
            check((page ?: "오늘의 기록") in texts()) { "Report page was lost after unlocking" }
            if (page != null) click("돌아가기")
        }
        click("메인으로")
        val item = SeedCatalog.quiz.first()
        render(activity) { QuizDoneScreen(knowledge, listOf(QuizResult(item.id, item.answer, true)), listOf(item),
            "이유를 함께 살펴봤어요.", { actions++ }, locked.value) }
        setLock(true)
        assertLocked("quiz-done-locked")
        setLock(false)
        check("맞았어요" in texts())
        assertDriverButton(activity, "메인으로")
        click("메인으로")
        runOnMainSync { check(actions == 4) }

        val longRemark = "오늘 주차하는 순서를 차근차근 익혀 봤어요.\n핸들을 돌리기 전에 주변을 살펴봐요.\n뒤 거리를 넉넉히 두고 천천히 움직여요.\n다음에도 서두르지 말고 함께 연습해요."
        val longSummary = "오늘은 주변을 살피며 주차하는 순서를 차근차근 익혀 봤어요.\n핸들을 돌리기 전에 잠깐 멈추고 주변에 다른 차나 사람이 있는지 살펴봐요.\n뒤 거리를 넉넉히 두고 천천히 움직이면 다음 동작을 준비하기 편해요.\n다음에도 서두르지 말고 익숙해질 때까지 함께 연습하며 움직임을 차근차근 돌아봐요."
        check(longRemark.length == 90 && longSummary.length == 160)
        listOf(task to pathRecord, SeedCatalog.predriveTask to checklistRecord).forEach { (resultTask, attempt) ->
            render(activity) { DoneScreen(resultTask, 1, attempt.copy(remark = longRemark), "주변을 살피며 천천히 연습해요.", {}, {}, demo) }
            assertFullText(activity, longRemark, "한 번 더", 4)
            capture(if (resultTask == task) "done-long" else "done-checklist-long")
        }
        render(activity) { ReportScreen(report.copy(summary = longSummary), {}) }
        assertFullText(activity, longSummary, "메인으로", 4)
        capture("report-long")
        // The Cloud path has two sentences; also check wrapping without the explicit four breaks.
        render(activity) { ReportScreen(report.copy(summary = longSummary.replace('\n', ' ')), {}) }
        assertFullText(activity, longSummary, "메인으로", 4)

        val missingScore = report.best.copy(badge = AvailabilityBadge(0, 0, ParkingRecorder.CHECKLIST_KEYS.size),
            missingSignals = ParkingRecorder.CHECKLIST_KEYS.toList())
        val missingReport = report.copy(task = SeedCatalog.predriveTask, best = missingScore,
            attempts = listOf(checklistRecord.copy(score = missingScore)), summary = longSummary,
            unverifiedGuideSteps = SeedCatalog.predriveGuide.map { it.say })
        render(activity) { ReportScreen(missingReport, {}) }
        assertFullText(activity, longSummary, "메인으로", 4)
        capture("report-all-missing")
        click("자세히 보기")
        val back = buttonBounds("돌아가기")
        val badge = textBounds(badgeText(missingScore.badge))
        capture("report-all-missing-details")
        val scroll = nodes().first { it.isScrollable }
        repeat(8) {
            scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            Thread.sleep(100)
        }
        val lastList = missingReport.unverifiedGuideSteps.joinToString(" · ")
        assertFullText(activity, lastList, "돌아가기")
        check(buttonBounds("돌아가기") == back && textBounds(badgeText(missingScore.badge)) == badge)
        capture("report-all-missing-scrolled")
        click("돌아가기")

        val goodRecorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true)).apply {
            ParkingScenarios.good.steps.forEach { onDelta((it.atSeconds * 1000).toLong(), it.values) }
        }
        val perfect = checkNotNull(goodRecorder.score())
        render(activity) { ReportScreen(report.copy(best = perfect, attempts = listOf(record.copy(score = perfect)), unverifiedGuideSteps = emptyList()), {}) }
        click("자세히 보기")
        check(texts().count { it == "100" } == 2)
        capture("report-parking-perfect")
        render(activity) { ReportScreen(report.copy(attempts = listOf(record, record.copy(index = 2, score = perfect))), {}) }
        capture("report-multiple")
        click("자세히 보기")
        check(textBounds("1회차").right < textBounds("2회차").left)
        check(textBounds("1회차").top == textBounds("2회차").top)
        check(texts().containsAll(listOf("+40", "+45", "→")))
        assertMetricRow(activity, "급정지", "1")
        assertMetricRow(activity, "급정지", "0")
        assertCompactProvenance(activity, report)
        capture("details-comparison")
        render(activity) { ReportScreen(report.copy(attempts = List(3) { record.copy(index = it + 1) }), {}) }
        click("자세히 보기")
        check("2회차" in texts() && "3회차" in texts() && "1회차" !in texts())
        click("이전 회차")
        check("1회차" in texts() && "2회차" in texts() && "3회차" !in texts())
        click("다음 회차")
        check("2회차" in texts() && "3회차" in texts())
        capture("details-history")
        listOf("live7-sim1-fixture" to AvailabilityBadge(7, 1, 0), "live-missing-fixture" to AvailabilityBadge(7, 0, 1)).forEach { (name, availability) ->
            val sourceScore = report.best.copy(badge = availability,
                missingSignals = if (availability.missing == 0) emptyList() else listOf(ParkingRecorder.KEYS.last()))
            render(activity) { ReportScreen(report.copy(best = sourceScore), {}) }
            check(badgeText(availability) in texts())
            capture("report-$name")
        }
        val longQuestion = SeedCatalog.quiz.first { it.id == "night-highbeam" }.copy(
            question = "비가 내리는 어두운 도로에서 앞차를 따라 천천히 달리고 있어요. 마주 오는 차가 있을 때 주변 운전자의 시야를 방해하지 않으려면 상향등은 어떻게 해야 할까요?")
        render(activity) { QuizScreen(knowledge, 0, 1, longQuestion, false, null, 0, {}, {}, {}) }
        assertFullText(activity, longQuestion.question)
        capture("quiz-long")
        pass("Round11: result lock removes numbers and touch, unlock restores pages/actions; full 90/160-character results; 12 missing signals and seven unverified steps scroll above fixed back/badge; capture gaps")
    }

    private fun assertFullText(activity: MainActivity, text: String, action: String? = null, minLines: Int = 1) {
        var height = 0
        runOnMainSync {
            val node = composeNodes(activity).first { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }
            val layouts = mutableListOf<TextLayoutResult>()
            check(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
            val layout = layouts.single()
            height = layout.size.height
            check(!layout.hasVisualOverflow && layout.lineCount >= minLines && layout.getLineEnd(layout.lineCount - 1) == text.length) {
                "Text clipped: ${layout.lineCount} lines, ${layout.layoutInput.style.fontSize}, size=${layout.size}, paragraph=${layout.multiParagraph.height}, end=${layout.getLineEnd(layout.lineCount - 1)}/${text.length}, $text"
            }
            if (action != null) check(layout.layoutInput.style.fontSize.value >= 32)
        }
        val bounds = textBounds(text)
        check(abs(bounds.height() - height) <= 1) { "Text clipped by viewport: ${bounds.height()} / $height, $text" }
        val image = screenshot()
        check(bounds.top >= 0 && bounds.bottom <= image.height && bounds.right <= image.width)
        if (action != null) check(bounds.bottom <= buttonBounds(action).top) { "Text overlaps $action: $bounds" }
    }

    private fun tap(x: Int, y: Int) {
        val now = SystemClock.uptimeMillis()
        listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP).forEach { action ->
            val event = android.view.MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, x.toFloat(), y.toFloat(), 0)
            check(uiAutomation.injectInputEvent(event, true))
            event.recycle()
        }
        Thread.sleep(350)
        uiAutomation.waitForIdle(100, 2_000)
        refreshAccessibility()
    }

    private fun verifySeedSpeech() {
        val initialized = java.util.concurrent.CountDownLatch(1)
        var status = android.speech.tts.TextToSpeech.ERROR
        val engine = android.speech.tts.TextToSpeech(targetContext) { status = it; initialized.countDown() }
        try {
            check(initialized.await(15, java.util.concurrent.TimeUnit.SECONDS) && status == android.speech.tts.TextToSpeech.SUCCESS)
            check(engine.setLanguage(java.util.Locale.KOREAN) >= 0)
            engine.voices.firstOrNull { it.name == com.moah.hackathon.BuildConfig.TTS_VOICE }?.let { engine.voice = it }
            engine.setSpeechRate(1.05f)
            val lines = (SeedCatalog.parkingGuide + SeedCatalog.predriveGuide).flatMap { listOf(it.say, it.confirm) }
            lines.forEachIndexed { index, line ->
                val done = java.util.concurrent.CountDownLatch(1)
                var succeeded = false
                engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String) {}
                    override fun onDone(id: String) { succeeded = true; done.countDown() }
                    @Deprecated("platform callback")
                    override fun onError(id: String) { done.countDown() }
                })
                val file = File(targetContext.filesDir, "seed-guide-$index.wav")
                check(engine.synthesizeToFile(line, Bundle(), file, "seed-$index") == android.speech.tts.TextToSpeech.SUCCESS)
                check(done.await(20, java.util.concurrent.TimeUnit.SECONDS) && succeeded && file.length() > 44) { "TTS failed: $line" }
                pass("Seed speech ${index + 1}/${lines.size}: $line")
            }
        } finally {
            engine.shutdown()
        }
    }

    private fun replayChecklist(scenario: Scenario): Pair<ParkingScore, ManeuverDisplayState> {
        val registry = SignalRegistry(ParkingRecorder.CHECKLIST_KEYS, simulated = true)
        val recorder = ParkingRecorder(registry, ParkingRecorder.CHECKLIST_KEYS)
        // Reuse the existing fake's initial values; androidTest has no direct VSS stub dependency.
        recorder.onDelta(0L, ParkingScenarios.good.steps.first().values)
        var snapshot = VehicleSnapshot()
        scenario.steps.forEach { step ->
            recorder.onDelta((step.atSeconds * 1000).toLong(), step.values)
            snapshot = snapshot.apply(step.values)
        }
        val score = checkNotNull(recorder.scoreChecklist())
        val phase = LessonPhase.Maneuver(SeedCatalog.predriveTask, LessonMode.GUIDE, 1, snapshot, null, null,
            score.metrics.motion.movingSegments, score.metrics.motion.totalMillis, true,
            registry.snapshot(), score.metrics.preDrive)
        return score to phase.toDisplayState()
    }

    private fun quizQuitFlow(activity: MainActivity) {
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        runOnMainSync {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            vm.restart()
            vm.demo?.stopCar()
        }
        render(activity) { LessonRoute(vm) }
        click("과제·모드 바꾸기")
        click("지식")
        click("시작")
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (vm.phase.value !is LessonPhase.Quiz && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        check(vm.phase.value is LessonPhase.Quiz) { "Knowledge briefing did not reach Quiz" }
        repeat(3) { index ->
            val quiz = vm.phase.value as LessonPhase.Quiz
            val answer = quiz.item.choices[quiz.item.answer]
            // The phase flow changes before Compose publishes the new question's accessibility tree.
            afterUiSettles("Quiz question ${index + 1}") {
                check(quiz.item.question in texts())
                check(buttonNode(answer).refresh())
            }
            click(answer)
            if (index < 2) click("다음 문제")
        }
        click("그만하기")
        val done = vm.phase.value as LessonPhase.QuizDone
        check(done.results.size == 3 && done.results.all { it.correct })
        check(texts().contains("메인으로") && texts().none { it == "시작" })
        capture("quiz-done-quit-three")
        click("메인으로")
        check(vm.phase.value is LessonPhase.Setup)
        check(allText().containsAll(listOf("시작", "과제·모드 바꾸기")))
        pass("Quiz quit: real LessonRoute retains all three correct answers in QuizDone; restart returns to Setup")
    }

    private fun onMainChecked(action: () -> Unit) {
        var failure: Throwable? = null
        runOnMainSync { try { action() } catch (caught: Throwable) { failure = caught } }
        failure?.let { throw it }
    }

    private fun round25bContract(activity: MainActivity) {
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        onMainChecked {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            vm.admin!!.applyPreset("rear-two")
            vm.admin!!.setCoachInput(CoachInputMode.OFF)
        }
        fun setup() = vm.phase.value as LessonPhase.Setup
        fun awaitQuiz() {
            repeat(150) {   // 브리핑이 음성 끝까지 머문다(최대 12 s, 라운드 26)
                if (vm.phase.value is LessonPhase.Quiz) return
                Thread.sleep(100)
            }
            error("Quiz did not begin after briefing: ${vm.phase.value}")
        }
        fun tagged(tag: String) = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
        fun hasTag(tag: String) = composeNodes(activity).any { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
        fun noInput() = onMainChecked { check(composeNodes(activity).none { it.config.contains(SemanticsActions.SetText) }) }
        render(activity) { LessonRoute(vm) }
        click("코치와 대화")
        noInput()
        onMainChecked { check(!hasTag("coach-speech-cards")) }
        check("말로 답하기 — 준비 중" in texts())
        click("돌아가기")
        render(activity) { AdminHome(vm.admin!!, setup(), {}) }
        listOf("끔" to CoachInputMode.OFF, "카드" to CoachInputMode.CARDS, "카드 + 글" to CoachInputMode.CARDS_AND_TEXT).forEach { (label, mode) ->
            if (vm.admin!!.coachInput.value != mode) click(label)
            afterUiSettles("Admin input $label") { check(vm.admin!!.coachInput.value == mode); assertSelected(label); check("음성 입력 · $label" in texts()) }
            assertFullText(activity, label)
        }
        capture("admin-home")
        click("카드")
        render(activity) { LessonRoute(vm) }
        click("코치와 대화")
        noInput(); check("보내기" !in texts() && "음성 입력 · 시뮬레이션" !in texts())
        val opening = setup().coach!!.cards
        check(opening.size == 4)
        val cardScale = designScale(activity)
        onMainChecked {
            val row = tagged("coach-speech-cards").boundsInWindow
            opening.forEach { card ->
                val node = tagged("speech-card-${card.id}")
                if (node.boundsInWindow.width > 0) {
                    check(node.boundsInWindow.height / cardScale >= 95)
                    check(abs(node.boundsInWindow.center.y - row.center.y) <= 3 * cardScale)
                }
                check(!node.config.contains(SemanticsProperties.Disabled))
            }
            check(hasTag("speech-cards-more"))
        }
        capture("setup-coach-cards-only")
        click(opening.first().text)
        afterUiSettles("Card enters dialogue and advances stage") {
            check(setup().coach!!.turns.any { it.fromDriver && it.text == opening.first().text })
            check(setup().coach!!.cards.all { it.stage == CardStage.FOLLOW_UP })
        }
        check(setup().coach!!.cards.none { it.needs == CardNeed.BOOKING || it.needs == CardNeed.LAST })
        capture("setup-coach-cards-follow")
        onMainChecked { check(tagged("speech-cards-scroll").config[SemanticsActions.ScrollBy].action?.invoke(10_000f, 0f) == true) }
        afterUiSettles("Last speech card is reachable") { check(buttonBounds("내 프로필 고칠래요").width() > 100) }
        click("내 프로필 고칠래요")
        afterUiSettles("Card profile intent consumed") { check("앱이 본 것" in texts() && !setup().profileRequest) }
        click("돌아가기")
        onMainChecked { vm.admin!!.setCoachInput(CoachInputMode.CARDS_AND_TEXT) }
        click("코치와 대화")
        afterUiSettles("Cards with composer") { check("코치에게 글로 말해 보세요" in texts() && "보내기" in texts()) }
        onMainChecked {
            check(tagged("coach-speech-cards").boundsInWindow.bottom < tagged("coach-text-input").boundsInWindow.top)
        }
        capture("setup-coach-cards")
        click("돌아가기")
        onMainChecked { vm.admin!!.applyPreset("reserved-exam"); vm.openCoach() }
        afterUiSettles("Booking opening cards") { check(setup().coach != null) }
        click(opening.first().text)
        afterUiSettles("Booking follow-up includes venue card") { check(setup().coach!!.cards.any { it.needs == CardNeed.BOOKING }) }
        onMainChecked { vm.closeCoach() }
        // The pure Real fixture shares the state, deliberately leaving the model input mode enabled.
        lateinit var real: LessonViewModel
        onMainChecked { real = LessonViewModel(container.lesson, container.tts) }
        render(activity) { LessonRoute(real) }
        click("코치와 대화"); noInput()
        onMainChecked { check(!hasTag("coach-speech-cards")); check(!hasTag("admin-home")); check(!hasTag("admin-band")) }
        check("음성 입력 · 시뮬레이션" !in texts())
        onMainChecked { vm.closeCoach() }

        var sends = 0
        val waiting = mutableStateOf(true)
        val dialog = CoachDialog("어떤 연습부터 함께할까요?", listOf(CoachChoice.PARKING_PRACTICE), cards = opening)
        render(activity) { PosterSurface { CoachTextSheet(dialog.copy(waiting = waiting.value), {}, {}, {}, CoachInputMode.CARDS, { sends++ }) } }
        noInput()
        onMainChecked { opening.forEach { check(tagged("speech-card-${it.id}").config.contains(SemanticsProperties.Disabled)) } }
        val first = textBounds(opening.first().text)
        tap(first.centerX(), first.centerY()); check(sends == 0)
        onMainChecked { waiting.value = false }
        afterUiSettles("Speech card enabled after reply") {
            onMainChecked { check(!tagged("speech-card-${opening.first().id}").config.contains(SemanticsProperties.Disabled)) }
        }
        tap(first.centerX(), first.centerY()); check(sends == 1)

        val signs = com.moah.hackathon.data.RoadSigns
        signs.quiz.forEachIndexed { index, item ->
            val figure = checkNotNull(item.figure)
            render(activity) { QuizScreen(signs.task, index, signs.quiz.size, item, false, null, 0, {}, {}, {}) }
            assertFullText(activity, item.question)
            // Each answer merges its number and sentence for accessibility. Measure the sentence leaf.
            onMainChecked {
                item.choices.forEach { choice ->
                    val leaf = composeNodes(activity, unmerged = true).single {
                        it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == choice
                    }
                    val layouts = mutableListOf<TextLayoutResult>()
                    check(leaf.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts) == true)
                    check(!layouts.single().hasVisualOverflow)
                    check(layouts.single().getLineEnd(layouts.single().lineCount - 1) == choice.length)
                }
            }
            lateinit var art: androidx.compose.ui.geometry.Rect
            onMainChecked { art = tagged("road-figure-${figure.name}").boundsInWindow }
            capture("quiz-road-${figure.name.lowercase()}") { bitmap ->
                for ((color, expected) in listOf(CoachColors.RoadMarkingYellow to (figure == RoadFigure.YELLOW_SOLID_CENTER),
                    CoachColors.RoadMarkingBlue to (figure == RoadFigure.BLUE_BUS_LANE))) {
                    val pixels = colorBounds(bitmap, Rect(0, 0, bitmap.width, bitmap.height), color.toArgb())
                    check(pixels.isEmpty != expected)
                    if (expected) check(pixels.left >= art.left && pixels.right <= art.right && pixels.top >= art.top && pixels.bottom <= art.bottom)
                }
            }
            if (index == 0) capture("quiz-road-sign")
            render(activity) { QuizScreen(signs.task, index, signs.quiz.size, item, false, item.answer, 1, {}, {}, {}) }
            assertFullText(activity, item.why)
            onMainChecked { check(hasTag("road-figure-${figure.name}")) }
            render(activity) { QuizScreen(signs.task, index, signs.quiz.size, item, true, null, 0, {}, {}, {}) }
            check(nodes().none(::hasTouchAction))
            check(allText().none { Regex("\\d").containsMatchIn(it) })
            onMainChecked { check(!hasTag("road-figure-${figure.name}")) }
        }
        onMainChecked { vm.admin!!.applyPreset("rear-two"); vm.admin!!.setCoachInput(CoachInputMode.OFF) }
        render(activity) { LessonRoute(vm) }
        click("과제·모드 바꾸기"); click("점검"); click(signs.task.title)
        assertSelected("지식 테스트")
        check("가이드" !in texts() && "힌트" !in texts() && "평가" !in texts())
        check(allText().none { Regex("\\d").containsMatchIn(it) })
        capture("setup-sheet-checklist")
        click("시작")
        awaitQuiz()
        afterUiSettles("Road sign card begins quiz") { check(signs.quiz.first().question in texts()) }
        onMainChecked { vm.restart() }
        render(activity) { LessonRoute(vm) }
        click("과제·모드 바꾸기"); click("지식")
        check(SeedCatalog.tasks.count { it.type == TaskType.KNOWLEDGE } == 6)
        capture("setup-sheet-knowledge")
        check(nodes().first { it.isScrollable }.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
        afterUiSettles("Last knowledge tasks reachable") { check("헷갈리는 상식" in texts()) }
        click("헷갈리는 상식")
        assertSelected("지식 테스트")
        check(allText().none { Regex("\\d").containsMatchIn(it) })
        capture("setup-sheet-knowledge-end")
        click("시작")
        awaitQuiz()
        afterUiSettles("Two-choice knowledge task") { check("맞아요" in texts() && "아니에요" in texts()) }
        onMainChecked { vm.restart() }
        round25bPracticeMap(activity)
        pass("Round25b: OFF/Real unchanged; three input modes; 96 dp scrolling cards, follow-up/context/profile and waiting guard; six road figures/color boundary/locks; six knowledge and two checklist cards, quiz-only mode and two-choice task")
    }

    private fun round25bPracticeMap(activity: MainActivity) {
        val course = com.moah.hackathon.data.TrackCourses.exam.forMode(evaluate = false)
        val task = SeedCatalog.tasks.single { it.course?.id == course.id }
        val recorder = CourseRecorder(course)
        var snapshot = VehicleSnapshot()
        val frames = mutableListOf<LessonPhase.Drive>()
        com.moah.hackathon.data.CourseScenarios.examPracticeS.steps.forEach { step ->
            val t = (step.atSeconds * 1000).toLong()
            recorder.onDelta(t, step.values); snapshot = snapshot.apply(step.values)
            val progress = recorder.progress()
            val point = progress.pose?.at
            if (point != null && when (frames.size) {
                0 -> progress.currentZoneId == "exam-s-curve" && point.x > 24f && point.y < 54f
                1 -> progress.currentZoneId == "exam-s-curve" && point.x > 48f && point.y < 54f
                2 -> point.x in 30f..40f && point.y in 54f..58f
                else -> false
            }) frames += LessonPhase.Drive(task, LessonMode.GUIDE, 1, snapshot, course, progress,
                course.zone("exam-s-curve")!!.guide, null, t, false,
                CourseRecorder.KEYS.associateWith { SignalAvailability.SIMULATED })
        }
        check(frames.size == 3)
        frames.forEachIndexed { index, state ->
            render(activity) { DriveScreen(state, null, {}, null) }
            check(nodes().none(::hasTouchAction)); assertNoScores()
            check(texts().any { it.contains("시험에는 없는 연습") })
            capture(if (index == 1) "drive-exam-practice-s" else "drive-exam-practice-s-$index") {
                assertCourseCar(activity, it, course, checkNotNull(state.progress.pose))
            }
        }
        pass("Round25b S practice: scenario-derived entry/turn/return poses render on practice route with simulation provenance and no moving touch targets")
    }

    private fun round25Locks(activity: MainActivity) {
        val container = (activity.application as App).container
        val fake = container.vehicle as FakeVehiclePort
        lateinit var vm: LessonViewModel
        lateinit var real: LessonViewModel
        onMainChecked {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            real = LessonViewModel(container.lesson, container.tts)
            vm.admin!!.applyPreset("rear-two")
            vm.admin!!.setBand(false) // The emergency gesture is independent of the admin band.
        }
        check(real.admin == null)
        fun awaitPhase(test: (LessonPhase) -> Boolean) {
            repeat(150) {   // 브리핑이 음성 끝까지 머문다(최대 12 s, 라운드 26 #230)
                if (test(vm.phase.value)) return
                Thread.sleep(100)
            }
            error("Round25 phase did not settle: ${vm.phase.value}")
        }
        fun locked() = when (val p = vm.phase.value) {
            is LessonPhase.Done -> p.locked
            is LessonPhase.Report -> p.locked
            is LessonPhase.Quiz -> p.locked
            is LessonPhase.QuizDone -> p.locked
            else -> false
        }
        fun hold(duration: Long) {
            val r = textBounds("DRIVE COACH")
            val down = SystemClock.uptimeMillis()
            fun event(action: Int) {
                val e = android.view.MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                    r.exactCenterX(), r.exactCenterY(), 0)
                check(uiAutomation.injectInputEvent(e, true)); e.recycle()
            }
            event(android.view.MotionEvent.ACTION_DOWN)
            Thread.sleep(duration)
            event(android.view.MotionEvent.ACTION_UP)
        }
        fun exercise(name: String) {
            // Use the same machine/signal stream with and without admin capability.
            render(activity) { LessonRoute(real) }
            fake.play(ParkingScenarios.bad)
            runBlocking { fake.holdSpeed(12f) }
            awaitPhase { locked() }
            afterUiSettles("Real $name lock") { check("운전에 집중해 주세요" in texts()) }
            check(allText().none { Regex("\\d|시연|관리자").containsMatchIn(it) })
            check(nodes().none(::hasTouchAction))
            hold(2_100)
            check(locked()) { "Real $name exposed a simulator gesture" }
            render(activity) { LessonRoute(vm) }
            val note = "시연 · 시나리오가 끝나면 차가 멈춰요 — 막히면 워드마크를 길게"
            check(note in texts())
            check(nodes().none(::hasTouchAction))
            check(allText().none { Regex("\\d|점수|감점").containsMatchIn(it) })
            assertFullText(activity, note)
            if (name == "done") capture("done-locked")
            hold(700)
            check(locked()) { "$name short hold stopped the car" }
            hold(2_100)
            awaitPhase { !locked() }
            afterUiSettles("$name unlocked") { check("운전에 집중해 주세요" !in texts()) }
            check(fake.playback.value == null)
            check(fake.speedOverrideKmh == 0f)
        }
        onMainChecked { vm.begin(SeedCatalog.predriveTask.id, LessonMode.GUIDE) }
        awaitPhase { it is LessonPhase.Maneuver }
        onMainChecked { vm.finishAttempt() }
        awaitPhase { it is LessonPhase.Done }
        exercise("done")
        onMainChecked { vm.endSession() }
        awaitPhase { it is LessonPhase.Report }
        exercise("report")
        onMainChecked {
            vm.restart()
            vm.begin(SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }.id, LessonMode.QUIZ)
        }
        awaitPhase { it is LessonPhase.Quiz }
        exercise("quiz")
        onMainChecked { vm.endSession() }
        awaitPhase { it is LessonPhase.QuizDone }
        exercise("quiz-done")
        onMainChecked { vm.restart(); vm.admin!!.setBand(true) }
        pass("Round25 locks: Done/Report/Quiz/QuizDone flat, no numbers or touch actions; Real has no hint or hold; demo short hold ignored, two-second hold stops at zero with band hidden")
    }

    private fun round24Contract(activity: MainActivity) {
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        onMainChecked {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            vm.restart(); vm.cancelReservation()
            vm.admin!!.setProfile(com.moah.hackathon.data.AdminPresets.PROFILE_RUSTY)
            vm.admin!!.applyPreset(com.moah.hackathon.data.AdminPresets.REAR_TWO)
            vm.admin!!.setTextInput(false)
            vm.admin!!.setBand(false)
        }
        render(activity) { LessonRoute(vm) }
        fun tagged(tag: String) = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
        fun assertNoInput() = onMainChecked {
            check(composeNodes(activity).none { it.config.contains(SemanticsActions.SetText) })
        }
        fun performTextInput(text: String) {
            onMainChecked {
                check(tagged("coach-text-input").config[SemanticsActions.SetText].action
                    ?.invoke(androidx.compose.ui.text.AnnotatedString(text)) == true)
            }
            afterUiSettles("Composer text committed") {
                onMainChecked { check(tagged("coach-text-input").config[SemanticsProperties.EditableText].text == text) }
            }
        }
        fun assertSendDisabled(expected: Boolean) = onMainChecked {
            val send = composeNodes(activity).single { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "보내기" } == true &&
                    node.config.contains(SemanticsActions.OnClick)
            }
            check(send.config.contains(SemanticsProperties.Disabled) == expected)
        }
        check("코치와 대화" in texts())
        click("코치와 대화")
        assertNoInput()
        check("보내기" !in texts() && "음성 입력 · 시뮬레이션" !in texts())
        check("말로 답하기 — 준비 중" in texts())
        click("돌아가기")
        onMainChecked { vm.admin!!.setTextInput(true) }
        afterUiSettles("Text input enabled") { check("코치와 대화" in texts()) }
        click("과제·모드 바꾸기"); click("전면 직각 주차"); click("돌아가기")
        afterUiSettles("Manual task before talking") { check("전면 직각 주차 ▼" in texts()) }
        click("코치와 대화")
        afterUiSettles("Composer") { check("코치에게 글로 말해 보세요" in texts()) }
        check("음성 입력 · 시뮬레이션" !in texts() && "말로 답하기 — 준비 중" !in texts())
        assertSendDisabled(true)
        performTextInput("   "); assertSendDisabled(true)
        performTextInput("안녕"); assertSendDisabled(false)
        click("보내기")
        afterUiSettles("Fake ask more") {
            val dialog = checkNotNull((vm.phase.value as LessonPhase.Setup).coach)
            check(dialog.turns.size == 2 && !dialog.waiting)
            check(dialog.turns.first() == CoachTurn(true, "안녕") && !dialog.turns.last().fromDriver)
            check(coachDisplayText(dialog.turns.last().text) in texts())
        }
        assertSendDisabled(true)
        check("코치에게 글로 말해 보세요" in texts())
        check(texts().none { Regex("\\d").containsMatchIn(it) })
        capture("setup-coach-text")
        performTextInput("후면 직각 주차"); click("보내기")
        afterUiSettles("Unchanged model recommendation replaces manual choice") {
            check((vm.phase.value as LessonPhase.Setup).coach == null)
            check("후면 직각 주차 ▼" in texts())
        }
        click("코치와 대화")
        performTextInput("평행 주차")
        onMainChecked { check(tagged("coach-text-input").config[SemanticsActions.OnImeAction].action?.invoke() == true) }
        afterUiSettles("Fake pins parallel parking from IME send") {
            check((vm.phase.value as LessonPhase.Setup).coach == null)
            check("평행 주차 ▼" in texts())
        }
        click("코치와 대화")
        afterUiSettles("Fresh conversation") { check("코치에게 글로 말해 보세요" in texts()) }
        check((vm.phase.value as LessonPhase.Setup).coach!!.turns.isEmpty())
        onMainChecked { check(tagged("coach-text-input").config[SemanticsActions.RequestFocus].action?.invoke() == true) }
        lateinit var connection: android.view.inputmethod.InputConnection
        afterUiSettles("Platform input connection") {
            onMainChecked {
                connection = checkNotNull(activity.currentFocus?.onCreateInputConnection(android.view.inputmethod.EditorInfo()))
            }
        }
        onMainChecked { check(connection.setComposingText("안녕", 1)) }
        afterUiSettles("Korean composition") {
            onMainChecked { check(tagged("coach-text-input").config[SemanticsProperties.EditableText].text == "안녕") }
        }
        onMainChecked { check(connection.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND)) }
        afterUiSettles("IME composing send clears the draft") {
            check((vm.phase.value as LessonPhase.Setup).coach!!.turns.size == 2)
            onMainChecked { check(tagged("coach-text-input").config[SemanticsProperties.EditableText].text.isEmpty()) }
        }
        performTextInput("프로필"); click("보내기")
        afterUiSettles("Profile request consumed") {
            check("앱이 본 것" in texts())
            check(!(vm.phase.value as LessonPhase.Setup).profileRequest)
        }
        click("돌아가기")
        afterUiSettles("Profile stays closed") { check("코치와 대화" in texts() && "앱이 본 것" !in texts()) }

        // A suspended reply fixture makes waiting deterministic without changing the model or ports.
        val dialog = mutableStateOf(CoachDialog("어떤 연습부터 함께할까요?", listOf(CoachChoice.PARKING_PRACTICE),
            listOf(CoachTurn(true, "안녕")), waiting = true,
            cards = com.moah.hackathon.data.SpeechCards.all.filter { it.stage == CardStage.FOLLOW_UP && it.needs == CardNeed.NONE }))
        var sent = 0
        var chosen: CoachChoice? = null
        render(activity) {
            SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, SeedCatalog.parkingTask, LessonMode.GUIDE,
                "함께 연습해요.", null, { _, _ -> }, coach = dialog.value, coachInput = CoachInputMode.CARDS_AND_TEXT,
                onSendCoachText = { sent++ }, onChooseCoach = { chosen = it })
        }
        assertSendDisabled(true)
        onMainChecked {
            check(tagged("coach-text-input").config.contains(SemanticsProperties.Disabled))
            check(tagged("coach-waiting").boundsInWindow.bottom <= tagged("coach-transcript").boundsInWindow.bottom)
        }
        check("…" in texts() && texts().none { Regex("\\d").containsMatchIn(it) })
        capture("setup-coach-waiting")
        check("주차 연습" !in texts() && "고르면 바로 그 자리로 가요." !in texts())
        check(chosen == null && sent == 0) { "Waiting cards must not dispatch a hidden choice or send" }
        onMainChecked {
            dialog.value = dialog.value.copy(waiting = false, turns = List(12) { index ->
                CoachTurn(index % 2 == 0, if (index % 2 == 0) "차분하게 연습하고 싶어요." else "괜찮아요. 함께 천천히 골라 봐요.")
            } + CoachTurn(false, "마지막 이야기까지 함께 보고 있어요."))
        }
        afterUiSettles("Transcript follows the last turn") {
            onMainChecked {
                val scroll = tagged("coach-transcript").config[SemanticsProperties.VerticalScrollAxisRange]
                check(scroll.maxValue() > 0 && scroll.value() == scroll.maxValue())
                val last = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text == "마지막 이야기까지 함께 보고 있어요." } == true }
                check(last.boundsInWindow.bottom <= tagged("coach-transcript").boundsInWindow.bottom)
                check(!tagged("coach-text-input").config.contains(SemanticsProperties.Disabled))
            }
        }
        onMainChecked { check(tagged("coach-transcript-fade").boundsInWindow.height > 0) }
        capture("setup-coach-long")
        performTextInput("보내기 확인"); click("보내기"); check(sent == 1)

        val setup = vm.phase.value as LessonPhase.Setup
        render(activity) { AdminHome(vm.admin!!, setup, {}) }
        check("시뮬레이션 음성 입력" in texts() && "음성 입력 · 카드 + 글" in texts())
        assertSelected("카드 + 글")
        val configNote = checkNotNull(aiLine(CopilotAuth.State.NoConfig)).detail
        assertFullText(activity, configNote)
        check(textBounds(configNote).bottom + 18 * designScale(activity) <= buttonBounds("이 설정으로 홈").top)
        capture("admin-home")
        click("끔")
        afterUiSettles("Admin input off") { check(!vm.admin!!.textInput.value && "음성 입력 · 끔" in texts()) }
        click("카드 + 글")
        afterUiSettles("Admin input on") { check(vm.admin!!.textInput.value) }
        lateinit var real: LessonViewModel
        onMainChecked { real = LessonViewModel(container.lesson, container.tts) }
        check(real.admin == null)
        render(activity) { LessonRoute(real) }
        check("코치와 대화" in texts())
        click("코치와 대화"); assertNoInput()
        check("보내기" !in texts() && "시뮬레이션 음성 입력" !in texts())
        onMainChecked { vm.closeCoach(); vm.admin!!.setTextInput(false); vm.admin!!.setBand(true) }
        pass("Round24: off/Real have no input; admin toggle; Korean SetText + button/IME send; Fake ask-more/parallel/profile; fresh draft; waiting disables input/send and cards hide legacy choices; transcript follows last turn; no digits")
    }

    private fun round23bContract(activity: MainActivity) {
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        runOnMainSync {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            vm.restart()
            vm.admin!!.resetProfile()
            vm.admin!!.setBand(true)
        }
        render(activity) { LessonRoute(vm) }
        check("건너뛰기" in texts() && "시작하기" !in texts())
        check(ProfileField.entries.all { it.title in texts() })
        check(texts().none { Regex("\\d|마이크|말로 답하기").containsMatchIn(it) })
        capture("profile-onboarding")
        click("주차")
        afterUiSettles("Second onboarding question") { check(ProfileField.LAST_DRIVE.question in texts()) }
        click("십 년 넘게")
        afterUiSettles("Two questions complete") {
            check("시작하기" in texts())
            check(ProfileField.entries.none { it.question in texts() })
        }
        capture("profile-onboarding-complete")
        click("시작하기")
        afterUiSettles("Onboarding closes") { check("과제·모드 바꾸기" in allText()) }
        val setup = vm.phase.value as LessonPhase.Setup
        click(profileLine(setup.profile))
        afterUiSettles("Profile sheet") { check("앱이 본 것" in texts()) }
        check(ProfileField.entries.none { it.question in texts() })
        capture("profile-sheet")
        click("필요한 일")
        click("아이 등하원")
        afterUiSettles("Next empty profile field") { check(ProfileField.LICENSE.question in texts()) }
        click("작년쯤")
        afterUiSettles("Car last") { check(ProfileField.CAR.question in texts()) }
        click("준중형")
        click("돌아가기")
        afterUiSettles("Profile persisted") {
            val stored = checkNotNull(FileProfileStore(File(targetContext.filesDir, "profile.properties")).load())
            check(stored.onboarded && stored.answered.size == 5)
            check(stored.statement.goal == "아이 등하원" && stored.statement.car == "준중형")
        }
        fun holdBrand(duration: Long) {
            val rect = textBounds("DRIVE COACH")
            val now = SystemClock.uptimeMillis()
            fun event(action: Int) {
                val e = android.view.MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, rect.centerX().toFloat(), rect.centerY().toFloat(), 0)
                check(uiAutomation.injectInputEvent(e, true)); e.recycle()
            }
            event(android.view.MotionEvent.ACTION_DOWN)
            Thread.sleep(duration)
            event(android.view.MotionEvent.ACTION_UP)
            Thread.sleep(350)
        }
        holdBrand(700)
        check("시연 준비" !in texts())
        holdBrand(2_100)
        afterUiSettles("Admin home") { check("시연 준비" in texts()) }
        capture("admin-home")
        vm.admin!!.presets.forEach { preset ->
            click(preset.title)
            afterUiSettles("Preset ${preset.id}") {
                val state = vm.phase.value as LessonPhase.Setup
                check(state.suggestedTask.id == preset.taskId && state.suggestedMode == preset.mode)
            }
        }
        click("이 설정으로 홈")
        click("더 보기 ▴")
        afterUiSettles("Expanded band on booking home") {
            check(abs(buttonBounds("코치와 대화").height() / designScale(activity) - 112) <= 1)
            check(abs(buttonBounds("시작").height() / designScale(activity) - 140) <= 1)
            assertBandBelow(activity, "코치와 대화")
        }
        capture("admin-band-booking")
        click("접기 ▾")
        holdBrand(2_100)
        click("초보")
        click("숨김")
        click("기록 초기화")
        check(!vm.admin!!.bandVisible.value)
        check((vm.phase.value as LessonPhase.Setup).profile.observation.attempts == 0)
        click("이 설정으로 홈")
        check("관리자" !in texts())
        holdBrand(2_100)
        click("아래 띠")
        click("프로필 초기화")
        click("이 설정으로 홈")
        check("건너뛰기" in texts())
        click("건너뛰기")
        check((vm.phase.value as LessonPhase.Setup).onboarding == null)
        check((vm.phase.value as LessonPhase.Setup).profileRows.all { it.answer == null })
        check(abs(buttonBounds(profileLine((vm.phase.value as LessonPhase.Setup).profile)).height() / designScale(activity) - 52) <= 2)
        lateinit var real: LessonViewModel
        runOnMainSync { real = LessonViewModel(container.lesson, container.tts) }
        check(real.admin == null)
        render(activity) { LessonRoute(real) }
        runOnMainSync { check(composeNodes(activity).none { it.config.contains(SemanticsActions.OnLongClick) }) }
        check("관리자" !in texts() && "시연" !in allText())
        runOnMainSync {
            activity.startActivity(Intent(activity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(com.moah.hackathon.data.AdminPresets.EXTRA_PRESET, "rear-two"))
        }
        afterUiSettles("Warm preset intent") { check((vm.phase.value as LessonPhase.Setup).suggestedMode == LessonMode.HINT) }
        check((vm.phase.value as LessonPhase.Setup).onboarding == null)
        pass("Round23b: two-second Setup entry, five presets, profile/panel/reset actions, P2 two questions + five-row sheet + persisted answers, Real has no entry or band, preset intent")
    }

    private fun round22Contract(activity: MainActivity) {
        val task = SeedCatalog.parkingTask
        fun attempt(index: Int, scenario: Scenario): AttemptRecord {
            val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
            scenario.steps.forEach { recorder.onDelta((it.atSeconds * 1000).toLong(), it.values) }
            return AttemptRecord(index, task.id, LessonMode.HINT, checkNotNull(recorder.score()), null,
                "차분히 연습을 마쳤어요.", 0, verdict = recorder.verdict())
        }
        val bad = attempt(1, ParkingScenarios.bad)
        val good = attempt(2, ParkingScenarios.good)
        val visible = mutableStateOf(true)
        render(activity) { if (visible.value) DoneScreen(task, 2, good, null, { visible.value = false }, {}) else LessonText("다음 회차") }
        click("한 번 더")
        check("다음 회차" in texts())
        val report = LessonReport(task, LessonMode.HINT, listOf(bad, good), good.score, "차분히 연습을 마쳤어요.",
            task, LessonMode.GUIDE, "핸들 타이밍을 한 단계씩 함께 익혀요.", ShareLevel.entries, SeedCatalog.benefits, emptyList())
        render(activity) { ReportScreen(report, {}) }
        click("자세히 보기")
        assertMetricRow(activity, "조향 왕복", "3"); assertMetricRow(activity, "조향 왕복", "1")
        assertCompactProvenance(activity, report)
        capture("details-round22")
        click("돌아가기"); click("진단서")
        assertCertificateLayout(activity, report)
        capture("certificate")
        reservationFlow(activity)
        captureDoneSettle(activity, task, good)
    }

    private fun assertCertificateLayout(activity: MainActivity, report: LessonReport) {
        assertActionGap(activity, "메인으로")
        assertCompactProvenance(activity, report, aboveActions = true)
        val back = buttonBounds("돌아가기")
        val scale = designScale(activity)
        check(abs(back.centerY() - buttonBounds("메인으로").centerY()) <= 1)
        runOnMainSync {
            val nodes = composeNodes(activity, unmerged = true)
            val link = nodes.single { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == "공유 예시 보기" }
            val linkLayouts = mutableListOf<TextLayoutResult>()
            check(link.config[SemanticsActions.GetTextLayoutResult].action?.invoke(linkLayouts) == true)
            val linkLayout = linkLayouts.single()
            // TextAction includes 16 dp vertical padding on each side; its intrinsic width rounds to a pixel.
            check(linkLayout.lineCount == 1 && linkLayout.getLineEnd(0) == "공유 예시 보기".length &&
                linkLayout.getLineRight(0) <= link.boundsInWindow.width + 1 &&
                link.boundsInWindow.height >= linkLayout.multiParagraph.height + 32 * scale - 1 &&
                link.boundsInWindow.bottom < back.top) { "Clipped preview link: ${link.boundsInWindow}, ${linkLayout.size}" }
            val cards = report.shareLevels.reversed().map { level ->
                nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "share-${level.name}" }
            }
            check(cards.zipWithNext().all { (a, b) -> a.boundsInWindow.bottom < b.boundsInWindow.top })
            check(cards.first().boundsInWindow.top > 0 && cards.last().boundsInWindow.bottom < back.top)
            check(nodes.none { it.config.getOrNull(SemanticsActions.ScrollBy) != null }) { "Certificate must not need scrolling" }
            report.shareLevels.forEach { level ->
                val card = cards.single { it.config[SemanticsProperties.TestTag] == "share-${level.name}" }
                listOf(level.label, level.description, level.benefit, "${level.condition} · 예시").forEach { text ->
                    val node = nodes.single { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == text }
                    val layouts = mutableListOf<TextLayoutResult>()
                    check(node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts) == true)
                    check(!layouts.single().hasVisualOverflow && card.boundsInWindow.contains(node.boundsInWindow.topLeft) &&
                        card.boundsInWindow.contains(node.boundsInWindow.bottomRight - androidx.compose.ui.geometry.Offset(1f, 1f))) { "Clipped scope card: $text" }
                }
            }
        }
    }

    private fun assertShareExample(activity: MainActivity, scope: ShareLevel) {
        check("공유 예시 · ${scope.label}" in texts())
        check("위치 · 대화 · 음성 없음" in texts())
        check("메인으로" !in texts())
        val backTop = buttonBounds("돌아가기").top
        runOnMainSync {
            val nodes = composeNodes(activity)
            ShareLevel.RAW.includes.forEach { item ->
                val node = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "share-item-$item" }
                check(node.config[SemanticsProperties.StateDescription] == if (item in scope.includes) "포함" else "범위 밖")
                check(node.boundsInWindow.bottom < backTop)
            }
        }
        click("기관"); assertSelected("기관")
    }

    private fun round23aContract(activity: MainActivity) {
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        runOnMainSync {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            checkNotNull(vm.admin).resetRecords()
            checkNotNull(vm.admin).setProfile(com.moah.hackathon.data.AdminPresets.PROFILE_RUSTY)
            vm.restart(); vm.cancelReservation()
        }
        render(activity) { LessonRoute(vm) }
        fun awaitHome() = afterUiSettles("coach home") { check("코치와 대화" in texts()) }
        fun assertLinkGap() {
            val start = buttonBounds("시작")
            val coach = buttonBounds("코치와 대화")
            check(abs((coach.top - start.bottom) / designScale(activity) - 57) <= 1)
            check(buttonBounds("과제·모드 바꾸기").bottom < start.top)
            check(abs(coach.right - start.right) <= 1) { "Coach pill must align with start right edge" }
        }
        val profile = buttonBounds(profileLine((vm.phase.value as LessonPhase.Setup).profile))
        check(abs(profile.height() / designScale(activity) - 52) <= 2)
        val screenshot = screenshot()
        val margin = (20 * designScale(activity)).roundToInt()
        val closeup = Bitmap.createBitmap(screenshot, profile.left - margin, profile.top - margin,
            profile.width() + 2 * margin, profile.height() + 2 * margin)
        File(targetContext.filesDir, "lesson-profile-pill.png").outputStream().use { closeup.compress(Bitmap.CompressFormat.PNG, 100, it) }
        closeup.recycle(); screenshot.recycle()
        click(profileLine((vm.phase.value as LessonPhase.Setup).profile))
        check("앱이 본 것" in texts())
        click("돌아가기"); awaitHome()
        assertLinkGap(); capture("setup-coach")
        click("코치와 대화")
        check("주차 연습" in texts() && "예약한 시험장으로" !in texts() && "지난번 이어서" !in texts())
        runOnMainSync {
            val microphone = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "말로 답하기 — 준비 중" } == true }
            check(microphone.config.contains(SemanticsProperties.Disabled) && microphone.config.getOrNull(SemanticsActions.OnClick) == null)
        }
        click("주차 연습")
        afterUiSettles("coach parking sheet") { assertSelected("주차") }
        runOnMainSync { check((vm.phase.value as LessonPhase.Setup).sheetRequest == null) }
        click("돌아가기"); awaitHome()
        click("과제·모드 바꾸기"); assertSelected("주차")
        click("지식"); click("돌아가기"); awaitHome()
        val manualTaskLine = texts().single { it.endsWith("▼") && !it.startsWith("가이드") && !it.startsWith("힌트") && !it.startsWith("평가") && !it.startsWith("지식 테스트") }
        click("코치와 대화"); click("돌아가기"); awaitHome()
        check(manualTaskLine in texts()) { "Closing coach replaced a manual task choice" }

        val recorder = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        ParkingScenarios.good.steps.forEach { recorder.onDelta((it.atSeconds * 1000).toLong(), it.values) }
        val task = SeedCatalog.parkingTask
        val last = AttemptRecord(1, task.id, LessonMode.HINT, checkNotNull(recorder.score()), null,
            "차분히 연습을 마쳤어요.", 0, verdict = recorder.verdict())
        val venue = SeedCatalog.venues.first()
        runOnMainSync {
            container.store.add(last)
            vm.restart()
            vm.reserve(venue.id, venue.slots.first { it.available }.id,
                venue.courses.single { SeedCatalog.TASK_TRACK_EXAM in it.taskIds }.id)
        }
        awaitHome()
        afterUiSettles("booking card") { check("예약 · 서초 14:00" in texts()) }
        assertLinkGap()
        fun assertBookingGaps() {
            val scale = designScale(activity)
            val startTop = buttonBounds("시작").top
            runOnMainSync {
                val nodes = composeNodes(activity)
                val card = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "home-booking-card" }.boundsInWindow
                val reason = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "home-reason" }.boundsInWindow
                check(abs((card.top - reason.bottom) / scale - 33) <= 1)
                check(abs((startTop - card.bottom) / scale - 33) <= 1)
            }
        }
        assertBookingGaps(); capture("setup-booking-card")
        click("코치와 대화")
        CoachChoice.entries.forEach { assertFullText(activity, it.label) }
        check(texts().containsAll(CoachChoice.entries.map { it.label }))
        capture("setup-coach-sheet")
        click("예약한 시험장으로"); awaitHome()
        runOnMainSync { check((vm.phase.value as LessonPhase.Setup).highlightBooking) }
        capture("setup-booking-highlight")
        click("모의시험")
        afterUiSettles("booking evaluate") { assertSelected("모의시험"); check(texts().containsAll(listOf("장내기능 모의시험 ▼", "평가 ▼"))) }
        runOnMainSync { check((vm.phase.value as LessonPhase.Setup).let { !it.highlightBooking && it.suggestedMode == LessonMode.EVALUATE }) }
        assertBookingGaps(); assertLinkGap(); capture("setup-booking-mock")
        click("시작")
        runOnMainSync {
            val state = vm.phase.value
            check(when (state) {
                is LessonPhase.Briefing -> state.task.id == SeedCatalog.TASK_TRACK_EXAM && state.mode == LessonMode.EVALUATE
                is LessonPhase.Drive -> state.task.id == SeedCatalog.TASK_TRACK_EXAM && state.mode == LessonMode.EVALUATE
                else -> false
            }) { "Booking did not begin its exact task/mode: $state" }
            vm.restart()
        }
        awaitHome(); click("코스 연습")
        afterUiSettles("booking practice") { assertSelected("코스 연습") }
        runOnMainSync { check((vm.phase.value as LessonPhase.Setup).suggestedMode in listOf(LessonMode.GUIDE, LessonMode.HINT)) }
        click("코치와 대화"); click("주차 연습")
        afterUiSettles("parking request with booking choice") { assertSelected("주차"); assertSelected(task.title) }
        click("돌아가기"); awaitHome()
        click("코치와 대화"); click("지난번 이어서"); awaitHome()
        afterUiSettles("continue exact last task") { check(texts().containsAll(listOf("${task.title} ▼", "힌트 ▼"))) }
        runOnMainSync { vm.cancelReservation() }

        val report = LessonReport(task, LessonMode.HINT, listOf(last), last.score, last.remark,
            task, LessonMode.GUIDE, "함께 연습해요.", ShareLevel.entries, SeedCatalog.benefits, emptyList())
        render(activity) { ReportScreen(report, {}) }
        click("진단서"); assertCertificateLayout(activity, report)
        capture("certificate-round23a")
        ShareLevel.entries.forEach { level ->
            // Android exposes an already selected radio as checked, without a redundant click action.
            if (!isSelected(level.label)) click(level.label)
            click("공유 예시 보기")
            assertShareExample(activity, level)
            click("돌아가기"); assertSelected(level.label)
        }
        pass("Round23a: three complete scope cards without scrolling, horizontal actions, preview inclusions/recipient; A1 gaps, conditional coach chips, disabled mic, consumed parking request, reservation highlight/options and exact begin/continue")
    }

    private fun assertReserveDisabled(activity: MainActivity) {
        var bounds = androidx.compose.ui.geometry.Rect.Zero
        onMainChecked {
            val reserve = composeNodes(activity).single { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "예약" } == true &&
                    node.config.contains(SemanticsProperties.Disabled)
            }
            bounds = reserve.boundsInWindow
        }
        check("시간과 코스를 고르면 예약할 수 있어요." in texts())
        tap(bounds.center.x.roundToInt(), bounds.center.y.roundToInt())
        check("예약 확인" !in texts()) { "Disabled reserve accepted a tap" }
        onMainChecked {
            val reserve = composeNodes(activity).single { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "예약" } == true &&
                    node.config.contains(SemanticsProperties.Disabled)
            }
            check(reserve.boundsInWindow == bounds)
        }
    }

    private fun assertActionGap(activity: MainActivity, primary: String) {
        val secondary = buttonBounds("돌아가기")
        val main = buttonBounds(primary)
        check((main.left - secondary.right) / designScale(activity) >= 63.5f) { "Action gap: $secondary / $main" }
    }

    private fun assertMetricRow(activity: MainActivity, label: String, value: String) = runOnMainSync {
        val nodes = composeNodes(activity, unmerged = true)
        fun matching(text: String) = nodes.filter { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == text }
        val pair = matching(label).firstNotNullOfOrNull { name ->
            matching(value).firstOrNull { number -> number.boundsInWindow.left > name.boundsInWindow.right &&
                abs(number.boundsInWindow.center.y - name.boundsInWindow.center.y) < 2 }?.let { name to it }
        }
        checkNotNull(pair) { "Missing aligned metric row: $label / $value" }
        listOf(pair.first, pair.second).forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            check(node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts) == true)
            check(layouts.single().lineCount == 1 && !layouts.single().hasVisualOverflow) { "Clipped metric: $label / $value" }
        }
    }

    private fun assertCompactProvenance(activity: MainActivity, report: LessonReport, aboveActions: Boolean = false) {
        val badge = badgeText(report.best.badge)
        if (aboveActions) check(textBounds(badge).bottom < buttonBounds("돌아가기").top)
        else {
            check(textBounds(badge).left > buttonBounds("돌아가기").right)
            check(abs(textBounds(badge).centerY() - buttonBounds("돌아가기").centerY()) < 40 * designScale(activity))
        }
        assertFullText(activity, badge)
        assertFullText(activity, "신호 출처 · ")
    }

    /** Explicit animation timestamps also capture the first composed Done frame before TTS catches up. */
    private fun captureDoneSettle(activity: MainActivity, task: Task, record: AttemptRecord, name: String = "done-settle-strip") {
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(AndroidUiDispatcher.Main + clock)
        val recomposer = Recomposer(scope.coroutineContext)
        val subtitle = mutableStateOf<String?>("다 되셨나요? 다 됐으면 버튼을 눌러 주세요.")
        lateinit var view: ComposeView
        runOnMainSync {
            view = ComposeView(activity).apply {
                setParentCompositionContext(recomposer)
                setContent { MaterialTheme(colorScheme = coachColorScheme()) { DesignScale {
                    DoneScreen(task, record.index, record, subtitle.value, {}, {})
                } } }
            }
            activity.setContentView(view)
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
        }
        val frames = mutableListOf<Bitmap>()
        val ys = mutableListOf<Float>()
        try {
            for (index in 0..2) {
                runOnMainSync { if (index > 0) subtitle.value = if (index == 1) record.remark else null }
                repeat(3) {
                    runOnMainSync { clock.sendFrame((16L + index * 500) * 1_000_000) }
                    waitForIdleSync(); Thread.sleep(40)
                }
                runOnMainSync {
                    val action = composeNodes(activity).first { node ->
                        node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "한 번 더" } == true &&
                            node.config.getOrNull(SemanticsActions.OnClick) != null
                    }
                    ys += action.boundsInWindow.top
                }
                frames += screenshot()
            }
            check(ys.distinct().size == 1) { "Done action moved at 0/500/1000 ms: $ys" }
            val strip = Bitmap.createBitmap(640 * 3, 396, Bitmap.Config.ARGB_8888)
            try {
                val canvas = android.graphics.Canvas(strip)
                canvas.drawColor(CoachColors.Paper.toArgb())
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = CoachColors.Ink.toArgb(); textSize = 22f; isFilterBitmap = true
                }
                frames.forEachIndexed { index, frame ->
                    canvas.drawText("${index * 500} ms / y=${ys[index]}", index * 640f + 12, 26f, paint)
                    canvas.drawBitmap(frame, null, Rect(index * 640, 36, (index + 1) * 640, 396), paint)
                }
                File(targetContext.filesDir, "lesson-$name.png").outputStream().use { strip.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally { strip.recycle() }
            pass("$name: Done pill y at 0/500/1000 ms = $ys")
        } finally {
            frames.forEach { it.recycle() }
            runOnMainSync {
                view.disposeComposition(); recomposer.cancel(); scope.cancel()
                // Activity.setContent reuses its ComposeView. Drop the custom clock's root so
                // subsequent fixtures get a live window recomposer instead of this cancelled one.
                activity.setContentView(android.widget.FrameLayout(activity))
            }
        }
    }

    private fun sheetHeadingContract(activity: MainActivity) {
        val scale = designScale(activity)
        runOnMainSync {
            fun text(label: String) = composeNodes(activity).first {
                it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == label
            }
            listOf("연습할 과제", "주차 세부 과제").forEach { label ->
                val layouts = mutableListOf<TextLayoutResult>()
                check(text(label).config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts) == true)
                check(layouts.single().layoutInput.style.fontSize == 32.sp)
                check(layouts.single().layoutInput.style.color == CoachColors.Periwinkle)
            }
            check((text("주차").boundsInWindow.top - text("연습할 과제").boundsInWindow.bottom) / scale >= 40f)
            val card = composeNodes(activity).first { node ->
                node.config.getOrNull(SemanticsProperties.Selected) == true &&
                    node.boundsInWindow.height > 400 * scale && node.boundsInWindow.width < 500 * scale
            }
            check(abs((card.boundsInWindow.top - text("주차 세부 과제").boundsInWindow.bottom) / scale - 24f) <= 1f)
        }
    }

    private fun guideBaselineContract(activity: MainActivity, step: String) = runOnMainSync {
        fun baseline(label: String): Float {
            val node = composeNodes(activity).first { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == label }
            val layouts = mutableListOf<TextLayoutResult>()
            check(node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts) == true)
            return node.boundsInWindow.top + layouts.single().firstBaseline
        }
        check(abs(baseline("코치") - baseline(step)) <= 1f) { "Coach and step baselines differ" }
    }

    private fun reservationFlow(activity: MainActivity) {
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        runOnMainSync {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            // #182 뒤 앱은 저장된 프로필이 없으면 빈 프로필로 시작한다 — 이 흐름은 시연 프로필의 눈썹 문구를 기준으로 본다
            checkNotNull(vm.admin) { "demo build has admin controls" }.setProfile(com.moah.hackathon.data.AdminPresets.PROFILE_RUSTY)
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
                onCancelReservation = { cancelCalls++; vm.cancelReservation() },
                bookingOptions = setup.bookingOptions, bookingChoice = setup.bookingChoice,
                highlightBooking = setup.highlightBooking, onChooseBooking = vm::chooseBooking)
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
        assertReserveDisabled(activity)
        assertReservationNumbers()
        capture("venues") { bitmap ->
            SeedCatalog.venues.forEach { venue ->
                val bounds = buttonBounds(venue.name)
                check(kotlin.math.abs(bounds.height() / designScale(activity) - 512f) <= 1f)
                check(bitmap.getPixel(bounds.left + 2, bounds.top + 2) == CoachColors.Paper.toArgb())
                val map = Rect(bounds.left + 12, bounds.top + 12, bounds.right - 12,
                    bounds.top + (180 * designScale(activity)).roundToInt())
                check(bitmap.getPixel(map.left, map.top) == CoachColors.Ink.toArgb())
                check(!colorBounds(bitmap, map, CoachColors.Periwinkle.toArgb()).isEmpty)
            }
        }
        clickAndAwait(seocho.name) { texts().containsAll(seocho.slots.map { it.label }) }
        check(texts().containsAll(seocho.slots.map { it.label }))
        assertPlannedTask(seocho.slots.single { !it.available }.label)
        assertReserveDisabled(activity)
        capture("venue-slots")
        clickAndAwait(parking.title) { isSelected(parking.title) }
        assertReserveDisabled(activity)
        clickAndAwait(seocho.slots.first().label) { isSelected(seocho.slots.first().label) && "예약" in texts() }
        val closed = textBounds(seocho.slots.single { !it.available }.label)
        tap(closed.centerX(), closed.centerY())
        assertSelected(seocho.slots.first().label)
        check("마감" in texts())
        assertDriverButton(activity, "예약")
        clickAndAwait(gangnam.name) { isSelected(gangnam.name) && "예약" in texts() }
        assertReserveDisabled(activity)
        assertPlannedTask(gangnam.slots.single { !it.available }.label)
        clickAndAwait(gangnam.slots.first { it.available }.label) { isSelected(gangnam.slots.first { it.available }.label) }
        assertReserveDisabled(activity)
        click("돌아가기")
        check("시간" !in texts()); assertReserveDisabled(activity)
        clickAndAwait(seocho.name) { texts().containsAll(seocho.slots.map { it.label }) }
        clickAndAwait(seocho.slots.first().label) { isSelected(seocho.slots.first().label) }
        assertReserveDisabled(activity)
        clickAndAwait(parking.title) { isSelected(parking.title) && "예약" in texts() }
        assertSelected(seocho.name)
        assertSelected(seocho.slots.first().label)
        assertSelected(parking.title)
        assertDriverButton(activity, "예약")
        assertReservationNumbers()
        capture("venue-slots-ready") { bitmap ->
            val card = buttonBounds(seocho.name)
            check(bitmap.getPixel(card.left + 2, card.top + 2) == CoachColors.Periwinkle.toArgb())
            check(bitmap.getPixel(card.left + 12, card.bottom - 12) == CoachColors.Periwinkle.toArgb())
            assertSelected(seocho.name)
            assertActionGap(activity, "예약")
            val map = Rect(card.left + 12, card.top + 12, card.right - 12,
                card.top + (180 * designScale(activity)).roundToInt())
            check(!colorBounds(bitmap, map, CoachColors.Signal.toArgb()).isEmpty) { "Selected venue route must be red" }
            val unavailable = textBounds(seocho.slots.single { !it.available }.label)
            check(!colorBounds(bitmap, unavailable, CoachColors.Ink.copy(alpha = .35f).compositeOver(CoachColors.Lavender).toArgb(), tolerance = 1).isEmpty)
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
        val badge = "예약 · 서초 14:00"
        check(texts().contains(badge))
        check(textBounds(badge).top > textBounds(profileLine(SeedCatalog.demoProfile)).bottom)
        check(textBounds(badge).bottom < buttonBounds("시작").top)
        check(texts().containsAll(listOf("${SeedCatalog.parkingTask.title} ▼", "가이드 ▼")))
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
        check(texts().contains("예약한 시험장을 누르면 확인할 수 있어요."))
        capture("venues-booked") { bitmap ->
            val bounds = buttonBounds(seocho.name)
            check(bitmap.getPixel(bounds.left + 2, bounds.top + 2) == CoachColors.Paper.toArgb())
            check(!colorBounds(bitmap, textBounds("예약됨"), CoachColors.Signal.toArgb()).isEmpty)
        }
        clickAndAwait(seocho.name) { "예약 확인" in texts() }
        check(texts().contains("예약 확인"))
        runOnMainSync { check(reserveCalls == 1) }
        click("취소")
        runOnMainSync { check(cancelCalls == 1 && container.store.reservation == null) }
        check(texts().containsAll(SeedCatalog.venues.map { it.name }))
        check(texts().none { it == "예약됨" || it == "예약 확인" })
        click("돌아가기")
        click("돌아가기")
        check(texts().none { it.startsWith("예약 · ") })
        val profileLabel = profileLine(SeedCatalog.demoProfile)
        val deadline = SystemClock.uptimeMillis() + 1_000
        while (textBounds(profileLabel) != originalProfileBounds && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(50)
        }
        check(textBounds(profileLabel) == originalProfileBounds) {
            "Cancelled badge left empty space after morph settled: ${textBounds(profileLabel)} / $originalProfileBounds"
        }
        pass("Reservation: three 512 dp map cards, Periwinkle selected card/Paper name/red route, unavailable slot disabled, both choices required/reset per venue, reserve/cancel once, real ViewModel recommendation, confirmation/reopen, badge without leftover space")
    }

    /** Selection/navigation only: never retry reserve/cancel callbacks. Reacquire after a missed tap. */
    private fun clickAndAwait(label: String, expected: () -> Boolean) {
        repeat(2) { attempt ->
            if (attempt > 0 && expected()) return
            check(buttonNode(label).performAction(AccessibilityNodeInfo.ACTION_CLICK))
            val deadline = SystemClock.uptimeMillis() + 1_000
            do {
                if (expected()) return
                Thread.sleep(50)
            } while (SystemClock.uptimeMillis() < deadline)
        }
        error("$label did not reach the expected state after one retry (1 s polling per tap)")
    }

    private fun isSelected(label: String) = nodes().any { node ->
        (node.isSelected || node.isChecked || node.stateDescription?.toString() in listOf("Selected", "선택됨")) &&
            descendants(node).any { it.text?.toString() == label }
    }

    private fun assertReservationNumbers() {
        allText().forEach { text ->
            val withoutAllowed = text.replace(Regex("\\d{2}:\\d{2}|\\d+(?:\\.\\d+)? km|주차 3종"), "")
            check(!Regex("\\d|점수|감점").containsMatchIn(withoutAllowed)) { "Reservation leaked other numbers: $text" }
        }
    }

    private fun checkAiPanel(activity: MainActivity, state: MutableState<StateFlow<CopilotAuth.State>?>) {
        check(allText().none { it.startsWith("AI ") })
        val auth = MutableStateFlow<CopilotAuth.State>(CopilotAuth.State.NeedsLogin)
        runOnMainSync { state.value = auth }
        fun awaitState(value: CopilotAuth.State) {
            runOnMainSync { auth.value = value }
            val line = checkNotNull(aiLine(value))
            afterUiSettles("Admin AI ${line.title}") {
                check(line.title in texts())
                check(("AI 연결" in texts()) == line.showConnect)
                if (value is CopilotAuth.State.Code) {
                    check(value.uri in texts() && value.userCode in texts())
                    assertFullText(activity, value.uri)
                    assertFullText(activity, value.userCode)
                } else {
                    check(line.detail in texts())
                    assertFullText(activity, line.detail)
                }
            }
        }
        awaitState(CopilotAuth.State.NeedsLogin)
        click("AI 연결")
        awaitState(CopilotAuth.State.Code("ABCD-1234", "https://github.com/login/device"))
        check("AI 로그인" in texts())
        check(textBounds("AI 로그인").bottom <= textBounds("https://github.com/login/device").top)
        capture("admin-band-ai-code")
        awaitState(CopilotAuth.State.Ready)
        awaitState(CopilotAuth.State.NoConfig)
        awaitState(CopilotAuth.State.Error("연결 상태를 확인할 수 없어요. 잠시 후 다시 연결해 주세요. ".repeat(3).take(80)))
        click("AI 연결")
        runOnMainSync { state.value = null }
        afterUiSettles("AI disabled") { check(allText().none { it.startsWith("AI ") }) }
        pass("Admin band: null and five live AI states; connect/reconnect and complete device URI/code")
    }

    // The full course suite is also runnable with -e round18Only true. Keep the existing
    // lesson_shots 300-second budget and every legacy assertion; its default adds Drive safety.
    private fun courseContract(activity: MainActivity, full: Boolean = true) {
        val courses = com.moah.hackathon.data.TrackCourses
        val scenarios = com.moah.hackathon.data.CourseScenarios
        fun task(course: TrackCourse) = SeedCatalog.tasks.single { it.course?.id == course.id }
        fun frame(course: TrackCourse, scenario: Scenario, predicate: (CourseProgress, VehicleSnapshot) -> Boolean): LessonPhase.Drive {
            val recorder = CourseRecorder(course)
            var snapshot = VehicleSnapshot()
            for (step in scenario.steps) {
                recorder.onDelta((step.atSeconds * 1000).toLong(), step.values)
                snapshot = snapshot.apply(step.values)
                val progress = recorder.progress()
                if (predicate(progress, snapshot)) return LessonPhase.Drive(task(course), LessonMode.EVALUATE, 1,
                    snapshot, course, progress, course.zone(progress.currentZoneId.orEmpty())?.announce, null,
                    (step.atSeconds * 1000).toLong(), snapshot.stopped,
                    CourseRecorder.KEYS.associateWith { SignalAvailability.SIMULATED })
            }
            error("No matching course frame: ${course.id}")
        }
        val exam = frame(courses.exam, scenarios.examGood) { p, s -> p.currentZoneId == "exam-accel" && s.locked }
        val parked = frame(courses.exam, scenarios.examGood) { p, s -> p.currentZoneId == "exam-parking" && s.stopped }
        val road = frame(courses.road, scenarios.roadGood) { _, s -> s.signal == TrackSignal.RED && s.stopped }
        val round = frame(courses.roundabout, scenarios.roundGood) { p, s -> p.currentZoneId == "rb-ring" && (p.pose?.at?.x ?: 0f) > 43f && s.locked }
        val demo: @Composable () -> Unit = { AdminBand(listOf(scenarios.examGood, scenarios.examBad), null, {}, {}, {}, {}, {}) }
        var finished = 0
        fun driverText(live: Boolean = true) {
            if (live) assertNoScores()
            check(allText().filterNot { it.endsWith("회차") || it.endsWith("km/h") }
                .none { Regex("\\d").containsMatchIn(it) }) { "Course driver text leaked numbers: ${allText()}" }
        }
        val frames = listOf(exam to "drive-exam", parked to "drive-exam-parking", road to "drive-road-red", round to "drive-round")
        for ((state, name) in if (full) frames else frames.take(2)) {
            render(activity) { DriveScreen(state, null, { finished++ }, demo) }
            driverText()
            if (state.locked) check(nodes().none(::hasTouchAction))
            else {
                // The shared FinishButton pulses twice when askedDone; measure its resting bounds.
                Thread.sleep(1_250)
                assertDriverButton(activity, "다 됐어요"); click("다 됐어요")
            }
            check(allText().any { it.contains("코스 도면") })
            check(texts().any { "시험장 위치·신호 · 시뮬레이션" == it })
            if (state.snapshot.signal == TrackSignal.OFF) check(texts().none { it.startsWith("신호등") || it.startsWith("신호 ·") })
            if (state.snapshot.signal == TrackSignal.RED) check("신호 · 빨간불" in texts())
            capture(name) { bitmap -> state.progress.pose?.let { assertCourseCar(activity, bitmap, state.course, it) } }
        }
        check(finished == if (full) 2 else 1)
        if (!full) {
            pass("Round18 Drive safety: actual exam acceleration has zero touch targets/scores, parking stop exposes FinishButton once; simulation provenance")
            return
        }
        for ((signal, label) in listOf(null to "신호등 미측정", TrackSignal.YELLOW to "신호 · 노란불", TrackSignal.GREEN to "신호 · 초록불")) {
            render(activity) { DriveScreen(exam.copy(snapshot = exam.snapshot.copy(signal = signal)), null, {}, demo) }
            // Refresh unchanged node IDs: a signal-only render can retain the previous cached text.
            val signalNodes = nodes().onEach { it.refresh() }
            capture("drive-signal-${signal?.name ?: "missing"}")
            check(signalNodes.any { it.text?.toString() == label }) { "Missing $label; actual ${signalNodes.map { it.text }}" }
            check(signalNodes.none(::hasTouchAction)) { "Signal $signal leaked touch targets: ${texts()}" }
            driverText()
        }
        render(activity) { DriveScreen(exam.copy(snapshot = exam.snapshot.copy(speedKmh = 5.1f, emergency = true)), null, {}, demo) }
        check(nodes().none(::hasTouchAction)); check("돌발 상황" in texts()); driverText(); capture("drive-emergency")
        render(activity) { DriveScreen(parked.copy(progress = parked.progress.copy(pose = null, positionMeasured = false),
            availability = emptyMap()), null, {}, demo) }
        check("시험장 위치 미측정" in texts()); driverText(); capture("drive-missing")
        val hybrid = parked.copy(availability = parked.availability + (SimOnlySignals.TRACK_POSITION_X_M to SignalAvailability.LIVE))
        render(activity) { DriveScreen(hybrid, null, {}, demo) }
        check(texts().any { "실신호" in it && "시뮬레이션" in it }); capture("drive-hybrid")
        fun record(course: TrackCourse, scenario: Scenario, index: Int): AttemptRecord {
            val registry = SignalRegistry(CourseRecorder.KEYS, simulated = true)
            val parking = ParkingRecorder(registry, CourseRecorder.KEYS)
            val recorder = CourseRecorder(course)
            scenario.steps.forEach {
                val t = (it.atSeconds * 1000).toLong()
                parking.onDelta(t, it.values); recorder.onDelta(t, it.values)
            }
            val result = recorder.result()
            return AttemptRecord(index, task(course).id, LessonMode.EVALUATE,
                parking.score()!!.copy(skill = result.score), null,
                com.moah.hackathon.ports.CourseRemarks.remark(result), 0, course = result)
        }
        val bad = record(courses.exam, scenarios.examBad, 1)
        val good = record(courses.exam, scenarios.examGood, 2)
        val left = record(courses.leftTurn, scenarios.leftBad, 1)
        check(bad.course!!.score == 70 && bad.course!!.deductions.size == 3 && good.course!!.score == 100)
        for ((course, attempt, name) in listOf(Triple(courses.exam, bad, "done-exam-bad"),
            Triple(courses.exam, good, "done-exam-good"), Triple(courses.leftTurn, left, "done-left-bad"))) {
            render(activity) { DoneScreen(task(course), attempt.index, attempt, null, {}, {}) }
            driverText(live = false)
            check(courseVerdict(attempt.course!!) in texts())
            attempt.course!!.deductions.take(3).forEach { check(it.reason in texts()) }
            Thread.sleep(3_100)
            capture(name) { bitmap ->
                val bounds = Rect().also { r -> nodes().first { it.contentDescription?.toString() == "${course.title} 코스 도면" }.getBoundsInScreen(r) }
                val scale = designScale(activity)
                val unit = minOf((bounds.width() - 56 * scale) / course.map.widthM, (bounds.height() - 56 * scale) / course.map.heightM)
                val ox = bounds.exactCenterX() - course.map.widthM * unit / 2
                val oy = bounds.exactCenterY() - course.map.heightM * unit / 2
                attempt.course!!.deductions.mapNotNull { it.at }.forEach { at ->
                    check(bitmap.getPixel((ox + at.x * unit).roundToInt(), (oy + (course.map.heightM - at.y) * unit).roundToInt()) == CoachColors.Signal.toArgb()) {
                        "Missing deduction marker at $at"
                    }
                }
            }
        }
        val report = LessonReport(task(courses.exam), LessonMode.EVALUATE, listOf(bad, good), good.score,
            "코스를 차분히 마쳤어요.\n다음에도 흐름을 이어가요.", task(courses.exam), LessonMode.EVALUATE, "다시 연습해요.",
            ShareLevel.entries, listOf("연습 기록을 돌아봐요."), emptyList())
        render(activity) { ReportScreen(report, {}) }
        check("합격" in texts() && "최고 회차의 코스" in texts())
        check(texts().none { Regex("\\d+점").containsMatchIn(it) })
        check("구간과 위치·신호등은 시험장 신호(시뮬레이션)로 측정했어요." in texts())
        capture("report-exam")
        click("자세히 보기")
        check(texts().any { "코스 70점 · 합격선 80점" in it })
        bad.course!!.deductions.forEach { check(it.reason in texts()) }
        capture("report-exam-details")
        check(texts().count { it == "구간" } == 1 && texts().count { it == "사유" } == 1 && texts().count { it == "감점" } == 1)
        assertFullText(activity, "감점 없음", action = "돌아가기")
        runOnMainSync {
            val node = composeNodes(activity).first { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == "감점 없음" }
            val layouts = mutableListOf<TextLayoutResult>()
            check(node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts) == true)
            check(layouts.single().layoutInput.style.fontSize == 32.sp && layouts.single().layoutInput.style.color == CoachColors.Muted)
        }
        click("돌아가기"); click("진단서")
        check("장내기능 모의시험 · 합격" in texts())
        capture("certificate-exam"); assertCertificateLayout(activity, report)
        val missingResult = good.course!!.copy(zones = good.course!!.zones.take(1).map { it.copy(unmeasured = listOf("방향지시등")) })
        render(activity) { CourseDetails(report.copy(attempts = listOf(good.copy(course = missingResult))), Modifier.fillMaxSize()) }
        check("감점 없음" in texts() && texts().none { it in listOf("구간", "사유", "감점") })
        check("${missingResult.zones.single().title} · 방향지시등 · 확인 못 함" in texts())
        pass("Round19: OFF signal row absent, null/yellow/green/red retained; empty deductions replace headers with 32 sp Muted line above back, missing rules retained")
        render(activity) { DoneScreen(task(courses.exam), 1, bad, null, {}, {}, demo, locked = true) }
        check(nodes().none(::hasTouchAction)); check("불합격" !in texts()); driverText()
        render(activity) { ReportScreen(report, {}, locked = true) }
        check(nodes().none(::hasTouchAction)); check("합격" !in texts())
        for ((id, scenario) in listOf("parking-parallel" to com.moah.hackathon.data.MoreParkingScenarios.parallelGood,
            "parking-angle" to com.moah.hackathon.data.MoreParkingScenarios.angleGood)) {
            val parkingTask = SeedCatalog.tasks.single { it.id == id }
            val recorder = ParkingRecorder(SignalRegistry(parkingTask.parkingSpec.keys, simulated = true), parkingTask.parkingSpec.keys)
            scenario.steps.forEach { recorder.onDelta((it.atSeconds * 1000).toLong(), it.values) }
            val attempt = AttemptRecord(1, id, LessonMode.HINT, recorder.score()!!, null,
                "차분하게 들어갔어요.\n마무리도 편안했어요.", 0, path = recorder.path(),
                verdict = recorder.verdict(targetHeadingDeg = parkingTask.parkingSpec.targetHeadingDeg))
            render(activity) { DoneScreen(parkingTask, 1, attempt, null, {}, {}) }
            Thread.sleep(3_100)
            driverText()
            capture("done-${id.removePrefix("parking-")}-good") { bitmap ->
                val bounds = Rect().also { r -> nodes().first { it.contentDescription == "추정 궤적" }.getBoundsInScreen(r) }
                val density = designScale(activity)
                val target = parkingTask.parkingSpec.targetHeadingDeg
                val viewport = pathViewport(attempt.path, bounds.width() / density, bounds.height() / density, targetHeading = target, neighborBays = true, parallel = id == "parking-parallel")
                val end = attempt.path.last()
                val cx = bounds.right - viewport.x(end.x) * density
                val cy = bounds.bottom - viewport.y(end.y) * density
                val angle = Math.toRadians((180f - target).toDouble())
                val unit = viewport.scale * density
                for ((x, y) in listOf(-1.125f to -2f, 1.125f to -2f, -1.125f to 2f, 1.125f to 2f, 0f to 2.5875f)) {
                    val px = (cx + (cos(angle) * x - sin(angle) * y) * unit).roundToInt()
                    val py = (cy + (sin(angle) * x + cos(angle) * y) * unit).roundToInt()
                    check((-2..2).any { dx -> (-2..2).any { dy -> bitmap.getPixel(px + dx, py + dy) == CoachColors.Lavender.toArgb() } }) {
                        "Arrival bay must follow $target degrees: $id at $x,$y"
                    }
                }
            }
        }
        // Each added exam chip fits, is selectable and dispatches the exact venue course.
        for (venue in SeedCatalog.venues) {
            var reserved: String? = null
            render(activity) { androidx.compose.runtime.key(venue.id) {
                SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, SeedCatalog.parkingTask, LessonMode.GUIDE,
                    "함께 연습해요.", null, { _, _ -> }, venues = SeedCatalog.venues,
                    onReserve = { _, _, c -> reserved = c })
            } }
            afterUiSettles("course venue setup ${venue.id}") { check(buttonNode("과제·모드 바꾸기").refresh()) }
            click("과제·모드 바꾸기")
            // The sheet morph takes 400 ms; await its link before sending the next action.
            afterUiSettles("course venue link ${venue.id}") { check(buttonNode("제휴 시험장").refresh()) }
            click("제휴 시험장")
            click(venue.name)
            val examCourse = venue.courses.single { SeedCatalog.TASK_TRACK_EXAM in it.taskIds }
            assertFullText(activity, examCourse.title)
            venue.courses.forEach {
                assertFullText(activity, it.title)
                check(buttonBounds(it.title).right <= 2496 * designScale(activity))
            }
            runOnMainSync {
                val labels = composeNodes(activity, unmerged = true).filter { node ->
                    node.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text in listOf("오늘 자리 있음", "오늘 자리 없음")
                }
                check(labels.size == 3)
                labels.forEach { node ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    check(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
                    check(!layouts.single().hasVisualOverflow) { "Venue availability was clipped" }
                }
            }
            click(examCourse.title); click(venue.slots.first { it.available }.label); click("예약")
            check(reserved == examCourse.id)
            capture("venue-exam-${venue.id}")
        }
        val container = (activity.application as App).container
        lateinit var vm: LessonViewModel
        runOnMainSync {
            vm = ViewModelProvider(activity, LessonViewModel.factory(container))[LessonViewModel::class.java]
            vm.restart(); vm.cancelReservation()
            val venue = SeedCatalog.venues.first()
            vm.reserve(venue.id, venue.slots.first { it.available }.id,
                venue.courses.single { SeedCatalog.TASK_TRACK_EXAM in it.taskIds }.id)
        }
        render(activity) {
            val phase by vm.phase.collectAsStateWithLifecycle()
            val setup = phase as LessonPhase.Setup
            SetupScreen(setup.profile, setup.tasks, setup.suggestedTask, setup.suggestedMode, setup.reason,
                null, vm::begin, venues = setup.venues, booking = setup.booking,
                bookingOptions = setup.bookingOptions, bookingChoice = setup.bookingChoice,
                highlightBooking = setup.highlightBooking, onChooseBooking = vm::chooseBooking)
        }
        check("장내기능 모의시험 ▼" in texts())
        capture("setup-reserved-exam")
        runOnMainSync { vm.cancelReservation(); vm.restart() }
        captureDoneSettle(activity, task(courses.exam), good, "done-course-settle-strip")
        pass("Round18: Drive lock/touch/numbers/provenance, real course replay markers, course Done/Report/certificate, parallel/angle arrival and exam venue chips")
    }

    private fun render(activity: MainActivity, content: @Composable () -> Unit) {
        runOnMainSync { activity.setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalSelectionMotion provides false) {
                MaterialTheme(colorScheme = coachColorScheme()) { DesignScale { content() } }
            }
        } }
        Thread.sleep(700)
        runOnMainSync {}
        // A replaced Compose root can leave the prior lock subtree in UiAutomation's cache.
        if (android.os.Build.VERSION.SDK_INT >= 33) uiAutomation.clearCache()
    }
    @OptIn(ExperimentalComposeUiApi::class)
    private fun composeNodes(activity: MainActivity, unmerged: Boolean = false): List<SemanticsNode> {
        fun roots(view: View): List<RootForTest> = when (view) {
            is RootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return roots(activity.window.decorView).flatMap { descendants(if (unmerged) it.semanticsOwner.unmergedRootSemanticsNode else it.semanticsOwner.rootSemanticsNode) }
    }
    private fun round26bContract(activity: MainActivity) {
        val task = SeedCatalog.parkingTask
        var begins = 0
        render(activity) { SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, task, LessonMode.GUIDE,
            "함께 연습해요.", null, { _, _ -> begins++ }, picked = false) }
        check("처음 오셨네요" in texts() && "과제 고르기 ▼" in texts())
        check(texts().none { it.contains(task.title) })
        check(!buttonNode("시작").isEnabled && begins == 0)
        val disabledStart = buttonBounds("시작")
        tap(disabledStart.centerX(), disabledStart.centerY())
        check(begins == 0 && "과제 고르기 ▼" in texts())
        check(buttonBounds("과제·모드 바꾸기").height() / designScale(activity) >= 96)
        capture("setup-first")
        click("과제·모드 바꾸기")
        check("시작" in texts() && "주차" in texts())
        var chosen: LessonMode? = null
        render(activity) { SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, task, LessonMode.GUIDE,
            modeDescription(LessonMode.GUIDE), null, { _, _ -> }, onChooseHomeMode = { chosen = it }) }
        click("모드 바꾸기")
        check(texts().containsAll(listOf("가이드", "힌트", "평가")) && "지식 테스트" !in texts())
        capture("setup-mode-popup")
        click("힌트")
        check(chosen == LessonMode.HINT && "힌트 ▼" in texts())
        check(buttonBounds("과제·모드 바꾸기").bottom < buttonBounds("시작").top)
        capture("setup-task-title")
        val longTask = SeedCatalog.tasks.single { it.id == SeedCatalog.TASK_TRACK_EXAM }
        render(activity) { SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, longTask, LessonMode.EVALUATE,
            modeDescription(LessonMode.EVALUATE), null, { _, _ -> }) }
        assertFullText(activity, "${longTask.title} ▼", "시작")
        assertFullText(activity, "모드로 해 볼까요?", "시작")
        capture("setup-long-task")
        val knowledge = SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }
        render(activity) { SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, knowledge, LessonMode.QUIZ,
            modeDescription(LessonMode.QUIZ), null, { _, _ -> }) }
        click("모드 바꾸기")
        check("지식 테스트" in texts() && texts().none { it in listOf("가이드", "힌트", "평가") })

        var skipped = 0
        val line = "출발 전 점검, 가이드 모드. 제가 단계마다 말하고 확인할게요. 오늘은 문부터 지시등까지 봅니다."
        render(activity) { BriefingScreen(SeedCatalog.predriveTask, LessonMode.GUIDE, line, null,
            expectedMillis = 2_000, locked = false, onSkip = { skipped++ }) }
        check(texts().containsAll(briefingSentences(line)))
        check("서두르지 않아도 괜찮아요." !in texts() && "음성 안내 중" !in texts())
        check(nodes().count(::hasTouchAction) == 1)
        capture("briefing-captions")
        click("건너뛰기 ›"); check(skipped == 1)
        Thread.sleep(2_000)
        onMainChecked {
            val current = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == "briefing-current" }
            check(current.config[SemanticsProperties.Text].single().text == briefingSentences(line).last())
        }
        render(activity) { BriefingScreen(task, LessonMode.GUIDE, line, null, locked = true, onSkip = { skipped++ }) }
        check(nodes().none(::hasTouchAction) && "건너뛰기 ›" !in texts())
        capture("briefing-locked")
        briefingRouteContract(activity)

        val base = replayChecklist(ChecklistScenarios.bad).second.copy(guideStep = "4/7", guideText = "브레이크를 밟고\n시동을 켜 주세요.",
            doorOpen = false, belt = true, gear = "P", indicatorLeft = false, indicatorRight = false, hazard = false,
            leftIndicatorChecked = false, rightIndicatorChecked = false, hazardChecked = false,
            ignitionOn = true, brakeAtIgnition = false, brakePressed = false)
        val frames = mutableListOf<Bitmap>()
        for ((mode, reveal) in listOf(LessonMode.GUIDE to ChecklistReveal.ALL, LessonMode.HINT to ChecklistReveal.MISTAKES,
            LessonMode.EVALUATE to ChecklistReveal.NAMES_ONLY)) {
            val state = base.copy(mode = mode, checklistReveal = reveal)
            render(activity) { ManeuverScreen(state, false, true, null, {}, taskTitle = SeedCatalog.predriveTask.title) }
            check(nodes().count(::hasTouchAction) == 1) { "Mode ladder must be display-only" }
            assertNoScores()
            if (mode == LessonMode.EVALUATE) {
                check(texts().none { it in listOf("닫힘", "채움", "P", "브레이크 없이 켜짐", "✓", "—") })
                check("조용히 지켜봐요" in texts())
            } else if (mode == LessonMode.HINT) {
                check(texts().count { it == "✓" } == 3 && texts().count { it == "—" } == 3)
                check("브레이크 없이 켜짐" in texts() && "틀릴 때만 말해요" in texts())
            }
            capture("maneuver-checklist-${mode.name.lowercase()}")
            frames += screenshot()
            render(activity) { ManeuverScreen(state.copy(doorSignal = SignalAvailability.MISSING), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check("미측정" in texts())
            capture("maneuver-checklist-${mode.name.lowercase()}-missing")
            render(activity) { ManeuverScreen(state.copy(speed = "6"), true, false, null, {}, taskTitle = SeedCatalog.predriveTask.title) }
            check(nodes().none(::hasTouchAction))
        }
        saveStrip("checklist-modes", frames, listOf("GUIDE", "HINT", "EVALUATE"))
        frames.forEach(Bitmap::recycle)
        captureChecklistWheel(activity, base.copy(mode = LessonMode.GUIDE, checklistReveal = ChecklistReveal.ALL))
        val text = "방향은 맞았어요.\n마무리만 한 번 더 살펴봐요."
        render(activity) { ResultHeadline(text, Modifier.width(1100.dp)) }
        assertFullText(activity, text, minLines = 2)
        fun assertResultTypography() = onMainChecked {
            val node = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            val input = layouts.single().layoutInput
            check(input.style.fontSize == 88.sp)
            check(input.text.spanStyles.single().item.fontSize == 48.sp)
            check(input.text.spanStyles.single().item.color == CoachColors.Muted)
        }
        assertResultTypography()
        val score = replayChecklist(ChecklistScenarios.good).first
        val attempt = AttemptRecord(1, task.id, LessonMode.GUIDE, score, null, text, 0)
        val report = LessonReport(task, LessonMode.GUIDE, listOf(attempt), score, text, task, LessonMode.GUIDE,
            "", emptyList(), emptyList(), emptyList(), ProfileRow(ProfileField.GOAL, null, ProfileChips.chips(ProfileField.GOAL)))
        render(activity) { ReportScreen(report, {}) }
        assertResultTypography()
        check(textBounds(taskModeLine(task, LessonMode.GUIDE)).bottom < textBounds("하나만 물어볼게요").top)
        capture("report-ask-one-short")
        pass("Round26b: unpicked home, title targets, supported modes, long title, captions/skip lock, three checklist reveals/missing values, fixed-center wheel and two-level typography")
    }

    private fun briefingRouteContract(activity: MainActivity) {
        val scope = CoroutineScope(AndroidUiDispatcher.Main + kotlinx.coroutines.SupervisorJob())
        val vehicle = FakeVehiclePort(simulate = false)
        val tts = com.moah.hackathon.ports.FakeTtsPort()
        val machine = LessonStateMachine(vehicle, tts, FakeCoachPort(RemarkPool(SeedCatalog.remarks)),
            SignalRegistry(ParkingRecorder.CHECKLIST_KEYS, simulated = true), ProgressStore(),
            SeedCatalog.tasks, SeedCatalog::guideFor, benefits = SeedCatalog.benefits,
            profile = SeedCatalog.demoProfile, scope = scope, briefingMillis = 60_000, briefingMaxMillis = 60_000)
        val vm = LessonViewModel(machine, tts)
        try {
            runBlocking { vehicle.holdSpeed(0f) }
            onMainChecked { vm.begin(SeedCatalog.TASK_PARKING_REAR, LessonMode.GUIDE) }
            render(activity) { LessonRoute(vm) }
            check("건너뛰기 ›" in texts() && nodes().count(::hasTouchAction) == 1)
            capture("briefing-route-stopped")
            runBlocking { vehicle.holdSpeed(12f) }
            afterUiSettles("Briefing speed lock") {
                check((vm.phase.value as LessonPhase.Briefing).locked)
                check("건너뛰기 ›" !in texts() && nodes().none(::hasTouchAction))
            }
            onMainChecked { vm.skipBriefing(); check(vm.phase.value is LessonPhase.Briefing) }
            capture("briefing-route-locked")
            runBlocking { vehicle.holdSpeed(0f) }
            afterUiSettles("Briefing stop unlock") {
                check(!(vm.phase.value as LessonPhase.Briefing).locked)
                check("건너뛰기 ›" in texts())
            }
            click("건너뛰기 ›")
            check(vm.phase.value is LessonPhase.Maneuver)
            pass("Round26b Briefing route: live speed lock removes skip and all touch actions; stopped skip enters Maneuver")
        } finally { scope.cancel(); vehicle.dispose(); tts.dispose() }
    }

    private fun saveStrip(name: String, frames: List<Bitmap>, labels: List<String>) {
        val width = 853
        val height = (frames.first().height * width.toFloat() / frames.first().width).roundToInt()
        val strip = Bitmap.createBitmap(width * frames.size, height + 40, Bitmap.Config.ARGB_8888)
        try {
            val canvas = android.graphics.Canvas(strip)
            canvas.drawColor(CoachColors.Paper.toArgb())
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = CoachColors.Ink.toArgb(); textSize = 24f; isFilterBitmap = true
            }
            frames.forEachIndexed { index, frame ->
                canvas.drawText(labels[index], (index * width + 16).toFloat(), 28f, paint)
                canvas.drawBitmap(frame, null, Rect(index * width, 40, (index + 1) * width, height + 40), paint)
            }
            File(targetContext.filesDir, "lesson-$name.png").outputStream().use { strip.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { strip.recycle() }
    }

    private fun captureChecklistWheel(activity: MainActivity, base: ManeuverDisplayState) {
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(AndroidUiDispatcher.Main + clock)
        val recomposer = Recomposer(scope.coroutineContext)
        val state = mutableStateOf(base)
        lateinit var view: ComposeView
        runOnMainSync {
            view = ComposeView(activity).apply {
                setParentCompositionContext(recomposer)
                setContent { MaterialTheme(colorScheme = coachColorScheme()) { DesignScale {
                    ManeuverScreen(state.value, false, true, null, {}, taskTitle = SeedCatalog.predriveTask.title)
                } } }
            }
            activity.setContentView(view)
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
        }
        var time = 16L
        fun advance(delta: Long) {
            time += delta
            repeat(3) {
                runOnMainSync { clock.sendFrame(time * 1_000_000) }
                waitForIdleSync(); Thread.sleep(40)
            }
        }
        fun center(tag: String): Float {
            var y = 0f
            onMainChecked { y = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == tag }.boundsInWindow.center.y }
            return y
        }
        val frames = mutableListOf<Bitmap>()
        try {
            advance(0); advance(500)
            val middle = center("checklist-wheel")
            check(abs(center("checklist-step-3") - middle) < 2)
            runOnMainSync { state.value = base.copy(guideStep = "5/7", guideText = "왼쪽 방향지시등을 켜 주세요.") }
            val y = mutableListOf<Float>()
            for (delta in listOf(0L, 150L, 150L)) {
                advance(delta); y += center("checklist-step-4"); frames += screenshot()
            }
            check(y[0] > y[1] && y[1] > y[2] && abs(y[2] - middle) < 2) { "Wheel must slide into the fixed center: $y vs $middle" }
            saveStrip("checklist-wheel-strip", frames, listOf("0 ms", "150 ms", "300 ms"))
        } finally {
            frames.forEach(Bitmap::recycle)
            runOnMainSync { view.disposeComposition(); recomposer.cancel(); scope.cancel() }
        }
    }

    private fun round26aContract(activity: MainActivity) {
        val task = SeedCatalog.parkingTask
        val speech = "출발 전 점검부터 천천히 시작해 보실래요?"
        render(activity) { SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, task, LessonMode.GUIDE,
            "함께 연습해요.", speech, { _, _ -> }) }
        check(speech !in texts()) { "Home must omit the speech subtitle" }
        check(profileLine(SeedCatalog.demoProfile) == "연수생 · 장롱 10년차 · 목표: 아이 등하원")
        val profile = buttonBounds(profileLine(SeedCatalog.demoProfile))
        check(abs(profile.height() / designScale(activity) - 52) <= 2)
        click("과제·모드 바꾸기")
        val posterScale = designScale(activity)
        afterUiSettles("Narrow task sheet poster") {
            onMainChecked {
                val poster = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == "setup-poster" }
                check(abs(poster.boundsInWindow.width / posterScale - 2560 * .21f) <= 2) {
                    "Poster width ${poster.boundsInWindow.width} / $posterScale"
                }
            }
        }
        listOf(LessonMode.GUIDE, LessonMode.HINT, LessonMode.EVALUATE).forEach { mode ->
            if (!isSelected(mode.label)) click(mode.label)
            assertFullText(activity, modeDescription(mode))
        }
        click("지식")
        assertFullText(activity, modeDescription(LessonMode.QUIZ))
        val coachLine = "천천히 가요. 준비됐나요? 좋아요!"
        render(activity) { CoachSheet(CoachDialog(coachLine, listOf(CoachChoice.PARKING_PRACTICE)), {}, {}) }
        assertFullText(activity, coachDisplayText(coachLine), minLines = 3)
        check("주차 연습" in texts() && "고르면 바로 그 자리로 가요." in texts())
        check(abs(textBounds("코치").left - textBounds(coachDisplayText(coachLine)).left - 4 * designScale(activity)) <= 2)
        var restarts = 0
        render(activity) { QuizDoneScreen(SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }, emptyList(),
            emptyList(), coachLine, { restarts++ }) }
        assertFullText(activity, coachDisplayText(coachLine), "메인으로", 3)
        val main = buttonBounds("메인으로")
        val scale = designScale(activity)
        check(abs(main.width() / scale - (2560 * .47f - 128) * 2 / 3) <= 2)
        check(abs(main.right / scale - (2560 - 120)) <= 2)
        click("메인으로"); check(restarts == 1)
        pass("Round26a: home subtitle absent, 52 dp profile, 21% poster, mode descriptions, display-only sentence breaks, coach inset and right-aligned two-thirds main action")
    }

    /** Pin the actual Compose clock: neither screenshots nor accessibility IPC advance the morph. */
    private fun captureSelectionMorph(activity: MainActivity) {
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(AndroidUiDispatcher.Main + clock)
        val recomposer = Recomposer(scope.coroutineContext)
        val mode = mutableStateOf(LessonMode.GUIDE)
        val width = mutableStateOf(1900)
        lateinit var view: ComposeView
        runOnMainSync {
            view = ComposeView(activity).apply {
                setParentCompositionContext(recomposer)
                setContent { MaterialTheme(colorScheme = coachColorScheme()) { DesignScale {
                    androidx.compose.material3.Surface(color = CoachColors.Paper) {
                        Box(Modifier.fillMaxSize().padding(64.dp)) {
                            Box(Modifier.width(width.value.dp)) {
                                TaskSheet(SeedCatalog.tasks, TaskType.PARKING, SeedCatalog.parkingTask, mode.value,
                                    {}, {}, { mode.value = it }, {}, {}, {})
                            }
                        }
                    }
                } } }
            }
            activity.setContentView(view)
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
        }
        var millis = 16L
        fun advance(delta: Long) {
            millis += delta
            repeat(3) { runOnMainSync { clock.sendFrame(millis * 1_000_000) }; waitForIdleSync(); Thread.sleep(40) }
        }
        fun face(): androidx.compose.ui.geometry.Rect {
            var bounds = androidx.compose.ui.geometry.Rect.Zero
            onMainChecked { bounds = composeNodes(activity).filter {
                it.config.getOrNull(SemanticsProperties.TestTag) == "selection-track-face"
            }.maxBy { it.boundsInWindow.top }.boundsInWindow }
            return bounds
        }
        fun target(label: String): androidx.compose.ui.geometry.Rect {
            var bounds = androidx.compose.ui.geometry.Rect.Zero
            onMainChecked { bounds = composeNodes(activity).single {
                it.config.getOrNull(SemanticsProperties.Role) == androidx.compose.ui.semantics.Role.RadioButton &&
                    it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true
            }.boundsInWindow }
            return bounds
        }
        fun matches(a: androidx.compose.ui.geometry.Rect, b: androidx.compose.ui.geometry.Rect) =
            abs(a.left - b.left) <= 1 && abs(a.width - b.width) <= 1
        val frames = mutableListOf<Bitmap>()
        try {
            advance(0)
            val start = face()
            check(matches(start, target("가이드"))) { "First frame must already be selected" }
            onMainChecked { mode.value = LessonMode.HINT }
            val positions = mutableListOf<androidx.compose.ui.geometry.Rect>()
            for (index in 0..2) {
                advance(if (index == 0) 0 else 125)
                positions += face()
                frames += screenshot()
            }
            val end = target("힌트")
            check(matches(positions[0], start) && matches(positions[2], end))
            check(positions[1].left > start.left && positions[1].left < end.left)
            check(positions[1].width < start.width && positions[1].width > end.width)
            val easing = androidx.compose.animation.core.FastOutSlowInEasing.transform(.5f)
            check(abs(positions[1].left - (start.left + (end.left - start.left) * easing)) <= 2)
            val frameWidth = 800
            val frameHeight = (frames[0].height * frameWidth.toFloat() / frames[0].width).roundToInt()
            val strip = Bitmap.createBitmap(frameWidth * 3, frameHeight + 36, Bitmap.Config.ARGB_8888)
            try {
                val canvas = android.graphics.Canvas(strip)
                canvas.drawColor(CoachColors.Paper.toArgb())
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = CoachColors.Ink.toArgb(); textSize = 22f; isFilterBitmap = true
                }
                frames.forEachIndexed { index, frame ->
                    canvas.drawText("${index * 125} ms", (index * frameWidth + 12).toFloat(), 26f, paint)
                    canvas.drawBitmap(frame, null, Rect(index * frameWidth, 36, (index + 1) * frameWidth, frameHeight + 36), paint)
                }
                File(targetContext.filesDir, "lesson-track-morph-strip.png").outputStream().use {
                    strip.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally { strip.recycle() }
            onMainChecked { width.value = 1600 }
            advance(0)
            check(matches(face(), target("힌트"))) { "Resized track must snap to final geometry" }
            pass("Selection morph: 0/125/250 ms $positions; FastOutSlowIn position/width, first frame and resize snap")
        } finally {
            frames.forEach { it.recycle() }
            runOnMainSync {
                view.disposeComposition(); recomposer.cancel(); scope.cancel()
                // Do not reuse this ComposeView's explicit frame clock for subsequent touch fixtures.
                activity.setContentView(android.widget.FrameLayout(activity))
            }
        }
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
                (node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true ||
                    node.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true) &&
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
                        // The explicit Compose clock stays at this sample time during screenshot retries.
                        frames += screenshot()
                    }
                    val scale = designScale(activity)
                    check(abs(widths.first() - 2560 * scale * if (opening) .53f else .21f) <= 2)
                    check(abs(widths.last() - 2560 * scale * if (opening) .21f else .53f) <= 2)
                    check(widths.zipWithNext().all { (a, b) -> if (opening) a > b else a < b }) {
                        "Poster must continuously change width: $widths"
                    }
                    check(widths[2] > 2560 * scale * .21f + 4 && widths[2] < 2560 * scale * .53f - 4)
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
            runOnMainSync {
                view.disposeComposition(); recomposer.cancel(); scope.cancel()
                activity.setContentView(android.widget.FrameLayout(activity))
            }
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
                val screenshot = screenshot()
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
        // D7 permits the top highlight. The side edges and body below it retain the palette.
        listOf(4 to 4, 2 to bounds.height() / 2,
            bounds.width() - 3 to bounds.height() / 2).forEach { (x, y) ->
            val pixel = bitmap.getPixel(bounds.left + x, bounds.top + y)
            check(listOf(0, 8, 16).all { abs(((pixel ushr it) and 255) - ((color ushr it) and 255)) <= 1 }) {
                "Bay face/edge differs at ($x,$y) in $bounds: $pixel / $color"
            }
        }
        val selected = color == CoachColors.Periwinkle.toArgb()
        val ready = color == CoachColors.Lavender.toArgb()
        val top = bitmap.getPixel(bounds.centerX(), bounds.top + 2)
        if (ready) {
            val alpha = .42f
            val ceiling = CoachColors.Paper.copy(alpha = alpha)
                .compositeOver(androidx.compose.ui.graphics.Color(color)).toArgb()
            check(listOf(0, 8, 16).all { shift ->
                val sample = (top ushr shift) and 255
                sample in (((color ushr shift) and 255) + 1)..(((ceiling ushr shift) and 255) + 1)
            }) { "Card highlight must brighten the top within D7: $top / $ceiling" }
        } else check(top == color) { "Selected/planned card must remain flat" }
    }

    private fun textureContract(activity: MainActivity) {
        var clicks = 0
        render(activity) {
            PosterSurface {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PrimaryPill("한 번 더", { clicks++ })
                }
            }
        }
        val bounds = buttonBounds("한 번 더")
        val scale = designScale(activity)
        val shadowY = bounds.bottom + (10 * scale).roundToInt()
        val normal = screenshot()
        val face = normal.getPixel(bounds.left + (80 * scale).roundToInt(), bounds.centerY())
        check(face == CoachColors.Signal.toArgb()) { "Button text backing changed D6 contrast" }
        val highlight = normal.getPixel(bounds.centerX(), bounds.top + (8 * scale).roundToInt())
        check(highlight == face) { "Primary pill must have a flat face" }
        val shadow = normal.getPixel(bounds.centerX(), shadowY)
        check(shadow == CoachColors.Paper.toArgb()) { "Primary pill must not cast a shadow" }
        val now = SystemClock.uptimeMillis()
        val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN,
            bounds.centerX().toFloat(), bounds.centerY().toFloat(), 0)
        check(uiAutomation.injectInputEvent(down, true))
        down.recycle()
        try {
            Thread.sleep(150)
            capture("texture-button-pressed") { pressed ->
                val sample = pressed.getPixel(bounds.centerX(), shadowY)
                val paper = CoachColors.Paper.toArgb()
                check(sample == paper) { "Pressed primary pill must not add a shadow" }
                check(pressed.getPixel(bounds.centerX(), bounds.top + (8 * scale).roundToInt()) == face) {
                    "Pressed primary pill must not add a highlight"
                }
            }
        } finally {
            val cancel = android.view.MotionEvent.obtain(now, SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_CANCEL,
                bounds.centerX().toFloat(), bounds.centerY().toFloat(), 0)
            uiAutomation.injectInputEvent(cancel, true)
            cancel.recycle()
            normal.recycle()
        }
        runOnMainSync { check(clicks == 0) }
        render(activity) { ResultLockedScreen() }
        capture("texture-locked-flat") { bitmap ->
            val ink = CoachColors.Ink.toArgb()
            for (x in listOf(bounds.left, bounds.centerX(), bounds.right)) {
                check(bitmap.getPixel(x, shadowY) == ink) { "Lock retained a shadow" }
            }
        }
        check(nodes().none(::hasTouchAction))
        render(activity) { PosterSurface { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BackPill { clicks++ } } } }
        val back = buttonBounds("돌아가기")
        check(back.height() < bounds.height() && back.width() < bounds.width())
        capture("back-pill")
        val pressTime = SystemClock.uptimeMillis()
        val press = android.view.MotionEvent.obtain(pressTime, pressTime, android.view.MotionEvent.ACTION_DOWN,
            back.centerX().toFloat(), back.centerY().toFloat(), 0)
        check(uiAutomation.injectInputEvent(press, true)); press.recycle()
        try {
            Thread.sleep(150)
            afterUiSettles("Back pressed texture") {
                capture("back-pill-pressed") { bitmap ->
                    check(bitmap.getPixel(back.centerX(), back.bottom + (10 * scale).toInt()) == CoachColors.Paper.toArgb()) {
                        "Pressed back must replace the outer shadow with an inset"
                    }
                    check(bitmap.getPixel(back.centerX(), back.top + (2 * scale).toInt()) != CoachColors.Lavender.toArgb())
                }
            }
        } finally {
            val cancel = android.view.MotionEvent.obtain(pressTime, SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_CANCEL,
                back.centerX().toFloat(), back.centerY().toFloat(), 0)
            uiAutomation.injectInputEvent(cancel, true); cancel.recycle()
        }
        check(clicks == 0)
        pass("Texture: flat primary including press, smaller Lavender back with pressed inset, flat touch-free lock")
    }
    private fun assertArrivalBay(activity: MainActivity, bitmap: Bitmap, path: List<PathPoint>, settled: Boolean) {
        val bounds = Rect().also { rect -> nodes().first { it.contentDescription == "추정 궤적" }.getBoundsInScreen(rect) }
        val density = designScale(activity)
        val viewport = pathViewport(path, bounds.width() / density, bounds.height() / density, targetHeading = 90f, neighborBays = true)
        val scale = viewport.scale * density
        val arrival = path.last()
        val cx = bounds.right - viewport.x(arrival.x) * density
        val cy = bounds.bottom - viewport.y(arrival.y) * density
        val bayAngle = Math.toRadians(-90.0)
        fun near(x: Float, y: Float, color: Int, tolerance: Int = 0): Boolean {
            val px = (cx + (cos(bayAngle) * x - sin(bayAngle) * y) * scale).roundToInt()
            val py = (cy + (sin(bayAngle) * x + cos(bayAngle) * y) * scale).roundToInt()
            return (-1..1).any { dx -> (-1..1).any { dy -> listOf(0, 8, 16).all { shift -> abs(((bitmap.getPixel(px + dx, py + dy) ushr shift) and 255) - ((color ushr shift) and 255)) <= tolerance } } }
        }
        for (x in listOf(-1.125f, 1.125f)) for (y in listOf(-2f, 0f, 2f))
            check(near(x, y, CoachColors.Lavender.toArgb())) { "Target-heading bay side missing" }
        check(near(0f, 2.5875f, CoachColors.Lavender.toArgb())) { "Target bay end missing" }
        check(near(0f, -2.5875f, CoachColors.Ink.toArgb())) { "Target bay entrance must remain open" }
        val faint = CoachColors.Lavender.copy(alpha = .25f).compositeOver(CoachColors.Ink).toArgb()
        for (x in listOf(-7.125f, -4.125f, 4.125f, 7.125f))
            check(near(x, 1f, faint, tolerance = 1)) { "Faint adjacent boundary missing" }
        check(near(5.5f, 1f, CoachColors.Ink.toArgb())) { "Parking lot must have no filled plane" }
        if (!settled) {
            check(near(0f, 0f, CoachColors.Ink.toArgb())) { "Arrival bay must be empty before replay arrives" }
            return
        }
        assertPathCarWindows(bitmap, cx, cy, Math.toRadians((180f - arrival.headingDeg).toDouble()), scale)
        val start = path.first()
        val sx = bounds.right - viewport.x(start.x) * density
        val sy = bounds.bottom - viewport.y(start.y) * density
        val a = Math.toRadians((180f - start.headingDeg).toDouble())
        val pixel = bitmap.getPixel((sx - sin(a) * .27f * scale).roundToInt(), (sy + cos(a) * .27f * scale).roundToInt())
        val dim = CoachColors.Lavender.copy(alpha = .28f).compositeOver(CoachColors.Ink).toArgb()
        check(listOf(0, 8, 16).all { abs(((pixel ushr it) and 255) - ((dim ushr it) and 255)) <= 1 })
    }
    private fun assertPathMargins(activity: MainActivity, bitmap: Bitmap) {
        val bounds = Rect().also { rect -> nodes().first { it.contentDescription == "추정 궤적" }.getBoundsInScreen(rect) }
        val paper = CoachColors.Ink.toArgb()
        var top = bounds.bottom
        var bottom = bounds.top
        for (y in bounds.top until bounds.bottom) for (x in bounds.left until bounds.right) {
            val pixel = bitmap.getPixel(x, y)
            if (listOf(0, 8, 16).any { abs(((pixel ushr it) and 255) - ((paper ushr it) and 255)) > 4 }) {
                top = minOf(top, y)
                bottom = maxOf(bottom, y)
            }
        }
        val scale = designScale(activity)
        val above = top - bounds.top
        val below = bounds.bottom - 1 - bottom
        check(above > 48 * scale && below > 48 * scale) { "Path mark touches viewport edge: $above / $below" }
        check(abs(above - below) <= 3 * scale) { "Unbalanced painted margins: $above / $below" }
        val caption = if ("앞으로 들어간 주차예요." in texts()) "앞으로 들어간 주차예요." else "신호로 추정한 궤적이에요."
        check(abs(textBounds(caption).top - bounds.bottom - 64 * scale) <= 1) { "Caption gap must match the 64 dp top inset" }
        pass("Path painted margins: top=$above px, bottom=$below px; caption gap=64 dp")
    }

    private fun cardLayoutContract(activity: MainActivity) {
        val task = SeedCatalog.parkingTask
        render(activity) { SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, task, LessonMode.HINT,
            "함께 연습해요.", null, { _, _ -> }) }
        click("과제·모드 바꾸기")
        Thread.sleep(500)
        runOnMainSync {
            val venue = composeNodes(activity, unmerged = true).first {
                it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == "제휴 시험장"
            }
            val layouts = mutableListOf<TextLayoutResult>()
            check(venue.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
            val layout = layouts.single()
            // TextAction's paragraph retains the available row width; inspect its actual glyph line.
            check(!layout.didOverflowHeight && layout.lineCount == 1 && layout.getLineEnd(0) == "제휴 시험장".length)
            check(layout.getLineRight(0) - layout.getLineLeft(0) <= layout.size.width + 1)
        }
        check("모드" !in texts())
        assertSelected("힌트")
        check(abs(buttonBounds("제휴 시험장").centerY() - textBounds("힌트").centerY()) < 2)
        check((buttonBounds("시작").top - textBounds("힌트").bottom) / designScale(activity) >= 150)
        assertActionGap(activity, "시작")
        capture("setup-sheet-c-contract")
        fun inspect(type: TaskType) {
            refreshAccessibility()
            val scale = designScale(activity)
            val screenshot = screenshot()
            SeedCatalog.tasks.filter { it.type == type }.forEach { item ->
                val visible = nodes().firstOrNull { it.text?.toString() == item.title } ?: return@forEach
                val titleBounds = Rect().also(visible::getBoundsInScreen)
                val card = taskBounds(item.title)
                val categorySize = SeedCatalog.tasks.count { it.type == type }
                val fullWidth = (2560 * .79f - 128 - 72 - if (categorySize > 4) 80 else 0) / 4
                if (card.width() < (fullWidth - 2) * scale) return@forEach // Skip either clipped edge of a scrolled bay.
                check('\n' !in visible.text.toString())
                check(abs(card.height() / scale - 464f) < 2f)
                check(titleBounds.top >= card.top + 288 * scale && titleBounds.bottom < card.bottom - 32 * scale)
                check(abs(titleBounds.left - card.left - 28 * scale) < 2f)
                onMainChecked {
                    val textNodes = composeNodes(activity, unmerged = true)
                    val titleNode = textNodes.first { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == item.title }
                    val layouts = mutableListOf<TextLayoutResult>()
                    check(titleNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
                    val title = layouts.single()
                    check(title.lineCount == 1 && title.layoutInput.style.fontSize.value in 36f..40f)
                    val documentedOverflow = false
                    check(title.isLineEllipsized(0) == documentedOverflow) { "Unexpected title overflow: ${item.id}" }
                    val detail = if (item.isReady) item.difficulty.label else item.status.label
                    val detailNode = textNodes.first { node ->
                        node.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == detail &&
                            node.boundsInWindow.left >= card.left && node.boundsInWindow.right <= card.right &&
                            node.boundsInWindow.top >= titleNode.boundsInWindow.top - 16 * scale && node.boundsInWindow.bottom <= titleNode.boundsInWindow.bottom + 16 * scale
                    }
                    val detailLayouts = mutableListOf<TextLayoutResult>()
                    check(detailNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(detailLayouts) == true)
                    val baseline = titleNode.boundsInWindow.top + title.firstBaseline
                    val detailBaseline = detailNode.boundsInWindow.top + detailLayouts.single().firstBaseline
                    check(abs(baseline - detailBaseline) <= 1.5f) { "Card baselines differ: ${item.title}" }
                    check(titleNode.boundsInWindow.right + 6 * scale <= detailNode.boundsInWindow.left) { "Title and status overlap: ${item.id}: ${titleNode.boundsInWindow} / ${detailNode.boundsInWindow}" }
                    pass("Card C ${item.id}: ${title.layoutInput.style.fontSize.value} sp; ellipsis=${title.isLineEllipsized(0)}; available=${title.size.width / scale} dp; natural=${title.multiParagraph.maxIntrinsicWidth / scale} dp")
                }
                val art = Rect(card.left + (48 * scale).toInt(), card.top + (40 * scale).toInt(),
                    card.right - (48 * scale).toInt(), card.top + (248 * scale).toInt())
                val selected = item.id == (if (type == TaskType.PARKING) task.id else SeedCatalog.tasks.firstOrNull { it.type == type && it.isReady }?.id)
                val face = if (selected) CoachColors.Periwinkle else if (item.isReady) CoachColors.Lavender
                    else CoachColors.Lavender.copy(alpha = .4f).compositeOver(CoachColors.Paper)
                val ink = if (selected) CoachColors.Paper else if (item.isReady) CoachColors.Ink
                    else CoachColors.Ink.copy(alpha = .55f).compositeOver(face)
                val pixels = colorBounds(screenshot, art, ink.toArgb(), tolerance = 1)
                check(!pixels.isEmpty) { "Empty category illustration: ${item.id}" }
                check(item.isCourse || abs(pixels.exactCenterY() - card.top - 144 * scale) <= 12 * scale) { "Art is not vertically centred: ${item.id}, $pixels" }
            }
        }
        inspect(TaskType.PARKING)
        val trackScale = designScale(activity)
        val modeWidth = buttonBounds("가이드").width()
        val modeLeft = buttonBounds("가이드").left
        for ((type, name) in listOf(TaskType.DRIVING to "setup-sheet-driving", TaskType.CHECKLIST to "setup-sheet-checklist", TaskType.KNOWLEDGE to "setup-sheet-knowledge")) {
            click(taskTypeLabel(type))
            if (type == TaskType.DRIVING) afterUiSettles("lane thumbnail pixels") {
                capture(name) { assertLanePreview(activity, it) }
            } else capture(name)
            if (type == TaskType.KNOWLEDGE) {
                afterUiSettles("One-cell knowledge mode") {
                    onMainChecked {
                        // Android marks a selected radio button non-clickable; use its Compose bounds.
                        val pill = composeNodes(activity).single { node ->
                            node.config.contains(SemanticsProperties.Selected) &&
                                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "지식 테스트" } == true
                        }.boundsInWindow
                        check(abs(pill.width - modeWidth) <= 40 * trackScale)
                        check(abs(pill.left - modeLeft) <= 1)
                    }
                }
            }
            inspect(type)
            if (type == TaskType.DRIVING || type == TaskType.KNOWLEDGE) {
                check(nodes().first { it.isScrollable }.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
                Thread.sleep(350)
                inspect(type)
                capture(if (type == TaskType.DRIVING) "setup-sheet-driving-end" else "setup-sheet-knowledge-end")
            }
        }
        pass("Card C: 288/144 art and band, centred category marks, one-line 36-40 sp titles, shared baselines, ready/planned states")
    }

    private fun assertLanePreview(activity: MainActivity, bitmap: Bitmap) {
        val course = com.moah.hackathon.data.TrackCourses.laneChange
        val card = taskBounds(course.title)
        val density = designScale(activity)
        val sx = 232 * density / course.map.widthM
        val sy = 168 * density / course.map.heightM
        fun x(value: Float) = (card.exactCenterX() + (value - course.map.widthM / 2) * sx).roundToInt()
        fun y(value: Float) = (card.top + 60 * density + (course.map.heightM - value) * sy).roundToInt()
        fun near(px: Int, py: Int, color: Int) = (-1..1).any { dx -> (-1..1).any { dy -> bitmap.getPixel(px + dx, py + dy) == color } }
        val ink = CoachColors.Ink.toArgb()
        for (edge in listOf(11.5f, 18.5f)) for (along in listOf(20f, 60f, 100f)) {
            check(near(x(edge), y(along), ink)) { "Two-lane thumbnail road edge missing: $edge,$along" }
        }
        val route = course.route
        check(abs(x(route.first().x) - x(route.last().x)) >= 20 * density) { "Lane change must be visible at card size" }
        route.forEach { check(near(x(it.x), y(it.y), CoachColors.Periwinkle.toArgb())) { "Expected route missing at $it" } }
        val centerPixels = (y(35f)..y(10f)).map { bitmap.getPixel(x(15f), it) }
        check(centerPixels.distinct().size > 1 && centerPixels.any { pixel ->
            val dashed = CoachColors.Ink.copy(alpha = .6f).compositeOver(CoachColors.Lavender).toArgb()
            listOf(0, 8, 16).all { abs(((pixel ushr it) and 255) - ((dashed ushr it) and 255)) <= 4 }
        }) { "Two-lane thumbnail must have a dashed center line" }
        pass("Round19 card: road edges, dashed center and Periwinkle course.route with visible lane change")
    }

    private fun assertParkingCardWindows(activity: MainActivity, bitmap: Bitmap, frontSelected: Boolean) {
        val scale = designScale(activity)
        for ((task, front) in listOf(SeedCatalog.parkingTask to false, SeedCatalog.frontParkingTask to true)) {
            val card = taskBounds(task.title)
            val selected = front == frontSelected
            val body = if (selected) CoachColors.Paper else CoachColors.Ink
            val panel = if (selected) CoachColors.Lavender else CoachColors.Periwinkle
            val glass = if (selected) CoachColors.Periwinkle else CoachColors.Lavender
            // The 160 dp silhouette is centred in the 288 dp art area; its source is rear-up.
            fun y(sourceY: Float) = (card.top + (64f + (if (front) 250f - sourceY else sourceY) * 160f / 250f) * scale).roundToInt()
            fun glassWidth(sourceY: Float): Int {
                val row = y(sourceY)
                val cx = card.centerX()
                check(bitmap.getPixel(cx, row) == glass.toArgb()) { "Missing card glass: ${task.id}" }
                var left = cx
                var right = cx
                while (left > card.left && bitmap.getPixel(left - 1, row) == glass.toArgb()) left--
                while (right < card.right - 1 && bitmap.getPixel(right + 1, row) == glass.toArgb()) right++
                return right - left + 1
            }
            val windshield = glassWidth(160f)
            val rearWindow = glassWidth(41f)
            check(windshield > rearWindow * 1.2f) { "Card windshield must be wider: ${task.id}: $windshield / $rearWindow" }
            check(bitmap.getPixel(card.centerX(), y(110f)) == body.toArgb())
            for (sourceY in listOf(15f, 210f)) check(bitmap.getPixel(card.centerX(), y(sourceY)) == panel.toArgb())
            val halfBay = (160f * .43f / 2 + 20f) * scale
            for (side in listOf(-1, 1)) {
                val x = (card.exactCenterX() + side * halfBay).roundToInt()
                for (height in listOf(80f, 144f, 208f)) check(bitmap.getPixel(x, (card.top + height * scale).roundToInt()) == body.toArgb()) {
                    "Parking card side missing: ${task.id}"
                }
                // Between car and side line, the top must be open in both selection states.
                val gapX = (card.exactCenterX() + side * 44f * scale).roundToInt()
                for (dy in -1..1) check(bitmap.getPixel(gapX, (card.top + 64f * scale).roundToInt() + dy) != body.toArgb()) {
                    "Parking card top must be open: ${task.id}"
                }
            }
            pass("Card silhouette ${task.id}: large front glass ${if (front) "above" else "below"}, widths=$windshield/$rearWindow px; selected=$selected")
        }
    }

    private fun frontParking(activity: MainActivity) {
        val task = SeedCatalog.frontParkingTask
        var started: Pair<String, LessonMode>? = null
        render(activity) {
            SetupScreen(SeedCatalog.demoProfile, SeedCatalog.tasks, SeedCatalog.parkingTask, LessonMode.GUIDE,
                "함께 연습해요.", null, { id, mode -> started = id to mode })
        }
        click("과제·모드 바꾸기")
        click(task.title)
        assertSelected(task.title)
        click("힌트")
        capture("setup-sheet-front") { bitmap -> assertParkingCardWindows(activity, bitmap, frontSelected = true) }
        click("시작")
        runOnMainSync { check(started == task.id to LessonMode.HINT) }
        val state = ManeuverDisplayState("3", -450f, "D", null, false,
            SeedCatalog.frontParkingGuide[3].say, "4/6", null, 1, 2, 18, false,
            SignalAvailability.SIMULATED, SignalAvailability.SIMULATED, SignalAvailability.MISSING,
            entryGear = task.parkingSpec.entryGear, rearDistanceApplies = task.parkingSpec.usesRearDistance)
        for ((gear, name) in listOf("D" to "maneuver-front", "R" to "maneuver-front-fix")) {
            render(activity) { ManeuverScreen(state.copy(gear = gear), false, false, null, {}, taskTitle = task.title) }
            check(allText().none { "뒤 거리" in it || "미측정" in it })
            check(gear in texts())
            check(allText().any { "앞쪽이 화면 위" in it })
            assertNoScores()
            capture(name) { bitmap ->
                val bounds = Rect().also { rect -> nodes().first {
                    it.contentDescription?.contains("차량 도식") == true
                }.getBoundsInScreen(rect) }
                val bodyTop = bounds.top + bounds.height() * .15f
                val bodyHeight = bounds.height() * .66f
                check(bitmap.getPixel(bounds.centerX(), (bodyTop + bodyHeight * .35f).roundToInt()) == CoachColors.Periwinkle.toArgb()) {
                    "Front windshield must be above centre"
                }
                check(bitmap.getPixel(bounds.centerX(), (bodyTop + bodyHeight * .70f).roundToInt()) == CoachColors.Paper.toArgb())
                val arrow = colorBounds(bitmap, bounds, CoachColors.Signal.toArgb())
                check(!arrow.isEmpty)
                check(if (gear == "D") arrow.bottom < bodyTop else arrow.top > bodyTop + bodyHeight) {
                    "Chevron does not follow gear $gear: $arrow"
                }
            }
        }
        render(activity) { ManeuverScreen(state.copy(speed = "5.1"), true, false, null, {}, taskTitle = task.title) }
        check(nodes().none(::hasTouchAction))
        check(allText().none { "뒤 거리" in it || "차량 도식" in it })
        capture("maneuver-front-locked")
        val coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, kotlin.random.Random(7)))
        fun attempt(good: Boolean, index: Int): AttemptRecord {
            val registry = SignalRegistry(task.parkingSpec.keys, simulated = true)
            val recorder = ParkingRecorder(registry, task.parkingSpec.keys)
            recorder.onDelta(0L, mapOf(task.parkingSpec.keys.single { signalName(it) == "운전석 도어" } to "false"))
            (if (good) FrontParkingScenarios.good else FrontParkingScenarios.bad).steps.forEach {
                recorder.onDelta((it.atSeconds * 1000).toLong(), it.values)
            }
            val score = recorder.score()!!
            val verdict = recorder.verdict(targetHeadingDeg = task.parkingSpec.targetHeadingDeg)
            return AttemptRecord(index, task.id, LessonMode.HINT, score, null,
                runBlocking { coach.remark(task, score, null, SeedCatalog.demoProfile, index, verdict) }, 0,
                path = recorder.path(), verdict = verdict)
        }
        val bad = attempt(false, 1)
        val good = attempt(true, 2)
        for ((record, name) in listOf(bad to "done-front", good to "done-front-good")) {
            render(activity) { DoneScreen(task, record.index, record, null, {}, {}) }
            capture("$name-empty") { bitmap -> assertFrontArrival(activity, bitmap, record.path, settled = false) }
            Thread.sleep(3_000)
            check(texts().containsAll(verdictLines(record.verdict).map { it.text }))
            val marks = if (record == good) listOf("✓", "✓", "✓", "✓") else listOf("△", "△", "✓", "✗")
            check(texts().filter { it in listOf("✓", "△", "✗", "—") } == marks)
            assertNoDoneMetrics()
            assertFullText(activity, "앞으로 들어간 주차예요.")
            capture(name) { bitmap ->
                assertFrontArrival(activity, bitmap, record.path)
                assertPathMargins(activity, bitmap)
            }
        }
        // Isolate each replay direction so screenshot timing cannot cross a D/R transition.
        for (reversing in listOf(false, true)) {
            val path = listOf(PathPoint(0, 0f, 0f, 0f, reversing),
                PathPoint(10_000, 0f, if (reversing) -6f else 6f, 0f, reversing))
            render(activity) { DoneScreen(task, 2, good.copy(path = path), null, {}, {}) }
            capture(if (reversing) "done-front-replay-r" else "done-front-replay-d") { bitmap ->
                val bounds = Rect().also { rect -> nodes().first { it.contentDescription == "추정 궤적" }.getBoundsInScreen(rect) }
                val arrow = colorBounds(bitmap, bounds, CoachColors.Signal.toArgb())
                check(!arrow.isEmpty)
                val density = designScale(activity)
                val viewport = pathViewport(path, bounds.width() / density, bounds.height() / density,
                    frontEntry = true, targetHeading = 90f, neighborBays = true)
                val unit = viewport.scale * density
                // The Signal tip locates the moving pose; body/trail/bay now share the light palette.
                val cy = if (reversing) arrow.bottom - 1 - 3 * density - 2.85f * unit
                    else arrow.top + 3 * density + 2.85f * unit
                assertPathCarWindows(bitmap, arrow.exactCenterX(), cy, 0.0, unit)
                check(if (reversing) arrow.top > cy + 2.25f * unit else arrow.bottom < cy - 2.25f * unit)

            }
        }
        val report = LessonReport(task, LessonMode.HINT, listOf(bad, good), good.score,
            "차분히 연습을 마쳤어요.", task, LessonMode.HINT, "같은 감각을 이어 가요.",
            emptyList(), emptyList(), emptyList())
        render(activity) { ReportScreen(report, {}) }
        check("실신호 0 · 시뮬레이션 7 · 미측정 0" in texts())
        capture("report-front")
        click("자세히 보기")
        assertMetricRow(activity, "앞 근접", "1"); assertMetricRow(activity, "앞 근접", "0")
        check(allText().none { "뒤 거리" in it || "뒤 최소" in it })
        capture("details-front")
        pass("Front parking: sheet dispatch, front-up windshield, D/R chevrons, no rear-distance text, locked touch zero, four verdicts, rear-open dashed entrance, settled/start chevrons, front caption, replay D/R, details and 7-signal badge")
    }

    private fun assertFrontArrival(activity: MainActivity, bitmap: Bitmap, path: List<PathPoint>, settled: Boolean = true) {
        val bounds = Rect().also { rect -> nodes().first { it.contentDescription == "추정 궤적" }.getBoundsInScreen(rect) }
        val density = designScale(activity)
        val viewport = pathViewport(path, bounds.width() / density, bounds.height() / density, frontEntry = true, targetHeading = 90f, neighborBays = true)
        val end = path.last()
        val cx = bounds.left + viewport.x(end.x) * density
        val cy = bounds.top + viewport.y(end.y) * density
        val scale = viewport.scale * density
        val angle = Math.toRadians(90.0)
        fun sample(x: Float, y: Float, color: Int, tolerance: Int = 0): Boolean {
            val px = (cx + cos(angle) * x - sin(angle) * y).roundToInt()
            val py = (cy + sin(angle) * x + cos(angle) * y).roundToInt()
            return (-1..1).any { dx -> (-1..1).any { dy ->
                val pixel = bitmap.getPixel(px + dx, py + dy)
                listOf(0, 8, 16).all { shift -> abs(((pixel ushr shift) and 255) - ((color ushr shift) and 255)) <= tolerance }
            } }
        }
        val w = .9f * scale * 1.25f
        val h = 2.25f * scale * 1.15f
        // The fixed target bay can be covered by a misaligned car; inspect its exposed corners.
        for (side in listOf(-1f, 1f)) for (endSign in listOf(-1f, 1f)) {
            check(listOf(.85f, .9f, .95f).any { sample(side * w, endSign * it * h, CoachColors.Lavender.toArgb()) }) {
                "Front arrival side missing: side=$side end=$endSign"
            }
        }
        check(sample(0f, -h, CoachColors.Lavender.toArgb())) { "Front edge should be closed" }
        check(!sample(0f, h, CoachColors.Lavender.toArgb())) { "Rear edge should be open" }
        if (!settled) {
            // Validate the entrance before the misaligned car and later harsh-stop marker can cover it.
            val entrance = CoachColors.Lavender.copy(alpha = .4f).compositeOver(CoachColors.Ink).toArgb()
            for (side in listOf(-1f, 1f)) {
                check((55..95).any { sample(side * w * it / 100f, h, entrance, tolerance = 1) }) { "Dashed entrance side missing" }
            }
            return
        }
        assertPathCarWindows(bitmap, cx, cy, Math.toRadians(-end.headingDeg.toDouble()), scale)
        val carAngle = Math.toRadians(-end.headingDeg.toDouble())
        val ax = (cx + sin(carAngle) * 2.85f * scale).roundToInt()
        val ay = (cy - cos(carAngle) * 2.85f * scale).roundToInt()
        check((-2..2).any { x -> (-2..2).any { y -> bitmap.getPixel(ax + x, ay + y) == CoachColors.Signal.toArgb() } }) { "Settled front chevron missing" }
        val start = path.first()
        val next = path.first { abs(it.x - start.x) + abs(it.y - start.y) > .001f }
        val dx = next.x - start.x; val dy = next.y - start.y
        val length = kotlin.math.hypot(dx, dy)
        val sx = bounds.left + viewport.x(start.x) * density + dx / length * .8f * scale
        val sy = bounds.top + viewport.y(start.y) * density - dy / length * .8f * scale
        check((-2..2).any { x -> (-2..2).any { y ->
            bitmap.getPixel(sx.roundToInt() + x, sy.roundToInt() + y) == CoachColors.Signal.toArgb()
        } }) { "Initial travel chevron missing" }
    }

    private fun assertCourseCar(activity: MainActivity, bitmap: Bitmap, course: TrackCourse, pose: Pose) {
        val bounds = Rect().also { r -> nodes().first { it.contentDescription == "${course.title} 코스 도면" }.getBoundsInScreen(r) }
        val density = designScale(activity)
        val unit = minOf((bounds.width() - 56 * density) / course.map.widthM, (bounds.height() - 56 * density) / course.map.heightM)
        val cx = bounds.exactCenterX() + (pose.at.x - course.map.widthM / 2) * unit
        val cy = bounds.exactCenterY() - (pose.at.y - course.map.heightM / 2) * unit
        val a = Math.toRadians(-pose.headingDeg.toDouble())
        val carScale = unit * com.moah.hackathon.data.TrackCourses.MAP_CAR_SCALE
        // Nose/tail panels reach beyond the old unscaled body, on both ends of the measured pose.
        for (y in listOf(-1.98f, 1.98f)) {
            val px = (cx - sin(a) * y * carScale).roundToInt()
            val py = (cy + cos(a) * y * carScale).roundToInt()
            check((-1..1).any { dx -> (-1..1).any { dy -> bitmap.getPixel(px + dx, py + dy) == CoachColors.Periwinkle.toArgb() } }) {
                "Map car must use MAP_CAR_SCALE at $y"
            }
        }
    }

    private fun assertPathCarWindows(bitmap: Bitmap, cx: Float, cy: Float, angle: Double, scale: Float) {
        fun pixel(x: Float, y: Float) = bitmap.getPixel(
            (cx + cos(angle) * x - sin(angle) * y).roundToInt(),
            (cy + sin(angle) * x + cos(angle) * y).roundToInt())
        val glass = CoachColors.Ink.toArgb()
        // At heading zero the shared source's large windshield is at y=-.63 m, tail glass at +1.512 m.
        check(pixel(0f, -.63f * scale) == glass && pixel(0f, 1.512f * scale) == glass) {
            "Silhouette front glass must follow the measured body heading"
        }
        fun glassWidth(y: Float): Int {
            var left = 0
            var right = 0
            val limit = (4.5f * .43f / 2 * scale).toInt()
            while (left > -limit && pixel((left - 1).toFloat(), y * scale) == glass) left--
            while (right < limit && pixel((right + 1).toFloat(), y * scale) == glass) right++
            return right - left + 1
        }
        val front = glassWidth(-.63f)
        val rear = glassWidth(1.512f)
        check(front > rear * 1.2f) { "Silhouette windshield must be larger: $front/$rear" }
        check(pixel(0f, .27f * scale) == CoachColors.Lavender.toArgb())
        for (y in listOf(-1.53f, 1.98f)) check(pixel(0f, y * scale) == CoachColors.Periwinkle.toArgb())
        pass("Path silhouette: large front glass follows body heading; front=$front px, rear=$rear px; Ink/Periwinkle/Lavender")
    }

    private fun round12Verdicts(activity: MainActivity, baseReport: LessonReport) {
        val task = SeedCatalog.parkingTask
        val coach = FakeCoachPort(RemarkPool(SeedCatalog.remarks, kotlin.random.Random(7)))
        fun attempt(good: Boolean, index: Int): AttemptRecord {
            val registry = SignalRegistry(ParkingRecorder.KEYS, simulated = true)
            val recorder = ParkingRecorder(registry).apply {
                (if (good) ParkingScenarios.good else ParkingScenarios.bad).steps.forEach {
                    onDelta((it.atSeconds * 1000).toLong(), it.values)
                }
            }
            val score = ParkingScorer.score(recorder.metrics()!!, registry.badge(), registry.missingKeys())
            val verdict = recorder.verdict()
            return AttemptRecord(index, task.id, LessonMode.HINT, score, null,
                runBlocking { coach.remark(task, score, null, SeedCatalog.demoProfile, index, verdict) }, 0,
                path = recorder.path(), verdict = verdict)
        }
        val bad = attempt(false, 1)
        val good = attempt(true, 2)
        fun assertVerdict(record: AttemptRecord) {
            val lines = verdictLines(record.verdict)
            check(texts().containsAll(lines.map { it.text })) { "Verdict lines missing: ${texts()}" }
            lines.groupingBy { it.mark.symbol }.eachCount().forEach { (mark, count) ->
                check(texts().count { it == mark } == count) { "Wrong verdict marks: ${texts()}" }
            }
            check(("신호로 추정" in texts()) == lines.any { it.note != null })
            check(texts().none { it.startsWith("방향 편차") })
            lines.forEach { assertFullText(activity, it.text) }
        }
        for ((record, name) in listOf(good to "done", bad to "done-verdict-fix")) {
            render(activity) { DoneScreen(task, record.index, record, null, {}, {}) }
            Thread.sleep(3_000)
            assertVerdict(record)
            assertNoDoneMetrics()
            assertFullText(activity, record.remark, "한 번 더")
            capture(name)
        }
        val report = baseReport.copy(attempts = listOf(bad, good), best = good.score,
            summary = runBlocking { coach.summarize(task, LessonMode.HINT, listOf(bad, good), SeedCatalog.demoProfile) },
            unverifiedGuideSteps = emptyList())
        render(activity) { ReportScreen(report, {}) }
        assertVerdict(good)
        check("마지막 회차의 판정" in texts())
        capture("report")
        click("자세히 보기")
        assertMetricRow(activity, "방향 편차", "17°"); assertMetricRow(activity, "방향 편차", "2°")
        check(texts().none { it in verdictLines(good.verdict).map { line -> line.text } })
        capture("details")
        // A worse last attempt must not inherit the best score's favourable verdict.
        render(activity) { ReportScreen(report.copy(attempts = listOf(good, bad.copy(index = 3))), {}) }
        assertVerdict(bad)
        capture("report-verdict-last")
        val unknown = good.copy(verdict = good.verdict!!.copy(
            heading = ParkingVerdict.Heading.UNKNOWN, headingErrorDeg = null))
        render(activity) { DoneScreen(task, 2, unknown, null, {}, {}) }
        assertVerdict(unknown)
        capture("done-verdict-unknown") { bitmap ->
            val muted = CoachColors.Muted.compositeOver(CoachColors.Lavender).toArgb()
            check(!colorBounds(bitmap, textBounds("—"), muted, tolerance = 1).isEmpty)
        }
        render(activity) { ReportScreen(report.copy(attempts = listOf(unknown)), {}) }
        assertVerdict(unknown)
        click("자세히 보기")
        assertMetricRow(activity, "방향 편차", "미측정")
        capture("details-verdict-missing")
        val absent = good.copy(verdict = null)
        render(activity) { DoneScreen(task, 2, absent, null, {}, {}) }
        assertVerdict(absent)
        capture("done-verdict-missing")
        val locked = mutableStateOf(false)
        fun assertLocked() {
            runOnMainSync { locked.value = true }
            Thread.sleep(350)
            check(nodes().none(::hasTouchAction))
            check(allText().none { Regex("\\d|✓|△|✗|—|방향|판정|신호로 추정").containsMatchIn(it) })
            check("운전에 집중해 주세요" in texts())
            runOnMainSync { locked.value = false }
            Thread.sleep(350)
            assertVerdict(good)
        }
        render(activity) { DoneScreen(task, 2, good, null, {}, {}, locked = locked.value) }
        assertLocked()
        render(activity) { ReportScreen(report, {}, locked.value) }
        assertLocked()
        pass("Round12 verdict: recorded good/fix/unknown/null, last attempt rather than best, angles only in details, Done/Report lock hides all four lines and restores them")
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
    private fun refreshAccessibility() {
        if (android.os.Build.VERSION.SDK_INT >= 33) uiAutomation.clearCache()
    }
    private fun nodes(): List<AccessibilityNodeInfo> {
        // Scroll and subtree replacement events can leave UiAutomation with stale children.
        refreshAccessibility()
        return descendants(checkNotNull(uiAutomation.rootInActiveWindow))
    }
    private fun texts() = nodes().mapNotNull { it.text?.toString() }
    private fun allText() = nodes().flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString(), it.stateDescription?.toString()) }
    private fun assertSelected(label: String) {
        // Selected tabs omit the click action; buttons and radio buttons expose other selection fields.
        // Switching categories replaces the subtree; accessibility can lag behind the drawn frame.
        repeat(10) {
            refreshAccessibility()
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
    private fun assertBandBelow(activity: MainActivity, action: String) {
        val actionBottom = buttonBounds(action).bottom
        runOnMainSync {
            val band = composeNodes(activity).single { it.config.getOrNull(SemanticsProperties.TestTag) == "admin-band" }.boundsInWindow
            check(actionBottom <= band.top) { "$action overlaps the admin band" }
        }
    }
    private fun assertAdminBand() {
        check("시연" !in allText())
        check(texts().containsAll(listOf("관리자", "정차", "출발", "문 열기", "문 닫기", "시나리오 정지", "더 보기 ▴")))
    }
    private fun textBounds(label: String) = Rect().also { rect ->
        nodes().first { it.text?.toString() == label }.getBoundsInScreen(rect)
    }
    private fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
        add(node)
        repeat(node.childCount) { node.getChild(it)?.let { child -> addAll(descendants(child)) } }
    }
    private fun afterUiSettles(label: String, verify: () -> Unit) {
        var last: RuntimeException? = null
        repeat(6) {
            waitForIdleSync()
            uiAutomation.waitForIdle(100, 2_000)
            refreshAccessibility()
            try { verify(); return } catch (failure: IllegalStateException) { last = failure }
            catch (failure: NoSuchElementException) { last = failure }
            Thread.sleep(100)
        }
        throw IllegalStateException("$label did not settle; assertion unchanged", last)
    }

    private fun click(label: String) {
        val button = buttonNode(label)
        check(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        Thread.sleep(300)
        // Read the updated semantics after Compose's accessibility events, not a cached subtree.
        uiAutomation.waitForIdle(100, 2_000)
        refreshAccessibility()
    }
    private fun buttonNode(label: String): AccessibilityNodeInfo {
        val current = nodes()
        return current.firstOrNull { it.isClickable && descendants(it).any { node ->
            node.text?.toString() == label || node.contentDescription?.toString() == label
        } } ?: error("Missing button $label: " + current.map {
            "${it.packageName}: ${it.text ?: it.contentDescription} clickable=${it.isClickable} enabled=${it.isEnabled}"
        })
    }
    private fun buttonBounds(label: String) = Rect().also { buttonNode(label).getBoundsInScreen(it) }
    private fun assertDriverButton(activity: MainActivity, label: String) {
        val bounds = buttonBounds(label)
        val density = designScale(activity)
        check(kotlin.math.abs(bounds.height() / density - 140f) <= 1f && bounds.width() / density >= if (label == "메인으로") 716f else 719f) {
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
        val rear = CoachColors.Lavender.copy(alpha = .4f).compositeOver(CoachColors.Ink).toArgb()
        val rearRegion = Rect(diagram.left, diagram.top, diagram.right, (diagram.top + diagram.height() * .14f).toInt())
        check(!colorBounds(bitmap, rearRegion, rear, tolerance = 1).isEmpty) { "Rear wheel guides missing" }
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
    private fun screenshot(): Bitmap {
        var bitmap: Bitmap? = null
        repeat(3) { attempt ->
            bitmap = try {
                uiAutomation.takeScreenshot()
            } catch (_: NullPointerException) {
                // API 34 can dereference a null ScreenshotHardwareBuffer before returning a bitmap.
                null
            }
            bitmap?.let { return it }
            if (attempt < 2) Thread.sleep(50)
        }
        return checkNotNull(bitmap) { "UiAutomation.takeScreenshot() returned null after 3 attempts" }
    }
    private fun capture(name: String, verify: (Bitmap) -> Unit = {}) {
        val screenshot = screenshot()
        try {
            File(targetContext.filesDir, "lesson-$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            verify(screenshot)
        } finally {
            screenshot.recycle()
        }
    }
    private fun pass(message: String) { sendStatus(0, Bundle().apply { putString("stream", "PASS $message\n") }) }
}
