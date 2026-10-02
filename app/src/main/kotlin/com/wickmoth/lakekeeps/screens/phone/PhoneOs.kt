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
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.messages.Notice
import com.wickmoth.lakekeeps.game.messages.Thread
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.haptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Everything that moves inside a held phone: the Messages app window opening from where it was
 * launched, the inbox and chat, the notification shade and the heads-up banner.
 */
@Stable
class PhoneOs internal constructor(
    val owner: Owner,
    val messages: Messages,
    private val scope: CoroutineScope,
    private val audio: GameAudio,
    private val feedback: (Haptic) -> Unit,
) {
    /** The Messages window is on screen (including while it opens or closes). */
    var appOpen by mutableStateOf(false)
        private set

    /** 0 = shrunk into [launchedFrom], 1 = full screen. */
    val appIn = Animatable(0f)

    /** Where the app window grows from and shrinks back to, in window pixels. */
    var launchedFrom by mutableStateOf(Rect.Zero)
        private set

    /** The Messages icon on the home screen, where the app always shrinks back to when closed. */
    var appIcon = Rect.Zero

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

    fun feedback(kind: Haptic) = feedback.invoke(kind)

    fun openApp(from: Rect, thread: Thread? = null) {
        if (appOpen) {
            thread?.let(::openThread)
            return
        }
        launchedFrom = from
        this.thread = thread
        appOpen = true
        audio.play(Sfx.AppOpen)
        scope.launch {
            contact.snapTo(0f)
            chatIn.snapTo(if (thread != null) 1f else 0f)
            appIn.snapTo(0f)
            appIn.animateTo(1f, tween(440, easing = Ease.Emphasized))
        }
    }

    fun closeApp() {
        if (!appOpen || appIn.targetValue == 0f) return
        audio.play(Sfx.AppClose)
        if (appIcon != Rect.Zero) launchedFrom = appIcon
        scope.launch {
            appIn.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
            appOpen = false
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

    /** Opens a notification's conversation, closing whatever surface it was shown on. */
    fun openNotice(notice: Notice, from: Rect) {
        headsUpJob?.cancel()
        scope.launch {
            headsUpIn.snapTo(0f)
            headsUp = null
        }
        if (shade.targetValue > 0f) {
            scope.launch { shade.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f)) }
        }
        openApp(from, notice.thread)
    }

    private companion object {
        const val HEADS_UP_MS = 4500L
    }
}

@Composable
fun rememberPhoneOs(owner: Owner, messages: Messages): PhoneOs {
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val view = LocalView.current
    return remember(owner, messages) { PhoneOs(owner, messages, scope, audio) { view.haptic(it) } }
}
