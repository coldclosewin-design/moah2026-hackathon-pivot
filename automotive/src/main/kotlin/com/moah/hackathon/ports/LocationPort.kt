package com.moah.hackathon.ports

import com.moah.hackathon.vehicle.VehiclePort
import com.moah.hackathon.vehicle.toVssFloat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mobis.vss.VssConstants

/**
 * 위치 계약. 상태기계는 "목적지까지 남은 거리(m)"만 필요로 한다.
 * 실구현은 GPS(FusedLocation) → haversine. 외부 시연은 [FakeLocationPort] (차량 속도 적분).
 */
interface LocationPort {
    /** 현재 위치. 시연에서는 시드 출발지. */
    suspend fun currentLocation(): LatLng

    /** origin→dest 남은 거리(m) 스트림. 첫 emit 은 전체 거리. */
    fun observeRemaining(origin: LatLng, dest: LatLng): Flow<Double>
}

/**
 * VehiclePort 의 속도를 적분해 남은 거리를 줄인다. [demoSpeedFactor] 로 시연 시간을 압축한다
 * (기본 10배: 12 km 를 평균 60 km/h 로 약 70초).
 */
class FakeLocationPort(
    private val vehicle: VehiclePort,
    private val origin: LatLng,
    private val demoSpeedFactor: Double = 10.0,
    private val tickMillis: Long = 500L,
) : LocationPort {

    override suspend fun currentLocation(): LatLng = origin

    override fun observeRemaining(origin: LatLng, dest: LatLng): Flow<Double> = flow {
        var remaining = origin.distanceTo(dest)
        emit(remaining)
        coroutineScope {
            val speedKmh = MutableStateFlow(0f)
            val sub = vehicle.observe(listOf(VssConstants.VEHICLE_SPEED))
                .onEach { m -> m[VssConstants.VEHICLE_SPEED]?.toVssFloat()?.let { speedKmh.value = it } }
                .launchIn(this)
            try {
                while (remaining > 0) {
                    delay(tickMillis)
                    val metersPerSec = speedKmh.value / 3.6 * demoSpeedFactor
                    remaining = (remaining - metersPerSec * tickMillis / 1000.0).coerceAtLeast(0.0)
                    emit(remaining)
                }
            } finally {
                sub.cancel()
            }
        }
    }
}
