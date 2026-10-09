# 라운드 31c · E4 앱 로고

#270 머지 커밋 `7a01065`의 최신 `origin/main`에서 새 워크트리와 `codex/ui-round31c`를 만들었다. [31c 발주서](../../../handoffs/2026-10-09_codex_ui_round31c.md)의 로고 3차 **20번 E4**를 그대로 옮겼다.

변경은 기존 `drawable/ic_launcher.xml`, 새 `mipmap-anydpi-v26/ic_launcher.xml`, 매니페스트의 `icon`·`roundIcon` 연결이다. 108×108 viewport와 네 path의 좌표·순서, Onyx `#222526`, 노랑 `#F2C230`, 선 굵기 6/2.5, 둥근 선끝·꺾임을 보존했다. 흰 사각형 바탕과 적응형 아이콘의 흰 background를 사용한다. `app_name`은 이미 `Drive Coach`여서 수정하지 않았다.

## 캡처

외부 Fake 환경의 **emulator-5556**만 사용했다. 밀도는 160 dpi다. 런처는 앱 재설치 직후 이전 아이콘을 캐시하여, 런처를 재시작한 뒤 새 로고를 캡처했다.

| 증거 | 파일 |
|---|---|
| 실제 AAOS 런처 전체 원본 · 2560×1440 | [런처](launcher.png) |
| 시안 E4와 실제 런처 아이콘 비교 | [비교판](compare-31c-logo.png) |
| 런처 아이콘 84×84 px 원본 크롭 | [원본 크롭](launcher-icon-84px.png) |
| 같은 캡처를 72/48 dp로 축소 | [72 dp](launcher-icon-72dp.png) · [48 dp](launcher-icon-48dp.png) |

비교판 왼쪽은 `5-app-logo-sprout-sizes-b.png`의 20번 E4 흰 바탕·노란 잎 시안 크롭, 가운데는 실제 런처의 84 px 아이콘을 확대해 나란히 놓은 것이다. 런처 크롭 좌표는 UI 트리의 `app_icon` 경계 `[1238,217][1322,301]`이다. 오른쪽은 같은 런처 크롭을 72/48 px로 축소한 캡처다(160 dpi에서 1 dp = 1 px). 앱 아이콘의 도형·색·배치를 합성하거나 보정하지 않았다.

## 검증

| 검사 | 결과 | 증거 |
|---|---|---|
| `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest --max-workers=1` | 빌드 통과 · 단위 테스트 368, 실패·오류·건너뜀 0 | [빌드](build.txt) · [테스트 수·APK SHA256](unit-and-apk.txt) |
| `aapt dump badging` | `application-label:'Drive Coach'` · 아이콘은 `res/mipmap-anydpi-v26/ic_launcher.xml` | [라벨](aapt-label.txt) |
| SVG와 VectorDrawable 비교 | 네 path·색·굵기·둥근 선끝/꺾임 일치 · 시안 HTML `i20`의 좌표와도 일치 | [대조 결과](source-check.txt) |
| 전체 `lesson_shots.sh` 1회 | `Lesson contract passed` · 2026-10-09 11:02 KST | [계약](contract.txt) |

첫 빌드는 D8의 출력 폴더 생성 중 `FileAlreadyExistsException`으로 실패했다. 코드·빌드 설정 파일 변경 없이 `--max-workers=1`로 재실행해 통과했다. `lesson_shots`에는 `ANDROID_SERIAL=emulator-5556`, `ADB_TIMEOUT=1200`을 전달했다.

화면 코드·계측 계약·홈 `DRIVE COACH` 워드마크·Gradle·도구·NEXT는 변경하지 않았다. 사내 런처 직접 확인은 사내 검증에서 이어가며, 여기의 런처 캡처는 외부 AAOS 에뮬레이터에서 실제 적응형 아이콘을 원형으로 표시한 증거다.
