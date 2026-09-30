# Codex 오더 — UI 라운드 9: 사내 피드백 전 자체 점검 (2026-09-30)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/07_round9_selfcheck.md(점검 목록·분류·근거 캡처), 라운드 8 발주서(면 카드·정착 폴링)를 읽어라.
브랜치: codex/ui-round9 를 origin/main 에서 새로 만들어 작업(이 발주서 PR 머지 뒤).
작업: §1(규칙 위반 1건 — 반드시) → §2(화면 다듬기) → §3(계측 흔들림). §4 는 결정이 적힌 것만.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed"(리뷰 환경에서 3회 연속), bash tools/emu_flow.sh → result: PASS(원본),
          docs/screenshots/lesson/ 의 해당 캡처 교체(§별 목록), gh pr create.
```

## 0. 왜

사내 이관 1차 성공(9/29) 뒤 사내 피드백이 오기 전에, 지금 `main` 의 화면을 캡처·코드·에뮬 워크스루로 한 바퀴 점검했다(`docs/design/07_round9_selfcheck.md`). 규칙 위반 1건과 다듬을 곳을 모았다. 사내 피드백이 먼저 오면 그것을 우선하고 이 목록은 뒤에 붙인다.

## 1. 규칙 위반 — 반드시

### 1.1 지식 테스트 잠금 화면의 "맞은 문제 N" (`QuizScreen.kt`)
- 지금: 잠금(주행 중, 5 km/h 초과)이어도 하단 행에 `맞은 문제 0` 이 남는다. AGENTS 규칙 10(주행 중 점수·누계 없음) 위반.
- 바꿀 것: `locked` 면 하단 행 전체(`그만하기`·`맞은 문제`)를 그리지 않는다. 구분선(`PosterRule`)도 같이.
- 계측: 잠금 렌더에서 `Regex("\\d")` 가 문제 번호 패널(왼쪽) 밖 텍스트에 없음. `lesson-quiz-locked.png` 교체.

## 2. 화면 다듬기

### 2.1 "미측정" 이중 표기 (`ManeuverScreen.kt`)
- 지금: 신호가 없으면 값 자리 `미측정` + 그 아래 출처 칸 `미측정` 이 겹친다(조향각·기어·뒤 거리).
- 바꿀 것: 값이 `미측정` 이면 출처 칸을 그리지 않는다(`SignalValue` 와 조향각 블록). 점검 칩(`ChecklistValue`)은 이미 이 규칙이다.
- 캡처 `lesson-missing.png` 교체. 계측: 한 칸 안에 `미측정` 텍스트 노드가 1개.

### 2.2 잠금 화면의 남은 눈썹 (`ManeuverScreen.kt`)
- 지금: 주행 중 잠금이면 도식이 사라지는데 왼쪽 아래 `조향 방향 도식` 눈썹은 그대로.
- 바꿀 것: 잠금이면 이 눈썹을 그리지 않는다. `lesson-locked.png` 교체.

### 2.3 출발 전 점검 Done 왼쪽 (`DoneScreen.kt`)
- 지금: 점검은 궤적이 없어 왼쪽이 빈 남색 띠(12 %)뿐.
- 바꿀 것: 점검 과제면 왼쪽 38 % 를 남색 패널로 하고 **일곱 항목 요약**을 세로로 — 항목명 + `✓`(Periwinkle) / `✗`(Signal) / `미측정`(Muted). 데이터는 리포트와 같은 `checklistResults(record.score)` 를 쓴다(숫자 없이 — 벨트·시동 초 표시는 빼고 기호만). 주차 과제·궤적 없음(이동 0.5 m 미만)은 지금대로.
- 캡처 `lesson-done-checklist.png` 교체(잘한 점검 · 못한 점검 두 장).

### 2.4 도식 없는 과제 카드 (`TaskSheet.kt`)
- 지금: 주행 5개·출발 전 점검·지식 카드는 도식 자리(176 dp)가 비어 위쪽 절반이 빈다(라운드 6 관찰부터).
- 바꿀 것: 도식이 없는 과제는 도식 칸을 빼고 제목·난이도를 카드 세로 가운데로. 카드 높이는 같은 줄의 다른 카드와 같게(주차 줄과 주행 줄의 높이가 달라도 된다). 가능하면 출발 전 점검에는 간단한 체크 아이콘 도식(선 3줄 + 체크) — `ParkingTaskDiagram` 과 같은 선 굵기·색.
- 캡처 `lesson-setup-sheet-driving.png`·`lesson-setup-knowledge.png` 교체 + 조작 카테고리 캡처 추가.

### 2.5 지식 테스트 왼쪽 패널의 연보라 사각형 (`QuizScreen.kt` `QuizNumber`)
- 지금: 패널 오른쪽 아래 15 % 사각형(`drawRect(Lavender, …)`)이 남은 조각처럼 보인다.
- 바꿀 것: 지운다. (포스터 구성 요소로 남기고 싶으면 C절에 이유를 적고 둔다.)

### 2.6 예약 뒤 시험장 목록의 안내 문장 (`VenueSheet.kt`)
- 지금: 이미 예약했는데도 `연습할 시험장을 골라 주세요.`
- 바꿀 것: 예약이 있으면 `예약한 시험장을 누르면 확인할 수 있어요.`

### 2.7 리포트 왼쪽 숫자의 뜻 (`ReportScreen.kt`)
- 지금: `연습 기록` + `01` — 회차 수인데 점수로 읽힌다(특히 100회 캡처).
- 바꿀 것: 눈썹을 `연습한 회차` 로. 숫자 아래에 작은 `회`(Paper 60 %).

### 2.8 카테고리 이름 `조작` → `점검` (`LessonPresentation.kt`)
- 지금 이 카테고리의 과제는 출발 전 점검 하나다. 시트 띠·세부 과제 제목("조작 세부 과제")이 같이 바뀐다.

### 2.9 점검 칩은 **기록**으로 완료 표시 (`ManeuverScreen.kt`·`ChecklistPresentation.kt`) — Claude 선행 완료
- 지금: 가이드가 "왼쪽 켜졌어요" 라고 한 직후 칩 `좌 지시등` 이 다시 `아직`(지시등은 켰다 끄므로). `브레이크 / 시동` 도 시동 뒤 브레이크를 떼면 회색 `시동 켜짐`.
- 새 필드(`ManeuverDisplayState`, 이번 Claude PR): `leftIndicatorChecked`·`rightIndicatorChecked`·`hazardChecked`(회차 중 한 번이라도 켰나 — 켰다 꺼도 true), `brakeAtIgnition`(시동 순간 브레이크, 시동 전 null). 신호 없음은 null.
- 바꿀 것: 지시등·비상등 칩 — 기록이 true 면 완료 색 + `확인`, 지금 켜져 있으면 `켜짐`(완료 색), 둘 다 아니면 `아직`. 브레이크/시동 칩 — `ignitionOn == true && brakeAtIgnition == true` 면 완료 색 `밟음 → 켜짐`(시동 뒤 브레이크를 떼도 유지), `brakeAtIgnition == false` 면 대기 색 `브레이크 없이 켜짐`, 시동 전은 지금대로.
- 계측: 잘한 점검 재생 끝(모든 등화 꺼짐)에서 칩 7개가 전부 완료 색. `lesson-maneuver-checklist.png` 교체.

## 3. 계측 흔들림

- 예약 흐름의 칩 `click` 뒤 기대 상태(`예약` 노드 등장 등)를 ≤ 1 s 폴링으로 확인하고 없으면 한 번 재탭(#55 리뷰 5회 중 1회).
- 완료 기준의 "리뷰 환경에서 3회 연속 PASS" 를 PR 본문에 3회 로그로.

## 4. 결정이 필요한 것 (적힌 것만 한다)

- 4.1 **자세히 보기에 항목별 수치**(조향 왕복·기어 전환·근접·급정지)를 회차 행 아래에 — 진단서 공유 범위 "항목별" 과 맞추기. 데이터는 `ParkingMetrics` 에 이미 있다. **결정: (사용자)**
- 4.2 **브리핑 제목에 볼 항목 전부** — 지금은 앞 두 개만(`briefingHeadline`). 추천: 주차는 셋 전부, 점검은 `문부터 비상등까지\n순서대로 볼게요.`(개수를 말하지 않는다). **결정: (사용자)**

## 5. 불변

| 규칙 | 검사 |
|---|---|
| 주행 중 화면에 점수·누계 숫자 없음(퀴즈 포함) | lesson_shots(추가) |
| 시트 첫 렌더 모드·시작, 주 버튼·알약·잠금 계약, Done 도착 칸 | 기존 |
| `ui/` `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 만, 새 문자열 리소스 0 | grep |
