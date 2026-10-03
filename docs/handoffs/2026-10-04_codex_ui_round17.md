# Codex 오더 — 라운드 17: Done 의 차도 카드와 같은 실루엣으로 · 전면 카드 주차 라인 (2026-10-04)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/screenshots/lesson/README.md 를 읽어라.
브랜치: origin/main(#135 이후) 에서 새 워크트리 codex/ui-round17. PR 하나(scope ui).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라. 시연 본편의 흐름·고정값·라벨 13개 불변. Maneuver 도식은 바꾸지 않는다. 이번엔 후면·전면 Done 캡처가 바뀐다(차 모양) — 허용.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS · clashes 0(후면 60/55·100/100·힌트 3종·배지 8 불변), 전면 흐름 1회 PASS·배지 7. 캡처는 docs/screenshots/lesson/ 에 교체하고 README 표 갱신.
```

## 0. 왜

사용자(10/4) 둘:
1. **Done 화면의 차가 주차 세부 카드의 차와 아예 다른 모양이다** — 카드와 같은 그림(라운드 16 의 `VehicleSilhouette`, Maneuver 큰 차 실루엣)이어야 한다. 지금 Done 은 라운드 15 의 `SmallCarMark`(둥근 사각형 + 사다리꼴 유리)를 쓴다.
2. **전면 직각 주차 카드는 위가 막힌 U자 칸이라 "벽에 부딪친" 느낌** — 후면 카드처럼 주차 라인을 고친다.

## ① Done 의 차 = `VehicleSilhouette`(카드·Maneuver 와 같은 모양)

| # | 어디 | 바꿀 것 | 수치·색 |
|---|---|---|---|
| 1.1 | `EstimatedPath` 의 끝 차·재생 중 차(`pathCar`) | `smallCarMark` 대신 `vehicleSilhouette`. 경로는 뒤가 위(100×250)이니 `PathPoint` 의 앞(+y·heading 0 = 화면 위)과 맞게 **180° 돌려** 그린다 — 앞 유리·미러가 진행 방향 쪽 | 차길이 4.5 m(지금 그대로), 폭은 실루엣 비율(.43 × 길이). 색 = 카드 "일반" 조합: 몸체 `Ink` · 패널 `Periwinkle` · 유리 `Lavender` |
| 1.2 | 시작 자세 윤곽 | 같은 실루엣을 흐리게: 몸체 `Lavender` · 패널/유리 `Paper` (채움, 선 아님) | — |
| 1.3 | 셰브론·도착 칸·입구 점선·급제동 점·캡션 | 그대로. 셰브론 위치만 새 차길이에 맞으면 됨 | — |
| 1.4 | 세로 가운데(`pathVerticalBounds`) | 실루엣의 **미러 폭**(경로 x −8…108)까지 넣어 다시 계산 — 라운드 15 의 위아래 균등이 유지돼야 한다 | 계측 `Path painted margins` 그대로 통과 |
| 1.5 | `SmallCarMark` | Done 에서도 안 쓰게 되면 파일·`SmallCarGeometry` 를 지운다(카드는 #132 에서 이미 안 씀). 계측 `Small car windows` 는 "앞 유리(큰 유리)가 진행 방향 쪽" 검사로 바꾼다 | — |

확인 캡처: `lesson-done.png`(후면)·`lesson-done-front.png`·`lesson-done-front-good.png`·`lesson-done-front-replay-d.png`·`lesson-done-front-replay-r.png`·`lesson-done-path.png` 교체.

## ② 전면 카드 칸 = 후면 카드와 같은 양옆 두 줄

| # | 지금 | 바꿀 것 | 확인 |
|---|---|---|---|
| 2.1 | `TaskSheet.kt` `ParkingTaskDiagram` 이 `parking-front` 일 때 칸 위쪽 가로선을 더 그린다(U자) | 위쪽 가로선을 **그리지 않는다** — 후면·사선과 같은 양옆 실선 두 줄. 차 방향(앞이 위)은 그대로라 앞뒤는 실루엣이 구분 | `lesson-setup-sheet.png`·`lesson-setup-sheet-front.png` |
| 2.2 | 계측에 전면 카드의 "닫힌 끝" 기대가 있으면 | "양옆 두 줄" 로. Done 도착 칸 검사는 Done 쪽이면 그대로 | lesson_shots |

## 3. 불변

| 규칙 | 검사 |
|---|---|
| Maneuver·Report 캡처 불변 | 캡처 diff |
| 후면 본편 흐름·고정값·라벨 불변 · 잠금 터치 0 | `emu_flow` · lesson_shots |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 토큰 0 | grep |
| 시드·채점·상태기계 구조 불변 | `git diff --stat` 에 `feature/`·`scoring/`·`data/` 없음 |

머지 뒤 Claude 가 태그 `inhouse-20261004-1`(동결 태그 교체)을 만든다.
