// mobis.vss 컴파일 전용 스텁. 사내에서는 이 모듈을 쓰지 않고
// compileOnly(files("/system/framework/mobis.framework.core.jar")) 로 교체한다.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "mobis.vss"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
