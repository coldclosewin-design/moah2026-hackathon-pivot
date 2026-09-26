package com.moah.hackathon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moah.hackathon.ui.lesson.BrandMark

/**
 * 주제 확정 전 자리표시자. 속도(sensor)·도어(actuator) 왕복 경로를 눈으로 확인하는 용도.
 * 2560×1440 가로 자동차 디스플레이 기준으로 크게 그린다.
 */
@Composable
fun DashboardScreen(vm: DashboardViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
    Box(modifier = Modifier.padding(48.dp)) {
        BrandMark(Modifier.align(Alignment.TopStart))

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.speedKmh?.let { "%.0f".format(it) } ?: "--",
                    fontSize = 220.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(text = "km/h", fontSize = 48.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                StatusCard(
                    title = "Driver door",
                    value = when (state.driverDoorOpen) {
                        true -> "OPEN"
                        false -> "CLOSED"
                        null -> "--"
                    },
                )
                StatusCard(
                    title = "ABS",
                    value = when (state.absEnabled) {
                        true -> "ENABLED"
                        false -> "DISABLED"
                        null -> "--"
                    },
                )
                Button(onClick = vm::toggleDriverDoor) {
                    Text(text = "Toggle driver door", fontSize = 32.sp, modifier = Modifier.padding(8.dp))
                }
                state.lastSetError?.let {
                    Text(text = "set failed: $it", color = MaterialTheme.colorScheme.error, fontSize = 24.sp)
                }
            }
        }
    }
    }
}

@Composable
private fun StatusCard(title: String, value: String) {
    Card(modifier = Modifier.width(420.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = value, fontSize = 56.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
