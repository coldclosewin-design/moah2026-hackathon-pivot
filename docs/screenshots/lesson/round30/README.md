# 라운드 30 — 선택 시안과 전체 화면 재검증

기준 `origin/main` **3cb9f23** (#258 시안 · #259 발주서 머지 뒤). 새 워크트리의 `codex/ui-round30`에서 작업했다. 모든 설치·계측·시연은 **emulator-5556**에서만 실행한다.

비교 왼쪽은 `docs/design/round30-proposals/`의 선택 칸 원본 크롭, 오른쪽은 앱 캡처다. 시스템 상·하단을 제외하고 비율을 유지해 크기를 맞췄다. 화면 속 배치·문장·기록 값은 편집하지 않았다. 원본 전체 화면과 움직임 프레임 띠는 `lesson-*.png`로 함께 보존한다.

| 주문 | 선택 칸 | 시안 칸 / 앱 캡처 |
|---|---|---|
| ① 홈 ↔ 코치 | A1 | [도착 화면](compare-30-coach.png) · [450ms 열기](compare-30-coach-open-motion.png) · [350ms 돌아가기](compare-30-coach-close-motion.png) |
| ② 홈 시작 | B 지금 + 사용자 안 | [누름 0/93/187/280ms](compare-30-home-start-motion.png) |
| ③ 진단서 | C1 = A2 | [총점만](compare-30-certificate-score_only.png) · [항목별](compare-30-certificate-per_item.png) · [원시 신호](compare-30-certificate-raw.png) · [범위 선택](compare-30-scope-motion.png) |
| ④ 공유 예시 | S1 + A2 | [480ms 이동](compare-30-share-motion.png) · [총점만](compare-30-share-score_only.png) · [항목별](compare-30-share-per_item.png) · [원시 신호](compare-30-share-raw.png) |
| ⑤ 출발 전 점검 | A4 | [일반 신호](compare-30-checklist.png) · [미측정](compare-30-checklist-missing.png) |
| ⑥ 지식 테스트 결과 | B1 | [답 알약 카드](compare-30-quiz.png) · [네 문항](compare-30-quiz-four.png) · [열 문항](compare-30-quiz-ten.png) |
| ⑦ 제휴 시험장 진입 | A5 | [같은 흰 판 · 560ms](compare-30-venue-entry-motion.png) |
| ⑧ 시험장 선택 | B1 | [선택 움직임](compare-30-venue-select-motion.png) · [서초](compare-30-venue-seocho.png) · [강남](compare-30-venue-gangnam.png) · [분당](compare-30-venue-bundang.png) |
| ⑨ 관리자 띠 | C3 | [손잡이](compare-30-admin.png) · [서랍 움직임](compare-30-admin-motion.png) · [열림](lesson-30-admin-open.png) · [더 보기](lesson-30-admin-more.png) · [패널 숨김](lesson-30-admin-hidden.png) |
| ⑩ 예약 취소 | 별도 시안 없음 | [28c 확대 화면 / 취소 추가](compare-30-booking-cancel.png) |
| ⑪ 과제 그림·조향 설명 | 28c A3 + A1 | [주차](compare-30-task-art-parking.png) · [주행](compare-30-task-art-driving.png) · [점검](compare-30-task-art-checklist.png) · [지식](compare-30-task-art-knowledge.png) · [조향 540°](compare-30-steering.png) |

## 동작과 비교 기준

- 코치 알약과 시트는 `SharedTransitionLayout`의 같은 경계를 공유한다. 열기 450ms, 돌아가기 350ms이며 시트 바탕은 이동 중에도 보이고 글자는 마지막 220ms에 나타난다. 도착 화면은 기존 기본 말 카드와 대화 내용을 유지한다. 시작 글자는 평소 Paper색이고 누름 채움 안에서 흰색으로 드러난다. 접근성 이름 `시작`, 바로 브리핑으로 가는 동작, 취소 후 복귀는 유지한다.
- 진단서의 큰 숫자는 `ShareLevel.includes.size`다. 표는 실제 못한/잘한 주차 기록을 표시한다. 범위 밖은 흐려지고, 공유 기둥에서는 값이 보이지 않으며 접근성에서도 제외된다. 원시 시계열은 **기록 없음**, 얻지 못한 측정은 **미측정**이다. 범례는 표 아래에 있고 버튼 줄 위에는 별도 범례를 두지 않는다.
- 공유 시안의 별도 폰을 앱 오른쪽에 함께 배치했다. 선택한 범위와 발급 대상이 폰에도 반영되며 **예시입니다 — 실제 전송·계약은 없습니다**를 유지한다. 작은 폰 안의 원시 신호 전체 항목과 하단 유효 안내까지 표시한다.
- 점검은 A4 좌표의 선과 흰 차·바닥 그림자를 사용한다. 카드와 차의 영역이 겹치지 않는지 계측한다. 완료·현재·아직·틀림·미측정을 상태와 출처 기호로 구분한다. 오른쪽 코치 판은 기존 내용을 유지한다.
- 시험장 카드는 스프링(.8/380)으로 크기·높이·투명도·그림자가 변한다. 합집합의 없는 코스는 폭이 0으로 줄고 접근성에서 즉시 제외된다. 실제 데이터는 서초 넷, 강남 도로 B 없음, 분당 도로 A 없음이다. 마감 표시는 해당 시험장의 마감 시간으로 이동한다.
- C3는 `admin != null`을 별도 자리 예약 값으로 전달한다. 본문은 **1236dp**, 바닥은 **32dp**다. 서랍 열기·더 보기·행동 뒤 자동 접힘·준비실에서 패널 숨김 모두 본문 높이가 같다. 손잡이의 접근성 이름은 **관리자 띠 열기**, 기존 텍스트 기반 도구용 `시연` 별칭도 남겼다. Real의 `admin == null`에는 자리·손잡이·탈출 제스처가 없다.
- 새 높이에 맞춰 준비실 여백, 과제 제목 높이, 예약 확대 화면의 버튼 영역, 공유 폰 하단을 확인했다. 실제 시연에서 발견한 리포트 출처 안내의 잘림은 글자 크기를 유지하고 카드 세로 여백을 줄여 고쳤다. 주차·점검·코스의 안내 전문이 표시 영역에 들어오는지 계측에 추가했다. 과제 시트는 기존 도구를 위해 제목 전환 뒤 과제 카드까지 열린다. 예약 취소는 기존 `vm.cancelReservation`을 호출한다.

움직임 비교는 왼쪽 원본 시안의 프레임을 그대로 보존했다. 앱은 실제 Compose 프레임 시계의 0/⅓/⅔/끝을 캡처했다. 시안 A5의 표시 시점은 0/200/400/600ms이고 앱은 발주서의 560ms를 따른다. 범위 선택은 시안 항목별→원시, 앱 총점만→원시이며, S1은 시안 항목별/앱 총점만이다. 시험장 선택은 시안 서초→분당, 앱 서초→강남이다. C3 시안은 닫힘/열림/더 보기/선택 후 네 상태이고 앱 띠 열기는 0/93/187/280ms다. 중간 시안 프레임을 임의로 만들지 않았다. ⑩과 조향 비교의 왼쪽은 명시된 28c 앱 캡처다.

## 전체 검증

| 검사 | 결과 | 증거 |
|---|---|---|
| `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` | 통과 · JVM 368개, 실패·오류·건너뜀 0 | [빌드](build.txt) · [APK 지문·단위 테스트 수](unit-and-apk.txt) |
| 전체 화면 계약 1회차 | `Lesson contract passed` · 리포트 보정 포함 | [계약 1](contract-1.txt) |
| 전체 화면 계약 2회차 | `Lesson contract passed` · 리포트 보정 포함 | [계약 2](contract-2.txt) |
| 전체 화면 계약 3회차 | `Lesson contract passed` · 리포트 보정 포함 | [계약 3](contract-3.txt) |
| `RESERVE=1 emu_flow.sh` | PASS · clashes 0 · 예약 → 주차 두 회차 → 리포트 | [주차 시연](emu-flow.txt) |
| `adb install -r` | Success · 동일 최종 APK 재설치 | [재설치](reinstall.txt) |
| 재설치 후 `course_flow.sh` | PASS · 70점 불합격/감점 3 → 100점 합격/감점 0 | [코스 시연](course-flow.txt) |

리포트 보정 후 동일 최종 APK로 세 번 연속 전체 화면 계약을 통과했다(10/8 20:44·20:53·21:02 KST). C3 본문 높이 고정·행동 뒤 접힘·예약 취소·시험장별 코스와 마감·정답 표시·공유 범위 외 값 제외, 기존 잠금·주행의 클릭/길게 누르기/스크롤 액션 0, Real 관리자 없음 계약을 포함한다. 이후 예약 주차 시연을 통과하고 같은 APK를 재설치한 뒤 코스 시연도 통과했다. 실제 주차·코스 리포트 캡처에서도 출처 안내 전문을 확인했다.

주차 시연 캡처: [예약 홈](flow-rear-reserved.png) · [못한 주차](flow-rear-done-1.png) · [잘한 주차](flow-rear-done-2.png) · [리포트](flow-rear-report.png).

코스 시연 캡처: [못한 시험](flow-course-done-1.png) · [잘한 시험](flow-course-done-2.png) · [리포트](flow-course-report.png).

실행 환경은 Windows PowerShell·JDK 17·Git Bash와 `emulator-5556`이다. 전체 계약은 `ANDROID_SERIAL=emulator-5556`, `ADB_TIMEOUT=1800`으로 실행했다. 두 시연 도구의 시작 검사에 남아 있는 `-s emulator-5554`는 도구 파일을 수정하지 않고 `ADB` 환경변수의 로컬 래퍼에서 장치 인자만 제거한 뒤 `adb -s emulator-5556`으로 고정한다. 검증 로직·라벨·시나리오는 그대로 실행한다.

전체 계약 1회차에서 나온 원본 **153개(전체 화면 136개 · 프레임 띠 17개)**를 함께 보존했다. 아래 모아보기는 전체 화면의 시스템 막대만 제외한 축소본이다. 파일명의 `28a`·`28b`·`28c`는 기존 계약 이름이며 모두 이번 최종 APK에서 다시 찍었다. 기존 계약이 전환 중에 남기는 캡처도 원본 그대로 두었고, 선택 시안의 정지 화면은 위 `compare-30-*`에서 비교한다.

| 전체 화면 모아보기 | | | |
|---|---|---|---|
| [01](all-screens-01.png) | [02](all-screens-02.png) | [03](all-screens-03.png) | [04](all-screens-04.png) |
| [05](all-screens-05.png) | [06](all-screens-06.png) | [07](all-screens-07.png) | [08](all-screens-08.png) |
| [09](all-screens-09.png) | [10](all-screens-10.png) | [11](all-screens-11.png) | [12](all-screens-12.png) |

변경 범위는 UI·화면 계측·이 증거 묶음이다. 차량·포트·채점·상태기계·데이터 구조·Gradle·도구·NEXT는 변경하지 않았다.
