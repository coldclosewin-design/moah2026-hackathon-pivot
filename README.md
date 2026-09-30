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
vss-stub/     mobis.vss 컴파일 전용 스텁 (사내에서는 local.properties 의 mobis.vss.jar 로 시스템 jar 사용 — 코드 변경 없음)
tools/        에뮬 자동 시연·캡처 스크립트 (새 흐름에 맞춰 수정 예정)
docs/         가이드·계약·체크리스트·일지
```

## 사내 빌드 (설정 한 줄 + jar 파일)

코드는 고치지 않는다. 저장소 밖(추적 안 되는) 파일 둘만 둔다:

1. 사내 jar 를 `automotive/libs/mobis.framework.core.jar` 에 복사(경로는 자유, `automotive/libs/` 는 gitignore).
2. `local.properties` 에 한 줄:

```properties
mobis.vss.jar=automotive/libs/mobis.framework.core.jar
```

이 키가 있으면 `settings.gradle.kts` 가 `:vss-stub` 을 빼고, `automotive/build.gradle.kts` 가 그 jar 로 `compileOnly` 하며 `USE_FAKE_VSS=false`(RealVehiclePort) 로 빌드한다. 키가 없으면 사외와 똑같이 스텁·Fake. 빌드 로그 첫 줄 `mobis.vss: …` 로 어느 쪽인지 확인한다.

전체 절차는 [docs/06_inhouse_migration.md](docs/06_inhouse_migration.md), 체크 항목·가정 로그는 [docs/INTEGRATION.md](docs/INTEGRATION.md), 사외/사내 역할은 [docs/07_two_site_workflow.md](docs/07_two_site_workflow.md).
