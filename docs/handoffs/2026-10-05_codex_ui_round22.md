# Codex 오더 — 라운드 22: 직접 조작 피드백 중 바로 고칠 것 4건 + 결정된 시안 2건 (2026-10-05 밤)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/11_round22_feedback.md(§1 의 3·5·6·1 항목), docs/screenshots/lesson/README.md 를 읽어라.
시작 조건: 라운드 21(#173) 머지됨 — origin/main 최신에서 새 워크트리 codex/ui-round22. PR 하나(scope ui). ①~④ 는 버그, ⑤·⑥ 은 사용자가 고른 시안(docs/design/round22-proposals/3-mode-spacing.html 의 C, 6-details-badge.html 의 B — 페이지를 열어 그대로). 나머지 시안 항목(진단서 B·분류 트랙 색·홈 코치 대화+예약 카드·관리자 모드·프로필)은 Claude 모델 선행 뒤 라운드 23 — 이 PR 에 넣지 않는다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라.
     시연 본편의 흐름·고정값·라벨 13개 불변. 새 색 토큰 0(다섯 토큰 + 투명도만), Bold 0, 그림자는 CoachTexture 값 재사용.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → PASS · clashes 0(60/55·100/100·힌트 3종·배지 8),
          adb install -r 뒤 bash tools/course_flow.sh → PASS. 바뀐 캡처 교체 + README 표. ② 는 캡처로 증명이 안 되니 Done 진입 직후 0.5 s 간격 3프레임(스크린샷 또는 screenrecord 프레임 스트립)으로 알약 y 좌표가 같음을 보인다.
```

## 0. 항목

| # | 어디 | 증상(사용자 원문) | 원인 | 할 것 |
|---|---|---|---|---|
| ① | `VenueSheet.kt` `VenueCard` | "선택된 카드 색상이 변하지 않고 테두리만 하이라이트" | `chosen` 이면 `border(6.dp, Signal)` 만 더하고 바탕은 `CoachTexture.Card` 그대로(97행) | 과제 카드(`TaskSheet`)와 같은 선택 규칙: 바탕 **Periwinkle**(`CoachTexture.SelectedCard`) + 글자 Paper(이름 48·코스 34·상태 36 — 상태 글자는 `예약됨` 이면 Signal 유지, 아니면 Paper 80%), 지도 띠는 Ink 그대로에 경로 Signal. 빨간 테두리는 **뺀다**(과제 카드도 테두리가 없다). 선택 전환 애니메이션은 없음. 계측: 선택 카드의 `selected` 시맨틱과 바탕 색 픽셀 검사 하나 추가 |
| ② | `DoneScreen.kt` 66~78행(주차·점검·코스 Done 공통) | "'다 됐어요' 알약을 누르면 잠깐 아래에 텍스트가 있다가 그 텍스트가 없어지면서 알약이 부자연스럽게 내려옴" | 오른쪽 열 = `[제목][멘트 Box weight(1f)][한 번 더 Row][SpeechFooter]`. Done 진입 직후 `subtitle` 은 Maneuver 의 마지막 자막("다 되셨나요? …")이라 푸터가 그려지고, `tts.speak(remark)` 로 자막이 멘트와 같아지면 `takeUnless` 로 푸터가 사라져 weight(1f) 가 늘어나며 알약이 내려감 | 알약 위치를 **처음부터 고정**: (가) 푸터 자리를 고정 높이(지금 푸터 1줄 높이)로 잡고 글자만 있다 없다 하게(없을 때 투명·같은 높이) — Maneuver·Drive·Quiz 의 `SpeechFooter` 도 같은 부품이면 같이 적용, 또는 (나) Done 에서는 묵은 자막을 아예 그리지 않는다(멘트 = 자막이므로 정보 손실 없음). **(가)를 권장**(다른 화면에서도 같은 떨림을 막는다). 알약이 정지한 뒤 두 번 펄스하는 기존 동작 유지 |
| ③ | `AttemptComparison.kt` 84~85행 | "카드 내 조향 왕복 0 근접 0 등이 나열로 되어 있음 … 잘린 느낌" | `parkingDetailLine` 한 줄(32 dp)이 두 회차 카드 폭보다 길어 마지막 값만 다음 줄로 떨어짐(`lesson-details-session.png` 의 `급정지\n1`) — 아래 신호 출처 블록 때문이 아니라 **폭** 문제 | **⑥ 과 함께 시안 6-B 대로**: 카드 지표를 **라벨/값 목록**(한 줄에 한 지표: 숙련·안전 / 이동 / 시간 / 조향 왕복 / 기어 전환 / 근접 / 급정지 / 방향 편차 — 라벨 32 dp Muted 왼쪽, 값 36 dp Ink 오른쪽 정렬, 변화량 칩은 값 왼쪽), 미측정은 값 자리에 `미측정`. 줄바꿈이 구조적으로 없어진다. 회차 카드 하나짜리(`lesson-report-parking-perfect.png`)·셋 이상·코스 카드(감점 표)는 영향 없음을 캡처로 |
| ④ | `ReportScreen.kt` 하단 Row(진단서·자세히 보기), `QuizDoneScreen`, `TaskSheet`, `VenueSheet` | "돌아가기 / 다시 시작 알약이 너무 붙어 있어서 조치 필요 (다른 곳에도 통일성을 감안하여 검토 필요)" | 진단서 하단 Row 가 `SpaceBetween` 이지만 주 알약 폭이 커서 틈이 약 24 dp | 공통 규칙을 한 부품(`BottomActions(secondary, primary)`)으로: **보조 알약 왼쪽 정렬 · 주 알약 오른쪽 정렬 · 사이 최소 64 dp** — 주 알약은 남는 폭을 쓰되 최소 폭(Setup `시작` 과 같은 1075.2 dp 규칙, 라운드 20 ⑧)과 충돌하면 **최소 폭을 양보하고 간격을 지킨다**. 적용: 진단서(`돌아가기`·`다시 시작`), 자세히 보기(`돌아가기` 만 — 변화 없음), 과제 시트(`돌아가기`·`시작`), 시험장 시트(`돌아가기`·`예약`), QuizDone(`다시 시작` 만). `오늘은 여기까지`·`그만하기` 같은 글자 링크는 그대로 |

| ⑤ | `TaskSheet.kt` 모드 행 · `VenueSheet.kt` 시간·코스 행 | 사용자 선택 **3-C 알약 트랙**: "가이드/힌트/평가 부분과 돌아가기 시작 부분이 너무 인접" | 모드 칩 바닥 → `시작` 윗변 약 17 dp | 모드를 **눈썹 없이** 분류 트랙과 같은 부품(라운드 21 ②)으로 — 높이 88 dp(분류 트랙보다 낮게 위계 구분), 폭 = 카드 행의 절반(약 832 dp), 카드 바닥 → 24 dp → 트랙, 같은 줄 오른쪽에 `제휴 시험장` 글자 링크, 트랙 → `시작` 윗변 빈 띠 약 186 dp. 시험장 시트: 카드 바닥 → 24 → 시간 트랙 88(전체 폭, 자리 없는 시간대는 흐린 글자·눌리지 않음) → 16 → 코스 트랙 88(전체 폭) → 빈 띠 약 133 → `예약`. **`돌아가기`·`시작`·`예약` 위치·크기 불변**(사용자 지시). 분류 트랙의 색·글자는 이번엔 그대로(시안 2b 선택 뒤 라운드 23) — 공통 부품으로 만들되 색 인자를 받게 해 두면 라운드 23 이 쉬워진다. 계측: 모드 선택 시맨틱(`selected`)·시트 숫자 0·자리 없는 시간대 클릭 불가 유지 |
| ⑥ | `ReportScreen.kt` 신호 출처 블록(자세히 보기·진단서) | 사용자 선택 **6-B 한 줄**: "아래 신호 출처 등으로 돌아가기 및 카드가 위로 올라가면서 잘린 느낌" | 자세히 보기·진단서 하단 고정 블록(눈썹 + 개수 줄 + 설명 + 구분선)이 패널 높이의 약 1/8 | 자세히 보기·진단서에서는 **32 dp 한 줄**(`신호 출처 · 실신호 0 · 시뮬레이션 8 · 미측정 0`, Periwinkle)을 `돌아가기` 와 같은 줄 오른쪽에, 설명문("주차 과정만 측정했어요." / 코스는 한 줄로 줄인 문장)은 그 아래 28 dp Muted. 비운 높이는 카드에. **리포트 요약 페이지의 블록은 지금 그대로**(정직성 원칙·사내 `실신호 7` 장면). 계측이 신호 출처 텍스트를 찾는 태그는 유지 |

## 1. 검증·캡처

- 교체: `lesson-venue-slots.png`·`lesson-venues-booked.png`(①·⑤), `lesson-details-session.png`·`lesson-details.png`·`lesson-details-comparison.png`(③·⑥), `lesson-certificate.png`·`lesson-certificate-exam.png`(④·⑥), `lesson-setup-sheet*.png`(⑤ — 모드 트랙). README 표에 ⑤·⑥ 은 시안 페이지(`../../design/round22-proposals/3-mode-spacing.html`·`6-details-badge.html`)와 나란히 비교할 수 있게. 추가: `lesson-done-settle-strip.png`(② 3프레임 스트립, 알약 y 동일).
- README 표: 항목 ①~④ × 캡처 × 확인한 것. ② 는 원인·조치를 한 줄로.
- `emu_flow.sh`(후면)·`course_flow.sh`(장내) PASS 는 필수 — ② 가 Done 의 `한 번 더` 탭 좌표를 바꿀 수 있으니 스크립트가 라벨로 찾는 것을 확인한다(스크립트 수정 금지).

## 2. 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 터치 0 · 운전자 화면 숫자 0(속도·자세히 보기 제외) · 잠금 화면 평면 | 계측 |
| 시트 숫자 0 · 라벨 13개 문자열 불변 | 계측 · emu_flow |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 색 토큰 0 | grep |
| `feature/`·`scoring/`·`data/` 구조 불변 | `git diff --stat` |

머지 뒤 Claude 가 리뷰하고, 라운드 23(시안 선택분)과 합쳐 태그 `inhouse-20261006-2` 를 만든다.
