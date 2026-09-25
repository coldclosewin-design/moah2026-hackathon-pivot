package com.moah.hackathon

import android.app.Application
import android.content.Context
import com.moah.hackathon.ports.AndroidTtsPort
import com.moah.hackathon.ports.FakeLocationPort
import com.moah.hackathon.ports.GpsLocationPort
import com.moah.hackathon.ports.LatLng
import com.moah.hackathon.ports.LocationPort
import com.moah.hackathon.ports.TtsPort
import com.moah.hackathon.vehicle.VehiclePort
import com.moah.hackathon.vehicle.VehiclePortFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 포트 조립 단일 지점. 실구현으로 바꿀 때는 여기만 고친다.
 *  - VehiclePort: BuildConfig.USE_FAKE_VSS 로 Fake/Real (VehiclePortFactory)
 *  - LocationPort: USE_FAKE_LOCATION 으로 Fake(속도 적분)/GPS
 *  - TtsPort: Android TTS (엔진 없으면 자막만)
 *
 * 피벗 직후의 골격이다. 연수 세션 상태기계·채점 엔진·코치 포트는 여기에 추가된다(docs/NEXT.md).
 */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val vehicle: VehiclePort = VehiclePortFactory.create(context)
    val tts: TtsPort = AndroidTtsPort(context, preferredVoice = BuildConfig.TTS_VOICE)
    /** 외부 시연 기본값은 Fake(속도 적분, DEMO_SPEED_FACTOR 배 압축). USE_FAKE_LOCATION=false 면 GPS 우선 + Fake 폴백. */
    val location: LocationPort = FakeLocationPort(
        vehicle, DEFAULT_ORIGIN, demoSpeedFactor = BuildConfig.DEMO_SPEED_FACTOR,
    ).let { fake ->
        if (BuildConfig.USE_FAKE_LOCATION) fake else GpsLocationPort(context, fallback = fake)
    }

    fun dispose() {
        tts.dispose()
        vehicle.dispose()
    }

    private companion object {
        /** 연수 코스 시드가 생기기 전까지의 기본 출발지(서초). 코스 시드가 들어오면 그쪽 값을 쓴다. */
        val DEFAULT_ORIGIN = LatLng(37.4837, 127.0324)
    }
}

class App : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onTerminate() {
        container.dispose()
        super.onTerminate()
    }
}
