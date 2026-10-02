# 사내 이관 런북 — clone 부터 제출까지

사내 첫날 이 문서를 위에서 아래로 따라간다. 세부 체크 항목과 가정 로그는 [INTEGRATION.md](INTEGRATION.md), 시연 진행은 [05_demo_script.md](05_demo_script.md).

> 이 문서는 **외부에서 쓴 계획**이다. 사내 환경(WebIDE, infoLINK 에뮬, VSS 서비스)은 외부에서 본 적이 없다. "외부에서 확인함"이라고 적힌 것만 사실이고, 나머지는 첫날 확인할 가정이다. 다르면 이 문서를 고치지 말고 메모해 두었다가 외부에서 반영한다(아래 7절).

> **화면 크기(2026-09-21 추가)**: 앱은 `DesignScale` 로 기기 밀도와 무관하게 2560×1268 dp 의 창으로 그린다. 사내 첫 실행 때 `adb logcat -s MOAH/DesignScale` 한 줄을 확인한다 — `window=` 의 픽셀 크기·비율이 2560×1268 과 크게 다르면 화면 캡처를 떠서 외부로 가져올 수 없으니 **그 한 줄의 숫자만** 적어 온다(`INTEGRATION.md` B절).

## 0. 외부에서 미리 해 둘 것

- [x] `main` 이 `.\gradlew.bat assembleDebug testDebugUnitTest` 통과, `bash tools/emu_flow.sh <폴더>` 가 `result: PASS` — 태그마다 반복
- [x] 이관할 커밋에 태그: `git tag inhouse-YYYYMMDD-N && git push origin inhouse-YYYYMMDD-N` (사내에서 뭘 가져갔는지 나중에 알 수 있게). 번들도 같이: `git bundle create build/moah2026-YYYYMMDD-N.bundle HEAD refs/heads/main refs/tags/inhouse-YYYYMMDD-N` → 빈 폴더에 clone 테스트
- [x] 사내에서 저장소에 닿는 방법: **public 저장소 익명 HTTPS clone**(개인 계정 로그인 불가 → private 는 안 됨, 10/1 확정). 막히면 `git bundle`(`build/moah2026-*.bundle`, 태그·main 포함) 을 반입 절차에 따라 옮긴다
- [x] 사내 Gradle 이 외부 저장소에 닿는다(9/30 1차 이관에서 **A 경로로 빌드 성공**). 1절 B(템플릿에 얹기)는 그것이 막힐 때의 대안이다

## 1. 코드 가져오기

### A. 저장소를 그대로 빌드 (**현재 방식** — 9/30·10/1 사내에서 이 경로로 빌드됨)

```bash
git clone https://github.com/coldclosewin-design/moah2026-hackathon-pivot.git moah2026 && cd moah2026 && git checkout inhouse-YYYYMMDD-N
```

그다음 2절(설정 한 줄 + jar) → 3절 빌드 → 4절 설치. **코드는 고치지 않는다.**

### B. 사내 템플릿에 얹기 (대안 — A 의 의존성 resolve 가 사내망에서 막힐 때만. 9/30·10/1 에는 필요 없었다)

```bash
git clone ssh://<사내 Bitbucket>/mobis_sw_hackathon/moah_template_app.git
```

템플릿의 `automotive` 모듈 위에 아래를 복사한다. **템플릿의 Gradle 래퍼·`settings.gradle*`·루트 `build.gradle*` 는 건드리지 않는다.**

| 가져갈 것 | 비고 |
|---|---|
| `automotive/src/main/kotlin/com/moah/hackathon/**` | 앱 전체. `mobis.vss` 를 직접 참조하는 파일은 `vehicle/RealVehiclePort.kt` 하나 |
| `automotive/src/main/res/**`, `automotive/src/main/AndroidManifest.xml` | 템플릿 매니페스트와 **병합**: `android:name=".App"`, `MainActivity`, `distractionOptimized`, 위치 권한 2개 |
| `automotive/src/test/**` | 선택. 사내에서 단위 테스트가 시스템 jar 를 못 읽으면 건너뛴다 |
| `automotive/build.gradle.kts` 의 **내용** | 파일째 덮어쓰지 말고 템플릿 것에 옮겨 적는다: `namespace`/`applicationId`, `buildConfigField` **7개**(`USE_FAKE_VSS`, `FILL_MISSING_WITH_FAKE`, `USE_FAKE_LOCATION`, `TTS_VOICE`, `DEMO_SPEED_FACTOR`, `SHOW_DEMO_PANEL`, `CLOUD_COACH` — 하나라도 빠지면 `BuildConfig.…` 컴파일 오류. 값은 외부 `automotive/build.gradle.kts` 의 기본값 그대로, `USE_FAKE_VSS` 만 false), `buildFeatures { buildConfig = true; compose = true }`, Compose·coroutines·lifecycle 의존성, 사내 jar `compileOnly(files(...))`, 매니페스트의 `uses-library mobis.framework`·`INTERNET` 권한 |
| `gradle/libs.versions.toml` 의 버전 | 템플릿에 버전 카탈로그가 없으면 의존성을 좌표 문자열로 풀어 적는다 |

가져가지 **않는** 것: `vss-stub/`(실물과 동시에 있으면 duplicate class), `tools/`(외부 에뮬 전용), `emulator/`, `tmp-info/`.

템플릿이 Compose 를 안 쓰거나 Kotlin/AGP 버전이 다르면: 외부 기준은 AGP 8.7.3 / Kotlin 2.0.21 / Compose 컴파일러 플러그인(`org.jetbrains.kotlin.plugin.compose`) / JDK 17 / compileSdk 35. Kotlin 이 2.0 미만이면 Compose 컴파일러 설정 방식이 다르다(`composeOptions.kotlinCompilerExtensionVersion`) — 템플릿 버전을 보고 그 자리에서 결정한다.

## 2. 스위치 (설정 한 줄 + jar 파일 — 코드 변경 없음)

2026-09-30 사내 이관 1차 뒤 "두 줄 교체" 를 없앴다. 같은 커밋이 사외(Fake)와 사내(Real)에서 그대로 빌드된다.

1. 사내 jar 를 `automotive/libs/mobis.framework.core.jar` 로 복사한다(`automotive/libs/` 는 gitignore — 절대 커밋하지 않는다. 복사 뒤 `git status` 에 `automotive/libs/` 가 **안 보여야** 정상 — 검증 #3 에서 뒤 글자 주석 때문에 보였던 것은 #96 으로 고침). jar 경로는 가이드 문서 기준이고 WebIDE 에서 다르면 `find / -name "mobis.framework*.jar" 2>/dev/null` 로 찾는다.
2. `local.properties`(추적 안 됨)에 한 줄:

```properties
mobis.vss.jar=automotive/libs/mobis.framework.core.jar
```

효과: `settings.gradle.kts` 가 `:vss-stub` 을 포함하지 않고, `automotive/build.gradle.kts` 가 그 jar 로 `compileOnly` + `USE_FAKE_VSS=false`. 빌드 로그 첫 줄 `mobis.vss: jar … → USE_FAKE_VSS=false` 로 확인. 키를 지우면 사외와 같다.

매니페스트의 `<uses-library android:name="mobis.framework" android:required="false"/>` 가 런타임에 `mobis.vss` 를 시스템에서 받게 한다(사외 에뮬에서는 없어도 설치·실행이 막히지 않는다). **10/1 사내 검증 #2**: 이 줄이 e541131 커밋에서 빠져(본문에만 적힘) 사내 Real 빌드가 `NoClassDefFoundError: mobis/vss/VSSManager` 로 Fake 폴백했다 → PR #68 에서 보강. 사내에서 임시로 넣으니 실신호 7 · 시뮬레이션 1.

`gradlew` 는 실행 비트가 있어야 한다(사내 Linux 에서 126 으로 실패했던 것 — 9/30 부터 저장소에 `+x` 로 들어 있다. 안 되면 `chmod +x gradlew`).

## 3. 빌드 — 예상되는 실패와 대응

```bash
./gradlew assembleDebug
```

| 증상 | 원인 | 대응 |
|---|---|---|
| `exception RemoteException is never thrown` | 어딘가에서 RemoteException 을 catch | 외부에서는 0건(grep 확인). 나오면 그 catch 를 지운다 |
| `cannot find symbol` / 인자 타입 불일치 (`VSSManager`, `VSSAppData`, 리스너) | 스텁 시그니처 ≠ 실물 | 고칠 곳은 **`RealVehiclePort.kt` 하나**. 실제 시그니처를 메모 → 외부에서 `vss-stub` 과 `02_vss_api_contract.md` 갱신 |
| `VssConstants.XXX` 없음 | 상수 이름이 실물과 다름 | 상수 이름은 **경로를 대문자·밑줄로 바꾼 것**(9/30 사내 규칙, AGENTS 규칙 4). 앱이 쓰는 12개(아래)는 그 규칙으로 이미 맞췄다. 그래도 없으면 실물 이름으로 바꾸고 메모 |
| `Duplicate class mobis.vss.*` | `vss-stub` 이 아직 include 됨 | 2절 |
| 의존성 resolve 실패 / 타임아웃 | 사내망에서 외부 저장소 차단 | 경로 B 로 전환(템플릿의 저장소 설정 사용) |
| 단위 테스트가 jar 를 못 읽음 | `testImplementation(vssApi)` | `assembleDebug` 만. 테스트는 외부에서 이미 통과 |

앱이 참조하는 VSS 상수: **A층(필수)** `VEHICLE_SPEED`, `VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN` — 이 둘만 있으면 세션·채점·리포트가 전부 성립한다. **B층(선택)** 안전벨트·기어·방향지시등·조향각·브레이크·비상등·IGN — `docs/topics/01_driving_coach.md` 신호 표. 경로가 실물과 다르면 **컴파일은 되지만 값이 안 온다**(조용히 무시) → 앱이 `MISSING` 으로 표시하고 채점에서 뺀다. 이름이 비슷하게 있으면 `VssConstants` 의 문자열만 고쳐 살린다. `VEHICLE_ADAS_ABS_ISENABLED`, `VEHICLE_ADAS_CRUISECONTROL_SPEEDSET`, `VEHICLE_BODY_HORN_ISACTIVE` 는 초기 대시보드·Fake 기본값용이라 없으면 지워도 된다.

## 4. 설치 · 첫 실행 · Real 모드에서 달라지는 것

```bash
adb devices
adb install -r automotive/build/outputs/apk/debug/automotive-debug.apk
adb logcat -s "MOAH/VehiclePortFactory:*" "MOAH/LessonStateMachine:*" "MOAH/RealVehiclePort:*" "MOAH/DesignScale:*"
```

설치 뒤 런처에서 **"드라이브 코치"** 를 연다(또는 `adb shell am start -n com.moah.hackathon/.ui.MainActivity`). **성공 판정**: Setup 화면("오늘은 가볍게, 주차부터 해볼까요?" + 우상단 `시연` 알약)이 뜨고 아래 첫 줄 로그가 `RealVehiclePort ready` 다. 사람 손 대신 `bash tools/inhouse_check.sh` 가 설치부터 요약까지 한다(`docs/07` §3).

첫 줄 로그로 어느 포트가 붙었는지 본다.

| 로그 | 뜻 | 다음 |
|---|---|---|
| `RealVehiclePort ready` | VSS 서비스 연결 | 5절 |
| `VSS service unavailable → falling back to FakeVehiclePort` | `getInstance()` 가 null | `adb shell service list \| grep -i vss`. 에뮬 이미지·서비스 기동 문제 → 운영진 문의. 앱은 Fake 로 계속 동작 |
| `mobis.vss not present at runtime → falling back…` / `NoClassDefFoundError: mobis/vss/VSSManager` | 런타임에 클래스 없음 — **원인 둘** | ① 이 에뮬이 infoLINK 이미지가 아니다(**외부에서 확인함**: 순정 AAOS 에뮬에 Real 빌드를 설치하면 크래시 없이 이 로그를 남기고 Fake 로 돈다, 2026-09-18) ② **매니페스트에 `<uses-library android:name="mobis.framework" android:required="false"/>` 가 없다**(10/1 사내 검증 #2 에서 실제로 이것이었다 — `aapt dump badging automotive-debug.apk \| grep uses-library` 로 확인) |

Real 모드에서 달라지는 동작:

- **시연 패널("시연" 토글 · 잘한/못한 주차·점검 · 정차/출발 · 문 열기/닫기 · AI 코치 줄)은 기본 빌드에서 사내에서도 보인다** — `USE_FAKE_VSS=false` 면 `HybridVehiclePort(real, fake)` 다. **9/30 보정(write-through)**: 패널·시나리오의 조작은 Fake 가 저장하기 전에 **실물에 먼저 `setVSS`** 되고 실물 구독으로 돌아온다 → 실물이 주는 키(속도·조향·기어·벨트·시동)에도 패널이 **먹는다**(10/1 실측: 시나리오로 가이드가 끝까지 진행). 실물이 쓰기를 거부한 키(읽기 전용 sensor)만 `forced` 로 Fake 값이 화면까지 간다(배지 "시뮬레이션"). 어느 쪽이든 리포트 배지 `실신호 N · 시뮬레이션 N` 이 진실이다: logcat `MOAH/HybridVehiclePort` 의 `live +[…]`·`setVSS rejected […]` 줄.
- 회차 시작 로그 `attempt N start (MODE) missing=[…]` 의 **`missing` 목록**이 실물에서 값이 오지 않은 키다(10/1 실측: 주차 8키 중 `Rear.Distance` 만 — 실물에 없어 Fake 전용). Hybrid 라 Fake 가 채운 키도 "온 것"으로 잡힌다 → 실신호 여부는 배지로 본다.
- 단계 전환 조건: 잠금 `Speed > 5`(Maneuver·Quiz·**Done·Report·QuizDone**, #80), 리포트는 `Speed < 1` **그리고** 운전석 도어 열림 **또는** `오늘은 여기까지` 버튼(도어 신호가 없어도 된다). 회차 종료는 "다 됐어요" 버튼(기어 P + 정차를 보면 앱이 먼저 "다 되셨나요?" 를 묻는다). 위치·GPS 는 주차 과제에 쓰지 않는다.
- 시작 가능한 과제는 **출발 전 점검 · 후면 직각 주차 · 지식 테스트** 셋(`Task.status == READY`). 나머지 과제·맞지 않는 모드는 상태기계가 거부하고 음성으로 알려 준다 — 사내에서 잘못 눌러도 주차 채점기가 돌지 않는다. **출발 전 점검은 실차에서 가장 먼저 돌려 볼 과제다** — 움직이지 않고 **7단계**(문 → 벨트 → 주차 기어 → 브레이크+시동 → 좌 지시등 → 우 지시등 → 비상등)의 신호만 있으면 되므로, B층 신호가 실제로 바뀌는지(`missing=[…]` 가 비는지, 칩이 `확인` 으로 바뀌는지) 확인하는 가장 싼 방법이다.
- **AI 코치**: `copilot_config.json` 을 `/data/local/tmp/` 에 push 하고 패널 `AI 연결` → GitHub 에서 코드 입력 + **Authorize** 까지(15분 만료). OAuth 는 device-protected 저장소라 `install -r` 뒤에도 남는다(10/1 실측: 저장 위치가 사내 참고 구현과 달라 첫 1회 재로그인). 설정 파일이 없거나 로그인 전·실패면 `CloudCoachPort` 가 시드 문장으로 **자동 폴백**(logcat `MOAH/CloudCoachPort … fallback`, 패널 `AI 코치 · 설정 없음 / 로그인 필요`).

## 5. 신호 확인 (INTEGRATION.md A절과 같이 본다)

1. Signal Simulator 에서 `Vehicle.Speed` 를 바꾼다 → `Maneuver` 우상단 속도 숫자가 따라오는지, 5 를 넘기면 버튼·패널이 사라지는지. **안 따라오면** 경로 오타(조용히 무시됨) 또는 구독 실패 → `MOAH/RealVehiclePort` 로그.
2. 값 포맷을 로그로 확인: 속도가 `"3.0"` 인지 `"3"` 인지, 정차 시 정확히 `0` 인지(노이즈가 있으면 `STOP_SPEED_KMH = 1` 을 못 넘어 "다 됐어요" 가 안 뜬다), 도어가 `"true"/"false"` 소문자인지. 파서는 `VssValues.kt`·`VssGear.kt` 두 곳.
3. **B층 키 10개 대조** (`docs/topics/01_driving_coach.md` §5 표 ↔ pageId 1323873443): 회차 시작 로그 `missing=[…]` 에 든 키는 실물 이름이 다르다. 비슷한 경로를 찾아 `VssConstants.java` 의 문자열과 **상수 이름을 함께**(이름 = 경로 대문자·밑줄 규칙) 고친다 — 단 사내에서 고치지 않고 메모만(7절). 특히 `SelectedGear`(P=126/D=127 인코딩이 다르면 `VssGear.kt` 파서), `SteeringWheel.Angle` 부호(양수=왼쪽 가정), `ObstacleDetection.Rear.Distance`(비표준 — 없을 가능성 가장 높음 → `IsWarning` 만으로 근접 경고).
4. Signal Simulator 에 조향각·기어가 있으면 넣어 본다 → 도식의 바퀴·기어 글자가 따라오고 칩이 "시뮬" → **"실신호"** 로 바뀌는지. 리포트 배지의 `실신호 N` 이 올라간 화면을 **메모로만** 적어 온다(캡처 반출 금지).
5. 도어 `Vehicle.Cabin.Door.Row1.DriverSide.IsOpen` 을 3D 에뮬에서 연다(정차 상태에서) → `Done` 또는 `Maneuver` 에서 `Report` 로 넘어가는지. 주행 중 열면 무해해야 한다.
6. 콜백 스레드·ANR: **시작** 을 눌러 Briefing → Maneuver 로 갈 때 화면이 멎지 않는지.
7. 앱을 나갔다 들어와도 값이 계속 오는지, 로그에 구독 중복이 없는지.
8. TTS: `MOAH/AndroidTtsPort` 의 `ko voices`·`voice=` 줄. kob 가 없으면 로컬 최고 품질로 내려간다 — 목소리가 바뀌면 대본 시각 재측정.

**시나리오 재생기는 사내에서도 쓸 수 있다**(Hybrid) — Signal Simulator 에 없는 신호는 패널의 "못한 주차"가 채운다. 단 실물이 주는 속도·도어는 실물이 이기므로, 시나리오와 실물 속도가 싸우면 화면이 이상해진다 → 실물 속도를 0 에 두고 재생하거나, 속도까지 실물로 조작하고 시나리오는 끈다.

## 6. 안 될 때의 결정표

| 상황 | 결정 |
|---|---|
| VSS 서비스가 끝내 안 붙는다 | `local.properties` 의 `mobis.vss.jar` 줄을 지우고 빌드하면 사외와 같은 Fake 빌드 — 코드 수정 없이 시연·제출 가능. 발표에서는 "포트 하나로 Fake/Real 전환, Real 은 사내 검증 #2 에서 확인" 이라고 사실대로 말한다 |
| 속도는 오는데 도어 신호를 조작할 수 없다 | 리포트는 `오늘은 여기까지` 버튼으로도 들어간다(도어 없이). 도어 전환 자체가 필요하면 메모(7절) → 사외에서 상태기계(`feature/lesson`, Claude 소유) |
| TTS 엔진이 없다 | 그대로 진행. 자막 패널이 같은 문장을 보여준다(`AndroidTtsPort` 가 엔진 없음을 흡수). 사내 에뮬은 TTS·사운드 없음(9/30) — 대본 타이밍은 자막 기준 |
| Cloud Copilot 로그인이 안 된다 / 네트워크가 없다 | 아무것도 바꾸지 않는다 — `CloudCoachPort` 가 시드 문장으로 자동 폴백(회차 멘트·총평 전부). 패널 `AI 코치` 줄이 상태를 보여 준다. `CLOUD_COACH=false` 빌드(`-PcloudCoach=false`)로 AI 줄 자체를 숨길 수도 있다 |
| 화면이 2560×1440 이 아니다 | 레이아웃은 weight 기반. 깨지는 화면을 스크린샷 대신 **글로** 메모 → Codex 핸드오프 |

## 7. 사내 → 외부로 되가져오기 (소스·스크린샷 반출 금지)

사내에서 고친 코드, 사내 화면 캡처, 에뮬 이미지는 가져오지 않는다. 가져오는 것은 **내가 손으로 적은 사실**뿐이다:

```
- [날짜] [stub] 실제 시그니처: <메서드> (<인자 타입>) → <반환 타입>
- [날짜] [real] 속도 포맷 "60.0", 주기 약 N ms, 정차 시 "0.0"
- [날짜] [env] GPS 제공자 있음/없음, TTS 있음/없음, GMS 있음/없음
```

외부에서 이것을 `INTEGRATION.md` B절(가정 → **확인 완료**/정정)과 `02_vss_api_contract.md`·`vss-stub` 에 반영하고, 다음 이관 태그를 만든다. 사내에서만 고친 채로 두면 다음 이관 때 같은 수정을 반복한다.

## 8. 제출 (4종 — 마감 2026-10-09(금), 체크리스트는 `docs/07_two_site_workflow.md` §4)

1. **소스** → 팀 Bitbucket 프로젝트(`<사내 Bitbucket>/projects/MOBIS_SW_HACKATHON`) 의 **`submission` 브랜치** = 사외 태그 + `inhouse:` 커밋 1개(사내 jar — 그 브랜치에서만 `.gitignore` 의 `automotive/libs/` 를 풀고, `local.properties` 대신 README 한 줄). `main` 에는 jar 를 절대 넣지 않는다
2. **APK** → WebIDE `market_uploader` 로 `automotive-debug.apk` 제출 — 카테고리 **VEHICLE**, 재업로드마다 `versionCode` +1(`automotive/build.gradle.kts`)
3. **시연 영상** → 3D 에뮬 연동 화면 녹화(A안). 진행은 [05_demo_script.md](05_demo_script.md), 조작만 시연 버튼 → Signal Simulator 로 바뀐다. 자막·나레이션 원고는 `presentation/03_submission_onepager.md` B3. 형식·길이는 주최 공지 확인
4. **PPT 1장**(주최 양식) → 원고 `presentation/03_submission_onepager.md` B1(짧/중/장 3안·캡처 3장). PPT·영상은 MOAH@mobis.com 으로 — 메일 주소·양식은 pageId 1317526558 에서 재확인
