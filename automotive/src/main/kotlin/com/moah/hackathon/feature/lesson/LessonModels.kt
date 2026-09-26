package com.moah.hackathon.feature.lesson

import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingScore
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
    val remark: String,
    val atMillis: Long,
)

/** 진단서 공유 범위 (§3.5, 구조 A). 단계가 올라갈수록 보상이 커진다. 실제 전송은 없다. */
enum class ShareLevel(val label: String, val description: String) {
    SCORE_ONLY("총점만", "숙련·안전 두 점수만 공유"),
    PER_ITEM("항목별", "구간 수·조향·기어·근접 등 항목별 수치까지"),
    RAW("원시 신호", "회차의 속도·조향각 시계열까지"),
}

data class ReservationCard(val venue: String, val slot: String, val course: String, val note: String)

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
