package com.moah.hackathon.ports

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 실제 위치(GPS) 기반 LocationPort. Android LocationManager 만 사용한다(Play services 의존 없음).
 *
 * 폴백 규칙: 위치 권한이 없거나 GPS 제공자가 꺼져 있으면 [fallback](FakeLocationPort, 속도 적분)으로 넘긴다.
 * 그래서 외부 에뮬·사내 에뮬·실차 어디서든 여정이 멈추지 않는다.
 *
 * 사내 확인 사항(INTEGRATION.md): 사내 에뮬/실차에 GPS 제공자가 있는지, 없으면 `Vehicle.CurrentLocation.*`
 * 신호로 이 클래스와 같은 인터페이스를 구현한다.
 */
class GpsLocationPort(
    context: Context,
    private val fallback: LocationPort,
    private val minIntervalMillis: Long = 1000L,
    private val minDistanceMeters: Float = 2f,
) : LocationPort {

    private val app = context.applicationContext
    private val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun gpsAvailable(): Boolean = try {
        hasPermission() && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (e: RuntimeException) {
        Log.w(TAG, "isProviderEnabled failed", e); false
    }

    /** 이 포트가 실제로 GPS 를 쓰고 있는지 (UI/로그용). */
    val isUsingGps: Boolean get() = gpsAvailable()

    @Suppress("MissingPermission")
    override suspend fun currentLocation(): LatLng {
        if (!gpsAvailable()) return fallback.currentLocation()
        val last = try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: RuntimeException) {
            Log.w(TAG, "getLastKnownLocation failed", e); null
        }
        return last?.toLatLng() ?: fallback.currentLocation().also { Log.i(TAG, "no last fix → fallback origin") }
    }

    @Suppress("MissingPermission")
    override fun observeRemaining(origin: LatLng, dest: LatLng): Flow<Double> {
        if (!gpsAvailable()) {
            Log.i(TAG, "GPS unavailable (permission=${hasPermission()}) → fallback")
            return fallback.observeRemaining(origin, dest)
        }
        return callbackFlow {
            trySend(origin.distanceTo(dest))
            val listener = LocationListener { loc: Location ->
                trySend(loc.toLatLng().distanceTo(dest))
            }
            try {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, minIntervalMillis, minDistanceMeters, listener, Looper.getMainLooper(),
                )
                Log.i(TAG, "GPS updates started")
            } catch (e: RuntimeException) {
                Log.w(TAG, "requestLocationUpdates failed → closing", e)
                close(e)
            }
            awaitClose {
                try { lm.removeUpdates(listener) } catch (e: RuntimeException) { Log.w(TAG, "removeUpdates failed", e) }
            }
        }
    }

    private fun Location.toLatLng() = LatLng(latitude, longitude)

    private companion object {
        const val TAG = "MOAH/GpsLocationPort"
    }
}
