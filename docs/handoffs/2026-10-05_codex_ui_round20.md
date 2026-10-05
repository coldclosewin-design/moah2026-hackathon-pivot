# Codex 오더 — 라운드 20: 전체 기능 검토 피드백 중 "바로 고칠 것" (2026-10-05)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/screenshots/lesson/README.md 를 읽어라.
브랜치: origin/main 최신에서 새 워크트리 codex/ui-round20. PR 하나(scope ui).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라.
     시연 본편의 흐름·고정값 불변. 라벨 13개 중 이번에 바꾸는 것은 없다(문구 변경은 아래 ⑤ "아직 → 미수행" 하나).
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → PASS · clashes 0(60/55·100/100·힌트 3종·배지 8),
          adb install -r 뒤 bash tools/course_flow.sh → PASS(백그라운드). 바뀐 캡처 교체 + README 표.
```

## 0. 왜 · 분담

사용자가 22분 전체 기능 검토 영상을 보고 피드백 12건을 줬다(10/5). 둘로 나눈다.

| 묶음 | 누가 | 언제 |
|---|---|---|
| **이 발주(라운드 20)** — 시안 없이 바로 고칠 것 7 | Codex | 지금 |
| 시안을 먼저 보고 고를 것 8(시험장 카드 5 · 상위 메뉴 5 · 돌아가기 3 · 점검 카드 5 · 자세히 보기 5 · Done 시뮬레이션 3 · 지식 테스트 정답 화면 5 · 코스 지도 차 크기 2) | Claude 가 시안 페이지 → 사용자 선택 | 라운드 21 로 발주 |
| 상태기계·코치 문장 버그 2(단순 전진 후 정지에서 "다 되셨나요?" 누락 · 도로 과제 총평의 "근접") | Claude | 별도 PR |

라운드 21 이 같은 화면을 다시 만지므로 **이번엔 구조를 크게 바꾸지 말고** 아래 항목만.

## 바로 고칠 것

| # | 피드백 | 어디 | 바꿀 것 | 확인 캡처 |
|---|---|---|---|---|
| ① | **모든 카드 섹션에 그림자**(예약 확인 포함) | 공통 | 질감 B 의 버튼·과제 카드 그림자(`CoachTexture`)를 **카드형 면 전부**에: 예약 확인 카드(`lesson-venues-booked`), 시험장 카드, 시간대·코스 칩, 출발 전 점검 왼쪽 7칸, 지식 테스트 선택지, Done 판정 패널, 리포트 자세히 보기의 회차 묶음. 새 토큰 0 — 기존 질감 값 재사용. 잠금 화면은 평면 유지(라운드 13 규칙) | 해당 화면 캡처 |
| ② | 상위 메뉴(주차·주행·점검·지식)가 "연습할 과제" 제목에 붙어 있다 | `TaskSheet.kt` 46행 아래 | 제목과 메뉴 사이를 **+32 dp** 내린다(메뉴 모양은 라운드 21 시안) | `lesson-setup-sheet*.png` |
| ③ | "○○ 세부 과제" 제목이 카드에 붙고 크기·색이 안 어울림 | `TaskSheet.kt` 54행 | `Eyebrow` 로 바꿔 **"연습할 과제" 와 같은 색(Periwinkle)·크기(32)**, 카드와의 간격 **+24 dp** | 같은 |
| ④ | "모드" 라벨 | `TaskSheet.kt` 75행 | **"연습할 과제" 와 같은 색·크기**(Periwinkle·32) | 같은 |
| ⑤ | 점검 칸의 "아직" → **"미수행"** | `ChecklistPresentation.kt` 15행, `ManeuverScreen.kt` 58행 | 문구만. 계측의 기대 문자열도 같이 | `lesson-maneuver-checklist*.png` |
| ⑥ | "코치" 와 "4/7" 이 같은 줄로 안 보인다(기준선 어긋남) | `ManeuverScreen.kt` 116~119행 | 두 `Eyebrow` 를 `Modifier.alignByBaseline()` 로(또는 Row `verticalAlignment = Alignment.Bottom` + 같은 lineHeight) — 숫자 글꼴 높이 차로 어긋난다 | `lesson-maneuver-guides*.png` |
| ⑦ | 지식 테스트 `그만하기` 가 결과 없이 첫 화면으로 간다 | `LessonRoute.kt` 40행 | `QuizScreen` 의 마지막 인자를 `vm::restart` → **`vm::endSession`**(상태기계가 푼 문제까지로 `QuizDone` 을 만든다 — 이미 있는 경로). `QuizDone` 의 `다시 시작` 은 그대로 `restart` | `lesson-quiz-done*.png` + 계측: 3문제 뒤 그만하기 → QuizDone 3/3 |
| ⑧ | 빨간 주 알약이 화면마다 길이가 다르다(지식 테스트 `다음 문제` 가 너무 짧음) | `PrimaryPill` | **최소 폭을 Setup `시작` 알약과 같게**(같은 높이·글자 크기). 화면 폭이 좁은 자리만 예외 | `lesson-quiz-answered.png` |

## 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 터치 0 · 운전자 화면 숫자 0(속도 제외) · 잠금 화면 평면 | 계측 |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 색 토큰 0 | grep |
| `feature/`·`scoring/`·`data/` 구조 불변 | `git diff --stat` |

머지 뒤 Claude 가 라운드 21(시안 선택분)을 발주한다.
