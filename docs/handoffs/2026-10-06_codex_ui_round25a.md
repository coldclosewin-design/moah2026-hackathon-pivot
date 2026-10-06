# Codex 오더 — 라운드 25a: 홈 버튼·프로필 알약·마감 시간대·한 칸 알약·잠금 탈출·다듬기 (2026-10-06)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/13_round25_decisions.md,
         고른 시안 페이지(docs/design/round25-proposals/1-coach-button.html · 2-profile-eyebrow.html 시안 B · 3-venue-slots.html 시안 A ·
         6-lock-admin.html 시안 C·B — 보기 창이 HTML 을 못 열면 같은 이름 .png)를 읽어라.
시작 조건: origin/main 최신에서 새 워크트리, 브랜치 codex/ui-round25a, scope ui, PR 하나. 모델 변경 없이 되는 것만(25b 는 Claude 모델 뒤에 따로 온다).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지(필요하면 INTEGRATION C 절에). docs/NEXT.md 금지.
     시연 본편 흐름·고정값·도구가 찾는 라벨(못한 주차 · 잘한 주차 · 정차 · 출발 · 문 열기 · 문 닫기 · 시나리오 정지 · 과제·모드 바꾸기 · 시작 · 다 됐어요 · 한 번 더 · 오늘은 여기까지 · 다시 시작 · 이 설정으로 홈 · 켬 · 코치에게 글로 말해 보세요 · 보내기)은 불변.
     새 색 토큰 0, Bold 0, 그림자는 CoachTexture 값 재사용.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", RESERVE=1 bash tools/emu_flow.sh → PASS · clashes 0,
          adb install -r 뒤 bash tools/course_flow.sh → PASS. 바뀐 캡처 교체 + README 표(시안 페이지와 나란히). 에뮬은 네 것(emulator-5556)만.
```

선행 상태: 모델 변경 없음 — 바로 시작 가능

## 항목

| # | 어디 | 할 것 · 주의 |
|---|---|---|
| ① | `SetupScreen.kt` 링크 줄 · `CoachHome.kt` `CoachPill` · 코치 시트 눈썹 | 버튼 이름을 **`코치와 대화`** 하나로(입력 켬/끔 분기 삭제), 앞에 마이크(지금 아이콘, Periwinkle 선) 항상. 링크 줄에서 `과제·모드 바꾸기` 는 왼쪽, 알약은 **오른쪽 끝**(= `시작` 알약 오른쪽 끝에 맞춤). 예약 카드가 있을 때도 같은 줄. 시트 눈썹 `코치와 이야기` → `코치와 대화`. 계측(`LessonScreenInstrumentation` 의 `코치와 고르기`·`코치에게 말하기`)은 새 이름으로 고친다. `inhouse_check.sh` 는 Claude 가 두 이름을 다 찾게 미리 고친다 |
| ② | 홈 눈썹 | 시안 2 **B 프로필 알약** — 눈썹 문장(`연수생 · 장롱 10년차 · 목표 아이 등하원`)을 Lavender 알약 안에 사람 기호 + 문장 + `›`, 질감 그림자, 높이 ≥ 76 dp. 누르면 지금처럼 프로필 시트. 첫 실행·프로필 비움일 때 문장이 짧아도 같은 모양 |
| ③ | 시험장 시트 시간 트랙(`SelectionTrack` 의 `!available`) | 시안 3 **A 마감** — 마감 칸: 글자 먹 35% + 취소선 + 작은 `마감` 꼬리표, 그림자 없음, 탭 반응 없음(지금처럼), 칸 자리는 그대로. 시드에 시험장마다 마감 하나(서초 16:00–17:00 · 강남 14:00–15:00 · 분당 18:00–19:00) — 캡처는 마감이 보이는 시험장으로 |
| ④ | 선택 트랙 전역 | **모드가 하나뿐이어도 알약은 한 칸 폭** — 지식 과제의 `지식 테스트` 가 세 칸 폭 트랙을 다 차지하던 것. 칸 폭은 세 칸 트랙의 한 칸과 같게, 트랙 바탕은 알약을 감싸는 폭(왼쪽 정렬). 칸 수가 모자란 다른 트랙도 같은 규칙 |
| ⑤ | 잠금 화면(Done·Report·Quiz·QuizDone 의 locked) | 시안 6 **B**: 시연 빌드(`vm.admin != null`)에서만 잠금 화면의 `DRIVE COACH` 워드마크를 **약 2 초 길게** 누르면 `admin.demo.stopScenario()` + `admin.demo.stopCar()`(속도 0 → 잠금이 풀린다). 보이는 버튼·표시 없음(절대 규칙 10 — 운전자 화면 터치 타깃 0 은 그대로, 이건 시뮬레이터 조작). 시안 6 **C 의 안내 한 줄**: 시연 빌드 잠금 화면에만 옅은 글자(Paper 40%) "시연 · 시나리오가 끝나면 차가 멈춰요 — 막히면 워드마크를 길게" — 터치 타깃 아님. 순수 Real(`admin == null`)에는 둘 다 없음(계측으로) |
| ⑥ | 다듬기 셋(라운드 24 리뷰 관찰) | (가) 준비실: "copilot_config.json 없음 — …" 상태 줄이 `이 설정으로 홈` 위 페이드에 가리지 않게 간격 줄이기 (나) 띠 `더 보기`: "재생 대기" 같은 재생 상태는 신호 출처 줄로 옮기고, AI 로그인 주소·코드 묶음 위에 작은 표지 "AI 로그인" (다) 코치 대화 시트: 대화가 길어질 때 맨 위 말풍선이 그냥 잘리지 않게 대화 영역 위쪽에 짧은 페이드(Paper → 투명) |

## 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 보이는 터치 타깃 0 · 운전자 화면 숫자 0 · 잠금 화면 평면 | 계측 |
| Real 빌드(`admin == null`)에 워드마크 길게 누르기·안내 줄·준비실·띠 0 | 계측(Real 흉내 fixture) |
| 도구 라벨(위 목록) 불변 · `emu_flow`·`course_flow` 그대로 PASS | 도구 |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 색 토큰 0 | grep |

캡처: 교체 `lesson-setup.png`·`lesson-setup-reserved.png`·`lesson-setup-coach*.png`(버튼·눈썹 이름)·`lesson-venue-slots.png`(마감 보이게)·`lesson-setup-sheet-knowledge.png`(한 칸 알약)·`lesson-done-locked.png`(시연 안내 줄)·`lesson-admin-home.png`·`lesson-admin-band-ai-code.png`, 새 `lesson-profile-pill.png`(눈썹 알약 확대) — 홈의 예약 이유는 #203 문구("예약한 코스부터, …")로 다시 찍힌다.

머지 뒤 Claude 가 리뷰한다. 말 카드 줄·S자 곁가지·도로 표시 그림은 Claude 모델 PR(M1~M5) 뒤 25b 로 온다.
