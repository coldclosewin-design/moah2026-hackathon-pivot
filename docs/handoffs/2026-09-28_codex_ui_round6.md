# Codex 오더 — UI 라운드 6: 과제 시트를 "카테고리 → 세부 과제" 두 층으로 (2026-09-28)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, 라운드 4 발주서 docs/handoffs/2026-09-28_codex_ui_round4.md 의 Setup 절(띠·주 버튼 규칙)을 읽어라.
선행: 디자인 세션 docs/handoffs/2026-09-28_codex_design_sheet.md(시안 3~4안) → 사용자 선택 → Claude 가 이 발주서 §1 을 선택안으로 갱신 → 그 뒤 구현.
브랜치: codex/ui-round6 를 origin/main 에서 새로 만들어 작업(선택안 반영 PR 머지 뒤).
작업: Setup 의 "과제·모드 바꾸기" 시트를 한 층(카드 9개 3×3)에서 두 층(카테고리 4 → 그 안의 세부 과제)으로, 사용자가 고른 시안대로. 데이터·상태기계·채점·도구는 손대지 않는다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(원본 그대로 — 시트를 열면 제안 과제의 카테고리가 이미 펼쳐져 "힌트"·"시작" 이 바로 보여야 한다),
          docs/screenshots/lesson/ 에 lesson-setup-sheet.png(카테고리 층)·lesson-setup-sheet-parking.png(주차 펼침) 교체/추가, gh pr create.
```

## 0. 왜 (라운드 5 B안 영상 피드백, 2026-09-28)

사용자: "연습 과제 카드를 봤을 때 **최상위 카테고리를 먼저** 띄워 주고, 그 안에 세부 항목(평행 주차·직각 주차 …)이 **다음 층**으로 나오면 보기 편하고, 더 많은 내용을 담을 수 있다."

지금 시트는 9장을 3×3 으로 한 번에 보여 주고 유형은 왼쪽 띠 색으로만 구분한다(라운드 4). 과제가 늘면(도로 과제·주차 변형) 한 화면에 안 들어간다. 데이터는 이미 두 층이다 — `Task.type`(`CHECKLIST · DRIVING · PARKING · KNOWLEDGE`) 이 카테고리, `SeedCatalog.tasks` 가 세부 과제. **새 필드·새 시드 없음.** 화면이 `tasks.groupBy { it.type }` 으로 나누면 된다.

## 1. 시트 구조 (`SetupScreen.kt`)

> 아래는 **기본안(디자인 세션의 A · 두 줄 카드)** 기준이다. 사용자가 B·C·D 를 고르면 Claude 가 이 절을 선택안의 배치로 고쳐 쓴다. §2 불변과 §3 데이터는 안과 무관하게 그대로다.

읽는 영역 70 % 안에서 두 층. 시트를 열 때 **제안 과제(`suggestedTask`)의 카테고리가 이미 펼쳐진 상태**로 연다 — `emu_flow.sh` 와 실제 시연은 시트를 연 직후 `힌트` → `시작` 을 누르므로, 첫 렌더에 모드 칩과 `시작` 이 보여야 한다(불변).

### 1층 — 카테고리 4 (`taskTypeLabel` 순서: 주차 · 주행 · 조작 · 지식)
- 가로 한 줄 카드 4개, 같은 폭, 높이 160 dp. 카드 = 라운드 4 의 유형 띠 색을 **왼쪽 12 dp 띠**로 그대로(주차 `Periwinkle` · 주행 `Ink` · 조작 `Signal` · 지식 `Periwinkle` 40 %), 제목 40 sp(`주차` / `주행` / `조작` / `지식`), 그 아래 32 sp `Muted` 로 **세부 과제 수와 준비 상태**는 숫자 없이 — `2개` 대신 "직각 · 평행" 처럼 세부 제목을 `·` 로 이어 한 줄(넘치면 말줄임). 준비된 과제가 하나도 없는 카테고리(지금은 주행)는 카드 전체를 `Muted` 로, 우상단 32 sp `준비 중`.
- 펼쳐진 카테고리 카드는 바탕 `Periwinkle`·글자 `Paper`·띠 `Ink`(라운드 5 선택 규칙과 같음).
- 카테고리 순서는 `TaskType` enum 순서가 아니라 **시연 순서**(주차가 첫 칸) — `LessonPresentation` 에 `categoryOrder` 순수 함수로 두고 단위 테스트.

### 2층 — 세부 과제 (펼친 카테고리 안)
- 1층 바로 아래, 가로 한 줄에 최대 3장(넘치면 가로 스크롤 `LazyRow`, 이 카탈로그에선 주행 5개만 해당). 카드 높이 220 dp·라운드 4 규칙(제목 2줄 제한, 유형·난이도 한 줄은 **난이도만** `하/중/상` — 유형은 위 층이 말한다, 준비 중 라벨 우상단, 선택 = `Periwinkle` 바탕 + `Ink` 띠).
- 카테고리를 바꾸면 그 카테고리의 첫 READY 과제를 선택(없으면 선택 없음·`시작` 비활성이 아니라 **`시작` 숨김** — 터치 타깃 규칙). 모드 칩은 선택 과제의 `supports` 로 필터(라운드 2 규칙 그대로).
- 예약 카드(`ReservationInfo`)는 2층 아래 그대로.

### 공통
- 하단 행 `돌아가기`(왼쪽) · `시작`(오른쪽, 주 버튼 규칙 140 dp × ≥ 720 dp) 그대로. 모드 칩 220 × 96 dp 그대로.
- 시트를 닫고 다시 열면 선택 과제의 카테고리가 펼쳐진다(`rememberSaveable`).
- 라벨 문자열 전부 그대로(`과제·모드 바꾸기` / `돌아가기` / `시작` / 모드 라벨 / 과제 제목). 카테고리 제목 4개는 `taskTypeLabel` 을 그대로 쓴다(새 문자열 없음).

## 2. 불변 (라운드 5 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| 시트 첫 렌더에 제안 과제의 카테고리가 펼쳐져 **`가이드`·`힌트`·`평가` 칩과 `시작` 이 보인다**, 제안 과제가 선택 상태 | lesson_shots(수정) + emu_flow(원본 — `힌트`→`시작` 탭이 그대로 통과) |
| 지식 카테고리를 누르면 지식 과제가 선택되고 모드 칩은 `지식 테스트` 만, `시작` → `(knowledge, QUIZ)` | lesson_shots(수정: `scrollTo(knowledge.title)` 대신 카테고리 `지식` 클릭 → 과제 클릭) |
| 준비 중 과제·준비 중 카테고리는 클릭 노드 없음(`disabled` 시맨틱) | lesson_shots(기존 + 카테고리) |
| 선택 세부 카드 왼쪽 12 dp `Ink` 띠, 펼친 카테고리 카드 띠 `Ink` | lesson_shots(픽셀, 기존 방식) |
| 주 버튼 4개 140 dp × ≥ 720 dp, 패널 접힘·알약 토글, 잠금 터치 0 | lesson_shots(기존) |
| `ui/` 에 `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 에만, 새 문자열 리소스 0 | grep |

## 3. 데이터 — 새로 쓰는 것 없음

| 필드 | 뜻 |
|---|---|
| `Task.type: TaskType` | 카테고리(1층) |
| `taskTypeLabel(type)` (UI) | 카테고리 제목 |
| `Task.isReady` / `Task.status.label` | 준비 중 표시 — 카테고리 카드는 `tasks.any { it.isReady }` 로 |
| `Task.supports(mode)` | 모드 칩 필터 |

## 4. 캡처

`lesson-setup-sheet.png`(주차 펼침 = 시트 첫 렌더) 교체, `lesson-setup-sheet-driving.png`(주행 펼침 — 5개 가로 스크롤, 전부 준비 중) 추가, `lesson-setup-knowledge.png` 는 카테고리 경로로 다시.

## 5. 참고

- 라운드 3 피드백 #1("카테고리별 블록 색")이 띠 → **층**으로 자란 것이다. 띠 색 규칙은 그대로 1층 카드에 옮긴다.
- `emu_flow.sh` 는 시트에서 `힌트`·`시작` 만 누른다 — 시트가 제안 카테고리로 열리면 스크립트 수정이 없다. 열리지 않게 만들면 스크립트가 멈춘다(tools 는 Claude 영역 — 그 경우 C절에 적는다).
