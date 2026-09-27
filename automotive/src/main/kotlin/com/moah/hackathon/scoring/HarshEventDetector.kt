package com.moah.hackathon.scoring

import kotlin.math.abs

/**
 * A층: 속도열에서 급출발·급정지를 찾는다.
 *
 * 가속도는 **고정 시간창**([windowMillis], 기본 300 ms) 차분으로 잰다 — 창보다 촘촘한 샘플은 창 너비로 묶여 잔떨림이 눌리고,
 * 창보다 성긴 샘플(500 ms 틱)은 그냥 이웃 차분이 된다. (이동 평균은 0.15 s 간격의 진짜 급정지 -3.7 m/s² 를 -2.5 로 깎아 버려 뺐다.)
 * 임계값은 UBI 상품이 흔히 쓰는 3 m/s² 근처(가정, 사내 실측 후 조정). 같은 종류 이벤트는 [debounceMillis] 안에서 한 번만 센다.
 */
enum class HarshKind { ACCELERATION, BRAKING }

data class HarshEvent(val tMillis: Long, val kind: HarshKind, val accelMps2: Float)

object HarshEventDetector {
    const val HARSH_ACCEL_MPS2 = 3.0f
    // -3.5 → -3.0 (2026-09-28): 시연 시나리오의 급정지가 임계에 17 ms 여유로 걸쳐 있어 녹화·덤프 부하에서 두 번 빠졌다.
    // 가속과 같은 3 m/s² 로 맞추고, 시나리오 쪽도 4.8 km/h → 0 (-4.4 m/s²) 으로 벌려 여유 144 ms.
    const val HARSH_BRAKE_MPS2 = -3.0f
    const val DEBOUNCE_MILLIS = 1000L
    const val WINDOW_MILLIS = 300L
    /** 샘플 시각 지터(테스트의 ms 절삭, 에뮬 틱 흔들림)로 창이 1~2 ms 빗나가는 것을 막는다. */
    const val WINDOW_TOLERANCE_MILLIS = 20L

    fun detect(
        samples: List<SpeedSample>,
        accelThreshold: Float = HARSH_ACCEL_MPS2,
        brakeThreshold: Float = HARSH_BRAKE_MPS2,
        debounceMillis: Long = DEBOUNCE_MILLIS,
        windowMillis: Long = WINDOW_MILLIS,
    ): List<HarshEvent> {
        val events = ArrayList<HarshEvent>()
        var lastAccelAt: Long? = null
        var lastBrakeAt: Long? = null
        forEachAcceleration(samples, windowMillis) { t, a ->
            if (a >= accelThreshold && (lastAccelAt == null || t - lastAccelAt!! >= debounceMillis)) {
                events.add(HarshEvent(t, HarshKind.ACCELERATION, a)); lastAccelAt = t
            } else if (a <= brakeThreshold && (lastBrakeAt == null || t - lastBrakeAt!! >= debounceMillis)) {
                events.add(HarshEvent(t, HarshKind.BRAKING, a)); lastBrakeAt = t
            }
        }
        return events
    }

    fun maxAbsAccel(samples: List<SpeedSample>, windowMillis: Long = WINDOW_MILLIS): Float {
        var max = 0f
        forEachAcceleration(samples, windowMillis) { _, a -> if (abs(a) > max) max = abs(a) }
        return max
    }

    /**
     * 샘플 i 마다, i 보다 [windowMillis] 이상 앞선 것 중 가장 늦은 샘플 j 를 골라 (v_i - v_j)/(t_i - t_j) 를 m/s² 로 낸다.
     * 그런 j 가 없으면(첫 샘플들) 가장 앞 샘플과 비교한다.
     */
    private inline fun forEachAcceleration(samples: List<SpeedSample>, windowMillis: Long, block: (tMillis: Long, accelMps2: Float) -> Unit) {
        if (samples.size < 2) return
        var j = 0
        for (i in 1 until samples.size) {
            while (j + 1 < i && samples[i].tMillis - samples[j + 1].tMillis >= windowMillis - WINDOW_TOLERANCE_MILLIS) j++
            val dtSec = (samples[i].tMillis - samples[j].tMillis) / 1000f
            if (dtSec <= 0f) continue
            val dvMps = (samples[i].value - samples[j].value) / 3.6f
            block(samples[i].tMillis, dvMps / dtSec)
        }
    }
}
