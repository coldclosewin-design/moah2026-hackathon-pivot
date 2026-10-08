package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.SignalAvailability

/** A7: seven labels remain anchored to the physical controls in all three reveal modes. */
@Composable
internal fun ChecklistCarScreen(state: ManeuverDisplayState, stopped: Boolean, subtitle: String?,
    onFinish: () -> Unit, demo: (@Composable () -> Unit)?, taskTitle: String) {
    val steps = checklistSteps(state)
    val focus = checklistFocus(state, steps)
    PosterSurface(band = demo) {
        Row(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CoachColors.Lavender, CoachColors.Platinum)))
            .padding(72.dp), horizontalArrangement = Arrangement.spacedBy(52.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                BrandMark()
                Spacer(Modifier.height(48.dp))
                LessonText("$taskTitle · ${state.attempt}회차", 36, CoachColors.Muted)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().testTag("checklist-car")) {
                    val w = maxWidth
                    val h = maxHeight
                    // V4 faces right. Each leader terminates at a control location on the vehicle.
                    Canvas(Modifier.fillMaxSize()) {
                        val carScale = size.width * .62f / 250f
                        withTransform({ translate(size.width * .09f, size.height * .67f); scale(carScale, carScale, Offset.Zero); rotate(-90f, Offset.Zero) }) {
                            vehicleSilhouette(CoachColors.Ink, CoachColors.Platinum, CoachColors.Paper, outlineWidth = 2.4f)
                        }
                        listOf(
                            .43f to .38f to (.43f to .21f), .44f to .48f to (.19f to .25f),
                            .54f to .48f to (.63f to .17f), .69f to .37f to (.80f to .37f),
                            .69f to .66f to (.80f to .76f), .60f to .51f to (.80f to .55f),
                        ).forEach { (start, end) ->
                            val a = Offset(start.first * size.width, start.second * size.height)
                            val b = Offset(end.first * size.width, end.second * size.height)
                            drawLine(CoachColors.Ink, a, b, 2.dp.toPx()); drawCircle(CoachColors.Ink, 9.dp.toPx(), a)
                        }
                    }
                    val positions = listOf(.34f to .20f, .015f to .20f, .25f to .49f,
                        .56f to .00f, .77f to .31f, .77f to .74f, .77f to .53f)
                    steps.forEachIndexed { index, step ->
                        val (x, y) = positions[index]
                        val active = state.checklistReveal == ChecklistReveal.ALL && focus == index
                        val mistake = state.checklistReveal == ChecklistReveal.MISTAKES && step.failed && step.measured
                        val filled = state.checklistReveal != ChecklistReveal.NAMES_ONLY && step.satisfied && !active
                        val ink = if (filled) CoachColors.Paper else if (mistake) CoachColors.Signal else CoachColors.Ink
                        val width = when { index == 3 -> w * .43f; index >= 4 -> w * .23f; else -> w * .30f }
                        Column(Modifier.offset(w * x, h * y).width(width).testTag("checklist-step-$index")
                            .shadow(if (active || filled) 5.dp else 0.dp, RoundedCornerShape(44.dp))
                            .background(if (filled) CoachColors.Ink else CoachColors.Paper, RoundedCornerShape(44.dp))
                            .then(if (state.checklistReveal == ChecklistReveal.NAMES_ONLY) Modifier.border(2.dp, CoachColors.Platinum, RoundedCornerShape(44.dp)) else Modifier)
                            .padding(horizontal = 22.dp, vertical = if (active) 24.dp else 14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LessonText(step.label, if (active) 44 else 36, ink, modifier = Modifier.weight(1f))
                                if (state.checklistReveal != ChecklistReveal.NAMES_ONLY)
                                    SignalShape(if (step.measured) step.signal else SignalAvailability.MISSING, Modifier.size(28.dp), ink)
                            }
                            val value = checklistValue(step, state.checklistReveal)
                            if (value.isNotEmpty()) LessonText(value, if (active) 44 else 36, if (step.failed) CoachColors.Signal else ink)
                            else Box(Modifier.padding(top = 12.dp).size(30.dp).border(2.dp, CoachColors.Platinum))
                        }
                    }
                }
            }
            SheetCard(Modifier.width(680.dp).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(52.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    LessonText("${state.speed} km/h", 72)
                    Row(Modifier.fillMaxWidth().background(CoachColors.Lavender, RoundedCornerShape(100)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround) {
                        listOf(LessonMode.GUIDE, LessonMode.HINT, LessonMode.EVALUATE).forEach { mode ->
                            LessonText(mode.label, 32, if (mode == state.mode) CoachColors.Paper else CoachColors.Muted,
                                modifier = Modifier.background(if (mode == state.mode) CoachColors.Ink else CoachColors.Lavender, RoundedCornerShape(100)).padding(horizontal = 20.dp, vertical = 12.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Eyebrow(if (state.mode == LessonMode.GUIDE) "코치 ${state.guideStep ?: "${focus + 1}/7"}" else "코치")
                        YellowModeIcon(bars = when (state.mode) { LessonMode.GUIDE -> 3; LessonMode.HINT -> 2; else -> 1 })
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Headline(coachDisplayText(when (state.mode) {
                            LessonMode.GUIDE -> state.guideText ?: subtitle ?: "순서대로 해 보세요."
                            LessonMode.HINT -> state.hintText ?: "순서대로 해 보세요."
                            else -> "끝나면\n다 됐어요를 눌러 주세요."
                        }), size = 72)
                        if (state.mode != LessonMode.GUIDE) LessonText(if (state.mode == LessonMode.HINT) "틀릴 때만 말해요" else "조용히 지켜봐요", 36, CoachColors.Muted)
                    }
                    LessonText(steps.map { if (it.measured) it.source.replace('\n', ' ') else "미측정" }.distinct().joinToString(" · "), 32, CoachColors.Muted)
                    if (stopped) FinishButton(state.askedDone, onFinish)
                }
            }
        }
    }
}
