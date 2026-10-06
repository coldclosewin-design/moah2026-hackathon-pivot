package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moah.hackathon.feature.lesson.LessonPhase
import com.moah.hackathon.feature.lesson.CoachInputMode
import com.moah.hackathon.feature.lesson.LessonViewModel
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

/** Only Setup supplies this action, and only when the model exposes admin controls. */
@Composable
internal fun SetupBrandMark(modifier: Modifier = Modifier, onAdmin: (() -> Unit)? = null, onInk: Boolean = false) {
    DemoBrandMark(modifier, onAdmin, onInk, "시연 준비실 열기")
}

/** The locked-screen gesture intentionally exposes no button or accessibility action. */
@Composable
internal fun DemoBrandMark(modifier: Modifier = Modifier, onHold: (() -> Unit)? = null,
    onInk: Boolean = false, actionLabel: String? = null) {
    val config = LocalViewConfiguration.current
    val adminConfig = remember(config) { object : ViewConfiguration by config { override val longPressTimeoutMillis = 2_000L } }
    CompositionLocalProvider(LocalViewConfiguration provides adminConfig) {
        BrandMark(modifier.then(if (onHold == null) Modifier else Modifier
            .then(if (actionLabel == null) Modifier else Modifier.semantics { onLongClick(actionLabel) { onHold(); true } })
            .pointerInput(onHold) { detectTapGestures(onLongPress = { onHold() }) }),
            if (onInk) CoachColors.Paper else CoachColors.Ink)
    }
}

@Composable
internal fun AdminHome(admin: LessonViewModel.AdminControls, setup: LessonPhase.Setup, onHome: () -> Unit) {
    val band by admin.bandVisible.collectAsStateWithLifecycle()
    val inputMode by admin.coachInput.collectAsStateWithLifecycle()
    val profileId by admin.profileId.collectAsStateWithLifecycle()
    val auth = admin.demo.aiState?.collectAsStateWithLifecycle()?.value
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    PosterSurface {
        Row(Modifier.fillMaxSize().testTag("admin-home")) {
            Column(Modifier.weight(.32f).fillMaxHeight().background(CoachColors.Ink)
                .padding(start = 100.dp, end = 64.dp, top = 64.dp, bottom = 80.dp)) {
                BrandMark(color = CoachColors.Paper)
                Spacer(Modifier.height(96.dp))
                Eyebrow("관리자 모드", color = CoachColors.Paper.copy(alpha = .6f))
                Spacer(Modifier.height(24.dp))
                Headline("시연 준비", color = CoachColors.Paper)
                Spacer(Modifier.height(32.dp))
                LessonText("프리셋을 고르고\n홈에서 연습을 시작해요.", 40, CoachColors.Paper)
                Spacer(Modifier.weight(1f))
                Eyebrow("지금", color = CoachColors.Paper.copy(alpha = .6f))
                Spacer(Modifier.height(24.dp))
                LessonText(aiLine(auth)?.title ?: "AI 코치 · 규칙 문장", 32, CoachColors.Paper)
                LessonText("신호 출처 · ${admin.signalSource}", 32, CoachColors.Paper)
                LessonText("기록 · ${setup.profile.observation.attempts}회", 32, CoachColors.Paper)
                LessonText(profileLine(setup.profile), 32, CoachColors.Paper)
                LessonText("음성 입력 · ${coachInputLabel(inputMode)}", 32, CoachColors.Paper)
            }
            Column(Modifier.weight(.68f).fillMaxHeight().padding(64.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Eyebrow("시연 프리셋")
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        admin.presets.forEach { preset ->
                            val chosen = selectedPreset == preset.id
                            Column(Modifier.weight(1f).height(230.dp)
                                .surfaceTexture(if (chosen) CoachColors.Periwinkle else CoachColors.Lavender,
                                    if (chosen) CoachTexture.SelectedCard else CoachTexture.Card)
                                .clickable(role = Role.RadioButton) { selectedPreset = preset.id; admin.applyPreset(preset.id) }
                                .semantics { selected = chosen }.padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                LessonText(preset.title, 36, if (chosen) CoachColors.Paper else CoachColors.Ink)
                                LessonText(preset.subtitle, 28, if (chosen) CoachColors.Paper else CoachColors.Muted)
                            }
                        }
                    }
                    Eyebrow("프로필")
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        admin.profilePresets.forEach { preset ->
                            SelectionChip(preset.label, profileId == preset.id && setup.profile.statement == preset.profile.statement, { admin.setProfile(preset.id) }, Modifier.weight(1f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Eyebrow("신호 출처")
                            LessonText(admin.signalSource, 32, CoachColors.Muted)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Eyebrow("세션 중 패널")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SelectionChip("아래 띠", band, { admin.setBand(true) }, Modifier.weight(1f))
                                SelectionChip("숨김", !band, { admin.setBand(false) }, Modifier.weight(1f))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow("시뮬레이션 음성 입력", Modifier.weight(1f))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CoachInputMode.entries.forEach { mode ->
                                SelectionChip(coachInputLabel(mode), inputMode == mode,
                                    { admin.setCoachInput(mode) }, Modifier.weight(1f))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                        TextAction("기록 초기화", admin::resetRecords)
                        TextAction("프로필 초기화", admin::resetProfile)
                    }
                    AdminAiDetail(auth, admin.demo::connectAi)
                }
                BottomActions(primary = { PrimaryPill("이 설정으로 홈", onHome) })
            }
        }
    }
}

private fun coachInputLabel(mode: CoachInputMode) = when (mode) {
    CoachInputMode.OFF -> "끔"
    CoachInputMode.CARDS -> "카드"
    CoachInputMode.CARDS_AND_TEXT -> "카드 + 글"
}
