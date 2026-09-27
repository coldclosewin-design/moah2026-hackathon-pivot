package com.moah.hackathon.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.moah.hackathon.App
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.concepts.DesignScale
import com.moah.hackathon.ui.lesson.*
import com.moah.hackathon.vehicle.*
import java.io.File
import java.io.FileInputStream

/** Platform accessibility checks and screenshots; no additional Gradle dependency is needed. */
class LessonScreenInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    override fun onStart() {
        val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            val task = SeedCatalog.parkingTask
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
                    "처음은 제가 순서대로 함께할게요.", SeedCatalog.reservation, "오늘은 편안한 곳에서 주차부터 연습해 봐요.",
                    { id, mode -> started = id to mode }, setupDemo)
            }
            check(allText().containsAll(listOf(setupProposal(TaskType.PARKING), "시작", "시연")))
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
            check(texts().contains("연습할 과제"))
            check(texts().containsAll(listOf("가이드", "힌트", "평가")))
            check(texts().none { it == "지식 테스트" })
            val planned = SeedCatalog.tasks.first { !it.isReady }
            check(nodes().none { it.isClickable && descendants(it).any { child -> child.text?.toString() == planned.title } })
            check(texts().contains("준비 중"))
            assertDriverButton(activity, "시작")
            capture("setup-sheet")
            click("힌트")
            click("시작")
            runOnMainSync { check(started == task.id to LessonMode.HINT) }
            val knowledge = SeedCatalog.tasks.first { it.type == TaskType.KNOWLEDGE }
            scrollTo(knowledge.title)
            click(knowledge.title)
            check(texts().contains("지식 테스트")) { "Knowledge selection: ${texts()}" }
            check(texts().none { it in listOf("가이드", "힌트", "평가") })
            click("시작")
            runOnMainSync { check(started == knowledge.id to LessonMode.QUIZ) }
            click("돌아가기")
            check(texts().contains(setupProposal(TaskType.KNOWLEDGE)))
            capture("setup-knowledge")
            pass("Setup selection dispatches task and mode; demo collapsed, opens on 시연, collapses on play without resizing text")

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
            capture("maneuver-guides")
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
                guideStep = "3/3", taskType = TaskType.CHECKLIST, belt = true, ignitionOn = false,
                beltSignal = SignalAvailability.SIMULATED, ignitionSignal = SignalAvailability.SIMULATED)
            val checklistDemo: @Composable () -> Unit = {
                DemoPanel(container.scenariosFor(SeedCatalog.predriveTask), null, {}, {}, {}, {}, {})
            }
            render(activity) { ManeuverScreen(checklist, false, true, null, {}, checklistDemo, SeedCatalog.predriveTask.title) }
            check(texts().containsAll(listOf("안전벨트", "기어", "시동", "채움", "P", "꺼짐", "다 됐어요")))
            check(allText().none { Regex("조향각|뒤 거리|cm|이동 \\d+회").containsMatchIn(it) })
            assertNoScores()
            capture("maneuver-checklist")
            render(activity) { ManeuverScreen(checklist.copy(belt = null, ignitionOn = null,
                beltSignal = SignalAvailability.MISSING, ignitionSignal = SignalAvailability.MISSING), false, true, null, {},
                taskTitle = SeedCatalog.predriveTask.title) }
            check(texts().count { it == "미측정" } >= 2)
            check(texts().contains("P"))
            capture("maneuver-checklist-missing")
            render(activity) { ManeuverScreen(checklist.copy(speed = "5"), true, false, null, {}, checklistDemo) }
            check(nodes().none(::hasTouchAction))
            assertNoScores()
            pass("Checklist: belt/gear/ignition, no parking telemetry, missing signals, locked touch gate")

            var nextCount = 0
            SeedCatalog.quiz.forEachIndexed { index, item ->
                val chosen = mutableStateOf<Int?>(null)
                render(activity) { QuizScreen(knowledge, index, SeedCatalog.quiz.size, item, false,
                    chosen.value, index, { chosen.value = it }, { nextCount++ }) }
                check(item.choices.all { it in texts() })
                check(nodes().count { it.isClickable } == item.choices.size)
                check(texts().none { it in listOf("다음 문제", "결과 보기") })
                if (index == 0) capture("quiz")
                val selected = if (index == 0) (item.answer + 1) % item.choices.size else item.answer
                click(item.choices[selected])
                runOnMainSync { check(chosen.value == selected) }
                check(texts().contains(item.why))
                check(nodes().count { it.isClickable } == 1)
                if (index == 0) capture("quiz-answered")
                click(if (index == SeedCatalog.quiz.lastIndex) "결과 보기" else "다음 문제")
            }
            runOnMainSync { check(nextCount == SeedCatalog.quiz.size) }
            listOf<Int?>(null, 0).forEach { chosen ->
                render(activity) { QuizScreen(knowledge, 0, 5, SeedCatalog.quiz.first(), true, chosen, 0, {}, {}) }
                check(texts().contains("정차 후 답해 주세요"))
                check(SeedCatalog.quiz.first().choices.none { it in texts() })
                check(nodes().none(::hasTouchAction))
            }
            capture("quiz-locked")
            val items = SeedCatalog.quiz.take(3)
            val results = listOf(QuizResult(items[1].id, items[1].answer, true), QuizResult(items[0].id, 0, false))
            var quizRestarted = 0
            render(activity) { QuizDoneScreen(knowledge, results, items, "이유까지 기억하면 충분해요.", { quizRestarted++ }) }
            check(texts().contains("정답: ${items[0].choices[items[0].answer]}"))
            check(texts().contains(items[0].why))
            check(texts().none { it == items[1].why })
            check(texts().contains("맞았어요"))
            scrollTo("안 풀었어요")
            check(texts().contains("안 풀었어요"))
            click("다시 시작")
            runOnMainSync { check(quizRestarted == 1) }
            render(activity) { QuizDoneScreen(knowledge, SeedCatalog.quiz.mapIndexed { index, item ->
                QuizResult(item.id, if (index == 0) 0 else item.answer, index != 0)
            }, SeedCatalog.quiz, "5문제 중 4개를 맞혔어요. 이유까지 기억하면 충분해요.", {}) }
            capture("quiz-done")
            pass("Quiz: five questions, choice dispatch, answer explanations, next/result, lock, keyed results and restart")

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
            capture("done")
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
            Thread.sleep(3_000) // 500 ms initial delay + 3 s playback, including render's 700 ms.
            check(allText().contains("추정 궤적"))
            assertPanelCollapsed()
            check(texts().containsAll(listOf("신호로 추정한 궤적이에요.", "실제 위치와 다를 수 있어요.")))
            assertNoDoneMetrics()
            capture("done-path-contract")
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
            recordMotionClips(activity, moving, pathRecord)
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Lesson contract passed\n") })
        } catch (failure: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", failure.stackTraceToString()) })
        }
    }

    private fun render(activity: MainActivity, content: @Composable () -> Unit) {
        runOnMainSync { activity.setContent { MaterialTheme(colorScheme = coachColorScheme()) { DesignScale { content() } } } }
        Thread.sleep(700)
        runOnMainSync {}
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
    }
    private fun buttonNode(label: String) = nodes().first { it.isClickable && descendants(it).any { node ->
        node.text?.toString() == label || node.contentDescription?.toString() == label
    } }
    private fun buttonBounds(label: String) = Rect().also { buttonNode(label).getBoundsInScreen(it) }
    private fun assertDriverButton(activity: MainActivity, label: String) {
        val bounds = buttonBounds(label)
        var density = 1f
        runOnMainSync {
            val content = activity.findViewById<android.view.View>(android.R.id.content)
            density = checkNotNull(com.moah.hackathon.ui.concepts.designDensity(content.width, content.height))
        }
        check(kotlin.math.abs(bounds.height() / density - 140f) <= 1f && bounds.width() / density >= 719f) {
            "$label driver bounds: $bounds / $density"
        }
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
    private fun capture(name: String) {
        val screenshot = checkNotNull(uiAutomation.takeScreenshot())
        File(targetContext.filesDir, "lesson-$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
    private fun pass(message: String) { sendStatus(0, Bundle().apply { putString("stream", "PASS $message\n") }) }
}
