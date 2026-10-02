package com.wickmoth.lakekeeps.screens.phone.calls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wickmoth.lakekeeps.audio.GameAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.phone.CallLog
import com.wickmoth.lakekeeps.game.phone.PhoneBook
import com.wickmoth.lakekeeps.game.phone.dialable
import com.wickmoth.lakekeeps.screens.phone.shake
import com.wickmoth.lakekeeps.ui.Haptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A call in progress: the number, the saved name for it if there is one, and whether it has ended. */
@Immutable
data class OngoingCall(val number: String, val name: String?, val ended: Boolean = false)

/**
 * The Phone app: which page shows, the number being typed, and the call in progress. Nobody picks
 * up yet, so a call rings out unanswered unless the player hangs up first.
 */
@Stable
class Dialer internal constructor(
    private val owner: Owner,
    private val log: CallLog,
    private val clock: () -> Int,
    private val scope: CoroutineScope,
    private val audio: GameAudio,
    private val feedback: (Haptic) -> Unit,
) {
    enum class Page { Recents, Keypad }

    var page by mutableStateOf(Page.Recents)
        private set

    /** The number typed on the keypad. */
    var digits by mutableStateOf("")
        private set

    var call by mutableStateOf<OngoingCall?>(null)
        private set

    /** A call is ringing, or has just ended and is still on screen. */
    val inCall: Boolean get() = call != null

    /** Jumps to 1 as each ring starts and fades back to 0 over the ring. */
    val ring = Animatable(0f)

    /** Shakes the number display when the dialer refuses something. */
    val nudge = Animatable(0f)

    private var placedFrom = Page.Recents
    private var ringing: Job? = null
    private var ringSound = 0

    fun show(page: Page) {
        if (inCall || this.page == page) return
        this.page = page
        audio.play(Sfx.Tap, 0.5f)
        feedback(Haptic.Tick)
    }

    /** A keypad key: it sounds its tone, and adds to the number unless a call is on. */
    fun press(key: Char) {
        audio.play(Sfx.key(key), 0.8f)
        feedback(Haptic.Tick)
        if (!inCall && digits.length < MAX_DIGITS) digits += key
    }

    fun delete() {
        if (inCall || digits.isEmpty()) return
        digits = digits.dropLast(1)
        feedback(Haptic.Tick)
    }

    fun clear() {
        if (inCall || digits.isEmpty()) return
        digits = ""
        feedback(Haptic.Press)
    }

    /** The call button under the keypad. */
    fun dial() {
        if (inCall) return
        if (digits.isEmpty()) refuse() else call(digits)
    }

    /** Rings [number]; the call plays out on the keypad page, then returns to where it was placed from. */
    fun call(number: String) {
        if (inCall) return
        val dialled = dialable(number)
        if (dialled.isEmpty()) return
        placedFrom = page
        page = Page.Keypad
        log.place(owner, dialled, clock())
        call = OngoingCall(dialled, PhoneBook.find(owner, dialled)?.name)
        feedback(Haptic.Confirm)
        ringing = scope.launch {
            delay(CONNECT_MS)
            repeat(RINGS) {
                ringSound = audio.start(Sfx.Ringback, 0.85f)
                launch {
                    ring.snapTo(1f)
                    ring.animateTo(0f, tween(RING_MS.toInt(), easing = LinearEasing))
                }
                delay(RING_MS)
            }
            finish()
        }
    }

    fun hangUp() {
        if (call?.ended != false) return
        ringing?.cancel()
        finish()
    }

    /** No: the player tried to call nothing, or to leave mid-call. */
    fun refuse() {
        audio.play(Sfx.Denied, 0.6f)
        feedback(Haptic.Reject)
        scope.launch { nudge.shake() }
    }

    private fun finish() {
        audio.stop(ringSound)
        ringSound = 0
        audio.play(Sfx.CallEnd)
        feedback(Haptic.Press)
        call = call?.copy(ended = true)
        scope.launch {
            ring.snapTo(0f)
            delay(ENDED_MS)
            call = null
            if (placedFrom == Page.Keypad) digits = ""
            page = placedFrom
        }
    }

    private companion object {
        const val MAX_DIGITS = 15
        const val CONNECT_MS = 1200L
        const val RING_MS = 3000L
        const val RINGS = 3
        const val ENDED_MS = 1600L
    }
}
