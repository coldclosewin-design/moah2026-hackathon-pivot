# 개발 환경 현황 (외부 PC)

조사일: 2026-09-16. 변경 시 갱신한다.

## 이 PC

| 항목 | 상태 |
|---|---|
| OS | Windows 11 Pro, RAM 31GB, 디스크 여유 약 625GB, 가상화 지원 O |
| JDK | 17 (Eclipse Adoptium), `JAVA_HOME` 설정됨 |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` (`ANDROID_HOME` 설정됨). platforms;android-35, build-tools 34.0.0/35.0.0, platform-tools 37.0.1, cmdline-tools latest |
| 에뮬레이터 | **설치 완료 (2026-09-16)**: emulator 37.1.11.0, system-images/cstd, AVD `CSTDe_API_34`. 콜드 부팅 약 30초. 실행: `emulator -avd CSTDe_API_34 -no-snapshot-load` |
| Android Studio | 미설치 (빌드는 Gradle CLI로 수행) |
| Gradle | 8.10.2 배포판 캐시됨 (`~/.gradle/wrapper/dists`). 이 저장소의 wrapper가 이를 사용 |
| AGP / Kotlin | AGP 8.7.3, Kotlin 2.0.21 + Compose Compiler 플러그인 (프로젝트 `gradle/libs.versions.toml`) |
| git / gh | git 설치, gh 로그인됨 (repo scope). 전역 user.name/email 없음 → repo-local 설정 사용 |
| 에이전트 | Claude Code (이 세션), Codex CLI 0.142 (`gpt-6-astra`). Codex 전역 `~/.codex/AGENTS.md`는 빈 파일 → 프로젝트 `AGENTS.md`가 유일 지침 |

## 빌드 명령

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
# APK: automotive\build\outputs\apk\debug\automotive-debug.apk
```

## 에뮬레이터 설치 (`emulator/setting.zip`)

> **17번(피벗) 저장소 주석**: 이 zip(3.7 GB)은 `C:\Project\16_hackathon\emulator\setting.zip` 에 있고 이 저장소에는 복사하지 않았다. AVD `CSTDe_API_34` 는 `%LOCALAPPDATA%\Android\Sdk` 에 이미 설치되어 16번과 **공용**이므로 아래 설치 절차는 새 PC 에서만 필요하다.

동료가 배포한 외부 개발용 에뮬레이터 설정. 사내 WebIDE의 가상 환경을 외부에서 쓸 수 없어, 해커톤 화면 크기의 AVD를 Android Studio Device Manager에 추가하기 위한 파일이다.

zip 내용:
- `cstd/x86_64/` — AAOS 시스템 이미지. `source.properties`: API 34, `android-automotive,google_apis`, x86_64, emulator#35.1.9 필요. `build.prop` 지문: `google/sdk_gcar_x86_64/emulator_car64_x86_64:14/UAA1.250512.001` (순정 Google 자동차 에뮬 이미지로 보임, system.img 5.8GB)
- `CSTDe_API_34.avd/` + `CSTDe_API_34.ini` — AVD. `config.ini`: 2560×1440, density 160, RAM 4096, 4코어, data 30G, `tag.id=connect_s`, `image.sysdir.1=system-images\cstd\x86_64\`
- `setup_avd.ps1 / .bat / .sh` — `ANDROID_HOME\system-images\cstd` 와 `%USERPROFILE%\.android\avd` 로 robocopy, ini의 `path=`를 현재 PC 경로로 보정

설치 절차 (프로젝트 폴더 밖에 쓰므로 사용자 승인 후 실행):

```powershell
# 1) emulator 패키지
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "emulator"
# 2) zip 해제 후 스크립트 실행 (약 12GB 복사)
Expand-Archive .\emulator\setting.zip -DestinationPath <임시폴더>
& <임시폴더>\setup_avd.ps1
# 3) 부팅
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd CSTDe_API_34
```

부팅 후 확인 결과 (2026-09-16):

| 확인 항목 | 명령 | 결과 |
|---|---|---|
| 빌드 지문 | `adb shell getprop ro.build.fingerprint` | `google/sdk_gcar_x86_64/emulator_car64_x86_64:14/UAA1.250512.001/13479943:userdebug/dev-keys` (순정 Google AAOS 14) |
| mobis 프레임워크 jar 존재 | `adb shell ls /system/framework \| grep -i mobis` | **없음** (`/system /vendor /product /system_ext` 전체에 `*mobis*` 없음) |
| VSS 서비스 존재 | `adb shell service list \| grep -iE 'vss\|mobis\|infolink'` | **없음** |
| 화면 크기 | `adb shell wm size` | `2560x1440` (해커톤 화면 크기 일치) |
| 앱 설치·실행 | `adb install` → `am start com.moah.hackathon/.ui.MainActivity` | 정상. 로그 `USE_FAKE_VSS=true → FakeVehiclePort`. 스크린샷 `docs/screenshot_placeholder_fake.png` |

| Google TTS | `pm list packages \| grep tts` | `com.google.android.tts` 있음 (기본 엔진 설정값은 null → 앱에서 엔진 지정 또는 기본 사용) |
| Play 서비스 / 스토어 | `pm list packages \| grep gms` | `com.google.android.gms`, `com.android.vending` 있음 → Google Maps SDK 사용 가능 |
| 음성인식 서비스 | `dumpsys package r \| grep RecognitionService` | Google TTS 패키지의 `GoogleTTSRecognitionService` 존재 (실동작·언어 지원은 테스트 필요) |
| 인터넷 | `ping 8.8.8.8` | 정상 (RTT 약 470 ms) |
| 로케일 | `getprop persist.sys.locale` | 미설정(en-US 기본). 한국어 시연 시 `adb shell setprop persist.sys.locale ko-KR` 후 재부팅 검토 |

**결론: 외부 에뮬에는 mobis 프레임워크/VSS 서비스가 없다.** 대신 TTS·GMS·인터넷은 있어 지도/음성/LLM 실구현을 외부에서 검증할 수 있다. 외부는 `USE_FAKE_VSS=true` 전제 확정. `RealVehiclePort` 경로는 사내에서만 검증한다. 이 에뮬의 가치는 "해커톤과 같은 화면 크기의 AAOS 14에서 실제 앱 UI·수명주기를 돌려보는 것"이다.

GPS 검증(GpsLocationPort, `-PfakeLocation=false` 빌드):

```bash
adb shell cmd location set-location-enabled true
adb shell pm grant com.moah.hackathon android.permission.ACCESS_FINE_LOCATION
adb emu geo fix 127.0324 37.4837        # 경도 위도 순. 서초 출발지
# 이후 1~2초 간격으로 경로상의 좌표를 계속 보내면 남은 거리가 줄어든다
```

주의: 에뮬 디스플레이가 2개라 `screencap`에는 `-d 4619827259835644672`가 필요하고, Git Bash에서는 `MSYS_NO_PATHCONV=1`과 `</dev/null`(stdin 닫기)을 붙여야 `adb shell`이 멈추지 않는다.

자동 시연 스크립트는 `tools/`에 있다. 반드시 [tools/README.md](../tools/README.md)의 규칙을 따른다. 요약: 한 번에 하나만 실행(잠금), 이 환경의 `pkill`은 동작하지 않으니 PID 파일로 `kill`, 주행 화면에서는 `uiautomator dump`가 실패하므로 진행 판정은 앱 로그로, 실행 끝의 uiautomator 충돌 건수가 0이어야 유효한 검증이다.

자주 쓰는 명령:

```powershell
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd CSTDe_API_34 -no-snapshot-load
adb install -r automotive\build\outputs\apk\debug\automotive-debug.apk
adb shell am start -n com.moah.hackathon/.ui.MainActivity
adb exec-out screencap -p > shot.png
adb logcat -s "MOAH/VehiclePortFactory:*" "MOAH/RealVehiclePort:*" "AndroidRuntime:E"
```

## 사내 WebIDE (참고, 접근 불가)

- infoLINK 에뮬레이터, 3D 에뮬레이터/Signal Simulator, `/system/framework/mobis.framework.core.jar`, M.ADI 템플릿(Maven 사내 인증), MarketUploader.
- 신호 흐름: `[앱] --setVSS--> [Databroker] <--WS--> [Signal Simulator] --TCP--> [3D Emulator]` (`adb forward 8090`)
