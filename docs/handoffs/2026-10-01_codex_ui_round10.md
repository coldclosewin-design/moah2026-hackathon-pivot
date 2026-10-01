# Codex 오더 — UI 라운드 10: 사내 검증 #2 + 라운드 9 리뷰 사소 묶음 (2026-10-01)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md(커밋 규칙 8 추가됨), 이 발주서를 읽어라.
브랜치: codex/ui-round10 을 origin/main(#68~#72 머지 뒤)에서 새로 만들어 작업. PR 하나 = 이 발주서 하나(scope ui).
작업: 아래 ⓐ~ⓓ 네 가지. 전부 작아서 한 PR.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 구조, build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell — 증분 빌드에서 kotlin stdlib NoClassDefFoundError 가 나면 clean 뒤 다시), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS, 캡처 lesson-briefing.png·lesson-panel-ai-code.png 교체, gh pr create(제목 = squash 제목 `ui: …` 50자 안팎, 본문 = 불릿 2~5 + 마지막 `검증:` 줄).
```

## ⓐ 브리핑 제목 3항목의 `과` 중복 (`ui/lesson/LessonPresentation.kt` `briefingHeadline`)
- 지금: `핸들 방향과 기어 전환과 뒤 거리를 볼게요.` — 2·3항목에서 `과`/`와` 가 항목마다 붙는다.
- 바꿀 것: 마지막 앞 항목만 `과/와`(`withAndParticle`), 그 앞은 `, ` 로 — `핸들 방향, 기어 전환과 뒤 거리를 볼게요.` 2항목은 지금대로(`A과 B를`). 단위 테스트의 기대 문자열 갱신.

## ⓑ 계측 `takeScreenshot()` null 재시도 (`androidTest/.../LessonScreenInstrumentation.kt`)
- 지금: `checkNotNull(uiAutomation.takeScreenshot())` 이 세 곳(`captureSetupMorph` 프레임, 클립, `capture`). 10/1 리뷰에서 5회 중 2회 첫 캡처가 null 로 NPE(`UiAutomation.java:1181`, 에뮬 플레이크).
- 바꿀 것: `screenshot()` 헬퍼 하나 — null 이면 50 ms 쉬고 최대 3회, 끝내 null 이면 지금처럼 `checkNotNull` 메시지로 실패. 세 곳 모두 이 헬퍼로. 모핑 프레임 캡처(0/100/200/300/400 ms)는 재시도로 시각이 밀리면 안 되므로 **첫 프레임 전**에 한 번 워밍업 캡처를 버리는 방식도 허용.

## ⓒ `DemoPanel` `Code` 상태의 `Text` 스타일 (`ui/lesson/DemoPanel.kt`)
- 지금: `Code` 상태만 `material3.Text(buildAnnotatedString…)` 를 직접 써서 `LessonText` 의 `ko-KR` 로케일·Phrase 줄바꿈 스타일이 빠진다.
- 바꿀 것: `LessonText` 에 `AnnotatedString` 오버로드(같은 스타일)를 두고 그것을 쓴다. 코드 40 sp `Ink` 스팬은 유지.

## ⓓ 패널 코드 상태의 주소를 `https://` 포함 그대로 (`ui/lesson/DemoPanel.kt`)
- 사내 검증 #2: 사람이 화면을 보고 주소를 옮겨 적는다 — `removePrefix("https://")` 를 없애고 `state.uri` 그대로. 10/1 발주서 §3 의 "떼도 된다" 를 뒤집는 것. 계측 `detail` 기대값(`github.com/login/device  ABCD-1234` → `https://github.com/login/device  ABCD-1234`)과 캡처 `lesson-panel-ai-code.png` 교체. 28 sp 로 한 줄에 들어가는지 캡처로 확인(안 들어가면 주소 줄·코드 줄 두 줄).

## 불변
| 규칙 | 검사 |
|---|---|
| 패널 폭·알약·시나리오 버튼 좌표, 첫 렌더 계약 | 기존 계측 |
| `ui/` `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 만, 새 문자열 리소스 0 | grep |
| 운전자 문장에 숫자 없음 | 기존 |
