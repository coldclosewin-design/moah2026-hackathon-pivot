# 화면 검증 캡처

## 동승자 공유 (2026-09-28)

`codex/companion-share` · 기준 `origin/main=98ec8f8`(선행 모델 PR #41 머지) · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. [동승자 공유 발주서](../../handoffs/2026-09-28_codex_companion_share.md) §2 구현.

- `lesson-companion.png`: 원본 `emu_flow.sh`에서 못한 주차 → 잘한 주차를 마친 실제 두 회차 리포트의 동승자 탭. 칭찬·도움말, 예시 고지, 기본 총평만, 응원 세 개와 다시 시작을 보여 준다.
- `lesson-setup-cheer.png`: 같은 실제 세션에서 `오늘도 천천히 가요`를 고르고 다시 시작한 Setup. 프로필 위에 `동승자 · 오늘도 천천히 가요`가 표시된다.

`build-companion.txt`: PowerShell `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 단위 테스트 **164개, 실패·오류 0**.

`contract-companion.txt`: 수정 없는 `tools/lesson_shots.sh build/companion-contract-complete`에서 **Lesson contract passed**. 동승자 액션 순서·두 문장·예시 고지·숫자/점수 비노출, 공유 범위와 응원 각각 단일 선택, 모든 콜백의 인자·횟수, 탭 재진입 선택 유지, 칩 96dp·내용 폭·한 줄·선택 색, 다시 시작 140dp × 720dp 이상, 긴 조언의 옵션 노출을 검사했다. Setup은 응원 있음/없음, 프로필 위의 Periwinkle 눈썹, 시트·Briefing 비노출, 응원이 없을 때 첫 렌더 배치가 같은지 확인했다. 기존 잠금·패널·주 버튼·시트·퀴즈·Done/Report 계약도 통과했다.

전체 계측 캡처는 `build/companion-contract-complete/`에 있다. `lesson-companion-selected.png`, `lesson-companion-long-note.png`, `lesson-setup-cheer.png`를 눈으로 확인했다. 공유 범위 여백을 줄여 72sp 조언이 세 줄이어도 마지막 설명이 하단 버튼에 가려지지 않게 했다.

정적 확인: `ui/`의 `FontWeight.Bold` 0, `Color(0x`는 `CoachStyle.kt`에만 있다. 문자열 리소스는 `lesson_companion` 한 개만 추가했다. 차량·포트·채점·상태기계·데이터·빌드 파일·`tools/` 변경은 없다.

`flow-companion.txt`: 수정 없는 `tools/emu_flow.sh build/companion-flow` **PASS**, uiautomator 충돌 **0**. 못한 주차 60/55·이동 4회·필수 힌트 세 종류 → 잘한 주차 100/100·이동 2회·추가 힌트 없음 → 도어 열림 후 리포트(세션 시작부터 **132초**) → 동승자 탭 → 첫 응원 선택 로그 → 다시 시작한 Setup에 응원이 보이는 전체 경로를 확인했다. 원본 캡처는 `build/companion-flow/`에 있다.

기존 추천 문구의 `가이드를 0번 통과했어요`는 `feature/lesson/ProgressStore.kt`의 `ModeAdvisor`에서 온다. Setup 캡처와 자막에도 그대로 보이며, 발주서의 수정 금지 영역이므로 [INTEGRATION C절](../../INTEGRATION.md#c-요청-codex--claude--claude--codex)에 후속 수정 요청을 기록했다.

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
