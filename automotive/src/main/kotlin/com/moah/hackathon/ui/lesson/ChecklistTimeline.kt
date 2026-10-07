package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.ChecklistReveal
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.SignalAvailability
import kotlin.math.abs
import kotlin.math.roundToInt

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
internal fun ChecklistTimeline(state: ManeuverDisplayState, modifier: Modifier) {
    val steps = checklistSteps(state)
    if (state.checklistReveal == ChecklistReveal.ALL) {
        val current = checklistFocus(state, steps)
        val position by animateFloatAsState(current.toFloat(), tween(if (selectionMotionEnabled()) 300 else 0), label = "checklist-wheel")
        BoxWithConstraints(modifier.clipToBounds().testTag("checklist-wheel")) {
            val available = maxHeight
            steps.forEachIndexed { index, step ->
                val distance = abs(index - position)
                val emphasis = (1f - distance).coerceIn(0f, 1f)
                val rowHeight = (96 + 72 * emphasis).dp
                val offset = checklistWheelOffset(index - position) * available.value / 1040f
                val size = (38 + 24 * emphasis - (distance - 1).coerceAtLeast(0f) * 2).roundToInt().coerceAtLeast(26)
                val alpha = (1f - distance * .18f).coerceAtLeast(.24f)
                Column(Modifier.fillMaxWidth().height(rowHeight)
                    .offset { IntOffset(0, ((available - rowHeight) / 2 + offset.dp).roundToPx()) }
                    .testTag("checklist-step-$index"), verticalArrangement = Arrangement.Center) {
                    if (index == current) PosterRule(color = CoachColors.Paper.copy(alpha = .22f))
                    Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.width(6.dp).height(48.dp).background(
                            if (index == current) CoachColors.Signal else CoachColors.Ink))
                        Column(Modifier.weight(1f)) {
                            LessonText(step.label, size, CoachColors.Paper.copy(alpha = alpha))
                            if (index == current && (step.measured || step.source != "미측정"))
                                LessonText(step.source.replace('\n', ' '), 28, CoachColors.Paper.copy(alpha = .6f))
                        }
                        LessonText(checklistValue(step, state.checklistReveal), if (distance < .5f) 44 else 34,
                            if (step.failed) CoachColors.Signal else CoachColors.Paper.copy(alpha = alpha))
                    }
                    if (index == current) PosterRule(color = CoachColors.Paper.copy(alpha = .22f))
                }
            }
        }
    } else {
        val paper = state.mode == LessonMode.EVALUATE
        val foreground = if (paper) CoachColors.Ink else CoachColors.Paper
        Column(modifier.then(if (paper) Modifier.border(2.dp, CoachColors.Ink).padding(24.dp) else Modifier)) {
            steps.forEach { step ->
                val mistake = state.checklistReveal == ChecklistReveal.MISTAKES && step.failed && step.measured
                Row(Modifier.fillMaxWidth().weight(1f).then(if (mistake) Modifier.background(CoachColors.Ink) else Modifier)
                    .padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (state.checklistReveal == ChecklistReveal.NAMES_ONLY) Box(Modifier.size(30.dp).border(2.dp, foreground))
                    LessonText(step.label, 38, foreground, modifier = Modifier.weight(1f))
                    LessonText(checklistValue(step, state.checklistReveal), 38, foreground)
                }
            }
        }
    }
}

internal fun checklistFocus(state: ManeuverDisplayState, steps: List<ChecklistStep> = checklistSteps(state)): Int =
    state.guideStep?.substringBefore('/')?.toIntOrNull()?.minus(1)?.takeIf { it in steps.indices }
        ?: steps.indexOfFirst { it.failed || it.pending }.takeIf { it >= 0 } ?: steps.lastIndex

internal fun checklistValue(step: ChecklistStep, reveal: ChecklistReveal): String = when {
    !step.measured -> "미측정"
    reveal == ChecklistReveal.NAMES_ONLY -> ""
    reveal == ChecklistReveal.ALL || step.failed -> step.value.orEmpty()
    step.satisfied -> "✓"
    else -> "—"
}

/** Compress distant rows while the selected row remains at the panel's vertical center. */
internal fun checklistWheelOffset(distance: Float): Float {
    val positions = listOf(0f, 156f, 252f, 332f, 396f, 450f, 494f)
    val d = abs(distance).coerceAtMost(6f)
    val index = d.toInt()
    val value = positions[index] + (positions[(index + 1).coerceAtMost(6)] - positions[index]) * (d - index)
    return if (distance < 0) -value else value
}

@Composable
internal fun ChecklistModeLadder(mode: LessonMode) {
    val foreground = if (mode == LessonMode.EVALUATE) CoachColors.Ink else CoachColors.Paper
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("checklist-mode-ladder")) {
        listOf(LessonMode.GUIDE, LessonMode.HINT, LessonMode.EVALUATE).forEach { item ->
            LessonText(item.label, 26, if (item == mode) {
                if (mode == LessonMode.EVALUATE) CoachColors.Paper else CoachColors.Ink
            } else foreground.copy(alpha = .6f), modifier = Modifier
                .background(if (item == mode) foreground else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 8.dp).semantics { selected = item == mode })
        }
    }
}
