@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.moah.hackathon.ui.lesson

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

internal val LocalHomeShared = staticCompositionLocalOf<SharedTransitionScope?> { null }
internal val LocalHomeVisibility = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
internal val LocalCoachOpening = staticCompositionLocalOf { true }

/** T2/B2 keep the actual title words and ticket as the shared objects, in either direction. */
@Composable
internal fun Modifier.homeShared(key: String): Modifier {
    val shared = LocalHomeShared.current ?: return this
    val visibility = LocalHomeVisibility.current ?: return this
    val coachOpening = LocalCoachOpening.current
    return with(shared) {
        this@homeShared.sharedBounds(rememberSharedContentState(key), visibility,
            resizeMode = if (key in listOf("home-start", "coach-surface", "venue-paper") || key.startsWith("scope-")) SharedTransitionScope.ResizeMode.RemeasureToBounds else SharedTransitionScope.ResizeMode.ScaleToBounds(),
            boundsTransform = { _, _ -> when {
                key == "coach-surface" -> tween(if (coachOpening) 450 else 350, easing = if (coachOpening) CoachMotion.Shared else CubicBezierEasing(.3f, 0f, .8f, .15f))
                key.startsWith("scope-") -> spring(.82f, 300f)
                key == "home-start" -> tween(440, delayMillis = 120, easing = CoachMotion.Shared)
                else -> tween(560, easing = CoachMotion.Shared)
            } },
            enter = when (key) {
                "coach-surface", "venue-paper" -> EnterTransition.None
                "home-start" -> fadeIn(tween(200, delayMillis = 360))
                else -> if (key.startsWith("scope-")) EnterTransition.None else fadeIn(tween(400))
            },
            exit = fadeOut(tween(if (key == "coach-surface" || key == "venue-paper" || key.startsWith("scope-")) 1 else 200)))
    }
}

/** The shared paper stays opaque; only its words wait for the last part of the morph. */
@Composable
internal fun Modifier.coachTextArrival(): Modifier {
    val opening = LocalCoachOpening.current
    return sharedTextArrival(if (opening) 230 else 250, if (opening) 220 else 100)
}

@Composable
internal fun Modifier.sharedTextArrival(delay: Int, duration: Int): Modifier {
    val visibility = LocalHomeVisibility.current ?: return this
    val opacity by visibility.transition.animateFloat(transitionSpec = {
        if (targetState == EnterExitState.Visible) tween(duration, delayMillis = delay)
        else tween(100)
    }, label = "shared-words") { if (it == EnterExitState.Visible) 1f else 0f }
    return graphicsLayer { alpha = opacity }
}
