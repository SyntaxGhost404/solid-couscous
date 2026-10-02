package com.wickmoth.lakekeeps.screens.office

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import com.wickmoth.lakekeeps.audio.GameAudio
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.case.CaseProgress
import com.wickmoth.lakekeeps.game.littlebird.Column
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.LittleBird
import com.wickmoth.lakekeeps.game.littlebird.Symptom
import com.wickmoth.lakekeeps.game.littlebird.Verb
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.haptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What the player is doing on the board right now: Sam's second thought on show, the symptom card
 * being dragged or picked up, and the thread being tied. The case itself lives in [game]'s
 * progress; this only animates and checks moves against it.
 */
@Stable
internal class OfficeState(
    val game: GameState,
    private val scope: CoroutineScope,
    private val audio: GameAudio,
    private val feedback: (Haptic) -> Unit,
) {
    val progress: CaseProgress get() = game.progress

    // ---------------------------------------------------------------- Sam's second thoughts

    /** A line in Sam's hand on a strip of paper, for a few seconds. */
    var note by mutableStateOf<String?>(null)
        private set
    val noteIn = Animatable(0f)
    private var noteJob: Job? = null

    fun say(text: String) {
        noteJob?.cancel()
        note = text
        noteJob = scope.launch {
            noteIn.animateTo(1f, tween(260))
            delay(NOTE_MS)
            noteIn.animateTo(0f, tween(360))
            note = null
        }
    }

    // ---------------------------------------------------------------- sorting Amy's symptoms

    fun isSorted(card: Symptom) = progress.has(Flag.sorted(card.id))

    /** Top-left of each card in design dp: in the tray, in a hand, or pinned in its column. */
    val positions: Map<Symptom, Animatable<Offset, AnimationVector2D>> = Symptom.entries.associateWith { card ->
        Animatable(if (isSorted(card)) OfficeLayout.slot(card).topLeft else OfficeLayout.traySlot(card).topLeft, Offset.VectorConverter)
    }

    /** 0 = tray size, 1 = full size and pinned. */
    val placed: Map<Symptom, Animatable<Float, AnimationVector1D>> = Symptom.entries.associateWith { card ->
        Animatable(if (isSorted(card)) 1f else 0f)
    }

    var dragging by mutableStateOf<Symptom?>(null)
    var selected by mutableStateOf<Symptom?>(null)

    fun lift(card: Symptom) {
        if (isSorted(card)) return
        feedback(Haptic.Tick)
        audio.play(Sfx.Paper, 0.45f)
        selected = if (selected == card) null else card
    }

    fun drag(card: Symptom, by: Offset) {
        dragging = card
        selected = null
        scope.launch { positions.getValue(card).snapTo(positions.getValue(card).value + by) }
    }

    /** Lets go of a dragged card: into the column under its middle, or back to the tray. */
    fun drop(card: Symptom) {
        dragging = null
        val size = OfficeLayout.traySize
        val middle = positions.getValue(card).value + Offset(size.width / 2f, size.height / 2f)
        val column = Column.entries.firstOrNull { OfficeLayout.dropZone(it).contains(middle) }
        if (column != null) place(card, column) else backToTray(card)
    }

    /** Puts [card] under [column]: it pins there if that's where it belongs, or Sam thinks again. */
    fun place(card: Symptom, column: Column) {
        selected = null
        if (isSorted(card)) return
        if (card.column == column) {
            progress.set(Flag.sorted(card.id))
            audio.play(Sfx.Pin, 0.6f)
            feedback(Haptic.Confirm)
            scope.launch { placed.getValue(card).animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 380f)) }
            scope.launch { positions.getValue(card).animateTo(OfficeLayout.slot(card).topLeft, spring(dampingRatio = 0.78f, stiffness = 300f)) }
            if (Symptom.entries.all(::isSorted)) progress.set(Flag.SORTED)
        } else {
            audio.play(Sfx.Denied, 0.5f)
            feedback(Haptic.Reject)
            say(card.notes[column] ?: DOES_NOT_HOLD)
            backToTray(card)
        }
    }

    private fun backToTray(card: Symptom) {
        scope.launch {
            positions.getValue(card).animateTo(OfficeLayout.traySlot(card).topLeft, spring(dampingRatio = 0.72f, stiffness = 260f))
        }
    }

    // ---------------------------------------------------------------- tying threads

    /** The spool is out: taps pick things to tie together instead of looking at them. */
    var connecting by mutableStateOf(false)
        private set

    /** The first thing picked, and then the pair waiting for its verb. */
    var first by mutableStateOf<String?>(null)
        private set
    var pair by mutableStateOf<Pair<String, String>?>(null)
        private set

    /** A thread that didn't hold, falling away from [failed]'s pin. */
    var failed by mutableStateOf<String?>(null)
        private set
    val fall = Animatable(0f)

    fun toggleConnect() {
        feedback(Haptic.Tick)
        audio.play(Sfx.Paper, 0.5f)
        connecting = !connecting
        first = null
        pair = null
    }

    fun cancelConnect() {
        if (pair != null) {
            pair = null
            first = null
        } else {
            connecting = false
            first = null
        }
    }

    fun pick(id: String) {
        if (!connecting || pair != null) return
        val a = first
        feedback(Haptic.Tick)
        when {
            a == null -> {
                first = id
                audio.play(Sfx.Paper, 0.5f)
            }
            a == id -> first = null
            else -> pair = LittleBird.sentence(a, id)
        }
    }

    fun choose(verb: Verb) {
        val (a, b) = pair ?: return
        pair = null
        first = null
        val link = LittleBird.holds(a, b, verb)
        if (link != null && !progress.has(Flag.linked(link.id))) {
            progress.set(Flag.linked(link.id))
            audio.play(Sfx.Thread)
            feedback(Haptic.Confirm)
            if (LittleBird.act1Done(progress)) {
                progress.set(Flag.ACT1_DONE)
                connecting = false
            }
        } else if (link != null) {
            say("Already tied.")
        } else {
            audio.play(Sfx.Denied, 0.5f)
            feedback(Haptic.Reject)
            say(LittleBird.linkNote(a, b, verb))
            failed = a
            scope.launch {
                fall.snapTo(0f)
                fall.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
                failed = null
            }
        }
    }

    companion object {
        private const val NOTE_MS = 2800L
        const val DOES_NOT_HOLD = "Does not hold."
    }
}

@Composable
internal fun rememberOfficeState(game: GameState): OfficeState {
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val view = LocalView.current
    return remember(game) { OfficeState(game, scope, audio) { view.haptic(it) } }
}
