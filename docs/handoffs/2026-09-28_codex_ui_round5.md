# Codex 오더 — UI 라운드 5: Done 궤적을 "뒤가 위" 로 (후진이 후진으로 보이게) + 선택 카드 띠 (2026-09-28)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, 라운드 4 발주서 docs/handoffs/2026-09-28_codex_ui_round4.md 의 Done 절을 읽어라.
브랜치: codex/ui-round5 를 origin/main 에서 새로 만들어 작업(이 발주서 PR 머지 뒤).
작업: 두 가지 — ① Done 추정 궤적의 화면 관습을 Maneuver 도식과 같은 "뒤가 위" 로 바꾸고 후진 구간에 셰브론을 붙인다 ② Setup 시트 선택 카드의 유형 띠가 보이게 한다. 궤적 데이터·채점·상태기계는 손대지 않는다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS,
          docs/screenshots/lesson/ 의 Done 3장(lesson-done-path · lesson-done-path-contract · 실제 세션 16_done_2)과 lesson-setup-sheet.png 교체, Done 재생 클립(계측의 lesson-round4-done.mp4 그대로) PR 본문에 경로, gh pr create.
```

## 0. 왜 (라운드 4 B안 영상 피드백, 2026-09-28)

사용자: "주차 후기 시뮬레이션이 **주차에서 나오는** 느낌 — 반대로 재생되는 것 아닌지."
확인 결과 시간 순서·물리는 맞다(`pathThroughTime` 은 시작 → 끝, `PathReconstructor` 는 +y = 차의 앞 = 화면 위, 후진이면 아래로). **관습이 문제다**:

1. Maneuver 도식은 **뒤가 위**(차가 위쪽으로 후진해 들어가는 그림, 셰브론이 위). Done 은 **앞이 위**라 같은 차가 아래로 내려간다 — 화면 사이에서 관습이 뒤집힌다.
2. 움직이는 차 아이콘은 코 방향으로 간다고 읽힌다. 코가 위인데 아래로 가면 "앞으로 빠져나감" 으로 보이고, 자리에 남는 옅은 시작 차가 **주차 칸**처럼 보여 그 인상을 굳힌다.

그래서 **그리기만** 바꾼다. 데이터(`AttemptRecord.path`)와 `PathPresentation` 의 순수 함수(`pathViewport`·`pathLegs`·`pathThroughTime`)는 그대로.

## 1. Done — 뒤가 위 (`EstimatedPath.kt`)

- **캔버스 전체를 180° 회전**: `rotate(180f, pivot = center)` 로 감싸거나 `PathViewport` 의 `x()`·`y()` 를 화면 좌표로 옮길 때 부호를 뒤집는다(둘 중 하나, 순수 함수 시그니처는 유지). 결과: 시작 차가 **아래**, 후진하면 **위**로 들어가고, 오른쪽 조향 후진은 Maneuver 처럼 **화면 왼쪽**으로 휜다(운전자 기준 오른쪽 = 앞이 아래일 때 화면 왼쪽 — `round3-topview/README.md` 좌우 판독 절). `pathCar` 의 유리(앞유리 = 큰 쪽)는 회전 뒤 자연히 아래를 향한다 — 별도 수정 없음.
- 여백·스케일 규칙(120 dp, 1 m ≥ 36 dp) 그대로. bbox 계산은 회전 전 좌표로 해도 대칭이라 같다.
- **후진 셰브론**: 재생 중 `revealed.last().reversing == true` 이면 짙은 차의 **뒤쪽(앞장서는 쪽)** 바깥 0.6 m 지점에 Maneuver 와 같은 빨간(`Signal`) 셰브론 하나(선 6 dp, 폭 = 차폭 × .72, 차 heading 으로 회전). 전진 보정 구간(`reversing == false`)에서는 없음. 재생이 끝난 정지 그림에서는 **없음**(끝 상태 = 주차 완료). 계측: 정지 상태 `contentDescription` 에 셰브론 언급 없음.
- **시작 차**: 지금 `Lavender` 채움 → **`Lavender` 3 dp 윤곽선만**(채움 없음, 유리 생략). "칸" 으로 오해되지 않게. 끝 차(`Ink` 채움)는 그대로.
- 급정지 점·구간 색(후진 `Periwinkle`/전진 `Lavender`)·캡션 두 줄·`추정 궤적` contentDescription·재생 타이밍(0.5 s 뒤 3 s, 한 번)은 라운드 4 그대로.

## 2. Setup 시트 — 선택 카드 띠 (`SetupScreen.kt`)

라운드 4 리뷰 관찰: 선택된 주차 카드의 띠를 `Paper` 로 반전했지만 카드 왼쪽 바깥 페이지 바탕도 `Paper` 라 **띠가 안 보이고 카드가 12 dp 좁아진 것처럼** 보인다.
- 선택 카드의 띠는 **`Ink`** 로(바탕 `Periwinkle` 위에 대비). 다른 카드 규칙(주차 `Periwinkle` · 주행 `Ink` · 조작 `Signal` · 지식 `Periwinkle` 40 %)은 그대로 — 선택되면 유형과 무관하게 `Ink` 띠.
- 색 토큰 5개 밖의 색 없음.

## 3. 불변 (라운드 4 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| `Done` 정지 상태: `추정 궤적`·캡션 두 줄, `\d+회(?!차)`·`\d+초`·`지난번보다`·`cm` 없음, 셰브론 언급 없음 | lesson_shots(기존 + 추가) |
| `Done` 회전: 계측 고정 경로(`PathPoint(1_000, -1f, -3f, …)` 등 y 음수 = 후진)에서 **끝 차의 bounds 가 시작 차보다 위** | lesson_shots(추가: Canvas 는 노드가 없으므로 Compose 테스트 대신 픽셀 검사 — `lesson-done-path-contract.png` 의 위 절반에 `Ink` 픽셀이 있고 아래 절반에 `Lavender` 윤곽이 있는지, 또는 `pathViewport` 를 회전 좌표로 바꿨다면 단위 테스트로 `y(-3f) < y(0f)`) |
| 시트 선택 카드 왼쪽 12 dp 가 `Ink` | lesson_shots(추가: 캡처 픽셀) 또는 grep |
| 주 버튼 4개 140 dp × ≥ 720 dp, 패널 접힘·토글 `contentDescription`, 잠금 터치 0 | lesson_shots(기존) |
| `ui/` 에 `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 에만 | grep |

## 4. 캡처·클립

`lesson-done-path.png`(실제 2회차 — `emu_flow` 의 `16_done_2.png`, 대기 4 s 뒤 정지 그림) · `lesson-done-path-contract.png` · `lesson-setup-sheet.png` 교체. Done 재생 클립은 계측이 남기는 `/sdcard/lesson-round4-done.mp4` 그대로(이름 유지). PR 본문에 회전 전/후 Done 캡처를 나란히.

## 5. 참고

- 이 라운드는 데이터가 아니라 **읽힘**의 문제다. `PathReconstructor` 의 좌표계(+y = 앞)는 그대로 둔다 — 채점·테스트가 그 위에 있다.
- Maneuver 의 관습 "뒤가 위" 는 `03_round2_feedback.md` #7·`round3-topview/README.md` 에서 정한 것. Done 을 거기에 맞추는 것이지 새 관습을 만드는 게 아니다.
