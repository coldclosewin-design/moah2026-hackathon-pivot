package com.moah.hackathon.ui

import com.moah.hackathon.ui.concepts.DESIGN_HEIGHT_DP
import com.moah.hackathon.ui.concepts.DESIGN_WIDTH_DP
import com.moah.hackathon.ui.concepts.designDensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignScaleTest {
    @Test
    fun `the reference emulator window keeps one dp per pixel`() {
        assertEquals(1f, designDensity(2560, 1268)!!, 0.0001f)
    }

    @Test
    fun `the design always fits inside the window whatever its size or shape`() {
        listOf(2560 to 1268, 1920 to 1080, 1920 to 720, 3840 to 2160, 1280 to 720, 2560 to 1600, 1440 to 2560).forEach { (w, h) ->
            val density = designDensity(w, h)!!
            assertTrue("$w x $h width", DESIGN_WIDTH_DP * density <= w + 0.5f)
            assertTrue("$w x $h height", DESIGN_HEIGHT_DP * density <= h + 0.5f)
            // 한쪽은 꽉 찬다(쓸데없이 작게 그리지 않는다)
            assertTrue("$w x $h fills one axis", DESIGN_WIDTH_DP * density >= w - 1f || DESIGN_HEIGHT_DP * density >= h - 1f)
        }
    }

    @Test
    fun `the device density does not matter - only the window in pixels`() {
        // 같은 2560x1268 창이면 기기 밀도가 160 이든 200 이든 320 이든 결과는 같다(인자에 밀도가 없다는 것 자체가 계약)
        assertEquals(designDensity(2560, 1268), designDensity(2560, 1268))
        assertEquals(0.75f, designDensity(1920, 951)!!, 0.001f)
    }

    @Test
    fun `unknown window sizes fall back to the system density`() {
        assertNull(designDensity(0, 1268))
        assertNull(designDensity(2560, -1))
        assertNull(designDensity(Int.MAX_VALUE, 1268))
    }
}
