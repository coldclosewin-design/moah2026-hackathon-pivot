# Codex 오더 — 라운드 19: 코스 화면 다듬기 셋 (2026-10-05)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/screenshots/lesson/README.md 의 라운드 18 절을 읽어라.
브랜치: origin/main(#154 이후) 에서 새 워크트리 codex/ui-round19. PR 하나(scope ui).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라.
     시연 본편(후면 직각 주차)의 흐름·고정값·라벨 13개 불변. 주차·점검·지식 화면 캡처 불변. 이것이 동결(10/7 오전) 전 마지막 화면 라운드다.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → PASS · clashes 0(60/55·100/100·힌트 3종·배지 8),
          adb install -r 뒤 bash tools/course_flow.sh → PASS(70 불합격 → 100 합격, 약 6분이라 백그라운드로). 바뀐 캡처는 docs/screenshots/lesson/ 에서 교체하고 README 표 갱신.
```

## 0. 왜

라운드 18(#152) 리뷰에서 머지는 했고, 작은 점 셋을 동결 전에 고친다. 사용자 승인(10/5).

## ① Drive — 신호등이 없는 구간의 "신호등 꺼짐" 줄

| 지금 | 바꿀 것 | 확인 |
|---|---|---|
| `DriveScreen` 오른쪽이 `snapshot.signal` 이 `OFF` 여도 "신호등 꺼짐"(Muted) 줄을 늘 그린다 — 장내 코스 대부분이 신호등 없는 구간이라 잡음 | `OFF` 이면 줄을 **그리지 않는다**(자리도 비우지 말고 아래가 올라와도 됨). `null`(신호 미측정)은 지금처럼 "신호등 미측정" 유지 — 정직 표시. 빨강·노랑·초록은 그대로 | `lesson-drive-exam.png`·`lesson-drive-exam-parking.png` 교체, `lesson-drive-road-red.png` 불변 |

## ② Report 자세히 보기 — 감점 없는 회차

| 지금 | 바꿀 것 | 확인 |
|---|---|---|
| 감점이 0건인 회차도 "구간 · 사유 · 감점" 표 머리만 그리고 비어 있다(`lesson-report-exam-details.png` 2회차) | 감점 0건이면 표 머리 대신 **"감점 없음"** 한 줄(32 sp, Muted). 미측정 규칙이 있으면 그 아래 "확인 못 함: ○○" 는 그대로 | `lesson-report-exam-details.png` 교체 — 2회차가 한 줄로 끝나 `돌아가기` 와 겹치지 않는지 |

## ③ 시트 주행 카드 — 차선 변경 그림

| 지금 | 바꿀 것 | 확인 |
|---|---|---|
| "차선 변경" 카드의 축소 도면이 세로선 하나라 "단순 전진 후 정지" 와 구별이 안 된다 | 차로가 둘이면(`MapShape.Road.lanes ≥ 2`) **도로 폭 두 줄 + 가운데 점선**을 그리고, 그 위에 **기대 경로(`course.route`)를 Periwinkle 선**으로 얹는다 — S 자가 보이게. 다른 카드도 기대 경로를 같은 방식으로 얹으면 더 좋다(선택, 단 단순 전진 후 정지는 정지선 표시) | `lesson-setup-sheet-driving.png`·`-driving-end.png` 교체 |

## 3. 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 터치 0 · 운전자 화면 숫자 0(속도 제외) | 계측 |
| 주차·점검·지식·리포트(주차) 캡처 불변 | diff |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 토큰 0 | grep |
| `feature/`·`scoring/`·`data/` 구조 불변 | `git diff --stat` |

머지 뒤 Claude 가 동결 태그를 만든다(10/7 오전).
