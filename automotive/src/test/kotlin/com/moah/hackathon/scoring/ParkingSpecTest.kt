package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.Gear
import com.moah.hackathon.vehicle.SimOnlySignals
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingSpecTest {

    @Test
    fun `rear perpendicular is the historical default - reverse entry, 90 degrees, all eight parking keys`() {
        val rear = ParkingSpec.REAR_PERPENDICULAR
        assertEquals(Gear.REVERSE, rear.entryGear)
        assertEquals(Gear.DRIVE, rear.fixGear)
        assertEquals(ParkingVerdicts.PERPENDICULAR_TARGET_DEG, rear.targetHeadingDeg)
        assertEquals(ParkingRecorder.KEYS, rear.keys)
        assertEquals(8, rear.keys.size)
        assertTrue(rear.usesRearDistance)
    }

    @Test
    fun `front perpendicular drives in - no rear distance key, proximity only through the real warning flag`() {
        val front = ParkingSpec.FRONT_PERPENDICULAR
        assertEquals(Gear.DRIVE, front.entryGear)
        assertEquals(Gear.REVERSE, front.fixGear)
        assertEquals(ParkingVerdicts.PERPENDICULAR_TARGET_DEG, front.targetHeadingDeg)
        assertEquals(7, front.keys.size)
        assertFalse(SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM in front.keys)
        assertTrue(VssConstants.VEHICLE_ADAS_OBSTACLEDETECTION_ISWARNING in front.keys)
        assertFalse(front.usesRearDistance)
        // 뒤 거리만 빠지고 나머지는 후면과 같다 — 배지 분모가 7 이 되는 근거
        assertEquals(ParkingRecorder.KEYS - SimOnlySignals.OBSTACLE_REAR_DISTANCE_CM, front.keys)
    }
}
