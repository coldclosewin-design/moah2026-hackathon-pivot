# Codex 오더 — 라운드 17: 전면 직각 주차 카드의 주차 라인을 후면처럼 (2026-10-04)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/screenshots/lesson/README.md 를 읽어라.
브랜치: origin/main(#133 이후) 에서 새 워크트리 codex/ui-round17. PR 하나(scope ui).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라. 시연 본편의 흐름·고정값·라벨 13개 불변. 이번 변경은 시트의 전면 카드 도식 한 줄뿐 — Done 의 도착 칸(입구 점선 포함)은 건드리지 않는다.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS · clashes 0(후면 60/55·100/100·힌트 3종·배지 8 불변), 전면 흐름 1회 PASS·배지 7. 캡처는 docs/screenshots/lesson/ 에 교체하고 README 표 갱신.
```

## 0. 왜

사용자(10/4): 라운드 16 의 카드 실루엣은 좋지만, **전면 직각 주차 카드는 위가 막힌 U자 칸 안에 차 앞이 닿아 있어 "벽에 부딪친" 느낌** — 후면 카드처럼 주차 라인을 고쳐 달라.

## ① 전면 카드 칸 = 후면 카드와 같은 양옆 두 줄

| # | 지금 | 바꿀 것 | 확인 |
|---|---|---|---|
| 1.1 | `TaskSheet.kt` `ParkingTaskDiagram` 이 `parking-front` 일 때 칸 위쪽 가로선을 더 그린다(U자, 위가 막힘) | 위쪽 가로선을 **그리지 않는다** — 후면·사선과 같은 양옆 실선 두 줄. 차 방향(앞이 위)·크기·색은 그대로라 앞뒤 구분은 실루엣이 한다 | `lesson-setup-sheet.png`·`lesson-setup-sheet-front.png` |
| 1.2 | 계측에 전면 카드의 "닫힌 끝" 을 기대하는 검사가 있으면 | "양옆 두 줄, 위아래 열림" 으로 바꾼다. Done 도착 칸 검사(`Front edge should be closed` 등)는 **Done 쪽이면 그대로** 둔다 | lesson_shots |

## 2. 불변

| 규칙 | 검사 |
|---|---|
| Maneuver·Done·Report 캡처 불변(카드 한 장만 바뀜) | 캡처 diff |
| 후면 본편 흐름·고정값·라벨 불변 · 잠금 터치 0 | `emu_flow` · lesson_shots |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 토큰 0 | grep |

머지 뒤 Claude 가 태그 `inhouse-20261004-1`(동결 태그 교체)을 만든다.
