package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.SimOnlySignals
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 직선 도로(+y) 위에 구간을 차례로 놓은 시험용 코스로 규칙 하나하나를 확인한다. */
class CourseRecorderTest {

    private val bay = Area(-1.5f, 182f, 1.5f, 188f)
    private val course = TrackCourse(
        id = "t", title = "시험 코스",
        map = TrackMap("m", "시험", 20f, 220f, emptyList()),
        start = Pose(Vec2(0f, 2f), 0f), route = emptyList(),
        zones = listOf(
            CourseZone("dev", "장치 조작", ZoneKind.DEVICE, Area(-3f, 0f, 3f, 10f), 0f,
                listOf(ZoneRule.Devices(listOf(Device.HEADLIGHT, Device.WIPER))), "장치를 조작해요"),
            CourseZone("slope", "경사로", ZoneKind.SLOPE, Area(-3f, 20f, 3f, 40f), 0f,
                listOf(ZoneRule.StopInside(ExamPoints.SLOPE_NO_STOP), ZoneRule.NoRollback(), ZoneRule.DepartWithin()), "경사로"),
            CourseZone("left", "좌회전", ZoneKind.TURN_LEFT, Area(-3f, 50f, 3f, 60f), 0f,
                listOf(ZoneRule.Indicator(Side.LEFT), ZoneRule.NoLineContact(ExamPoints.TURN_CONTACT)), "좌회전"),
            CourseZone("signal", "신호 교차로", ZoneKind.SIGNAL, Area(-3f, 70f, 3f, 80f), 0f,
                listOf(ZoneRule.NoEntryOnRed()), "신호"),
            CourseZone("emg", "돌발", ZoneKind.EMERGENCY, Area(-3f, 90f, 3f, 120f), 0f,
                listOf(ZoneRule.StopOnEmergency(), ZoneRule.HazardOnEmergency()), "돌발"),
            CourseZone("accel", "가속", ZoneKind.ACCEL, Area(-3f, 130f, 3f, 160f), 0f,
                listOf(ZoneRule.ReachSpeed()), "가속"),
            CourseZone("park", "직각 주차", ZoneKind.PARKING, Area(-3f, 170f, 3f, 190f), 0f,
                listOf(ZoneRule.StopInsideBay(bay), ZoneRule.NoLineContact(ExamPoints.PARKING_CONTACT)), "주차"),
            CourseZone("finish", "종료", ZoneKind.FINISH, Area(-3f, 200f, 3f, 215f), 0f,
                listOf(ZoneRule.StopInside()), "종료"),
        ),
        passScore = ExamPoints.PASS_SCORE,
    )

    /** 시각 t(초)에 y 위치·속도와 덧붙일 신호를 넣는다. */
    private class Drive(val r: CourseRecorder) {
        val all = ArrayList<Deduction>()
        val entered = ArrayList<String>()
        fun at(t: Double, y: Float?, kmh: Float, vararg extra: Pair<String, String>) {
            val m = HashMap<String, String>()
            if (y != null) { m[SimOnlySignals.TRACK_POSITION_X_M] = "0.0"; m[SimOnlySignals.TRACK_POSITION_Y_M] = y.toString() }
            m[VssConstants.VEHICLE_SPEED] = kmh.toString()
            m.putAll(extra)
            val u = r.onDelta((t * 1000).toLong(), m)
            all += u.deductions
            u.entered?.let { entered += it.id }
        }
    }

    private val baseSignals = arrayOf(
        VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to "false",
        VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING to "false",
        VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING to "false",
        SimOnlySignals.LIGHTS_BEAM_LOW_ISON to "false",
        SimOnlySignals.WIPER_FRONT_MODE to "OFF",
        SimOnlySignals.TRACK_SIGNAL_STATE to "GREEN",
        SimOnlySignals.TRACK_EVENT_EMERGENCY to "false",
        SimOnlySignals.TRACK_LINE_CONTACT to "false",
    )

    /** 모범 주행. [skip] 에 든 동작은 빼먹는다. */
    private fun run(skip: Set<String> = emptySet(), red: Boolean = false, lineIn: Boolean = false, lineOut: Boolean = false): Pair<CourseResult, Drive> {
        val r = CourseRecorder(course)
        val d = Drive(r)
        d.at(0.0, 2f, 0f, *baseSignals)
        if ("headlight" !in skip) { d.at(1.0, 2f, 0f, SimOnlySignals.LIGHTS_BEAM_LOW_ISON to "true"); d.at(2.0, 2f, 0f, SimOnlySignals.LIGHTS_BEAM_LOW_ISON to "false") }
        if ("wiper" !in skip) { d.at(3.0, 2f, 0f, SimOnlySignals.WIPER_FRONT_MODE to "SLOW"); d.at(4.0, 2f, 0f, SimOnlySignals.WIPER_FRONT_MODE to "OFF") }
        d.at(5.0, 12f, 10f)
        if (lineOut) { d.at(5.5, 15f, 10f, SimOnlySignals.TRACK_LINE_CONTACT to "true"); d.at(6.0, 16f, 10f, SimOnlySignals.TRACK_LINE_CONTACT to "false") }
        d.at(6.5, 22f, 8f)
        if ("slopeStop" !in skip) {
            d.at(8.0, 30f, 0f)
            if ("rollback" in skip) d.at(9.0, 28.5f, 0f)
            d.at(if ("lateDepart" in skip) 45.0 else 12.0, 30f, 0f)
            d.at(if ("lateDepart" in skip) 46.0 else 13.0, 35f, 8f)
        } else d.at(10.0, 30f, 8f)
        d.at(15.0, 45f, 10f)
        if ("indicator" !in skip) d.at(16.0, 48f, 10f, VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to "true")
        d.at(17.0, 55f, 10f)
        if (lineIn) { d.at(17.2, 56f, 10f, SimOnlySignals.TRACK_LINE_CONTACT to "true"); d.at(17.4, 57f, 10f, SimOnlySignals.TRACK_LINE_CONTACT to "false") }
        d.at(18.0, 65f, 10f, VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING to "false")
        if (red) d.at(19.0, 69f, 10f, SimOnlySignals.TRACK_SIGNAL_STATE to "RED")
        d.at(20.0, 75f, 10f)
        d.at(21.0, 85f, 10f, SimOnlySignals.TRACK_SIGNAL_STATE to "GREEN")
        d.at(22.0, 95f, 15f)
        d.at(23.0, 100f, 15f, SimOnlySignals.TRACK_EVENT_EMERGENCY to "true")
        if ("emergencyStop" in skip) d.at(26.0, 110f, 10f) else d.at(24.0, 103f, 0f)
        if ("hazard" !in skip) d.at(25.0, if ("emergencyStop" in skip) 112f else 103f, 0f, VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING to "true")
        d.at(28.0, 113f, 0f, SimOnlySignals.TRACK_EVENT_EMERGENCY to "false", VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING to "false")
        d.at(30.0, 125f, 12f)
        d.at(32.0, 140f, if ("accel" in skip) 18f else 24f)
        d.at(34.0, 165f, 10f)
        d.at(36.0, 175f, 5f)
        d.at(40.0, if ("bay" in skip) 178f else 185f, 0f)
        d.at(44.0, 195f, 8f)
        d.at(48.0, 205f, 0f)
        return r.result() to d
    }

    @Test fun `clean run passes with full score and visits every zone in order`() {
        val (res, d) = run()
        assertEquals(emptyList<Deduction>(), res.deductions)
        assertEquals(100, res.score)
        assertEquals(true, res.passed)
        assertEquals(course.zones.map { it.id }, d.entered)
        assertTrue(res.zones.all { it.visited && it.unmeasured.isEmpty() })
        assertTrue(res.trail.size > 10)
    }

    @Test fun `slope rules - no stop, rollback, late departure`() {
        assertEquals(listOf("경사로 미정지"), run(setOf("slopeStop")).first.deductions.map { it.reason })
        assertEquals(listOf("뒤로 밀림"), run(setOf("rollback")).first.deductions.map { it.reason })
        assertEquals(listOf("출발 지연"), run(setOf("lateDepart")).first.deductions.map { it.reason })
    }

    @Test fun `devices missed are deducted per item`() {
        val res = run(setOf("headlight", "wiper")).first
        assertEquals(listOf("전조등 미조작", "와이퍼 미조작"), res.deductions.map { it.reason })
        assertEquals(100 - 2 * ExamPoints.DEVICE_EACH, res.score)
    }

    @Test fun `missing indicator, accel, bay and emergency lapses`() {
        val res = run(setOf("indicator", "accel", "bay", "emergencyStop", "hazard")).first
        assertEquals(setOf("방향지시등 미점등", "가속 부족", "주차 칸 밖 정지", "돌발 정지 지연", "비상등 미점등"), res.deductions.map { it.reason }.toSet())
        assertEquals(100 - 5 - 10 - 10 - 10 - 10, res.score)
        assertEquals(false, res.passed)
    }

    @Test fun `red light entry disqualifies even with a high score`() {
        val res = run(red = true).first
        assertTrue(res.disqualified)
        assertEquals(100, res.score)
        assertEquals(false, res.passed)
        assertEquals("signal", res.deductions.single().zoneId)
    }

    @Test fun `line contact uses zone points inside and lane points outside`() {
        assertEquals(listOf(ExamPoints.TURN_CONTACT), run(lineIn = true).first.deductions.map { it.points })
        val out = run(lineOut = true).first.deductions.single()
        assertEquals(ExamPoints.LANE_CONTACT, out.points)
        assertNull(out.zoneId)
    }

    @Test fun `deduction sentences carry no digits`() {
        val res = run(setOf("headlight", "slopeStop", "indicator", "accel", "bay", "emergencyStop", "hazard"), red = true, lineIn = true, lineOut = true).first
        assertTrue(res.deductions.size >= 8)
        res.deductions.forEach { assertFalse(it.say, it.say.any(Char::isDigit)); assertFalse(it.reason.any(Char::isDigit)) }
    }

    @Test fun `missing signals are not deducted but reported unmeasured`() {
        val r = CourseRecorder(course)
        // 지시등·장치·시험장 이벤트 신호가 하나도 오지 않는다 — 위치와 속도만
        listOf(2f to 0f, 12f to 10f, 30f to 0f, 35f to 8f, 55f to 10f, 75f to 10f, 100f to 15f, 140f to 25f, 185f to 0f, 205f to 0f)
            .forEachIndexed { i, (y, v) -> r.onDelta(i * 2000L, mapOf(SimOnlySignals.TRACK_POSITION_X_M to "0", SimOnlySignals.TRACK_POSITION_Y_M to y.toString(), VssConstants.VEHICLE_SPEED to v.toString())) }
        val res = r.result()
        assertEquals(emptyList<String>(), res.deductions.map { it.reason })
        assertTrue("왼쪽 방향지시등" in res.zones.first { it.zoneId == "left" }.unmeasured)
        assertTrue("장치 조작" in res.zones.first { it.zoneId == "dev" }.unmeasured)
        assertTrue("신호 지키기" in res.zones.first { it.zoneId == "signal" }.unmeasured)
    }

    @Test fun `without position nothing is judged and pass is unknown`() {
        val r = CourseRecorder(course)
        r.onDelta(0, mapOf(VssConstants.VEHICLE_SPEED to "10"))
        val res = r.result()
        assertFalse(res.positionMeasured)
        assertNull(res.passed)
        assertTrue(res.zones.none { it.visited })
        assertFalse(r.progress().positionMeasured)
    }

    @Test fun `progress tracks current and next zone without any score`() {
        val r = CourseRecorder(course)
        r.onDelta(0, mapOf(SimOnlySignals.TRACK_POSITION_X_M to "0", SimOnlySignals.TRACK_POSITION_Y_M to "2", VssConstants.VEHICLE_SPEED to "0"))
        assertEquals("dev", r.progress().currentZoneId)
        assertEquals("slope", r.progress().nextZoneId)
        r.onDelta(1000, mapOf(SimOnlySignals.TRACK_POSITION_Y_M to "15", VssConstants.VEHICLE_SPEED to "10"))
        val p = r.progress()
        assertNull(p.currentZoneId)
        assertEquals("slope", p.nextZoneId)
        assertEquals(listOf("dev"), p.passedZoneIds)
        // 진행 화면 타입에는 점수·감점 필드가 없다(절대 규칙 10)
        assertTrue(CourseProgress::class.java.declaredFields.none { it.name.contains("score", true) || it.name.contains("deduction", true) })
    }

    @Test fun `zones only move forward so a later pass through an earlier area is ignored`() {
        val r = CourseRecorder(course)
        val d = Drive(r)
        d.at(0.0, 2f, 0f)
        d.at(1.0, 30f, 0f)
        d.at(2.0, 55f, 5f)
        d.at(3.0, 30f, 5f)   // 경사로 자리로 되돌아와도 재진입 아님
        assertEquals(listOf("dev", "slope", "left"), d.entered)
    }

    @Test fun `heading helpers are inverse and follow the path convention`() {
        assertEquals(0f, Vec2.ofHeading(0f).x, 1e-5f)
        assertEquals(-1f, Vec2.ofHeading(90f).x, 1e-5f)   // 왼쪽(반시계) 90° = -x
        assertEquals(135f, Vec2.headingOf(Vec2.ofHeading(135f)), 1e-3f)
    }
}
