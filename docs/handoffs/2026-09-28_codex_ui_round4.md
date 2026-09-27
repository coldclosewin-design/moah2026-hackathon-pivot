# Codex 오더 — UI 라운드 4: 움직임과 손맛 (조향 애니메이션 · 궤적 재생 · 버튼 크기 · 시연 도구 정리) (2026-09-28)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/04_round3_feedback.md(피드백 원문·분류·결정 2건), 라운드 3 발주서 docs/handoffs/2026-09-27_codex_ui_round3.md 를 읽어라.
브랜치: codex/ui-round4 를 origin/main 에서 새로 만들어 작업(이 발주서 PR 머지 뒤 — emu_flow 가 content-desc "시연" 도 누른다).
작업: 라운드 3 영상 피드백의 화면 항목 9건 — 조향 도식 애니메이션 + 점선 보조선, Done 궤적 재생, 주 버튼 크기 규칙, 시트 카테고리 띠, 시연 토글 두 안 캡처 + 레일 정리, 조향각 풀이 문구. 먼저 드래프트 PR(토글 두 안 캡처) → 사용자 결정 2건 → Ready.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS,
          docs/screenshots/lesson/ 캡처 교체(+ lesson-demo-toggle-text.png · lesson-demo-toggle-pill.png · lesson-maneuver-guides.png), 애니메이션 2개는 screenrecord 5 초 클립(docs 에 넣지 말고 PR 본문 링크 또는 build/ 경로), gh pr create --draft.
```

## 0. 이번 라운드의 규칙 셋

1. **움직임은 데이터에서.** 조향 애니메이션은 `steeringDeg` 의 변화를, 궤적 재생은 `path[i].tMillis` 를 따른다. 화면이 만들어 내는 가짜 움직임은 없다. 한 번 재생, 반복 없음(시선 유도 규칙 — 주행 중 화면을 계속 보게 하지 않는다).
2. **운전자가 누르는 주 버튼은 한 크기.** `PrimaryPill` 에 `driver = true` 인자(또는 `DriverPill`) — **높이 140 dp · 최소 폭 720 dp**. Maneuver `다 됐어요`(전폭)·Setup 첫 화면 `시작`(전폭)·**시트 `시작`·Done `한 번 더`** 가 이 규칙. 보조 액션(`돌아가기`·`오늘은 여기까지`·`자세히 보기`)은 `TextAction` 그대로.
3. **시연 도구는 더 조용히.** 토글은 두 안 캡처 → 사용자 결정. 레일은 정렬·구분선·간격 규칙(§1).

## 1. 화면별

### Setup 시트 (#1 · #2)
- 과제 카드 왼쪽에 **세로 띠 12 dp** — 유형별: 주차 `Periwinkle` · 주행 `Ink` · 조작 `Signal` · 지식 `Periwinkle.copy(alpha = .4f)`. 준비 중 카드도 띠는 유지(카드 바탕만 회색). 선택 카드(바탕 `Periwinkle`)에서 주차 띠가 안 보이면 띠를 `Paper` 로 반전. **색 토큰을 늘리지 않는다.**
- 하단 행 `시작` → 규칙 2(최소 폭 720·높이 140). `돌아가기` 왼쪽 그대로.

### 시연 토글·레일 (#3 · #4 · #10)
- **드래프트 PR 에 두 안 캡처**(같은 Maneuver 상태):
  - (가) `lesson-demo-toggle-text.png` — 지금(텍스트 `시연`, 밑줄).
  - (나) `lesson-demo-toggle-pill.png` — `Lavender` 알약 **64×32 dp**, 텍스트 없음, 밑줄 없음, 우상단 패딩 top 24 · end 32, 아래 속도값과 **24 dp 이격**. `Modifier.semantics { contentDescription = "시연"; role = Button }` — 계측 `click("시연")` 과 `emu_flow.sh`(`text` 또는 `content-desc` 가 `시연`)가 그대로 돈다. 접힘/펼침 상태는 알약 색으로만(펼침 = `Periwinkle`).
  - 사용자가 고르면 나머지 안을 지운다. 둘 다 **잠금 화면에서는 없음**(라운드 3 불변).
- 펼친 레일(공통 `DemoRail`): 폭 420 dp, 바탕 `Lavender`, 모서리 0, 패딩 24 dp. 안쪽 순서와 규칙:
  1. 토글(우상단 그대로)
  2. 눈썹 `시뮬레이션 신호` → 시나리오 버튼들(**왼쪽 정렬, 같은 폭(전폭), 높이 72 dp, 간격 12 dp**)
  3. `PosterRule` 구분선(위아래 16 dp)
  4. 눈썹 `재생 대기`(또는 진행 `N/M` — 시연 도구라 숫자 허용) → `정차`/`출발` 한 줄 2열 → `문 열기`/`문 닫기` 한 줄 2열 → `시나리오 정지` 전폭
  - 버튼 라벨 문자열은 전부 그대로(`emu_flow` 가 누른다).

### Maneuver — 조향 애니메이션 + 보조선 + 풀이 (#5 · #7, D2 `조수석`)
- `VehicleDiagram`: `val angle by animateFloatAsState(state.steeringDeg ?: 0f, tween(350, easing = FastOutSlowInEasing))` **하나**로 ① 앞바퀴 2개 회전(`wheelRotation(angle)`, ±38°, 운전자 기준 오른쪽 조향 = 앞이 아래이므로 화면에서 `/` 방향 — 라운드 2 의 판독 그대로) ② 방향 호 굴곡(`steeringArcBend(angle)`). 미측정(`steeringDeg == null`)이면 바퀴 직진·호 없음·보조선 없음.
- **점선 보조선 2줄**: 앞바퀴 각각의 중심에서 시작해 호와 **같은 곡률**로 같은 길이, `Periwinkle.copy(alpha = .4f)`, 4 dp, `PathEffect.dashPathEffect(floatArrayOf(12.dp, 12.dp))`. 가운데 호(6 dp 실선)는 그대로. 접근성 텍스트에 `보조선` 한 단어 추가.
- **조향각 풀이**: `steeringTurnsLabel(deg: Float?): String?` 순수 함수(`LessonPresentation.kt`) — `null → null`, `|deg| < 45 → "중립"`, `< 200 → "{방향}으로 조금"`, `< 380 → "{방향}으로 반 바퀴"`, `< 560 → "{방향}으로 한 바퀴"`, `< 740 → "{방향}으로 한 바퀴 반"`, 그 이상 `"{방향}으로 끝까지"`. 값(`오른쪽 450°`) 아래 32 sp `Muted` 한 줄. 단위 테스트 5건. 숫자 값은 계기 값이라 유지.
- **`조수석` 눈썹**: 사용자 결정 D2 — (가) `코치` / (나) `옆자리에서` / (다) 제거. 드래프트는 **(가) `코치`** 로 두고 결정에 따라 바꾼다. 가이드 단계 눈썹(`4/6`)은 그대로.

### Done — 궤적 재생 + 버튼 (#8 · #9)
- `EstimatedPath`: 화면 진입 0.5 s 뒤 **한 번** 재생. `Animatable(0f)` 을 0 → `path.last().tMillis` 로 **3 초**(`tween(3000, easing = LinearEasing)`) 진행시키고 매 프레임:
  - 진행 시각 `t` 까지의 점만 선으로(구간별 색은 그대로), 마지막 구간은 보간.
  - 끝 차(`Ink`) 를 `t` 위치·보간된 `headingDeg` 로 그린다. 시작 차(`Lavender`) 는 처음부터 고정.
  - 급정지 점은 `event.tMillis <= t` 가 된 순간부터.
  - 끝나면 지금의 정지 그림과 **같아야 한다**(계측이 정지 상태를 검사).
- 재생 중에도 `한 번 더`·`오늘은 여기까지` 는 누를 수 있다(잠금 아님). 화면을 떠나면 애니메이션 취소.
- `한 번 더` → 규칙 2(최소 폭 720·높이 140). `오늘은 여기까지` 는 `TextAction` 그대로.
- 캡션 두 줄·`추정 궤적` contentDescription 은 그대로.

### 그 외
- `Report`·`Quiz`·`Briefing` 변경 없음.

## 2. 불변 (라운드 3 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| 라벨 문자열 전부 그대로(시작 / 다 됐어요 / 한 번 더 / 오늘은 여기까지 / 다시 시작 / 시연 / 정차 / 출발 / 문 열기 / 문 닫기 / 시나리오 정지 / 과제·모드 바꾸기 / 돌아가기 / 다음 문제 / 결과 보기 + 시나리오 제목 + 모드 라벨). **알약 안이면 `시연` 은 `contentDescription`** | emu_flow(`text` 또는 `content-desc`) |
| 패널 기본 접힘 → `시연` 으로 열림 → 시나리오 클릭 뒤 접힘, 본문 폭 불변 | lesson_shots(기존) |
| `locked` 면 클릭·스크롤 노드 0(토글 포함) | lesson_shots |
| `Maneuver`·`Done` 에 점수·감점·`\d+점`·`이동 \d+회`·`\d+초`·`지난번보다`·`cm`(Done) 없음 | lesson_shots(기존) |
| `Maneuver` 접근성: 기어 R → `후진 중`, 조향 있음 → `조향 방향 호`·`보조선`, 미측정 → 둘 다 없음 | lesson_shots(수정: `보조선` 추가) |
| `Maneuver` 값 아래 풀이 문구가 값과 같은 방향(`오른쪽 450°` ↔ `오른쪽으로 한 바퀴 반`), 미측정이면 없음 | lesson_shots(추가) + 단위 테스트 |
| `Done` 정지 상태: `추정 궤적`·캡션 두 줄, 재생 중 `한 번 더` 클릭 가능 | lesson_shots(수정: 재생 대기 후 검사 또는 애니메이션 비활성 훅) |
| **주 버튼 4개**(첫 화면 `시작`·시트 `시작`·`다 됐어요`·`한 번 더`) 높이 140 dp·폭 ≥ 720 dp | lesson_shots(추가: bounds 로) |
| 시트 카드마다 유형 띠 1개, 색 토큰 5개 밖의 색 0 | grep(`Color(0x`) 는 `CoachStyle.kt` 에만) |
| `ui/` 에 `FontWeight.Bold` 0 | grep |

애니메이션은 계측으로 못 본다 — **screenrecord 5 초 클립 2개**(조향 0 → 450° → 0, Done 진입 재생)를 PR 본문에 링크. Claude 리뷰는 그 클립과 실제 `emu_flow` 영상으로 본다.

## 3. 데이터 — 새로 쓰는 것 없음

| 필드 | 뜻 |
|---|---|
| `ManeuverDisplayState.steeringDeg: Float?` | 신호마다 갱신 — 애니메이션 입력 |
| `AttemptRecord.path[i].tMillis / x / y / headingDeg / reversing` | 재생 시각·위치·방향 |
| `ParkingScore.metrics.harshEvents[i].tMillis` | 급정지 점 등장 시각 |

## 4. 캡처

`docs/screenshots/lesson/` 교체 + 추가: `lesson-demo-toggle-text.png`·`lesson-demo-toggle-pill.png`(드래프트, 결정 뒤 하나만 남김) · `lesson-maneuver-guides.png`(오른쪽 450° 호 + 점선 2 + 풀이 문구) · `lesson-setup-sheet.png`(띠) · `lesson-panel-open.png`(정리된 레일). 실제 세션 Done 은 `emu_flow` 의 `16_done_2.png`(재생 끝 상태).

## 5. 참고

- 라운드 3 에서 바퀴를 고정한 이유는 **툭 바뀌는 것**이 어색해서였다(`03_round2_feedback.md` #7). 부드럽게 돌면 바퀴를 살린다 — 시안 B 의 정지 그림은 그대로이고 움직임만 붙는다.
- `emu_flow.sh` 는 `시연` 을 `text` 또는 `content-desc` 로 찾는다(이 발주서 PR). 라벨 문자열이 바뀌면 스크립트가 멈춘다.
- 재생 3 초는 회차 44 s 를 압축한 값 — 길면 시선을 잡아 둔다. 상태기계·채점은 손대지 않는다.
