# MOAH 2026 Hackathon App — 운전 연수 어시스턴트 (가칭 DriveCoach)

MOAH 2026 (MOBIS SW Hackathon) 출품용 Android Automotive OS 앱. 초보운전자가 혼자 도로에 나갈 때 차량 신호로 운전 행위를 측정해 **주행 중엔 음성으로 코칭**, **세우고 내릴 때 진단 리포트**를 펼친다. 외부 PC에서 개발하고 사내에서 머지·빌드·시연한다.

2026-09-25 에 `16_hackathon`(Gift Drive)에서 플랫폼 계층만 물려받아 아이템을 전면 교체했다. 경위는 [docs/journal/2026-09-25.md](docs/journal/2026-09-25.md), 주제 정의는 [docs/topics/01_driving_coach.md](docs/topics/01_driving_coach.md).

- 개발 지침: [AGENTS.md](AGENTS.md)
- 새 세션 시작점: [docs/NEXT.md](docs/NEXT.md)
- 문서: [docs/](docs/) — 개요, 외부 개발 가이드, VSS API 계약, 환경, 에이전트 분담, [INTEGRATION.md](docs/INTEGRATION.md)

## 빌드

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

APK: `automotive/build/outputs/apk/debug/automotive-debug.apk`

## 구조

```
automotive/   AAOS 앱 (Kotlin, Compose). vehicle/ = VehiclePort + Fake/Real · ports/ = 음성·위치 · scoring/ = 채점(예정) · feature/lesson/ = 세션 상태기계(예정)
vss-stub/     mobis.vss 컴파일 전용 스텁 (사내에서는 시스템 jar로 교체)
tools/        에뮬 자동 시연·캡처 스크립트 (새 흐름에 맞춰 수정 예정)
docs/         가이드·계약·체크리스트·일지
```

## 사내 머지 (두 줄)

`automotive/build.gradle.kts`:

```kotlin
// val vssApi: Any = project(":vss-stub")
val vssApi: Any = files("/system/framework/mobis.framework.core.jar")
buildConfigField("boolean", "USE_FAKE_VSS", "false")
```

`settings.gradle.kts`에서 `include(":vss-stub")` 제거.

전체 절차는 [docs/06_inhouse_migration.md](docs/06_inhouse_migration.md), 체크 항목·가정 로그는 [docs/INTEGRATION.md](docs/INTEGRATION.md).
