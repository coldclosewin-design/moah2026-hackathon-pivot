package com.moah.hackathon.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moah.hackathon.R
import com.moah.hackathon.feature.lesson.LessonMode
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.ui.CoachColors

@Composable
internal fun BriefingScreen(task: Task, mode: LessonMode, line: String, subtitle: String?) {
    PosterSurface {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(.57f).fillMaxHeight().padding(start = 180.dp, end = 100.dp, top = 64.dp, bottom = 100.dp)) {
                BrandMark()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Eyebrow("연습 준비", color = CoachColors.Periwinkle)
                    Spacer(Modifier.height(32.dp))
                    Headline(briefingHeadline(task.watch), size = 80)
                    Spacer(Modifier.height(40.dp))
                    LessonText("${task.title} · ${mode.label} 모드", 40)
                    Spacer(Modifier.height(20.dp))
                    LessonText("서두르지 않아도 괜찮아요.", 40, CoachColors.Muted)
                    Spacer(Modifier.height(36.dp))
                    LessonText(line, 32, CoachColors.Muted)
                    if (!subtitle.isNullOrBlank() && subtitle != line) {
                        Spacer(Modifier.height(16.dp))
                        LessonText(subtitle, 32, CoachColors.Muted)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Canvas(Modifier.size(80.dp, 72.dp)) {
                        listOf(.3f, .65f, 1f, .65f, .3f).forEachIndexed { index, fraction ->
                            val h = size.height * fraction
                            drawRect(CoachColors.Periwinkle, Offset(index * size.width / 5, (size.height - h) / 2), Size(size.width / 12, h))
                        }
                    }
                    Eyebrow("음성 안내 중", color = CoachColors.Periwinkle)
                }
            }
            Image(painterResource(R.drawable.poster_wheel_b), null, Modifier.weight(.43f).fillMaxHeight(), contentScale = ContentScale.Crop)
        }
    }
}
