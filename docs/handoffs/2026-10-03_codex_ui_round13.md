# Codex 오더 — 라운드 13: 버튼·카드 질감 B(그림자 + 물방울) · 전면 직각 주차 화면 · 시드 정리 (2026-10-03)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/02_design_brief.md(불변 표 — D7 추가), docs/design/round13-front/(캡처 4장 + texture-preview.html), docs/screenshots/lesson/README.md 를 읽어라.
브랜치: origin/main(#117 이후) 에서 새 워크트리. PR 셋, 순서대로: ① codex/texture-b(scope ui — 질감 B) → ② codex/ui-front-parking(scope ui — 전면 직각 주차 화면) → ③ codex/seed-round13(scope data — aligned 단독 서두 제거 · 퀴즈 5 → 10). ③ 은 시간이 남을 때만(동결 10/6).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조(타입·필드·판정 규칙), build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라. 시연 본편(후면 직각 주차)의 화면·흐름·고정값은 바뀌면 안 된다.
완료 기준(PR 마다): .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS · clashes 0(후면 고정값 60/55·100/100·힌트 3종·배지 8 불변). 캡처는 docs/screenshots/lesson/ 에 교체·추가하고 README 표 갱신.
```

## 0. 왜

10/2 밤 사용자 결정 셋: **질감은 B**(살짝 그림자 + 물방울), **주차 과제는 전면 직각부터 하나만**, **기능 동결은 10/6 그대로**. 전면 직각 주차의 상태기계·채점·힌트·시드는 Claude 가 #115·#116·#117 로 끝냈고 사외 에뮬에서 끝까지 돈다(`docs/design/round13-front/` 캡처). 남은 것은 **화면**이다 — 지금은 후면 화면이 그대로 쓰여 ① 뒤 거리 칸이 "미측정" 으로 보이고 ② 도식·궤적이 "뒤가 위" 다(앞으로 들어가는 과제인데).

화면이 받는 새 값(#116, 전부 기본값이 후면이라 기존 호출은 그대로):

| 어디 | 무엇 | 뜻 |
|---|---|---|
| `Task.parking: ParkingSpec?` · `Task.parkingSpec` | `entryGear`(R/D) · `targetHeadingDeg` · `keys` · `usesRearDistance` · `fixGear` | 과제의 주차 사양. `Done`·`Report`·시트는 `task` 에서 꺼낸다 |
| `ManeuverDisplayState.entryGear` | `Gear.REVERSE` / `Gear.DRIVE` | 도식 방향 — R 이면 뒤가 위(지금), D 면 **앞이 위** |
| `ManeuverDisplayState.rearDistanceApplies` | Boolean | false 면 뒤 거리 칸을 **그리지 않는다**(미측정 아님). 값(`rearDistanceCm`)도 null 로 온다 |

## ① `ui: 질감 B — 살짝 그림자 + 물방울 하이라이트`

사용자가 `docs/design/round13-front/texture-preview.html`(세 열: 지금 / A / **B**)을 보고 B 를 골랐다. 색·글자·레이아웃·도식은 그대로, **면의 질감만** 바뀐다.

| # | 대상 | 바꿀 것 | 수치(디자인 dp, 2560×1268 기준) |
|---|---|---|---|
| 1.1 토큰 | `ui/CoachStyle.kt` | 그림자·하이라이트 토큰을 **한 곳**에 추가(이번 라운드만 새 토큰 허용 — 불변 표 "새 토큰 0" 의 예외, D7). `Color(0x…)` 는 여전히 CoachStyle 에만 | 바깥 그림자: 카드 `Ink` 10 % · y 6 · blur 16, 판정 패널 `Ink` 22 % · y 12 · blur 28, 주 버튼 `Signal` 28 % · y 10 · blur 22, 칩 `Ink` 10 % · y 3 · blur 8. 물방울: 위쪽 하이라이트 `Paper` 42 %→0 (버튼·칩은 높이의 46 %, 카드는 38 %), 아래쪽 안쪽 그림자 `Ink` 6 %(카드)·진한 쪽 18 %(버튼). 바탕 세로 그라데이션은 색 차 2~4 % 이내 |
| 1.2 주 버튼 | `시작`·`다 됐어요`·`한 번 더`·`다시 시작`(140 dp 알약) | 바깥 색 그림자 + 위쪽 하이라이트 + 아래쪽 안쪽 그림자. 눌림(pressed)은 그림자를 반으로 | 미리보기 B 열의 `한 번 더` |
| 1.3 모드 칩·카테고리 메뉴 | 시트의 `가이드/힌트/평가`, 선택 칩 | 얕은 그림자 + 하이라이트. 선택(`Periwinkle`)은 색 그림자 | 미리보기 B 열의 칩 |
| 1.4 과제 카드 | 시트의 면 카드(`Lavender`), 선택 카드(`Ink`) | 그림자 + 위쪽 하이라이트. 선택 카드의 하이라이트는 흰 14 % | 미리보기 B 열의 카드 |
| 1.5 판정 패널·점검 패널 | `Done`·`Report` 왼쪽 아래 남색 면 | 그림자 + 윗선 하이라이트 12 %. 글자·✓/△/✗ 는 손대지 않는다 | 미리보기 B 열의 판정 |
| 1.6 안 바꾸는 것 | `Maneuver` 왼쪽 **조향 도식·차·보조선**, 글자, 눈썹, 배지 줄, 자막 판, 시연 패널 알약 | 평면 기하 그대로(브리프 "도식 = 평면 기하"). 시연 패널은 운전자 화면이 아니라 그대로 | — |
| 1.7 잠금 화면 | 주행 잠금 레이어(라운드 11 ②) | 잠금 중에는 그림자도 없는 평면(터치 타깃처럼 보이면 안 된다) | lesson_shots 5.1 km/h 캡처에서 확인 |

확인: 캡처 — `lesson-setup-sheet.png`(카드·칩)·`lesson-done.png`(버튼·판정 패널)·`lesson-report.png` 교체, 잠금 캡처 불변. **대비**(D6): 하이라이트가 글자 위를 지나면 흰 글자 대비가 떨어진다 — 버튼 글자 영역에서 하이라이트 알파 ≤ 30 %, `Paper/Signal` 3.8:1 유지. 사내 디스플레이에서의 실제 느낌은 사내 재검증 ⑦ 로 본다(아래 §4).

## ② `ui: 전면 직각 주차 화면 — 앞이 위, 뒤 거리 칸 없음, 도착 칸 방향`

| # | 지금(캡처) | 바꿀 것 | 확인 |
|---|---|---|---|
| 2.1 `Maneuver` 도식 | `12_maneuver_front.png`: 후면과 같은 "뒤가 위", 기어 D 에도 셰브론 없음 | `entryGear == DRIVE` 면 차를 **앞이 위**로(회전 180°), 조향 보조선·바퀴 기하는 그대로 따라 돈다. 전진(D)일 때 차 앞쪽(화면 위)에 셰브론 — 후면의 R 셰브론과 같은 모양·색. 후진 보정(R) 중엔 차 뒤쪽(화면 아래)에 | 캡처 `lesson-maneuver-front.png`(D·우 끝)·`lesson-maneuver-front-fix.png`(R 보정) |
| 2.2 `Maneuver` 오른쪽 칸 | `뒤 거리 미측정` 이 보인다 | `rearDistanceApplies == false` 면 뒤 거리 칸을 **빼고** 기어 칸만(폭 재배치). "미측정" 이라고 쓰지 않는다 — 안 재는 것이지 못 잰 게 아니다 | 같은 캡처 |
| 2.3 `Done` 궤적 | `14_done_front_1.png`·`16_done_front_2.png`: 180° 회전된 채라 앞으로 들어간 차가 거꾸로 보인다 | `task.parkingSpec.entryGear == DRIVE` 면 **회전하지 않는다**(앞이 위, +y 가 그대로 위). 도착 칸(U자)은 차의 **뒤쪽이 열린** 모양(후면은 앞쪽이 열림). 재생 중 셰브론은 D 구간에 앞쪽, R 보정 구간에 뒤쪽 | 캡처 `lesson-done-front.png`(△△✓✗)·`lesson-done-front-good.png`(✓✓✓✓) |
| 2.4 `Report` | 그대로 동작(배지 7, 판정 네 줄) | `자세히 보기` 의 근접 행은 뒤 거리 없이 경고 횟수만 — 이미 `parkingDetailLine` 이 null 을 "미측정" 으로 쓰니, 전면이면 그 항목을 "앞 근접 N회" 로. Codex 가 `LessonPresentation` 에서 `task.parkingSpec.usesRearDistance` 로 분기 | 캡처 `lesson-details-front.png` |
| 2.5 시트 카드 | `10b_sheet_front.png`: 이미 선택 가능(`중`) | 그대로. 도식 선(`parking-front` 의 위 선)은 현행 | — |
| 2.6 계측 | — | `LessonScreenInstrumentation` 에 전면 묶음 1개: 시트에서 `전면 직각 주차` 선택 → Maneuver 에 "뒤 거리" 문자열 없음 · 도식 방향 픽셀 검사(차 앞 유리가 위) · Done 네 줄 | `lesson_shots` 3/3 |
| 2.7 `emu_flow` 불변 | 후면 흐름 | 바꾸지 않는다. 전면 흐름은 발주서와 같은 폴더의 `front_flow` 순서(시트 `주차` → `전면 직각 주차` → `힌트` → `시작` → 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 다 됐어요 → 문 열기)로 **한 번** 돌려 로그 `attempt 1: skill=60 safety=55` · `attempt 2: skill=100 safety=100` · `badge=…simulated=7` 을 PR 본문에 | PR 본문 |

## ③ `data: aligned 단독 서두 제거 · 퀴즈 5 → 10` (선택 — 동결 전 여유가 있을 때만)

| # | 지금 | 바꿀 것 | 확인 |
|---|---|---|---|
| 3.1 서두 | `RemarkTemplate(EXCELLENT/GOOD, {"aligned"}, "신호로 추정하면 방향도 맞게 섰어요.")` 가 2회차 서두로 자주 뽑힌다(10/2·10/3 실측 둘 다). 판정 둘째 줄과 같은 말이고 "한 번에 들어갔어요" 보다 약하다 | 두 템플릿을 지운다. `one_go` 서두가 2회차의 기본이 되게. `RemarkPoolTest` 의 판정 필터 테스트가 그대로 통과해야 한다 | 단위 테스트 · `emu_flow` PASS |
| 3.2 퀴즈 | 문항 5 | 10 으로 — 주제는 주차·정차·신호등·횡단보도·고속도로 진입 중에서. 기존 5 의 형식(3지선다·해설 `요.` 끝·숫자는 교육 수치만)과 `SeedCatalogTest` 규칙 그대로. 지식 테스트 과제의 문항 수를 세는 테스트가 있으면 함께 | 단위 테스트 |

## 4. 불변 · 사내 확인

| 규칙 | 검사 |
|---|---|
| 시연 본편(후면 직각)의 화면·흐름·고정값·캡처 불변 | `emu_flow` PASS · 후면 캡처 diff 는 질감 변화만 |
| 주행 잠금 화면 터치 타깃·점수·숫자 0, 잠금 중 그림자 없음 | lesson_shots |
| 운전자 문장 숫자 없음 · `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 문자열 리소스 0 | grep · 단위 테스트 |
| 새 토큰은 이번 라운드의 그림자·하이라이트 묶음만(CoachStyle 한 곳) | 코드 리뷰 |
| 시드·채점·상태기계 **구조** 불변 | `git diff --stat` 에 `feature/`·`scoring/` 없음 |

사내 확인 항목(NEXT 에 ⑦ 로 추가): 질감 B 가 사내 디스플레이에서 "살짝" 으로 보이는지, 전면 직각 주차가 Real 신호로 끝까지 가는지(배지 7).
