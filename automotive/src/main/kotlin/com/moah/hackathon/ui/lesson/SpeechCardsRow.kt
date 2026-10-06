package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moah.hackathon.feature.lesson.SpeechCard
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

/** One spoken sentence per card; the separate square choices still navigate directly. */
@Composable
internal fun SpeechCardsRow(cards: List<SpeechCard>, waiting: Boolean, onSend: (String) -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(cards.map { it.id }) { scroll.scrollTo(0) }
    Box(Modifier.fillMaxWidth().height(112.dp).testTag("coach-speech-cards")) {
        Row(Modifier.fillMaxSize().horizontalScroll(scroll).testTag("speech-cards-scroll")
            .padding(top = 6.dp, bottom = 10.dp, end = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            cards.forEach { card ->
                Row(Modifier.height(96.dp).testTag("speech-card-${card.id}")
                    .alpha(if (waiting) .45f else 1f)
                    .surfaceTexture(CoachColors.Paper, CoachTexture.Chip, pill = true)
                    .border(1.5.dp, CoachColors.Periwinkle, RoundedCornerShape(100))
                    .clip(RoundedCornerShape(100))
                    .clickable(enabled = !waiting, role = Role.Button) { onSend(card.id) }
                    .padding(horizontal = 30.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(32.dp)) {
                        val ink = CoachColors.Periwinkle
                        val stroke = 2.5.dp.toPx()
                        drawRoundRect(ink, Offset(size.width * .34f, 0f), Size(size.width * .32f, size.height * .6f),
                            CornerRadius(size.width * .16f), style = Stroke(stroke))
                        drawArc(ink, 0f, 180f, false, Offset(size.width * .12f, size.height * .2f),
                            Size(size.width * .76f, size.height * .6f), style = Stroke(stroke, cap = StrokeCap.Round))
                        drawLine(ink, Offset(size.width / 2, size.height * .8f), Offset(size.width / 2, size.height), stroke)
                        drawLine(ink, Offset(size.width * .3f, size.height), Offset(size.width * .7f, size.height), stroke)
                    }
                    LessonText(card.text, 40, maxLines = 1)
                }
            }
        }
        if (scroll.canScrollForward) Box(Modifier.align(Alignment.CenterEnd).width(88.dp).fillMaxHeight()
            .testTag("speech-cards-more").background(Brush.horizontalGradient(
                listOf(CoachColors.Paper.copy(alpha = 0f), CoachColors.Paper))), contentAlignment = Alignment.CenterEnd) {
            LessonText("›", 40, CoachColors.Periwinkle, modifier = Modifier.padding(end = 8.dp))
        }
    }
}
