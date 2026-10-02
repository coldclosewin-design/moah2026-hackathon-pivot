# 화면 검증 캡처

## UI 라운드 8 (2026-09-29)

`codex/ui-round8` · 기준 `origin/main=c013d39`(PR #54) · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [발주서](../../handoffs/2026-09-28_codex_ui_round8.md)의 ①②③을 구현했으며, ③은 §3 (나) 도착 칸이다.

- [과제 시트](lesson-setup-sheet.png): U자 테두리를 삭제하고 선택 `Periwinkle`·준비 `Lavender`·계획 `Lavender` 40% 면으로 바꿨다. 선택 글자·도식·난이도는 `Paper`, 준비는 `Ink`, 계획은 `Muted`. 차의 창은 카드 바탕색으로 뚫어 흰 실루엣에서도 구분된다. 카드 아래 변은 기존 체크 원 중심(전체 높이 − 32 dp)에 맞추고 흰 테·빨간 원·크기·간격·스크롤·고정 하단은 유지했다. [준비 상태 검사용 캡처](lesson-setup-sheet-ready-contract.png)는 비선택 READY 색 검사를 위해 평행 주차만 READY로 복사한 계측 데이터이며 시드는 변경하지 않았다.
- [열기 5프레임](lesson-setup-morph-strip.png)·[돌아가기 5프레임](lesson-setup-morph-return-strip.png): 실제 Compose 애니메이션 시계를 0·100·200·300·400 ms로 전진시키고 각 시점의 에뮬 화면을 캡처했다. 왼쪽 폭은 열기 `1357 → 1217 → 900 → 792 → 768 px`, 돌아가기 `768 → 907 → 1225 → 1333 → 1357 px`. `FastOutSlowInEasing` 400 ms, `CenterStart` 크롭, 같은 비율에서 확대·왼쪽 이동을 보간한다. 오른쪽 본문은 300 ms fade/40 dp slide이며 시트 첫 프레임부터 모드·시작 노드가 있다. 시계 제어는 계측에만 있고 앱은 정상 프레임 시계를 사용한다.
- [Done](lesson-done.png)·[빈 도착 칸](lesson-done-arrival-empty.png): 시작 윤곽 대신 지름 12 dp `Lavender` 점, 도착 자세에 폭 ×1.25·깊이 ×1.15의 `Periwinkle` 4 dp U자 칸을 그린다. 열린 변은 차 앞이다. 원본 경로·뷰포트·시간 순서·3초 재생·뒤가 위 관습·후진 셰브론·급정지 점은 유지한다. [경로 없는 Done](lesson-done-no-path.png)도 기존 폴백을 검증한다.
- [Done 재생 클립](lesson-round4-done.mp4)과 [프레임 스트립](lesson-round8-done-strip.png)을 확인했다. 빈 칸으로 진입해 칸 안에서 끝나며 마지막 프레임에는 셰브론이 없다. 스트립의 마지막 정지 프레임은 빈 셀을 없애기 위해 연장했다. 영상 이름은 기존 계측 계약을 유지한다.

`build-round8.txt`: PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 단위 테스트 **179개, 실패·오류 0**.

`contract-round8.txt`: 수정 없는 `tools/lesson_shots.sh build/round8-contract-final`에서 **Lesson contract passed**. 카드 안쪽/옛 윤곽 위치 색·선택 글자/도식·체크 원, 양방향 200 ms 중간 폭·첫 프레임 컨트롤, 도착 전 칸 선·열린 앞쪽·도착 차량의 네 모서리·시작 윤곽 부재를 검사했다. 퀴즈 답 클릭 후 클릭 가능 노드 두 개 검사는 최대 1초 동안 50 ms 간격으로 폴링한다. 기존 주 버튼·잠금·숫자·미측정·점검·리포트·실제 VM 예약/취소 계약도 통과했다. 전체 원본 캡처는 `build/round8-contract-final/`에 있다.

정적 확인: `ui/`의 `FontWeight.Bold` 0, `Color(0x`는 `CoachStyle.kt`에만 있다. 새 문자열 리소스 0. `PathPresentation.kt`·`pathViewport`·차량·포트·채점·상태기계·데이터·빌드·`tools/`·`docs/NEXT.md` 변경 없음.

`flow-round8.txt`: 수정 없는 `tools/emu_flow.sh build/round8-flow` **PASS·uiautomator 충돌 0**. 시트 첫 렌더의 `힌트` → `시작`, 필수 힌트 세 종류, 못한 주차 **60/55·이동 4회** → 잘한 주차 **100/100·이동 2회·추가 힌트 없음**, 도어 열림 → 리포트·`실신호 0 · 시뮬레이션 8 · 미측정 0`을 확인했다. 리포트까지 **139초**. 실제 두 번째 회차의 [도착 칸](lesson-done-path.png)·`16_done_2.png`도 교체했다.

## 출발 전 점검 7단계 (2026-09-28)

`codex/predrive-7` · 기준 `origin/main=97d81c8`(PR #47 선행 포함) · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [발주서 §2](../../handoffs/2026-09-28_codex_predrive_7steps.md)의 화면을 구현했다.

- `lesson-maneuver-checklist.png`: 도어·안전벨트·기어·브레이크/시동은 왼쪽, 좌/우 지시등·비상등은 오른쪽. 완료는 Periwinkle, 아직은 Ink 60%, 미측정은 Lavender·Muted. 각 칩은 240 dp 높이이며 스크롤 없이 일곱 개가 보인다.
- `lesson-maneuver-checklist-missing.png`: 새 신호가 없는 차. 브레이크가 미측정이어도 시동의 시뮬레이션 출처를 따로 표시한다. 기존 세 신호뿐 아니라 여덟 신호의 출처가 모두 같을 때만 공통 배지로 묶는다.
- `lesson-done-checklist.png`: 기존 두 문장 멘트·주 버튼 유지. 숫자 지표·주차 도식은 없다.
- `lesson-report-checklist.png`·`lesson-report-checklist-bad.png`·`lesson-report-checklist-missing.png`: 회차별 일곱 항목을 ✓ / ✗ / 미측정으로 표시. 벨트·시동 시각과 벨트 순서를 보이고, 주차 이동 횟수·전체 시간 행은 점검 표에서 제외했다. 기존 점수·회차 머리말은 유지한다.

Maneuver는 `ManeuverDisplayState`의 **현재 값**을 그대로 표시한다(등화를 끄면 `아직`, 브레이크를 떼면 `시동 켜짐`). 이를 화면 하단에 `현재 상태`로 명시했다. Report는 `PreDriveSummary`의 **회차 확인 이력**을 사용하므로 켰다 끈 등화도 ✓로 남는다. 브레이크 없이 시동을 켰거나 벨트가 시동보다 늦으면 해당 행은 ✗다. 신호가 없거나 순서를 확인할 수 없으면 미측정이다.

비상등 확인은 실제 조건인 **켜짐**에 맞춰 `비상등 확인. 이제 끄고 버튼을 눌러 주세요.`로 다듬었다. 시동 확인 조건은 브레이크를 검사하지 않으므로 확인 문장에 브레이크 성공을 단정하지 않았다. 기존 도어·브레이크 힌트는 유지했다.

`build-predrive-7.txt`: PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 단위 테스트 **179개, 실패·오류 0**. 추가한 UI 매핑 테스트는 실제 점검 시나리오를 사용해 일곱 완료 항목·도어/순서/브레이크/비상등 실패·신호 누락을 확인하며, 기존 `ChecklistScenarioTest`의 100/100·30/40 고정값도 통과했다.

`contract-predrive-7.txt`: 수정 없는 `tools/lesson_shots.sh build/predrive-contract`에서 **Lesson contract passed**. 일곱 칩의 4+3 배치, 완료/대기/미측정 픽셀, 브레이크·시동의 서로 다른 출처, 미측정 리포트 행, 조향·거리 비노출을 검사했다. 기존 잠금 터치 0·주 버튼·시연 패널·Done/Report·퀴즈 계약을 유지했다. 위 여섯 캡처를 직접 확인했고 글자 겹침·잘림은 없다. 전체 계측 캡처는 `build/predrive-contract/`에 있다.

`flow-predrive-7.txt`: 수정 없는 `tools/emu_flow.sh build/predrive-flow` **PASS**, uiautomator 충돌 **0**. 못한 주차 60/55·이동 4회·필수 힌트 세 종류 → 잘한 주차 100/100·이동 2회·추가 힌트 없음 → 도어 열림 후 리포트. 주차 배지는 `실신호 0 · 시뮬레이션 8 · 미측정 0` 그대로이며, 리포트까지 **134초**다.

## UI 라운드 7 (2026-09-28)

`codex/ui-round7` · 기준 `origin/main=2173242`(PR #44) · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [라운드 7 발주서](../../handoffs/2026-09-28_codex_ui_round7.md)를 구현했다.

- `lesson-quiz-answered.png`: 정답은 Periwinkle 바탕·Paper 글자와 `정답` 눈썹, 내가 고른 오답은 Lavender 바탕·Ink 글자·Signal 4dp 윤곽과 `내 답` 눈썹. 맞게 고르면 `내 답 · 정답`으로 합쳐진다.
- `lesson-quiz.png`·`lesson-quiz-locked.png`: 정차 중 하단 왼쪽의 `그만하기`, 잠금 시 선택지·그만하기·다음 액션 제거.
- `lesson-quiz-done.png`: 틀린 문제의 내 답(Signal) → 정답(Periwinkle)과 이유. 맞은 문제는 `맞았어요`, 미응답은 `안 풀었어요`를 유지한다.
- `lesson-maneuver-guides.png`: 운전자 기준 오른쪽 조향에서 오른쪽 앞바퀴(화면 왼쪽)가 더 꺾인다. 점선 두 개는 앞 차축 옆의 회전 중심을 공유하며 아래로 진행한다. 도식 밖의 호는 잘라 조향각 글자에 겹치지 않게 했다.
- `lesson-report.png`·`lesson-setup.png`: 동승자 탭·응원 눈썹이 없는 화면. `lesson-companion*.png`·`lesson-setup-cheer.png`를 삭제했다. 이전 동승자 구현·검증 이력은 PR #40~#43과 `build-companion.txt`·`contract-companion.txt`·`flow-companion.txt`에 남아 있다.

`build-round7.txt`: PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 단위 테스트 **169개, 실패·오류 0**. `wheelAngles`의 중립·미측정·좌우 반전·안쪽 각 우위와 ±45°/±38° 제한을 네 테스트로 확인한다.

`contract-round7.txt`: 수정 없는 `tools/lesson_shots.sh build/round7-contract`에서 **Lesson contract passed**. 정답·오답 눈썹/글자색·4dp 윤곽, 정차 중 답변 전후 그만하기 콜백 각 1회와 Setup 표시, 잠금 터치 0, QuizDone의 색상 구분·정답/미응답 행을 확인했다. 좌/우 회전 점선의 아래쪽 간격이 위쪽보다 넓고 두 선이 같은 방향으로 휘며, 중립에서는 두 직선임을 캡처 픽셀로 검증한다. 반투명 합성의 채널값 차이는 ±1만 허용한다. 기존 패널·주 버튼·시트·미측정·Done/Report 계약도 통과했다. 클릭 뒤에는 접근성 이벤트가 안정될 때까지 기다려 이전 과제 시트의 캐시를 읽지 않게 했다.

조향 영상 [lesson-round4-steering.mp4](lesson-round4-steering.mp4)(기존 계측 파일명 유지)와 [프레임 스트립](lesson-round7-steering-strip.png)을 확인했다. 중립 → 오른쪽 조향에서 좌우 바퀴 각 차이 → 중립 복귀가 보인다. 전체 원본 캡처·Done 영상은 `build/round7-contract/`에 있다.

정적 확인: `ui/`의 `FontWeight.Bold` 0, `Color(0x`는 `CoachStyle.kt`에만 있다. 문자열 리소스는 `lesson_quit` +1, `lesson_companion` −1. 차량·포트·채점·상태기계·데이터·빌드 파일·`tools/` 변경은 없다.

`flow-round7.txt`: 수정 없는 `tools/emu_flow.sh build/round7-flow` **PASS**, uiautomator 충돌 **0**. 못한 주차 60/55·이동 4회·필수 힌트 세 종류 → 잘한 주차 100/100·이동 2회·추가 힌트 없음 → 운전석 도어 열림 후 리포트·신호 출처 배지를 확인했다. 세션 시작부터 리포트까지 **130초**. 동승자 탭이 없어 원본 스크립트가 해당 단계를 자동으로 건너뛰었다. 원본 캡처는 `build/round7-flow/`에 있다.

## UI 라운드 6 검증 캡처

2026-09-28 · `codex/ui-round6` · 기준 `origin/main=7b5e3f2` · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [라운드 6 발주서](../../handoffs/2026-09-28_codex_ui_round6.md)의 사용자 선택 시안 05를 구현했다.

| 시안 05 | 에뮬레이터 캡처 |
|---|---|
| ![시안 05](../../design/round6-sheet/a-refinements/05-type-bays.png) | ![주차 시트](lesson-setup-sheet.png) |

- `lesson-setup-sheet.png`: 주차가 펼쳐진 시트. 제안 과제인 후면 직각 주차가 선택되어 있고, 주차 네 칸과 가이드·힌트·평가·시작이 함께 보인다. 비교를 위해 첫 렌더에서 힌트만 선택한 뒤 캡처했다.
- `lesson-setup-sheet-driving.png`: 준비 중인 주행 과제 네 칸과 다음 칸의 일부. 모드와 시작은 없다.
- `lesson-setup-sheet-driving-end.png`: 가로 스크롤 끝의 회전교차로까지 확인한다. 준비 중 다섯 과제 모두 클릭 액션 없이 disabled로 노출된다.
- `lesson-setup-knowledge.png`: 지식 카테고리로 이동해 과제를 고르고 닫았다가 다시 연 화면. 지식 테스트 모드만 보이며 시작은 `(knowledge-hazard-weather, QUIZ)`를 전달한다.

주차에만 160dp 차량 도식을 그리고, 다른 카테고리에는 같은 높이의 빈 도식 공간을 둔다. 글자 메뉴 56sp, 과제 제목 40sp·최대 두 줄, 상태·난이도 32sp, 각진 U자 윤곽 4dp(선택 6dp), 체크 원 64dp를 사용한다. 제목 영역은 한글 두 줄의 실제 글꼴 여백까지 담도록 120dp다. 예약 예시는 공간에 맞춰 접히며, 펼친 본문만 스크롤하고 토글과 하단 컨트롤은 고정된다.

`build-round6.txt`: PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 단위 테스트 **157개, 실패·오류 0**. 카테고리 순서, 준비 중 주차 시드 두 개의 순서·난이도·상태, READY 세 개 유지 검사를 포함한다.

`contract-round6.txt`: 수정 없는 `tools/lesson_shots.sh build/round6-shots-delivery`에서 **Lesson contract passed**. 첫 렌더·카테고리 순서·준비 중 클릭 차단·주행 스크롤·조작/지식 시작 전달·재개방 선택·모드 필터·예약 펼침 후 고정 하단을 검사했다. 준비 중 목록을 둘러본 뒤 돌아가도 직전 READY 과제·모드를 유지한다. Signal 글자·밑줄·아래 화살표, 선택 칸의 Ink 윤곽과 빨간 체크 원·흰 체크를 픽셀로 확인했다. 원 외곽의 안티앨리어싱 때문에 정확한 단색 픽셀 범위는 양쪽 한 픽셀씩의 여유를 둔다.

기존 네 주 버튼 140dp × 720dp 이상, 시연 알약·패널 접힘, 잠금 터치 0, 미측정·자막·퀴즈·Done/Report 계약도 통과했다. 전체 이번 캡처는 `build/round6-shots-delivery/`에 있으며, 이 폴더의 다른 화면 PNG와 기존 라운드 로그는 이전 검증 이력이다.

정적 확인: `ui/`의 `FontWeight.Bold` 0, `Color(0x`는 `CoachStyle.kt`의 다섯 토큰뿐, 문자열 리소스 변경 0. `tools/`, 빌드 파일, 상태기계·차량·포트·채점과 데이터 구조는 변경하지 않았다. 시드 내용에 준비 중 주차 두 개만 추가했다.

`flow-round6.txt`: 수정 없는 `tools/emu_flow.sh build/round6-flow` **PASS**, uiautomator 충돌 **0**. 시트를 연 직후 기존 `힌트` → `시작` 조작으로 세션에 들어간다. 못한 주차 60/55·이동 4회와 안전벨트·근접·급제동 힌트, 잘한 주차 100/100·이동 2회·추가 힌트 없음, 도어 열림 → 리포트·배지를 확인했다. 세션 시작부터 리포트까지 **124초**이며 원본 캡처는 `build/round6-flow/`에 있다.

## 제휴 시험장 예약 (2026-09-28)

`codex/reservation` · 기준 `origin/main=97d81c8`(PR #48) · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [예약 발주서 §2](../../handoffs/2026-09-28_codex_reservation.md)를 구현했다.

- 시트 아래 `제휴 시험장`을 누르면 같은 본문 영역에 사각 카드 세 개가 나타난다. 카드 높이 220 dp, 이름 40 sp·지역/거리/코스/상태 32 sp. 시험장 시드 이름은 `서초 시험장`·`강남 시험장`·`분당 시험장`으로 다듬었다.
- 시험장을 고르면 시간·코스 칩이 나타난다. 자리 없는 시간대는 회색이며 클릭 액션이 없다. 두 선택을 모두 마쳐야 140 dp × 720 dp 이상의 `예약` 버튼이 보이며, 시험장을 바꾸면 선택을 초기화한다.
- 예약 콜백 뒤 받은 `booking`으로 확인 카드를 그린다. 확인 카드의 `돌아가기`는 Setup 요약으로, `취소`는 예약 취소 후 시험장 목록으로 간다. 목록에서 예약된 시험장을 누르면 기존 확인 카드를 연다.
- Setup 배지는 프로필 위의 `예약 · 서초 14:00 · 주차 3종`. 예약·취소가 들어오면 수동 선택을 초기화해 상태기계의 과제·모드·이유를 반영한다. 예약이 없으면 배지와 여백도 없다.
- 목록·시간 선택·확인에서 `Reservation.EXAMPLE_NOTE`를 그대로 표시한다. 시간·거리와 명시된 시드 코스명 `주차 3종`만 숫자 검사에서 허용하며 점수·횟수는 없다. 문자열 리소스는 `예약`·`취소` 두 개만 추가했다.

발주서 §1 구현 메모의 소유 예외에 따라 `ReservationCard`·`Reservation.toCard`·`SeedCatalog.reservation`·`Setup.reservation`·상태기계 생성자 인자 및 호출부/호환 테스트만 삭제했다. `booking` 이름과 예약 동작은 유지한다. 차량·포트·채점·Gradle·tools 변경은 없다. 이 브랜치는 요청대로 `origin/main`에서 시작했으며, 별도 PR #49의 점검 화면 변경을 포함하지 않는다.

`build-reservation.txt`: PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 단위 테스트 **177개, 실패·오류 0**. 예약 모델 테스트와 선택한 장소·시간·코스의 표시, 삭제된 항목을 확인 카드로 만들지 않는 처리, 거리 소수/누락 표시를 검증했다.

`contract-reservation.txt`: 수정 없는 `tools/lesson_shots.sh build/reservation-contract-final`에서 **Lesson contract passed**. 220 dp 카드·비활성 시간 클릭 차단/회색·시간과 코스 동시 선택·시험장 변경 시 초기화·예약 콜백 1회 및 인자·확인 카드 재진입·취소 콜백 1회·프로필 위 배지와 취소 후 여백 제거를 검증했다. 실제 `LessonViewModel`을 사용해 지식 과제를 수동 선택한 뒤 주차 코스를 예약하면 상태기계의 주차 제안으로 돌아오는 것도 확인했다. 기존 시트·주 버튼·시연 알약·잠금·숫자 비노출 계약을 유지했다. 회색 픽셀 검사는 반투명 Muted를 Lavender 위에 합성한 값에 채널별 ±1만 허용한다.

필수 캡처 `lesson-venues.png`·`lesson-venue-slots.png`·`lesson-reservation.png`·`lesson-setup-reserved.png`를 추가하고 직접 확인했다. 카드 상태 문구를 포함해 글자 잘림·겹침은 없다. 예약된 카드의 Periwinkle 배경은 `lesson-venues-booked.png`, 새 진입 액션과 기존 힌트·시작은 교체한 `lesson-setup-sheet.png`에서 볼 수 있다. 전체 캡처는 `build/reservation-contract-final/`에 있다.

`flow-reservation.txt`: 수정 없는 `tools/emu_flow.sh build/reservation-flow` **PASS**, uiautomator 충돌 **0**. 시트 첫 렌더의 `힌트` → `시작` 경로, 필수 힌트 세 종류, 못한 주차 60/55·이동 4회 → 잘한 주차 100/100·이동 2회, 도어 열림 → 리포트를 확인했다. 배지는 `실신호 0 · 시뮬레이션 8 · 미측정 0` 그대로, 리포트까지 **136초**다.

## UI 라운드 9 (2026-10-01)

`codex/ui-round9` · 기준 `origin/main=05b7f79` · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [발주서](../../handoffs/2026-09-30_codex_ui_round9.md)의 §1 → §2 → §3 → §4 순서로 구현했다.

| 절 | 반영 및 캡처 |
|---|---|
| §1.1 | 퀴즈 잠금 시 하단 누계·그만하기·구분선 제거. 문제 번호 패널 밖 숫자 및 터치 액션 0 검사. `lesson-quiz-locked.png` |
| §2.1–2.2 | 미측정 값마다 텍스트 노드 하나, 잠금 도식 눈썹 제거. `lesson-missing.png`, `lesson-mixed.png`, `lesson-locked.png` |
| §2.3 | 점검 Done 왼쪽 38% 패널에 리포트와 같은 일곱 결과를 기호로 표시. `lesson-done-checklist.png`, `-bad.png`, `-missing.png` |
| §2.4·2.8 | 주행·지식 카드의 빈 도식 칸 제거 및 중앙 정렬, 점검에는 같은 4dp 선의 체크 도식. 조작 → 점검. `lesson-setup-sheet-driving.png`, `-driving-end.png`, `lesson-setup-knowledge.png`, `lesson-setup-sheet-checklist.png` |
| §2.5–2.7 | 퀴즈 연보라 사각형 제거, 예약 뒤 목록 문구 수정, 리포트 눈썹 `연습한 회차`·단위 `회`. 관련 퀴즈·예약·리포트 캡처 교체 |
| §2.9 | 기록된 등화 확인과 시동 순간 브레이크로 완료 상태 유지. 실제 `ChecklistScenarios`를 recorder·snapshot·display mapper에 재생해 검사. `lesson-maneuver-checklist.png`는 등화와 브레이크를 모두 끈 뒤 일곱 칩이 완료색. `-pending.png`, `-bad.png`, `-missing.png`도 보존 |
| §3 | 예약 선택 칩은 기대 상태를 50ms 간격으로 최대 1초 폴링, 미반영 시 한 번 재탭. 예약/취소 콜백은 재탭 대상에서 제외하고 각각 한 번 호출됨을 검사. 취소 뒤 복귀 모핑도 원래 좌표에 정착했는지 최대 1초 확인 |
| §4.1 | 주차 회차마다 상세 수치 한 줄(32sp Muted), null은 미측정, 급가속이 있으면 추가. `lesson-details.png`의 못한 주차는 실제 시나리오 지표 `조향 왕복 3 · 기어 전환 2 · 근접 1 · 급정지 1`. 복수 회차·미측정과 점검 표 유지도 검사 |
| §4.2 | 0·1·2·3·5 항목과 한국어 조사 단위 검사. 주차의 세 항목을 두 줄로 모두 표시. `lesson-briefing.png` |

점검 Done의 미측정은 Ink 바탕과 구분되도록 기존 Maneuver와 같은 **Paper 60%**로 표시한다. `CoachColors.Muted`는 Ink 60%라 Ink 위에서는 보이지 않는다. 새 색 토큰은 추가하지 않았고 픽셀로 가독성을 확인했다.

필수 캡처와 추가 상태 캡처를 직접 확인했다. Done 기호·점검 완료 칩·카드 제목·주차 브리핑 두 줄·상세 수치 한 줄에 잘림이나 겹침이 없다. 32개 관련 PNG(카테고리 이름이 바뀐 모핑 스트립 포함)를 최종 코드 첫 PASS 실행에서 교체/추가했다.

빌드: `build-round9-clean.txt`는 캐시를 끈 clean 빌드(`:automotive:clean :vss-stub:clean assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest --no-build-cache --no-configuration-cache --no-parallel`)이며 단위 테스트 183개, 실패·오류 0. 이어 지정 명령 `.\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest`도 성공(`build-round9.txt`). 중간 증분 실행에서 Kotlin/JUnit의 `NoClassDefFoundError`·`Truncated class file`이 실행마다 다른 클래스에서 발생했고 clean 빌드로 해소했다. 빌드 설정·의존성 변경은 없다.

첫 계측의 `contract-round9-before-settle.txt`는 예약 취소 뒤 복귀 모핑 도중 프로필 좌표를 비교해 실패했다. 원래 좌표를 완화하지 않고 최대 1초 정착 폴링을 추가했다. 이후 실행들은 같은 최종 코드·APK를 사용한다.

정적 확인: `ui/`의 `FontWeight.Bold` 0, `Color(0x`는 `CoachStyle.kt`의 기존 다섯 토큰뿐, 새 문자열 리소스 0. `vehicle/`, `ports/`, `scoring/`, `feature/lesson/`, `data/`, 빌드 파일, `tools/`, `docs/NEXT.md` 변경 0. 시트 첫 렌더·주 버튼·알약·주행 잠금·Done 도착 칸 계약은 기존 계측으로 유지한다.

같은 최종 APK로 수정 없는 `bash tools/lesson_shots.sh build/round9-contract-N`를 **3회 연속 PASS**했다. 각 로그는 기존 계약과 추가 검사를 포함하며 마지막 줄이 `Lesson contract passed`다.

| 연속 실행 | 전체 로그 | 결과 |
|---|---|---|
| 1 | [contract-round9-1.txt](contract-round9-1.txt) | Lesson contract passed |
| 2 | [contract-round9-2.txt](contract-round9-2.txt) | Lesson contract passed |
| 3 | [contract-round9-3.txt](contract-round9-3.txt) | Lesson contract passed |

APK SHA-256: 앱 `9FCBA44C9EDC6636B19D192552CA02D8A899B7D58CEC33B4626E17570D7AA198`, 계측 `7ED473DFD22214992E3695EA4C9A6897ECCF88CFF42AD3F9E595C30BD6134BA8`.

원본 `bash tools/emu_flow.sh build/round9-flow` **PASS**, uiautomator 충돌 **0**([flow-round9.txt](flow-round9.txt)). 첫 회차 60/55·이동 4회, 잘한 주차 100/100·이동 2회, 필수 힌트 세 종류와 두 번째 회차 추가 힌트 없음, 문 열림 → 리포트·배지를 확인했다. 세션 시작부터 리포트까지 **124초**다.

## 라운드 11 ① 시드 톤 (2026-10-01)

`codex/seed-tone` · 기준 `204a404` · 외부 CSTDe_API_34 · Fake 기본 배율 1.0. [발주서 ①](../../handoffs/2026-10-01_codex_ui_round11.md)의 시드 문구와 직접 관련된 검증·캡처다. 제품 화면 코드·데이터 구조·선택 조건·채점·힌트·라벨·예약 정보·퀴즈 정답 및 교육 수치는 그대로다. 모든 이미지는 **외부 fixture**이며 사내 신호를 실측한 캡처가 아니다.

| 원 항목 | 교체 캡처 | 근거 |
|---|---|---|
| A3-01 | `lesson-maneuver-guides.png` | 시드의 오른쪽 조향 요청·후진 확인 문장을 직접 사용 |
| A3-02 | `lesson-maneuver-checklist-pending.png` | 시드의 브레이크·시동 안내, 일곱 칩 대기 상태 |
| A3-03·04 | `lesson-done.png`·`lesson-done-checklist.png` | 실제 `FakeCoachPort`가 바뀐 서두와 지표 기반 조언을 조합한 두 문장 |
| A3-07 | `lesson-setup.png`·`lesson-setup-sheet.png` | 기존 선택 흐름 보존. UI에 있는 제안 띄어쓰기는 ②에서 처리 |
| A3-08 | `lesson-quiz-answered.png` | 비상등 문항의 완결된 새 해설, 오답·정답 구분 유지 |

단위 테스트 **207개, 실패·오류 0**, 지정 `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 서두 33개 모두 문장 부호 하나·해요체·숫자/횟수 0, 주차·점검 실제 상태기계 흐름의 서두+조언 두 문장, 가이드 전체의 기호/숫자 제거를 확인했다. 문자열 리터럴을 제외한 `SeedCatalog.kt` 내용은 기준과 동일하다.

동일 APK에서 원본 `tools/lesson_shots.sh` **3회 연속 Lesson contract passed**: `build/seed-pass-{1,2,3}/contract.txt`. 캡처는 첫 실행에서 가져왔고 점검 Done만 두 번째 실행이다. 첫 실행의 점검 Done PNG는 일부 글자·버튼 픽셀이 누락되어 사용하지 않았으며, 두 번째 원본에서 전문·브랜드·버튼을 직접 확인했다. 최종 7장 모두 잘림·겹침 없이 확인했다.

앱 SHA-256 `C5A31DF43D2A88DD19F7773DFA1270AAE7D702D7FE898A9CE328CFDA75BFBFF3`, 계측 SHA-256 `CBA15350E3BF22868786B7BBADFB13E50BC7A186896AEDCF48065EF7CBF5CAD2`.

별도 `am instrument --user 10 -w -e seedSpeech true com.moah.hackathon.test/com.moah.hackathon.ui.LessonScreenInstrumentation`에서 주차 **6단계**·점검 **7단계**의 요청/확인 **26문장 전부 한국어 TTS 합성 완료** 콜백과 WAV 생성을 확인했다(`build/seed-speech.txt`, `Seed guide speech passed`). 발주서 확인 열의 주차 7단계는 기존 실제 단계 수와 달라 6단계를 유지했다. 이 검증은 합성 성공과 원문 기호 검사이며 사람의 청취 평가는 아니다.

범위 밖 한계: `AdviceRules`의 `MOVED_DURING_CHECK`·`SEGMENTS`는 조언만 두 문장이라 해당 분기에서는 총 세 문장이다. 조언의 횟수 표현과 함께 `INTEGRATION.md` C절에 ports 후속을 요청했다. 첫 PR에서 포트를 변경하거나 모든 조합이 두 문장이라고 판정하지 않는다.

원본 `bash tools/emu_flow.sh build/seed-flow` **PASS·uiautomator clashes 0**, 리포트까지 **105초**. 필수 힌트 3종, 첫 회차 60/55·이동 4회, 두 번째 100/100·이동 2회·추가 힌트 0, 도어 열림 → 리포트와 출처 배지 0/8/0을 확인했다. 도구·기대 로그·채점은 수정하지 않았다. 로그는 `build/seed-flow/log.txt`에 보존했다.


## 라운드 11 ② 결과 화면·계측 (2026-10-02)

`codex/ui-round11` · 기준 `origin/main=f5b5822`(① #82 머지 뒤) · 외부 CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [발주서 ② 2.1~2.7](../../handoffs/2026-10-01_codex_ui_round11.md)에 한정했다. 아래 이미지는 모두 **외부 fixture**이며 사내 실측 캡처가 아니다. 특히 `live7-sim1-fixture`는 사내 관찰 숫자 7/1/0을 외부 모델에 넣은 배지 표시 검사다.

| 항목 | 교체·추가 캡처 | 확인 내용 |
|---|---|---|
| 2.1 결과 잠금 | 추가 `lesson-done-locked.png`, `lesson-done-checklist-locked.png`, `lesson-report-locked.png`, `lesson-report-locked-details.png`, `lesson-report-locked-certificate.png`, `lesson-quiz-done-locked.png` | `LessonRoute`가 세 단계의 `locked`를 화면에 전달. 잠금 중 결과 subtree를 남색 안내로 교체해 버튼·링크·스크롤·시연 알약과 숫자 0. 해제 후 결과·리포트 페이지·액션 복원, 시연 패널은 닫힘 |
| 2.2 긴 메인 문장 | 추가 `lesson-done-long.png`, `lesson-done-checklist-long.png`, `lesson-report-long.png` | 명시 줄바꿈 네 줄과 Cloud 상한 90자/160자를 동시에 넣음. 결과 전용 글자 배치가 필요할 때 72→64→56→48→40→32 sp로 줄이며 줄 수 제한 없음. 전체 글자 layout·화면에서 보이는 높이·본문 32 sp 하한·주 버튼과 비중첩 검사. 줄바꿈 없는 160자도 검사. 기존 짧은 Headline의 최대 세 줄 규칙은 유지 |
| 2.3 점검 설명 | 교체 `lesson-report-checklist-bad.png`, `lesson-report-checklist-missing.png`; 함께 교체 `lesson-report-checklist.png` | 성공·실패·미측정의 설명 시제를 `checklistResults`에서만 정리. 도어·벨트·기어·브레이크/시동·등화의 값과 기호는 원래 판정 그대로. 벨트·시동 시각은 측정됐을 때만 표시 |
| 2.4 최대 누락 | 추가 `lesson-report-all-missing.png`, `lesson-report-all-missing-details.png`, `lesson-report-all-missing-scrolled.png`; 재현 근거 `lesson-report-all-missing-before.png`, `lesson-report-all-missing-details-before.png` | 변경 전 주차 키 8개·가이드 6개 누락을 먼저 재현. 고정 하단이 본문을 밀어내므로 긴 목록만 본문 스크롤로 이동. 변경 후 더 큰 점검 키 12개·가이드 7개 및 160자 총평을 주입해 마지막 목록까지 표시·스크롤 전후 배지와 돌아가기 좌표 고정 검사. 모든 키 누락 fixture는 **레이아웃 경계값**이며 실제 채점 결과를 뜻하지 않음 |
| 2.5 카테고리 | 교체 `lesson-setup-sheet.png` | 닫힌 주행 메뉴 글자 Muted 픽셀 검사. 준비 중 과제 칸의 면·회색·클릭 불가 유지 |
| 2.6 캡처 공백 | 추가 `lesson-briefing-checklist.png`, `lesson-briefing-knowledge.png`, `lesson-maneuver-hint.png`, `lesson-maneuver-evaluate.png`, `lesson-maneuver-checklist-locked.png`, `lesson-maneuver-checklist-mixed.png`, `lesson-report-parking-perfect.png`, `lesson-report-multiple.png`, `lesson-report-live7-sim1-fixture.png`, `lesson-report-live-missing-fixture.png`, `lesson-quiz-correct.png`, `lesson-quiz-long.png`, `lesson-panel-ai-no-config.png`, `lesson-panel-ai-ready.png`, `lesson-panel-ai-error.png`; 교체 `lesson-done.png` | 실제 시드·시나리오 및 화면용 fixture. 정답·긴 문항, AI 오류 80자도 표시하며 AI 상세의 기존 28 sp 유지. 긴 퀴즈는 기존 상향등 문항의 선택지·정답과 일치 |
| 2.7 정차 조작 | 교체 `lesson-report.png`, `lesson-quiz-done.png`, `lesson-certificate.png`, `lesson-demo-toggle-pill.png` | 리포트 요약·진단서와 QuizDone 재시작을 기존 driver 규칙(높이 140 dp·최소 폭 720 dp)으로. 시연 알약은 64×32 dp 외형·좌표 그대로, 터치 영역만 88×88 dp. 확대 영역 양 끝 모서리에 실제 포인터를 넣어 열기 확인 |

①에서 UI로 남긴 제안 띄어쓰기(`해 볼까요`·`익혀 볼까요`)도 완료해 `lesson-setup.png`를 교체했다. D1 숫자 메타·교육 수치, D2 정차 결과의 Signal 빨강, D3 패널 28 sp는 그대로다. 새 색 토큰·문자열 리소스·`FontWeight.Bold` 추가 없음. 차량·포트·채점·상태기계·데이터·빌드 파일·도구·`docs/NEXT.md`는 바꾸지 않았다.

지정 PowerShell 빌드 `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공, 단위 테스트 **215개(실패·오류·건너뜀 0)**. 앱 SHA-256 `01F0047193AD58E58846B2BDB5204F2D6E2C7E7DED6CC678B190630935B9D021`, 계측 SHA-256 `C3AB20F8F6F0AB07292FB39D36A9BC8C32042FC17481891E33AD78C2000D2B9B`.

[빌드 로그](build-round11.txt) · [최종 계측 3회 로그](contract-round11.txt). 동일한 최종 APK로 원본 `tools/lesson_shots.sh build/round11-verified-{1,2,3}` **3회 연속 Lesson contract passed**. 최종 캡처는 첫 실행에서 가져왔다(변경 전 재현 두 장 제외). Muted 픽셀은 Paper 배경과 합성해 비교하고, 알약은 확대 영역 양 모서리에서 실제 터치한다. 이전 계측 fixture의 점검 잠금 기본 과제명이 주차로 나오는 것을 바로잡은 뒤 세 번을 다시 실행했다.

원본 `bash tools/emu_flow.sh build/round11-flow` **PASS·uiautomator clashes 0**, 세션 시작부터 리포트까지 **104초**([흐름 로그](flow-round11.txt)). 필수 힌트 세 종류, 첫 회차 60/55·이동 4회, 두 번째 100/100·이동 2회·추가 힌트 없음, 도어 열림 → 리포트와 배지 0/8/0을 확인했다. 스크립트·힌트 기대 로그·채점은 수정하지 않았다. 위 목록은 교체 10장·추가 29장(변경 전 재현 2장 포함), 총 39장이다.

## 라운드 12 ① 주차 서두·시나리오 기하 (2026-10-02)

`codex/seed-verdict` · 기준 `origin/main=9f990a3` · 외부 CSTDe_API_34 · Fake 기본 배율 1.0. [발주서 ①](../../handoffs/2026-10-02_codex_ui_round12.md)의 데이터 **내용**과 관련 검증이다. 화면·계측·선택기·채점 규칙·빌드 파일·도구·NEXT는 바꾸지 않았다.

| 항목 | 교체·추가 캡처 | 근거 |
|---|---|---|
| 1.1 서두 | 교체 `lesson-done.png` | 실제 시드의 새 서두와 지표 기반 조언. 궤적은 기존 화면 계약 fixture |
| 1.2 시나리오 기하 | 추가 `lesson-done-seed-good.png`, `lesson-done-seed-bad.png` | 원본 `emu_flow`에서 실제 시나리오를 재생한 두 회차. 외부 Fake 신호이며 실차 관찰이 아님 |

주차 서두 **18개**를 숙련 밴드에 근거한 긍정/보완 문장으로 정리했다. 전체 33개의 한 문장·횟수/숫자 없음·`요.` 가드, 과제별 분리, 반복 회피 검사는 유지했다. `RemarkTemplate`의 밴드·태그·순서와 `RemarkPool` 선택 로직도 그대로다. 아직 선택기가 verdict를 받지 않으므로 `한 번에 들어갔어요`·`방향도 맞게 섰어요`를 활성 풀에 넣지 않았으며, 판정별 후보·필수 조건 필터·최근 문구 폴백·측정 판정 표현의 가드 예외를 [INTEGRATION C](../../INTEGRATION.md)에 요청했다. 이는 판정 직접 연계가 끝난 상태를 뜻하지 않는다.

좋은 주차는 첫 회전의 저속 유지 시간을 3.5초 늘렸고, 못한 주차는 마지막 회전의 중립 시점을 32초에서 27초로 앞당겼다. 두 회전 구간의 일정 속도도 기존 램프와 같은 0.5초 간격으로 주입해 긴 원호가 큰 직선 구간으로 적분되는 것을 피했다. 속도 크기·가감속 램프·기어 전환·안전 사건은 유지했다. 못한 주차의 변경 전 끝 방향은 현재 기준에서 **108.543°**로, 발주서의 약 71°와 달랐다. 판정 임계값을 바꾸지 않고 시나리오를 요구 범위로 맞췄다.

| 시나리오 | 길이 | 끝 방향 | 목표 대비 편차 | 이동 거리 | 숙련/안전·구간 | 판정 |
|---|---:|---:|---:|---:|---|---|
| 잘한 주차 | 29.5 s | 88.485° | 1.515° | 11.389 m | 100/100 · 2 | ONE_GO · ALIGNED · CLEAN · SAFE |
| 못한 주차 | 44.0 s | 73.421° | 16.579° | 17.144 m | 60/55 · 4 | ONE_FIX · SLIGHT · CLEAN · UNSAFE |

위 수치는 시드 타임스탬프를 실제 `ParkingRecorder`에 넣은 JVM 재생 결과다([원문](geometry-seed-verdict.txt)). `ParkingVerdictTest`에 ALIGNED/SLIGHT, `PathReconstructorTest`에 80~100°/65~80°를 고정했다. 기존 60/55·100/100, 이동 4/2·조향 왕복 3/1·전환 2/0, 근접/급제동, 힌트 단위 검사도 유지된다.

지정 PowerShell 빌드 `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공, 단위 테스트 **215개(실패·오류·건너뜀 0)**([빌드 로그](build-seed-verdict.txt)). 앱 SHA-256 `2654F1E3F9BC25B070E91047817397AEE5B3707060EE764431FDDEDDBBEA23CE`, 계측 SHA-256 `7760181FFECCA231CD3F25C08ECB84CE18CB2DC2CC8D044AD727A65E767B0564`.

동일 APK로 수정 없는 `tools/lesson_shots.sh build/seed-verdict-pass-{1,2,3}` **3회 연속 Lesson contract passed**([세 실행 로그](contract-seed-verdict.txt)). 기존 잠금·문구 길이·출처 목록·예약·픽셀·조향 도식 계약을 그대로 통과했다. 위 교체 캡처는 첫 실행 결과다.

원본 `tools/emu_flow.sh build/seed-verdict-flow` **PASS·uiautomator clashes 0**, 세션 시작부터 리포트까지 **109초**([흐름 로그](flow-seed-verdict.txt)). 필수 힌트 3종, 못한 주차 60/55·4구간 → 잘한 주차 100/100·2구간·추가 힌트 없음, 도어 열림 → 리포트와 배지 0/8/0을 확인했다. 두 실제 흐름 캡처를 추가하고 세 장 모두 문구·버튼·궤적의 잘림이 없는지 직접 확인했다. 캡처 총 **3장(교체 1·추가 2)**. ②의 네 줄 판정·각도 상세·조향 도식 캡처는 이 PR 범위에 포함하지 않는다.

## 라운드 12 ①′ 판정 서두 활성화 (2026-10-02)

`codex/seed-verdict-lines` · 기준 `origin/main=041155f`(#91·#92 머지 뒤) · 외부 CSTDe_API_34 · Fake 기본 배율 1.0. #91 C 절의 후보 네 문장을 #92의 판정 필수 필터에 연결하는 시드 **내용** PR이다. 제품 화면·선택기·채점·시나리오·빌드·도구·NEXT는 그대로다.

| 문장 | 필수 태그 | 밴드 |
|---|---|---|
| 한 번에 들어갔어요. | `one_go` | EXCELLENT · GOOD |
| 한 번 다시 넣고 들어갔어요. | `one_fix` | GOOD · OK |
| 여러 번 오가며 들어갔어요. | `many` | OK · ROUGH |
| 신호로 추정하면 방향도 맞게 섰어요. | `aligned` | EXCELLENT · GOOD |

8개 템플릿을 추가해 전체 서두는 **41개**다. 기존 33개 문구·밴드·태그와 순서는 유지했다. 전체 풀의 한 문장·숫자 없음·`요.` 종결 가드는 유지하며, 한글 횟수 표현은 요청된 두 진입 문장과 각 필수 태그의 조합에 한해서만 허용한다. 판정이 없거나 다른 경우와 최근 문구 회피 폴백을 실제 전체 시드로 반복 검사한다. 좋은 주차의 `ParkingRecorder → verdict → FakeCoachPort` 경로에서 첫 두 번의 선택은 `one_go`/`aligned` 문장이고, OK 밴드의 못한 주차는 `many`가 아닌 `one_fix` 문장이다.

| 항목 | 교체·추가 캡처 | 확인 내용 |
|---|---|---|
| ①′ / 1.1 | 교체 `lesson-done.png` | 못한 주차의 실제 판정으로 선택한 `one_fix` 서두. 궤적은 기존 화면 계약 fixture |
| ①′ / 1.1 | 추가 `lesson-done-seed-one-go.png`, `lesson-done-seed-aligned.png` | 잘한 주차 시드를 recorder로 재생하고 같은 코치 풀에서 두 판정 서두를 선택. 기존 Done 화면의 전문·버튼 비중첩 검사 |
| ①′ / 1.1 | 추가 `lesson-done-seed-many-fixture.png` | 못한 주차 지표의 이동 구간을 다섯 개로 설정한 **외부 레이아웃 fixture**. 점수와 MANY 판정을 다시 계산하고 선택된 문장을 표시. 실주행 시나리오나 실차 측정이 아니며 궤적은 넣지 않음 |
| ①′ / 실제 흐름 | 교체 `lesson-done-seed-good.png`, `lesson-done-seed-bad.png` | 원본 `emu_flow`의 두 실제 Fake 시나리오 결과. 판정에 맞는 새 서두 표시 |

지정 PowerShell 빌드 `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공, 단위 테스트 **220개(실패·오류·건너뜀 0)**([빌드 로그](build-seed-verdict-lines.txt)). 앱 SHA-256 `706F7CDD57D30E8F6F5E5185DFE60CDC91C1DD9A4F6EE9369682E68D21F8E812`, 계측 SHA-256 `82476273BB9DA3EFB803443E378DFD2EEECA1F20C063EC73E965B89F9673C483`.

같은 APK로 원본 `tools/lesson_shots.sh build/seed-lines-pass-{1,2,3}` **3회 연속 Lesson contract passed**([전체 로그](contract-seed-verdict-lines.txt)). 새 `Seed verdict openers` 검사와 기존 결과 잠금·긴 문장·출처 목록·예약·조향 도식 검사를 모두 통과했다. 네 문장별 캡처는 첫 실행에서 가져왔으며 전문·버튼·궤적을 직접 확인했다.

원본 `tools/emu_flow.sh build/seed-lines-flow` **PASS·uiautomator clashes 0·리포트까지 109초**([흐름 로그](flow-seed-verdict-lines.txt)). 못한 주차는 `한 번 다시 넣고 들어갔어요.`·60/55·4구간, 잘한 주차는 `신호로 추정하면 방향도 맞게 섰어요.`·100/100·2구간을 표시했다. 필수 힌트 3종·잘한 주차 추가 힌트 없음·도어 열림 → 리포트·배지 0/8/0 유지. 실제 흐름 두 캡처도 직접 확인했으며 총 **6장(교체 3·추가 3)**을 반영했다. ②의 판정 네 줄·방향 편차·조향 도식은 이 PR에 포함하지 않는다.

## 라운드 12 ② 판정 네 줄·조향 도식 (2026-10-02)

`codex/ui-round12` · 기준 `origin/main=76b14fe`(#91·#93 반영) · 외부 CSTDe_API_34 · Fake 기본 배율 1.0. [발주서 ②](../../handoffs/2026-10-02_codex_ui_round12.md)의 UI 범위만 구현했다.

| 항목 | 교체·추가 캡처 | 확인 내용 |
|---|---|---|
| 2.1 판정 네 줄 | 교체 `lesson-done.png`, `lesson-report.png`; 추가 `lesson-done-verdict-fix.png` | 실제 좋은 주차는 ✓ 네 개, 못한 주차는 ONE_FIX·SLIGHT·CLEAN·UNSAFE(△·△·✓·✗). Done은 궤적 아래 Ink 면, Report 요약은 왼쪽 아래 **마지막 회차의 판정** |
| 2.1 미측정·회차 선택 | 추가 `lesson-done-verdict-unknown.png`, `lesson-done-verdict-missing.png`, `lesson-report-verdict-last.png` | 방향 UNKNOWN/null verdict 표시 경계 fixture. 좋은 회차 다음에 못한 회차를 넣어 최고 점수와 마지막 판정을 혼동하지 않는지 확인 |
| 2.2 방향 편차 | 교체 `lesson-details.png`; 추가 `lesson-details-verdict-missing.png` | 실제 시나리오의 16.579°·1.515°를 17°·2°로 반올림, null은 미측정. 요약·Done의 네 줄에는 숫자 없음 |
| 2.3 조향 도식 | 교체 `lesson-maneuver-guides.png`; 추가 `lesson-maneuver-guides-left.png`, `lesson-maneuver-guides-straight.png` | 발주서의 오른쪽/왼쪽/중립 세 장. 기준 main에는 왼쪽/중립 파일이 없어 새로 추적한다. 앞바퀴 점선 둘·중앙 실선·뒷바퀴 점선 둘이 뒷차축 위 같은 회전 중심 사용 |
| 2.3 애니메이션 | 추가 [프레임 스트립](lesson-steering-round12-strip.png), [원본 조향 클립](lesson-steering-round12.mp4) | 첫 계약 실행의 5초 screenrecord. ffmpeg fps=10에서 0.9~1.6초 프레임을 왼쪽 패널로 크롭하고 시간 라벨만 붙였다. 350 ms 보간 중에도 바퀴와 호가 함께 움직임 |

PNG **12장(교체 4·추가 8)** 및 원본 클립 1개. 첫 전체 계약 실행 `build/round12-pass-1`의 캡처를 사용했다. 필수 항목인 Done 좋은/수정, Report, 자세히 보기, 조향 세 방향과 스트립을 눈으로 점검했다. 경계 fixture는 좋은 기록의 verdict만 UNKNOWN/null로 바꿔 표현을 검사하며, 실차 측정 또는 조향 누락 시나리오를 재현한 자료가 아니다.

`verdictLines`는 `AttemptRecord.verdict`의 72개 조합과 null을 네 줄로 옮기는 순수 함수다. 점수를 재판정하지 않는다. 방향이 측정되었을 때만 `신호로 추정` 꼬리표를 단다. ✓ Periwinkle·△ Paper 60%·✗ Signal을 사용하고, —는 Ink 위에서도 보이도록 Lavender 받침 위에 **기존 Muted**로 표시한다. 점검 결과에는 주차 판정을 넣지 않는다. 기존 Done/Report 잠금 분기 안에서 판정을 표시하며, 잠금 시 네 줄과 터치가 모두 사라지고 해제하면 복원된다.

조향 기하는 도식 폭 100 단위·기존 차체 비율에서 계산한다. 중앙 바퀴각을 뒷차축의 회전 반경으로 바꾸고 각 앞바퀴의 접선 각도를 따로 구한다. 최대 조향은 안쪽 45°가 되는 **공통 반경**을 제한해 접선을 유지한다. 타이어는 화면 좌표에서 회전해 차체의 가로/세로 배율 차이가 각도를 왜곡하지 않게 했다. 중립 앞바퀴 중심은 차체 윤곽으로부터 폭의 1/6 안쪽(폭의 약 1/3이 밖), 뒷바퀴 궤적은 Lavender 40%. 모두 설명용 도식이며 실차 치수·보정값은 아니다. 공유 회전 중심·접선·좌우 대칭·중립 근처 연속성은 단위 테스트, 실제 앞쪽 곡률 차이와 뒷바퀴 점선은 픽셀 검사로 확인한다.

지정 PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공, 단위 테스트 **226개(실패·오류·건너뜀 0)**. [빌드 로그](build-round12-ui.txt). 앱 SHA-256 `268446A9F588636C950ADD7F27B9FEC15398510E7EB03708E01C1D11F422CCFF`, 계측 SHA-256 `81DDA588C4679FE4F324FA03A7ADEE5375AB7C87BE384F8A8EF2797F87F7E1AA`.

차량·포트·채점·상태기계·시드·빌드 파일·tools·문자열 리소스·NEXT는 변경하지 않았다. 새 색 토큰·`FontWeight.Bold` 추가 0. 기존 잠금, 90/160자 결과 문장, 140 dp 결과 버튼, 88 dp 시연 알약, 최대 누락 목록 계약도 유지한다.

동일한 최종 APK로 원본 `tools/lesson_shots.sh build/round12-pass-{1,2,3}` **3회 연속 Lesson contract passed**([계약 로그](contract-round12-ui.txt)). 원본 `tools/emu_flow.sh build/round12-flow`도 **PASS·uiautomator clashes 0·리포트까지 110초**([흐름 로그](flow-round12-ui.txt)): 첫 회차 60/55·4구간, 두 번째 100/100·2구간, 필수 벨트/근접/급제동 힌트·좋은 주차 추가 힌트 없음·도어 열림→리포트·배지 0/8/0을 확인했다. 위 고정 캡처의 0/7/1은 시나리오만 직접 재생해 도어 신호를 주지 않은 계측 기록이고, 실제 전체 흐름에서는 도어 신호까지 받아 0/8/0이다.

## 라운드 13 ③ 서두 정리·지식 퀴즈 10문항 (2026-10-03)

`codex/seed-round13` · 기준 `origin/main=11be5f6`(#117 이후) · 외부 CSTDe_API_34 · Fake 기본 배율 1.0. ①·②와 같은 main에서 독립 분기한 시드 **내용** PR이다. 제품 UI·채점·태그 필터·최근 문구 회피·상태기계·타입·필드·차량·포트·build·tools·NEXT는 그대로다. 리뷰·머지 순서는 ① → ② → ③이다.

EXCELLENT/GOOD의 `aligned` 단독 서두 두 개만 제거해 전체 서두는 39개다. `one_go`가 실제 좋은 주차의 첫 선택이며, 좋은 주차가 연속되면 기존 최근 문구 회피에 따라 일반 서두로 돌아간다. 판정 필터·다른 밴드 폴백 검사는 그대로 유지한다. 과거 라운드 12 ①′의 `lesson-done-seed-aligned.png`는 당시 기록이며 현재 선택되는 문장이 아니다.

기존 다섯 문항의 순서·선택지·정답·해설·교육 수치는 유지하고 아래 다섯 개를 뒤에 추가했다. 모두 3지선다·유일 ID·유효 정답·해설 `요.` 종결이며 새 숫자는 없다. 단위 검사는 10문항 완주(9정답)·다섯째에서 계속·열째에서 결과·중도 종료의 전체 수·주행 중 답변 잠금·저장을 확인한다.

| 문항 | 내용 | 확인한 공식 근거(2026-10-03) |
|---|---|---|
| 6 `parking-shift-stop` | 완전히 멈추고 브레이크를 밟은 뒤 D→R | [기아 2026 셀토스 설명서, IVT 변속 위치/잠금](https://ownersmanual.kia.com/full_webhelp/SP2/2026/ko_KR/topics/chapter5_5_1.html) |
| 7 `parking-brake` | 주차 기어와 주차 브레이크 함께 사용 | [같은 설명서, P(주차)](https://ownersmanual.kia.com/full_webhelp/SP2/2026/ko_KR/topics/chapter5_5_1.html) |
| 8 `blocked-green` | 초록불이어도 교차로 안에 멈춰 통행을 막을 상황이면 진입 전 대기 | [도로교통법 제25조 제5항](https://www.law.go.kr/lsLawLinkInfo.do?chrClsCd=010202&lsJoLnkSeq=1000720047) |
| 9 `crosswalk-yield` | 횡단 중이거나 횡단하려는 보행자 앞 일시정지 | [도로교통법 제27조 제1항](https://www.law.go.kr/lsLinkCommonInfo.do?lsJoLnkSeq=1000188979) |
| 10 `highway-entry-priority` | 일반 차량 진입 시 본선 차량에 양보 | [도로교통법 제65조](https://www.law.go.kr/lsLawLinkInfo.do?chrClsCd=010202&lsJoLnkSeq=1000719974) |

법령은 확인 시점에 시행 중인 2026-07-01 시행본을 기준으로 새 문항을 작성했다. 계측은 전체 문항을 선택·해설 확인 후 넘겨 마지막에만 `결과 보기`를 누르며, 새 해설 전문과 버튼의 비중첩도 검사한다.


| 항목 | 교체·추가 캡처 | 확인 내용 |
|---|---|---|
| 퀴즈 진행·해설 | 교체 `lesson-quiz.png`, `lesson-quiz-answered.png`, `lesson-quiz-correct.png`, `lesson-quiz-locked.png` | 전체 수 10, 기존 정오답·해설·잠금 계약 유지 |
| 새 퀴즈 다섯 개 | 추가 `lesson-quiz-round13-6.png` ~ `lesson-quiz-round13-10.png` | 문항·선택지·정답·해설 전문, 마지막 문항의 결과 보기 |
| 퀴즈 결과 | 교체 `lesson-quiz-done.png` | 10문항 결과 목록과 다시 시작, 정답 요약 10문제 중 9개 |
| 서두 선택 | 교체 `lesson-done-seed-one-go.png`, 추가 `lesson-done-seed-repeat.png` | 실제 좋은 시드의 one_go 우선과 같은 코치 풀에서 반복 시 일반 서두로 복귀. 판정 네 줄 유지 |
| 실제 후면 흐름 | 교체 `lesson-done-seed-bad.png`, `lesson-done-seed-good.png` | 못한 주차 one_fix → 잘한 주차 one_go 서두, 고정 점수·판정 유지 |

최종 계약 첫 실행에서 퀴즈 10장과 서두 2장, 원본 후면 시연에서 결과 2장을 반영한다(PNG 총 14장: 교체 8·추가 6). 새 문항 다섯 개의 질문·선택지·해설과 열째 결과 버튼, 기존 정오답/잠금/결과 화면, 일반 서두의 전문을 직접 확인했다. 후면 조향 도식과 Done 잠금의 앱 영역은 기존 캡처와 픽셀이 완전히 동일하다.

지정 PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공, 단위 테스트 **235개(실패·오류·건너뜀 0)**. [빌드 로그](build-seed-round13.txt). 기존 테스트를 10문항과 두 템플릿 제거에 맞게 확장했으며 판정 필터 테스트는 유지했다. 앱 SHA-256 `C4C076A1334DDE9E1031C0D82DE4F46445693FFF0C7B934C50AF3433E55199EF`, 계측 SHA-256 `E22797418D947E59CAFFDC7DD0A0BE6410FDBBF6B5FFDD0DACDDB370F151DC8C`.

동일 APK로 원본 `tools/lesson_shots.sh build/seed-round13-final-{1,2,3}` **3회 연속 Lesson contract passed**([계약 로그](contract-seed-round13.txt)). 전체 10문항, 잠금·터치·결과와 기존 판정·도식·예약 계약을 모두 통과했다.

원본 `tools/emu_flow.sh build/seed-round13-flow` **PASS·clashes 0·112초**([흐름 로그](flow-seed-round13.txt)): 첫 회차 60/55·4구간·one_fix → 둘째 100/100·2구간·one_go, 필수 벨트/뒤 근접/급제동 힌트 3종·좋은 주차 추가 힌트 0·도어→Report·배지 0/8/0 유지. 두 실제 Done 캡처에서도 서두·판정 네 줄·궤적·버튼을 확인했다. ① 질감/② 전면 UI는 이 독립 브랜치에 포함되어 있지 않다.
## 라운드 13 ② 전면 직각 주차 화면 (2026-10-03)

`codex/ui-front-parking` · 기준 `origin/main=11be5f6`(#117 이후) · 외부 CSTDe_API_34 · Fake 기본 배율 1.0. ① 질감 B와 같은 main에서 독립 분기했으므로 아래 캡처는 기존 평면 질감이다. 리뷰·머지 순서는 ① → ② → ③이며, 후면 본편 캡처는 교체하지 않았다.

| 항목 | 추가 캡처 | 확인 내용 |
|---|---|---|
| 시트 | `lesson-setup-sheet-front.png` | 현행 전면 카드·중 난이도·힌트 선택과 시작 콜백 유지 |
| 도식 D/R | `lesson-maneuver-front.png`, `lesson-maneuver-front-fix.png` | 앞 유리가 위, 바퀴와 보조선 함께 회전, 전진 화살표 위·후진 보정 화살표 아래. 뒤 거리 칸·접근성의 뒤 거리 미측정 문구 제거, 기어 칸 폭 재배치 |
| 잠금 | `lesson-maneuver-front-locked.png` | 5.1 km/h에서 도식·터치 없음, 기존 평면 잠금 유지 |
| 완료 판정 | `lesson-done-front.png`, `lesson-done-front-good.png` | 실제 전면 시드를 recorder에 재생한 △△✓✗ / ✓✓✓✓, 원좌표 방향의 추정 궤적과 차 뒤쪽이 열린 도착 칸 |
| 궤적 재생 | `lesson-done-front-replay-d.png`, `lesson-done-front-replay-r.png` | 방향별 외부 2점 레이아웃 fixture에서 D 앞쪽·R 뒤쪽 화살표. 실제 주행 측정 캡처가 아님 |
| 리포트 | `lesson-report-front.png`, `lesson-details-front.png` | 마지막 회차 판정·배지 0/7/0, 상세의 앞 근접 1회/0회. 뒤 거리 항목 없음 |

PNG 10장을 추가하고 전문·차 방향·U자 열린 쪽·버튼·배지의 잘림을 직접 확인했다. 전면용 `LessonScreenInstrumentation` 묶음은 시트 선택→D/R 도식 픽셀·뒤 거리 노드 없음→잠금→Done 판정 네 줄·도착 칸 픽셀→Report 상세까지 검사한다. 뒤 거리의 MISSING은 전면의 공통 출처 판정에서도 제외하며, 적용 대상 신호의 혼합/누락은 기존대로 따로 표시한다. 시드·타입·필드·판정 규칙·차량·포트·상태기계·build·tools·NEXT는 변경하지 않았다. 새 문자열 리소스·색상 토큰·Bold 추가 0.

후면 도식 오른쪽/왼쪽/중립은 기존 캡처와 앱 영역을 비교해 기하·색상·문구가 동일하며, 차체 가장자리에서 채널값 1 이내의 안티앨리어싱 차이만 확인했다(오른쪽 16픽셀). `lesson-done-locked.png` 앱 영역은 완전히 동일하다. 기존 후면 도착 칸·조향·잠금·숫자·터치 계약도 통과했다.

지정 PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공, 단위 테스트 **238개(실패·오류·건너뜀 0)**. [빌드 로그](build-round13-front.txt). 앱 SHA-256 `64232449FA28E60E6BA651228954C88095AD7217E0E4F185D7C847FFB89053F4`, 계측 SHA-256 `E734BB9F8927211B69DC6400DF36F1A710798A1170A4725E38C116E83383EA4B`.

동일 APK로 원본 `tools/lesson_shots.sh build/front-final-{1,2,3}` **3회 연속 Lesson contract passed**([계약 로그](contract-round13-front.txt)). 캡처는 첫 실행 결과다.

원본 `tools/emu_flow.sh build/front-rear-flow` **PASS·clashes 0·113초**([후면 로그](flow-round13-front-rear.txt)): 60/55·4구간 → 100/100·2구간, 필수 벨트/뒤 근접/급제동 힌트·좋은 주차 추가 힌트 0·도어→Report·배지 0/8/0 유지.

전면 전체 흐름도 **PASS·clashes 0·112초**([전면 로그](flow-round13-front.txt)). 시트 `주차 → 전면 직각 주차 → 힌트 → 시작 → 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 다 됐어요 → 문 열기` 순서다. 원본 emu_flow를 수정하지 않고 무시되는 `build/front_flow.sh` 사본에 선택 단계·앞 근접 힌트·배지 7 검사만 적용했다. 첫 회차 `attempt 1: skill=60 safety=55 segments=4 badge=AvailabilityBadge(live=0, simulated=7, missing=0)`, 둘째 `attempt 2: skill=100 safety=100 segments=2 badge=AvailabilityBadge(live=0, simulated=7, missing=0)`를 확인했다. 벨트·앞 근접·급제동 힌트 3종과 좋은 주차 추가 힌트 0, 운전석 문 열기→Report가 유지된다. 실제 차량의 방향 없는 근접 경고 해석과 Real 7키 완주는 사내 확인 ⑦ 대상이다.

## 라운드 13 ① 질감 B (2026-10-03)

`codex/texture-b` · 기준 `origin/main=11be5f6`(#117 이후) · 외부 CSTDe_API_34 2560×1440 · Fake 기본 배율 1.0. [발주서 ①](../../handoffs/2026-10-03_codex_ui_round13.md)의 면 질감만 적용했다.

| 대상 | 교체·추가 캡처 | 확인 |
|---|---|---|
| 카드·선택 칩 | 교체 [과제 시트](lesson-setup-sheet.png) | READY 카드 10%·y 6·blur 16, 칩 10%·y 3·blur 8. 선택 칩은 Periwinkle 색 그림자, 선택 카드 하이라이트 14%. 준비 중 카드와 글자만 있는 카테고리 메뉴는 기존 표현 유지 |
| 주 버튼·판정 | 교체 [Done](lesson-done.png), [Report](lesson-report.png) | 버튼 Signal 28%·y 10·blur 22, 판정 Ink 22%·y 12·blur 28와 위선 Paper 12%. 네 판정의 문구·기호·색 유지 |
| 점검 패널 | 교체 [점검 Done](lesson-done-checklist.png) | 일곱 줄과 배치·색을 유지하며 패널 면에만 같은 효과 |
| 눌림 | 추가 [누른 버튼](lesson-texture-button-pressed.png) | 실제 포인터 DOWN 중 외부 그림자 농도가 절반, CANCEL은 콜백 0회 |
| 잠금 | 추가 [평면 잠금](lesson-texture-locked-flat.png) | 잠금 레이어는 원래 Ink 단색·터치 0. 기존 잠금 PNG는 교체하지 않음 |

효과 값은 `CoachStyle.CoachTexture` 한 곳에 모았다. 블러 마스크는 크기가 바뀔 때만 캐시하고 눌림은 그리는 알파만 바꾼다. 하이라이트 영역은 버튼·칩 높이의 46%, 카드 38% 안이며 글자에 닿기 전에 투명해진다. 글자 뒤는 기존 Signal 그대로여서 Paper/Signal 대비 **3.820:1**을 유지한다. 면 아래쪽만 Ink 6%(카드·칩)·18%(버튼)의 얕은 안쪽 그림자다. 바탕색을 바꾸는 추가 그라데이션은 넣지 않았다. 선택 카드는 발주서의 Ink 표기와 달리 현행 Periwinkle이므로, 색 불변 조건을 우선해 그대로 유지했다. 새 글자·리소스·글자 크기·도식·시연 패널 변경 없음.

지정 PowerShell 빌드 `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공, 단위 테스트 **235개(실패·오류·건너뜀 0)**. [빌드 로그](build-texture-b.txt). 같은 최종 APK에서 수정 없는 `tools/lesson_shots.sh build/texture-final-{1,2,3}` **3회 연속 Lesson contract passed**([계약 로그](contract-texture-b.txt)). 초기 검증에서 기존 단색 윗면 검사와 새 검사 좌표가 각각 실패했으며, D7 상한 검사와 글자 옆 배경 좌표로 수정 후 위 세 실행을 다시 했다.

앱 SHA-256 `2E33CB4C66302B5378253680FA6A82B2406501A166DFBA9B5F74D540D1F73971`, 계측 SHA-256 `8DF71A2805D5DF7E1577BF1827E1B29B451EFD37E94BC7DC05F77419FE437D78`. 최종 6장 모두 첫 PASS 실행에서 가져왔다. 조향 가이드·Done 잠금은 기존 PNG와 앱 영역 픽셀이 동일하다. Maneuver 잠금의 차이는 기존 PNG의 옛 시드 문장 `돌리세요`와 현재 main의 `돌려 주세요` 한 곳이며 잠금 배경·도식 비노출·조작 0은 그대로다. 중간 진단 실행의 Report PNG는 판정 글리프가 빠진 프레임이라 사용하지 않았고, 최종 첫 실행의 네 줄이 모두 보이는 캡처를 확인했다.

차량·포트·채점·상태기계·데이터·빌드 파일·tools·NEXT 변경 없음. 사내 디스플레이에서 질감이 적절한지는 라운드 13 머지 후 다음 태그의 재검증 ⑦ 대상이다.

원본 `bash tools/emu_flow.sh build/texture-flow` **PASS·uiautomator clashes 0·리포트까지 112초**([흐름 로그](flow-texture-b.txt)). 못한 주차 60/55·4구간 → 잘한 주차 100/100·2구간, 필수 벨트/뒤 근접/급제동 힌트 세 종류·좋은 주차 추가 힌트 0·도어→리포트·배지 0/8/0을 유지했다.
