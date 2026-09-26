# Codex 오더 — UI 재설계 라운드 2: 나머지 화면 전부 (`Setup`·`Briefing`·`Done`·`Quiz`·`QuizDone`·점검 과제 `Maneuver`) + 에셋 (2026-09-26)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/02_design_brief.md, docs/design/geometric-poster-development/README.md, 라운드 1 발주서 docs/handoffs/2026-09-26_codex_ui_round1.md 를 읽어라.
브랜치: codex/ui-round2 를 origin/main 에서 새로 만들어 작업 (main 에 ManeuverDisplayState 의 점검용 필드와 emu_flow 의 시트 대응이 들어간 뒤 — PR #16 머지 확인).
작업: 라운드 1 디자인 시스템(CoachStyle·LessonComponents)으로 남은 화면을 전부 다시 그린다 — Setup(제안 문장 + 과제·모드 시트), Briefing(핸들 B 에셋), Done, Quiz·QuizDone(새 화면 2장), 출발 전 점검 과제용 Maneuver(칩 3개).
      라운드 1 리뷰 관찰 2건 반영. 옛 컴포넌트(LessonCanvas·LessonFrame·LessonCard·LessonButton) 제거. 글자 없는 일러스트 에셋 2개.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라. 시드 **문구**(제안 문장·퀴즈 문항·멘트)는 다듬어도 된다.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS,
          캡처 전부(lesson-*.png, docs/screenshots/lesson/ 교체)를 PR 에 첨부, gh pr create (드래프트 아님 — 라운드 1 에서 톤은 확정됐다).
```

## 1. 범위 한 장

| 화면 | 시안 | 바뀌는 것 |
|---|---|---|
| `Setup` | `01-setup-v1.png` | 제안 문장 + 빨간 **시작** + `과제·모드 바꾸기` 시트. 준비 중 과제 회색, 미지원 모드 숨김. 예약 카드는 시트 안 |
| `Briefing` | `steering-alternatives/b-oblique-rim-v1.png` | 왼쪽 문장, 오른쪽 핸들 B 에셋, `음성 안내 중`. 터치 0 |
| `Done` | 시안 없음 — README "회차 완료" 행 | 흰 바탕 + 작은 색면, 멘트가 주인공, **한 번 더** / 오늘은 여기까지 |
| `Quiz`·`QuizDone` | 시안 없음 — Report 구도 변주 | 새 화면 2장(INTEGRATION C절 9/26 지식 테스트 요청) |
| `Maneuver` 점검 변형 | 시안 없음 — Maneuver 구도 유지 | `taskType == CHECKLIST` 면 도식 대신 벨트·기어·시동 칩 3개 |
| `Maneuver`·`Report` | 라운드 1 그대로 | 관찰 1(접힌 상태 라벨) 반영만 |
| 공통 | — | `BrandMark` 통일(Bold 0), 옛 컴포넌트 삭제, 라벨 리소스 추가 |

## 2. 에셋 — 글자 없는 일러스트 2개

시안 이미지에는 글자가 박혀 있어 그대로 쓸 수 없다. **글자·UI 없이 그림만** 다시 생성한다(`image_gen`, 입력은 해당 시안 1장 + "remove all text and UI, keep the illustration and color fields only").

| 파일 | 내용 | 영역 |
|---|---|---|
| `res/drawable-nodpi/poster_car.webp` | 설정 시안의 왼쪽 — 크게 잘린 자동차 + 남색·보랏빛 색면·원호. 오른쪽 끝은 흰색으로 끝나 읽는 영역과 이어진다 | 화면 왼쪽 약 53 %(1360 × 1268) |
| `res/drawable-nodpi/poster_wheel_b.webp` | 핸들 B 사선 단면 — 남색 배경 위 핸들만 | 화면 오른쪽 약 43 %(1100 × 1268) |

규칙: 팔레트 5색만(`#070827 #FCFCFA #5B60A1 #E5E6F0 #F52D48`), 평면, 각 **1 MB 이하**(WebP, 무손실 또는 q90), `contentDescription = null`(장식). `Image(contentScale = Crop)` 로 영역에 맞춘다. 생성이 안 되면 막히지 말고 **Canvas 기하(원호·사선·사각)** 로 대신 그리고 PR 에 적는다 — 에셋은 라운드 3 에서 넣어도 된다.

## 3. `Setup` — 시안 `01-setup-v1.png`

왼쪽 53 % `poster_car` (또는 Canvas 대체), 오른쪽 읽는 영역.

**첫 화면(요약)**
- 좌상단 `BrandMark`(그림 위면 `Ink` — 시안은 흰 바탕 모서리).
- 눈썹: `profileLine(profile)` — "연수생 · 장롱 10년차 · 목표 아이 등하원"(32 sp `Muted`).
- 메인 문장(72–88 sp, 2줄): **제안 문장**. 과제 유형별 시드 문구 — 주차 "오늘은 가볍게,\n주차부터 해볼까요?" / 점검 "시동 켜기 전,\n순서부터 맞춰 볼까요?" / 지식 "정차 중이니\n머리로 한 번 풀어 볼까요?" (문구는 다듬어도 된다. `SetupScreen` 안의 매핑 함수로, 시드 구조는 건드리지 않는다).
- 부제 40 sp: `{task.title} · {mode.label} 모드`. 그 아래 40 sp `Muted`: `selectionReason(...)`(제안 그대로면 `reason`, 바꿨으면 `task.summary`).
- `PrimaryPill("시작")` → `onBegin(task.id, mode)`. 아래 `TextAction("과제·모드 바꾸기")` → 시트.
- 하단 옅은 구분선 + `subtitle`(자막, 32–40 sp `Muted`, 있을 때만).

**과제·모드 바꾸기 시트** — Report 의 "자세히 보기" 와 같은 방식: **오른쪽 읽는 영역만 바뀐다**(화면 전환·다이얼로그 아님).
- 눈썹 `연습할 과제`. 과제 3×3 그리드(`tasks`): 제목 40 sp + `타입 · 난이도` 32 sp. 선택 = `Periwinkle` 채움 + `Paper` 글자, 접근성 `selected`. **`Task.isReady == false` 면 회색(`Lavender` 배경·`Muted` 글자) + 32 sp `준비 중`(`TaskStatus.label`) + 클릭 불가**(`enabled = false`, 접근성에 클릭 액션 없음).
- 눈썹 `모드`. 칩은 **`task.supports(mode)` 인 것만**(주차·점검 → 가이드/힌트/평가, 지식 → 지식 테스트). 라벨은 `LessonMode.label` 그대로. 선택 칩 `Periwinkle`.
- 예약 카드(`reservation`): 눈썹 `제휴 시험장 예시` + venue·slot·course 40 sp + `note`("시드 데이터 — 실제 예약 연계 없음") 32 sp `Muted`. 카드 테두리 없이 구분선으로.
- 하단: `PrimaryPill("시작")` + `TextAction("돌아가기")`. **시작은 요약과 시트 양쪽에 있다** — `emu_flow` 가 시트에서 `힌트` → `시작` 을 누른다.
- 시연 패널: 우상단 `시연` 토글, 라운드 1 Maneuver 와 같은 세로 레일, **기본 펼침**(계측이 첫 렌더에서 `잘한 주차` 를 본다). 과제 미정이라 시나리오 4개 전부. 레일이 읽는 영역을 가리지 않게 읽는 영역 폭을 줄인다(Maneuver 와 같은 방식).

## 4. `Briefing` — 시안 `b-oblique-rim-v1.png`

시그니처를 `BriefingScreen(task: Task, mode: LessonMode, line: String, subtitle: String?)` 로 바꾼다(`LessonRoute` 에서 `state.task, state.mode` 전달. `Quiz` 자리표시자 호출부는 이 PR 에서 없어진다).
- 왼쪽 57 % 흰색: `BrandMark`, 눈썹 `연습 준비`, 메인 문장 80 sp 2줄 = **오늘 볼 것** — `task.watch` 앞 두 개로 "핸들 방향과\n기어 전환을 볼게요." (조사는 화면에서 붙인다 — 마지막 항목이 받침으로 끝나면 `을`, 아니면 `를`. `ports/Models.kt` 의 `withSubjectParticle` 은 이/가 용이니 참고만). 부제 40 sp `{task.title} · {mode.label} 모드`. 40 sp `Muted` `서두르지 않아도 괜찮아요.`.
- 그 아래 `line`(TTS 문장) 32 sp `Muted`, 4줄까지 — 계측이 `texts().contains(line)` 을 본다.
- 좌하단: 음파 글리프(Canvas 세로 막대 5개, 정적 또는 0.8 s 주기의 잔잔한 높이 변화) + `Periwinkle` 32 sp `음성 안내 중`.
- 오른쪽 43 %: `poster_wheel_b`(Crop). **터치 타깃 0**(계측).

## 5. `Done` — 시안 없음, README "회차 완료" 행

흰 바탕. 왼쪽 12 % 정도의 세로 남색 띠 하나(가장자리 크롭된 색면) — Report 의 큰 그래픽을 반복하지 않는다. 시그니처 `DoneScreen(task: Task, attempt: Int, record: AttemptRecord, subtitle: String?, onAgain, onEnd, demo)`.
- `BrandMark`, 눈썹 `{task.title} · {attempt}회차`.
- 메인 문장 72 sp 최대 3줄 = `record.remark`(머리말 "2번 만에, 26초." 가 이미 앞에 붙어 온다 — `attemptHead(task, score)` 를 다시 붙이지 않는다).
- 과정 한 줄 40 sp `Muted`: 주차 → `metricLines(record.score.metrics)` 를 ` · ` 로 이어서("이동 4회 · 44초 · 조향 왕복 3회 · 기어 전환 2회 · 뒤 최소 35 cm"). **점검(`task.type == CHECKLIST`)** → `record.score.metrics.preDrive` 로 "벨트 2초 · 시동 5초 · 벨트 먼저 · 움직임 0회"(`beltOnMillis`·`ignitionOnMillis`·`beltBeforeIgnition`(true "벨트 먼저"/false "시동 먼저"/null 생략)·`motion.movingSegments`). 미측정은 생략.
- `deltaLines(record.delta)` 가 있으면 한 줄씩 40 sp — 좋아진 것 `Periwinkle`, 나빠진 것 `Muted`. `delta == null` 이면 "지난번보다" 문자열 없음(계측).
- 주차만 32 sp `Muted`: `칸 안의 위치는 확인할 수 없어요. 오늘은 주차 과정을 돌아봤어요.`
- `PrimaryPill("한 번 더")` → `onAgain`, `TextAction("오늘은 여기까지")` → `onEnd`.
- 시연 패널 레일 유지(**`emu_flow` 가 Done 에서 `문 열기` 를 누른다**), 기본 펼침. 자막은 하단 구분선 아래 32 sp.

## 6. `Quiz` · `QuizDone` — 새 화면 2장 (INTEGRATION C절 9/26)

데이터: `LessonPhase.Quiz(task, index, total, item, locked, chosen, correctSoFar)`(`answered`·`isLast` 계산 속성), `LessonPhase.QuizDone(task, results, items, remark)`(`correct`·`total`). ViewModel: `answer(choice: Int)`, `nextQuestion()`, `restart()`. `LessonRoute` 의 `[cross]` 자리표시자 줄을 이 두 화면 호출로 바꾼다.

**`Quiz`** — Report 구도 변주: 왼쪽 38 % 남색 형태 + 큰 숫자 = **문제 번호** `index + 1`(두 자리 0 채움 "01"), 눈썹 `{index + 1} / {total}`. 오른쪽:
- 눈썹 `지식 테스트 · {task.title}`, 메인 문장 72 sp = `item.question`.
- **선택지 3개**: 세로로 쌓인 전폭 행(높이 ≥ 120 dp, 48 sp, `Lavender` 배경, 왼쪽에 32 sp `1` `2` `3`). 누르면 `answer(i)`. 답하기 전엔 셋 모두 클릭 가능.
- `locked` 면 **선택지를 그리지 않고** 40 sp `정차 후 답해 주세요` 만 — 클릭·스크롤 노드 0(시연 패널 포함, Maneuver 와 같은 게이트).
- 답한 뒤(`chosen != null`): 정답 행 `Periwinkle` 채움 + `Paper` 글자, 고른 오답 행은 `Muted` 글자 + `Lavender` 유지(붉게 하지 않는다), 나머지는 흐리게. 아래 `item.why` 40 sp. `PrimaryPill(isLast ? "결과 보기" : "다음 문제")` → `nextQuestion()`. 답하기 전엔 이 버튼 없음.
- 하단 32 sp `Muted`: `맞은 문제 {correctSoFar}`. 자막은 별도로 두지 않는다(질문·이유가 곧 TTS 문장).

**`QuizDone`** — 왼쪽 큰 숫자 = **맞은 수** `correct`(두 자리), 눈썹 `맞은 문제 / {total}`. 오른쪽: 눈썹 `지식 테스트 · {task.title}`, 메인 문장 = `remark`, 문제 목록(`items` ↔ `results` 를 `itemId` 로 짝지음): 한 줄 40 sp `{n}. {question}` + 맞음 `Periwinkle` "맞았어요" / 틀림 `Muted` "정답: {choices[answer]}" + 다음 줄 32 sp `why`(틀린 것만). 안 푼 문항(중간 종료)은 `Muted` "안 풀었어요". `PrimaryPill("다시 시작")` → `restart()`.

라벨 리소스 추가: `다음 문제` / `결과 보기` / `과제·모드 바꾸기` / `돌아가기` / `정차 후 답해 주세요`. `준비 중` 은 `TaskStatus.label`.

## 7. `Maneuver` 점검 변형 — `state.taskType == TaskType.CHECKLIST`

`ManeuverDisplayState` 에 `taskType`·`belt`·`ignitionOn`·`beltSignal`·`ignitionSignal` 이 추가됐다(PR #16). 점검 과제면:
- 왼쪽 남색: 차 도식·화살표·조향각 대신 **칩 3개 세로**(각 높이 ≈ 220 dp): 눈썹 `안전벨트` / `기어` / `시동` + 값 80 sp — 벨트 `채움`/`아직`, 기어 `state.gear`(P 이면 `P`), 시동 `켜짐`/`꺼짐`. 충족(벨트 true·기어 P·시동 true) = `Paper` 글자 + 왼쪽 8 dp `Periwinkle` 세로 막대, 미충족 = `Paper` 60 %. 미측정(값 null 또는 신호 MISSING) = `미측정` 회색. 좌하단 캡션 `출발 전 점검`(도식 캡션 자리).
- 오른쪽: 상단·조수석·메인 문장은 그대로. **기어·뒤 거리 값 행은 그리지 않는다**(기어는 왼쪽 칩에 있다). 정차 상태 줄은 `{attempt}회차 · {elapsedSeconds}초`(이동 횟수 생략). `다 됐어요` 규칙 동일.
- 상태 라벨 접기 규칙의 대상은 벨트·기어·시동 세 신호.
- 계측 추가: 점검 Maneuver 에 `조향각`·`뒤 거리`·`cm` 텍스트 없음, 칩 3개 텍스트 있음.

## 8. 라운드 1 리뷰 관찰 반영

1. **접힌 상태 라벨** — 세 신호가 모두 같은 상태일 때 속도 아래 한 줄로 접는 규칙을 이렇게 바꾼다: `SIMULATED` → `시뮬레이션 신호`, `LIVE` → `실신호`, **`MISSING` 이면 접지 않고 값마다 `미측정`**(속도는 살아 있는데 "미측정" 으로 읽히던 문제). `commonSignal()` 반환에서 MISSING 제외.
2. **Bold 0** — `LessonCanvas` 의 Bold 브랜드 라벨 포함 옛 프레임(`LessonCanvas`·`LessonFrame`·`LessonCard`·`LessonButton`)을 쓰는 화면이 없어지면 **삭제**한다. `DashboardScreen` 은 부트스트랩 참조용이라 남기되 `BrandMark` 로 바꾼다. 끝나면 `ui/` 에 `FontWeight.Bold` 0건.

## 9. 불변 (라운드 1 표 + 추가분)

라운드 1 발주서 5절 표 12줄 전부 그대로. 추가:

| 규칙 | 검사 |
|---|---|
| `Setup` 첫 화면에 `시작`·`시연`·(펼친 패널의) 시나리오 라벨. `과제·모드 바꾸기` 시트에 모드 라벨(`힌트` 등)과 `시작` | emu_flow(`힌트` 가 첫 화면에 없으면 `과제·모드 바꾸기` 를 먼저 누른다 — 이미 반영) · lesson_shots |
| 준비 중 과제는 클릭 액션 없음, `준비 중` 텍스트 있음. 지식 과제 선택 시 모드 칩은 `지식 테스트` 만 | lesson_shots(추가) |
| `Briefing` 터치 0, `line` 텍스트 존재 | lesson_shots(기존) |
| `Done` 에서 `문 열기` 가 눌린다(패널 존재) | emu_flow |
| `Quiz` `locked` 면 클릭·스크롤 노드 0 · 선택지 텍스트 0 | lesson_shots(추가) |
| `Quiz` 답한 뒤 `다음 문제`(마지막 `결과 보기`), `QuizDone` 에 `다시 시작` | lesson_shots(추가) |
| 점검 `Maneuver` 에 `조향각`·`뒤 거리` 없음, 벨트·기어·시동 칩 있음 | lesson_shots(추가) |
| `ui/` 에 `FontWeight.Bold` 0건, 옛 컴포넌트 0건 | grep |

`LessonScreenInstrumentation` 은 항목을 빼지 않고 추가한다. Setup 의 `"오늘은 뭘 해볼까요?"` 검사처럼 **문구가 바뀌는 검사는 새 문구로 고친다**(PR 본문에 적는다).

## 10. 참고

- 데이터 계약 전체는 라운드 1 발주서 + `feature/lesson/LessonPhase.kt`. 화면은 `LessonPhase` 를 직접 받지 않는다(`LessonRoute` 만 분기).
- `attemptHead(task, score)` 는 `com.moah.hackathon.ports` 의 최상위 함수 — 필요하면 부를 수 있지만 `remark` 에 이미 포함돼 있다.
- 시연 대본 `docs/05_demo_script.md` 의 Setup 단계 표기(힌트 → 시작)는 라운드 2 머지 뒤 Claude 가 실측해 고친다.
- 캡처는 `docs/screenshots/lesson/` 의 같은 이름으로 교체 + 새 화면(`lesson-quiz.png`·`lesson-quiz-locked.png`·`lesson-quiz-done.png`·`lesson-maneuver-checklist.png`·`lesson-setup-sheet.png`) 추가.
