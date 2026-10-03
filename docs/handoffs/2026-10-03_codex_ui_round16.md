# Codex 오더 — 라운드 16: 주차 세부 과제 카드의 차를 실루엣으로 (2026-10-03)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/round13-front/card-silhouette-preview.html(시안 — 아래 줄), docs/screenshots/lesson/README.md 를 읽어라.
브랜치: origin/main(#130 이후) 에서 새 워크트리 codex/ui-round16. PR 하나(scope ui).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라. 시연 본편의 흐름·고정값·라벨 13개 불변. Done 의 작은 차 마크(SmallCarMark)는 이번 범위가 아니다 — 카드만.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS · clashes 0(후면 60/55·100/100·힌트 3종·배지 8 불변), 전면 흐름 1회 PASS·배지 7. 캡처는 docs/screenshots/lesson/ 에 교체하고 README 표 갱신.
```

## 0. 왜

사용자(10/3 오후): 라운드 15 의 카드 차(둥근 사각형 + 사다리꼴 유리 둘, `SmallCarMark`)가 **너무 별로** — 자동차 실루엣으로. 시안(`card-silhouette-preview.html` 아래 줄)은 **Maneuver 화면의 큰 차**(`VehicleDiagram` 의 몸체·보닛/트렁크 패널·앞/뒤 유리·옆 유리·미러 경로, 라운드 4·7 에서 사용자가 승인한 그 차)를 축소해 쓴다. 앞뒤 구분은 실루엣 자체가 한다(큰 앞 유리, 앞쪽 미러, 긴 보닛).

## ① 카드 차 = Maneuver 차 실루엣

| # | 바꿀 것 | 수치 |
|---|---|---|
| 1.1 경로 공유 | `VehicleDiagram` 의 몸체·패널·유리·미러 경로(100×250 좌표, 뒤가 위)를 **함수 하나로 뽑아** Maneuver 와 카드가 같이 쓴다(바퀴·보조선·셰브론은 Maneuver 쪽에만). 경로 숫자는 바꾸지 않는다 | 가로 배율은 지금처럼 차폭 = 차길이 × .43 |
| 1.2 카드 4종 | `ParkingTaskDiagram` 의 `smallCarMark` 를 1.1 로 교체. **후면 = 경로 그대로(뒤가 칸 안쪽 = 위)**, **전면 = 180°(앞이 위, 칸의 닫힌 쪽)**, 평행 = 뒤가 위, 사선 = 뒤가 위 + 30° | 차길이 160 dp(사선은 지금처럼 맞춤 축소) |
| 1.3 색 | 선택 카드: 몸체 `Paper` · 보닛/트렁크 `Lavender` · 유리 `Periwinkle`(Maneuver 와 같은 조합). 일반 카드: 몸체 `Ink` · 패널 `Periwinkle` · 유리 `Lavender`. 준비 중: 지금처럼 55 % 불투명 | 새 토큰 0 |
| 1.4 칸 선 | 지금 그대로(후면·사선 양옆 실선, 평행 오른쪽 점선, 전면 위·양옆) | — |
| 1.5 계측 | 시트 캡처 픽셀 검사 한 줄: 후면 카드는 큰 유리(앞)가 아래, 전면 카드는 큰 유리가 위 | lesson_shots |

확인 캡처: `lesson-setup-sheet.png`(후면 선택)·`lesson-setup-sheet-front.png`(전면 선택) 교체. Maneuver 캡처는 **바뀌지 않아야** 한다(경로 추출만).

## 2. 불변

| 규칙 | 검사 |
|---|---|
| Maneuver 도식·Done·Report 캡처 불변(카드만 바뀜) | 캡처 diff |
| 후면 본편 흐름·고정값·라벨 불변 · 잠금 터치 0 | `emu_flow` · lesson_shots |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 토큰 0 · 새 리소스 0 | grep |
| 시드·채점·상태기계 구조 불변 | `git diff --stat` 에 `feature/`·`scoring/`·`data/` 없음 |

머지 뒤 Claude 가 태그 `inhouse-20261003-4`(동결 태그 교체)를 만든다.
