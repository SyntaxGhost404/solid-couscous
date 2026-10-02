package com.wickmoth.lakekeeps.screens.phone.messages

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.messages.Day
import com.wickmoth.lakekeeps.game.messages.Thread
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.screens.phone.AppHeader
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.HeaderButton
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneOs
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.dayShort
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.screens.phone.preview
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** The Messages app: the inbox, with a conversation sliding in over it. */
@Composable
internal fun MessagesApp(os: PhoneOs, fit: FrameFit) {
    DesignScale(fit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.AppBackground),
        ) {
            Inbox(
                os,
                Modifier.graphicsLayer {
                    val c = os.chatIn.value
                    translationX = -0.28f * size.width * c
                    alpha = 1f - 0.6f * c
                },
            )
            os.thread?.let { thread ->
                key(thread.id) {
                    Chat(os, thread, Modifier.graphicsLayer { translationX = size.width * (1f - os.chatIn.value) })
                }
            }
        }
    }
    val inChat = os.thread != null
    PredictiveBackHandler(enabled = os.appOpen && !inChat && !os.shadeOpen) { gesture ->
        try {
            gesture.collect { os.peekBackFromApp(it.progress) }
            os.closeApp()
        } catch (cancelled: CancellationException) {
            os.restoreApp()
            throw cancelled
        }
    }
    val live = os.thread?.let(os.messages::isLive) == true
    PredictiveBackHandler(enabled = inChat && !live && !os.shadeOpen && !os.contactOpen) { gesture ->
        try {
            gesture.collect { os.peekBackFromThread(it.progress) }
            os.closeThread()
        } catch (cancelled: CancellationException) {
            os.restoreThread()
            throw cancelled
        }
    }
}

@Composable
private fun Inbox(os: PhoneOs, modifier: Modifier) {
    val threads = os.messages.inbox(os.owner)
    Column(modifier.fillMaxSize().background(PhoneColors.AppBackground)) {
        AppHeader {
            PhoneText(stringResource(R.string.messages_title), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
            HeaderButton(Glyph.Close, stringResource(R.string.close), Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) {
                os.closeApp()
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(threads, key = { _, t -> t.id }) { i, thread ->
                InboxRow(os, thread, appear = { window(os.appIn.value, 0.42f + i * 0.06f, 0.4f, Ease.OutCubic) }) {
                    os.feedback(Haptic.Tick)
                    os.openThread(thread)
                }
            }
        }
    }
}

@Composable
private fun InboxRow(os: PhoneOs, thread: Thread, appear: () -> Float, onOpen: () -> Unit) {
    val messages = os.messages
    val last = messages.lines(thread).last()
    val unread = messages.unread(thread)
    val online = messages.isLive(thread)
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val name = thread.contact.name
    val label = if (unread > 0) "$name, ${pluralStringResource(R.plurals.unread, unread, unread)}" else name
    Row(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .height(78.dp)
            .tactile(label, onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 800f)) } }, onTap = onOpen)
            .graphicsLayer {
                val a = appear()
                alpha = a
                translationY = ((1f - a) * 14f).dp.toPx()
                val s = 1f - 0.02f * press.value
                scaleX = s
                scaleY = s
            }
            .drawBehind { drawRect(PhoneColors.SurfacePressed, alpha = press.value) }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(thread.contact.face, 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhoneText(name, phoneText(16.5.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(7.dp))
                PresenceDot(online, 8.dp)
            }
            Spacer(Modifier.height(3.dp))
            val you = if (last.mine) "You: " else ""
            PhoneText(
                you + preview(last),
                phoneText(14.sp, if (unread > 0) FontWeight.SemiBold else FontWeight.Medium, if (unread > 0) PhoneColors.Text else PhoneColors.TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val time = if (last.day == Day.Today) formatClock(last.minutes) else dayShort(last.day, os.owner.case.calendar)
            PhoneText(time, phoneText(12.sp, color = if (unread > 0) PhoneColors.Accent else PhoneColors.TextFaint))
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (unread > 0) {
                    Box(Modifier.size(22.dp).background(PhoneColors.Accent, CircleShape), contentAlignment = Alignment.Center) {
                        PhoneText(unread.toString(), phoneText(11.sp, FontWeight.Bold, Color.White))
                    }
                }
            }
        }
    }
}

@Composable
internal fun PresenceDot(online: Boolean, size: Dp) {
    val color by animateColorAsState(if (online) PhoneColors.Online else PhoneColors.Offline, tween(400), label = "presence")
    Box(
        Modifier
            .size(size)
            .drawBehind { drawCircle(color) },
    )
}
