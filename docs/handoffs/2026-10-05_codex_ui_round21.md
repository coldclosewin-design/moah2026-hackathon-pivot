# Codex 오더 — 라운드 21: 시안 선택분 구현 (2026-10-05)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/round21-proposals/(index.html 과 각 페이지의 고른 시안), docs/screenshots/lesson/README.md 를 읽어라.
시작 조건: 라운드 20(#167) 머지됨 — origin/main 최신에서 새 워크트리 codex/ui-round21. PR 하나(scope ui). 양이 많으면 PR 둘(①~④ 설정·시트 / ⑤~⑧ 결과·주행)로 나눠도 된다 — 각각 완료 기준을 지킨다.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라.
     시연 본편의 흐름·고정값 불변. 새 색 토큰 0(시안이 쓴 다섯 토큰 + 투명도만), Bold 0, 그림자는 질감 B 값 재사용.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → PASS · clashes 0(60/55·100/100·힌트 3종·배지 8),
          adb install -r 뒤 bash tools/course_flow.sh → PASS(백그라운드). 바뀐 캡처 교체 + README 표(시안 페이지와 나란히 비교할 수 있게 캡처 이름을 적는다).
```

## 0. 사용자 선택 (10/5)

| # | 주제 | 선택 | 시안 페이지 |
|---|---|---|---|
| ① | 제휴 시험장 카드 | **B 지도** — 카드 위쪽 Ink 미니 트랙 지도, 선택은 빨간 테두리 + 경로 빨강 | `1-venue-cards.html` |
| ② | 과제 분류 메뉴(주차·주행·점검·지식) | **A 알약 트랙** — Lavender 트랙 위 고른 칸만 Periwinkle 알약, 제목과의 간격 넓힘 | `2-category-menu.html` |
| ③ | 돌아가기 | **B 면** — Lavender 채운 알약 + 질감 그림자, 누르면 그림자가 안쪽으로 | `3-back-button.html` |
| ④ | 출발 전 점검 왼쪽 카드 | **B 의 상단 진행 막대 + E 순서 점** — 위에 숫자 없는 칸 막대, 아래 세로 순서 점 타임라인(지금 할 일 한 줄만 질감 카드로 떠오름) | `4-checklist-cards.html` |
| ⑤ | 리포트 자세히 보기 | **A 나란히** — 회차 카드 두 장 사이 → 화살표, 변화량 칩 | `5-details.html` |
| ⑥ | 주차 결과 시뮬레이션 | **크기 B(왼쪽 50%, 판정 2×2) + 6b 의 B-1 칸 하나에 B-2 의 흐린 옆 줄만** — 목표 칸 = 양옆 줄 + 안쪽 끝 줄(입구 열림), 옆 칸 경계 = 아주 흐린 줄(20~30%), **주차장 면(옅은 사각형)은 넣지 않는다** | `6b-parking-result-minimal.html`(B-1·B-2) |
| ⑦ | 지식 테스트 정답 확인 | **E 판정 패널 + D 인용 카드** — 왼쪽 Ink 패널에 큰 ✓/✕ 와 "맞았어요/아쉬워요", 오른쪽 보기(배지)와 **해설은 큰따옴표 인용 카드**, 넓은 "다음 문제 →" 알약 | `7-quiz-answer.html` |
| ⑧ | 코스 지도 차 크기 | **×1.3** — 칸은 Claude 가 데이터에서 이미 ×1.3(#166) | `8-map-car-size.html` |
| ⑨ | 계측 플레이크 굳히기 | 라운드 19·20 리뷰에서 매번 다른 타이밍 검사가 떨어졌다(Claude 쪽 5회 중 2회 PASS): 차선 카드 픽셀(`assertLanePreview` "Two-lane thumbnail road edge"), 시험장 시트 196행 부근, 잠금 해제 직후 Done 멘트(라운드 20 브랜치 818행), 시트 다시 열기 직후 시작 알약 위치(246행). **애니메이션·재구성이 끝날 때까지 기다린 뒤 검사**(waitForIdle + 짧은 재시도)로 바꾼다. 검사 내용은 그대로 | 계측 |

## 1. 항목별 구현 메모

| # | 어디 | 할 것 · 주의 |
|---|---|---|
| ① | `VenueSheet.kt` | 미니 지도는 `TrackCourses.exam.map` 을 축소해 그린다(라운드 18 의 주행 카드 축소 도면 코드 재사용). 시험장 셋이 같은 도면이면 카드마다 기대 경로 색·각도만 달라도 된다. 거리·코스·"오늘 자리 있음" 글자 크기를 시안대로 키우고, 시간·코스가 아래로 내려가도 된다(사용자 허용). "예시입니다 — 실제 예약 연계 없음" 유지 |
| ② | `TaskSheet.kt` | 네 칸 같은 폭, 고른 칸 = Periwinkle 알약에 Paper 글자. **시트 숫자 0**(계측) 유지 |
| ③ | 공통 보조 버튼 | 과제 시트·시험장 시트·자세히 보기·진단서의 `돌아가기` 를 같은 부품으로. 주 알약(빨강)보다 한 단계 아래로 읽혀야 한다 — 크기는 주 알약보다 작게. 다른 글자 링크(오늘은 여기까지·그만하기·과제·모드 바꾸기)는 이번엔 그대로 |
| ④ | `ManeuverScreen` 점검 분기 | 순서 = 문 → 벨트 → 기어 P → 브레이크+시동 → 좌 지시등 → 우 지시등 → 비상등(`SeedCatalog.predriveGuide` 순서). 상태 문구 "미수행"(라운드 20 ⑤)·"미측정"·"잘못함(사실 문장)" 구분 유지. 지금 할 일 = 첫 미수행. 운전자 화면 숫자 0(점·막대 칸으로만) |
| ⑤ | `ReportScreen` 자세히 보기 | 주차·코스 둘 다. 코스 회차 카드는 코스 점수·합격선·감점 표(라운드 19 의 "감점 없음" 규칙 유지). 회차가 하나면 카드 한 장, 셋 이상이면 가로 스크롤 또는 마지막 둘만 + "이전 회차" |
| ⑥ | `DoneScreen`·`EstimatedPath` | 옆 칸 경계: 후면·전면·사선은 칸 축과 수직 방향으로 간격 약 3 m 에 양쪽 둘씩, 평행은 칸 축을 따라 앞뒤로 하나씩(시안 B-2 의 평행 그림). 사내 에뮬에서 흐린 줄이 안 보이면 30% 까지 올린다. 공통: 짙은 Ink 바탕, 왼쪽 50%, 판정 2×2, 차는 **지금 실루엣 모양 그대로**(지도 위라 밝게: 몸체 Lavender · 패널 Periwinkle · 유리 Ink), 궤적 후진 Lavender · 전진 보정 Periwinkle 점선 · 급정지 Signal 점, 캡션 "신호로 추정한 궤적이에요…" 유지, 칸 방향은 `task.parkingSpec.targetHeadingDeg` |
| ⑦ | `QuizScreen`·`QuizDoneScreen` | 왼쪽 패널 판정은 답한 뒤에만(답 전엔 지금처럼 진행 표시). "정답은 ○번" 같은 숫자 문장은 쓰지 않는다. 주 알약 폭 = 시작 알약(라운드 20 ⑧). 선택지 카드 질감 그림자 |
| ⑧ | `CourseMap.kt` | 차 크기 `VehicleSilhouetteGeometry.WIDTH/LENGTH × scale × TrackCourses.MAP_CAR_SCALE`(=1.3, data 에 이미 있음). 주행 Drive·코스 Done 지도 둘 다. 칸은 데이터가 이미 1.3배 |

## 2. 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 터치 0 · 운전자 화면 숫자 0(속도 제외) · 잠금 화면 평면 | 계측 |
| 시트 숫자 0 · 라벨 13개(시작·다 됐어요·한 번 더·오늘은 여기까지·다시 시작·그만하기·다음 문제·결과 보기·과제·모드 바꾸기·돌아가기·예약·취소·정차 후 답해 주세요) 문자열 불변 | 계측 · emu_flow |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 색 토큰 0 | grep |
| `feature/`·`scoring/`·`data/` 구조 불변 | `git diff --stat` |

머지 뒤 Claude 가 리뷰하고 태그 `inhouse-20261006-N` 을 만든다.
