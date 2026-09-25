# tools — 에뮬 자동 시연 스크립트

> **17번(피벗) 저장소 주석 (2026-09-25)**: 세 스크립트는 아직 **Gift Drive 흐름**(기분 선택 → 픽업 → 최종 도착, `ConceptContractInstrumentation`)을 가리키므로 지금 상태로는 실패한다. 운전 연수 세션 화면이 들어오면(`docs/NEXT.md` Step 7) 탭 라벨·로그 태그(`MOAH/LessonStateMachine`)·단계 순서만 바꾼다. **아래 "이 스크립트가 지키는 규칙" 표의 함정들은 전부 그대로 유효**하니 새로 쓰지 말고 고쳐 쓴다.

외부 에뮬(`CSTDe_API_34`)에서 앱 흐름을 자동으로 재생하고 단계별 스크린샷을 남긴다. Git Bash 에서 실행.

```bash
# 기본(Fake 위치) 빌드를 설치한 뒤
bash tools/emu_flow.sh /tmp/flow          # 배고픔+1시간 → 픽업 → 다음 장소로 → 최종 도착
# GPS 모드 빌드(-PfakeLocation=false)를 설치한 뒤
bash tools/gps_flow.sh /tmp/gps           # 구간마다 geo fix 40개로 전체 여정(픽업 → 최종 도착 → 가는 길)을 GPS 로 주행
```

`gps_flow.sh` 가 확인하는 것: `GPS updates started` 가 구간마다 찍힘(안 찍히면 Fake 폴백으로 달린 것 — 빌드·권한 확인), 두 구간의 공개·도착(앱 로그), 최종 도착의 "다시 떠나기"·"가는 길". 약 3분. **끝나면 기본 빌드(`assembleDebug`)를 다시 설치한다** — GPS 빌드가 남아 있으면 `emu_flow.sh` 가 fix 를 기다리며 멈춘다.

결과: `<출력폴더>/log.txt`, `1x_*.png`. 끝에 앱 상태기계 로그, `uiautomator clashes` 건수, `result: PASS/FAIL` 을 찍는다(종료 코드도 같다).

`emu_flow.sh` 가 확인하는 것: 주문 4단계 전이(앱 로그), 공개 화면의 "준비 완료" 칩, 도착 화면의 "픽업 완료" 칩, 최종 도착 도달. `[t+NNs]` 줄은 여정 시작부터의 경과 시간이다 — 시연 길이는 `-PdemoSpeed=<배율>` 로 조절한다(`docs/05_demo_script.md`).

## concept_shots.sh — 디자인 컨셉 계약 검사 + 비교 스크린샷

여정을 돌리지 않고 고정 데이터로 기분·주행·공개·도착 화면을 바로 그려서(컨셉당 약 40초) `ConceptContractInstrumentation` 의 공통 규칙 7묶음을 검사하고 6장을 꺼낸다.

```bash
# 먼저: .\gradlew.bat assembleDebug :automotive:assembleDebugAndroidTest
bash tools/concept_shots.sh /tmp/concepts            # 등록된 컨셉 전부
bash tools/concept_shots.sh /tmp/concepts d-road     # 하나만
```

- 끝에 `== result: PASS` 여야 한다. 실패 메시지가 어긴 규칙을 말해 준다(예: `hidden leg leaked shop/menu/destination`).
- 검사가 실제로 잡는지 확인함(2026-09-18): 숨긴 구간에 메뉴를 그리는 임시 컨셉 → FAIL.
- 시작할 때 이전 실행의 스크린샷을 지운다(`run-as … --user 10`). 못 지우면 중단한다 — 예전에는 `--user 10` 이 빠져 다른 컨셉의 옛 파일이 이번 결과처럼 딸려 나왔다.
- `emu_flow.sh` 와 같은 잠금을 쓴다. 스크린샷은 앱 filesDir 에 저장되므로 `run-as … --user 10` 으로 꺼낸다.

## 계측 테스트 전부 돌리기

`concept_shots.sh` 는 공통 계약만 돌린다. 화면을 고친 뒤에는 나머지 둘도 돌린다(둘 다 `assembleDebugAndroidTest` 산출물이 설치돼 있어야 한다):

```bash
adb -s emulator-5554 shell am instrument --user 10 -w com.moah.hackathon.test/com.moah.hackathon.ui.JourneyScreenInstrumentation   # classic 10묶음
adb -s emulator-5554 shell am instrument --user 10 -w com.moah.hackathon.test/com.moah.hackathon.ui.DRoadInstrumentation           # d-road 23묶음(2026-09-21 기준, 화면 PR 마다 늘어난다)
```

`Unable to find instrumentation info` 가 나오면 `adb shell pm list instrumentation | grep moah` 로 등록된 항목을 본다. 3개가 아니면
`automotive/build.gradle.kts` 의 `testInstrumentationRunner` 주석을 읽어라 — AGP 가 매니페스트의 첫 항목 이름을 덮어쓴다.

## 이 스크립트가 지키는 것 (전부 실제로 당한 문제)

| 규칙 | 이유 |
|---|---|
| **한 번에 하나만 실행** (`emu_flow.lock` 디렉터리 잠금, 두 스크립트 공유) | 두 인스턴스가 겹치면 서로의 앱을 강제 종료하고 탭이 끼어들어 결과가 오염된다. 오염된 실행은 uiautomator "already registered" 충돌이 수십 건 찍힌다 → **끝의 충돌 건수가 0 이어야 유효한 실행** |
| **멈출 때는 `kill "$(cat <출력폴더의 상위>/emu_flow.pid)"`** | 이 환경(Git Bash + Claude 셸 스냅샷)의 `pkill` 은 없는 바이너리를 부르는 래퍼라 exit 127 로 아무것도 죽이지 않는다. `2>/dev/null` 로 가리면 멈춘 줄 착각한다 |
| **기기가 없으면 즉시 종료** | 기기 대기 상태로 남았다가 에뮬이 뜨면 뒤늦게 되살아나 다른 실행을 방해한다 |
| **덤프 전에 양쪽 파일 삭제, 실패는 `<DUMP_FAILED/>`** | 주행 화면처럼 계속 갱신되는 화면에서는 `uiautomator dump` 가 "could not get idle state" 로 실패하고 파일을 만들지 않는다. 예전 파일을 읽으면 낡은 화면을 현재로 착각한다 |
| **공개 단계도 앱 로그(`reveal N/M`)로 판단, 시연 버튼은 좌표를 기억해 둔다** | 공개 화면은 남은 거리(와 앞으로 지도)가 계속 갱신돼 주행 화면과 같은 이유로 덤프가 안 될 수 있다. 시연 버튼은 모든 화면에서 같은 자리라, 덤프가 될 때 좌표를 기억했다가 안 될 때 그 좌표로 누른다 |
| **여정 시작·진행은 앱 로그(`MOAH/JourneyStateMachine`)로 판단** (`wait_log`) | 위와 같은 이유. 화면 텍스트는 정지 화면(기분·공개·도착)에서만 믿는다 |
| **화면을 검사할 때는 `now_texts`** (덤프 후 읽기) | `texts` 는 마지막 덤프를 읽는다. 탭·대기 뒤에 `texts` 만 부르면 한 단계 전 화면이 나온다 — 멀쩡한 도착 화면을 "칩 없음"으로 오판한 적이 있다 |
| **탭 라벨은 정확히 일치** (`text="라벨"`) | 접두사 매칭이던 `gps_flow.sh` 가 "정차" 대신 공개 화면의 안내 문구 "정차 후 문을 열면 선물이 열려요" 를 눌러 도착으로 넘어가지 못했다(2026-09-19). 화면 문구는 Codex 가 바꿀 수 있다 |
| **`adb shell wm density <값>` 을 쓴 뒤에는 에뮬을 재부팅** (`adb reboot`) | `wm density reset` 으로 값은 160 으로 돌아오지만 에뮬이 불안정해진다: 타이밍에 민감한 계측(`DRoadInstrumentation` 의 "replay did not restart", "emphasis hold keeps invalidating its image")이 23묶음 중 4~9묶음에서 무작위로 실패했고, main 빌드도 똑같이 실패했다. 재부팅 후 main·작업 브랜치 모두 23/23 두 번 연속 통과(2026-09-21). 실패가 새 변경 탓인지 의심되면 **main 빌드로 같은 검사를 돌려 가른다** |
| `MSYS_NO_PATHCONV=1`, 모든 adb 호출에 `</dev/null`, 로컬 경로는 `cygpath -w` | 경로 변환으로 `/sdcard` 가 깨지고, stdin 이 열려 있으면 `adb shell` 이 멈춘다 |
| 권한·설정은 `--user 10` | AAOS 에뮬은 앱이 user 10 으로 실행된다 |
| `screencap -d 4619827259835644672` | 에뮬 디스플레이가 2개 |

프로세스 이름으로 실행 여부를 확인하지 말 것: 검사 명령 자신(과 그 자식 셸)이 패턴에 걸려 오탐이 난다. 잠금 디렉터리와 PID 파일이 기준이다.
