package mobis.vss;

/**
 * VSS dot 경로 상수. 문자열은 사내 문서(pageId 1323873443, 1,251개)와 정확히 일치해야 한다.
 * 오타는 예외 없이 조용히 무시되므로(데이터 미수신) 새 상수 추가 시 원문과 대조하고
 * docs/INTEGRATION.md B절에 기록한다.
 *
 * <p><b>이름 규칙(2026-09-30, 사내 이관 1차에서 확인)</b>: 사내 jar 의 {@code VssConstants} 는 경로를 그대로
 * 대문자·밑줄로 바꾼 이름이다 — {@code Vehicle.Cabin.Door.Row1.DriverSide.IsOpen} →
 * {@code VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN}. 스텁도 같은 이름을 써야 사내 컴파일이 깨지지 않는다.
 * 실물에 없는 경로는 여기 두지 않는다(Fake 전용은 앱의 {@code vehicle/SimOnlySignals.kt}).
 */
public class VssConstants {

    private VssConstants() {
    }

    /** Type: sensor, float, km/h */
    public static final String VEHICLE_SPEED = "Vehicle.Speed";
    /** Type: sensor, boolean */
    public static final String VEHICLE_ADAS_ABS_ISENABLED = "Vehicle.ADAS.ABS.IsEnabled";
    /** Type: actuator, float, km/h */
    public static final String VEHICLE_ADAS_CRUISECONTROL_SPEEDSET = "Vehicle.ADAS.CruiseControl.SpeedSet";
    /** Type: actuator, boolean */
    public static final String VEHICLE_BODY_HORN_ISACTIVE = "Vehicle.Body.Horn.IsActive";
    /** 도어 예제 신호 (Template App 기준). Type: actuator, boolean */
    public static final String VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN = "Vehicle.Cabin.Door.Row1.DriverSide.IsOpen";

    // ───── 아래는 2026-09-26 운전 연수(주차·주행) B층 채점용. 2026-09-30 사내 이관 1차에서 경로·이름 확인 ─────
    // 경로가 실물과 다르면 예외 없이 값이 오지 않는다 → 앱은 SignalAvailability.MISSING 으로 표시하고 채점에서 뺀다.

    /** 핸들 각도. Type: sensor, int16/float, degree. COVESA: 양수 = 왼쪽, 음수 = 오른쪽 */
    public static final String VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE = "Vehicle.Chassis.SteeringWheel.Angle";
    /** 선택 기어. Type: sensor, int8. COVESA: 0=N, 1..=D(단수), -1..=R, 126=P, 127=D */
    public static final String VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR = "Vehicle.Powertrain.Transmission.SelectedGear";
    /** 운전석 안전벨트. Type: sensor, boolean */
    public static final String VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED = "Vehicle.Cabin.Seat.Row1.DriverSide.IsBelted";
    /** 저전압 시스템 상태(시동 대용). Type: sensor, string enum: UNDEFINED/LOCK/OFF/ACC/ON/START. 사내 실물은 회차 시작 때 빈 문자열을 준다(→ 미수신 취급) */
    public static final String VEHICLE_LOWVOLTAGESYSTEMSTATE = "Vehicle.LowVoltageSystemState";
    /** 방향지시등 좌. Type: sensor, boolean */
    public static final String VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING = "Vehicle.Body.Lights.DirectionIndicator.Left.IsSignaling";
    /** 방향지시등 우. Type: sensor, boolean */
    public static final String VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING = "Vehicle.Body.Lights.DirectionIndicator.Right.IsSignaling";
    /** 비상등. Type: sensor, boolean */
    public static final String VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING = "Vehicle.Body.Lights.Hazard.IsSignaling";
    /** 전조등(하향등) 켜짐. Type: actuator, boolean. 2026-10-06 사내 피드백 #4: 사내 jar 에 같은 경로·이름이 있어 SimOnlySignals 에서 옮김 */
    public static final String VEHICLE_BODY_LIGHTS_BEAM_LOW_ISON = "Vehicle.Body.Lights.Beam.Low.IsOn";
    /** 앞 와이퍼 모드. Type: actuator, string(COVESA: OFF/SLOW/MEDIUM/FAST/INTERVAL/RAINSENSOR — 사내 값 포맷 미확인). 2026-10-06 사내 피드백 #4 */
    public static final String VEHICLE_BODY_WINDSHIELD_FRONT_WIPING_MODE = "Vehicle.Body.Windshield.Front.Wiping.Mode";
    /** 브레이크 페달. Type: sensor, uint8, percent */
    public static final String VEHICLE_CHASSIS_BRAKE_PEDALPOSITION = "Vehicle.Chassis.Brake.PedalPosition";
    /** 장애물 감지 경고(주차센서). Type: sensor, boolean. ObstacleDetection 아래 실물에 있는 것은 IsEnabled/IsError/IsWarning 뿐 */
    public static final String VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING = "Vehicle.ADAS.ObstacleDetection.IsWarning";
}
