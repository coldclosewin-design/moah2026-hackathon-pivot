# Codex 오더 — 라운드 26b: 홈 제목 속 과제 · 브리핑 자막 · 점검 바퀴/모드 면 · 코치 두 층 (2026-10-07)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, 고른 시안 페이지
         (docs/design/round26-proposals/1-home-task.html 시안 6 · 2-briefing.html 시안 B · 3-checklist-focus.html 시안 C ·
          4-checklist-modes.html 시안 3 · 5-coach-lines.html 시안 B — 보기 창이 HTML 을 못 열면 같은 이름 .png)를 읽어라.
시작 조건: 26a(codex/ui-round26a) 머지 + Claude 모델 PR #230 · #231 · #232 · #233 머지 뒤 origin/main 최신에서 새 워크트리,
         브랜치 codex/ui-round26b, scope ui, PR 하나.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조, build 파일, tools/ 수정 금지(필요하면 INTEGRATION C 절에). docs/NEXT.md 금지.
     도구가 찾는 라벨(못한 주차 · 잘한 주차 · 정차 · 출발 · 문 열기 · 문 닫기 · 시나리오 정지 · 시작 · 다 됐어요 · 한 번 더 · 오늘은 여기까지 · 메인으로 ·
     이 설정으로 홈 · 켬 · 카드 + 글 · 코치와 대화 · 코치에게 글로 말해 보세요 · 보내기 · 말 카드 문장들)은 불변.
     **`과제·모드 바꾸기`** 는 글자 링크가 사라져도(① 시안 6) 제목 속 과제 낱말의 접근성 이름(contentDescription)으로 남겨라 — emu_flow·course_flow·inhouse_check 가 이 이름으로 시트를 연다.
     새 색 토큰 0, Bold 0. 다크 모드(시안 6)는 마감 뒤 — 이번엔 하지 않는다.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell),
          bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", RESERVE=1 bash tools/emu_flow.sh → PASS · clashes 0,
          adb install -r 뒤 bash tools/course_flow.sh → PASS. 바뀐 캡처 교체 + README 표(시안 페이지와 나란히). 에뮬은 네 것(emulator-5556)만.
```

선행 상태: Claude 모델 PR 넷(아래 "모델 API") 머지 뒤. 사용자 결정(10/7): **1-6 · 2-B · 3-C · 4-3 · 5-B**, 6(다크)은 마감 뒤·발표에 설계만.

## 모델 API (Claude PR — 화면은 이것만 읽고 부른다)

| PR | 무엇 | 화면이 쓰는 것 |
|---|---|---|
| #230 | 브리핑이 음성 끝까지 머문다(최소 3 s · 음성 끝 + 1 s · 최대 12 s) | `LessonPhase.Briefing.line`(마침표마다 자막 한 줄) · `.expectedMillis`(진행 막대 길이 어림) · `vm.skipBriefing()`(정차 중만 `건너뛰기 ›`) |
| #231 | 홈이 처음엔 과제를 정하지 않는다 · 홈에서 모드만 바꾸기 | `Setup.picked`(false = 과제 미정 — `suggestedTask` 를 보이지 말 것) · `vm.chooseHomeMode(mode)` |
| #232 | 점검 목록 공개 범위가 모드마다 | `ManeuverDisplayState.mode` · `.checklistReveal`(ALL · MISTAKES · NAMES_ONLY) — **미측정 줄은 세 모드 모두 "미측정"** |
| #233 | 코치 문장 = "제목.\n한 마디." (제목 ≤ 12자 · 한 마디 ≤ 24자) | 회차 멘트 `record.remark` · 총평 · 대화 `say` 가 이미 `\n` 로 두 줄 — 첫 줄 / 둘째 줄을 나눠 그린다 |

## 항목

| # | 어디 | 할 것 · 주의 |
|---|---|---|
| ① | 홈 — 시안 1 **6 제목 속 밑줄 링크** | 눈썹 `오늘의 과제 · <분류>`(주차·주행·점검·지식). 제목 = **"<과제 이름>을/를\n<모드> 모드로 해 볼까요?"** — 과제 이름 · 모드 두 낱말을 Periwinkle 밑줄 + `▼`. 과제 낱말 → 과제 시트(지금 `과제·모드 바꾸기` 와 같은 동작, 접근성 이름 `과제·모드 바꾸기`) · 모드 낱말 → 작은 팝업(그 과제가 지원하는 모드만, 고르면 `vm.chooseHomeMode`). 아래 이유 한 줄(모드 설명)은 그대로. 지금의 과제 줄(`후면 직각 주차 · 힌트 모드`)과 `과제·모드 바꾸기` 링크 줄은 없앤다(`코치와 대화` 알약은 오른쪽 그대로). **첫 실행(`picked = false`)**: 눈썹 `처음 오셨네요` · 제목 "**과제 고르기 ▼** 부터\n해 볼까요?"(점선 밑줄 = 시트 열기, 접근성 이름 `과제·모드 바꾸기`) · 한 줄 "밑줄을 누르면 과제를 고를 수 있어요." · `시작` 흐림(탭 무반응). 긴 과제 이름(`장내기능 모의시험`)은 제목 자동 축소(지금 제목 규칙) — 세 줄 넘지 않게. 터치 영역은 낱말 글자보다 넉넉히(최소 높이 96 dp). 예약 카드가 있을 때도 같은 규칙 |
| ② | 브리핑 — 시안 2 **B 자막 + 진행 막대** | "음성 안내 중" 자리를 **지금 읽는 문장 자막**으로: `line` 을 마침표마다 한 줄, 지금 문장만 진하게(나머지 흐림) — 지금 문장 = 경과 시간 ÷ `expectedMillis` 를 글자 수 비율로 나눈 어림. 아래 가는 진행 막대(숫자 없음)가 `expectedMillis` 동안 차고, 넘어가기 전까진 끝에서 멈춤. 오른쪽 아래 작은 글자 버튼 `건너뛰기 ›` — 정차 중(잠금 아님)만, `vm.skipBriefing()`. 사진에서 지운 흐린 줄 "서두르지 않아도 괜찮아요." 는 없앤다. 과제 표기는 `<분류> › <과제> · <모드> 모드` |
| ③ | 출발 전 점검 — 시안 3 **C 가운데 고정 바퀴** | 왼쪽 목록이 가사 화면처럼 흐른다: **지금 줄이 늘 패널 세로 가운데**에 크게(약 1.6배 글자·높이), 위아래 줄은 멀수록 작고 흐리게, 지금 줄이 바뀌면 목록이 위로 미끄러진다(약 300 ms). 위 단계 막대(7칸) 삭제(26a 에서 아래 눈썹은 이미 삭제). 신호 출처 작은 글은 지금 줄에만. 오른쪽 `코치 n/7` 은 남김. 잠금(> 5 km/h) 모습은 지금 규칙 그대로 |
| ④ | 점검 모드 — 시안 4 **3 면 색 + 모드 사다리** | 왼쪽 패널 면: 가이드 = Ink(지금) · 힌트 = Periwinkle · 평가 = Paper 시험지(먹 선 테두리, 둥근 점 대신 네모 칸). 패널 오른쪽 위 작은 사다리 `가이드 · 힌트 · 평가`(지금 칸 채움, **누를 수 없는 표시** — 테두리 없이). 목록 내용은 `checklistReveal` 대로: ALL = 이름 · 값 · ③ 의 바퀴 / MISTAKES = 된 줄 `✓` · 아직 `—` · 틀린 줄만 값까지(Periwinkle 위 빨강은 대비가 약하니 Ink 띠 + 흰 글자) / NAMES_ONLY = 이름 + 빈 네모 칸, 바퀴 확대 없음. 오른쪽 열: 힌트 = 눈썹 "틀릴 때만 말해요" + 지시문 "순서대로 해 보세요." · 평가 = "조용히 지켜봐요" + "끝나면\n다 됐어요를 눌러 주세요." 판정 화면(Done)은 세 모드 같은 모양. **미측정 줄은 세 모드 모두 "미측정" 그대로**(가림과 구별) |
| ⑤ | 코치 문장 두 층 — 시안 5 **B 제목 + 한 마디** | 회차 판정(Done)의 멘트 · 오늘의 기록 총평 · 코치 대화 말풍선: 첫 줄(제목)은 크게 Ink, 둘째 줄(한 마디)은 작게 흐린 Ink. 크기: 판정·리포트 제목 88 dp · 한 마디 48 dp / 말풍선 52 · 44 dp. 한 줄뿐인 대화 답은 제목 크기 하나. 26a 의 마침표 줄바꿈 포맷터와 같이 쓴다(문장이 이미 `\n` 두 줄) |
| ⑥ | 과제 표기 전역 | 리포트·판정·브리핑의 과제 줄을 `<분류> › <과제> · <모드> 모드` 로 통일(예: `주차 › 후면 직각 주차 · 가이드 모드`) — 사용자 "카테고리를 명확하게". 홈은 ① 의 눈썹+제목이 대신한다 |

## 불변

| 규칙 | 검사 |
|---|---|
| 잠금 중 보이는 터치 타깃 0 · 운전자 화면 숫자 0 · 잠금 화면 평면 | 계측 |
| 브리핑 `건너뛰기` 는 잠금 중 없음 · 모드 사다리는 탭 대상 아님 | 계측 |
| Real 빌드(`admin == null`)에 준비실·띠·워드마크 길게 누르기 0 | 계측 |
| 도구 라벨 불변 · `과제·모드 바꾸기` 접근성 이름 유지 · `emu_flow`·`course_flow` 그대로 PASS | 도구 |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 색 토큰 0 | grep |

캡처: 교체 `lesson-setup*.png`(제목 속 과제·모드 밑줄, 첫 실행 = `picked=false` fixture) · `lesson-briefing*.png`(자막·막대·건너뛰기) · `lesson-maneuver-checklist*.png`(바퀴, 가이드/힌트/평가 셋) · `lesson-done*.png`·`lesson-report-session.png`·`lesson-setup-coach-*.png`(두 층 문장), 새 `lesson-setup-mode-popup.png` · `lesson-checklist-wheel-strip.png`(0/150/300 ms) · `lesson-checklist-modes.png`(세 모드 나란히).

머지 뒤 Claude 가 리뷰하고, 새 태그 `inhouse-20261008-1` 로 사내 검증 · 녹화(10/9 가능).
