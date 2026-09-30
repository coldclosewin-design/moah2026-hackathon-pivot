# Codex 오더 — 시연 패널 `AI 코치` 줄 + `AI 연결` 버튼 (2026-10-01, 사내 이관 1차 작업 D 의 화면 부분)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/07_two_site_workflow.md(사내 로그인 함정)를 읽어라.
브랜치: codex/ai-panel 를 origin/main 에서 새로 만들어 작업(라운드 9 PR 과 독립 — 겹치면 먼저 머지된 쪽에 리베이스).
작업: 시연 패널(`DemoPanel.kt`) 맨 아래에 `AI 코치 · <상태>` 한 줄 + 상세 한 줄 + `AI 연결` 버튼(NeedsLogin·Error 일 때만). 매퍼 `aiLine(state)` 순수 함수 + 단위 테스트.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(원본 — 패널 알약·시나리오 버튼 위치 불변), docs/screenshots/lesson/lesson-panel-open.png 교체 + lesson-panel-ai-code.png(Code 상태) 추가, gh pr create.
```

## 0. 왜

사내 이관 1차(9/30): AI 코치는 Cloud Copilot 뿐이고(온디바이스 보드 미제공) 로그인은 device code 방식이라 **사용자가 GitHub 에서 코드를 넣고 Authorize 까지 눌러야** 한다(15분 만료). 그 코드·주소를 시연 패널에서 보여 주고, 버튼 하나로 흐름을 시작한다. 전송 계층·인증·상태는 Claude 가 넣었다(PR #64 예정: `ports/copilot/`).

## 1. 데이터 (이미 있음)

- `LessonViewModel.DemoControls.aiState: StateFlow<CopilotAuth.State>?` — **null 이면 AI 줄 자체를 그리지 않는다**(빌드 플래그 `CLOUD_COACH=false`).
- `CopilotAuth.State`: `NoConfig`(기기에 설정 파일 없음) / `NeedsLogin` / `Code(userCode, uri)` / `Ready` / `Error(message)`.
- `DemoControls.connectAi()` — device code 흐름 시작. Ready 면 아무 일도 없다.

## 2. 매퍼 `aiLine(state): AiLine` (`ui/lesson/LessonPresentation.kt`, 순수 함수)

| 상태 | 제목(32 sp) | 상세(28 sp `Muted`) | 버튼 |
|---|---|---|---|
| null | (줄 없음) | | |
| `NoConfig` | `AI 코치 · 설정 없음` | `copilot_config.json 없음 — 시드 문장으로 말해요` | 없음 |
| `NeedsLogin` | `AI 코치 · 로그인 필요` | `GitHub 에서 코드를 넣고 Authorize 까지 눌러 주세요` | `AI 연결` |
| `Code(code, uri)` | `AI 코치 · 코드 입력 중` | **`{uri}  {code}`** — 주소와 코드를 **그대로**(사람이 읽고 옮겨 적는다), 코드는 40 sp `Ink` | 없음(진행 중) |
| `Ready` | `AI 코치 · 연결됨` | `회차 멘트·총평을 Copilot 이 써요` | 없음 |
| `Error(msg)` | `AI 코치 · 오류` | `msg` 그대로(80자에서 자름) | `AI 연결` |

`data class AiLine(val title: String, val detail: String, val showConnect: Boolean)`. 테스트: 다섯 상태 + null.

## 3. 화면 (`DemoPanel.kt`)

- 패널 맨 아래, 기존 `시나리오 정지` 아래에 `PosterRule` → 제목 줄 → 상세 줄 → (조건) `AI 연결`(`TextAction`). 패널 폭·알약 위치·시나리오 버튼 위치는 그대로(`emu_flow`·`lesson_shots` 가 좌표 대신 글자로 찾지만 첫 렌더 계약은 유지).
- `Code` 상태의 코드는 복사할 수 없는 화면이라 **글자 크기 40 sp 이상**. 주소는 짧게(`github.com/login/device`) 보여도 된다 — `uri` 에서 `https://` 를 뗀다.
- 상태는 `aiState` 를 `collectAsState()` 로. 버튼은 `vm.demo?.connectAi()`.

## 4. 불변

| 규칙 | 검사 |
|---|---|
| `aiState == null` 이면 AI 관련 노드 0 | lesson_shots(추가) |
| 다섯 상태 텍스트, `AI 연결` 은 NeedsLogin·Error 에서만 | 단위 테스트(매퍼) + lesson_shots(렌더 2개: NeedsLogin·Code) |
| 패널 첫 렌더·알약·시나리오 버튼 계약 | 기존 |
| `ui/` `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 만, 새 문자열 리소스 ≤ 1(`AI 연결`) | grep |
