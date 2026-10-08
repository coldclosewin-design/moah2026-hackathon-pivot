package com.moah.hackathon.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.moah.hackathon.ui.CoachColors

/** V4: rear-up rounded body, one cabin and a front marker; shared by all four diagrams. */
internal fun DrawScope.vehicleSilhouette(bodyColor: Color, panelColor: Color, glassColor: Color, outlineWidth: Float = 8f) {
    drawRoundRect(glassColor, Offset(5f, 0f), Size(90f, 250f), CornerRadius(40f, 40f))
    drawRoundRect(bodyColor, Offset(5f, 0f), Size(90f, 250f), CornerRadius(40f, 40f), style = Stroke(outlineWidth))
    drawRoundRect(bodyColor, Offset(23f, 55f), Size(54f, 110f), CornerRadius(16f, 16f), style = Stroke(outlineWidth))
    drawLine(bodyColor, Offset(26f, 207f), Offset(74f, 207f), outlineWidth, StrokeCap.Round)
}

@Composable
internal fun HomeVehicle(modifier: Modifier = Modifier) {
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(Unit) { arrival.animateTo(1f, tween(900, easing = CoachMotion.Fill)) }
    Canvas(modifier) {
        withTransform({
            translate((arrival.value - 1f) * size.width, 0f)
            scale(size.width / 290f, size.height / 100f, Offset.Zero)
        }) {
            val ink = CoachColors.Ink
            val stroke = Stroke(3.2f, cap = StrokeCap.Round)
            drawRoundRect(ink, Offset(10f,40f), Size(262f,34f), CornerRadius(17f), style = stroke)
            drawPath(Path().apply { moveTo(74f,40f); lineTo(98f,16f); lineTo(180f,16f); lineTo(206f,40f) }, ink, style = stroke)
            drawLine(ink, Offset(140f,16f), Offset(140f,40f), 3.2f)
            listOf(74f,210f).forEach { x ->
                val center = Offset(x,74f)
                drawCircle(CoachColors.Paper,16f,center)
                drawCircle(ink,16f,center,style=stroke)
                rotate(arrival.value * 360f,center) { drawLine(ink,center,Offset(x,65f),3.2f,StrokeCap.Round) }
                drawCircle(ink,3f,center)
            }
        }
    }
}
