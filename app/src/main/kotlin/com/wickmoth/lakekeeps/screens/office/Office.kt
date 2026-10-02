package com.wickmoth.lakekeeps.screens.office

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.SamsPhone
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.screens.board.Backdrop
import com.wickmoth.lakekeeps.screens.board.Landing
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Sequence
import com.wickmoth.lakekeeps.ui.rememberIdleClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/** A tool opened from the desk: the notebook at a page, or the evidence files (at one of them). */
sealed interface DeskTool {
    data class Notebook(val page: NotebookPage) : DeskTool
    data class Files(val evidence: Evidence?) : DeskTool
}

enum class NotebookPage { Case, Intake, Memos }

/** A contact who writes first waits this long after their part of the case opens. */
private const val OPENER_DELAY_MS = 2600L

/** And stays online this long after a line with nothing to follow it. */
private const val SIGN_OFF_MS = 1500L

/**
 * Sam's office in Little Bird: the board, bare when the case opens and filling in as it goes, and
 * the desk with his phone (the only one), his notebook and the tray where evidence lands. [cover]
 * (0..1) is the phone or a desk tool coming up over it. [onOpen] opens a desk tool from a spot on
 * the office, in window pixels.
 */
@Composable
internal fun Office(
    state: GameState,
    fit: FrameFit,
    intro: Sequence,
    introCues: Boolean,
    hidden: Owner?,
    landing: Landing,
    cover: () -> Float,
    toolOpen: Boolean,
    onPickUp: (Owner) -> Unit,
    onOpen: (DeskTool, Rect) -> Unit,
) {
    val office = rememberOfficeState(state)
    val covered by remember { derivedStateOf { cover() >= 0.999f } }
    val idle by rememberIdleClock(running = !covered)
    fun window(r: Rect) = Rect(fit.toWindow(r.left, r.top), fit.toWindow(r.right, r.bottom))
    val open: (DeskTool, Rect) -> Unit = { tool, rect -> onOpen(tool, window(rect)) }

    CaseDirector(state)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val c = cover()
                alpha = if (c >= 0.999f) 0f else 1f
                val s = 1f - 0.035f * c
                scaleX = s
                scaleY = s
            }
            .drawWithContent {
                drawContent()
                drawRect(Color.Black, alpha = 0.62f * cover())
            }
            // under the phone or a tool, the office is out of sight for screen readers too
            .then(if (covered) Modifier.clearAndSetSemantics { } else Modifier),
    ) {
        Backdrop(fit, intro)
        DesignFrame(fit) {
            val visible = !covered
            OfficeBoard(office, intro, introCues, visible, open)
            OfficeDesk(state, office, intro, { idle }, hidden, landing, visible, onPickUp, open)
        }
    }
    BackHandler(enabled = office.connecting && hidden == null && !toolOpen) { office.cancelConnect() }
}

/**
 * Plays something in when it first becomes [shown] while the office is [visible] (after
 * [delayMs]); anything already shown when the office opens is simply there.
 */
@Composable
internal fun reveal(shown: Boolean, visible: Boolean, delayMs: Long = 0L, ms: Int = 520, onStart: () -> Unit = {}): Animatable<Float, AnimationVector1D> {
    val a = remember { Animatable(if (shown) 1f else 0f) }
    val start by rememberUpdatedState(onStart)
    LaunchedEffect(shown, visible) {
        if (!shown) {
            a.snapTo(0f)
            return@LaunchedEffect
        }
        if (!visible || a.value >= 1f) return@LaunchedEffect
        delay(delayMs)
        if (a.value == 0f) start()
        a.animateTo(1f, tween((ms * (1f - a.value)).toInt().coerceAtLeast(1), easing = LinearEasing))
    }
    return a
}

/**
 * The case moving on by itself: it opens once the office is laid out, and a contact whose part
 * starts with their own message writes a moment after it opens (unless their conversation is on
 * screen, where the chat plays it).
 */
@Composable
private fun CaseDirector(state: GameState) {
    val progress = state.progress
    val messages = state.messages
    LaunchedEffect(Unit) {
        snapshotFlow { state.boardSettled }.first { it }
        progress.set(Flag.CASE_OPEN)
    }
    LaunchedEffect(Unit) {
        snapshotFlow {
            Threads.of(Owner.Sam).firstOrNull { thread ->
                val next = messages.nextLine(thread) ?: return@firstOrNull false
                !next.mine && !messages.isLive(thread) && state.openThread != thread.id
            }
        }.collectLatest { thread ->
            if (thread == null) return@collectLatest
            delay(OPENER_DELAY_MS)
            messages.deliver(thread)
            // someone who only had a line to say goes offline again, read or not
            if (messages.isOver(thread)) {
                delay(SIGN_OFF_MS)
                if (state.openThread != thread.id) messages.signOff(thread)
            }
        }
    }
}

/** The files that have reached Sam so far, in the order they came. */
internal fun received(state: GameState): List<Evidence> {
    val sent = Threads.of(Owner.Sam).flatMap { state.messages.lines(it) }.mapNotNull { it.attachment }
    return sent.mapNotNull(Evidence::byId).distinct()
}

/** When [evidence] reached Sam, as a time of day in minutes, if it has. */
internal fun receivedAt(state: GameState, evidence: Evidence): Int? =
    state.messages.lines(SamsPhone.AmyHart).firstOrNull { it.attachment == evidence.id }?.minutes
