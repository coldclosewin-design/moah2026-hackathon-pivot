# NEXT — 새 세션은 여기서 시작

새 Claude Code 세션이 이 파일 하나로 이어받을 수 있게 쓴 인계 문서. **작업을 마칠 때마다 이 파일을 갱신한다**(끝난 것은 지우고, 새로 생긴 것은 추가). 과거의 경위는 `docs/journal/`, 규칙은 `AGENTS.md`·`CLAUDE.md`.

마지막 갱신: 2026-09-25 · **피벗 첫 커밋 직후** · 열린 Codex 오더 없음 · 마감 2026-10-07

## 1. 지금 되는 것 (한 문단)

`16_hackathon`(Gift Drive) 에서 플랫폼 계층만 물려받은 **빈 껍데기가 빌드·단위테스트(21개) 통과** 상태다. 앱을 켜면 16번 초기의 Dashboard(속도 220sp 큰 글자, 운전석 도어·ABS 카드, "Toggle driver door" 왕복, `source=FAKE` 배지)가 뜬다 — 이것은 `VehiclePort` 가 살아 있음을 눈으로 확인하는 용도이고 연수 세션 화면이 들어오면 `MainActivity` 에서 교체한다. 살아 있는 것: `vehicle/`(VehiclePort·Fake·Real·Factory·VssValues), `ports/TtsPort`(자막 동기·청크 분할·URGENT 큐·kob 음성 선택), `ports/{LocationPort,GpsLocationPort}`(속도 적분 Fake + GPS), `ports/Route`(경로 → 화면용 RouteView, 아직 Gift Drive 의 `hidden` 개념이 남아 있음), `ui/concepts/DesignScale`(2560×1268 dp 고정), `ui/CoachStyle`(팔레트), `tools/` 3종(아직 Gift Drive 흐름을 가리킴), 문서 골격 전부.

## 2. 남은 일 (의존 순서. 날짜 배정이 아니다)

### Step 3 — 신호 계층 (Claude)
| # | 일 | 메모 |
|---|---|---|
| 3a | `vss-stub/.../VssConstants.java` 에 B층 경로 9개 추가 | `docs/topics/01_driving_coach.md` §신호 표. COVESA 표준명 기준, 전부 사내 미확인 → `INTEGRATION.md` B절 9/25 항목 |
| 3b | `SignalAvailability { LIVE, SIMULATED, MISSING }` + 신호 레지스트리 | `MISSING` = 구독했으나 일정 시간 값 없음. Fake 가 낸 값은 `SIMULATED`. 리포트 배지 "실신호 N · 시뮬 N · 미측정 N" 의 근거 |
| 3c | `FakeVehiclePort` 시나리오 재생기 | 지금은 500 ms 틱 sin 속도. 타임라인 선언(t, 경로, 값)을 `DEMO_SPEED_FACTOR` 로 압축 재생. 시나리오 2벌(잘한 주행·못한 주행) |
| 3d | `scoring/` A층 지표 엔진 | 속도열 스무딩 → a(급가속·급제동) → jerk(승차감) · 과속(구간 제한속도) · 정속 편차 · 정지/출발 부드러움. **JVM 테스트로 합성 속도열 검출 확인** |
| 3e | `scoring/` B층 채점표 | 항목마다 필요 신호가 `MISSING` 이면 채점 제외. 도로주행시험 감점표 배점 참고, 70점 합격선 |

### Step 4 — 상태기계 + 코스 시드 (Claude)
| # | 일 | 메모 |
|---|---|---|
| 4a | `feature/lesson/LessonPhase.kt`, `LessonStateMachine.kt`, `LessonViewModel.kt` | `Setup → Briefing → [Driving → SegmentReview]×n → Report`. 임계값 `LOCK 5 km/h`, `STOP 1 km/h` + 운전석 도어 열림 → Report. 주행 중 도어 열림은 무해. 16번 `JourneyStateMachine` 구조(StateFlow, 포트 주입, Log 외 안드로이드 의존 없음) 그대로 |
| 4b | `Segment`/`ExpectedAction` 모델 | `Route` 위에 구간 경계·제한속도·기대 행동(방향지시등·완전정지·감속·기어P·비상등·안전벨트) |
| 4c | 새 `data/SeedCatalog.kt` | 실생활 경로 1~2개(손으로 찍은 polyline + `smoothed()`) + 구간 정의 + 코칭 문구. 시드 **내용**(문구·좌표)은 Codex 가 다듬는다 |
| 4d | 규칙 기반 실시간 코칭 → `TtsPort.speak(…, URGENT)` | 감점 순간 즉시. 같은 항목 반복 시 쿨다운 |
| 4e | `DrivingDisplayState` 매퍼 | 주행 중 화면에 점수·감점 누계·터치 타깃을 주지 않는다(AGENTS 규칙 10). 유닛테스트로 강제 |

### Step 5 — 화면 (Codex 발주, `docs/handoffs/`)
`Setup`(코스·난이도 선택) / `Briefing` / `Driving`(잠금·자막·음성 중심) / `SegmentReview` / `Report`(진단 리포트 + **제출용 진단서 레이아웃 한 장** — 2순위 서사). 시연 조작(정차·출발·문 열기·닫기·시나리오 전환)은 Fake 일 때만 노출.

### Step 6 — AI 총평 (Claude)
`ports/CoachPort.kt` + `FakeCoachPort`. `reviewSegment(segment, events)`, `summarize(report)`. **예외를 던지지 않고 규칙 문장으로 폴백** — 테스트로 강제. Cloud Copilot 구현체는 사내 인증 확인 후.

### Step 7 — 시연 파이프라인 (Claude)
`tools/emu_flow.sh` 를 새 흐름(탭 라벨·로그 태그 `MOAH/LessonStateMachine`·단계)에 맞게 수정 → `result: PASS`. `docs/05_demo_script.md` 재작성(16번 것은 지웠다. 구조: 준비 명령 → 분단위 타임라인(시각/조작/화면·음성/말할 것) → 시간 조절 → 사고 대응표). 계측 계약(`androidTest/`)을 새로 만들 때 `build.gradle.kts` 의 `testInstrumentationRunner` 주석을 먼저 읽는다.

### Step 8 — 제출
발표 덱(16번 `docs/presentation/01_deck_outline.md` 의 슬라이드 7~11 서사 — "차량 신호가 UX 규칙의 근거다", "사내 머지는 두 줄", "1인 + AI 에이전트 2", "정직한 한계" — 는 그대로 쓸 수 있다. 16번 저장소 `0c95d18` 에서 읽는다), 시연 영상 **B안(외부 에뮬 녹화)을 먼저 완성**, 사내에서 Bitbucket·MarketUploader·A안(3D 에뮬) 녹화.

### 사용자 결정·행동이 필요한 것
| # | 일 | 상태 |
|---|---|---|
| U1 | **제품명** | 가칭 DriveCoach. `res/values/strings.xml` 한 곳 |
| U2 | **사내 출근 일정** | 제출물 3종(Bitbucket·MarketUploader·3D 에뮬 녹화)이 전부 사내 전용. 사용자 방침: 외부 개발 후 별도 절차. **가능하면 빈 껍데기(지금 상태)로 clone→빌드→설치 경로만 먼저 뚫어 보는 것을 권한다**(`06_inhouse_migration.md`) |
| U3 | 연수 코스 실제 경로 | 시드에 넣을 "집 → ○○" 경로 1~2개. 없으면 서초 기준으로 지어 넣고 `INTEGRATION.md` 에 가정 표기 |

### 뒤로 미루는 항목 (시간이 아니라 기술적 불확실성 때문)
| 항목 | 이유 |
|---|---|
| STT 음성 질의응답 | 외부 에뮬 `RecognitionService` 존재만 확인·실동작 미검증, 사내 미확인. 대화감이 필요하면 **정차 중 화면 버튼 질문**으로 STT 없이 먼저 |
| 주행 중 실시간 AI 발화 | 네트워크 왕복 → 급제동 경고가 늦으면 무의미. 사내 네트워크·Copilot 인증 미확인 |
| UI 컨셉 경쟁(`-PuiConcept`) | 이번엔 주행 중 화면을 거의 안 쓴다. 필요해지면 16번 `ui/concepts/ConceptScreens.kt` + `ConceptContractInstrumentation` 을 되가져온다 |
| 의사 3D 지도(`RoadMap3D` 등 ~927줄) | 주행 화면에 지도가 필요하다고 판단되면 16번에서 되가져온다(아래 §5) |

### 알고 있지만 검증하지 못한 것 (숨기지 말 것)
- B층 신호 경로 9개 전부 — 사내 1,251개 목록(pageId 1323873443)과 대조한 적 없음. 틀리면 조용히 무시된다.
- 사내 환경 전부(16번 `NEXT.md` 의 "사내 이관 1차 시도"는 한 번도 수행되지 않았다).
- 16번에서 검증된 것 중 이번에도 유효한 것: 외부 에뮬에 mobis 프레임워크 없음(→ `USE_FAKE_VSS=true` 확정), Google TTS 존재·kob 음성, 2560×1440·밀도 160, `DesignScale` 동작.

## 3. 일하는 방식 (16번에서 굳은 것, 그대로)

- **역할**: Claude = 인프라·포트·채점·상태기계·데이터 구조·문서·리뷰·머지. Codex = 화면(`ui/`, `feature/setup|driving|report/`)·테스트·시드 문구. 화면이 필요하면 직접 고치지 말고 `docs/handoffs/YYYY-MM-DD_codex_<topic>.md` 에 오더를 써서 사용자에게 준다(급한 한 줄은 `[cross]`).
- **흐름**: `claude/<topic>` 브랜치 → PR → 사용자가 "N 머지해" → `gh pr merge N --squash`. **머지 승인 없이 다음 작업을 쌓지 않는다.** 일지의 근거는 PR 번호로.
- **Codex PR 리뷰 루틴**: 그 브랜치를 **직접 빌드** → `assembleDebug testDebugUnitTest` → 에뮬 캡처 → **캡처를 열어 눈으로 본다** → `04_agent_workflow.md` 체크리스트 → PR 코멘트.
- **에뮬**: `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd CSTDe_API_34 -no-snapshot-load` 를 백그라운드로. 먼저 `adb get-state`. 앱은 user 10. 함정은 `tools/README.md`.
- **셸 함정**: 큰 heredoc 에 한국어 본문 → Bash 파싱 실패(9/25 재현). 긴 파일은 Write 도구, 커밋 메시지는 `-F 파일`.
- **사용자 선호**: 한국어. 선택지가 있으면 추천과 함께 묻는다. 검증하지 못한 것은 그렇다고 말한다. 남은 일수를 이유로 범위를 깎지 않는다(밤을 새워서라도 진행) — 미루는 이유는 기술적 불확실성만.

## 4. 어디에 무엇이 있나

| 찾는 것 | 위치 |
|---|---|
| 차량 신호 포트 | `vehicle/VehiclePort.kt`(계약), `FakeVehiclePort.kt`(시나리오 재생기 들어갈 곳), `RealVehiclePort.kt`(VSSManager 유일 참조), `VehiclePortFactory.kt`(USE_FAKE_VSS 스위치) |
| 신호 상수 | `vss-stub/src/main/java/mobis/vss/VssConstants.java` — 지금 5개, B층 9개 추가 예정 |
| 음성 | `ports/TtsPort.kt` — `speak(text, priority)`, `lastSpoken`(자막), `splitForSpeech`, `SpeechPriority.URGENT` 는 큐를 비운다 |
| 위치 | `ports/LocationPort.kt`(Fake 속도 적분), `GpsLocationPort.kt` |
| 경로 | `ports/Route.kt` — `view(progress, hidden, …)` → `RouteView`. `Segment` 는 여기에 얹는다 |
| 화면 골격 | `ui/MainActivity.kt`(Dashboard 배선), `ui/CoachStyle.kt`, `ui/concepts/DesignScale.kt` |
| 빌드 플래그 4개 | `automotive/build.gradle.kts` — `USE_FAKE_VSS`, `USE_FAKE_LOCATION`, `TTS_VOICE`, `DEMO_SPEED_FACTOR` |
| 도구 | `tools/emu_flow.sh`, `gps_flow.sh`, `concept_shots.sh`, `README.md`(함정 목록) — **전부 Gift Drive 흐름을 가리킨다. Step 7 에서 수정** |
| 주제·채점 설계 | `docs/topics/01_driving_coach.md`, 피벗 결정 `docs/topics/_comparison.md` 하단 |
| 가정 원장 | `docs/INTEGRATION.md` B절 — 9/25 항목이 피벗 이후 |

## 5. 16번에서 되가져올 수 있는 것 (읽기 전용 참조)

`C:\Project\16_hackathon` main `0c95d18`(2026-09-22). 파일 하나 꺼내기:

```bash
git -C /c/Project/16_hackathon show 0c95d18:automotive/src/main/kotlin/com/moah/hackathon/ui/concepts/droad/RoadMap3D.kt > <대상 경로>
```

| 필요해질 수 있는 것 | 16번 경로 |
|---|---|
| 상태기계 구조 참고 | `feature/journey/JourneyStateMachine.kt`(349줄), `JourneyPhase.kt`, `JourneyViewModel.kt`(`DemoControls` — Fake 일 때만 시연 버튼) |
| 화면 필터 패턴 | `feature/drive/DrivingDisplayState.kt` + `DrivingDisplayStateTest.kt` |
| 의사 3D 지도 | `ui/concepts/droad/{RoadProjection,RoadMap3D,RoadStyle}.kt` + `ui/RoadProjectionTest.kt` |
| UI 컨셉 계약·계측 | `ui/concepts/ConceptScreens.kt`, `androidTest/.../ConceptContractInstrumentation.kt` |
| 시연 대본·발표·영상 구조 | `docs/05_demo_script.md`, `docs/presentation/{01_deck_outline,02_video_shotlist}.md` |
| Codex 오더 형식 | `docs/handoffs/2026-09-18_codex_order_status.md` 등 13건 |
| 시드 데이터 형태 | `data/SeedCatalog.kt`(경로 polyline·`smoothed()` 사용법) |
