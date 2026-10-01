package com.moah.hackathon.scoring

import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 네 가지 판정(docs/design/09) — 점수와 같은 지표에서 파생, 운전자의 질문에 답한다. */
class ParkingVerdictTest {
    private fun motion(segments: Int) = MotionSummary(segments, 40_000L, 20_000L, 4f, 1000L)
    private fun metrics(
        segments: Int = 2, shifts: Int = 0, endedInPark: Boolean = true, harsh: Int = 0, warnings: Int = 0, belt: Boolean? = true,
        steering: SteeringSummary? = SteeringSummary(1, 450f), gear: Boolean = true,
    ) = ParkingMetrics(
        motion(segments), List(harsh) { HarshEvent(it * 2000L, HarshKind.BRAKING, -4f) }, steering,
        if (gear) GearSummary(shifts, endedInPark = endedInPark, everReversed = true) else null,
        ProximitySummary(warnings, 80f), PreDriveSummary(belt, true),
    )
    /** 시작 0° → 끝 [endDeg] 인 두 점짜리 궤적. */
    private fun path(endDeg: Float) = listOf(PathPoint(0L, 0f, 0f, 0f, false), PathPoint(1000L, 0f, 1f, endDeg, true))

    @Test
    fun `entry - one go needs two segments and no shift, one fix allows up to four and two shifts, otherwise many`() {
        assertEquals(ParkingVerdict.Entry.ONE_GO, ParkingVerdicts.of(metrics(), path(90f), 0f, true).entry)
        assertEquals(ParkingVerdict.Entry.ONE_FIX, ParkingVerdicts.of(metrics(segments = 2, shifts = 1), path(90f), 0f, true).entry)
        assertEquals(ParkingVerdict.Entry.ONE_FIX, ParkingVerdicts.of(metrics(segments = 4, shifts = 2), path(90f), 0f, true).entry)
        assertEquals(ParkingVerdict.Entry.MANY, ParkingVerdicts.of(metrics(segments = 5, shifts = 2), path(90f), 0f, true).entry)
        assertEquals(ParkingVerdict.Entry.MANY, ParkingVerdicts.of(metrics(segments = 3, shifts = 3), path(90f), 0f, true).entry)
        assertEquals(ParkingVerdict.Entry.ONE_GO, ParkingVerdicts.of(metrics(gear = false), path(90f), 0f, true).entry)   // 기어 없으면 구간만
    }

    @Test
    fun `heading - estimated from the path end, unknown without steering, sign does not matter`() {
        val v = ParkingVerdicts.of(metrics(), path(-85f), 0f, true)
        assertEquals(ParkingVerdict.Heading.ALIGNED, v.heading); assertEquals(5f, v.headingErrorDeg!!, 0.01f)
        assertEquals(ParkingVerdict.Heading.SLIGHT, ParkingVerdicts.of(metrics(), path(70f), 0f, true).heading)
        assertEquals(ParkingVerdict.Heading.OFF, ParkingVerdicts.of(metrics(), path(40f), 0f, true).heading)
        val unknown = ParkingVerdicts.of(metrics(steering = null), path(0f), null, true)
        assertEquals(ParkingVerdict.Heading.UNKNOWN, unknown.heading); assertNull(unknown.headingErrorDeg)
        assertEquals(ParkingVerdict.Heading.UNKNOWN, ParkingVerdicts.of(metrics(), emptyList(), 0f, true).heading)
    }

    @Test
    fun `finish - clean needs stopped, park and a neutral wheel, missing signals are not asked`() {
        assertEquals(ParkingVerdict.Finish.CLEAN, ParkingVerdicts.of(metrics(), path(90f), 12f, true).finish)
        assertEquals(ParkingVerdict.Finish.LOOSE, ParkingVerdicts.of(metrics(), path(90f), 0f, false).finish)
        assertEquals(ParkingVerdict.Finish.LOOSE, ParkingVerdicts.of(metrics(endedInPark = false), path(90f), 0f, true).finish)
        assertEquals(ParkingVerdict.Finish.LOOSE, ParkingVerdicts.of(metrics(), path(90f), 200f, true).finish)
        assertEquals(ParkingVerdict.Finish.CLEAN, ParkingVerdicts.of(metrics(gear = false), path(90f), null, true).finish)
    }

    @Test
    fun `safety - one incident is watch, two or a missing belt is unsafe, unmeasured items are not counted`() {
        assertEquals(ParkingVerdict.Safety.SAFE, ParkingVerdicts.of(metrics(), path(90f), 0f, true).safety)
        assertEquals(ParkingVerdict.Safety.WATCH, ParkingVerdicts.of(metrics(harsh = 1), path(90f), 0f, true).safety)
        assertEquals(ParkingVerdict.Safety.UNSAFE, ParkingVerdicts.of(metrics(harsh = 1, warnings = 1), path(90f), 0f, true).safety)
        assertEquals(ParkingVerdict.Safety.UNSAFE, ParkingVerdicts.of(metrics(belt = false), path(90f), 0f, true).safety)
        assertEquals(ParkingVerdict.Safety.SAFE, ParkingVerdicts.of(metrics(belt = null), path(90f), 0f, true).safety)
    }

    private fun recorded(scenario: Scenario): ParkingVerdict {
        val r = ParkingRecorder(SignalRegistry(ParkingRecorder.KEYS, simulated = true))
        for (s in scenario.steps) r.onDelta((s.atSeconds * 1000).toLong(), s.values)
        return r.verdict()!!
    }

    @Test
    fun `scenarios - good parking is one go clean and safe, bad parking is one fix and unsafe - heading is estimated for both`() {
        val good = recorded(ParkingScenarios.good)
        assertEquals(good.toString(), ParkingVerdict.Entry.ONE_GO, good.entry)
        assertEquals(good.toString(), ParkingVerdict.Finish.CLEAN, good.finish)
        assertEquals(good.toString(), ParkingVerdict.Safety.SAFE, good.safety)
        assertNotNull(good.toString(), good.headingErrorDeg)
        assertTrue(good.toString(), good.heading != ParkingVerdict.Heading.UNKNOWN)
        // 10/2 실측: 잘한 주차 시나리오의 dead-reckoning 끝 헤딩은 약 41°(목표 90° 대비 49° → OFF), 못한 주차는 약 71°(SLIGHT).
        // 판정 규칙이 아니라 Fake 시나리오의 기하(조향각·시간)가 직각 주차를 다 돌지 않는 것 — 시드 내용(Codex, 라운드 12)에서 맞춘다.

        val bad = recorded(ParkingScenarios.bad)
        assertEquals(bad.toString(), ParkingVerdict.Entry.ONE_FIX, bad.entry)              // 4구간 · 전환 2
        assertEquals(bad.toString(), ParkingVerdict.Safety.UNSAFE, bad.safety)             // 벨트 없음 + 급제동 + 근접
        assertNotNull(bad.toString(), bad.headingErrorDeg)
    }
}
