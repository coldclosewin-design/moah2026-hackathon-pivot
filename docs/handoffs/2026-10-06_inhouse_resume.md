# 사내 재검증 재개 — 태그 `inhouse-20261006-4` (2026-10-06)

사내 세션은 이 파일 하나를 보고 진행한다. **사내에서는 코드를 고치지 않는다 — 검증·녹화만.**
이 파일은 태그를 만든 뒤 main 에 들어갔으므로 태그 체크아웃 폴더에는 없다. clone 한 뒤 `git show origin/main:docs/handoffs/2026-10-06_inhouse_resume.md` 로 읽는다(빌드는 태그로).

## 순서

1. **저장소**: public clone 후 `git checkout inhouse-20261006-4` (= `64462c0`). clone 이 막히면 번들 `moah2026-20261006-4.bundle` 을 반입해 clone(그 브랜치 `inhouse/base-20261006-4` 가 바로 체크아웃된다). `-2` 화면 + 사내 검증 #4 수정 셋 + **홈 코치 텍스트 대화**(#4 기능 요청). 텍스트 대화가 사내에서 문제면 `inhouse-20261006-3`(= `624cf04`, #4 수정만)으로 녹화해도 된다.
2. **jar 복사 직후** `REPO_ONLY=1 bash tools/inhouse_check.sh` → `PASS (저장소만)`.
3. **빌드**: `local.properties` 에 `mobis.vss.jar=automotive/libs/mobis.framework.core.jar` 한 줄 → `./gradlew assembleDebug` (로그 첫 줄에 `USE_FAKE_VSS=false`).
4. **세션 준비**: `adb root` · Wi-Fi · `copilot_config.json` push · `adb logcat -G 16M` → `bash tools/inhouse_check.sh`. 요약 전체를 적는다 — 배지 `live=` 값, `missing=[…]`, 우리 앱 FATAL 수, Copilot 상태·fallback 수.
5. **이번 태그에서 새로 확인할 것(#4 수정 + 홈 코치 텍스트 대화)**:
   - **단위 테스트** `./gradlew testDebugUnitTest` 가 사내 jar 로 전부 통과하는가(340 — #4 의 `SimOnlySignalsTest` 실패가 없어야 한다).
   - **홈 코치 텍스트 대화** — `inhouse_check.sh` 마지막 단계가 자동으로 한다(리포트 → 다시 시작 → 준비실 "시뮬레이션 음성 입력 · 켬" → 코치에게 말하기 → 영어 두 줄). 요약의 `home coach: say …` · `intent=… (ai|rule|fallback) N ms` 두 쌍을 그대로 옮긴다. **목표: Copilot 로그인 뒤 둘 다 `(ai)`, 둘째 줄 `PIN_TASK(parking-parallel,HINT)`, 지연 수 초.** `(fallback)` 이면 AI 응답이 거절된 것 — `MOAH/CloudCoachPort` 의 `dialog response rejected` 줄 앞 60자만 요지로.
   - 손으로 한 번: 준비실 음성 입력 **끔** → 홈 버튼이 `코치와 고르기`(마이크 없음), **켬** → `코치에게 말하기` + 시트에 입력 칸·`보내기`·"음성 입력 · 시뮬레이션" 배지. 사내 에뮬 키보드로 **한국어 입력이 되는지**(되면 "평행 주차 힌트로 할래요" 한 줄 → 시트가 닫히고 홈 제안이 평행 주차 · 힌트).
   - (선택) 페르소나: `copilot_config.json` 에 `"persona": "…한 문장…"` 을 넣어 push → 앱 재시작 → 회차 멘트·대화 말투가 같이 바뀌는가. 숫자·금지어 규칙은 그대로여야 한다.
   - **회차 시작 직후, 움직이기 전에 띠 `문 열기`** → 화면이 멈추지 않고 홈(과제 고르기)으로 돌아오는가. 움직인 뒤 `문 열기` → 리포트(코치가 느려도 최대 8초 안에).
   - `pm clear --user 10` 직후(첫 실행 상태)에도 `inhouse_check.sh` 가 프로필 질문을 건너뛰고 끝까지 가는가. 순수 Real 이면 "준비실 프리셋부터" 안내하고 멈추는 게 정상.
   - 모의시험 배지 `live=` 가 #4(11)보다 늘었는가 — 전조등·와이퍼가 이제 실물 키라 사내 에뮬이 값을 주면 live 로 잡힌다. 와이퍼가 늘 미측정이면 값 포맷이 다른 것(관찰 노트에 값만 적는다).

   #4 에서 이미 확인한 것(다시 볼 필요 없음, 녹화 전에 한 번 훑기만):
   - 홈 `DRIVE COACH` 를 약 2초 길게 → **준비실**이 열리는가(기본 Hybrid 빌드). 순수 Real(`-PfillMissing=false`)이면 열리지 않아야 한다.
   - 준비실 프리셋 `후면 주차 두 회차` → `이 설정으로 홈` → 홈 눈썹이 `연수생 · 장롱 10년차 · 목표 아이 등하원` 인가.
   - 화면 맨 아래 **띠**의 `정차`·`문 열기` 가 실차 신호와 함께 먹는가(리포트 배지 `실신호 N · 시뮬레이션 N`). `출발` 은 차를 움직이는 버튼이 아니라 `정차`(속도 0 고정)를 **푸는** 버튼이다 — 시나리오가 없을 때 누르면 아무 일도 없다.
   - `adb install -r` 재설치 뒤 **첫 실행 프로필 질문이 다시 나오지 않는가**(프로필 파일 유지). `adb shell pm clear --user 10 com.moah.hackathon` 뒤에는 나오는가 — AAOS 에뮬은 운전자 **사용자 10** 으로 앱이 돈다. 그냥 `pm clear` 는 사용자 0 만 지워 프로필·로그인이 남는다(사내 검증 #4).
   - **장내기능 모의시험**을 평가 모드로 한 번 — 지도 위 차가 움직이고, 감점 문장이 화면에 남는가.
6. **A안 녹화**: 문제가 없으면 준비실 프리셋으로 시작해 `docs/05_demo_script.md` 조작 순서대로(첫 실행 질문이 보이면 프리셋이 안 들어간 것). 시각은 곧 main 에서 새로 잰 값으로 바뀐다 — 순서는 같다.

## 결과 남기는 법

`docs/INTEGRATION.md` B절 형식의 **관찰 노트**로, 사람이 옮길 요지만 적는다. 문제가 있으면 무엇을 눌렀고 무엇이 보였는지 한 줄씩. **사내 캡처·로그 원문·계정·토큰·내부 호스트명은 적지 않는다**(public 저장소).
