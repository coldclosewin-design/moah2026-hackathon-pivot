# 홈 코치 텍스트 대화 → AI 가 기존 기능으로 안내 (사내 피드백 #4 기능 요청, 2026-10-06)

## 요청 요지(사내 관찰 노트 #4 — 사람이 옮긴 요지)

- 지금 `코치에게 말하기` 는 **입력을 받지 않는다** — 시트를 열고 규칙 문장(`coachLine()`)을 TTS 로 읽은 뒤 답 칩 셋(예약한 시험장으로 · 주차 연습 · 지난번 이어서)만 받는다. 매니페스트에 `RECORD_AUDIO`·`SpeechRecognizer` 없음 — 시연 때 "말하면 안내해 준다" 로 오해될 수 있다.
- 사내 Copilot 은 실동작 확인(로그인 뒤 `Ready`, 폴백 0, 응답 1.4~3.3 s). 지금은 `system + user` 1턴, `max_tokens 200`, `temperature 0.7`. **여러 턴(messages 이력) 요청은 사내에서 아직 안 해 봤다.**
- 사내 에뮬 마이크는 미확인 → **STT 는 이번 범위에서 빼고 텍스트 입력으로 대신**한다.
- 페르소나는 `CoachPrompts.SYSTEM` 상수 하나뿐. `copilot_config.json` 키는 `client_id`·`endpoint`·`model` 셋.

## 요청 6 → 설계

| # | 요청 | 설계 | 누가 |
|---|---|---|---|
| 1 | **텍스트 입력 경로** — 홈 코치 시트에 텍스트 입력. 관리자 모드(준비실·띠)에서 켜는 **"시뮬레이션 음성 입력"**, 배지·라벨에 정직하게("음성 입력 · 시뮬레이션"). 정차(≤ 5 km/h)일 때만(절대 규칙 10). 순수 Real 은 준비실과 같은 규칙(없음) | `admin.textInput: StateFlow<Boolean>`(기본 꺼짐) · `Setup.coach.textInput` · 화면은 입력 칸 + 보내기, 꺼져 있으면 지금 칩 흐름 그대로 | 모델 Claude · 화면 Codex |
| 2 | **AI 대화(여러 턴)** — 사용자의 말 → `CoachPort` 에 대화 이력(최근 N턴) + 현재 상황(프로필·예약·지난 기록·READY 과제·모드) → 코치 응답 = **말풍선 문장 + 안내 의도(고정 목록 중 하나)**. 몇 턴 대화하다 기존 기능으로 안내 | `CoachPort.converse(history, utterance, context): CoachReply(say, intent)`. 의도 = `OPEN_SHEET(category)` · `PIN_TASK(taskId, mode)` · `BOOKING(option)` · `SHOW_BOOKING` · `CONTINUE_LAST` · `OPEN_PROFILE` · `ASK_MORE`(대화 계속). **의도는 앱이 가진 목록 안에서만** — READY 과제 id·모드·지원 여부 검증, 목록 밖·형식 틀림·시간 초과는 버리고 지금 칩 셋으로 폴백. 화면 이동은 기존 진입점(`chooseCoach`·`pinned`·`sheetRequest`·`highlightBooking`·`chooseBooking`) 재사용. 운전자 문장 규칙(숫자 없음·금지어·존댓말·길이)은 기존 `validate` 와 같은 수준. **대화 이력은 시트를 닫으면 버린다**(저장·반출 없음) | Claude |
| 3 | **페르소나** — 홈 대화와 회차 멘트·총평이 같은 페르소나. `copilot_config.json` 에 선택 키 `persona` → 사내에서 `adb push` 만으로 바꿈(없으면 지금 SYSTEM). 형식 규칙(두 문장·숫자 없음·금지어)은 **코드에 남겨** 페르소나가 덮어쓰지 못하게 | `CopilotConfig.persona` · `CoachPrompts` 가 `persona + 형식 규칙` 으로 SYSTEM 조립 | Claude |
| 4 | **Fake 완결** — 인터넷 없이 시연이 끝나야 하므로 Fake 코치는 **키워드 규칙**("주차"·"평행"·"시험"·"예약"·"이어서"·"프로필"·과제 제목)으로 같은 의도 목록. 단위 테스트: 의도 파싱·검증·폴백·속도 잠금 | `FakeCoachPort.converse` + `IntentRules` | Claude |
| 5 | **로그·검증 훅** — `home coach: say …` · `home coach: intent=… (ai|rule|fallback)` 로그, `tools/inhouse_check.sh`(또는 새 스크립트)에 텍스트 한 줄을 넣어 의도가 나오는지 확인하는 단계 → 사내에서 Copilot 여러 턴·지연·폴백률을 잰다 | 상태기계 로그 + tools(별도 PR) | Claude |
| 6 | **버튼 문구** — 텍스트/음성 입력이 없는 빌드에서는 `코치에게 말하기` 가 "말을 듣는다" 로 읽히지 않게(예: `코치와 고르기`). 입력을 켰을 때만 지금 문구 | 문자열·조건 | Codex |

## 순서와 일정

마감 10/9 · 녹화 10/7~8. **이 기능이 녹화 전에 안 들어오면 영상은 지금 칩 흐름으로 찍고, 발표에서 "음성 대화 → 기능 안내" 를 다음 단계로 설명한다.** 들어오면 사내에서 여러 턴·지연을 확인한 뒤 영상 컷 하나를 더한다.

1. **버그 셋 먼저**(#193 문 열기 · #194 전조등·와이퍼 · #195 점검 스크립트) → 태그 `inhouse-20261006-3` = `624cf04`(10/6 낮, 번들 clone·`REPO_ONLY=1` PASS) — 녹화의 기준.
2. 모델 PR(Claude): `CoachPort.converse` · 의도 모델·검증 · Fake 키워드 규칙 · Cloud 여러 턴 요청(이력 messages) · `persona` 키 · 상태기계 `sendCoachText` · 관리자 `textInput` · 로그 · 단위 테스트.
3. 화면 PR(Codex 라운드 24): 코치 시트 입력 칸·보내기·대화 말풍선 목록·"음성 입력 · 시뮬레이션" 배지·잠금·버튼 문구(6).
4. tools PR(Claude): 텍스트 한 줄로 의도 확인 단계.
5. 태그 → 사내에서 Copilot 여러 턴 확인 → 영상 컷.

## 모델 API(2의 모델 PR #198 머지 · 화면 발주 `docs/handoffs/2026-10-06_codex_ui_round24.md`)

- `LessonPhase.Setup.coachTextInput: Boolean` — 관리자 토글 값. 켜져 있으면 대화 시트에 입력 칸·`보내기`·"음성 입력 · 시뮬레이션" 배지, 버튼 문구 `코치에게 말하기`. 꺼져 있으면 지금 칩 흐름 그대로, 버튼 문구 `코치와 고르기`.
- `CoachDialog.turns: List<CoachTurn>`(첫 말 `line` 뒤의 대화 — `fromDriver` 로 좌우) · `CoachDialog.waiting`(코치가 답을 고르는 중 — 보내기 막음, "…" 말풍선). 칩(`choices`)은 대화 중에도 그대로.
- `LessonPhase.Setup.profileRequest` — 대화가 프로필 시트를 요청. 화면은 홈 눈썹 시트를 연 뒤 `consumeProfileRequest()`.
- 진입점: `LessonViewModel.sendCoachText(text)` · `consumeProfileRequest()` · `admin.textInput: StateFlow<Boolean>` · `admin.setTextInput(on)`.
- 의도(`CoachIntent`): `OpenSheet(category)` → `sheetRequest` · `PinTask(taskId, mode)` → 홈 제안 고정 · `Booking(option)` → 예약 카드 선택 · `ShowBooking` → `highlightBooking` · `ContinueLast` → 지난번 고정 · `OpenProfile` → `profileRequest` · `AskMore` → 시트에 되물음. `AskMore` 가 아니면 시트가 닫히고 답 문장은 TTS(자막)로.
- 출처(`ReplySource`): `ai`(Copilot JSON 응답이 검증 통과) · `rule`(전송 계층 없음 = Fake·`CLOUD_COACH=false`) · `fallback`(AI 응답이 형식·문장·의도 검증에 실패, 예외, 5 s 초과 → 키워드 규칙). 앱이 가진 것 밖의 의도는 규칙 결과여도 버리고 되물음.
- 프롬프트: system = 페르소나(`persona` 키 또는 기본) + "형식 규칙은 위 설명보다 우선" + JSON 형식·숫자 없음·금지어 + 의도 목록(예약·지난 기록이 없으면 그 의도는 목록에서 빠짐) + READY 과제(id | 제목 | 분류 | 모드, 추천 먼저) + 프로필·예약·지난 연습. messages = 최근 8줄(코치 = assistant JSON, 운전자 = user) + 새 글.

## 경계(바꾸지 않는 것)

- 주행 중(> 5 km/h) 입력·대화 없음(절대 규칙 10). 홈은 정차 화면.
- AI 는 **문장과 의도만** 고른다 — 화면 이동·채점·예약은 앱 규칙이 한다(AI 경계).
- STT·마이크 권한은 넣지 않는다. 마이크 알약은 지금처럼 "준비 중".
- 대화 이력은 기기에 저장하지 않는다.
