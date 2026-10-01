# MOAH 2026 해커톤 — 외부 PC 개발 가이드 (전사본)

> 목적: 사내 토큰/자원 절약을 위해 개인 PC(외부)에서 최대한 개발한 뒤, 사내 WebIDE에서 최소 수정으로 머지·빌드·시연하기 위한 실전 가이드.
> 핵심 제약: 데이터는 **외부 → 사내 단방향(inbound only)**. 사내 소스/스켈레톤을 외부로 반출 불가. 외부에서는 문서화된 공개 API 계약만 근거로 개발한다.
> 작성 근거: 사내 Confluence HACKATHON 스페이스 — Getting Started(1328361635), VehicleAPI Reference(1037767644), Template App(1324683678), 제출 가이드(1038298817). 최종 기준은 항상 원문/주최 공지 우선.

## 0. 한 장 요약 (TL;DR)

- 리스크의 정체는 "정보 부족"이 아니라 **"외부에서 실행 불가"** 다. VSS 계약은 완전히 문서화되어 있으니 바이트 단위로 정확한 스텁을 만들 수 있다.
- 앱 로직을 본인 인터페이스(`VehiclePort`) 뒤로 격리 → 외부에선 `FakeVehiclePort`로 실제 실행/시연, 사내에선 `RealVehiclePort`(VSSManager 어댑터)로 전환.
- 사내 머지는 **build.gradle의 compileOnly 한 줄 교체 + 주입 플래그 하나**로 끝나도록 설계한다. 앱 본문은 손대지 않는다.
- 컴파일 함정(RemoteException 미선언 / 값은 String / 동기 메서드 백그라운드 호출 / getInstance null)을 외부 코드에 미리 박아 둔다.
- 정책 확인: 사내 소스의 개인 PC 반출/복제가 해커톤 규정상 허용되는지 주최측(FAQ/QNA)에 먼저 확인. 순수 자작 코드를 사내 repo로 push하는 방향이면 이 리스크는 대부분 회피된다.

## 1. 해커톤 개요 · 주제

→ [00_hackathon_overview.md](00_hackathon_overview.md)

## 2. 개발환경 비교 — 사내(WebIDE) vs 외부(개인 PC)

| 구성요소 | 사내 WebIDE | 외부 개인 PC | 외부 대응 |
|---|---|---|---|
| Android Studio + AAOS 템플릿 | O | O(직접 설치) | 그대로 사용 |
| 앱 로직·UI·아키텍처 (토큰 多) | O | O | **여기서 개발** (개인 AI 도구로 토큰 절약) |
| `mobis.vss.*` (VSSManager 등) | O(시스템 jar) | X | 스텁 소스로 재현(compileOnly) |
| infoLINK 에뮬레이터 | O | X | 일반 AAOS 에뮬 + Fake 런타임 |
| 3D 에뮬레이터 / Signal Simulator | O | X | 시연 당일 사내에서 |
| M.ADI 템플릿(Maven 사내 인증) | O | X | 사내에서 clone, 외부는 feature만 |
| MarketUploader 제출 | O | X | 시연/제출은 사내 |

결론: 외부는 환경 독립적인 80%(로직/화면/알고리즘)를, 사내는 환경 의존적인 20%(VSS 실행·에뮬·제출)를 담당한다.

(이 PC의 실제 상태는 [03_environment.md](03_environment.md) 참고. 동료가 배포한 해커톤 화면 크기 AVD 설정을 사용할 수 있다.)

## 3. VSS API 실제 계약

→ [02_vss_api_contract.md](02_vss_api_contract.md) (시그니처 전문, 타입 변환표)

## 4. 머지 시 컴파일 터지는 함정 (외부에서 반드시 반영)

"외부에선 됐는데 사내에서 컴파일 실패"는 대부분 아래에서 난다.

1. **RemoteException으로 catch하면 컴파일 에러.** `getVSS/setVSS/subscribeVSS/unsubscribeVSS`는 `throws RemoteException`을 선언하지 않고 unchecked `RuntimeException`을 던진다. → 스텁에도 `throws RemoteException`을 붙이지 말 것. 서비스 오류는 `catch (RuntimeException e)`로 처리.
   ```java
   // ❌ 사내에서 "exception RemoteException is never thrown" 컴파일 에러
   try { vss.getVSS(keys); } catch (RemoteException e) { ... }
   // ✅
   try { vss.getVSS(keys); } catch (RuntimeException e) { ... }
   ```
2. **모든 값은 String** → 변환표대로 파싱.
3. **getVSS/setVSS는 동기** → UI 스레드에서 호출 시 ANR. 반드시 백그라운드 스레드에서 호출, 결과만 `runOnUiThread`.
4. **getInstance는 null 가능** → 항상 null 체크.
5. **compileOnly 경로**: 실물은 `compileOnly files('/system/framework/mobis.framework.core.jar')`. 외부엔 이 파일이 없으므로 build.gradle 한 줄만 스텁으로 교체(§6).
6. **VssConstants 경로 오타는 조용히 무시됨** (예외 없이 데이터 미수신). 경로 문자열을 문서와 정확히 일치시킬 것.

## 5. 아키텍처 — 외부 실행과 사내 실물을 코드 무수정으로 잇기

크럭스: `VSSManager`는 compileOnly라 **외부에선 컴파일만 되고 실행 불가**. 앱이 이를 직접 부르면 외부 가상 테스트가 안 된다. → 본인 인터페이스로 격리가 유일 해법.

```
[앱 로직 / 화면]
   │  (앱은 오직 이 인터페이스에만 의존 — 본인 코드, 100% 확실)
   ▼
interface VehiclePort
   ├── RealVehiclePort → mobis.vss.VSSManager 호출  (외부: 컴파일만 / 사내: 실행)
   └── FakeVehiclePort → 순수 in-memory 구현        (외부에서 실제 실행 O — 시연 가능)
```

가이드 원안(Java):

```java
// 앱이 의존하는 유일한 계약 (본인 코드)
public interface VehiclePort {
    float speed();
    void setDoorOpen(boolean open);
    void observe(java.util.List<String> keys, java.util.function.Consumer<java.util.Map<String,String>> cb);
    void dispose();
}

// 외부 가상 테스트에서 "실제로 도는" 구현
public class FakeVehiclePort implements VehiclePort {
    private final java.util.Map<String,String> store = new java.util.concurrent.ConcurrentHashMap<>();
    // setVSS 흉내: store에 쓰고 리스너 emit / 신호 시뮬레이션 타이머 등으로 실제 UI 검증
}

// VSSManager 어댑터 — 외부에선 실행하지 않음(컴파일만), 사내에서 활성
public class RealVehiclePort implements VehiclePort {
    private final mobis.vss.VSSManager vss;
    public RealVehiclePort(android.content.Context ctx) {
        this.vss = mobis.vss.VSSManager.getInstance(ctx); // null 체크 필요
    }
    // getVSS/setVSS/subscribeVSS를 VehiclePort로 매핑 (RuntimeException 처리, String 파싱)
}
```

주입 스위치는 한 곳만:

```java
VehiclePort port = BuildConfig.USE_FAKE_VSS
        ? new FakeVehiclePort()
        : new RealVehiclePort(context);
```

효과: 외부에선 `USE_FAKE_VSS=true`로 앱 전체를 실제 실행/시연, 사내 머지 시 (1) build.gradle compileOnly 한 줄 교체 + (2) `USE_FAKE_VSS=false`만 바꾸면 됨. 앱 로직·UI 본문은 손대지 않는다.

이 저장소에서는 위 원안을 Kotlin으로, 주제에 독립적인 범용 계약(`get/set/observe/dispose`)으로 구현한다. → `automotive/src/main/kotlin/.../vehicle/`

## 6. Gradle 구성 — 스텁 격리 & 한 줄 스위치

`mobis.vss` 스텁은 **별도 모듈(`vss-stub`)**에 격리한다 (앱 모듈과 같은 패키지 `mobis.vss`).

```groovy
// app/build.gradle (dependencies)
dependencies {
    // — 외부(개인 PC): 스텁을 compileOnly로 참조 —
    compileOnly project(':vss-stub')

    // — 사내(WebIDE) 머지 시: 위 한 줄을 아래로 교체 —
    // compileOnly files('/system/framework/mobis.framework.core.jar')
}

android {
    defaultConfig {
        // 외부: true(Fake 실행) / 사내: false(Real)
        buildConfigField "boolean", "USE_FAKE_VSS", "true"
    }
    buildFeatures { buildConfig true }
}
```

⚠ 스텁과 실물 시스템 jar를 동시에 compileOnly로 두면 duplicate class가 난다. 반드시 둘 중 **하나만** 활성화(한 줄 교체). `FakeVehiclePort`는 compileOnly가 **아니다** — 외부에서 실제 실행되어야 하므로 일반 `implementation` 소스에 둔다.

이 저장소의 실제 구현(`automotive/build.gradle.kts`)은 Kotlin DSL이며, 스텁/jar 선택을 `val vssApi: Any = …` 변수 한 줄로 묶어 `compileOnly(vssApi)`와 `testImplementation(vssApi)`(JVM 단위 테스트용)에 함께 쓴다. 교체 지점은 여전히 한 줄이다.

## 7. 외부에서 검증 불가 → 사내 수렴 체크리스트

→ [INTEGRATION.md](INTEGRATION.md)

## 8. 머지 · 빌드 · 제출 (사내에서 진행)

### 8.1 템플릿 기반 구조 맞추기 (권장)

```bash
git clone ssh://<사내 Bitbucket>/mobis_sw_hackathon/moah_template_app.git
cd moah_template_app
```

- 사내에서 이 템플릿을 먼저 확보해 두고, 외부에서 개발한 feature 코드를 이 구조 위에 얹는다.
- 외부 프로젝트의 패키지 구조·모듈명을 템플릿(`automotive` 모듈)과 최대한 동일하게 맞춰두면 파일을 그대로 떨어뜨릴 수 있어 머지 마찰이 준다.
- M.ADI/Template의 Gradle·모듈 구조를 외부에서 재현하려 하지 말 것(시간 낭비).

### 8.2 빌드

```bash
./gradlew assembleDebug
# 산출물: automotive/build/outputs/apk/debug/automotive-debug.apk
```

- 개발 중엔 `assembleDebug`(빠름). `clean build`는 전체+테스트+lint라 느림(결과 debug APK는 동일).

### 8.3 설치 & 실행

```bash
adb devices
adb push copilot_config.json /data/local/tmp/copilot_config.json   # Cloud AI 사용 시
adb install automotive/build/outputs/apk/debug/automotive-debug.apk
```

### 8.4 제출물 (3종)

1. **Source code** → 팀별 제공 Bitbucket 프로젝트에 업로드 (`<사내 Bitbucket>/projects/MOBIS_SW_HACKATHON`)
2. **APK** → WebIDE의 `market_uploader` 실행 → APK 선택 후 제출
3. **시연 영상** → 3D 에뮬레이터 연동 동작 녹화 (세부 형식은 주최 공지 확인)

## 9. (선택) 앱 내 AI 기능

| 방식 | 요건 | 외부 개발 가능성 |
|---|---|---|
| ☁ Cloud Copilot AI (GitHub Copilot Chat API, GPT-4o) | 인터넷 + GitHub Copilot 구독 + OAuth App | 인증 흐름/HTTP 호출은 외부에서 목킹 가능, 실제 호출은 네트워크 필요 |
| 🖥 On-Device AI (AIMO SDK: GPT/STT/Vision) | Connect H/U(SA8255 NPU) 보드 보유 팀 한정 | 외부/에뮬 실행 불가 — 보드에서만 |

- Cloud AI도 인터페이스 뒤로 격리하면(예: `AiPort` + `FakeAiPort`) 외부에서 대화 UI를 목 응답으로 완성하고, 사내/네트워크 환경에서 실제 API로 전환할 수 있다.

## 10. 정책 · 보안 유의 (반드시 선확인)

→ [00_hackathon_overview.md](00_hackathon_overview.md#정책보안-유의-반드시-선확인)

## 부록 A. 외부 개발 순서 요약

1. Android Studio + AAOS(Automotive) 프로젝트 생성 (Java/Kotlin, minSDK 29+)
2. `vss-stub` 모듈에 `mobis.vss.*` 스텁 작성 (§3, 함정 §4 반영)
3. `VehiclePort` 인터페이스 + `FakeVehiclePort`(실행) + `RealVehiclePort`(어댑터) 작성 (§5)
4. `BuildConfig.USE_FAKE_VSS=true`로 앱 로직·UI 개발 및 에뮬 실행/시연
5. 가정은 `INTEGRATION.md`에 계속 기록 (§7)
6. (사내) 템플릿 clone → 코드 이식 → build.gradle 한 줄 교체 + 플래그 false → 에뮬 검증 → APK/소스/영상 제출

## 부록 B. 근거 페이지

- Getting Started: <사내 Confluence>/pages/viewpage.action?pageId=1328361635
- VehicleAPI(VSS) Reference: <사내 Confluence>/pages/viewpage.action?pageId=1037767644
- Template App: <사내 Confluence>/pages/viewpage.action?pageId=1324683678
- 앱 제출 가이드: <사내 Confluence>/pages/viewpage.action?pageId=1038298817
- 최종자료 제출: <사내 Confluence>/pages/viewpage.action?pageId=1317526558
- 차량 신호(1,251개): <사내 Confluence>/pages/viewpage.action?pageId=1323873443

본 문서는 사내 Confluence 문서를 근거로 정리한 개인 개발용 참고 자료다. 공식 규칙·주제·평가·일정·제출 형식은 항상 주최측 최신 공지를 우선한다.
