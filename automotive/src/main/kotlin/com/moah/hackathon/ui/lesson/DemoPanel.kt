package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.ScenarioPlayback

// The rail floats above the poster; opening it never changes the reading width.
internal val LocalDemoExpansion = compositionLocalOf<MutableState<Boolean>?> { null }
// Draft comparison only: remove the unselected option after the user's screenshot review.
internal enum class DemoToggleStyle { TEXT, PILL }
internal val LocalDemoToggleStyle = compositionLocalOf { DemoToggleStyle.PILL }

@Composable
internal fun DemoPanel(scenarios: List<Scenario>, playback: ScenarioPlayback?, onPlay: (String) -> Unit,
    onStopScenario: () -> Unit, onStopCar: () -> Unit, onResumeCar: () -> Unit, onDoor: (Boolean) -> Unit) {
    val ownExpansion = rememberSaveable { mutableStateOf(false) }
    val expansion = LocalDemoExpansion.current ?: ownExpansion
    val play: (String) -> Unit = { id -> expansion.value = false; onPlay(id) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val toggleLabel = stringResource(R.string.demo_toggle)
        if (LocalDemoToggleStyle.current == DemoToggleStyle.TEXT) {
            TextAction(toggleLabel, { expansion.value = !expansion.value }, Modifier.align(Alignment.End), size = 32)
        } else {
            Box(Modifier.align(Alignment.End).size(64.dp, 32.dp).clip(RoundedCornerShape(100))
                .background(if (expansion.value) CoachColors.Periwinkle else CoachColors.Lavender)
                .clickable { expansion.value = !expansion.value }
                .semantics { contentDescription = toggleLabel; role = Role.Button })
        }
        if (expansion.value) {
            Eyebrow("시뮬레이션 신호", color = CoachColors.Periwinkle)
            scenarios.forEach { scenario -> DemoButton(scenario.title, { play(scenario.id) }) }
            // The parent supplies 12 dp spacing; four more makes 16 dp on each side.
            Box(Modifier.padding(vertical = 4.dp)) {
                PosterRule(color = CoachColors.Periwinkle.copy(alpha = .4f))
            }
            val progress = playback?.let { (it.stepIndex + 1).coerceAtMost(it.stepCount) } ?: 0
            Eyebrow(playback?.let { "${if (it.finished) "재생 완료" else "재생 중"} · $progress/${it.stepCount}" }
                ?: "재생 대기", color = CoachColors.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DemoButton(stringResource(R.string.demo_stop_car), onStopCar, Modifier.weight(1f))
                DemoButton(stringResource(R.string.demo_resume_car), onResumeCar, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DemoButton(stringResource(R.string.demo_open_door), { onDoor(true) }, Modifier.weight(1f))
                DemoButton(stringResource(R.string.demo_close_door), { onDoor(false) }, Modifier.weight(1f))
            }
            DemoButton(stringResource(R.string.demo_stop_scenario), onStopScenario)
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick, modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CoachColors.Paper.copy(alpha = .5f), contentColor = CoachColors.Ink),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        LessonText(label, 32, modifier = Modifier.fillMaxWidth(), maxLines = 1)
    }
}
