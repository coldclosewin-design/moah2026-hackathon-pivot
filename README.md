# 드라이브 코치 — 화내지 않는 조수석 (MOAH 2026 Hackathon App)

MOAH 2026 (MOBIS SW Hackathon) 출품용 Android Automotive OS 앱. 초보·장롱면허 운전자가 두려운 상황(후면 직각 주차·출발 전 점검·지식 테스트)을 **과제 단위**로 연습할 때, 차량 신호로 **과정**을 측정해 가이드 → 힌트 → 평가 순으로 손을 떼 가며 코칭하고, 세우고 내리면 진단 리포트를 펼친다. 카메라는 없다 — 칸에 반듯이 들어갔는지는 모른다고 정직하게 둔다. **외부 PC 에서 개발(이 public 저장소) → 사내에서는 검증·녹화·제출만**(코드를 고치지 않는다, `docs/07_two_site_workflow.md`).

## 처음 받는 사람이 10분 안에 돌리려면

- 준비물: **JDK 17**(`JAVA_HOME`), **Android SDK**(`ANDROID_HOME` 또는 `local.properties` 의 `sdk.dir`) — 플랫폼 35 · Build-Tools 34/35. Android Studio 는 없어도 된다(Gradle CLI).
- 사외(Fake): 아래 "빌드" 한 명령 → APK 가 나오면 끝. 에뮬(AAOS `CSTDe_API_34`)에 `adb install -r` 하고 런처에서 **"드라이브 코치"** 를 열면 Setup 화면("오늘은 가볍게, 주차부터 해볼까요?")이 뜬다. 인터넷·키·실신호 없이 전체 시연이 된다(전 포트 Fake).
- 사내(Real): 아래 "사내 빌드" 의 설정 한 줄 + jar. 절차 전체는 `docs/06_inhouse_migration.md`.

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
automotive/   AAOS 앱 (Kotlin, Compose). vehicle/ = VehiclePort + Fake/Real/Hybrid · ports/ = 음성(TTS)·위치·코치(Cloud Copilot + 시드 폴백, ports/copilot/) · scoring/ = 채점(A층 속도 미분 + B층 점검표) · feature/lesson/ = 세션 상태기계·ViewModel · data/ = 시드(과제·가이드·멘트·퀴즈) · ui/ = 화면(Setup·Briefing·Maneuver·Done·Report·Quiz·시연 패널)
vss-stub/     mobis.vss 컴파일 전용 스텁 (사내에서는 local.properties 의 mobis.vss.jar 로 시스템 jar 사용 — 코드 변경 없음)
tools/        에뮬 자동 시연(emu_flow.sh)·화면 계약 캡처(lesson_shots.sh)·사내 검증(inhouse_check.sh) 스크립트 — tools/README.md
docs/         가이드·계약·체크리스트·일지·발주서·감사·발표 원고
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
