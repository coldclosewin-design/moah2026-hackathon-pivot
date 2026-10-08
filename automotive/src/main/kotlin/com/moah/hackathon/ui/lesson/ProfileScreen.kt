package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
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
        Column(Modifier.fillMaxSize().background(CoachColors.Paper).padding(84.dp).testTag("profile-onboarding")) {
            SetupBrandMark(onAdmin = onAdmin)
            Spacer(Modifier.height(40.dp))
            ProfileScreen(rows, onAnswer, onFinish, onboarding = onboarding)
        }
    }
}

/** S4: the five real statements stay on the white wall, with one answer row underneath. */
@Composable
internal fun ColumnScope.ProfileScreen(rows: List<ProfileRow>, onAnswer: (ProfileField, String) -> Unit,
    onBack: () -> Unit, onboarding: ProfileOnboarding? = null, observedLines: List<String> = emptyList(),
    profile: Profile? = null) {
    val allowed = onboarding?.fields ?: ProfileField.entries
    var expanded by rememberSaveable { mutableStateOf(if (onboarding != null) rows.firstOrNull { it.field in allowed && it.answer == null }?.field?.name else null) }
    val complete = onboarding != null && rows.filter { it.field in allowed }.all { it.answer != null }
    val parts = profile?.let(::profileLine)?.split(" · ")
    Eyebrow(if (onboarding == null) "내 프로필" else "처음 뵙겠습니다", color = CoachColors.Muted)
    Spacer(Modifier.height(28.dp))
    if (onboarding == null && parts != null) {
        Headline("${parts.getOrNull(1).orEmpty()} ${parts.firstOrNull().orEmpty()},", size = 132)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LessonText("목표는 ", 132, CoachColors.Muted, bold = true)
            LessonText(parts.getOrNull(2).orEmpty().removePrefix("목표: ") + ".", 132, bold = true)
        }
    } else Headline("내 프로필을 같이 채워요.", size = 104)
    Spacer(Modifier.height(32.dp))
    PosterRule(color = CoachColors.Ink)
    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth().testTag("profile-rows"), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        rows.forEach { row ->
            val active = row.field.name == expanded
            Column(Modifier.weight(1f).heightIn(min = 148.dp).testTag("profile-${row.field.name}")
                .clickable(role = Role.Button) { expanded = if (active) null else row.field.name }
                .semantics(mergeDescendants = true) {
                    contentDescription = "${row.field.title}, ${row.answer?.label ?: "아직"}, 바꾸기"
                    stateDescription = if (active) "펼침" else "접힘"
                }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LessonText(row.field.title, 34, CoachColors.Muted)
                LessonText((row.answer?.label ?: "아직") + " ▾", 51)
                if (active) Box(Modifier.fillMaxWidth().height(3.dp).background(CoachColors.Ink))
            }
        }
    }
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 32.dp)) {
        rows.firstOrNull { it.field.name == expanded && it.field in allowed }?.let { active ->
            val columnStart = (maxWidth + 32.dp) / rows.size * rows.indexOf(active)
            val chipStart = if (onboarding != null) 0.dp else columnStart.coerceAtMost((maxWidth - 1400.dp).coerceAtLeast(0.dp))
            Column(Modifier.padding(start = chipStart), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProfileQuestion(active, showQuestion = onboarding != null) { id ->
                    onAnswer(active.field, id)
                    expanded = if (onboarding != null) rows.firstOrNull { it.field in allowed && it.field != active.field && it.answer == null }?.field?.name else null
                }
                if (onboarding != null) TextAction("닫기", { expanded = null }, size = 32)
            }
        }
    }
    if (onboarding != null) {
        LessonText("나머지는 연습하면서", 32, CoachColors.Muted)
        BottomActions(secondary = { TextAction("건너뛰기", onBack) }, primary = if (complete) ({ PrimaryPill("시작하기", onBack) }) else null)
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(80.dp)) {
        BackPill(onBack)
        Canvas(Modifier.size(72.dp).background(CoachColors.Ink, RoundedCornerShape(20.dp))) {
            withTransform({ scale(size.width / 64f, size.height / 64f, Offset.Zero) }) {
                drawPath(Path().apply {
                    moveTo(12f, 32f); quadraticTo(32f, 7f, 52f, 32f)
                    quadraticTo(32f, 57f, 12f, 32f); close()
                }, CoachColors.Paper, style = Stroke(3.5f))
                drawCircle(CoachColors.Paper, 7f, Offset(32f, 32f))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("앱이 본 것", color = CoachColors.Muted)
            observedLines.forEach { LessonText(it, 36, CoachColors.Muted) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileQuestion(row: ProfileRow, showQuestion: Boolean = true, onAnswer: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (showQuestion) LessonText(row.field.question, 56)
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
