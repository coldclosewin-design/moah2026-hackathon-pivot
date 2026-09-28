# Codex 오더 — UI 라운드 7: 지식 테스트 답 표시·그만하기 · 조향 애커만 바퀴와 보조선 · 동승자 화면 제거 (2026-09-28)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/05_round7_feedback.md(피드백 원문·분류), 라운드 4 발주서 docs/handoffs/2026-09-28_codex_ui_round4.md 의 Maneuver 절(호·보조선 규격)을 읽어라.
브랜치: codex/ui-round7 를 origin/main 에서 새로 만들어 작업(이 발주서 PR 머지 뒤).
작업: ① Quiz 답 공개 화면에 내 답·정답 표시, QuizDone 문제별 "내 답 → 정답" ② Quiz 에 정차 중 `그만하기`(→ Setup) ③ 조향 도식 앞바퀴 좌우 각 분리(애커만) + 보조선을 회전 중심을 공유하는 두 호로 ④ 동승자 화면 전부 제거(Report 탭·Setup 응원 눈썹·계측·캡처·문자열). 상태기계·채점·데이터·도구는 손대지 않는다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(원본 — 동승자 단계는 탭이 없으면 스스로 건너뛴다),
          docs/screenshots/lesson/ 에 lesson-quiz-answered.png(내 답·정답)·lesson-quiz-done.png·lesson-maneuver-guides.png 교체, lesson-companion*.png·lesson-setup-cheer.png 삭제, gh pr create.
```

## 1. 지식 테스트 (`QuizScreen.kt`, `QuizDoneScreen.kt`)

### 답 공개 상태 (#1)
- 선택지 3 중 **정답**: 바탕 `Periwinkle`·글자 `Paper`(지금과 같음) + 왼쪽 눈썹 `정답`(32 sp `Periwinkle`).
- **내가 고른 오답**: 바탕 `Lavender`·글자 `Ink` + 윤곽 `Signal` 4 dp + 왼쪽 눈썹 `내 답`(32 sp `Signal`). 맞았으면 정답 칸 하나에 눈썹 `내 답 · 정답`.
- 나머지 오답: 지금처럼 `Lavender`·`Muted`.
- `item.why` 와 `다음 문제`/`결과 보기` 는 그대로. 눈썹 문구 2개(`정답`·`내 답`)는 리소스 없이 인라인.

### 그만하기 (#2)
- 하단 왼쪽 `TextAction("그만하기")` — **`locked` 가 아닐 때만**(정차 중). 잠금이면 선택지와 함께 숨긴다(터치 0 불변). 누르면 `vm.restart()` → Setup(푼 것은 저장하지 않음 — 상태기계 `reset()` 이 이미 그렇게 동작한다). `결과 보기`(마지막 문제 뒤) 와 `QuizDone` 의 `다시 시작` 은 그대로.
- 문자열 리소스 1개 추가(`lesson_quit` = "그만하기").

### QuizDone 문제별 행
- 틀린 문제: `내 답 → 정답` 한 줄(40 sp, 내 답 `Signal`·정답 `Periwinkle`) + `why`(32 sp `Muted`). 맞은 문제: `맞았어요`(지금과 같음). 안 푼 문제(중간 종료가 아니라 `결과 보기`로 끝낸 경우는 없지만 `endSession` 경로): `안 풀었어요` 그대로.

## 2. 조향 도식 — 애커만 (`VehicleDiagram.kt`, `LessonPresentation.kt`) (#3)

- **순수 함수** `wheelAngles(steeringDeg: Float?): Pair<Float, Float>` (왼쪽 앞바퀴, 오른쪽 앞바퀴, 도 단위, 미측정이면 `0f to 0f`):
  - 바깥쪽 = `wheelRotation(steeringDeg)`(기존 ±38°), **안쪽 = 바깥쪽 × 1.25**, 안쪽 최대 ±45°.
  - 운전자 기준 오른쪽 조향(`steeringDeg < 0`)이면 **오른쪽 바퀴가 안쪽**(더 꺾임), 왼쪽 조향이면 왼쪽 바퀴가 안쪽. 단위 테스트 4건(0°·±450°·미측정·안쪽 > 바깥쪽).
- 바퀴 그리기: 두 앞바퀴를 각각의 각으로 `rotate`(지금은 같은 각). 애니메이션은 라운드 4 의 `animateFloatAsState(steeringDeg)` 하나를 그대로 입력으로.
- **보조선 = 회전 중심을 공유하는 두 호**: 앞이 아래이므로 회전 중심은 차 앞쪽(아래) 옆에 있다. 화면 좌표에서 회전 중심 `C = (cx ± R, frontAxleY)`(오른쪽 조향 → 화면 왼쪽), `R = 휠베이스 / tan(바깥쪽 바퀴각)`(도식 단위, `wheelbase = carHeight × 0.6`). 각 앞바퀴 위치에서 `C` 를 중심으로 하는 원호를 **진행 방향(아래)으로** 90° 만큼 그린다 — 안쪽 바퀴 호가 반지름이 작고 바깥쪽이 크다. 두 호는 서로 겹치지 않고 회전 중심 쪽으로 모인다. 조향 0 이면 두 직선(아래로). 점선·색·굵기는 라운드 4 규격(`Periwinkle` 40 %, 4 dp, dash 12/12).
- 가운데 실선 호(`steeringArcBend`)는 그대로. 접근성 텍스트 그대로(`조향 방향 호, 보조선`).
- `lesson-maneuver-guides.png` 교체 + 조향 클립(계측 `lesson-round4-steering.mp4`)으로 좌우 각 차이가 보이게.

## 3. 동승자 화면 제거 (#5)

- `ReportScreen`: `ReportPage.COMPANION`·`CompanionContent`·`동승자` 액션·`onShareWithCompanion`/`onCheer` 파라미터 제거. `LessonRoute` 호출도 원복.
- `SetupScreen`: `cheer` 파라미터·눈썹 제거. `LessonRoute` 원복.
- `LessonComponents.SelectionChip` 의 `modifier` 파라미터는 남겨도 된다(무해).
- 계측: 동승자 2묶음·`companion`/`setup-cheer` 캡처·`assertExclusiveSelection` 중 동승자 전용 부분 제거. `LessonReport(...)` 생성자 호출의 `companion =`·`cheers =` 인자 제거(기본값이라 그냥 빼면 된다 — Claude 가 뒤이어 필드를 지운다).
- `strings.xml` 의 `lesson_companion` 삭제. `docs/screenshots/lesson/` 의 `lesson-companion*.png`·`lesson-setup-cheer.png` 삭제, README 갱신.
- **모델·상태기계·시드·`emu_flow` 의 동승자 코드는 건드리지 않는다** — 머지 뒤 Claude 가 지운다. 그때까지 `emu_flow` 는 탭이 없어 스스로 건너뛴다.

## 4. 불변 (라운드 6 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| `Quiz` 답 공개: 정답 칸 `정답` 눈썹, 오답 선택 시 그 칸 `내 답` 눈썹 + `Signal` 윤곽 픽셀, 맞으면 `내 답 · 정답` | lesson_shots(수정) |
| `Quiz` 정차 중 `그만하기` 보임 → 클릭 시 `restart` 콜백 1회; 잠금이면 `그만하기`·선택지 모두 없음(터치 0) | lesson_shots(추가) |
| `QuizDone` 틀린 문제 행에 `내 답 → 정답`, 맞은 문제 `맞았어요` | lesson_shots(수정) |
| `wheelAngles`: 안쪽 > 바깥쪽, 오른쪽 조향은 오른쪽 바퀴가 안쪽, 미측정 0/0 | 단위 테스트 |
| 보조선 두 호가 서로 다른 반지름(픽셀: 두 점선의 아래 끝 x 간격 > 위 끝 x 간격 — 회전 중심 쪽으로 모임) | lesson_shots(추가, 캡처 픽셀) |
| Report 에 `동승자` 없음, Setup 에 `동승자 ·` 없음 | lesson_shots(수정) |
| 주 버튼·알약·패널·잠금·숫자 없음 계약 | lesson_shots(기존) |
| `ui/` 에 `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 에만, 문자열 리소스 +1(`lesson_quit`) −1(`lesson_companion`) | grep |

## 5. 데이터 — 새로 쓰는 것 없음

| 필드 | 뜻 |
|---|---|
| `LessonPhase.Quiz.chosen` / `item.answer` / `item.choices` | 내 답·정답 표시 |
| `LessonPhase.QuizDone.results[i].chosen / correct` | 문제별 행 |
| `vm.restart()` | 그만하기(어느 단계에서든 Setup) |
| `ManeuverDisplayState.steeringDeg` | 애커만 입력 |

## 6. 참고

- 동승자 공유는 PR #40~#43 으로 하루 만에 넣고 뺀 것 — 이유는 `05_round7_feedback.md` #5. 되돌리기는 git 이력으로.
- 출발 전 점검 확장·시험장 예약은 **사용자 결정 뒤 별도 발주**(Claude 선행 → Codex). 이 라운드에 넣지 않는다.
