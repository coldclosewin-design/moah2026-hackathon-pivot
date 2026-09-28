# 오더 — 동승자 공유: 옆자리 사람을 코치에서 응원자로 (2026-09-28)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/topics/01_driving_coach.md §3.4·§3.5, 이 발주서, 라운드 6 발주서(주 버튼·시트 규칙)를 읽어라.
브랜치: codex/companion-share 를 origin/main 에서 새로 만들어 작업(§1 Claude 선행 PR 이 머지된 뒤 — NEXT Step 12 에 표시).
작업: Report 에 세 번째 탭 `동승자` (동승자에게 건네는 두 문장 + 공유 범위 + 응원 한마디 고르기) 와, 다음 세션 Setup 첫 화면에 그 응원 한마디를 눈썹으로. 실제 전송은 없다(진단서와 같은 "예시" 원칙).
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(원본 그대로),
          docs/screenshots/lesson/ 에 lesson-companion.png · lesson-setup-cheer.png 추가, gh pr create.
```

## 0. 왜 · 무엇

이 앱의 이름은 "화내지 않는 조수석" 이다 — 옆자리 사람이 기억과 감정으로 실시간 피드백을 주던 자리를 앱이 데이터로 대신한다. **동승자 공유**는 그 사람을 내쫓는 게 아니라 **응원자로 자리를 바꿔 주는** 기능이다(덱 12장 확장 항목, §3.5 수신자 셋 중 "동승자").

동승자에게 가는 것은 점수가 아니다. 세 가지다:
1. **오늘 잘한 것 하나** — 운전자 회차 멘트와 같은 규칙(숫자 없음), 단 **동승자에게 하는 말**("오늘은 한 번에 들어갔어요").
2. **다음에 옆에서 도울 것 하나** — `AdviceRules` 의 조언을 동승자 행동으로 바꾼 것("출발 전에 벨트 같이 확인해 주세요").
3. **응원 한마디** — 동승자가 세 문장 중 하나를 고르면 **다음 세션 Setup 첫 화면**에 눈썹으로 뜬다. 옆자리 사람의 말이 앱의 첫 줄이 된다.

공유 범위는 진단서와 같은 구조(3단계)지만 라벨이 다르다(§2). 실제 전송·계정·네트워크는 없다 — 화면에 "예시입니다 — 실제 전송은 없습니다" 를 진단서 탭과 같은 자리에 쓴다.

## 1. Claude 선행 (데이터·코치·상태기계) — ✅ 완료(브랜치 `claude/companion-model`, 아래 표대로 구현 · 단위 테스트 164) → Codex 시작 가능

| # | 무엇 | 어디 |
|---|---|---|
| A1 | `CompanionShareLevel { SUMMARY("총평만", "잘한 것·도울 것 두 문장"), PROCESS("과정까지", "회차별 궤적과 힌트 이력"), FULL("진단서 전체", "항목별 수치까지") }` | `feature/lesson/LessonModels.kt` |
| A2 | `CompanionNote(praise: String, help: String)` — 두 문장(`text` = "praise\nhelp"). `CoachPort.companionNote(task, attempts, profile)`; `FakeCoachPort` 는 `CompanionRules.note`: praise = **최고 회차** 밴드별 풀 `CompanionRules.PRAISE`(주차 4밴드 × 2, 점검 × 1, 숫자 없음), help = **마지막 회차**의 `AdviceRules.pick(...)` → `Advice.companion`(`AdviceRules.Advice` enum 이 운전자 문장·동승자 문장을 쌍으로 가짐: 벨트 → "출발 전에 벨트 같이 확인해 주세요." / 급제동 → "멈추기 전에 '천천히' 한 마디만 해 주세요." / 근접 → "뒤를 같이 봐 주세요. 가까우면 손으로 알려 주세요." / 조향·기어·구간 → "핸들 타이밍은 말없이 기다려 주세요." / P → "다 들어오면 기어 P 까지 같이 확인해 주세요." / 없음 → "오늘은 그냥 잘했다고 해 주세요."). `CloudCoachPort.companionNote` 는 프롬프트(`CoachPrompts.companion`, 120자) + 검증 + 두 줄이 아니면 폴백 | `ports/CoachPort.kt`(`AdviceRules.Advice`·`CompanionRules`), `ports/CloudCoachPort.kt`, `CompanionRulesTest`·`CloudCoachPortTest` |
| A3 | `LessonReport.companion: CompanionNote`, `LessonReport.companionShareLevels: List<CompanionShareLevel>`, `LessonReport.cheers: List<String>`(시드 3: "오늘도 천천히 가요" / "지난번보다 나아졌어요, 내가 봤어요" / "다음엔 내가 옆에서 조용히 있을게요") | `LessonModels.kt`, `data/SeedCatalog.kt`(문구는 Codex 가 다듬음) |
| A4 | `LessonStateMachine.shareWithCompanion(level: CompanionShareLevel)` → 로그 `companion: share=<LEVEL>`(전송 없음), `cheer(text: String)` → `ProgressStore.cheer = text` → 다음 `Setup` 의 `LessonPhase.Setup.cheer: String?`. `LessonViewModel` 에 두 진입점. `reset()` 은 cheer 를 지우지 않는다(다음 세션에 보이는 게 목적) | `LessonStateMachine.kt`, `LessonPhase.kt`, `ProgressStore.kt`, `LessonViewModel.kt`, 테스트 |
| A5 | `emu_flow.sh`: Report 뒤 선택 단계 — 화면에 `동승자` 가 **있으면** 탭 → `예시입니다` 확인 → 첫 응원 `오늘도 천천히 가요` → `companion: cheer=` 로그 → `다시 시작` → Setup 텍스트에 그 문장(`18_companion.png`·`19_setup_cheer.png`). **없으면 건너뛴다**(지금 main 은 건너뜀 — Codex 화면이 들어오면 자동으로 검사). 기본 흐름 판정 불변 | `tools/emu_flow.sh` |

Codex 가 화면에서 쓸 진입점·필드는 §4 그대로: `vm.shareWithCompanion(level)`·`vm.cheer(text)`, `report.companion.praise/help`(또는 `.text`), `report.companionShareLevels`, `report.cheers`, `LessonPhase.Setup.cheer`. `LessonReport`·`Setup` 의 새 필드는 기본값이 있어 계측의 기존 생성자 호출은 그대로 컴파일된다 — 동승자 검사에는 `cheers = SeedCatalog.cheers`, `companion = CompanionNote(...)` 를 명시해라.

## 2. Codex — 화면

### Report `동승자` 탭 (`ReportScreen.kt`, `ReportPage.COMPANION`)
- 요약 화면의 하단 텍스트 액션에 **`동승자`** 추가(`자세히 보기` · `진단서` · `동승자` 순). 라벨 문자열 리소스 1개 추가(`lesson_companion` = "동승자").
- 레이아웃은 진단서 탭과 같은 구도(왼쪽 회차 그래픽 그대로, 오른쪽 읽는 영역):
  - 눈썹 `동승자에게` · 상단 40 sp `Periwinkle` "예시입니다 — 실제 전송은 없습니다"(진단서와 같은 자리·같은 문장 형식).
  - `Headline`(72 sp, 규칙 3) = `report.companion.praise + "\n" + report.companion.help` 두 줄.
  - 눈썹 `공유 범위` + 라디오 3(`CompanionShareLevel.label`, 설명 32 sp `Muted`), 기본 `총평만`. 선택 시 `vm.shareWithCompanion(level)`.
  - 눈썹 `응원 한마디` + **선택 칩 3**(`report.cheers`, `SelectionChip` 과 같은 모양·높이 96 dp·폭은 내용에 맞게, 한 줄) — 누르면 `vm.cheer(text)`, 선택 상태 `Periwinkle`. 하나만 선택.
  - 하단: `돌아가기`(`TextAction`) · `다시 시작`(주 버튼 규칙 140 dp × ≥ 720 dp — 진단서 탭과 같은 배치).
- 숫자 없음(계측 `\d` 검사 — 회차 그래픽 `02` 는 왼쪽 색면이라 예외, 기존 진단서 탭과 같은 처리).

### Setup 첫 화면 — 응원 눈썹 (`SetupScreen.kt`)
- `LessonPhase.Setup.cheer` 가 있으면 프로필 눈썹(`연수생 · 장롱 10년차 …`) **위**에 32 sp `Periwinkle` 눈썹 `동승자 · {cheer}`. 없으면 자리도 없음(레이아웃이 밀리지 않게 `Spacer` 로 자리 예약 X — 첫 렌더 불변 유지).
- 시트·Briefing 이후에는 안 보인다.

### 공통
- 라벨 문자열: `동승자`(신규 1개) 외 전부 그대로. 시연 패널·주 버튼·잠금 계약 불변.
- `emu_flow` 는 Claude 가 A5 에서 `동승자` 탭 → 첫 응원 → `다시 시작` 을 누른다 — 칩 라벨은 `report.cheers` 문자열 그대로.

## 3. 불변 (라운드 6 표 + 변경분)

| 규칙 | 검사 |
|---|---|
| Report 요약 첫 렌더에 `동승자` 액션, 탭 안에 두 문장·`예시입니다 — 실제 전송은 없습니다`·라디오 3·칩 3·`다시 시작` | lesson_shots(추가) |
| 라디오 선택 → `shareWithCompanion` 호출(계측은 콜백 카운트), 칩 선택 → `cheer` 호출 | lesson_shots(추가) |
| `Setup` 에 `cheer` 가 있으면 `동승자 · …` 눈썹, 없으면 없음(텍스트 0) | lesson_shots(추가: 두 렌더) |
| 동승자 탭에 숫자 없음(왼쪽 회차 그래픽 제외), 점수 문장 없음 | lesson_shots(추가) |
| 잠금·패널·주 버튼·시트 계약 | lesson_shots(기존) |
| `ui/` 에 `FontWeight.Bold` 0, `Color(0x` 는 `CoachStyle.kt` 에만, 새 문자열 리소스 1(`lesson_companion`) | grep |

## 4. 데이터 (A 선행이 주는 것)

| 필드 | 뜻 |
|---|---|
| `LessonReport.companion: CompanionNote(praise, help)` | 두 문장, 숫자 없음 |
| `LessonReport.companionShareLevels: List<CompanionShareLevel>` | 라디오 3 |
| `LessonReport.cheers: List<String>` | 응원 칩 3 |
| `LessonPhase.Setup.cheer: String?` | 직전 세션에서 고른 응원, 없으면 null |
| `LessonViewModel.shareWithCompanion(level)` / `cheer(text)` | 진입점 2 |

## 5. 캡처·시연

- `lesson-companion.png`(동승자 탭, 잘한 주차 2회차 실제 세션), `lesson-setup-cheer.png`(다시 시작 뒤 Setup 눈썹).
- 시연 대본(Claude): Report 뒤 진단서 15 s 를 **진단서 10 s + 동승자 10 s** 로 나눈다 — "옆자리 사람에게 가는 건 점수가 아니라 잘한 것 하나, 도울 것 하나. 그 사람이 남긴 한마디가 다음 세션의 첫 줄이 됩니다." 덱 12장 확장 항목에서 동승자 공유가 "있는 것" 으로 옮겨 간다.

## 6. 안 하는 것

실제 전송(휴대폰·QR·링크), 동승자 계정·인증, 동승자용 별도 앱·화면, 동승자가 세션 중에 누르는 조작(주행 중 터치 규칙과 충돌). STT 로 응원 녹음은 사내 확인 뒤.
