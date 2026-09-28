# tools — 에뮬 자동 시연 스크립트

외부 에뮬(`CSTDe_API_34`)에서 **후면 직각 주차 세션**을 자동으로 재생하고 단계별 스크린샷을 남긴다. Git Bash 에서 실행.
**두 스크립트는 화면(Step 5, Codex)이 들어와야 돈다** — 라벨·계측 클래스 이름은 `docs/handoffs/2026-09-26_codex_lesson_screens.md` 와 맞춰 두었다.
16번의 도로 흐름 스크립트(`gps_flow.sh`, `concept_shots.sh`)는 지웠다 — 필요하면 `git -C /c/Project/16_hackathon show 0c95d18:tools/<이름>`.

```bash
# 기본 빌드를 설치한 뒤 (PowerShell: .\gradlew.bat assembleDebug ; adb install -r automotive/build/outputs/apk/debug/automotive-debug.apk)
bash tools/emu_flow.sh /tmp/flow      # Setup(힌트) → 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 다 됐어요 → 문 열기 → Report
RESERVE=1 RECORD=/sdcard/demo.mp4 bash tools/emu_flow.sh build/rec   # 9/28: 예약 배지 상태로 시작(시트 → 제휴 시험장 → 서초 14:00 주차 3종 → 예약) + 예약 뒤부터 screenrecord(1920×1080, 180 s 한도) → build/rec/demo.mp4. 둘 다 기본 꺼짐, 판정 불변
# 계측 APK 까지 빌드한 뒤 (.\gradlew.bat assembleDebug :automotive:assembleDebugAndroidTest)
bash tools/lesson_shots.sh /tmp/shots  # 고정 데이터로 5화면 계약 검사 + 캡처
```

`emu_flow.sh` 가 확인하는 것: 세션 시작 로그, 못한 주차에서 **힌트 3종**(`hint: 안전벨트`·`뒤가 가까워요`·`제동이 급했어요`), 기어 P 뒤 `asked done`, 회차 1·2 채점 로그(`attempt N: skill=… safety=…`), 잘한 주차에서 **힌트 0건**, 도어 열림 → `report: attempts=2`, 리포트 화면의 "다시 시작"과 배지(실신호·시뮬레이션·미측정). `[t+NNs]` 는 세션 시작부터의 경과 시간 — 시연 길이는 `-PdemoSpeed=<배율>`(기본 1.0 = 실시간, 잘한 26 s·못한 44 s)로 조절한다.

결과: `<출력폴더>/log.txt`, `1x_*.png`(10 setup · 11 briefing · 12 maneuver hint · 13 parked · 14 done 1 · 15 maneuver good · 16 done 2 · 17 report). 끝에 앱 로그, `uiautomator clashes` 건수, `result: PASS/FAIL`(종료 코드도 같다).

## lesson_shots.sh — 화면 계약 검사 + 캡처

라운드 4 부터 계측이 끝에 **애니메이션 클립 2개**(조향 0° → 450° → 0°, Done 궤적 재생)를 `screenrecord --time-limit 5` 로 찍어 `/sdcard/lesson-round4-steering.mp4`·`/sdcard/lesson-round4-done.mp4` 에 남긴다(약 10 s 추가). 리뷰 때 `adb pull` 한 뒤 `ffmpeg -i x.mp4 -vf "fps=2,scale=640:-1,tile=5x2" strip.png` 로 프레임 스트립을 만들어 눈으로 본다. **경로에 `\\$var` 를 쓰지 말 것** — Git Bash 가 `$` 를 리터럴로 남겨 `shots$c.mp4` 같은 파일이 생긴다(9/28). 출력 폴더로 `cd` 한 뒤 상대 경로가 안전하다.

`LessonScreenInstrumentation`(Codex 소유)이 고정 데이터로 5화면을 그려 (a) `Maneuver` 잠금 상태에서 클릭 가능한 노드 0개 (b) `Maneuver` 접근성 트리에 "점수"·"감점"·"N점" 없음 (c) 캡처 `lesson-*.png` 를 `filesDir` 에 저장. 끝에 `Lesson contract passed` 를 찍어야 PASS.

`Unable to find instrumentation info` 가 나오면 `adb shell pm list instrumentation | grep moah` 로 등록된 항목을 보고, `automotive/build.gradle.kts` 의 `testInstrumentationRunner` 주석을 읽어라 — AGP 가 매니페스트의 첫 항목 이름을 덮어쓴다.

## 이 스크립트가 지키는 것 (전부 16번에서 실제로 당한 문제)

| 규칙 | 이유 |
|---|---|
| **한 번에 하나만 실행** (`emu_flow.lock` 디렉터리 잠금, 두 스크립트 공유) | 두 인스턴스가 겹치면 서로의 앱을 강제 종료하고 탭이 끼어들어 결과가 오염된다. 오염된 실행은 uiautomator "already registered" 충돌이 수십 건 찍힌다 → **끝의 충돌 건수가 0 이어야 유효한 실행** |
| **멈출 때는 `kill "$(cat <출력폴더의 상위>/emu_flow.pid)"`** | 이 환경(Git Bash + Claude 셸)의 `pkill` 은 없는 바이너리를 부르는 래퍼라 exit 127 로 아무것도 죽이지 않는다 |
| **기기가 없으면 즉시 종료** | 기기 대기 상태로 남았다가 에뮬이 뜨면 뒤늦게 되살아나 다른 실행을 방해한다 |
| **덤프 전에 양쪽 파일 삭제, 실패는 `<DUMP_FAILED/>`** | 도식처럼 계속 갱신되는 화면에서는 `uiautomator dump` 가 "could not get idle state" 로 실패하고 파일을 만들지 않는다. 예전 파일을 읽으면 낡은 화면을 현재로 착각한다 |
| **진행 판정은 앱 로그(`MOAH/LessonStateMachine`)로** (`wait_log`), 버튼은 좌표를 기억해 둔다 | 위와 같은 이유. 화면 텍스트는 정지 화면(Setup·Done·Report)에서만 믿는다. 시연 패널·회차 버튼은 화면마다 같은 자리라 덤프가 될 때 좌표를 기억했다가 안 될 때 그 좌표로 누른다 |
| **화면을 검사할 때는 `now_texts`** (덤프 후 읽기) | `texts` 는 마지막 덤프를 읽는다. 탭·대기 뒤에 `texts` 만 부르면 한 단계 전 화면이 나온다 |
| **`lesson_shots` 의 adb 타임아웃 300 s** (2026-09-28, 120 → 300) | 계약이 12묶음(≈2 분)이 되자 120 s 에 `am instrument -w` 가 끊겨 화면 검사는 전부 PASS 인데 결과 보고("Lesson contract passed")만 못 받아 FAIL 로 찍혔다(기기 로그 `Failure reporting to instrumentation watcher`). 묶음이 더 늘면 다시 올린다 |
| **탭 라벨은 정확히 일치** (`text="라벨"`) | 접두사 매칭은 안내 문구를 잘못 누른다(16번 2026-09-19). 라벨은 발주서의 `strings.xml` 문자열 그대로 |
| **`adb shell wm density <값>` 을 쓴 뒤에는 에뮬을 재부팅** | `wm density reset` 뒤 에뮬이 불안정해져 타이밍 계측이 무작위로 실패했다(16번 2026-09-21). 실패가 새 변경 탓인지 의심되면 **main 빌드로 같은 검사를 돌려 가른다** |
| `MSYS_NO_PATHCONV=1`, 모든 adb 호출에 `</dev/null`, 로컬 경로는 `cygpath -w` | 경로 변환으로 `/sdcard` 가 깨지고, stdin 이 열려 있으면 `adb shell` 이 멈춘다 |
| 권한·설정은 `--user 10` | AAOS 에뮬은 앱이 user 10 으로 실행된다 |
| `screencap -d 4619827259835644672`, 탭은 그냥 `input tap`(`-d` 는 실패) | 에뮬 디스플레이가 2개 |
| **Gradle 은 PowerShell 로** | Git Bash 에서 `cmd //c gradlew.bat` 은 이 환경에서 실행되지 않는다(2026-09-26) |

프로세스 이름으로 실행 여부를 확인하지 말 것: 검사 명령 자신이 패턴에 걸려 오탐이 난다. 잠금 디렉터리와 PID 파일이 기준이다.
