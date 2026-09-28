# 오더 — 출발 전 점검 7단계 확장 (2026-09-28) · 결정 D1

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/05_round7_feedback.md §2.1, 라운드 2 발주서의 점검 과제 절(칩 3개), 이 발주서를 읽어라.
브랜치: codex/predrive-7 를 origin/main 에서 새로 만들어 작업(§1 Claude 선행 PR 머지 뒤 — NEXT Step 11 에 표시).
작업: 출발 전 점검 과제가 3단계(벨트·P·시동)에서 7단계(도어·벨트·P·브레이크+시동·좌 지시등·우 지시등·비상등)로 늘어난다. 화면은 점검 Maneuver 칩 3 → 7, Done·Report 의 점검 행 확장, 가이드 확인 문장·힌트 문구 다듬기(시드 내용).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(주차 본편 불변),
          docs/screenshots/lesson/ 에 lesson-maneuver-checklist.png(7칩)·lesson-maneuver-checklist-missing.png·lesson-done-checklist.png 교체, gh pr create.
```

## 0. 왜 · 왜 지금

사용자(라운드 6·동승자 영상 피드백): "출발 전 점검이 너무 간단하다 — 추가 콘텐츠 필요." 결정 D1(9/28): **7단계 그대로**. 시연 본편은 주차지만, READY 과제 셋 중 하나가 8 s 짜리 3단계면 카탈로그가 비어 보인다.

원칙: **새 VSS 경로 없이** 스텁에 이미 있는 신호(`Door`·`DirectionIndicator.Left/Right`·`Hazard`·`Brake.PedalPosition`)만 쓴다 → 사내 이관 위험 0. 신호가 `MISSING` 이면 그 단계는 확인 없이 읽고 넘기고 감점하지 않는다(기존 규칙).

## 1. Claude 선행 (신호·채점·가이드·시나리오·표시 상태) — ⬜

| # | 무엇 | 어디 |
|---|---|---|
| A1 | `VehicleSnapshot` 에 `brakePressed: Boolean?`(PedalPosition > 0)·`indicatorLeft/Right: Boolean?`·`hazard: Boolean?` + `apply` 파싱 | `feature/lesson/LessonModels.kt` |
| A2 | **점검 신호 집합** `ParkingRecorder.CHECKLIST_KEYS = KEYS + {LIGHT_INDICATOR_LEFT, LIGHT_INDICATOR_RIGHT, LIGHT_HAZARD, BRAKE_PEDAL_POSITION}`. 상태기계가 과제 유형별 키로 구독·레지스트리 시딩 → **주차 배지(실신호 0 · 시뮬 8)는 불변**, 점검 배지만 12 로 | `scoring/ParkingRecorder.kt`, `LessonStateMachine.kt`, `SignalRegistry`(키 집합 교체 가능하게) |
| A3 | `PreDriveSummary` 확장: `doorClosedBeforeIgnition: Boolean?`, `brakeBeforeIgnition: Boolean?`, `leftIndicatorChecked / rightIndicatorChecked / hazardChecked: Boolean?`(null = 미측정, false = 신호는 있는데 안 함). `PreDriveChecklist.summarize` 가 시계열에서 계산(시동 시각 전 도어 닫힘·브레이크 > 0, 세션 안에 지시등/비상등 true 가 한 번이라도) | `scoring/PreDriveChecklist.kt` |
| A4 | `ChecklistRubric` 추가: 숙련 — 브레이크 없이 시동 -20 · 좌/우 지시등·비상등 건너뜀 각 -10 / 안전 — 도어 열린 채 시동 -30. `ChecklistScorer` 반영. 미측정은 무감점 | `scoring/ChecklistScorer.kt` |
| A5 | 가이드 7단계(`SeedCatalog.predriveGuide`): 도어 → 벨트 → P 확인 → 브레이크 밟고 시동 → 좌 지시등 → 우 지시등 → 비상등 켰다 끄기. 확인 조건은 신호로. **문장은 Codex 가 다듬는다** | `data/SeedCatalog.kt` |
| A6 | 힌트(`HintRules(checklist = true)`) 추가 2: 도어 열린 채 시동 켜짐 "문이 아직 열려 있어요." / 브레이크 없이 시동 "시동은 브레이크를 밟고요." (URGENT 아님). "다 되셨나요?" 조건 = 벨트·시동·P + 지시등 둘·비상등 확인(미측정은 제외) | `feature/lesson/HintRules.kt`, `LessonStateMachine.kt` |
| A7 | `AdviceRules.Advice` 점검 항목 추가: `DOOR_OPEN_AT_IGNITION`("문을 닫고 시동을 켜요."), `NO_BRAKE_AT_IGNITION`("시동은 브레이크를 밟은 채로요."), `LIGHTS_SKIPPED`("지시등과 비상등도 출발 전에 한 번씩 켜 봐요.") — 우선순위: 벨트 순서 → 도어 → 브레이크 → 움직임 → P → 지시등 → 유지 | `ports/CoachPort.kt` |
| A8 | Fake 시나리오 2벌 갱신(`ChecklistScenarios`): 잘한 점검 ~14 s(7단계 순서대로) → 100/100 · 못한 점검 ~20 s(도어 열린 채 시동 + 브레이크 없이 + 우 지시등·비상등 건너뜀) → 고정값은 테스트가 정한다(`ChecklistScenarioTest`) | `data/ChecklistScenarios.kt` |
| A9 | `ManeuverDisplayState` 점검 필드 확장: `doorOpen`, `brakePressed`, `indicatorLeft`, `indicatorRight`, `hazard` + 각 `…Signal`. `Task.watch`(브리핑 "오늘은 ○○을 봅니다")는 "도어와 벨트와 기어와 시동과 지시등" | `feature/lesson/ManeuverDisplayState.kt`, `SeedCatalog` |
| A10 | `docs/INTEGRATION.md` B절: 점검 신호 4개의 경로 가정(COVESA) + 배지 12 | 문서 |

## 2. Codex — 화면

### 점검 `Maneuver` (`ManeuverScreen.kt`) — 칩 3 → 7
- 왼쪽 남색 영역에 `ChecklistValue` **7개, 2열**(왼쪽 4 · 오른쪽 3) 또는 세로 7행(높이가 허용하면 — 1268 dp 에 7 × 120 dp + 간격). 순서 = 가이드 순서: 도어 · 안전벨트 · 기어 · 브레이크/시동 · 좌 지시등 · 우 지시등 · 비상등. 값 표현: 도어 `닫힘/열림`, 브레이크·시동은 한 칩(`밟음 → 켜짐` / `꺼짐`), 지시등·비상등 `확인/아직`. 완료(원하는 상태)는 `Periwinkle`, 아직은 `Ink` 60 %, 미측정 회색 + `미측정`.
- 라운드 2·3 의 점검 규칙(조향·거리 텍스트 없음, 이동 구간 숨김, 주 버튼 규칙) 그대로.

### 점검 `Done` / `Report` 행
- `Done`: 두 문장 멘트만(라운드 3 규칙 — 숫자 행 없음). 변경 없음.
- `Report` 자세히 보기: 점검 과제면 행을 `벨트 N초 · 시동 N초 · 순서 · 움직임` 에서 **7항목 체크 표**(✓ / ✗ / 미측정)로. 숫자는 시각 둘만.

### 가이드·힌트 문구(시드 내용)
- A5·A6 의 문장을 다듬는다. 확인 문장 예: 도어 "닫혔어요." / 브레이크+시동 "브레이크 밟고 시동, 좋아요." / 지시등 "왼쪽 켜졌어요. 이제 오른쪽." / 비상등 "비상등 확인. 이제 꺼 주세요."

## 3. 불변

| 규칙 | 검사 |
|---|---|
| 주차 본편 불변: `emu_flow` PASS, 주차 배지 `실신호 0 · 시뮬레이션 8 · 미측정 0` | emu_flow(원본) |
| 점검 `Maneuver` 칩 7, 미측정 칩 회색 + `미측정`, 조향·거리 텍스트 없음 | lesson_shots(수정) |
| 점검 시나리오 고정값(`ChecklistScenarioTest`) | 단위 테스트 |
| 잠금·주 버튼·패널·숫자 없음 계약 | lesson_shots(기존) |

## 4. 안 하는 것

사이드미러·시트 조정(신호 없음 → 자기 신고 버튼은 정직 원칙에서 약함). 새 VSS 경로. STT.
