package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalView
import com.wickmoth.lakekeeps.audio.GameAudio
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.mail.MailBox
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.messages.Notice
import com.wickmoth.lakekeeps.game.messages.Thread
import com.wickmoth.lakekeeps.game.phone.CallLog
import com.wickmoth.lakekeeps.screens.phone.calls.Dialer
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.haptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The apps that open on the in-game phones. */
enum class PhoneApp { Messages, Calls, Mail, Gallery, Settings }

/**
 * Everything that moves inside a held phone: an app's window opening from where it was launched,
 * the Messages inbox and chat, the Phone app's dialer, the notification shade and the banner.
 * [state] is the whole game, which the phones' apps read and the Settings app can start over.
 */
@Stable
class PhoneOs internal constructor(
    val owner: Owner,
    val state: GameState,
    private val scope: CoroutineScope,
    private val audio: GameAudio,
    private val feedback: (Haptic) -> Unit,
) {
    val messages: Messages = state.messages
    val calls: CallLog = state.calls
    val mail: MailBox = state.mail

    /** The app on screen (including while its window opens or closes), if any. */
    var app by mutableStateOf<PhoneApp?>(null)
        private set

    val appOpen: Boolean get() = app != null

    /** 0 = shrunk into [launchedFrom], 1 = full screen. */
    val appIn = Animatable(0f)

    /** Where the app window grows from and shrinks back to, in window pixels. */
    var launchedFrom by mutableStateOf(Rect.Zero)
        private set

    /** Each app's icon on the home screen, where its window always shrinks back to when closed. */
    var appIcons: Map<PhoneApp, Rect> = emptyMap()

    /** The Phone app's pages, number and call. */
    val dialer = Dialer(owner, calls, { messages.clock(owner.case) }, scope, audio, feedback)

    /** The open conversation; null shows the inbox. */
    var thread by mutableStateOf<Thread?>(null)
        private set

    /** 0 = inbox, 1 = conversation slid fully in. */
    val chatIn = Animatable(0f)

    /** 0 = contact details hidden, 1 = slid fully up over the conversation. */
    val contact = Animatable(0f)

    /** 0 = notification shade closed, 1 = fully pulled down. */
    val shade = Animatable(0f)

    var headsUp by mutableStateOf<Notice?>(null)
        private set

    val headsUpIn = Animatable(0f)
    private var headsUpJob: Job? = null

    val shadeOpen: Boolean get() = shade.targetValue > 0f

    val contactOpen: Boolean get() = contact.targetValue > 0f

    /**
     * A call or a live conversation has the player's attention: they can't put the phone down or
     * jump to a notification until it is over.
     */
    val busy: Boolean
        get() = dialer.inCall || (app == PhoneApp.Messages && thread?.let(messages::isLive) == true)

    fun feedback(kind: Haptic) = feedback.invoke(kind)

    /** Opens [app] out of [from] (optionally straight into [thread]); another open app gives way. */
    fun openApp(app: PhoneApp, from: Rect, thread: Thread? = null) {
        if (this.app == app) {
            thread?.let(::openThread)
            return
        }
        val switching = this.app != null
        audio.play(Sfx.AppOpen)
        if (!switching) show(app, from, thread)
        scope.launch {
            if (switching) {
                // the other app's window closes at once, under the new one growing in
                appIn.snapTo(0f)
                show(app, from, thread)
            }
            contact.snapTo(0f)
            chatIn.snapTo(if (thread != null) 1f else 0f)
            appIn.snapTo(0f)
            appIn.animateTo(1f, tween(440, easing = Ease.Emphasized))
        }
    }

    private fun show(app: PhoneApp, from: Rect, thread: Thread?) {
        launchedFrom = from
        this.app = app
        this.thread = thread
    }

    fun closeApp() {
        val app = app ?: return
        if (appIn.targetValue == 0f) return
        audio.play(Sfx.AppClose)
        appIcons[app]?.let { launchedFrom = it }
        scope.launch {
            appIn.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
            this@PhoneOs.app = null
            thread = null
            chatIn.snapTo(0f)
        }
    }

    fun openThread(thread: Thread) {
        this.thread = thread
        scope.launch {
            contact.snapTo(0f)
            chatIn.animateTo(1f, tween(360, easing = Ease.Emphasized))
        }
    }

    fun closeThread() {
        if (thread == null || chatIn.targetValue == 0f) return
        scope.launch {
            chatIn.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            thread = null
        }
    }

    /** Follows a predictive back gesture out of the conversation ([progress] 0..1). */
    fun peekBackFromThread(progress: Float) {
        scope.launch { chatIn.snapTo(1f - 0.32f * progress) }
    }

    fun restoreThread() {
        scope.launch { chatIn.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 500f)) }
    }

    /** Follows a predictive back gesture out of the app ([progress] 0..1). */
    fun peekBackFromApp(progress: Float) {
        scope.launch { appIn.snapTo(1f - 0.12f * progress) }
    }

    fun restoreApp() {
        scope.launch { appIn.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 500f)) }
    }

    fun openContact() {
        scope.launch { contact.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 380f)) }
    }

    fun closeContact() {
        scope.launch { contact.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f)) }
    }

    /** Moves the contact details with a finger; [fraction] is the drag as a share of the sheet's height. */
    fun dragContact(fraction: Float) {
        scope.launch { contact.snapTo((contact.value + fraction).coerceIn(0f, 1f)) }
    }

    fun openShade() {
        if (shade.targetValue == 1f && shade.value == 1f) return
        audio.play(Sfx.AppOpen, 0.45f)
        scope.launch { shade.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 360f)) }
    }

    fun closeShade() {
        if (shade.targetValue == 0f && shade.value == 0f) return
        audio.play(Sfx.AppClose, 0.45f)
        scope.launch { shade.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f)) }
    }

    /** Moves the shade with a finger; [fraction] is the drag as a share of the panel's height. */
    fun dragShade(fraction: Float) {
        scope.launch { shade.snapTo((shade.value + fraction).coerceIn(0f, 1f)) }
    }

    /** Lets a dragged shade settle open or shut; [velocity] in panel heights per second. */
    fun settleShade(velocity: Float) {
        val open = velocity > 1.2f || (velocity > -1.2f && shade.value > 0.4f)
        if (open) openShade() else closeShade()
    }

    /** Slides a banner in for a message that arrived outside its conversation. */
    fun postHeadsUp(notice: Notice) {
        headsUpJob?.cancel()
        headsUp = notice
        audio.play(Sfx.Notify)
        feedback(Haptic.Confirm)
        headsUpJob = scope.launch {
            headsUpIn.snapTo(0f)
            headsUpIn.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 420f))
            delay(HEADS_UP_MS)
            headsUpIn.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            headsUp = null
        }
    }

    fun dismissHeadsUp() {
        val job = headsUpJob ?: return
        job.cancel()
        headsUpJob = scope.launch {
            headsUpIn.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            headsUp = null
        }
    }

    /**
     * Opens a notification's conversation, closing whatever surface it was shown on. While the
     * player is [busy] it refuses instead and returns false, so the notification can shake.
     */
    fun openNotice(notice: Notice, from: Rect): Boolean {
        if (busy) {
            audio.play(Sfx.Denied, 0.6f)
            feedback(Haptic.Reject)
            return false
        }
        headsUpJob?.cancel()
        scope.launch {
            headsUpIn.snapTo(0f)
            headsUp = null
        }
        if (shade.targetValue > 0f) {
            scope.launch { shade.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f)) }
        }
        openApp(PhoneApp.Messages, from, notice.thread)
        return true
    }

    private companion object {
        const val HEADS_UP_MS = 4500L
    }
}

@Composable
fun rememberPhoneOs(owner: Owner, state: GameState): PhoneOs {
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val view = LocalView.current
    return remember(owner, state) { PhoneOs(owner, state, scope, audio) { view.haptic(it) } }
}
