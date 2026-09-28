# 오더 — 제휴 시험장 예약 시스템 (2026-09-28) · 결정 D3 = (나)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/topics/01_driving_coach.md §3.3, docs/design/05_round7_feedback.md §2.2, 라운드 6 발주서(시트 두 층·주 버튼 규칙), 이 발주서를 읽어라.
브랜치: codex/reservation 를 origin/main 에서 새로 만들어 작업(§1 Claude 선행 PR 머지 뒤 — NEXT Step 13 에 표시).
작업: 시트 아래 카드 한 장이던 "제휴 시험장" 을 흐름으로 — ① 시험장 목록 → ② 시간대·코스 → ③ 예약 확인(취소) → ④ Setup 첫 화면 배지 + 그 코스의 과제 제안. 실제 연계는 없다("예시").
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(원본 — 시트 첫 렌더 힌트·시작 불변),
          docs/screenshots/lesson/ 에 lesson-venues.png · lesson-venue-slots.png · lesson-reservation.png · lesson-setup-reserved.png 추가, gh pr create.
```

## 0. 왜 · 왜 지금

사용자(라운드 6·동승자 영상 피드백): "주행장 예약 시스템 콘텐츠가 없다 — 디벨롭·기획 필요." 결정 D3(9/28): 범위 **(나)** — 예약 흐름 ①~③ + Setup 배지·과제 제안 ④. 리포트·혜택 연동(⑤)은 진단서 예시와 겹쳐 뺀다.

제품 축 §3.3 "장소"(실도로 / 편안한 주차공간 / 제휴 시험장) 중 셋째가 카드 한 장으로만 있었다. 실제 예약 API 는 없고 앞으로도 시연 범위 밖 — 화면에 "예시입니다 — 실제 예약 연계 없음" 을 진단서와 같은 방식으로 쓴다.

## 1. Claude 선행 (모델·시드·상태기계·제안) — ✅ (`claude/reservation-model`, 9/28)

| # | 무엇 | 어디 |
|---|---|---|
| A1 | 모델: `Venue(id, name, area, distanceKm: Float?, courses: List<Course>, slots: List<Slot>)`, `Course(id, title, taskIds: List<String>)`, `Slot(id, start: String, end: String, available: Boolean)`, `Reservation(venueId, slotId, courseId, madeAtMillis)`. `ReservationCard` 는 **제거**(예약 확인 카드는 `Reservation` + `Venue` 로 그린다) | `feature/lesson/LessonModels.kt` |
| A2 | 시드: 시험장 3(서초 · 강남 · 분당 — U3 미결이라 거리는 예시값), 코스 3(주차 3종 = 후면 직각·평행·전면 직각 / 도로 A = 단순 전진·좌회전 지시등 / 도로 B = 차선 변경·회전교차로), 시간대 오늘 3(하나는 `available = false`). **문구·지명은 Codex 가 다듬는다** | `data/SeedCatalog.kt` |
| A3 | `ProgressStore.reservation: Reservation?`. 상태기계 `reserve(venueId, slotId, courseId)`·`cancelReservation()` — **Setup 에서만**, 로그 `reservation: …`. `LessonPhase.Setup` 에 `venues: List<Venue>`, `reservation: Reservation?`(`ReservationCard?` 대체). `LessonViewModel` 진입점 2 | `LessonStateMachine.kt`, `LessonPhase.kt`, `ProgressStore.kt`, `LessonViewModel.kt` |
| A4 | `ModeAdvisor.suggestTask(profile, tasks, reservation)` — 예약이 있으면 그 코스의 **READY 과제**를 먼저(없으면 기존 순서). 제안 이유 문장에 "예약한 코스" 언급(숫자 없음) | `ProgressStore.kt` |
| A5 | 테스트: 예약 → Setup 제안이 코스 과제 / 취소 → 원래대로 / Setup 밖에서 무시 / 시드 코스의 taskId 가 카탈로그에 존재 | `LessonStateMachineTest`, `SeedCatalogTest` |
| A6 | `emu_flow.sh`: 변경 없음(시트 첫 렌더 불변). 선택 단계로 "예약 → Setup 배지" 를 넣을지는 Codex 화면 뒤 판단 | — |

구현 메모(9/28, Codex 가 알아야 할 것 — A1·A3 에서 두 가지가 발주서와 다르다):
- **`ReservationCard` 는 아직 남겨 두었다**(`ui/TaskSheet.kt`·`SetupScreen.kt`·`LessonRoute.kt`·계측이 쓰고 있어 내가 지우면 Codex 영역이 깨진다). `LessonPhase.Setup.reservation: ReservationCard?` 는 그대로 있되 **예약이 있으면 그 예약을 카드 모양으로**(`Reservation.toCard(venues)`), 없으면 시드 예시를 준다. 새 필드는 **`Setup.venues: List<Venue>`** 와 **`Setup.booking: Reservation?`**(발주서의 `reservation: Reservation?` 대신 — 이름 충돌 회피). 화면을 `venues`/`booking` 으로 옮긴 뒤 **`ReservationCard`·`SeedCatalog.reservation`·`Setup.reservation`·상태기계 생성자 인자 `reservation` 삭제는 Codex PR 에서 함께 해도 된다**(소유 예외 허용, 순수 삭제 + 테스트 `assertNotNull(setup.reservation)` 한 줄 제거). 그때 `booking` → `reservation` 개명은 하지 않는다(그대로 `booking`).
- 시드 id: 시험장 `venue-seocho`·`venue-gangnam`·`venue-bundang`, 시간대 `slot-14`·`slot-16`·`slot-18`(서초는 16시, 강남은 14시, 분당은 18시가 자리 없음), 코스 `SeedCatalog.COURSE_PARKING`(주차 3종: 후면 직각·평행·전면 직각)·`COURSE_ROAD_A`(단순 전진·좌회전 지시등)·`COURSE_ROAD_B`(차선 변경·회전교차로). 강남은 주차·도로 A, 분당은 주차·도로 B. `Slot.label` = `14:00–15:00`. 이름·지역·거리는 예시 — 다듬어도 된다(테스트는 개수·id·`available` 만 본다).
- 진입점: `vm.reserve(venueId, slotId, courseId)` / `vm.cancelReservation()` — Setup 밖·모르는 id·`available=false` 는 조용히 무시(로그 `reservation: …`). 성공하면 Setup 이 다시 그려지고 **제안 과제가 그 코스의 첫 READY**(주차 3종 → 후면 직각), 이유 문장 앞에 `ModeAdvisor.RESERVED_REASON`("예약한 코스의 과제부터 해요.")이 붙는다. 도로 A/B 는 전부 계획 과제라 예약해도 제안은 원래 규칙. `reset` 은 예약을 지우지 않는다(응원과 같은 규칙).
- 문장: `Reservation.EXAMPLE_NOTE` = "예시입니다 — 실제 예약 연계 없음" — 화면·확인 카드가 이 상수를 그대로 쓰면 된다.
- 단위 테스트 166 → 168(`LessonStateMachineTest` 예약 흐름 · `SeedCatalogTest` 시드 · `ProgressStoreTest` 제안). `emu_flow` PASS(시트 첫 렌더 불변).

## 2. Codex — 화면 (정차 중 선택 화면 — 시간·거리 숫자 허용, 점수·횟수 숫자는 없음)

### ① 시험장 목록 (시트 하단 `제휴 시험장` → 새 층)
- 시트의 `제휴 시험장 예시` 접힘 행을 **`제휴 시험장` 텍스트 액션**으로 → 시트 본문이 시험장 층으로 바뀐다(카테고리 층과 같은 자리, 뒤로는 `돌아가기`). 상단 40 sp `Periwinkle` "예시입니다 — 실제 예약 연계 없음".
- 카드 3(가로, 라운드 6 U자 칸 아닌 **사각 카드**, 높이 220): 이름(40 sp) · 지역·거리(32 sp `Muted`, 예 `서초 · 3 km`) · 코스 종류(`주차 3종 · 도로 A`) · 오늘 빈 시간대 수는 숫자 대신 `오늘 자리 있음/없음`. 예약된 시험장은 `Periwinkle` 바탕 + `예약됨`.

### ② 시간대·코스 (카드 선택)
- 카드 아래 두 줄: 눈썹 `시간` + 칩 3(`14:00–15:00` 형식, `available=false` 는 회색·클릭 없음) / 눈썹 `코스` + 칩(`SelectionChip`, 코스 제목). 하단 `돌아가기` · **`예약`**(주 버튼 규칙 140 dp × ≥ 720 dp, 시간·코스 둘 다 골라야 보임).

### ③ 예약 확인 · 취소
- `예약` → `vm.reserve(venueId, slotId, courseId)` → 시트가 **확인 카드**로: `서초 · 오늘 14:00–15:00 · 주차 3종` + "예시입니다 — 실제 예약 연계 없음" + `취소`(`TextAction` → `vm.cancelReservation()`) + `돌아가기`. 이미 예약이 있으면 ①에서 그 카드가 `예약됨` 이고 누르면 확인 카드.

### ④ Setup 첫 화면 배지 · 과제 제안
- 프로필 눈썹 **위**에 32 sp `Periwinkle` `예약 · 서초 14:00 · 주차 3종`(예약 없으면 자리 없음 — 동승자 응원 눈썹과 같은 규칙). 제안 과제·이유는 상태기계가 준다(A4) — 화면은 그대로 그린다.
- 시트를 열면 제안 과제의 카테고리가 펼쳐진 채(라운드 6 불변).

## 3. 불변

| 규칙 | 검사 |
|---|---|
| 시트 첫 렌더에 모드 칩·`시작`(→ `emu_flow` 원본 통과) | emu_flow + lesson_shots |
| 시험장 층: 카드 3, `예시입니다 — 실제 예약 연계 없음`, 시간 칩 중 회색 1 클릭 없음, 시간·코스 둘 다 골라야 `예약` | lesson_shots(추가) |
| `예약` → `reserve` 콜백 1회(인자 확인) → 확인 카드 → `취소` → `cancelReservation` 1회 → 목록 | lesson_shots(추가) |
| Setup 배지: 예약 있으면 `예약 · …`, 없으면 없음(자리도 없음) | lesson_shots(추가) |
| 점수·횟수 숫자 없음(시간·거리는 허용), 주 버튼·알약·잠금 계약 | lesson_shots(기존 + 정규식 조정: `\d{2}:\d{2}`·`\d+ km` 는 허용 목록) |
| `ui/` 에 `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 에만, 새 문자열 리소스 ≤ 2(`예약`·`취소`) | grep |

## 4. 시연·문서

- 대본 0:10: Setup 에서 "예약 · 서초 14:00" 배지가 보이는 상태로 시작(녹화 전에 예약해 둔다) — 본편 흐름 불변. `05_demo_script.md` 준비 절에 한 줄.
- 덱 12장 확장 항목 "제휴 시험장 예약" 을 "있는 것" 으로(캡처 `lesson-reservation.png`).

## 5. 안 하는 것

실제 예약 API·결제·알림, 지도, 리포트·혜택 연동(⑤ — 진단서 예시와 겹침), 시험장 위치 기반 자동 감지.
