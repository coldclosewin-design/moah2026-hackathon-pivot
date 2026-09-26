package com.moah.hackathon.vehicle

import android.content.Context
import android.util.Log
import com.moah.hackathon.BuildConfig

/**
 * 주입 스위치는 이 한 곳뿐이다.
 *
 * - `BuildConfig.USE_FAKE_VSS == true`  → [FakeVehiclePort] (외부 개발/시연)
 * - `false` → [RealVehiclePort]. 기본으로 [HybridVehiclePort] 로 감싸 **실물에서 안 오는 키만 Fake 가 채운다**
 *   (`BuildConfig.FILL_MISSING_WITH_FAKE`, `-PfillMissing=false` 면 순수 Real).
 *   외부 에뮬에서 실수로 false 로 빌드했거나 vss 서비스가 없으면(NoClassDefFoundError / IllegalStateException) 크래시 대신 Fake 로 폴백하고 경고를 남긴다.
 */
object VehiclePortFactory {

    private const val TAG = "MOAH/VehiclePortFactory"

    fun create(context: Context): VehiclePort {
        if (BuildConfig.USE_FAKE_VSS) {
            Log.i(TAG, "USE_FAKE_VSS=true → FakeVehiclePort")
            return FakeVehiclePort()
        }
        val real = try {
            RealVehiclePort(context).also { Log.i(TAG, "RealVehiclePort ready") }
        } catch (e: LinkageError) {
            // mobis.vss 클래스가 런타임에 없음 (외부 에뮬에서 stub 으로 빌드된 경우)
            Log.e(TAG, "mobis.vss not present at runtime → falling back to FakeVehiclePort", e)
            return FakeVehiclePort()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "VSS service unavailable → falling back to FakeVehiclePort", e)
            return FakeVehiclePort()
        }
        if (!BuildConfig.FILL_MISSING_WITH_FAKE) return real
        // 기본 시뮬레이션(sin 속도)은 끈다 — 속도는 실물이 주고, 없는 키는 시나리오 재생기가 채운다
        Log.i(TAG, "FILL_MISSING_WITH_FAKE=true → HybridVehiclePort(real, fake)")
        return HybridVehiclePort(real, FakeVehiclePort(simulate = false))
    }
}
