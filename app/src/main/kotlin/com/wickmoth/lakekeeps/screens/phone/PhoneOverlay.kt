package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.screens.board.DeskPhone
import com.wickmoth.lakekeeps.screens.board.PhoneShape
import com.wickmoth.lakekeeps.screens.board.deskPhone
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.LocalViewport
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.window

/** How far a back gesture has been dragged and from which edge, for predictive back. */
@Immutable
data class BackPeek(val progress: Float, val fromLeft: Boolean)

/**
 * The lifted phone. At [lift] = 0 it sits exactly on the desk, rotated and small, its screen off;
 * at 1 its screen fills the window. The casing scales with it and slides off past the edges.
 */
@Composable
fun PhoneOverlay(
    owner: Owner,
    messages: Messages,
    boardFit: FrameFit,
    lift: () -> Float,
    content: () -> Float,
    back: () -> BackPeek,
    onBack: () -> Unit,
) {
    val viewport = LocalViewport.current
    val phone = deskPhone(owner)
    val geometry = remember(phone, boardFit, viewport) { LiftGeometry(phone, boardFit, viewport) }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val e = lift()
                val peek = back()
                val g = geometry.at(e)
                val backScale = 1f - 0.1f * peek.progress
                scaleX = g.scale * backScale
                scaleY = g.scale * backScale
                rotationZ = g.rotation
                val shift = (if (peek.progress > 0f) (if (peek.fromLeft) 1f else -1f) else 0f) * 18f * density * peek.progress
                translationX = g.center.x - viewport.width / 2f + shift
                translationY = g.center.y - viewport.height / 2f
                transformOrigin = TransformOrigin.Center
            },
    ) {
        // Casing and its shadow, drawn around the visible screen in the phone's own frame.
        Canvas(Modifier.fillMaxSize()) {
            val e = lift()
            val g = geometry.at(e)
            val unit = boardFit.unit / g.scale
            val bezel = PhoneShape.BEZEL * unit
            val body = Size(g.visible.width + bezel * 2, g.visible.height + bezel * 2)
            val topLeft = Offset((size.width - body.width) / 2f, (size.height - body.height) / 2f)
            val raised = window(e, 0f, 0.5f)
            val shadowDrop = Offset(2f, 6f + 22f * raised) * unit
            for (k in 3 downTo 1) {
                val grow = (2f + 8f * raised) * unit * k / 3f
                drawRoundRect(
                    Color.Black,
                    topLeft = topLeft + shadowDrop - Offset(grow, grow),
                    size = Size(body.width + grow * 2, body.height + grow * 2),
                    cornerRadius = CornerRadius(PhoneShape.BODY_CORNER * unit + grow),
                    alpha = (0.18f - 0.045f * k) * (1f - window(e, 0.7f, 0.3f)),
                )
            }
            drawRoundRect(phone.casing, topLeft = topLeft, size = body, cornerRadius = CornerRadius(PhoneShape.BODY_CORNER * unit))
            drawRoundRect(
                phone.edge,
                topLeft = topLeft,
                size = body,
                cornerRadius = CornerRadius(PhoneShape.BODY_CORNER * unit),
                style = Stroke(width = 0.9f * unit),
                alpha = 0.8f,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val e = lift()
                    val g = geometry.at(e)
                    val peek = back()
                    val radius = (lerp(PhoneShape.SCREEN_CORNER * boardFit.unit, 0f, e) + 26f * density * peek.progress) / g.scale
                    shape = VisibleWindow(g.visible, radius)
                    clip = true
                }
                .drawWithContent {
                    drawContent()
                    val e = lift()
                    // the screen wakes as the phone comes up, and sleeps as it goes back down
                    val dark = 1f - window(e, 0.03f, 0.4f)
                    if (dark > 0f) drawRect(Color(0xFF07090B), alpha = dark)
                    val glare = 1f - window(e, 0.2f, 0.5f)
                    if (glare > 0f) drawRect(Color.White, alpha = 0.06f * glare)
                },
        ) {
            PhoneScreen(owner, messages, content, onPutDown = onBack)
        }
    }
}

/** A centred rounded window of [visible] size inside the overlay's full-screen bounds. */
private class VisibleWindow(private val visible: Size, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val left = (size.width - visible.width) / 2f
        val top = (size.height - visible.height) / 2f
        return Outline.Rounded(RoundRect(left, top, left + visible.width, top + visible.height, CornerRadius(radius)))
    }
}

/**
 * Interpolates between the phone's pose on the desk and full screen. The overlay keeps the window's
 * size and is scaled uniformly; only its visible window changes aspect, so nothing stretches.
 */
private class LiftGeometry(phone: DeskPhone, fit: FrameFit, private val viewport: Size) {
    class Pose(val center: Offset, val scale: Float, val rotation: Float, val visible: Size)

    private val start = fit.toWindow(phone.center.x, phone.center.y)
    private val rotation0 = phone.rotation
    private val screen0 = Size(PhoneShape.screen.width * fit.unit, PhoneShape.screen.height * fit.unit)
    private val visible0: Size = run {
        val aspect = screen0.width / screen0.height
        if (aspect <= viewport.width / viewport.height) Size(viewport.height * aspect, viewport.height)
        else Size(viewport.width, viewport.width / aspect)
    }

    fun at(e: Float): Pose {
        val visible = Size(lerp(visible0.width, viewport.width, e), lerp(visible0.height, viewport.height, e))
        val onScreenHeight = lerp(screen0.height, viewport.height, e)
        val center = Offset(lerp(start.x, viewport.width / 2f, e), lerp(start.y, viewport.height / 2f, e))
        return Pose(center, onScreenHeight / visible.height, lerp(rotation0, 0f, e), visible)
    }
}
