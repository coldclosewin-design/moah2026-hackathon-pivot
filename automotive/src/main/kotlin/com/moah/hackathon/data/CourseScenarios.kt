package com.moah.hackathon.data

import mobis.vss.VssConstants
import com.moah.hackathon.scoring.Pose
import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.vehicle.TrackSignal
import com.moah.hackathon.vehicle.VssValues
import mobis.vss.VssConstants as V

/**
 * 코스 과제 시연용 Fake 시나리오 — 코스마다 잘한/못한 주행 2벌(10/4). [TrackCourses] 와 같은 경로([Turtle])를 달린다.
 * 위치·신호등·돌발·검지선은 시뮬레이션 신호(`Track.*`), 나머지는 차량 신호. 시각·속도는 전부 가정이다.
 * 기대 결과(감점)는 `CourseScenariosTest` 가 고정한다 — 바꾸면 그 테스트와 시연 대본을 같이 고친다.
 */
object CourseScenarios {

    private val T = VssValues.TRUE
    private val F = VssValues.FALSE
    private const val LEFT = V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING
    private const val RIGHT = V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING
    private const val HAZARD = V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING
    private const val GEAR = V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR
    private const val SIGNAL = SimOnlySignals.TRACK_SIGNAL_STATE
    private const val EMERGENCY = SimOnlySignals.TRACK_EVENT_EMERGENCY
    private const val CONTACT = SimOnlySignals.TRACK_LINE_CONTACT

    /** 시동 켜짐·벨트·P·등화 꺼짐·신호 없음에서 시작한다(코스 과제는 출발 전 점검을 마친 차). */
    private fun script(id: String, title: String, start: Pose): DriveScript = DriveScript(id, title, start).set(
        V.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON",
        V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to T,
        V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to F,
        GEAR to Gear.PARK.vss,
        V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "0.0",
        LEFT to F, RIGHT to F, HAZARD to F,
        VssConstants.VEHICLE_BODY_LIGHTS_BEAM_LOW_ISON to F,
        VssConstants.VEHICLE_BODY_WINDSHIELD_FRONT_WIPING_MODE to "OFF",
        SIGNAL to TrackSignal.OFF.name, EMERGENCY to F, CONTACT to F,
    ).hold(1.0)

    private fun DriveScript.go(): DriveScript = set(GEAR to Gear.DRIVE.vss).hold(1.0)
    private fun DriveScript.park(): DriveScript = hold(1.0).set(GEAR to Gear.PARK.vss).hold(2.0)
    private fun on(key: String) = arrayOf(key to T)
    private fun off(key: String) = arrayOf(key to F)

    // ───────── 장내기능 모의시험 ─────────

    private fun exam(id: String, title: String, good: Boolean, sCurve: Boolean = false): Scenario {
        val e = TrackCourses.Exam
        val s = script(id, title, e.start)
        // 장치 조작 — 전조등 · 왼쪽/오른쪽 지시등 · 와이퍼 · 기어 D
        s.set(VssConstants.VEHICLE_BODY_LIGHTS_BEAM_LOW_ISON to T).hold(1.5).set(VssConstants.VEHICLE_BODY_LIGHTS_BEAM_LOW_ISON to F).hold(1.0)
        s.set(LEFT to T).hold(1.5).set(LEFT to F).hold(1.0)
        s.set(RIGHT to T).hold(1.5).set(RIGHT to F).hold(1.0)
        s.set(VssConstants.VEHICLE_BODY_WINDSHIELD_FRONT_WIPING_MODE to "SLOW").hold(1.5).set(VssConstants.VEHICLE_BODY_WINDSHIELD_FRONT_WIPING_MODE to "OFF").hold(1.0)
        s.go()
        // 경사로 — 정지, (못한: 뒤로 밀림), 출발
        s.drive(e.toSlope, 15.0)
        s.hold(2.0)
        if (!good) s.drive(Turtle(Pose(e.toSlope.end.at, 180f)).straight(1.5f).path(), 3.0, reverse = true).hold(1.0)
        if (sCurve) {
            // 연습: 경사로 뒤 S자 곁가지를 천천히 돌아 원래 모서리로(라운드 25 결정 4) — 끝 무렵 오른쪽 지시등(다음 우회전)
            s.drive(e.slopeToBranch, 10.0, endKmh = 8.0)
            s.drive(e.sBranch, 8.0, endKmh = 8.0, events = listOf(e.sBranch.length - 8f to on(RIGHT)))
        } else {
            val fromSlope = if (good) e.slopeToCorner else Turtle(Pose(e.toSlope.end.at.copy(y = e.toSlope.end.at.y - 1.5f), 0f)).straight(29.5f).path()
            s.drive(fromSlope, 15.0, endKmh = 10.0, events = listOf(fromSlope.length - 12f to on(RIGHT)))
        }
        s.drive(e.rightTurn1, 10.0, endKmh = 10.0)
        // 직각 주차 — 칸을 지나 정지 → 후진으로 칸 안 → 앞으로 빠져나옴
        s.drive(e.toParkingStop, 15.0, events = listOf(2f to off(RIGHT)))
        s.set(GEAR to Gear.REVERSE.vss).hold(1.5)
        s.drive(e.reverseIn, 5.0, reverse = true, events = if (good) emptyList() else listOf(4f to on(CONTACT), 5f to off(CONTACT)))
        s.hold(2.0).set(GEAR to Gear.DRIVE.vss).hold(1.0)
        s.drive(e.pullOut, 6.0, endKmh = 6.0)
        // 신호 교차로 — 빨간불에 정지선 앞 정지, 초록불에 출발
        s.drive(e.toSignalStop, 15.0, events = listOf(0f to arrayOf(SIGNAL to TrackSignal.RED.name)))
        s.hold(3.0).set(SIGNAL to TrackSignal.GREEN.name).hold(1.0)
        s.drive(e.throughSignal, 15.0, endKmh = 10.0, events = listOf(12f to arrayOf(SIGNAL to TrackSignal.OFF.name)))
        s.drive(e.rightTurn2, 10.0, endKmh = 10.0)
        // 가속 구간
        s.drive(e.accelRoad, 25.0, endKmh = 10.0)
        s.drive(e.rightTurn3, 10.0, endKmh = 10.0)
        // 돌발 — 경보 → 바로 정지 → 비상등(못한: 안 켬) → 경보 끝 → 출발
        s.drive(e.toEmergency, 10.0, endKmh = 10.0)
        s.set(EMERGENCY to T)
        s.drive(e.emergencyBrake, 10.0, decel = 2.6)
        s.hold(0.5)
        if (good) s.set(HAZARD to T)
        s.hold(3.0).set(EMERGENCY to F, HAZARD to F).hold(1.0)
        // 좌회전 → 종료
        s.drive(e.toLeftTurn, 15.0, endKmh = 10.0, events = listOf(12f to on(LEFT)))
        s.drive(e.leftTurn, 10.0, endKmh = 8.0)
        s.drive(e.toFinish, 10.0, events = listOf(2f to off(LEFT)))
        return s.park().build()
    }

    /** 장내기능 모범 주행 — 감점 0, 합격. */
    val examGood: Scenario by lazy { exam("exam-good", "잘한 시험", good = true) }
    /** 경사로 밀림 · 주차 검지선 접촉 · 돌발 비상등 미점등 — 감점 셋, 불합격. */
    val examBad: Scenario by lazy { exam("exam-bad", "못한 시험", good = false) }
    /** 코스 연습(가이드·힌트) — 모범 주행 + S자 연습 곁가지. 평가 모드에선 S자 구간이 없어 채점이 같다. */
    val examPracticeS: Scenario by lazy { exam("exam-practice-s", "S자 연습", good = true, sCurve = true) }

    // ───────── 도로 연습 ─────────

    val straightGood: Scenario by lazy {
        val c = TrackCourses.Straight
        script("straight-good", "잘한 주행", c.start).go().drive(c.toStop, 25.0).park().build()
    }
    /** 과속 · 정지선 넘어 급정지. */
    val straightBad: Scenario by lazy {
        val c = TrackCourses.Straight
        script("straight-bad", "못한 주행", c.start).go().drive(c.overrun, 38.0, accel = 2.0, decel = 4.5).park().build()
    }

    val leftGood: Scenario by lazy {
        val c = TrackCourses.Left
        script("left-good", "잘한 주행", c.start).go()
            .drive(c.approach, 25.0, endKmh = 12.0, events = listOf(12f to on(LEFT)))
            .drive(c.turn, 12.0, endKmh = 12.0)
            .drive(c.exit, 18.0, events = listOf(3f to off(LEFT)))
            .park().build()
    }
    /** 지시등 없이 · 크게 벌어져 선 접촉. */
    val leftBad: Scenario by lazy {
        val c = TrackCourses.Left
        script("left-bad", "못한 주행", c.start).go()
            .drive(c.approach, 25.0, endKmh = 15.0)
            .drive(c.wideTurn, 15.0, endKmh = 15.0, events = listOf(8f to on(CONTACT), 9.5f to off(CONTACT)))
            .drive(c.wideExit, 18.0)
            .park().build()
    }

    val laneGood: Scenario by lazy {
        val c = TrackCourses.Lane
        script("lane-good", "잘한 주행", c.start).go()
            .drive(c.before, 35.0, endKmh = 35.0, events = listOf(20f to on(LEFT)))
            .drive(c.change, 35.0, endKmh = 35.0)
            .drive(c.after, 35.0, events = listOf(2f to off(LEFT)))
            .park().build()
    }
    /** 지시등 없이 · 빠르게. */
    val laneBad: Scenario by lazy {
        val c = TrackCourses.Lane
        script("lane-bad", "못한 주행", c.start).go()
            .drive(c.before, 48.0, endKmh = 48.0, accel = 2.5)
            .drive(c.change, 48.0, endKmh = 48.0)
            .drive(c.after, 48.0, decel = 2.0)
            .park().build()
    }

    val roundGood: Scenario by lazy {
        val c = TrackCourses.Round
        script("round-good", "잘한 주행", c.start).go()
            .drive(c.approach, 20.0, endKmh = 12.0)
            .drive(c.enter, 12.0, endKmh = 12.0)
            .drive(c.ring, 15.0, endKmh = 12.0, events = listOf(c.ring.length - 8f to on(RIGHT)))
            .drive(c.exit, 12.0, endKmh = 12.0)
            .drive(c.leave, 20.0, events = listOf(2f to off(RIGHT)))
            .park().build()
    }
    /** 빠르게 돌고 · 나갈 때 지시등 없음. */
    val roundBad: Scenario by lazy {
        val c = TrackCourses.Round
        script("round-bad", "못한 주행", c.start).go()
            .drive(c.approach, 28.0, endKmh = 25.0, accel = 2.0)
            .drive(c.enter, 25.0, endKmh = 25.0)
            .drive(c.ring, 25.0, endKmh = 22.0)
            .drive(c.exit, 22.0, endKmh = 20.0)
            .drive(c.leave, 20.0)
            .park().build()
    }

    val roadGood: Scenario by lazy {
        val c = TrackCourses.Road
        script("road-good", "잘한 주행", c.start).go()
            .drive(c.toSignal, 25.0, events = listOf(30f to arrayOf(SIGNAL to TrackSignal.RED.name)))
            .hold(3.0).set(SIGNAL to TrackSignal.GREEN.name).hold(1.0)
            .drive(c.toCorner, 30.0, endKmh = 12.0, events = listOf(15f to arrayOf(SIGNAL to TrackSignal.OFF.name), c.toCorner.length - 12f to on(RIGHT)))
            .drive(c.rightTurn, 12.0, endKmh = 12.0)
            .drive(c.toFinish, 30.0, events = listOf(3f to off(RIGHT)))
            .park().build()
    }
    /** 보호구역 과속 · 우회전 지시등 없음. 신호는 지킨다. */
    val roadBad: Scenario by lazy {
        val c = TrackCourses.Road
        script("road-bad", "못한 주행", c.start).go()
            .drive(c.toSignal, 40.0, accel = 2.0, decel = 2.0, events = listOf(30f to arrayOf(SIGNAL to TrackSignal.RED.name)))
            .hold(3.0).set(SIGNAL to TrackSignal.GREEN.name).hold(1.0)
            .drive(c.toCorner, 35.0, endKmh = 15.0, events = listOf(15f to arrayOf(SIGNAL to TrackSignal.OFF.name)))
            .drive(c.rightTurn, 15.0, endKmh = 15.0)
            .drive(c.toFinish, 30.0)
            .park().build()
    }

    /** 코스 id → 시연 패널 시나리오(잘한 것 먼저). */
    fun forCourse(courseId: String): List<Scenario> = when (courseId) {
        TrackCourses.EXAM -> listOf(examGood, examBad, examPracticeS)
        TrackCourses.STRAIGHT_STOP -> listOf(straightGood, straightBad)
        TrackCourses.LEFT_TURN -> listOf(leftGood, leftBad)
        TrackCourses.LANE_CHANGE -> listOf(laneGood, laneBad)
        TrackCourses.ROUNDABOUT -> listOf(roundGood, roundBad)
        TrackCourses.ROAD -> listOf(roadGood, roadBad)
        else -> emptyList()
    }
}
