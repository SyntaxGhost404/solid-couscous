package com.wickmoth.lakekeeps.screens.phone

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
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
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.messages.Notice
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.screens.phone.calls.CallsApp
import com.wickmoth.lakekeeps.screens.phone.gallery.GalleryApp
import com.wickmoth.lakekeeps.screens.phone.mail.MailApp
import com.wickmoth.lakekeeps.screens.phone.messages.MessagesApp
import com.wickmoth.lakekeeps.screens.phone.settings.SettingsApp
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
 * A held phone: its home screen and apps, the status bar and its shade. [content] (0..1) is the
 * phone waking up as it is lifted; [onPutDown] sets it back on the desk.
 */
@Composable
fun PhoneScreen(owner: Owner, state: GameState, content: () -> Float, onPutDown: () -> Unit) {
    val os = rememberPhoneOs(owner, state)
    val messages = state.messages
    val fit = rememberFrameFit(Alignment.TopCenter)
    val viewport = LocalViewport.current
    val spec = remember(owner) { phoneSpec(owner) }
    var panelHeight by remember { mutableFloatStateOf(1f) }
    val busy by remember { derivedStateOf { os.busy } }
    val appIcons = remember(fit) {
        spec.apps.withIndex().mapNotNull { (i, app) ->
            val at = iconTopLeft(i)
            app.opens?.let { it to Rect(fit.toWindow(at.x, at.y), Size(ICON * fit.unit, ICON * fit.unit)) }
        }.toMap()
    }
    SideEffect { os.appIcons = appIcons }

    if (owner.case == CaseId.Prototype) {
        // A contact with a live script reaches out once the phone is awake.
        LaunchedEffect(owner) {
            snapshotFlow { content() >= 1f }.first { it }
            delay(FIRST_MESSAGE_DELAY_MS)
            Threads.of(owner).filter { it.live != null && messages[it].delivered == 0 }.forEach { thread ->
                val first = messages.nextLine(thread) ?: return@forEach
                if (first.mine) return@forEach
                messages.deliver(thread)
                if (os.thread != thread) os.postHeadsUp(Notice(thread, messages.lines(thread).last(), messages.unread(thread)))
            }
        }
    } else {
        // The case delivers its own messages; in hand, the phone shows a banner for each one that
        // arrives outside the open conversation.
        LaunchedEffect(owner) {
            val threads = Threads.of(owner)
            var seen = threads.map { messages[it].delivered }
            snapshotFlow { threads.map { messages[it].delivered } }.collect { now ->
                threads.forEachIndexed { i, thread ->
                    if (now[i] <= seen[i] || os.thread == thread) return@forEachIndexed
                    val last = messages.lines(thread).last()
                    if (!last.mine) os.postHeadsUp(Notice(thread, last, messages.unread(thread)))
                }
                seen = now
            }
        }
    }

    // Black behind the home screen, which shrinks back a little while an app is open over it.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HomeScreen(
            owner = owner,
            content = content,
            unread = { app ->
                when (app) {
                    PhoneApp.Messages -> messages.unread(owner)
                    PhoneApp.Mail -> state.mail.unread(owner)
                    else -> 0
                }
            },
            onOpen = { app -> os.openApp(app, appIcons.getValue(app)) },
            modifier = Modifier.graphicsLayer {
                val s = 1f - 0.06f * os.appIn.value
                scaleX = s
                scaleY = s
            },
        )
        os.app?.let { app ->
            val icon = spec.apps.first { it.opens == app }.icon
            AppWindow(os.launchedFrom, icon, appIcons.getValue(app).width, viewport, progress = { os.appIn.value }) {
                when (app) {
                    PhoneApp.Messages -> MessagesApp(os, fit)
                    PhoneApp.Calls -> CallsApp(os, fit)
                    PhoneApp.Mail -> MailApp(os, fit)
                    PhoneApp.Gallery -> GalleryApp(os, fit)
                    PhoneApp.Settings -> SettingsApp(os, fit)
                }
            }
        }
        DesignFrame(fit) {
            StatusBar(
                owner = stringResource(spec.owner),
                clock = formatClock(messages.clock(owner.case), withHalf = false),
                notices = messages.notices(owner).size,
                content = content,
                onHome = { 1f - os.appIn.value },
                diamondEnabled = !busy,
                onDiamond = onPutDown,
                onShadeDrag = { delta -> os.dragShade(delta / panelHeight) },
                onShadeRelease = { velocity -> os.settleShade(velocity / panelHeight) },
                onShadeTap = os::openShade,
            )
        }
        // Above the status bar, as on a real phone, so the banner's top edge still takes taps.
        HeadsUp(os, fit)
        Shade(os, fit, messages.clock(owner.case)) { panelHeight = it }
    }
    // Registered after the board's handler, so back closes the shade before it puts the phone down.
    BackHandler(enabled = os.shadeOpen) { os.closeShade() }
}

/**
 * An app's window growing out of the place it was launched from (its icon, or a notification):
 * the app's [icon] swells and fades while the window opens to full screen and the content settles
 * in. [iconSide] is the icon's size in window pixels.
 */
@Composable
private fun AppWindow(from: Rect, @DrawableRes icon: Int, iconSide: Float, viewport: Size, progress: () -> Float, content: @Composable () -> Unit) {
    val splash = painterResource(icon)
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
                with(splash) { draw(Size(side, side), alpha = fade) }
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
