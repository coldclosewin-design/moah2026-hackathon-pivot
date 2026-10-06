# Codex 오더 — 라운드 25b: 말 카드 · S자 곁가지 · 도로 상식 · 도로 표시 그림 (2026-10-06)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/13_round25_decisions.md,
         docs/design/12_home_coach_dialog.md "말 카드" 절, 시안 docs/design/round25-proposals/7-word-cards.html(시안 A) · 4-track-center.html(시안 A) ·
         5-road-knowledge.html(A · C′ · H)를 읽어라(HTML 이 안 열리면 같은 이름 .png).
시작 조건: 라운드 25a(codex/ui-round25a) 머지 + Claude 모델 PR 다섯(M1 말 카드 · M2 시나리오 끝 정차 · M3 S자 곁가지 · M4 도로 상식 과제 · M5 도로 표시 퀴즈) 머지.
          origin/main 최신에서 새 워크트리, 브랜치 codex/ui-round25b, scope ui, PR 하나.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지(필요하면 INTEGRATION C 절에). docs/NEXT.md 금지.
     도구가 찾는 라벨 불변(라운드 25a 목록 + 코치와 대화 · 코치에게 글로 말해 보세요 · 보내기). 새 색 토큰 0 — 단 ③ 의 도로 표시 그림 안에서만 예외(아래).
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", RESERVE=1 bash tools/emu_flow.sh → PASS · clashes 0,
          adb install -r 뒤 bash tools/course_flow.sh → PASS. 바뀐 캡처 교체 + README 표. 에뮬은 네 것(emulator-5556)만.
```

선행 상태: ✅ M1 #211 · M2 #212 · M3 #215 · M4 #213 · M5 #214(main `dfebf32`, 테스트 350) · ✅ 25a #217 `7009e5a` — 시작 가능. `lesson_shots` 상한은 #218(900 s · `ANDROID_SERIAL`) 머지 뒤 래퍼 없이

## 모델 (Claude — 화면이 쓰는 값)

| 값 · 진입점 | 뜻 |
|---|---|
| `setup.coachInput: CoachInputMode` (`OFF` · `CARDS` · `CARDS_AND_TEXT`) | `OFF` = 칩만 · `CARDS` = 말 카드만, **입력 칸·보내기를 접는다**(키보드가 없는 사내) · `CARDS_AND_TEXT` = 카드 + 글. `setup.coachTextInput` 은 `coachInput != OFF` 와 같다 |
| `setup.coach.cards: List<SpeechCard>` | 지금 보일 카드(대화 단계 · 예약 · 지난 기록에 맞는 것만). `id`, `text`(한 줄, 숫자 없음) |
| `vm.sendCoachCard(id)` | 카드를 누르면. 시트에 없는 카드·답 대기 중이면 무시 |
| `admin.coachInput` · `admin.setCoachInput(mode)` | 준비실 토글을 세 칸으로. 예전 `setTextInput(on)` 은 켬 = 카드 + 글 |
| 코스 지도 | 장내 도면에 S자 곁가지 도로 + 라벨 "S자 연습 · 시험 항목 아님" 이 이미 `MapShape.Road`/`Label` 로 들어 있다. 연습 모드(가이드·힌트)의 `Drive.course.route` 는 곁가지를 지나는 길. 시연 시나리오 `S자 연습` 추가 |
| 지식 과제 | 지식 분류 카드가 여섯(기존 + 노면 표시·차선 · 신호와 우회전 · 교차로 우선 · 운전 매너 · 헷갈리는 상식). 헷갈리는 상식은 두 보기(맞아요/아니에요) |
| 점검 분류 퀴즈 | 점검 분류에 둘째 카드 "도로 표시 읽기"(`Task.quizOnly` — 모드는 지식 테스트 하나). `QuizItem.figure: RoadFigure?` — `LEFT_ARROW` · `STRAIGHT_LEFT_ARROW` · `WHITE_SOLID` · `YELLOW_SOLID_CENTER` · `ZIGZAG` · `BLUE_BUS_LANE` |

## 항목 (Codex)

| # | 어디 | 할 것 · 주의 |
|---|---|---|
| ① | 코치 시트(`CoachTextSheet`) · 준비실 | 시안 7 **A**: 입력 줄 바로 위에 `coach.cards` 를 **가로 스크롤 한 줄**(카드 높이 ≥ 96 dp, Paper 면 + Periwinkle 테두리 + 앞 마이크, 한 줄 글자, 오른쪽 끝 흐림 + `›` 로 더 있음 표시). 누르면 `vm.sendCoachCard(id)`. `waiting` 이면 카드 비활성. **`coachInput == CARDS` 면 입력 칸·`보내기` 를 그리지 않는다**(카드 줄이 그 자리로 내려온다), 배지 "음성 입력 · 시뮬레이션" 은 그대로. 준비실 "시뮬레이션 음성 입력" 을 **끔 / 카드 / 카드 + 글** 세 칸으로(`admin.setCoachInput`) — 왼쪽 "지금" 줄도 그 값으로 |
| ② | 장내 코스 지도(`CourseMap`) | S자 곁가지 도로·라벨이 다른 도로와 같은 모양으로 그려지는지, 라벨이 도로와 겹치지 않는지 확인(필요하면 라벨 글자 크기만). 연습 모드에서 `S자 연습` 시나리오로 지도 위 차가 곁가지를 도는 캡처 |
| ③ | 퀴즈 화면(`Quiz`) | `item.figure` 가 있으면 문항 위(또는 왼쪽 Ink 패널)에 **위에서 본 차로 도식**: 화살표(좌회전 · 직진+좌회전), 흰 실선, 노란 실선 중앙선, 지그재그 선, 파란 버스전용차로. **색 예외(사용자 허용, 10/6 밤)**: 도로 표시 그림 안에서만 실제 의미 색 두 가지(노랑 · 파랑)를 쓴다 — 그림 밖은 토큰 다섯 그대로. 그림은 정차 화면이라 잠금(속도 > 5)이면 지금처럼 평면 잠금 |
| ④ | 과제 시트 | 지식 분류 카드 여섯·점검 분류 카드 둘이 카드 C 모양 그대로 넘겨지는지(난이도 하·중·상 꼬리표), 점검의 "도로 표시 읽기" 는 모드 트랙에 `지식 테스트` 한 칸(25a ④). 시트 숫자 0 |

## 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 터치 0 · 운전자 화면 숫자 0 · 잠금 평면 | 계측 |
| `coachInput == OFF` 시트 = 25a 시트 · 도구 라벨 불변 · `emu_flow`·`course_flow` PASS(모의시험 70 → 100 그대로) | 도구 |
| Real 빌드(`admin == null`)에 카드·입력·준비실 0 | 계측 |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만(도로 표시 두 색은 CoachStyle 에 "RoadMarking" 이름으로) | grep |

캡처: 새 `lesson-setup-coach-cards.png`(첫 화면 카드) · `lesson-setup-coach-cards-follow.png`(되물은 뒤) · `lesson-setup-coach-cards-only.png`(입력 칸 접힘) · `lesson-drive-exam-practice-s.png` · `lesson-quiz-road-sign.png` · `lesson-setup-sheet-knowledge.png`·`lesson-setup-sheet-checklist.png` 교체 · `lesson-admin-home.png` 교체.

머지 뒤 Claude 가 리뷰하고, `inhouse_check` 에 말 카드 한 장 단계를 더한 뒤 태그를 만든다.
