package com.moah.hackathon.feature.lesson

import com.moah.hackathon.scoring.CourseProgress
import com.moah.hackathon.scoring.PreDriveSummary
import com.moah.hackathon.scoring.TrackCourse
import com.moah.hackathon.vehicle.SignalAvailability

/**
 * 연수 세션의 단계 (docs/topics/01_driving_coach.md §4.1). 화면은 이것과 `LessonViewModel.subtitle` 만 본다.
 *
 * Setup → Briefing → [Maneuver → Done] × 회차 → Report → (reset) Setup
 * 코스 과제(도로 5종 · 장내기능 모의시험, 10/4)는 Maneuver 자리에 [Drive].
 *
 * 주차 과제는 [Maneuver] 에서 저속(< 5 km/h)이라 화면 도식을 그려도 된다. 도로 과제는 같은 자리에 잠금 화면이 들어간다.
 * **[Maneuver] 에는 점수·감점 누계가 없다** — 화면에 줄 수 없게 타입에서부터 빠져 있다(AGENTS 규칙 10).
 */
sealed interface LessonPhase {

    /** 정차 중 대화로 설정. 앱이 프로필·이력을 보고 과제·모드를 제안하고, 운전자가 고른다. */
    data class Setup(
        val profile: Profile,
        val tasks: List<Task>,
        val suggestedTask: Task,
        val suggestedMode: LessonMode,
        val reason: String,
        /**
         * 홈이 과제를 "골라 둔" 상태인가(라운드 26 시안 1-6, 10/7). false = 이번 실행에서 아직 연습·퀴즈 기록이 없고 고정(프리셋·대화·홈 모드)도 예약도 없다
         * → 화면은 [suggestedTask] 를 보이지 않고 "과제 고르기" 빈칸을 띄우며 `시작` 을 흐리게 둔다(임의로 정하지 않는다). 기록은 메모리라 앱을 새로 켜면 다시 false.
         */
        val picked: Boolean = true,
        /** 제휴 시험장 목록(D3, 9/28). 화면의 시험장 층이 그린다. */
        val venues: List<Venue> = emptyList(),
        /** 지금 예약. 없으면 null — Setup 배지 자리도 없다. */
        val booking: Reservation? = null,
        /** 홈 코치 대화(라운드 22 결정 4 = A 알약 → 시트). 열려 있을 때만 — 화면은 오른쪽 열을 대화 시트로 바꾼다. */
        val coach: CoachDialog? = null,
        /** 예약 카드의 선택지(결정 4 = D). 예약이 있을 때만. */
        val bookingOptions: List<BookingOption> = emptyList(),
        /** 예약 카드에서 고른 것 — 칩 강조. 아직 안 골랐으면 null. */
        val bookingChoice: BookingOption? = null,
        /** 대화에서 "예약한 시험장으로" 를 골랐다 — 예약 카드에 강조 테두리. */
        val highlightBooking: Boolean = false,
        /** 대화가 "과제 시트를 이 분류로 열어라" 를 요청했다. 화면은 시트를 연 뒤 `consumeSheetRequest` 를 부른다. */
        val sheetRequest: TaskType? = null,
        /** 첫 실행 프로필 질문(결정 7 = P2). 아직 안 마쳤을 때만 — 화면은 전체 화면으로 이 줄들을 차례로 펼친다. */
        val onboarding: ProfileOnboarding? = null,
        /** 프로필 다섯 줄(질문 · 지금 답 · 칩) — 첫 실행 화면과 홈 눈썹 시트가 같이 쓴다. */
        val profileRows: List<ProfileRow> = emptyList(),
        /** 시트 아래 "앱이 본 것" — 숫자 없는 문장. */
        val observedLines: List<String> = emptyList(),
        /**
         * 홈 코치 텍스트 입력(10/6, 관리자 "시뮬레이션 음성 입력"). 켜져 있으면 대화 시트에 입력 칸·보내기와 "음성 입력 · 시뮬레이션" 배지,
         * 버튼 문구는 `코치에게 말하기`. 꺼져 있으면 지금 칩 흐름 그대로이고 버튼 문구는 `코치와 고르기`(말을 듣는다고 읽히지 않게).
         */
        val coachTextInput: Boolean = false,
        /** 입력 방식(라운드 25 결정 7) — [coachTextInput] 은 `coachInput.on`, 입력 칸은 `coachInput.textField` 일 때만(사내 = [CoachInputMode.CARDS] 로 접는다). */
        val coachInput: CoachInputMode = CoachInputMode.OFF,
        /** 대화가 "프로필 시트를 열어라" 를 요청했다. 화면은 홈 눈썹 시트를 연 뒤 `consumeProfileRequest` 를 부른다. */
        val profileRequest: Boolean = false,
    ) : LessonPhase

    /** "후면 직각 주차, 가이드 모드. 오늘은 핸들 방향과 기어 전환을 봅니다." */
    /**
     * 과제 시작 전 브리핑. [line] 을 TTS 가 읽는 동안 머문다 — 음성이 끝나면 1 초 뒤 넘어간다(최소 3 초 · 최대 12 초, 라운드 26 시안 2-B).
     * [expectedMillis] = 음성 길이 어림(글자 수) — 화면의 진행 막대·지금 문장 강조용. 정차 중이면 `건너뛰기`(`skipBriefing`).
     */
    data class Briefing(val task: Task, val mode: LessonMode, val line: String, val expectedMillis: Long = 0L,
        /** 속도 > 5 km/h — 화면은 `건너뛰기` 를 숨긴다(절대 규칙 10). 브리핑 동안만 속도를 구독해 갱신한다. */
        val locked: Boolean = false) : LessonPhase

    /** 주차(또는 주행) 중. 회차마다 새로 시작한다. */
    data class Maneuver(
        val task: Task,
        val mode: LessonMode,
        val attempt: Int,
        val snapshot: VehicleSnapshot,
        /** 가이드 모드에서 지금 기다리는 단계. 다른 모드는 null. */
        val guide: GuideStepView?,
        /** 힌트 모드에서 마지막으로 말한 힌트. */
        val lastHint: String?,
        /** 이동 구간 수 — 점수가 아니라 진행 상황이라 화면에 보여도 된다. */
        val movingSegments: Int,
        val elapsedMillis: Long,
        /** 기어 P + 정차를 보고 "다 되셨나요?" 를 이미 물었나. */
        val askedDone: Boolean,
        /** 도식이 "시뮬"·"미측정" 칩을 달 수 있게. */
        val availability: Map<String, SignalAvailability>,
        /**
         * 출발 전 점검의 **기록된** 확인(9/30) — 지시등·비상등은 켰다 끄는 게 정상이라 [snapshot] 만 보면 확인 뒤 곧 "아직" 이 된다.
         * 회차 시작부터 지금까지의 요약. 주차 과제·기록 없음은 null.
         */
        val preDrive: PreDriveSummary? = null,
    ) : LessonPhase

    /**
     * 코스 과제 주행 중(10/4) — 지도 위 차·지금 구간·다음 구간·신호등·돌발 경보. **점수·감점이 없다**(절대 규칙 10, [CourseProgress] 타입에서부터).
     * 감점은 그 순간 음성으로만 말하고, 결과는 정차 뒤 [Done] 에서. 속도 > 5 km/h([locked])면 화면은 터치 타깃을 두지 않는다 — 지도는 도식이라 그려도 된다.
     */
    data class Drive(
        val task: Task,
        val mode: LessonMode,
        val attempt: Int,
        val snapshot: VehicleSnapshot,
        val course: TrackCourse,
        val progress: CourseProgress,
        /** 마지막으로 읽은 구간 문장(가이드 = 구간 안내, 시험 = 구간 방송). 없으면 null. */
        val zoneLine: String?,
        /** 힌트 모드에서 마지막으로 말한 힌트(감점 순간 문장 포함). */
        val lastHint: String?,
        val elapsedMillis: Long,
        val askedDone: Boolean,
        /** 배지·칩 — 위치·신호등 등 시뮬레이션 신호의 출처. */
        val availability: Map<String, SignalAvailability>,
    ) : LessonPhase {
        val locked: Boolean get() = snapshot.locked
    }

    /** 운전자가 "다 됐어요" → 회차 결과와 멘트. 다음 회차 또는 세션 종료. */
    /** 회차 끝 화면. [locked](속도 > 5) 면 화면은 버튼·터치 타깃을 숨기고 "운전에 집중" 안내만 — 정차 전용 화면에서 다시 움직였을 때(절대 규칙 10, 감사 08 A1-01). */
    data class Done(val task: Task, val mode: LessonMode, val attempt: Int, val record: AttemptRecord, val locked: Boolean = false) : LessonPhase

    /** 세션 종료(버튼 또는 정차 + 운전석 도어 열림) → 진단 리포트. */
    data class Report(val report: LessonReport, val locked: Boolean = false) : LessonPhase

    /**
     * 지식 테스트 한 문제. **정차 중에만** 답할 수 있다 — [locked](속도 > 5) 면 화면은 선택지를 숨기고 상태기계는 [LessonStateMachine.answer] 를 무시한다.
     * [chosen] 이 null 이면 답 대기, 아니면 정답 공개 상태(이유를 읽어 준 뒤 "다음 문제").
     */
    data class Quiz(
        val task: Task,
        val index: Int,
        val total: Int,
        val item: QuizItem,
        val locked: Boolean,
        val chosen: Int?,
        val correctSoFar: Int,
    ) : LessonPhase {
        val answered: Boolean get() = chosen != null
        val isLast: Boolean get() = index >= total - 1
    }

    /** 문제를 다 풀었거나 중간에 끝냄. 정답 수와 문제별 결과(복습용). */
    data class QuizDone(val task: Task, val results: List<QuizResult>, val items: List<QuizItem>, val remark: String, val locked: Boolean = false) : LessonPhase {
        val correct: Int get() = results.count { it.correct }
        val total: Int get() = items.size
    }
}

data class GuideStepView(val index: Int, val count: Int, val say: String, val waitingFor: String, val unverified: Boolean)
