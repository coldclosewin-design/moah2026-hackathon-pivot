@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.moah.hackathon.ui.lesson

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

internal val LocalHomeShared = staticCompositionLocalOf<SharedTransitionScope?> { null }
internal val LocalHomeVisibility = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** T2/B2 keep the actual title words and ticket as the shared objects, in either direction. */
@Composable
internal fun Modifier.homeShared(key: String): Modifier {
    val shared = LocalHomeShared.current ?: return this
    val visibility = LocalHomeVisibility.current ?: return this
    return with(shared) {
        this@homeShared.sharedBounds(rememberSharedContentState(key), visibility,
            resizeMode = if (key == "home-start") SharedTransitionScope.ResizeMode.RemeasureToBounds else SharedTransitionScope.ResizeMode.ScaleToBounds(),
            boundsTransform = { _, _ -> if (key == "home-start") tween(440, delayMillis = 120, easing = CoachMotion.Shared) else tween(560, easing = CoachMotion.Shared) },
            enter = if (key == "home-start") fadeIn(tween(200, delayMillis = 360)) else fadeIn(tween(400)),
            exit = if (key == "home-start") fadeOut(tween(200, delayMillis = 360)) else fadeOut(tween(200)))
    }
}
