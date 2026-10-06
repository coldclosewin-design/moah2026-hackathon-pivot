package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.ports.copilot.CopilotAuth
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture
import com.moah.hackathon.vehicle.Scenario
import com.moah.hackathon.vehicle.ScenarioPlayback
import kotlinx.coroutines.flow.StateFlow

/** Measured below the content, never over a driver action. Playback keeps the controls visible. */
@Composable
internal fun AdminBand(scenarios: List<Scenario>, playback: ScenarioPlayback?, onPlay: (String) -> Unit,
    onStopScenario: () -> Unit, onStopCar: () -> Unit, onResumeCar: () -> Unit, onDoor: (Boolean) -> Unit,
    aiState: StateFlow<CopilotAuth.State>? = null, onConnectAi: () -> Unit = {},
    onResetRecords: () -> Unit = {}, onHide: () -> Unit = {}, signalSource: String = "") {
    var more by rememberSaveable { mutableStateOf(false) }
    val auth = aiState?.collectAsState()?.value
    Column(Modifier.fillMaxWidth().background(CoachColors.Ink).testTag("admin-band")) {
        Row(Modifier.fillMaxWidth().height(90.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LessonText("관리자", 28, CoachColors.Paper.copy(alpha = .6f))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                scenarios.forEach { scenario -> BandAction(scenario.title, { onPlay(scenario.id) }) }
            }
            BandAction(stringResource(R.string.demo_stop_scenario), onStopScenario)
            BandAction(stringResource(R.string.demo_stop_car), onStopCar)
            BandAction(stringResource(R.string.demo_resume_car), onResumeCar)
            BandAction(stringResource(R.string.demo_open_door), { onDoor(true) })
            BandAction(stringResource(R.string.demo_close_door), { onDoor(false) })
            aiLine(auth)?.let { LessonText(it.title, 28, CoachColors.Paper) }
            BandAction(if (more) "접기 ▾" else "더 보기 ▴", { more = !more })
        }
        if (more) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            BandAction("기록 초기화", onResetRecords)
            BandAction("패널 숨김", onHide)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val progress = playback?.let { "${if (it.finished) "재생 완료" else "재생 중"} · ${(it.stepIndex + 1).coerceAtMost(it.stepCount)}/${it.stepCount}" } ?: "재생 대기"
                LessonText(listOf(signalSource, progress).filter { it.isNotEmpty() }.joinToString(" · "), 28, CoachColors.Paper.copy(alpha = .6f))
                AdminAiDetail(auth, onConnectAi, onInk = true)
            }
        }
    }
}

@Composable
private fun BandAction(label: String, onClick: () -> Unit) {
    Box(Modifier.height(64.dp).surfaceTexture(CoachColors.Paper.copy(alpha = .12f), CoachTexture.Chip, pill = true)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        LessonText(label, 28, CoachColors.Paper, maxLines = 1)
    }
}

@Composable
internal fun AdminAiDetail(state: CopilotAuth.State?, onConnect: () -> Unit, onInk: Boolean = false) {
    aiLine(state)?.let { line ->
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LessonText(if (state is CopilotAuth.State.Code) state.uri else line.detail, 28,
                    if (onInk) CoachColors.Paper.copy(alpha = .7f) else CoachColors.Muted)
                if (state is CopilotAuth.State.Code) LessonText(state.userCode, 40,
                    if (onInk) CoachColors.Paper else CoachColors.Ink)
            }
            if (line.showConnect) {
                if (onInk) BandAction(stringResource(R.string.demo_connect_ai), onConnect)
                else TextAction(stringResource(R.string.demo_connect_ai), onConnect)
            }
        }
    }
}
