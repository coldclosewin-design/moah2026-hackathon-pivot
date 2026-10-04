package com.moah.hackathon.ui.lesson

import com.moah.hackathon.feature.lesson.AttemptRecord
import com.moah.hackathon.scoring.CourseResult
import com.moah.hackathon.scoring.PathPoint
import com.moah.hackathon.scoring.TrackPoint
import com.moah.hackathon.scoring.ZoneResult
import com.moah.hackathon.vehicle.SignalAvailability
import com.moah.hackathon.vehicle.SimOnlySignals

/** Only track sources belong in the map's provenance; absent keys remain unmeasured. */
internal fun trackSource(availability: Map<String, SignalAvailability>): String =
    SimOnlySignals.TRACK_KEYS.map { availability[it] ?: SignalAvailability.MISSING }.distinct()
        .sortedBy { it.ordinal }.joinToString(" · ", transform = ::signalLabel)

internal fun courseVerdict(result: CourseResult): String = when {
    !result.positionMeasured -> "시험장 위치 미측정"
    result.disqualified -> "실격 사유 있음"
    result.passed == true -> "합격"
    result.passed == false -> "불합격"
    result.deductions.isEmpty() -> "놓친 것 없음"
    else -> "놓친 것"
}

internal fun courseZoneStatus(result: CourseResult, zone: ZoneResult): String = when {
    !result.positionMeasured -> "미측정"
    !zone.visited || zone.deductions.isNotEmpty() -> "놓침"
    zone.unmeasured.isNotEmpty() -> "미측정"
    else -> "지남"
}

internal fun bestCourseAttempt(attempts: List<AttemptRecord>): AttemptRecord? = attempts
    .filter { it.course != null }.maxWithOrNull(compareBy<AttemptRecord> { it.course!!.positionMeasured }
        .thenBy { !it.course!!.disqualified }.thenBy { it.course!!.score })

/** Share the parking replay's measured-time interpolation, including wraparound headings. */
internal fun courseTrailThroughTime(trail: List<TrackPoint>, millis: Long): List<TrackPoint> =
    pathThroughTime(trail.map { PathPoint(it.tMillis, it.x, it.y, it.headingDeg, false) }, millis)
        .map { TrackPoint(it.tMillis, it.x, it.y, it.headingDeg) }
