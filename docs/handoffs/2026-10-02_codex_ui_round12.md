# Codex 오더 — 라운드 12: 네 가지 판정 화면 · 서두 풀 재분류 · 조향 도식 보정 · 시나리오 기하 (2026-10-02)

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md, 이 발주서, docs/design/09_parking_verdict.md(판정 설계·결정 V1~V4), docs/놓치지 말고 확인해야 할 사항.md(2번 조향 도식), scoring/ParkingVerdict.kt(#88) 를 읽어라.
브랜치: 라운드 11 ②(codex/ui-round11) 머지 뒤 origin/main 에서 새 워크트리. PR 둘, 순서대로: ① codex/seed-verdict(scope data — 서두 풀 재분류 + 시나리오 기하) → ② codex/ui-round12(scope ui — 판정 네 줄·조향 도식). 한 PR = 한 scope.
제약: vehicle/, ports/, scoring/, feature/lesson/, data/ 의 구조(타입·필드·판정 규칙), build 파일, tools/ 수정 금지. docs/NEXT.md 는 고치지 말고 docs/INTEGRATION.md C 절에 적어라.
완료 기준(PR 마다): .\gradlew.bat assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest 통과(PowerShell), bash tools/lesson_shots.sh 3회 연속 "Lesson contract passed", bash tools/emu_flow.sh → result: PASS(① 에서 시나리오를 고치면 고정값 60/55·100/100·힌트 3종·배지 8 이 그대로인지가 합격 기준 — 바뀌면 PR 에 이유와 새 값, Claude 가 emu_flow 기대값을 같이 고친다), 캡처 목록은 각 절, gh pr create(제목 50자 안팎, 본문 불릿 2~5 + `검증:` 줄).
```

## 0. 왜

사용자 메모 1번("이 정도면" 납득 가능한 판정)에 대한 답이 `docs/design/09` 와 #88 이다: 운전자의 질문 넷 — **한 번에 들어갔나 · 방향이 맞게 섰나(추정) · 깔끔하게 마무리했나 · 안전했나** — 를 `AttemptRecord.verdict: ParkingVerdict?` 로 상태기계가 채운다(점검 과제는 null). 점수(0~100)는 그대로이고 `자세히 보기`·진단서에만 남긴다. 이 라운드는 그것을 **화면과 시드**에 올리는 일 + 메모 2번(조향 도식).

`ParkingVerdict` 값:

| 필드 | 값 | 뜻 |
|---|---|---|
| `entry` | `ONE_GO` / `ONE_FIX` / `MANY` | 한 번에 / 한 번 다시 / 여러 번 |
| `heading` | `ALIGNED` / `SLIGHT` / `OFF` / `UNKNOWN` | 방향 맞게 / 조금 틀어짐 / 많이 틀어짐 / 미측정(조향각 없음) — **추정** |
| `headingErrorDeg` | Float? | ②의 근거 각도. `자세히 보기` 전용(운전자 문장 금지) |
| `finish` | `CLEAN` / `LOOSE` | 정차·주차 기어·핸들 중립 모두 / 하나라도 아님 |
| `safety` | `SAFE` / `WATCH` / `UNSAFE` | 사건 0 / 1 / 2 이상 또는 벨트 없음 |

## ① `data: 서두 풀을 판정별로 · 잘한/못한 주차 시나리오 기하`

| # | 지금 | 바꿀 것 | 확인 |
|---|---|---|---|
| 1.1 서두 풀 재분류 | `RemarkTemplate(band, tags, text)` 가 점수 밴드·태그로만 고른다. 감사 뒤 서두가 중립("연습을 마쳤어요")이라 잘했는지 말하지 않는다 | 시드 **내용**만: 각 밴드 안에서 **측정된 판정에 근거한 긍정/중립 문장**으로 — EXCELLENT·`ONE_GO` 계열 "한 번에 들어갔어요.", `ALIGNED` "방향도 맞게 섰어요.", GOOD·`ONE_FIX` "한 번 다시 넣고 들어갔어요.", OK·`MANY` "여러 번 오가며 들어갔어요.", ROUGH "서두르지 않고 끝까지 했어요.". **구조(`RemarkTemplate` 필드·`RemarkPool` 선택 로직)는 그대로** — 태그 집합에 `one_go`/`one_fix`/`many`/`aligned` 가 필요하면 C 절에 적고 Claude 가 `RemarkPool` 이 verdict 에서 태그를 뽑도록 넣는다(이 PR 에서는 기존 태그만) | `RemarkPoolTest`·시드 전수 테스트(한 문장·숫자 0·`요.`) 유지, 라운드 11 ① 의 가드 테스트 통과 |
| 1.2 시나리오 기하 | 10/2 실측: `ParkingScenarios.good` 의 dead-reckoning 끝 헤딩 ≈ **41°**(목표 90° 대비 49° → `OFF`), `bad` ≈ 71°(`SLIGHT`). 판정 규칙이 아니라 Fake 시나리오의 조향각·시간이 직각을 다 돌지 않는 것 | `good` 의 후진 조향 구간(핸들 −450°)을 길게 하거나 속도를 조금 올려 끝 헤딩이 **90 ± 10°**(`ALIGNED`), `bad` 는 **65~80°**(`SLIGHT`) 가 되게. `PathReconstructor` 상수(휠베이스 2.7 m·조향비 15·바퀴 최대 35°)는 불변. **`emu_flow` 고정값(60/55·100/100·구간 4/2·힌트 3종·배지 8)이 바뀌면 안 된다** — 시간 감점 유예 60 s 안에서 조정 | `ParkingVerdictTest` 의 시나리오 테스트에 `ALIGNED`/`SLIGHT` assert 추가(지금은 null 아님만), `PathReconstructorTest` 범위 갱신, `emu_flow` PASS 로그 첨부 |

## ② `ui: Done·Report 에 판정 네 줄 · 조향 도식 보정`

| # | 원 항목 | 지금 | 바꿀 것 | 캡처 |
|---|---|---|---|---|
| 2.1 | **판정 네 줄(V1 = 가)** | Done 왼쪽은 궤적(주차)·7항목 패널(점검), Report 요약은 두 문장 + 배지 | 주차 과제의 **Done 왼쪽 아래**(궤적 아래, 점검 패널과 같은 남색 면)와 **Report 요약 탭**에 네 줄 — `한 번에 들어갔어요 ✓` / `방향은 맞게 섰어요 ✓ (신호로 추정)` / `마무리가 깔끔했어요 ✓` / `안전했어요 ✓`. 값별 문장·기호: ✓ `Periwinkle` · △ `Paper 60 %`(한 번 다시·조금 틀어짐·주의) · ✗ `Signal`(여러 번·많이 틀어짐·위험 — D2 예외) · `—` `Muted`(미측정·"방향은 알 수 없어요"). **숫자 없음**(각도는 `자세히 보기` 에 `방향 편차 N°` 한 줄로). 매퍼 `verdictLines(verdict): List<VerdictLine>` 순수 함수 + 단위 테스트(모든 조합·null) | `lesson-done.png`·`lesson-report.png` 교체, `lesson-done-verdict-fix.png`(못한 주차 — △·✗ 섞임) 추가 |
| 2.2 | 판정과 점수의 자리(V1) | 점수는 Report 요약에 없음, `자세히 보기` 에 | 그대로. `자세히 보기` 회차 줄에 `방향 편차 N°` 추가(`headingErrorDeg`, null 이면 `미측정`). 운전자 문장에는 각도 금지 | `lesson-details.png` 교체 |
| 2.3 | **조향 도식**(메모 2번) | 조향을 틀면 전방 실선/점선과 바퀴가 정렬되지 않아 어색: 안쪽·바깥쪽 점선이 같은 곡률, 바퀴가 차체 안에 들어가 보임, 뒷바퀴 보조선 없음 | 실제 애커만 기하로: 안쪽 바퀴 점선은 더 급하게·바깥쪽은 완만하게(회전 중심 공유 — 라운드 7 의 "두 호" 를 전방 점선에도 적용), 실선은 그 중간(차체 중심 궤적). 앞바퀴는 차체 윤곽에 **살짝 걸치게**(바퀴 폭의 1/3 이 윤곽 밖). 가능하면 **뒷바퀴 궤적 점선**(후진 주차의 핵심 — 뒷바퀴가 그리는 호)도 `Lavender` 40 % 로. 색 토큰 5개 밖 금지 | `lesson-maneuver-guides-left.png`·`-straight.png`·`lesson-maneuver-guides.png` 교체 + 조향 클립 프레임 스트립 |

## 3. 불변

| 규칙 | 검사 |
|---|---|
| 주행 잠금 화면(Maneuver·Quiz·Done·Report·QuizDone)에 터치 타깃·점수·숫자 0 — 판정 네 줄도 잠금이면 숨김(라운드 11 ② 의 잠금 레이어 위에 얹는다) | lesson_shots |
| 운전자 문장에 숫자 없음(D1 경계 — `자세히 보기` 각도는 예외) | 단위 테스트 |
| `ui/` Bold 0 · `Color(0x` 는 CoachStyle 만 · 새 토큰 0 · 새 문자열 리소스 0 | grep |
| `emu_flow` 고정값(60/55·100/100·구간 4/2·힌트 3종·배지 8)·라벨 13개 | emu_flow PASS |
| 시드·채점·상태기계 **구조** 불변 | `git diff --stat` 에 `feature/`·`scoring/` 없음 |

## 4. 다음(라운드 12 뒤, Claude)

- V4 = 나: 밴드를 판정 기반으로 전환(`ScoreBand.of(verdict)`)은 ①이 끝나고 서두 풀이 판정별로 갖춰진 뒤 Claude 가 `RemarkPool` 에서. 시나리오 고정값은 점수라 불변.
- 사내 재검증(실신호 7)에서 ②(헤딩 추정)가 실차 조향각으로 말이 되는지 — `자세히 보기` 의 `방향 편차` 를 관찰 노트에.
