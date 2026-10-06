package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        Row(Modifier.fillMaxSize().testTag("profile-onboarding")) {
            Column(Modifier.weight(.35f).fillMaxHeight().background(CoachColors.Ink)
                .padding(start = 100.dp, end = 64.dp, top = 64.dp, bottom = 80.dp)) {
                SetupBrandMark(onAdmin = onAdmin, onInk = true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Eyebrow("처음 뵙겠습니다", color = CoachColors.Paper.copy(alpha = .6f))
                    Spacer(Modifier.height(32.dp))
                    Headline("내 프로필을\n같이 채워요.", color = CoachColors.Paper)
                    Spacer(Modifier.height(40.dp))
                    LessonText("나머지는 연습하면서\n하나씩 천천히 물어볼게요.", 40, CoachColors.Paper)
                }
            }
            Column(Modifier.weight(.65f).fillMaxHeight().padding(64.dp)) {
                ProfileScreen(rows, onAnswer, onFinish, onboarding = onboarding)
            }
        }
    }
}

/** P2: the same five rows in onboarding and the home sheet; only the active row unfolds. */
@Composable
internal fun ColumnScope.ProfileScreen(rows: List<ProfileRow>, onAnswer: (ProfileField, String) -> Unit,
    onBack: () -> Unit, onboarding: ProfileOnboarding? = null, observedLines: List<String> = emptyList()) {
    val allowed = onboarding?.fields ?: ProfileField.entries
    var expanded by rememberSaveable { mutableStateOf(if (onboarding != null) rows.firstOrNull { it.field in allowed && it.answer == null }?.field?.name else null) }
    val complete = onboarding != null && rows.filter { it.field in allowed }.all { it.answer != null }
    Eyebrow("내 프로필")
    Spacer(Modifier.height(20.dp))
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("profile-rows")) {
        rows.forEach { row ->
            val open = expanded == row.field.name
            Column(Modifier.fillMaxWidth().testTag("profile-${row.field.name}")) {
                Row(Modifier.fillMaxWidth().height(80.dp)
                    .then(if (row.field in allowed) Modifier.clickable(role = Role.Button) {
                        expanded = if (open) null else row.field.name
                    } else Modifier), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    LessonText(row.field.title, 32)
                    LessonText(row.answer?.label ?: "아직", 32, CoachColors.Muted)
                }
                if (open) {
                    ProfileQuestion(row) { id ->
                        onAnswer(row.field, id)
                        expanded = rows.firstOrNull { it.field in allowed && it.field != row.field && it.answer == null }?.field?.name
                    }
                    Spacer(Modifier.height(24.dp))
                }
                PosterRule()
            }
        }
        Spacer(Modifier.height(28.dp))
        if (onboarding != null) {
            LessonText("나머지는 연습하면서", 36, CoachColors.Muted)
        } else {
            Eyebrow("앱이 본 것", color = CoachColors.Muted)
            Spacer(Modifier.height(16.dp))
            observedLines.forEach { LessonText(it, 32, CoachColors.Muted) }
        }
    }
    Spacer(Modifier.height(28.dp))
    if (onboarding != null) BottomActions(secondary = { TextAction("건너뛰기", onBack) },
        primary = if (complete) ({ PrimaryPill("시작하기", onBack) }) else null)
    else BottomActions(secondary = { BackPill(onBack) })
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
                    .surfaceTexture(if (chosen) CoachColors.Periwinkle else CoachColors.Lavender,
                        if (chosen) CoachTexture.SelectedChip else CoachTexture.Chip, pill = true)
                    .clickable(role = Role.RadioButton) { onAnswer(chip.id) }.semantics { selected = chosen }
                    .padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                    LessonText(chip.label, 36, if (chosen) CoachColors.Paper else CoachColors.Ink)
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
