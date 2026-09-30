import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ───────── 사내/사외 스위치 (2026-09-30, 사내 이관 1차 결과) — 설정 파일 한 줄 ─────────
// `local.properties` 에 `mobis.vss.jar=<사내 jar 경로>` 가 있으면 그 jar 로 컴파일하고 USE_FAKE_VSS=false(RealVehiclePort → VSSManager).
// 없으면 컴파일 전용 스텁 모듈(:vss-stub)과 USE_FAKE_VSS=true(FakeVehiclePort). settings.gradle.kts 가 같은 키로 :vss-stub 포함 여부를 정한다.
// 같은 커밋이 양쪽에서 그대로 빌드된다 — 코드를 고치는 사내 머지는 없다. jar 는 automotive/libs/(gitignore) 에 두는 것을 권장.
val mobisVssJar: String? = File(rootDir, "local.properties").takeIf { it.isFile }?.let { f ->
    Properties().apply { f.inputStream().use { load(it) } }.getProperty("mobis.vss.jar")?.trim()?.takeIf { it.isNotEmpty() }
}
val useFakeVss: Boolean = mobisVssJar == null
val vssApi: Any = mobisVssJar?.let { files(if (File(it).isAbsolute) it else File(rootDir, it).path) } ?: project(":vss-stub")
logger.lifecycle("mobis.vss: ${if (useFakeVss) "stub (:vss-stub) → USE_FAKE_VSS=true" else "jar $mobisVssJar → USE_FAKE_VSS=false"}")

android {
    namespace = "com.moah.hackathon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moah.hackathon"
        minSdk = 29
        targetSdk = 34
        // MarketUploader 재업로드마다 versionCode +1 (2026-09-30 사내 지침). versionName 은 제출 버전 1.0.0
        versionCode = 1
        versionName = "1.0.0"

        // 계측 테스트(androidTest)를 다시 만들 때 주의: AGP 는 테스트 매니페스트의 **첫 번째** <instrumentation> 의 android:name 을
        // testInstrumentationRunner 값으로 덮어쓴다. 항목이 2개 이상이면 첫 항목과 같은 이름을 여기에 명시해 고정해야 한다
        // (16번 저장소 PR #7 에서 "Unable to find instrumentation info" 로 실제 겪음).

        // 사외: true (FakeVehiclePort) / 사내(local.properties 에 mobis.vss.jar): false (RealVehiclePort → VSSManager). 위 스위치가 정한다
        buildConfigField("boolean", "USE_FAKE_VSS", useFakeVss.toString())
        // Real 일 때 실물에서 안 오는 키만 Fake(시나리오)로 채운다 → 배지가 "실신호 2 · 시뮬레이션 6" 처럼 섞인다. 순수 Real 로 보려면 -PfillMissing=false
        buildConfigField("boolean", "FILL_MISSING_WITH_FAKE", (project.findProperty("fillMissing") ?: "true").toString())
        // 위치: "true" = 속도 적분 Fake(시연 시간 압축) / "false" = GPS 우선, 권한·제공자 없으면 Fake 폴백
        // 빌드 시 -PfakeLocation=false 로 바꿀 수 있다 (에뮬 geo fix 검증용)
        buildConfigField("boolean", "USE_FAKE_LOCATION", (project.findProperty("fakeLocation") ?: "true").toString())
        // TTS 음성 이름. 기본 ko-kr-x-kob-network(16번에서 사용자가 네 후보를 듣고 결정). 오프라인·미설치면 같은 계열 로컬(kob-local) → 로컬 최고 품질로 복귀.
        // 후보는 logcat "MOAH/AndroidTtsPort: ko voices". 로컬 자동 선택으로 돌리려면 -PttsVoice=auto
        buildConfigField("String", "TTS_VOICE", "\"${(project.findProperty("ttsVoice") ?: "ko-kr-x-kob-network").toString().let { if (it == "auto") "" else it }}\"")
        // Fake 신호 시나리오·Fake 위치의 시간 압축 배율. 주차 시나리오(잘한 26 s·못한 44 s)는 실시간이 맞아 기본 1.0.
        // 시연 시간이 모자라면 -PdemoSpeed=1.5. (16번 도로 주행은 12 km 라 10 이었다)
        buildConfigField("double", "DEMO_SPEED_FACTOR", (project.findProperty("demoSpeed") ?: "1.0").toString().toDouble().toString())
        // 시연 조작 패널(Fake 신호 버튼). 녹화·사내에서 화면에서 지우려면 -PdemoPanel=false — 그때는 스크립트도 못 누른다
        buildConfigField("boolean", "SHOW_DEMO_PANEL", (project.findProperty("demoPanel") ?: "true").toString())
        // AI 코치 전송 계층(Cloud Copilot). -PcloudCoach=false 면 항상 시드 문장. 설정 파일(/data/local/tmp/copilot_config.json)이 없어도 시드로 폴백한다
        buildConfigField("boolean", "CLOUD_COACH", (project.findProperty("cloudCoach") ?: "true").toString())
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

dependencies {
    compileOnly(vssApi)          // APK 에 포함되지 않음 (런타임은 시스템이 제공 — 매니페스트 <uses-library mobis.framework>)
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
