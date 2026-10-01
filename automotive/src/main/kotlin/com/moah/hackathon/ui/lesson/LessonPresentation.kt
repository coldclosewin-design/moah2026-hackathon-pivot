package com.moah.hackathon.ui.lesson

import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.scoring.ParkingDelta
import com.moah.hackathon.scoring.ParkingMetrics
import com.moah.hackathon.scoring.HarshKind
import com.moah.hackathon.ports.withAndParticle
import com.moah.hackathon.ports.withObjectParticle
import com.moah.hackathon.vehicle.AvailabilityBadge
import com.moah.hackathon.vehicle.SignalAvailability
import mobis.vss.VssConstants as V
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun profileLine(profile: Profile): String = listOfNotNull(profile.name,
    profile.rustyYears?.let { "장롱 ${it}년차" }, profile.statement.goal?.let { "목표 $it" }).joinToString(" · ")

internal fun taskTypeLabel(type: TaskType): String = when (type) {
    TaskType.CHECKLIST -> "점검"
    TaskType.DRIVING -> "주행"
    TaskType.PARKING -> "주차"
    TaskType.KNOWLEDGE -> "지식"
}

internal fun categoryOrder(): List<TaskType> =
    listOf(TaskType.PARKING, TaskType.DRIVING, TaskType.CHECKLIST, TaskType.KNOWLEDGE)

internal fun selectionReason(task: Task, mode: LessonMode, suggestedTask: Task, suggestedMode: LessonMode, reason: String) =
    if (task.id == suggestedTask.id && mode == suggestedMode) reason else task.summary

internal fun supportedMode(task: Task, mode: LessonMode) =
    mode.takeIf(task::supports) ?: LessonMode.entries.first(task::supports)

internal fun setupProposal(type: TaskType) = when (type) {
    TaskType.CHECKLIST -> "시동 켜기 전,\n순서를 익혀볼까요?"
    TaskType.KNOWLEDGE -> "정차 중이니\n머리로 풀어 볼까요?"
    TaskType.PARKING -> "오늘은 가볍게,\n주차부터 해볼까요?"
    TaskType.DRIVING -> "오늘은 천천히,\n함께 달려 볼까요?"
}

internal fun briefingHeadline(watch: List<String>): String = when (watch.size) {
    0 -> "오늘의 연습을\n함께 준비할게요."
    1 -> "${watch.first().withObjectParticle()}\n볼게요."
    2, 3 -> "${watch.dropLast(1).joinToString(" ") { it.withAndParticle() }}\n${watch.last().withObjectParticle()} 볼게요."
    else -> "${watch.first()}부터 ${watch.last()}까지\n순서대로 볼게요."
}

internal fun parkingDetailLine(metrics: ParkingMetrics): String = buildList {
    add("조향 왕복 ${metrics.steering?.reversals ?: "미측정"}")
    add("기어 전환 ${metrics.gear?.reverseDriveShifts ?: "미측정"}")
    add("근접 ${metrics.proximity?.warnings ?: "미측정"}")
    add("급정지 ${metrics.harshEvents.count { it.kind == HarshKind.BRAKING }}")
    val acceleration = metrics.harshEvents.count { it.kind == HarshKind.ACCELERATION }
    if (acceleration > 0) add("급가속 $acceleration")
}.joinToString(" · ")

internal fun processLine(type: TaskType, metrics: ParkingMetrics): String =
    if (type == TaskType.CHECKLIST) buildList {
        metrics.preDrive.let { pre ->
            pre.beltOnMillis?.let { add("벨트 ${it / 1000}초") }
            pre.ignitionOnMillis?.let { add("시동 ${it / 1000}초") }
            pre.beltBeforeIgnition?.let { add(if (it) "벨트 먼저" else "시동 먼저") }
        }
        add("움직임 ${metrics.motion.movingSegments}회")
    }.joinToString(" · ") else metricLines(metrics).joinToString(" · ") { "${it.label} ${it.value}" }

internal fun taskDeltaLines(type: TaskType, delta: ParkingDelta?) =
    deltaLines(if (type == TaskType.CHECKLIST) delta?.copy(reversals = null, shifts = null) else delta)

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
// The current coach contract still prefixes the summary with "과제, 모드 N회.".
// Keep task/mode and explicit newlines; attempt counts belong to the record graphic and details.
internal fun driverReportSummary(summary: String) = summary.replace(
    Regex("(?m)^(.* 모드) \\d+회\\.(?=\\n|$)"), "$1.")
internal fun signalLabel(signal: SignalAvailability) = when (signal) {
    SignalAvailability.LIVE -> "실신호"
    SignalAvailability.SIMULATED -> "시뮬레이션"
    SignalAvailability.MISSING -> "미측정"
}

internal fun signalName(key: String): String = when (key) {
    V.VEHICLE_SPEED -> "속도"
    V.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE -> "조향각"
    V.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR -> "기어"
    SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM -> "뒤 거리"
    V.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING -> "근접 경고"
    V.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED -> "안전벨트"
    V.VEHICLE_LOWVOLTAGESYSTEMSTATE -> "시동"
    V.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN -> "운전석 도어"
    V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING -> "왼쪽 방향지시등"
    V.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING -> "오른쪽 방향지시등"
    V.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING -> "비상등"
    V.VEHICLE_CHASSIS_BRAKE_PEDALPOSITION -> "브레이크"
    else -> "기타 차량 신호"
}

internal fun ManeuverDisplayState.proximityAlert() = obstacleWarning || (rearDistanceCm?.let { it < 40f } == true)
internal fun ManeuverDisplayState.commonSignal(): SignalAvailability? {
    val signals = if (taskType == TaskType.CHECKLIST) listOf(doorSignal, beltSignal, gearSignal, brakeSignal,
        ignitionSignal, indicatorLeftSignal, indicatorRightSignal, hazardSignal)
        else listOf(steeringSignal, gearSignal, distanceSignal)
    return signals.first().takeIf { it != SignalAvailability.MISSING && signals.all { value -> value == it } }
}

internal fun collapsedSignalLabel(signal: SignalAvailability) =
    if (signal == SignalAvailability.SIMULATED) "시뮬레이션 신호" else signalLabel(signal)
internal fun ManeuverDisplayState.diagramDescription() = buildList {
    add("차량 도식, 뒤쪽이 화면 위")
    add("조향각 ${steeringDeg?.let { "${it.roundToInt()}도" } ?: "미측정"}, ${signalLabel(steeringSignal)}")
    add("기어 ${gear ?: "미측정"}, ${signalLabel(gearSignal)}")
    if (gear == "R") add("후진 중")
    if (steeringDeg != null) add("조향 방향 호, 보조선")
    add(rearDistanceCm?.let { "뒤 ${it.roundToInt()} cm" } ?: "뒤 거리 미측정")
    add(signalLabel(distanceSignal))
    if (proximityAlert()) add("뒤가 가까워요")
}.joinToString(". ")

// Wheelbase ratio is illustrative, not a measured road-wheel angle. Preserve COVESA's positive-left sign.
internal fun wheelRotation(steeringDeg: Float?) = ((steeringDeg ?: 0f) / 15f).coerceIn(-38f, 38f)
/** Driver-side left/right angles; the inner front wheel turns further toward the shared centre. */
internal fun wheelAngles(steeringDeg: Float?): Pair<Float, Float> {
    val outer = wheelRotation(steeringDeg)
    val inner = (outer * 1.25f).coerceIn(-45f, 45f)
    return if (outer < 0f) outer to inner else inner to outer
}
// Positive means left steering: with the nose down, its arc bends to screen-right.
internal fun steeringArcBend(steeringDeg: Float?) = steeringDeg?.let { (it / 450f).coerceIn(-1f, 1f) }
internal fun steeringTurnsLabel(deg: Float?): String? {
    val magnitude = abs(deg ?: return null)
    if (magnitude < 45f) return "중립"
    val direction = if (deg > 0f) "왼쪽" else "오른쪽"
    val amount = when {
        magnitude < 200f -> "조금"
        magnitude < 380f -> "반 바퀴"
        magnitude < 560f -> "한 바퀴"
        magnitude < 740f -> "한 바퀴 반"
        else -> "끝까지"
    }
    return "${direction}으로 $amount"
}
internal fun distanceFraction(distanceCm: Float?) = ((distanceCm ?: 0f) / 250f).coerceIn(0f, 1f)

// lastSpoken can still contain the previous Done/Report sentence at the next attempt.
// Do not bring a prior result back onto the moving screen through free-form speech text.
private val scoreText = Regex("점수|감점|\\d+\\s*점|이동\\s*\\d+회|\\d+\\s*초")
internal fun maneuverText(text: String?): String? = text?.takeUnless { scoreText.containsMatchIn(it) }
