package com.moah.hackathon.scoring

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * 코스 과제(도로 주행 5종 · 장내기능 모의시험)의 도면과 규칙 — docs/design/10_full_scope.md §1 (2026-10-04).
 *
 * 좌표는 **트랙 좌표계 미터**, +x 오른쪽 · +y 도면 위. 방향(°)은 +y 에서 반시계 양수([PathPoint] 와 같은 규약).
 * 위치는 시뮬레이션 신호 `Track.Position.*`(실제로는 제휴 시험장 RTK·검지 센서)로 들어온다. 안드로이드 의존 없음.
 */

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(k: Float) = Vec2(x * k, y * k)
    fun dot(o: Vec2): Float = x * o.x + y * o.y
    fun length(): Float = hypot(x.toDouble(), y.toDouble()).toFloat()
    fun distanceTo(o: Vec2): Float = (this - o).length()

    companion object {
        /** 방향(°, +y 기준 반시계) → 단위 벡터. 0° = (0, 1), 90° = (-1, 0). */
        fun ofHeading(deg: Float): Vec2 {
            val r = Math.toRadians(deg.toDouble())
            return Vec2((-sin(r)).toFloat(), cos(r).toFloat())
        }

        /** 단위 벡터 → 방향(°). [ofHeading] 의 역. */
        fun headingOf(v: Vec2): Float = Math.toDegrees(atan2(-v.x.toDouble(), v.y.toDouble())).toFloat()
    }
}

/** 축 정렬 사각 영역(m). 구간·주차 칸 판정에 쓴다 — 시험장 코스는 직각 격자라 이것으로 충분하다. */
data class Area(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    init { require(maxX > minX && maxY > minY) { "empty area" } }
    operator fun contains(p: Vec2): Boolean = p.x in minX..maxX && p.y in minY..maxY
    val center: Vec2 get() = Vec2((minX + maxX) / 2f, (minY + maxY) / 2f)
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY

    companion object {
        fun around(c: Vec2, halfW: Float, halfH: Float) = Area(c.x - halfW, c.y - halfH, c.x + halfW, c.y + halfH)
    }
}

/** 차 자세 — 위치 + 방향. */
data class Pose(val at: Vec2, val headingDeg: Float)

/** 도면 요소 — 화면(Codex)이 그린다. 채점은 [CourseZone] 만 본다. */
sealed interface MapShape {
    /** 도로 — 중심선 꺾은선 + 폭. [lanes] 가 2 이상이면 가운데 차선을 점선으로. */
    data class Road(val points: List<Vec2>, val widthM: Float, val lanes: Int = 1) : MapShape
    /** 원형 도로(회전교차로) — 중심 + 반지름(차로 가운데) + 폭. */
    data class Ring(val center: Vec2, val radiusM: Float, val widthM: Float) : MapShape
    /** 주차 칸. [headingDeg] = 칸 안쪽(차가 들어가는) 방향. [target] = 이 코스가 쓰는 칸. */
    data class Bay(val center: Vec2, val widthM: Float, val lengthM: Float, val headingDeg: Float, val target: Boolean = false) : MapShape
    data class StopLine(val a: Vec2, val b: Vec2) : MapShape
    data class Crosswalk(val a: Vec2, val b: Vec2, val widthM: Float) : MapShape
    /** 신호등 기둥. [facingDeg] = 신호가 보는 방향(다가오는 차 쪽). */
    data class Light(val at: Vec2, val facingDeg: Float) : MapShape
    /** 경사로 — 오르는 방향 [upHeadingDeg]. */
    data class Ramp(val area: Area, val upHeadingDeg: Float) : MapShape
    /** 도면 위 이름표(예: "경사로"). 운전자 문장 규칙(숫자 없음)과 같다. */
    data class Label(val at: Vec2, val text: String) : MapShape
}

/** 도면 한 장. [widthM]×[heightM] 이 그리기 범위(원점 = 왼쪽 아래). */
data class TrackMap(val id: String, val title: String, val widthM: Float, val heightM: Float, val shapes: List<MapShape>)

/** 구간 종류 — 장내기능시험 항목 + 도로 과제 항목. [label] 은 화면·리포트 이름. */
enum class ZoneKind(val label: String) {
    START("출발"),
    DEVICE("장치 조작"),
    LANE("차로 준수"),
    SLOPE("경사로"),
    TURN_LEFT("좌회전"),
    TURN_RIGHT("우회전"),
    SIGNAL("신호 교차로"),
    PARKING("직각 주차"),
    ACCEL("가속"),
    EMERGENCY("돌발"),
    STOP("정지"),
    LANE_CHANGE("차로 변경"),
    ROUNDABOUT("회전교차로"),
    SCHOOL_ZONE("어린이 보호구역"),
    /** 연습 구간(라운드 25 결정 4) — 실제 장내기능시험(2016-12 개정 이후)에는 없다. 시험 모드에선 코스에서 빠진다. */
    S_CURVE("S자"),
    FINISH("종료"),
}

enum class Side(val label: String) { LEFT("왼쪽"), RIGHT("오른쪽") }

/** 장치 조작 항목(장내기능 "장치 조작" 참고). */
enum class Device(val label: String) { HEADLIGHT("전조등"), WIPER("와이퍼"), LEFT_INDICATOR("왼쪽 방향지시등"), RIGHT_INDICATOR("오른쪽 방향지시등"), DRIVE_GEAR("기어 D") }

/**
 * **모의 감점표** — 도로교통공단 장내기능시험을 참고해 정한 값(가정, INTEGRATION B 10/4). 발표 전 공단 기준으로 교정한다.
 * 합격선 80. 신호 위반은 실격.
 */
object ExamPoints {
    const val PASS_SCORE = 80
    const val DEVICE_EACH = 5
    const val LANE_CONTACT = 15
    const val TURN_CONTACT = 5
    const val PARKING_CONTACT = 10
    const val SLOPE_NO_STOP = 10
    const val SLOPE_ROLLBACK = 10
    const val SLOPE_LATE_DEPART = 10
    const val NO_INDICATOR = 5
    const val PARKING_NOT_IN_BAY = 10
    const val PARKING_OVERTIME = 10
    const val ACCEL_UNDER = 10
    const val EMERGENCY_LATE_STOP = 10
    const val EMERGENCY_NO_HAZARD = 10
    const val NO_STOP = 10
    const val OVER_SPEED = 10

    const val SLOPE_ROLLBACK_M = 1.0f
    const val SLOPE_DEPART_SECONDS = 30
    const val PARKING_LIMIT_SECONDS = 120
    const val ACCEL_TARGET_KMH = 20f
    const val EMERGENCY_STOP_SECONDS = 2f
    const val DEVICE_LIMIT_SECONDS = 35
}

/**
 * 구간 규칙. 위반 = [Deduction] 하나. 규칙이 필요한 신호가 한 번도 오지 않았으면(MISSING) 감점하지 않고 "미측정" 으로 남긴다.
 * [points] 는 깎는 점수, [disqualify] 면 점수와 무관하게 불합격.
 */
sealed interface ZoneRule {
    val points: Int
    val disqualify: Boolean get() = false
    /** 화면·리포트에 쓰는 규칙 이름(숫자 없음). */
    val label: String

    /** 구간 안에서 한 번 완전히 멈춘다(경사로·정지선·종료). */
    data class StopInside(override val points: Int = ExamPoints.NO_STOP, override val label: String = "멈추기") : ZoneRule
    /** 멈춘 뒤 구간 방향의 반대로 [maxM] 넘게 밀리지 않는다(경사로). */
    data class NoRollback(val maxM: Float = ExamPoints.SLOPE_ROLLBACK_M, override val points: Int = ExamPoints.SLOPE_ROLLBACK, override val label: String = "뒤로 밀리지 않기") : ZoneRule
    /** 멈춘 뒤 [seconds] 안에 다시 출발한다(경사로). */
    data class DepartWithin(val seconds: Int = ExamPoints.SLOPE_DEPART_SECONDS, override val points: Int = ExamPoints.SLOPE_LATE_DEPART, override val label: String = "제때 출발") : ZoneRule
    /** 구간 안(또는 진입 [leadSeconds] 전부터)에서 [side] 방향지시등을 한 번 켠다. */
    data class Indicator(val side: Side, val leadSeconds: Float = 3f, override val points: Int = ExamPoints.NO_INDICATOR, override val label: String = "${side.label} 방향지시등") : ZoneRule
    /** 빨간 신호에 구간(교차로 안)으로 들어가지 않는다. 실격. */
    data class NoEntryOnRed(override val points: Int = 0, override val label: String = "신호 지키기") : ZoneRule {
        override val disqualify: Boolean get() = true
    }
    /** 구간 안에서 [kmh] 이상 낸다(가속 구간). */
    data class ReachSpeed(val kmh: Float = ExamPoints.ACCEL_TARGET_KMH, override val points: Int = ExamPoints.ACCEL_UNDER, override val label: String = "속도 올리기") : ZoneRule
    /** 구간 안에서 [kmh] 를 넘지 않는다(보호구역·회전교차로). 구간당 한 번. */
    data class MaxSpeed(val kmh: Float, override val points: Int = ExamPoints.OVER_SPEED, override val label: String = "속도 지키기") : ZoneRule
    /** 돌발 경보가 켜지면 [withinSeconds] 안에 멈춘다. */
    data class StopOnEmergency(val withinSeconds: Float = ExamPoints.EMERGENCY_STOP_SECONDS, override val points: Int = ExamPoints.EMERGENCY_LATE_STOP, override val label: String = "돌발 시 멈추기") : ZoneRule
    /** 돌발 경보 중 비상등을 켠다. */
    data class HazardOnEmergency(override val points: Int = ExamPoints.EMERGENCY_NO_HAZARD, override val label: String = "돌발 시 비상등") : ZoneRule
    /** 구간 안에서 멈췄을 때 차 위치가 [bay] 안이다(주차 완료). */
    data class StopInsideBay(val bay: Area, override val points: Int = ExamPoints.PARKING_NOT_IN_BAY, override val label: String = "칸 안에 세우기") : ZoneRule
    /** 구간에 [seconds] 넘게 머무르지 않는다(주차 제한 시간). */
    data class TimeLimit(val seconds: Int, override val points: Int = ExamPoints.PARKING_OVERTIME, override val label: String = "제한 시간") : ZoneRule
    /** 구간 안에서 [items] 를 각각 한 번 조작한다(항목마다 [points]). */
    data class Devices(val items: List<Device>, override val points: Int = ExamPoints.DEVICE_EACH, override val label: String = "장치 조작") : ZoneRule
    /** 구간 안에서 검지선에 닿지 않는다(닿을 때마다). */
    data class NoLineContact(override val points: Int, override val label: String = "선 밟지 않기") : ZoneRule
}

/**
 * 코스의 한 구간. [area] 에 들어오면 진입, 나가면 이탈. [direction] 은 구간의 진행 방향(°) — 밀림 계산에 쓴다.
 * [guide] 는 가이드 모드에서 진입할 때 읽는 문장, [announce] 는 시험(평가) 모드의 구간 안내 — 둘 다 숫자 없음.
 * [practiceOnly] = 연습 모드(가이드·힌트)에서만 있는 곁가지 구간 — [TrackCourse.practiceZones] 에 두고 [TrackCourse.forMode] 가 연습 모드에만 끼운다. 안 지나도 "미통과" 가 아니다.
 */
data class CourseZone(
    val id: String,
    val title: String,
    val kind: ZoneKind,
    val area: Area,
    val directionDeg: Float,
    val rules: List<ZoneRule>,
    val guide: String,
    val announce: String = "${kind.label} 구간입니다.",
    val practiceOnly: Boolean = false,
)

/**
 * 코스 과제 하나 — 도면 + 순서 있는 구간 + 기대 경로.
 * @param route 화면이 옅게 그리는 기대 경로(m). 채점과 무관.
 * @param passScore 합격선. null 이면 합격 판정이 없는 연습 코스(도로 과제) — 리포트는 "놓친 것" 만.
 * @param laneContactPoints 구간 밖(또는 선 규칙이 없는 구간)에서 검지선에 닿을 때 깎는 점수(차로 준수). 0 이면 안 깎는다.
 * @param practiceZones 연습 모드에만 끼우는 곁가지 구간 — 키 = 그 앞 구간 id. [zones] 는 시험(평가) 그대로라 기존 채점·화면이 바뀌지 않는다.
 * @param practiceRoute 연습 모드의 기대 경로(곁가지를 지나는 길). null 이면 [route] 그대로.
 */
data class TrackCourse(
    val id: String,
    val title: String,
    val map: TrackMap,
    val start: Pose,
    val route: List<Vec2>,
    val zones: List<CourseZone>,
    val passScore: Int? = null,
    val laneContactPoints: Int = ExamPoints.LANE_CONTACT,
    val practiceZones: Map<String, CourseZone> = emptyMap(),
    val practiceRoute: List<Vec2>? = null,
) {
    init {
        require(zones.map { it.id }.toSet().size == zones.size) { "duplicate zone id in $id" }
    }

    val isExam: Boolean get() = passScore != null
    fun zone(id: String): CourseZone? = zones.firstOrNull { it.id == id }

    /**
     * 모드별 코스 — [evaluate](평가 = 모의시험)면 그대로, 연습 모드면 곁가지 구간([practiceZones])을 끼우고 곁가지를 지나는 기대 경로로.
     * 도면은 같다(곁가지 도로는 시설로 늘 그려진다).
     */
    fun forMode(evaluate: Boolean): TrackCourse = if (evaluate || practiceZones.isEmpty()) this
        else copy(zones = zones.flatMap { z -> listOfNotNull(z, practiceZones[z.id]) }, route = practiceRoute ?: route, practiceZones = emptyMap())
}
