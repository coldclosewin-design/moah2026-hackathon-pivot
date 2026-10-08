# 라운드 28c — 선택 시안 비교와 검증

기준 `origin/main` **769edfa** · 브랜치 `codex/ui-round28c` · 에뮬레이터 **emulator-5556**만 사용.

왼쪽은 `docs/design/round29-proposals/`의 선택 칸을 원본 HTML에서 렌더한 크롭, 오른쪽은 앱 계측 캡처다. 화면 캡처의 시스템 상·하단만 제외하고 같은 비율로 축소했다. 화면 안의 배치나 숫자는 편집하지 않았다. 원본 앱 캡처와 프레임 띠도 이 폴더에 보존한다.

| 주문 | 선택 칸 | 시안 칸 / 앱 캡처 |
|---|---|---|
| ① 홈 → 과제 | T2 | [560ms 프레임 비교](compare-28c-home-task-motion.png) |
| ② 예약 홈 | B2 | [홈](compare-28c-booking.png) · [확대](compare-28c-booking-expanded.png) · [전환](compare-28c-booking-motion.png) |
| ③ 프로필 | P3 + S4 | [홈 세 칸](compare-28c-profile-home.png) · [다섯 칸](compare-28c-profile.png) · [칩 편집](compare-28c-profile-edit.png) |
| ④ 조향 움직임 | A3 + A6 | [링·방울](compare-28c-ring-motion.png) · [단계 완료 물결](compare-28c-ripple-motion.png) |
| ⑤ 단계 그림 | B5 | [코치 문장 옆 타일](compare-28c-parking-stage.png) |
| ⑥ 기어·거리 | C3 | [원형 기어와 거리 막대](compare-28c-parking-gear.png) · [미측정](compare-28c-parking-missing.png) |
| ⑦ 과제 카드 | A3 + A1 | [주차](compare-28c-tasks-parking.png) · [주행](compare-28c-tasks-driving.png) · [점검](compare-28c-tasks-checklist.png) · [지식](compare-28c-tasks-knowledge.png) |
| ⑧ 분류 움직임 | M4 + M1 | [카드 줄 480ms·알약 340ms](compare-28c-category-motion.png) |
| ⑨ 제휴 시험장 | C1 | [둥근 지도 카드와 흰 판](compare-28c-venue.png) |
| ⑩ 진단서·공유 | A4 + A7 | 진단서 [총점](compare-28c-certificate-score_only.png) / [항목](compare-28c-certificate-per_item.png) / [원시](compare-28c-certificate-raw.png), 공유 [총점](compare-28c-share-score_only.png) / [항목](compare-28c-share-per_item.png) / [원시](compare-28c-share-raw.png) |
| ⑪ 시연 탈출 | B1 + B3 | [잠금 안내](compare-28c-escape.png) · [길게 누르기 프레임](compare-28c-escape-motion.png) |
| ⑫ 관리자 조향각 | 기존 띠 문법 | [기존 띠 / 조향각 9단계](compare-28c-admin-steering.png) |
| ⑬ 지도 차 | 흰 차·Onyx 선 | [기존 지도 / 변경 지도](compare-28c-course-car.png) |

## 움직임과 실제 자료

- T2는 공유 낱말 560ms, 배경 400ms, 홈 글자 200ms, 시트 글자 280ms 지연 뒤 280ms, 시작 원→알약 120–560ms다. 홈의 시작은 여전히 즉시 브리핑을 시작한다. 제목 전환 뒤 620ms에 과제 카드 편집부가 열려 기존 도구가 분류·과제를 바로 고를 수 있다.
- B2 원본 HTML은 시작·확대 후 두 정지 칸만 제공한다. 비교 왼쪽에 두 칸을 그대로 두고, 오른쪽에는 실제 앱의 0/187/373/560ms 네 프레임을 붙였다. 중간 시안 프레임을 만들어 넣지 않았다.
- A3의 링은 spring(.88, 110), 방울은 spring(.35, 420)이다. 숫자는 첫 프레임부터 현재 신호다. A6은 가이드 단계가 완료되어 다음 단계로 갈 때만 700ms 재생한다. 비교는 원본 HTML의 A3/A6 애니메이션과 앱 링 영역을 같은 네 시점으로 잘랐다.
- M4의 나가는 줄은 160ms·48dp, 들어오는 카드는 80ms + 카드당 40ms 지연 후 280ms·64dp다. 알약에는 선택한 M1의 easeOutQuint 340ms, 글자색에는 140ms를 적용했다.
- B1은 실제 터치로 0/667/1333/2100ms를 기록했다. 마지막은 2초 임계 뒤 홈이 그려진 프레임이다. 원본 B1의 표기 시점(0/700/1400/2000ms)은 그대로 남겼다.
- ⑫·⑬은 별도 시안이 없는 주문이다. 왼쪽에는 main에 있던 28b 앱 캡처를 명시해 붙였다.
- 진단서의 값은 기존 못한/잘한 주차 시드를 `ParkingRecorder`로 기록한 실제 fixture 결과다. 첫·마지막 회차 및 차이를 보여 준다. 보관하지 않는 원시 시계열은 **기록 없음**, 얻지 못한 지표는 **미측정**이다. 출처는 저장된 회차 배지 기준임을 표 아래에 명시했다. 시안의 예시 숫자로 바꾸지 않았다.
- 공유는 진단서와 같은 표를 사용하며 범위 밖 값과 출처를 그리지 않는다. 접근성 정보에도 제외 값이 없다. 받는 쪽 미리보기에도 같은 범위가 적용된다. 실제 전송·예약·계약은 없다.

## 검증

동일한 최종 APK로 아래 검증을 통과했다. [APK SHA-256과 JVM 집계](unit-and-apk.txt).

| 확인 | 결과 |
|---|---|
| `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` | [빌드 PASS](build.txt) · 단위 테스트 368개, 실패·오류·건너뜀 0 |
| `ANDROID_SERIAL=emulator-5556 bash tools/lesson_shots.sh` 3회 연속 | 모두 `Lesson contract passed` · [1회](contract-1.txt) · [2회](contract-2.txt) · [3회](contract-3.txt) |
| `RESERVE=1 bash tools/emu_flow.sh` | [PASS · clashes 0](flow-rear.txt) · 예약·필수 힌트 3종·60/55 → 100/100·도어 → Report |
| APK `install -r` 후 `bash tools/course_flow.sh` | [재설치 Success](install-course.txt) · [PASS](flow-course.txt) · 70 불합격/감점 3 → 100 합격/감점 0·도어 → Report |

실제 자동 시연 캡처: 주차 [첫 회차](flow-rear-done-first.png) · [둘째 회차](flow-rear-done-second.png) · [리포트](flow-rear-report.png), 코스 [첫 회차](flow-course-done-first.png) · [둘째 회차](flow-course-done-second.png) · [리포트](flow-course-report.png).

계측 종료 직후 첫 시연 시작에서는 앱 대신 차량 런처가 전면에 남아 그 실행을 중단했다. 사용자 10에서 앱을 콜드 재시작한 뒤 같은 APK·원본 도구로 위 주차·코스 흐름을 통과했다. 앱과 도구 소스는 이 재실행을 위해 변경하지 않았다.

계측은 예약 시 제목 크기 유지, 시간·코스 선택 전 비활성 및 마감 시간 차단, 프로필 편집, 공유 범위 세 단계, 조향각 아홉 단계의 신호 즉시 반영을 확인한다. 브리핑·주차·잠금·주행·S자·회차 결과·리포트에서 700ms는 유지되고 2초에는 시나리오 정지·정차·홈으로 돌아간다. 홈은 준비실로 간다. 관리자 기능을 주지 않은 화면에는 탈출 제스처와 안내가 없고, 기존 실제 `admin == null` ViewModel 계약도 유지한다. 잠금·주행 화면의 클릭·길게 클릭·스크롤 액션은 0이다.

`tools/`는 수정하지 않았다. 기존 도구의 선두 `-s emulator-5554` 검사도 ignored `build/` ADB 래퍼가 **emulator-5556**으로 고정해 실행했다. 순서는 전체 계측 세 번, 예약 포함 주차, APK 재설치, 코스 시연이다. 도구의 대기·탭·판정은 그대로다.

변경 범위는 UI, 계측, 이 증거 묶음이다. 관리자 구현은 main의 `setSteering`, `steeringSteps`, `escapeHome()`을 호출한다. 차량·포트·채점·상태기계·데이터 구조·Gradle·NEXT는 변경하지 않았다.
