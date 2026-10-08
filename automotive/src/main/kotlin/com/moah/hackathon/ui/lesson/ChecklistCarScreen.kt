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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.vehicle.SignalAvailability

/** A4: the labels surround the white car; their leaders never cross another control. */
@Composable
internal fun ChecklistCarScreen(state: ManeuverDisplayState, stopped: Boolean, subtitle: String?,
    onFinish: () -> Unit, demo: (@Composable () -> Unit)?, taskTitle: String) {
    val steps = checklistSteps(state)
    val focus = checklistFocus(state, steps)
    val showChecklist = state.mode != LessonMode.EVALUATE
    PosterSurface(band = demo) {
        Row(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CoachColors.Lavender, CoachColors.Platinum)))
            .padding(72.dp), horizontalArrangement = Arrangement.spacedBy(52.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                BrandMark()
                Spacer(Modifier.height(48.dp))
                LessonText("$taskTitle · ${state.attempt}회차", 36, CoachColors.Muted)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().testTag("checklist-car")) {
                    val w = maxWidth / 561
                    val h = maxHeight / 333
                    val positions = listOf(100 to 14, 244 to 14, 220 to 272, 388 to 14,
                        424 to 82, 424 to 220, 424 to 150)
                    val anchors = listOf(232f to 115f, 251f to 142f, 258f to 166f,
                        292f to 142f, 369f to 130f, 369f to 202f, 302f to 166f)
                    val ends = listOf(180f to 60f, 262f to 60f, 278f to 272f,
                        410f to 60f, 424f to 105f, 424f to 243f, 424f to 173f)
                    Canvas(Modifier.fillMaxSize()) {
                        val sx = size.width / 561f
                        val sy = size.height / 333f
                        withTransform({ scale(sx, sy, Offset.Zero) }) {
                            withTransform({ translate(120f, 110f) }) {
                                // A4: soft layered floor shadow, four tires, mirrors and two glass faces.
                                for (i in 8 downTo 1) drawRoundRect(CoachColors.Ink.copy(alpha = .012f), Offset(-i.toFloat(), 8f-i),
                                    Size(262f+i*2,112f+i*2), CornerRadius(34f+i))
                                for (x in listOf(32f,192f)) for (y in listOf(-7f,103f))
                                    drawRoundRect(CoachColors.Ink, Offset(x,y), Size(40f,16f), CornerRadius(6f))
                                for (y in listOf(-10f,109f)) drawRoundRect(CoachColors.Periwinkle, Offset(168f,y), Size(10f,13f), CornerRadius(3f))
                                drawRoundRect(CoachColors.Paper, size = Size(262f,112f), cornerRadius = CornerRadius(34f))
                                drawRoundRect(CoachColors.Platinum, size = Size(262f,112f), cornerRadius = CornerRadius(34f), style = Stroke(1f))
                                val glass = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(58f,14f,186f,98f,22f,22f)) }
                                drawPath(glass, CoachColors.Lavender)
                                clipPath(glass) {
                                    drawPath(Path().apply { moveTo(190f,0f); lineTo(156f,0f); quadraticBezierTo(144f,56f,156f,112f); lineTo(190f,112f); close() },CoachColors.Periwinkle)
                                    drawPath(Path().apply { moveTo(54f,0f); lineTo(73f,0f); quadraticBezierTo(66f,56f,73f,112f); lineTo(54f,112f); close() },CoachColors.Periwinkle)
                                }
                                for (y in listOf(12f,80f)) drawRoundRect(CoachColors.Platinum,Offset(244f,y),Size(9f,20f),CornerRadius(4f))
                                for (y in listOf(14f,82f)) drawRoundRect(CoachColors.Periwinkle,Offset(9f,y),Size(6f,16f),CornerRadius(3f))
                            }
                            if (showChecklist) anchors.forEachIndexed { index, (x,y) ->
                                val (ex,ey) = ends[index]
                                drawLine(CoachColors.Muted,Offset(x,y),Offset(ex,ey),1.2f)
                                drawCircle(CoachColors.Paper,5.8f,Offset(x,y))
                                drawCircle(CoachColors.Ink,4.2f,Offset(x,y))
                            }
                        }
                    }
                    Box(Modifier.offset(w * 120, h * 100).size(w * 262, h * 132).testTag("checklist-vehicle-bounds"))
                    if (showChecklist) steps.forEachIndexed { index, step ->
                        val (x,y) = positions[index]
                        val active = state.checklistReveal == ChecklistReveal.ALL && focus == index
                        val visible = state.checklistReveal != ChecklistReveal.NAMES_ONLY
                        val filled = visible && step.satisfied && !active
                        val ink = if (filled) CoachColors.Paper else CoachColors.Ink
                        Row(Modifier.offset(w*x,h*y).size(if (index == 3) 456.dp else 396.dp,138.dp)
                            .testTag("checklist-step-$index").shadow(if (filled || active) 5.dp else 0.dp, RoundedCornerShape(42.dp))
                            .background(if (filled) CoachColors.Ink else CoachColors.Paper,RoundedCornerShape(42.dp))
                            .border(if (active) 4.dp else 1.5.dp,if (active) CoachColors.Ink else CoachColors.Platinum,RoundedCornerShape(42.dp))
                            .padding(horizontal=30.dp,vertical=20.dp), horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.SpaceBetween) {
                                LessonText(step.label,32,ink,maxLines=1)
                                Spacer(Modifier.weight(1f))
                                LessonText(checklistValue(step,state.checklistReveal),if(index==3) 30 else 32,
                                    if(visible && step.failed) CoachColors.Signal else ink,maxLines=1)
                            }
                            Column(Modifier.width(48.dp).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween) {
                                if(visible) SignalShape(if(step.measured) step.signal else SignalAvailability.MISSING,Modifier.size(28.dp),ink)
                                else Spacer(Modifier.size(28.dp))
                                if(visible && step.measured && (filled || step.failed)) LessonText(if(step.failed) "✗" else "✓",30,if(step.failed) CoachColors.Signal else ink)
                                else if (visible && !step.measured) SignalShape(SignalAvailability.MISSING, Modifier.size(30.dp), CoachColors.Muted)
                                else Canvas(Modifier.size(30.dp)) {
                                    drawCircle(if (active) CoachColors.Ink else CoachColors.Muted,
                                        radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(2.dp.toPx()))
                                }
                            }
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
                    if (stopped) FinishButton(state.askedDone, onFinish)
                }
            }
        }
    }
}
