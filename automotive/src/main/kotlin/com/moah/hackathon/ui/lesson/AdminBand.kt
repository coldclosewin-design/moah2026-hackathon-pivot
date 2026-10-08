package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
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

/** C3: only the 32 dp rail is reserved; the drawer overlays the unchanged content. */
@Composable
internal fun AdminBand(scenarios: List<Scenario>, playback: ScenarioPlayback?, onPlay: (String) -> Unit,
    onStopScenario: () -> Unit, onStopCar: () -> Unit, onResumeCar: () -> Unit, onDoor: (Boolean) -> Unit,
    aiState: StateFlow<CopilotAuth.State>? = null, onConnectAi: () -> Unit = {},
    onResetRecords: () -> Unit = {}, onHide: () -> Unit = {}, signalSource: String = "",
    steeringSteps: List<Float> = emptyList(), onSteering: (Float) -> Unit = {}) {
    var more by rememberSaveable { mutableStateOf(false) }
    var open by rememberSaveable { mutableStateOf(false) }
    fun act(action: () -> Unit) { action(); open = false; more = false }
    val auth = aiState?.collectAsState()?.value
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    AnimatedVisibility(open, enter = slideInVertically(spring(.9f, 500f)) { it } + fadeIn(),
        exit = slideOutVertically(spring(.9f, 500f)) { it } + fadeOut()) {
    Column(Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
        .background(CoachColors.Periwinkle).testTag("admin-band")) {
        Row(Modifier.fillMaxWidth().height(90.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LessonText("관리자", 28, CoachColors.Paper.copy(alpha = .6f))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                scenarios.forEach { scenario -> BandAction(scenario.title, { act { onPlay(scenario.id) } }) }
            }
            BandAction(stringResource(R.string.demo_stop_scenario), { act(onStopScenario) })
            BandAction(stringResource(R.string.demo_stop_car), { act(onStopCar) })
            BandAction(stringResource(R.string.demo_resume_car), { act(onResumeCar) })
            BandAction(stringResource(R.string.demo_open_door), { act { onDoor(true) } })
            BandAction(stringResource(R.string.demo_close_door), { act { onDoor(false) } })
            aiLine(auth)?.let { LessonText(it.title, 28, CoachColors.Paper) }
            BandAction(if (more) "접기 ▾" else "더 보기 ▴", { more = !more })
        }
        if (more) Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            BandAction("기록 초기화", { act(onResetRecords) })
            BandAction("패널 숨김", { act(onHide) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val progress = playback?.let { "${if (it.finished) "재생 완료" else "재생 중"} · ${(it.stepIndex + 1).coerceAtMost(it.stepCount)}/${it.stepCount}" } ?: "재생 대기"
                LessonText(listOf("신호 출처", signalSource, progress).filter { it.isNotEmpty() }.joinToString(" · "),
                    28, CoachColors.Paper.copy(alpha = .6f), modifier = Modifier.testTag("admin-playback-source"))
                AdminAiDetail(auth, onConnectAi, onInk = true)
            }
        }
        if (steeringSteps.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LessonText("조향각", 28, CoachColors.Paper)
            steeringSteps.forEach { deg -> BandAction(steeringControlLabel(deg), { act { onSteering(deg) } }) }
            LessonText("시나리오를 멈춘 뒤 조작", 28, CoachColors.Platinum)
        }
        }
    }
    }
    // The invisible text alias preserves the text-driven demo scripts; the spoken name is explicit.
    Box(Modifier.fillMaxWidth().height(32.dp).background(CoachColors.Lavender).testTag("admin-rail"), contentAlignment = Alignment.Center) {
        Box(Modifier.width(160.dp).requiredHeight(64.dp).offset(y = (-16).dp)
            .clickable(role = Role.Button) { open = !open; if (!open) more = false }
            .semantics { contentDescription = if (open) "관리자 띠 닫기" else "관리자 띠 열기"; text = AnnotatedString("시연") }
            .testTag("admin-handle"), contentAlignment = Alignment.Center) {
            Box(Modifier.offset(y = 16.dp).size(160.dp, 28.dp).background(CoachColors.Periwinkle, RoundedCornerShape(100)))
        }
    }
    }
}

internal fun steeringControlLabel(deg: Float) = when {
    deg < 0f -> "왼쪽 ${-deg.toInt()}°"
    deg > 0f -> "오른쪽 ${deg.toInt()}°"
    else -> "중립 0°"
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
                if (state is CopilotAuth.State.Code) LessonText("AI 로그인", 24,
                    if (onInk) CoachColors.Paper.copy(alpha = .6f) else CoachColors.Muted)
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
