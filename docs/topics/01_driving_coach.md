# ✅ 확정 주제: 운전 연수 어시스턴트 (가칭 DriveCoach) — 2026-09-25 피벗

## 한 줄

초보운전자가 혼자 도로에 나갈 때, **차량 신호로 운전 행위를 정량 측정**해 **주행 중에는 음성으로 즉시 코칭**하고, **세우고 내릴 때(속도<1 且 운전석 도어 열림) 능력 진단 리포트**를 펼치는 AAOS 앱. 동승자가 있어도 앱이 강사다.

## 왜 이 주제인가 (16번 Gift Drive 에서 바꾼 이유)

- 16번은 `_comparison.md` 의 *"심사에서 VSS 활용 비중은 양념 수준이면 충분"* 이라는 **가정** 위에 서 있었다. 이 주제는 차량 신호가 없으면 성립하지 않으므로 그 가정에 기대지 않는다.
- `00_hackathon_overview.md` 의 요구 *"운전자에게 편의/안전 관점의 서비스"* 에 정면으로 부합한다.
- 확장 서사가 자연스럽다: 장롱면허 재적응 → 면허갱신·보험사 제출용 진단(고령운전자 능력평가) → 휴무일 도로주행시험장 모의주행. **시연은 첫 페르소나 하나**, 나머지는 발표 슬라이드.

## 사용자·상황

| 우선순위 | 누구 | 무대 | 이번 시연 |
|---|---|---|---|
| 1 | 초보운전자 (면허 취득 직후·장롱면허) | **일반 도로 실주행 연수** ("집 → 마트" 같은 실생활 경로) | **○ 본편** |
| 2-1 | 면허갱신·보험사 제출용 진단 | 같은 측정, 리포트 수신자만 다름 | 리포트 화면에 "제출용 진단서" 레이아웃 한 장 |
| 2-2 | 휴무일 도로주행시험장 모의주행 | 정해진 코스 | 발표 확장 서사 |

혼자가 주. 동승자는 예의주시·검토만 한다.

## 세션 구조 (16번 상태기계 1:1 치환)

| 16번 `JourneyPhase` | `LessonPhase` | 내용 |
|---|---|---|
| `Mood` | **`Setup`** | 오늘의 코스·난이도 선택 (정차 중) |
| `Planning` | **`Briefing`** | 코스 안내 + "오늘 볼 항목" 음성 고지 |
| `Driving` | **`Driving`** | 속도>5 화면 잠금. 규칙 기반 즉시 음성 코칭. **점수 비표시** |
| `Reveal` | **`SegmentReview`** | 구간 종료(정차/구간 끝) → AI 구간 총평 + 다음 구간 조언 |
| `Arrival` | **`Report`** | 속도<1 且 운전석 도어 열림 → 진단 리포트 공개 |

**일반 도로 코스의 "채점 근거가 약하다"는 약점 해소** — 경로를 구간(Segment)으로 쪼개고 구간마다 기대 행동을 붙인다:

```
Segment(id, type, startProgress, endProgress, speedLimitKmh, expected: List<ExpectedAction>)
  type           = STRAIGHT | LEFT_TURN | RIGHT_TURN | LANE_CHANGE | INTERSECTION | PARKING | START | STOP
  ExpectedAction = SIGNAL_LEFT | SIGNAL_RIGHT | FULL_STOP | SLOW_DOWN | GEAR_P | HAZARD_ON | BELT_ON
```

## 채점 2층 구조

### A층 — 속도 파생 (`Vehicle.Speed` 하나로 무조건 동작)

| 지표 | 계산 | 감점 |
|---|---|---|
| 급가속 / 급제동 | a = dv/dt (스무딩 후) | 임계 초과 |
| 승차감 | jerk = da/dt RMS | 임계 초과 |
| 과속 | v vs `Segment.speedLimitKmh` | 초과 지속시간 비례 |
| 정속 유지 | 구간 내 v 표준편차 | 편차 과다 |
| 정지·출발 부드러움 | 0 km/h 전후 a 곡선 | 급출발·급정지 |

### B층 — 도로주행시험 채점표 (신호가 들어올 때만 활성화)

| 감점 항목 | 필요 신호 | 예상 VSS 경로 (COVESA 표준 기준) | 사내 확인 |
|---|---|---|---|
| 안전벨트 미착용 | 안전벨트 | `Vehicle.Cabin.Seat.Row1.DriverSide.IsBelted` | ⬜ |
| 기어 변속 미숙 · 출발 전 P→D | 기어 | `Vehicle.Powertrain.Transmission.CurrentGear` | ⬜ |
| 방향지시등 미점등 | 방향지시등 | `Vehicle.Body.Lights.DirectionIndicator.Left.IsSignaling`, `…Right.IsSignaling` | ⬜ |
| 진로변경 시 안전 미확인 | 조향각 (+방향지시등) | `Vehicle.Chassis.SteeringWheel.Angle` | ⬜ |
| 급제동(실측) | 브레이크 페달 | `Vehicle.Chassis.Brake.PedalPosition` | ⬜ |
| 비상등 미사용 | 비상등 | `Vehicle.Body.Lights.Hazard.IsSignaling` | ⬜ |
| 시동 절차 | IGN | `Vehicle.LowVoltageSystemState` | ⬜ |
| 주차 미숙 | 주차센서 | (경로 불확실) | ⬜ |
| 도어 | 운전석 도어 | `Vehicle.Cabin.Door.Row1.DriverSide.IsOpen` | ✅ (16번 확정) |

경로가 틀려도 예외 없이 조용히 무시되므로(`02_vss_api_contract.md` 함정 6) 신호마다 상태를 둔다:

```
enum SignalAvailability { LIVE, SIMULATED, MISSING }
```
`LIVE` 실신호 → 채점 / `SIMULATED` Fake 시나리오 → 채점 + "시뮬" 표시 / `MISSING` 값 없음 → 채점 제외 + "미측정" 표시.
리포트 배지 `실신호 N · 시뮬레이션 N · 미측정 N` — 무엇이 진짜인지 정직하게 구분하는 것이 **가점 요소**가 된다.

### Fake 신호 시나리오 재생기

`FakeVehiclePort` 의 500 ms 틱을 타임라인 재생기로 확장. 시나리오 2벌(잘한 주행·못한 주행), `-PdemoSpeed` 로 압축.

```
t=0.0  IGN on · t=3.0 belt on · t=5.0 gear D
t=12.0 signal left on            ← 기대 행동 이행
t=18.0 speed 0→42 in 3 s         ← A층 급가속
t=31.0 lane change, signal off   ← B층 방향지시등 미점등
```

## AI 경계

| 시점 | 담당 | 실패 시 |
|---|---|---|
| 감점 순간 (주행 중) | **규칙 → TTS `URGENT`** (지연 0) | — |
| 구간 종료 / 정차 | AI `CoachPort.reviewSegment` | 규칙 문장 폴백 |
| 세션 종료 | AI `CoachPort.summarize` | 규칙 문장 폴백 |

STT·주행 중 실시간 AI 발화는 넣지 않는다(`NEXT.md` 미루는 항목).

## 평가표 (`_template.md` 7항목)

| 기준 | 점수 | 근거 |
|---|---|---|
| VSS 신호 존재 | 3 | 확정은 속도·도어 2개. B층 9개는 COVESA 표준명 기준 추정, 전부 사내 미확인. **A층이 속도만으로 성립하므로 무너지지 않는다** |
| Fake 외부 시연 | 5 | 전 포트 Fake + 시나리오 재생기로 인터넷·실신호 없이 완결 |
| 3D 에뮬 임팩트 | 5 | Signal Simulator 로 속도·도어·(있으면) 방향지시등을 조작하면 그 즉시 코칭 음성이 나온다 — 신호 연동이 시연의 본체 |
| 1인·기간 규모 | 4 | 16번 플랫폼 계층 재사용(~2,300줄). 새로 쓰는 것은 채점·상태기계·화면 3~5장·시드 |
| AI 현실성 | 4 | 총평·조언에 자연스럽게 필요. 규칙 폴백이 있어 사내 인증 실패에도 시연 무사 |
| 편의/안전 설득력 | 5 | "운전자에게 편의/안전 관점의 서비스"에 정면 부합. 도로주행시험 채점표라는 실재 근거로 "왜 그 점수인가"에 답한다 |
| 사내 머지 리스크 | 5 | VehiclePort 외 플랫폼 의존 없음. compileOnly 한 줄 + 플래그 |

**합계: 31/35** (16번 Gift Drive 30, 03 주차 안내 30)

## 시연 시나리오 (약 90초, 시나리오 "못한 주행")

1. 정차 중 `Setup`: 코스 "집 → ○○마트 (4.2 km)", 난이도 2 선택 → `Briefing` 음성 "오늘은 방향지시등과 부드러운 출발을 봅니다."
2. 출발 → 속도 6 km/h 에서 화면 잠금(터치 타깃 0개). 자막만.
3. 급가속 → 즉시 "지금 가속이 급했어요. 앞차와의 간격을 보며 천천히." (규칙, URGENT)
4. 차선 변경 구간에서 방향지시등 없음 → "차선을 바꿀 때는 3초 전에 방향지시등." (B층, 시뮬 신호)
5. 구간 끝 정차 → `SegmentReview`: AI "첫 구간, 급가속 1회·방향지시등 1회 놓쳤어요. 다음은 좌회전이 두 번 …"
6. 마지막 구간 → 정차 → **운전석 도어 열림** → `Report`: 82점 / 항목별 감점 / 실신호 2 · 시뮬 6 · 미측정 1 배지 / AI 총평 + 다음 과제 / "제출용 진단서" 탭.

## 사내에서 확인할 것

- B층 경로 9개 실재 여부(pageId 1323873443). 없으면 `MISSING` 으로 자동 처리되지만, **이름이 비슷하게 있으면 상수만 바꿔 살릴 수 있다.**
- Signal Simulator 에서 조작 가능한 신호 범위(속도·도어 외에 기어·방향지시등이 되는가).
- TTS 엔진·한국어 음성, 디스플레이 물리 크기(`MOAH/DesignScale`), Cloud Copilot 인증.
