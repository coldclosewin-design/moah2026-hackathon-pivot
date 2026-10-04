# Codex 오더 — 라운드 18: 코스 과제 화면 — 주행 지도(Drive) · 코스 결과(Done·Report) · 주행 카드 (2026-10-04)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/10_full_scope.md, 이 발주서, docs/screenshots/lesson/README.md 를 읽어라.
브랜치: origin/main(코스 상태기계 PR 이후) 에서 새 워크트리 codex/ui-round18. PR 하나(scope ui).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지(시드 문구는 다듬어도 된다). docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라.
     시연 본편(후면 직각 주차)의 흐름·고정값·라벨 13개 불변. 주차·점검·지식 화면 캡처 불변.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → PASS · clashes 0(60/55·100/100·힌트 3종·배지 8),
          bash tools/course_flow.sh → PASS(장내기능 못함 70 불합격 → 잘함 100 합격). 캡처는 docs/screenshots/lesson/ 에 추가하고 README 표 갱신.
```

## 0. 왜

사용자(10/4) 결정: **일정·신호 제약으로 줄이지 말고 전부 구현, 없는 신호는 시뮬레이션**(`docs/design/10_full_scope.md`). Claude 가 코스 과제의 계약·채점·상태기계를 끝냈다(#141~#144):

- 주행 과제 5개(단순 전진 후 정지·좌회전 방향지시등·차선 변경·일반 도로 코스·회전교차로) + **장내기능 모의시험**이 READY. 도면 6장·시나리오 12벌(`data/TrackCourses.kt`·`CourseScenarios.kt`).
- 새 단계 **`LessonPhase.Drive`**(Maneuver 자리). 지금 화면은 Claude 가 컴파일용으로 넣은 **임시 `ui/lesson/DriveInterimScreen.kt`** — 이번 라운드에서 제대로 된 `DriveScreen` 으로 교체하고 임시 파일은 지운다.
- 회차 결과 `AttemptRecord.course: CourseResult?` — Done·Report 가 아직 이것을 그리지 않는다(지금은 왼쪽이 비고 멘트만).

도면·궤적이 어떻게 생겼는지는 `CoursePreviewDump` 단위 테스트가 `automotive/build/course-preview/<코스 id>.json` 으로 써 준다(도로·칸·정지선·신호등·구간·궤적·감점 위치).

## 1. 화면이 받는 값 (구조 변경 금지 — 읽기만)

| 어디 | 값 | 비고 |
|---|---|---|
| `LessonPhase.Drive` | `task` · `mode` · `attempt` · `snapshot`(속도·기어·지시등·`signal`·`emergency`·`trackX/Y/HeadingDeg`) · `course: TrackCourse` · `progress: CourseProgress` · `zoneLine` · `lastHint` · `askedDone` · `availability` · `locked` | **점수·감점 필드가 없다**(절대 규칙 10, 타입에서부터) |
| `CourseProgress` | `pose`(위치 m·방향 °) · `currentZoneId` · `nextZoneId` · `passedZoneIds` · `signal` · `emergency` · `positionMeasured` | 방향 0° = 도면 위(+y), 반시계 양수 |
| `TrackCourse` | `map: TrackMap`(폭·높이 m, `shapes`) · `zones`(id·title·kind·`area`) · `route`(기대 경로) · `isExam` | `MapShape` 8종: Road(꺾은선+폭+차로 수) · Ring · Bay(target) · StopLine · Crosswalk · Light · Ramp · Label |
| `AttemptRecord.course` | `CourseResult`: `passed`(시험만, 연습 null) · `disqualified` · `deductions`(reason·say·zoneTitle·**points**·at) · `zones`(visited·deductions·unmeasured) · `trail` · `score` · `positionMeasured` | 코스 회차는 `path` 가 비고 `verdict` 가 null |

## ① Drive 화면 — `DriveScreen`

| # | 영역 | 내용 | 규칙 |
|---|---|---|---|
| 1.1 | 왼쪽 53% Ink 패널 | **트랙 지도**: 도로(Paper 28%)·회전교차로·주차 칸(target = Periwinkle)·정지선·횡단보도·경사로·이름표. 기대 경로 옅게. **지금 구간** 면 강조(Periwinkle 25%), 지나간 구간은 아주 옅게 | 도식이라 주행 중에도 그린다. 점수·감점 표시 금지(지나간 구간에 ✓/✗ 금지) |
| 1.2 | 지도 위 차 | `VehicleSilhouette`(카드·Maneuver·Done 과 같은 모양)를 `pose` 위치·방향으로, 실제 크기(길이 4.5 m) 비율. 앞 유리·미러가 진행 방향 | 위치 미측정이면 차를 그리지 않고 "시험장 위치 미측정" |
| 1.3 | 신호등·돌발 | `Light` 도형은 `snapshot.signal` 색(빨강 = Signal, 초록 = Periwinkle, 꺼짐 = Muted). 돌발 경보 중 지도 위 Signal 띠 "돌발 상황" | 시뮬레이션 신호 — 아래 1.6 출처 |
| 1.4 | 오른쪽 47% | 위: `과제 · N회차`(왼쪽) · 속도(오른쪽, 큰 숫자 — 속도는 허용). 그 아래 Eyebrow "지금 ○○ · 다음 ○○". 가운데 Headline = `lastHint` → `zoneLine` → 자막 순 | 문장은 상태기계가 준다(숫자 없음) |
| 1.5 | 아래 | **정차 + 잠금 아님**일 때만 "다 됐어요"(Maneuver 의 `FinishButton` 과 같은 모양, `askedDone` 이면 맥동) | 잠금(> 5 km/h) 중 터치 타깃 0 — 버튼·시연 레일 모두 숨김 |
| 1.6 | 출처 한 줄 | `availability` 로 "시험장 위치·신호 · 시뮬레이션"(Hybrid 에서 일부 실신호면 그 표기) | 정직 표시 원칙 |
| 1.7 | 시험(평가 + `course.isExam`) | 오른쪽 위에 작은 "모의시험" 칩. 구간 방송 문장이 Headline 으로 | 감점 누계·점수 금지 |

확인 캡처(새): `lesson-drive-exam.png`(가속 구간 주행 중, 잠금) · `lesson-drive-exam-parking.png`(직각 주차 구간 정차, 버튼 보임) · `lesson-drive-road-red.png`(일반 도로 빨간불 정차) · `lesson-drive-round.png`(회전교차로).

## ② Done — 코스 회차 결과

| # | 영역 | 내용 | 규칙 |
|---|---|---|---|
| 2.1 | 왼쪽 38% | 지도 + **지나간 자리**(`trail`, 3 초 재생 — Done 궤적 재생과 같은 리듬) + 감점 위치 표식(작은 Signal 원) · 끝 차 실루엣 | 표식 옆에 숫자 금지 |
| 2.2 | 왼쪽 아래 판정 패널(주차의 `VerdictPanel` 자리) | 시험: **"합격" / "불합격" / "실격 사유 있음"** 한 줄 + 놓친 것 최대 3줄(`reason`, 구간 이름). 연습: "놓친 것" 목록 또는 "놓친 것 없음" | **점수·감점 숫자 금지**(운전자 화면) |
| 2.3 | 오른쪽 | 지금과 같다(Eyebrow·멘트·한 번 더·오늘은 여기까지) | — |

확인 캡처: `lesson-done-exam-bad.png`(불합격, 표식 셋) · `lesson-done-exam-good.png`(합격) · `lesson-done-left-bad.png`(연습, 놓친 것 둘).

## ③ Report — 코스 과제

| # | 페이지 | 내용 |
|---|---|---|
| 3.1 | 요약 | 주차의 판정 자리에 최고 회차의 구간 목록(구간 이름 + 지남/놓침/미측정 — 숫자 없음). 출처 문구: 주차 "주차 과정만 측정했어요." 대신 **"구간과 위치·신호등은 시험장 신호(시뮬레이션)로 측정했어요."** |
| 3.2 | 자세히 보기 | 회차별 **코스 점수·합격선·감점 표**(구간 · 사유 · 점수) — 숫자는 여기서만. 미측정 규칙은 "확인 못 함" 으로 |
| 3.3 | 진단서 | 코스 과제면 "장내기능 모의시험 · 합격/불합격" 줄 추가(점수 숫자 허용 — 진단서) |

확인 캡처: `lesson-report-exam.png` · `lesson-report-exam-details.png`.

## ④ Setup 시트 — 주행 카드

| # | 지금 | 바꿀 것 |
|---|---|---|
| 4.1 | 주행 카테고리 5장이 "준비 중"(흐림), 카드 그림은 주차용 | 6장 전부 READY(장내기능 모의시험 포함). 카드 그림 면 = **그 코스 도면의 축소판**(도로만, Ink/Periwinkle, 칸·정지선 정도) — 주차 카드의 실루엣처럼 "무엇을 하는지" 가 보이게 |
| 4.2 | — | 장내기능 모의시험 카드에 작은 "모의시험" 표식. 난이도 상 |
| 4.3 | 계측 `driving.size == 5` · `assertPlannedTask` · 닫힌 카테고리 색 검사 | 새 카탈로그에 맞게(주행 6 READY, 준비 중은 평행·사선 주차 둘) |

## 4. 불변·금지

| 규칙 | 검사 |
|---|---|
| 주행 중(잠금) 화면 터치 타깃 0 · 점수·감점 누계 0 | 계측 + 캡처 |
| 운전자 화면 문장·라벨 숫자 0(속도 표시 제외) — Done·Drive 에 점수 숫자 금지 | 계측 `allText()` 숫자 검사를 Drive·Done 에도 |
| 주차·점검·지식 캡처 불변, 본편 흐름 불변 | diff · emu_flow |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 토큰 0 | grep |
| `feature/`·`scoring/`·`data/` 구조 불변 | `git diff --stat` |

머지 뒤 Claude 가 다음 단계(평행·사선 주차 C1, 예약 → 모의시험 C2)를 이어 간다.
