package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.*
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

@Composable
internal fun ProfileOnboardingScreen(rows: List<ProfileRow>, onboarding: ProfileOnboarding,
    onAnswer: (ProfileField, String) -> Unit, onFinish: () -> Unit, onAdmin: (() -> Unit)?) {
    PosterSurface {
        Column(Modifier.fillMaxSize().background(CoachColors.Lavender).padding(84.dp).testTag("profile-onboarding")) {
            SetupBrandMark(onAdmin = onAdmin)
            Spacer(Modifier.height(40.dp))
            ProfileScreen(rows, onAnswer, onFinish, onboarding = onboarding)
        }
    }
}

/** B1: the actual five fields read as a sentence; a word opens its answer card below. */
@Composable
internal fun ColumnScope.ProfileScreen(rows: List<ProfileRow>, onAnswer: (ProfileField, String) -> Unit,
    onBack: () -> Unit, onboarding: ProfileOnboarding? = null, observedLines: List<String> = emptyList()) {
    val allowed = onboarding?.fields ?: ProfileField.entries
    var expanded by rememberSaveable { mutableStateOf(if (onboarding != null) rows.firstOrNull { it.field in allowed && it.answer == null }?.field?.name else null) }
    val complete = onboarding != null && rows.filter { it.field in allowed }.all { it.answer != null }
    Eyebrow(if (onboarding == null) "내 프로필" else "처음 뵙겠습니다")
    if (onboarding != null) { Spacer(Modifier.height(20.dp)); LessonText("내 프로필을 같이 채워요.", 64, CoachColors.Muted) }
    Spacer(Modifier.height(28.dp))
    Box(Modifier.weight(1f).fillMaxWidth().testTag("profile-rows")) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            val visible = if (expanded != null) rows.filter { it.field.name == expanded } else if (onboarding != null) rows.filter { it.field in allowed } else rows
            visible.take(3).forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.testTag("profile-${row.field.name}")) {
                    LessonText(row.field.title + if (row.field in listOf(ProfileField.LICENSE, ProfileField.CAR)) "는" else "은", if (expanded == null) 82 else 64)
                    WordPill(row.answer?.label ?: "아직", "${row.field.title}, ${row.answer?.label ?: "아직"}, 바꾸기", if (expanded == null) 82 else 64,
                        chosen = expanded == row.field.name, placeholder = row.answer == null) { expanded = if (expanded == row.field.name) null else row.field.name }
                    LessonText(if (row.field == ProfileField.GOAL) "." else ",", 64, CoachColors.Muted)
                }
            }
            if (onboarding == null && expanded == null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                rows.drop(3).forEach { row ->
                    LessonText(row.field.title + "는", 48)
                    WordPill(row.answer?.label ?: "아직", "${row.field.title}, ${row.answer?.label ?: "아직"}, 바꾸기", 48,
                        chosen = expanded == row.field.name, placeholder = row.answer == null) { expanded = if (expanded == row.field.name) null else row.field.name }
                }
            }
        }
        val active = rows.firstOrNull { it.field.name == expanded && it.field in allowed }
        if (active != null) {
            SheetCard(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (onboarding != null) 440.dp else 390.dp)) {
                Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Eyebrow(active.field.title)
                        TextAction("닫기", { expanded = null }, size = 32)
                    }
                    ProfileQuestion(active) { id ->
                        onAnswer(active.field, id)
                        expanded = if (onboarding != null) rows.firstOrNull { it.field in allowed && it.field != active.field && it.answer == null }?.field?.name else null
                    }
                }
            }
        } else if (onboarding == null) {
            SheetCard(Modifier.align(Alignment.BottomEnd).width(1080.dp).height(390.dp)) {
                Row(Modifier.fillMaxSize().padding(40.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    YellowContextIcon(ContextIcon.Eye)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Eyebrow("앱이 본 것", color = CoachColors.Muted)
                        observedLines.forEach { LessonText(it, 36, CoachColors.Ink) }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    if (onboarding != null) {
        LessonText("나머지는 연습하면서", 32, CoachColors.Muted)
        BottomActions(secondary = { TextAction("건너뛰기", onBack) }, primary = if (complete) ({ PrimaryPill("시작하기", onBack) }) else null)
    } else BottomActions(secondary = { BackPill(onBack) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileQuestion(row: ProfileRow, onAnswer: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LessonText(row.field.question, 56)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            row.chips.forEach { chip ->
                val chosen = row.answer?.id == chip.id
                Box(Modifier.height(112.dp)
                    .surfaceTexture(if (chosen) CoachColors.Ink else CoachColors.Lavender,
                        if (chosen) CoachTexture.SelectedChip else CoachTexture.Chip, pill = true)
                    .clickable(role = Role.RadioButton) { onAnswer(chip.id) }.semantics { selected = chosen }
                    .padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                    LessonText(chip.label, 36, if (chosen) CoachColors.Paper else CoachColors.Periwinkle)
                }
            }
        }
    }
}

@Composable
internal fun ProfileAskCard(row: ProfileRow, onAnswer: (ProfileField, String) -> Unit, onSkip: (ProfileField) -> Unit) {
    Column(Modifier.fillMaxWidth().surfaceTexture(CoachColors.Lavender.copy(alpha = .4f), CoachTexture.Card)
        .padding(28.dp).testTag("profile-ask-one"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("하나만 물어볼게요")
            TextAction("다음에요", { onSkip(row.field) }, size = 32)
        }
        ProfileQuestion(row) { onAnswer(row.field, it) }
    }
}
