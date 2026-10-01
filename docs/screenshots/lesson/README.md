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
