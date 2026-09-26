package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.ScenarioPlayback

@Composable
internal fun DemoPanel(scenarios: List<Scenario>, playback: ScenarioPlayback?, onPlay: (String) -> Unit,
    onStopScenario: () -> Unit, onStopCar: () -> Unit, onResumeCar: () -> Unit, onDoor: (Boolean) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    LessonCard(Modifier.fillMaxWidth(), padding = 20, spacing = 12) {
        LessonButton(stringResource(R.string.demo_toggle), { expanded = !expanded }, Modifier.fillMaxWidth())
        if (expanded) {
            LessonText("시뮬레이션 신호", 32, CoachColors.Simulated)
            scenarios.forEach { scenario ->
                val label = when (scenario.title) {
                    "잘한 주차" -> stringResource(R.string.demo_good)
                    "못한 주차" -> stringResource(R.string.demo_bad)
                    else -> scenario.title
                }
                LessonButton(label, { onPlay(scenario.id) }, Modifier.fillMaxWidth())
            }
            val progress = playback?.let { (it.stepIndex + 1).coerceAtMost(it.stepCount) } ?: 0
            LessonText(playback?.let { "${if (it.finished) "재생 완료" else "재생 중"} · $progress/${it.stepCount}" } ?: "재생 대기", 32, CoachColors.Muted)
            LinearProgressIndicator(progress = { playback?.let { progress.toFloat() / it.stepCount.coerceAtLeast(1) } ?: 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = CoachColors.Simulated, trackColor = CoachColors.Outline)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LessonButton(stringResource(R.string.demo_stop_car), onStopCar, Modifier.weight(1f))
                LessonButton(stringResource(R.string.demo_resume_car), onResumeCar, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LessonButton(stringResource(R.string.demo_open_door), { onDoor(true) }, Modifier.weight(1f))
                LessonButton(stringResource(R.string.demo_close_door), { onDoor(false) }, Modifier.weight(1f))
            }
            LessonButton(stringResource(R.string.demo_stop_scenario), onStopScenario, Modifier.fillMaxWidth())
        }
    }
}
