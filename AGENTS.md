# AGENTS.md — MOAH 2026 해커톤 앱 · 운전 연수 어시스턴트 (공통 지침)

Claude Code와 Codex가 공유하는 단일 지침. 세부 문서는 `docs/`.

## 프로젝트가 무엇인가

- MOAH 2026 (MOBIS SW Hackathon) 출품용 **Android Automotive OS(AAOS) 앱**. 1인 팀. 마감 **2026-10-09(금)**(10-07 아님, 9/30 사내 확인). 제출 4종: PPT 1장(주최 양식) · 팀 Bitbucket `submission` 브랜치 · MarketUploader APK(카테고리 VEHICLE, 재업로드마다 versionCode +1) · 에뮬 시연 영상 → PPT·영상은 MOAH@mobis.com. 제품명 **"드라이브 코치"** 1.0.0.
- **주제 (확정, 2026-09-25 피벗 · 9/26 v2): 운전 연수 어시스턴트 — "화내지 않는 조수석".** 초보·장롱면허 운전자가 두려운 상황을 **과제 단위**로 연습할 때,
  차량 신호로 **과정**을 측정해 **가이드 → 힌트 → 평가** 순으로 손을 떼 가며 코칭하고, 세션이 쌓이면 본인·동승자·(동의 시) 기관이 보는 진단 리포트가 된다.
  **시연 본편은 후면 직각 주차 과제.** 단계: `Setup(대화·제안) → Briefing → Maneuver(주차 중, 저속이라 화면 도식 허용) → Done("다 됐어요"·회차 멘트) → Report(운전석 도어 열림)`.
  도로 주행 과제는 `Maneuver` 자리에 `Driving`(잠금·음성만). 카메라 없음 — 칸에 반듯이 들어갔는지는 모른다고 정직하게 둔다. 상세: `docs/topics/01_driving_coach.md`.
- 이 저장소는 `C:\Project\16_hackathon`(Gift Drive, 2026-09-16~22)에서 **플랫폼 계층만 물려받아** 시작했다. 16번은 읽기 전용 참고용으로 남아 있다.
- **채점은 2층**: A층 = `Vehicle.Speed` 하나를 미분한 지표(급가속·급제동·과속·정속 편차) — **무조건 동작**. B층 = 도로주행시험 채점표 항목(안전벨트·기어·방향지시등·조향·비상등·시동) — **신호가 들어올 때만 활성화**.
  신호마다 `LIVE / SIMULATED / MISSING` 상태를 갖고, `MISSING` 은 채점에서 빼고 "미측정"으로 표시한다. **무엇이 실신호이고 무엇이 시뮬레이션인지 화면·리포트에 정직하게 구분해 보여준다** — 보여주는 자리는 셋(10/8 사용자 결정, 라운드 31a): 리포트 `자세히 보기` 의 집계 한 줄 · 출발 전 점검 카드의 항목별 기호 · 준비실/관리자 띠. 그 밖의 화면(홈·주차 중·Done·리포트 본문·진단서·공유 예시·주행)에는 집계 줄을 두지 않는다(운전자에겐 정보만 많다).
- **AI 경계**: 감점 순간은 규칙이 지연 0으로 TTS `URGENT` 발화. AI(`CoachPort`)는 구간 종료·정차·세션 종료의 총평과 **홈(정차) 코치 텍스트 대화**(10/6 — 문장 + 고정 목록의 의도만 고르고 화면 이동은 앱 규칙, `docs/design/12_home_coach_dialog.md`)만 맡고, 실패하면 규칙 문장으로 폴백한다. STT·주행 중 실시간 AI 발화는 넣지 않는다(`docs/NEXT.md` 미루는 항목).
- **외부 PC에서 개발 → GitHub **public** 저장소 → 사내에서 익명 clone → 검증·녹화·제출.** 사내 개발환경(WebIDE, infoLINK 에뮬, VSS 실물)은 외부에서 접근 불가. **저장소는 public 이 유일한 길이다**(10/1 확정 — 사내에서는 개인 GitHub 계정 로그인 자체가 안 돼 private 를 clone 할 수 없다). 그래서 **공개 저장소 규칙**: 사내 소스·jar·캡처·로그 원문·계정·토큰·`copilot_config.json` 은 절대 올리지 않고(기존 규칙 8), 사내 문서는 pageId 만 적고 **내부 호스트명·URL 은 적지 않는다**, 사내 관찰은 사람이 옮긴 요지만.
- 사내 빌드는 **코드 변경 없이** `local.properties` 의 `mobis.vss.jar=<jar 경로>` 한 줄 + jar 파일(`automotive/libs/`, gitignore)로 끝나야 한다(9/30 — 그 전엔 compileOnly 한 줄 교체였다). 같은 커밋이 사외(Fake)·사내(Real)에서 그대로 빌드된다. 이 원칙을 깨는 변경은 금지.
- **전 포트 Fake 로 인터넷·키·실신호 없이 전체 시연이 완결**되어야 한다. 사내 네트워크·TTS·신호 경로가 전부 미확인이기 때문이다.
- **전 범위 구현 원칙(2026-10-04 사용자 결정, `docs/design/10_full_scope.md`)**: 존재하는 신호·정밀도로 기능을 제약하지 않는다. 필요한 신호가 없으면 **시뮬레이션 신호를 정의해 구현**하고(실물에 없는 경로는 `vehicle/SimOnlySignals.kt`, 제휴 시험장 신호는 `Track.*`), 배지·출처에는 "시뮬레이션" 으로 정직하게 표시하며, 발표에서 실제 대응(RTK·검지 센서·시험장 API 등)을 설명한다. 주행 중 화면 규칙(절대 규칙 10)은 그대로.
- **두 자리 작업 방식(9/30 확정, `docs/07_two_site_workflow.md`)**: 사외 = 개발 전부(UI·상태기계·채점·프롬프트·테스트·문서, 완료 기준 Fake 전체 흐름 + 단위 테스트) / 사내 = 검증·녹화·제출뿐, **사내에서 코드를 고치지 않는다**. 사내 결과는 `INTEGRATION.md` B절 형식 관찰 노트로 사람이 요지만 옮기고, 사외 → 사내 전달 단위는 태그 `inhouse-YYYYMMDD-N`. 사내 세션 시작은 한 줄 — "`git fetch origin && git show origin/main:docs/handoffs/INHOUSE_NOW.md` 를 읽고 그대로 진행해"(경로 고정, **사외가 태그를 새로 만들 때마다 이 파일의 태그·새로 볼 것을 갱신**한다). 그 안의 순서: 태그 체크아웃 → jar 복사 → `REPO_ONLY=1 bash tools/inhouse_check.sh`(`.gitignore` 검사, 10/2 #100) → `local.properties` 한 줄 → 빌드 → `adb root` · Wi-Fi · `copilot_config.json` push · `logcat -G 16M` · `tools/inhouse_check.sh`. 태그는 문서에 전체 이름(`inhouse-20261002-3`)으로 — 짧은 `-N` 은 날짜가 다른 태그와 섞인다.

## 절대 규칙

1. 차량 데이터는 **오직 `VehiclePort`** (`automotive/src/main/kotlin/com/moah/hackathon/vehicle/VehiclePort.kt`)를 통해서만 접근한다. `mobis.vss.VSSManager`를 직접 참조하는 파일은 `RealVehiclePort.kt` 하나뿐이다.
2. `RemoteException`을 import/catch하지 않는다. VSS API는 unchecked `RuntimeException`을 던진다. (사내에서 "exception RemoteException is never thrown" 컴파일 에러)
3. VSS 값은 전부 `String`이다. `VssValues.kt`의 파서(`toVssFloat()` 등)를 쓰고 직접 캐스팅하지 않는다.
4. 신호 경로 문자열 리터럴 금지. `mobis.vss.VssConstants` 상수만 쓴다. 새 상수는 사내 문서(pageId 1323873443) 경로와 정확히 일치해야 하고, **이름은 경로를 그대로 대문자·밑줄로 바꾼 것**(`Vehicle.Cabin.Door.Row1.DriverSide.IsOpen` → `VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN` — 사내 jar 규칙, 2026-09-30)이어야 하며 추가 시 `docs/INTEGRATION.md` B절에 가정을 기록한다. 실물에 없는 경로(Fake 전용)는 `vehicle/SimOnlySignals.kt` 에 둔다. **경로가 틀려도 예외 없이 조용히 무시된다** — 그래서 모든 B층 신호는 `MISSING` 을 견디게 만든다.
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
- 빌드 플래그 7개(`automotive/build.gradle.kts`): `USE_FAKE_VSS`(하드코딩), `FILL_MISSING_WITH_FAKE`(`-PfillMissing`, Real 일 때 실물에서 안 오는 키만 Fake 로 — 배지가 "실신호 N · 시뮬 N" 로 섞임), `USE_FAKE_LOCATION`(`-PfakeLocation`), `TTS_VOICE`(`-PttsVoice`), `DEMO_SPEED_FACTOR`(`-PdemoSpeed`, 기본 1.0), `SHOW_DEMO_PANEL`(`-PdemoPanel`, 기본 true — false 면 시연 조작 패널을 그리지 않는다), `CLOUD_COACH`(`-PcloudCoach`, 기본 true — false 면 Copilot 인증·전송 계층을 만들지 않고 패널 AI 줄도 없다).
- **운전자에게 보이는·들리는 문장에는 숫자(횟수·초·점수)를 넣지 않는다**(2026-09-27). **경계(D1, 10/2)**: "문장" = TTS·회차 멘트·힌트·총평·가이드. 계기 값(속도·조향각·뒤 거리)·가이드 단계 `n/N`·퀴즈 문항 번호·교육 수치·회차/연차 메타·정차 선택 화면의 시간·거리·`자세히 보기`·진단서는 명시 예외(`docs/design/02_design_brief.md` 불변 표). 회차 멘트는 "서두.\n조언." 두 문장(`CoachPort` + `AdviceRules`) — **라운드 26(10/7)부터 코치 문장 = 제목 12자 · 한 마디 24자(대화 28자) 이내**, AI 응답도 같은 규칙으로 거른다(`CoachPrompts.titleAndLine`). 숫자는 리포트 "자세히 보기"·진단서에만.

## 코드 스타일

- Kotlin(앱) / Java(vss-stub만). Compose Material3. 코루틴/Flow.
- 패키지 루트 `com.moah.hackathon`. 하위: `vehicle`(포트), `ports`(음성·위치·코치), `scoring`(채점), `feature/<name>`(기능 단위), `ui`(화면).
- 화면은 2560×1440 가로 자동차 디스플레이 기준(`DesignScale` 이 2560×1268 dp 로 고정). 운전 중 주의분산 최소화(큰 글자, 적은 상호작용, 주행 중 음성이 본체).
- 로그 태그는 `Log.d("MOAH/<클래스>", …)`.
- 상태기계·채점 엔진은 `android.util.Log` 외 안드로이드 의존 없이 써서 JVM 유닛테스트로 돌린다.

## 브랜치 · 커밋

- `main`은 항상 빌드 통과. 작업은 `claude/<topic>` / `codex/<topic>` 브랜치 → PR → Claude 리뷰 → squash merge. **머지 승인 없이 다음 작업을 쌓지 않는다.**
- 커밋 규칙(2026-09-30, 사내 브랜치에 먼저 넣은 것과 같은 내용 — `docs/07_two_site_workflow.md` §5):
  1. 한 커밋 = 한 논리 변경. 제목 `<scope>: <무엇을 어떻게>` — 한국어 50자 안팎, 마침표 없음, `fix`/`update`/`wip` 금지. scope: build, stub, vehicle, ports, scoring, feature, ui, data, docs, test, tools, **inhouse**(사내 전용 변경 표시).
  2. 본문 `- ` 불릿 2~5개(무엇·왜·영향), 끝에 `검증: 빌드 · 단위 테스트 N`(+ 실제로 한 확인만).
  3. 코드+테스트+관련 문서는 같은 커밋. 이름 바꾸기·포맷팅과 동작 변경은 분리.
  4. 모든 커밋이 빌드·테스트 통과.
  5. push 전 실험·진단 커밋은 `git commit --fixup` → `GIT_SEQUENCE_EDITOR=: git rebase -i --autosquash <기준>` 으로 합친다. push 뒤에는 이력 재작성·force push 금지.
  6. PR 제목 = squash 커밋 제목(같은 형식).
  7. `submission` 브랜치 = 사외 태그 + `inhouse:` 커밋 1개(사내 jar).
  8. **PR 하나 = 한 scope · 한 변경**(10/1 사내 검증 #2에서 지적 — A+B 를 한 PR 에 넣지 않는다). PR 본문 = 커밋 본문 그대로(`- ` 불릿 2~5 + 마지막 `검증:` 줄, `## 검증` 헤더 없이). 이미 머지된 것은 다시 쓰지 않는다.

## 참고 문서

- **`docs/NEXT.md` — 새 세션은 여기서 시작**(지금 되는 것, 남은 일, 일하는 방식, 파일 지도). 작업을 마칠 때마다 갱신한다.
- `docs/00_hackathon_overview.md` — 해커톤 개요·제출·정책
- `docs/01_external_dev_guide.md` — 외부 개발 가이드 전문
- `docs/02_vss_api_contract.md` — VSS API 시그니처·타입·함정
- `docs/03_environment.md` — 이 PC 환경·에뮬
- `docs/04_agent_workflow.md` — 분담·핸드오프 템플릿·리뷰 체크리스트
- `docs/06_inhouse_migration.md` — 사내 이관 런북 · `docs/07_two_site_workflow.md` — 사외/사내 역할·태그·커밋 규칙
- `docs/INTEGRATION.md` — 사내 수렴 체크리스트·가정 로그·요청
- `docs/topics/01_driving_coach.md` — 확정 주제 정의·채점 설계·신호 표. `_comparison.md` 에 피벗 결정 기록
- `docs/05_demo_script.md` — 시연 대본(실측 시각·시간 조절·사고 대응). `docs/presentation/` — 발표 덱 구성·시연 영상 컷 목록
- `docs/journal/` — 개발일지(16번 9/16~9/22 포함, 9/25 피벗)
