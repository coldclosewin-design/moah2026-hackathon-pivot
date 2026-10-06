# 사내 세션 — 지금 할 일 (항상 이 파일)

> 사내 세션 시작 한 줄: **`git fetch origin && git show origin/main:docs/handoffs/INHOUSE_NOW.md` 를 읽고 그대로 진행해.**
> 이 파일은 경로가 바뀌지 않는다 — 사외에서 태그를 새로 만들 때마다 아래 "태그"·"새로 볼 것" 만 바꾼다.
> **사내에서는 코드를 고치지 않는다 — 검증·녹화만.**

갱신: 2026-10-06 저녁 (사외) — 사내 검증 #5(15:30) 결과 반영: **이 태그로 녹화 OK**

## 태그

- **`inhouse-20261006-5`** (= `f2af874`) — 녹화 기준. 홈 코치 텍스트 대화(#198·#200·#201) + 예약 홈 이유 한 문장(#203) + 사내 검증 #4 수정 셋(#193·#194·#195).
- public clone 이 막히면 번들 `moah2026-20261006-5.bundle` 을 반입해 clone(브랜치 `inhouse/base-20261006-5` 가 바로 체크아웃).
- 텍스트 대화가 사내에서 문제면 `inhouse-20261006-3`(= `624cf04`, #4 수정만)으로 녹화해도 된다.

## 순서

자세한 단계는 `git show origin/main:docs/handoffs/2026-10-06_inhouse_resume.md` — 요약:

1. `git checkout inhouse-20261006-5` → jar 복사 → `REPO_ONLY=1 bash tools/inhouse_check.sh` → `PASS (저장소만)`
2. `local.properties` 에 `mobis.vss.jar=automotive/libs/mobis.framework.core.jar` 한 줄 → `./gradlew assembleDebug testDebugUnitTest`
3. `adb root` · Wi-Fi · `copilot_config.json` push · `adb logcat -G 16M` → `bash tools/inhouse_check.sh` → 요약 전체를 적는다
4. 아래 "새로 볼 것" 넷
5. 문제가 없으면 준비실 프리셋으로 시작해 `docs/05_demo_script.md` 순서대로 녹화

## 녹화할 때 알아 둘 것 (#5 에서 나온 것)

- 문이 이미 열린 채 다음 회차를 시작하면 띠 `문 열기` 는 반응하지 않는다 — 리포트 뒤에는 `문 닫기` 를 한 번 눌러 둔다.
- `am start --es preset …` 은 앱이 떠 있으면 무시된다 — `adb shell am force-stop com.moah.hackathon` 뒤에 실행(다음 태그에서 고친다).
- 프리셋을 다시 적용하면 "시뮬레이션 음성 입력" 이 꺼진다 — 대화 컷을 찍으려면 준비실에서 다시 켬.
- 한국어 입력은 사내 에뮬에서 안 된다(키보드 앱이 뜨지 않음) — 대화 컷은 다음 태그의 "말 카드" 로 찍는다. 그 전엔 칩 흐름으로.

## 새로 볼 것 (이번 태그 — #5 에서 확인 끝)

1. **단위 테스트** — 사내 jar 로 **341 전부 통과**(지난번 실패한 `SimOnlySignalsTest` 포함).
2. **움직이기 전 문 열기** — 회차 시작 직후, 차가 움직이기 전에 띠 `문 열기` → 화면이 멈추지 않고 홈(과제 고르기)으로. 움직인 뒤 `문 열기` → 리포트(최대 8초 안에).
3. **홈 코치 대화** — `inhouse_check.sh` 요약 끝의 `home coach: say …` · `intent=… (ai|rule|fallback) N ms` 두 쌍을 그대로 옮긴다. 목표: Copilot 로그인 뒤 **둘 다 `(ai)`**, 둘째 줄 `PIN_TASK(parking-parallel,HINT)`, 지연 수 초. `(fallback)` 이면 `MOAH/CloudCoachPort` 의 `dialog response rejected` 줄 앞부분 요지만.
4. **한국어 입력** — 준비실 "시뮬레이션 음성 입력 · 켬" → 홈 `코치에게 말하기` → 사내 에뮬 키보드로 한국어가 입력되는가. 되면 "평행 주차 힌트로 할래요" 한 줄 → 시트가 닫히고 홈 제안이 평행 주차 · 힌트. (끔이면 버튼이 `코치와 고르기`.)

선택: `copilot_config.json` 에 `"persona": "…한 문장…"` 을 넣어 push → 앱 재시작 → 회차 멘트·대화 말투가 같이 바뀌는가.

## 결과 남기는 법

`docs/INTEGRATION.md` B절 형식의 **관찰 노트**(사람이 옮길 요지만). 문제는 "무엇을 눌렀고 무엇이 보였는지" 한 줄씩.
**사내 캡처·로그 원문·계정·토큰·내부 호스트명은 적지 않는다**(public 저장소). 노트를 사외로 가져오면 다음 태그에 반영한다.
