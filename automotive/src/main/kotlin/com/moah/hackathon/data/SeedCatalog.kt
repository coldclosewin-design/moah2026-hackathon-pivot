package com.moah.hackathon.data

import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.feature.lesson.Difficulty
import com.moah.hackathon.feature.lesson.GuideStep
import com.moah.hackathon.scoring.ParkingSpec
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
    const val TASK_PARKING_FRONT = "parking-front"
    const val TASK_PARKING_PARALLEL = "parking-parallel"
    const val TASK_PARKING_ANGLE = "parking-angle"
    const val TASK_KNOWLEDGE = "knowledge-hazard-weather"
    const val TASK_TRACK_EXAM = "track-exam"

    val tasks: List<Task> = listOf(
        Task(TASK_PREDRIVE, "출발 전 점검", TaskType.CHECKLIST, Difficulty.EASY, "차를 세운 채로 문과 벨트, 주차 기어, 브레이크와 시동, 지시등과 비상등을 순서대로 확인해요.",
            listOf("문", "안전벨트", "기어 P", "시동", "지시등"),
            setOf(V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN, V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION, V.VEHICLE_LOWVOLTAGESYSTEMSTATE,
                V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING), requiresDriving = false,
            status = TaskStatus.READY),   // 가이드 7단계(9/28, D1) + ChecklistScorer + 시나리오 2벌. 움직이지 않는 유일한 조작 과제
        // 코스 과제(10/4 전 범위 구현): 도면·구간 규칙은 TrackCourses, 위치·신호등·돌발·검지선은 시뮬레이션 신호(SimOnlySignals.TRACK_KEYS)
        Task("straight-stop", "단순 전진 후 정지", TaskType.DRIVING, Difficulty.EASY, "천천히 출발해 정지선 앞에 부드럽게 멈춰요.",
            listOf("출발", "속도", "정지선"), setOf(V.VEHICLE_SPEED) + SimOnlySignals.TRACK_KEYS, requiresDriving = true,
            status = TaskStatus.READY, course = TrackCourses.straightStop),
        Task("left-turn-signal", "좌회전 방향지시등", TaskType.DRIVING, Difficulty.EASY, "좌회전하기 전에 방향지시등을 미리 켜고, 돌고 나서 꺼요.",
            listOf("방향지시등", "회전 속도", "선"), setOf(V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE) + SimOnlySignals.TRACK_KEYS,
            requiresDriving = true, status = TaskStatus.READY, course = TrackCourses.leftTurn),
        Task("lane-change", "차선 변경", TaskType.DRIVING, Difficulty.MEDIUM, "방향지시등을 켜고 주변을 확인한 뒤 부드럽게 이동해요.",
            listOf("방향지시등", "속도"), setOf(V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE) + SimOnlySignals.TRACK_KEYS,
            requiresDriving = true, status = TaskStatus.READY, course = TrackCourses.laneChange),
        Task("road-course", "일반 도로 코스", TaskType.DRIVING, Difficulty.MEDIUM, "어린이 보호구역, 신호, 우회전을 차례로 지나며 구간마다 할 일을 익혀요.",
            listOf("보호구역 속도", "신호", "방향지시등"), setOf(V.VEHICLE_SPEED) + SimOnlySignals.TRACK_KEYS, requiresDriving = true,
            status = TaskStatus.READY, course = TrackCourses.road),
        Task(TASK_TRACK_EXAM, "장내기능 모의시험", TaskType.DRIVING, Difficulty.HARD, "제휴 시험장 장내 코스를 실제 시험처럼 달려요. 구간마다 감점을 매기고 합격선을 봐요.",
            listOf("장치 조작", "경사로", "직각 주차", "신호", "돌발"), SimOnlySignals.TRACK_KEYS + SimOnlySignals.DEVICE_KEYS, requiresDriving = true,
            status = TaskStatus.READY, course = TrackCourses.exam),
        Task(TASK_PARKING_REAR, "후면 직각 주차", TaskType.PARKING, Difficulty.HARD, "핸들을 돌리고 천천히 후진하며 방향을 맞춰요.",
            listOf("핸들 방향", "기어 전환", "뒤 거리"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM), requiresDriving = true,
            status = TaskStatus.READY, parking = ParkingSpec.REAR_PERPENDICULAR),   // 시연 본편. 채점기·가이드 6단계·시나리오 2벌
        Task(TASK_PARKING_PARALLEL, "평행 주차", TaskType.PARKING, Difficulty.HARD, "길가의 주차 칸에 뒤로 들어가 나란히 서요.",
            listOf("핸들 방향", "기어 전환", "뒤 거리"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM), requiresDriving = true,
            status = TaskStatus.READY, parking = ParkingSpec.PARALLEL),   // 10/4: 우 끝 → 좌 끝 정석, 가이드 7단계·시나리오 2벌(MoreParkingScenarios)
        Task(TASK_PARKING_FRONT, "전면 직각 주차", TaskType.PARKING, Difficulty.MEDIUM, "앞을 살피며 주차 칸에 곧게 들어가요.",
            listOf("핸들 방향", "기어 전환"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR), requiresDriving = true,
            status = TaskStatus.READY, parking = ParkingSpec.FRONT_PERPENDICULAR),   // 10/2 추가: 앞으로 들어가므로 뒤 거리 없음. 가이드 6단계·시나리오 2벌(FrontParkingScenarios)
        Task(TASK_PARKING_ANGLE, "사선 주차", TaskType.PARKING, Difficulty.HARD, "기울어진 주차 칸의 방향에 맞춰 들어가요.",
            listOf("핸들 방향", "기어 전환", "뒤 거리"), setOf(V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM), requiresDriving = true,
            status = TaskStatus.READY, parking = ParkingSpec.ANGLE),   // 10/4: 45° 칸, 가이드 6단계·시나리오 2벌
        Task("roundabout", "회전교차로", TaskType.DRIVING, Difficulty.HARD, "우선순위를 확인하고 들어간 뒤 방향지시등을 켜고 나와요.",
            listOf("속도", "방향지시등"), setOf(V.VEHICLE_SPEED, V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING) + SimOnlySignals.TRACK_KEYS, requiresDriving = true,
            status = TaskStatus.READY, course = TrackCourses.roundabout),
        Task(TASK_KNOWLEDGE, "비상등·날씨별 행동", TaskType.KNOWLEDGE, Difficulty.EASY, "정차 중에 문제를 풀고 이유를 함께 살펴봐요.",
            listOf("비상등", "우천", "야간"), emptySet(), requiresDriving = false,
            status = TaskStatus.READY),   // 지식 테스트 모드로만 시작된다(Task.supports)
    )

    val parkingTask: Task get() = tasks.first { it.id == TASK_PARKING_REAR }
    val frontParkingTask: Task get() = tasks.first { it.id == TASK_PARKING_FRONT }
    val predriveTask: Task get() = tasks.first { it.id == TASK_PREDRIVE }

    /** 시연 패널이 과제별로 보여 주는 Fake 시나리오. 주차는 과제마다(후면·전면), 코스 과제는 코스마다 잘한/못한 2벌(10/4). */
    fun scenariosFor(task: Task): List<Scenario> = when {
        task.course != null -> CourseScenarios.forCourse(task.course.id)
        task.id == TASK_PARKING_FRONT -> FrontParkingScenarios.all
        task.id == TASK_PARKING_PARALLEL -> MoreParkingScenarios.parallel
        task.id == TASK_PARKING_ANGLE -> MoreParkingScenarios.angle
        task.type == TaskType.PARKING -> ParkingScenarios.all
        task.type == TaskType.CHECKLIST -> ChecklistScenarios.all
        else -> emptyList()
    }

    // ───────── 가이드 단계 — 신호 확인형 (§4.3) ─────────

    fun guideFor(task: Task): List<GuideStep> = when (task.id) {
        TASK_PARKING_REAR -> parkingGuide
        TASK_PARKING_FRONT -> frontParkingGuide
        TASK_PARKING_PARALLEL -> parallelParkingGuide
        TASK_PARKING_ANGLE -> angleParkingGuide
        TASK_PREDRIVE -> predriveGuide
        else -> emptyList()
    }

    val parkingGuide: List<GuideStep> = listOf(
        GuideStep("belt", "안전벨트를 매 주세요.", V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED, "확인했어요.") { s, _ -> s.belt == true },
        GuideStep("ignition", "브레이크를 밟고 시동을 켜 주세요.", V.VEHICLE_LOWVOLTAGESYSTEMSTATE, "좋아요.") { s, _ -> s.ignitionOn == true },
        GuideStep("reverse", "기어를 후진에 놓아 주세요.", V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, "좋아요.") { s, _ -> s.gear == Gear.REVERSE },
        GuideStep("steer-right", "핸들을 오른쪽 끝까지 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "다 돌렸어요. 이제 천천히 후진해요.") { s, _ -> (s.steeringDeg ?: 0f) <= -400f },
        GuideStep("center", "차가 비스듬해지면 핸들을 중립으로 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "곧게 후진해요.") { s, moved -> moved && kotlin.math.abs(s.steeringDeg ?: 999f) < 30f },
        GuideStep("park", "다 들어왔으면 멈추고 주차 기어에 놓아 주세요.", V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, "주차를 마쳤으면 버튼을 눌러 주세요.") { s, _ -> s.stopped && s.gear == Gear.PARK },
    )

    /**
     * 전면 직각 주차 6단계(10/2) — 후면과 같은 뼈대, 기어가 주행(D)이고 앞으로 들어간다. 벨트·시동·주차 단계는 후면 것을 그대로 쓴다.
     * **문장은 Codex 가 다듬는다.**
     */
    val frontParkingGuide: List<GuideStep> = listOf(
        parkingGuide.first { it.id == "belt" },
        parkingGuide.first { it.id == "ignition" },
        GuideStep("drive", "기어를 주행에 놓아 주세요.", V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, "좋아요.") { s, _ -> s.gear == Gear.DRIVE },
        GuideStep("steer-right", "핸들을 오른쪽 끝까지 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "다 돌렸어요. 이제 천천히 앞으로 가요.") { s, _ -> (s.steeringDeg ?: 0f) <= -400f },
        GuideStep("center", "차가 칸과 나란해지면 핸들을 중립으로 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "곧게 들어가요.") { s, moved -> moved && kotlin.math.abs(s.steeringDeg ?: 999f) < 30f },
        parkingGuide.first { it.id == "park" },
    )

    /** 평행 주차 7단계(10/4) — 우 끝으로 뒤로 들어가다 비스듬해지면 좌 끝, 나란해지면 중립. **문장은 Codex 가 다듬는다.** */
    val parallelParkingGuide: List<GuideStep> = listOf(
        parkingGuide.first { it.id == "belt" },
        parkingGuide.first { it.id == "ignition" },
        parkingGuide.first { it.id == "reverse" },
        GuideStep("steer-right", "앞차 뒤범퍼와 내 뒷바퀴가 나란해지면 핸들을 오른쪽 끝까지 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "천천히 후진해요.") { s, _ -> (s.steeringDeg ?: 0f) <= -400f },
        GuideStep("steer-left", "차가 비스듬해지면 멈추고 핸들을 왼쪽 끝까지 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "다시 천천히 후진해요.") { s, moved -> moved && (s.steeringDeg ?: 0f) >= 400f },
        GuideStep("center", "길과 나란해지면 멈추고 핸들을 중립으로 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "나란히 섰어요.") { s, moved -> moved && kotlin.math.abs(s.steeringDeg ?: 999f) < 30f },
        parkingGuide.first { it.id == "park" },
    )

    /** 사선 주차 6단계(10/4) — 후면 직각과 같은 뼈대, 칸 방향(45°)에 맞으면 중립. **문장은 Codex 가 다듬는다.** */
    val angleParkingGuide: List<GuideStep> = listOf(
        parkingGuide.first { it.id == "belt" },
        parkingGuide.first { it.id == "ignition" },
        parkingGuide.first { it.id == "reverse" },
        GuideStep("steer-right", "핸들을 오른쪽 끝까지 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "다 돌렸어요. 천천히 후진해요.") { s, _ -> (s.steeringDeg ?: 0f) <= -400f },
        GuideStep("center", "차가 기울어진 칸과 나란해지면 핸들을 중립으로 돌려 주세요.", V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE, "곧게 후진해요.") { s, moved -> moved && kotlin.math.abs(s.steeringDeg ?: 999f) < 30f },
        parkingGuide.first { it.id == "park" },
    )

    /**
     * 출발 전 점검 7단계(9/28, 결정 D1) — 시동 꺼진 차에서 시작한다. 문 → 벨트 → 기어 P 확인 → 브레이크 밟고 시동 → 좌 지시등 → 우 지시등 → 비상등.
     * 확인 신호는 전부 스텁에 있던 것(새 VSS 경로 없음). **문장은 Codex 가 다듬는다.**
     */
    val predriveGuide: List<GuideStep> = listOf(
        GuideStep("door", "운전석 문을 닫아 주세요.", V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN, "닫혔어요.") { s, _ -> !s.doorOpen },
        parkingGuide.first { it.id == "belt" },
        GuideStep("park-check", "기어가 주차에 있는지 확인해 주세요.", V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR, "주차 기어를 확인했어요.") { s, _ -> s.gear == Gear.PARK },
        GuideStep("ignition", "브레이크를 밟고 시동을 켜 주세요.", V.VEHICLE_LOWVOLTAGESYSTEMSTATE, "시동이 켜졌어요.") { s, _ -> s.ignitionOn == true },
        GuideStep("indicator-left", "왼쪽 방향지시등을 켜 봐요.", V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING, "왼쪽을 확인했어요. 이제 오른쪽도 켜 봐요.") { s, _ -> s.indicatorLeft == true },
        GuideStep("indicator-right", "오른쪽 방향지시등을 켜 봐요.", V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING, "오른쪽도 확인했어요.") { s, _ -> s.indicatorRight == true },
        GuideStep("hazard", "비상등을 켜 봐요.", V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING, "비상등을 확인했어요. 이제 끄고 버튼을 눌러 주세요.") { s, _ -> s.hazard == true },
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
     * 운전자에게 보여 주는 서두에는 숫자를 넣지 않는다. "한 번에/한 번 다시"는 측정된 진입 판정 태그가 있을 때만 쓴다.
     */
    val remarks: List<RemarkTemplate> = listOf(
        // 판정 문구는 #92의 필수 태그 필터를 거친다. 판정 없음·최근 문구 회피에는 기존 정성 문구를 쓴다.
        // EXCELLENT — 숙련 과정에 대한 긍정
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("one_go"), "한 번에 들어갔어요."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("rusty", "first"), "주차 과정을 능숙하게 이어 갔어요."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("rusty"), "주차 동작이 전반적으로 매끄러웠어요."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("improved"), "주차 흐름을 잘 이어 갔어요."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "주차 과정이 전반적으로 좋았어요."),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "이번 주차의 좋은 감각을 기억해 봐요."),
        // GOOD
        RemarkTemplate(ScoreBand.GOOD, setOf("one_go"), "한 번에 들어갔어요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("one_fix"), "한 번 다시 넣고 들어갔어요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("rusty", "first"), "주차 과정을 대체로 잘 이어 갔어요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("first"), "주차 동작에서 좋은 감각이 보였어요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("improved"), "주차 흐름에서 좋은 부분이 보였어요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("regressed"), "이번 주차에서 아쉬운 부분을 가볍게 되짚어 봐요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "주차 동작을 조금 더 다듬으면 좋겠어요."),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "잘 이어 간 주차 감각을 기억해 봐요."),
        // OK
        RemarkTemplate(ScoreBand.OK, setOf("one_fix"), "한 번 다시 넣고 들어갔어요."),
        RemarkTemplate(ScoreBand.OK, setOf("many"), "여러 번 오가며 들어갔어요."),
        RemarkTemplate(ScoreBand.OK, setOf("rusty", "first"), "주차 동작을 조금 더 익혀 가면 돼요."),
        RemarkTemplate(ScoreBand.OK, setOf("first"), "이번 주차에는 다듬어 볼 부분이 있었어요."),
        RemarkTemplate(ScoreBand.OK, setOf("improved"), "주차 흐름을 차근차근 익혀 가요."),
        RemarkTemplate(ScoreBand.OK, setOf("regressed"), "아쉬웠던 주차 동작을 천천히 돌아봐요."),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "주차 흐름을 조금 더 다듬어 봐요."),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "주차 과정을 천천히 익혀 가면 돼요."),
        // ROUGH
        RemarkTemplate(ScoreBand.ROUGH, setOf("many"), "여러 번 오가며 들어갔어요."),
        RemarkTemplate(ScoreBand.ROUGH, setOf("rusty", "first"), "주차 동작을 처음부터 천천히 되짚어 봐요."),
        RemarkTemplate(ScoreBand.ROUGH, setOf("first"), "주차 연습은 천천히 익혀도 괜찮아요."),
        RemarkTemplate(ScoreBand.ROUGH, setOf("any"), "어려웠던 주차 과정을 함께 돌아봐요."),
        RemarkTemplate(ScoreBand.ROUGH, setOf("any"), "서두르지 않고 다시 연습해도 돼요."),

        // ── 출발 전 점검 (TaskType.CHECKLIST) — 차는 서 있으니 "들어갔다" 류의 주차 표현을 쓰지 않는다 ──
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("rusty", "first"), "오랜만에 출발 준비를 연습했어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("improved"), "출발 준비를 차근차근 익히고 있어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "점검을 연습한 과정을 함께 돌아봐요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.EXCELLENT, setOf("any"), "출발 전 점검 연습을 끝까지 마쳤어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.GOOD, setOf("rusty", "first"), "다시 운전석에서 점검을 연습했어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "출발 전에 확인할 순서를 익혀 가요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.GOOD, setOf("any"), "출발 준비를 연습하는 시간을 가졌어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "점검 순서를 천천히 되짚어 봐요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.OK, setOf("any"), "이 순서를 다시 연습해 봐요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.OK, setOf("rusty"), "오랜만의 출발 준비 연습을 마쳤어요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.ROUGH, setOf("any"), "출발 준비도 서두르지 않고 익히면 돼요.", TaskType.CHECKLIST),
        RemarkTemplate(ScoreBand.ROUGH, setOf("rusty"), "다시 시작한 점검 연습을 함께 돌아봐요.", TaskType.CHECKLIST),
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
        QuizItem("hazard-when", "비상등은 언제 켜나요?", listOf("차선을 바꿀 때", "갑자기 서거나 고장·사고로 서 있을 때", "터널에 들어갈 때"), 1, "뒤차에 위험을 알리는 신호예요. 차선을 바꿀 때는 방향지시등을, 터널에 들어갈 때는 전조등을 켜요."),
        QuizItem("rain-braking", "비가 올 때 제동 거리는?", listOf("같다", "짧아진다", "길어진다"), 2, "노면 마찰이 줄어 제동 거리가 늘어요. 속도를 20% 줄이고 앞차와 거리를 더 둬요."),
        QuizItem("night-highbeam", "밤에 마주 오는 차가 있을 때 상향등은?", listOf("켠다", "끈다", "깜빡인다"), 1, "상향등은 마주 오는 운전자의 시야를 방해할 수 있어요. 마주 오는 차가 있으면 하향등으로 바꿔요."),
        QuizItem("following-distance", "앞차와의 안전거리는 보통?", listOf("속도계 숫자만큼 m", "1 m", "차 두 대 길이"), 0, "시속 60이면 60 m 정도 거리를 둬요. 비가 오면 그 두 배로 늘려요. 초보 때는 넉넉할수록 좋아요."),
        QuizItem("parking-shift-stop", "주차 중 전진에서 후진으로 바꾸기 전에는?", listOf("차가 움직여도 바로 바꿔요", "차를 완전히 멈춘 뒤 바꿔요", "가속 페달을 밟으며 바꿔요"), 1, "차를 완전히 멈추고 브레이크를 밟은 상태에서 기어를 바꿔요. 움직이는 중에 후진으로 바꾸면 사고나 변속기 손상이 생길 수 있어요."),
        QuizItem("parking-brake", "자동변속기 차량을 주차한 뒤에는?", listOf("주차 기어만 넣고 내려요", "중립 기어에 두고 내려요", "주차 기어와 주차 브레이크를 함께 사용해요"), 2, "차를 완전히 멈춘 뒤 주차 기어를 넣고 주차 브레이크도 작동해요. 주차 기어만으로 주차 브레이크를 대신하지 않아요."),
        QuizItem("blocked-green", "초록불이지만 교차로 건너편이 막혀 있다면?", listOf("교차로 안에 멈추지 않도록 진입 전에 기다려요", "초록불이니 교차로 안까지 들어가요", "경적을 울리며 들어가요"), 0, "교차로 안에 멈춰 다른 차의 통행을 막을 상황이면 진입하지 않아요. 건너편에 빠져나갈 공간이 생길 때까지 기다려요."),
        QuizItem("crosswalk-yield", "신호 없는 횡단보도에서 보행자가 건너려 한다면?", listOf("속도만 줄여서 지나가요", "횡단보도 앞에 멈춰 보행자에게 양보해요", "경적을 울려 보행자를 멈추게 해요"), 1, "보행자가 건너고 있거나 건너려 할 때는 횡단보도 앞에 일시정지해요. 정지선이 있으면 그 앞에 멈춰 보행자의 통행을 보호해요."),
        QuizItem("highway-entry-priority", "일반 차량이 고속도로에 진입할 때는?", listOf("진입하는 차가 먼저 들어가요", "속도가 빠른 차가 우선이에요", "본선에서 달리는 차의 통행을 방해하지 않아요"), 2, "진입하는 차는 이미 고속도로를 달리는 차의 통행을 방해하지 않도록 양보해요. 안전한 간격을 확인하고 합류해요."),
    )
}
