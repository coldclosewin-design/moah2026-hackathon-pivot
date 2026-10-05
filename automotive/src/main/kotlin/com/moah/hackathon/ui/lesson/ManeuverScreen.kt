package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.ManeuverDisplayState
import com.moah.hackathon.feature.lesson.TaskType
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture
import com.moah.hackathon.vehicle.SignalAvailability
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ManeuverScreen(state: ManeuverDisplayState, locked: Boolean, stopped: Boolean, subtitle: String?,
    onFinish: () -> Unit, demo: (@Composable () -> Unit)? = null, taskTitle: String = "후면 직각 주차") {
    var visibleHint by remember(state.hintText) { mutableStateOf(maneuverText(state.hintText)) }
    val hintAlpha = remember { Animatable(1f) }
    LaunchedEffect(state.hintText) {
        if (visibleHint != null) {
            hintAlpha.snapTo(0f)
            hintAlpha.animateTo(1f, tween(180))
            delay(3_620)
            hintAlpha.animateTo(0f, tween(200))
            visibleHint = null
        }
    }
    val expansion = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(locked) { if (locked) expansion.value = false }
    val commonSignal = state.commonSignal()
    val checklist = state.taskType == TaskType.CHECKLIST
    PosterSurface {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(.53f).fillMaxHeight().background(CoachColors.Ink)
                    .padding(start = 180.dp, end = 100.dp, top = 64.dp, bottom = 52.dp)) {
                    BrandMark(color = CoachColors.Paper)
                    if (locked) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Headline("운전에 집중해 주세요", color = CoachColors.Paper)
                            Spacer(Modifier.height(32.dp))
                            LessonText(if (checklist) "정차하면 점검 상태가 다시 보여요." else "속도를 낮추면 주차 도식이 다시 보여요.",
                                40, CoachColors.Paper.copy(alpha = .7f))
                        }
                    } else if (checklist) {
                        Row(Modifier.weight(1f).fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ChecklistValue("도어", state.doorOpen?.let { if (it) "열림" else "닫힘" }, state.doorOpen == false,
                                    state.doorSignal, commonSignal == null)
                                ChecklistValue("안전벨트", state.belt?.let { if (it) "채움" else "미수행" }, state.belt == true,
                                    state.beltSignal, commonSignal == null)
                                ChecklistValue("기어", state.gear, state.gear == "P", state.gearSignal, commonSignal == null)
                                val ignition = state.checklistIgnition()
                                ChecklistValue("브레이크 / 시동", ignition.value, ignition.satisfied,
                                    ignition.signal, commonSignal == null, ignition.source)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ChecklistValue("좌 지시등", checklistLightValue(state.indicatorLeft, state.leftIndicatorChecked),
                                    state.indicatorLeft == true || state.leftIndicatorChecked == true,
                                    state.indicatorLeftSignal, commonSignal == null)
                                ChecklistValue("우 지시등", checklistLightValue(state.indicatorRight, state.rightIndicatorChecked),
                                    state.indicatorRight == true || state.rightIndicatorChecked == true,
                                    state.indicatorRightSignal, commonSignal == null)
                                ChecklistValue("비상등", checklistLightValue(state.hazard, state.hazardChecked),
                                    state.hazard == true || state.hazardChecked == true,
                                    state.hazardSignal, commonSignal == null)
                            }
                        }
                    } else {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            VehicleDiagram(state, Modifier.fillMaxSize())
                        }
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Eyebrow("조향각", color = CoachColors.Paper)
                            LessonText(state.steeringDeg?.let {
                                "${if (it > 0) "왼쪽" else if (it < 0) "오른쪽" else "중립"} ${abs(it).roundToInt()}°"
                            } ?: "미측정", 80, if (state.steeringDeg == null) CoachColors.Paper.copy(alpha = .6f)
                                else CoachColors.Paper, bold = true)
                            // Muted on the ink panel needs the same contrast treatment as the other on-ink labels.
                            steeringTurnsLabel(state.steeringDeg)?.let { LessonText(it, 32, CoachColors.Paper.copy(alpha = .6f)) }
                            if (commonSignal == null && state.steeringDeg != null) {
                                StateLabel(signalLabel(state.steeringSignal), state.steeringSignal, onInk = true)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    if (!locked) Eyebrow(if (checklist) "출발 전 점검 · 확인 상태" else "조향 방향 도식",
                        color = CoachColors.Paper.copy(alpha = .7f))
                }
                Row(Modifier.weight(.47f).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 96.dp, bottom = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top) {
                            LessonText("$taskTitle · ${state.attempt}회차", 40, modifier = Modifier.weight(1f))
                            val speedOffset = if (demo != null) (-16).dp else 0.dp
                            Column(Modifier.offset(y = speedOffset), horizontalAlignment = Alignment.End) {
                                LessonText("${state.speed} km/h", 56, bold = true)
                                commonSignal?.let { StateLabel(collapsedSignalLabel(it), it) }
                            }
                        }
                        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                            val guide = maneuverText(state.guideText)?.takeIf { it.isNotBlank() }
                            val spoken = maneuverText(subtitle)?.takeIf { it.isNotBlank() && it != state.hintText }
                            // Long subtitles have their own unbounded text block, separate from the three-line headline.
                            val main = guide ?: visibleHint ?: spoken?.takeIf { it.count { c -> c == '\n' } < 3 }
                            if (main != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Eyebrow("코치", Modifier.alignByBaseline())
                                    if (guide != null) state.guideStep?.let { Eyebrow(it, Modifier.alignByBaseline(), color = CoachColors.Periwinkle) }
                                }
                                Spacer(Modifier.height(24.dp))
                                Headline(main, Modifier.graphicsLayer {
                                    alpha = if (guide == null && visibleHint != null) hintAlpha.value else 1f
                                }, size = 72)
                            }
                            if (spoken != null && spoken != main) {
                                Spacer(Modifier.height(24.dp))
                                LessonText(spoken, 40)
                            }
                        }
                        if (!locked) {
                            if (!checklist) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                    SignalValue("기어", state.gear ?: "미측정", state.gearSignal, commonSignal == null,
                                        Modifier.weight(.8f))
                                    if (state.rearDistanceApplies) {
                                        Box(Modifier.width(2.dp).height(140.dp).background(CoachColors.Lavender))
                                        SignalValue(if (state.proximityAlert()) "가까워요" else "뒤 거리",
                                            state.rearDistanceCm?.let { "${it.roundToInt()} cm" } ?: "미측정",
                                            state.distanceSignal, commonSignal == null, Modifier.weight(1.4f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(48.dp))
                            if (stopped) {
                                FinishButton(state.askedDone, onFinish)
                            } else Spacer(Modifier.height(140.dp))
                        }
                    }
                }
            }
            // Locked frames have no panel, toggle, scrolling, or click actions.
            if (!locked && demo != null) DemoRail(expansion, demo)
        }
    }
}

@Composable
private fun ChecklistValue(label: String, value: String?, satisfied: Boolean, signal: SignalAvailability,
    showSource: Boolean, source: String = signalLabel(signal)) {
    val measured = value != null && signal != SignalAvailability.MISSING
    val background = when {
        !measured -> CoachColors.Lavender
        satisfied -> CoachColors.Periwinkle
        else -> CoachColors.Ink.copy(alpha = .6f)
    }
    val foreground = if (measured) CoachColors.Paper else CoachColors.Muted
    Column(Modifier.fillMaxWidth().height(240.dp).surfaceTexture(background.compositeOver(CoachColors.Lavender),
            if (measured) CoachTexture.SelectedCard else CoachTexture.Card)
        .padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(label, color = foreground)
        LessonText(if (measured) value!! else "미측정", 48, foreground, bold = true)
        if (showSource && (measured || source != "미측정")) {
            LessonText(source, 32, foreground)
        }
    }
}

@Composable
private fun SignalValue(label: String, value: String, signal: SignalAvailability, showSource: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(label)
        LessonText(value, if (value == "미측정") 56 else 80,
            if (value == "미측정") CoachColors.Muted else CoachColors.Ink, bold = true)
        if (showSource && value != "미측정") StateLabel(signalLabel(signal), signal)
    }
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
