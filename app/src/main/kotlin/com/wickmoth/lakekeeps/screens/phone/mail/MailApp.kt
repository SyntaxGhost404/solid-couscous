package com.wickmoth.lakekeeps.screens.phone.mail

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.StoryCalendar
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.mail.Email
import com.wickmoth.lakekeeps.game.mail.Inboxes
import com.wickmoth.lakekeeps.game.messages.Face
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.screens.phone.AppHeader
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.GlyphIcon
import com.wickmoth.lakekeeps.screens.phone.HeaderButton
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneOs
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.PlaceholderImage
import com.wickmoth.lakekeeps.screens.phone.messages.Avatar
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.screens.phone.shake
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** The Mail app: the inbox, with a message sliding in over it. */
@Composable
internal fun MailApp(os: PhoneOs, fit: FrameFit) {
    var open by remember { mutableStateOf<Email?>(null) }
    // 0 = inbox, 1 = message slid fully in
    val shown = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current

    fun read(mail: Email) {
        os.feedback(Haptic.Tick)
        audio.play(Sfx.Tap, 0.5f)
        os.mail.open(mail)
        open = mail
        scope.launch { shown.animateTo(1f, tween(360, easing = Ease.Emphasized)) }
    }

    fun back() {
        if (open == null || shown.targetValue == 0f) return
        scope.launch {
            shown.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            open = null
        }
    }

    DesignScale(fit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.AppBackground),
        ) {
            Inbox(
                os,
                Modifier.graphicsLayer {
                    translationX = -0.28f * size.width * shown.value
                    alpha = 1f - 0.6f * shown.value
                },
                onRead = ::read,
            )
            open?.let { mail ->
                key(mail.id) {
                    Message(os, mail, Modifier.graphicsLayer { translationX = size.width * (1f - shown.value) }, onBack = ::back)
                }
            }
        }
    }
    PredictiveBackHandler(enabled = open == null && !os.shadeOpen) { gesture ->
        try {
            gesture.collect { os.peekBackFromApp(it.progress) }
            os.closeApp()
        } catch (cancelled: CancellationException) {
            os.restoreApp()
            throw cancelled
        }
    }
    PredictiveBackHandler(enabled = open != null && !os.shadeOpen) { gesture ->
        try {
            gesture.collect { event -> scope.launch { shown.snapTo(1f - 0.32f * event.progress) } }
            back()
        } catch (cancelled: CancellationException) {
            scope.launch { shown.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 500f)) }
            throw cancelled
        }
    }
}

@Composable
private fun Inbox(os: PhoneOs, modifier: Modifier, onRead: (Email) -> Unit) {
    val mails = Inboxes.of(os.owner)
    Column(
        modifier
            .fillMaxSize()
            .background(PhoneColors.AppBackground),
    ) {
        AppHeader {
            PhoneText(stringResource(R.string.app_mail), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
            HeaderButton(
                Glyph.Close,
                stringResource(R.string.close),
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            ) { os.closeApp() }
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "label") {
                PhoneText(
                    stringResource(R.string.mail_inbox).uppercase(),
                    phoneText(11.sp, FontWeight.Bold, PhoneColors.Accent).copy(letterSpacing = 0.12.em),
                    Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .padding(start = 22.dp, top = 18.dp, bottom = 6.dp),
                )
            }
            itemsIndexed(mails, key = { _, mail -> mail.id }) { i, mail ->
                MailRow(
                    mail = mail,
                    face = senderFace(os.owner, mail),
                    unread = os.mail.isUnread(mail),
                    calendar = os.owner.case.calendar,
                    appear = { window(os.appIn.value, 0.35f + i.coerceAtMost(5) * 0.05f, 0.4f, Ease.OutCubic) },
                ) { onRead(mail) }
            }
        }
    }
}

@Composable
private fun MailRow(mail: Email, face: Face, unread: Boolean, calendar: StoryCalendar, appear: () -> Float, onOpen: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val label = "${mail.sender}, ${mail.subject}".let { if (unread) stringResource(R.string.mail_unread, it) else it }
    Row(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .height(76.dp)
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
        Avatar(face, 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhoneText(
                    mail.sender,
                    phoneText(16.sp, if (unread) FontWeight.Bold else FontWeight.SemiBold),
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                PhoneText(arrived(mail, calendar), phoneText(12.sp, color = if (unread) PhoneColors.Accent else PhoneColors.TextFaint))
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhoneText(
                    mail.subject,
                    phoneText(14.sp, if (unread) FontWeight.SemiBold else FontWeight.Medium, if (unread) PhoneColors.Text else PhoneColors.TextMuted),
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (unread) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(PhoneColors.Accent, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun Message(os: PhoneOs, mail: Email, modifier: Modifier, onBack: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .background(PhoneColors.AppBackground),
    ) {
        AppHeader {
            HeaderButton(
                Glyph.Back,
                stringResource(R.string.back),
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
            ) { onBack() }
            PhoneText(stringResource(R.string.app_mail), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
            HeaderButton(
                Glyph.Close,
                stringResource(R.string.close),
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            ) { os.closeApp() }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 32.dp),
            ) {
                PhoneText(mail.subject, phoneText(21.sp, FontWeight.SemiBold).copy(lineHeight = 28.sp))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(senderFace(os.owner, mail), 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        PhoneText(mail.sender, phoneText(15.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        PhoneText(mail.address, phoneText(12.5.sp, color = PhoneColors.TextMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        PhoneText(
                            "${stringResource(R.string.mail_to_me)} · ${arrived(mail, os.owner.case.calendar, withTime = true)}",
                            phoneText(12.sp, color = PhoneColors.TextFaint),
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                mail.body.split("\n\n").forEach { paragraph ->
                    PhoneText(paragraph, phoneText(15.5.sp).copy(lineHeight = 23.sp))
                    Spacer(Modifier.height(14.dp))
                }
                mail.picture?.let { caption ->
                    Spacer(Modifier.height(4.dp))
                    PlaceholderImage(
                        mail.id,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(14.dp)),
                        glyphSize = 40.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    PhoneText(caption, phoneText(12.5.sp, color = PhoneColors.TextFaint))
                    Spacer(Modifier.height(14.dp))
                }
                mail.attachment?.let { Attachment(it) }
            }
        }
    }
}

/** A file attached to the message. The phone can't open it; it says so, with a shake. */
@Composable
private fun Attachment(name: String) {
    val audio = LocalAudio.current
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    var tried by remember { mutableStateOf(false) }
    val note by animateFloatAsState(if (tried) 1f else 0f, tween(260), label = "note")
    val shape = RoundedCornerShape(14.dp)
    Column {
        Row(
            Modifier
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .background(PhoneColors.Surface, shape)
                .border(1.dp, PhoneColors.Outline, shape)
                .tactile(stringResource(R.string.mail_attachment, name)) {
                    tried = true
                    audio.play(Sfx.Denied, 0.6f)
                    scope.launch { shake.shake() }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlyphIcon(Glyph.Attachment, PhoneColors.Accent, Modifier.size(20.dp))
            PhoneText(name, phoneText(14.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PhoneText(
            stringResource(R.string.mail_attachment_unavailable),
            phoneText(12.5.sp, color = PhoneColors.TextFaint),
            Modifier
                .padding(top = 8.dp)
                .graphicsLayer {
                    alpha = note
                    translationY = ((1f - note) * -4f).dp.toPx()
                },
        )
    }
}

/** When it arrived: the time today, "Yesterday", or the date; [withTime] adds the time to dates. */
@Composable
private fun arrived(mail: Email, calendar: StoryCalendar, withTime: Boolean = false): String {
    val time = formatClock(mail.minutes)
    return when (mail.daysAgo) {
        0 -> time
        1 -> stringResource(R.string.day_yesterday).let { if (withTime) "$it, $time" else it }
        else -> calendar.shortDate(mail.daysAgo).let { if (withTime) "$it, $time" else it }
    }
}

/**
 * Someone this phone already has a conversation with keeps the face they have in Messages;
 * anyone else gets their initial, the colour fixed by their address.
 */
private fun senderFace(owner: Owner, mail: Email): Face =
    Threads.of(owner).firstOrNull { it.contact.name == mail.sender }?.contact?.face
        ?: Face.Initial(mail.sender.first().uppercase(), SenderTints[mail.address.hashCode().mod(SenderTints.size)])

private val SenderTints = listOf(
    Color(0xFF8C6A4F),
    Color(0xFF2F7F7A),
    Color(0xFF5B6CA8),
    Color(0xFFA0525E),
    Color(0xFF6A8A4C),
    Color(0xFF8A6BAE),
)
