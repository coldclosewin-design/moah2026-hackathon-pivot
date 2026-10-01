# Codex 오더 — UI 라운드 11: 감사 08 의 B 항목 + 결과 화면 잠금 (2026-10-01 저녁)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md(커밋 규칙 8), 이 발주서, docs/design/08_consistency_audit.md(근거 — 항목 번호가 아래와 같다), docs/INTEGRATION.md C 절 10/1 [Claude→Codex] "결과 화면 잠금" 을 읽어라.
브랜치: origin/main(088320d 이후)에서 새 워크트리. **PR 둘, 순서대로**: ① codex/seed-tone(scope data — 시드 문구만) → 머지 뒤 ② codex/ui-round11(scope ui — 화면·계측·캡처). 한 PR = 한 scope.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 **구조**(타입·필드·선택 로직·채점), build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라. 발주서에 없는 항목(감사 08 의 A·D)은 하지 않는다.
완료 기준(PR 마다): .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell — 증분 빌드 stdlib NoClassDefFoundError 면 clean 뒤 다시), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(힌트 3종 기대 로그 불변 — ① 에서 힌트 문장은 바꾸지 않는다), 캡처 교체·추가 목록은 각 절에, gh pr create(제목 = squash 제목 50자 안팎, 본문 = 불릿 2~5 + 마지막 `검증:` 줄).
```

## 0. 왜

10/1 감사(`docs/design/08_consistency_audit.md`, Codex)가 B(화면·시드 내용·계측) 항목 9 + A 항목을 냈다. A 중 셋은 Claude 가 끝냈다(#78 회차 숫자 발화 제거 · #79 급가속 힌트 문장 · #80 결과 화면 `locked` 플래그). 이 라운드는 **B 9건 + #80 의 화면 쪽**이다. D1~D6 은 §4 — 사용자 결정 전까지는 **그대로 둔다**.

## ① `data: 시드 문구 톤 통일` — 시드 **내용만**(`data/SeedCatalog.kt`, `res/values/strings.xml`), 구조·키·단계 수·판정식 불변

| 원 항목 | 지금 | 바꿀 것 | 확인 |
|---|---|---|---|
| A3-01 주차 가이드 6단계 | "매 주세요 / 돌리세요 / 후진하세요 / 기어 P." 어미 혼용, `P`·`R`·`45도` 를 TTS 가 읽기 부담 | 해요체·짧은 요청형으로 통일 — "핸들을 오른쪽 끝까지 돌려 주세요." / "천천히 후진해요." / "다 들어왔으면 멈추고 주차 기어에 놓아 주세요." 각도·판정 조건은 그대로(숫자가 **화면 문장**에 남으면 안 된다 — 각도는 "끝까지" 로) | `lesson-maneuver-guides.png` 교체, 가이드 7단계 모두 TTS 로 읽어 어색한 기호 없음 |
| A3-02 점검 가이드 7단계 | "P 맞아요 / 이제 오른쪽 / 비상등 확인" 어미 생략 | "주차 기어를 확인했어요." / "왼쪽을 확인했어요. 이제 오른쪽도 켜 봐요." / "비상등을 확인했어요. 이제 끄고 버튼을 눌러 주세요." | `lesson-maneuver-checklist-pending.png` 교체 |
| A3-03 Done 서두(주차·점검 RemarkTemplate) | 서두가 여러 문장이라 조언을 붙이면 세 문장, "열었습니다" 하십시오체 혼입, "들어가긴 했어요 / 덜 헤맸어요 / 첫 회차는 원래 이래요" 는 평가·책망 | **모든 서두를 한 문장 해요체**로. 조언(`AdviceRules`, Claude 소유)과 합쳐 정확히 두 문장이 되는지 `RemarkPoolTest`/`LessonStateMachineTest` 로 확인 | `lesson-done.png`·`lesson-done-checklist.png` 교체 |
| A3-04 Done 서두의 사실 단정·횟수 | "마트 주차장은 됩니다 / 한 번에 들어갔어요 / 안전했어요 / 부딪히지 않았어요 / 제일 안전한 사람 / 다 켜졌어요" 는 skill 밴드만으로 보장 못 하는 사실, "세 번만 반복"·"순서가 한 번" 은 횟수 | 근거를 넘지 않는 서두로 — "오늘의 연습을 끝까지 마쳤어요.", "출발 준비를 차근차근 익히고 있어요.". 횟수는 "이 순서를 다시 연습해 봐요." 태그·밴드·선택 구조는 유지 | 단위 테스트: 모든 RemarkTemplate 에 숫자 0 · 문장 1(`.`·`!`·`?` 로 끝나는 조각 하나) |
| A3-07 제안·과제 설명·예약 | "해볼까요/풀어 볼까요" 띄어쓰기 혼용, 과제 설명이 `→`·`45°`·`3지선다` 기호 문장 | "해 볼까요 / 익혀 볼까요" 통일. 설명은 문장으로 — "핸들을 돌리고 천천히 후진하며 방향을 맞춰요.", "정차 중에 문제를 풀고 이유를 함께 살펴봐요." 예약 시각·거리·주차 3종·예시 고지는 보존(정차 선택 화면 허용) | `lesson-setup.png`·`lesson-setup-sheet.png` 교체 |
| A3-08 퀴즈 해설 | "차선 변경엔 방향지시등, 터널엔 전조등 / 하향등" 종결 생략, "눈을 멀게" 공포 표현 | 해요체 완결문, "상향등은 마주 오는 운전자의 시야를 방해할 수 있어요." 교육 수치(`20%`, `60 m`)·정답·문항 뜻은 **그대로**(D1 결정 전) | `lesson-quiz-answered.png` 교체 |

주의: `tools/emu_flow.sh` 가 기다리는 **힌트 문장**(`안전벨트가 아직이에요`·`뒤가 가까워요`·`제동이 급했어요`)과 **라벨 13개**는 `HintRules`/`strings.xml` 에 있고 이 PR 범위가 아니다. `AdviceRules` 조언 문장도 Claude 소유 — 바꾸고 싶으면 C 절.

## ② `ui: 결과 화면 잠금·긴 문장·점검 설명·출처 목록·카테고리 대비·캡처 공백`

| # | 원 항목 | 지금 | 바꿀 것 | 캡처 |
|---|---|---|---|---|
| 2.1 | **결과 화면 잠금**(#80, C 절) | `LessonPhase.Done`·`Report`·`QuizDone` 의 `locked`(속도 > 5) 를 화면이 안 씀 | `DoneScreen`·`ReportScreen`·`QuizDoneScreen`: `locked` 면 **Maneuver 잠금과 같은 모양**(남색 면 + "운전에 집중해 주세요", 부제 "속도를 낮추면 결과가 다시 보여요.") 으로 가리고 터치 타깃·점수·수치 0, 정차하면 복귀. 시연 패널 알약도 숨김(Maneuver 와 같은 규칙) | `lesson-done-locked.png`·`lesson-report-locked.png` 추가 |
| 2.2 | A1-06 긴 메인 문장 | Headline 72→64→56 sp 자동 축소는 3줄까지, Done·Report 의 긴 멘트(명시 줄바꿈 4줄·Cloud 상한)가 다 보인다는 보장 없음 | 긴 메인 문장용 배치(4줄 보장, 주 버튼과 겹치지 않음). 짧은 제목 2/3줄 규칙은 유지 | `lesson-done-long.png`·`lesson-report-long.png` 추가(최장 허용 입력) |
| 2.3 | A2-06 점검 결과 설명 | ✗·미측정 행에도 "시동 전 닫힘 / 벨트 먼저" 같은 **성공형** 설명 | 결과와 같은 시제로: 실패 "시동 때 문 열림", "브레이크 확인 안 됨"; 미측정 "순서를 확인할 수 없어요". 매퍼 `checklistResults`(ui/ChecklistPresentation) 만, 기록·채점 불변 | `lesson-report-checklist-bad.png`·`-missing.png` 교체, 매퍼 단위 테스트 |
| 2.4 | A4-02 Report 출처 목록 | `ReportProvenance` 고정 하단이 미측정 키·미확인 단계가 많으면 본문과 경쟁 | 최대 누락 fixture 로 먼저 재현, 필요하면 긴 설명만 본문 스크롤로. 배지·`돌아가기`는 항상 보임 | `lesson-report-all-missing.png` 추가 |
| 2.5 | A5-01 닫힌 카테고리 대비 | `TaskSheet` 닫힌 `주행` 메뉴 글자 Periwinkle 60 % = **2.52:1**(누를 수 있는데 비활성처럼) | 기존 토큰 중 4.5:1 이상(`Muted` 5.20)으로, 준비 중 **과제 칸**의 비활성 회색은 그대로 | `lesson-setup-sheet.png` 교체 |
| 2.6 | A4-01 캡처 공백 | 코드가 지원하는데 캡처가 없는 상태 | 계측 fixture 로 추가(새 동작 없음): 점검·지식 **Briefing**, 주차 **힌트·평가** Maneuver, 점검 **잠금**·**혼합 출처** 칩, 라운드 9 이후 주차 **Done**, 주차 **만점 상세**·**복수 회차 요약**, Report **실신호 7·시뮬 1·미측정 0**(사내 실측 숫자를 외부 fixture 로 — 파일명에 `fixture`) 과 **실신호+미측정**, Quiz **정답 선택**·**긴 문항**, AI 패널 **NoConfig·Ready·Error(80자)** | 파일명은 `lesson-<화면>-<상태>.png`, README 에 "외부 fixture" 명시 |

## 3. 불변

| 규칙 | 검사 |
|---|---|
| 라벨 13개·모드 라벨 문자열 그대로, `emu_flow` 힌트 기대 로그 그대로 | `emu_flow` PASS |
| 주행 잠금(Maneuver·Quiz·**Done·Report·QuizDone**)에서 클릭 가능 노드 0·점수·숫자 0 | lesson_shots(2.1 추가) |
| `ui/` `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 만, 새 색 토큰 0, 새 문자열 리소스 0 | grep |
| 운전자 문장에 숫자 없음(정차 선택 화면의 시간·거리, `자세히 보기`, 퀴즈 교육 수치는 D1 전까지 현행 유지) | 단위 테스트 |
| 시드 **구조**(필드·태그·밴드·선택 로직)·채점·상태기계 불변 | `git diff --stat` 에 `feature/`·`scoring/` 없음 |

## 4. 결정 대기 D1~D6(감사 08 "사용자에게 캡처로 물을 것") — 사용자가 정하면 §2 에 덧붙인다

| 결정 | Claude 추천 | 이유 |
|---|---|---|
| D1 숫자 경계 | (가) 운전자 **문장**(TTS·멘트·힌트·총평)만 숫자 금지, 계기 속도·가이드 n/N·문항 번호·회차/연차 메타·정차 교육 수치는 **명시 예외** | 지금 코드가 이미 그 경계고, 라운드 4·9 가 승인한 것. AGENTS 규칙 문장을 Claude 가 고친다 |
| D2 결과의 빨강 | (나) 정차 결과 화면(오답·✗·급조작 점)의 Signal 은 **예외로 명문화**, 주행 중엔 없음 | 라운드 7·9 에서 두 번 승인됐고 정차 중이라 시선 문제 없음 |
| D3 글자 하한 | (나) 시연 패널 상세 28 sp 만 예외(운전자 화면이 아님), 본문·눈썹·출처는 32 sp | 패널은 시연 담당자용 |
| D4 편집 규칙 | (가) 역할별 패널 폭·눈썹·자막 배치 **현행 유지 + 문서화**(02_design_brief 에 Claude 가 적음) | 라운드마다 승인된 결과물, 통일의 이득이 작음 |
| D5 정차 조작 | (가) Report·QuizDone `다시 시작` 도 **140 dp**, 시연 알약은 외형 유지 + hit 영역 88 dp | 운전석에서 누르는 버튼 규칙 하나로 |
| D6 대비 | (가) 팔레트 유지(큰 글자 예외), 실차에서 확인 — 단 A5-01(2.5:1 메뉴)은 ② 에서 고침 | 새 토큰 금지 원칙 |

D5 가 (가)로 정해지면 ② 에 "2.7 `PrimaryPill` 140 dp 통일 + 알약 hit 영역" 을 추가한다.
