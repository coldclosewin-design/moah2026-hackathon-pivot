package com.moah.hackathon.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionSegmenterTest {
    private fun s(vararg pairs: Pair<Long, Float>) = pairs.map { Sample(it.first, it.second) }

    @Test
    fun `counts stop-move-stop cycles as segments`() {
        val m = MotionSegmenter.summarize(s(0L to 0f, 1000L to 3f, 2000L to 4f, 3000L to 0f, 4000L to 0f, 5000L to 2f, 6000L to 0f))!!
        assertEquals(2, m.movingSegments)
        assertEquals(6000L, m.totalMillis)
        assertEquals(3000L, m.movingMillis)   // 1000→3000 + 5000→6000
        assertEquals(3000L, m.idleMillis)
        assertEquals(4f, m.maxSpeedKmh)
        assertEquals(1000L, m.firstMoveMillis)
    }

    @Test
    fun `creeping below the threshold is not motion`() {
        val m = MotionSegmenter.summarize(s(0L to 0f, 500L to 0.8f, 1000L to 0.9f, 1500L to 0f))!!
        assertEquals(0, m.movingSegments)
        assertNull(m.firstMoveMillis)
    }

    @Test
    fun `still moving at the end counts up to the last sample`() {
        val m = MotionSegmenter.summarize(s(0L to 0f, 1000L to 5f, 3000L to 5f))!!
        assertEquals(1, m.movingSegments)
        assertEquals(2000L, m.movingMillis)
    }

    @Test
    fun `empty input is null`() {
        assertNull(MotionSegmenter.summarize(emptyList()))
    }
}
