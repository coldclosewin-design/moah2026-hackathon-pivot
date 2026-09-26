package com.moah.hackathon

import android.app.Application
import android.content.Context
import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.LessonStateMachine
import com.moah.hackathon.feature.lesson.ProgressStore
import com.moah.hackathon.feature.lesson.RemarkPool
import com.moah.hackathon.ports.AndroidTtsPort
import com.moah.hackathon.ports.CloudCoachPort
import com.moah.hackathon.ports.CoachPort
import com.moah.hackathon.ports.FakeCoachPort
import com.moah.hackathon.ports.FakeLocationPort
import com.moah.hackathon.ports.GpsLocationPort
import com.moah.hackathon.ports.LatLng
import com.moah.hackathon.ports.LocationPort
import com.moah.hackathon.ports.TtsPort
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import com.moah.hackathon.vehicle.VehiclePort
import com.moah.hackathon.vehicle.VehiclePortFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 포트 조립 단일 지점. 실구현으로 바꿀 때는 여기만 고친다.
 *  - VehiclePort: BuildConfig.USE_FAKE_VSS 로 Fake/Real (VehiclePortFactory). 폴백으로 Fake 가 됐어도 `vehicle is FakeVehiclePort` 로 잡힌다
 *  - LocationPort: USE_FAKE_LOCATION 으로 Fake(속도 적분)/GPS — 도로 과제용, 주차 과제는 쓰지 않는다
 *  - TtsPort: Android TTS (엔진 없으면 자막만)
 *  - CoachPort: Fake(시드 멘트 풀). Cloud Copilot 은 사내 인증 확인 후
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

    /** 이 세션이 관심 있는 신호와 그 출처(실신호/시뮬/미측정). Hybrid 면 키별로 갈린다 — 리포트 배지의 근거. */
    val registry = SignalRegistry.forPort(ParkingRecorder.KEYS, vehicle)
    val store = ProgressStore()
    /**
     * 코치. 전송 계층(`CoachTransport`)은 사내 Cloud Copilot 인증 방식이 확인되기 전까지 null → 항상 시드 멘트 풀로 폴백한다.
     * 확인되면 `CoachTransport` 구현체 하나를 여기 넘기면 끝.
     */
    val coach: CoachPort = CloudCoachPort(fallback = FakeCoachPort(RemarkPool(SeedCatalog.remarks)), transport = null)
    /** 시연 조작 패널이 고르는 Fake 시나리오. Real 에서는 쓰이지 않는다. */
    val scenarios: List<Scenario> = ParkingScenarios.all

    val lesson = LessonStateMachine(
        vehicle = vehicle, tts = tts, coach = coach, registry = registry, store = store,
        tasks = SeedCatalog.tasks, guideFor = SeedCatalog::guideFor,
        reservation = SeedCatalog.reservation, benefits = SeedCatalog.benefits,
        profile = SeedCatalog.demoProfile, scope = appScope,
    )

    fun dispose() {
        tts.dispose()
        vehicle.dispose()
    }

    private companion object {
        /** 도로 과제 시드가 생기기 전까지의 기본 출발지(서초). */
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
