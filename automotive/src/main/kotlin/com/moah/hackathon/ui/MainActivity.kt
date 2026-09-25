package com.moah.hackathon.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moah.hackathon.App
import com.moah.hackathon.BuildConfig
import com.moah.hackathon.ui.concepts.DesignScale

class MainActivity : ComponentActivity() {

    // GpsLocationPort 용 위치 권한 요청. 거부돼도 앱은 Fake 위치로 동작한다.
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i("MOAH/MainActivity", "ACCESS_FINE_LOCATION granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.USE_FAKE_LOCATION &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val container = (application as App).container
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                // 피벗 직후: VehiclePort 왕복(속도 sensor · 도어 actuator)을 눈으로 확인하는 Dashboard.
                // 연수 세션 화면(feature/lesson)이 들어오면 여기서 교체한다.
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container.vehicle))
                // 기기 밀도와 무관하게 설계 크기(2560×1268 dp)로 그린다. 이유는 DesignScale.kt
                DesignScale { DashboardScreen(vm) }
            }
        }
    }
}
