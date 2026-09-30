import java.util.Properties

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

// ───────── 사내/사외 스위치 (2026-09-30, 사내 이관 1차 결과) ─────────
// `local.properties` 에 `mobis.vss.jar=<사내 jar 경로>` 가 있으면 Real(시스템 jar) 빌드, 없으면 Fake(스텁) 빌드.
// 같은 커밋이 양쪽에서 빌드된다 — 사내는 추적 안 되는 파일(local.properties · automotive/libs/)만 둔다.
// automotive/build.gradle.kts 가 같은 키를 읽어 compileOnly 소스와 USE_FAKE_VSS 를 정한다.
val mobisVssJar: String? = File(rootDir, "local.properties").takeIf { it.isFile }?.let { f ->
    Properties().apply { f.inputStream().use { load(it) } }.getProperty("mobis.vss.jar")?.trim()?.takeIf { it.isNotEmpty() }
}
if (mobisVssJar == null) include(":vss-stub")
