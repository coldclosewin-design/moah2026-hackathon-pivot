package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.SignalRegistry
import com.moah.hackathon.vehicle.toVssBoolean
import com.moah.hackathon.vehicle.toVssFloat
import com.moah.hackathon.vehicle.toVssGear
import com.moah.hackathon.vehicle.toVssIgnitionOn
import mobis.vss.VssConstants

/**
 * 한 회차 동안 [VehiclePort.observe] 의 delta 를 받아 신호별 시계열로 쌓고, 끝나면 [ParkingMetrics] 를 낸다.
 * 상태기계가 `onDelta(now, delta)` 만 불러 주면 된다. 안드로이드·코루틴 의존 없음.
 *
 * 신호가 [SignalRegistry] 에서 MISSING 이면 해당 B층 지표는 null 이다 — 시계열이 비어 있어서 자연히 그렇게 된다.
 */
class ParkingRecorder(private val registry: SignalRegistry) {
    private val speed = ArrayList<SpeedSample>()
    private val angle = ArrayList<AngleSample>()
    private val gear = ArrayList<GearSample>()
    private val distance = ArrayList<DistanceSample>()
    private val warning = ArrayList<FlagSample>()
    private val belt = ArrayList<FlagSample>()
    private val ignition = ArrayList<FlagSample>()

    private var lastMillis = 0L

    fun onDelta(tMillis: Long, delta: Map<String, String>) {
        registry.onValues(delta)
        if (tMillis > lastMillis) lastMillis = tMillis
        delta[VssConstants.VEHICLE_SPEED]?.toVssFloat()?.let { speed.add(Sample(tMillis, kotlin.math.abs(it))) }
        delta[VssConstants.STEERING_WHEEL_ANGLE]?.toVssFloat()?.let { angle.add(Sample(tMillis, it)) }
        delta[VssConstants.TRANSMISSION_SELECTED_GEAR]?.toVssGear()?.let { gear.add(Sample(tMillis, it)) }
        delta[VssConstants.OBSTACLE_REAR_DISTANCE_CM]?.toVssFloat()?.let { distance.add(Sample(tMillis, it)) }
        delta[VssConstants.OBSTACLE_IS_WARNING]?.toVssBoolean()?.let { warning.add(Sample(tMillis, it)) }
        delta[VssConstants.SEAT_DRIVER_ISBELTED]?.toVssBoolean()?.let { belt.add(Sample(tMillis, it)) }
        delta[VssConstants.LOW_VOLTAGE_SYSTEM_STATE]?.toVssIgnitionOn()?.let { ignition.add(Sample(tMillis, it)) }
    }

    /**
     * 속도가 한 번도 안 왔으면 null — A층조차 성립하지 않는다(속도는 확정 신호라 사실상 없다).
     * @param untilMillis 회차 종료 시각(버튼). null 이면 마지막 신호 시각까지.
     */
    fun metrics(untilMillis: Long? = null): ParkingMetrics? {
        val motion = MotionSegmenter.summarize(speed, endMillis = untilMillis ?: lastMillis) ?: return null
        return ParkingMetrics(
            motion = motion,
            harshEvents = HarshEventDetector.detect(speed),
            steering = SteeringReversalCounter.summarize(angle),
            gear = GearShiftCounter.summarize(gear),
            proximity = ProximityMonitor.fromDistance(distance) ?: ProximityMonitor.fromWarningFlag(warning),
            preDrive = PreDriveChecklist.summarize(belt, ignition, motion.firstMoveMillis),
        )
    }

    fun score(rubric: ParkingRubric = ParkingRubric(), untilMillis: Long? = null): ParkingScore? =
        metrics(untilMillis)?.let { ParkingScorer.score(it, registry.badge(), registry.missingKeys(), rubric) }

    /** 출발 전 점검 과제 — 같은 시계열을 [ChecklistScorer] 로. 움직이지 않아도 속도 0 샘플이 있으면 채점된다. */
    fun scoreChecklist(rubric: ChecklistRubric = ChecklistRubric(), untilMillis: Long? = null): ParkingScore? =
        metrics(untilMillis)?.let { ChecklistScorer.score(it, registry.badge(), registry.missingKeys(), rubric) }

    fun reset() {
        speed.clear(); angle.clear(); gear.clear(); distance.clear(); warning.clear(); belt.clear(); ignition.clear()
        lastMillis = 0L
    }

    companion object {
        /** 주차 회차가 구독하는 키 전부. 배지의 분모. */
        val KEYS: Set<String> = setOf(
            VssConstants.VEHICLE_SPEED,
            VssConstants.STEERING_WHEEL_ANGLE,
            VssConstants.TRANSMISSION_SELECTED_GEAR,
            VssConstants.OBSTACLE_REAR_DISTANCE_CM,
            VssConstants.OBSTACLE_IS_WARNING,
            VssConstants.SEAT_DRIVER_ISBELTED,
            VssConstants.LOW_VOLTAGE_SYSTEM_STATE,
            VssConstants.DOOR_DRIVER_ISOPEN,
        )
    }
}
