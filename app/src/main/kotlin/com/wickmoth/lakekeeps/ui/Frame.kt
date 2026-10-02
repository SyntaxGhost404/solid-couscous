package com.wickmoth.lakekeeps.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

/** Every screen is authored on a fixed 360 x 800 dp frame: the 9:20 reference screen. */
object Frame {
    const val W = 360f
    const val H = 800f
}

/** Size of the game window in pixels, provided once at the root. */
val LocalViewport = compositionLocalOf { Size.Zero }

/**
 * Where the design frame lands in the window: [unit] pixels per design dp, offset by [left]/[top].
 * The frame is scaled uniformly to fit, so art never stretches; screens paint full-bleed
 * backgrounds around it on other aspect ratios.
 */
@Immutable
data class FrameFit(val unit: Float, val left: Float, val top: Float) {
    fun toWindow(x: Float, y: Float) = Offset(left + x * unit, top + y * unit)

    companion object {
        fun of(viewport: Size, alignment: Alignment): FrameFit {
            if (viewport.width <= 0f || viewport.height <= 0f) return FrameFit(1f, 0f, 0f)
            val unit = min(viewport.width / Frame.W, viewport.height / Frame.H)
            val frame = IntSize((Frame.W * unit).roundToInt(), (Frame.H * unit).roundToInt())
            val window = IntSize(viewport.width.roundToInt(), viewport.height.roundToInt())
            val at = alignment.align(frame, window, LayoutDirection.Ltr)
            return FrameFit(unit, at.x.toFloat(), at.y.toFloat())
        }
    }
}

@Composable
fun rememberFrameFit(alignment: Alignment): FrameFit {
    val viewport = LocalViewport.current
    return remember(viewport, alignment) { FrameFit.of(viewport, alignment) }
}

/**
 * Lays out [content] on the design frame. Inside, 1.dp is one design unit and text ignores the
 * system font scale, because the lettering is part of the artwork.
 */
@Composable
fun DesignFrame(fit: FrameFit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalDensity provides Density(fit.unit, fontScale = 1f)) {
        Box(
            modifier
                .offset { IntOffset(fit.left.roundToInt(), fit.top.roundToInt()) }
                .size(Frame.W.dp, Frame.H.dp),
            content = content,
        )
    }
}
