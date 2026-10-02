package com.wickmoth.lakekeeps.screens.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.messages.Notice
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.screens.phone.messages.MessagesApp
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.LocalViewport
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.rememberFrameFit
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** How long after the phone wakes before a waiting contact makes their first move. */
private const val FIRST_MESSAGE_DELAY_MS = 1200L

/**
 * A held phone: its home screen, the Messages app, the status bar and its shade. [content] (0..1)
 * is the phone waking up as it is lifted; [onPutDown] sets it back on the desk.
 */
@Composable
fun PhoneScreen(owner: Owner, messages: Messages, content: () -> Float, onPutDown: () -> Unit) {
    val os = rememberPhoneOs(owner, messages)
    val fit = rememberFrameFit(Alignment.TopCenter)
    val viewport = LocalViewport.current
    val spec = remember(owner) { phoneSpec(owner) }
    var panelHeight by remember { mutableFloatStateOf(1f) }
    val live by remember { derivedStateOf { os.thread?.let(messages::isLive) == true && os.appOpen } }
    val messagesIcon = remember(fit) {
        val at = iconTopLeft(spec.apps.indexOfFirst { it.label == R.string.app_messages })
        val corner = fit.toWindow(at.x, at.y)
        Rect(corner, Size(ICON * fit.unit, ICON * fit.unit))
    }
    SideEffect { os.appIcon = messagesIcon }

    // A contact with a live script reaches out once the phone is awake.
    LaunchedEffect(owner) {
        snapshotFlow { content() >= 1f }.first { it }
        delay(FIRST_MESSAGE_DELAY_MS)
        Threads.of(owner).filter { it.live != null && messages[it].delivered == 0 }.forEach { thread ->
            val first = messages.nextLine(thread) ?: return@forEach
            if (first.first) return@forEach
            messages.deliver(thread)
            if (os.thread != thread) os.postHeadsUp(Notice(thread, messages.lines(thread).last(), messages.unread(thread)))
        }
    }

    // Black behind the home screen, which shrinks back a little while an app is open over it.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HomeScreen(
            owner = owner,
            content = content,
            unread = { messages.unread(owner) },
            onOpenMessages = { os.openApp(messagesIcon) },
            modifier = Modifier.graphicsLayer {
                val s = 1f - 0.06f * os.appIn.value
                scaleX = s
                scaleY = s
            },
        )
        if (os.appOpen) {
            AppWindow(os.launchedFrom, messagesIcon.width, viewport, progress = { os.appIn.value }) {
                MessagesApp(os, fit)
            }
        }
        DesignFrame(fit) {
            StatusBar(
                owner = stringResource(spec.owner),
                clock = formatClock(messages.clock, withHalf = false),
                notices = messages.notices(owner).size,
                content = content,
                onHome = { 1f - os.appIn.value },
                diamondEnabled = !live,
                onDiamond = onPutDown,
                onShadeDrag = { delta -> os.dragShade(delta / panelHeight) },
                onShadeRelease = { velocity -> os.settleShade(velocity / panelHeight) },
                onShadeTap = os::openShade,
            )
        }
        // Above the status bar, as on a real phone, so the banner's top edge still takes taps.
        HeadsUp(os, fit)
        Shade(os, fit, messages.clock) { panelHeight = it }
    }
    // Registered after the board's handler, so back closes the shade before it puts the phone down.
    BackHandler(enabled = os.shadeOpen) { os.closeShade() }
}

/**
 * An app's window growing out of the place it was launched from (its icon, or a notification):
 * the app's icon swells and fades while the window opens to full screen and the content settles
 * in. [iconSide] is the icon's size in window pixels.
 */
@Composable
private fun AppWindow(from: Rect, iconSide: Float, viewport: Size, progress: () -> Float, content: @Composable () -> Unit) {
    val icon = painterResource(R.drawable.icon_messages)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val e = progress().coerceIn(0f, 1f)
                val corner = from.minDimension * 0.22f
                shape = RectWindow(lerpRect(from, Rect(Offset.Zero, viewport), e), lerp(corner, 0f, e))
                clip = true
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.AppBackground),
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val e = progress()
                    alpha = window(e, 0.3f, 0.45f)
                    val s = lerp(0.94f, 1f, e)
                    scaleX = s
                    scaleY = s
                },
        ) { content() }
        Canvas(Modifier.fillMaxSize()) {
            val e = progress()
            val fade = 1f - window(e, 0.05f, 0.4f)
            if (fade <= 0f) return@Canvas
            val r = lerpRect(from, Rect(Offset.Zero, viewport), e)
            val side = iconSide * (1f + 0.6f * e)
            translate(r.center.x - side / 2f, r.center.y - side / 2f) {
                with(icon) { draw(Size(side, side), alpha = fade) }
            }
        }
    }
}

private fun lerpRect(a: Rect, b: Rect, f: Float) =
    Rect(lerp(a.left, b.left, f), lerp(a.top, b.top, f), lerp(a.right, b.right, f), lerp(a.bottom, b.bottom, f))

/** Clips a full-window layer to [rect] (in the layer's own pixels) with rounded corners. */
private class RectWindow(private val rect: Rect, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(rect, CornerRadius(radius)))
}
