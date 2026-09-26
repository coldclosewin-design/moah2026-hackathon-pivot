package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.Gear
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 회차 동안 차가 어떻게 움직였는지의 **추정** 궤적 한 점. 미터 단위, 회차 시작 위치가 원점, 시작 방향이 +y(화면 위).
 * [headingDeg] 는 +y 에서 반시계(왼쪽) 양수. [reversing] 은 그 구간의 기어가 R 이었나.
 *
 * **실제 위치가 아니다.** 속도(크기)·기어(방향)·조향각으로 dead-reckoning 한 값이라 화면에는 "신호로 추정한 궤적" 이라고 쓴다.
 */
data class PathPoint(val tMillis: Long, val x: Float, val y: Float, val headingDeg: Float, val reversing: Boolean)

/**
 * 자전거 모델 dead-reckoning. 속도 샘플 사이를 사다리꼴로 적분하고, 그 순간의 기어(R = 뒤로)·조향각(→ 앞바퀴 각)으로
 * 진행 방향을 돌린다. 휠베이스·조향비·최대 바퀴 각은 **중형 SUV 가정**(INTEGRATION B절 9/27) — 실차 값이 오면 상수만 바꾼다.
 */
object PathReconstructor {
    const val WHEELBASE_M = 2.7f
    /** 핸들 각 → 앞바퀴 각. 450° 핸들 ≈ 30° 바퀴. */
    const val STEERING_RATIO = 15f
    const val MAX_WHEEL_DEG = 35f

    fun reconstruct(speed: List<SpeedSample>, angle: List<AngleSample>, gear: List<GearSample>): List<PathPoint> {
        if (speed.isEmpty()) return emptyList()
        val out = ArrayList<PathPoint>(speed.size)
        var x = 0f; var y = 0f; var heading = 0.0   // rad, +y 기준 반시계 양수
        var prev = speed.first()
        out += PathPoint(prev.tMillis, 0f, 0f, 0f, reversing = gearAt(gear, prev.tMillis) == Gear.REVERSE)
        for (s in speed.drop(1)) {
            val dt = (s.tMillis - prev.tMillis) / 1000f
            if (dt > 0f) {
                val vMps = ((prev.value + s.value) / 2f) / 3.6f
                val reversing = gearAt(gear, s.tMillis) == Gear.REVERSE
                val dir = if (reversing) -1f else 1f
                val wheelRad = Math.toRadians(wheelDeg(angleAt(angle, s.tMillis)).toDouble())
                val dist = vMps * dt * dir
                // 자전거 모델: dθ = (v/L)·tan(δ)·dt. 왼쪽 조향(+) 전진 → 반시계, 후진이면 반대
                heading += (dist / WHEELBASE_M) * tan(wheelRad)
                x -= (dist * sin(heading)).toFloat()
                y += (dist * cos(heading)).toFloat()
                out += PathPoint(s.tMillis, x, y, Math.toDegrees(heading).toFloat(), reversing)
            }
            prev = s
        }
        return out
    }

    /** 이동 거리 합(미터) — 궤적 검증용. */
    fun travelled(path: List<PathPoint>): Float =
        path.zipWithNext { a, b -> kotlin.math.hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat() }.sum()

    private fun wheelDeg(steeringDeg: Float): Float = (steeringDeg / STEERING_RATIO).coerceIn(-MAX_WHEEL_DEG, MAX_WHEEL_DEG)
    private fun angleAt(samples: List<AngleSample>, t: Long): Float = samples.lastOrNull { it.tMillis <= t }?.value ?: 0f
    private fun gearAt(samples: List<GearSample>, t: Long): Gear? = samples.lastOrNull { it.tMillis <= t }?.value
}
