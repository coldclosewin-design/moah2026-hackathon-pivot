package com.moah.hackathon.feature.lesson

import android.util.Log
import com.moah.hackathon.data.SpeechCards
import com.moah.hackathon.ports.CoachPort
import com.moah.hackathon.ports.SpeechPriority
import com.moah.hackathon.ports.TtsPort
import com.moah.hackathon.ports.joinAsObjects
import com.moah.hackathon.ports.withObjectParticle
import com.moah.hackathon.ports.withTopicParticle
import com.moah.hackathon.scoring.ChecklistRubric
import com.moah.hackathon.scoring.CourseRecorder
import com.moah.hackathon.scoring.CourseUpdate
import com.moah.hackathon.scoring.ZoneRule
import com.moah.hackathon.vehicle.TrackSignal
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingRubric
import com.moah.hackathon.vehicle.SignalRegistry
import com.moah.hackathon.vehicle.VehiclePort
import com.moah.hackathon.vehicle.toVssFloat
import kotlinx.coroutines.CancellationException
import mobis.vss.VssConstants
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
 *  - **코스 과제**([Task.course], 10/4): Maneuver 대신 [LessonPhase.Drive]. [CourseRecorder] 가 구간을 따라가며 감점을 낸다.
 *    가이드 = 구간마다 할 일을 읽어 주고 감점 순간 이유를 말함 · 힌트 = 감점 순간·돌발·빨간불·지시등만 · 평가 = 시험 코스면 구간 방송 + "감점입니다", 연습 코스면 조용.
 *    회차 점수의 skill 은 코스 점수(100 − 감점), safety 는 급조작·벨트(주차와 같은 A층).
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
    /** 제휴 시험장(D3). 비어 있으면 예약 진입점은 무시된다. */
    private val venues: List<Venue> = emptyList(),
    private val benefits: List<String>,
    /** 저장된 프로필이 없을 때 쓰는 프로필(앱은 빈 진술 = 초보 가정, 테스트는 시연 프로필). */
    profile: Profile,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val briefingMillis: Long = 2_500L,
    private val rubric: ParkingRubric = ParkingRubric(),
    private val checklistRubric: ChecklistRubric = ChecklistRubric(),
    /** 프로필 저장소(라운드 22 결정 7, 10/5). 기본은 메모리 — 앱은 `FileProfileStore`. */
    private val profileStore: ProfileStore = MemoryProfileStore(),
) {
    private val savedProfile: StoredProfile? = profileStore.load()
    var profile: Profile = savedProfile?.let { profile.copy(statement = it.statement) } ?: profile
        private set
    /** 답한 줄 — 저장이 없으면 시작 프로필에 값이 있는 줄(시연 프로필은 다섯 줄 다). */
    private val answered: MutableSet<ProfileField> = (savedProfile?.answered ?: ProfileChips.filledFields(profile.statement)).toMutableSet()
    /** 리포트 끝 카드의 "다음에요" 횟수 — 두 번이면 그 줄은 시트에서만 묻는다. */
    private val askSkips: MutableMap<ProfileField, Int> = savedProfile?.skips.orEmpty().toMutableMap()
    /** 첫 실행 온보딩을 아직 안 마쳤다 — 저장된 프로필이 없거나 마치지 않았을 때. */
    private var onboardingPending: Boolean = savedProfile?.onboarded != true

    private val _phase = MutableStateFlow<LessonPhase>(setup())
    val phase: StateFlow<LessonPhase> = _phase

    private val recorder = ParkingRecorder(registry)
    /** 코스 과제 회차의 기록기. 주차·점검 회차는 null. */
    private var course: CourseRecorder? = null
    private var zoneLine: String? = null
    private var lastSignal: TrackSignal? = null
    private var lastEmergency = false
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
    /** 세션 세대 — 시작·리셋마다 올라간다. 늦게 끝난 채점 코루틴이 끝난 세션 위에 Done 을 덮어쓰지 않게(사내 피드백 #4, 10/6). */
    private var sessionGen = 0
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
        if (pinned != null && pinned != (task.id to mode)) pinned = null   // 운전자가 다른 걸 고르면 프리셋 고정이 풀린다
        clearHomeTransient()
        current = task to mode
        sessionGen++
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
        val now = vehicle.get(listOf(mobis.vss.VssConstants.VEHICLE_SPEED, mobis.vss.VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN))
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
        _phase.value = LessonPhase.QuizDone(task, record.results, quizItems, remark, locked = snapshot.locked)
        tts.speak(remark, SpeechPriority.URGENT)
        // 구독은 유지 — QuizDone 도 움직이면 잠근다(절대 규칙 10, 감사 08 A1-01). reset 이 끊는다
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
        val (task, mode) = when (val p = _phase.value) {
            is LessonPhase.Maneuver -> p.task to p.mode
            is LessonPhase.Drive -> p.task to p.mode
            else -> return
        }
        if (finishing) return
        val until = clock() - attemptStartMillis
        val checklist = task.type == TaskType.CHECKLIST
        val raw = (if (checklist) recorder.scoreChecklist(checklistRubric, until) else recorder.score(task.parkingSpec.rubric(rubric), until)) ?: run {
            tts.speak(if (checklist) "아직 신호가 없어요. 잠시 뒤 다시 눌러 주세요." else "아직 움직임이 없어요. 천천히 시작해 보세요.")
            return
        }
        finishing = true
        // 코스 과제: 구간 결과를 닫고 skill 을 코스 점수로(safety 는 A층 급조작·벨트 그대로)
        val courseResult = course?.result(until)
        val score = if (courseResult != null) raw.copy(skill = courseResult.score) else raw
        val previous = store.previous(task.id)
        val delta = previous?.let { ParkingDelta.of(score.metrics, it.score.metrics) }
        // 네 가지 판정(docs/design/09) — 서두 선택의 조건이자 화면의 네 줄. 목표 각은 과제 사양(후면·전면 직각 모두 90°). 코스·점검은 없음
        val verdict = if (checklist || courseResult != null) null else recorder.verdict(until, task.parkingSpec.targetHeadingDeg)
        val gen = sessionGen
        scope.launch {
            val remark = try {
                coach.remark(task, score, delta, profile, attempt, verdict, courseResult)
            } catch (e: RuntimeException) {
                Log.w(TAG, "coach.remark failed → rule sentence", e)
                courseResult?.let { com.moah.hackathon.ports.CourseRemarks.remark(it) }
                    ?: "수고했어요.\n${com.moah.hackathon.ports.AdviceRules.advice(task, score, rubric)}"
            }
            if (gen != sessionGen) { Log.w(TAG, "attempt $attempt finished after the session ended → dropped"); return@launch }
            val record = AttemptRecord(attempt, task.id, mode, score, delta, remark, clock(),
                path = if (courseResult != null) emptyList() else recorder.path(), verdict = verdict, course = courseResult)
            store.add(record)
            sessionRecords += record
            profile = profile.copy(observation = store.observation())
            _phase.value = LessonPhase.Done(task, mode, attempt, record, locked = snapshot.locked)
            finishing = false
            tts.speak(remark, SpeechPriority.URGENT)
            Log.i(TAG, "attempt $attempt: skill=${score.skill} safety=${score.safety} segments=${score.metrics.motion.movingSegments} badge=${score.badge}" +
                (courseResult?.let { " course=${it.score} passed=${it.passed} deductions=${it.deductions.size}" } ?: ""))
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
            is LessonPhase.Maneuver, is LessonPhase.Drive -> { finishAttempt(); scope.launch { toReport() } }
            is LessonPhase.Done -> scope.launch { toReport() }
            is LessonPhase.Quiz -> finishQuiz((_phase.value as LessonPhase.Quiz).task)   // 중간에 끝내기 — 푼 것까지로 결과
            else -> {}
        }
    }

    // ───────── 제휴 시험장 예약 (§3.3, D3 = (나)) — Setup 에서만. 실제 연계 없음 ─────────

    /** 예약. 시험장·시간대·코스가 시드에 있고 시간대가 비어 있을 때만. 로그 `reservation: …`. 성공하면 Setup 을 다시 그린다(제안 과제가 바뀔 수 있다). */
    fun reserve(venueId: String, slotId: String, courseId: String) {
        if (_phase.value !is LessonPhase.Setup) return
        val venue = venues.firstOrNull { it.id == venueId } ?: run { Log.w(TAG, "reservation: unknown venue $venueId"); return }
        val slot = venue.slots.firstOrNull { it.id == slotId } ?: run { Log.w(TAG, "reservation: unknown slot $slotId"); return }
        if (venue.courses.none { it.id == courseId }) { Log.w(TAG, "reservation: unknown course $courseId"); return }
        if (!slot.available) { Log.w(TAG, "reservation: slot $slotId not available"); return }
        store.reservation = Reservation(venueId, slotId, courseId, madeAtMillis = clock())
        pinned = null
        clearHomeTransient()
        Log.i(TAG, "reservation: made venue=$venueId slot=$slotId course=$courseId")
        _phase.value = setup()
    }

    /** 예약 취소 — Setup 에서만. 제안이 원래 규칙으로 돌아간다. */
    fun cancelReservation() {
        if (_phase.value !is LessonPhase.Setup) return
        val had = store.reservation ?: return
        store.reservation = null
        pinned = null
        clearHomeTransient()
        Log.i(TAG, "reservation: cancelled venue=${had.venueId} slot=${had.slotId}")
        _phase.value = setup()
    }

    /** 리포트에서 "다시 시작" 또는 오류 복구. */
    fun reset() {
        stopEverything()
        clearHomeTransient()
        _phase.value = setup()
    }

    private fun stopEverything() {
        sessionGen++
        finishing = false
        briefingJob?.cancel(); briefingJob = null
        vehicleJob?.cancel(); vehicleJob = null
        guide = null
        course = null
        current = null
        tts.stop()
    }

    // ───────── 관리자 모드 (라운드 22 결정 5, 10/5) — 시연 빌드의 준비실·띠만 부른다. 운전자 화면에는 진입점이 없다 ─────────

    /** 프리셋이 고정한 홈 제안(과제 id, 모드). 운전자가 다른 걸 시작하거나 예약을 바꾸거나 기록을 지우면 풀린다. */
    private var pinned: Pair<String, LessonMode>? = null

    /**
     * 시연 프리셋 적용 — 진행 중인 것을 멈추고, 기록·예약을 비우고, [presetProfile] 로 바꾸고, 홈 제안을 프리셋 과제·모드로 고정한다.
     * 과제가 없거나 준비 중이거나 모드를 지원하지 않으면 아무것도 바꾸지 않는다. 로그 `admin: preset …`.
     */
    fun applyPreset(preset: AdminPreset, presetProfile: Profile) {
        val task = tasks.firstOrNull { it.id == preset.taskId && it.isReady && it.supports(preset.mode) }
            ?: run { Log.w(TAG, "admin: preset ${preset.id} refused (task ${preset.taskId} ${preset.mode})"); return }
        stopEverything()
        store.clear()
        profile = presetProfile.copy(observation = ProfileObservation())
        adoptProfileStatement()
        preset.reservation?.let { seed ->
            val venue = venues.firstOrNull { it.id == seed.venueId }
            val slotOk = venue?.slots?.any { it.id == seed.slotId && it.available } == true
            val courseOk = venue?.courses?.any { it.id == seed.courseId } == true
            if (slotOk && courseOk) store.reservation = Reservation(seed.venueId, seed.slotId, seed.courseId, madeAtMillis = clock())
            else Log.w(TAG, "admin: preset ${preset.id} reservation ignored ($seed)")
        }
        pinned = task.id to preset.mode
        clearHomeTransient()
        _phase.value = setup()
        Log.i(TAG, "admin: preset ${preset.id} task=${task.id} mode=${preset.mode} profile=${presetProfile.statement}")
    }

    /** 프로필 전환 — 관측(기록에서 나온 것)은 그대로 두고 진술만 바꾼다. Setup 이면 제안을 다시 계산한다. */
    fun setProfile(next: Profile) {
        profile = next.copy(observation = store.observation())
        adoptProfileStatement()
        if (_phase.value is LessonPhase.Setup) _phase.value = setup()
        Log.i(TAG, "admin: profile ${next.statement}")
    }

    /** 기록 초기화 — 회차·퀴즈·예약·관측·프리셋 고정을 지운다. Setup 이면 제안을 다시 계산한다. */
    fun resetRecords() {
        store.clear()
        profile = profile.copy(observation = ProfileObservation())
        pinned = null
        clearHomeTransient()
        if (_phase.value is LessonPhase.Setup) _phase.value = setup()
        Log.i(TAG, "admin: records cleared")
    }

    /** 관리자 "프로필 초기화" — 저장을 지우고 빈 진술로, 다음 홈에서 첫 실행 질문이 다시 나온다. */
    fun resetProfile() {
        profileStore.clear()
        profile = profile.copy(statement = ProfileStatement())
        answered.clear()
        askSkips.clear()
        onboardingPending = true
        pinned = null
        clearHomeTransient()
        if (_phase.value is LessonPhase.Setup) _phase.value = setup()
        Log.i(TAG, "admin: profile cleared")
    }

    /** 프리셋·관리자 프로필 전환: 값이 있는 줄을 답한 것으로, 첫 실행은 마친 것으로 보고 저장한다. */
    private fun adoptProfileStatement() {
        answered.clear(); answered += ProfileChips.filledFields(profile.statement)
        askSkips.clear()
        onboardingPending = false
        saveProfile()
    }

    // ───────── 프로필 (라운드 22 결정 7 = P2 "한 장", 10/5) — 칩만. 첫 실행 · 홈 눈썹 시트 · 리포트 끝 카드가 같은 진입점 ─────────

    /** 칩 하나 — 그 줄을 답한 것으로 저장한다. Setup 이면 제안을 다시 계산하고, 리포트 끝 카드의 줄이면 카드를 닫는다. 로그 `profile: …`. */
    fun answerProfile(field: ProfileField, chipId: String) {
        val next = ProfileChips.apply(profile.statement, field, chipId, thisYear())
            ?: run { Log.w(TAG, "profile: unknown chip $field/$chipId"); return }
        profile = profile.copy(statement = next)
        answered += field
        askSkips.remove(field)
        saveProfile()
        when (val p = _phase.value) {
            is LessonPhase.Setup -> _phase.value = setup()
            is LessonPhase.Report -> if (p.report.askOne?.field == field) _phase.value = p.copy(report = p.report.copy(askOne = null))
            else -> {}
        }
        Log.i(TAG, "profile: $field=$chipId")
    }

    /** 첫 실행 끝 — "나머지는 연습하면서 · 시작하기" 와 `건너뛰기` 둘 다. 답한 줄까지 저장. */
    fun finishOnboarding() {
        if (!onboardingPending) return
        onboardingPending = false
        saveProfile()
        if (_phase.value is LessonPhase.Setup) _phase.value = setup()
        Log.i(TAG, "profile: onboarding done answered=$answered")
    }

    /** 리포트 끝 카드 `다음에요`. 같은 줄을 두 번 미루면 그 줄은 시트에서만 묻는다. */
    fun skipAsk(field: ProfileField) {
        val p = _phase.value as? LessonPhase.Report ?: return
        if (p.report.askOne?.field != field) return
        askSkips[field] = (askSkips[field] ?: 0) + 1
        saveProfile()
        _phase.value = p.copy(report = p.report.copy(askOne = null))
        Log.i(TAG, "profile: skip $field (${askSkips[field]})")
    }

    private fun saveProfile() = profileStore.save(StoredProfile(profile.statement, answered.toSet(), askSkips.toMap(), onboarded = !onboardingPending))

    private fun thisYear(): Int = java.time.Instant.ofEpochMilli(clock()).atZone(java.time.ZoneId.systemDefault()).year

    private fun profileRow(field: ProfileField) =
        ProfileRow(field, ProfileChips.answerOf(profile.statement, field, field in answered, thisYear()), ProfileChips.chips(field))

    /** 시트 아래 "앱이 본 것" — 기록에서 나온 관측을 숫자 없이 말로(7b 질문 7). */
    private fun observedLines(): List<String> {
        val o = profile.observation
        if (o.attempts == 0) return listOf("아직 함께한 연습이 없어요.")
        return buildList {
            add("연습이 쌓이고 있어요.")
            o.weakTaskId?.let { id -> tasks.firstOrNull { it.id == id }?.let { add("${it.title} 쪽을 조금 더 연습하면 좋아요.") } }
            if (o.harshEvents > 0) add("급하게 밟거나 멈춘 순간이 있었어요.")
        }
    }

    // ───────── 홈 코치 대화 (라운드 22 결정 4 = A 알약 → 시트 + D 예약 카드, 10/5) — Setup 에서만, 탭 대화(STT 없음) ─────────
    // 결과는 새 화면이 아니라 기존 것: 홈 제안을 바꾸거나(고정), 과제 시트를 분류로 열라고 요청하거나, 예약 카드를 강조한다.
    // 첫 말풍선은 규칙 문장. 텍스트 대화(10/6)를 켜면 운전자 글에 코치가 문장 + 의도로 답한다 — 의도는 앱이 가진 것 안에서만,
    // 결과는 위와 같은 기존 진입점(AI 는 문장과 의도만 고른다, docs/design/12_home_coach_dialog.md).

    private var coachOpen = false
    private var highlightBooking = false
    private var sheetRequest: TaskType? = null
    private var bookingChoice: BookingOption? = null
    private var profileRequest = false
    /**
     * 관리자 "시뮬레이션 음성 입력" — 기본 꺼짐(라운드 25: 끔 / 카드 / 카드 + 글).
     * 생성자의 첫 `setup()` 이 이 필드보다 먼저 돌아 그때는 null 이다 — 읽기는 [coachInput] 으로(null = 끔).
     */
    private var coachInputSet: CoachInputMode? = null
    private val coachInput: CoachInputMode get() = coachInputSet ?: CoachInputMode.OFF
    /** 이번 시트의 첫 말과 그 뒤 대화. 시트를 닫으면 버린다. */
    private var coachOpening: String? = null
    private val coachTurns = ArrayList<CoachTurn>()
    private var coachWaiting = false
    /** 시트 세대 — 닫히거나 새로 열리면 올라간다. 늦게 온 답이 닫힌 시트에 떨어지지 않게. */
    private var coachGen = 0

    private fun clearHomeTransient() {
        coachOpen = false
        highlightBooking = false
        sheetRequest = null
        bookingChoice = null
        profileRequest = false
        clearCoachTalk()
    }

    private fun clearCoachTalk() {
        coachOpening = null
        coachTurns.clear()
        coachWaiting = false
        coachGen++
    }

    /** `코치에게 말하기` — 대화 시트를 열고 말풍선을 읽어 준다. 로그 `home coach: open`. */
    fun openCoach() {
        if (_phase.value !is LessonPhase.Setup) return
        coachOpen = true
        highlightBooking = false
        clearCoachTalk()
        coachOpening = coachLine()
        val next = setup()
        _phase.value = next
        next.coach?.let { tts.speak(it.line) }
        Log.i(TAG, "home coach: open choices=${next.coach?.choices}")
    }

    /** 대화 시트 `돌아가기`. */
    fun closeCoach() {
        if (_phase.value !is LessonPhase.Setup || !coachOpen) return
        coachOpen = false
        clearCoachTalk()
        _phase.value = setup()
    }

    /** 답 칩. 시트에 없는 칩은 무시한다. 로그 `home coach: chose …`. */
    fun chooseCoach(choice: CoachChoice) {
        if (_phase.value !is LessonPhase.Setup || !coachOpen) return
        if (choice !in coachChoices()) { Log.w(TAG, "home coach: $choice not offered"); return }
        coachOpen = false
        clearCoachTalk()
        when (choice) {
            CoachChoice.RESERVED_VENUE -> highlightBooking = true
            CoachChoice.PARKING_PRACTICE -> sheetRequest = TaskType.PARKING
            CoachChoice.CONTINUE_LAST -> lastResumable()?.let { (task, mode) -> pinned = task.id to mode }
        }
        _phase.value = setup()
        Log.i(TAG, "home coach: chose $choice")
    }

    /** 화면이 시트를 연 뒤 부른다 — 같은 요청이 다시 그려질 때 시트를 또 열지 않게. */
    fun consumeSheetRequest() {
        if (sheetRequest == null) return
        sheetRequest = null
        if (_phase.value is LessonPhase.Setup) _phase.value = setup()
    }

    /** 화면이 프로필 시트를 연 뒤 부른다. */
    fun consumeProfileRequest() {
        if (!profileRequest) return
        profileRequest = false
        if (_phase.value is LessonPhase.Setup) _phase.value = setup()
    }

    /** 관리자 "시뮬레이션 음성 입력" 켜기·끄기 — 켬 = 카드 + 글(라운드 24 와 같은 동작). 세 값은 [setCoachInput]. */
    fun setCoachTextInput(on: Boolean) = setCoachInput(if (on) CoachInputMode.CARDS_AND_TEXT else CoachInputMode.OFF)

    /** 입력 방식(라운드 25 결정 7). 끄면 진행 중 대화를 버린다(칩은 남는다). 로그 `admin: coach input …`. */
    fun setCoachInput(mode: CoachInputMode) {
        if (coachInput == mode) return
        coachInputSet = mode
        if (!mode.on) { coachTurns.clear(); coachWaiting = false; coachGen++ }
        if (_phase.value is LessonPhase.Setup) _phase.value = setup()
        Log.i(TAG, "admin: coach input $mode")
    }

    /** 말 카드 — 지금 시트에 보이는 카드만 받는다. 그 문장을 운전자 말로 보낸다([sendCoachText]). 로그 `home coach: card …`. */
    fun sendCoachCard(cardId: String) {
        val card = coachCards().firstOrNull { it.id == cardId } ?: run { Log.w(TAG, "home coach: card $cardId not offered"); return }
        if (_phase.value !is LessonPhase.Setup || !coachOpen || coachWaiting) return
        Log.i(TAG, "home coach: card ${card.id}")
        sendCoachText(card.text)
    }

    /** 지금 보일 말 카드 — 입력이 켜져 있고 시트가 열려 있을 때, 코치가 한 번이라도 되물었으면 과제·상황 카드, 아니면 감정·인사 카드. */
    private fun coachCards(): List<SpeechCard> {
        if (!coachInput.on || !coachOpen) return emptyList()
        val stage = if (coachTurns.any { !it.fromDriver }) CardStage.FOLLOW_UP else CardStage.OPENING
        val booked = reservedReadyTask() != null
        val last = lastResumable() != null
        return SpeechCards.all.filter { c ->
            c.stage == stage && when (c.needs) { CardNeed.NONE -> true; CardNeed.BOOKING -> booked; CardNeed.LAST -> last }
        }
    }

    /**
     * 대화 시트의 `보내기` — 텍스트 입력이 켜져 있고 시트가 열려 있고 답을 기다리는 중이 아닐 때만. **움직이는 중(> 5 km/h)이면 답하지 않는다**(절대 규칙 10).
     * 코치 답의 의도가 [CoachIntent.AskMore] 면 시트에 되물음을 쌓고, 아니면 시트를 닫고 기존 진입점으로 떨어진다.
     * 로그 `home coach: heard …` · `home coach: say …` · `home coach: intent=… (ai|rule|fallback) N ms`.
     */
    fun sendCoachText(text: String) {
        if (_phase.value !is LessonPhase.Setup || !coachOpen || !coachInput.on || coachWaiting) return
        val utterance = text.trim().replace('\n', ' ').take(MAX_UTTERANCE)
        if (utterance.isEmpty()) return
        val history = listOfNotNull(coachOpening?.let { CoachTurn(false, it) }) + coachTurns
        val context = coachContext()
        val firstWords = coachTurns.none { !it.fromDriver }
        coachTurns += CoachTurn(true, utterance)
        coachWaiting = true
        val gen = coachGen
        _phase.value = setup()
        Log.i(TAG, "home coach: heard \"$utterance\"")
        scope.launch {
            val started = clock()
            val speed = runCatching { vehicle.get(listOf(VssConstants.VEHICLE_SPEED))[VssConstants.VEHICLE_SPEED].toVssFloat() }.getOrNull() ?: 0f
            val reply = when {
                speed > VehicleSnapshot.LOCK_SPEED_KMH -> CoachReply(IntentRules.MOVING_LINE, CoachIntent.AskMore)
                else -> try {
                    coach.converse(history, utterance, context)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "coach.converse failed → rules", e)
                    IntentRules.reply(utterance, context).copy(source = ReplySource.FALLBACK)
                }
            }.let { r -> if (context.allows(r.intent)) r else CoachReply(IntentRules.ASK_LINE, CoachIntent.AskMore, ReplySource.FALLBACK) }
                .let { r -> if (firstWords) askBackFirst(r, utterance, context) else r }
            if (gen != coachGen || _phase.value !is LessonPhase.Setup || !coachOpen) {
                Log.w(TAG, "home coach: reply after the sheet closed → dropped"); return@launch
            }
            coachWaiting = false
            Log.i(TAG, "home coach: say \"${reply.say}\"")
            Log.i(TAG, "home coach: intent=${reply.intent.code} (${reply.source.log}) ${clock() - started} ms")
            applyCoachReply(reply)
        }
    }

    /**
     * 첫 말이 감정·인사뿐이면(규칙이 되묻는 말이면) 과제로 바로 가지 않고 규칙 문장으로 한 번 되묻는다.
     * 사외 피드백 #6(10/7): AI 가 "오랜만이라 무서워요" 에 곧바로 출발 전 점검을 골라 시트가 닫혔다(2/2) — 대화로도 어색했다.
     */
    private fun askBackFirst(reply: CoachReply, utterance: String, context: CoachContext): CoachReply {
        if (reply.intent == CoachIntent.AskMore) return reply
        val rule = IntentRules.reply(utterance, context)
        if (rule.intent != CoachIntent.AskMore) return reply
        Log.i(TAG, "home coach: first words ask back — ${reply.intent.code} (${reply.source.log}) → ASK_MORE")
        return rule
    }

    private fun applyCoachReply(reply: CoachReply) {
        tts.speak(reply.say)
        when (val intent = reply.intent) {
            CoachIntent.AskMore -> coachTurns += CoachTurn(false, reply.say)
            else -> {
                coachOpen = false
                clearCoachTalk()
                when (intent) {
                    is CoachIntent.OpenSheet -> sheetRequest = intent.category
                    is CoachIntent.PinTask -> { pinned = intent.taskId to intent.mode; bookingChoice = null }
                    is CoachIntent.Booking -> pinBooking(intent.option)
                    CoachIntent.ShowBooking -> highlightBooking = true
                    CoachIntent.ContinueLast -> lastResumable()?.let { (task, mode) -> pinned = task.id to mode }
                    CoachIntent.OpenProfile -> profileRequest = true
                    CoachIntent.AskMore -> {}
                }
            }
        }
        _phase.value = setup()
    }

    /** 대화의 상황 — READY 과제(지원 모드·지금 추천) · 예약 · 지난 기록. 프롬프트와 의도 검증이 같이 쓴다. */
    private fun coachContext(): CoachContext {
        fun option(task: Task) = TaskOption(task.id, task.title, task.type, LessonMode.entries.filter { task.supports(it) },
            ModeAdvisor.suggest(task, store).mode.takeIf { task.supports(it) } ?: LessonMode.entries.first { task.supports(it) })
        val reserved = reservedReadyTask()
        val last = lastResumable()
        return CoachContext(profile, tasks.filter { it.isReady }.map(::option),
            bookingVenue = store.reservation?.venue(venues)?.name?.takeIf { reserved != null },
            bookingOptions = bookingOptionsFor(reserved), last = last?.first?.let(::option), lastMode = last?.second)
    }

    /** 예약 카드의 `코스 연습`/`모의시험` — 예약 코스 과제로 홈 제안을 고정한다. 로그 `home booking: …`. */
    fun chooseBooking(option: BookingOption) {
        if (_phase.value !is LessonPhase.Setup) return
        pinBooking(option)
    }

    private fun pinBooking(option: BookingOption) {
        val task = reservedReadyTask() ?: return
        if (option !in bookingOptionsFor(task)) return
        val mode = when (option) {
            BookingOption.COURSE_PRACTICE -> ModeAdvisor.suggest(task, store).mode.takeIf { it != LessonMode.EVALUATE && task.supports(it) }
                ?: listOf(LessonMode.HINT, LessonMode.GUIDE).first { task.supports(it) }
            BookingOption.MOCK_EXAM -> LessonMode.EVALUATE
        }
        pinned = task.id to mode
        bookingChoice = option
        highlightBooking = false
        _phase.value = setup()
        Log.i(TAG, "home booking: $option task=${task.id} mode=$mode")
    }

    private fun reservedReadyTask(): Task? = ModeAdvisor.reservedTask(tasks.filter { it.isReady }, store.reservation, venues)

    private fun bookingOptionsFor(task: Task?): List<BookingOption> = when (task) {
        null -> emptyList()
        else -> BookingOption.entries.filter { option ->
            when (option) {
                BookingOption.COURSE_PRACTICE -> task.supports(LessonMode.HINT) || task.supports(LessonMode.GUIDE)
                BookingOption.MOCK_EXAM -> task.supports(LessonMode.EVALUATE)
            }
        }
    }

    /** 마지막 회차의 과제·모드 — 아직 시작할 수 있을 때만. */
    private fun lastResumable(): Pair<Task, LessonMode>? {
        val last = store.all().lastOrNull() ?: return null
        val task = tasks.firstOrNull { it.id == last.taskId && it.isReady && it.supports(last.mode) } ?: return null
        return task to last.mode
    }

    private fun coachChoices(): List<CoachChoice> = buildList {
        if (reservedReadyTask() != null) add(CoachChoice.RESERVED_VENUE)
        add(CoachChoice.PARKING_PRACTICE)
        if (lastResumable() != null) add(CoachChoice.CONTINUE_LAST)
    }

    private fun coachLine(): String {
        val venue = store.reservation?.venue(venues)?.takeIf { reservedReadyTask() != null }
        return when {
            venue != null -> "오늘은 뭘 해 볼까요? 예약한 ${venue.name}에서 해도 돼요."
            lastResumable() != null -> "오늘은 뭘 해 볼까요? 지난번 것을 이어서 해도 돼요."
            else -> "오늘은 뭘 해 볼까요?"
        }
    }

    // ───────── 내부 ─────────

    private fun setup(): LessonPhase.Setup {
        val booking = store.reservation
        val pin = pinned?.let { (id, mode) -> tasks.firstOrNull { it.id == id }?.let { it to mode } }
        val task = pin?.first ?: ModeAdvisor.suggestTask(profile, tasks, booking, venues)
        val s = pin?.let { ModeAdvisor.Suggestion(it.second, ModeAdvisor.pinnedReason(it.second)) } ?: ModeAdvisor.suggest(task, store)
        val reserved = reservedReadyTask()
        val fromReservation = booking != null && reserved?.id == task.id
        val reason = if (fromReservation) ModeAdvisor.reservedReason(s.mode, mockExam = bookingChoice == BookingOption.MOCK_EXAM) else s.reason
        return LessonPhase.Setup(profile, tasks, task, s.mode, reason, venues = venues, booking = booking,
            coach = if (coachOpen) CoachDialog(coachOpening ?: coachLine(), coachChoices(), coachTurns.toList(), coachWaiting, coachCards()) else null,
            bookingOptions = if (booking != null) bookingOptionsFor(reserved) else emptyList(),
            bookingChoice = bookingChoice, highlightBooking = highlightBooking, sheetRequest = sheetRequest,
            onboarding = if (onboardingPending) ProfileOnboarding(ProfileField.ONBOARDING) else null,
            profileRows = ProfileField.entries.map { profileRow(it) }, observedLines = observedLines(),
            coachTextInput = coachInput.on, coachInput = coachInput, profileRequest = profileRequest)
    }

    private fun briefingLine(task: Task, mode: LessonMode): String {
        val watch = task.watch.joinAsObjects()   // "핸들 방향과 기어 전환과 뒤 거리를"
        return when (mode) {
            LessonMode.GUIDE -> "${task.title}, 가이드 모드. 제가 단계마다 말하고 확인할게요. 오늘은 $watch 봅니다."
            LessonMode.HINT -> "${task.title}, 힌트 모드. 조용히 있다가 필요한 순간에만 말할게요. 오늘은 $watch 봅니다."
            LessonMode.EVALUATE -> if (task.course?.isExam == true) "${task.title}, 시험 모드. 실제 시험처럼 구간 안내와 감점만 말할게요. 다 되면 버튼을 눌러 주세요."
                else "${task.title}, 평가 모드. 끝까지 조용히 보고 있을게요. 다 되면 버튼을 눌러 주세요."
            LessonMode.QUIZ -> "${task.title}. 정차 중이니 편하게 답해 주세요."
        }
    }

    private suspend fun startAttempt() {
        val (task, mode) = current ?: return
        attempt++
        val checklist = task.type == TaskType.CHECKLIST
        recorder.reset()
        // 배지 분모 — 점검 12(9/28) · 주차는 과제 사양의 키(후면 8 · 전면 7, 10/2). 힌트 문장도 진입 기어로 갈린다("전진/후진으로 보정")
        val spec = task.parkingSpec
        // 코스 과제(10/4)는 차량 9 + 장치 2 + 시험장 6 = 17키가 배지 분모
        course = task.course?.let { CourseRecorder(it.forMode(evaluate = mode == LessonMode.EVALUATE)) }   // 연습 곁가지(S자)는 연습 모드에만(라운드 25 결정 4)
        recorder.keys = when {
            checklist -> ParkingRecorder.CHECKLIST_KEYS
            course != null -> CourseRecorder.KEYS
            else -> spec.keys
        }
        hints = HintRules(checklist = checklist, entryGear = spec.entryGear, idealReversals = spec.idealReversals)
        zoneLine = null
        lastSignal = null
        lastEmergency = false
        attemptStartMillis = clock()
        lastHint = null
        askedDone = false
        finishing = false
        // 가이드가 "어느 신호를 확인할 수 있나"를 알려면 현재값을 먼저 봐야 한다 — 구독의 첫 emit 을 기다리지 않고 직접 읽는다.
        val now = vehicle.get(recorder.keys.toList())
        registry.onValues(now)
        snapshot = snapshot.apply(now)
        recorder.onDelta(0L, now)
        doorArmed = !snapshot.doorOpen
        if (!doorArmed) Log.i(TAG, "door already open at attempt start → door exit disarmed until it closes")
        course?.let { c ->
            lastSignal = snapshot.signal
            lastEmergency = snapshot.emergency == true
            startCourse(task, mode, c, now)
            return
        }
        guide = if (mode == LessonMode.GUIDE) GuideRunner(guideFor(task), registry) else null
        publishManeuver(task, mode, enter = true)
        ensureVehicleSubscription(recorder.keys)
        guide?.start()?.forEach { tts.speak(it) }
        guide?.unverified?.forEach { unverifiedSteps += it.say }
        publishManeuver(task, mode)
        // 회차 시작 한 마디 — 브리핑·지난 회차 멘트가 자막에 남지 않게 갈아 준다(가이드는 첫 단계 문장이 그 역할). 회차 번호는 말하지 않는다(운전자 문장 숫자 금지 — 감사 08 A1-02)
        when (mode) {
            LessonMode.HINT -> tts.speak(if (attempt > 1) "다시 시작해요. 필요할 때만 말할게요." else "필요할 때만 말할게요.")
            LessonMode.EVALUATE -> tts.speak(if (attempt > 1) "다시 시작해요. 조용히 볼게요." else "조용히 볼게요.")
            else -> {}
        }
        Log.i(TAG, "attempt $attempt start (${mode}) missing=${registry.missingKeys(recorder.keys)}")
    }

    // ───────── 코스 과제 (10/4) ─────────

    private fun startCourse(task: Task, mode: LessonMode, c: CourseRecorder, now: Map<String, String>) {
        guide = null
        publishDrive(task, mode, enter = true)
        ensureVehicleSubscription(recorder.keys)
        val exam = c.course.isExam
        when (mode) {
            LessonMode.GUIDE -> tts.speak(if (attempt > 1) "다시 시작해요. 구간마다 할 일을 말할게요." else "구간마다 할 일을 말할게요.")
            LessonMode.HINT -> tts.speak(if (attempt > 1) "다시 시작해요. 필요할 때만 말할게요." else "필요할 때만 말할게요.")
            LessonMode.EVALUATE -> tts.speak(if (exam) "시험을 시작합니다." else if (attempt > 1) "다시 시작해요. 조용히 볼게요." else "조용히 볼게요.")
            else -> {}
        }
        onCourseUpdate(task, mode, c.onDelta(0L, now))
        publishDrive(task, mode)
        Log.i(TAG, "attempt $attempt start (${mode}) course=${c.course.id} missing=${registry.missingKeys(recorder.keys)}")
    }

    /** 구간 진입·감점을 모드에 맞춰 말한다. 감점 순간은 규칙이 지연 0으로(AI 아님). 문장에 숫자 없음. */
    private fun onCourseUpdate(task: Task, mode: LessonMode, u: CourseUpdate) {
        val c = course ?: return
        val exam = c.course.isExam
        u.entered?.let { z ->
            Log.i(TAG, "course zone: ${z.id}")
            when (mode) {
                // 화면은 "가장 최근 문장" 을 보여야 한다(10/5, 시연 녹화에서 발견) — 새 구간 문장이 지난 감점 문장을 덮게 lastHint 를 비운다
                LessonMode.GUIDE -> { zoneLine = z.guide; lastHint = null; tts.speak(z.guide) }
                LessonMode.EVALUATE -> if (exam) { zoneLine = z.announce; lastHint = null; tts.speak(z.announce) }
                LessonMode.HINT -> z.rules.filterIsInstance<ZoneRule.Indicator>().firstOrNull()?.let { r ->
                    val on = if (r.side == com.moah.hackathon.scoring.Side.LEFT) snapshot.indicatorLeft else snapshot.indicatorRight
                    if (on == false) hint("${r.side.label} 방향지시등을 켜요.")
                }
                else -> {}
            }
        }
        for (d in u.deductions) {
            Log.i(TAG, "course deduction: ${d.reason} zone=${d.zoneId} points=${d.points} disqualify=${d.disqualify}")
            when (mode) {
                LessonMode.GUIDE, LessonMode.HINT -> hint(d.say)
                // 시험 방송도 화면에 남긴다 — 소리가 없는 사내 에뮬·무음 녹화에서도 감점 순간이 보이게(10/5)
                LessonMode.EVALUATE -> if (exam) {
                    val line = if (d.disqualify) "${d.reason}, 실격입니다. 연습은 끝까지 이어 가요." else "${d.reason}, 감점입니다."
                    lastHint = line
                    tts.speak(line, SpeechPriority.URGENT)
                }
                else -> {}
            }
        }
    }

    private fun hint(text: String) {
        lastHint = text
        tts.speak(text, SpeechPriority.URGENT)
        Log.i(TAG, "hint: $text")
    }

    private fun onDriveDelta(p: LessonPhase.Drive, delta: Map<String, String>, doorExit: Boolean) {
        val c = course ?: return
        val t = clock() - attemptStartMillis
        recorder.onDelta(t, delta)
        onCourseUpdate(p.task, p.mode, c.onDelta(t, delta))
        // 감점 전에 먼저 알려 줄 것 — 돌발(멈추세요)·빨간불(정지선). 평가 모드는 조용
        val emergency = snapshot.emergency == true
        if (emergency && !lastEmergency && p.mode != LessonMode.EVALUATE) hint("돌발 상황이에요. 멈추세요.")
        lastEmergency = emergency
        val signal = snapshot.signal
        if (signal == TrackSignal.RED && lastSignal != TrackSignal.RED && snapshot.moving && p.mode != LessonMode.EVALUATE) hint("빨간불이에요. 정지선 앞에서 멈춰요.")
        lastSignal = signal
        // 다 된 것 같으면 한 번 — 움직인 뒤 정차 + P 이고, 마지막 구간에 있거나 · 남은 구간이 없거나(마지막 구간을 지나쳐 섬, 10/5) · 위치를 모를 때
        val progress = c.progress()
        val atEnd = !progress.positionMeasured || progress.currentZoneId == c.course.zones.last().id ||
            (progress.currentZoneId == null && progress.nextZoneId == null && progress.passedZoneIds.isNotEmpty())
        val moved = recorder.metrics()?.motion?.firstMoveMillis != null
        if (!askedDone && atEnd && moved && snapshot.stopped && snapshot.gear == com.moah.hackathon.vehicle.Gear.PARK) {
            askedDone = true
            tts.speak("다 되셨나요? 다 됐으면 버튼을 눌러 주세요.")
            Log.i(TAG, "asked done (attempt $attempt)")
        }
        publishDrive(p.task, p.mode)
        if (doorExit) exitByDoor()
    }

    /** [publishManeuver] 와 같은 경합 규칙 — 지금이 Drive 이고 마무리 중이 아닐 때만 갱신. */
    private fun publishDrive(task: Task, mode: LessonMode, enter: Boolean = false) {
        val c = course ?: return
        val next = LessonPhase.Drive(
            task = task, mode = mode, attempt = attempt, snapshot = snapshot, course = c.course, progress = c.progress(),
            zoneLine = zoneLine, lastHint = lastHint, elapsedMillis = clock() - attemptStartMillis, askedDone = askedDone,
            availability = registry.snapshot(recorder.keys),
        )
        if (enter) _phase.value = next
        else _phase.update { cur -> if (cur is LessonPhase.Drive && !finishing) next else cur }
    }

    private fun ensureVehicleSubscription(keys: Set<String> = ParkingRecorder.KEYS) {
        if (vehicleJob?.isActive == true) return
        vehicleJob = vehicle.observe(keys.toList())
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
            is LessonPhase.Drive -> onDriveDelta(p, delta, doorExit)
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
                    // 7단계(9/28): 벨트·시동·P + 신호가 있는 등화는 전부 확인됐을 때. 미측정(null)은 제외 — 안 묻고 버튼을 기다린다
                    val pd = recorder.metrics()?.preDrive
                    snapshot.belt == true && snapshot.ignitionOn == true && snapshot.gear == com.moah.hackathon.vehicle.Gear.PARK &&
                        pd?.leftIndicatorChecked != false && pd?.rightIndicatorChecked != false && pd?.hazardChecked != false
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
                // 출발 전 점검(7단계, 9/28)은 "문 닫기" 가 1단계라 회차 중 도어 열림이 종료가 아니다 — 리포트 진입은 Done 에서만
                if (doorExit && p.task.type != TaskType.CHECKLIST) exitByDoor()
            }
            // 결과 화면(정차 전용)에서 다시 움직이면 잠근다 — 화면은 버튼을 숨기고 "운전에 집중" 만(절대 규칙 10, 감사 08 A1-01)
            is LessonPhase.Done -> {
                if (p.locked != snapshot.locked) _phase.value = p.copy(locked = snapshot.locked)
                if (doorExit) { Log.i(TAG, "door opened → report"); scope.launch { toReport() } }
            }
            is LessonPhase.Report -> if (p.locked != snapshot.locked) _phase.value = p.copy(locked = snapshot.locked)
            is LessonPhase.QuizDone -> if (p.locked != snapshot.locked) _phase.value = p.copy(locked = snapshot.locked)
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
            availability = registry.snapshot(recorder.keys),
            preDrive = if (task.type == TaskType.CHECKLIST) recorder.metrics()?.preDrive else null,
        )
        if (enter) _phase.value = next
        else _phase.update { current -> if (current is LessonPhase.Maneuver && !finishing) next else current }
    }

    /**
     * 정차 + 운전석 도어 열림 = 세션 끝. 이번 회차에 **움직임이 있었을 때만** 채점한다 — 회차 시작 직후(움직이기 전) 문을 열면
     * 만점짜리 빈 회차를 만들지 않고 지난 회차들로 리포트(없으면 Setup)로 간다(사내 피드백 #4, 10/6).
     */
    private fun exitByDoor() {
        val moved = recorder.metrics()?.motion?.firstMoveMillis != null
        if (moved) { Log.i(TAG, "door opened while stopped → finish + report"); finishAttempt() }
        else Log.i(TAG, "door opened before moving → report without scoring this attempt")
        scope.launch { toReport() }
    }

    private suspend fun toReport() {
        // finishAttempt 가 코루틴으로 Done 을 만들 때까지 기다린다 — 사내 Cloud 코치 응답이 1.4~3.3 s 라 예전 1 s 대기로는
        // "no attempts → back to setup" 뒤에 Done 이 늦게 덮여 화면이 멈췄다(사내 피드백 #4). 코치 시간 제한(5 s)보다 넉넉히
        var waited = 0
        while (finishing && waited < FINISH_WAIT_STEPS) { delay(50); waited++ }
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
            "${task.title} 연습을 마쳤어요.\n오늘도 끝까지 했어요. 수고했어요."
        }
        val nextTask = ModeAdvisor.suggestTask(profile, tasks)
        val next = ModeAdvisor.suggest(task, store)
        // 구독은 유지 — Report 도 움직이면 잠근다(절대 규칙 10, 감사 08 A1-01). reset 이 끊는다
        guide = null
        _phase.value = LessonPhase.Report(
            LessonReport(
                task = task, mode = mode, attempts = attempts, best = best, summary = summary,
                nextTask = nextTask, nextMode = next.mode, nextReason = next.reason,
                shareLevels = ShareLevel.entries.toList(), benefits = benefits,
                unverifiedGuideSteps = unverifiedSteps.toList(),
                askOne = ProfileChips.nextAsk(answered, askSkips)?.let { profileRow(it) },
            ),
            locked = snapshot.locked,
        )
        tts.speak("$summary\n다음엔 ${next.mode.label} 모드 어때요? ${next.reason}", SpeechPriority.URGENT)
        Log.i(TAG, "report: attempts=${attempts.size} best=${best.skill}/${best.safety} badge=${best.badge}")
    }

    private companion object {
        const val TAG = "MOAH/LessonStateMachine"
        /** 채점 코루틴을 기다리는 최대 횟수 × 50 ms = 8 s(Cloud 코치 시간 제한 5 s + 여유). */
        const val FINISH_WAIT_STEPS = 160
        /** 운전자 글의 최대 길이 — 넘으면 자른다. */
        const val MAX_UTTERANCE = 120
    }
}
