package com.moah.hackathon.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.moah.hackathon.data.AdminPresets
import com.moah.hackathon.App
import com.moah.hackathon.BuildConfig
import com.moah.hackathon.ui.concepts.DesignScale
import com.moah.hackathon.feature.lesson.LessonViewModel
import com.moah.hackathon.ui.lesson.LessonRoute

class MainActivity : ComponentActivity() {
    private lateinit var lesson: LessonViewModel

    // GpsLocationPort 용 위치 권한 요청. 거부돼도 앱은 Fake 위치로 동작한다.
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i("MOAH/MainActivity", "ACCESS_FINE_LOCATION granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // AAOS otherwise pans the whole poster and covers the bottom of the coach composer.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (!BuildConfig.USE_FAKE_LOCATION &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val container = (application as App).container
        lesson = ViewModelProvider(this, LessonViewModel.factory(container))[LessonViewModel::class.java]
        if (savedInstanceState == null) applyPreset(intent)
        setContent {
            MaterialTheme(colorScheme = coachColorScheme()) {
                // 기기 밀도와 무관하게 설계 크기(2560×1268 dp)로 그린다. 이유는 DesignScale.kt
                DesignScale { LessonRoute(lesson) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyPreset(intent)
    }

    private fun applyPreset(intent: Intent) {
        intent.getStringExtra(AdminPresets.EXTRA_PRESET)?.let { lesson.admin?.applyPreset(it) }
        intent.removeExtra(AdminPresets.EXTRA_PRESET)
    }
}
