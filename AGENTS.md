# AGENTS.md — MOAH 2026 해커톤 앱 · 운전 연수 어시스턴트 (공통 지침)

Claude Code와 Codex가 공유하는 단일 지침. 세부 문서는 `docs/`.

## 프로젝트가 무엇인가

- MOAH 2026 (MOBIS SW Hackathon) 출품용 **Android Automotive OS(AAOS) 앱**. 1인 팀. 마감 2026-10-07.
- **주제 (확정, 2026-09-25 피벗): 운전 연수 어시스턴트** — 초보운전자가 혼자 도로에 나갈 때, 차량 신호로 운전 행위를 정량 측정해
  **주행 중에는 음성으로 즉시 코칭**하고, **세우고 내릴 때(속도<1 且 운전석 도어 열림) 능력 진단 리포트**를 펼친다.
  동승자가 있어도 앱이 강사다. 단계: `Setup → Briefing → [Driving → SegmentReview] × 구간 → Report`. 상세: `docs/topics/01_driving_coach.md`.
- 이 저장소는 `C:\Project\16_hackathon`(Gift Drive, 2026-09-16~22)에서 **플랫폼 계층만 물려받아** 시작했다. 16번은 읽기 전용 참고용으로 남아 있다.
- **채점은 2층**: A층 = `Vehicle.Speed` 하나를 미분한 지표(급가속·급제동·과속·정속 편차) — **무조건 동작**. B층 = 도로주행시험 채점표 항목(안전벨트·기어·방향지시등·조향·비상등·시동) — **신호가 들어올 때만 활성화**.
  신호마다 `LIVE / SIMULATED / MISSING` 상태를 갖고, `MISSING` 은 채점에서 빼고 "미측정"으로 표시한다. **무엇이 실신호이고 무엇이 시뮬레이션인지 화면·리포트에 정직하게 구분해 보여준다.**
- **AI 경계**: 감점 순간은 규칙이 지연 0으로 TTS `URGENT` 발화. AI(`CoachPort`)는 구간 종료·정차·세션 종료의 총평만 맡고, 실패하면 규칙 문장으로 폴백한다. STT·주행 중 실시간 AI 발화는 넣지 않는다(`docs/NEXT.md` 미루는 항목).
- **외부 PC에서 개발 → GitHub private → 사내에서 clone → 머지·빌드·시연.** 사내 개발환경(WebIDE, infoLINK 에뮬, VSS 실물)은 외부에서 접근 불가.
- 사내 머지는 `automotive/build.gradle.kts`의 **compileOnly 한 줄 교체 + `USE_FAKE_VSS` 플래그 하나**로 끝나야 한다. 이 원칙을 깨는 변경은 금지.
- **전 포트 Fake 로 인터넷·키·실신호 없이 전체 시연이 완결**되어야 한다. 사내 네트워크·TTS·신호 경로가 전부 미확인이기 때문이다.

## 절대 규칙

1. 차량 데이터는 **오직 `VehiclePort`** (`automotive/src/main/kotlin/com/moah/hackathon/vehicle/VehiclePort.kt`)를 통해서만 접근한다. `mobis.vss.VSSManager`를 직접 참조하는 파일은 `RealVehiclePort.kt` 하나뿐이다.
2. `RemoteException`을 import/catch하지 않는다. VSS API는 unchecked `RuntimeException`을 던진다. (사내에서 "exception RemoteException is never thrown" 컴파일 에러)
3. VSS 값은 전부 `String`이다. `VssValues.kt`의 파서(`toVssFloat()` 등)를 쓰고 직접 캐스팅하지 않는다.
4. 신호 경로 문자열 리터럴 금지. `mobis.vss.VssConstants` 상수만 쓴다. 새 상수는 사내 문서(pageId 1323873443) 경로와 정확히 일치해야 하며 추가 시 `docs/INTEGRATION.md` B절에 가정을 기록한다. **경로가 틀려도 예외 없이 조용히 무시된다** — 그래서 모든 B층 신호는 `MISSING` 을 견디게 만든다.
5. `getVSS/setVSS`는 동기·블로킹이다. UI 스레드에서 호출하지 않는다 (RealVehiclePort가 전용 스레드로 처리하므로 VehiclePort 호출자는 suspend/콜백 규약을 지킨다).
6. 구독은 반드시 해제한다. 화면/뷰모델 수명 종료 시 `VehiclePort.dispose()` 또는 `observe`가 반환한 핸들을 닫는다.
7. `vss-stub/` 시그니처는 `docs/02_vss_api_contract.md`와 문자 그대로 일치해야 한다. 스텁 메서드에 실제 동작을 구현하지 않는다(외부에서 실행되면 즉시 예외로 드러나야 함).
8. 사내 소스(Template App, M.ADI, copilot_reference_app)를 이 저장소에 넣지 않는다. `tmp-info/`, `emulator/`는 git 추적 대상이 아니다(16번 폴더에 있다).
9. 외부에서 검증할 수 없는 가정을 코드에 넣을 때마다 `docs/INTEGRATION.md` B절에 한 줄 추가한다.
10. **주행 중(속도 > 5 km/h) 화면에는 점수·감점 누계·터치 타깃을 두지 않는다.** 점수를 보면 운전자가 화면을 본다. 화면은 `DisplayState` 매퍼가 걸러 준 값만 받는다.

## 소유권 (누가 어디를 고치는가)

| 영역 | 소유 |
|---|---|
| Gradle 파일 전부, `vss-stub/`, `automotive/.../vehicle/`, `.../ports/`, `.../scoring/`(채점 엔진), `.../data/`(시드 구조), `.../feature/lesson/`(상태기계·ViewModel), `App.kt`, `docs/`, `AGENTS.md`, `CLAUDE.md` | Claude Code |
| `tools/`, 계측 계약(`androidTest/` 의 공통 계약 클래스) | Claude Code |
| `automotive/.../ui/`(화면 전부), `.../feature/setup/`, `.../feature/driving/`, `.../feature/report/`, `res/`, 시드 데이터 **내용**(문구·코스 좌표), `src/test/`(화면·기능), `src/androidTest/`(화면 계측) | Codex |
| 코드 리뷰·PR 머지·사내 이관 | Claude Code |

화면이 필요로 하는 데이터는 전부 `LessonPhase`(단계별 값)와 `LessonViewModel.subtitle`(마지막 TTS 문장)에 있다. 화면은 ViewModel 의 진입점(시작·다음 구간·다시 시작·시연 조작)만 호출한다.
상대 영역이 필요하면 고치지 말고 `docs/INTEGRATION.md` C절에 요청을 적는다. 상세: `docs/04_agent_workflow.md`.

## 빌드 · 테스트

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
# APK: automotive\build\outputs\apk\debug\automotive-debug.apk
```

- 툴체인: JDK 17, AGP 8.7.3, Kotlin 2.0.21, Compose, compileSdk 35 / targetSdk 34 / minSdk 29. 버전은 `gradle/libs.versions.toml`.
- Android Studio 없음. 에뮬은 16번과 같은 AVD `CSTDe_API_34`(`docs/03_environment.md`).
- 완료 기준은 항상 "빌드+단위테스트 통과". 깨진 상태로 커밋하지 않는다.
- 빌드 플래그 4개(`automotive/build.gradle.kts`): `USE_FAKE_VSS`(하드코딩), `USE_FAKE_LOCATION`(`-PfakeLocation`), `TTS_VOICE`(`-PttsVoice`), `DEMO_SPEED_FACTOR`(`-PdemoSpeed`).

## 코드 스타일

- Kotlin(앱) / Java(vss-stub만). Compose Material3. 코루틴/Flow.
- 패키지 루트 `com.moah.hackathon`. 하위: `vehicle`(포트), `ports`(음성·위치·코치), `scoring`(채점), `feature/<name>`(기능 단위), `ui`(화면).
- 화면은 2560×1440 가로 자동차 디스플레이 기준(`DesignScale` 이 2560×1268 dp 로 고정). 운전 중 주의분산 최소화(큰 글자, 적은 상호작용, 주행 중 음성이 본체).
- 로그 태그는 `Log.d("MOAH/<클래스>", …)`.
- 상태기계·채점 엔진은 `android.util.Log` 외 안드로이드 의존 없이 써서 JVM 유닛테스트로 돌린다.

## 브랜치 · 커밋

- `main`은 항상 빌드 통과. 작업은 `claude/<topic>` / `codex/<topic>` 브랜치 → PR → Claude 리뷰 → squash merge. **머지 승인 없이 다음 작업을 쌓지 않는다.**
- 커밋 메시지 `<scope>: <요약>` (scope: build, stub, vehicle, ports, scoring, feature, ui, data, docs, test, tools).

## 참고 문서

- **`docs/NEXT.md` — 새 세션은 여기서 시작**(지금 되는 것, 남은 일, 일하는 방식, 파일 지도). 작업을 마칠 때마다 갱신한다.
- `docs/00_hackathon_overview.md` — 해커톤 개요·제출·정책
- `docs/01_external_dev_guide.md` — 외부 개발 가이드 전문
- `docs/02_vss_api_contract.md` — VSS API 시그니처·타입·함정
- `docs/03_environment.md` — 이 PC 환경·에뮬
- `docs/04_agent_workflow.md` — 분담·핸드오프 템플릿·리뷰 체크리스트
- `docs/06_inhouse_migration.md` — 사내 이관 런북
- `docs/INTEGRATION.md` — 사내 수렴 체크리스트·가정 로그·요청
- `docs/topics/01_driving_coach.md` — 확정 주제 정의·채점 설계·신호 표. `_comparison.md` 에 피벗 결정 기록
- `docs/journal/` — 개발일지(16번 9/16~9/22 포함, 9/25 피벗)
