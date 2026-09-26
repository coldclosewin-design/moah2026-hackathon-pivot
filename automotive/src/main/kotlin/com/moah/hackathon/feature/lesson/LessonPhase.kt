package com.moah.hackathon.feature.lesson

import com.moah.hackathon.vehicle.SignalAvailability

/**
 * 연수 세션의 단계 (docs/topics/01_driving_coach.md §4.1). 화면은 이것과 `LessonViewModel.subtitle` 만 본다.
 *
 * Setup → Briefing → [Maneuver → Done] × 회차 → Report → (reset) Setup
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
        val reservation: ReservationCard?,
    ) : LessonPhase

    /** "후면 직각 주차, 가이드 모드. 오늘은 핸들 방향과 기어 전환을 봅니다." */
    data class Briefing(val task: Task, val mode: LessonMode, val line: String) : LessonPhase

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
    ) : LessonPhase

    /** 운전자가 "다 됐어요" → 회차 결과와 멘트. 다음 회차 또는 세션 종료. */
    data class Done(val task: Task, val mode: LessonMode, val attempt: Int, val record: AttemptRecord) : LessonPhase

    /** 세션 종료(버튼 또는 정차 + 운전석 도어 열림) → 진단 리포트. */
    data class Report(val report: LessonReport) : LessonPhase
}

data class GuideStepView(val index: Int, val count: Int, val say: String, val waitingFor: String, val unverified: Boolean)
