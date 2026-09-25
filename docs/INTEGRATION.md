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
- [ ] 실제 값 포맷 (예: 속도 `"120.5"` vs `"120"`), 업데이트 주기/스레드 타이밍이 Fake와 다르지 않은지
- [ ] getVSS/setVSS 호출부가 백그라운드 스레드인지 (ANR 점검)
- [ ] 구독 해제(`unsubscribeVSS`)가 onPause/onDestroy에서 호출되는지 (메모리 누수)
- [ ] `copilot_config.json` push (Cloud AI 사용 시)
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
- [2026-09-25] [inhouse] 사내에서만 가능한 것 8항목(외부 개발 중 대체 불가): ① Bitbucket 소스 업로드 ② `market_uploader` APK 제출 ③ 3D 에뮬 연동 시연 영상 ④ `RealVehiclePort` 실행 ⑤ B층 경로 9개 실재 확인 ⑥ TTS 엔진·한국어 음성 ⑦ 디스플레이 물리 크기(`MOAH/DesignScale`) ⑧ Cloud Copilot 인증. 시연 영상은 **B안(외부 에뮬)을 먼저 완성**해 사내 방문이 1회여도 제출 가능하게 한다

## C. 요청 (Codex → Claude / Claude → Codex)

형식: `- [날짜] [from→to] 요청 내용 (상태)`

> 2026-09-25 이전 항목은 Gift Drive(16번) 시절 기록. 전부 완료 상태이며 이 저장소에는 해당 코드가 없다.

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
