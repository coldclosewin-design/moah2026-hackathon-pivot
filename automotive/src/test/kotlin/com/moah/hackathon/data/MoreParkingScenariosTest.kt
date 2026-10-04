package com.moah.hackathon.data

import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.scoring.ParkingRubric
import com.moah.hackathon.scoring.ParkingSpec
import com.moah.hackathon.scoring.ParkingVerdict
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.SignalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 평행·사선 주차 시나리오 4벌의 점수·판정 고정값(10/4). */
class MoreParkingScenariosTest {

    private data class Out(val skill: Int, val safety: Int, val verdict: ParkingVerdict)

    private fun replay(spec: ParkingSpec, s: Scenario): Out {
        val r = ParkingRecorder(SignalRegistry(spec.keys, simulated = true), spec.keys)
        s.steps.forEach { r.onDelta((it.atSeconds * 1000).toLong(), it.values) }
        val score = r.score(ParkingRubric(idealReversals = spec.idealReversals))!!
        val v = r.verdict(targetHeadingDeg = spec.targetHeadingDeg)!!
        println("${s.id}: skill=${score.skill} safety=${score.safety} segments=${score.metrics.motion.movingSegments} reversals=${score.metrics.steering?.reversals} heading=${v.headingErrorDeg} $v")
        return Out(score.skill, score.safety, v)
    }

    @Test fun `parallel good is aligned in one go with full score`() {
        val o = replay(ParkingSpec.PARALLEL, MoreParkingScenarios.parallelGood)
        assertEquals(ParkingVerdict.Entry.ONE_GO, o.verdict.entry)
        assertEquals(ParkingVerdict.Heading.ALIGNED, o.verdict.heading)
        assertEquals(ParkingVerdict.Finish.CLEAN, o.verdict.finish)
        assertEquals(ParkingVerdict.Safety.SAFE, o.verdict.safety)
        assertEquals(100, o.skill); assertEquals(100, o.safety)
    }

    @Test fun `parallel bad needs fixes and is unsafe`() {
        val o = replay(ParkingSpec.PARALLEL, MoreParkingScenarios.parallelBad)
        assertNotEquals(ParkingVerdict.Entry.ONE_GO, o.verdict.entry)
        assertEquals(ParkingVerdict.Safety.UNSAFE, o.verdict.safety)
        assertTrue(o.skill < 100 && o.safety < 100)
    }

    @Test fun `angle good is aligned in one go with full score`() {
        val o = replay(ParkingSpec.ANGLE, MoreParkingScenarios.angleGood)
        assertEquals(ParkingVerdict.Entry.ONE_GO, o.verdict.entry)
        assertEquals(ParkingVerdict.Heading.ALIGNED, o.verdict.heading)
        assertEquals(ParkingVerdict.Finish.CLEAN, o.verdict.finish)
        assertEquals(100, o.skill); assertEquals(100, o.safety)
    }

    @Test fun `angle bad over-rotates, fixes forward and is not aligned`() {
        val o = replay(ParkingSpec.ANGLE, MoreParkingScenarios.angleBad)
        assertNotEquals(ParkingVerdict.Entry.ONE_GO, o.verdict.entry)
        assertNotEquals(ParkingVerdict.Heading.ALIGNED, o.verdict.heading)
        assertEquals(ParkingVerdict.Safety.UNSAFE, o.verdict.safety)   // 근접 + 급정지 = 둘
        assertTrue(o.skill < 100)
    }
}
