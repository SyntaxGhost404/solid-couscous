package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch

/**
 * The phone's status bar, on every screen of both phones: the diamond back to the case board, the
 * clock and notification icons, the owner's name on the home screen, and wifi and battery. Pull it
 * down (or tap it) for the notification shade.
 */
@Composable
internal fun BoxScope.StatusBar(
    owner: String,
    clock: String,
    notices: Int,
    content: () -> Float,
    onHome: () -> Float,
    diamondEnabled: Boolean,
    onDiamond: () -> Unit,
    onShadeDrag: (Float) -> Unit,
    onShadeRelease: (Float) -> Unit,
    onShadeTap: () -> Unit,
) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val audio = LocalAudio.current
    val shown = { window(content() * ICONS_MS, 0f, 260f, Ease.OutCubic) }
    // The diamond fades during one half of this and the clock slides into its place in the other.
    val diamond by animateFloatAsState(if (diamondEnabled) 1f else 0f, tween(320), label = "diamond")
    val diamondShown = { window(diamond, 0.5f, 0.5f) }
    val shadeLabel = stringResource(R.string.shade_open)

    // Pull-down strip: everything right of the diamond.
    Box(
        Modifier
            .offset(56.dp, 0.dp)
            .size(304.dp, 86.dp)
            .semantics {
                contentDescription = shadeLabel
                role = Role.Button
                onClick { onShadeTap(); true }
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> onShadeDrag(delta) },
                onDragStopped = { velocity -> onShadeRelease(velocity) },
            )
            .pointerInput(Unit) { detectTapGestures { onShadeTap() } },
    )

    // The diamond takes the player back to the case board.
    Canvas(
        Modifier
            .offset(9.dp, 37.dp)
            .size(44.dp, 44.dp)
            .graphicsLayer {
                alpha = shown() * diamondShown()
                val s = (1f - 0.18f * press.value) * (0.7f + 0.3f * diamondShown())
                scaleX = s
                scaleY = s
                rotationZ = 90f * press.value
            }
            .tactile(
                label = stringResource(R.string.back_to_board),
                enabled = diamondEnabled,
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 600f)) } },
            ) {
                if (!diamondEnabled) return@tactile
                view.haptic(Haptic.Tick)
                audio.play(Sfx.Tap, 0.6f)
                onDiamond()
            },
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val w = 6.2f * density
        val h = 8.2f * density
        val path = Path().apply {
            moveTo(c.x, c.y - h)
            lineTo(c.x + w, c.y)
            lineTo(c.x, c.y + h)
            lineTo(c.x - w, c.y)
            close()
        }
        drawPath(path, Color.White, style = Stroke(width = 1.7f * density, join = StrokeJoin.Round))
        drawPath(path, Color.White, alpha = 0.35f * press.value)
    }

    // Clock, then an icon while notifications are waiting.
    val bell by animateFloatAsState(if (notices > 0) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "notice")
    Row(
        Modifier
            .offset { IntOffset((48f - 22f * window(1f - diamond, 0.5f, 0.5f, Ease.InOutSine)).dp.roundToPx(), 50.dp.roundToPx()) }
            .graphicsLayer { alpha = shown() }
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = clock,
            style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.92f)),
            softWrap = false,
        )
        Spacer(Modifier.width(6.dp))
        GlyphIcon(
            Glyph.Message,
            Color.White,
            Modifier
                .size(12.dp)
                .graphicsLayer {
                    scaleX = bell
                    scaleY = bell
                },
            alpha = { bell.coerceIn(0f, 1f) * 0.9f },
        )
    }

    BasicText(
        text = owner,
        style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.92f)),
        softWrap = false,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = 51.dp)
            .graphicsLayer {
                alpha = shown() * onHome()
                translationY = ((1f - shown()) * -4f).dp.toPx()
            },
    )

    Canvas(
        Modifier
            .offset(292.dp, 50.dp)
            .size(48.dp, 18.dp)
            .graphicsLayer { alpha = shown() },
    ) {
        val dp = density
        val cy = 9f * dp
        // wifi: a dot and three arcs, each arc lighting up in turn as the screen wakes
        val wifi = Offset(10f * dp, cy + 4.6f * dp)
        drawCircle(Color.White, radius = 1.3f * dp, center = wifi)
        for (k in 1..3) {
            val r = (2.6f + k * 2.6f) * dp
            val a = window(content() * ICONS_MS, 60f + k * 60f, 140f)
            drawArc(
                Color.White,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(wifi.x - r, wifi.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 1.5f * dp, cap = StrokeCap.Round),
                alpha = a,
            )
        }
        // battery
        val left = 26f * dp
        val bw = 17f * dp
        val bh = 9.4f * dp
        val top = cy - bh / 2f
        drawRoundRect(Color.White, topLeft = Offset(left, top), size = Size(bw, bh), cornerRadius = CornerRadius(2.4f * dp), style = Stroke(width = 1.3f * dp))
        drawRoundRect(Color.White, topLeft = Offset(left + bw + 0.8f * dp, cy - 2f * dp), size = Size(1.6f * dp, 4f * dp), cornerRadius = CornerRadius(0.8f * dp))
        for (k in 0 until 3) {
            val a = window(content() * ICONS_MS, 120f + k * 70f, 120f)
            drawRoundRect(
                Color.White,
                topLeft = Offset(left + (2.4f + k * 4.2f) * dp, top + 2.2f * dp),
                size = Size(3.0f * dp, bh - 4.4f * dp),
                cornerRadius = CornerRadius(0.6f * dp),
                alpha = a,
            )
        }
    }
}
