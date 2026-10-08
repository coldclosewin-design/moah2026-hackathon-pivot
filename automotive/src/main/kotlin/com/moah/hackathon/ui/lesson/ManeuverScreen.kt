package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.feature.lesson.TaskType

@Composable
internal fun ManeuverScreen(state: ManeuverDisplayState, locked: Boolean, stopped: Boolean, subtitle: String?,
    onFinish: () -> Unit, demo: (@Composable () -> Unit)? = null, taskTitle: String = "후면 직각 주차") {
    if (locked) {
        ResultLockedScreen(message = if (state.taskType == TaskType.CHECKLIST)
            "정차하면 점검 상태가 다시 보여요." else "속도를 낮추면 주차 도식이 다시 보여요.")
        return
    }
    if (state.taskType != TaskType.CHECKLIST) {
        ParkingRingScreen(state, stopped, subtitle, onFinish, demo, taskTitle)
        return
    }
    ChecklistCarScreen(state, stopped, subtitle, onFinish, demo, taskTitle)
}

@Composable
internal fun FinishButton(emphasized: Boolean, onFinish: () -> Unit) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(emphasized) {
        pulse.snapTo(1f)
        if (emphasized) repeat(2) {
            pulse.animateTo(1.025f, tween(450))
            pulse.animateTo(1f, tween(450))
        }
    }
    PrimaryPill(stringResource(R.string.lesson_finish), onFinish,
        Modifier.fillMaxWidth().graphicsLayer { scaleX = pulse.value; scaleY = pulse.value })
}
