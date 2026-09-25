pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "moah2026-pivot"

include(":automotive")
// 사내 머지 시: 아래 한 줄을 제거(또는 주석)하고 automotive/build.gradle.kts 의 compileOnly 를 시스템 jar 로 교체
include(":vss-stub")
