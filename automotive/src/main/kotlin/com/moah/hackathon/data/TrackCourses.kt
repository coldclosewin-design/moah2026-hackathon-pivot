package com.moah.hackathon.data

import com.moah.hackathon.scoring.Area
import com.moah.hackathon.scoring.CourseZone
import com.moah.hackathon.scoring.Device
import com.moah.hackathon.scoring.ExamPoints
import com.moah.hackathon.scoring.MapShape
import com.moah.hackathon.scoring.Pose
import com.moah.hackathon.scoring.Side
import com.moah.hackathon.scoring.TrackCourse
import com.moah.hackathon.scoring.TrackMap
import com.moah.hackathon.scoring.Vec2
import com.moah.hackathon.scoring.ZoneKind
import com.moah.hackathon.scoring.ZoneRule

/**
 * 코스 과제 도면 6장(10/4, docs/design/10_full_scope.md) — 제휴 시험장의 장내기능 모의시험 코스 1 + 도로 연습 코스 5.
 * **전부 가정한 예시 도면**이다(실제 시험장 도면 아님). 좌표 m, 원점 왼쪽 아래, +y 위. 경로는 [Turtle] 로 만들고 도면·시나리오가 같이 쓴다.
 * 구간 문장(guide·announce)은 숫자 없음 — 문구는 Codex 가 다듬을 수 있다.
 */
object TrackCourses {

    const val EXAM = "course-exam-track"
    const val STRAIGHT_STOP = "course-straight-stop"
    const val LEFT_TURN = "course-left-turn"
    const val LANE_CHANGE = "course-lane-change"
    const val ROUNDABOUT = "course-roundabout"
    const val ROAD = "course-road"

    private const val ROAD_W = 5f

    /** 지도에서 차를 [MAP_CAR_SCALE] 배로 그리므로 장내 주차 칸도 같은 비율로 키운다(10/5). 화면(CourseMap)이 같은 배율을 쓴다. */
    const val MAP_CAR_SCALE = 1.3f
    private const val BAY_WIDTH_M = 2.6f * MAP_CAR_SCALE
    private const val BAY_LENGTH_M = 5.5f * MAP_CAR_SCALE
    private const val BAY_PITCH_M = 3.0f * MAP_CAR_SCALE
    /** 칸이 길어진 만큼(7.15 − 5.5)의 절반을 더 들어간다 — 칸 중심 = 도로 가장자리 + 칸 길이/2. */
    private const val BAY_EXTRA_DEPTH = (BAY_LENGTH_M - 5.5f) / 2f

    // ───────── 장내기능 모의시험 (제휴 시험장) ─────────
    // 출발(장치 조작) → 북쪽 직선(경사로) → 우회전 → 동쪽 직선(직각 주차 · 신호 교차로) → 우회전 → 남쪽 직선(가속) → 우회전 → 서쪽 직선(돌발) → 좌회전 → 종료

    internal object Exam {
        val start = Pose(Vec2(10f, 4f), 0f)
        /** 출발 → 경사로 정지점(y=32). */
        val toSlope = Turtle(start).straight(28f).path()
        /** 경사로 정지점 → 북쪽 끝(우회전 직전). */
        val slopeToCorner = Turtle(toSlope.end).straight(28f).path()
        val rightTurn1 = Turtle(slopeToCorner.end).arc(6f, -90f).path()
        /** 동쪽 직선 → 주차 칸을 지나 정지(x=47). */
        val toParkingStop = Turtle(rightTurn1.end).straight(31f).path()
        /** 뒤로 칸에 넣기 — 진행 방향은 서쪽에서 북쪽으로, 칸 깊이에 맞춰 마지막에 곧게 조금 더(10/5, 칸 1.3배). */
        val reverseIn = Turtle(Pose(toParkingStop.end.at, 90f)).straight(1.5f).arc(5.25f, -90f).straight(BAY_EXTRA_DEPTH).path()
        /** 칸에서 앞으로 빠져나오기 — 곧게 나온 뒤 남쪽에서 동쪽으로. */
        val pullOut = Turtle(Pose(reverseIn.end.at, 180f)).straight(BAY_EXTRA_DEPTH).arc(5.25f, 90f).path()
        /** 신호 교차로 정지선 앞(x=59). */
        val toSignalStop = Turtle(pullOut.end).straight(59f - pullOut.end.at.x).path()
        val throughSignal = Turtle(toSignalStop.end).straight(86f - 59f).path()
        val rightTurn2 = Turtle(throughSignal.end).arc(6f, -90f).path()
        val accelRoad = Turtle(rightTurn2.end).straight(34f).path()
        val rightTurn3 = Turtle(accelRoad.end).arc(6f, -90f).path()
        /** 서쪽 직선 — 돌발 경보 지점(x=66)까지. */
        val toEmergency = Turtle(rightTurn3.end).straight(20f).path()
        /**
         * 돌발 시 정지 거리(10 km/h 에서 2.6 m/s² 로 약 1.3 초 — 기준 2 초에 여유 0.5 초 이상, 급제동 임계 3.0 아래).
         * 10/5: 12 km/h·2.8 m 는 약 1.95 초라 에뮬 부하에서 "돌발 정지 지연" 이 났다(#152 리뷰).
         */
        val emergencyBrake = Turtle(toEmergency.end).straight(1.5f).path()
        val toLeftTurn = Turtle(emergencyBrake.end).straight(emergencyBrake.end.at.x - 36f).path()
        val leftTurn = Turtle(toLeftTurn.end).arc(6f, 90f).path()
        val toFinish = Turtle(leftTurn.end).straight(9f).path()

        val bayCenter: Vec2 = reverseIn.end.at
        /** 도면의 도로 — 주차 칸 출입을 뺀 고리. */
        val roadLoop = toSlope + slopeToCorner + rightTurn1 + Turtle(rightTurn1.end).straight(70f).path() + rightTurn2 + accelRoad + rightTurn3 +
            toEmergency + emergencyBrake + toLeftTurn + leftTurn + toFinish
        /** 기대 경로 — 주차 칸에 후진으로 넣고 빼는 길까지. */
        val route = toSlope + slopeToCorner + rightTurn1 + toParkingStop + reverseIn + pullOut + toSignalStop + throughSignal + rightTurn2 +
            accelRoad + rightTurn3 + toEmergency + emergencyBrake + toLeftTurn + leftTurn + toFinish
    }

    val exam: TrackCourse by lazy {
        val e = Exam
        // 10/5 사용자 결정: 지도 위 차를 1.3배로 그린다(가독성) → 칸도 1.3배(폭 3.4 · 길이 7.2 · 간격 3.9 m), 도로 가장자리(y=68.5)에서 시작
        val bays = (-2..2).map { k ->
            MapShape.Bay(Vec2(e.bayCenter.x + k * BAY_PITCH_M, e.bayCenter.y), widthM = BAY_WIDTH_M, lengthM = BAY_LENGTH_M, headingDeg = 0f, target = k == 0)
        }
        val map = TrackMap(
            id = "map-exam", title = "제휴 시험장 장내 코스(예시)", widthM = 100f, heightM = 80f,
            shapes = listOf(
                MapShape.Road(listOf(Vec2(10f, 0f)) + e.roadLoop.points.thin(), ROAD_W),
                MapShape.Road(listOf(Vec2(66f, 56f), Vec2(66f, 78f)), ROAD_W),   // 교차로를 지나는 남북 도로(장식)
                MapShape.Ramp(Area(7.5f, 24f, 12.5f, 40f), upHeadingDeg = 0f),
                MapShape.StopLine(Vec2(63f, 63.5f), Vec2(63f, 68.5f)),
                MapShape.Light(Vec2(62f, 61f), facingDeg = 90f),
                MapShape.Crosswalk(Vec2(56f, 17.5f), Vec2(56f, 22.5f), 3f),
                MapShape.Label(Vec2(17f, 5f), "출발"),
                MapShape.Label(Vec2(17f, 32f), "경사로"),
                MapShape.Label(Vec2(40.25f, 78.5f), "직각 주차"),
                MapShape.Label(Vec2(72f, 72f), "신호"),
                MapShape.Label(Vec2(84f, 43f), "가속"),
                MapShape.Label(Vec2(60f, 12f), "돌발"),
                MapShape.Label(Vec2(38f, 5f), "종료"),
            ) + bays,
        )
        TrackCourse(
            id = EXAM, title = "장내기능 모의시험", map = map, start = e.start,
            route = e.route.points.thin(),
            zones = listOf(
                CourseZone("exam-device", "장치 조작", ZoneKind.DEVICE, Area(5f, 0f, 15f, 10f), 0f,
                    listOf(ZoneRule.Devices(listOf(Device.HEADLIGHT, Device.LEFT_INDICATOR, Device.RIGHT_INDICATOR, Device.WIPER, Device.DRIVE_GEAR))),
                    guide = "출발 전에 전조등, 방향지시등 왼쪽과 오른쪽, 와이퍼를 차례로 켰다 끄고 기어를 주행에 놓아요.",
                    announce = "장치 조작을 시작합니다."),
                CourseZone("exam-slope", "경사로", ZoneKind.SLOPE, Area(5f, 24f, 15f, 40f), 0f,
                    listOf(ZoneRule.StopInside(ExamPoints.SLOPE_NO_STOP), ZoneRule.NoRollback(), ZoneRule.DepartWithin()),
                    guide = "경사로예요. 정지 구간 안에서 완전히 멈췄다가 뒤로 밀리지 않게 출발해요."),
                CourseZone("exam-right", "우회전", ZoneKind.TURN_RIGHT, Area(4f, 55f, 24f, 72f), -90f,
                    listOf(ZoneRule.Indicator(Side.RIGHT), ZoneRule.NoLineContact(ExamPoints.TURN_CONTACT)),
                    guide = "오른쪽 방향지시등을 켜고 천천히 돌아요. 선에 닿지 않게 크게 돌아요."),
                CourseZone("exam-parking", "직각 주차", ZoneKind.PARKING, Area(28f, 61f, 54f, 77f), -90f,
                    listOf(ZoneRule.StopInsideBay(Area.around(e.bayCenter, 1.0f, 1.4f)), ZoneRule.NoLineContact(ExamPoints.PARKING_CONTACT),
                        ZoneRule.TimeLimit(ExamPoints.PARKING_LIMIT_SECONDS)),
                    guide = "직각 주차예요. 칸을 지나서 멈추고, 후진으로 칸 안에 넣었다가 앞으로 빠져나와요."),
                CourseZone("exam-signal", "신호 교차로", ZoneKind.SIGNAL, Area(63.5f, 63.5f, 68.5f, 68.5f), -90f,
                    listOf(ZoneRule.NoEntryOnRed()),
                    guide = "신호 교차로예요. 빨간불이면 정지선 앞에서 멈추고, 초록불에 지나가요."),
                CourseZone("exam-accel", "가속", ZoneKind.ACCEL, Area(88f, 32f, 96f, 54f), 180f,
                    listOf(ZoneRule.ReachSpeed()),
                    guide = "가속 구간이에요. 속도를 충분히 올렸다가 끝나면 줄여요."),
                CourseZone("exam-emergency", "돌발", ZoneKind.EMERGENCY, Area(44f, 16f, 76f, 24f), 90f,
                    listOf(ZoneRule.StopOnEmergency(), ZoneRule.HazardOnEmergency()),
                    guide = "돌발 구간이에요. 경보가 울리면 바로 멈추고 비상등을 켜요. 경보가 끝나면 비상등을 끄고 출발해요."),
                CourseZone("exam-left", "좌회전", ZoneKind.TURN_LEFT, Area(25f, 12f, 42f, 25f), 180f,
                    listOf(ZoneRule.Indicator(Side.LEFT), ZoneRule.NoLineContact(ExamPoints.TURN_CONTACT)),
                    guide = "왼쪽 방향지시등을 켜고 돌아요."),
                CourseZone("exam-finish", "종료", ZoneKind.FINISH, Area(25f, 0f, 35f, 11.9f), 180f,
                    listOf(ZoneRule.StopInside()),
                    guide = "종료 구간이에요. 멈추고 주차 기어에 놓아요.", announce = "종료 구간입니다."),
            ),
            passScore = ExamPoints.PASS_SCORE,
        )
    }

    // ───────── 도로 연습 1: 단순 전진 후 정지 ─────────

    internal object Straight {
        val start = Pose(Vec2(15f, 5f), 0f)
        /** 정지선(y=55) 앞 차 중심 y=52.5 에서 정지. */
        val toStop = Turtle(start).straight(47.5f).path()
        /** 못한 주행: 정지선을 넘어 y=57 에서 정지. */
        val overrun = Turtle(start).straight(52f).path()
    }

    val straightStop: TrackCourse by lazy {
        TrackCourse(
            id = STRAIGHT_STOP, title = "단순 전진 후 정지",
            map = TrackMap("map-straight", "직선 도로(예시)", 30f, 80f, listOf(
                MapShape.Road(listOf(Vec2(15f, 0f), Vec2(15f, 78f)), ROAD_W),
                MapShape.StopLine(Vec2(12.5f, 55f), Vec2(17.5f, 55f)),
                MapShape.Label(Vec2(21f, 5f), "출발"),
                MapShape.Label(Vec2(21f, 55f), "정지선"),
            )),
            start = Straight.start, route = Straight.toStop.points.thin(),
            zones = listOf(
                CourseZone("ss-go", "출발", ZoneKind.START, Area(10f, 0f, 20f, 10f), 0f, emptyList(),
                    guide = "기어를 주행에 놓고 브레이크를 천천히 떼서 출발해요."),
                CourseZone("ss-cruise", "직진", ZoneKind.LANE, Area(10f, 10f, 20f, 44f), 0f,
                    listOf(ZoneRule.MaxSpeed(30f)),
                    guide = "속도를 일정하게 유지해요. 너무 빠르지 않게요."),
                CourseZone("ss-stop", "정지선", ZoneKind.STOP, Area(10f, 44f, 20f, 53.5f), 0f,
                    listOf(ZoneRule.StopInside()),
                    guide = "정지선이 보이면 미리 브레이크를 나눠 밟아 선 앞에 멈춰요."),
            ),
        )
    }

    // ───────── 도로 연습 2: 좌회전 방향지시등 ─────────

    internal object Left {
        val start = Pose(Vec2(40f, 5f), 0f)
        val approach = Turtle(start).straight(34f).path()
        val turn = Turtle(approach.end).arc(6f, 90f).path()
        /** 못한 주행: 크게 벌어지는 회전(반지름 9). */
        val wideTurn = Turtle(approach.end).arc(9f, 90f).path()
        val exit = Turtle(turn.end).straight(28f).path()
        val wideExit = Turtle(wideTurn.end).straight(25f).path()
    }

    val leftTurn: TrackCourse by lazy {
        TrackCourse(
            id = LEFT_TURN, title = "좌회전 방향지시등",
            map = TrackMap("map-left", "사거리(예시)", 60f, 70f, listOf(
                MapShape.Road(listOf(Vec2(40f, 0f), Vec2(40f, 68f)), ROAD_W),
                MapShape.Road(listOf(Vec2(0f, 45f), Vec2(58f, 45f)), ROAD_W),
                MapShape.Label(Vec2(46f, 5f), "출발"),
                MapShape.Label(Vec2(8f, 50f), "도착"),
            )),
            start = Left.start, route = (Left.approach + Left.turn + Left.exit).points.thin(),
            zones = listOf(
                CourseZone("lt-approach", "접근", ZoneKind.LANE, Area(35f, 10f, 45f, 37.9f), 0f,
                    listOf(ZoneRule.MaxSpeed(30f)),
                    guide = "교차로가 가까워지면 속도를 줄이고 왼쪽 방향지시등을 미리 켜요."),
                CourseZone("lt-turn", "좌회전", ZoneKind.TURN_LEFT, Area(28f, 38f, 46f, 53f), 0f,
                    listOf(ZoneRule.Indicator(Side.LEFT, leadSeconds = 5f), ZoneRule.NoLineContact(ExamPoints.TURN_CONTACT), ZoneRule.MaxSpeed(20f)),
                    guide = "천천히 돌아요. 돌고 나면 방향지시등이 꺼졌는지 봐요."),
                CourseZone("lt-finish", "도착", ZoneKind.FINISH, Area(0f, 40f, 12f, 50f), 90f,
                    listOf(ZoneRule.StopInside()),
                    guide = "도착이에요. 멈추고 주차 기어에 놓아요."),
            ),
        )
    }

    // ───────── 도로 연습 3: 차선 변경 ─────────

    internal object Lane {
        val start = Pose(Vec2(16.75f, 5f), 0f)
        val before = Turtle(start).straight(35f).path()
        /** 오른쪽 → 왼쪽 차로로 3.5 m(완만한 S자). */
        val change = Turtle(before.end).arc(115f, 10f).arc(115f, -10f).path()
        val after = Turtle(change.end).straight(120f - change.end.at.y).path()
    }

    val laneChange: TrackCourse by lazy {
        TrackCourse(
            id = LANE_CHANGE, title = "차선 변경",
            map = TrackMap("map-lane", "편도 2차로(예시)", 30f, 130f, listOf(
                MapShape.Road(listOf(Vec2(15f, 0f), Vec2(15f, 128f)), 7f, lanes = 2),
                MapShape.Label(Vec2(22f, 5f), "출발"),
                MapShape.Label(Vec2(22f, 118f), "도착"),
            )),
            start = Lane.start, route = (Lane.before + Lane.change + Lane.after).points.thin(),
            zones = listOf(
                CourseZone("lc-change", "차로 변경", ZoneKind.LANE_CHANGE, Area(8f, 38f, 22f, 82f), 0f,
                    listOf(ZoneRule.Indicator(Side.LEFT), ZoneRule.MaxSpeed(40f)),
                    guide = "왼쪽 방향지시등을 먼저 켜고, 거울과 어깨 너머를 본 뒤 천천히 옮겨 가요."),
                CourseZone("lc-finish", "도착", ZoneKind.FINISH, Area(8f, 105f, 22f, 125f), 0f,
                    listOf(ZoneRule.StopInside()),
                    guide = "도착이에요. 멈추고 주차 기어에 놓아요."),
            ),
        )
    }

    // ───────── 도로 연습 4: 회전교차로 ─────────

    internal object Round {
        val start = Pose(Vec2(34f, 4f), 0f)
        val approach = Turtle(start).straight(20f).path()
        val enter = Turtle(approach.end).arc(6f, -90f).path()
        /** 반시계로 반 바퀴(남 → 북). */
        val ring = Turtle(enter.end).arc(10f, 180f).path()
        val exit = Turtle(ring.end).arc(6f, -90f).path()
        val leave = Turtle(exit.end).straight(16f).path()
        val center = Vec2(40f, 40f)
    }

    val roundabout: TrackCourse by lazy {
        TrackCourse(
            id = ROUNDABOUT, title = "회전교차로",
            map = TrackMap("map-round", "회전교차로(예시)", 80f, 80f, listOf(
                MapShape.Road(listOf(Vec2(34f, 0f)) + (Round.approach + Round.enter).points.thin(), ROAD_W),
                MapShape.Ring(Round.center, 10f, 6f),
                MapShape.Road((Round.exit + Round.leave).points.thin() + Vec2(34f, 78f), ROAD_W),
                MapShape.Label(Vec2(40f, 6f), "진입"),
                MapShape.Label(Vec2(40f, 72f), "진출"),
            )),
            start = Round.start, route = (Round.approach + Round.enter + Round.ring + Round.exit + Round.leave).points.thin(),
            zones = listOf(
                CourseZone("rb-approach", "진입 전", ZoneKind.LANE, Area(29f, 8f, 39f, 23.9f), 0f,
                    listOf(ZoneRule.MaxSpeed(30f)),
                    guide = "회전교차로 앞이에요. 속도를 줄이고 안에서 도는 차가 먼저예요."),
                CourseZone("rb-ring", "회전교차로", ZoneKind.ROUNDABOUT, Area(26f, 24f, 56f, 55f), -90f,
                    listOf(ZoneRule.MaxSpeed(20f), ZoneRule.Indicator(Side.RIGHT, leadSeconds = 0f)),
                    guide = "천천히 돌다가, 나가기 전에 오른쪽 방향지시등을 켜요."),
                CourseZone("rb-finish", "도착", ZoneKind.FINISH, Area(29f, 62f, 39f, 78f), 0f,
                    listOf(ZoneRule.StopInside()),
                    guide = "도착이에요. 멈추고 주차 기어에 놓아요."),
            ),
        )
    }

    // ───────── 도로 연습 5: 일반 도로 코스 (보호구역 → 신호 → 우회전 → 정지) ─────────

    internal object Road {
        val start = Pose(Vec2(10f, 5f), 0f)
        /** 신호 정지선(y=57) 앞 차 중심 y=54.5. */
        val toSignal = Turtle(start).straight(49.5f).path()
        val toCorner = Turtle(toSignal.end).straight(90f - 54.5f).path()
        val rightTurn = Turtle(toCorner.end).arc(8f, -90f).path()
        val toFinish = Turtle(rightTurn.end).straight(60f).path()
    }

    val road: TrackCourse by lazy {
        TrackCourse(
            id = ROAD, title = "일반 도로 코스",
            map = TrackMap("map-road", "동네 도로(예시)", 100f, 110f, listOf(
                MapShape.Road(listOf(Vec2(10f, 0f)) + (Road.toSignal + Road.toCorner + Road.rightTurn + Road.toFinish).points.thin(), ROAD_W),
                MapShape.Road(listOf(Vec2(0f, 62f), Vec2(30f, 62f)), ROAD_W),
                MapShape.StopLine(Vec2(7.5f, 57f), Vec2(12.5f, 57f)),
                MapShape.Light(Vec2(14f, 56f), facingDeg = 180f),
                MapShape.Crosswalk(Vec2(7.5f, 30f), Vec2(12.5f, 30f), 3f),
                MapShape.Label(Vec2(16f, 28f), "어린이 보호구역"),
                MapShape.Label(Vec2(18f, 62f), "신호"),
                MapShape.Label(Vec2(80f, 92f), "도착"),
            )),
            start = Road.start, route = (Road.toSignal + Road.toCorner + Road.rightTurn + Road.toFinish).points.thin(),
            zones = listOf(
                CourseZone("rd-school", "어린이 보호구역", ZoneKind.SCHOOL_ZONE, Area(5f, 12f, 15f, 46f), 0f,
                    listOf(ZoneRule.MaxSpeed(30f)),
                    guide = "어린이 보호구역이에요. 천천히, 횡단보도 앞에서는 더 천천히 가요."),
                CourseZone("rd-signal", "신호 교차로", ZoneKind.SIGNAL, Area(5f, 58f, 15f, 66f), 0f,
                    listOf(ZoneRule.NoEntryOnRed()),
                    guide = "신호를 봐요. 빨간불이면 정지선 앞에 멈춰요."),
                CourseZone("rd-right", "우회전", ZoneKind.TURN_RIGHT, Area(4f, 82f, 26f, 104f), 0f,
                    listOf(ZoneRule.Indicator(Side.RIGHT)),
                    guide = "오른쪽 방향지시등을 켜고 천천히 돌아요. 횡단보도 사람을 먼저 봐요."),
                CourseZone("rd-finish", "도착", ZoneKind.FINISH, Area(66f, 92f, 92f, 104f), -90f,
                    listOf(ZoneRule.StopInside()),
                    guide = "도착이에요. 멈추고 주차 기어에 놓아요."),
            ),
        )
    }

    val all: List<TrackCourse> by lazy { listOf(exam, straightStop, leftTurn, laneChange, roundabout, road) }

    fun byId(id: String): TrackCourse? = all.firstOrNull { it.id == id }

    /** 도면·기대 경로용으로 점을 1 m 간격 정도로 솎는다. */
    internal fun List<Vec2>.thin(stepM: Float = 1f): List<Vec2> {
        if (size < 3) return this
        val out = arrayListOf(first())
        for (p in drop(1).dropLast(1)) if (p.distanceTo(out.last()) >= stepM) out += p
        out += last()
        return out
    }
}
