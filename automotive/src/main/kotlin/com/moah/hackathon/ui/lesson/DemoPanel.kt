package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.ScenarioPlayback

// The rail floats above the poster; opening it never changes the reading width.
internal val LocalDemoExpansion = compositionLocalOf<MutableState<Boolean>?> { null }

@Composable
internal fun DemoPanel(scenarios: List<Scenario>, playback: ScenarioPlayback?, onPlay: (String) -> Unit,
    onStopScenario: () -> Unit, onStopCar: () -> Unit, onResumeCar: () -> Unit, onDoor: (Boolean) -> Unit) {
    val ownExpansion = rememberSaveable { mutableStateOf(false) }
    val expansion = LocalDemoExpansion.current ?: ownExpansion
    val rail = LocalDemoExpansion.current != null
    val play: (String) -> Unit = { id -> expansion.value = false; onPlay(id) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextAction(stringResource(R.string.demo_toggle), { expansion.value = !expansion.value },
            Modifier.align(Alignment.End), size = 32)
        if (expansion.value) {
            Eyebrow("시뮬레이션 신호", color = CoachColors.Periwinkle)
            if (rail) scenarios.forEach { scenario -> DemoButton(scenario.title, { play(scenario.id) }) }
            else scenarios.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { scenario -> DemoButton(scenario.title, { play(scenario.id) }, Modifier.weight(1f), compact = true) }
                }
            }
            val progress = playback?.let { (it.stepIndex + 1).coerceAtMost(it.stepCount) } ?: 0
            LessonText(playback?.let { "${if (it.finished) "재생 완료" else "재생 중"} · $progress/${it.stepCount}" }
                ?: "재생 대기", 32, CoachColors.Muted)
            if (rail) {
                DemoButton(stringResource(R.string.demo_stop_car), onStopCar)
                DemoButton(stringResource(R.string.demo_resume_car), onResumeCar)
                DemoButton(stringResource(R.string.demo_open_door), { onDoor(true) })
                DemoButton(stringResource(R.string.demo_close_door), { onDoor(false) })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoButton(stringResource(R.string.demo_stop_car), onStopCar, Modifier.weight(1f), compact = true)
                    DemoButton(stringResource(R.string.demo_resume_car), onResumeCar, Modifier.weight(1f), compact = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoButton(stringResource(R.string.demo_open_door), { onDoor(true) }, Modifier.weight(1f), compact = true)
                    DemoButton(stringResource(R.string.demo_close_door), { onDoor(false) }, Modifier.weight(1f), compact = true)
                }
            }
            DemoButton(stringResource(R.string.demo_stop_scenario), onStopScenario, compact = !rail)
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = if (compact) 72.dp else 84.dp), shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CoachColors.Lavender, contentColor = CoachColors.Ink),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        LessonText(label, 32)
    }
}
