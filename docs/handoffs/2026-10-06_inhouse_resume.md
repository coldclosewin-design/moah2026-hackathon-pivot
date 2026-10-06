# 사내 재검증 재개 — 태그 `inhouse-20261006-2` (2026-10-06)

사내 세션은 이 파일 하나를 보고 진행한다. **사내에서는 코드를 고치지 않는다 — 검증·녹화만.**
이 파일은 태그를 만든 뒤 main 에 들어갔으므로 태그 체크아웃 폴더에는 없다. clone 한 뒤 `git show origin/main:docs/handoffs/2026-10-06_inhouse_resume.md` 로 읽는다(빌드는 태그로).

## 순서

1. **저장소**: public clone 후 `git checkout inhouse-20261006-2` (= `0ab3c30`). clone 이 막히면 번들 `moah2026-20261006-2.bundle` 을 반입해 clone(그 브랜치 `inhouse/base-20261006-2` 가 바로 체크아웃된다).
2. **jar 복사 직후** `REPO_ONLY=1 bash tools/inhouse_check.sh` → `PASS (저장소만)`.
3. **빌드**: `local.properties` 에 `mobis.vss.jar=automotive/libs/mobis.framework.core.jar` 한 줄 → `./gradlew assembleDebug` (로그 첫 줄에 `USE_FAKE_VSS=false`).
4. **세션 준비**: `adb root` · Wi-Fi · `copilot_config.json` push · `adb logcat -G 16M` → `bash tools/inhouse_check.sh`. 요약 전체를 적는다 — 배지 `live=` 값, `missing=[…]`, 우리 앱 FATAL 수, Copilot 상태·fallback 수.
5. **이번 태그에서 새로 확인할 것**:
   - 홈 `DRIVE COACH` 를 약 2초 길게 → **준비실**이 열리는가(기본 Hybrid 빌드). 순수 Real(`-PfillMissing=false`)이면 열리지 않아야 한다.
   - 준비실 프리셋 `후면 주차 두 회차` → `이 설정으로 홈` → 홈 눈썹이 `연수생 · 장롱 10년차 · 목표 아이 등하원` 인가.
   - 화면 맨 아래 **띠**의 `정차`·`문 열기` 가 실차 신호와 함께 먹는가(리포트 배지 `실신호 N · 시뮬레이션 N`).
   - `adb install -r` 재설치 뒤 **첫 실행 프로필 질문이 다시 나오지 않는가**(프로필 파일 유지). `pm clear` 뒤에는 나오는가.
   - **장내기능 모의시험**을 평가 모드로 한 번 — 지도 위 차가 움직이고, 감점 문장이 화면에 남는가.
6. **A안 녹화**: 문제가 없으면 준비실 프리셋으로 시작해 `docs/05_demo_script.md` 조작 순서대로(첫 실행 질문이 보이면 프리셋이 안 들어간 것). 시각은 곧 main 에서 새로 잰 값으로 바뀐다 — 순서는 같다.

## 결과 남기는 법

`docs/INTEGRATION.md` B절 형식의 **관찰 노트**로, 사람이 옮길 요지만 적는다. 문제가 있으면 무엇을 눌렀고 무엇이 보였는지 한 줄씩. **사내 캡처·로그 원문·계정·토큰·내부 호스트명은 적지 않는다**(public 저장소).
