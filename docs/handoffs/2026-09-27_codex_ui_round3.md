# Codex 오더 — UI 라운드 3: 영상 피드백 반영 · 탑뷰 B 도식 · Done 추정 궤적 · 패널 접힘 (2026-09-27)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/03_round2_feedback.md(피드백 원문·분류), docs/design/round3-topview/README.md(선택 B·Done 궤적 시안), 라운드 2 발주서 docs/handoffs/2026-09-26_codex_ui_round2.md 를 읽어라.
브랜치: codex/ui-round3 를 origin/main 에서 새로 만들어 작업(main 에 AttemptRecord.path·두 문장 멘트·SHOW_DEMO_PANEL·emu_flow 패널 대응이 들어간 뒤 — 이 발주서 PR 머지 확인).
작업: 시연 영상 피드백의 화면 항목 전부(B 14건) + 탑뷰 차 도식을 B 안으로 Canvas 이식 + Done 화면에 추정 궤적 + 시연 패널 접힘 정책 + 공통 문장 규칙. 캡처 전부 교체.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS,
          docs/screenshots/lesson/ 캡처 교체(+ lesson-done-path.png 추가), gh pr create.
```

## 0. 이번 라운드의 세 규칙 (피드백을 관통하는 원인 — `03_round2_feedback.md` §0)

1. **시연 패널은 화면이 아니라 도구다.** 모든 화면에서 **기본 접힘** — 우상단 32 sp 텍스트 `시연` 만. 펼치면 읽는 영역 **위에 떠 있는** 얇은 레일(폭 420 dp, `Lavender` 배경, 본문 폭을 줄이지 않음). **시나리오 버튼을 누르면 자동으로 접힌다.** `SHOW_DEMO_PANEL=false`(빌드 플래그) 면 `demo == null` 이라 아무것도 안 그린다 — 이미 처리됨. `tools/emu_flow.sh` 는 누를 때마다 `시연` 으로 연다(반영됨).
2. **운전자 문장에 숫자 없음.** 회차 멘트(`record.remark`)·총평(`report.summary`)은 상태기계가 이미 두 문장·숫자 없음으로 준다. 화면은 **`\n` 을 줄바꿈으로 존중**하고, 화면 쪽에서 만들던 숫자 줄("1회차 · 이동 N회 · N초", `processLine`, `deltaLines`)을 **운전자 화면에서 제거**한다. 숫자는 Report **자세히 보기**·진단서에만.
3. **문장은 폭에 끌려다니지 않는다.** `Headline` 규칙: `\n` 존중, 최대 3줄, 넘치면 72 → 64 → 56 sp 자동 축소(`onTextLayout` 으로 overflow 감지), `LineBreak.Phrase` 유지. 모든 메인 문장이 이 컴포저블을 쓴다.

## 1. 화면별

### Setup (#2·#3·#4·#22)
- 첫 화면: 라운드 2 그대로 + 패널 접힘(규칙 1). 자막 푸터 유지.
- **시트가 열리면 왼쪽 그림을 30 %** 로 줄이고 읽는 영역 70 %. 과제 카드 3×3: **높이 통일**(행 고정 높이), 제목 **2줄 제한**(`maxLines = 2`, 말줄임), 타입·난이도 한 줄, 준비 중 라벨은 카드 우상단 32 sp.
- 모드 칩: **같은 폭**(`Modifier.width(220.dp)`), 높이 96 dp, 가로 한 줄.
- 하단 행: 왼쪽 `TextAction("돌아가기")`, 오른쪽 `PrimaryPill("시작")` — Report 진단서 화면의 하단 행과 같은 배치.

### Briefing (#5·#6)
- 보이는 것 넷만: 눈썹 `연습 준비` · 메인 문장(`briefingHeadline`) · 부제 · `서두르지 않아도 괜찮아요.` **TTS 원문(`line`) 줄과 자막 줄 제거.** 계측의 `texts().contains(line)` 은 **`allText()`(contentDescription) 로** 바꾸고, `Column` 에 `semantics { contentDescription = line }` 을 단다.
- `음성 안내 중` 32 → **40 sp**, 음파 글리프 비례 확대(80×72 → 104×94 dp).

### Maneuver — 탑뷰 B (#7·#8·#10·#11·#12·#13·#14)
- **차 도식 B** (`docs/design/round3-topview/b-steering-arc.png`) 를 Canvas 로: 차체 Path(보닛·트렁크 구분되는 실루엣), 앞·뒤 창 + 옆 창(보랏빛 면 3~4), 사이드미러 2, **바퀴 4개 고정**(회전 없음, `Periwinkle`). 약 17~21 도형. 뒤가 위.
- **조향 표현 = 방향 호**: 차 앞(화면 아래)에 `Periwinkle` 곡선 하나. `steeringDeg` 에 비례해 휘고(0° 직선 → ±450° 최대 곡률), **운전자 기준 오른쪽 조향은 화면 왼쪽으로** 휜다(앞이 아래라 반전 — README "좌우 판독" 절). 미측정이면 호 없음.
- **후진 표시 = 얇은 빨간 셰브론** 차 뒤(화면 위) 하나, 기어 R 일 때만. **텍스트 `차량 뒤쪽 ↑` 제거.** 접근성: Canvas `contentDescription` 에 "후진 중" 포함(계측이 볼 수 있게).
- 조향각 텍스트(`오른쪽 450°`)와 캡션 `조향 방향 도식` 은 유지.
- 오른쪽 읽는 영역:
  - 상단 좌 `{task.title} · {attempt}회차`(40 sp) — **"1회차 · 이동 N회 · N초" 줄은 제거**(규칙 2). 정차 상태에서도 없다.
  - 메인 문장은 `Headline` 규칙 3. 패널이 접혀 폭이 생기므로 2~3줄 안에 든다.
  - 기어/뒤 거리 값 행은 **위치 고정** — `다 됐어요` 가 없을 때도 같은 자리(버튼 자리는 항상 예약, `Spacer(140.dp)`).
  - `다 됐어요`: **읽는 영역 전폭**, 높이 140 dp, 위 여백 ≥ 48 dp.
- 점검 과제 변형(칩 3개)은 그대로.

### Done — 추정 궤적 (#15·#16)
시안 `docs/design/round3-topview/done-estimated-path.png`. 시그니처는 그대로(`record.path` 사용).
- 왼쪽 **38 %** 궤적 영역(흰 바탕 위, 세로 남색 띠는 왼쪽 6 % 로 축소). `record.path: List<PathPoint>`(미터, 시작 = 원점, +y = 시작 방향 = **화면 위**, `headingDeg` 반시계 양수, `reversing`).
  - 경로 bbox 를 영역에 맞춰 스케일(여백 120 dp, **1 m ≥ 36 dp** 이하로는 안 줄임 → 짧은 경로는 크게). 차 실루엣은 4.5 × 1.8 m 를 같은 스케일로: **시작 차 `Lavender`(옅게), 끝 차 `Ink`(진하게)**, 끝 차는 마지막 점의 `headingDeg` 로 회전.
  - 선: 연속 `reversing` 구간별 Path — 후진 `Periwinkle` 6 dp, 전진 `Lavender` 6 dp.
  - 급정지 점: `record.score.metrics.harshEvents` 의 `tMillis` 에 가장 가까운 `PathPoint` 위치에 빨간 원(반지름 10 dp).
  - 캡션 32 sp `Muted` 두 줄: `신호로 추정한 궤적이에요.` / `실제 위치와 다를 수 있어요.` — **반드시**.
  - `path.isEmpty()` 또는 이동 거리 < 0.5 m(출발 전 점검)면 궤적 영역을 그리지 않고 라운드 2 배치(남색 띠 12 %).
  - Canvas `contentDescription = "추정 궤적"`(계측).
- 오른쪽: 눈썹 `{task.title} · {attempt}회차`, **메인 문장 = `record.remark` 를 `\n` 으로 두 줄**(72 sp, 규칙 3), `PrimaryPill("한 번 더")`, `TextAction("오늘은 여기까지")`. **제거**: `processLine`, `taskDeltaLines`, "칸 안의 위치는 확인할 수 없어요" 줄(캡션이 대신한다). 자막 푸터는 유지(멘트와 같으면 생략).
- 패널 접힘(규칙 1). `emu_flow` 는 `시연` 을 눌러 `문 열기` 를 누른다(반영됨).

### Report (#18·#19)
- 총평 `Headline` 규칙 3(`\n` 세 문장 → 최대 3줄).
- 배지 줄 위에 눈썹 `신호 출처`(32 sp `Muted`). 배지·"주차 과정만 측정했어요." 는 그대로.

### Quiz·QuizDone
- `Headline` 규칙 3 적용 외 변경 없음.

## 2. 불변 (라운드 1·2 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| 라벨 문자열 전부 그대로(시작 / 다 됐어요 / 한 번 더 / 오늘은 여기까지 / 다시 시작 / 시연 / 정차 / 출발 / 문 열기 / 문 닫기 / 시나리오 정지 / 과제·모드 바꾸기 / 돌아가기 / 다음 문제 / 결과 보기 + 시나리오 제목 + 모드 라벨) | emu_flow |
| **패널 기본 접힘**: 첫 렌더에 `시연` 만, 시나리오 라벨 없음. `시연` 클릭 뒤 라벨 보임. 시나리오 클릭 뒤 다시 접힘 | lesson_shots(수정: "demo defaults expanded" → "collapsed, opens on 시연, collapses on play") |
| `locked` 면 클릭·스크롤 노드 0(접힌 `시연` 토글 포함) | lesson_shots |
| `Maneuver` 에 점수·감점·`\d+점`·**`이동 \d+회`·`\d+초`** 없음 | lesson_shots(추가) |
| `Maneuver` 접근성에 기어 R 이면 `후진 중`, 조향 방향 호는 `steeringDeg == null` 이면 없음 | lesson_shots(추가) |
| `Done` 에 `\d+회`·`\d+초`·`지난번보다`·`cm` 없음(눈썹의 `N회차` 는 허용), `path` 가 있으면 `추정 궤적`·캡션 두 줄, 없으면 궤적 없음 | lesson_shots(추가) |
| `Briefing` 터치 0, `line` 은 `allText()` 에 존재 | lesson_shots(수정) |
| `Report` 첫 렌더에 배지·`다시 시작`·`진단서`·(missing 있으면) `이 신호는 이 차에서 받지 못했어요`, `신호 출처` | lesson_shots |
| `Headline` 은 `\n` 을 줄바꿈으로, 4줄 자막 말줄임 없음 | lesson_shots(기존) |
| `ui/` 에 `FontWeight.Bold` 0, 옛 컴포넌트 0 | grep |

계측은 항목을 빼지 않는다. 라운드 2 의 "이동 N회 정차 줄" 검사처럼 **이번 결정으로 뒤집히는 검사는 반대로 고친다**(PR 본문에 적는다).

## 3. 데이터 — 이번 라운드에 새로 쓰는 것

| 필드 | 어디 | 뜻 |
|---|---|---|
| `AttemptRecord.path: List<PathPoint>` | `scoring/PathReconstructor.kt` | `tMillis, x, y, headingDeg, reversing`. 미터, 시작 원점, +y 시작 방향, 반시계 양수 |
| `AttemptRecord.remark` | — | `"서두\n조언"` 두 문장 |
| `LessonReport.summary` | — | `"과제, 모드 N회.\n흐름.\n안전."` 세 문장 |
| `ParkingScore.metrics.harshEvents[i].tMillis` | — | 급정지 점 위치 매칭용 |
| `BuildConfig.SHOW_DEMO_PANEL` | — | false 면 `demo == null` (이미 처리) |

## 4. 캡처

`docs/screenshots/lesson/` 전부 교체 + 추가: `lesson-done-path.png`(잘한 주차 2회차 실제 세션 Done), `lesson-maneuver-b.png`(오른쪽 450° 호 + 후진 셰브론), `lesson-setup-sheet.png`(30/70 시트), `lesson-panel-open.png`(레일 펼침). 실제 세션 캡처는 `emu_flow.sh` 의 `14_done_1.png`·`16_done_2.png` 를 가져오면 된다.

## 5. 참고

- 피드백 원문과 22건 분류: `docs/design/03_round2_feedback.md`. 이 발주서에 없는 항목(유지·A·D)은 그 문서 §1 참조.
- 시안 README 의 좌우 판독 절: B 의 호는 **운전자 기준** 좌우이므로 앞이 아래인 화면에서 반전된다.
- `emu_flow.sh` 는 시나리오를 누른 뒤 자동 접힘을 전제로 다음 탭 전에 `시연` 을 다시 연다(`open_demo_panel`). 라벨 문자열이 바뀌면 스크립트가 멈춘다.
