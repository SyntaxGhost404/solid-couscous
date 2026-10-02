package com.wickmoth.lakekeeps.screens.board

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.screens.phone.BackPeek
import com.wickmoth.lakekeeps.screens.phone.PhoneOverlay
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.rememberFrameFit
import com.wickmoth.lakekeeps.ui.rememberSequence
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** A phone was just set back down on the desk ([count] increments each time). */
@Immutable
data class Landing(val owner: Owner?, val count: Int)

private const val LIFT_MS = 620
private const val PUT_DOWN_MS = 520
private const val CONTENT_MS = 760

/** The case board with its desk, and whichever phone has been picked up from it. */
@Composable
fun CaseFile(state: GameState) {
    val fit = rememberFrameFit(Alignment.BottomCenter)
    val firstVisit = remember { !state.boardSettled }
    val intro = rememberSequence(BOARD_INTRO, startAt = if (firstVisit) 0f else BOARD_INTRO) {
        state.boardSettled = true
    }
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val view = LocalView.current
    val restored = state.phone
    val lift = remember { Animatable(if (restored != null) 1f else 0f) }
    val content = remember { Animatable(if (restored != null) 1f else 0f) }
    val peek = remember { Animatable(0f) }
    var peekFromLeft by remember { mutableStateOf(true) }
    var shown by remember { mutableStateOf(restored) }
    var moving by remember { mutableStateOf(false) }
    var landing by remember { mutableStateOf(Landing(null, 0)) }

    fun pickUp(owner: Owner) {
        if (moving || shown != null || intro.t < 900f) return
        moving = true
        shown = owner
        state.phone = owner
        view.haptic(Haptic.Press)
        audio.play(Sfx.Pickup)
        scope.launch {
            launch {
                delay(300)
                content.animateTo(1f, tween(CONTENT_MS, easing = LinearEasing))
            }
            lift.animateTo(1f, tween(LIFT_MS, easing = Ease.Emphasized))
            moving = false
        }
    }

    fun putDown() {
        val owner = shown
        if (moving || owner == null) return
        moving = true
        scope.launch {
            launch { content.animateTo(0f, tween(200, easing = LinearEasing)) }
            launch { peek.animateTo(0f, tween(420)) }
            delay(110)
            lift.animateTo(0f, tween(PUT_DOWN_MS, easing = FastOutSlowInEasing))
            audio.play(Sfx.Putdown)
            view.haptic(Haptic.Tick)
            shown = null
            state.phone = null
            landing = Landing(owner, landing.count + 1)
            moving = false
        }
    }

    PredictiveBackHandler(enabled = shown != null && !moving) { gestures ->
        try {
            gestures.collect { event ->
                peekFromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT
                peek.snapTo(event.progress)
            }
            putDown()
        } catch (cancelled: CancellationException) {
            scope.launch { peek.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f)) }
            throw cancelled
        }
    }

    Box(Modifier.fillMaxSize()) {
        CaseBoard(
            fit = fit,
            intro = intro,
            introCues = firstVisit,
            hidden = shown,
            landing = landing,
            cover = { lift.value },
            onPickUp = ::pickUp,
        )
        shown?.let { owner ->
            PhoneOverlay(
                owner = owner,
                messages = state.messages,
                calls = state.calls,
                boardFit = fit,
                lift = { lift.value },
                content = { content.value },
                back = { BackPeek(peek.value, peekFromLeft) },
                onBack = ::putDown,
            )
        }
    }
}
