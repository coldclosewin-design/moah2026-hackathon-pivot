package com.moah.hackathon.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalRegistryTest {
    private val keys = setOf("Vehicle.Speed", "Vehicle.Chassis.SteeringWheel.Angle", "Vehicle.Cabin.Door.Row1.DriverSide.IsOpen")

    @Test
    fun `keys never seen are MISSING and excluded from scoring`() {
        val r = SignalRegistry(keys, simulated = false)
        r.onValues(mapOf("Vehicle.Speed" to "12.0"))
        assertEquals(SignalAvailability.LIVE, r.availability("Vehicle.Speed"))
        assertEquals(SignalAvailability.MISSING, r.availability("Vehicle.Chassis.SteeringWheel.Angle"))
        assertFalse(r.isAvailable("Vehicle.Chassis.SteeringWheel.Angle"))
        assertEquals(listOf("Vehicle.Chassis.SteeringWheel.Angle", "Vehicle.Cabin.Door.Row1.DriverSide.IsOpen"), r.missingKeys())
        assertEquals(AvailabilityBadge(live = 1, simulated = 0, missing = 2), r.badge())
    }

    @Test
    fun `fake port marks everything it produced as SIMULATED`() {
        val r = SignalRegistry(keys, simulated = true)
        r.onValues(mapOf("Vehicle.Speed" to "0.0", "Vehicle.Chassis.SteeringWheel.Angle" to "0.0"))
        assertEquals(SignalAvailability.SIMULATED, r.availability("Vehicle.Speed"))
        assertEquals(AvailabilityBadge(live = 0, simulated = 2, missing = 1), r.badge())
        assertEquals(3, r.badge().total)
    }

    @Test
    fun `keys outside the session set are ignored and reported MISSING`() {
        val r = SignalRegistry(keys, simulated = false)
        r.onValues(mapOf("Vehicle.Does.Not.Matter" to "1"))
        assertEquals(SignalAvailability.MISSING, r.availability("Vehicle.Does.Not.Matter"))
        assertTrue(r.snapshot().values.all { it == SignalAvailability.MISSING })
    }
}
