package com.moah.hackathon.data

import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.feature.lesson.Difficulty
import com.moah.hackathon.feature.lesson.GuideStep
import com.moah.hackathon.feature.lesson.Profile
import com.moah.hackathon.feature.lesson.ProfileQuestion
import com.moah.hackathon.feature.lesson.ProfileStatement
import com.moah.hackathon.feature.lesson.QuizItem
import com.moah.hackathon.feature.lesson.RemarkTemplate
import com.moah.hackathon.feature.lesson.Course
import com.moah.hackathon.feature.lesson.Slot
import com.moah.hackathon.feature.lesson.Venue
import com.moah.hackathon.feature.lesson.ScoreBand
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskStatus
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import mobis.vss.VssConstants as V

/**
 * 시드 데이터. **구조**는 Claude, **문구·좌표**는 Codex 가 다듬는다 (AGENTS 소유권).
 * 실차 값이 아닌 것: 전부. 보상 혜택 문구는 예시이며 실제 전송·계약은 없다.
 */
object SeedCatalog {

    // ───────── 과제 카탈로그 (§3.1) — 시작 가능한 것은 status = READY 만. 나머지는 제품의 폭을 보여 주는 계획 ─────────

    const val TASK_PREDRIVE = "predrive-check"
    const val TASK_PARKING_REAR = "parking-rear-perpendicular"
    const val TASK_KNOWLEDGE = "knowledge-hazard-weather"

    val tasks: List<Task> = listOf(
        Task(TASK_PREDRIVE, "출발 전 점검", TaskType.CHECKLIST, Difficulty.EASY, "문·벨트·기어 P·브레이크와 시동·지시등·비상등을 순서대로. 차는 세운 채로.",
            listOf("문", "안전벨트", "기어 P", "시동", "지시등"),
            setOf(V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION, V.VEHICLE_LOWVOLTAGESYSTEMSTATE,
                V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING), requiresDriving = false,
            status = TaskStatus.READY),   // 가이드 7단계(9/28, D1) + ChecklistScorer + 시나리오 2벌. 움직이지 않는 유일한 조작 과제
        Task("straight-stop", "단순 전진 후 정지", TaskType.DRIVING, Difficulty.EASY, "천천히 출발해 부드럽게 멈추기.",
            listOf("출발", "정지"), setOf(V.VEHICLE_SPEED), requiresDriving = true),
        Task("left-turn-signal", "좌회전 방향지시등", TaskType.DRIVING, Difficulty.EASY, "좌회전 3초 전에 켜고, 돌고 나서 끄기.",
            listOf("방향지시등", "핸들"), setOf(V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE), requiresDriving = true),
        Task("lane-change", "차선 변경", TaskType.DRIVING, Difficulty.MEDIUM, "지시등 → 확인 → 부드럽게 이동.",
            listOf("방향지시등", "핸들", "속도"), setOf(V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE), requiresDriving = true),
        Task("road-course", "일반 도로 코스", TaskType.DRIVING, Difficulty.MEDIUM, "구간마다 기대 행동이 있는 실생활 경로.",
            listOf("속도 유지", "급조작", "방향지시등"), setOf(V.VEHICLE_SPEED), requiresDriving = true),
        Task(TASK_PARKING_REAR, "후면 직각 주차", TaskType.PARKING, Difficulty.HARD, "핸들 끝까지 → 후진 → 45°에서 중립 → 곧게.",
            listOf("핸들 방향", "기어 전환", "뒤 거리"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM), requiresDriving = true,
            status = TaskStatus.READY),   // 채점기·가이드·시나리오가 있는 유일한 과제. 나머지는 카탈로그(계획)만
        Task("parking-parallel", "평행 주차", TaskType.PARKING, Difficulty.HARD, "길가 한 칸에 뒤로 들어가기.",
            listOf("핸들 방향", "기어 전환", "뒤 거리"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM), requiresDriving = true),
        Task("parking-front", "전면 직각 주차", TaskType.PARKING, Difficulty.MEDIUM, "앞을 살피며 주차 칸에 곧게 들어가기.",
            listOf("핸들 방향", "기어 전환", "뒤 거리"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM), requiresDriving = true,
            status = TaskStatus.PLANNED),
        Task("parking-angle", "사선 주차", TaskType.PARKING, Difficulty.HARD, "기울어진 주차 칸의 방향에 맞춰 들어가기.",
            listOf("핸들 방향", "기어 전환", "뒤 거리"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM), requiresDriving = true,
            status = TaskStatus.PLANNED),
        Task("roundabout", "회전교차로", TaskType.DRIVING, Difficulty.HARD, "우선순위 확인 → 진입 → 지시등으로 진출.",
            listOf("속도", "방향지시등"), setOf(V.VEHICLE_SPEED, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING), requiresDriving = true),
        Task(TASK_KNOWLEDGE, "비상등·날씨별 행동", TaskType.KNOWLEDGE, Difficulty.EASY, "정차 중 3지선다 5문제. 채점보다 이유를 듣는 것.",
            listOf("비상등", "우천", "야간"), emptySet(), requiresDriving = false,
            status = TaskStatus.READY),   // 지식 테스트 모드로만 시작된다(Task.supports)
    )

    val parkingTask: Task get() = tasks.first { it.id == TASK_PARKING_REAR }
    val predriveTask: Task get() = tasks.first { it.id == TASK_PREDRIVE }

    /** 시연 패널이 과제별로 보여 주는 Fake 시나리오. 주행 과제는 아직 없다. */
    fun scenariosFor(task: Task): List<Scenario> = when (task.type) {
        TaskType.PARKING -> ParkingScenarios.all
        TaskType.CHECKLIST -> ChecklistScenarios.all
        else -> emptyList()
    }

    // ───────── 가이드 단계 — 신호 확인형 (§4.3) ─────────

    fun guideFor(task: Task): List<GuideStep> = when (task.id) {
        TASK_PARKING_REAR -> parkingGuide
        TASK_PREDRIVE -> predriveGuide
        else -> emptyList()
    }

    val parkingGuide: List<GuideStep> = listOf(
        GuideStep("belt", "안전벨트를 매 주세요.", V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED, "확인했어요.") { s, _ -> s.belt == true },
        GuideStep("ignition", "브레이크를 밟고 시동을 켜 주세요.", V.VEHICLE_LOWVOLTAGESYSTEMSTATE, "좋아요.") { s, _ -> s.ignitionOn == true },
        GuideStep("reverse", "기어를 R로 넣어 주세요.", V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, "좋아요.") { s, _ -> s.gear == Gear.REVERSE },
        GuideStep("steer-right", "핸들을 오른쪽 끝까지 돌리세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "다 돌렸어요. 이제 천천히 후진하세요.") { s, _ -> (s.steeringDeg ?: 0f) <= -400f },
        GuideStep("center", "차가 45도쯤 되면 핸들을 중립으로 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "곧게 후진하세요.") { s, moved -> moved && kotlin.math.abs(s.steeringDeg ?: 999f) < 30f },
        GuideStep("park", "다 들어왔으면 멈추고 기어 P.", V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, "다 되셨나요? 다 됐으면 버튼을 눌러 주세요.") { s, _ -> s.stopped && s.gear == Gear.PARK },
    )

    /**
     * 출발 전 점검 7단계(9/28, 결정 D1) — 시동 꺼진 차에서 시작한다. 문 → 벨트 → 기어 P 확인 → 브레이크 밟고 시동 → 좌 지시등 → 우 지시등 → 비상등.
     * 확인 신호는 전부 스텁에 있던 것(새 VSS 경로 없음). **문장은 Codex 가 다듬는다.**
     */
    val predriveGuide: List<GuideStep> = listOf(
        GuideStep("door", "운전석 문을 닫아 주세요.", V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN, "닫혔어요.") { s, _ -> !s.doorOpen },
        parkingGuide.first { it.id == "belt" },
        GuideStep("park-check", "기어가 P에 있는지 확인해 주세요.", V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, "P 맞아요.") { s, _ -> s.gear == Gear.PARK },
        GuideStep("ignition", "브레이크를 밟고 시동을 켜 주세요.", V.VEHICLE_LOWVOLTAGESYSTEMSTATE, "시동 켜졌어요.") { s, _ -> s.ignitionOn == true },
        GuideStep("indicator-left", "왼쪽 방향지시등을 켜 보세요.", V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, "왼쪽 켜졌어요. 이제 오른쪽.") { s, _ -> s.indicatorLeft == true },
        GuideStep("indicator-right", "오른쪽 방향지시등을 켜 보세요.", V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, "오른쪽도 좋아요.") { s, _ -> s.indicatorRight == true },
        GuideStep("hazard", "비상등을 켜 보세요.", V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING, "비상등 확인. 이제 끄고 버튼을 눌러 주세요.") { s, _ -> s.hazard == true },
    )

    // ───────── 프로필 (§3.4) — 첫 설정 대화 5문항 + 시연용 예시 프로필 ─────────

    val profileQuestions: List<ProfileQuestion> = listOf(
        ProfileQuestion("licenseYear", "면허는 언제 따셨어요?", "연도"),
        ProfileQuestion("monthsSinceLastDrive", "마지막으로 운전한 게 언제쯤이에요?", "몇 달 전"),
        ProfileQuestion("car", "오늘 타는 차는요?", "차종"),
        ProfileQuestion("goal", "운전이 필요한 일은 뭐예요?", "아이 등하원, 마트, 출퇴근…"),
        ProfileQuestion("fear", "제일 무서운 상황은요?", "주차, 고속도로, 야간…"),
    )

    /** 시연용. 장롱면허 10년차, 아이 등하원이 목표, 주차가 무섭다. */
    val demoProfile: Profile = Profile(
        name = "연수생",
        statement = ProfileStatement(licenseYear = 2016, monthsSinceLastDrive = 120, car = "중형 SUV", goal = "아이 등하원", fear = "주차"),
    )

    // ───────── 멘트 풀 (§3.4) — 점수 구간 × 상황 태그. 같은 점수에 같은 말 반복 금지는 RemarkPool 이 지킨다 ─────────

    /**
     * **서두**만 있다 — 조언 문장은 `AdviceRules` 가 지표에서 고른다(2026-09-27 결정: 운전자 문장에 숫자 없음).
     * 운전자에게 보여 주는 서두에는 횟수·초·점수·연차 숫자를 넣지 않는다.
     */
    val remarks: List<RemarkTemplate> = listOf(
        // EXCELLENT
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("rusty", "first"), "오랜만의 주차인데 몸이 기억하네요."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("rusty"), "장롱의 문을 활짝 열었어요. 이 정도면 마트 주차장은 됩니다."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("improved"), "핸들 움직임이 훨씬 매끈해졌어요."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "한 번에 들어갔어요. 이 감각을 몸이 기억하게."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "깔끔했어요. 옆자리에 누가 있었어도 할 말이 없었을 거예요."),
        // GOOD
        RemarkTemplate(ScoreBand.GOOD, setOf("rusty", "first"), "장롱의 문 정도는 열었습니다. 좋은 출발이에요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("first"), "첫 회차에 이만큼이면 충분해요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("improved"), "아까보다 덜 헤맸어요. 방향이 맞아요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("regressed"), "아까보다 조금 더 움직였지만 안전했어요. 그게 더 중요해요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "좋아요. 한 번 더 하면 핸들 타이밍이 손에 붙을 거예요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "잘 들어왔어요. 오늘 이 정도면 충분해요."),
        // OK
        RemarkTemplate(ScoreBand.OK, setOf("rusty", "first"), "오랜만의 연습인데 잘 마쳤어요."),
        RemarkTemplate(ScoreBand.OK, setOf("first"), "처음이라 조금 헤맸어요. 그게 정상이에요."),
        RemarkTemplate(ScoreBand.OK, setOf("improved"), "아까보다 나아졌어요. 이 방향 그대로."),
        RemarkTemplate(ScoreBand.OK, setOf("regressed"), "이번엔 조금 헤맸어요. 괜찮아요, 헤매는 게 연습이에요."),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "시간이 조금 걸렸지만 잘 마쳤어요."),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "들어가긴 했어요. 다음엔 조금 더 가볍게."),
        // ROUGH
        RemarkTemplate(ScoreBand.ROUGH, setOf("rusty", "first"), "오랜만에 다시 시작한 것만으로도 좋은 출발이에요."),
        RemarkTemplate(ScoreBand.ROUGH, setOf("first"), "첫 회차는 원래 이래요. 다음엔 가이드 모드로 같이 해 봐요."),
        RemarkTemplate(ScoreBand.ROUGH, setOf("any"), "많이 움직였지만 부딪히지 않았어요. 그게 오늘의 성과예요."),
        RemarkTemplate(ScoreBand.ROUGH, setOf("any"), "힘들었죠. 그래도 끝까지 했어요."),

        // ── 출발 전 점검 (TaskType.CHECKLIST) — 차는 서 있으니 "들어갔다" 류의 주차 표현을 쓰지 않는다 ──
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("rusty", "first"), "오랜만인데 점검 순서가 손에 남아 있네요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("improved"), "점검 순서가 습관이 되고 있어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "교과서 순서였어요. 지금 이 차에서 제일 안전한 사람은 당신이에요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "막힘이 없었어요. 이제 진짜 출발만 남았어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.GOOD, setOf("rusty", "first"), "장롱의 문고리는 잡았어요. 문 여는 건 다음 과제에서.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "다 켜졌어요. 순서 한 번만 더 몸에 넣으면 끝이에요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "좋아요. 출발 준비가 손에 익어 가요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "순서가 한 번 바뀌었지만 다 켜졌어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "조금 헤맸지만 다 켜졌어요. 이 순서를 세 번만 반복해 봐요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.OK, setOf("rusty"), "오랜만의 출발 준비를 잘 마쳤어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.ROUGH, setOf("any"), "빠진 게 있어요. 시동 꺼진 차 안이 제일 안전한 연습장이에요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.ROUGH, setOf("rusty"), "다시 운전석에 앉은 것부터가 좋은 출발이에요.", TaskType.CHECKLIST),
    )

    // ───────── 장소·보상 (§3.3·§3.5) — 시드, 실제 연계 없음 ─────────

    // ───────── 제휴 시험장 (§3.3 "장소", 결정 D3 = (나), 9/28) — 시드, 실제 연계 없음. 지명·거리는 예시(U3 미결). 문구는 Codex 가 다듬는다 ─────────

    const val COURSE_PARKING = "course-parking-3"
    const val COURSE_ROAD_A = "course-road-a"
    const val COURSE_ROAD_B = "course-road-b"

    /** 코스 3 — 과제 id 는 카탈로그와 일치해야 한다(`SeedCatalogTest`). 제안은 READY 만 고르므로 계획 과제가 섞여도 된다. */
    val courses: List<Course> = listOf(
        Course(COURSE_PARKING, "주차 3종", listOf(TASK_PARKING_REAR, "parking-parallel", "parking-front")),
        Course(COURSE_ROAD_A, "도로 A", listOf("straight-stop", "left-turn-signal")),
        Course(COURSE_ROAD_B, "도로 B", listOf("lane-change", "roundabout")),
    )

    private fun slotsToday(unavailable: Int): List<Slot> = listOf(
        Slot("slot-14", "14:00", "15:00", available = unavailable != 0),
        Slot("slot-16", "16:00", "17:00", available = unavailable != 1),
        Slot("slot-18", "18:00", "19:00", available = unavailable != 2),
    )

    /** 시험장 3. 시간대는 오늘 3(하나는 자리 없음). */
    val venues: List<Venue> = listOf(
        Venue("venue-seocho", "서초 시험장", "서초", 3f, courses, slotsToday(unavailable = 1)),
        Venue("venue-gangnam", "강남 시험장", "강남", 6f, listOf(courses[0], courses[1]), slotsToday(unavailable = 0)),
        Venue("venue-bundang", "분당 시험장", "분당", 14f, listOf(courses[0], courses[2]), slotsToday(unavailable = 2)),
    )

    /** 진단서의 예상 혜택 예시. 실제 전송·계약은 없다(화면에 그렇게 밝힌다). */
    val benefits: List<String> = listOf(
        "보험료 할인 — 운전습관연계보험(UBI) 안전운전점수 연계 예시",
        "적성검사 일부 면제 — 운전 숙련 인증 연계 예시",
        "제휴 시험장 대여료 할인",
    )

    /** 정차 중 3지선다 (§3.2 지식 테스트). 채점보다 `why` 를 듣는 것이 목적. 문항 내용은 Codex 가 다듬는다. */
    fun quizFor(task: Task): List<QuizItem> = when (task.id) {
        TASK_KNOWLEDGE -> quiz
        else -> emptyList()
    }

    val quiz: List<QuizItem> = listOf(
        QuizItem("roundabout-priority", "회전교차로에 들어갈 때 누가 우선인가요?", listOf("들어가는 차", "돌고 있는 차", "먼저 도착한 차"), 1, "회전 중인 차가 우선이에요. 들어가는 차가 양보하고, 빈틈이 생기면 천천히 들어가요."),
        QuizItem("hazard-when", "비상등은 언제 켜나요?", listOf("차선을 바꿀 때", "갑자기 서거나 고장·사고로 서 있을 때", "터널에 들어갈 때"), 1, "뒤차에 위험을 알리는 신호예요. 차선 변경엔 방향지시등, 터널엔 전조등."),
        QuizItem("rain-braking", "비가 올 때 제동 거리는?", listOf("같다", "짧아진다", "길어진다"), 2, "노면 마찰이 줄어 제동 거리가 늘어요. 속도를 20% 줄이고 앞차와 거리를 더 둬요."),
        QuizItem("night-highbeam", "밤에 마주 오는 차가 있을 때 상향등은?", listOf("켠다", "끈다", "깜빡인다"), 1, "상향등은 마주 오는 운전자의 눈을 멀게 해요. 마주 오는 차가 있으면 하향등."),
        QuizItem("following-distance", "앞차와의 안전거리는 보통?", listOf("속도계 숫자만큼 m", "1 m", "차 두 대 길이"), 0, "시속 60이면 60 m 쯤. 비 오면 그 두 배. 초보 때는 넉넉할수록 좋아요."),
    )
}
