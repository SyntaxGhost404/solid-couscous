package com.wickmoth.lakekeeps.screens.office

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.LittleBird
import com.wickmoth.lakekeeps.game.littlebird.Symptom
import com.wickmoth.lakekeeps.game.littlebird.Verb
import com.wickmoth.lakekeeps.screens.board.drawPartial
import com.wickmoth.lakekeeps.screens.board.drawPin
import com.wickmoth.lakekeeps.screens.board.drawThread
import com.wickmoth.lakekeeps.screens.evidence.AppInfoShot
import com.wickmoth.lakekeeps.screens.evidence.BatteryShot
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.Sequence
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.wickmoth.lakekeeps.game.littlebird.Column as Lane

private val Chalk = hand(15.sp, Palette.Chalk)
private val Marker = hand(15.sp, Palette.Marker, FontWeight.Medium)

/** Small tilts so the cards look pinned by hand. */
private val CardTilt = floatArrayOf(-2.5f, 1.5f, -1f, 2f, -2f, 1f, -1.5f)

/**
 * The board: bare but for a turned photograph and what Sam is after, until Amy's case goes up on
 * it piece by piece. Taps look at things; with the spool out, they tie threads.
 */
@Composable
internal fun BoxScope.OfficeBoard(office: OfficeState, intro: Sequence, introCues: Boolean, visible: Boolean, open: (DeskTool, Rect) -> Unit) {
    val progress = office.progress
    val audio = LocalAudio.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val swings = remember { mutableMapOf<String, Animatable<Float, AnimationVector1D>>() }
    fun swing(id: String) = swings.getOrPut(id) { Animatable(0f) }

    /** A tap on a piece of the board: tie it while the spool is out, or give it a little swing. */
    fun touch(id: String, linkable: Boolean, look: () -> Unit = {}) {
        if (office.connecting) {
            if (linkable) office.pick(id)
            return
        }
        view.haptic(Haptic.Press)
        audio.play(Sfx.Paper, 0.5f)
        scope.launch { swing(id).animateTo(0f, spring(dampingRatio = 0.18f, stiffness = 120f), initialVelocity = 70f) }
        look()
    }

    // ------------------------------------------------------------ always there

    TurnedPhoto(intro, introCues, swing("photo")) { touch("photo", linkable = false) }
    ObjectiveTagPiece(office, intro, visible) { open(DeskTool.Notebook(NotebookPage.Case), OfficeLayout.tag) }

    // ------------------------------------------------------------ the case goes up

    val intake = progress.has(Flag.INTAKE_DONE)
    val sorted = progress.has(Flag.SORTED)

    val title = reveal(intake, visible, delayMs = 300, ms = 900) { audio.play(Sfx.Scribble, 0.55f) }
    Writing("LITTLE BIRD", OfficeLayout.title, hand(25.sp, Palette.Chalk, FontWeight.Medium), width = 140f, progress = { title.value })
    Writing("client: Amy Hart", OfficeLayout.title + Offset(2f, 34f), hand(14.sp, Palette.Chalk), width = 130f, progress = { window(title.value, 0.55f, 0.45f) })
    BoardCanvas {
        val p = window(title.value, 0.3f, 0.4f)
        if (p > 0f) {
            val y = (OfficeLayout.title.y + 31f) * density
            val x0 = OfficeLayout.title.x * density
            drawPartial(Path().apply { moveTo(x0, y); lineTo(x0 + 118f * density, y - 2f * density) }, p, Palette.Chalk, 1.4f * density, alpha = 0.6f)
        }
    }

    val client = reveal(intake, visible, delayMs = 900) { audio.play(Sfx.Pin, 0.45f) }
    PinnedPiece(
        bounds = OfficeLayout.client,
        tilt = OfficeLayout.CLIENT_TILT,
        appear = { client.value },
        swing = swing(LittleBird.AMY),
        picked = { office.first == LittleBird.AMY },
        label = if (office.connecting) "Tie: Amy Hart" else "Photo of Amy Hart",
        onTap = { touch(LittleBird.AMY, linkable = true) },
    ) { PlaceholderPolaroid("Amy Hart", Modifier.fillMaxSize()) }

    val shadow = reveal(intake, visible, delayMs = 1600, ms = 1400)
    // nothing to find there (or to hear about) until Sam has chalked it up
    if (intake) {
        Box(
            Modifier
                .offset(OfficeLayout.shadow.x.dp, OfficeLayout.shadow.y.dp)
                .size(110.dp, 46.dp)
                .tactile("Ryan, in chalk. Not pinned") { touch("shadow", linkable = false) { office.say("Not pinned. Not proven.") } },
        )
    }
    Writing("Ryan?", OfficeLayout.shadow, hand(30.sp, Palette.Chalk), width = 110f, progress = { shadow.value }, tilt = -4f, alpha = 0.3f)

    // faint chalk lines between the three columns while the cards are being sorted
    val dividers = reveal(intake, visible, delayMs = 2000, ms = 900)
    val cleared = reveal(sorted, visible, delayMs = 200, ms = 700)
    BoardCanvas {
        val p = dividers.value
        if (p <= 0f) return@BoardCanvas
        listOf(Lane.Machine, Lane.Person).forEach { lane ->
            val x = (OfficeLayout.columnCenter(lane) + OfficeLayout.COLUMN_W / 2f + 3f) * density
            val top = (OfficeLayout.HEAD_Y + 2f) * density
            val bottom = (OfficeLayout.HEAD_Y + 2f + 330f * p) * density
            drawLine(Palette.Chalk, Offset(x, top), Offset(x + 2f * density, bottom), 1.2f * density, alpha = 0.22f * (1f - 0.7f * cleared.value))
        }
    }
    Lane.entries.forEachIndexed { i, lane ->
        val head = reveal(intake, visible, delayMs = 2000L + i * 260L, ms = 700) { if (i == 0) audio.play(Sfx.Scribble, 0.5f) }
        val rewrite = reveal(sorted, visible, delayMs = 500L + i * 420L, ms = 900) { if (i == 0) audio.play(Sfx.Scribble, 0.55f) }
        val at = Offset(OfficeLayout.columnCenter(lane) - OfficeLayout.COLUMN_W / 2f, OfficeLayout.HEAD_Y)
        Writing(
            lane.chalked,
            at,
            hand(15.sp, Palette.Chalk, align = TextAlign.Center),
            width = OfficeLayout.COLUMN_W,
            progress = { head.value },
            alpha = 1f - window(rewrite.value, 0f, 0.35f),
        )
        Writing(lane.thread, at, hand(14.5.sp, Palette.Marker, FontWeight.Medium, TextAlign.Center), width = OfficeLayout.COLUMN_W, progress = { window(rewrite.value, 0.3f, 0.7f) })
    }

    // ------------------------------------------------------------ evidence, as it's pinned

    EvidencePieces(office, visible, swing = ::swing, touch = ::touch, open = open)

    // ------------------------------------------------------------ Amy's symptoms

    office.selected?.let { card -> ColumnTargets(office, card) }
    Symptom.entries.forEach { card ->
        SymptomCard(office, card, shown = intake, visible, swing(card.id), onTap = { touch(card.id, linkable = office.isSorted(card)) })
    }

    // ------------------------------------------------------------ threads

    Threads(office, visible)
    if (sorted) Spool(office, visible)
    office.pair?.let { (a, b) -> VerbPicker(office, a, b) }
    NoteStrip(office)
}

/** A piece pinned to the board: tilted, swinging on its pin, dropping in as it [appear]s. */
@Composable
private fun BoxScope.PinnedPiece(
    bounds: Rect,
    tilt: Float,
    appear: () -> Float,
    swing: Animatable<Float, AnimationVector1D>,
    picked: () -> Boolean = { false },
    label: String,
    onTap: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val lift by animateFloatAsState(if (picked()) 1f else 0f, spring(stiffness = 500f), label = "picked")
    Box(
        Modifier
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .graphicsLayer {
                val a = appear()
                val d = Ease.OutCubic.transform(a.coerceIn(0f, 1f))
                alpha = window(a, 0f, 0.3f)
                transformOrigin = TransformOrigin(0.5f, Pieces.PIN_DROP / bounds.height)
                rotationZ = tilt + swing.value + (1f - d) * 7f
                val s = lerp(1.12f, 1f, d) * (1f + 0.05f * lift)
                scaleX = s
                scaleY = s
            }
            .tactile(label, onTap = onTap)
            .drawWithContent {
                drawContent()
                drawPin(Offset(size.width / 2f, Pieces.PIN_DROP * density), window(appear(), 0.7f, 0.3f, Ease.OutBack), density)
            },
        content = content,
    )
}

/** The photograph on the wall, turned round. It gets pinned with everything else in the intro. */
@Composable
private fun BoxScope.TurnedPhoto(intro: Sequence, introCues: Boolean, swing: Animatable<Float, AnimationVector1D>, onTap: () -> Unit) {
    val audio = LocalAudio.current
    LaunchedEffect(intro) {
        if (!introCues) return@LaunchedEffect
        snapshotFlow { intro.t >= PHOTO_PIN_AT }.first { it }
        audio.play(Sfx.Pin, 0.4f)
    }
    PinnedPiece(
        bounds = OfficeLayout.photo,
        tilt = OfficeLayout.PHOTO_TILT,
        appear = { window(intro.t, PHOTO_AT, 600f) },
        swing = swing,
        label = "A photograph, turned to face the board",
        onTap = onTap,
    ) { PolaroidBack(Modifier.fillMaxSize()) }
}

private const val PHOTO_AT = 1100f
private const val PHOTO_PIN_AT = PHOTO_AT + 420f
private const val TAG_AT = 2200f

/** The tag in the corner with what Sam is after. It turns over when that changes. */
@Composable
private fun BoxScope.ObjectiveTagPiece(office: OfficeState, intro: Sequence, visible: Boolean, onTap: () -> Unit) {
    val objective = LittleBird.objective(office.progress)
    var shown by remember { mutableStateOf(objective) }
    val flip = remember { Animatable(0f) }
    val audio = LocalAudio.current
    LaunchedEffect(objective, visible) {
        if (objective == shown || !visible) return@LaunchedEffect
        audio.play(Sfx.Paper, 0.5f)
        flip.snapTo(0f)
        flip.animateTo(0.5f, tween(160))
        shown = objective
        flip.animateTo(1f, tween(220))
        flip.snapTo(0f)
    }
    val bounds = OfficeLayout.tag
    ObjectiveTag(
        label = "Next",
        objective = shown.text,
        modifier = Modifier
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .graphicsLayer {
                val a = window(intro.t, TAG_AT, 500f, Ease.OutCubic)
                alpha = a
                translationY = ((1f - a) * -10f).dp.toPx()
                rotationZ = -2f
                val f = flip.value
                scaleY = if (f <= 0.5f) 1f - f * 2f else (f - 0.5f) * 2f
            }
            .tactile("Next: ${shown.text}. Open the notebook", onTap = onTap),
    )
}

/** What's been pinned from the files: printouts, sticky notes, and Sam's chalk beside them. */
@Composable
private fun BoxScope.EvidencePieces(
    office: OfficeState,
    visible: Boolean,
    swing: (String) -> Animatable<Float, AnimationVector1D>,
    touch: (String, Boolean, () -> Unit) -> Unit,
    open: (DeskTool, Rect) -> Unit,
) {
    val progress = office.progress
    val audio = LocalAudio.current

    val band = progress.has(Flag.PIN_BAND)
    val bandIn = reveal(band, visible, delayMs = 250) { audio.play(Sfx.Pin, 0.5f) }
    val ring = reveal(band, visible, delayMs = 1000, ms = 700) { audio.play(Sfx.Scribble, 0.5f) }
    if (band) {
        PinnedPiece(
            bounds = OfficeLayout.bandPrint,
            tilt = -1.5f,
            appear = { bandIn.value },
            swing = swing(LittleBird.BAND),
            picked = { office.first == LittleBird.BAND },
            label = if (office.connecting) "Tie: the 2 to 4 AM band" else "Printout: Amy's battery, the 2 to 4 AM band. Open it",
            onTap = { touch(LittleBird.BAND, true) { open(DeskTool.Files(Evidence.Battery), OfficeLayout.bandPrint) } },
        ) {
            Printout(Evidence.Battery, BatteryShot.chartRegion, Modifier.fillMaxSize()) { area, scale ->
                markerRing(BatteryShot.band.onPrint(BatteryShot.chartRegion, area, scale), ring.value, density)
            }
        }
    }
    val bandNote = reveal(band, visible, delayMs = 1600, ms = 900)
    Writing("2 to 4 AM, every night. screen off.", OfficeLayout.bandDeduction, Marker, width = 108f, progress = { bandNote.value }, tilt = -2f)

    val sync = progress.has(Flag.PIN_SYNC)
    val syncIn = reveal(sync, visible, delayMs = if (band) 1200 else 250) { audio.play(Sfx.Paper, 0.5f) }
    if (sync) {
        PinnedPiece(OfficeLayout.syncNote, 2f, { syncIn.value }, swing("sync"), label = "Note: System Sync Service, 19 percent, 2 hours in the background", onTap = { touch("sync", false) {} }) {
            StickyNote("Sync Service 19% · 2 h at night", Modifier.fillMaxSize())
        }
    }

    val tracker = progress.has(Flag.PIN_IMPOSTOR)
    val trackerIn = reveal(tracker, visible, delayMs = 250) { audio.play(Sfx.Pin, 0.5f) }
    if (tracker) {
        PinnedPiece(
            bounds = OfficeLayout.trackerPrint,
            tilt = 1.5f,
            appear = { trackerIn.value },
            swing = swing(LittleBird.TRACKER),
            picked = { office.first == LittleBird.TRACKER },
            label = if (office.connecting) "Tie: System Sync Service" else "Printout: System Sync Service, app info. Open it",
            onTap = { touch(LittleBird.TRACKER, true) { open(DeskTool.Files(Evidence.Permissions), OfficeLayout.trackerPrint) } },
        ) {
            Printout(Evidence.Permissions, AppInfoShot.header, Modifier.fillMaxSize())
        }
    }
    val trackerNote = reveal(tracker, visible, delayMs = 1100, ms = 1100) { audio.play(Sfx.Scribble, 0.5f) }
    Writing("not a system app. installed five months ago.", OfficeLayout.trackerDeduction, Marker, width = 108f, progress = { trackerNote.value }, tilt = -1.5f)

    listOf(
        Triple(Flag.PIN_PERMISSIONS, OfficeLayout.permissionsNote, "location always · messages · mic"),
        Triple(Flag.PIN_INSTALLED, OfficeLayout.installedNote, "installed 9 April"),
        // too long for one line of Sam's hand, so it may break after the second dot
        Triple(Flag.PIN_PACKAGE, OfficeLayout.packageNote, "com.keypr.\u200Bfamilysafe"),
    ).forEachIndexed { i, (flag, bounds, text) ->
        val pinned = progress.has(flag)
        val noteIn = reveal(pinned, visible, delayMs = 300L + i * 250L) { audio.play(Sfx.Paper, 0.5f) }
        if (pinned) {
            val label = "Note: " + text.replace("\u200B", "")
            PinnedPiece(bounds, if (i % 2 == 0) -2f else 1.5f, { noteIn.value }, swing(flag), label = label, onTap = { touch(flag, false) {} }) {
                StickyNote(text, Modifier.fillMaxSize())
            }
        }
    }

    val act = reveal(progress.has(Flag.ACT1_DONE), visible, delayMs = 600, ms = 1500) { audio.play(Sfx.Scribble, 0.6f) }
    Writing("someone put a tracker on her phone.", OfficeLayout.actDeduction, hand(18.sp, Palette.Marker, FontWeight.Medium), width = 228f, progress = { act.value }, tilt = -1f)
}

/** One of Amy's symptoms: waiting in the tray, in a hand, or pinned in its column. */
@Composable
private fun BoxScope.SymptomCard(office: OfficeState, card: Symptom, shown: Boolean, visible: Boolean, swing: Animatable<Float, AnimationVector1D>, onTap: () -> Unit) {
    val sorted = office.isSorted(card)
    val audio = LocalAudio.current
    // dealt into the tray one after another; every other card is heard landing
    val drop = reveal(shown, visible, delayMs = 2900L + card.ordinal * 110L, ms = 420) {
        if (card.ordinal % 2 == 0) audio.play(Sfx.Paper, 0.35f)
    }
    if (!shown) return
    val position = office.positions.getValue(card)
    val placed = office.placed.getValue(card)
    val dragging = office.dragging == card
    val lifted = office.selected == card
    val lift by animateFloatAsState(if (dragging || lifted) 1f else 0f, spring(stiffness = 600f), label = "lift")
    val tray = OfficeLayout.traySize
    val full = OfficeLayout.cardSize
    val description = when {
        office.connecting && sorted -> "Tie: ${card.text}"
        sorted -> "Card: ${card.text}, under ${card.column.heading}"
        lifted -> "Card: ${card.text}, picked up. Tap a column to put it there"
        else -> "Card: ${card.text}, to sort"
    }
    Box(
        Modifier
            .zIndex(if (dragging || lifted) 2f else 0f)
            .offset { IntOffset((position.value.x * density).roundToInt(), (position.value.y * density).roundToInt()) }
            .size(lerp(tray.width, full.width, placed.value).dp, lerp(tray.height, full.height, placed.value).dp)
            .graphicsLayer {
                val d = Ease.OutCubic.transform(drop.value.coerceIn(0f, 1f))
                alpha = window(drop.value, 0f, 0.3f)
                translationY = ((1f - d) * -16f).dp.toPx()
                transformOrigin = TransformOrigin(0.5f, 0.1f)
                rotationZ = CardTilt[card.ordinal] * (1f - lift) + swing.value
                val s = 1f + 0.07f * lift + 0.04f * (if (office.first == card.id) 1f else 0f)
                scaleX = s
                scaleY = s
            }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
                onClick {
                    if (sorted) onTap() else office.lift(card)
                    true
                }
                if (!sorted) {
                    customActions = Lane.entries.map { lane ->
                        CustomAccessibilityAction("Put under: ${lane.heading}") { office.place(card, lane); true }
                    }
                }
            }
            .pointerInput(card, sorted) {
                if (sorted) {
                    detectTapGestures { onTap() }
                } else {
                    detectTapGestures { office.lift(card) }
                }
            }
            .pointerInput(card, sorted) {
                if (sorted) return@pointerInput
                detectDragGestures(
                    onDragStart = { office.drag(card, Offset.Zero) },
                    onDrag = { change, by ->
                        change.consume()
                        office.drag(card, by / density)
                    },
                    onDragEnd = { office.drop(card) },
                    onDragCancel = { office.drop(card) },
                )
            },
    ) {
        IndexCard(card.text, Modifier.fillMaxSize(), lift = { lift }, textSize = lerp(13.5f, 15f, placed.value).sp)
        Canvas(Modifier.fillMaxSize()) {
            drawPin(Offset(size.width / 2f, Pieces.PIN_DROP * density), window(placed.value, 0.55f, 0.45f, Ease.OutBack), density)
        }
    }
}

/** While a card is picked up, each column takes a tap to put it there. */
@Composable
private fun BoxScope.ColumnTargets(office: OfficeState, card: Symptom) {
    Lane.entries.forEach { lane ->
        val zone = OfficeLayout.dropZone(lane)
        Box(
            Modifier
                .offset(zone.left.dp, zone.top.dp)
                .size(zone.width.dp, zone.height.dp)
                .semantics {
                    contentDescription = "Put ${card.text} under: ${lane.heading}"
                    role = Role.Button
                    onClick { office.place(card, lane); true }
                }
                .pointerInput(card) { detectTapGestures { office.place(card, lane) } },
        )
    }
}

/** The threads that held, the loose end of one being tied, and one that didn't hold, falling. */
@Composable
private fun BoxScope.Threads(office: OfficeState, visible: Boolean) {
    val progress = office.progress
    val draws = LittleBird.links.map { link ->
        val made = progress.has(Flag.linked(link.id))
        link to reveal(made, visible, ms = 650)
    }
    BoardCanvas {
        val dp = density
        draws.forEach { (link, drawn) ->
            val p = drawn.value
            if (p <= 0f) return@forEach
            val a = Pieces.anchor(link.a) * dp
            val b = Pieces.anchor(link.b) * dp
            drawThread(linkPath(a, b), p, lit = 0f, dp = dp)
            drawPin(a, 1f, dp)
            drawPin(b, 1f, dp)
        }
        office.first?.let { id ->
            // a loose end hanging from the first thing picked
            val a = Pieces.anchor(id) * dp
            val path = Path().apply {
                moveTo(a.x, a.y)
                quadraticTo(a.x + 14f * dp, a.y + 22f * dp, a.x + 6f * dp, a.y + 40f * dp)
            }
            drawThread(path, 1f, lit = 0.6f, dp = dp)
            drawPin(a, 1f, dp)
        }
        office.failed?.let { id ->
            val f = office.fall.value
            val a = Pieces.anchor(id) * dp
            val path = Path().apply {
                moveTo(a.x, a.y)
                quadraticTo(a.x - 10f * dp, a.y + (30f + 60f * f) * dp, a.x - 4f * dp, a.y + (50f + 120f * f) * dp)
            }
            drawPartial(path, 1f, Palette.Thread, 1.5f * dp, alpha = 1f - f)
        }
    }
    draws.forEach { (link, drawn) ->
        if (drawn.value > 0f) {
            val at = linkMiddle(Pieces.anchor(link.a), Pieces.anchor(link.b))
            VerbFlag(link.verb.text, at, appear = { window(drawn.value, 0.6f, 0.4f) })
        }
    }
}

/** The spool of red thread in the corner. Out, it turns taps into threads. */
@Composable
private fun BoxScope.Spool(office: OfficeState, visible: Boolean) {
    val appear = reveal(true, visible, delayMs = 2200, ms = 500)
    val active by animateFloatAsState(if (office.connecting) 1f else 0f, tween(220), label = "spool")
    val bounds = OfficeLayout.spool
    Canvas(
        Modifier
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .graphicsLayer {
                alpha = appear.value
                rotationZ = -12f + 10f * active
                val s = 1f + 0.08f * active
                scaleX = s
                scaleY = s
            }
            .tactile(if (office.connecting) "Put the thread away" else "Thread: tie two things together", onTap = office::toggleConnect),
    ) { drawSpool(density, active) }
    if (office.connecting && office.pair == null) {
        BasicText(
            if (office.first == null) "pick two things to tie" else "and what does it tie to?",
            style = hand(14.sp, Palette.Chalk, align = TextAlign.End),
            modifier = Modifier
                .offset((bounds.left - 172f).dp, (bounds.top + 10f).dp)
                .size(166.dp, 24.dp)
                .graphicsLayer { alpha = active },
        )
    }
}

/** Two things picked: which word ties them? Only true links hold. */
@Composable
private fun BoxScope.VerbPicker(office: OfficeState, a: String, b: String) {
    val appear = remember(a, b) { Animatable(0f) }
    LaunchedEffect(a, b) { appear.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 420f)) }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(Unit) { detectTapGestures { office.cancelConnect() } },
    )
    Column(
        Modifier
            .offset(26.dp, 360.dp)
            .width(308.dp)
            .graphicsLayer {
                alpha = appear.value
                translationY = ((1f - appear.value) * 14f).dp.toPx()
            }
            .background(Color(0xFF151B22), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF2C3540), RoundedCornerShape(6.dp))
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(Pieces.name(a), style = hand(19.sp, Palette.Chalk, FontWeight.Medium, TextAlign.Center))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Verb.entries.forEach { verb ->
                BasicText(
                    verb.text,
                    style = hand(17.sp, weight = FontWeight.Medium, align = TextAlign.Center),
                    modifier = Modifier
                        .background(Paper.Card)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .tactile("${Pieces.name(a)} ${verb.text} ${Pieces.name(b)}") { office.choose(verb) },
                )
            }
        }
        BasicText(Pieces.name(b), style = hand(19.sp, Palette.Chalk, FontWeight.Medium, TextAlign.Center))
    }
}

/** Sam's second thoughts, on a strip of paper, for a few seconds. */
@Composable
private fun BoxScope.NoteStrip(office: OfficeState) {
    val note = office.note ?: return
    val bounds = OfficeLayout.note
    Box(
        Modifier
            .zIndex(3f)
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .wrapContentSize()
            .graphicsLayer {
                val a = office.noteIn.value
                alpha = a
                translationY = ((1f - a) * 6f).dp.toPx()
                rotationZ = -1f
            }
            .background(Paper.Card)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        BasicText(note, style = hand(17.sp, weight = FontWeight.Medium, align = TextAlign.Center))
    }
}
