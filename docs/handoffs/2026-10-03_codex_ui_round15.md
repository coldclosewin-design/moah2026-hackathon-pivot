# Codex 오더 — 라운드 15: Done 궤적 영역 상하 여백 균등 · 작은 차 마크의 앞뒤 구분 (2026-10-03, 동결 전 마지막 손질)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/02_design_brief.md(불변 표 D7·D8), docs/screenshots/lesson/README.md 를 읽어라.
브랜치: origin/main(#127 이후) 에서 새 워크트리 codex/ui-round15. PR 하나(scope ui). 두 손질 모두 라운드 14 캡처에 대한 사용자 피드백이다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라. 시연 본편(후면 직각 주차)의 흐름·고정값·라벨 13개 불변. 이번엔 후면 Done 캡처도 바뀐다(차 마크 공통 규칙) — 그것은 허용.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS · clashes 0(후면 60/55·100/100·힌트 3종·배지 8 불변), 전면 흐름 1회 PASS·배지 7. 캡처는 docs/screenshots/lesson/ 에 교체하고 README 표 갱신.
```

## 0. 왜

사용자가 라운드 14 캡처(`lesson-done-front-good.png`·`lesson-done-front.png`·`lesson-setup-sheet.png`)를 보고 둘을 짚었다.
① **Done 의 궤적 영역(왼쪽 흰 면)에서 그림의 위아래 여백이 다르다** — 궤적·차·도착 칸이 위에 몰리고 캡션 위가 비어 보인다.
② **작은 차 마크는 앞뒤가 구분되지 않는다** — Done 의 끝 차(검은 실루엣에 흰 유리 둘이 대칭)·시작 윤곽·시트 카드의 차 도식 모두. 전면 주차가 생기면서 "어느 쪽이 앞인가" 가 뜻을 갖게 됐다.

## ① Done 궤적 영역 — 상하 여백 균등

| # | 지금 | 바꿀 것 | 확인 |
|---|---|---|---|
| 1.1 | `EstimatedPath` 캔버스(`weight(1f)`, 위 64 dp·아래 캡션 52 dp)에 `pathViewport` 가 궤적 bbox 를 캔버스 중앙에 맞추지만, 그려지는 것(궤적 + 도착 칸 + 차 + 셰브론 + 시작점)의 실제 상하 여백이 다르다(캡처: 위 ≈ 130 · 아래 ≈ 190 dp) | **보이는 모든 요소**(도착 칸·회전된 차 길이·셰브론 포함)의 bbox 로 맞춰 캔버스 안에서 세로 가운데, 위아래 여백이 같게. 캡션 위 여백과 캔버스 위 여백(64 dp)도 같은 값으로. 가로는 지금처럼 | `lesson-done.png`(후면)·`lesson-done-front.png`·`lesson-done-front-good.png`·점검 Done 은 그대로 |
| 1.2 | — | 시트 카드 그림 면(D8, 288 dp)도 같은 눈으로 점검: 차 도식 박스(240×176)가 정중앙인지, 도식 **안의** 그림(선·차)이 박스 안에서 위아래 균등인지. 다르면 도식 쪽을 맞춘다 | `lesson-setup-sheet.png` |

## ② 작은 차 마크 — 앞뒤 구분(공통 규칙 하나)

Maneuver 도식의 큰 차(앞 유리 넓음·보닛·미러)는 이미 구분된다. 작은 마크 셋에 **같은 규칙**을 쓴다:

| 규칙 | 값 |
|---|---|
| 앞 유리는 넓고 뒤 유리는 좁게 | 앞 유리 폭 = 차폭 .78, 뒤 유리 .58(Maneuver 큰 차의 비율을 그대로 축소) |
| 보닛(앞)은 트렁크(뒤)보다 길고 밝게 | 앞 1/3 을 `Lavender`(선택 카드·잉크 차에서는 `Paper` 70 %)로, 뒤는 몸체색 그대로 |
| 앞 모서리는 둥글고 뒤 모서리는 살짝 각지게 | 앞 코너 반경 = 차폭 .28, 뒤 .14 |

| # | 어디 | 바꿀 것 | 확인 |
|---|---|---|---|
| 2.1 | Done `pathCar`(끝 차, 재생 중 차) | 위 규칙. 색은 `Ink` 몸체 + `Paper` 유리 그대로. 셰브론 위치는 지금대로(진입 방향) | 후면·전면 Done 캡처 |
| 2.2 | Done 시작 차 윤곽(`Lavender` 선) | 같은 모양의 윤곽선 — 앞이 어디인지 윤곽만으로도 보이게 | 같은 캡처 |
| 2.3 | 시트 카드 `ParkingTaskDiagram` 의 차 4종(후면·평행·전면·사선) | 같은 규칙. **후면 카드는 차 뒤가 칸 쪽**, **전면 카드는 차 앞이 칸 쪽**으로 그려져 있는지 확인(지금은 둘 다 같은 방향으로 보인다) — 다르면 전면 카드의 차를 뒤집는다 | `lesson-setup-sheet.png`·`lesson-setup-sheet-front.png` |
| 2.4 | 계측 | 전면 Done 묶음에 "앞 유리가 뒤 유리보다 넓다"(픽셀 폭) 한 줄 | lesson_shots |

## 3. 불변

| 규칙 | 검사 |
|---|---|
| 후면 본편 흐름·고정값·라벨 불변(캡처는 차 마크만 바뀜) | `emu_flow` PASS |
| 주행 잠금 터치 타깃·점수·숫자 0 · 운전자 문장 숫자 없음 · `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 토큰 0 · 새 리소스 0 | lesson_shots · grep |
| 시드·채점·상태기계 **구조** 불변 | `git diff --stat` 에 `feature/`·`scoring/`·`data/` 없음 |

머지 뒤 Claude 가 태그 `inhouse-20261003-3`(동결 태그)을 만든다.
