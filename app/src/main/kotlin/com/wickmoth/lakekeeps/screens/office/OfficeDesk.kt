package com.wickmoth.lakekeeps.screens.office

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.LittleBird
import com.wickmoth.lakekeeps.game.littlebird.Memo
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.screens.board.Landing
import com.wickmoth.lakekeeps.screens.board.PhonePiece
import com.wickmoth.lakekeeps.screens.board.Steam
import com.wickmoth.lakekeeps.screens.board.deskPhone
import com.wickmoth.lakekeeps.screens.board.introLift
import com.wickmoth.lakekeeps.screens.board.softShadow
import com.wickmoth.lakekeeps.screens.evidence.EvidenceShot
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.Sequence
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.wave
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/** How long a voice memo's words stay up before they're filed. */
private const val MEMO_MS = 7500L

/** How long the phone's screen stays lit after it buzzes. */
private const val LIT_MS = 4200

/**
 * Sam's desk: his notebook, the tray where files from the case land, a coffee, and his phone, the
 * only phone in this case. The phone buzzes on the wood when someone writes.
 */
@Composable
internal fun BoxScope.OfficeDesk(
    state: GameState,
    office: OfficeState,
    intro: Sequence,
    idle: () -> Float,
    hidden: Owner?,
    landing: Landing,
    visible: Boolean,
    onPickUp: (Owner) -> Unit,
    open: (DeskTool, Rect) -> Unit,
) {
    val progress = state.progress
    val entries = LittleBird.findings.count { progress.has(it.flag) } + LittleBird.memos.count { progress.has(it.flag) }
    val unread = entries > 0 && !progress.has(Flag.seen("notebook.$entries"))
    NotebookProp(intro, unread) { open(DeskTool.Notebook(NotebookPage.Case), OfficeLayout.notebook) }

    val files = received(state)
    Tray(intro, files, visible) { open(DeskTool.Files(null), OfficeLayout.tray) }

    Mug(intro)
    Steam({ intro.t }, idle, OfficeLayout.steam)

    SamsPhone(state, intro, hidden, landing, idle, onPickUp)

    val memo = LittleBird.memos.firstOrNull { progress.has(it.flag) && !progress.has(Flag.seen("memo.${it.id}")) }
    if (memo != null && visible && hidden == null) MemoCaption(memo) { progress.set(Flag.seen("memo.${memo.id}")) }
}

/** A prop pressed on the desk lifts a little, as the prototype's props do. */
@Composable
private fun BoxScope.DeskThing(bounds: Rect, tilt: Float, index: Int, intro: Sequence, label: String, onTap: () -> Unit, draw: androidx.compose.ui.graphics.drawscope.DrawScope.(press: Float) -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val view = LocalView.current
    Canvas(
        Modifier
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .graphicsLayer {
                val e = introLift(intro.t, index)
                alpha = e
                translationY = ((1f - e) * 18f - press.value * 2f).dp.toPx()
                rotationZ = tilt
                val s = 1f + 0.05f * press.value
                scaleX = s
                scaleY = s
            }
            .tactile(
                label = label,
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(dampingRatio = if (down) 0.8f else 0.45f, stiffness = 520f)) } },
            ) {
                view.haptic(Haptic.Tick)
                audio.play(Sfx.Paper, 0.6f)
                onTap()
            },
    ) { draw(press.value) }
}

/** Sam's notebook: black covers, an elastic band, and a red ribbon out when there's something new in it. */
@Composable
private fun BoxScope.NotebookProp(intro: Sequence, unread: Boolean, onTap: () -> Unit) {
    DeskThing(OfficeLayout.notebook, OfficeLayout.NOTEBOOK_TILT, 0, intro, if (unread) "Notebook, new notes" else "Notebook", onTap) { press ->
        val dp = density
        val w = size.width
        val h = size.height
        softShadow(Rect(Offset.Zero, size), 3f * dp, press, dp)
        if (unread) {
            // the ribbon hangs out of the bottom
            drawRect(Color(0xFFB8343F), Offset(w * 0.3f, h - 6f * dp), Size(5f * dp, 14f * dp))
        }
        drawRoundRect(Color(0xFFE9E3D4), Offset(3f * dp, 2f * dp), Size(w - 3f * dp, h - 2f * dp), CornerRadius(3f * dp))
        for (k in 1..3) drawLine(Color(0xFFBFB7A6), Offset(w - k * 1.2f * dp, 4f * dp), Offset(w - k * 1.2f * dp, h - 3f * dp), 0.6f * dp)
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF2C2A28), Color(0xFF1A1918)), Offset.Zero, Offset(w, h)),
            size = Size(w - 4f * dp, h - 3f * dp),
            cornerRadius = CornerRadius(4f * dp),
        )
        drawRoundRect(Color.White, Offset(2f * dp, 2f * dp), Size(w - 8f * dp, h - 7f * dp), CornerRadius(3f * dp), alpha = 0.035f)
        drawRect(Color(0xFF0E0D0D), Offset(w * 0.78f, 0f), Size(3.2f * dp, h - 3f * dp))
        // a pencil laid across it
        drawLine(Color(0xFFD9A441), Offset(w * 0.12f, h * 0.86f), Offset(w * 0.98f, h * 0.62f), 4.4f * dp, StrokeCap.Butt)
        drawLine(Color(0xFFE8C9A0), Offset(w * 0.98f, h * 0.62f), Offset(w * 1.07f, h * 0.595f), 4.4f * dp, StrokeCap.Butt)
        drawLine(Color(0xFFB8343F), Offset(w * 0.04f, h * 0.882f), Offset(w * 0.12f, h * 0.86f), 4.4f * dp, StrokeCap.Butt)
    }
}

/** The wire tray where files from the case land, newest on top, each with its picture showing. */
@Composable
private fun BoxScope.Tray(intro: Sequence, files: List<Evidence>, visible: Boolean, onTap: () -> Unit) {
    val audio = LocalAudio.current
    var shown by remember { mutableIntStateOf(files.size) }
    val land = remember { Animatable(1f) }
    LaunchedEffect(files.size, visible) {
        if (files.size <= shown || !visible) return@LaunchedEffect
        delay(500)
        audio.play(Sfx.Paper, 0.6f)
        shown = files.size
        land.snapTo(0f)
        land.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))
    }
    val inTray = files.take(shown)
    val label = when (inTray.size) {
        0 -> "Evidence tray, empty"
        1 -> "Evidence tray, 1 file"
        else -> "Evidence tray, ${inTray.size} files"
    }
    val bounds = OfficeLayout.tray
    DeskThing(bounds, OfficeLayout.TRAY_TILT, 1, intro, label, onTap) { press ->
        val dp = density
        softShadow(Rect(Offset.Zero, size), 3f * dp, press, dp)
        drawRoundRect(Color(0xFF2A2F35), size = size, cornerRadius = CornerRadius(3f * dp))
        drawRoundRect(Color(0xFF4A525C), size = size, cornerRadius = CornerRadius(3f * dp), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f * dp))
        // wire floor
        var x = 8f * dp
        while (x < size.width - 4f * dp) {
            drawLine(Color(0xFF3A4048), Offset(x, 4f * dp), Offset(x, size.height - 4f * dp), 0.8f * dp)
            x += 9f * dp
        }
    }
    // the papers in it, as their own pieces so the newest can show its picture
    inTray.forEachIndexed { i, evidence ->
        val top = i == inTray.lastIndex
        val e = { introLift(intro.t, 1) }
        Box(
            Modifier
                .offset((bounds.left + 10f + i * 3f).dp, (bounds.top + 8f - i * 2f).dp)
                .size(58.dp, 60.dp)
                .graphicsLayer {
                    alpha = e() * if (top) window(land.value, 0f, 0.25f) else 1f
                    rotationZ = OfficeLayout.TRAY_TILT + (i - 1) * 3.5f
                    translationY = if (top) ((1f - land.value) * -24f).dp.toPx() else 0f
                }
                .drawBehind {
                    paperShadow(size, 0f, density)
                    drawRect(Paper.PrintMargin)
                },
        ) {
            if (top) {
                EvidenceShot(
                    evidence,
                    Modifier
                        .padding(3.dp)
                        .fillMaxSize(),
                )
            }
        }
    }
}

/** The coffee. It just sits there, steaming. */
@Composable
private fun BoxScope.Mug(intro: Sequence) {
    val bounds = OfficeLayout.mug
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val view = LocalView.current
    Image(
        painter = painterResource(R.drawable.art_mug),
        contentDescription = null,
        modifier = Modifier
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .graphicsLayer {
                val e = introLift(intro.t, 2)
                alpha = e
                translationY = ((1f - e) * 18f - press.value * 2f).dp.toPx()
                val s = 1f + 0.05f * press.value
                scaleX = s
                scaleY = s
            }
            .drawBehind { softShadow(Rect(5f * density, 45f * density, 45f * density, 57f * density), 0f, press.value, density, oval = true) }
            .tactile(
                label = "Coffee mug",
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(dampingRatio = if (down) 0.8f else 0.45f, stiffness = 520f)) } },
            ) {
                view.haptic(Haptic.Tick)
                audio.play(Sfx.Tap, 0.7f)
            },
    )
}

/** Sam's phone on the desk. It buzzes when someone writes, and its screen stays lit a while. */
@Composable
private fun BoxScope.SamsPhone(state: GameState, intro: Sequence, hidden: Owner?, landing: Landing, idle: () -> Float, onPickUp: (Owner) -> Unit) {
    val messages = state.messages
    val audio = LocalAudio.current
    val view = LocalView.current
    val alert = remember { Animatable(0f) }
    val lit = remember { Animatable(0f) }
    val inHand = hidden == Owner.Sam
    LaunchedEffect(inHand) {
        if (inHand) return@LaunchedEffect
        val threads = Threads.of(Owner.Sam)
        var seen = threads.sumOf { messages[it].delivered }
        snapshotFlow { threads.sumOf { messages[it].delivered } }.collect { now ->
            val fresh = now > seen
            seen = now
            if (!fresh) return@collect
            val newest = threads.flatMap { messages.lines(it) }.maxByOrNull { it.day.index * 1440 + it.minutes } ?: return@collect
            if (newest.mine) return@collect
            audio.play(Sfx.Buzz)
            view.haptic(Haptic.Confirm)
            launch {
                lit.snapTo(1f)
                delay(LIT_MS.toLong())
                lit.animateTo(0f, tween(900, easing = LinearEasing))
            }
            alert.snapTo(0f)
            alert.animateTo(1f, tween(900, easing = LinearEasing))
        }
    }
    val waiting = messages.unread(Owner.Sam) > 0
    PhonePiece(
        deskPhone(Owner.Sam),
        index = 3,
        intro = { intro.t },
        lifted = inHand,
        landing = landing,
        onPickUp = onPickUp,
        alert = { alert.value },
        glow = { maxOf(lit.value, if (waiting) 0.22f + 0.08f * wave(idle(), 2600f) else 0f) },
    )
}

/** Sam's voice memo, as words over the desk; tap to file it in the notebook sooner. */
@Composable
private fun BoxScope.MemoCaption(memo: Memo, onFiled: () -> Unit) {
    val appear = remember(memo.id) { Animatable(0f) }
    // until it's on screen it neither takes the desk's taps nor reaches a screen reader
    var playing by remember(memo.id) { mutableStateOf(false) }
    LaunchedEffect(memo.id) {
        delay(1200)
        playing = true
        appear.animateTo(1f, tween(420))
        delay(MEMO_MS)
        appear.animateTo(0f, tween(420))
        onFiled()
    }
    val scope = rememberCoroutineScope()
    fun file() {
        scope.launch {
            appear.animateTo(0f, tween(240))
            onFiled()
        }
    }
    Column(
        Modifier
            .offset(14.dp, 660.dp)
            .width(332.dp)
            .graphicsLayer {
                alpha = appear.value
                translationY = ((1f - appear.value) * 14f).dp.toPx()
            }
            .background(Color(0xE6101418), RoundedCornerShape(10.dp))
            .then(
                if (playing) {
                    Modifier
                        .semantics(mergeDescendants = true) { contentDescription = "Voice memo, ${memo.title}: ${memo.text}" }
                        .pointerInput(memo.id) { detectTapGestures { file() } }
                } else {
                    Modifier.clearAndSetSemantics { }
                },
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer { alpha = 0.6f + 0.4f * sin(appear.value * 12f) }
                    .background(Palette.Marker, CircleShape),
            )
            Spacer(Modifier.width(7.dp))
            BasicText(
                "VOICE MEMO · ${memo.title.uppercase()}",
                style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color(0xFF8E9AA7), letterSpacing = 1.2.sp),
            )
        }
        Spacer(Modifier.height(6.dp))
        BasicText(memo.text, style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFFE2E7EC), lineHeight = 19.sp))
    }
}
