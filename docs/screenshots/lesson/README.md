# 라운드 27a · Onyx 갤러리 화면 캡처

2026-10-08. 기준 `5540d85`, 브랜치 `codex/ui-round27a`. [발주서](../../handoffs/2026-10-08_codex_ui_round27a.md)의 01 · H8 · P2 · A1 · B1 · E1을 적용했다. 모든 실행은 `emulator-5556`에서 진행했다.

| 항목 | 실제 앱 캡처 | 적용 내용 |
|---|---|---|
| Onyx / P2 | [과제 선택](lesson-setup-sheet.png) · [예약](lesson-venue-slots-ready.png) · [프로필](lesson-profile-sheet.png) | White 벽, Onyx 글자·선택·주 행동, Graphite 링크, F2F2F2 면, Platinum 선. 입체 하이라이트·그라데이션 제거, 선택 알약과 주 행동에 얕은 그림자 한 겹 |
| H8 홈 | [홈](lesson-setup.png) · [첫 실행](lesson-setup-first.png) · [긴 과제](lesson-setup-long-task.png) · [모드](lesson-setup-mode-popup.png) · [예약 홈](lesson-setup-booking-mock.png) | 차 포스터 없는 흰 벽, 작은 워드마크, 오른쪽 위 프로필·코치, 큰 밑줄 제목, 오른쪽 아래 ↗ 시작. 처음에는 점선·흐림·비활성. 시작과 코치 터치 높이 112 dp 이상 |
| H8 움직임 | [0 / 93 / 187 / 280 ms](lesson-home-h8-strip.png) | 오른쪽 원이 왼쪽으로 번지면서 글자 반전, ↗가 →로 회전. 실제 Compose 프레임 시계와 터치 누름으로 캡처 |
| A1 주 행동 | [0 / 120 / 240 / 360 ms](lesson-fill-button-strip.png) | 공용 FillButton. 왼쪽 화살표 원에서 깊은 무채색 면으로 채움, 누름 360 ms (.2,.8,.2,1), 놓음 260 ms (.4,0,.6,1). 취소 시 이동 없음·원래 면 복원 검사 |
| B1 선택 | [0 / 93 / 187 / 280 ms](lesson-track-b1-strip.png) | 분류·모드·시험장 시간·코스의 공용 트랙. 280 ms (.3,1.25,.5,1), 지나친 뒤 복귀. 위치·너비 중간값, 첫 프레임, 크기 변경 검사 |
| E1 기호 | [13개 타일](lesson-icons-e1.png) · [점검](lesson-maneuver-checklist.png) · [판정](lesson-done.png) · [정직성 배지](lesson-report.png) | 음성·생각·불러오기·실신호·시뮬레이션·미측정·체크·듣기·잠금·근접·지시등·비상등·시동. 현재 자리가 있는 화면에 연결. 불러오기는 공용 타일과 도감까지 구현 |
| 브리핑 비교 | [기존 핸들 그림](lesson-briefing-wheel-reference.png) · [채택한 음성 타일](lesson-briefing-voice-tile.png) · [자막](lesson-briefing-captions.png) | 본편은 Onyx 한 면과 큰 음성 타일. 자막·진행 막대·건너뛰기의 동작은 유지 |
| 주행 잠금 | [주차](lesson-locked.png) · [점검](lesson-maneuver-checklist-locked.png) · [코스](lesson-drive-exam.png) · [브리핑](lesson-briefing-route-locked.png) | 화면 전체 Onyx, 운전에 집중해 주세요, 정적인 잠금 기호. 터치·점수·장식 움직임 없음. 정차 시 원래 화면 복원 |
| 결과서와 정보 시트 | [오늘의 기록](lesson-report-session.png) · [자세히 보기](lesson-details.png) · [진단서](lesson-certificate.png) · [시험 결과](lesson-done-exam-good.png) | 기존 배치를 유지하고 토큰·공용 버튼·배지와 기호만 적용. 결과서 6 · 정보 화면 2 · C1 · 새 차 그림은 27b |

E1 정직성은 ● / ◐ / ○의 차이를 타일 안에서 유지한다. 배지의 접근성 텍스트는 기존 `실신호 N · 시뮬레이션 N · 미측정 N` 그대로다. 주행 잠금에서는 타일 애니메이션을 시작하지 않으며, 실제 지시등은 들어온 신호 상태를 따른다. 코치 버튼의 듣기 움직임은 누르는 동안만 표시한다.

Signal은 근접·오답·감점·잘못된 점검 같은 경고에만 사용한다. 교육 도식의 황색 중앙선·청색 버스전용차로는 기존 도로 표지 의미를 보존한다. 시트의 기존 차 포스터는 형상을 유지하고 무채색으로 표시한다. 글꼴 파일을 추가하지 않았으며 시스템 산세리프와 Regular 400 / 제목 Medium 500 / 워드마크 Bold를 사용한다.

## 검증

[PowerShell 빌드](build-round27a.txt): `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest`. [단위 테스트](unit-round27a.txt) 368개, 실패·오류·건너뜀 0.

같은 최종 앱·계측 APK로 원본 `tools/lesson_shots.sh`를 3회 연속 실행했다. 기본 제한 900초를 유지했다.

| 실행 | 로그 | 결과 | 설치·가져오기 포함 |
|---|---|---|---|
| 1 | [계약 1](contract-round27a-1.txt) | Lesson contract passed | 447초 |
| 2 | [계약 2](contract-round27a-2.txt) | Lesson contract passed | 447초 |
| 3 | [계약 3](contract-round27a-3.txt) | Lesson contract passed | 446초 |

H8/A1 실제 누름·취소·복원, 좁은 보내기 버튼의 한 줄·잘림 없음, B1 중간 위치·너비·지나침·복귀, 첫 배치·크기 변경, 비활성 시작, 긴 과제, 잠금·Real 경계, 예약·프로필·대화·IME 계약을 확인했다. [E1 캡처 픽셀 확인](icons-round27a.txt)에서도 세 출처 모양과 근접 타일만 Signal인 것을 확인했다.

[추가 코스 계측](contract-round27a-extra.txt)도 통과했다. 신호 없음/노랑/초록/빨강, 주행 잠금, 실제 시드의 감점 위치, 시험 결과·진단서, 평행·사선 주차, 시험장 예약을 확인했다.

`RESERVE=1 bash tools/emu_flow.sh`는 **PASS · clashes 0**([로그](flow-round27a-rear.txt), 213초). 예약 → 후면 주차 힌트 모드 → **60/55·4구간 → 100/100·2구간**, 필수 힌트 세 종류·둘째 추가 힌트 없음·도어→Report·`실신호 0 · 시뮬레이션 8 · 미측정 0`을 확인했다. [오늘의 기록](lesson-report-session.png)과 [자세히 보기](lesson-details-session.png)는 이 실제 두 회차에서 얻었다.

최종 APK를 `adb -s emulator-5556 install -r`로 [재설치](install-round27a-course.txt)한 뒤 `bash tools/course_flow.sh`도 **PASS**([로그](flow-round27a-course.txt), 346초). **70 불합격·감점 3 → 100 합격·감점 0 → 도어→Report**를 확인했다.

`emu_flow.sh`와 `course_flow.sh`의 준비 검사에는 `emulator-5554`가 고정되어 있어, 이 두 실행만 ignored `build/adb-round27a.sh`를 `ADB`로 지정했다. 이 3줄 래퍼는 선두 serial 인수를 제거하고 모든 호출에 `-s emulator-5556`을 붙인다. 도구의 판정·탭·대기·캡처 코드와 제한 시간은 그대로다. `lesson_shots.sh`에는 래퍼 없이 `ANDROID_SERIAL=emulator-5556`만 지정했다.

## 캡처 범위

기존 PNG 197장을 모두 교체하고 9장을 추가했다(총 206장). 기존 MP4 3개와 조향·Done 프레임 띠 3개도 최종 앱으로 갱신했다. [파일 대조 결과](media-round27a.txt).

[S자 실제 주행](lesson-drive-exam-practice-s-live.png) · [진행 후](lesson-drive-exam-practice-s-live-turn.png)에서도 잠금과 앱 터치 대상 0개를 확인했다([로그](flow-round27a-s-practice.txt)). [키보드 화면](lesson-setup-coach-ime.png)은 ADB로 hello를 입력한 상태다. 말 카드·입력줄·한 줄 보내기·돌아가기가 키보드 위에 보인다. 실제 키 타이핑 검사로 세지 않았으며, 한국어 조합과 버튼/IME 전송은 전체 계측에서 확인했다.

[조향 영상](lesson-round4-steering.mp4) · [전체 전환 띠](lesson-round7-steering-strip.png)는 0.0~3.5초를 0.5초 간격으로, [조향 보간 띠](lesson-steering-round12-strip.png)는 0.9~1.6초를 0.1초 간격으로 추출했다. [Done 영상](lesson-round4-done.mp4) · [도착 띠](lesson-round8-done-strip.png)는 0.5~4.0초를 0.5초 간격으로 추출했다. 세 띠 모두 영상의 왼쪽 패널이다. Done 영상의 인코딩 길이는 약 3.76초이며, 띠의 마지막 칸은 녹화의 마지막 프레임을 연장해 빈 칸을 채웠다.

기존 문구·흐름을 유지한 계측 fixture 및 실제 Fake 시나리오 캡처다. 실차 측정 자료가 아니다. 새 H8/A1/B1 띠는 렌더링한 앱의 실제 프레임을 사용했다. 조향·Done 영상은 계측이 5초 제한으로 기록한 screenrecord이며, 기존 영상 링크도 같은 최종 앱의 새 영상으로 교체했다.

기존 링크를 유지하기 위해 `lesson-report-all-missing-before.png`와 `lesson-report-all-missing-details-before.png`도 현재의 미측정 화면으로 교체했다. `lesson-done-seed-aligned.png`는 현재 선택되는 one-go 판정 화면의 호환 파일명이다. 이 세 파일은 이전/이후 비교 자료가 아니다. 과거 화면·설명은 Git 이력에 남아 있다. 이전 라운드의 검증 로그는 이 폴더에 보존했으며, 이번 검증은 위의 round27a 로그를 기준으로 한다.
