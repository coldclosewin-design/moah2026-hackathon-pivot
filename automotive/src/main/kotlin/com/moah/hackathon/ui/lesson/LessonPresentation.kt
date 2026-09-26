package com.moah.hackathon.ui.lesson

import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingMetrics
import com.moah.hackathon.vehicle.AvailabilityBadge
import com.moah.hackathon.vehicle.SignalAvailability
import mobis.vss.VssConstants as V
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun profileLine(profile: Profile): String = listOfNotNull(profile.name,
    profile.rustyYears?.let { "장롱 ${it}년차" }, profile.statement.goal?.let { "목표 $it" }).joinToString(" · ")

internal fun taskTypeLabel(type: TaskType): String = when (type) {
    TaskType.CHECKLIST -> "조작"
    TaskType.DRIVING -> "주행"
    TaskType.PARKING -> "주차"
    TaskType.KNOWLEDGE -> "지식"
}

internal fun selectionReason(task: Task, mode: LessonMode, suggestedTask: Task, suggestedMode: LessonMode, reason: String) =
    if (task.id == suggestedTask.id && mode == suggestedMode) reason else task.summary

internal data class MetricLine(val label: String, val value: String)
internal fun metricLines(metrics: ParkingMetrics) = buildList {
    add(MetricLine("이동", "${metrics.motion.movingSegments}회"))
    add(MetricLine("시간", "${metrics.motion.totalMillis / 1000}초"))
    metrics.steering?.let { add(MetricLine("조향 왕복", "${it.reversals}회")) }
    metrics.gear?.let { add(MetricLine("기어 전환", "${it.reverseDriveShifts}회")) }
    metrics.proximity?.minDistanceCm?.let { add(MetricLine("뒤 최소 거리", "${it.roundToInt()} cm")) }
}

internal data class DeltaLine(val text: String, val improved: Boolean)
internal fun deltaLines(delta: ParkingDelta?): List<DeltaLine> = if (delta == null) emptyList() else buildList {
    fun addChange(label: String, amount: Long, unit: String) {
        add(DeltaLine(if (amount == 0L) "지난번과 $label 같아요" else
            "지난번보다 $label ${abs(amount)}$unit ${if (amount < 0) "↓" else "↑"}", amount < 0))
    }
    addChange("이동", delta.segments.toLong(), "회")
    addChange("시간", delta.seconds, "초")
    delta.reversals?.let { addChange("조향 왕복", it.toLong(), "회") }
    delta.shifts?.let { addChange("기어 전환", it.toLong(), "회") }
}

internal fun badgeText(badge: AvailabilityBadge) = "실신호 ${badge.live} · 시뮬레이션 ${badge.simulated} · 미측정 ${badge.missing}"
internal fun signalLabel(signal: SignalAvailability) = when (signal) {
    SignalAvailability.LIVE -> "실신호"
    SignalAvailability.SIMULATED -> "시뮬"
    SignalAvailability.MISSING -> "미측정"
}

internal fun signalName(key: String): String = when (key) {
    V.VEHICLE_SPEED -> "속도"
    V.STEERING_WHEEL_ANGLE -> "조향각"
    V.TRANSMISSION_SELECTED_GEAR -> "기어"
    V.OBSTACLE_REAR_DISTANCE_CM -> "뒤 거리"
    V.OBSTACLE_IS_WARNING -> "근접 경고"
    V.SEAT_DRIVER_ISBELTED -> "안전벨트"
    V.LOW_VOLTAGE_SYSTEM_STATE -> "시동"
    V.DOOR_DRIVER_ISOPEN -> "운전석 도어"
    V.LIGHT_INDICATOR_LEFT -> "왼쪽 방향지시등"
    V.LIGHT_INDICATOR_RIGHT -> "오른쪽 방향지시등"
    else -> "기타 차량 신호"
}

internal fun ManeuverDisplayState.proximityAlert() = obstacleWarning || (rearDistanceCm?.let { it < 40f } == true)
internal fun ManeuverDisplayState.diagramDescription() = buildList {
    add("차량 도식, 뒤쪽이 화면 위")
    add("조향 ${steeringDeg?.let { "${it.roundToInt()}도" } ?: "미측정"}, ${signalLabel(steeringSignal)}")
    add("기어 ${gear ?: "미측정"}, ${signalLabel(gearSignal)}")
    add(rearDistanceCm?.let { "뒤 ${it.roundToInt()} cm" } ?: "뒤 거리 미측정")
    add(signalLabel(distanceSignal))
    if (proximityAlert()) add("뒤가 가까워요")
}.joinToString(". ")

// Wheelbase ratio is illustrative, not a measured road-wheel angle. Preserve COVESA's positive-left sign.
internal fun wheelRotation(steeringDeg: Float?) = ((steeringDeg ?: 0f) / 15f).coerceIn(-38f, 38f)
internal fun distanceFraction(distanceCm: Float?) = ((distanceCm ?: 0f) / 250f).coerceIn(0f, 1f)

// lastSpoken can still contain the previous Done/Report sentence at the next attempt.
// Do not bring a prior result back onto the moving screen through free-form speech text.
private val scoreText = Regex("점수|감점|\\d+\\s*점")
internal fun maneuverText(text: String?): String? = text?.takeUnless { scoreText.containsMatchIn(it) }
