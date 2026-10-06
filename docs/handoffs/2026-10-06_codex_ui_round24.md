# Codex 오더 — 라운드 24: 홈 코치 텍스트 대화 화면 (2026-10-06)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/12_home_coach_dialog.md("모델 API" 절),
         라운드 23 발주서 ③(코치 대화 시트 간격표)를 읽어라.
시작 조건: Claude 모델 PR(홈 코치 대화 — CoachPort.converse · sendCoachText · admin.textInput) 머지됨. origin/main 최신에서 새 워크트리, 브랜치 codex/ui-round24, scope ui, PR 하나.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지(필요하면 INTEGRATION C 절에). docs/NEXT.md 금지.
     시연 본편의 흐름·고정값·라벨 13개 불변. 새 색 토큰 0(다섯 토큰 + 투명도), Bold 0, 그림자는 CoachTexture 값 재사용.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", RESERVE=1 bash tools/emu_flow.sh → PASS · clashes 0,
          adb install -r 뒤 bash tools/course_flow.sh → PASS. 바뀐 캡처 교체 + README 표.
```

선행 상태: ✅ **모델 PR #198 `577e8bb`** — 시작 가능

## 0. 왜

사내 검증 #4(10/6): 지금 `코치에게 말하기` 는 말을 듣지 않는다 — 시트를 열고 규칙 문장을 읽은 뒤 칩 셋만 받는다(마이크·STT 없음). 시연에서 "말하면 안내해 준다" 로 오해될 수 있어,
(가) 입력이 없는 빌드에서는 버튼 문구를 `코치와 고르기` 로 바꾸고, (나) 관리자가 **"시뮬레이션 음성 입력"** 을 켜면 시트에 글 입력 칸을 열어 코치(AI 또는 Fake 키워드 규칙)와 몇 턴 이야기한 뒤 기존 기능(과제 시트·홈 제안·예약 카드·프로필 시트)으로 안내받게 한다. STT 는 넣지 않는다.

## 1. 모델 (Claude — 화면이 쓰는 값·진입점)

| 값 · 진입점 | 뜻 |
|---|---|
| `setup.coachTextInput: Boolean` | 관리자 토글 값(기본 false). 켜짐 = 시트에 입력 칸·`보내기`·"음성 입력 · 시뮬레이션" 배지, 버튼 `코치에게 말하기` / 꺼짐 = 지금 시트 그대로, 버튼 `코치와 고르기` |
| `setup.coach.turns: List<CoachTurn>` | 첫 말(`coach.line`) 뒤의 대화. `fromDriver` = 운전자 글(오른쪽) / 코치 되물음(왼쪽) |
| `setup.coach.waiting: Boolean` | 코치가 답을 고르는 중(Copilot 1.4~3.3 s) — 보내기 비활성 + 코치 쪽 "…" 말풍선 |
| `setup.coach.choices` | 칩은 대화 중에도 그대로 둔다(AI 가 실패해도 고를 수 있게) |
| `setup.profileRequest: Boolean` | 대화가 프로필 시트를 요청 — 홈 눈썹 프로필 시트를 연 뒤 `consumeProfileRequest()` |
| `sheetRequest` · `highlightBooking` · `bookingChoice` · 홈 제안 | 라운드 23 ③ 그대로 — 대화의 결과가 이것들로 떨어진다(시트는 닫힘, 답 문장은 TTS 자막) |
| `vm.sendCoachText(text)` | `보내기`. 입력 꺼짐·답 대기 중·시트 닫힘이면 무시(화면도 막는다) |
| `vm.consumeProfileRequest()` | 위 |
| `admin.textInput: StateFlow<Boolean>` · `admin.setTextInput(on)` | 준비실 토글 |

## 2. 항목별 구현 메모 (Codex)

| # | 어디 | 할 것 · 주의 |
|---|---|---|
| ① | `SetupScreen.kt` 홈 A1 줄 | 보조 알약 문구: `setup.coachTextInput` 이면 `코치에게 말하기`(마이크 아이콘 그대로), 아니면 **`코치와 고르기`**(마이크 대신 말풍선 아이콘 — 기존 아이콘 자원 안에서, 없으면 아이콘 없이). 폭이 바뀌어도 A1 간격표 유지 |
| ② | `CoachSheet` | 입력 켜짐일 때만: 말풍선(`coach.line`) 아래에 `turns` 를 차례로(운전자 = 오른쪽 정렬 Periwinkle 채움 + Paper 글자, 코치 = 지금 말풍선 모양), 많아지면 시트 안에서만 스크롤하고 마지막 줄이 보이게. 그 아래 칩 줄(지금 그대로) → 입력 줄: 한 줄 텍스트 칸(48 dp 글자, 자리표시 "코치에게 글로 말해 보세요") + `보내기` 주 알약. `waiting` 이면 코치 쪽 "…" 말풍선 + `보내기` 비활성 + 칸 잠금. 보낸 뒤 칸 비움. 엔터(IME 보내기)도 `보내기`. 흐린 마이크 알약 "말로 답하기 — 준비 중" 자리에 **"음성 입력 · 시뮬레이션"** 배지(시뮬레이션 배지와 같은 모양 — 정직 표시). 입력 꺼짐이면 지금 시트와 픽셀 단위로 같게 |
| ③ | 프로필 요청 | `setup.profileRequest` 면 홈 눈썹의 프로필 시트를 열고 `consumeProfileRequest()` (`sheetRequest` 처리와 같은 방식) |
| ④ | 준비실(`AdminHome`) | "세션 중 패널(띠/숨김)" 아래에 토글 줄 **"시뮬레이션 음성 입력"**(켬/끔 — 띠/숨김과 같은 두 칸 모양) → `admin.setTextInput`. 왼쪽 "지금" 열에 켜졌을 때 한 줄 "음성 입력 · 시뮬레이션(글)" |
| ⑤ | 키보드 | AAOS 에뮬 IME 가 시트를 가리면 입력 줄이 IME 위로 올라오게(`imePadding`). 한국어 IME 가 없으면 영어로라도 입력이 되는지 확인 — 결과를 PR 본문에 한 줄 |
| ⑥ | 계측 | 입력 꺼짐: 버튼 `코치와 고르기` · 입력 칸 없음 / 켜짐: `코치에게 말하기` · 칸·`보내기` · 배지 있음, "평행 주차" 보내기 → 시트 닫힘 + 홈 제안 "평행 주차"(Fake 규칙 — `vm.admin!!.setTextInput(true)` 뒤 `performTextInput`), "안녕" → 시트 열린 채 되물음 말풍선, `waiting` 중 `보내기` 비활성. 시트·말풍선 숫자 0 |

## 3. 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 터치 0 · 운전자 화면 숫자 0 · 홈은 정차 화면(잠금 규칙은 지금 Setup 과 같게) | 계측 |
| 입력 꺼짐 시트 = 라운드 23 시트(칩 흐름·라벨 불변) — emu_flow·course_flow 는 칩·라벨로 간다 | emu_flow · course_flow |
| Real 빌드(`admin == null`)에 입력 토글·칸 0, 버튼 `코치와 고르기` | 계측(Real 흉내 fixture) |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 색 토큰 0 | grep |

캡처: 새 `lesson-setup-coach-text.png`(대화 두세 줄 + 입력 줄 + 배지) · `lesson-setup-coach-waiting.png` · `lesson-admin-home.png` 교체(토글 줄) · `lesson-setup.png` 교체(`코치와 고르기`).

머지 뒤 Claude 가 리뷰하고 tools(사내 점검에 텍스트 한 줄 단계)·태그를 만든다. 사내에서 Copilot 여러 턴·지연을 확인한 뒤 영상 컷을 더한다.
