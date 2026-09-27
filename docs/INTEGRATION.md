# INTEGRATION.md — 사내 수렴 체크리스트 & 가정 로그

사내 첫날의 전체 순서(clone → 스위치 → 빌드 → 신호 확인 → 제출)는 [06_inhouse_migration.md](06_inhouse_migration.md). 이 문서는 그 중 체크 항목·가정·요청의 원장이다.

외부에선 **컴파일 정확성**만 보장된다. 아래는 사내 에뮬에서만 확인 가능하니, 외부 개발 중 가정을 적어두고 사내 첫날 순서대로 검증한다.

## A. 사내 첫날 체크리스트 (순서대로)

- [ ] 사내 템플릿 clone (`moah_template_app`) → 외부 코드 이식 (모듈명 `automotive` 동일)
- [ ] `automotive/build.gradle.kts`의 `vssApi`를 시스템 jar로 교체, `USE_FAKE_VSS` → `"false"`
  ```kotlin
  // val vssApi: Any = project(":vss-stub")
  val vssApi: Any = files("/system/framework/mobis.framework.core.jar")
  buildConfigField("boolean", "USE_FAKE_VSS", "false")
  ```
  (`vssApi`는 `compileOnly`와 `testImplementation` 양쪽에 쓰인다. 사내에서 단위 테스트가 jar 경로를 못 읽으면 `testDebugUnitTest`는 건너뛴다.)
  `settings.gradle.kts`의 `include(":vss-stub")`도 제거(또는 모듈 삭제) — 스텁과 실물 동시 활성 시 duplicate class
- [ ] `./gradlew assembleDebug` 컴파일 통과 (스텁 시그니처 불일치가 있으면 여기서 드러남 → `docs/02_vss_api_contract.md` 갱신)
- [ ] `VSSManager.getInstance()` 가 null 아님 (`adb shell service list | grep vss`)
- [ ] 내가 사용한 `VssConstants` 경로가 실제 존재 (오타 시 조용히 무시됨) — pageId 1323873443 대조
- [ ] 신호 흐름 확인: `[앱] --setVSS--> [Databroker] <--WS--> [Signal Simulator] --TCP--> [3D Emulator]` (`adb forward 8090`)
- [ ] 도어 예제: `Vehicle.Cabin.Door.Row1.DriverSide.IsOpen` SET/GET 동작
- [ ] **B층 키 10개**: 회차 시작 로그 `attempt 1 start … missing=[…]` 를 적어 온다. 든 키마다 pageId 1323873443 에서 비슷한 이름을 찾아 `VssConstants.java` 문자열만 교정(상수명 유지). 기어 인코딩·조향 부호가 다르면 `VssGear.kt`. 리포트 배지 `실신호 N` 숫자 메모
- [ ] Signal Simulator 로 조향각·기어를 넣어 도식 칩이 "실신호" 로 바뀌는지, 없는 신호는 패널 시나리오가 채우는지(Hybrid)
- [ ] 실제 값 포맷 (예: 속도 `"3.0"` vs `"3"`, 정차 시 `0`), 업데이트 주기/스레드 타이밍이 Fake와 다르지 않은지
- [ ] getVSS/setVSS 호출부가 백그라운드 스레드인지 (ANR 점검)
- [ ] 구독 해제(`unsubscribeVSS`)가 onPause/onDestroy에서 호출되는지 (메모리 누수)
- [ ] Cloud Copilot 인증 방식·엔드포인트 확인 → `CoachTransport` 구현체 하나 → `App.kt` 의 `transport = null` 교체 (`copilot_config.json` push 가 필요하면 그때)
- [ ] APK 빌드 → MarketUploader 제출, 소스 Bitbucket push, 시연 영상 녹화

## B. 외부 개발 중 가정 로그 (append-only)

형식: `- [날짜] [영역] 가정 내용 → 사내 확인 방법`

> 2026-09-25 이전 항목 중 `[gift]` `[order]` `[indoor]` `[map]` `[journey]` 는 Gift Drive(16번) 시절 기록이다. 스텁·Real·Fake 포맷·환경·TTS·UI 밀도 항목은 이번 아이템에도 그대로 유효하다.

- [2026-09-16] [stub] `VSSManager`, `VSSAppData`, `VssConstants`는 `mobis.vss` 패키지의 public class이며 문서 §3 시그니처 그대로다 → 사내 컴파일 통과로 확인
- [2026-09-16] [stub] `OnDataChangedListener`는 `VSSManager`의 nested public interface다 → 컴파일로 확인
- [2026-09-16] [stub] `setVSS` 반환 null/빈 리스트 = 전체 성공 → 실제 반환값 로그로 확인
- [2026-09-16] [real] `subscribeVSS` 콜백은 넘긴 Executor에서 호출된다(UI 스레드 아님) → 스레드명 로그로 확인
- [2026-09-16] [real] `unsubscribeVSS(listener)`는 리스너 동일성으로 해제한다 → 해제 후 콜백 미수신 확인
- [2026-09-16] [fake] 속도 값 포맷은 `"12.5"` 같은 소수 문자열로 가정 → 실제 포맷 확인 후 Fake 시뮬레이션 조정
- [2026-09-16] [fake] 도어 IsOpen은 `"true"/"false"` 소문자 → 실제 포맷 확인
- [2026-09-16] [env] 외부 에뮬(`CSTDe_API_34`)에는 mobis 프레임워크가 없다 → **확인 완료** (순정 Google AAOS 14, `docs/03_environment.md`). Real 경로는 사내 전용
- [2026-09-17] [journey] 정차 판정은 `Vehicle.Speed < 1`, 주행 중 잠금은 `> 5 km/h` 로 가정 → 실제 값 갱신 주기·노이즈(정차 시 0.0 인지) 확인
- [2026-09-17] [journey] 도착 감지는 "정차 + 운전석 도어 열림"만 사용 (기어 P 는 사내 목록 확인 후 추가) → `Vehicle.Powertrain.Transmission.*` 경로 확인
- [2026-09-17] [journey] 남은 거리는 외부에서 속도 적분(FakeLocationPort). 사내에서는 GPS 또는 `Vehicle.CurrentLocation.*` 존재 여부 확인 후 LocationPort 실구현
- [2026-09-18] [location] `GpsLocationPort`는 Android `LocationManager.GPS_PROVIDER`를 쓰며 권한/제공자가 없으면 Fake로 폴백. 사내 에뮬·실차에 GPS 제공자가 있는지 확인 → 없으면 `Vehicle.CurrentLocation.Latitude/Longitude` 신호(사내 목록 확인)로 같은 인터페이스 구현. 빌드 플래그 `USE_FAKE_LOCATION`(기본 true, `-PfakeLocation=false`)
- [2026-09-17] [journey] 다중 정거장: 정거장 사이에 "다음 장소로" 버튼을 눌러야 다음 구간이 시작된다(정차 상태). 도어 닫힘·기어 D 자동 감지는 사내에서 기어 신호 확인 후 검토
- [2026-09-17] [gift] 선물은 시드(FakeGiftPort). 쿠폰·예약·픽업 주문은 전부 흉내(제휴 API 없음) → 발표에서 "제휴 확장" 으로 설명
- [2026-09-18] [order] `OrderPort`는 "앱이 남은 거리를 보고하면 매장이 상태를 바꾼다"는 계약. Fake는 남은 거리 50% 이하 → 제조 중, 600 m 이하 → 준비 완료, 도어 열림 → 픽업 완료. 실제 제휴 API가 생기면 같은 인터페이스로 교체. 숨긴 구간에서는 음성·화면 모두 매장명·메뉴를 노출하지 않는다

- [2026-09-18] [map] 지도는 자체 의사-3D(Canvas)로 그린다 — Google Maps SDK 는 사내 에뮬의 Play services·API 키·네트워크가 미확인이라 채택하지 않음. 경로 좌표는 손으로 찍은 시드(`SeedCatalog.routes`), 위치는 `진행률 × 경로 길이`(직선 남은 거리 기준이라 실제 GPS 위치와 조금 다름) → 사내에서 길찾기 API·Maps 가 되면 `MapPort.route` 만 실구현으로 교체
- [2026-09-18] [tts] 음성은 Android TTS 의 로컬 한국어 음성 중 최고 품질을 자동 선택(외부 에뮬: Google TTS, 로컬 4·네트워크 4종, 전부 quality 400). `-PttsVoice` 로 네트워크 음성 지정 가능, 합성 실패 시 로컬 복귀 → 사내 에뮬의 TTS 엔진·음성 목록은 logcat `MOAH/AndroidTtsPort: ko voices` 로 확인
- [2026-09-19] [indoor] 건물 안 경로는 실제 도면이 아니라 **시드 약도**(`SeedCatalog.hyundaiIndoor`: 층 평면 120×80 m, E3 엘리베이터·중앙 에스컬레이터·서쪽 계단)에서 최단 시간 경로를 계산한다. 시간 상수(보행 1.2 m/s, 엘리베이터 타고 내리기 40초 + 층당 4초, 에스컬레이터 층당 25초, 계단 층당 22초)는 가정. 실내 지도는 공개 API 가 없어 건물주 제휴가 필요 → 생기면 `IndoorPort` 실구현으로 교체. 화면에 약도임을 고지한다
- [2026-09-21] [ui] **화면은 2560×1440 px · 밀도 160(1dp = 1px)에 고정돼 있다.** 외부 에뮬에서 밀도만 200(1.25배)으로 올리자 기분 선택 화면이 깨졌다(시간 버튼 잘림, "드라이브 시작"이 화면 밖). 사내 에뮬·실차의 해상도·밀도가 다르면 글자 크기 이전에 배치가 깨진다 → 사내 첫날 `adb shell wm size`, `adb shell wm density` 확인. 다르면 화면 루트에서 밀도를 "가로 2560 기준"으로 고정하는 래퍼를 넣는다(NEXT.md 5번). 가독성: 12.3인치·75 cm 가정에서 36sp 이하(3.4 mm)는 ISO 15008 의 허용 기준(3.5 mm) 미달 — 실제 디스플레이의 물리 크기 확인 필요
- [2026-09-21] [ui] 위 가정의 대책: `ui/concepts/DesignScale.kt` 가 화면 루트에서 밀도를 다시 정해 앱을 항상 **2560 × 1268 dp 의 창**으로 그린다(설계 크기가 창 안에 통째로 들어가는 가장 큰 밀도, 글꼴 배율 1 고정). 외부 에뮬에서 밀도 200·320 으로 바꿔도 같은 모습임을 확인. 확인하지 못한 것: 창의 **픽셀 크기나 비율**이 다른 기기(예: 1920×720) — 계산상 잘리지는 않지만 한쪽에 여백 dp 가 생기고 실제 모습은 보지 못했다 → 사내 첫날 logcat `MOAH/DesignScale` 한 줄(window·systemDensity·고른 density)을 확인
- [2026-09-21] [gift] 엄마의 선물(설렘 → 화랑공원 야경): 공원의 **좌표(37.4040, 127.1060)·경로·주차장 위치("공원 남측 공영주차장")·메모 속 시설("프러포즈한 벤치", "언덕 쪽 벤치")은 확인하지 않고 지은 것**이다. 시드의 다른 좌표와 같은 근사지만, 실존 장소의 시설을 말하는 문구라 발표·영상에 쓰기 전에 사용자가 실제 공원에 맞게 고친다 → `data/SeedCatalog.kt` 의 `giftMom`, `parking[HWARANG_PARK]`
- [2026-09-21] [tts] 기본 음성을 **`ko-kr-x-kob-network`** 로 정했다(사용자가 네트워크 4종을 듣고 결정. `-PttsVoice=auto` 면 예전처럼 로컬 자동 선택). 외부 에뮬에서 네트워크를 끄고 돌려도 합성 실패 없이 말했다(Google TTS 가 기기 안에서 합성, 구간 시작이 1~3초 이르다) — **그 소리가 온라인과 같은 목소리인지는 귀로 확인하지 못했다.** 음성이 설치돼 있지 않으면 같은 계열 로컬(`…-kob-local`) → 로컬 최고 품질 순으로 고른다(이 경로는 외부에서 실행된 적이 없다 — 네 음성이 전부 설치돼 있어서). 사내 에뮬의 TTS 가 Google 이 아니면 목소리·속도가 달라지고 도착 시간표(`ArrivalTiming`)를 다시 재야 한다 → logcat `MOAH/AndroidTtsPort` 의 `ko voices`, `voice=`, `start`/`done` 줄

- [2026-09-25] [pivot] 아이템을 **운전 연수 어시스턴트**로 전면 교체(`docs/topics/01_driving_coach.md`). `16_hackathon` 에서 `vehicle/`·`vss-stub`·`TtsPort`·`LocationPort`·`Route`·`DesignScale`·`tools/`·문서 골격만 물려받음. 가정 로그의 스텁·Real·Fake·환경·TTS·UI 항목은 그대로 유효
- [2026-09-25] [vss] B층 채점표용 신호 경로 9개를 **COVESA VSS 표준명으로 추정**해 `VssConstants` 에 추가할 예정: `Vehicle.Cabin.Seat.Row1.DriverSide.IsBelted`, `Vehicle.Powertrain.Transmission.CurrentGear`, `Vehicle.Body.Lights.DirectionIndicator.Left.IsSignaling`, `…Right.IsSignaling`, `Vehicle.Chassis.SteeringWheel.Angle`, `Vehicle.Chassis.Brake.PedalPosition`, `Vehicle.Body.Lights.Hazard.IsSignaling`, `Vehicle.LowVoltageSystemState`(IGN 대용), 주차센서(경로 미정). **전부 사내 미확인.** 틀리면 조용히 무시되므로 앱은 신호마다 `LIVE/SIMULATED/MISSING` 을 관리하고 `MISSING` 은 채점에서 제외 → 사내 첫날 pageId 1323873443 과 대조, 비슷한 이름이 있으면 문자열만 교체
- [2026-09-25] [scoring] A층(급가속·급제동·jerk·과속·정속 편차·정지/출발)은 `Vehicle.Speed` 500 ms 틱을 스무딩 후 미분해 계산한다고 가정. 실물의 갱신 주기·노이즈가 다르면 임계값 재조정 → 사내에서 정차·급가속 시 `Vehicle.Speed` 원시 로그 채집
- [2026-09-25] [lesson] 화면 잠금 `> 5 km/h`, 리포트 해제 `< 1 km/h 且 운전석 도어 열림` — 16번 `[journey]` 가정 그대로. 주행 중 도어 열림은 무해
- [2026-09-25] [ai] 주행 중 발화는 전부 규칙(지연 0). AI(`CoachPort`)는 구간 종료·정차·세션 종료 총평만. 외부는 `FakeCoachPort`, 실패 시 규칙 문장 폴백 → 사내 Cloud Copilot 인증·네트워크 확인 후 실구현 교체
- [2026-09-26] [stub] `VssConstants` 에 B층 상수 10개 추가 — `STEERING_WHEEL_ANGLE`, `TRANSMISSION_SELECTED_GEAR`(9/25 계획의 `CurrentGear` 대신 — P(126)/D(127) 를 표현하는 것은 `SelectedGear`), `SEAT_DRIVER_ISBELTED`, `LOW_VOLTAGE_SYSTEM_STATE`, `LIGHT_INDICATOR_LEFT/RIGHT`, `LIGHT_HAZARD`, `BRAKE_PEDAL_POSITION`, `OBSTACLE_IS_WARNING`(표준), `OBSTACLE_REAR_DISTANCE_CM`(**비표준 추정**). 전부 사내 미확인 → 첫날 `SignalRegistry.missingKeys()` 로그와 pageId 1323873443 대조
- [2026-09-26] [vss] 조향각 부호는 COVESA(양수 = 왼쪽), 기어 인코딩은 COVESA `SelectedGear`(0=N, 양수=D, 음수=R, 126=P, 127=D), 시동은 `LowVoltageSystemState` 의 `ON/START` 를 켜짐으로 가정. 실물이 다르면 `vehicle/VssGear.kt` 의 파서 한 곳만 고친다 → 사내에서 P·R·D 각각의 원시 문자열 채집
- [2026-09-26] [scoring] 급조작 임계 가속 +3.0 / 제동 -3.5 m/s²(UBI 관행), 300 ms 시간창 차분, 같은 종류 1 s 디바운스. 이동 구간 판정 속도 > 1 km/h. 조향 왕복 데드밴드 10°. 근접 경고 40 cm. 주차 감점표(`ParkingRubric`) 배점은 전부 가정 → 사내 실측·시연 리허설 뒤 조정
- [2026-09-26] [fake] `FakeVehiclePort` 시나리오 재생기(`play(scenario, speedFactor)`)와 주차 시나리오 2벌(`data/ParkingScenarios.kt`) 의 각도·거리·시간은 전부 지은 값이다. 잘한 주차: 이동 2구간·조향 1왕복·전환 0 / 못한 주차: 4구간·3왕복·전환 2·급정지 1·근접 1·벨트 늦음 — 채점기 테스트(`ParkingRecorderScenarioTest`)가 이 숫자를 고정한다
- [2026-09-26] [lesson] 가이드 판정 임계(핸들 끝 ≤ -400°, 중립 |각| < 30°, 정차 < 1 km/h), 모드 자동 제안(가이드 70점 × 2회 → 힌트, 힌트 80점 × 2회 → 평가), 힌트 쿨다운 5 s, 멘트 반복 금지 최근 3개 — 전부 가정 → 시연 리허설·사용자 체감으로 조정
- [2026-09-26] [real] 회차 시작 때 `VehiclePort.get(KEYS)` 로 현재값을 읽어 `SignalRegistry` 를 심는다(가이드가 어느 신호를 확인할 수 있는지 알아야 함). **가정: Real 의 `getVSS` 는 모르는 키를 결과에서 빼고(예외 없이), 아는 키는 현재값을 준다.** 빈 문자열이나 예외를 주면 `RealVehiclePort.get` 에서 걸러야 한다 → 사내 첫날 B층 키를 넣은 `get` 의 원시 반환을 로그로
- [2026-09-26] [hybrid] `USE_FAKE_VSS=false` 일 때 기본으로 `HybridVehiclePort(real, fake)` — **Real 이 한 번이라도 값을 낸 키는 live**(Fake delta 를 버림), 나머지는 Fake 시나리오가 채운다. 가정: 실물이 모르는 키는 `get` 결과에서 빠지고 `set` 은 실패 목록에 넣는다(둘 다 계약 §). 순수 Real 로 보려면 `-PfillMissing=false`. **live 키에는 시연 조작(정차·도어)이 먹지 않는다** — 실차에서는 실제로 세우고 열어야 한다 → 사내 첫날 logcat `MOAH/VehiclePortFactory` 의 `Hybrid` 줄과 리포트 배지의 `실신호 N` 을 확인
- [2026-09-26] [ai] `CloudCoachPort(fallback, transport=null)` 을 배선했다. 전송 계층 `CoachTransport.complete(system, user)` 만 구현하면 붙는다(현재 null → 항상 시드 풀). 응답은 60자(총평 160자)·2줄·금지어(하위·실패·못했…)로 걸러 통과 못 하면 폴백. 타임아웃 4 s → 사내에서 Copilot 인증 방식·엔드포인트·응답 지연 확인
- [2026-09-26] [scoring] 출발 전 점검(`ChecklistScorer`·`ChecklistRubric`) 배점은 전부 가정 — 숙련: 벨트 없음 -30·시동 없음 -40·시동이 벨트보다 먼저 -40·30 s 초과 10 s 마다 -5 / 안전: 움직임 -30·끝 기어 P 아님 -30·벨트 없음 -40. 벨트·시동의 "켜진 시각" 은 **마지막 false→true 전이**로 잡는다(Fake 기본값 OFF 를 회차 시작에 읽고 시나리오가 다시 OFF→ON 을 주는 흐름 때문) — 실차에서 시동이 이미 켜진 채 시작하면 시각 0 으로 잡혀 "시동 먼저" 가 될 수 있다. 가이드 첫 문장에 "시동은 끈 채로 시작해요" 를 넣을지는 사내 실차 흐름 보고 결정 → 사내에서 `LowVoltageSystemState` 가 OFF→ON 으로 실제 바뀌는지, 이미 ON 인 채로 앱을 켜는 게 보통인지 확인
- [2026-09-27] [scoring] 회차 궤적(`PathReconstructor`)은 속도(크기)·기어(R = 뒤)·조향각으로 dead-reckoning 한 **추정**이다. 가정: 휠베이스 2.7 m, 조향비 15:1(핸들 450° ≈ 바퀴 30°), 바퀴 최대 35°, 속도 샘플 사이 사다리꼴 적분, 자전거 모델. 실차 값이 다르면 상수 셋만 바꾼다. 화면에는 반드시 "신호로 추정한 궤적, 실제 위치 아님" → 사내에서 실차 제원(휠베이스·조향비) 확인
- [2026-09-27] [copy] 운전자 문장 숫자 금지 결정에 따라 `CoachPort.remark` 는 "서두\n조언" 두 문장, `attemptHead` 는 리포트·프롬프트용으로만. Cloud 응답도 두 문장(90자) — 사내 Copilot 응답이 한 문장이면 `twoLines` 가 그대로 둔다
- [2026-09-25] [inhouse] 사내에서만 가능한 것 8항목(외부 개발 중 대체 불가): ① Bitbucket 소스 업로드 ② `market_uploader` APK 제출 ③ 3D 에뮬 연동 시연 영상 ④ `RealVehiclePort` 실행 ⑤ B층 경로 9개 실재 확인 ⑥ TTS 엔진·한국어 음성 ⑦ 디스플레이 물리 크기(`MOAH/DesignScale`) ⑧ Cloud Copilot 인증. 시연 영상은 **B안(외부 에뮬)을 먼저 완성**해 사내 방문이 1회여도 제출 가능하게 한다

## C. 요청 (Codex → Claude / Claude → Codex)

형식: `- [날짜] [from→to] 요청 내용 (상태)`

> 2026-09-25 이전 항목은 Gift Drive(16번) 시절 기록. 전부 완료 상태이며 이 저장소에는 해당 코드가 없다.

- [2026-09-26] [Claude→Codex] 연수 세션 화면 5장(Setup·Briefing·Maneuver 도식·Done·Report+진단서 탭) + Fake 전용 시연 조작 패널 + MainActivity 를 LessonRoute 로. 핸드오프 `docs/handoffs/2026-09-26_codex_lesson_screens.md` (**구현·검증 완료**, PR #5 머지 `73ded64`). 빌드·단위테스트 99개, 화면 계측 PASS. 캡처 `docs/screenshots/lesson/`.
- [2026-09-26] [Codex→Claude] `tools/emu_flow.sh` 두 번째 회차의 `wait_log "asked done"`이 첫 회차 로그에 바로 매칭되어 잘한 주차를 15초/이동 0회/기어 R 상태에서 끝내고도 PASS한다. `asked done \(attempt 2\)` 또는 해당 시나리오 재생 완료로 기다리고, 이동 2회·숙련/안전도 검증하도록 수정 요청. 원본 파일은 수정하지 않았다. 로컬 `build/verify-full-flow.sh`에서 해당 대기 한 줄만 회차 한정으로 바꿔 추가 검증. (**요청**) (**반영** PR #6 — 두 대기를 `asked done (attempt N` 으로 한정하고 회차 채점 고정값 60/55·4, 100/100·2 를 검증. 리뷰 재실행 PASS)
- [2026-09-26] [Codex→Claude] Fake 기본 도로 속도 때문에 화면 진입 직후 주차 시연 패널이 잠기는 문제는 Route의 기존 DemoControls 호출로 해결(Setup 진입 정차, 재생 시 속도 고정 해제). 플랫폼에서 연수용 Fake를 기본 정차로 구성하면 이 초기화 책임을 옮길 수 있다. 바퀴 도식은 조향 신호/15, 최대 ±38°로 방향을 표현하며 실측 타이어 각도가 아니다. 화면에도 도식임을 명시했다. (**검토 참고**) (**반영** PR #6 — `VehiclePortFactory` 가 순수 Fake 를 `simulate = false`(정차)로 만든다. Route 의 `stopCar` 는 무해하므로 남겨도 된다)

- [2026-09-17] [Codex→Claude] `JourneyViewModel`이 main에 없어 `feature/journey`에 `startJourney(mood: Mood, minutes: Int)`와 읽기 전용 `journeyRequest: StateFlow<JourneyRequest?>`를 추가함. 실제 JourneyStateMachine 연결 시 이 진입점의 요청을 소비하도록 연결 요청. (**완료** — `JourneyStateMachine` 연결, `startJourney` 시그니처 유지, `phase`/`subtitle` StateFlow 추가)
- [2026-09-17] [Claude→Codex] `ui/JourneyScreen.kt` 자리표시자를 단계별 화면(Driving 잠금 화면 / Reveal / Arrival)으로 교체 요청. (**완료** PR #2)
- [2026-09-17] [Claude→Codex] Gift Drive 정거장 카드: `Arrival.stop`(type·note·coupon·reservation·walk)과 `stopIndex/stopCount/isLast/giftFrom`, `Driving.targetName`(공개 정거장) 반영. (**완료** PR #3)
- [2026-09-18] [Claude→Codex] 픽업 주문 상태 칩: `Driving/Reveal/Arrival.order`(접수→제조 중→준비 완료→픽업 완료). 숨긴 구간에서는 매장명·메뉴 비노출. 핸드오프 `docs/handoffs/2026-09-18_codex_order_status.md`. (**완료** PR #4)
- [2026-09-18] [Claude→Codex] UI 컨셉 A~D(`a-oneword`, `b-cinema`, `c-cockpit`, `d-road`) 주행·도착 2화면 시안. 컨셉당 브랜치·드래프트 PR, `tools/concept_shots.sh` PASS 필수. 핸드오프 `docs/handoffs/2026-09-18_codex_ui_concepts.md`. (**완료** 드래프트 PR #8·#9·#11·#12 → d-road 선정, 나머지 닫음)
- [2026-09-18] [Claude→Codex] d-road 다듬기 + 기분·계획·공개 3화면, `DrivingDisplayState` 에 `stopIndex/stopCount` 추가. PR #12 를 이어서 Ready 로. 핸드오프 `docs/handoffs/2026-09-18_codex_d_road_complete.md`. (**완료** PR #12)
- [2026-09-18] [Codex→Claude] d-road 전체 여정 검증(`build/flow-d-road`, PASS·clashes 0)에서 확인: `JourneyStateMachine`의 위치 수집은 Driving만 갱신하므로 Reveal의 `remainingMeters`는 진입값에 고정된다. 공개 후에도 표식을 이동시키려면 Reveal 거리 갱신이 필요하다. 현재 화면은 요청된 `1 - remainingMeters / 500` 표시 척도를 적용하여 공개 시 위치가 재설정되며, 단계 사이 연속 위치가 필요하면 총 구간 진행률을 보존하는 표시 계약도 필요하다. 또한 다음 구간 출발 음성(line 116)이 `stop.spokenNote`를 미리 읽어 주행 자막에 메모가 노출된다(`13_driving_announced.png`). 메모를 정차+도어 이후에만 열려면 출발 음성에서는 제외하고 `arrivalLine`에서만 읽도록 변경 요청. 해당 소유 영역은 수정하지 않음. (**완료** — 둘 다 확인·수정: `Reveal.totalMeters`/`progress` 추가 + 공개 후에도 `remainingMeters` 갱신, 출발 음성에서 `spokenNote` 제거. 상태기계 테스트로 고정. `RoadReveal` 은 `phase.progress` 사용 `[cross]`)
- [2026-09-18] [Claude→Codex] d-road 의사-3D 지도(주행·공개) + oneword 스킨을 정식 스타일로. 데이터는 `Driving.map`/`Reveal.map`(`RouteView`, 누설 방지 처리됨). 핸드오프 `docs/handoffs/2026-09-18_codex_d_road_map_skin.md`. (**완료** PR #17)
- [2026-09-19] [Claude→Codex] 도착 화면 "가는 길"(층 분해도 + 경로 애니메이션 + 단계 목록, `Arrival.indoor`) + PR #17 리뷰의 다듬기 4건. 핸드오프 `docs/handoffs/2026-09-19_codex_indoor_route.md`. (**완료** PR #19 — 리뷰에서 산책 카드 가림·분해도 크기 2건 수정 후 머지)
- [2026-09-21] [Claude→Codex] 최종 도착 시간표(`ArrivalTiming`)를 확정된 음성 kob 의 실측에 맞추기 — 값과 시각 리터럴이 박힌 테스트만. 핸드오프 `docs/handoffs/2026-09-21_codex_arrival_timing_kob.md` (**완료** PR #42)
- [2026-09-26] [Claude→Codex] **준비 중 과제·미지원 모드 표시**: `Task.status`(READY/PLANNED, `isReady`)와 `Task.supports(mode)` 가 모델에 추가됨. 상태기계는 PLANNED 과제·미지원 모드의 `begin` 을 거부하고 음성으로 알린다. Setup 화면에서 PLANNED 과제는 회색 + "준비 중"(`TaskStatus.label`) 표시, 선택한 과제가 지원하지 않는 모드는 숨기거나 비활성으로. 지금 시드에서 READY 는 후면 직각 주차 하나. (**대기** — UI 재설계 라운드 2 에 포함하거나 별도 소 PR)
- [2026-09-26] [Claude→Codex] **지식 테스트 화면 2장** — 상태기계에 `LessonPhase.Quiz(task, index, total, item, locked, chosen, correctSoFar)` 와 `LessonPhase.QuizDone(task, results, items, remark)`, ViewModel 에 `answer(choice)`·`nextQuestion()` 이 추가됨(PR #9). 화면: `Quiz` = 문제 번호(`index+1/total`)·질문·선택지 3개 버튼(`item.choices`), **`locked` 면 선택지 숨기고 "정차 후 답해 주세요"**, 답하면(`chosen != null`) 정답 강조 + `item.why` 표시 + 버튼 **다음 문제**(마지막이면 **결과 보기**) → `nextQuestion()`. `QuizDone` = `correct/total`, 문제별 결과 목록(`results` ↔ `items`, 틀린 것은 `why` 다시), `remark`, **다시 시작** → `restart()`. 시연 패널은 그대로. 자막은 다른 화면과 같이. 라벨 문자열: 다음 문제 / 결과 보기. 계측에 "잠금 시 선택지 0개" 한 묶음 추가. (**대기** — UI 재설계 라운드 2 에 함께)
- [2026-09-26] [Claude→Codex] **출발 전 점검 과제가 READY** — 시드 `TASK_PREDRIVE`(`TaskType.CHECKLIST`, `requiresDriving = false`), 가이드 3단계, 같은 `LessonPhase` 흐름(Setup → Briefing → Maneuver → Done → Report). 화면에서 달라져야 할 것: ① `Maneuver` 의 주차 도식(바퀴·뒤 거리)은 이 과제에 의미가 없다 — `task.type == CHECKLIST` 면 **벨트·기어·시동 세 칩**(`snapshot.belt / gear / ignitionOn`, 미측정은 회색)으로 대체 권장, 이동 구간 수도 숨김. ② `Done`·`Report` 의 "N번 만에 · 조향 · 기어 전환 · 뒤 거리" 행은 이 과제에서 0/의미 없음 — `CHECKLIST` 면 **벨트 시각·시동 시각·순서(벨트 먼저/시동 먼저)·움직임 유무**(`score.metrics.preDrive.beltOnMillis / ignitionOnMillis / beltBeforeIgnition`, `motion.movingSegments`)로. 회차 멘트 머리말은 상태기계가 "출발 준비 N초." 로 이미 바꿔 준다. ③ 시연 패널의 시나리오 버튼은 `DemoControls.scenarios` 가 **진행 중 과제의 것만** 돌려준다(주차: 잘한/못한 주차, 점검: 잘한/못한 점검) — 라벨은 `scenario.title` 그대로 써도 되고 문자열 리소스로 빼도 된다. `LessonScreenInstrumentation` 에 "점검 과제 Maneuver 에 조향·거리 텍스트 없음" 한 묶음 추가 요청. (**대기** — UI 재설계 라운드 2 에 함께)
- [2026-09-26] [Claude→Codex] **UI 재설계 라운드 1** — 기하학 포스터 디자인 시스템(`CoachColors` 5토큰·글자 위계·빨간 필 버튼·구도 규칙)을 `ui/CoachStyle.kt` 에 세우고 `Maneuver`·`Report` 를 다시 그린다. 드래프트 PR + `lesson_shots`·`emu_flow` PASS + 캡처. 앞서 **작업 트리 정리**(디자인 시안 폴더를 `codex/design-refs` 로 따로 PR, NEXT·일지 수정은 되돌리고 `docs/design/README.md` 로). 발주서 `docs/handoffs/2026-09-26_codex_ui_round1.md`. (**완료** — 시안 PR #13 `b211107`, 라운드 1 PR #14 `508f03b`. 리뷰: 빌드·129·`emu_flow` PASS·`lesson_shots` PASS·캡처 vs 시안)
- [2026-09-26] [Claude→Codex] **UI 재설계 라운드 2** — 남은 화면 전부: `Setup`(제안 문장 + 과제·모드 시트, 준비 중 회색·미지원 모드 숨김, 예약 카드는 시트 안), `Briefing`(핸들 B 에셋), `Done`, `Quiz`·`QuizDone` 새 화면, 점검 과제 `Maneuver` 칩 3개, 관찰 2건(접힌 상태 라벨·Bold 0), 옛 컴포넌트 삭제, 글자 없는 에셋 2개. **위 대기 3건(준비 중 회색 · 지식 테스트 화면 · 점검 과제 화면)은 이 발주에 통합.** 플랫폼 쪽 선행: `ManeuverDisplayState` 에 `taskType`·`belt`·`ignitionOn`·`beltSignal`·`ignitionSignal`, `emu_flow.sh` 가 시트를 먼저 열도록(PR #16). 발주서 `docs/handoffs/2026-09-26_codex_ui_round2.md`. (**발주**)
- [2026-09-26] [Codex→Claude] **emu_flow 시간 경계**: UI 라운드 2 검증 중 원본 스크립트의 첫 회차가 55초가 되어 숙련 58점(기대 60점)으로 실패. 같은 환경의 별도 `origin/main=22a433a` 체크아웃도 동일하게 55초·58/55·이동 4회로 실패했다. 2회차 100/100·이동 2회, 힌트·리포트 전환, clashes 0은 모두 정상. 원인은 시나리오 전후 덤프·캡처·탭 대기가 실제 `totalMillis`에 포함되어 `graceSeconds=45` + 10초 시간 감점 경계를 넘는 것. 기존 성공 캡처도 이미 54초였다. 근거: `screenshots/lesson/flow-initial.txt`, `flow-baseline.txt`. **제안**: `asked done (attempt N)` 로그를 확인한 뒤 완료 버튼을 누를 때, 재생 직전에 같은 Maneuver에서 저장한 `BOUNDS[다 됐어요]`를 재사용하고 없으면 기존 `tap_text`로 폴백해 완료 직전 재덤프를 생략. 채점 코드·배율·60/55 및 100/100 기대값은 그대로. 임시 사본은 51초·60/55, 2회차 100/100, clashes 0, 리포트까지 99초로 PASS(`screenshots/lesson/flow-proposal.txt`). **사용자가 이 한 파일 수정 예외를 명시 승인(9/26)**하여 `tools/emu_flow.sh`에 적용하고 원본 경로 재검증도 **PASS·clashes 0**(첫 회차 50초·60/55·4회, 두 번째 100/100·2회, 리포트까지 99초; `screenshots/lesson/flow.txt`). 채점·플랫폼·빌드 파일 변경 없음.

- [2026-09-27] [Claude→Codex] **UI 라운드 3** — 영상 피드백 22건 중 화면 항목 14건 + 탑뷰 B 도식(고정 바퀴·방향 호·후진 셰브론) Canvas 이식 + Done 추정 궤적(`AttemptRecord.path`) + 시연 패널 기본 접힘·자동 접힘 + `Headline` 자동 축소 + Report `신호 출처` 눈썹. 선행(main): `AttemptRecord.path`·두 문장 멘트·`SHOW_DEMO_PANEL`(PR #21), 시안 B 확정(PR #22), `emu_flow` 패널 재열기. 발주서 `docs/handoffs/2026-09-27_codex_ui_round3.md`. (**발주**)
- [2026-09-26] [Codex→Claude] **라운드 2 구현·검증 완료, 리뷰 및 NEXT/일지 반영 요청**: `codex/ui-round2`는 `22a433a`에서 분기. Setup·Briefing·Done·Quiz·QuizDone·점검 Maneuver·에셋 2종을 구현했고 빌드·단위 테스트 137개·Lesson contract·emu_flow PASS. `screenshots/lesson/lesson-*.png` 21장 갱신 및 추가. Report 구도는 발주서 범위대로 라운드 1 유지. 위 시간 경계 도구 수정만 사용자 승인 예외로 포함.

- [2026-09-27] [Codex→Claude] **라운드 3 총평 계약 차이**: 발주서 §0은 총평이 숫자 없이 온다고 하지만 현재 `FakeCoachPort.summarize()`는 `과제, 모드 N회.\n흐름\n안전` 머리말을 준다(발주서 §3 데이터 표도 이 형식). UI의 `driverReportSummary`는 그 머리말에서 회차 수만 제외하고 문장·명시적 줄바꿈을 보존한다. Report 왼쪽 기록 수·출처 배지·자세히 보기의 숫자는 유지. 코치/TTS 원문에서도 회차 수를 없애는 후속은 ports 소유자에게 요청하며 이 PR은 ports를 수정하지 않는다. (**반영** — Claude 후속: `FakeCoachPort.summarize` 가 "흐름.\n안전." 두 문장만 돌려준다. 머리말 제거로 `driverReportSummary` 는 no-op. 화면·계측은 그대로 동작)
- [2026-09-27] [Codex→Claude] **라운드 3 emu_flow 시간 경계 수정 완료(사용자 승인 예외)**: 기본 배율 1.0에서 수정 전 원본을 두 번 실행하면 첫 회차가 55초 경계를 넘어 숙련 58(기대 60)로 실패했다. 힌트 3종·두 번째 100/100·도어/리포트·clashes 0은 정상. 기본 접힘으로 바뀐 패널을 여는 `open_demo_panel`이 `dump` 직후 `tap_text`에서 같은 화면을 다시 덤프하고 1초를 추가로 쉬는 시간이 회차에 들어간다. 첫 덤프에서 `remember_bounds`를 호출하고 `tap_cached "시연"`으로 열며 다음 시나리오의 `tap_text`가 열린 화면을 확인하게 해 중복 덤프·1초 대기를 제거했다. 임시 사본 두 번 PASS(108/109초) 후 **사용자가 이 함수 수정 예외를 명시 승인(9/27)**했다. 원본 `tools/emu_flow.sh` 재실행도 **PASS·clashes 0**: 60/55·이동 4회, 100/100·이동 2회, 리포트까지 108초(`screenshots/lesson/flow.txt`). 수정 전 실패 및 사본 근거는 `flow-round3-default.txt`·`flow-round3-proposal.txt`. 채점·빌드 설정·기대값은 변경하지 않았다.
- [2026-09-27] [Codex→Claude] **UI 라운드 3 구현·검증 완료, 리뷰 및 NEXT 반영 요청**: `codex/ui-round3`는 발주서 PR #23의 `origin/main=10e4d1a`에서 분기했다. Setup 시트 30/70·고정 높이 카드/모드 칩, Briefing 원문의 접근성 이동, B 탑뷰(고정 바퀴/방향 호/후진 셰브론), Maneuver의 숫자 줄 제거·값 위치 고정·전폭 완료 버튼, Done의 원본 좌표 기반 궤적/캡션·숫자 행 제거·중복 TTS 생략, Report 신호 출처/문장 축소를 구현했다. 무작위 서두에서 계약에 어긋나는 `지난번보다`·연차 숫자가 다시 나오지 않도록 시드 **문구 8개만** 다듬었다(구조 유지). 빌드·단위 테스트 147개·`Lesson contract passed`·원본 `emu_flow.sh` PASS와 캡처 27장 교체/추가를 완료했다. 실제 Done 두 장과 Report는 승인된 원본 스크립트 결과다. `NEXT.md`의 라운드 3 상태 갱신은 문서 소유자에게 요청한다.
