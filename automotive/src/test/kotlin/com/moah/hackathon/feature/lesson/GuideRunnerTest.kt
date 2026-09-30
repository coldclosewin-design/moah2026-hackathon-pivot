package com.moah.hackathon.feature.lesson

import com.moah.hackathon.data.ParkingScenarios
import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.scoring.ParkingRecorder
import com.moah.hackathon.vehicle.SignalRegistry
import mobis.vss.VssConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideRunnerTest {

    private fun fullRegistry(): SignalRegistry = SignalRegistry(ParkingRecorder.KEYS, simulated = true).also {
        it.onValues(ParkingRecorder.KEYS.associateWith { "x" })
    }

    @Test
    fun `good parking scenario confirms all six steps in order`() {
        val runner = GuideRunner(SeedCatalog.parkingGuide, fullRegistry())
        val spoken = ArrayList<String>()
        spoken += runner.start()
        var snap = VehicleSnapshot()
        for (step in ParkingScenarios.good.steps) {
            snap = snap.apply(step.values)
            spoken += runner.onSnapshot(snap)
        }
        assertTrue(runner.finished)
        assertTrue(runner.unverified.isEmpty())
        val confirms = SeedCatalog.parkingGuide.map { it.confirm }
        // 확인 문장이 단계 순서대로 전부 나온다
        assertEquals(confirms, spoken.filter { it in confirms })
        assertEquals("안전벨트를 매 주세요.", spoken.first())
        assertTrue(spoken.last().startsWith("다 되셨나요?"))
    }

    @Test
    fun `center step waits until the car has actually moved after full lock`() {
        val runner = GuideRunner(SeedCatalog.parkingGuide, fullRegistry())
        runner.start()
        var snap = VehicleSnapshot().apply(mapOf(
            VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to "true", VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON",
            VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to "-1", VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "-450",
        ))
        runner.onSnapshot(snap)
        assertEquals("center", runner.current!!.id)
        // 움직이지 않고 핸들만 중립으로 → 아직 확인 안 됨
        snap = snap.apply(mapOf(VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "0"))
        assertTrue(runner.onSnapshot(snap).isEmpty())
        assertEquals("center", runner.current!!.id)
        // 움직인 뒤 중립 → 확인
        snap = snap.apply(mapOf(VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "-450", VssConstants.VEHICLE_SPEED to "3.0"))
        runner.onSnapshot(snap)
        snap = snap.apply(mapOf(VssConstants.VEHICLE_CHASSIS_STEERINGWHEEL_ANGLE to "0"))
        assertEquals(listOf("곧게 후진하세요.", "다 들어왔으면 멈추고 기어 P."), runner.onSnapshot(snap))
    }

    @Test
    fun `steps whose signal is MISSING are read aloud but skipped and remembered`() {
        val registry = SignalRegistry(ParkingRecorder.KEYS, simulated = false).also {
            it.onValues(mapOf(VssConstants.VEHICLE_SPEED to "0", VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to "false",
                VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE to "OFF", VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to "126"))
        }
        val runner = GuideRunner(SeedCatalog.parkingGuide, registry)
        runner.start()
        var snap = VehicleSnapshot().apply(mapOf(VssConstants.VEHICLE_CABIN_SEAT_ROW1_DRIVERSIDE_ISBELTED to "true"))
        runner.onSnapshot(snap)
        snap = snap.apply(mapOf(VssConstants.VEHICLE_LOWVOLTAGESYSTEMSTATE to "ON"))
        runner.onSnapshot(snap)
        snap = snap.apply(mapOf(VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to "-1"))
        val out = runner.onSnapshot(snap)
        // R 확인 → 조향각 두 단계는 신호가 없어 읽고 넘김 → P 단계에서 대기
        assertEquals(listOf("좋아요.", "핸들을 오른쪽 끝까지 돌리세요.", "차가 45도쯤 되면 핸들을 중립으로 돌려 주세요.", "다 들어왔으면 멈추고 기어 P."), out)
        assertEquals(listOf("steer-right", "center"), runner.unverified.map { it.id })
        assertEquals("park", runner.current!!.id)
        assertFalse(runner.view()!!.unverified)
        snap = snap.apply(mapOf(VssConstants.VEHICLE_POWERTRAIN_TRANSMISSION_SELECTEDGEAR to "126"))
        runner.onSnapshot(snap)
        assertTrue(runner.finished)
    }
}
