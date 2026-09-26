package com.moah.hackathon.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.moah.hackathon.App
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.*
import com.moah.hackathon.ui.concepts.DesignScale
import com.moah.hackathon.ui.lesson.*
import com.moah.hackathon.vehicle.*
import java.io.File

/** Platform accessibility checks and screenshots; no additional Gradle dependency is needed. */
class LessonScreenInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    override fun onStart() {
        val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            val task = SeedCatalog.parkingTask
            val container = (activity.application as App).container
            val setupDemo: @Composable () -> Unit = {
                DemoPanel(container.scenarios, null, {}, {}, {}, {}, {})
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
            check(texts().containsAll(listOf("오늘은 뭘 해볼까요?", "시작", "시연", "잘한 주차", "못한 주차")))
            capture("setup")
            click("힌트")
            click("시작")
            runOnMainSync { check(started == task.id to LessonMode.HINT) }
            pass("Setup selection dispatches task and mode; demo defaults expanded")

            val line = "후면 직각 주차, 가이드 모드. 오늘은 핸들 방향과 기어 전환을 봅니다."
            render(activity) { BriefingScreen(line, line) }
            check(texts().contains(line))
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
            check(allText().none { Regex("회차|이동 \\d+회|18초").containsMatchIn(it) })
            capture("maneuver")
            click("시연")
            check(texts().none { it == "잘한 주차" })
            capture("maneuver-collapsed")

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
            check(texts().contains(longSubtitle))
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

            val metrics = ParkingMetrics(MotionSummary(4, 44_000, 26_000, 4f, 1_000), emptyList(),
                SteeringSummary(3, 450f), GearSummary(2, true, true), ProximitySummary(1, 35f), PreDriveSummary(false, true))
            val score = ParkingScore(60, 55, metrics, AvailabilityBadge(0, 7, 1), listOf(ParkingRecorder.KEYS.last()))
            val record = AttemptRecord(1, task.id, LessonMode.HINT, score, null,
                "장롱의 문 정도는 열었습니다. 좋은 출발이에요.", 0)
            var again = 0
            var ended = 0
            render(activity) { DoneScreen(task.title, 1, record, record.remark, { again++ }, { ended++ }, demo) }
            check(texts().none { "지난번보다" in it })
            capture("done")
            click("한 번 더")
            click("오늘은 여기까지")
            runOnMainSync { check(again == 1 && ended == 1) }

            val report = LessonReport(task, LessonMode.HINT, listOf(record), score,
                "첫 연습을 마쳤어요. 다음에는 뒤 거리를 조금 더 남겨 볼까요?",
                task, LessonMode.GUIDE, "핸들 타이밍을 한 단계씩 함께 익혀요.", ShareLevel.entries,
                SeedCatalog.benefits, listOf("안전벨트 확인"))
            var restarted = 0
            render(activity) { ReportScreen(report, { restarted++ }) }
            check(texts().containsAll(listOf(badgeText(score.badge), "다시 시작", "이 신호는 이 차에서 받지 못했어요")))
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
    private fun hasTouchAction(node: AccessibilityNodeInfo) = node.isClickable || node.isLongClickable || node.isScrollable
    private fun nodes() = descendants(checkNotNull(uiAutomation.rootInActiveWindow))
    private fun texts() = nodes().mapNotNull { it.text?.toString() }
    private fun allText() = nodes().flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString(), it.stateDescription?.toString()) }
    private fun assertNoScores() { check(allText().none { Regex("점수|감점|\\d+\\s*점").containsMatchIn(it) }) }
    private fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
        add(node)
        repeat(node.childCount) { node.getChild(it)?.let { child -> addAll(descendants(child)) } }
    }
    private fun click(label: String) {
        val button = nodes().first { it.isClickable && descendants(it).any { node -> node.text?.toString() == label } }
        check(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        Thread.sleep(300)
    }
    private fun capture(name: String) {
        val screenshot = checkNotNull(uiAutomation.takeScreenshot())
        File(targetContext.filesDir, "lesson-$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
    private fun pass(message: String) { sendStatus(0, Bundle().apply { putString("stream", "PASS $message\n") }) }
}
