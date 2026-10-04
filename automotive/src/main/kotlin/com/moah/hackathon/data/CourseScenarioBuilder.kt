package com.moah.hackathon.data

import com.moah.hackathon.scoring.PathReconstructor
import com.moah.hackathon.scoring.Pose
import com.moah.hackathon.scoring.Vec2
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.ScenarioBuilder
import com.moah.hackathon.vehicle.SimOnlySignals
import mobis.vss.VssConstants as V
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 코스 도면과 시나리오가 **같은 경로**를 쓰게 하는 도구(10/4). 거북이처럼 "직진 N m · 반지름 R 로 몇 도" 를 이어 붙여 경로를 만들고,
 * 도면의 도로·구간도 이 경로에서 잰다. 그래서 시나리오의 차는 항상 도로 위를 달린다.
 * 방향은 +y 기준 반시계 양수(°), 왼쪽으로 도는 각이 양수.
 */
class TrackPath(val points: List<Vec2>, val headings: List<Float>) {
    init { require(points.size >= 2 && points.size == headings.size) { "path needs at least two points" } }

    private val cumulative: FloatArray = FloatArray(points.size).also { c ->
        for (i in 1 until points.size) c[i] = c[i - 1] + points[i].distanceTo(points[i - 1])
    }
    val length: Float get() = cumulative.last()
    val start: Pose get() = Pose(points.first(), headings.first())
    val end: Pose get() = Pose(points.last(), headings.last())

    /** 경로 시작에서 [s] m 지점의 위치·진행 방향. */
    fun at(s: Float): Pose {
        val d = s.coerceIn(0f, length)
        var i = 1
        while (i < points.size - 1 && cumulative[i] < d) i++
        val seg = cumulative[i] - cumulative[i - 1]
        val f = if (seg <= 0f) 1f else (d - cumulative[i - 1]) / seg
        val p = points[i - 1] + (points[i] - points[i - 1]) * f
        return Pose(p, headings[i - 1] + angleDiff(headings[i - 1], headings[i]) * f)
    }

    operator fun plus(other: TrackPath): TrackPath = TrackPath(points + other.points.drop(1), headings + other.headings.drop(1))

    companion object {
        /** b - a 를 (-180, 180] 로. */
        fun angleDiff(a: Float, b: Float): Float {
            var d = (b - a) % 360f
            if (d > 180f) d -= 360f
            if (d <= -180f) d += 360f
            return d
        }
        fun normalize(deg: Float): Float = angleDiff(0f, deg)
    }
}

/** 거북이 경로 작성기. [straight]·[arc] 를 이어 [path] 로. 간격 [step] m. */
class Turtle(start: Pose, private val step: Float = 0.25f) {
    private val pts = arrayListOf(start.at)
    private val hdg = arrayListOf(start.headingDeg)
    val pose: Pose get() = Pose(pts.last(), hdg.last())

    fun straight(len: Float): Turtle {
        val n = max(1, (len / step).toInt())
        val dir = Vec2.ofHeading(hdg.last())
        val p0 = pts.last(); val h = hdg.last()
        for (i in 1..n) { pts += p0 + dir * (len * i / n); hdg += h }
        return this
    }

    /** 반지름 [radius] 로 [deltaDeg] 만큼 돈다(양수 = 왼쪽). */
    fun arc(radius: Float, deltaDeg: Float): Turtle {
        val p0 = pts.last(); val h0 = hdg.last()
        val left = Vec2.ofHeading(h0 + 90f)
        val center = if (deltaDeg > 0) p0 + left * radius else p0 - left * radius
        val arcLen = radius * Math.toRadians(kotlin.math.abs(deltaDeg).toDouble()).toFloat()
        val n = max(2, (arcLen / step).toInt())
        for (i in 1..n) {
            val phi = Math.toRadians((deltaDeg * i / n).toDouble())
            val v = p0 - center
            val c = kotlin.math.cos(phi).toFloat(); val s = kotlin.math.sin(phi).toFloat()
            pts += Vec2(center.x + v.x * c - v.y * s, center.y + v.x * s + v.y * c)
            hdg += h0 + deltaDeg * i / n
        }
        return this
    }

    fun path(): TrackPath = TrackPath(pts.toList(), hdg.toList())
}

/**
 * 코스 시나리오 작성기 — [drive] 가 경로를 따라 속도 곡선(가속 → 순항 → 끝 속도)으로 움직이며 [dt] 마다 속도·위치·방향·조향각을 낸다.
 * 위치·방향은 `Track.*`(시뮬레이션), 조향각은 경로 곡률에서 자전거 모델로 역산한다. 전부 가정 값이다.
 */
class DriveScript(id: String, title: String, start: Pose, private val dt: Double = 0.25) {
    private val b = ScenarioBuilder(id, title)
    var t = 0.0
        private set
    private var pose = start
    private var vMps = 0.0

    /** 지금 시각에 신호 값을 넣는다. */
    fun set(vararg values: Pair<String, String>): DriveScript { b.at(t, *values); return this }

    /** 멈춘 채 [seconds] 를 보낸다(끝에 속도 0·위치를 한 번 더 낸다 — 정차가 신호로 보이게). */
    fun hold(seconds: Double): DriveScript {
        emitPose(0.0)
        t += seconds
        emitPose(0.0)
        return this
    }

    /**
     * [path] 를 따라 달린다. [reverse] 면 뒤로(차 방향 = 진행 방향 + 180°). 끝에서 [endKmh] 가 된다(0 이면 정확히 끝점에서 정지).
     * @param events 경로 시작에서 몇 m 지점에서 넣을 신호(지시등·검지선·돌발 등).
     */
    fun drive(
        path: TrackPath,
        cruiseKmh: Double,
        endKmh: Double = 0.0,
        reverse: Boolean = false,
        accel: Double = 1.2,
        decel: Double = 1.5,
        events: List<Pair<Float, Array<out Pair<String, String>>>> = emptyList(),
    ): DriveScript {
        require(endKmh <= cruiseKmh + 1e-9) { "endKmh must not exceed cruiseKmh — 끝에서 속도가 튀면 급가속으로 잡힌다" }
        val l = path.length.toDouble()
        val cruise = cruiseKmh / 3.6
        val vEnd = endKmh / 3.6
        var s = 0.0
        val pending = events.sortedBy { it.first }.toMutableList()
        while (s < l - 1e-4) {
            // 이산 정지 곡선: 이번 스텝(v·dt) + 남은 감속 거리((v² - vEnd²)/2a) = 남은 거리 를 v 로 푼다 → 스텝마다 a·dt 씩 줄며 끝점에 닿는다
            val reach = (l - s) + vEnd * vEnd / (2 * decel)
            val stopProfile = decel * (-dt + sqrt(dt * dt + 2 * reach / decel))
            val target = min(cruise, stopProfile)
            vMps = when {
                vMps < target -> min(target, vMps + accel * dt)
                target == stopProfile -> target                     // 끝점에 맞춰 서는 곡선
                else -> max(target, vMps - decel * dt)               // 순항 속도가 지금보다 낮으면 부드럽게 줄인다
            }
            vMps = max(vMps, 0.4)
            val before = s
            s = min(l, s + vMps * dt)
            t += dt
            val p = path.at(s.toFloat())
            pose = Pose(p.at, TrackPath.normalize(if (reverse) p.headingDeg + 180f else p.headingDeg))
            val steering = steeringAt(path, s.toFloat(), reverse)
            // 서는 구간은 실제로 간 거리 / 시간 — 마지막 스텝이 짧으면 속도도 작아져 정지가 급정지로 잡히지 않는다.
            // 이어 달리는 구간은 모델 속도 — 끝 스텝이 잘려도 속도가 꺼지지 않게(꺼지면 급제동·급가속 한 쌍으로 잡힌다)
            val shown = if (vEnd == 0.0) (s - before) / dt else vMps
            emitPose(shown * 3.6, V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to fmt(steering))
            while (pending.isNotEmpty() && pending.first().first <= s) b.at(t, *pending.removeAt(0).second)
        }
        pending.forEach { b.at(t, *it.second) }
        vMps = vEnd
        if (vEnd == 0.0) { t += dt; emitPose(0.0) }
        return this
    }

    private fun emitPose(kmh: Double, vararg extra: Pair<String, String>) {
        b.at(t,
            V.VEHICLE_SPEED to ScenarioBuilder.formatSpeed(kmh),
            SimOnlySignals.TRACK_POSITION_X_M to fmt(pose.at.x),
            SimOnlySignals.TRACK_POSITION_Y_M to fmt(pose.at.y),
            SimOnlySignals.TRACK_HEADING_DEG to fmt(pose.headingDeg),
            *extra)
    }

    /** 경로 곡률 → 앞바퀴 각 → 핸들 각(PathReconstructor 의 휠베이스·조향비). 후진이면 부호가 바뀐다. */
    private fun steeringAt(path: TrackPath, s: Float, reverse: Boolean): Float {
        val a = path.at(s - 0.5f); val c = path.at(s + 0.5f)
        val ds = (min(path.length, s + 0.5f) - max(0f, s - 0.5f)).takeIf { it > 0.05f } ?: return 0f
        val dTheta = Math.toRadians(TrackPath.angleDiff(a.headingDeg, c.headingDeg).toDouble())
        val signedDs = if (reverse) -ds else ds
        val wheelDeg = Math.toDegrees(atan(PathReconstructor.WHEELBASE_M * dTheta / signedDs)).toFloat()
        return (wheelDeg * PathReconstructor.STEERING_RATIO).coerceIn(-540f, 540f)
    }

    fun build(): Scenario = b.build()

    private fun fmt(v: Float): String = String.format(java.util.Locale.US, "%.2f", v)
}
