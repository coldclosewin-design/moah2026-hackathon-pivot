package com.moah.hackathon.feature.lesson

import android.util.Log
import com.moah.hackathon.ports.CoachPort
import com.moah.hackathon.ports.SpeechPriority
import com.moah.hackathon.ports.TtsPort
import com.moah.hackathon.ports.joinAsObjects
import com.moah.hackathon.ports.withObjectParticle
import com.moah.hackathon.ports.withTopicParticle
import com.moah.hackathon.scoring.ChecklistRubric
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingRubric
import com.moah.hackathon.vehicle.SignalRegistry
import com.moah.hackathon.vehicle.VehiclePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 연수 세션의 두뇌. 포트만 의존하며 Android 프레임워크에 의존하지 않는다(Log 제외) — JVM 테스트로 돈다.
 *
 * 입력: [VehiclePort] (속도·기어·조향각·벨트·IGN·도어·센서), 운전자 버튼([begin]·[finishAttempt]·[nextAttempt]·[endSession])
 * 출력: [phase], [TtsPort] 발화
 *
 * 흐름 (§4.1): Setup → Briefing → [Maneuver → Done] × 회차 → Report
 *  - 가이드 모드: [GuideRunner] 가 단계마다 말하고 신호로 확인. 신호가 MISSING 이면 읽고 넘긴다.
 *  - 힌트 모드: [HintRules] 가 채점 지표 증가분을 보고 틀린 순간만 말한다(규칙, 지연 0).
 *  - 평가 모드: 조용. 끝나면 총평.
 *  - 회차 종료: 운전자가 "다 됐어요"([finishAttempt]). 기어 P + 정차를 보면 먼저 "다 되셨나요?" 한 번 묻는다.
 *  - 세션 종료: [endSession] 또는 **정차 + 운전석 도어 열림**. 주행 중 도어 열림은 무해.
 *  - 채점·멘트는 [ParkingRecorder]·[CoachPort]. 코치가 실패해도 규칙 문장으로 이어진다(CoachPort 계약).
 *  - **출발 전 점검 과제**([TaskType.CHECKLIST]): 같은 흐름, 차는 서 있다. 채점은 [ParkingRecorder.scoreChecklist],
 *    힌트는 점검 규칙만, "다 되셨나요?" 는 벨트·시동·P 가 다 보이면.
 */
class LessonStateMachine(
    private val vehicle: VehiclePort,
    private val tts: TtsPort,
    private val coach: CoachPort,
    private val registry: SignalRegistry,
    private val store: ProgressStore,
    private val tasks: List<Task>,
    private val guideFor: (Task) -> List<GuideStep>,
    private val quizFor: (Task) -> List<QuizItem> = { emptyList() },
    private val reservation: ReservationCard?,
    private val benefits: List<String>,
    profile: Profile,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val briefingMillis: Long = 2_500L,
    private val rubric: ParkingRubric = ParkingRubric(),
    private val checklistRubric: ChecklistRubric = ChecklistRubric(),
) {
    var profile: Profile = profile
        private set

    private val _phase = MutableStateFlow<LessonPhase>(setup())
    val phase: StateFlow<LessonPhase> = _phase

    private val recorder = ParkingRecorder(registry)
    private var hints = HintRules()
    private var guide: GuideRunner? = null
    private var snapshot = VehicleSnapshot()
    private var vehicleJob: Job? = null
    private var briefingJob: Job? = null
    private var attempt = 0
    private var attemptStartMillis = 0L
    private var lastHint: String? = null
    private var askedDone = false
    private var finishing = false
    /**
     * 도어 종료 조건의 무장(armed) 여부. 회차 시작 시점에 운전석 도어가 **이미 열려 있으면**(타고 있는 중, 또는 지난 세션에서 열고 닫지 않음)
     * 닫히는 것을 한 번 본 뒤에만 "정차 + 도어 열림 → 리포트" 가 살아난다. 안 그러면 첫 신호에서 회차가 바로 끝난다.
     */
    private var doorArmed = true
    private val sessionRecords = ArrayList<AttemptRecord>()
    private val unverifiedSteps = LinkedHashSet<String>()
    private var current: Pair<Task, LessonMode>? = null

    // ───────── 운전자 버튼 ─────────

    /** Setup 에서 과제·모드를 골라 시작. */
    fun begin(taskId: String, mode: LessonMode) {
        if (_phase.value !is LessonPhase.Setup) return
        val task = tasks.firstOrNull { it.id == taskId } ?: run { Log.w(TAG, "unknown task $taskId"); return }
        // 준비 중인 과제·맞지 않는 모드는 시작하지 않는다 — 주차 채점기가 다른 과제에 돌아가는 사고 방지. Setup 에 그대로 남는다.
        if (!task.isReady) {
            Log.i(TAG, "refused: ${task.id} is ${task.status}")
            val ready = tasks.filter { it.isReady }.joinToString("·") { it.title }
            tts.speak("${task.title.withTopicParticle()} 아직 준비 중이에요. 지금은 ${ready.withObjectParticle()} 할 수 있어요.")
            return
        }
        if (!task.supports(mode)) {
            Log.i(TAG, "refused: ${task.id} does not support $mode")
            tts.speak("${task.title.withTopicParticle()} ${mode.label} 모드로는 할 수 없어요. 가이드·힌트·평가 중에서 골라 주세요.")
            return
        }
        current = task to mode
        sessionRecords.clear()
        unverifiedSteps.clear()
        attempt = 0
        val line = briefingLine(task, mode)
        _phase.value = LessonPhase.Briefing(task, mode, line)
        tts.speak(line)
        Log.i(TAG, "begin ${task.id} ${mode}")
        briefingJob = scope.launch {
            if (briefingMillis > 0) delay(briefingMillis)
            if (mode == LessonMode.QUIZ) startQuiz(task) else startAttempt()
        }
    }

    // ───────── 지식 테스트 ─────────

    private var quizItems: List<QuizItem> = emptyList()
    private val quizResults = ArrayList<QuizResult>()

    private suspend fun startQuiz(task: Task) {
        quizItems = quizFor(task)
        if (quizItems.isEmpty()) {
            Log.w(TAG, "no quiz items for ${task.id} → back to setup")
            tts.speak("이 과제에는 아직 문제가 없어요.")
            _phase.value = setup(); current = null
            return
        }
        quizResults.clear()
        val now = vehicle.get(listOf(mobis.vss.VssConstants.VEHICLE_SPEED, mobis.vss.VssConstants.DOOR_DRIVER_ISOPEN))
        snapshot = snapshot.apply(now)
        ensureVehicleSubscription()
        publishQuiz(task, 0, chosen = null)
        speakQuestion(quizItems[0], 0)
        Log.i(TAG, "quiz start ${task.id} items=${quizItems.size}")
    }

    /** 선택지 버튼. 잠금(속도 > 5)·이미 답한 문제·퀴즈 밖에서는 무시. */
    fun answer(choice: Int) {
        val p = _phase.value as? LessonPhase.Quiz ?: return
        if (p.locked || p.answered || choice !in p.item.choices.indices) return
        val correct = choice == p.item.answer
        quizResults += QuizResult(p.item.id, choice, correct)
        publishQuiz(p.task, p.index, chosen = choice)
        val verdict = if (correct) "맞아요." else "아쉬워요. 정답은 ${p.item.choices[p.item.answer]}."
        tts.speak("$verdict ${p.item.why}", SpeechPriority.URGENT)
        Log.i(TAG, "quiz answer ${p.item.id}: chosen=$choice correct=$correct")
    }

    /** "다음 문제" / 마지막이면 "결과 보기". 답하기 전에는 무시. */
    fun nextQuestion() {
        val p = _phase.value as? LessonPhase.Quiz ?: return
        if (!p.answered) return
        if (p.isLast) { finishQuiz(p.task); return }
        val next = p.index + 1
        publishQuiz(p.task, next, chosen = null)
        speakQuestion(quizItems[next], next)
    }

    private fun finishQuiz(task: Task) {
        val record = QuizRecord(task.id, quizResults.toList(), clock())
        store.addQuiz(record)
        val remark = quizRemark(record.correct, quizItems.size)
        _phase.value = LessonPhase.QuizDone(task, record.results, quizItems, remark)
        tts.speak(remark, SpeechPriority.URGENT)
        vehicleJob?.cancel(); vehicleJob = null
        Log.i(TAG, "quiz done ${task.id}: ${record.correct}/${quizItems.size}")
    }

    private fun quizRemark(correct: Int, total: Int): String = when {
        total == 0 -> "문제가 없었어요."
        correct == total -> "${total}문제 다 맞았어요. 이건 몸으로도 기억해 두면 좋아요."
        correct == 0 -> "${total}문제 중 맞은 게 없지만, 이유를 들었으니 다음엔 달라요."
        correct * 2 >= total -> "${total}문제 중 ${correct}개. 틀린 것의 이유만 한 번 더 읽어 봐요."
        else -> "${total}문제 중 ${correct}개. 틀린 게 더 많지만 그래서 하는 거예요."
    }

    private fun publishQuiz(task: Task, index: Int, chosen: Int?) {
        _phase.value = LessonPhase.Quiz(
            task = task, index = index, total = quizItems.size, item = quizItems[index],
            locked = snapshot.locked, chosen = chosen, correctSoFar = quizResults.count { it.correct },
        )
    }

    private fun speakQuestion(item: QuizItem, index: Int) {
        val choices = item.choices.mapIndexed { i, c -> "${listOf("첫째", "둘째", "셋째", "넷째")[i.coerceAtMost(3)]}, $c" }.joinToString(". ")
        tts.speak("${index + 1}번. ${item.question}\n$choices")
    }

    /** "다 됐어요" — 회차를 채점하고 Done 으로. 버튼과 도어 열림이 겹쳐도 한 번만 기록한다. */
    fun finishAttempt() {
        val p = _phase.value as? LessonPhase.Maneuver ?: return
        if (finishing) return
        val until = clock() - attemptStartMillis
        val checklist = p.task.type == TaskType.CHECKLIST
        val score = (if (checklist) recorder.scoreChecklist(checklistRubric, until) else recorder.score(rubric, until)) ?: run {
            tts.speak(if (checklist) "아직 신호가 없어요. 잠시 뒤 다시 눌러 주세요." else "아직 움직임이 없어요. 천천히 시작해 보세요.")
            return
        }
        finishing = true
        val previous = store.previous(p.task.id)
        val delta = previous?.let { ParkingDelta.of(score.metrics, it.score.metrics) }
        scope.launch {
            val remark = try {
                coach.remark(p.task, score, delta, profile, attempt)
            } catch (e: RuntimeException) {
                Log.w(TAG, "coach.remark failed → rule sentence", e)
                "수고했어요.\n${com.moah.hackathon.ports.AdviceRules.advice(p.task, score, rubric)}"
            }
            val record = AttemptRecord(attempt, p.task.id, p.mode, score, delta, remark, clock(), path = recorder.path())
            store.add(record)
            sessionRecords += record
            profile = profile.copy(observation = store.observation())
            _phase.value = LessonPhase.Done(p.task, p.mode, attempt, record)
            finishing = false
            tts.speak(remark, SpeechPriority.URGENT)
            Log.i(TAG, "attempt $attempt: skill=${score.skill} safety=${score.safety} segments=${score.metrics.motion.movingSegments} badge=${score.badge}")
        }
    }

    /** Done 에서 "한 번 더". */
    fun nextAttempt() {
        if (_phase.value !is LessonPhase.Done) return
        scope.launch { startAttempt() }
    }

    /** Done(또는 Maneuver) 에서 "오늘은 여기까지". */
    fun endSession() {
        when (_phase.value) {
            is LessonPhase.Maneuver -> { finishAttempt(); scope.launch { toReport() } }
            is LessonPhase.Done -> scope.launch { toReport() }
            is LessonPhase.Quiz -> finishQuiz((_phase.value as LessonPhase.Quiz).task)   // 중간에 끝내기 — 푼 것까지로 결과
            else -> {}
        }
    }

    /** 리포트에서 "다시 시작" 또는 오류 복구. */
    fun reset() {
        briefingJob?.cancel(); briefingJob = null
        vehicleJob?.cancel(); vehicleJob = null
        guide = null
        current = null
        tts.stop()
        _phase.value = setup()
    }

    // ───────── 내부 ─────────

    private fun setup(): LessonPhase.Setup {
        val task = ModeAdvisor.suggestTask(profile, tasks)
        val s = ModeAdvisor.suggest(task, store)
        return LessonPhase.Setup(profile, tasks, task, s.mode, s.reason, reservation)
    }

    private fun briefingLine(task: Task, mode: LessonMode): String {
        val watch = task.watch.joinAsObjects()   // "핸들 방향과 기어 전환과 뒤 거리를"
        return when (mode) {
            LessonMode.GUIDE -> "${task.title}, 가이드 모드. 제가 단계마다 말하고 확인할게요. 오늘은 $watch 봅니다."
            LessonMode.HINT -> "${task.title}, 힌트 모드. 조용히 있다가 필요한 순간에만 말할게요. 오늘은 $watch 봅니다."
            LessonMode.EVALUATE -> "${task.title}, 평가 모드. 끝까지 조용히 보고 있을게요. 다 되면 버튼을 눌러 주세요."
            LessonMode.QUIZ -> "${task.title}. 정차 중이니 편하게 답해 주세요."
        }
    }

    private suspend fun startAttempt() {
        val (task, mode) = current ?: return
        attempt++
        recorder.reset()
        hints = HintRules(checklist = task.type == TaskType.CHECKLIST)
        attemptStartMillis = clock()
        lastHint = null
        askedDone = false
        finishing = false
        // 가이드가 "어느 신호를 확인할 수 있나"를 알려면 현재값을 먼저 봐야 한다 — 구독의 첫 emit 을 기다리지 않고 직접 읽는다.
        val now = vehicle.get(ParkingRecorder.KEYS.toList())
        registry.onValues(now)
        snapshot = snapshot.apply(now)
        recorder.onDelta(0L, now)
        doorArmed = !snapshot.doorOpen
        if (!doorArmed) Log.i(TAG, "door already open at attempt start → door exit disarmed until it closes")
        guide = if (mode == LessonMode.GUIDE) GuideRunner(guideFor(task), registry) else null
        publishManeuver(task, mode, enter = true)
        ensureVehicleSubscription()
        guide?.start()?.forEach { tts.speak(it) }
        guide?.unverified?.forEach { unverifiedSteps += it.say }
        publishManeuver(task, mode)
        // 회차 시작 한 마디 — 브리핑·지난 회차 멘트가 자막에 남지 않게 갈아 준다(가이드는 첫 단계 문장이 그 역할)
        when (mode) {
            LessonMode.HINT -> tts.speak(if (attempt > 1) "${attempt}회차예요. 필요할 때만 말할게요." else "필요할 때만 말할게요.")
            LessonMode.EVALUATE -> tts.speak(if (attempt > 1) "${attempt}회차예요. 조용히 볼게요." else "조용히 볼게요.")
            else -> {}
        }
        Log.i(TAG, "attempt $attempt start (${mode}) missing=${registry.missingKeys()}")
    }

    private fun ensureVehicleSubscription() {
        if (vehicleJob?.isActive == true) return
        vehicleJob = vehicle.observe(ParkingRecorder.KEYS.toList())
            .onEach { delta -> onDelta(delta) }
            .launchIn(scope)
    }

    private fun onDelta(delta: Map<String, String>) {
        snapshot = snapshot.apply(delta)
        registry.onValues(delta)
        if (!doorArmed && !snapshot.doorOpen) { doorArmed = true; Log.i(TAG, "door closed → door exit armed") }
        val doorExit = doorArmed && snapshot.doorOpen && snapshot.stopped
        val p = _phase.value
        when (p) {
            is LessonPhase.Maneuver -> {
                val t = clock() - attemptStartMillis
                recorder.onDelta(t, delta)
                guide?.let { g ->
                    g.onSnapshot(snapshot).forEach { tts.speak(it) }
                    g.unverified.forEach { unverifiedSteps += it.say }
                }
                if (p.mode == LessonMode.HINT) {
                    hints.evaluate(t, recorder.metrics(), snapshot).forEach { h ->
                        lastHint = h.text
                        tts.speak(h.text, h.priority)
                        Log.i(TAG, "hint: ${h.text}")   // tools/emu_flow.sh 가 이 줄을 기다린다
                    }
                }
                // 다 된 것 같으면 한 번 묻는다 — 주차: 움직인 뒤 기어 P + 정차 / 출발 전 점검: 벨트·시동·P 가 다 보임(MISSING 이면 안 묻고 버튼을 기다린다)
                val looksDone = if (p.task.type == TaskType.CHECKLIST) {
                    snapshot.belt == true && snapshot.ignitionOn == true && snapshot.gear == com.moah.hackathon.vehicle.Gear.PARK
                } else {
                    snapshot.stopped && snapshot.gear == com.moah.hackathon.vehicle.Gear.PARK && recorder.metrics()?.motion?.firstMoveMillis != null
                }
                if (!askedDone && looksDone && p.mode != LessonMode.GUIDE) {
                    askedDone = true
                    tts.speak("다 되셨나요? 다 됐으면 버튼을 눌러 주세요.")
                    Log.i(TAG, "asked done (attempt $attempt)")
                }
                if (guide?.finished == true && !askedDone) { askedDone = true; Log.i(TAG, "asked done (attempt $attempt, guide finished)") }
                publishManeuver(p.task, p.mode)
                if (doorExit) {
                    Log.i(TAG, "door opened while stopped → finish + report")
                    finishAttempt()
                    scope.launch { toReport() }
                }
            }
            is LessonPhase.Done -> if (doorExit) {
                Log.i(TAG, "door opened → report")
                scope.launch { toReport() }
            }
            // 퀴즈는 정차 중에만 — 움직이면 잠금만 갱신하고(선택지 숨김), 도어는 무관
            is LessonPhase.Quiz -> if (p.locked != snapshot.locked) _phase.value = p.copy(locked = snapshot.locked)
            else -> {}
        }
    }

    /**
     * @param enter 회차 시작(Briefing·Done → Maneuver). false 면 **지금 단계가 Maneuver 일 때만** 갱신한다 —
     *   신호 delta 를 처리하던 코루틴이 [finishAttempt] 가 막 만든 Done 을 묵은 Maneuver 로 덮어쓰는 경합을 막는다
     *   (화면 녹화 부하에서 재현: 회차 점수는 기록됐는데 화면은 Maneuver 에 남고 "다 됐어요" 가 다시 보임).
     */
    private fun publishManeuver(task: Task, mode: LessonMode, enter: Boolean = false) {
        val next = LessonPhase.Maneuver(
            task = task, mode = mode, attempt = attempt, snapshot = snapshot,
            guide = guide?.view(), lastHint = lastHint,
            movingSegments = recorder.metrics()?.motion?.movingSegments ?: 0,
            elapsedMillis = clock() - attemptStartMillis,
            askedDone = askedDone,
            availability = registry.snapshot(),
        )
        if (enter) _phase.value = next
        else _phase.update { current -> if (current is LessonPhase.Maneuver && !finishing) next else current }
    }

    private suspend fun toReport() {
        // finishAttempt 가 코루틴으로 Done 을 만들 때까지 잠시 기다린다
        var waited = 0
        while (_phase.value is LessonPhase.Maneuver && waited < 20) { delay(50); waited++ }
        val p = _phase.value
        if (p is LessonPhase.Report) return
        val (task, mode) = current ?: return
        val attempts = sessionRecords.toList()
        val best = attempts.maxByOrNull { it.score.skill }?.score
            ?: run { Log.w(TAG, "no attempts → back to setup"); reset(); return }
        val summary = try {
            coach.summarize(task, mode, attempts, profile)
        } catch (e: RuntimeException) {
            Log.w(TAG, "coach.summarize failed → rule sentence", e)
            "${task.title} ${attempts.size}회.\n오늘도 끝까지 했어요. 수고했어요."
        }
        val nextTask = ModeAdvisor.suggestTask(profile, tasks)
        val next = ModeAdvisor.suggest(task, store)
        vehicleJob?.cancel(); vehicleJob = null
        guide = null
        _phase.value = LessonPhase.Report(
            LessonReport(
                task = task, mode = mode, attempts = attempts, best = best, summary = summary,
                nextTask = nextTask, nextMode = next.mode, nextReason = next.reason,
                shareLevels = ShareLevel.entries.toList(), benefits = benefits,
                unverifiedGuideSteps = unverifiedSteps.toList(),
            ),
        )
        tts.speak("$summary\n다음엔 ${next.mode.label} 모드 어때요? ${next.reason}", SpeechPriority.URGENT)
        Log.i(TAG, "report: attempts=${attempts.size} best=${best.skill}/${best.safety} badge=${best.badge}")
    }

    private companion object {
        const val TAG = "MOAH/LessonStateMachine"
    }
}
