package com.moah.hackathon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moah.hackathon.BuildConfig
import com.moah.hackathon.vehicle.VehiclePort
import com.moah.hackathon.vehicle.VssValues
import com.moah.hackathon.vehicle.toVssBoolean
import com.moah.hackathon.vehicle.toVssFloat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mobis.vss.VssConstants

/** 주제 확정 전 자리표시자 화면의 상태. VehiclePort 사용 예시 역할도 한다. */
data class DashboardState(
    val speedKmh: Float? = null,
    val driverDoorOpen: Boolean? = null,
    val absEnabled: Boolean? = null,
    val lastSetError: String? = null,
    val source: String = if (BuildConfig.USE_FAKE_VSS) "FAKE" else "REAL",
)

class DashboardViewModel(private val port: VehiclePort) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    private val keys = listOf(
        VssConstants.VEHICLE_SPEED,
        VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN,
        VssConstants.VEHICLE_ADAS_ABS_ISENABLED,
    )

    init {
        // viewModelScope 가 취소되면 collect 가 취소되어 구독이 해제된다.
        viewModelScope.launch {
            port.observe(keys).collect { delta -> _state.update { it.apply(delta) } }
        }
    }

    fun toggleDriverDoor() {
        viewModelScope.launch {
            val next = !(_state.value.driverDoorOpen ?: false)
            val failed = port.set(mapOf(VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN to VssValues.ofBoolean(next)))
            _state.update { it.copy(lastSetError = failed.takeIf { f -> f.isNotEmpty() }?.joinToString()) }
        }
    }

    private fun DashboardState.apply(delta: Map<String, String>): DashboardState = copy(
        speedKmh = delta[VssConstants.VEHICLE_SPEED]?.toVssFloat() ?: speedKmh,
        driverDoorOpen = delta[VssConstants.VEHICLE_CABIN_DOOR_ROW1_DRIVERSIDE_ISOPEN]?.toVssBoolean() ?: driverDoorOpen,
        absEnabled = delta[VssConstants.VEHICLE_ADAS_ABS_ISENABLED]?.toVssBoolean() ?: absEnabled,
    )

    companion object {
        fun factory(port: VehiclePort): ViewModelProvider.Factory = viewModelFactory {
            initializer { DashboardViewModel(port) }
        }
    }
}
