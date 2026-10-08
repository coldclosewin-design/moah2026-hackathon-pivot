# 라운드 31a

[기존 화면 열두 항목의 고치기 전·고친 뒤 및 검증 증거](round31a/README.md)

# 라운드 30

[선택 칸 11항목 비교·움직임 프레임·C3 전체 화면 재검증](round30/README.md)

# 라운드 28c

[선택 칸 13항목 비교·움직임 프레임·검증 증거](round28c/README.md)

# 라운드 28b · 선택한 시안 칸과 앱 비교

2026-10-08 · `codex/ui-round28b` · 시작 기준 `origin/main`의 `8331bc0` (28a 머지). [28b 발주서](../../handoffs/2026-10-08_codex_ui_round28b.md)의 8개 화면을 재구성하고, 추가 28a 리뷰의 정직성 기호를 그린 도형으로 교체했다. 모든 앱 캡처와 도구 실행은 `emulator-5556`에서 얻었다.

## 화면별 비교

각 비교 이미지의 **왼쪽은 고른 시안 칸**, **오른쪽은 앱 캡처**다. 원본 PNG를 잘라 사용했고, 앱은 2560×1440 캡처에서 시스템 바만 제외한 `(0,76)–(2560,1344)` 영역을 853×423으로 축소했다. 앱 요소를 옮기거나 합성하지 않았다. [8개 화면 한눈에 보기](compare-28b-overview.png).

| 화면 | 고른 시안 | 시안 칸 ↔ 앱 | 상태별 원본 캡처 |
|---|---|---|---|
| ① 출발 전 점검 | round28 `3-checklist-drive` A7 차 위 그림 | [비교](compare-28b-checklist.png) | [가이드](lesson-28b-checklist-guide.png) · [힌트](lesson-28b-checklist-hint.png) · [평가](lesson-28b-checklist-evaluate.png) · [평가의 미측정](lesson-28b-checklist-evaluate-missing.png) |
| ② 코스·도로 주행 | round28 `3-checklist-drive` B4 전면 지도 + 떠 있는 흰 카드 | [비교](compare-28b-drive.png) | [코스](lesson-28b-drive-course.png) · [도로](lesson-28b-drive-road.png) · [돌발](lesson-28b-drive-emergency.png) · [정차](lesson-28b-drive-stopped.png) · [위치 미측정](lesson-28b-drive-missing.png) |
| ③ 진단서 | round28 `4-results` C1 트랙으로 고르는 범위 | [비교](compare-28b-certificate.png) | [총점만](lesson-28b-certificate-score_only.png) · [항목별](lesson-28b-certificate-per_item.png) · [원시 신호](lesson-28b-certificate-raw.png) |
| ④ 지식 퀴즈 | round28 `5-quiz-venue-admin` Q2 큰 02 + 그림 칸 | [비교](compare-28b-quiz.png) | [문제](lesson-28b-quiz.png) · [해설](lesson-28b-quiz-explanation.png) · [결과](lesson-28b-quiz-result.png) |
| ⑤ 제휴 시험장 | round28 `5-quiz-venue-admin` V1 문장 예약 시트 | [비교](compare-28b-venue.png) | [시험장](lesson-28b-venue.png) · [시간·예약 비활성](lesson-28b-venue-time.png) · [시간 선택](lesson-28b-venue-time-selected.png) · [코스](lesson-28b-venue-course.png) · [예약 준비](lesson-28b-venue-ready.png) |
| ⑥ 준비실 + 관리자 띠 | round28 `5-quiz-venue-admin` C1 흰 카드 대시보드 + Graphite 띠 | [비교](compare-28b-admin.png) | [준비실](lesson-28b-admin.png) · [관리자 띠](lesson-28b-admin-band.png) |
| ⑦ 프로필 + 첫 실행 질문 | round28 `1-home-popups` B1 문장 프로필 | [비교](compare-28b-profile.png) | [다섯 필드](lesson-28b-profile.png) · [낱말 편집](lesson-28b-profile-edit.png) · [첫 실행](lesson-28b-onboarding.png) |
| ⑧ 예약 카드 홈 | round28 `1-home-popups` D4 반쯤 올라온 카드 | [비교](compare-28b-booking.png) | [접힘](lesson-28b-booking-home.png) · [펼침](lesson-28b-booking-expanded.png) · [코스 연습 선택](lesson-28b-booking-selected.png) · [펼친 상태의 모드 바퀴](lesson-28b-booking-mode-wheel.png) |

크롭 좌표는 `x, y, width, height` 순서로 A7 `(24,5750,853,423)`, B4 `(24,9573,853,423)`, 진단서 C1 `(44,11716,853,423)`, Q2 `(44,1503,853,423)`, V1 `(44,5435,853,423)`, 준비실 C1 `(44,10150,853,423)`, B1 `(44,4643,853,423)`, D4 `(44,13844,853,423)`이다. 원본은 `docs/design/round28-proposals/`의 같은 이름 PNG다.

## 추가 28a 리뷰 · 정직성 기호

`SignalShape`가 **채운 원 / 반 채운 원 / 속이 빈 점선 원**을 Canvas로 그린다. 작은 타일 안의 글리프를 쓰던 공용 출처 줄과 대시보드의 글자 기호를 교체했다. 접근성의 기존 출처 문장과 수는 유지한다. 세 도형의 중심·좌우 픽셀도 계측한다.

| 화면 | 시안 칸 ↔ 수정한 앱 | 원본 |
|---|---|---|
| 판정 | [비교](compare-28b-honesty-done.png) | [캡처](lesson-28b-honesty-done.png) |
| 자세히 보기 | [비교](compare-28b-honesty-details.png) | [캡처](lesson-28b-honesty-details.png) |
| 대시보드 | [비교](compare-28b-honesty-dashboard.png) | [캡처](lesson-28b-honesty-dashboard.png) |

이 비교의 시안 크롭은 28a와 같은 round28 `4-results` A5 `(44,4076,853,423)`, round27 `3-results` 6 `(44,5394,853,423)`, round28 `4-results` B6 `(44,10027,853,423)`이다. [도형 픽셀 검사 캡처](lesson-28b-honesty-shapes.png)는 세 상태를 크게 분리해 보여 준다.

## 구현과 유지한 동작

A7은 V4 차를 오른쪽으로 향하게 놓고 실제 위치에 일곱 꼬리표를 단다. 가이드는 값과 현재 항목, 힌트는 완료·대기 기호와 잘못된 값, 평가는 이름과 빈 칸을 표시한다. 미측정은 세 모드 모두 빈 칸 대신 `미측정`으로 남는다. 꼬리표가 서로 겹치지 않는지 계측한다.

B4는 지도 위에 정보 카드가 떠 있는 구성이다. 속도 > 5 km/h에서도 지도는 보이며 시연 띠, 완료 버튼, 스크롤, 장식 움직임을 그리지 않는다. 빨간불과 돌발에만 Signal 경고를 쓰고, 위치·신호의 시뮬레이션/미측정 표기는 유지한다. 정차 때만 `다 됐어요`를 표시한다.

진단서는 왼쪽 실제 한 과제 표와 오른쪽 공유 범위 트랙으로 나눈다. 표의 화살표는 첫 회차와 마지막 회차의 실제 기록이다. 없는 다른 과제의 합계를 만들지 않는다. 총점만→항목별→원시 신호 트랙과 포함 항목 예시, 실제 전송 없음 안내를 유지한다. 퀴즈는 기존 시드의 3지선다, 실제 도로 표시 그림, 답 선택·해설·종료·결과 흐름을 유지한다.

V1은 시험장·시간·코스 낱말로 흰 편집 카드를 연다. 마감 시간에는 클릭 동작이 없고, 시간과 코스를 모두 고르기 전에는 고정된 `예약`이 비활성이다. 시험장을 바꾸면 이전 시간·코스 선택을 비우고, 예약 확인·취소·저장과 홈 추천은 기존 모델을 사용한다. 도구가 누르는 시험장·시간·코스·버튼 라벨은 유지한다.

프로필은 실제 다섯 필드와 앱 관찰 내용을 사용하며, 낱말을 누르면 현재 질문과 흰 답 카드로 편집한다. 첫 실행의 두 질문과 나머지 질문을 연습하면서 받는 흐름도 유지한다. D4는 H8 시작 줄 아래의 예약 카드가 펼쳐져 코스 연습/모의시험을 고르게 한다. 펼쳤을 때도 과제 링크와 A3 모드 바퀴를 각각 사용할 수 있다. 바퀴가 열리면 예약 카드의 노란 아이콘은 회색으로 바뀌어 한 화면 한 노란 포인트를 유지한다.

준비실은 상태 카드·프리셋 카드·기존 선택 트랙을 흰 대시보드로 배치하고 관리자 띠는 Graphite로 그린다. 관리자 라벨·순서와 Real 경로에서 관리자 UI가 없는 계약을 유지한다. `vehicle/`, `ports/`, `scoring/`, `feature/lesson/`, 데이터 구조, Gradle, `tools/`, `docs/NEXT.md`는 수정하지 않았다.

## 검증

동일한 최종 앱 APK로 화면 계측 세 번을 연속 실행한 뒤 예약 포함 주차, APK 재설치, 모의시험 순으로 실행했다.

| 확인 | 결과와 증거 |
|---|---|
| `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` | [빌드 PASS](build-round28b.txt) · [JVM 368개, 실패/오류/건너뜀 0 · APK SHA-256](unit-round28b.txt) |
| `ANDROID_SERIAL=emulator-5556 bash tools/lesson_shots.sh` 3회 연속 | 모두 `Lesson contract passed` · [1회](contract-round28b-1.txt) · [2회](contract-round28b-2.txt) · [3회](contract-round28b-3.txt) |
| `RESERVE=1 bash tools/emu_flow.sh` | [PASS · clashes 0](flow-round28b-rear.txt) · 예약, 필수 힌트 3종, 60/55 → 100/100, 도어 → Report |
| `adb -s emulator-5556 install -r` 뒤 `bash tools/course_flow.sh` | [재설치 Success](install-round28b-course.txt) · [PASS](flow-round28b-course.txt) · 70 불합격/감점 3 → 100 합격/감점 0 · 도어 → Report |
| 주행 터치·점수·지도 | [시연 접근성 노드 계측](touch-round28b.txt) · 코스/도로/돌발 >5 km/h fixture도 각각 터치 타깃 0·점수 0·지도 표시 |

`course_flow`의 주행 관찰에서 **14 km/h · 터치 타깃 0 · 점수 노드 0 · 지도 표시**를 확인했다. [접근성 XML](nodes-round28b-course-22_drive_slope-sample-3-nodes.xml) · [같은 관찰의 추가 캡처](lesson-28b-flow-course-22_drive_slope-sample-3-audit.png). XML과 화면은 순차 수집했다. XML은 14 km/h, 직후 추가 캡처는 15 km/h이며 둘 다 잠금 속도 범위다. 기존 순간 경고인 “뒤로 밀림, 감점입니다.”는 문구를 보존하고 별도 경고 항목에 기록했다. 점수·감점 누계·N점 수치는 없었다. `emu_flow`의 주차 캡처는 3 km/h이며, 기존 저속 주차 시드라 >5 km/h 잠금 검사의 대상이 아니다. 낮은 속도의 터치 수를 0으로 보고하지 않고 원래 관찰값을 함께 기록했다.

자동 시연 캡처: 주차 [첫 회차](lesson-28b-flow-rear-done-bad.png) · [둘째 회차](lesson-28b-flow-rear-done-good.png) · [기록](lesson-28b-flow-rear-report.png), 모의시험 [불합격](lesson-28b-flow-course-done-bad.png) · [합격](lesson-28b-flow-course-done-good.png) · [기록](lesson-28b-flow-course-report.png).

도구의 시작 검사에는 `emulator-5554`가 하드코딩돼 있어 28a와 동일하게 ignored `build/`의 `ADB` 래퍼로 모든 호출을 `adb.exe -s emulator-5556`에 고정했다. 자동 시연 래퍼는 지정 주행 캡처를 당긴 직후 접근성 XML과 추가 화면을 순차 저장한다. 도구의 소스·탭·대기·판정은 수정하지 않았으며 별도의 동시 UI 자동화는 실행하지 않았다.

기존 28a 화면, A1/H8 누름 프레임, B1 선택 프레임, 브리핑 경로, 실제 예약, 대화·한국어 입력·IME, 음성 시드, 도로 표시, Real/시연 잠금, 퀴즈 종료 계약을 함께 실행했다. 추가한 28b 계약은 점검 세 모드·미측정·일곱 꼬리표, 지도 잠금, 한 과제 진단서·공유 범위, 그린 출처 도형, 퀴즈·프로필·준비실·예약 카드의 콜백을 검증한다.

비교용 점검·주행·결과 데이터는 기존 Fake 시드를 재생한 계측 fixture다. 출처 모양 비교는 세 상태를 함께 보여 주도록 명시한 5/2/1 fixture이며 실차 측정 자료가 아니다. 퀴즈 결과 캡처의 0은 미응답 종료 fixture다. 이번 자료는 `lesson-28b-*`, `compare-28b-*`, `*-round28b-*`이며 아래 28a 자료는 당시 기록으로 보존한다.

---

# 라운드 28a · 선택한 시안 칸과 앱 비교

2026-10-08 · `codex/ui-round28a` · 시작 기준 `origin/main`의 `3b6b057` (#246). [발주서](../../handoffs/2026-10-08_codex_ui_round28a.md)의 화면 구성을 구현했다. 모든 앱 캡처와 도구 실행은 `emulator-5556`에서 얻었다.

## 화면별 비교

각 비교 이미지의 **왼쪽은 지정 시안의 칸**, **오른쪽은 실제 앱 캡처**다. 원본 앱 캡처 2560×1440에서 시스템 바만 제외한 `(0,76)–(2560,1344)` 영역을 시안과 같은 853×423으로 축소했다. 앱 요소를 옮기거나 합성하지 않았다. 웹 시안은 로컬 HTML 열기가 지원되지 않아 발주서가 허용한 같은 이름 PNG를 사용했다.

| 화면 | 선택한 시안 | 시안 칸 ↔ 앱 | 원본 앱 캡처 |
|---|---|---|---|
| ① 과제 시트 | round27 `4-dense` · 2 문장 시트 | [접힘](compare-28a-task.png) · [과제 펼침](compare-28a-task-expanded.png) | [접힘](lesson-28a-task.png) · [펼침](lesson-28a-task-expanded.png) · [모드 카드](lesson-28a-task-modes.png) |
| ② 홈 모드 팝업 | round28 `1-home-popups` · A3 제자리 낱말 바퀴 | [비교](compare-28a-mode.png) | [캡처](lesson-28a-mode.png) |
| ③ 코치와 대화 | round28 `1-home-popups` · C1 바닥 시트 + 말풍선 | [비교](compare-28a-coach.png) | [대화](lesson-28a-coach.png) · [기다림](lesson-28a-coach-waiting.png) |
| ④ 브리핑 | round28 `2-session` · A5 H8 포스터 | [비교](compare-28a-briefing.png) | [캡처](lesson-28a-briefing.png) |
| ⑤ 주차 중 | round28 `2-session` · B5 핸들 링 | [비교](compare-28a-parking.png) | [가이드](lesson-28a-parking-guide.png) · [힌트](lesson-28a-parking-hint.png) · [평가](lesson-28a-parking-evaluate.png) · [정차](lesson-28a-parking-stopped.png) · [미측정](lesson-28a-parking-missing.png) |
| ⑥ 잠금 | round28 `2-session` · C1 Onyx 한 면 + 잠금 타일 | [비교](compare-28a-locked.png) | [캡처](lesson-28a-locked.png) |
| ⑦ 회차 판정 | round28 `4-results` · A5 아래에서 올라오는 판정 카드 | [비교](compare-28a-done.png) | [주차](lesson-28a-done.png) · [점검](lesson-28a-done-checklist.png) · [모의시험 합격](lesson-28a-done-exam.png) · [모의시험 불합격](lesson-28a-done-exam-bad.png) |
| ⑧ 오늘의 기록 | round28 `4-results` · B6 흰 카드 대시보드 | [비교](compare-28a-report.png) | [주차](lesson-28a-report.png) · [점검](lesson-28a-report-checklist.png) · [모의시험](lesson-28a-report-exam.png) · [추가 질문](lesson-28a-report-ask.png) |
| ⑨ 자세히 보기 | round27 `3-results` · 6 전→후 쌍 알약 | [비교](compare-28a-details.png) | [주차](lesson-28a-details.png) · [시험 0–100 자 / 합격선 80](lesson-28a-details-exam.png) |
| ⑩ V4 차 | round27 `6-vehicle` · V4 기하 | [홈 옆모습 비교](compare-28a-vehicle.png) | [홈](lesson-28a-vehicle.png) · [과제](lesson-28a-task-expanded.png) · [궤적](lesson-28a-done.png) · [지도](lesson-28a-vehicle-map.png) · [주차 도식](lesson-28a-parking-guide.png) |

시안 PNG 크롭은 모두 `x=44, width=853, height=423`이다. 파일별 y 좌표는 `round27/4-dense.png` 1542(문장)·2005(펼침), `round28/1-home-popups.png` 1380(A3)·8553(C1), `round28/2-session.png` 3111(A5)·9264(B5)·13231(C1), `round28/4-results.png` 4076(A5)·10027(B6), `round27/3-results.png` 5394(6), `round27/6-vehicle.png` 4220(V4)이다. 원본은 `docs/design/round27-proposals/`와 `round28-proposals/`에 있다.

## 구현과 기존 흐름

과제 시트는 **열 때부터 과제 카드가 펼쳐져 있다**. 분류·과제·모드 글자와 `시작`을 바로 누를 수 있도록 과제 카드 아래에 작은 모드 트랙, 시트 아래에 기존 행동 줄을 함께 둔다. 자동 시연 도구가 `주차`·`주행`·`후면 직각 주차`·`장내기능 모의시험`·`힌트`·`평가`·`시작`을 누르는 흐름을 유지한다. 문장 접힘에서는 132dp, 편집에서는 90dp이며 C1 흰 카드와 뒤 카드 0.965 배율을 사용한다.

홈 모드 바퀴는 고른 낱말을 가운데에 두고 280ms에 걸쳐 이동한다. 코치 대화는 홈 위의 C1 시트 안에 코치/운전자 말풍선·말 카드·기다림을 배치한다. 브리핑은 흰 벽의 큰 문장, 회색 끝말, 가는 가로 진행선과 세 자막 열로 구성한다. 주차는 실제 조향 값으로 그린 이중 링과 V4 주차 칸, 모드·속도·기어·뒤 거리·출처로 구성한다.

Done은 위쪽 코치 문장과 아래쪽 Onyx 카드의 궤적/판정/행동 세 열이다. 기록은 회차·총평·판정·신호 출처의 네 카드다. 추가 프로필 질문은 별도 시트에 표시해 네 카드를 압축하지 않는다. 상세는 지난 값의 흐린 테두리와 현재 값의 Platinum 알약, 40dp 변화량, 비교 표를 쓴다. 시험에는 0–100 자와 합격선 80을 표시한다. 정직성 안내는 시안보다 크게 유지한다.

모의시험 실패 회차는 감점 설명이 지도를 밀어내지 않도록 지도 아래 설명에 스크롤을 둔다. 시연 조작 띠와 감점 세 항목이 함께 있는 상태에서도 지도 높이와 마지막 감점 설명 접근을 계측한다.

V4의 둥근 차체·실내 사각·앞 선을 과제·궤적·지도·주차 도식이 공유한다. 홈 옆모습은 900ms에 걸쳐 선 위에 들어와 멈춘다. 브리핑·과제 시트에서 포스터 webp를 제거했다. 프로필의 기존 포스터는 28b 범위다. 노란색은 `CoachColors.Accent = #F2C230` 한 토큰만 추가했으며 한 화면 한 원 아이콘에 사용한다. 궤적 방향 꺾쇠는 Onyx, Signal은 경고에만 사용한다.

## 검증

동일한 최종 APK로 아래 검증을 모두 통과했다. 화면 계측 세 번을 연속 실행한 뒤 예약 포함 주차, APK 재설치, 모의시험 순으로 실행했다.

| 확인 | 결과와 증거 |
|---|---|
| `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` | [빌드 PASS](build-round28a.txt) · [JVM 368개, 실패/오류/건너뜀 0 · APK SHA-256](unit-round28a.txt) |
| `ANDROID_SERIAL=emulator-5556 bash tools/lesson_shots.sh` 3회 연속 | 모두 `Lesson contract passed` · [1회](contract-round28a-1.txt) · [2회](contract-round28a-2.txt) · [3회](contract-round28a-3.txt) |
| `RESERVE=1 bash tools/emu_flow.sh` | [PASS · clashes 0](flow-round28a-rear.txt) · 예약, 필수 힌트 3종, 60/55 → 100/100, 도어 → Report · 세션 시작 후 Report까지 133초 |
| `adb -s emulator-5556 install -r` 뒤 `bash tools/course_flow.sh` | [재설치 Success](install-round28a-course.txt) · [PASS](flow-round28a-course.txt) · 70 불합격/감점 3 → 100 합격/감점 0 · 도어 → Report |
| 누름·선택 프레임 | [A1 누름](lesson-28a-fill-button-strip.png) · [H8 누름](lesson-28a-home-h8-strip.png) · [B1 선택](lesson-28a-track-b1-strip.png) |

자동 시연에서 얻은 주차 캡처: [첫 회차](lesson-28a-flow-rear-done-bad.png) · [둘째 회차](lesson-28a-flow-rear-done-good.png) · [기록](lesson-28a-flow-rear-report.png).

자동 시연에서 얻은 모의시험 캡처: [불합격](lesson-28a-flow-course-done-bad.png) · [합격](lesson-28a-flow-course-done-good.png) · [기록](lesson-28a-flow-course-report.png).

`emu_flow.sh`·`course_flow.sh`의 시작 검사에는 아직 `emulator-5554`가 하드코딩돼 있어, 기존 [INTEGRATION C절의 serial 후속 요청](../../INTEGRATION.md#c-요청-codex--claude--claude--codex)과 같은 방식으로 ignored `build/adb-round28a.sh`를 `ADB` 환경 변수에 지정했다. 래퍼는 선두 `-s <기기>`가 있으면 제거한 뒤 모든 호출을 `adb.exe -s emulator-5556`으로 실행한다. 도구의 내용·대기·탭·판정은 수정하지 않았다.

계측은 이전 포스터 폭·왼쪽 패널·예전 차의 보닛 픽셀처럼 폐기된 배치를 가정하던 검사를 새 28a 화면 계약으로 교체했다. 과제/모드 선택과 콜백, 브리핑 정차/잠금, 주차 세 모드와 미측정, 결과/리포트/시험, 회차 탐색, 출처의 기존 접근성 문장, 추가 질문 응답/건너뜀과 공유 예시를 확인한다. A1/H8 누름·취소·복원, B1 위치/너비/지나침/크기 변경, 실제 예약 흐름, 한국어 입력/IME/대화, Real·시연 잠금과 워드마크 길게 누르기, 퀴즈 종료 검사는 유지한다. B1은 전체 과제 시트 대신 공용 SelectionTrack 자체의 프레임 시계를 구동한다. V4 지도 검사는 보닛 면 대신 새 차체 앞뒤 외곽을 측정한다.

위 비교용 결과 값은 기존 Fake 주차/점검/시험 시드를 기록한 계측 fixture다. 실차 자료가 아니며 시안의 예시 숫자로 바꾸지 않았다. `lesson-28a-*`, `compare-28a-*`, `*-round28a-*`가 이번 라운드의 산출물이고, 기존 라운드 캡처·로그는 과거 자료로 보존한다.
