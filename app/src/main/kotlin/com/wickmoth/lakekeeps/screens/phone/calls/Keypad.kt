package com.wickmoth.lakekeeps.screens.phone.calls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.game.phone.formatNumber
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.GlyphIcon
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.tactile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Rows = listOf("123", "456", "789", "*0#")
private val KeyCorner = 16.dp

/** The number being typed (or the call in progress), the keys, and the call and delete keys. */
@Composable
internal fun Keypad(dialer: Dialer) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // keys give up a little height on short screens, so the number always has room
        val keyHeight = ((maxHeight - 120.dp - 24.dp - 50.dp) / 5).coerceIn(52.dp, 72.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Display(
                dialer,
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Column(
                Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Rows.forEachIndexed { row, keys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        keys.forEach { key -> Key(key, row, keyHeight, Modifier.weight(1f), dialer::press) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Spacer(Modifier.weight(1f))
                    CallKey(dialer, keyHeight, Modifier.weight(1f))
                    DeleteKey(dialer, keyHeight, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Display(dialer: Dialer, modifier: Modifier) {
    val call = dialer.call
    Box(
        modifier.graphicsLayer { translationX = dialer.nudge.value.dp.toPx() },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = call?.let { it.name ?: formatNumber(it.number) },
            transitionSpec = { (fadeIn(tween(220, delayMillis = 90)) + scaleIn(tween(320), initialScale = 0.94f)) togetherWith fadeOut(tween(120)) },
            label = "display",
        ) { calling ->
            if (calling != null) {
                CallStatus(calling, ended = dialer.call?.ended ?: true, ring = { dialer.ring.value })
            } else {
                Number(dialer.digits)
            }
        }
    }
}

/** The typed number, grouped as it grows; each new digit drops into place. */
@Composable
private fun Number(digits: String) {
    val shown = formatNumber(digits)
    val size = when {
        shown.length <= 9 -> 38.sp
        shown.length <= 12 -> 32.sp
        else -> 26.sp
    }
    Row(
        Modifier.clearAndSetSemantics { contentDescription = shown },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEachIndexed { i, ch -> key(i) { Digit(ch, size) } }
    }
}

@Composable
private fun Digit(ch: Char, size: TextUnit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 700f)) }
    PhoneText(
        ch.toString(),
        phoneText(size, FontWeight.Medium),
        Modifier.graphicsLayer {
            alpha = appear.value.coerceIn(0f, 1f)
            translationY = ((1f - appear.value) * -10f).dp.toPx()
        },
    )
}

/**
 * Who is being called, and "calling…", which brightens with each ring, or "ended" in red.
 * [ring] jumps to 1 as a ring starts and runs down to 0 before the next.
 */
@Composable
private fun CallStatus(name: String, ended: Boolean, ring: () -> Float) {
    Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PhoneText(name, phoneText(30.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        AnimatedContent(
            targetState = ended,
            transitionSpec = { fadeIn(tween(160, delayMillis = 80)) togetherWith fadeOut(tween(80)) },
            label = "status",
        ) { done ->
            PhoneText(
                stringResource(if (done) R.string.call_ended else R.string.call_calling),
                phoneText(17.sp, FontWeight.Medium, if (done) PhoneColors.Alert else PhoneColors.TextMuted),
                Modifier
                    .graphicsLayer {
                        if (!done) {
                            // bright while the ring sounds (its first second), dim between rings
                            val since = (1f - ring()) * 3f
                            alpha = 0.5f + 0.5f * (1f - ((since - 1.1f) / 0.6f).coerceIn(0f, 1f))
                        }
                    }
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** A digit, star or hash key. Like a real keypad it sounds the moment it goes down. */
@Composable
private fun Key(key: Char, row: Int, height: Dp, modifier: Modifier, onKey: (Char) -> Unit) {
    val press = remember { Animatable(0f) }
    val appear = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        delay(row * 40L)
        appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 420f))
    }
    val label = when (key) {
        '*' -> stringResource(R.string.key_star)
        '#' -> stringResource(R.string.key_hash)
        else -> key.toString()
    }
    Box(
        modifier
            .height(height)
            .graphicsLayer {
                val a = appear.value
                alpha = a.coerceIn(0f, 1f)
                val s = lerp(0.86f, 1f, a) * (1f - 0.05f * press.value)
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                drawRoundRect(lerp(PhoneColors.Surface, PhoneColors.SurfacePressed, press.value), cornerRadius = CornerRadius(KeyCorner.toPx()))
            }
            .onKeyDown(label, onDown = { onKey(key) }) { down ->
                scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = if (down) 1400f else 500f)) }
            },
        contentAlignment = Alignment.Center,
    ) {
        PhoneText(
            key.toString(),
            phoneText(if (key == '*') 32.sp else 26.sp, FontWeight.SemiBold),
            // the star sits high in the font; bring it down to the digits' middle
            Modifier.offset(y = if (key == '*') 5.dp else 0.dp),
        )
    }
}

/** Acts as soon as the key goes down, and reports when it is held and let go. */
@Composable
private fun Modifier.onKeyDown(label: String, onDown: () -> Unit, onPress: (Boolean) -> Unit): Modifier {
    val down by rememberUpdatedState(onDown)
    val press by rememberUpdatedState(onPress)
    return this
        .semantics(mergeDescendants = true) {
            contentDescription = label
            role = Role.Button
            onClick { down(); true }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    down()
                    press(true)
                    tryAwaitRelease()
                    press(false)
                },
            )
        }
}

/** Call, which turns into a red hang-up handset while a call is on. */
@Composable
private fun CallKey(dialer: Dialer, height: Dp, modifier: Modifier) {
    val call = dialer.call
    val ended = call?.ended == true
    val hang by animateFloatAsState(if (call != null) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 260f), label = "hang")
    val dim by animateFloatAsState(if (ended) 0.45f else 1f, tween(220), label = "dim")
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val tint: () -> Color = { lerp(PhoneColors.Online, PhoneColors.Alert, hang.coerceIn(0f, 1f)) }
    Box(
        modifier
            .height(height)
            .graphicsLayer {
                alpha = dim
                val s = 1f - 0.05f * press.value
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                val corner = CornerRadius(KeyCorner.toPx())
                drawRoundRect(PhoneColors.Surface, cornerRadius = corner)
                drawRoundRect(tint(), cornerRadius = corner, alpha = 0.16f + 0.12f * press.value)
            }
            .tactile(
                label = stringResource(if (call != null) R.string.end_call else R.string.call),
                enabled = !ended,
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 900f)) } },
            ) {
                if (dialer.inCall) dialer.hangUp() else dialer.dial()
            },
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(Glyph.Handset, tint(), Modifier.size(30.dp), turn = { 135f * hang })
    }
}

/** Deletes the last digit; held, it clears the number. Only there while there is a number. */
@Composable
private fun DeleteKey(dialer: Dialer, height: Dp, modifier: Modifier) {
    val active = dialer.digits.isNotEmpty() && !dialer.inCall
    val shown by animateFloatAsState(if (active) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "delete")
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val deleteLabel = stringResource(R.string.delete)
    val clearLabel = stringResource(R.string.clear)
    Box(
        modifier
            .height(height)
            .graphicsLayer {
                alpha = shown.coerceIn(0f, 1f)
                val s = lerp(0.7f, 1f, shown) * (1f - 0.05f * press.value)
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                drawRoundRect(lerp(PhoneColors.Surface, PhoneColors.SurfacePressed, press.value), cornerRadius = CornerRadius(KeyCorner.toPx()))
            }
            .then(
                if (!active) {
                    Modifier
                } else {
                    Modifier
                        .semantics {
                            contentDescription = deleteLabel
                            role = Role.Button
                            onClick { dialer.delete(); true }
                            onLongClick(clearLabel) { dialer.clear(); true }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    scope.launch { press.animateTo(1f, spring(stiffness = 1400f)) }
                                    tryAwaitRelease()
                                    scope.launch { press.animateTo(0f, spring(stiffness = 500f)) }
                                },
                                onLongPress = { dialer.clear() },
                                onTap = { dialer.delete() },
                            )
                        }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(Glyph.Backspace, PhoneColors.Text, Modifier.size(26.dp))
    }
}
