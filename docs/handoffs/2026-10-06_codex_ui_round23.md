# Codex 오더 — 라운드 23: 라운드 22 시안 선택분 (2026-10-06)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/11_round22_feedback.md("사용자 결정" 두 표),
         docs/design/round22-proposals/ 의 고른 시안 페이지(아래 §0 표), docs/screenshots/lesson/README.md 를 읽어라.
시작 조건: 라운드 22(codex/ui-round22) 머지됨 + Claude 모델 PR M1~M4 머지됨(§1 — 발주서 맨 위 "선행 상태" 줄을 Claude 가 갱신한다).
          origin/main 최신에서 새 워크트리. 양이 많아 PR 둘로 나눈다 — 23a(codex/ui-round23a: ①②③) → 23b(codex/ui-round23b: ④⑤). 각각 scope ui, 각각 완료 기준을 지킨다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지(필요하면 INTEGRATION C 절에). docs/NEXT.md 금지.
     시연 본편의 흐름·고정값·라벨 13개 불변. 새 색 토큰 0(다섯 토큰 + 투명도), Bold 0, 그림자는 CoachTexture 값 재사용.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → PASS · clashes 0(60/55·100/100·힌트 3종·배지 8),
          adb install -r 뒤 bash tools/course_flow.sh → PASS. 바뀐 캡처 교체 + README 표(시안 페이지와 나란히).
```

선행 상태: ✅ M1(#178) · ✅ M2(#179) · 🟡 M3(PR 대기) · ⬜ M4 · ⬜ tools(프리셋 extra — M4 와 함께, 별도 PR)

## 0. 사용자 선택 (10/5 밤)

| # | 주제 | 선택 | 시안 페이지 | PR |
|---|---|---|---|---|
| ① | 진단서 | **B 단계** — 공유 범위 세로 카드 셋(위 = 원시 신호 → 아래 = 총점만), 카드 한 장 = 범위 + 혜택 + 조건, 선택 카드 Periwinkle 채움 | `1-certificate.html` 시안 B · 공유 예시 시트(같은 페이지 아래 절) | 23a |
| ② | 분류 알약 트랙 | **B 먹** — 트랙 바탕 Lavender 50% · 96 dp, 선택 알약 Ink 바탕 + Paper 글자(질감 B `SelectedChip` 그대로), 비선택 글자 Ink 60%, 라벨 48 dp | `2b-category-track.html` 시안 B | 23a |
| ③ | 홈 — 코치 대화 + 예약 카드 | **A1 링크 줄 내리기** — `시작` 아래 57 dp 에 `과제·모드 바꾸기` + 마이크 달린 보조 알약 `코치에게 말하기` 한 줄, 예약 카드는 이유 줄과 `시작` 사이(위·아래 33 dp), 예약 배지는 카드 눈썹 "예약 · 서초 14:00" 으로 흡수. 코치 대화 시트(말풍선 + 답 칩 셋 + 흐린 마이크 "말로 답하기 — 준비 중") | `4b-home-ai-ad.html` 시안 A1 + "말풍선 하나 · 칩 셋 · 마이크 자리" 절 + "칩 셋은 어디로 가나" 절 | 23a |
| ④ | 관리자 모드 | **D 준비실 + 세션 중 C 띠** — Setup 에서 워드마크 `DRIVE COACH` 를 약 2초 길게 눌러 준비실(별도 화면), 세션 중엔 화면 맨 아래 Ink 띠 | `5-admin-mode.html` 시안 D + C + "진입 방법" 절 | 23b |
| ⑤ | 프로필 | **P2 한 장** — 온보딩과 프로필 시트가 같은 부품(다섯 줄, 지금 묻는 줄만 크게 펼침). 첫 실행 = 전체 화면, 두 줄(무서운 상황 · 마지막 운전) 뒤 "나머지는 연습하면서" 로 멈춤. 홈 눈썹 = 시트. 리포트 요약 `다시 시작` 위 D 카드 "하나만 물어볼게요" | `7b-profile-abd.html` 조합 P2 + 질문 표 | 23b |

프로필 질문 10개는 사용자가 따로 답하지 않아 **7b 표의 추천 기본값**으로 간다(§2). 다르면 사용자가 말할 때 고친다.

## 1. Claude 선행 모델 (M1~M4 — 화면이 쓰는 값·진입점)

| # | PR(scope) | 무엇 | 화면이 쓰는 것 |
|---|---|---|---|
| M1 | data | `ShareLevel` 에 `benefit`·`condition` 추가. 원시 신호 → "적성검사 연계·연구 참여 대상" / "원시 신호까지 공유하면", 항목별 → "보험사 안전운전 특약 할인 심사 대상" / "항목별 기록을 꾸준히 공유하면", 총점만 → "제휴 시험장 대여료 할인" / "총점만 공유해도". `description` 은 시안 문구로("숙련·안전 두 점수만" · "구간 수·조향·기어·근접 등 수치까지" · "속도·조향각 시계열까지"). `LessonReport.benefits`·`SeedCatalog.benefits` 는 지금 화면이 깨지지 않게 `ShareLevel.benefit` 을 범위 순서로 모은 목록으로 남겨 두고(① 를 하면 화면은 `shareLevels` 만 쓴다 — Claude 가 뒤에 지운다), 공유 예시 시트용 `ShareLevel.includes`(포함 항목, 넓은 범위가 좁은 범위를 다 포함)·`ShareLevel.EXCLUDED`("위치" · "대화" · "음성") | `report.shareLevels` 각 원소의 `label`·`description`·`benefit`·`condition`·`includes` |
| M2 | feature | 관리자: `LessonViewModel.admin`(Fake/Hybrid + `SHOW_DEMO_PANEL` 일 때만, Real 은 null) — `presets`(다섯: 후면 주차 두 회차 · 장내 모의시험 불합격→합격 · 점검 일곱 단계 · 지식 테스트 · 예약→시험장; 첫째만 기록 시드까지, 나머지는 과제·모드·시나리오 순서), `applyPreset(id)`, `profilePresets`(장롱 10년차 · 초보 · 숙련 · 비움) + `setProfile(id)`, `resetRecords()`, `bandVisible: StateFlow<Boolean>`(기본 true) + `setBand(Boolean)`, `signalSource`(빌드 플래그를 읽어 "Fake 전부" / "Hybrid" 글자만), 기존 `DemoControls`(시나리오·정차·문·AI)는 그대로 `admin.demo` 로. 도구용 인텐트 extra 상수 `EXTRA_PRESET`(`--es preset rear-two`) | 준비실 화면 · 띠 |
| M3 | feature | 홈 대화: `LessonPhase.Setup` 에 `coach: CoachDialog?`(열렸을 때만 — `line`(규칙 문장: 예약이 있으면 "오늘은 뭘 해 볼까요? 예약한 서초 시험장에서 해도 돼요.", 기록이 있으면 "…지난번 것을 이어서 해도 돼요.", 열 때 TTS 로 읽음), `choices`: 예약 코스에 READY 과제가 있을 때만 `RESERVED_VENUE` + 늘 `PARKING_PRACTICE` + 이어서 할 기록이 있을 때만 `CONTINUE_LAST`, 칩 글자는 `CoachChoice.label`) · `bookingOptions`(예약이 있을 때만: `COURSE_PRACTICE` = 예약 코스 첫 과제를 지금 실력에 맞는 모드(가이드/힌트), `MOCK_EXAM` = **같은 과제를 평가 모드**로, 이유 "시험장 코스 그대로, 제가 채점만 할게요." — 4b 시안 대로) · `bookingChoice`(고른 칩 강조) · `highlightBooking`(대화에서 "예약한 시험장으로" → 카드 강조 테두리) · `sheetRequest: TaskType?`(대화가 "시트를 이 분류로 열어라"). ViewModel 진입점 `openCoach()` · `chooseCoach(choice)` · `closeCoach()` · `chooseBooking(option)` · `consumeSheetRequest()`(시트를 연 뒤 부른다). 세션을 시작하거나 다시 시작하거나 예약을 바꾸면 대화 상태는 지워진다 | 홈 A1 · 대화 시트 · 예약 카드 |
| M4 | feature + data | 프로필: `ProfileStatement` 를 칩 값 열거형으로(§2 표), `ProfileStore`(앱 내부 `profile.json`, 저장 실패는 로그만), 첫 실행 판정(`Setup.onboarding: ProfileOnboarding?` — 저장된 프로필이 없고 프리셋도 안 썼을 때), `Setup.profileRows`(다섯 줄: 질문 · 지금 답(없으면 "아직") · 칩 목록), `Setup.observedLines`(앱이 본 것, 숫자 없는 문장 둘~셋), `LessonReport.askOne: ProfileRow?`(D 카드 — 세션당 하나, 빈 줄부터, "다음에요" 두 번이면 그 줄은 시트에서만). 진입점 `answerProfile(rowId, chip)` · `skipOnboarding()` · `skipAsk(rowId)`. 프로필이 바꾸는 것 = 눈썹 · 첫 제안 · 멘트 서두 톤 · Copilot 프롬프트. 채점 불변 | 첫 실행 화면 · 프로필 시트 · 리포트 D 카드 |

M2 의 인텐트 extra(`AdminPresets.EXTRA_PRESET` = `preset`)는 **도구(emu_flow·course_flow)가 첫 실행 온보딩을 건너뛰고 같은 프로필로 시작**하기 위한 것이다 — Claude 가 tools 를 별도 PR(scope tools, 한 PR = 한 scope)로 고친다(emu_flow `--es preset rear-two`, course_flow `--es preset exam-fail-pass`). 프리셋 id 는 `data/AdminPresets.kt` 상수. 화면(`MainActivity`)이 extra 를 읽어 `admin?.applyPreset(...)` 을 부르는 한 줄은 ④ 에서 Codex 가.

## 2. 프로필 질문 — 기본값으로 확정 (사용자 답이 오면 바꾼다)

| # | 질문 | 기본값 |
|---|---|---|
| 1 | 호칭 | 받지 않음, 눈썹 "연수생" |
| 2 | 진술 다섯 | 유지(면허 · 마지막 운전 · 타는 차 · 필요한 일 · 무서운 상황), "타는 차" 는 맨 끝 — D 로만 묻는다 |
| 3 | 답 형식 | 칩만, STT·대화·키보드·"말로 답하기" 자리 없음 |
| 4 | 숫자 답 | 범위 칩 "작년쯤 / 몇 년 전 / 십 년 넘게 / 기억이 안 나요" → 내부 12 / 36 / 120 / null 개월 |
| 5 | 첫 실행에 묻는 수 | 둘 — 무서운 상황 · 마지막 운전, 그 뒤 "나머지는 연습하면서" |
| 6 | D 카드 | 리포트 요약 `다시 시작` 위, 세션당 하나, 빈 줄부터, "다음에요" 두 번이면 시트에서만 |
| 7 | 앱이 본 것 | 시트 아래 읽기 전용, 숫자 없이 말로 |
| 8 | 저장·초기화·다중 운전자 | 앱 내부 `profile.json` 하나, 초기화·프리셋 복원은 관리자에서만, 운전자 한 명(파일은 목록 구조) |
| 9 | 시연 시작 프로필 | 관리자 프리셋 "장롱 10년차"(온보딩·D 카드 건너뜀), 영상 오프닝에 첫 질문 한 장만 짧게 |
| 10 | 건너뛰면·영향 범위 | 빈 프로필(초보 가정) → 첫 제안 후면 직각 주차 · 가이드. 범위: 눈썹 · 첫 제안 · 멘트 서두 톤 · Copilot 프롬프트, 채점 불변 |

## 3. 항목별 구현 메모 (Codex)

| # | 어디 | 할 것 · 주의 |
|---|---|---|
| ① | `ReportScreen.kt` `CertificateContent` | 시안 B 대로: 눈썹 `진단서` → 고지 → 눈썹 "공유 범위 · 위로 갈수록 넓게" → 카드 셋(위 원시 신호 / 가운데 항목별 / 아래 총점만 — **`report.shareLevels` 를 뒤집어 그린다**), 카드 왼쪽 = `label` 40 + `description` 32 Muted, 오른쪽 = `benefit` 36 + "`condition` · 예시" 28 Muted, 선택 카드 = Periwinkle 채움 + Paper 글자(`SelectedCard`), 나머지 = Lavender(`Card`). 라디오는 없애고 카드가 선택 타깃(`selectable`, Role.RadioButton 시맨틱 유지 — 계측). 카드 아래 한 줄 "넓은 범위일수록 혜택이 늘어요 · 포함 항목은 공유 예시에서 확인"(28 Muted). `공유 예시 보기` 는 토스트 대신 **공유 예시 시트**(시안 페이지 "공유 예시 보기 — 존재 의미와 세부 항목" 절의 모형: 발급 대상 칩 · 과제 · 포함 항목 체크리스트(선택 범위에 따라 켜짐/흐림) · 제외 "위치 · 대화 · 음성" · 대각선 워터마크 "예시 — 실제 전송 없음" · `← 돌아가기`). 하단 알약은 라운드 22 ④ 규칙. 신호 출처는 라운드 22 ⑥ 의 한 줄. 모의시험(코스) 진단서도 같은 틀 |
| ② | 라운드 22 ⑤ 의 공통 트랙 부품 | 분류 트랙에만 B 먹 색을 준다: 바탕 `Lavender.copy(alpha=.5f)`, 높이 96 dp, 선택 알약 `Ink` + `Paper` 글자 + `CoachTexture.SelectedChip`, 비선택 `Ink.copy(alpha=.6f)`, 라벨 48 dp. **모드 트랙(라운드 22 ⑤)은 지금 색 그대로**(분류 = 먹, 모드 = 지금 Periwinkle — 위계). 준비 중 표시(32 dp)도 같은 색 규칙. 시트 숫자 0 |
| ③ | `SetupScreen.kt` + 새 `CoachSheet`(Setup 안 보조 화면) | A1 간격표(시안 페이지)를 그대로: `시작` → 57 dp → [`과제·모드 바꾸기` 글자 링크 · `코치에게 말하기` 보조 알약(마이크 아이콘, 112 dp)] 한 줄, 예약 있으면 이유 줄 → 33 → 예약 카드(미니 지도 + "예약한 서초 시험장 · 주차 3종 — 거기서 할 걸 골라요" + `bookingOptions` 칩) → 33 → `시작`, 예약 배지 줄은 카드 눈썹으로 흡수(따로 그리지 않음). 예약 없으면 카드 자리 없음(위 묶음이 그만큼 내려옴 — 화면 전환 때만 바뀌므로 허용). `코치에게 말하기` → `openCoach()` → 오른쪽 열이 대화 시트로(왼쪽 일러스트 유지): 눈썹 `코치와 이야기` → 27 → 말풍선(`coach.line`) → 33 → 답 칩(`coach.choices`) → 18 → 안내 한 줄 → 57 → 흐린 마이크 알약 "말로 답하기 — 준비 중"(opacity .55, 그림자 없음, 눌러도 아무 일 없음) · `← 돌아가기`(과제 시트와 같은 자리). 칩 → `chooseCoach` → 시트 닫힘, `sheetRequest` 가 있으면 과제 시트를 그 분류로 연다(`consumeSheetRequest`), `RESERVED_VENUE` 면 홈 예약 카드에 빨간 테두리 6 dp. 홈은 정차 화면이지만 잠금 규칙은 지금 Setup 과 같게 |
| ④ | 새 `AdminHome`(준비실) · 새 `AdminBand`(띠) · `DemoPanel` 대체 · `MainActivity` | 진입: Setup 의 `BrandMark` 를 약 2초 길게 누르면 준비실(`admin != null` 일 때만 핸들러를 단다 — Real 빌드엔 아예 없음). 준비실 = 시안 D: 왼쪽 Ink "지금"(AI 상태 · 신호 출처 · 기록 · 프로필), 오른쪽 프리셋 카드 다섯 → 프로필 전환 칩 넷 → 신호 출처(읽기 전용 글자) · 세션 중 패널(띠/숨김) · 기록 초기화 · AI 연결 → `이 설정으로 홈 →`(주 알약). 띠 = 시안 C: 화면 맨 아래 Ink 약 90 dp, 첫 줄 시나리오 칩(지금 과제) · 정지 · 정차 · 출발 · 문 열기 · 문 닫기 · AI 상태, `더 보기 ▴` 둘째 줄 기록 초기화 · AI 연결 · 패널 숨김. 띠가 있으면 콘텐츠 영역이 그만큼 위로 줄어든다(아래 알약을 가리지 않음). **라벨은 지금 패널과 같은 글자**(못한 주차 · 잘한 주차 · 정차 · 출발 · 문 열기 · 문 닫기 · 시나리오 정지 — emu_flow 가 글자로 찾는다). 잠금(속도 > 5) 중엔 지금 패널처럼 띠도 없앤다. 우상단 `시연` 알약은 **없앤다**(Fake 빌드는 띠가 기본으로 보이므로 emu_flow 의 `open_demo_panel` 은 시나리오 라벨을 보고 그냥 지나간다). `MainActivity`: 인텐트 extra `EXTRA_PRESET` 이 있으면 `admin?.applyPreset(...)` |
| ⑤ | 새 `ProfileScreen`(첫 실행 전체 화면 / 홈 눈썹 시트 두 모양) · Report D 카드 | P2: 다섯 줄 아코디언 — 접힌 줄 = 질문 짧은 이름 + 지금 답(없으면 "아직", Muted) 한 줄, 펼친 줄 = 질문 문장 56 dp + 범위 칩(시트의 칩 모양, 112 dp). 칩을 누르면 `answerProfile` → 그 줄이 접히고 다음 빈 줄이 펼쳐진다. 첫 실행(`setup.onboarding != null`)은 왼쪽 Ink 패널을 붙인 전체 화면, 두 줄 뒤 "나머지는 연습하면서" + `시작하기 →`(주 알약) / 언제든 `건너뛰기` 글자 링크(`skipOnboarding`). 홈의 프로필 눈썹을 누르면 같은 화면이 오른쪽 시트로 + 아래 "앱이 본 것"(`observedLines`, 읽기 전용) + `← 돌아가기`. 리포트 요약: `report.askOne` 이 있으면 `다시 시작` 위에 카드 "하나만 물어볼게요" + 그 줄의 펼친 모양 + `다음에요`(`skipAsk`). 운전자 문장 숫자 0 |

## 4. 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 터치 0 · 운전자 화면 숫자 0(속도·자세히 보기·진단서 제외) · 잠금 화면 평면 | 계측 |
| 시트 숫자 0 · 라벨 13개 문자열 불변 · 시연 조작 라벨(못한 주차 등) 불변 | 계측 · emu_flow |
| Real 빌드(`demo == null`)에 준비실·띠·길게 누르기 핸들러 0 | 계측(Real 흉내 fixture) |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 색 토큰 0 | grep |
| `feature/`·`scoring/`·`data/` 구조 불변(이 PR 에서) | `git diff --stat` |

캡처: ① `lesson-certificate*.png`·새 `lesson-certificate-share-example.png` ② `lesson-setup-sheet*.png` ③ 새 `lesson-setup-coach.png`·`lesson-setup-coach-sheet.png`·`lesson-setup-booking-card.png` + 교체 `lesson-setup.png`·`lesson-setup-reserved*.png` ④ 새 `lesson-admin-home.png`·`lesson-admin-band.png`, 삭제 `lesson-demo-toggle-pill.png`·`lesson-panel-*.png`(README 에 대체 표기) ⑤ 새 `lesson-profile-onboarding.png`·`lesson-profile-sheet.png`·`lesson-report-ask-one.png`.

머지 뒤 Claude 가 리뷰하고 태그 `inhouse-20261006-2`(또는 `-3`) 를 만든다. 대본(`05_demo_script.md`)·컷 목록·덱의 "시연" 알약 설명은 Claude 가 같은 날 고친다.
