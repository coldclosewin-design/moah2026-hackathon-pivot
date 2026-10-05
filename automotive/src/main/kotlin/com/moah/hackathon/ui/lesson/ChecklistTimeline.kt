package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture
import com.moah.hackathon.vehicle.SignalAvailability

internal data class ChecklistStep(val label: String, val value: String?, val satisfied: Boolean,
    val signal: SignalAvailability, val source: String = signalLabel(signal), val failed: Boolean = false) {
    val measured get() = value != null && signal != SignalAvailability.MISSING
    val pending get() = measured && !satisfied && !failed
}

/** Same signal decisions as the cards, in the predrive guide's order. */
internal fun checklistSteps(state: ManeuverDisplayState): List<ChecklistStep> {
    val ignition = state.checklistIgnition()
    return listOf(
        ChecklistStep("도어", state.doorOpen?.let { if (it) "열림" else "닫힘" }, state.doorOpen == false, state.doorSignal),
        ChecklistStep("안전벨트", state.belt?.let { if (it) "채움" else "미수행" }, state.belt == true, state.beltSignal),
        ChecklistStep("기어", state.gear, state.gear == "P", state.gearSignal),
        ChecklistStep("브레이크 / 시동", ignition.value, ignition.satisfied, ignition.signal, ignition.source,
            failed = ignition.value == "브레이크 없이 켜짐"),
        ChecklistStep("좌 지시등", checklistLightValue(state.indicatorLeft, state.leftIndicatorChecked),
            state.indicatorLeft == true || state.leftIndicatorChecked == true, state.indicatorLeftSignal),
        ChecklistStep("우 지시등", checklistLightValue(state.indicatorRight, state.rightIndicatorChecked),
            state.indicatorRight == true || state.rightIndicatorChecked == true, state.indicatorRightSignal),
        ChecklistStep("비상등", checklistLightValue(state.hazard, state.hazardChecked),
            state.hazard == true || state.hazardChecked == true, state.hazardSignal),
    )
}

@Composable
internal fun ChecklistTimeline(state: ManeuverDisplayState, showSource: Boolean, modifier: Modifier) {
    val steps = checklistSteps(state)
    val current = steps.indexOfFirst { it.pending }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            steps.forEach { step ->
                Box(Modifier.weight(1f).height(12.dp).background(
                    if (step.measured && step.satisfied) CoachColors.Periwinkle else CoachColors.Paper.copy(alpha = .14f),
                    RoundedCornerShape(100)))
            }
        }
        steps.forEachIndexed { index, step ->
            val active = index == current
            val muted = CoachColors.Paper.copy(alpha = .6f)
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Canvas(Modifier.width(56.dp).fillMaxHeight()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    if (index > 0) drawLine(CoachColors.Paper.copy(alpha = .16f), Offset(center.x, 0f), center, 4.dp.toPx())
                    if (index < steps.lastIndex) drawLine(CoachColors.Paper.copy(alpha = .16f), center,
                        Offset(center.x, size.height), 4.dp.toPx())
                    val radius = (if (active) 22 else 16).dp.toPx()
                    if (active) drawCircle(CoachColors.Signal.copy(alpha = .18f), radius + 10.dp.toPx(), center)
                    drawCircle(CoachColors.Ink, radius, center)
                    when {
                        !step.measured -> drawCircle(muted, radius, center, style = Stroke(4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))))
                        step.satisfied -> drawCircle(CoachColors.Periwinkle, radius, center)
                        step.failed -> drawCircle(CoachColors.Signal, radius, center)
                        else -> drawCircle(CoachColors.Signal, radius, center, style = Stroke(4.dp.toPx()))
                    }
                }
                Column(Modifier.weight(1f).then(if (active) Modifier.surfaceTexture(
                    CoachColors.Paper.copy(alpha = .08f).compositeOver(CoachColors.Ink), CoachTexture.SelectedCard) else Modifier)
                    .padding(horizontal = 32.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        LessonText(step.label, 38, if (step.satisfied) muted else CoachColors.Paper,
                            modifier = Modifier.weight(1f))
                        LessonText(if (step.measured) step.value!! else "미측정", if (active) 44 else 38,
                            when { !step.measured -> muted; step.satisfied -> CoachColors.Paper; else -> CoachColors.Signal })
                    }
                    if (showSource && (step.measured || step.source != "미측정"))
                        LessonText(step.source.replace('\n', ' '), 28, muted)
                }
            }
        }
    }
}
