# Codex 오더 — 연수 세션 화면 5장 + 시연 조작 패널 (2026-09-26)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md 와 docs/topics/01_driving_coach.md 를 읽어라.
브랜치: codex/lesson-screens 를 main 에서 새로 만들어 작업.
작업: 아래 "화면" 절의 5화면 + 시연 조작 패널을 구현하고 MainActivity 를 Dashboard 에서 LessonRoute 로 바꾼다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/SeedCatalog.kt 의 **구조**, build 파일은 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
       시드 **문구**(SeedCatalog 의 멘트·과제 설명·질문)는 다듬어도 된다.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest 통과(PowerShell), 에뮬 CSTDe_API_34 에서 5화면 캡처를 PR 에 첨부, gh pr create.
```

## 왜 이 화면들인가 (한 문단)

"화내지 않는 조수석". 초보·장롱면허 운전자가 **후면 직각 주차**를 연습한다. 정차 중에 대화로 설정하고(`Setup`), 한 줄 고지(`Briefing`) 뒤 주차한다(`Maneuver`). 주차는 5 km/h 미만이라 **화면을 봐도 되는 유일한 주행 구간** — 위에서 본 차 도식에 조향각·기어·뒤 거리를 그린다. 운전자가 "다 됐어요"를 누르면 회차 멘트(`Done`), 세우고 내리면(정차 + 운전석 도어 열림) 진단 리포트(`Report`). **주차 중 화면에는 점수·감점 누계가 없다** — 운전자가 화면을 보게 만드는 값은 `ManeuverDisplayState` 타입에서부터 빠져 있다.

## 데이터 계약 — 화면은 이것만 본다

- `LessonViewModel` (`feature/lesson/LessonViewModel.kt`) — `LessonViewModel.factory(container)` 로 만든다.
  - `phase: StateFlow<LessonPhase>` — 아래 5단계 중 하나
  - `subtitle: StateFlow<String?>` — 마지막으로 **말하기 시작한** 문장(자막). 모든 화면에 큰 글자로.
  - 진입점: `begin(taskId, mode)` / `finishAttempt()` / `nextAttempt()` / `endSession()` / `restart()`
  - `demo: DemoControls?` — **Fake 차량일 때만 non-null.** null 이면 패널을 그리지 않는다(사내 Real 에서 버튼이 보이면 Real 이 아니다).
    `demo.scenarios`(id·title), `demo.playback`(재생 상태), `demo.play(id)`, `demo.stopScenario()`, `demo.stopCar()`, `demo.resumeCar()`, `demo.setDoor(open)`
- `LessonPhase` (`feature/lesson/LessonPhase.kt`)

| 단계 | 필드 | 화면이 할 일 |
|---|---|---|
| `Setup(profile, tasks, suggestedTask, suggestedMode, reason, reservation)` | | 앱의 제안을 크게("후면 직각 주차 · 가이드 모드 — {reason}"), 과제 목록·모드 4개 중 고르기, `reservation` 카드 1장(시드, "실제 예약 연계 없음" 문구 포함), 프로필 요약 한 줄("{name} · 장롱 {rustyYears}년차 · 목표 {goal}"). **시작** 버튼 → `begin(task.id, mode)` |
| `Briefing(task, mode, line)` | | `line` 을 큰 글자로. 터치 없음. 2.5 초 뒤 자동 전환 |
| `Maneuver(...)` → **`toDisplayState()`** (`feature/lesson/ManeuverDisplayState.kt`) | `speed, steeringDeg, gear, rearDistanceCm, obstacleWarning, guideText, guideStep, hintText, attempt, movingSegments, elapsedSeconds, askedDone, steeringSignal/gearSignal/distanceSignal` | 도식(아래) + 자막. **`snapshot.locked`(속도 > 5) 이면 터치 타깃 0개.** 정차 중이면 **다 됐어요** 버튼 → `finishAttempt()`. `askedDone` 이면 버튼을 강조 |
| `Done(task, mode, attempt, record)` | `record.score.skill/safety`, `record.score.metrics`, `record.delta`, `record.remark` | 회차 요약: 멘트(`remark`) 크게, 이동 {movingSegments}회 · {totalMillis/1000}초, 지난번보다(`delta`: 음수 = 좋아짐, null 이면 숨김). **한 번 더** → `nextAttempt()`, **오늘은 여기까지** → `endSession()` |
| `Report(report)` | `LessonReport`: `summary, attempts, best, nextTask, nextMode, nextReason, shareLevels, benefits, unverifiedGuideSteps` | 아래 "리포트" 절 |

- 신호 칩 색: `SignalAvailability.LIVE` 실신호 / `SIMULATED` "시뮬" / `MISSING` "미측정". 도식의 조향·기어·거리 옆에 각각.

## 화면

### 1. Setup — 정차 중 대화형 설정
- 상단: "오늘은 뭘 해볼까요?" + 앱 제안 카드(과제 제목·모드·`reason`). **시작** = 제안대로.
- 과제 목록(`tasks`, 난이도 `Difficulty.label` 하/중/상, `type`), 모드 4개(`LessonMode.label`). 고르면 제안 카드가 바뀐다.
- 예약 카드(`reservation`: venue·slot·course·note). 클릭 없음.
- 프로필 한 줄. 첫 설정 5문항 화면은 **이번 오더에서 제외**(시드 프로필 사용).

### 2. Briefing
- `line` 한 문장, 큰 글자, 배경만. 브랜드 라벨(`CoachStyle.LessonCanvas`).

### 3. Maneuver — 도식 (`Canvas`)
- 위에서 본 차(직사각형) 중앙, 뒤쪽이 화면 위. 앞바퀴 두 개를 `steeringDeg` 로 회전(양수 = 왼쪽, COVESA). 각도 숫자도 옆에.
- 기어 `gear`("P/R/N/D") 큼직하게. R 이면 차 뒤로 화살표.
- 뒤 거리 `rearDistanceCm` — 차 뒤에 거리 막대(250 cm 가득 → 0). 40 cm 미만 또는 `obstacleWarning` 이면 붉게 + 접근성 텍스트 "뒤 {n} cm".
- 좌측 상단: 회차 `attempt`, 이동 `movingSegments`회, `elapsedSeconds`. **점수는 없다(있을 수 없다).**
- 가이드 모드: `guideText` 를 자막 위에 한 줄 + `guideStep`("3/6"). 힌트 모드: `hintText` 를 잠깐(4 s) 띄우고 사라짐.
- 자막(`subtitle`) 항상 하단 큰 글자. `Canvas` 에는 `contentDescription` 을 넣는다(계측이 접근성 트리로 검사).
- 정차 중(`speed` 가 "0"~"1")에만 **다 됐어요** 버튼. `askedDone` 이면 펄스.

### 4. Done
- `remark` 가 주인공(큰 글자). 아래 작은 표: 이동 횟수·시간·(있으면) 조향 왕복·기어 전환·뒤 최소 거리.
- `delta` 가 있으면 "지난번보다 이동 {n}회 ↓" 식. 좋아진 것만 초록, 나빠진 것은 회색(붉게 하지 않는다 — 톤 규칙).
- **한 번 더** / **오늘은 여기까지**.

### 5. Report — 진단 리포트 + 진단서 탭
- 탭 1 **오늘**: `summary`, 숙련 `best.skill` · 안전 `best.safety` 두 축(원형 게이지 2개), 회차 표(`attempts`: 회차·숙련·안전·이동·시간), **배지** "실신호 {live} · 시뮬레이션 {simulated} · 미측정 {missing}"(`best.badge`), `best.missingSignals` 가 있으면 "이 신호는 이 차에서 받지 못했어요" 목록, `unverifiedGuideSteps` 가 있으면 "이 단계는 확인할 수 없었어요".
  다음 제안: "{nextTask.title} · {nextMode.label} — {nextReason}". **다시 시작** → `restart()`.
- 탭 2 **진단서**(2순위 서사): 공유 범위 3단계(`shareLevels`: label·description) 라디오, 예상 혜택(`benefits`) 목록, 상단에 **"예시입니다 — 실제 전송·계약은 없습니다"** 고정 문구. 공유 버튼은 누르면 토스트 "예시 화면입니다"만.

### 시연 조작 패널 (Fake 일 때만, 우상단 접이식)
- 접힌 상태의 토글 버튼 라벨은 **시연**. 펼치면 아래 버튼들이 보인다. 기본은 **펼친 상태**(시연 중 한 번 덜 누르게).
- `demo.scenarios` 마다 버튼(제목: "잘한 주차", "못한 주차") → `demo.play(id)`. `demo.playback` 으로 진행 표시(`stepIndex/stepCount`).
- **정차** `stopCar()` / **출발** `resumeCar()` / **문 열기** `setDoor(true)` / **문 닫기** `setDoor(false)` / **시나리오 정지** `stopScenario()`.
- 라벨은 `res/values/strings.xml` 에 두고 **정확히 이 문자열**로(`tools/emu_flow.sh` 가 라벨로 탭한다): 시작 / 다 됐어요 / 한 번 더 / 오늘은 여기까지 / 다시 시작 / 시연 / 잘한 주차 / 못한 주차 / 정차 / 출발 / 문 열기 / 문 닫기 / 시나리오 정지. 모드 선택 버튼은 `LessonMode.label`(가이드 / 힌트 / 평가 / 지식 테스트) 그대로.
- 리포트 화면에는 배지 문자열 "실신호 N · 시뮬레이션 N · 미측정 N" 과 **다시 시작** 이 있어야 한다(스크립트가 이 둘로 도달을 판정).
- 계측 클래스 이름은 `com.moah.hackathon.ui.LessonScreenInstrumentation`, 캡처 파일명은 `files/lesson-<화면>.png`, 통과 시 `Lesson contract passed` 를 출력 — `tools/lesson_shots.sh` 가 그대로 기다린다.

## 규칙 (16번에서 배운 것, 그대로)
- 화면 composable 은 `LessonPhase` 를 직접 받지 말고 각 단계 데이터(또는 `toDisplayState()`)만 받는다 — 미리보기·테스트가 쉬워진다.
- `DesignScale` 안에서 그린다(2560×1268 dp 고정). 글자 하한 32 sp. 시스템 기본 글꼴만. 한국어 `LineBreak`/`wordBreak = Phrase`.
- 폴더명 = 패키지명(하이픈 없음). `ui/lesson/` 아래 `SetupScreen.kt`, `BriefingScreen.kt`, `ManeuverScreen.kt`, `DoneScreen.kt`, `ReportScreen.kt`, `DemoPanel.kt`, `LessonRoute.kt`.
- 색은 `ui/CoachStyle.kt` 의 `CoachColors`. 바꾸고 싶으면 그 파일에서.
- 큰 빈 영역 금지. 자막 말줄임 금지(네 줄까지 받는다).
- `MainActivity`: `DashboardScreen` → `LessonRoute(vm)`. Dashboard 파일은 지우지 말고 남긴다(부트스트랩 검증용).

## 테스트 (Codex 소유)
- `src/test/`: 각 화면의 표시 매핑 유닛테스트(예: `Done` 에서 `delta == null` 이면 "지난번보다" 가 없다).
- `src/androidTest/`: 계측 1개 `LessonScreenInstrumentation` — (a) `Maneuver` 에서 `locked` 이면 클릭 가능한 노드 0개, (b) `Maneuver` 접근성 트리에 "점수"·"감점"·숫자 뒤 "점" 문자열 없음, (c) 5화면 캡처를 `filesDir` 에 저장. **매니페스트 `<instrumentation>` 을 넣을 때 `build.gradle.kts` 의 `testInstrumentationRunner` 주석을 먼저 읽는다**(AGP 가 첫 항목 이름을 덮어쓴다).

## 참고
- 시연 시나리오 6단계: `docs/topics/01_driving_coach.md` §4.5. 잘한 주차는 26 s(숙련 100·안전 100), 못한 주차는 44 s(60·55).
- 16번의 d-road 화면(`git -C /c/Project/16_hackathon show 0c95d18:automotive/src/main/kotlin/com/moah/hackathon/ui/concepts/droad/RoadStyle.kt`)에서 자막 판·헤더 스타일을 참고해도 된다. 가져올 때는 `feature/journey` 의존을 걷어낸다.
