# 라운드 31b · B 모드 롤 / O1 주행 시트

2026-10-09 · `codex/ui-round31b` · 시작 기준 `ded59a5` (`origin/main`, 31a #268의 `9eeca8a` 포함).
[발주서](../../../handoffs/2026-10-09_codex_ui_round31b.md)의 두 항목을 구현했다. 모든 설치·계측·시연은 `emulator-5556`에서 실행한다.

## 시안 칸 | 앱 캡처

왼쪽은 `docs/design/round31-proposals/`의 원본 PNG를 자른 시안 칸, 오른쪽은 앱 캡처다. 앱 내부 요소를 옮기거나 합성하지 않았으며, 시스템 바만 제외한 `(0,76)–(2560,1344)` 영역을 `853×423`으로 축소했다. 시안 HTML의 로컬 URL은 브라우저 정책으로 열리지 않아, 발주서가 허용한 같은 이름의 PNG와 HTML 소스를 확인했다.

| 화면 | 시안 | 비교 | 앱 원본 |
|---|---|---|---|
| 홈 모드 | `1-mode-roller` B | [비교](compare-31b-mode.png) | [힌트](lesson-31b-mode-hint.png) · [평가](lesson-31b-mode-evaluate.png) · [가이드](lesson-31b-mode-guide.png) |
| 예약 홈 모드 | 같은 B 부품 | [비교](compare-31b-mode-booking.png) | [예약 홈](lesson-31b-mode-booking.png) |
| 도로 주행 | `2-drive-map` O1 | [비교](compare-31b-drive.png) | [주행](lesson-31b-drive-moving.png) |
| 돌발 | O1 첫 변형 | [비교](compare-31b-drive-emergency.png) | [돌발](lesson-31b-drive-emergency.png) |
| 정차 | O1 둘째 변형 | [비교](compare-31b-drive-stopped.png) | [정차](lesson-31b-drive-stopped.png) |
| 신호등 미측정 | O1 셋째 변형 | [비교](compare-31b-drive-signal-missing.png) | [미측정](lesson-31b-drive-signal-missing.png) |

원본 시안과 발주서가 다른 두 곳은 발주서를 따랐다. 모드의 오른쪽 기호는 과제의 아래쪽 기호와 같은 글리프·크기·색·간격이며 방향만 회전했다. O1 원본의 시뮬레이션/실신호 집계 줄은 앱에서 제거하고, 상태인 `신호등 미측정`만 시트에 남겼다.

주행 캡처는 실제 시드 시나리오의 위치·구간 데이터를 사용한 고정 화면 계측이다. 비교를 위해 속도·돌발·정차·미측정 상태만 바꿨다. 코스 좌표·지명·차 크기는 앱의 실제 데이터이므로 시안의 예시 도면과 다르다. 지도는 왼쪽 영역 안에서 종횡비를 유지한다. 과제 카드의 지도 축소판은 변경하지 않았다.

다른 종횡비도 같은 왼쪽 영역에 맞춘다: [장내기능 코스](lesson-31b-drive-course-exam-track.png) · [회전교차로](lesson-31b-drive-course-roundabout.png).

## 움직임

- [시안·앱 확대 비교](compare-31b-mode-motion.png): 동일한 `0 / 120 / 240 / 360 ms`.
- [모드 가로 롤 프레임 띠](lesson-31b-mode-roll-strip.png): 프로덕션 `AnimatedContent`를 명시적인 Compose 프레임 시계로 구동했다. 지금 낱말이 왼쪽으로, 다음 낱말이 오른쪽에서 이동한다. `360 ms FastOutSlowIn`, 슬롯·뒤 문장의 좌표는 모든 프레임에서 같다.
- [설명 교차 전환 프레임 띠](lesson-31b-mode-description-strip.png): `0 / 360 / 460 / 560 ms`. 낱말이 멈춘 다음 설명을 `200 ms Crossfade`로 바꾼다.

예약 홈도 같은 슬롯을 사용한다. 탭 순서는 가이드 → 힌트 → 평가 → 가이드이며, 현재 모델의 `task.supports`로 허용 모드만 순환한다. 지식 전용 과제는 `지식 테스트`를 유지한다. 과제의 `과제·모드 바꾸기`와 모드의 `모드 바꾸기` 접근성 이름은 유지하며, 모드에는 현재 값의 `stateDescription`이 있다. 과제 시트는 기존처럼 과제 카드가 펼쳐진 상태로 열려 시연 도구의 과제·모드·시작 글자 선택을 유지한다.

오른쪽 시트는 점검 화면과 동일한 `SheetCard`, 너비 `680 dp`, 바깥 여백 `72 dp`, 내부 여백 `52 dp`를 사용한다. 속도 숫자는 `72 sp`, 단위는 `32 sp`다. 주행에서는 카드 등장·장식 애니메이션을 끈다. 돌발이면 시트만 Signal로 바뀌고, 정차 때만 시트 하단에 `다 됐어요`가 나온다. 미측정 신호등은 지도에서 점선 빈 원으로 그린다.

## 검증

| 검사 | 결과 | 증거 |
|---|---|---|
| 빌드·단위·계측 APK | 통과 · JVM 368개, 실패·오류·건너뜀 0 | [빌드](build.txt) · [APK 지문](unit-and-apk.txt) |
| 전체 lesson_shots 1회차 | Lesson contract passed · 10/09 02:20 KST | [계약 1](contract-1.txt) |
| 전체 lesson_shots 2회차 | Lesson contract passed · 10/09 02:31 KST | [계약 2](contract-2.txt) |
| 전체 lesson_shots 3회차 | Lesson contract passed · 10/09 02:43 KST | [계약 3](contract-3.txt) |
| 잠금 계약 단독 3회 | Locks contract passed, 같은 검증 유지 | [1회](locks-1.txt) · [2회](locks-2.txt) · [3회](locks-3.txt) |
| RESERVE=1 emu_flow | PASS · clashes 0 · 예약 → 주차 두 회차 → 리포트 | [로그](emu-flow.txt) |
| 최종 APK 재설치 후 course_flow | PASS · 70점 불합격/감점 3 → 100점 합격/감점 0 | [재설치](reinstall.txt) · [로그](course-flow.txt) |

동일 최종 앱·계측 APK로 전체 화면 계약을 **세 번 연속 통과**했다. 비교판과 원본은 세 번째 전체 실행의 캡처를 사용했다. 31b 보류 항목은 없다.

시연 증거: [예약 홈](flow-rear-10c_setup_reserved.png) · [주차 첫 결과](flow-rear-14_done_1.png) · [주차 둘째 결과](flow-rear-16_done_2.png) · [주차 리포트](flow-rear-17_report.png) · [코스 돌발](flow-course-24_drive_emergency.png) · [코스 정차](flow-course-25_drive_finish.png) · [코스 첫 결과](flow-course-26_done_1.png) · [코스 둘째 결과](flow-course-27_done_2.png) · [코스 리포트](flow-course-28_report.png).

[실제 시드 주행의 흰 차와 장내기능 지도](lesson-31b-drive-exam-moving.png)도 왼쪽 영역 안에서 표시된다.

전체 계약의 기존 `Real done lock` 검사에서 한 번 잠금 전환 실패가 나왔다([실패 로그](diagnostic-lock-before.txt)). 준비 코드가 시나리오를 실행한 뒤 `holdSpeed(12f)`를 호출해 첫 정차 스텝과 경합할 수 있었다. 속도를 먼저 고정하고 시나리오를 시작하도록 계측의 두 호출 순서를 바꿨다. 잠금 문구·터치 액션 0·숫자 0·Real 관리자 동작 없음·시연 탈출의 검증은 그대로다. 해당 항목을 단독 실행하는 `locksOnly` 인자로 재검증한 뒤 전체 연속 검증을 수행했다.

실행 환경은 Windows PowerShell·JDK 17·Git Bash다. `ANDROID_SERIAL=emulator-5556`, `ADB_TIMEOUT=1200`을 사용했다. 기존 `emu_flow`·`course_flow` 시작 검사에 남은 `-s emulator-5554 get-state`는 로컬 `ADB` 래퍼가 5556으로 연결한다. 모든 실제 호출은 `adb -s emulator-5556`으로 고정하며 다른 장치 동작은 거부한다. 저장소 도구 파일과 검증 로직은 변경하지 않았다.

차량·포트·채점·상태기계·시드 구조·Gradle·도구·NEXT는 변경하지 않았다. 로고는 31c 범위다.
