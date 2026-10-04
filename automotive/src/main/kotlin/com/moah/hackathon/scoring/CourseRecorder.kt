package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.SimOnlySignals
import com.moah.hackathon.vehicle.TrackSignal
import com.moah.hackathon.vehicle.toTrackSignal
import com.moah.hackathon.vehicle.toVssBoolean
import com.moah.hackathon.vehicle.toVssFloat
import com.moah.hackathon.vehicle.toVssGear
import com.moah.hackathon.vehicle.toWiperOn
import mobis.vss.VssConstants

/** 감점 한 건. [reason] 은 리포트용 명사구("검지선 접촉"), [say] 는 코칭 문장 — 둘 다 숫자 없음. [points] 는 리포트 "자세히 보기"·진단서에만. */
data class Deduction(
    val tMillis: Long,
    val zoneId: String?,
    val zoneTitle: String,
    val rule: String,
    val reason: String,
    val say: String,
    val points: Int,
    val disqualify: Boolean,
    /** 감점 순간의 위치(지도 표시용). 위치 미측정이면 null. */
    val at: Vec2?,
)

/** 차가 지나간 자리 — Done·리포트 지도의 궤적. 시뮬레이션 위치 신호에서 온 값이라 화면은 "시험장 위치(시뮬레이션)" 로 표시. */
data class TrackPoint(val tMillis: Long, val x: Float, val y: Float, val headingDeg: Float)

/** [CourseRecorder.onDelta] 한 번의 결과 — 상태기계가 발화를 정한다. */
data class CourseUpdate(
    val entered: CourseZone? = null,
    val exited: CourseZone? = null,
    val deductions: List<Deduction> = emptyList(),
)

/**
 * 주행 중 화면이 받는 진행 상황 — **점수·감점이 없다**(절대 규칙 10). 지도 위 차·지금 구간·다음 구간·신호등·돌발 경보만.
 */
data class CourseProgress(
    val pose: Pose?,
    val currentZoneId: String?,
    val nextZoneId: String?,
    val passedZoneIds: List<String>,
    val signal: TrackSignal?,
    val emergency: Boolean,
    val positionMeasured: Boolean,
)

data class ZoneResult(
    val zoneId: String,
    val title: String,
    val kind: ZoneKind,
    val visited: Boolean,
    val deductions: List<Deduction>,
    /** 신호가 없어 확인하지 못한 규칙 이름. 감점하지 않았다. */
    val unmeasured: List<String>,
) {
    val clean: Boolean get() = visited && deductions.isEmpty()
}

data class CourseResult(
    val courseId: String,
    val title: String,
    /** 100 에서 감점 합을 뺀 값(0 아래로 안 간다). */
    val score: Int,
    val passScore: Int?,
    val disqualified: Boolean,
    val deductions: List<Deduction>,
    val zones: List<ZoneResult>,
    val trail: List<TrackPoint>,
    val durationMillis: Long,
    /** 위치 신호가 한 번도 오지 않았으면 false — 구간 판정이 전부 "미측정". */
    val positionMeasured: Boolean,
) {
    /** 합격 여부. 연습 코스(합격선 없음)·위치 미측정(구간을 판정하지 못함)은 null. */
    val passed: Boolean? get() = if (!positionMeasured) null else passScore?.let { !disqualified && score >= it }
    val lost: Int get() = deductions.sumOf { it.points }
    val visitedZones: Int get() = zones.count { it.visited }
}

/**
 * 코스 과제 한 회차의 기록기 — `Track.*`(시뮬레이션) + 차량 신호 delta 를 받아 지금 구간을 추적하고, 구간 규칙 위반을 [Deduction] 으로 낸다.
 * 상태기계가 `onDelta(t, delta)` 만 부르면 된다. 안드로이드·코루틴 의존 없음.
 *
 * 구간 판정: 코스 순서대로 **앞으로만** 나아간다(지나간 구간에 다시 들어가도 재진입이 아니다 — 주차 후 빠져나오는 길이 겹쳐도 안전).
 * 규칙이 쓰는 신호가 한 번도 오지 않았으면 감점하지 않고 [ZoneResult.unmeasured] 에 남긴다(MISSING 견디기, 절대 규칙 4).
 */
class CourseRecorder(val course: TrackCourse) {

    private class ZoneRun(val zone: CourseZone, val index: Int) {
        var enteredAt: Long? = null
        var exitedAt: Long? = null
        var stoppedAt: Long? = null
        var stopProgress: Float? = null
        var minProgressAfterStop: Float? = null
        var departedAt: Long? = null
        var indicatorLeftSeen = false
        var indicatorRightSeen = false
        var maxSpeed = 0f
        var stoppedInBay = false
        val devicesSeen = HashSet<Device>()
        var emergencyAt: Long? = null
        var emergencyStopped = false
        var emergencyHazard = false
        val fired = HashSet<String>()
        val deductions = ArrayList<Deduction>()
    }

    private val runs = course.zones.mapIndexed { i, z -> ZoneRun(z, i) }
    private var current: ZoneRun? = null
    private var nextIdx = 0
    private val outside = ArrayList<Deduction>()
    private val trail = ArrayList<TrackPoint>()
    private val seen = HashSet<String>()

    private var x: Float? = null
    private var y: Float? = null
    private var heading: Float = course.start.headingDeg
    private var speed = 0f
    private var gear: Gear? = null
    private var left = false
    private var right = false
    private var hazard = false
    private var headlight = false
    private var wiper = false
    private var signal: TrackSignal? = null
    private var emergency = false
    private var lineContact = false
    private var lastLeftOn: Long? = null
    private var lastRightOn: Long? = null
    private var lastMillis = 0L

    val position: Vec2? get() = if (x != null && y != null) Vec2(x!!, y!!) else null

    fun onDelta(tMillis: Long, delta: Map<String, String>): CourseUpdate {
        if (tMillis > lastMillis) lastMillis = tMillis
        val prevEmergency = emergency
        val prevContact = lineContact
        for (k in delta.keys) if (k in KEYS) seen += k
        delta[VssConstants.VEHICLE_SPEED]?.toVssFloat()?.let { speed = kotlin.math.abs(it) }
        delta[VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR]?.toVssGear()?.let { gear = it }
        delta[VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING]?.toVssBoolean()?.let { left = it }
        delta[VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING]?.toVssBoolean()?.let { right = it }
        delta[VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING]?.toVssBoolean()?.let { hazard = it }
        delta[SimOnlySignals.LIGHTS_BEAM_LOW_ISON]?.toVssBoolean()?.let { headlight = it }
        delta[SimOnlySignals.WIPER_FRONT_MODE]?.toWiperOn()?.let { wiper = it }
        delta[SimOnlySignals.TRACK_POSITION_X_M]?.toVssFloat()?.let { x = it }
        delta[SimOnlySignals.TRACK_POSITION_Y_M]?.toVssFloat()?.let { y = it }
        delta[SimOnlySignals.TRACK_HEADING_DEG]?.toVssFloat()?.let { heading = it }
        delta[SimOnlySignals.TRACK_SIGNAL_STATE]?.let { v -> v.toTrackSignal()?.let { signal = it } }
        delta[SimOnlySignals.TRACK_EVENT_EMERGENCY]?.toVssBoolean()?.let { emergency = it }
        delta[SimOnlySignals.TRACK_LINE_CONTACT]?.toVssBoolean()?.let { lineContact = it }
        if (left) lastLeftOn = tMillis
        if (right) lastRightOn = tMillis

        val p = position
        if (p != null) appendTrail(tMillis, p)

        var entered: CourseZone? = null
        var exited: CourseZone? = null
        val fresh = ArrayList<Deduction>()

        // 구간 이동
        if (p != null) {
            val located = locate(p)
            if (located !== current) {
                current?.let { run ->
                    run.exitedAt = tMillis
                    val d = exitChecks(run, tMillis)
                    d.forEach { run.fired += it.rule }
                    run.deductions += d; fresh += d
                    exited = run.zone
                    nextIdx = maxOf(nextIdx, run.index + 1)
                }
                current = located
                located?.let { run ->
                    run.enteredAt = tMillis
                    nextIdx = maxOf(nextIdx, run.index)
                    run.indicatorLeftSeen = lastLeftOn?.let { tMillis - it <= leadMillis(run) } ?: false
                    run.indicatorRightSeen = lastRightOn?.let { tMillis - it <= leadMillis(run) } ?: false
                    entered = run.zone
                    for (rule in run.zone.rules) if (rule is ZoneRule.NoEntryOnRed && signal == TrackSignal.RED) {
                        fresh += fire(run, rule, tMillis, "신호 위반", "빨간 신호에 들어갔어요. 신호가 바뀔 때까지 정지선 앞에서 기다려요.")
                    }
                }
            }
        }

        // 구간 안 연속 규칙
        current?.let { run -> fresh += during(run, tMillis, prevEmergency, prevContact) }
            ?: run {
                if (lineContact && !prevContact && course.laneContactPoints > 0) {
                    val d = Deduction(tMillis, null, ZoneKind.LANE.label, "lane", "검지선 접촉", "선에 닿았어요. 차로 가운데로 가요.",
                        course.laneContactPoints, false, position)
                    outside += d; fresh += d
                }
            }
        return CourseUpdate(entered, exited, fresh)
    }

    private fun leadMillis(run: ZoneRun): Long =
        ((run.zone.rules.filterIsInstance<ZoneRule.Indicator>().maxOfOrNull { it.leadSeconds } ?: 0f) * 1000).toLong()

    private fun locate(p: Vec2): ZoneRun? {
        current?.let { if (p in it.zone.area) return it }
        for (i in nextIdx until runs.size) if (p in runs[i].zone.area) return runs[i]
        return null
    }

    private fun progressOf(run: ZoneRun, p: Vec2): Float = p.dot(Vec2.ofHeading(run.zone.directionDeg))

    private fun during(run: ZoneRun, t: Long, prevEmergency: Boolean, prevContact: Boolean): List<Deduction> {
        val out = ArrayList<Deduction>()
        val p = position
        val stopped = speed < STOP_KMH
        if (speed > run.maxSpeed) run.maxSpeed = speed
        if (left) run.indicatorLeftSeen = true
        if (right) run.indicatorRightSeen = true
        if (headlight) run.devicesSeen += Device.HEADLIGHT
        if (wiper) run.devicesSeen += Device.WIPER
        if (left) run.devicesSeen += Device.LEFT_INDICATOR
        if (right) run.devicesSeen += Device.RIGHT_INDICATOR
        if (gear == Gear.DRIVE) run.devicesSeen += Device.DRIVE_GEAR

        if (stopped && run.stoppedAt == null) {
            run.stoppedAt = t
            run.stopProgress = p?.let { progressOf(run, it) }
            run.minProgressAfterStop = run.stopProgress
        }
        if (run.stoppedAt != null && run.departedAt == null && !stopped) run.departedAt = t
        if (p != null && run.stopProgress != null) {
            val prog = progressOf(run, p)
            if (prog < (run.minProgressAfterStop ?: prog)) run.minProgressAfterStop = prog
        }
        if (stopped && p != null) {
            for (rule in run.zone.rules) if (rule is ZoneRule.StopInsideBay && p in rule.bay) run.stoppedInBay = true
        }
        if (emergency && !prevEmergency) { run.emergencyAt = t; run.emergencyStopped = false; run.emergencyHazard = false }
        if (run.emergencyAt != null) {
            if (stopped) run.emergencyStopped = true
            if (hazard) run.emergencyHazard = true
        }

        for (rule in run.zone.rules) {
            when (rule) {
                is ZoneRule.NoRollback -> {
                    val back = (run.stopProgress ?: continue) - (run.minProgressAfterStop ?: continue)
                    if (back > rule.maxM && rule.label !in run.fired)
                        out += fire(run, rule, t, "뒤로 밀림", "뒤로 밀렸어요. 브레이크를 떼기 전에 가속 페달을 살짝 밟아 두세요.")
                }
                is ZoneRule.DepartWithin -> {
                    val at = run.stoppedAt ?: continue
                    val waited = (run.departedAt ?: t) - at
                    if (waited > rule.seconds * 1000L && rule.label !in run.fired)
                        out += fire(run, rule, t, "출발 지연", "출발이 늦었어요. 멈춘 뒤 바로 출발 준비를 해요.")
                }
                is ZoneRule.MaxSpeed -> if (speed > rule.kmh && rule.label !in run.fired)
                    out += fire(run, rule, t, "속도 초과", "속도가 빨라요. 천천히 가요.")
                is ZoneRule.StopOnEmergency -> {
                    val at = run.emergencyAt ?: continue
                    if (!run.emergencyStopped && t - at > (rule.withinSeconds * 1000).toLong() && rule.label !in run.fired)
                        out += fire(run, rule, t, "돌발 정지 지연", "돌발 상황에서는 바로 브레이크를 밟아 멈춰요.")
                }
                is ZoneRule.HazardOnEmergency -> {
                    // 경보가 꺼지는 순간 판정 — 그때까지 비상등을 한 번도 안 켰으면
                    if (run.emergencyAt != null && prevEmergency && !emergency && !run.emergencyHazard && rule.label !in run.fired && measured(HAZARD_KEY))
                        out += fire(run, rule, t, "비상등 미점등", "돌발 상황에서는 멈춘 뒤 비상등을 켜요.")
                }
                is ZoneRule.TimeLimit -> {
                    val at = run.enteredAt ?: continue
                    if (t - at > rule.seconds * 1000L && rule.label !in run.fired)
                        out += fire(run, rule, t, "시간 초과", "시간이 길어졌어요. 다음엔 순서를 미리 떠올려 봐요.")
                }
                is ZoneRule.NoLineContact -> if (lineContact && !prevContact && measured(SimOnlySignals.TRACK_LINE_CONTACT))
                    out += fire(run, rule, t, "검지선 접촉", "선에 닿았어요. 핸들을 조금 늦게 돌려 봐요.", repeatable = true)
                else -> {}
            }
        }
        // 선 규칙이 없는 구간에서의 접촉 = 차로 준수 위반
        if (run.zone.rules.none { it is ZoneRule.NoLineContact } && lineContact && !prevContact && course.laneContactPoints > 0) {
            val d = Deduction(t, run.zone.id, run.zone.title, "lane", "검지선 접촉", "선에 닿았어요. 차로 가운데로 가요.", course.laneContactPoints, false, position)
            run.deductions += d; out += d
        }
        return out
    }

    /** 구간을 나갈 때(또는 회차 종료 때) 판정하는 규칙. 상태를 바꾸지 않는다 — [result] 가 열린 구간에도 쓴다. */
    private fun exitChecks(run: ZoneRun, t: Long): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (rule in run.zone.rules) {
            if (rule.label in run.fired) continue
            when (rule) {
                is ZoneRule.StopInside -> if (run.stoppedAt == null)
                    out += make(run, rule, t, "${run.zone.title} 미정지", "${run.zone.title}에서 멈추지 않고 지나갔어요.")
                is ZoneRule.Indicator -> {
                    val key = if (rule.side == Side.LEFT) LEFT_KEY else RIGHT_KEY
                    val ok = if (rule.side == Side.LEFT) run.indicatorLeftSeen else run.indicatorRightSeen
                    if (!ok && measured(key)) out += make(run, rule, t, "방향지시등 미점등", "${rule.side.label} 방향지시등을 켜지 않았어요. 돌기 전에 먼저 켜요.")
                }
                is ZoneRule.ReachSpeed -> if (run.maxSpeed < rule.kmh)
                    out += make(run, rule, t, "가속 부족", "가속 구간에서는 속도를 더 올려요.")
                is ZoneRule.StopInsideBay -> if (!run.stoppedInBay && position != null)
                    out += make(run, rule, t, "주차 칸 밖 정지", "칸 안에 다 들어가기 전에 멈췄어요.")
                is ZoneRule.Devices -> for (d in rule.items) {
                    if (d !in run.devicesSeen && measured(deviceKey(d)))
                        out += make(run, rule, t, "${d.label} 미조작", "${d.label} 조작을 놓쳤어요.", ruleKey = "${rule.label}:${d.name}")
                }
                is ZoneRule.HazardOnEmergency -> if (run.emergencyAt != null && !run.emergencyHazard && measured(HAZARD_KEY))
                    out += make(run, rule, t, "비상등 미점등", "돌발 상황에서는 멈춘 뒤 비상등을 켜요.")
                else -> {}
            }
        }
        return out
    }

    private fun measured(key: String): Boolean = key in seen

    private fun unmeasured(run: ZoneRun): List<String> = run.zone.rules.mapNotNull { rule ->
        val keys = when (rule) {
            is ZoneRule.Indicator -> listOf(if (rule.side == Side.LEFT) LEFT_KEY else RIGHT_KEY)
            is ZoneRule.NoEntryOnRed -> listOf(SimOnlySignals.TRACK_SIGNAL_STATE)
            is ZoneRule.StopOnEmergency -> listOf(SimOnlySignals.TRACK_EVENT_EMERGENCY)
            is ZoneRule.HazardOnEmergency -> listOf(SimOnlySignals.TRACK_EVENT_EMERGENCY, HAZARD_KEY)
            is ZoneRule.NoLineContact -> listOf(SimOnlySignals.TRACK_LINE_CONTACT)
            is ZoneRule.Devices -> rule.items.map { deviceKey(it) }
            else -> emptyList()
        }
        rule.label.takeIf { keys.any { !measured(it) } }
    }

    private fun deviceKey(d: Device): String = when (d) {
        Device.HEADLIGHT -> SimOnlySignals.LIGHTS_BEAM_LOW_ISON
        Device.WIPER -> SimOnlySignals.WIPER_FRONT_MODE
        Device.LEFT_INDICATOR -> LEFT_KEY
        Device.RIGHT_INDICATOR -> RIGHT_KEY
        Device.DRIVE_GEAR -> VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR
    }

    private fun make(run: ZoneRun, rule: ZoneRule, t: Long, reason: String, say: String, ruleKey: String = rule.label) =
        Deduction(t, run.zone.id, run.zone.title, ruleKey, reason, say, rule.points, rule.disqualify, position)

    private fun fire(run: ZoneRun, rule: ZoneRule, t: Long, reason: String, say: String, repeatable: Boolean = false): Deduction {
        val d = make(run, rule, t, reason, say)
        if (!repeatable) run.fired += rule.label
        run.deductions += d
        return d
    }

    private fun appendTrail(t: Long, p: Vec2) {
        val last = trail.lastOrNull()
        if (last == null || Vec2(last.x, last.y).distanceTo(p) >= TRAIL_STEP_M || kotlin.math.abs(last.headingDeg - heading) >= 5f)
            trail += TrackPoint(t, p.x, p.y, heading)
    }

    /** 주행 화면용 — 점수 없음. */
    fun progress(): CourseProgress {
        val passed = runs.filter { it.exitedAt != null }.map { it.zone.id }
        val next = runs.drop(current?.let { it.index + 1 } ?: nextIdx).firstOrNull { it.enteredAt == null }?.zone?.id
        return CourseProgress(
            pose = position?.let { Pose(it, heading) },
            currentZoneId = current?.zone?.id,
            nextZoneId = next,
            passedZoneIds = passed,
            signal = signal.takeIf { measured(SimOnlySignals.TRACK_SIGNAL_STATE) },
            emergency = emergency,
            positionMeasured = position != null,
        )
    }

    /** 회차 결과. 지금 머무는 구간은 "나갔다고 치고" 판정을 더한다(상태는 바꾸지 않는다). */
    fun result(untilMillis: Long? = null): CourseResult {
        val t = untilMillis ?: lastMillis
        val zones = runs.map { run ->
            val pending = if (run === current) exitChecks(run, t) else emptyList()
            ZoneResult(run.zone.id, run.zone.title, run.zone.kind, visited = run.enteredAt != null,
                deductions = run.deductions + pending, unmeasured = unmeasured(run))
        }
        val all = (zones.flatMap { it.deductions } + outside).sortedBy { it.tMillis }
        return CourseResult(
            courseId = course.id, title = course.title,
            score = (100 - all.sumOf { it.points }).coerceIn(0, 100),
            passScore = course.passScore,
            disqualified = all.any { it.disqualify },
            deductions = all, zones = zones, trail = trail.toList(), durationMillis = t,
            positionMeasured = trail.isNotEmpty(),
        )
    }

    companion object {
        const val STOP_KMH = 1f
        const val TRAIL_STEP_M = 0.5f
        private const val LEFT_KEY = VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_LEFT_ISSIGNALING
        private const val RIGHT_KEY = VssConstants.VEHICLE_BODY_LIGHTS_DIRECTIONINDICATOR_RIGHT_ISSIGNALING
        private const val HAZARD_KEY = VssConstants.VEHICLE_BODY_LIGHTS_HAZARD_ISSIGNALING

        /** 코스 과제가 구독하는 키 — 배지 분모. 차량 9 + 장치 2 + 시험장 6 = 17. */
        val KEYS: Set<String> = setOf(
            VssConstants.VEHICLE_SPEED,
            VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE,
            VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR,
            VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED,
            VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE,
            VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN,
            LEFT_KEY, RIGHT_KEY, HAZARD_KEY,
        ) + SimOnlySignals.DEVICE_KEYS + SimOnlySignals.TRACK_KEYS
    }
}
