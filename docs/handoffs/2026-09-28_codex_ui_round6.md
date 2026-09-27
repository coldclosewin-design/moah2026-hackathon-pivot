# Codex 오더 — UI 라운드 6: 과제 시트를 "카테고리 → 세부 과제" 두 층으로 — 시안 05(큰 글자 + 주차 칸) (2026-09-28)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/round6-sheet/a-refinements/README.md(선택안 05 와 그 프롬프트), 라운드 4 발주서의 Setup 절(주 버튼 규칙)을 읽어라.
브랜치: codex/ui-round6 를 origin/main 에서 새로 만들어 작업(이 발주서 PR 머지 뒤).
작업: Setup 의 "과제·모드 바꾸기" 시트를 한 층(카드 9개 3×3)에서 두 층(카테고리 4 → 세부 과제)으로, 사용자가 고른 시안 05(docs/design/round6-sheet/a-refinements/05-type-bays.png)대로. 시드에 준비 중 주차 과제 2개 추가(문구·시드 내용은 Codex 영역). 상태기계·채점·도구는 손대지 않는다.
제약: vehicle/, ports/, scoring/, feature/lesson/ 의 구조와 data/ 의 구조(Task 필드), build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(원본 그대로 — 시트를 열면 주차가 이미 펼쳐져 "힌트"·"시작" 이 바로 보여야 한다),
          docs/screenshots/lesson/ 에 lesson-setup-sheet.png(주차 펼침)·lesson-setup-sheet-driving.png(주행 펼침)·lesson-setup-knowledge.png 교체/추가, gh pr create.
```

## 0. 왜 · 무엇을 골랐나

사용자(라운드 5 영상 피드백): 과제 카드를 **최상위 카테고리 먼저**, 세부 과제는 **다음 층**으로 — 보기 편하고 더 담을 수 있게. 이어서 화면 UI 를 여러 안으로 보고 **시안 05 · 큰 글자와 주차 칸**(3번의 글자 메뉴 상단 + 2번의 주차 칸 하단)을 골랐다(2026-09-28, `docs/design/round6-sheet/README.md` 첫 줄).

데이터는 이미 두 층이다 — `Task.type`(`PARKING · DRIVING · CHECKLIST · KNOWLEDGE`) 이 카테고리, `SeedCatalog.tasks` 가 세부 과제. `Task` 필드는 그대로이고, **시드 내용만** 준비 중 주차 과제 2개를 더한다(§3).

세부 결정 3건(Claude 제안, 사용자 확정 없이 진행 — 리뷰 때 뒤집을 수 있다):
1. **도식은 주차 카테고리에만.** 주행·조작·지식 세부 과제는 같은 칸 모양에 글자만(도식 없음).
2. 시안의 **전면 직각 주차·사선 주차를 시드에 준비 중(PLANNED)으로 추가** — 두 층으로 바꾼 이유가 "더 담기 위해서" 다.
3. 상위 카테고리의 선택 표시(빨간 글자·밑줄·▼)는 **시안대로 `Signal`** — 디자인 브리프의 "빨강은 주 버튼과 화살표에만" 규칙의 **예외**로 기록(`02_design_brief.md`). 다른 곳으로 번지지 않는다.

## 1. 시트 구조 (`SetupScreen.kt`) — 시안 05

읽는 영역 70 %(오른쪽 1792 dp). 시트를 열 때 **제안 과제(`suggestedTask`)의 카테고리가 이미 펼쳐진 상태** — `emu_flow.sh` 와 실제 시연이 시트 직후 `힌트` → `시작` 을 누르므로 첫 렌더에 모드 칩과 `시작` 이 보여야 한다(불변).

### 1층 — 글자 메뉴 (카테고리 4, 순서: 주차 · 주행 · 조작 · 지식)
- 눈썹 `연습할 과제` 아래 **가로 한 줄, 배경 없는 큰 글자 4개**(56 sp, 같은 간격으로 4열 분배, 각 열 가운데 정렬). 카드·띠 없음.
- **펼친 카테고리**: 글자 `Signal`, 아래에 `Signal` 밑줄(두께 6 dp, 글자 폭) + 그 밑 가운데 작은 ▼(`Signal`, 폭 32 dp) — 아래 층을 가리킨다.
- 나머지 카테고리: 글자 `Periwinkle`. READY 과제가 없는 카테고리(지금은 주행)는 글자 `Periwinkle` 60 % + 아래 32 sp `준비 중`. 클릭은 되지만(펼쳐서 준비 중 목록을 본다) `시작` 은 숨김.
- 카테고리 순서는 `TaskType` enum 순서가 아니라 시연 순서 — `LessonPresentation` 에 `categoryOrder(): List<TaskType>` 순수 함수 + 단위 테스트. 라벨은 `taskTypeLabel` 그대로(새 문자열 없음).

### 2층 — 세부 과제 칸 (펼친 카테고리 안)
- 눈썹 `{카테고리} 세부 과제`(예: `주차 세부 과제`, 40 sp) 아래 **가로 한 줄 최대 4칸**(넘치면 `LazyRow`, 다음 칸 일부가 보이게). 칸 = **U자 주차 칸 윤곽**(위가 열린 사각, 선 4 dp, 모서리 0) 안에 도식 + 제목(40 sp, 2줄 제한) + 난이도(32 sp `하/중/상`) 또는 `준비 중`.
  - 선택 칸: 윤곽 `Ink` 6 dp, 제목 `Ink`, 아래 가운데 **빨간 원 + 흰 체크**(`Signal`, 지름 64 dp, 윤곽선 아래 가장자리에 걸침) — 시안 05 의 후면 직각 주차.
  - 비선택 READY 칸: 윤곽 `Periwinkle`, 제목 `Ink`.
  - 준비 중 칸: 윤곽 `Lavender`, 제목·도식 `Periwinkle` 40 %, 아래 32 sp `준비 중`, 클릭 노드 없음(`disabled`).
- **도식(주차만)**: 칸 안 위쪽에 탑뷰 차 실루엣(`Maneuver` 의 `VehicleDiagram` 축소판 — 차체·창만, 바퀴·호 없음, 높이 160 dp) + 과제별 칸 표시선: 후면 직각 = 양옆 세로 실선 2, 평행 = 오른쪽 세로 점선 1(길가), 전면 직각 = 위 가로 실선 1 + 양옆, 사선 = 차·선을 30° 기울임. `Canvas` 하나에 `when (task.id)` 로. 선택 칸의 도식은 `Ink`, 그 외 `Periwinkle`. **주행·조작·지식 카테고리는 도식 없이** 제목·난이도만(칸 높이는 같게 — 도식 자리는 빈 공간).
- 카테고리를 바꾸면 그 카테고리의 첫 READY 과제를 선택(없으면 선택 없음·`시작` 숨김). 모드 칩은 선택 과제의 `supports` 로 필터(라운드 2 규칙).
- 예약 카드(`ReservationInfo`)는 2층 아래 그대로(넘치면 시트 전체가 아니라 예약 카드만 접힘 — 하단 행은 고정).

### 하단 (공통)
- 눈썹 `모드` + 칩 3(220 × 96 dp, 선택 `Periwinkle`) · `돌아가기`(`TextAction`, 왼쪽에 `‹` 글리프 없이 라운드 5 그대로) · `시작`(주 버튼 140 dp × ≥ 720 dp, 오른쪽). 시연 알약 우상단 그대로.
- 시트를 닫고 다시 열면 선택 과제의 카테고리가 펼쳐진다(`rememberSaveable`). 라벨 문자열 전부 그대로.

## 2. 불변 (라운드 5 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| 시트 첫 렌더에 제안 과제의 카테고리(주차)가 펼쳐져 **모드 칩 3 과 `시작` 이 보인다**, 제안 과제가 선택 상태 | lesson_shots(수정) + emu_flow(원본 — `힌트`→`시작` 그대로 통과) |
| `지식` 을 누르면 지식 과제가 선택되고 모드 칩은 `지식 테스트` 만, `시작` → `(knowledge, QUIZ)` | lesson_shots(수정: `scrollTo(knowledge.title)` 대신 카테고리 `지식` 클릭 → 과제 클릭) |
| `주행` 을 누르면 준비 중 5개가 보이고 클릭 노드 없음, `시작` 없음 | lesson_shots(추가) |
| 준비 중 과제·칸은 클릭 노드 없음(`disabled` 시맨틱) | lesson_shots(기존) |
| 선택 칸 아래 빨간 체크 원 픽셀, 펼친 카테고리 글자 `Signal` 픽셀 | lesson_shots(픽셀, 라운드 5 방식) |
| 주 버튼 4개 140 dp × ≥ 720 dp, 패널 접힘·알약 토글, 잠금 터치 0 | lesson_shots(기존) |
| 시트에 숫자 없음(과제 수·번호), `ui/` 에 `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 에만, 새 문자열 리소스 0 | grep |

## 3. 데이터 — 시드 내용만 (Codex 영역)

| 항목 | 뜻 |
|---|---|
| `Task.type: TaskType` | 카테고리(1층). 필드 추가 없음 |
| `taskTypeLabel(type)` (UI) | 카테고리 제목 |
| **시드 추가 2**: `parking-front`(전면 직각 주차, `PARKING`, `MEDIUM`, `status = PLANNED`), `parking-angle`(사선 주차, `PARKING`, `HARD`, `PLANNED`) | `SeedCatalog.tasks` 에 평행 주차 다음에. `summary`·`watch`·`signals` 는 평행 주차와 같은 형식으로 짧게. **`SeedCatalogTest` 의 과제 수·READY 수 단언이 있으면 함께 갱신**(READY 는 여전히 3) |
| `Task.isReady` / `Task.status.label` | 준비 중 표시 — 카테고리는 `tasks.any { it.isReady }` |
| `Task.supports(mode)` | 모드 칩 필터 |

## 4. 캡처

`lesson-setup-sheet.png`(주차 펼침 = 첫 렌더, 4칸 + 체크) 교체, `lesson-setup-sheet-driving.png`(주행 펼침, 5개 준비 중 가로 스크롤) 추가, `lesson-setup-knowledge.png` 는 카테고리 경로로 다시. PR 본문에 시안 05 와 캡처를 나란히.

## 5. 참고

- 시안 05 는 `image_gen` 결과라 픽셀 규격이 아니다 — 위 dp 값이 규격이고, 시안은 형태·위계 기준.
- 라운드 3 피드백 #1(카테고리 색 띠) → 라운드 4 띠 → 이번 층. 띠는 이 시트에서 사라진다(카테고리 색은 글자 메뉴·도식으로). 다른 화면엔 영향 없음.
- `emu_flow.sh` 는 시트에서 `힌트`·`시작` 만 누른다 — 시트가 주차 펼침으로 열리면 스크립트 수정이 없다. 열리지 않게 만들면 스크립트가 멈춘다(tools 는 Claude 영역 — 그 경우 C절에 적는다).
