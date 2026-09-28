package com.moah.hackathon.feature.lesson

import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingScore
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
) {
    val isReady: Boolean get() = status == TaskStatus.READY

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
) {
    val stopped: Boolean get() = speedKmh < STOP_SPEED_KMH
    val moving: Boolean get() = speedKmh > MOVING_SPEED_KMH
    val locked: Boolean get() = speedKmh > LOCK_SPEED_KMH

    fun apply(delta: Map<String, String>): VehicleSnapshot = copy(
        speedKmh = delta[VssConstants.VEHICLE_SPEED]?.toVssFloat()?.let { kotlin.math.abs(it) } ?: speedKmh,
        gear = delta[VssConstants.TRANSMISSION_SELECTED_GEAR]?.toVssGear() ?: gear,
        steeringDeg = delta[VssConstants.STEERING_WHEEL_ANGLE]?.toVssFloat() ?: steeringDeg,
        belt = delta[VssConstants.SEAT_DRIVER_ISBELTED]?.toVssBoolean() ?: belt,
        ignitionOn = delta[VssConstants.LOW_VOLTAGE_SYSTEM_STATE]?.toVssIgnitionOn() ?: ignitionOn,
        doorOpen = delta[VssConstants.DOOR_DRIVER_ISOPEN]?.toVssBoolean() ?: doorOpen,
        rearDistanceCm = delta[VssConstants.OBSTACLE_REAR_DISTANCE_CM]?.toVssFloat() ?: rearDistanceCm,
        obstacleWarning = delta[VssConstants.OBSTACLE_IS_WARNING]?.toVssBoolean() ?: obstacleWarning,
        brakePressed = delta[VssConstants.BRAKE_PEDAL_POSITION]?.toVssFloat()?.let { it > 0f } ?: brakePressed,
        indicatorLeft = delta[VssConstants.LIGHT_INDICATOR_LEFT]?.toVssBoolean() ?: indicatorLeft,
        indicatorRight = delta[VssConstants.LIGHT_INDICATOR_RIGHT]?.toVssBoolean() ?: indicatorRight,
        hazard = delta[VssConstants.LIGHT_HAZARD]?.toVssBoolean() ?: hazard,
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
)

/** 진단서 공유 범위 (§3.5, 구조 A). 단계가 올라갈수록 보상이 커진다. 실제 전송은 없다. */
enum class ShareLevel(val label: String, val description: String) {
    SCORE_ONLY("총점만", "숙련·안전 두 점수만 공유"),
    PER_ITEM("항목별", "구간 수·조향·기어·근접 등 항목별 수치까지"),
    RAW("원시 신호", "회차의 속도·조향각 시계열까지"),
}

/**
 * 시트 아래 카드 한 장(9/26). **9/28 결정 D3 (나) 로 [Venue]·[Reservation] 흐름이 대체한다** — Codex 화면 전환(`codex/reservation`) 뒤 삭제.
 * 그때까지 [LessonPhase.Setup.reservation] 은 예약이 있으면 그 예약을, 없으면 시드 예시를 이 모양으로 준다.
 */
data class ReservationCard(val venue: String, val slot: String, val course: String, val note: String)

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

    /** 옛 카드 모양(화면 전환 전 호환). 시험장을 못 찾으면 null. */
    fun toCard(venues: List<Venue>): ReservationCard? {
        val v = venue(venues) ?: return null
        return ReservationCard(
            venue = v.name,
            slot = slot(venues)?.let { "오늘 ${it.label}" } ?: "오늘",
            course = course(venues)?.title ?: "",
            note = EXAMPLE_NOTE,
        )
    }

    companion object {
        /** 화면·확인 카드에 그대로 쓰는 문장(진단서와 같은 방식). */
        const val EXAMPLE_NOTE = "예시입니다 — 실제 예약 연계 없음"
    }
}

// ───────── 동승자 공유 (§3.5 수신자 "동승자" · 덱 12장) — 옆자리 사람을 코치에서 응원자로. 실제 전송은 없다 ─────────

/** 동승자에게 보여 주는 범위. 진단서 [ShareLevel] 과 같은 3단계 구조, 라벨만 동승자용. */
enum class CompanionShareLevel(val label: String, val description: String) {
    SUMMARY("총평만", "잘한 것·도울 것 두 문장"),
    PROCESS("과정까지", "회차별 궤적과 힌트 이력"),
    FULL("진단서 전체", "항목별 수치까지"),
}

/**
 * 동승자에게 건네는 두 문장 — 점수가 아니다. [praise] 오늘 잘한 것 하나, [help] 다음에 옆에서 도울 것 하나.
 * 운전자 멘트와 같은 규칙: 숫자(횟수·초·점수) 없음. 화면은 "praise\nhelp" 두 줄로 그린다.
 */
data class CompanionNote(val praise: String, val help: String) {
    val text: String get() = "$praise\n$help"
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
    /** 동승자 탭 — 잘한 것 하나·도울 것 하나. 상태기계가 [com.moah.hackathon.ports.CoachPort.companionNote] 로 채운다. */
    val companion: CompanionNote = CompanionNote("", ""),
    val companionShareLevels: List<CompanionShareLevel> = CompanionShareLevel.entries.toList(),
    /** 동승자가 고르는 응원 한마디 후보(시드 3). 고른 문장은 다음 세션 [LessonPhase.Setup.cheer] 가 된다. */
    val cheers: List<String> = emptyList(),
)
