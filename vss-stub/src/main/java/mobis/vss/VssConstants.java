package mobis.vss;

/**
 * VSS dot 경로 상수. 문자열은 사내 문서(pageId 1323873443, 1,251개)와 정확히 일치해야 한다.
 * 오타는 예외 없이 조용히 무시되므로(데이터 미수신) 새 상수 추가 시 원문과 대조하고
 * docs/INTEGRATION.md B절에 기록한다.
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
    public static final String DOOR_DRIVER_ISOPEN = "Vehicle.Cabin.Door.Row1.DriverSide.IsOpen";

    // ───── 아래는 2026-09-26 운전 연수(주차·주행) B층 채점용. COVESA VSS 표준명 기준 **추정** — 전부 사내 미확인 ─────
    // 경로가 실물과 다르면 예외 없이 값이 오지 않는다 → 앱은 SignalAvailability.MISSING 으로 표시하고 채점에서 뺀다.
    // 사내에서 pageId 1323873443 과 대조해 이름이 다르면 문자열만 고친다 (docs/INTEGRATION.md B절 2026-09-26).

    /** 핸들 각도. Type: sensor, int16/float, degree. COVESA: 양수 = 왼쪽, 음수 = 오른쪽 */
    public static final String STEERING_WHEEL_ANGLE = "Vehicle.Chassis.SteeringWheel.Angle";
    /** 선택 기어. Type: sensor, int8. COVESA: 0=N, 1..=D(단수), -1..=R, 126=P, 127=D */
    public static final String TRANSMISSION_SELECTED_GEAR = "Vehicle.Powertrain.Transmission.SelectedGear";
    /** 운전석 안전벨트. Type: sensor, boolean */
    public static final String SEAT_DRIVER_ISBELTED = "Vehicle.Cabin.Seat.Row1.DriverSide.IsBelted";
    /** 저전압 시스템 상태(시동 대용). Type: sensor, string enum: UNDEFINED/LOCK/OFF/ACC/ON/START */
    public static final String LOW_VOLTAGE_SYSTEM_STATE = "Vehicle.LowVoltageSystemState";
    /** 방향지시등 좌. Type: sensor, boolean */
    public static final String LIGHT_INDICATOR_LEFT = "Vehicle.Body.Lights.DirectionIndicator.Left.IsSignaling";
    /** 방향지시등 우. Type: sensor, boolean */
    public static final String LIGHT_INDICATOR_RIGHT = "Vehicle.Body.Lights.DirectionIndicator.Right.IsSignaling";
    /** 비상등. Type: sensor, boolean */
    public static final String LIGHT_HAZARD = "Vehicle.Body.Lights.Hazard.IsSignaling";
    /** 브레이크 페달. Type: sensor, uint8, percent */
    public static final String BRAKE_PEDAL_POSITION = "Vehicle.Chassis.Brake.PedalPosition";
    /** 장애물 감지 경고(주차센서). Type: sensor, boolean. COVESA 표준에 있는 것은 이 boolean 뿐 */
    public static final String OBSTACLE_IS_WARNING = "Vehicle.ADAS.ObstacleDetection.IsWarning";
    /** 후방 장애물 거리(cm). Type: sensor, float. **비표준 추정** — 사내 목록에 없을 가능성이 가장 높다. 없으면 OBSTACLE_IS_WARNING 만 쓴다 */
    public static final String OBSTACLE_REAR_DISTANCE_CM = "Vehicle.ADAS.ObstacleDetection.Rear.Distance";
}
