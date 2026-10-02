package com.wickmoth.lakekeeps.screens.phone.messages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.StoryCalendar
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.game.messages.Beat
import com.wickmoth.lakekeeps.game.messages.Day
import com.wickmoth.lakekeeps.game.messages.Line
import com.wickmoth.lakekeeps.game.messages.Thread
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.screens.phone.AppHeader
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.HeaderButton
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneOs
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.dayChip
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.rememberIdleClock
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private val ReplyPanelHeight = 196.dp

/** How long a contact "types" before a line arrives; a picture takes a moment longer to send. */
private fun typingMs(text: String, attachment: Boolean = false): Long =
    (700L + text.length * 28L).coerceAtMost(2600L) + if (attachment) 900L else 0L

/** How long a contact stays online after their last line before signing off. */
private const val SIGN_OFF_MS = 1200L

/** One conversation. While its script is live the player can only answer, not leave. */
@Composable
internal fun Chat(os: PhoneOs, thread: Thread, modifier: Modifier) {
    val messages = os.messages
    val audio = LocalAudio.current
    val scope = rememberCoroutineScope()
    val lines = messages.lines(thread)
    val live = messages.isLive(thread)
    val question = messages.question(thread)
    var typing by remember { mutableStateOf(false) }
    // the last question answered, and with which reply; kept while its replies fade away
    var answered by remember { mutableStateOf<Pair<Beat.Ask, Int>?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val viewer = remember { Animatable(0f) }
    val openedWith = remember { lines.size }
    val nudge = remember { Animatable(0f) }

    LaunchedEffect(lines.size) { messages.markRead(thread) }

    // The game knows which conversation is on screen, so the case leaves its lines to the chat.
    DisposableEffect(thread.id) {
        os.state.openThread = thread.id
        onDispose { if (os.state.openThread == thread.id) os.state.openThread = null }
    }

    // Plays the script: the contact types and replies; the player's picked answers go out. When the
    // script waits for the case, it waits here too, and picks up if the case moves on meanwhile.
    LaunchedEffect(thread.id) {
        while (isActive) {
            val next = messages.nextLine(thread)
            when {
                next != null && next.mine -> {
                    messages.deliver(thread)
                    audio.play(Sfx.MessageOut)
                    delay(650)
                }
                next != null -> {
                    delay(450)
                    typing = true
                    delay(typingMs(next.text, next.attachment != null))
                    typing = false
                    messages.deliver(thread)
                    audio.play(Sfx.MessageIn)
                    os.feedback(Haptic.Tick)
                    delay(320)
                }
                messages.question(thread) != null -> {
                    val asked = messages[thread].chosen.size
                    snapshotFlow { messages[thread].chosen.size }.first { it > asked }
                }
                else -> {
                    if (messages.isLive(thread)) {
                        delay(SIGN_OFF_MS)
                        messages.signOff(thread)
                        audio.play(Sfx.Offline)
                    }
                    snapshotFlow { messages.nextLine(thread) != null || messages.question(thread) != null }.first { it }
                }
            }
        }
    }

    fun view(attachment: String) {
        os.feedback(Haptic.Tick)
        audio.play(Sfx.AppOpen, 0.35f)
        viewing = attachment
        scope.launch { viewer.animateTo(1f, tween(320, easing = Ease.Emphasized)) }
    }

    fun closeViewer() {
        if (viewer.targetValue == 0f) return
        audio.play(Sfx.AppClose, 0.35f)
        scope.launch {
            viewer.animateTo(0f, tween(240))
            viewing = null
        }
    }

    // A question takes one answer. A second tap, even one landing as its replies fade away, would
    // otherwise answer whichever question comes next before it's asked.
    fun pick(ask: Beat.Ask, reply: Int) {
        if (answered?.first === ask || messages.question(thread) !== ask) return
        answered = ask to reply
        os.feedback(Haptic.Tick)
        scope.launch {
            delay(230)
            messages.choose(thread, reply)
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.AppBackground),
        ) {
            ChatHeader(
                thread = thread,
                live = live,
                onBack = os::closeThread,
                onClose = os::closeApp,
                onContact = {
                    os.feedback(Haptic.Tick)
                    audio.play(Sfx.Tap, 0.6f)
                    os.openContact()
                },
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                MessageList(lines, typing, openedWith, os.owner.case.calendar, ::view)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PhoneColors.Divider),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ReplyPanelHeight)
                    .graphicsLayer { translationX = nudge.value.dp.toPx() },
                contentAlignment = Alignment.Center,
            ) {
                val panel = if (question != null && !typing) question else null
                AnimatedContent(
                    targetState = panel,
                    // one out, then the other in: replies and the status line share the same spot
                    transitionSpec = { fadeIn(tween(220, delayMillis = 120)) togetherWith fadeOut(tween(120)) },
                    label = "replies",
                ) { ask ->
                    if (ask != null) {
                        Replies(ask, answered?.takeIf { it.first === ask }?.second) { pick(ask, it) }
                    } else {
                        Presence(thread.contact.name, typing, live)
                    }
                }
            }
        }
        ContactSheet(os, thread)
        viewing?.let { PictureViewer(it, { viewer.value }, ::closeViewer) }
    }

    // While the contact is live there is no leaving: back just nudges the replies.
    BackHandler(enabled = live && !os.shadeOpen && !os.contactOpen) {
        os.feedback(Haptic.Reject)
        audio.play(Sfx.Denied, 0.6f)
        scope.launch {
            nudge.animateTo(0f, keyframes {
                durationMillis = 380
                -8f at 50
                8f at 120
                -5f at 190
                4f at 260
                0f at 380
            })
        }
    }
    BackHandler(enabled = os.contactOpen && !os.shadeOpen) { os.closeContact() }
    // registered last, so back closes a picture before anything else
    BackHandler(enabled = viewing != null && !os.shadeOpen) { closeViewer() }
}

@Composable
private fun ChatHeader(thread: Thread, live: Boolean, onBack: () -> Unit, onClose: () -> Unit, onContact: () -> Unit) {
    val navigation by animateFloatAsState(if (live) 0f else 1f, tween(280), label = "navigation")
    val details = stringResource(R.string.contact_details, thread.contact.name)
    AppHeader {
        HeaderButton(Glyph.Back, stringResource(R.string.back), Modifier.align(Alignment.CenterStart).padding(start = 6.dp), visible = { navigation }) {
            onBack()
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .tactile(details, onTap = onContact),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(thread.contact.face, 34.dp)
                Spacer(Modifier.width(10.dp))
                PhoneText(thread.contact.name, phoneText(19.sp, FontWeight.Bold), maxLines = 1)
                Spacer(Modifier.width(9.dp))
                PresenceDot(live, 9.dp)
            }
            Spacer(Modifier.height(5.dp))
            PhoneText(thread.contact.subtitle, phoneText(12.sp, color = PhoneColors.TextFaint))
        }
        HeaderButton(Glyph.Close, stringResource(R.string.close), Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), visible = { navigation }) {
            onClose()
        }
    }
}

/** A row in the conversation: a day chip, a message, or the contact typing. */
private sealed interface Entry {
    val key: Any

    data class Chip(val day: Day) : Entry {
        override val key: Any get() = "day-$day"
    }

    data class Message(val line: Line, val index: Int, val tail: Boolean, val groupStart: Boolean) : Entry {
        override val key: Any get() = index
    }

    data object Typing : Entry {
        override val key: Any get() = "typing"
    }
}

private fun entries(lines: List<Line>, typing: Boolean): List<Entry> {
    val out = mutableListOf<Entry>()
    lines.forEachIndexed { i, line ->
        val prev = lines.getOrNull(i - 1)
        val next = lines.getOrNull(i + 1)
        if (prev == null || prev.day != line.day) out += Entry.Chip(line.day)
        val groupStart = prev == null || prev.mine != line.mine || prev.day != line.day
        val tail = next == null || next.mine != line.mine || next.day != line.day
        out += Entry.Message(line, i, tail, groupStart)
    }
    if (typing) out += Entry.Typing
    return out
}

/** The last row the conversation has scrolled for, so each new arrival is handled once. */
private class Newest(var key: Any?)

@Composable
private fun MessageList(lines: List<Line>, typing: Boolean, openedWith: Int, calendar: StoryCalendar, onPicture: (String) -> Unit) {
    val rows = remember(lines, typing) { entries(lines, typing).asReversed() }
    val state = rememberLazyListState()
    val newest = rows.firstOrNull()
    val handled = remember { Newest(newest?.key) }
    val nearLatest = with(LocalDensity.current) { 24.dp.toPx() }
    // A new row stays in view if the player was already reading the latest one; their own reply
    // always brings them back down. Applied before the list measures, so nothing jumps.
    SideEffect {
        if (newest == null || newest.key == handled.key) return@SideEffect
        handled.key = newest.key
        val atLatest = state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset <= nearLatest
        if (atLatest || (newest is Entry.Message && newest.line.mine)) state.requestScrollToItem(0)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = state,
        reverseLayout = true,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(rows, key = { it.key }) { row ->
            Box(
                Modifier
                    .widthIn(max = 344.dp)
                    .fillMaxWidth()
                    .animateItem(fadeInSpec = null, fadeOutSpec = tween(150), placementSpec = spring(dampingRatio = 0.85f, stiffness = 420f)),
            ) {
                when (row) {
                    is Entry.Chip -> DayChip(row.day, calendar)
                    is Entry.Message -> Bubble(row.line, row.tail, isNew = row.index >= openedWith, Modifier.padding(top = if (row.groupStart) 12.dp else 3.dp), onPicture)
                    Entry.Typing -> TypingBubble(Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun DayChip(day: Day, calendar: StoryCalendar) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        PhoneText(
            dayChip(day, calendar),
            phoneText(12.5.sp, FontWeight.SemiBold, PhoneColors.TextMuted),
            Modifier
                .background(PhoneColors.Header, RoundedCornerShape(10.dp))
                .padding(horizontal = 13.dp, vertical = 5.dp),
        )
    }
}

/**
 * A message bubble. New ones grow from their tail; the player's rise up from the reply panel. A
 * picture sits at the top of its bubble; tap it to see it whole.
 */
@Composable
private fun Bubble(line: Line, tail: Boolean, isNew: Boolean, modifier: Modifier, onPicture: (String) -> Unit) {
    val appear = remember { Animatable(if (isNew) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (isNew) appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 380f))
    }
    val mine = line.mine
    val big = 18.dp
    val small = 6.dp
    Box(modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier
                .widthIn(max = 268.dp)
                .graphicsLayer {
                    val p = appear.value
                    alpha = p.coerceIn(0f, 1f)
                    val s = lerp(if (mine) 0.92f else 0.84f, 1f, p)
                    scaleX = s
                    scaleY = s
                    translationY = ((1f - p) * (if (mine) 34f else 8f)).dp.toPx()
                    transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f)
                }
                .background(
                    if (mine) PhoneColors.Outgoing else PhoneColors.Incoming,
                    RoundedCornerShape(
                        topStart = big,
                        topEnd = big,
                        bottomStart = if (!mine && tail) small else big,
                        bottomEnd = if (mine && tail) small else big,
                    ),
                )
                .padding(start = 14.dp, end = 14.dp, top = if (line.attachment != null) 6.dp else 9.dp, bottom = 8.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            line.attachment?.let { picture ->
                val label = stringResource(R.string.picture_open, Evidence.byId(picture)?.title ?: picture)
                AttachmentPicture(
                    picture,
                    Modifier
                        .padding(top = 2.dp, bottom = if (line.text.isEmpty()) 2.dp else 8.dp)
                        .size(196.dp, 252.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .tactile(label) { onPicture(picture) },
                )
            }
            if (line.text.isNotEmpty()) PhoneText(line.text, phoneText(15.5.sp, color = if (mine) Color.White else PhoneColors.Text))
            Spacer(Modifier.height(3.dp))
            PhoneText(formatClock(line.minutes), phoneText(11.sp, color = if (mine) PhoneColors.OutgoingTime else PhoneColors.TextFaint))
        }
    }
}

@Composable
private fun TypingBubble(modifier: Modifier) {
    val clock by rememberIdleClock()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f)) }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier
                .graphicsLayer {
                    val p = appear.value
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = lerp(0.6f, 1f, p)
                    scaleY = lerp(0.6f, 1f, p)
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .background(PhoneColors.Incoming, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(7.dp)
                        .graphicsLayer {
                            val wave = max(0f, sin(2f * PI.toFloat() * (clock / 1050f - i * 0.16f)))
                            translationY = (-3.5f * wave).dp.toPx()
                            alpha = 0.45f + 0.55f * wave
                        }
                        .background(PhoneColors.TextMuted, CircleShape),
                )
            }
        }
    }
}

/** The player's possible answers. The picked one lights up as the rest fall away. */
@Composable
private fun Replies(ask: Beat.Ask, answering: Int?, onPick: (Int) -> Unit) {
    val enter = remember(ask) { Animatable(0f) }
    LaunchedEffect(ask) { enter.animateTo(1f, tween(520)) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ask.replies.forEachIndexed { i, reply ->
            ReplyButton(
                text = reply.text,
                appear = { window(enter.value * 520f, i * 80f, 300f, Ease.OutCubic) },
                chosen = answering == i,
                dropped = answering != null && answering != i,
            ) { onPick(i) }
        }
    }
}

@Composable
private fun ReplyButton(text: String, appear: () -> Float, chosen: Boolean, dropped: Boolean, onTap: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val lit by animateFloatAsState(if (chosen) 1f else 0f, tween(160), label = "lit")
    val gone by animateFloatAsState(if (dropped) 1f else 0f, tween(170), label = "gone")
    val shape = RoundedCornerShape(25.dp)
    Box(
        Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .graphicsLayer {
                val a = appear()
                alpha = a * (1f - gone)
                translationY = ((1f - a) * 12f + gone * 6f).dp.toPx()
                val s = (1f - 0.04f * press.value) * (1f + 0.02f * lit)
                scaleX = s
                scaleY = s
            }
            .background(lerpColor(PhoneColors.Surface, PhoneColors.Outgoing, lit), shape)
            .border(1.dp, lerpColor(PhoneColors.Outline, PhoneColors.Accent, maxOf(lit, press.value * 0.6f)), shape)
            .tactile(
                label = stringResource(R.string.reply_option, text),
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 800f)) } },
                onTap = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PhoneText(
            text,
            phoneText(15.sp, FontWeight.SemiBold).copy(textAlign = TextAlign.Center),
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            maxLines = 3,
        )
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = androidx.compose.ui.graphics.lerp(a, b, f.coerceIn(0f, 1f))

/** "Name is online / typing… / offline", announced to screen readers as it changes. */
@Composable
private fun Presence(name: String, typing: Boolean, live: Boolean) {
    val presence = when {
        typing -> R.string.presence_typing
        live -> R.string.presence_online
        else -> R.string.presence_offline
    }
    // out, then in: two centred lines of different lengths would smear into each other
    AnimatedContent(
        targetState = presence,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { fadeIn(tween(140, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
        label = "presence",
    ) { shown ->
        val state = stringResource(shown)
        val full = stringResource(R.string.presence, name, state)
        val text = buildAnnotatedString {
            append(full)
            val n = full.indexOf(name)
            if (n >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = PhoneColors.TextMuted), n, n + name.length)
            val s = full.lastIndexOf(state)
            val online = shown != R.string.presence_offline
            if (s >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = if (online) PhoneColors.TextMuted else PhoneColors.TextFaint), s, s + state.length)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PhoneText(
                text,
                phoneText(16.sp, color = PhoneColors.TextFaint),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** The contact's details, slid up over the conversation; drag it down or tap outside to close. */
@Composable
private fun ContactSheet(os: PhoneOs, thread: Thread) {
    val progress = os.contact
    val visible by remember { derivedStateOf { progress.value > 0f || progress.targetValue > 0f } }
    if (!visible) return
    var height by remember { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = 0.6f * progress.value.coerceIn(0f, 1f)) }
                .pointerInput(Unit) { detectTapGestures { os.closeContact() } },
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { height = it.height.toFloat() }
                .graphicsLayer { translationY = (1f - progress.value) * size.height }
                .background(PhoneColors.Header, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> os.dragContact(-delta / height) },
                    onDragStopped = { velocity ->
                        if (velocity > 900f || progress.value < 0.6f) os.closeContact() else os.openContact()
                    },
                )
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 22.dp)
                    .size(36.dp, 4.dp)
                    .background(PhoneColors.Outline, CircleShape),
            )
            Row(
                Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(thread.contact.face, 88.dp)
                Spacer(Modifier.width(22.dp))
                Column {
                    PhoneText(thread.contact.name, phoneText(22.sp, FontWeight.Bold))
                    Spacer(Modifier.height(3.dp))
                    PhoneText(thread.contact.subtitle, phoneText(14.sp, color = PhoneColors.TextFaint))
                    Spacer(Modifier.height(10.dp))
                    PhoneText(thread.contact.detail, phoneText(14.5.sp, FontWeight.SemiBold, PhoneColors.TextMuted))
                }
            }
        }
    }
}
