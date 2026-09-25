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
}
