# Codex 오더 — UI 재설계 라운드 1: 기하학 포스터 디자인 시스템 + `Maneuver`·`Report` (2026-09-26)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, docs/design/02_design_brief.md, docs/design/geometric-poster-development/README.md 를 읽어라.
브랜치: 아래 "0. 작업 트리 정리" 를 먼저 한 뒤, codex/ui-round1 을 **origin/main** 에서 새로 만들어 작업.
작업: 기하학 포스터 디자인 시스템(색·글자·버튼·구도 규칙)을 ui/CoachStyle.kt 에 세우고, Maneuver 와 Report 두 화면을 그 규칙으로 다시 그린다. 나머지 화면은 라운드 2.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 는 수정 금지. 필요하면 docs/INTEGRATION.md C절에 적어라.
       아래 "불변" 표의 문자열·조건은 디자인이 어떻게 바뀌어도 지킨다 — tools/emu_flow.sh 와 LessonScreenInstrumentation 이 검사한다.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest 통과(PowerShell), bash tools/lesson_shots.sh → "Lesson contract passed", bash tools/emu_flow.sh → result: PASS,
          캡처(lesson-maneuver·locked·missing·report·certificate)를 PR 에 첨부, **드래프트 PR**(gh pr create --draft). 사용자가 톤을 보고 확정하면 라운드 2 발주.
```

## 0. 작업 트리 정리 (한 번만)

본 트리(`C:\Project\17_hackathon-pivot`)는 지금 삭제된 `codex/lesson-screens`(`6ff5193`) 위에 있고, 디자인 시안 폴더 4개(`docs/design/{paper-ui,pinterest-concepts,references,geometric-poster-development}`, 약 66 MB·PNG 50장)가 미추적, `docs/NEXT.md`·`docs/journal/2026-09-26.md`·`docs/journal/README.md` 가 수정 상태다. `main` 은 그 사이 PR #6~#11 로 크게 앞서 있어(일지·NEXT 는 Claude 가 이미 다시 썼다) 그 세 파일은 **충돌한다**.

1. 세 문서의 내 수정을 `docs/design/README.md`(새 파일, 디자인 탐색 인덱스: 종이 UI → 미니멀 A/B/C → Pinterest 3안 → 기하학 포스터 디벨롭 → 핸들 A–D, 각 폴더 링크와 사용자 선택 한 줄씩)로 옮기고, 세 문서는 되돌린다: `git checkout -- docs/NEXT.md docs/journal/2026-09-26.md docs/journal/README.md`. 일지·NEXT 반영은 Claude 가 한다(이미 한 줄 넣어 두었다).
2. `git checkout -b codex/design-refs && git add docs/design && git commit -m "docs: 디자인 시안·프롬프트 보존 (종이 UI·Pinterest·기하학 포스터·핸들 대안)"` → `git fetch origin && git rebase origin/main`(새 파일만이라 충돌 없음) → `gh pr create`. 이 PR 은 이미지만이라 Claude 가 바로 머지한다.
3. `git checkout -b codex/ui-round1 origin/main` 에서 아래 작업. 시안 PNG 는 코드에 필요 없다(라운드 1 은 Canvas·도형만).

## 1. 왜 다시 그리나 (한 문단)

PR #5 의 5화면은 기능 확인용 보편 UI 였다. 사용자가 여러 방향(종이 UI·미니멀·Pinterest 3안)을 본 뒤 **기하학 포스터**를 기준으로 정했고, Codex 디자인 세션이 설정·연습 준비·주차 안내·리포트 4장의 시안과 정보 구조 제안(`geometric-poster-development/README.md`)을 만들었다. 이제 그 시안을 **동작하는 Compose 화면**으로 옮긴다. 원칙은 시안 README 그대로 — **그래픽은 크게, 한 화면의 행동은 적게.** 큰 남색·흰색 색면으로 공간을 나누고, 빨간 버튼 하나가 다음 행동을 말한다. 카드·표·굵은 제목으로 위계를 만들지 않는다.

## 2. 디자인 시스템 (`ui/CoachStyle.kt` 에 세운다 — 라운드 2 가 그대로 쓴다)

### 색 토큰 (`CoachColors` 교체)

| 토큰 | 값 | 쓰는 곳 |
|---|---|---|
| `Ink` | `#070827` | 남색 색면, 흰 바탕 위 본문 글자 |
| `Paper` | `#FCFCFA` | 기본 바탕, 남색 위 글자 |
| `Periwinkle` | `#5B60A1` | 형태·구획 보조, 상태 라벨("시뮬레이션"), 눈썹 라벨 |
| `Lavender` | `#E5E6F0` | 옅은 구분선, 미측정 값의 회색 톤(글자는 `Ink` 60 %) |
| `Signal` | `#F52D48` | **주 행동 버튼 하나** + 도식의 후진 화살표·인덱스 마크. 경고·감점에 쓰지 않는다 |

기존 어두운 녹색 계열(`Background #111916` 등)은 지운다. 화면 바탕은 `Paper`. 나빠진 값은 **회색**(`Ink` 60 %), 좋아진 값만 `Periwinkle` — 붉게 하지 않는다(브리프 톤 규칙).

### 글자

시스템 기본 sans-serif 만. 굵기는 **Regular 400** 중심, 숫자·값만 **Medium 500**. Bold 700 금지(브랜드 마크 제외).

| 역할 | 크기(sp, 2560×1268 기준) | 예 |
|---|---|---|
| 메인 문장(지시·피드백·제안) | 72–88, 최대 3줄 | "천천히 후진하세요." |
| 주요 수치 | 80–96 Medium | "R", "85 cm", "01" |
| 본문·부제 | 40 | "후면 직각 주차 · 가이드 모드" |
| 눈썹·상태·보조 | 32 (하한) | "조수석", "시뮬레이션", "연습 기록" |
| 브랜드 마크 | 32, 자간 넓게, `DRIVE COACH` | 항상 좌상단. 남색 위면 `Paper`, 흰 바탕 위면 `Ink` |

한국어 `LineBreak.Paragraph`/`wordBreak = Phrase`. 자막은 4줄까지 말줄임 없이.

### 구도

- 화면을 **그래픽 영역**과 **읽는 영역**으로 크게 둘로 나눈다. 카드 반복 금지. 바깥 여백 7 %(약 180 dp).
- 색면 경계는 곧거나 큰 원호. 종이 질감·그림자·베벨·글래스·둥근 대시보드 타일 금지.
- 정차 상태의 주 행동은 **빨간 필(pill) 버튼 하나**(높이 약 120 dp, 라벨 40 sp Medium + 오른쪽 화살표 →). 부 행동은 밑줄 있는 텍스트(40 sp). 상태 정보는 버튼처럼 보이지 않는 짧은 라벨.
- 공통 컴포저블: `BrandMark()`, `PrimaryPill(label, onClick)`, `TextAction(label, onClick)`, `StateLabel(text, availability)`, `Eyebrow(text)`, `Headline(text)`. `LessonComponents.kt` 를 이걸로 갈아 낀다.

### 조수석 (자막의 인격)

말풍선·이름·아바타 없음. 읽는 영역에 눈썹 `조수석`(32 sp `Ink`) + 그 아래 메인 문장(72–88 sp). 자막(`subtitle`)이 바로 그 메인 문장이다 — 별도 자막 바를 두지 않는다. 가이드 문장(`guideText`)이 있으면 그것이 메인 문장이고, 자막은 그 아래 40 sp 로.

## 3. `Maneuver` — 시안 `03-maneuver-v2.png`

왼쪽 약 53 % 남색 색면 = **도식**, 오른쪽 흰 색면 = **읽는 영역**. 경계는 곧은 수직선.

### 왼쪽 남색 — 도식 (`Canvas`, `contentDescription` 필수)
- 위에서 본 차. 흰 차체 + `Periwinkle` 창·유리, 바퀴 4개는 `Periwinkle` 막대. **뒤가 화면 위, 앞이 아래**. 앞바퀴 두 개를 `steeringDeg` 로 회전(COVESA 양수 = 왼쪽; 시안은 오른쪽 450° → 앞바퀴 아래끝이 화면 왼쪽으로 `/`). 회전 표시 최대 ±38° 는 지금 코드 그대로(도식이며 실측 각이 아님을 좌하단 32 sp 로: `조향 방향 도식`).
- 기어가 R 이면 차 위에 작은 **빨간 위 화살표** + 32 sp `차량 뒤쪽 ↑`. R 이 아니면 화살표 없음.
- 차 아래 눈썹 `조향각` + 값 `오른쪽 450°`(80 sp Medium, `Paper`). 미측정이면 값 대신 `미측정`(회색) — 접근성 텍스트는 지금처럼 `뒤 거리 미측정`·`조향각 미측정`.
- 좌상단 `DRIVE COACH`(`Paper`). **주차 칸·경로·센서 호·장애물은 그리지 않는다**(앱이 모르는 것).

### 오른쪽 흰 — 읽는 영역
- 상단 좌 `후면 직각 주차`(40 sp), 상단 우 속도 `3 km/h`(56 sp Medium) 와 그 아래 상태 라벨.
- **상태 라벨 규칙(불변: 세 상태가 구분돼 보여야 한다)**: 조향·기어·거리 세 신호가 모두 같은 상태면 속도 아래 한 줄(`시뮬레이션` / `실신호` / `미측정`)로 접고, 하나라도 다르면 각 값 옆에 32 sp 로 따로 단다. 색: 실신호 `Ink`, 시뮬 `Periwinkle`, 미측정 회색.
- 가운데: 눈썹 `조수석` + 메인 문장. 모드별 — **가이드**: `guideText`(단계 `guideStep` 은 눈썹 옆 32 sp `3/6`) / **힌트**: `hintText` 가 4 s 동안 메인 문장을 대신하고 사라짐(지금 동작 유지) / **평가**: `subtitle`. 문장이 없으면 영역을 비운다(자리표시자 금지).
- 아래: 두 값 `기어 R` · `뒤 거리 85 cm`(눈썹 32 + 값 80 Medium, 사이에 옅은 세로 구분선). 40 cm 미만이거나 `obstacleWarning` 이면 값 색을 `Ink` 로 유지하고 눈썹만 `가까워요` 로 바꾼다 — 붉게 하지 않는다. 접근성 텍스트 `뒤 85 cm` 유지.
- **정차(`stopped`)일 때만** 읽는 영역 하단에 `PrimaryPill("다 됐어요")`. `askedDone` 이면 살짝 펄스. 속도 표시가 "1" 이어도 `stopped` 가 false 면 버튼 없음(계측이 5.1·20·1 을 검사).
- 회차·이동 횟수·경과 시간은 **이동 중에는 보이지 않는다**(시안 결정). 정차 상태에서 `다 됐어요` 위에 32 sp 한 줄 `1회차 · 이동 2회 · 18초` 허용. 점수·감점·"N점" 문자열은 접근성 트리 포함 어디에도 없다.

### 잠금과 시연 패널
- `locked`(속도 > 5)면 **클릭·스크롤 가능한 노드 0개** — 시연 패널·토글 포함. 지금 계약 그대로.
- 시연 패널: 우상단 속도 옆에 32 sp 텍스트 토글 `시연`. 펼치면 흰 영역 우측에 세로로 좁게(폭 약 420 dp) 버튼들 — 라벨은 아래 "불변" 표 그대로, 스타일은 `Lavender` 배경의 작은 사각 버튼(빨간 필 금지 — 주 행동과 구분). `demo.scenarios` 의 제목(주차 과제면 잘한/못한 주차, 점검 과제면 잘한/못한 점검)을 그대로 라벨로. 기본 **펼침**(시연 중 한 번 덜 누르게), 열려 있어도 메인 문장을 가리지 않게 배치.

## 4. `Report` — 시안 `04-report-v1.png`

흰 바탕. 왼쪽 약 38 % 에 가장자리로 잘린 남색 세로 형태 + `Periwinkle` 원호 한 조각. 그 안에 눈썹 `연습 기록`(`Paper` 32 sp) 과 **큰 숫자 `01`**(회차 수 `attempts.size`, 두 자리 0 채움, 약 400 sp Regular `Paper`) + 작은 빨간 사선 하나. 숫자 길이가 늘면(≥ 3자리) 폭에 맞춰 축소. 이 숫자는 회차 수다 — 점수가 아니다.

오른쪽 읽는 영역, 세로 그리드 정렬:
- 눈썹 `오늘의 기록`, 메인 문장 = `summary`(72–88 sp, 최대 3줄), 부제 `{task.title} · {mode.label}`.
- `PrimaryPill("다시 시작")` → `restart()`.
- 부 행동 한 줄: `TextAction("자세히 보기")` · `TextAction("진단서")` — **둘 다 첫 화면에서 바로 보이고 클릭 가능**(계측이 첫 렌더에서 `진단서` 를 클릭한다).
- 하단, 옅은 가로 구분선 아래 32–40 sp: 배지 `실신호 {live} · 시뮬레이션 {simulated} · 미측정 {missing}`(`badgeText(score.badge)` 그대로) 와 오른쪽 `주차 과정만 측정했어요.`. `best.missingSignals` 가 비지 않으면 그 아래 **별도 텍스트 노드**로 `이 신호는 이 차에서 받지 못했어요` + 신호 이름 목록(32 sp 회색). `unverifiedGuideSteps` 가 있으면 같은 자리에 `이 단계는 확인할 수 없었어요` + 목록.

### 자세히 보기 (같은 화면 안에서 오른쪽 영역이 바뀐다 — 화면 전환 아님)
- 숙련 `best.skill` · 안전 `best.safety` — 두 개의 **큰 숫자**(96 sp Medium) + 눈썹. 원형 게이지·표 금지. 정차 화면이라 점수 표시 가능.
- 회차 목록: 회차마다 한 줄 `1회차 · 숙련 60 · 안전 55 · 이동 4회 · 44초`(40 sp, 구분선만). 좋아진 값 `Periwinkle`, 나빠진 값 회색.
- 다음 제안 한 줄: `다음엔 {nextTask.title} · {nextMode.label} — {nextReason}`.
- `TextAction("돌아가기")` 로 요약으로.

### 진단서 (`진단서` 클릭 → 오른쪽 영역 교체)
- 상단 고정 문구 **`예시입니다 — 실제 전송·계약은 없습니다`**(40 sp `Periwinkle`).
- 공유 범위 3개(`shareLevels`: `총점만` / `항목별` / `원시 신호`) — 라디오 의미의 선택 목록. 선택된 항목은 접근성 `isChecked` 가 true 여야 한다(계측이 `항목별` 클릭 뒤 검사). 각 항목 아래 `description` 32 sp.
- 예상 혜택 `benefits` 목록(40 sp, 글머리 없이 줄 구분선).
- `TextAction("공유 예시 보기")` → 토스트 "예시 화면입니다". `PrimaryPill("다시 시작")` 은 이 화면에도.

## 5. 불변 (계측·스크립트가 검사한다 — 위치·모양은 자유, 문자열·조건은 고정)

| 규칙 | 검사 |
|---|---|
| 라벨 문자열: 시작 / 다 됐어요 / 한 번 더 / 오늘은 여기까지 / 다시 시작 / 시연 / 정차 / 출발 / 문 열기 / 문 닫기 / 시나리오 정지 + 시나리오 제목(잘한 주차 / 못한 주차 / 잘한 점검 / 못한 점검) + 모드 라벨(가이드 / 힌트 / 평가 / 지식 테스트) — `res/values/strings.xml` 그대로 | emu_flow 가 라벨로 탭 |
| `locked` 면 클릭·스크롤 노드 0개(시연 패널 포함) | lesson_shots 5.1·20 km/h |
| `Maneuver` 접근성 트리에 `점수`·`감점`·`\d+점` 없음 | lesson_shots |
| `stopped` 일 때만 `다 됐어요`. 표시 속도 "1" 이어도 `stopped=false` 면 없음 | lesson_shots |
| `뒤 85 cm` / `뒤 거리 미측정` 접근성 텍스트 | lesson_shots |
| 자막 4줄 그대로(말줄임 없음), 힌트 4 s 뒤 사라짐 | lesson_shots |
| `Done` 에서 `delta == null` 이면 "지난번보다" 없음 | lesson_shots(라운드 2 대상이지만 지금 통과 상태 유지) |
| `Report` 첫 렌더에 배지 문자열·`다시 시작`·(missing 있으면) `이 신호는 이 차에서 받지 못했어요`·클릭 가능한 `진단서` | lesson_shots · emu_flow |
| 진단서에 `예시입니다 — 실제 전송·계약은 없습니다`, `총점만`·`항목별`·`원시 신호`(선택 시 `isChecked`), `공유 예시 보기` | lesson_shots |
| 화면 composable 은 `LessonPhase` 를 받지 않고 단계 데이터·`toDisplayState()` 만 | 코드 리뷰 |
| `DesignScale` 2560×1268 dp 고정, 글자 하한 32 sp, 시스템 글꼴만 | 눈 + 로그 |
| 나빠진 값은 회색, 붉은색은 주 행동·화살표에만 | 눈 |

`LessonScreenInstrumentation` 은 Codex 소유다 — 레이아웃이 바뀌어 셀렉터를 손봐야 하면 고쳐도 되지만, 위 표의 **검사 항목을 빼면 안 된다**. 바꾼 줄은 PR 본문에 적는다.

## 6. 라운드 2 예고 (이번 PR 에 넣지 않는다)

`Setup`(시안 `01-setup-v1.png`: 제안 한 문장 + 빨간 시작 + `과제·모드 바꾸기` 시트 — 준비 중 과제 회색·미지원 모드 숨김·예약 카드는 시트 안으로), `Briefing`(시안 `steering-alternatives/b-oblique-rim-v1.png`: 핸들 B 크롭 + `음성 안내 중`), `Done`(흰 바탕 + 작은 색면, `attemptHead` 눈썹 + `remark` 메인 문장 + 한 번 더 / 오늘은 여기까지), `Quiz`·`QuizDone`, 출발 전 점검 과제용 `Maneuver` 칩 3개(INTEGRATION C절 9/26 세 건). 설정·준비의 자동차·핸들 일러스트는 **글자 없는 에셋**으로 다시 생성해(`drawable-nodpi`, WebP, 각 1 MB 이하, 팔레트 정확히) 라운드 2 에 넣는다 — 라운드 1 PR 리뷰 중에 미리 만들어 두어도 좋다.

## 7. 참고

- 시안·정보 구조·색 토큰·글자 크기 출발점: `docs/design/geometric-poster-development/README.md`("시각 규칙", "전체 흐름과 정보 배치"). 시안의 값(3 km/h·R·450°·85 cm·실신호 0·시뮬 7·미측정 1)은 지금 `emu_flow` 잘한 주차 흐름과 같다.
- 지금 화면과 캡처: `docs/screenshots/lesson/`(PR #5). 바뀐 뒤 같은 이름으로 교체한다.
- `ManeuverDisplayState` 에 점수 필드가 타입상 없다(리플렉션 테스트) — 화면이 점수를 그릴 방법이 없어야 정상이다.
