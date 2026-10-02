package com.wickmoth.lakekeeps.screens.office

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.LocalViewport
import com.wickmoth.lakekeeps.ui.lerp

/**
 * A desk tool opening out of the thing it was opened from ([from], in window pixels) to fill the
 * screen as [progress] runs to 1, its pages fading in as it grows.
 */
@Composable
internal fun DeskToolWindow(state: GameState, tool: DeskTool, from: Rect, fit: FrameFit, progress: () -> Float, onClose: () -> Unit) {
    val viewport = LocalViewport.current
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val e = progress().coerceIn(0f, 1f)
                shape = Window(lerpRect(from, Rect(Offset.Zero, viewport), e), lerp(12f * fit.unit, 0f, e))
                clip = true
                alpha = (e * 3f).coerceAtMost(1f)
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val e = progress()
                    alpha = toolContent(e)
                    val s = lerp(0.96f, 1f, e)
                    scaleX = s
                    scaleY = s
                },
        ) {
            when (tool) {
                is DeskTool.Notebook -> Notebook(state, fit, tool.page, onClose)
                is DeskTool.Files -> Files(state, fit, tool.evidence, onClose)
            }
        }
    }
}

private fun lerpRect(a: Rect, b: Rect, f: Float) =
    Rect(lerp(a.left, b.left, f), lerp(a.top, b.top, f), lerp(a.right, b.right, f), lerp(a.bottom, b.bottom, f))

/** Clips a full-window layer to [rect] (in the layer's own pixels) with rounded corners. */
private class Window(private val rect: Rect, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(rect, CornerRadius(radius)))
}
