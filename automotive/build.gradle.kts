plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.moah.hackathon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moah.hackathon"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // 계측 테스트(androidTest)를 다시 만들 때 주의: AGP 는 테스트 매니페스트의 **첫 번째** <instrumentation> 의 android:name 을
        // testInstrumentationRunner 값으로 덮어쓴다. 항목이 2개 이상이면 첫 항목과 같은 이름을 여기에 명시해 고정해야 한다
        // (16번 저장소 PR #7 에서 "Unable to find instrumentation info" 로 실제 겪음).

        // 외부: "true" (FakeVehiclePort 실행) / 사내 머지 시: "false" (RealVehiclePort → VSSManager)
        buildConfigField("boolean", "USE_FAKE_VSS", "true")
        // 위치: "true" = 속도 적분 Fake(시연 시간 압축) / "false" = GPS 우선, 권한·제공자 없으면 Fake 폴백
        // 빌드 시 -PfakeLocation=false 로 바꿀 수 있다 (에뮬 geo fix 검증용)
        buildConfigField("boolean", "USE_FAKE_LOCATION", (project.findProperty("fakeLocation") ?: "true").toString())
        // TTS 음성 이름. 기본 ko-kr-x-kob-network(16번에서 사용자가 네 후보를 듣고 결정). 오프라인·미설치면 같은 계열 로컬(kob-local) → 로컬 최고 품질로 복귀.
        // 후보는 logcat "MOAH/AndroidTtsPort: ko voices". 로컬 자동 선택으로 돌리려면 -PttsVoice=auto
        buildConfigField("String", "TTS_VOICE", "\"${(project.findProperty("ttsVoice") ?: "ko-kr-x-kob-network").toString().let { if (it == "auto") "" else it }}\"")
        // Fake 위치·Fake 신호 시나리오의 시간 압축 배율. 시연 길이에 맞춰 -PdemoSpeed=12 처럼 바꾼다 (GPS 모드에서는 위치에 무시)
        buildConfigField("double", "DEMO_SPEED_FACTOR", (project.findProperty("demoSpeed") ?: "10.0").toString().toDouble().toString())
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    testOptions {
        // JVM 단위 테스트에서 android.util.Log 등 프레임워크 호출이 기본값을 돌려주도록
        unitTests.isReturnDefaultValues = true
    }
}

// ───────── mobis.vss API 소스 — 둘 중 하나만 활성화 (사내 머지 시 이 한 줄만 교체) ─────────
// 외부(개인 PC): 컴파일 전용 스텁 모듈
val vssApi: Any = project(":vss-stub")
// 사내(WebIDE): 시스템 jar. 위 줄을 아래로 교체하고 settings.gradle.kts 의 include(":vss-stub") 제거
// val vssApi: Any = files("/system/framework/mobis.framework.core.jar")
// ─────────────────────────────────────────────────────────────────────────────────

dependencies {
    compileOnly(vssApi)          // APK 에 포함되지 않음 (런타임은 시스템이 제공)
    testImplementation(vssApi)   // JVM 단위 테스트에서 VssConstants 참조용

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
