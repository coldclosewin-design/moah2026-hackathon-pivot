# NEXT — 새 세션은 여기서 시작

새 Claude Code 세션이 이 파일 하나로 이어받을 수 있게 쓴 인계 문서. **작업을 마칠 때마다 이 파일을 갱신한다**(끝난 것은 지우고, 새로 생긴 것은 추가). 과거의 경위는 `docs/journal/`, 규칙은 `AGENTS.md`·`CLAUDE.md`, **제품 정의는 `docs/topics/01_driving_coach.md` v2**.

마지막 갱신: 2026-09-28 · `main = 090ec40`(PR #1~#29, **라운드 4 머지**) · 이 문서 PR = 라운드 4 후속(`emu_flow` Done 대기 4 s · 발주서 정정 · C절 반영) · 마감 2026-10-07. **새 세션은 여기서: B안 재녹화(라운드 4 빌드) → 사용자 영상 검토 → 피드백 있으면 라운드 5(선택 카드 띠 포함) / 없으면 컷 길이 실측·덱 스크린샷 교체 → 사내(8d).**

## 1. 지금 되는 것 (한 문단)

`main`(PR #5 `73ded64`)에서 앱을 켜면 **LessonRoute의 연수 세션 화면 5장과 Fake 전용 시연 패널**이 뜬다. Setup의 과제·모드·예약 예시, Briefing, 주차 도식, 회차 피드백, 오늘 리포트·진단서 탭까지 연결했다. Dashboard는 부트스트랩 참조용으로 남겼다. **빌드·단위테스트 99개 통과**, CSTDe_API_34에서 `Lesson contract passed`; 5화면과 진단서·잠금·미측정/4줄 자막 캡처는 `docs/screenshots/lesson/`. 플랫폼·상태기계·채점·시드 구조·Gradle 파일은 변경하지 않았다.

**제품 정의 v2 (9/26)**: 시연 본편은 **후면 직각 주차 과제**. `Setup(대화·제안) → Briefing → Maneuver(도식) → Done("다 됐어요") → Report(도어 열림)`. 채점은 과정만. 모드 가이드→힌트→평가→지식테스트. 상세는 `topics/01_driving_coach.md`.

**시작 가능한 과제(READY) 셋**: 출발 전 점검(Step 11, 가이드·힌트·평가, 차는 서 있음) · 후면 직각 주차(시연 본편) · 지식 테스트(Step 10, 화면은 라운드 2 에서 완성). 나머지 6개는 "준비 중". 단위 테스트 141(main `10e4d1a`, 9/27 확인).

**9/27 결정 3건이 main 에 반영됨(PR #21 → 화면 PR #25)**: 운전자 문장에 숫자 없음("서두.\n조언." 두 문장 + 총평 "흐름.\n안전." 두 문장) · 회차 시작 발화 · `SHOW_DEMO_PANEL` 플래그 · `AttemptRecord.path`(추정 궤적 — Done 왼쪽에 그려짐). 시연 패널은 기본 접힘(`시연`). 단위 테스트 147.

## 2. 남은 일 (의존 순서. 날짜 배정이 아니다)

### Step 3 — 신호 계층 + 채점 (Claude) — ✅ 2026-09-26 브랜치 `claude/signals-scoring` (PR 대기)
| # | 일 | 어디 |
|---|---|---|
| 3a ✅ | B층 상수 10개 | `vss-stub/.../VssConstants.java` (COVESA 추정, `SelectedGear` 로 P/D 표현), 파서 `vehicle/VssGear.kt`(`Gear.parse`, `toVssIgnitionOn`) |
| 3b ✅ | `SignalAvailability { LIVE, SIMULATED, MISSING }` + `SignalRegistry` + `AvailabilityBadge` | `vehicle/SignalAvailability.kt`. 세션 동안 값이 온 키만 기록. Fake 면 전부 `SIMULATED` |
| 3c ✅ | `Scenario`/`ScenarioBuilder`(`at`, `speedRamp`), `FakeVehiclePort.play(scenario, speedFactor)`·`stop`·`holdSpeed`·`playback` | `vehicle/Scenario.kt`, `vehicle/FakeVehiclePort.kt`. 주차 2벌 `data/ParkingScenarios.kt`(잘한 26 s / 못한 44 s). 기본 sin 속도 시뮬은 도로용으로 그대로 |
| 3d ✅ | A층 `MotionSegmenter`, `HarshEventDetector`(300 ms 시간창 차분·1 s 디바운스) | `scoring/`. 이동 평균은 진짜 급정지를 깎아서 뺐다(일지 9/26) |
| 3e ✅ | B층 `SteeringReversalCounter`, `GearShiftCounter`, `ProximityMonitor`(거리 → 없으면 boolean), `PreDriveChecklist` | `scoring/`. 입력이 비면 null = 미측정 |
| 3f ✅ | `ParkingMetrics`·`ParkingRubric`·`ParkingScorer`(숙련/안전)·`ParkingDelta`·**`ParkingRecorder`**(delta 를 받아 시계열 → metrics → score) | `scoring/ParkingScorer.kt`, `ParkingRecorder.kt`. 상태기계는 `recorder.onDelta(now, delta)` 만 부르면 된다. `ParkingRecorderScenarioTest` 가 시나리오 2벌의 숫자를 고정 |
| 3g ✅ | `HybridVehiclePort(real, fake)` — Real 이 한 번이라도 값을 낸 키는 live, 나머지는 Fake 시나리오. `SignalRegistry.forPort` 가 키별 출처를 갈라 배지가 "실신호 2 · 시뮬 6" 로 섞인다 | `vehicle/HybridVehiclePort.kt`. 팩토리가 Real 을 기본으로 감싼다(`-PfillMissing=false` 면 순수 Real). live 키엔 시연 조작이 먹지 않는다 |

단위 테스트 21 → 62 (실패 0).

### Step 4 — 상태기계 + 시드 (Claude) — ✅ 2026-09-26 브랜치 `claude/lesson-state-machine` (PR 대기)
| # | 일 | 어디 |
|---|---|---|
| 4a ✅ | `LessonPhase` — `Setup · Briefing · Maneuver · Done · Report` (+ `GuideStepView`) | `feature/lesson/LessonPhase.kt`. `Maneuver` 에는 점수 필드가 **타입상 없다** |
| 4b ✅ | `LessonStateMachine`(`begin·finishAttempt·nextAttempt·endSession·reset`), `LessonViewModel`(+ `DemoControls`: 시나리오 재생·정차·도어 — Fake 일 때만) | `feature/lesson/LessonStateMachine.kt`, `LessonViewModel.kt`. 회차 시작 때 `vehicle.get()` 으로 레지스트리를 먼저 심는다(안 그러면 가이드가 전 단계를 MISSING 으로 보고 한 번에 읽어 버린다 — 일지 9/26) |
| 4c ✅ | `GuideRunner` — 단계 = (대사, 신호, 확인 문장, 판정). 한 스냅샷에 여러 단계가 충족돼 있으면 연달아 확인 | `feature/lesson/GuideRunner.kt`. 주차 6단계는 `SeedCatalog.parkingGuide` |
| 4d ✅ | `HintRules` — 채점 지표 **증가분**으로 힌트(벨트·근접·급조작 URGENT / 조향·기어 NORMAL), 규칙별 5 s 쿨다운 | `feature/lesson/HintRules.kt` |
| 4e ✅ | `SeedCatalog` — 과제 9, 가이드 6단계, 프로필 질문 5 + 시연 프로필(장롱 10년차·주차 공포), 멘트 21(밴드 4 × 태그), 지식 3, 예약 카드, 혜택 예시 | `data/SeedCatalog.kt`. **문구는 Codex 가 다듬는다** |
| 4f ✅ | `ManeuverDisplayState` 매퍼 + 리플렉션 테스트(skill/safety/reversal/shift/score 필드 없음) | `feature/lesson/ManeuverDisplayState.kt` |
| 4g ✅ | `Profile`(진술+관측), `ProgressStore`(인메모리), `ModeAdvisor`(가이드 70점×2 → 힌트, 힌트 80점×2 → 평가; 과제는 공포 → 약점 → 첫 쉬운 것) | `feature/lesson/LessonModels.kt`, `ProgressStore.kt` |
| 4h ✅ | `CoachPort` + `FakeCoachPort`(멘트 풀 변주·총평) — Step 6 의 Fake 를 앞당김. AI 구현체만 남음 | `ports/CoachPort.kt`, `feature/lesson/RemarkPool.kt` |
| 4i ✅ | `App.kt` 배선: registry·store·coach·scenarios·lesson. **`MainActivity` 는 아직 Dashboard** — 화면은 Step 5 Codex | `App.kt` |

단위 테스트 62 → 89 (실패 0). 첫 Codex 발주서: `docs/handoffs/2026-09-26_codex_lesson_screens.md`.

### Step 5 — 화면 (Codex) — ✅ PR #5 머지 `73ded64` (Claude 리뷰: 빌드·emu_flow PASS·lesson_shots PASS·캡처 눈으로)
`ui/lesson/`의 Setup / Briefing / Maneuver / Done / Report + DemoPanel + LessonRoute. 화면은 단계별 데이터만 받고 Route만 LessonPhase를 분기한다. Maneuver는 원래 snapshot의 locked/stopped 판정을 전달받는다(표시 속도 반올림으로 5.1 km/h를 해제하지 않음). 잠금 시 시연 패널까지 제거, 정차 때만 완료 버튼, 힌트는 4초 후 소거, 완료 버튼은 두 번 펄스 후 정지한다. 전 화면 32 sp 이상·한국어 Phrase 줄바꿈·말줄임 없는 자막.

- `LessonPresentationTest`: 표시 매핑 10개 추가(전체 99개). 미측정 지표 생략, 비교 없음/개선/악화, 배지·도식 경계, 이전 회차 TTS의 점수 문장 차단 등. Maneuver의 자막·힌트·가이드에서 `점수/감점/N점` 문장을 걸러 접근성 트리로도 새지 않게 한다.
- `LessonScreenInstrumentation`: 추가 Gradle 의존성 없이 플랫폼 접근성 검사. 잠금 상태 클릭·스크롤 0, 점수 비노출, 화면 액션, 신호 누락, 4줄 자막, 힌트 만료, 진단서 라디오. AGP가 덮어쓰는 첫 instrumentation 항목은 기본 runner 자리표시자로 보존했다.
- Fake 기본 속도가 도로용 45~95 km/h이므로 Setup 진입 시 DemoControls.stopCar(), 시나리오 재생 시 stopScenario → resumeCar → play를 호출한다. Real은 demo=null이라 해당 호출·패널이 없다.
- 원본 `tools/emu_flow.sh`는 PASS/clashes 0이지만 두 번째 회차가 끝나기 전에 완료할 수 있는 대기 조건 오류를 발견했다. `INTEGRATION.md` C절의 도구 수정 요청을 먼저 확인할 것.
- 두 번째 `asked done`만 `(attempt 2)`로 한정한 로컬 검증본으로 전체 재생 PASS/clashes 0: 못한 주차 60/55·이동 4회, 잘한 주차 100/100·이동 2회, 도어 → Report. 시작부터 리포트 104초(덤프·탭 대기 포함), 회차 시간 53초/34초. 실시간 Fake 배지는 `실신호 0 · 시뮬레이션 8 · 미측정 0`으로 실제 수신 개수를 표시한다.

### Step 6 — AI (Claude) — 🟡 전송 계층만 남음
`ports/CoachPort.kt`(계약) + `FakeCoachPort`(시드 풀, Step 4) + **`CloudCoachPort(fallback, transport)`**(프롬프트 조립 `CoachPrompts`·타임아웃 4 s·응답 검증 길이/줄수/금지어·실패 시 폴백, 예외 안 던짐 — 테스트로 강제). `App.kt` 는 `transport = null` 로 배선 → 지금은 항상 시드 풀. **사내 Copilot 인증 방식 확인 → `CoachTransport.complete(system, user)` 구현체 하나** 넣으면 끝.

### Step 7 — 시연 파이프라인 (Claude) — ✅ 실행 검증 완료 (PR #6 에서 대기 조건·고정값 검증 보강)
| # | 일 | 상태 |
|---|---|---|
| 7a | `tools/emu_flow.sh` 주차 흐름(Setup 힌트 → 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 문 열기 → Report). 힌트 3종·회차 2·배지를 로그(`hint:`·`asked done`·`attempt N: skill=`·`report:`)와 라벨로 판정 | ✅ PASS·clashes 0·103 s. PR #6: `asked done (attempt N` 한정 + 회차 채점 고정값(60/55·4 · 100/100·2) 검증 |
| 7b | `tools/lesson_shots.sh` — Codex 의 `LessonScreenInstrumentation` 을 돌려 `lesson-*.png` 를 꺼낸다. `concept_shots.sh`·`gps_flow.sh` 는 삭제(16번 `0c95d18` 에 있음) | ✅ 실행 PASS·8장 캡처 |
| 7c | `docs/05_demo_script.md` — 2분 10초 타임라인·말할 것·시간 조절·사고 대응 | ✅ 실측 시각 반영(PR #6) |
| 7d | `DEMO_SPEED_FACTOR` 기본 10 → **1.0**(주차 시나리오는 실시간이 맞다). 상태기계에 스크립트용 로그 `hint:`·`asked done` | ✅ |
| 7e | 화면 PR 리뷰 뒤: `emu_flow.sh` PASS → 대본 시각 실측 → 캡처를 눈으로. 계측 계약을 만들 때 `build.gradle.kts` 의 `testInstrumentationRunner` 주석을 먼저 읽는다 | ✅ 9/26 (Codex 는 자리표시자 첫 항목으로 함정 회피) |

### Step 8 — 발표·영상·제출 — 🟡 문서 초안 완료
| # | 일 | 상태 |
|---|---|---|
| 8a | `docs/presentation/01_deck_outline.md` — 12장·7분, 장마다 화면·말·근거·초, 길이 조절(3/5/7/10분), 예상 질문 8, 발표자가 채울 것 | ✅ 초안(9/26). ⚠ 8·9·11장은 사내 이관 뒤 숫자 갱신 |
| 8b | `docs/presentation/02_video_shotlist.md` — 컷 13개(약 2분 05초), A 사내판(Hybrid·Signal Simulator)/B 외부판, 편집 원칙, 녹화 절차·함정 | ✅ 초안(9/26) |
| 8c | **시연 영상 B안 녹화**(외부 에뮬, 사람이 직접 누름, 소리 포함은 PC 녹화) | 🟡 소리 없는 판은 ✅ 9/27 Claude(`screenrecord` + `emu_flow`, 1920×1080, 109 s, `build/demo-round3.mp4`). **소리 포함 제출용은 ⬜ 사용자**(PC 녹화, 대본 타임라인 그대로) |
| 8d | 사내: 이관 → A안 녹화 → Bitbucket 소스 → MarketUploader APK | ⬜ 사용자 |
| 8e | 슬라이드를 사내 양식으로 옮기기, 2번 장 개인 계기 | ⬜ 사용자 |

### Step 9 — UI 재설계 (Codex 별도 세션 · 기준 = 기하학 포스터) — 🟡 라운드 1·2 머지(PR #14·#18), 라운드 3 Codex 구현 중
| # | 일 | 상태 |
|---|---|---|
| 9a | `docs/design/02_design_brief.md` — 라운드 0~3, 채울 것 9, **불변 규칙 12** + 검사 수단. 레퍼런스 = `docs/design/geometric-poster-development/`(시안 4장·핸들 B·시각 규칙·색 토큰 5). 탐색 이력(종이 UI·미니멀·Pinterest)은 `docs/design/README.md`(Codex) | ✅ 채움(모션 포함 9개 모두 사용자 확인 9/26) |
| 9b | 라운드 1 발주서 `docs/handoffs/2026-09-26_codex_ui_round1.md` — 디자인 시스템(`CoachStyle`) + `Maneuver`·`Report`, 드래프트 PR, `lesson_shots`·`emu_flow` PASS, 불변 표 12줄. **0절: Codex 본 트리 정리**(시안 폴더 → `codex/design-refs` PR, NEXT·일지 수정 되돌리기) | ✅ 시안 PR #13 `b211107` · 라운드 1 PR #14 `508f03b` (리뷰: 빌드·129·`emu_flow` PASS·`lesson_shots` PASS·캡처 vs 시안·불변 12줄). 사용자 톤 확정 |
| 9c | 라운드 2 — `Setup`(제안 문장 + 과제·모드 시트, 준비 중 회색)·`Briefing`(핸들 B 에셋)·`Done`·`Quiz`/`QuizDone`·점검 과제 칩 3개(C절 3건), 글자 없는 일러스트 에셋(WebP ≤ 1 MB), `emu_flow.sh` PASS. **라운드 1 관찰 반영**: 접힌 상태 라벨에 대상 붙이기(전부 MISSING 이면 접지 않기), `LessonCanvas` Bold → `BrandMark` 통일. 발주서 `docs/handoffs/2026-09-26_codex_ui_round2.md`. 선행(PR #16): `ManeuverDisplayState` 점검용 필드 5개, `emu_flow.sh` 시트 대응 | ✅ 발주 → ⬜ Codex |
| 9e | 상태기계 후속(Claude): ① 도어 선열림 `doorArmed` ② 조사 헬퍼 → 브리핑 "뒤 거리를 봅니다" | ✅ PR #17 `22a433a` |
| 9f | **라운드 2 Codex PR #18** — 리뷰 완료(빌드·137·`emu_flow` PASS 98 s·`lesson_shots` PASS·캡처 21장 vs 시안). 시연 영상 녹화 중 **경합 발견 → PR #19**(`publishManeuver` CAS: delta 코루틴이 Done 을 묵은 Maneuver 로 덮어쓰던 것). 영상(2분 14초)은 #18 + #19 빌드 | ⬜ 사용자 승인 대기: **#18 → #19 순서로 머지** |
| 9g | **라운드 2 영상 피드백 22건 분류** → `docs/design/03_round2_feedback.md`. 결정 3건 ✅(9/27): 숫자는 운전자 문장에서 전부 순화 / 궤적 시뮬레이션 진행 / 패널은 자동 접힘. **A 묶음(Claude, 브랜치 `claude/driver-copy-path`)**: `CoachPort` 두 문장(`AdviceRules` 조언 + 서두 풀), 총평 숫자 제거, 회차 시작 발화, `SHOW_DEMO_PANEL` 플래그, `PathReconstructor` → `AttemptRecord.path`. **디자인 세션 발주** `docs/handoffs/2026-09-27_codex_design_topview.md`(차 도식 3~4안·후진 도형·Done 궤적 시안) | 🟡 A 묶음 PR 대기 · 디자인 세션 → 사용자 선택 → 라운드 3 발주서(Claude) |
| 9h | **A 묶음 PR #21 머지**(`955adfa`). 디자인 시안 PR #22 머지(`88c29fe`) → **사용자 선택 B**(고정 바퀴 + 방향 호 + 후진 셰브론), Done 궤적 시안 확정 | ✅ |
| 9d | **라운드 3 발주서** `docs/handoffs/2026-09-27_codex_ui_round3.md` — 세 규칙(패널 접힘·숫자 없음·Headline 자동 축소) + Setup 시트 30/70·Briefing 정리·Maneuver B 도식(방향 호·셰브론)·Done 추정 궤적·Report 신호 출처 + 불변 표 + 캡처 교체. `emu_flow.sh` 는 접힌 패널을 `문 열기` 전에 다시 연다 | ✅ 발주 → ✅ **Codex PR #25 머지 `c0c3dfa`**(리뷰: 빌드·147·`emu_flow` PASS 108 s·`lesson_shots` PASS·캡처 24장 vs 시안·불변 표 — 관찰 3: 총평 머리말 중복(Claude 후속 ✅ PR #26), 회차 1 유예 여유 2 s(대본 사고 대응에 기록), `시연` 토글 상단 여백(라운드 4 후보)) → ✅ 대본 시각 실측 반영(`05_demo_script.md`) → ✅ **재녹화(B안, 소리 없음)** → ⬜ 사용자 영상 피드백 → ⬜ 컷 목록 "길이" 열 실측 · 덱 5·6장 스크린샷 교체 |
| 9i | **라운드 3 영상 피드백 11건** → `docs/design/04_round3_feedback.md`(B 9 · D 2 · A 0 — "움직임과 손맛"). **라운드 4 발주서** `docs/handoffs/2026-09-28_codex_ui_round4.md`: 조향 애니메이션(바퀴 회전 + 호 굴곡 + 점선 보조선 2) · Done 궤적 3 초 재생 · 주 버튼 규칙(140 dp × ≥ 720 dp: 시트 `시작`·`한 번 더`) · 시트 카테고리 띠 · 시연 토글 두 안 캡처 + 레일 정리 · 조향각 풀이(`오른쪽으로 한 바퀴 반`) · `조수석` 눈썹. 선행: `emu_flow.sh` 가 `시연` 을 `text`/`content-desc` 로 찾음 · **`ParkingRubric.graceSeconds` 45 → 60**(55 s 경계에서 60/58 이 흔들려 — 일지 9/28) | ✅ 발주 → ✅ 드래프트 PR #29 → ✅ 결정(D1 알약 · D2 `코치`) → ✅ **리뷰·머지 `090ec40`**(빌드·154·`emu_flow` PASS 115 s·`lesson_shots` PASS 25장·클립 2개 프레임 스트립·불변 표. 관찰: 선택 카드 `Paper` 띠가 바탕과 같아 안 보임 → 라운드 5 후보 / Done 캡처 대기 4 s·발주서 §2 오기 → ✅ 이 PR) → ⬜ 재녹화 |
| 9d-준비 ✅ | 리뷰 뒤 단계 **선반영**(9/27, 브랜치 `claude/round3-prep`): `docs/05_demo_script.md`(패널 `시연` 조작·두 문장 멘트·탑뷰 B·추정 궤적·`신호 출처`·사고 대응 2줄, **시각은 잠정**), `presentation/02_video_shotlist.md`(컷 7·9 궤적, 편집 원칙 "숫자 보이면 옛 빌드", 녹화 함정 ③④), `01_deck_outline.md`(5·6장 화면 파일명·한 줄), `topics/01` §4.1·§4.5 Done 문장. main `10e4d1a` 기준선: 빌드·141 통과 | ✅ |

리뷰 루틴(Claude): worktree 에서 head 빌드 → `emu_flow.sh` → `lesson_shots.sh` → 캡처를 시안(`docs/design/round3-topview/`)과 나란히 눈으로 → 발주서 §2 불변 표 grep(`이동 \d+회`·`\d+초` 가 `Maneuver`·`Done` 에 없음, `추정 궤적` 캡션 두 줄, `신호 출처`, 패널 기본 접힘) → PR 코멘트. `codex/design-refs`(이미지만) 는 바로 머지. **리뷰 뒤**: `emu_flow.sh` 의 `[t+NNs]` 로 대본 시각 열을 고치고, 컷 목록 "길이" 열은 재녹화에서 잰다.

**순서**: 디자인(9) → 녹화(8c) → 사내(8d). 디자인 뒤에 찍어야 두 번 찍지 않는다. **지금 트리 배치(9/27)**: 본 트리 = `codex/design-topview`(Codex 가 남긴 것, 건드리지 않음) · Codex = `.worktrees/ui-round3`(`codex/ui-round3`) · Claude = `.worktrees/hybrid` — 동시 작업 규칙(§3).

### Step 10 — 지식 테스트 (Claude 상태기계 ✅ PR #9 · Codex 화면 ⬜)
| # | 일 | 상태 |
|---|---|---|
| 10a | `LessonPhase.Quiz`(index·item·locked·chosen·correctSoFar)·`QuizDone`(results·items·remark), `QuizItem`·`QuizResult`·`QuizRecord`. 상태기계 `answer(choice)`·`nextQuestion()`, 잠금(속도 > 5) 중 답 무시, `endSession` 은 푼 것까지로 결과. 문제·선택지·정답 이유를 음성으로 | ✅ |
| 10b | 시드: 지식 과제 READY(QUIZ 모드만), 문항 5(회전교차로·비상등·우천 제동·야간 상향등·안전거리). 문구는 Codex 가 다듬는다 | ✅ |
| 10c | 화면 `Quiz`·`QuizDone` — C절 요청. 그때까지 `LessonRoute` 에 `[cross]` 자리표시자(Briefing 화면 재사용, 자막으로 진행) | ⬜ Codex(UI 재설계 라운드 2 에 함께) |
| 10d | (선택) `CloudCoachPort` 로 퀴즈 총평 변주 · STT 로 음성 답변(사내 확인 뒤) | ⬜ |

### Step 11 — 출발 전 점검 과제 (Claude ✅ PR #10 머지 `08e966b` · Codex 화면 ⬜)
| # | 일 | 상태 |
|---|---|---|
| 11a | 시드 `TASK_PREDRIVE` READY(`CHECKLIST`, 주행 불필요), 가이드 3단계(벨트 → P 확인 → 시동), 점검 멘트 12, `SeedCatalog.scenariosFor(task)` | ✅ |
| 11b | `PreDriveSummary` 에 `beltOnMillis`·`ignitionOnMillis`·`beltBeforeIgnition`(마지막 false→true 전이) · `ChecklistScorer`/`ChecklistRubric`(숙련 = 순서·완성·시간, 안전 = 움직임·P·벨트) · `ParkingRecorder.scoreChecklist` | ✅ |
| 11c | 상태기계: 과제 유형으로 채점기 선택, 힌트 규칙 분리(`HintRules(checklist = true)` — 시동 먼저·움직임), "다 되셨나요?" 는 벨트·시동·P 가 다 보이면. `CoachPort.remark(task, …)` 로 과제 전달, 머리말 `attemptHead(task, score)`("출발 준비 N초."), `RemarkTemplate.taskType` 으로 멘트 풀 분리 | ✅ |
| 11d | `ChecklistScenarios` 잘한 점검(8 s, 100/100) · 못한 점검(12 s, 60/70). `DemoControls.scenarios` 는 진행 중 과제의 것만 | ✅ |
| 11e | 화면: 점검 과제용 `Maneuver` 칩 3개·`Done/Report` 행 교체 — C절 요청 | ⬜ Codex(UI 재설계 라운드 2 에 함께) |

단위 테스트 113 → 129. `emu_flow.sh`(주차) 는 영향 없음 — 시연 본편은 그대로 주차.

### 사용자 결정·행동이 필요한 것
| # | 일 | 상태 |
|---|---|---|
| U1 | 제품명 | 가칭 DriveCoach. `res/values/strings.xml` 한 곳 |
| U2 | 사내 출근 일정 | 제출물 3종이 사내 전용. 방침: 외부 개발 후 별도 절차. 가능하면 빈 껍데기로 clone→빌드→설치 경로 먼저 |
| U3 | 주차장·코스 시드 좌표 | 없으면 서초 기준 임의, `INTEGRATION.md` 가정 표기 |
| U4 | 라운드 1 드래프트 PR 캡처를 보고 톤 확정 (브리프 답 9개·이동 중 진행 정보 숨김은 ✅ 9/26 확인) | `docs/design/02_design_brief.md` "채울 것" 모션 행. 레퍼런스·나머지 8답은 ✅ (9/26 Codex 디자인 세션에서 도출) |

### 뒤로 미루는 항목 (기술적 불확실성)
| 항목 | 이유 |
|---|---|
| STT | 설계에 자리만(정차 중 답변·Setup 대화). 구현은 3지선다 버튼부터. 사내 확인 후 |
| 주행 중 실시간 AI 발화 | 네트워크 왕복. 힌트는 규칙 |
| UI 컨셉 경쟁 | 필요해지면 16번 `ConceptScreens`·`ConceptContractInstrumentation` 되가져오기 |
| 의사 3D 지도 | 도로 과제 화면에 필요하면 16번에서 되가져오기(§5) |

### 알고 있지만 검증하지 못한 것
- B층 경로 전부 — 사내 1,251개 목록(pageId 1323873443)과 대조한 적 없음. **주차 시연은 조향각·기어 의존이 커서** 둘이 없으면 도식·가이드 확인이 전부 시뮬레이션이 된다(A층으로 회차 피드백은 성립).
- 주차센서 VSS 경로는 추정조차 불확실.
- 사내 환경 전부.

## 3. 일하는 방식 (16번에서 굳은 것, 그대로)

- **역할**: Claude = 인프라·포트·채점·상태기계·데이터 구조·문서·리뷰·머지. Codex = 화면·테스트·시드 문구. 화면은 `docs/handoffs/YYYY-MM-DD_codex_<topic>.md` 오더로(급한 한 줄은 `[cross]`).
- **흐름**: `claude/<topic>` 브랜치 → PR → 사용자가 "N 머지해" → `gh pr merge N --squash`. **머지 승인 없이 다음 작업을 쌓지 않는다.** (부트스트랩·기획 문서는 Day 0~1 이라 main 직접 커밋 — 이후는 PR)
- **Codex PR 리뷰 루틴**: 직접 빌드 → 에뮬 캡처 → **눈으로 본다** → `04_agent_workflow.md` 체크리스트 → PR 코멘트.
- **동시 작업 규칙 (9/26 사고 뒤)**: Codex 가 본 트리(`C:\Project\17_hackathon-pivot`)에서 작업 중이면 Claude 는 **거기서 `git checkout` 을 하지 않는다** — Codex 의 미커밋 파일이 Claude 브랜치로 넘어온다. Claude 는 `git worktree add .worktrees/<topic> <브랜치>` 로 별도 트리에서 빌드·커밋·PR 한다(`.worktrees/` 는 `.git/info/exclude`). 본 트리의 브랜치는 Codex 것이 유지돼야 한다. 반대로 Claude 만 일할 때는 본 트리를 쓴다.
- **에뮬**: `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd CSTDe_API_34 -no-snapshot-load` 백그라운드. 먼저 `adb get-state`. 앱은 user 10. 함정은 `tools/README.md`. 스크린샷은 `screencap -d 4619827259835644672`, 탭은 그냥 `input tap`(`-d` 는 실패).
- **셸 함정**: 큰 heredoc + 한국어 → Bash 파싱 실패(9/25 재현). 긴 파일은 Write 도구, 커밋 메시지는 `-m` 여러 개 또는 `-F 파일`. **Gradle 은 PowerShell 로**(`.\gradlew.bat …`) — Git Bash 에서 `cmd //c gradlew.bat` 은 이 환경에서 실행되지 않는다(9/26). 작은 치환은 `sed -i`, Kotlin 백틱 테스트명은 heredoc 에 넣지 않는다.
- **사용자 선호**: 한국어. 선택지가 있으면 추천과 함께. 검증 못 한 것은 그렇다고. 남은 일수를 이유로 범위를 깎지 않는다 — 미루는 이유는 기술적 불확실성만. **코드에 이름이 박히기 전에 기획을 넓히는 타이밍을 중시한다**(9/26).

## 4. 어디에 무엇이 있나

| 찾는 것 | 위치 |
|---|---|
| 제품 정의·시연 시나리오·신호 표 | `docs/topics/01_driving_coach.md` (v2) |
| 차량 신호 포트 | `vehicle/VehiclePort.kt`, `FakeVehiclePort.kt`(시나리오 재생기 들어갈 곳), `RealVehiclePort.kt`, `VehiclePortFactory.kt` |
| 신호 상수 | `vss-stub/src/main/java/mobis/vss/VssConstants.java` — 지금 5개 |
| 음성 | `ports/TtsPort.kt` — `speak(text, priority)`, `lastSpoken`, `SpeechPriority.URGENT` |
| 위치·경로 | `ports/LocationPort.kt`, `GpsLocationPort.kt`, `Route.kt` |
| 화면 골격 | `ui/MainActivity.kt`(Dashboard 배선), `ui/CoachStyle.kt`, `ui/concepts/DesignScale.kt` |
| 빌드 플래그 6개 | `automotive/build.gradle.kts` — `USE_FAKE_VSS`, `FILL_MISSING_WITH_FAKE`(`-PfillMissing`, Real 일 때 Hybrid), `USE_FAKE_LOCATION`, `TTS_VOICE`, `DEMO_SPEED_FACTOR`(기본 1.0 = 실시간), `SHOW_DEMO_PANEL`(`-PdemoPanel`, false 면 시연 패널 없음) |
| 도구 | `tools/emu_flow.sh`(주차 세션 자동 재생 — 라운드 3 부터 `시연` 으로 패널을 열고 누른다), `lesson_shots.sh`(계측 캡처), `README.md`(함정 목록) |
| 시연 대본 | `docs/05_demo_script.md` — 라운드 3 선반영, **시각은 라운드 2 실측(약 100 s) 잠정** |
| 발표·영상 | `docs/presentation/01_deck_outline.md`(12장·대본·예상 질문), `02_video_shotlist.md`(컷 13·녹화 절차 A/B·함정 4) |
| 화면 캡처 | `docs/screenshots/lesson/` — 라운드 3 계측 24장 + 실제 세션 `lesson-done-path.png`·`lesson-maneuver-b.png`·`lesson-panel-open.png`, 검증 로그 `flow.txt`·`flow-round3-*.txt` |
| 디자인 | `docs/design/02_design_brief.md`(불변 규칙·채울 것), `03_round2_feedback.md`(피드백 22건·결정 3), `04_round3_feedback.md`(피드백 11건·결정 2), `round3-topview/`(탑뷰 B·Done 궤적 시안), `references/`, `01_ui_concept_candidates.md`(16번 후보 20) |
| 가정 원장 | `docs/INTEGRATION.md` B절 |

## 5. 16번에서 되가져올 수 있는 것 (읽기 전용 참조)

`C:\Project\16_hackathon` main `0c95d18`(2026-09-22).

```bash
git -C /c/Project/16_hackathon show 0c95d18:<16번 경로> > <대상 경로>
```

| 필요해질 수 있는 것 | 16번 경로 |
|---|---|
| 상태기계 구조 | `automotive/src/main/kotlin/com/moah/hackathon/feature/journey/{JourneyStateMachine,JourneyPhase,JourneyViewModel}.kt` (`DemoControls` 포함) |
| 화면 필터 패턴 + 테스트 | `.../feature/drive/DrivingDisplayState.kt`, `automotive/src/test/.../DrivingDisplayStateTest.kt` |
| 의사 3D 지도 | `.../ui/concepts/droad/{RoadProjection,RoadMap3D,RoadStyle}.kt` |
| UI 컨셉 계약·계측 | `.../ui/concepts/ConceptScreens.kt`, `automotive/src/androidTest/.../ConceptContractInstrumentation.kt` |
| 시연 대본·발표·영상 구조 | `docs/05_demo_script.md`, `docs/presentation/{01_deck_outline,02_video_shotlist}.md` |
| Codex 오더 형식 | `docs/handoffs/2026-09-18_codex_order_status.md` 등 13건 |
| 시드 데이터 형태 | `.../data/SeedCatalog.kt` |
