package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moah.hackathon.feature.lesson.CoachChoice
import com.moah.hackathon.feature.lesson.CoachDialog
import com.moah.hackathon.feature.lesson.CoachInputMode
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

/** The transcript alone scrolls; choices, composer and return stay within the sheet. */
@Composable
internal fun CoachTextSheet(coach: CoachDialog, onChoose: (CoachChoice) -> Unit, onBack: () -> Unit,
    onSend: (String) -> Unit, inputMode: CoachInputMode = CoachInputMode.CARDS_AND_TEXT,
    onSendCard: (String) -> Unit = {}) {
    // Preserve the IME's composition/selection until submission; never save conversation drafts.
    var draft by remember { mutableStateOf(TextFieldValue()) }
    val scroll = rememberScrollState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val send = {
        if (!coach.waiting && draft.text.isNotBlank()) {
            val text = draft.text.trim()
            // Finishing focus can synchronously commit the IME's composing text.
            // Clear afterwards so that final commit cannot restore the submitted draft.
            focus.clearFocus()
            keyboard?.hide()
            draft = TextFieldValue()
            onSend(text)
        }
    }
    LaunchedEffect(coach.turns, coach.waiting, scroll.maxValue, scroll.viewportSize) {
        scroll.scrollTo(scroll.maxValue)
    }
    DisposableEffect(Unit) {
        onDispose { focus.clearFocus(); keyboard?.hide() }
    }
    Column(Modifier.fillMaxSize().testTag("coach-text-sheet")) {
        Eyebrow("코치와 대화")
        Spacer(Modifier.height(27.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().testTag("coach-transcript").verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                CoachMessage(coach.line)
                coach.turns.forEach { turn ->
                    CoachMessage(turn.text, turn.fromDriver,
                        Modifier.align(if (turn.fromDriver) Alignment.End else Alignment.Start))
                }
                if (coach.waiting) CoachMessage("…", modifier = Modifier.testTag("coach-waiting"))
            }
            if (scroll.value > 0) Box(Modifier.fillMaxWidth().height(36.dp).align(Alignment.TopCenter)
                .testTag("coach-transcript-fade").background(Brush.verticalGradient(
                    listOf(CoachColors.Paper, CoachColors.Paper.copy(alpha = 0f)))))
        }
        Spacer(Modifier.height(33.dp))
        if (coach.cards.isEmpty()) {
            CoachChoices(coach.choices, onChoose)
            Spacer(Modifier.height(18.dp))
            LessonText("고르면 바로 그 자리로 가요.", 32, CoachColors.Muted)
            Spacer(Modifier.height(24.dp))
        }
        if (coach.cards.isNotEmpty()) {
            SpeechCardsRow(coach.cards, coach.waiting, onSendCard)
            if (inputMode == CoachInputMode.CARDS_AND_TEXT) Spacer(Modifier.height(18.dp))
        }
        if (inputMode == CoachInputMode.CARDS_AND_TEXT) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(draft, { draft = it }, Modifier.weight(1f).height(140.dp)
                .testTag("coach-text-input").background(CoachColors.Lavender, RoundedCornerShape(36.dp))
                .padding(horizontal = 28.dp, vertical = 24.dp),
                enabled = !coach.waiting, singleLine = true,
                textStyle = TextStyle(color = CoachColors.Ink, fontSize = 48.sp, localeList = LocaleList("ko-KR")),
                cursorBrush = SolidColor(CoachColors.Periwinkle),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                decorationBox = { field ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (draft.text.isEmpty()) LessonText("코치에게 글로 말해 보세요", 48, CoachColors.Muted, maxLines = 1)
                        field()
                    }
                })
            PrimaryPill("보내기", send, Modifier.width(260.dp), enabled = !coach.waiting && draft.text.isNotBlank())
        }
        Spacer(Modifier.height(24.dp))
        BottomActions(secondary = { BackPill(onBack) })
    }
}

@Composable
private fun CoachMessage(text: String, fromDriver: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(if (fromDriver) .88f else 1f)
        .surfaceTexture(if (fromDriver) CoachColors.Periwinkle else CoachColors.Lavender, CoachTexture.Card,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp,
                bottomEnd = if (fromDriver) 9.dp else 36.dp, bottomStart = if (fromDriver) 36.dp else 9.dp))
        .padding(horizontal = 36.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!fromDriver) Eyebrow("코치")
        if (fromDriver) LessonText(text, 48, CoachColors.Paper)
        else CoachLines(text, titleSize = 52, adviceSize = 44)
    }
}
