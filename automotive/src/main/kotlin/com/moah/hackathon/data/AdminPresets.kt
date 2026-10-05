package com.moah.hackathon.data

import com.moah.hackathon.feature.lesson.AdminPreset
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Profile
import com.moah.hackathon.feature.lesson.ProfilePreset
import com.moah.hackathon.feature.lesson.ProfileStatement
import com.moah.hackathon.feature.lesson.ReservationSeed

/**
 * 관리자 모드 준비실의 시연 프리셋·프로필 프리셋(라운드 22 결정 5, 10/5 — 시안 `docs/design/round22-proposals/5-admin-mode.html` D).
 * 시연 빌드(Fake/Hybrid + `SHOW_DEMO_PANEL`)에서만 쓰인다. 문구는 Codex 가 다듬어도 되지만 id 는 도구가 쓴다.
 */
object AdminPresets {
    /** 도구(emu_flow·course_flow)가 `am start --es preset <id>` 로 넘기는 인텐트 extra 이름. 화면(`MainActivity`)이 읽어 `admin.applyPreset` 을 부른다. */
    const val EXTRA_PRESET = "preset"

    const val PROFILE_RUSTY = "rusty-10y"
    const val PROFILE_NOVICE = "novice"
    const val PROFILE_SKILLED = "skilled"
    const val PROFILE_EMPTY = "empty"

    val profiles: List<ProfilePreset> = listOf(
        ProfilePreset(PROFILE_RUSTY, "장롱 10년차", SeedCatalog.demoProfile),
        ProfilePreset(PROFILE_NOVICE, "초보", Profile(name = "연수생",
            statement = ProfileStatement(licenseYear = 2026, monthsSinceLastDrive = 0, car = "준중형", goal = "출퇴근", fear = "주차"))),
        ProfilePreset(PROFILE_SKILLED, "숙련", Profile(name = "연수생",
            statement = ProfileStatement(licenseYear = 2012, monthsSinceLastDrive = 0, car = "중형 SUV", goal = "여행", fear = null))),
        ProfilePreset(PROFILE_EMPTY, "비움", Profile(name = "연수생", statement = ProfileStatement())),
    )

    const val REAR_TWO = "rear-two"
    const val EXAM_FAIL_PASS = "exam-fail-pass"
    const val PREDRIVE = "predrive"
    const val KNOWLEDGE = "knowledge"
    const val RESERVED_EXAM = "reserved-exam"

    /** 시연 대본 순서(1부 후면 주차 → 2부 장내 모의시험 → 3부 점검·지식·예약). 첫째가 기본 프리셋. */
    val presets: List<AdminPreset> = listOf(
        AdminPreset(REAR_TWO, "후면 주차 두 회차", "못한 주차 → 잘한 주차", SeedCatalog.TASK_PARKING_REAR, LessonMode.HINT,
            PROFILE_RUSTY, listOf(ParkingScenarios.bad.id, ParkingScenarios.good.id)),
        AdminPreset(EXAM_FAIL_PASS, "장내 모의시험", "불합격 → 합격", SeedCatalog.TASK_TRACK_EXAM, LessonMode.EVALUATE,
            PROFILE_RUSTY, listOf(CourseScenarios.examBad.id, CourseScenarios.examGood.id)),
        AdminPreset(PREDRIVE, "출발 전 점검", "일곱 단계", SeedCatalog.TASK_PREDRIVE, LessonMode.GUIDE,
            PROFILE_RUSTY, listOf(ChecklistScenarios.bad.id, ChecklistScenarios.good.id)),
        AdminPreset(KNOWLEDGE, "지식 테스트", "정차 중 문항", SeedCatalog.TASK_KNOWLEDGE, LessonMode.QUIZ, PROFILE_RUSTY),
        AdminPreset(RESERVED_EXAM, "예약 → 시험장", "서초 장내기능 모의시험 예약", SeedCatalog.TASK_TRACK_EXAM, LessonMode.EVALUATE,
            PROFILE_RUSTY, listOf(CourseScenarios.examBad.id, CourseScenarios.examGood.id),
            ReservationSeed("venue-seocho", "slot-14", SeedCatalog.COURSE_EXAM)),
    )

    fun preset(id: String): AdminPreset? = presets.firstOrNull { it.id == id }
    fun profile(id: String): ProfilePreset? = profiles.firstOrNull { it.id == id }
}
