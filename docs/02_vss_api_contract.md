# VSS API 실제 계약 (외부에서 이대로 스텁 작성)

> 근거: VehicleAPI(VSS) Reference (pageId 1037767644, 차량통신미들웨어팀, 2026-08-26 최종 변경). `vss-stub/src/main/java/mobis/vss/` 의 시그니처는 이 문서와 **문자 그대로** 일치해야 한다.
>
> **현재 상태(2026-10-02 밤, 태그 `inhouse-20261002-3`)**: 아래 시그니처와 상수 14개는 사내 jar 로 세 번 컴파일·실행됐다(9/30 이관 1차 · 10/1 검증 #2 · 10/2 검증 #3, 추적 소스 수정 0). 사내 실측 배지 **실신호 7 · 시뮬레이션 1 · 미측정 0**(주차 8키 — 시뮬 1 은 실물에 없는 `Rear.Distance`, `SimOnlySignals`). 스텁/jar 전환은 코드가 아니라 `local.properties` 한 줄(§"패키지 / 의존성").

## VSS란

VSS(Vehicle Signal Specification)는 차량의 각 신호를 `Vehicle.Speed`처럼 점(dot)으로 구분된 경로로 표준화해서 표현하는 스펙이다. VSS API는 Android 앱이 차량 데이터와 상호 작용할 수 있도록 하는 프레임워크로, 신호를 조회·설정·구독한다.

- **sensor**: 차량이 실제로 측정한 값을 앱에 전달하는 신호. 앱은 읽기만 한다. (예: 현재 속도, ABS 작동 여부)
- **actuator**: 차량의 동작을 제어하기 위한 신호. 앱이 값을 써서 차량 동작을 요청할 수 있다. (예: 크루즈 컨트롤 목표 속도)
- `VssConstants`의 각 상수 주석에 있는 `Type` 필드로 sensor/actuator 여부를 확인할 수 있다.

## 패키지 / 의존성

- import 패키지는 `mobis.vss.*` (jar 이름 `mobis.framework.core`와 다름 — 혼동 주의).
- 외부: `compileOnly project(':vss-stub')` — 기본.
- 사내 실물: `local.properties` 에 `mobis.vss.jar=automotive/libs/mobis.framework.core.jar` 한 줄(jar 는 gitignore). 이 키가 있으면 `settings.gradle.kts` 가 `:vss-stub` 을 빼고 `automotive/build.gradle.kts` 가 그 jar 를 `compileOnly`·`testImplementation` 으로 쓰며 `USE_FAKE_VSS=false`. 코드·Gradle 파일 수정 0(9/30, `docs/06` §2). 그 전의 "`compileOnly files('/system/framework/…jar')` 한 줄 교체" 는 폐기.

## 시그니처 전문

```java
package mobis.vss;

// ⚠ 이 시그니처를 "정확히" 재현해야 사내 머지 시 앱 코드 무수정
public class VSSManager {
    public static VSSManager getInstance(android.content.Context ctx); // nullable!

    // 동기 메서드 — throws 선언 없음. RemoteException 아닌 unchecked RuntimeException을 던짐
    public java.util.List<VSSAppData> getVSS(java.util.List<String> keys);
    // 실패한 nodePath 목록 반환. null 또는 빈 리스트 = 전체 성공
    public java.util.List<String> setVSS(java.util.List<VSSAppData> data);

    public void subscribeVSS(java.util.List<String> keys,
                             java.util.concurrent.Executor executor,
                             OnDataChangedListener listener);
    public void unsubscribeVSS(OnDataChangedListener listener);

    public interface OnDataChangedListener {
        void onDataChanged(java.util.List<VSSAppData> data);
    }
}

public class VSSAppData {                 // 값은 항상 String
    public VSSAppData();
    public VSSAppData(String nodePath, String value);
    public String getNodePath();  public void setNodePath(String nodePath);
    public String getValue();     public void setValue(String value);
    public String toString();
}

public class VssConstants {               // "Vehicle.Xxx.Yyy" dot 경로 문자열
    public static final String VEHICLE_SPEED = "Vehicle.Speed";                                   // float, km/h
    public static final String VEHICLE_ADAS_ABS_ISENABLED = "Vehicle.ADAS.ABS.IsEnabled";         // boolean
    public static final String VEHICLE_ADAS_CRUISECONTROL_SPEEDSET = "Vehicle.ADAS.CruiseControl.SpeedSet"; // float, km/h
    public static final String VEHICLE_BODY_HORN_ISACTIVE = "Vehicle.Body.Horn.IsActive";         // boolean
    // 도어 예제 신호 (Template App 기준)
    public static final String VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN = "Vehicle.Cabin.Door.Row1.DriverSide.IsOpen";  // boolean

    // 2026-09-26 운전 연수 B층 채점용 — 2026-09-30 사내 이관 1차에서 경로·이름 확인(이름 = 경로 대문자·밑줄). Rear.Distance 는 실물에 없어 앱 SimOnlySignals 로
    public static final String VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE = "Vehicle.Chassis.SteeringWheel.Angle";                   // int16/float, degree, 양수 = 왼쪽
    public static final String VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR = "Vehicle.Powertrain.Transmission.SelectedGear";     // int8: 0=N, 1..=D, -1..=R, 126=P, 127=D
    public static final String VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED = "Vehicle.Cabin.Seat.Row1.DriverSide.IsBelted";           // boolean
    public static final String VEHICLE_LOWVOLTAGESYSTEMSTATE = "Vehicle.LowVoltageSystemState";                      // string: UNDEFINED/LOCK/OFF/ACC/ON/START
    public static final String VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING = "Vehicle.Body.Lights.DirectionIndicator.Left.IsSignaling";   // boolean
    public static final String VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING = "Vehicle.Body.Lights.DirectionIndicator.Right.IsSignaling"; // boolean
    public static final String VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING = "Vehicle.Body.Lights.Hazard.IsSignaling";                        // boolean
    public static final String VEHICLE_CHASSIS_BRAKE_PEDALPOSITION = "Vehicle.Chassis.Brake.PedalPosition";                   // uint8, percent
    public static final String VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING = "Vehicle.ADAS.ObstacleDetection.IsWarning";                // boolean (표준)
    // 10/4: 실물 목록에 없거나 미확인인 경로(뒤 거리·전조등·와이퍼·제휴 시험장 Track.*)는 스텁이 아니라 앱 vehicle/SimOnlySignals.kt 에 둔다(Fake 전용, Real 은 MISSING)
    // ... 앱에 필요한 신호만 추가 (전체 목록: pageId 1323873443, 1,251개)
}
```

**경로가 틀리면 어떻게 되나 (함정 6의 실전 대응).** 위 B층 상수는 컴파일은 되지만 실물 이름이 다르면 값이 영영 오지 않는다. 앱은 `vehicle/SignalAvailability.kt` 의 `SignalRegistry` 로 세션 동안 값이 온 키를 기록해 `LIVE / SIMULATED / MISSING` 을 정하고, `MISSING` 인 신호가 필요한 채점 항목은 null(미측정)로 빼며 리포트에 "실신호 N · 시뮬레이션 N · 미측정 N" 배지를 단다. 사내 첫날 `missingKeys()` 를 로그로 찍어 pageId 1323873443 과 대조하고, 비슷한 이름이 있으면 **문자열만** 고친다. **사내 실측(9/30 → 10/1·10/2)**: 주차 8키 중 `Speed`·`SteeringWheel.Angle`·`SelectedGear`·`IsBelted`·`LowVoltageSystemState`·`Door.IsOpen`·`ObstacleDetection.IsWarning` 7개가 실신호, `Rear.Distance` 만 시뮬레이션, 미측정 0 — 경로 교정은 필요 없었다. `LowVoltageSystemState` 는 회차 시작 때 **빈 문자열**을 주므로 `RealVehiclePort` 가 `isNullOrBlank()` 를 미수신으로 버리고 Fake 가 채운다(INTEGRATION B 9/30 [hybrid]). 점검 12키 쪽(지시등·비상등·브레이크)은 사내에서 아직 돌리지 않았다.

**기어 인코딩.** `SelectedGear` 의 COVESA 인코딩은 `vehicle/VssGear.kt` 의 `Gear.parse` 가 P/R/N/D 넷으로 접는다(문자 "P/R/N/D" 도 받음). 실물이 다른 인코딩이면 그 함수 한 곳만 고친다.

## 값 타입 변환 (모든 값은 String으로 옴)

| VSS Data Type | Java 변환 | Kotlin (`VssValues.kt`) |
|---|---|---|
| boolean | `Boolean.parseBoolean(v)` | `v.toVssBoolean()` |
| uint8/int8/uint16/int16/int32 | `Integer.parseInt(v)` | `v.toVssInt()` |
| uint32/int64 | `Long.parseLong(v)` | `v.toVssLong()` |
| uint64 | `new BigInteger(v)` | `v.toVssBigInteger()` |
| float | `Float.parseFloat(v)` | `v.toVssFloat()` |
| double | `Double.parseDouble(v)` | `v.toVssDouble()` |
| string | 그대로 사용 | 그대로 사용 |

실제 값 포맷(예: 속도 `"120.5"` vs `"120"`)은 사내 에뮬에서만 확인 가능 → 파서는 `toFloatOrNull()` 계열로 관대하게 작성한다.

## 컴파일 함정 6개와 반영 위치

| # | 함정 | 스텁/코드 반영 | 리뷰 체크 |
|---|---|---|---|
| 1 | `throws RemoteException` 미선언 → `catch (RemoteException)` 하면 컴파일 에러 | `vss-stub`에 `throws` 절 없음. `RealVehiclePort`는 `catch (e: RuntimeException)` | 코드에서 `RemoteException` import/catch 0건 (주석 제외) |
| 2 | 모든 값은 String | `VSSAppData.value: String`, `VssValues.kt` 파싱 헬퍼 | 숫자 직접 캐스팅 금지 |
| 3 | `getVSS/setVSS` 동기 → UI 스레드 호출 시 ANR | `RealVehiclePort`는 전용 단일 스레드 Executor에서 호출, 결과는 콜백/코루틴으로 전달 | UI 코드에서 `VSSManager` 직접 호출 금지 |
| 4 | `getInstance` null 가능 | `RealVehiclePort` 생성자에서 null이면 `IllegalStateException` → `VehiclePortFactory`가 Fake로 폴백 + 경고 로그 | — |
| 5 | compileOnly 경로 (스텁 vs 시스템 jar 동시 활성 시 duplicate class) | `local.properties` 의 `mobis.vss.jar` 키 유무로 `settings.gradle.kts` 가 `:vss-stub` 포함 여부를 정한다 — 둘이 동시에 켜질 수 없다 | 사내 빌드 로그 첫 줄 `mobis.vss: jar … → USE_FAKE_VSS=false` · `REPO_ONLY=1 bash tools/inhouse_check.sh` 로 jar·`local.properties` 가 커밋에 안 들어가는지 |
| 6 | `VssConstants` 경로 오타는 조용히 무시 | 경로 문자열은 `VssConstants`만 사용, 리터럴 금지. 상수 **이름 = 경로를 대문자·밑줄로**(사내 jar 규칙, 9/30) | 새 신호 추가 시 pageId 1323873443 원문과 대조, 이름 규칙 확인, INTEGRATION B 에 가정 한 줄 |

## 구독 수명주기

- `subscribeVSS(keys, executor, listener)` 로 등록, `unsubscribeVSS(listener)` 로 해제. 리스너 객체 동일성으로 해제되므로 **리스너 인스턴스를 보관**한다.
- 해제는 `onPause/onDestroy` (Compose: `DisposableEffect` / ViewModel `onCleared`)에서 `VehiclePort.dispose()` 를 통해 수행한다.
