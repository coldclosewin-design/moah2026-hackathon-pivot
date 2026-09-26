# 사내 이관 런북 — clone 부터 제출까지

사내 첫날 이 문서를 위에서 아래로 따라간다. 세부 체크 항목과 가정 로그는 [INTEGRATION.md](INTEGRATION.md), 시연 진행은 [05_demo_script.md](05_demo_script.md).

> 이 문서는 **외부에서 쓴 계획**이다. 사내 환경(WebIDE, infoLINK 에뮬, VSS 서비스)은 외부에서 본 적이 없다. "외부에서 확인함"이라고 적힌 것만 사실이고, 나머지는 첫날 확인할 가정이다. 다르면 이 문서를 고치지 말고 메모해 두었다가 외부에서 반영한다(아래 7절).

> **화면 크기(2026-09-21 추가)**: 앱은 `DesignScale` 로 기기 밀도와 무관하게 2560×1268 dp 의 창으로 그린다. 사내 첫 실행 때 `adb logcat -s MOAH/DesignScale` 한 줄을 확인한다 — `window=` 의 픽셀 크기·비율이 2560×1268 과 크게 다르면 화면 캡처를 떠서 외부로 가져올 수 없으니 **그 한 줄의 숫자만** 적어 온다(`INTEGRATION.md` B절).

## 0. 외부에서 미리 해 둘 것

- [ ] `main` 이 `.\gradlew.bat assembleDebug testDebugUnitTest` 통과, `bash tools/emu_flow.sh <폴더>` 가 `result: PASS`
- [ ] 이관할 커밋에 태그: `git tag inhouse-YYYYMMDD && git push origin inhouse-YYYYMMDD` (사내에서 뭘 가져갔는지 나중에 알 수 있게)
- [ ] 사내에서 GitHub private 저장소에 닿는 방법 확인(HTTPS + 토큰? 프록시?). **안 되면** `git bundle create moah.bundle main` 또는 소스 zip 을 반입 절차에 따라 옮긴다
- [ ] 사내 Gradle 이 외부 저장소(google, mavenCentral, services.gradle.org)에 닿는지 모른다 → 1절의 "B. 템플릿에 얹기"를 기본 경로로 생각해 둔다

## 1. 코드 가져오기

### A. 저장소를 그대로 빌드 (인터넷·저장소 접근이 되는 경우)

```bash
git clone <GitHub private URL> moah2026 && cd moah2026 && git checkout inhouse-YYYYMMDD
```

### B. 사내 템플릿에 얹기 (권장 기본값 — 사내 Gradle 설정·미러를 그대로 쓴다)

```bash
git clone ssh://git@bitbucket.mobis.co.kr:7999/mobis_sw_hackathon/moah_template_app.git
```

템플릿의 `automotive` 모듈 위에 아래를 복사한다. **템플릿의 Gradle 래퍼·`settings.gradle*`·루트 `build.gradle*` 는 건드리지 않는다.**

| 가져갈 것 | 비고 |
|---|---|
| `automotive/src/main/kotlin/com/moah/hackathon/**` | 앱 전체. `mobis.vss` 를 직접 참조하는 파일은 `vehicle/RealVehiclePort.kt` 하나 |
| `automotive/src/main/res/**`, `automotive/src/main/AndroidManifest.xml` | 템플릿 매니페스트와 **병합**: `android:name=".App"`, `MainActivity`, `distractionOptimized`, 위치 권한 2개 |
| `automotive/src/test/**` | 선택. 사내에서 단위 테스트가 시스템 jar 를 못 읽으면 건너뛴다 |
| `automotive/build.gradle.kts` 의 **내용** | 파일째 덮어쓰지 말고 템플릿 것에 옮겨 적는다: `namespace`/`applicationId`, `buildConfigField` 5개(`USE_FAKE_VSS`, `USE_FAKE_LOCATION`, `UI_CONCEPT`, `TTS_VOICE`, `DEMO_SPEED_FACTOR` — 하나라도 빠지면 `BuildConfig.…` 컴파일 오류. 값은 외부 `automotive/build.gradle.kts` 의 기본값 그대로), `buildFeatures { buildConfig = true; compose = true }`, Compose·coroutines·lifecycle 의존성, `compileOnly(vssApi)` |
| `gradle/libs.versions.toml` 의 버전 | 템플릿에 버전 카탈로그가 없으면 의존성을 좌표 문자열로 풀어 적는다 |

가져가지 **않는** 것: `vss-stub/`(실물과 동시에 있으면 duplicate class), `tools/`(외부 에뮬 전용), `emulator/`, `tmp-info/`.

템플릿이 Compose 를 안 쓰거나 Kotlin/AGP 버전이 다르면: 외부 기준은 AGP 8.7.3 / Kotlin 2.0.21 / Compose 컴파일러 플러그인(`org.jetbrains.kotlin.plugin.compose`) / JDK 17 / compileSdk 35. Kotlin 이 2.0 미만이면 Compose 컴파일러 설정 방식이 다르다(`composeOptions.kotlinCompilerExtensionVersion`) — 템플릿 버전을 보고 그 자리에서 결정한다.

## 2. 스위치 (이게 전부여야 한다)

`automotive/build.gradle.kts`:

```kotlin
// val vssApi: Any = project(":vss-stub")
val vssApi: Any = files("/system/framework/mobis.framework.core.jar")
buildConfigField("boolean", "USE_FAKE_VSS", "false")
```

`settings.gradle.kts` 에서 `include(":vss-stub")` 제거 (경로 A 일 때만. B 는 애초에 없다).

jar 경로는 가이드 문서 기준이다. WebIDE 에서 다르면 `find / -name "mobis.framework*.jar" 2>/dev/null` 로 찾는다.

## 3. 빌드 — 예상되는 실패와 대응

```bash
./gradlew assembleDebug
```

| 증상 | 원인 | 대응 |
|---|---|---|
| `exception RemoteException is never thrown` | 어딘가에서 RemoteException 을 catch | 외부에서는 0건(grep 확인). 나오면 그 catch 를 지운다 |
| `cannot find symbol` / 인자 타입 불일치 (`VSSManager`, `VSSAppData`, 리스너) | 스텁 시그니처 ≠ 실물 | 고칠 곳은 **`RealVehiclePort.kt` 하나**. 실제 시그니처를 메모 → 외부에서 `vss-stub` 과 `02_vss_api_contract.md` 갱신 |
| `VssConstants.XXX` 없음 | 상수 이름이 실물과 다름 | 앱이 쓰는 상수는 5개(아래). 실물 이름으로 바꾸고 메모 |
| `Duplicate class mobis.vss.*` | `vss-stub` 이 아직 include 됨 | 2절 |
| 의존성 resolve 실패 / 타임아웃 | 사내망에서 외부 저장소 차단 | 경로 B 로 전환(템플릿의 저장소 설정 사용) |
| 단위 테스트가 jar 를 못 읽음 | `testImplementation(vssApi)` | `assembleDebug` 만. 테스트는 외부에서 이미 통과 |

앱이 참조하는 VSS 상수: **A층(필수)** `VEHICLE_SPEED`, `DOOR_DRIVER_ISOPEN` — 이 둘만 있으면 세션·채점·리포트가 전부 성립한다. **B층(선택)** 안전벨트·기어·방향지시등·조향각·브레이크·비상등·IGN — `docs/topics/01_driving_coach.md` 신호 표. 경로가 실물과 다르면 **컴파일은 되지만 값이 안 온다**(조용히 무시) → 앱이 `MISSING` 으로 표시하고 채점에서 뺀다. 이름이 비슷하게 있으면 `VssConstants` 의 문자열만 고쳐 살린다. `VEHICLE_ADAS_ABS_ISENABLED`, `VEHICLE_ADAS_CRUISECONTROL_SPEEDSET`, `VEHICLE_BODY_HORN_ISACTIVE` 는 초기 대시보드·Fake 기본값용이라 없으면 지워도 된다.

## 4. 설치 · 첫 실행 · Real 모드에서 달라지는 것

```bash
adb devices
adb install -r automotive/build/outputs/apk/debug/automotive-debug.apk
adb logcat -s "MOAH/VehiclePortFactory:*" "MOAH/LessonStateMachine:*" "MOAH/RealVehiclePort:*" "MOAH/DesignScale:*"
```

첫 줄 로그로 어느 포트가 붙었는지 본다.

| 로그 | 뜻 | 다음 |
|---|---|---|
| `RealVehiclePort ready` | VSS 서비스 연결 | 5절 |
| `VSS service unavailable → falling back to FakeVehiclePort` | `getInstance()` 가 null | `adb shell service list \| grep -i vss`. 에뮬 이미지·서비스 기동 문제 → 운영진 문의. 앱은 Fake 로 계속 동작 |
| `mobis.vss not present at runtime → falling back…` | 런타임에 클래스 없음 | 이 에뮬은 infoLINK 이미지가 아니다. **외부에서 확인함**: 순정 AAOS 에뮬에 `USE_FAKE_VSS=false` 빌드를 설치하면 크래시 없이 이 로그를 남기고 Fake 로 돈다(2026-09-18) |

Real 모드에서 달라지는 동작:

- **시연 패널("시연" 토글 · 잘한/못한 주차 · 정차/출발 · 문 열기/닫기)은 기본 빌드에서 사내에서도 보인다** — `USE_FAKE_VSS=false` 면 `HybridVehiclePort(real, fake)` 가 되어 **실물에서 안 오는 키만 Fake 시나리오가 채우기** 때문이다(`LessonViewModel.demo` 는 Hybrid 에서도 non-null). 실물이 주는 키(속도·도어가 오면)에는 패널의 정차·도어 버튼이 **먹지 않는다** — 실제로 세우고 열어야 한다. 순수 Real 로 보려면 `-PfillMissing=false` 로 빌드(그때는 패널이 사라진다). 어느 쪽이든 리포트 배지 `실신호 N` 이 진실이다: logcat `MOAH/VehiclePortFactory` 에 `Hybrid` 또는 `RealVehiclePort ready` 줄.
- **남은 거리**는 기본값(`USE_FAKE_LOCATION=true`)에서 *실제 `Vehicle.Speed` 를 적분 × 배율 10* 으로 줄어든다. 즉 Signal Simulator 에서 속도를 60 으로 올리면 첫 구간(10.5 km)이 약 63초, 0 으로 내리면 진행과 주문 상태가 멈춘다. 시연에 이 방식이 가장 다루기 쉽다. GPS 는 사내 에뮬에 제공자가 있는지 확인한 뒤에만 `-PfakeLocation=false`.
- 단계 전환 조건: 잠금 `Speed > 5`, 공개 `남은 거리 ≤ 500 m`, 도착 `Speed < 1` **그리고** 운전석 도어 열림. 다음 구간은 도착 화면의 "다음 장소로" 버튼으로 시작한다(신호로 자동 전환하지 않음).

## 5. 신호 확인 (INTEGRATION.md A절과 같이 본다)

1. Signal Simulator 에서 `Vehicle.Speed` 를 바꾼다 → 주행 화면의 큰 숫자가 따라오는지. **안 따라오면** 경로 오타(조용히 무시됨) 또는 구독 실패 → `MOAH/RealVehiclePort` 로그.
2. 값 포맷을 로그로 확인: 속도가 `"60.0"` 인지 `"60"` 인지, 정차 시 정확히 `0` 인지(노이즈가 있으면 `STOP_SPEED_KMH = 1` 을 못 넘는다), 도어가 `"true"/"false"` 소문자인지. 파서는 `VssValues.kt` 한 곳.
3. 도어 `Vehicle.Cabin.Door.Row1.DriverSide.IsOpen` 을 3D 에뮬에서 연다 → 공개 화면에서 도착 화면으로 넘어가는지.
4. 콜백 스레드·ANR: 기분 선택 → 시작 시 화면이 멎지 않는지.
5. 앱을 나갔다 들어와도 값이 계속 오는지, 로그에 구독 중복이 없는지.

주제용 추가 신호(기어/좌석/연료/외기온)는 **없어도 시연이 성립**한다. pageId 1323873443 에서 경로와 값 의미만 적어 온다([topics/01_mood_drive.md](topics/01_mood_drive.md) "사내에서 확인할 것").

## 6. 안 될 때의 결정표

| 상황 | 결정 |
|---|---|
| VSS 서비스가 끝내 안 붙는다 | `USE_FAKE_VSS=true` 로 빌드해 시연·제출. 발표에서는 "포트 하나로 Fake/Real 전환, Real 경로는 컴파일 검증"이라고 사실대로 말한다 |
| 속도는 오는데 도어 신호를 조작할 수 없다 | 도착 전환이 막힌다 → 그날 외부로 메모를 가져와 "정차 N초 유지" 대체 조건을 추가(상태기계 `feature/journey`, Claude 소유) |
| TTS 엔진이 없다 | 그대로 진행. 자막 패널이 같은 문장을 보여준다(`AndroidTtsPort` 가 엔진 없음을 흡수) |
| Cloud Copilot 인증이 안 된다 | `FakeAiPort` 유지(현재 기본값). 미구현 상태라 바꿀 것 없음 |
| 화면이 2560×1440 이 아니다 | 레이아웃은 weight 기반. 깨지는 화면을 스크린샷 대신 **글로** 메모 → Codex 핸드오프 |

## 7. 사내 → 외부로 되가져오기 (소스·스크린샷 반출 금지)

사내에서 고친 코드, 사내 화면 캡처, 에뮬 이미지는 가져오지 않는다. 가져오는 것은 **내가 손으로 적은 사실**뿐이다:

```
- [날짜] [stub] 실제 시그니처: <메서드> (<인자 타입>) → <반환 타입>
- [날짜] [real] 속도 포맷 "60.0", 주기 약 N ms, 정차 시 "0.0"
- [날짜] [env] GPS 제공자 있음/없음, TTS 있음/없음, GMS 있음/없음
```

외부에서 이것을 `INTEGRATION.md` B절(가정 → **확인 완료**/정정)과 `02_vss_api_contract.md`·`vss-stub` 에 반영하고, 다음 이관 태그를 만든다. 사내에서만 고친 채로 두면 다음 이관 때 같은 수정을 반복한다.

## 8. 제출

1. **소스** → 팀 Bitbucket 프로젝트(`bitbucket.mobis.co.kr/projects/MOBIS_SW_HACKATHON`). `vss-stub` 제거·스위치 반영된 사내 빌드 상태 그대로
2. **APK** → WebIDE `market_uploader` 로 `automotive-debug.apk` 제출
3. **시연 영상** → 3D 에뮬 연동 화면 녹화. 진행은 [05_demo_script.md](05_demo_script.md), 조작만 시연 버튼 → Signal Simulator 로 바뀐다. 형식·길이는 주최 공지 확인
