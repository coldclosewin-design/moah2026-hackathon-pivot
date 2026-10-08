# 라운드 31a · 지금 화면의 열두 항목

시작 기준은 `origin/main` **524dd73**이다. #262 발주서, #263 첫 실행 다섯 질문, #264 `emu_flow` 로그 판정이 포함된 새 워크트리의 `codex/ui-round31a`에서 작업했다. [발주서](../../../handoffs/2026-10-08_codex_ui_round31a.md)에 따라 기존 앱 화면을 직접 고쳤다.

## 고치기 전 | 고친 뒤

왼쪽은 기준 커밋의 앱 APK, 오른쪽은 이번 최종 APK다. 같은 고정 데이터와 화면 경로를 사용했다. 비교판에서는 **2560×1440 화면 전체를 1280×720으로 축소**하고 제목만 덧붙였다. 앱 요소를 이동하거나 합성하지 않았고, 잘라 내지 않았다. 전체 해상도 원본은 [before](before/)와 [after](after/)에 있다. 외부 Fake 환경의 `emulator-5556`에서만 캡처했다.

| 항목 | 바뀐 화면 | 전후 비교 |
|---|---|---|
| ① 과제 시트 | 약한 그림자, 제목 들여쓰기, 모드에 따른 노란 아이콘, 밝은 원 중앙의 화살표 | [평가](compare-31a-01-task.png) · [가이드](compare-31a-01-guide.png) · [힌트](compare-31a-01-hint.png) |
| ② 오늘의 기록 | 긴 과제 제목과 두 줄 총평이 본문 안에 표시됨 | [리포트](compare-31a-02-report.png) |
| ③ 홈 모드 드롭다운 | 뒤 판을 없애고 각 선택 칸에 약한 그림자 적용 | [모드 선택](compare-31a-03-mode.png) |
| ④ 평가 점검 | 점검 카드·연결선 없이 차만 표시, 오른쪽 코치 판 유지 | [평가](compare-31a-04-evaluate.png) |
| ⑤ 프로필 | 질문 바로 아래 칩, 선택한 원목 차를 왼쪽 방향으로 미리 표시, 차종 없는 홈의 차 제거 | [면허](compare-31a-05-profile.png) · [타는 차](compare-31a-05-car-chips.png) · [미리 보기](compare-31a-05-car-preview.png) · [빈 홈](compare-31a-05-empty-home.png) |
| ⑥ 예약 카드 | 시간·코스 바꾸기 알약의 글자가 모두 표시됨 | [예약 확대](compare-31a-06-booking.png) |
| ⑦ 글자 잘림 | 모든 지식 문항의 질문·보기·해설 검사, 최장 해설 두 줄, 긴 보기 전체 표시 | [72자 해설](compare-31a-07-quiz.png) · [긴 보기](compare-31a-07-long-choice.png) |
| ⑧ 출처 배지 | 자세히 보기·점검 항목 기호·준비실/관리자 띠만 유지, 기호와 숫자 정렬 | [리포트 본문](compare-31a-08-source.png) · [자세히 보기](compare-31a-08-details.png) · [점검 기호](compare-31a-08-checklist.png) |
| ⑨ 해설 카드 | 문항·결과 모두 중첩 카드 없이 한 겹 | [문항](compare-31a-07-quiz.png) · [신호와 우회전 결과](compare-31a-09-explanation.png) |
| ⑩ 코치 알약 | 테두리를 없애고 흰 바탕과 약한 그림자 적용 | [홈](compare-31a-10-coach.png) |
| ⑪ 점검 끝 | 오늘은 여기까지 글자를 알약 전체의 중앙에 정렬 | [끝 화면](compare-31a-11-end.png) |
| ⑫ 앱 이름 | 실제 런처의 앱 이름이 Drive Coach | [런처](compare-31a-12-name.png) |

첫 실행은 모델의 다섯 질문 순서를 그대로 따른다. 기준 APK에도 #263이 들어 있으므로 이 비교는 새로 추가한 단계의 전후가 아니라 **다섯 단계 유지와 화면 배치 확인**이다.

| 첫 실행 순서 | 비교 |
|---|---|
| 가장 걱정되는 것 | [FEAR](compare-31a-05-onboarding-fear.png) |
| 마지막 운전 | [LAST_DRIVE](compare-31a-05-onboarding-last_drive.png) |
| 연습 목표 | [GOAL](compare-31a-05-onboarding-goal.png) |
| 면허 | [LICENSE](compare-31a-05-onboarding-license.png) |
| 타는 차 | [CAR](compare-31a-05-onboarding-car.png) |
| 다섯 답변 완료 | [완료](compare-31a-05-onboarding-complete.png) |

출처 표기 제거의 추가 비교: [회차 판정](compare-31a-08-done.png) · [주차 중](compare-31a-08-parking.png) · [주행](compare-31a-08-drive.png) · [진단서](compare-31a-08-certificate.png) · [공유 예시](compare-31a-08-share.png). 측정하지 못한 값의 `미측정`과 공유의 `예시` 안내는 그대로다. 작은 [코치 보내기 알약](compare-31a-07-send.png)도 공용 화살표 정렬에 맞춰 폭을 보정했다.

②는 실제 과제 중 가장 긴 제목에 기존 주차 기록과 두 문장 총평을 결합한 **글자 배치 검사용 고정 데이터**다. 실제 코스 실행 결과를 뜻하지 않는다. ⑥의 수정 전은 기준 APK의 전체 계측이 남긴 `lesson-28b-booking-expanded.png`다. 같은 서초 시험장·시간·과제의 전환이 끝난 정지 화면을 사용했다. ⑦은 `parking-shift-stop`의 최장 해설과 `priority-unprotected-left`의 긴 보기를 사용한다. ⑨에는 요청한 `신호와 우회전 — 정지선 앞에서 일단 멈춘 뒤 돌아요`가 포함된다. ⑫는 실제 런처에서 캡처했으며 이름 변경에 따라 정렬 위치도 바뀐다.

## 검증

| 검사 | 결과 | 증거 |
|---|---|---|
| `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` | 통과 · JVM 368개, 실패·오류·건너뜀 0 | [빌드](build.txt) · [APK 지문·단위 테스트 수](unit-and-apk.txt) |
| 전체 `lesson_shots` 1회차 | `Lesson contract passed` · 10/9 00:04 KST | [계약 1](contract-1.txt) |
| 전체 `lesson_shots` 2회차 | `Lesson contract passed` · 10/9 00:15 KST | [계약 2](contract-2.txt) |
| 전체 `lesson_shots` 3회차 | `Lesson contract passed` · 10/9 00:26 KST | [계약 3](contract-3.txt) |
| `RESERVE=1 emu_flow.sh` | PASS · clashes 0 · 예약 → 주차 두 회차 → 리포트 | [주차 시연](emu-flow.txt) |
| `adb install -r` | Success · 같은 최종 APK 재설치 | [재설치](reinstall.txt) |
| 재설치 후 `course_flow.sh` | PASS · 70점 불합격/감점 3 → 100점 합격/감점 0 | [코스 시연](course-flow.txt) |

동일 최종 APK로 전체 화면 계약을 **세 번 연속 통과**했다. 비교판 32쌍과 전체 해상도 원본을 보존했다. 수정 후 앱 내부 화면은 마지막 전체 계측의 캡처를 사용한다.

예약 주차 시연: [예약 홈](flow-rear-reserved.png) · [못한 주차](flow-rear-done-1.png) · [잘한 주차](flow-rear-done-2.png) · [리포트](flow-rear-report.png). 숙련/안전은 60/55 → 100/100, 리포트 로그의 출처 집계는 `live=0, simulated=8, missing=0`이다.

코스 시연: [못한 시험](flow-course-done-1.png) · [잘한 시험](flow-course-done-2.png) · [리포트](flow-course-report.png). 10/9 00:36 KST에 통과했으며 리포트 로그의 출처 집계는 `live=0, simulated=17, missing=0`이다. 런처는 이 시연 뒤 같은 설치에서 다시 캡처했다.

실행 환경은 Windows PowerShell·JDK 17·Git Bash다. `ANDROID_SERIAL=emulator-5556`, 전체 계측의 `ADB_TIMEOUT=1200`으로 실행한다. 기존 시연 도구의 읽기 전용 시작 검사에 남은 `-s emulator-5554 get-state`는 로컬 `ADB` 래퍼가 `emulator-5556`으로 연결한다. 래퍼는 모든 실제 호출을 `adb -s emulator-5556`으로 고정하며, 다른 장치를 대상으로 한 동작은 거부한다. 도구의 시나리오·검증 로직은 수정하지 않았다.

새 화면 계약은 첫 실행 다섯 질문, 질문과 칩의 상대 위치, 전체 지식 문항의 질문·보기·해설 끝까지 표시, 긴 리포트 제목·총평·측정 안내, 평가 점검의 카드 부재, 자세히 보기의 집계와 기호 정렬을 확인한다. 기존 계약은 잠금·주행 중 터치 액션/점수 없음, Real 관리자 없음, 공유 범위, 관리자 띠, 예약과 전환을 계속 검사한다. 공용 화살표 배치에 맞춰 작은 코치 `보내기` 알약도 넓혀 글자를 한 줄로 보존했다.

## 범위

수정 대상은 UI·화면 계측·문자열·이 증거 묶음이다. **31a 보류 항목은 없다.** 시안이 필요한 모드 롤링·주행 지도·로고는 31b 발주 범위다. 차량·포트·채점·상태기계·Gradle·도구·NEXT는 변경하지 않았다.
