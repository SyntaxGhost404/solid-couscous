package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.messages.Day
import com.wickmoth.lakekeeps.game.messages.Notice
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.screens.phone.messages.Avatar
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** The notification shade: the time, sound, and every conversation still waiting to be read. */
@Composable
internal fun Shade(os: PhoneOs, fit: FrameFit, clock: Int, onPanelHeight: (Float) -> Unit) {
    val visible by remember { derivedStateOf { os.shade.value > 0f || os.shade.targetValue > 0f } }
    if (!visible) return
    val audio = LocalAudio.current
    val notices = os.messages.notices(os.owner)
    var height by remember { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = 0.6f * os.shade.value.coerceIn(0f, 1f)) }
                .pointerInput(Unit) { detectTapGestures { os.closeShade() } },
        )
        DesignScale(fit) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged {
                        height = it.height.toFloat()
                        onPanelHeight(height)
                    }
                    .graphicsLayer { translationY = -(1f - os.shade.value) * size.height }
                    .background(PhoneColors.Shade, RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> os.dragShade(delta / height) },
                        onDragStopped = { velocity -> os.settleShade(velocity / height) },
                    )
                    .pointerInput(Unit) { detectTapGestures { } }
                    .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 500f)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 14.dp, top = 40.dp)
                        .graphicsLayer { alpha = window(os.shade.value, 0.15f, 0.6f) },
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                PhoneText(formatClock(clock, withHalf = false), phoneText(36.sp, FontWeight.Bold))
                                PhoneText(
                                    if (clock % 1440 < 720) "AM" else "PM",
                                    phoneText(14.sp, FontWeight.SemiBold, PhoneColors.TextMuted),
                                    Modifier.padding(start = 5.dp, bottom = 7.dp),
                                )
                            }
                            PhoneText(stringResource(R.string.shade_date), phoneText(13.sp, color = PhoneColors.TextMuted))
                        }
                        val muted = audio.muted
                        ShadeButton(
                            glyph = if (muted) Glyph.SoundOff else Glyph.SoundOn,
                            label = stringResource(if (muted) R.string.sound_off else R.string.sound_on),
                            lit = !muted,
                        ) {
                            audio.muted = !muted
                            if (muted) audio.play(Sfx.Tap)
                        }
                        Spacer(Modifier.width(6.dp))
                        ShadeButton(Glyph.Close, stringResource(R.string.close), lit = false) { os.closeShade() }
                    }
                    Spacer(Modifier.height(22.dp))
                    PhoneText(
                        stringResource(R.string.shade_notifications).uppercase(),
                        phoneText(11.sp, FontWeight.Bold, PhoneColors.TextFaint).copy(letterSpacing = 0.12.em),
                    )
                    Spacer(Modifier.height(10.dp))
                    if (notices.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
                            PhoneText(stringResource(R.string.shade_empty), phoneText(14.sp, color = PhoneColors.TextMuted))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(end = 10.dp)) {
                        notices.forEach { notice ->
                            key(notice.thread.id) {
                                NoticeCard(
                                    notice = notice,
                                    onOpen = { rect -> os.openNotice(notice, rect) },
                                    onDismiss = { os.messages.dismiss(notice.thread) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Box(
                    Modifier
                        .padding(bottom = 10.dp)
                        .size(36.dp, 4.dp)
                        .background(PhoneColors.Outline, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun ShadeButton(glyph: Glyph, label: String, lit: Boolean, onTap: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(44.dp)
            .tactile(label, onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 700f)) } }, onTap = onTap)
            .graphicsLayer {
                val s = 1f - 0.08f * press.value
                scaleX = s
                scaleY = s
            }
            .background(if (lit) PhoneColors.Accent.copy(alpha = 0.22f) else PhoneColors.Surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, if (lit) Color(0xFF9CC2F5) else PhoneColors.Text, Modifier.size(22.dp))
    }
}

/** One waiting conversation. Tap to open it; swipe it sideways to clear it. */
@Composable
private fun NoticeCard(notice: Notice, onOpen: (Rect) -> Unit, onDismiss: () -> Unit) {
    val dx = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableFloatStateOf(1f) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val label = "${notice.thread.contact.name}: ${notice.line.text}"
    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                width = it.size.width.toFloat()
                bounds = it.boundsInRoot()
            }
            .graphicsLayer {
                translationX = dx.value
                alpha = 1f - (abs(dx.value) / width).coerceIn(0f, 1f) * 0.9f
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick { onOpen(bounds); true }
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> scope.launch { dx.snapTo(dx.value + delta) } },
                onDragStopped = { velocity ->
                    if (abs(dx.value) > width * 0.35f || abs(velocity) > 1800f) {
                        val direction = if (dx.value != 0f) sign(dx.value) else sign(velocity)
                        dx.animateTo(direction * width * 1.1f, tween(180))
                        onDismiss()
                    } else {
                        dx.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
                    }
                },
            )
            .pointerInput(Unit) { detectTapGestures { onOpen(bounds) } },
    ) {
        NoticeRow(notice, Modifier.background(PhoneColors.Surface, RoundedCornerShape(16.dp)).border(1.dp, PhoneColors.Outline, RoundedCornerShape(16.dp)))
    }
}

/** Avatar, app and time, sender and the latest line: shared by the shade and the banner. */
@Composable
internal fun NoticeRow(notice: Notice, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(notice.thread.contact.face, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.icon_messages), null, Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                val time = if (notice.line.day == Day.Today) formatClock(notice.line.minutes) else stringResource(notice.line.day.short)
                PhoneText("${stringResource(R.string.messages_title)} · $time", phoneText(11.sp, color = PhoneColors.TextFaint))
            }
            PhoneText(notice.thread.contact.name, phoneText(14.5.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            PhoneText(notice.line.text, phoneText(13.sp, color = PhoneColors.TextMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (notice.unread > 1) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(22.dp)
                    .background(PhoneColors.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PhoneText(notice.unread.toString(), phoneText(11.sp, FontWeight.Bold, Color.White))
            }
        }
    }
}

/** A banner for a message that arrives outside its conversation: tap to open, flick up to dismiss. */
@Composable
internal fun HeadsUp(os: PhoneOs, fit: FrameFit) {
    val notice = os.headsUp ?: return
    val drag = remember(notice) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val unreadLabel = pluralStringResource(R.plurals.unread, notice.unread, notice.unread)
    val text = notice.line.text.let { if (it.last() in ".?!…") it else "$it." }
    val label = "${notice.thread.contact.name}: $text $unreadLabel"
    val from = remember(fit) {
        val at = fit.toWindow(12f, 76f)
        Rect(at.x, at.y, at.x + 336f * fit.unit, at.y + 72f * fit.unit)
    }
    DesignFrame(fit) {
        Box(
            Modifier
                .offset(12.dp, 76.dp)
                .width(336.dp)
                .graphicsLayer {
                    val p = os.headsUpIn.value
                    translationY = lerp(-120.dp.toPx(), 0f, p) + drag.value.coerceAtMost(0f)
                    alpha = p.coerceIn(0f, 1f)
                    shadowElevation = 12.dp.toPx() * p.coerceIn(0f, 1f)
                    shape = RoundedCornerShape(18.dp)
                    clip = true
                }
                .background(PhoneColors.Surface)
                .border(1.dp, PhoneColors.Outline, RoundedCornerShape(18.dp))
                .semantics {
                    contentDescription = label
                    role = Role.Button
                    onClick { os.openNotice(notice, from); true }
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> scope.launch { drag.snapTo(drag.value + delta) } },
                    onDragStopped = { velocity ->
                        if (drag.value < -24f * fit.unit || velocity < -900f) {
                            os.dismissHeadsUp()
                        } else {
                            drag.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
                        }
                    },
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        os.feedback(Haptic.Tick)
                        os.openNotice(notice, from)
                    }
                },
        ) {
            NoticeRow(notice)
        }
    }
}
