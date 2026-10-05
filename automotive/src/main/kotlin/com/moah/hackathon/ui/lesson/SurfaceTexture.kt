package com.moah.hackathon.ui.lesson

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.moah.hackathon.ui.CoachColors
import com.moah.hackathon.ui.CoachTexture
import kotlin.math.ceil

/** A cached software mask keeps the specified blur and offset independent of Android elevation. */
internal fun Modifier.surfaceTexture(
    color: Color,
    texture: CoachTexture.Surface,
    pill: Boolean = false,
    pressed: () -> Boolean = { false },
    insetWhenPressed: Boolean = false,
    shape: Shape? = null,
): Modifier = drawWithCache {
    val blur = texture.blur.dp.toPx()
    val y = texture.y.dp.toPx()
    val margin = ceil(blur * 2 + y).toInt()
    val radius = if (pill) size.height / 2 else 0f
    val outline = Path().apply {
        when (val custom = shape?.createOutline(size, layoutDirection, this@drawWithCache)) {
            is Outline.Rounded -> addRoundRect(custom.roundRect)
            is Outline.Rectangle -> addRect(custom.rect)
            is Outline.Generic -> addPath(custom.path)
            null -> addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(radius)))
        }
    }
    val mask = Bitmap.createBitmap(ceil(size.width).toInt() + margin * 2,
        ceil(size.height).toInt() + margin * 2, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = texture.shadow.toArgb()
        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    }
    Canvas(mask).apply {
        if (shape == null) drawRoundRect(margin.toFloat(), margin + y, margin + size.width,
            margin + y + size.height, radius, radius, paint)
        else { translate(margin.toFloat(), margin + y); drawPath(outline.asAndroidPath(), paint) }
    }
    val shadow = mask.asImageBitmap()
    val top = CoachColors.Paper.copy(alpha = texture.highlight)
    val highlight = Brush.verticalGradient(
        0f to top, CoachTexture.HighlightFade to Color.Transparent, 1f to Color.Transparent,
        endY = (size.height * texture.highlightHeight).coerceAtLeast(1f))
    val inset = Brush.verticalGradient(listOf(Color.Transparent, CoachColors.Ink.copy(alpha = texture.inset)),
        startY = size.height - CoachTexture.InnerDepth.toPx(), endY = size.height)
    onDrawBehind {
        if (!insetWhenPressed || !pressed()) drawImage(shadow, Offset(-margin.toFloat(), -margin.toFloat()),
            alpha = if (pressed()) CoachTexture.PressedShadow else 1f)
        clipPath(outline) {
            drawRect(color)
            if (insetWhenPressed && pressed()) {
                drawRect(CoachColors.Periwinkle.copy(alpha = texture.inset))
                drawRect(Brush.verticalGradient(listOf(texture.shadow, Color.Transparent),
                    endY = CoachTexture.InnerDepth.toPx()))
            } else if (texture.highlightHeight > 0f) {
                val insetX = size.width * CoachTexture.HighlightInset
                drawRoundRect(highlight, Offset(insetX, 0f),
                    Size(size.width - insetX * 2, size.height * texture.highlightHeight),
                    CornerRadius(size.height * texture.highlightHeight))
                drawRect(inset)
            } else {
                drawRect(top, size = Size(size.width, CoachTexture.PanelHighlight.toPx()))
            }
        }
    }
}
