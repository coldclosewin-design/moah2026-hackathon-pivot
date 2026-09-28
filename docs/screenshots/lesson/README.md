# 화면 검증 캡처

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
