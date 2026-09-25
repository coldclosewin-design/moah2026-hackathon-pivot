# NEXT — 새 세션은 여기서 시작

새 Claude Code 세션이 이 파일 하나로 이어받을 수 있게 쓴 인계 문서. **작업을 마칠 때마다 이 파일을 갱신한다**(끝난 것은 지우고, 새로 생긴 것은 추가). 과거의 경위는 `docs/journal/`, 규칙은 `AGENTS.md`·`CLAUDE.md`, **제품 정의는 `docs/topics/01_driving_coach.md` v2**.

마지막 갱신: 2026-09-26 · 제품 정의 v2 확정, Step 3 착수 직전 · 열린 Codex 오더 없음 · 마감 2026-10-07

## 1. 지금 되는 것 (한 문단)

`16_hackathon`(Gift Drive) 에서 플랫폼 계층만 물려받은 **빈 껍데기가 빌드·단위테스트(21개) 통과** 상태다. 앱을 켜면 16번 초기의 Dashboard(속도 큰 글자, 운전석 도어·ABS 카드, "Toggle driver door" 왕복, `source=FAKE` 배지)가 뜬다 — `VehiclePort` 가 살아 있음을 눈으로 확인하는 용도이고 연수 세션 화면이 들어오면 `MainActivity` 에서 교체한다(9/25 에뮬에서 속도 갱신·도어 토글 확인). 살아 있는 것: `vehicle/`(VehiclePort·Fake·Real·Factory·VssValues), `ports/TtsPort`(자막 동기·청크 분할·URGENT 큐·kob 음성), `ports/{LocationPort,GpsLocationPort}`, `ports/Route`(아직 Gift Drive 의 `hidden` 개념 잔존), `ui/concepts/DesignScale`, `ui/CoachStyle`, `tools/` 3종(아직 Gift Drive 흐름), 문서 골격 전부.

**제품 정의 v2 (9/26)**: 시연 본편은 **후면 직각 주차 과제**. `Setup(대화·제안) → Briefing → Maneuver(도식) → Done("다 됐어요") → Report(도어 열림)`. 채점은 과정만. 모드 가이드→힌트→평가→지식테스트. 상세는 `topics/01_driving_coach.md`.

## 2. 남은 일 (의존 순서. 날짜 배정이 아니다)

### Step 3 — 신호 계층 + 채점 (Claude)
| # | 일 | 메모 |
|---|---|---|
| 3a | `vss-stub/.../VssConstants.java` 에 B층 상수 추가 | 조향각·기어·주차센서·벨트·IGN·방향지시등 L/R·브레이크·비상등 (`01_driving_coach.md` §5). COVESA 표준명 기준, 전부 사내 미확인 → `INTEGRATION.md` B절 |
| 3b | `vehicle/SignalAvailability.kt` — `LIVE / SIMULATED / MISSING` + 레지스트리 | 포트가 Fake 면 `SIMULATED`, Real 이면 값이 온 키만 `LIVE`, 일정 시간 없으면 `MISSING`. 리포트 배지의 근거 |
| 3c | `FakeVehiclePort` 시나리오 재생기 | `Scenario(steps: [(초, 경로→값)])` 를 `DEMO_SPEED_FACTOR` 로 압축 재생. **주차 2벌**(잘한 40 s / 못한 90 s, §4.4). 기존 sin 속도는 "도로" 시나리오로 남김 |
| 3d | `scoring/` A층 — `MotionSegmenter`(속도열 → 이동 구간 수·총 시간·정지 대기), `HarshEventDetector`(급출발·급정지) | 속도만으로 성립. JVM 테스트로 합성 속도열 검출 확인 |
| 3e | `scoring/` B층 — `SteeringReversalCounter`, `GearShiftCounter`, `ProximityMonitor`, `PreDriveChecklist` | 입력 신호가 `MISSING` 이면 결과가 `null`(미측정). 테스트로 강제 |
| 3f | `scoring/ParkingScore` — 숙련 축 / 안전 축 / 배지(실신호·시뮬·미측정 수) / "지난번보다" 비교 | 점수 구간 정의는 시드에 |

### Step 4 — 상태기계 + 시드 (Claude)
| # | 일 | 메모 |
|---|---|---|
| 4a | `feature/lesson/LessonPhase.kt` — `Setup · Briefing · Maneuver · Done · Report` | `Maneuver` 는 저속 화면 허용, `Driving`(도로 과제)은 잠금. 둘 다 같은 기계 |
| 4b | `LessonStateMachine.kt`, `LessonViewModel.kt` | `StateFlow<LessonPhase>`, 포트 주입, Log 외 안드로이드 의존 없음. `LOCK 5 km/h`, `STOP 1 km/h`. 세션 종료 = 버튼 또는 운전석 도어 열림. Fake 일 때만 `DemoControls`(시나리오 선택·재생·도어) |
| 4c | 가이드 단계 엔진 `GuideRunner` | 단계 = (대사, 확인 조건). 신호로 확인 → 다음. `MISSING` 이면 확인 없이 읽고 리포트에 남김 (§4.3) |
| 4d | 규칙 힌트 `HintRules` → `TtsPort.speak(…, URGENT)` | 너무 일찍 중립·근접·급조작. 같은 힌트 쿨다운 |
| 4e | `data/SeedCatalog.kt` — 과제 카탈로그(§3.1), 프로필 질문 5, **멘트 풀**(점수 구간 × 프로필 × 이력, 반복 금지), 지식 문항, 시험장 예약 카드 1 | 문구 **내용**은 Codex 가 다듬는다 |
| 4f | `ManeuverDisplayState` / `DrivingDisplayState` 매퍼 | 도식에 필요한 것만(조향각·기어·센서 거리·구간 수). **점수·감점 누계는 주지 않는다**(AGENTS 규칙 10). 테스트로 강제 |
| 4g | `Profile`(진술·관측) + `ProgressStore`(세션 누적, 인메모리 → 나중에 파일) | "지난번보다" 와 자동 제안의 근거 |

### Step 5 — 화면 (Codex 발주, `docs/handoffs/`)
`Setup`(대화형 설정·앱 제안·과제/모드 선택·예약 카드) / `Briefing` / **`Maneuver`**(위에서 본 차 도식: 조향각·기어·센서, 터치 없음) / `Done`(회차 요약·"다 됐어요") / `Report`(숙련 축·안전 축·배지·AI 총평·다음 과제 / **진단서 탭**: 공유 범위 3단계 + 예상 혜택, "실제 전송 없음" 문구). 시연 조작은 Fake 일 때만.

### Step 6 — AI (Claude)
`ports/CoachPort.kt` + `FakeCoachPort` — `phrase(score, profile, history)`(멘트 변주, 실패 시 시드 풀), `summarize(report)`, `suggestNext(profile)`. **예외를 던지지 않는다** — 테스트로 강제. Cloud Copilot 구현체는 사내 인증 확인 후.

### Step 7 — 시연 파이프라인 (Claude)
`tools/emu_flow.sh` 를 주차 흐름(탭 라벨·`MOAH/LessonStateMachine`·시나리오 전환)에 맞게 수정 → `result: PASS`. `docs/05_demo_script.md` 신규(§4.5 기준, 분단위 타임라인·시간 조절·사고 대응표). 계측 계약을 만들 때 `build.gradle.kts` 의 `testInstrumentationRunner` 주석을 먼저 읽는다.

### Step 8 — 제출
발표 덱(16번 `docs/presentation/01_deck_outline.md` 슬라이드 7~11 서사 재사용), 시연 영상 **B안(외부 에뮬)을 먼저**, 사내에서 Bitbucket·MarketUploader·A안(3D 에뮬) 녹화.

### 사용자 결정·행동이 필요한 것
| # | 일 | 상태 |
|---|---|---|
| U1 | 제품명 | 가칭 DriveCoach. `res/values/strings.xml` 한 곳 |
| U2 | 사내 출근 일정 | 제출물 3종이 사내 전용. 방침: 외부 개발 후 별도 절차. 가능하면 빈 껍데기로 clone→빌드→설치 경로 먼저 |
| U3 | 주차장·코스 시드 좌표 | 없으면 서초 기준 임의, `INTEGRATION.md` 가정 표기 |

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
- **에뮬**: `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd CSTDe_API_34 -no-snapshot-load` 백그라운드. 먼저 `adb get-state`. 앱은 user 10. 함정은 `tools/README.md`. 스크린샷은 `screencap -d 4619827259835644672`, 탭은 그냥 `input tap`(`-d` 는 실패).
- **셸 함정**: 큰 heredoc + 한국어 → Bash 파싱 실패(9/25 재현). 긴 파일은 Write 도구, 커밋 메시지는 `-m` 여러 개 또는 `-F 파일`.
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
| 빌드 플래그 4개 | `automotive/build.gradle.kts` — `USE_FAKE_VSS`, `USE_FAKE_LOCATION`, `TTS_VOICE`, `DEMO_SPEED_FACTOR` |
| 도구 | `tools/` — **전부 Gift Drive 흐름. Step 7 에서 수정** |
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
