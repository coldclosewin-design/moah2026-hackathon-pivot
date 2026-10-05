package com.moah.hackathon.feature.lesson

import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.scoring.CourseResult
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.TrackCourse
import com.moah.hackathon.vehicle.TrackSignal
import com.moah.hackathon.vehicle.toTrackSignal
import com.moah.hackathon.vehicle.toWiperOn
import com.moah.hackathon.scoring.ParkingScore
import com.moah.hackathon.scoring.ParkingSpec
import com.moah.hackathon.scoring.ParkingVerdict
import com.moah.hackathon.scoring.PathPoint
import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.toVssBoolean
import com.moah.hackathon.vehicle.toVssFloat
import com.moah.hackathon.vehicle.toVssGear
import com.moah.hackathon.vehicle.toVssIgnitionOn
import mobis.vss.VssConstants

// ───────── 과제 카탈로그 (docs/topics/01_driving_coach.md §3.1) ─────────

enum class TaskType { CHECKLIST, DRIVING, PARKING, KNOWLEDGE }

enum class Difficulty(val label: String) { EASY("하"), MEDIUM("중"), HARD("상") }

/**
 * 과제가 실제로 돌아가는가. 카탈로그에는 계획된 과제도 보여 주되(제품의 폭), **시작은 [READY] 만** 된다 —
 * 채점기·가이드가 없는 과제를 주차 채점기로 돌리는 사고를 막고, 화면은 "준비 중"으로 정직하게 표시한다.
 */
enum class TaskStatus(val label: String) { READY("가능"), PLANNED("준비 중") }

data class Task(
    val id: String,
    val title: String,
    val type: TaskType,
    val difficulty: Difficulty,
    val summary: String,
    /** Briefing 에서 "오늘은 ○○과 ○○을 봅니다" 로 읽는 항목. */
    val watch: List<String>,
    /** B층 채점·가이드 확인에 쓰는 신호. 없어도 과제는 진행된다(미측정). */
    val requiredSignals: Set<String>,
    val requiresDriving: Boolean,
    val status: TaskStatus = TaskStatus.PLANNED,
    /**
     * 주차 과제의 사양(진입 기어·목표 각·구독 키, 10/2). null 이면 후면 직각([ParkingSpec.REAR_PERPENDICULAR]) — 9/26 부터의 동작 그대로.
     * 상태기계·힌트·조언·화면(도식 방향)이 이것으로 "앞으로 들어가는 주차인가" 를 안다.
     */
    val parking: ParkingSpec? = null,
    /**
     * 코스 과제(도로 주행 5종 · 장내기능 모의시험, 10/4)의 도면·구간 규칙. 있으면 상태기계가 주차 흐름 대신 [LessonPhase.Drive] 로 진행하고
     * [com.moah.hackathon.scoring.CourseRecorder] 로 채점한다. 위치는 시뮬레이션 신호(`Track.*`).
     */
    val course: TrackCourse? = null,
) {
    val isReady: Boolean get() = status == TaskStatus.READY
    val isCourse: Boolean get() = course != null

    /** 주차 사양 — 과제에 없으면 후면 직각. 점검·주행 과제도 주차 채점기를 쓰므로 항상 값이 있다. */
    val parkingSpec: ParkingSpec get() = parking ?: ParkingSpec.REAR_PERPENDICULAR

    /** 이 과제로 그 모드를 시작할 수 있나. 지식 테스트는 지식 과제에만, 나머지 셋은 주행·주차 과제에만. */
    fun supports(mode: LessonMode): Boolean = when (mode) {
        LessonMode.QUIZ -> type == TaskType.KNOWLEDGE
        else -> type != TaskType.KNOWLEDGE
    }
}

/** 손을 떼 가는 순서 (§3.2). */
enum class LessonMode(val label: String) { GUIDE("가이드"), HINT("힌트"), EVALUATE("평가"), QUIZ("지식 테스트") }

// ───────── 프로필 두 층 (§3.4) ─────────

/** 첫 설정 대화 5문항 — 자기 진술. */
data class ProfileStatement(
    val licenseYear: Int? = null,
    val monthsSinceLastDrive: Int? = null,
    val car: String? = null,
    val goal: String? = null,
    val fear: String? = null,
)

/** 앱이 세션에서 알아낸 것 — 관측. [ProgressStore.observation] 이 갱신한다. */
data class ProfileObservation(
    val attempts: Int = 0,
    val meanSegments: Float? = null,
    val meanReversals: Float? = null,
    val harshEvents: Int = 0,
    val weakTaskId: String? = null,
)

data class Profile(
    val name: String,
    val statement: ProfileStatement,
    val observation: ProfileObservation = ProfileObservation(),
) {
    /** 마지막 운전 이후 햇수 — "장롱 N년차" 의 근거. 모르면 null. */
    val rustyYears: Int? get() = statement.monthsSinceLastDrive?.let { it / 12 }
    val isFirstTimer: Boolean get() = observation.attempts == 0
}

data class ProfileQuestion(val id: String, val ask: String, val hint: String)

// ───────── 차량 스냅샷 — 상태기계·가이드·힌트가 보는 현재 값 ─────────

data class VehicleSnapshot(
    val speedKmh: Float = 0f,
    val gear: Gear? = null,
    val steeringDeg: Float? = null,
    val belt: Boolean? = null,
    val ignitionOn: Boolean? = null,
    val doorOpen: Boolean = false,
    val rearDistanceCm: Float? = null,
    val obstacleWarning: Boolean? = null,
    /** 출발 전 점검 7단계(9/28)용. 신호가 없으면 null(미측정). */
    val brakePressed: Boolean? = null,
    val indicatorLeft: Boolean? = null,
    val indicatorRight: Boolean? = null,
    val hazard: Boolean? = null,
    /** 코스 과제(10/4) — 장치 조작·시험장 신호. 전부 시뮬레이션(SimOnlySignals), 없으면 null. */
    val headlight: Boolean? = null,
    val wiper: Boolean? = null,
    val trackX: Float? = null,
    val trackY: Float? = null,
    val trackHeadingDeg: Float? = null,
    val signal: TrackSignal? = null,
    val emergency: Boolean? = null,
    val lineContact: Boolean? = null,
) {
    val stopped: Boolean get() = speedKmh < STOP_SPEED_KMH
    val moving: Boolean get() = speedKmh > MOVING_SPEED_KMH
    val locked: Boolean get() = speedKmh > LOCK_SPEED_KMH

    fun apply(delta: Map<String, String>): VehicleSnapshot = copy(
        speedKmh = delta[VssConstants.VEHICLE_SPEED]?.toVssFloat()?.let { kotlin.math.abs(it) } ?: speedKmh,
        gear = delta[VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR]?.toVssGear() ?: gear,
        steeringDeg = delta[VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE]?.toVssFloat() ?: steeringDeg,
        belt = delta[VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED]?.toVssBoolean() ?: belt,
        ignitionOn = delta[VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE]?.toVssIgnitionOn() ?: ignitionOn,
        doorOpen = delta[VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN]?.toVssBoolean() ?: doorOpen,
        rearDistanceCm = delta[SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM]?.toVssFloat() ?: rearDistanceCm,
        obstacleWarning = delta[VssConstants.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING]?.toVssBoolean() ?: obstacleWarning,
        brakePressed = delta[VssConstants.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION]?.toVssFloat()?.let { it > 0f } ?: brakePressed,
        indicatorLeft = delta[VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING]?.toVssBoolean() ?: indicatorLeft,
        indicatorRight = delta[VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING]?.toVssBoolean() ?: indicatorRight,
        hazard = delta[VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING]?.toVssBoolean() ?: hazard,
        headlight = delta[SimOnlySignals.LIGHTS_BEAM_LOW_ISON]?.toVssBoolean() ?: headlight,
        wiper = delta[SimOnlySignals.WIPER_FRONT_MODE]?.toWiperOn() ?: wiper,
        trackX = delta[SimOnlySignals.TRACK_POSITION_X_M]?.toVssFloat() ?: trackX,
        trackY = delta[SimOnlySignals.TRACK_POSITION_Y_M]?.toVssFloat() ?: trackY,
        trackHeadingDeg = delta[SimOnlySignals.TRACK_HEADING_DEG]?.toVssFloat() ?: trackHeadingDeg,
        signal = delta[SimOnlySignals.TRACK_SIGNAL_STATE]?.toTrackSignal() ?: signal,
        emergency = delta[SimOnlySignals.TRACK_EVENT_EMERGENCY]?.toVssBoolean() ?: emergency,
        lineContact = delta[SimOnlySignals.TRACK_LINE_CONTACT]?.toVssBoolean() ?: lineContact,
    )

    companion object {
        /** 화면 잠금 — 이 위로는 터치 타깃이 없다(16번 그대로). */
        const val LOCK_SPEED_KMH = 5f
        /** 정차 판정 — 리포트 해제(도어 열림과 함께)와 "다 되셨나요?" 의 조건. */
        const val STOP_SPEED_KMH = 1f
        const val MOVING_SPEED_KMH = 1f
    }
}

// ───────── 회차·세션 결과 ─────────

data class AttemptRecord(
    val index: Int,
    val taskId: String,
    val mode: LessonMode,
    val score: ParkingScore,
    val delta: ParkingDelta?,
    /** "서두.\n조언." 두 문장. 숫자 없음(2026-09-27). 화면은 줄바꿈 단위로 줄을 끊는다. */
    val remark: String,
    val atMillis: Long,
    /** 속도·기어·조향각으로 **추정**한 회차 궤적(미터, 시작 = 원점, +y = 시작 방향). Done 화면의 탑뷰 시뮬레이션. 실제 위치가 아니다. */
    val path: List<PathPoint> = emptyList(),
    /** 네 가지 판정(한 번에·방향(추정)·마무리·안전, docs/design/09). 점검 과제는 null. 운전자 화면은 점수 대신 이것을 보여 준다. */
    val verdict: ParkingVerdict? = null,
    /**
     * 코스 과제의 구간별 결과(10/4) — 감점 사건·구간·지나간 자리·합격. 주차·점검은 null.
     * 감점 점수는 리포트 "자세히 보기"·진단서에만(운전자 문장 숫자 금지). 이때 [score] 의 skill 은 코스 점수, safety 는 급조작·벨트 기준.
     */
    val course: CourseResult? = null,
)

/**
 * 진단서 공유 범위 (§3.5, 구조 A). 단계가 올라갈수록 보상이 커진다. 실제 전송은 없다.
 *
 * 라운드 22 결정(진단서 B, 10/5): 범위마다 **받을 수 있는 혜택**([benefit])과 **그 조건**([condition])을 짝으로 둔다 —
 * "예상 혜택" 이 아니라 "이 범위로 공유하면 무엇이 생기나". 화면은 둘 다 "· 예시" 를 붙여 보인다(실제 연계 없음).
 * [includes] 는 공유 예시 시트의 포함 항목(범위가 넓을수록 앞 단계 것을 다 포함), [EXCLUDED] 는 어느 범위에서도 나가지 않는 것.
 */
enum class ShareLevel(
    val label: String,
    val description: String,
    val benefit: String,
    val condition: String,
    val includes: List<String>,
) {
    SCORE_ONLY("총점만", "숙련·안전 두 점수만",
        "제휴 시험장 대여료 할인", "총점만 공유해도",
        listOf("숙련 점수", "안전 점수")),
    PER_ITEM("항목별", "구간 수·조향·기어·근접 등 수치까지",
        "보험사 안전운전 특약 할인 심사 대상", "항목별 기록을 꾸준히 공유하면",
        listOf("숙련 점수", "안전 점수", "이동 구간 수", "조향 왕복", "기어 전환", "근접", "급정지", "방향 편차")),
    RAW("원시 신호", "속도·조향각 시계열까지",
        "적성검사 연계·연구 참여 대상", "원시 신호까지 공유하면",
        listOf("숙련 점수", "안전 점수", "이동 구간 수", "조향 왕복", "기어 전환", "근접", "급정지", "방향 편차",
            "속도 시계열", "조향각 시계열")),
    ;

    companion object {
        /** 어느 범위에서도 공유하지 않는 것 — 공유 예시 시트의 "제외" 줄. */
        val EXCLUDED: List<String> = listOf("위치", "대화", "음성")
    }
}

// ───────── 제휴 시험장 예약 (§3.3 "장소", 결정 D3 = (나), 9/28) — 실제 연계 없음("예시") ─────────

/** 시험장. [distanceKm] 은 U3(위치) 미결이라 예시값, 모르면 null. */
data class Venue(
    val id: String,
    val name: String,
    val area: String,
    val distanceKm: Float?,
    val courses: List<Course>,
    val slots: List<Slot>,
)

/** 코스 — 그 시험장에서 연습하는 과제 묶음. [taskIds] 는 카탈로그 과제 id(계획 과제 포함, 제안은 READY 만). */
data class Course(val id: String, val title: String, val taskIds: List<String>)

/** 오늘의 시간대. [start]/[end] 는 "14:00" 꼴 — 정차 중 선택 화면이라 시각 숫자는 허용. */
data class Slot(val id: String, val start: String, val end: String, val available: Boolean) {
    val label: String get() = "$start–$end"
}

/** 예약 한 건 — 세션 저장소([ProgressStore.reservation])에 하나만. 전송·연계 없음. */
data class Reservation(val venueId: String, val slotId: String, val courseId: String, val madeAtMillis: Long) {
    fun venue(venues: List<Venue>): Venue? = venues.firstOrNull { it.id == venueId }
    fun course(venues: List<Venue>): Course? = venue(venues)?.courses?.firstOrNull { it.id == courseId }
    fun slot(venues: List<Venue>): Slot? = venue(venues)?.slots?.firstOrNull { it.id == slotId }

    companion object {
        /** 화면·확인 카드에 그대로 쓰는 문장(진단서와 같은 방식). */
        const val EXAMPLE_NOTE = "예시입니다 — 실제 예약 연계 없음"
    }
}

// ───────── 지식 테스트 (§3.2) — 정차 중 3지선다. STT 는 사내 확인 뒤, 지금은 화면 버튼 ─────────

data class QuizItem(
    val id: String,
    val question: String,
    val choices: List<String>,
    /** [choices] 의 인덱스. */
    val answer: Int,
    /** 정답·오답 어느 쪽이든 읽어 주는 이유 — 퀴즈의 목적은 채점이 아니라 이걸 듣는 것. */
    val why: String,
) {
    init { require(answer in choices.indices) { "answer index out of range for $id" } }
}

data class QuizResult(val itemId: String, val chosen: Int, val correct: Boolean)

data class QuizRecord(val taskId: String, val results: List<QuizResult>, val atMillis: Long) {
    val correct: Int get() = results.count { it.correct }
    val total: Int get() = results.size
}

data class LessonReport(
    val task: Task,
    val mode: LessonMode,
    val attempts: List<AttemptRecord>,
    val best: ParkingScore,
    val summary: String,
    val nextTask: Task,
    val nextMode: LessonMode,
    val nextReason: String,
    val shareLevels: List<ShareLevel>,
    val benefits: List<String>,
    /** 가이드 모드에서 신호가 없어 확인 없이 읽고 넘긴 단계. 리포트에 "확인할 수 없었어요" 로. */
    val unverifiedGuideSteps: List<String>,
)

// ───────── 관리자 모드 (라운드 22 결정 5 = 준비실 D + 세션 중 띠 C, 10/5) — Fake/Hybrid 시연 빌드에만. Real 빌드엔 진입점이 없다 ─────────

/** 프로필 프리셋 — 준비실의 "프로필 전환" 칩. [id] 는 도구·로그용. */
data class ProfilePreset(val id: String, val label: String, val profile: Profile)

/** 프리셋이 미리 해 두는 예약. 시드에 없거나 빈 시간대가 아니면 무시된다. */
data class ReservationSeed(val venueId: String, val slotId: String, val courseId: String)

/**
 * 시연 프리셋 — 누르면 기록을 비우고 [profileId] 프로필로 바꾼 뒤 홈 제안을 [taskId]·[mode] 로 고정한다(운전자가 다른 걸 고르면 풀린다).
 * [scenarioOrder] 는 띠가 "다음에 누를 시나리오" 를 앞에 두는 순서, [reservation] 은 홈 예약 카드를 보여 줄 때.
 */
data class AdminPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val taskId: String,
    val mode: LessonMode,
    val profileId: String,
    val scenarioOrder: List<String> = emptyList(),
    val reservation: ReservationSeed? = null,
)

// ───────── 홈 코치 대화 (라운드 22 결정 4 = A 알약 → 시트 + D 예약 카드, 4b 간격 A1, 10/5) — 탭 대화, STT 없음 ─────────

/** 대화 시트의 답 칩. [label] 은 화면 글자(숫자 없음). */
enum class CoachChoice(val label: String) {
    RESERVED_VENUE("예약한 시험장으로"),
    PARKING_PRACTICE("주차 연습"),
    CONTINUE_LAST("지난번 이어서"),
}

/** 대화 시트 — 코치 말풍선 한 줄 + 답 칩. 칩은 상황에 맞는 것만(예약이 없으면 예약 칩 없음, 기록이 없으면 "지난번" 없음). */
data class CoachDialog(val line: String, val choices: List<CoachChoice>)

/** 예약 카드의 선택지 — 둘 다 예약한 코스의 과제. 코스 연습 = 지금 실력에 맞는 모드, 모의시험 = 평가 모드(채점만). */
enum class BookingOption(val label: String) {
    COURSE_PRACTICE("코스 연습"),
    MOCK_EXAM("모의시험"),
}
