package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moah.hackathon.scoring.ParkingVerdict
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture

@Composable
internal fun VerdictPanel(verdict: ParkingVerdict?, modifier: Modifier = Modifier, title: String? = null, grid: Boolean = false, dark: Boolean = true, compact: Boolean = false, stagger: Boolean = false) {
    val ink = if (dark) CoachColors.Paper else CoachColors.Ink
    Column(modifier.background(if (dark) CoachColors.Ink else CoachColors.Paper).padding(start = if (compact) 0.dp else if (grid) 64.dp else 120.dp,
        end = if (compact) 0.dp else if (grid) 64.dp else 72.dp, top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (title != null) Eyebrow(title, color = ink.copy(alpha = .6f))
        verdictLines(verdict).chunked(if (grid) 2 else 1).forEachIndexed { row, lines ->
            val visible = remember { Animatable(if (stagger) 0f else 1f) }
            LaunchedEffect(Unit) { if (stagger) { delay(row * 160L); visible.animateTo(1f, tween(420)) } }
            Row(Modifier.fillMaxWidth().graphicsLayer { alpha = visible.value; translationY = (1f - visible.value) * 24.dp.toPx() }, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                lines.forEach { line ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            LessonText(line.text, 40, ink)
                            line.note?.let { LessonText(it, 32, ink.copy(alpha = .6f)) }
                        }
                        // A light backing keeps an unmeasured mark legible on Ink.
                        Box(Modifier.size(56.dp).then(if (line.mark == VerdictMark.MISSING)
                            Modifier.background(CoachColors.Lavender) else Modifier), contentAlignment = Alignment.Center) {
                            if (line.mark == VerdictMark.PASS) Box {
                                SymbolTile(CoachSymbol.Check, Modifier.size(56.dp), tile = false, onLight = !dark)
                                LessonText(line.mark.symbol, 40, androidx.compose.ui.graphics.Color.Transparent)
                            } else LessonText(line.mark.symbol, 40, when (line.mark) {
                                VerdictMark.PASS -> CoachColors.Periwinkle
                                VerdictMark.CAUTION -> ink.copy(alpha = .6f)
                                VerdictMark.FAIL -> CoachColors.Signal
                                VerdictMark.MISSING -> CoachColors.Muted
                            })
                        }
                    }
                }
            }
        }
    }
}
