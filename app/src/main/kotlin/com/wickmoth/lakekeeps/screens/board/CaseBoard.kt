package com.wickmoth.lakekeeps.screens.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.Sequence
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.rememberIdleClock
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Length of the board's lay-out intro, in ms. */
internal const val BOARD_INTRO = 3800f

private const val CARDS_AT = 450f
private const val CARD_STEP = 95f
private const val THREADS_AT = 1500f
private const val NOTES_AT = 1900f
private const val DASHES_AT = 2400f
private const val MARK_AT = 2200f
private const val SCRIBBLE_AT = 3000f

private fun cardDrop(t: Float, order: Int) = window(t, CARDS_AT + order * CARD_STEP, 460f, Ease.OutCubic)
private fun cardShown(t: Float, order: Int) = window(t, CARDS_AT + order * CARD_STEP, 160f)
private fun pinPop(t: Float, order: Int) = window(t, CARDS_AT + order * CARD_STEP + 380f, 170f, Ease.OutBack)
private fun pinTime(order: Int) = CARDS_AT + order * CARD_STEP + 380f

/** Intro slots for the two clues that are pinned up alongside the cards. */
private val TagOrder = Board.pinOrder.indexOfFirst { it.person == Person.Heron } + 0.5f
private val NoteOrder = Board.pinOrder.size.toFloat()

/**
 * The case board and desk. [intro] is the lay-out sequence and [introCues] whether its sounds
 * play (not when the board is restored already laid out); [cover] (0..1) is how far a phone has
 * been lifted over the board, which pushes the board back and dims it.
 */
@Composable
internal fun CaseBoard(
    fit: FrameFit,
    intro: Sequence,
    introCues: Boolean,
    hidden: Owner?,
    landing: Landing,
    cover: () -> Float,
    onPickUp: (Owner) -> Unit,
) {
    val audio = LocalAudio.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val covered by remember { derivedStateOf { cover() >= 0.999f } }
    val idle by rememberIdleClock(running = !covered)

    val swings = remember { Person.entries.associateWith { Animatable(0f) } }
    val focusPulse = remember { Animatable(0f) }
    var focus by remember { mutableStateOf<Person?>(null) }

    // Intro sound cues: a pin pressed home for each card, then the marker circling the stranger.
    LaunchedEffect(intro) {
        if (!introCues) return@LaunchedEffect
        val cues = (Board.pinOrder.indices.map { pinTime(it) to Sfx.Pin } + (SCRIBBLE_AT to Sfx.Scribble)).sortedBy { it.first }
        var next = 0
        snapshotFlow { intro.t }.collect { t ->
            while (next < cues.size && t >= cues[next].first) {
                if (t - cues[next].first < 250f) audio.play(cues[next].second, if (cues[next].second == Sfx.Pin) 0.45f else 0.8f)
                next++
            }
        }
    }

    fun tap(person: Person, fromRight: Boolean, doublePinned: Boolean) {
        view.haptic(Haptic.Press)
        audio.play(Sfx.Paper, 0.6f)
        focus = person
        scope.launch {
            focusPulse.snapTo(0f)
            focusPulse.animateTo(1f, tween(1300, easing = LinearEasing))
        }
        if (!doublePinned) {
            scope.launch {
                swings.getValue(person).animateTo(0f, spring(dampingRatio = 0.16f, stiffness = 110f), initialVelocity = if (fromRight) 75f else -75f)
            }
        }
    }

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
            },
    ) {
        Backdrop(fit, intro)
        DesignFrame(fit) {
            val lit = { person: Person? ->
                if (person != null && person == focus) sin(PI.toFloat() * focusPulse.value) else 0f
            }
            BoardSurface(intro, lit)
            Board.pinOrder.forEachIndexed { order, card ->
                PinnedCard(card, order.toFloat(), intro, swings.getValue(card.person)) { fromRight -> tap(card.person, fromRight, card.doublePinned) }
            }
            HandleTag(intro, swings.getValue(Person.Heron)) { fromRight -> tap(Person.Heron, fromRight, false) }
            CrumpledNote(intro)
            Threads(intro, focus = { focus }, pulse = { focusPulse.value })
            Desk(
                intro = { intro.t },
                idle = { idle },
                hidden = hidden,
                landing = landing,
                onPickUp = onPickUp,
            )
        }
    }
}

/** The slate board and the desk, painted once per window size, fading up with the [intro]. */
@Composable
internal fun Backdrop(fit: FrameFit, intro: Sequence) {
    val density = LocalDensity.current
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = window(intro.t, 0f, 600f, Ease.InOutSine) }
            .drawWithCache {
                val deskTop = fit.toWindow(0f, DESK_TOP).y
                val bitmap = paintBackdrop(size.width.toInt(), size.height.toInt(), deskTop, fit.unit, density)
                onDrawBehind { drawImage(bitmap) }
            },
    )
}

/** Notes, chalk dashes and the marker symbol: everything written straight onto the board. */
@Composable
private fun BoxScope.BoardSurface(intro: Sequence, lit: (Person?) -> Float) {
    Canvas(Modifier.fillMaxSize()) {
        val t = intro.t
        Board.dashes.forEachIndexed { i, dash ->
            drawDash(dash.scaled(density), window(t, DASHES_AT + i * 140f, 450f, Ease.InOutSine), density)
        }
        val origin = Board.mark - Offset(Board.MARK_SIZE / 2f, Board.MARK_SIZE / 2f)
        Mark.paths(origin * density, density).forEachIndexed { i, path ->
            drawPartial(path, window(t, MARK_AT + i * 170f, 300f + i * 40f, Ease.InOutSine), Color(0xFFB8303B), 2.1f * density, alpha = 0.92f)
        }
    }
    val markLabel = stringResource(R.string.clue_mark)
    Box(
        Modifier
            .offset((Board.mark.x - Board.MARK_SIZE / 2f).dp, (Board.mark.y - Board.MARK_SIZE / 2f).dp)
            .size(Board.MARK_SIZE.dp)
            .semantics { contentDescription = markLabel },
    )
    Board.notes.forEachIndexed { i, note -> BoardNote(note, i, intro, lit) }
}

private fun Dash.scaled(k: Float) = Dash(from * k, to * k)

@Composable
private fun BoxScope.BoardNote(note: Note, index: Int, intro: Sequence, lit: (Person?) -> Float) {
    val text = stringResource(note.text)
    val start = NOTES_AT + index * 90f
    val duration = 150f + text.length * 28f
    val color = if (note.red) Palette.Marker else Palette.Chalk
    // Measured box centred on the anchor, then rotated and revealed left to right as if written.
    Box(
        Modifier
            .offset((note.at.x - 90f).dp, (note.at.y - 14f).dp)
            .size(180.dp, 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Normal, fontSize = note.size.sp, color = color),
            softWrap = false,
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .graphicsLayer {
                    rotationZ = note.tilt
                    val glow = lit(note.about)
                    alpha = (if (note.red) 0.95f else 0.82f) + 0.18f * glow
                    val s = 1f + 0.05f * glow
                    scaleX = s
                    scaleY = s
                }
                .drawWithContent {
                    val p = window(intro.t, start, duration, LinearEasing)
                    if (p >= 1f) {
                        drawContent()
                    } else if (p > 0f) {
                        clipRect(right = size.width * p) { this@drawWithContent.drawContent() }
                    }
                },
        )
    }
}

/** A polaroid hung on its pin. Taps set it swinging; double-pinned cards just flex. */
@Composable
private fun BoxScope.PinnedCard(
    card: Card,
    order: Float,
    intro: Sequence,
    swing: Animatable<Float, AnimationVector1D>,
    onTap: (fromRight: Boolean) -> Unit,
) {
    val name = stringResource(card.person.label)
    val photoLabel = stringResource(R.string.photo_of, name)
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val tap by rememberUpdatedState(onTap)
    val size = card.size
    val photo = size.width - 10f
    val o = order.toInt()
    Box(
        Modifier
            .offset(card.topLeft.x.dp, card.topLeft.y.dp)
            .size(size.width.dp, size.height.dp)
            .graphicsLayer {
                val t = intro.t
                val d = cardDrop(t, o)
                alpha = cardShown(t, o)
                transformOrigin = TransformOrigin(0.5f, PIN_INSET / size.height)
                rotationZ = card.tilt + swing.value + (1f - d) * 7f
                val s = lerp(1.12f, 1f, d) * (1f - 0.03f * press.value)
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                val lift = 1f - cardDrop(intro.t, o)
                cardShadow(this.size, lift, density)
                drawRect(Palette.Polaroid)
            }
            .semantics(mergeDescendants = true) {
                contentDescription = photoLabel
                role = Role.Button
                onClick { tap(false); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { press.animateTo(1f, spring(stiffness = 900f)) }
                        tryAwaitRelease()
                        scope.launch { press.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 500f)) }
                    },
                    onTap = { at -> tap(at.x > this.size.width / 2f) },
                )
            },
    ) {
        Image(
            painter = painterResource(card.person.photo),
            contentDescription = null,
            modifier = Modifier
                .offset(5.dp, 5.dp)
                .size(photo.dp),
        )
        BasicText(
            text = name,
            style = TextStyle(fontFamily = Fonts.Marker, fontSize = if (card.doublePinned) 18.sp else 15.sp, color = Palette.PolaroidInk),
            softWrap = false,
            // The missing girl's card also carries the time she was last seen, so her name sits left.
            modifier = (
                if (card.doublePinned) {
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 11.dp, bottom = 1.dp)
                } else {
                    Modifier.align(Alignment.BottomCenter)
                }
                ).clearAndSetSemantics { },
        )
        if (card.person == Person.Mira) MissingMarks()
    }
}

/** The red pen on the missing girl's own photo: when she went missing and when she was last seen. */
@Composable
private fun BoxScope.MissingMarks() {
    val style = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Medium, color = Palette.Marker)
    BasicText(
        text = stringResource(R.string.note_missing),
        style = style.copy(fontSize = 13.sp),
        softWrap = false,
        modifier = Modifier
            .offset(9.dp, 6.dp)
            .graphicsLayer { rotationZ = -7f },
    )
    BasicText(
        text = stringResource(R.string.note_last_seen),
        style = style.copy(fontSize = 13.sp),
        softWrap = false,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset((-7).dp, (-4).dp)
            .graphicsLayer { rotationZ = -5f },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.cardShadow(size: Size, lift: Float, dp: Float) {
    val drop = Offset(1.4f * dp, (2.4f + 6f * lift) * dp)
    for (k in 3 downTo 1) {
        val grow = (1.2f + 3f * lift) * dp * k / 3f
        drawRect(
            Color.Black,
            topLeft = drop - Offset(grow, grow),
            size = Size(size.width + grow * 2, size.height + grow * 2),
            alpha = 0.16f - 0.035f * k,
        )
    }
}

/** The username tag for the faceless account, pinned above its photo. */
@Composable
private fun BoxScope.HandleTag(intro: Sequence, swing: Animatable<Float, AnimationVector1D>, onTap: (Boolean) -> Unit) {
    val size = Board.handleSize
    val label = stringResource(R.string.clue_handle)
    val tap by rememberUpdatedState(onTap)
    Box(
        Modifier
            .offset((Board.handleTag.x - size.width / 2f).dp, (Board.handleTag.y - size.height / 2f).dp)
            .size(size.width.dp, size.height.dp)
            .graphicsLayer {
                val t = intro.t
                val d = window(t, CARDS_AT + TagOrder * CARD_STEP, 460f, Ease.OutCubic)
                alpha = window(t, CARDS_AT + TagOrder * CARD_STEP, 160f)
                transformOrigin = TransformOrigin(0.5f, 0.2f)
                rotationZ = Board.HANDLE_TILT + swing.value * 0.6f + (1f - d) * 6f
                val s = lerp(1.1f, 1f, d)
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                cardShadow(this.size, 0f, density)
                drawRect(Color(0xFF0C0E11))
                drawRect(Color.White, alpha = 0.04f, size = Size(this.size.width, this.size.height * 0.45f))
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick { tap(false); true }
            }
            .pointerInput(Unit) { detectTapGestures { at -> tap(at.x > this.size.width / 2f) } },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(R.string.note_handle),
            style = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Medium, fontSize = 19.sp, color = Color(0xFFF1F1EE)),
            softWrap = false,
        )
    }
}

@Composable
private fun BoxScope.CrumpledNote(intro: Sequence) {
    val label = stringResource(R.string.clue_note)
    Image(
        painter = painterResource(R.drawable.art_note),
        contentDescription = label,
        modifier = Modifier
            .offset((Board.crumpledNote.x - 23f).dp, (Board.crumpledNote.y - 21f).dp)
            .size(46.dp, 42.dp)
            .graphicsLayer {
                val t = intro.t
                val d = window(t, CARDS_AT + NoteOrder * CARD_STEP, 460f, Ease.OutCubic)
                alpha = window(t, CARDS_AT + NoteOrder * CARD_STEP, 160f)
                rotationZ = Board.CRUMPLED_TILT + (1f - d) * 8f
                val s = lerp(1.15f, 1f, d)
                scaleX = s
                scaleY = s
            },
    )
}

/** Red threads from the missing girl to everyone around her, the pins, and the circled stranger. */
@Composable
private fun BoxScope.Threads(intro: Sequence, focus: () -> Person?, pulse: () -> Float) {
    val mira = Board.card(Person.Mira)
    val ordered = remember {
        Board.threads.sortedBy { (Board.card(it.to).pin - mira.pin).getDistance() }
    }
    val unknown = Board.card(Person.Unknown)
    Canvas(Modifier.fillMaxSize()) {
        val t = intro.t
        val dp = density
        val f = focus()
        val p = pulse()
        ordered.forEachIndexed { j, thread ->
            val from = Board.card(thread.from).let { if (thread.fromLower) it.lowerPin else it.pin }
            val to = Board.card(thread.to).pin
            val progress = window(t, THREADS_AT + j * 80f, 520f, Ease.InOutSine)
            val lit = when {
                f == null -> 0f
                f == Person.Mira -> sin(PI.toFloat() * (p * 1.5f - j * 0.05f).coerceIn(0f, 1f))
                f == thread.from || f == thread.to -> sin(PI.toFloat() * p)
                else -> 0f
            }
            drawThread(threadPath(from * dp, to * dp), progress, lit, dp)
        }
        val scribble = window(t, SCRIBBLE_AT, 650f, Ease.InOutSine)
        if (scribble > 0f) {
            val path = scribblePath(unknown.center * dp, 50f, 57f, dp)
            drawPartial(path, scribble, Color(0xFFC4313D), 1.8f * dp, alpha = 0.9f)
        }
        Board.pinOrder.forEachIndexed { order, card ->
            drawPin(card.pin * dp, pinPop(t, order), dp)
            if (card.doublePinned) drawPin(card.lowerPin * dp, pinPop(t, order), dp)
        }
        val tagPin = (Board.handleTag + Offset(0f, -Board.handleSize.height * 0.3f)) * dp
        drawPin(tagPin, window(t, CARDS_AT + TagOrder * CARD_STEP + 380f, 170f, Ease.OutBack), dp, head = Color(0xFFD9A441))
        val notePin = (Board.crumpledNote + Offset(1f, -13f)) * dp
        drawPin(notePin, window(t, CARDS_AT + NoteOrder * CARD_STEP + 380f, 170f, Ease.OutBack), dp, head = Color(0xFFD9A441))
    }
}
