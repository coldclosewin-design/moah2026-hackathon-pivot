package com.moah.hackathon.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VssGearTest {
    @Test
    fun `COVESA numeric encoding folds to four gears`() {
        assertEquals(Gear.PARK, "126".toVssGear())
        assertEquals(Gear.DRIVE, "127".toVssGear())
        assertEquals(Gear.DRIVE, "3".toVssGear())
        assertEquals(Gear.NEUTRAL, "0".toVssGear())
        assertEquals(Gear.REVERSE, "-1".toVssGear())
        assertEquals(Gear.REVERSE, "-2.0".toVssGear())
    }

    @Test
    fun `letters are accepted leniently and garbage is null`() {
        assertEquals(Gear.PARK, " p ".toVssGear())
        assertEquals(Gear.REVERSE, "Reverse".toVssGear())
        assertNull("??".toVssGear())
        assertNull(null.toVssGear())
    }

    @Test
    fun `ignition state maps ON and START to true`() {
        assertEquals(true, "ON".toVssIgnitionOn())
        assertEquals(true, "start".toVssIgnitionOn())
        assertEquals(false, "ACC".toVssIgnitionOn())
        assertEquals(false, "OFF".toVssIgnitionOn())
        assertEquals(true, "1".toVssIgnitionOn())
        assertNull("maybe".toVssIgnitionOn())
    }
}
