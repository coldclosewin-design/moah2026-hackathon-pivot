# Codex 오더 — UI 라운드 8: 과제 칸 면 카드 · Setup 일러스트 모핑 · Done 궤적(결정 D1) (2026-09-28)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/06_round8_feedback.md(피드백 원문·분류·D1 두 안), 라운드 6 발주서(시트 칸 규격)·라운드 5 발주서(Done 궤적 "뒤가 위")를 읽어라.
브랜치: codex/ui-round8 를 origin/main 에서 새로 만들어 작업(이 발주서 PR 머지 뒤).
작업: ① 시트 과제 칸을 테두리 없는 면 카드로(모드 칩과 같은 색) ② Setup → 시트 전환에서 왼쪽 일러스트를 모핑(비율·크롭 연속 애니메이션) ③ Done 추정 궤적 — **결정 D1 = (나) 도착 칸**(§3 (나)). (가) 는 만들지 않는다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(원본 — 시트 첫 렌더 `힌트`·`시작` 불변),
          docs/screenshots/lesson/ 에 lesson-setup-sheet.png 교체 + lesson-setup-morph-strip.png(전환 중 프레임 5장 스트립) + (③이면) lesson-done.png 교체·lesson-round4-done.mp4 클립, gh pr create.
```

## 0. 왜

라운드 8 영상(`build/demo-round8.mp4`) 피드백 3건 — `docs/design/06_round8_feedback.md`. ①②는 바로, ③은 시간 순서 문제라 사용자 결정(D1) 뒤.

## 1. 시트 과제 칸 — 테두리 → 면 카드 (#1, `TaskSheet.kt` `TaskBay`)

- U자 윤곽선(`drawPath` 세 변) **삭제**. 칸 전체를 면으로: 준비된 칸 `Lavender`, **선택 칸 `Periwinkle`**(모드 칩 선택 상태와 같은 색), 계획 칸 `Lavender` 40 %.
- 글자·도식 색: 준비 `Ink`, 선택 **`Paper`**(도식 `ParkingTaskDiagram` 도 `Paper`), 계획 `Muted`. 난이도 눈썹·`준비 중` 눈썹도 같은 규칙.
- 빨간 체크 원(`Signal` 32 dp + `Paper` 40 dp 테)은 그대로 **칸 아래 변 중앙**(y = 칸 높이 − 32 dp). 면 카드 위에 겹치므로 `Paper` 테가 카드와 분리해 준다.
- 칸 크기·간격·가로 스크롤·카테고리 띠·모드 칩·`시작` 규칙은 라운드 6 그대로. 모서리 0(포스터 규칙 — `RoundedCornerShape` 금지).
- 카드 배경은 `Modifier.background`, `Canvas` 는 체크 원만. 계측: 칸 안쪽 픽셀(왼쪽 위 + 4 px)이 `Lavender`(준비)·`Periwinkle`(선택), **칸 위 변 바로 아래 픽셀에 `Ink`/`Periwinkle` 선이 없음**(윤곽선 부재).

## 2. Setup → 시트 일러스트 모핑 (#2, `SetupScreen.kt`)

- 지금: `Box(Modifier.weight(if (sheet) .30f else .53f))` 가 즉시 바뀌어 `poster_car`(`ContentScale.Crop`) 크롭이 튄다.
- 바꿀 것: `val fraction by animateFloatAsState(if (sheet) .30f else .53f, tween(400, easing = FastOutSlowInEasing), label = "poster")` → `Modifier.weight(fraction)`. 이미지는 `Crop` 그대로 두면 폭이 줄며 자동으로 크롭이 이어지지만 **초점이 튀지 않게** `alignment` 를 `Alignment.CenterStart` 로 고정하고, 같은 `fraction` 으로 `graphicsLayer { scaleX = scaleY = 1f + (.53f − fraction) * .35f; translationX = … }` 를 걸어 차가 왼쪽으로 **미끄러지며 커지는** 연속 변형(= 모핑)으로. 값은 캡처로 조정해도 된다 — 원칙은 "한 프레임에 크롭이 바뀌지 않는다".
- 오른쪽 본문(요약 ↔ 시트)은 `AnimatedContent(targetState = sheet)` 로 `fadeIn + slideInHorizontally(+40 dp)` / `fadeOut`, 300 ms. **시트 첫 렌더 계약**(모드 칩·`시작`이 첫 프레임에 있어야 `emu_flow` 가 탭한다)은 애니메이션이 끝나기 전에도 노드가 존재하므로 깨지지 않지만, `lesson_shots` 의 시트 검사 전에 `waitForIdle` 을 둔다.
- 돌아가기(시트 → 요약)도 같은 곡선으로 되돌아온다. 캡처 `lesson-setup-morph-strip.png`: 전환 0·100·200·300·400 ms 프레임 5장 가로 스트립(계측에서 `screenrecord --time-limit 3` 뒤 ffmpeg, 또는 `Modifier.drawWithContent` 캡처 — 방법은 자유).

## 3. Done 추정 궤적 (#3, `EstimatedPath.kt`) — **결정 D1 = (나) 도착 칸**(9/28). (가) 는 기록용으로만 남긴다

두 안 모두 데이터(`AttemptRecord.path`)·`PathPresentation` 순수 함수·`pathViewport` 는 건드리지 않는다.

### (가) 역재생 — 기각(기록용)
- `time` 애니메이션은 그대로 0 → `total`. 그리는 시각을 `shown = total − elapsed` 로 뒤집는다: `revealed = pathThroughTime(path, shown)` 은 **시작 → shown** 까지의 궤적이므로, 실차는 `revealed.last()`(시간상 shown 시점)에 두고 궤적은 `pathLegs(path).drop(revealed)`… 가 아니라 **전체 궤적을 처음부터 다 그려 두고** 실차만 `total → 0` 으로 되짚어 가게 한다(궤적이 사라지는 게 아니라 차가 거슬러 올라간다). 실차는 `Lavender` 윤곽(시작 자세) 위에서 끝난다.
- 셰브론(후진 구간)은 `current.reversing` 그대로 — 시간이 거꾸로라 후진 구간에서 차가 앞으로 가는 것처럼 보인다. 급정지 점은 지점 기준이라 그대로.
- 계측: 재생 끝 실차 중심이 시작 윤곽 중심과 같다(± 4 dp).

### (나) 도착 칸 — **채택**
- 시작 윤곽(`pathCar(..., outline = true)`) **삭제** → 시작점에 `Lavender` 지름 12 dp 점 하나.
- **주차 칸**: 도착 자세(`path.last()` 의 위치·헤딩)를 기준으로 U자 두 줄 + 바닥 — 라운드 6 과제 칸 도식과 같은 모양. 크기: 폭 = 차 폭 × 1.25, 깊이 = 차 길이 × 1.15, 열린 쪽은 차의 **앞**(뒤가 위 관습에서 아래). `Periwinkle` 4 dp, 처음부터(0 ms) 그려져 있어 차가 빈 칸으로 들어가는 것으로 읽힌다. 헤딩은 도착 헤딩을 그대로 쓴다(못한 주차는 칸이 비스듬 — 그게 사실이다).
- 실차·궤적·셰브론·급정지 점·3 s 재생·180° 회전 전부 그대로.
- 계측: 도착 실차의 네 모서리가 칸 U자 안에 있다 / 시작 윤곽 픽셀 없음 / 칸 선 픽셀 `Periwinkle` 존재. `lesson-done.png` 교체, `lesson-round4-done.mp4` 클립 재기록.

## 4. 불변 (라운드 7 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| 시트 첫 렌더에 모드 칩·`시작`(→ `emu_flow` 원본 통과) | emu_flow + lesson_shots |
| 과제 칸: 윤곽선 없음, 준비 `Lavender`·선택 `Periwinkle` 면, 선택 칸 글자 `Paper`, 체크 원 위치 | lesson_shots(수정, 픽셀) |
| Setup ↔ 시트 전환 400 ms 동안 왼쪽 비율이 연속(중간 프레임에 .30 도 .53 도 아닌 값) | lesson_shots(추가: 전환 직후 200 ms 에 왼쪽 폭 측정) + 스트립 캡처 |
| Done: 실차 네 모서리가 칸 U자 안 · 시작 윤곽 픽셀 없음 · 칸 선 `Periwinkle` 존재 | lesson_shots(수정) |
| 주 버튼·알약·잠금·숫자 없음 계약, `ui/` `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 만, 새 문자열 리소스 0 | 기존 + grep |
| **계측 퀴즈 묶음: 답 클릭 직후 `nodes().count { it.isClickable } == 2` 를 폴링(≤ 1 s)으로** — 두 번 흔들린 것, 이번에 같이 | lesson_shots(수정) |

## 5. 안 하는 것

Done 궤적의 데이터·시간 축 변경(양쪽 안 모두 `path` 불변). 새 문자열 리소스. 시트 카테고리 띠·모드 칩 변경.
