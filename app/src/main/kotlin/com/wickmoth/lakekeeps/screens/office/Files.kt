package com.wickmoth.lakekeeps.screens.office

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.screens.board.drawPin
import com.wickmoth.lakekeeps.screens.evidence.EvidenceShot
import com.wickmoth.lakekeeps.screens.evidence.Find
import com.wickmoth.lakekeeps.screens.evidence.SHOT_H
import com.wickmoth.lakekeeps.screens.evidence.SHOT_W
import com.wickmoth.lakekeeps.screens.evidence.find
import com.wickmoth.lakekeeps.screens.evidence.hotspots
import com.wickmoth.lakekeeps.screens.evidence.shotAspect
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.tactile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

private object Light {
    val Table = Color(0xFF0E1216)
    val Bar = Color(0xFF080B0E)
    val Row = Color(0xFF151B21)
    val Line = Color(0xFF222B34)
    val Text = Color(0xFFE2E7EC)
    val Muted = Color(0xFF8E9AA7)
    val Faint = Color(0xFF5E6A76)
}

private fun ui(size: Float, weight: FontWeight = FontWeight.Medium, color: Color = Light.Text, align: TextAlign = TextAlign.Start) =
    TextStyle(fontFamily = Fonts.Quicksand, fontWeight = weight, fontSize = size.sp, color = color, textAlign = align)

/** The first and last hex digits of a file's SHA-256, as the Vault stamps it when it comes in. */
internal fun fingerprint(evidence: Evidence): String {
    val digest = MessageDigest.getInstance("SHA-256").digest("littlebird/${evidence.fileName}".toByteArray())
    val hex = digest.joinToString("") { "%02x".format(it) }
    return "${hex.take(8)}…${hex.takeLast(6)}"
}

/**
 * The evidence files: the Vault's list of everything that has come in, stamped with when and a
 * fingerprint, and the Lab, where one is examined. In the Lab, pressing and holding a spot pins
 * it to the board, if there's anything there worth pinning.
 */
@Composable
internal fun Files(state: GameState, fit: FrameFit, start: Evidence?, onClose: () -> Unit) {
    var open by remember { mutableStateOf(start) }
    val fromList = remember { start == null }
    val audio = LocalAudio.current
    DesignScale(fit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Light.Table),
        ) {
            AnimatedContent(
                targetState = open,
                transitionSpec = {
                    if (targetState != null) {
                        slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(240)) togetherWith fadeOut(tween(160))
                    } else {
                        fadeIn(tween(240)) togetherWith slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200))
                    }
                },
                label = "files",
            ) { evidence ->
                if (evidence == null) {
                    Vault(state, onOpen = {
                        audio.play(Sfx.Paper, 0.5f)
                        open = it
                    }, onClose = onClose)
                } else {
                    Lab(state, evidence, onBack = { if (fromList) open = null else onClose() })
                }
            }
        }
    }
    BackHandler {
        if (open != null && fromList) open = null else onClose()
    }
}

@Composable
private fun TopBar(title: String, detail: String?, back: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Light.Bar)
            .padding(top = 34.dp, bottom = 12.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            "←",
            style = ui(22f, FontWeight.Bold),
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .tactile(back, onTap = onBack),
        )
        Column(Modifier.weight(1f)) {
            BasicText(title, style = ui(17f, FontWeight.Bold), maxLines = 1)
            if (detail != null) BasicText(detail, style = ui(11f, color = Light.Muted), maxLines = 1)
        }
    }
}

/** Everything that has come in, newest first, each with when it came and its fingerprint. */
@Composable
private fun Vault(state: GameState, onOpen: (Evidence) -> Unit, onClose: () -> Unit) {
    val files = received(state).asReversed()
    Column(Modifier.fillMaxSize()) {
        val count = when (files.size) {
            0 -> "Nothing in yet"
            1 -> "1 file · fingerprinted as it came in"
            else -> "${files.size} files · fingerprinted as they came in"
        }
        TopBar("Evidence", count, "Close the evidence", onClose)
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (files.isEmpty()) {
                BasicText(
                    "Files Amy sends land here. Nothing yet.",
                    style = ui(14f, color = Light.Muted),
                    modifier = Modifier.padding(top = 24.dp, start = 6.dp),
                )
            }
            files.forEach { evidence ->
                val at = receivedAt(state, evidence)
                Row(
                    Modifier
                        .padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .background(Light.Row, RoundedCornerShape(10.dp))
                        .border(1.dp, Light.Line, RoundedCornerShape(10.dp))
                        .tactile("${evidence.title}, from ${evidence.from}. Open it in the Lab") { onOpen(evidence) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvidenceShot(
                        evidence,
                        Modifier
                            .size(46.dp, 64.dp)
                            .background(Color.White),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(evidence.title, style = ui(15f, FontWeight.SemiBold), maxLines = 1)
                        BasicText("From ${evidence.from}${at?.let { " · ${formatClock(it)}" } ?: ""}", style = ui(12f, color = Light.Muted))
                        BasicText("SHA-256 ${fingerprint(evidence)}", style = ui(10.5f, color = Light.Faint))
                    }
                }
            }
        }
    }
}

/** A pin or a passing thought, where the player pressed. */
@Immutable
private data class Mark(val at: Offset, val text: String, val pinned: Boolean, val key: Int)

/** One file under the Lab's light: pinch or double-tap to look closer; press and hold to pin. */
@Composable
private fun Lab(state: GameState, evidence: Evidence, onBack: () -> Unit) {
    val progress = state.progress
    val audio = LocalAudio.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var mark by remember { mutableStateOf<Mark?>(null) }
    val markIn = remember { Animatable(0f) }
    var markJob by remember { mutableStateOf<Job?>(null) }
    val at = receivedAt(state, evidence)

    Column(Modifier.fillMaxSize()) {
        TopBar(
            evidence.title,
            "From ${evidence.from}${at?.let { " · ${formatClock(it)}" } ?: ""} · SHA-256 ${fingerprint(evidence)}",
            "Back",
            onBack,
        )
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .drawBehind {
                    // the light table's faint grid
                    val step = 24f * density
                    var x = 0f
                    while (x < size.width) {
                        drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 0.5f * density, alpha = 0.03f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 0.5f * density, alpha = 0.03f)
                        y += step
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current.density
            val heightDp = constraints.maxHeight / density - 20f
            val widthDp = minOf(constraints.maxWidth / density - 24f, heightDp * SHOT_W / SHOT_H)
            val pinnable = hotspots(evidence).filter { it.find is Find.Pin }

            fun pinAt(press: Offset, box: androidx.compose.ui.geometry.Size) {
                // undo the zoom and pan, then scale from the box to the page
                val center = Offset(box.width / 2f, box.height / 2f)
                val content = center + (press - pan - center) / zoom
                val shot = Offset(content.x / box.width * SHOT_W, content.y / box.height * SHOT_H)
                val found = find(evidence, shot)
                val text = when (found) {
                    is Find.Pin -> if (progress.has(found.flag)) "Already on the board." else "Pinned: ${found.label}"
                    is Find.Pass -> found.note
                }
                val pinned = found is Find.Pin && !progress.has(found.flag)
                if (found is Find.Pin) progress.set(found.flag)
                if (pinned) {
                    audio.play(Sfx.Pin, 0.8f)
                    view.haptic(Haptic.Confirm)
                } else {
                    audio.play(Sfx.Tap, 0.5f)
                    view.haptic(Haptic.Tick)
                }
                mark = Mark(press, text, pinned, (mark?.key ?: 0) + 1)
                markJob?.cancel()
                markJob = scope.launch {
                    markIn.snapTo(0f)
                    markIn.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 420f))
                    delay(2600)
                    markIn.animateTo(0f, tween(320))
                    mark = null
                }
            }

            Box(
                Modifier
                    .size(widthDp.dp, (widthDp * SHOT_H / SHOT_W).dp)
                    .semantics {
                        contentDescription = "${evidence.title}. Press and hold anything worth pinning"
                        customActions = pinnable.map { spot ->
                            val pin = spot.find as Find.Pin
                            CustomAccessibilityAction("Pin: ${pin.label}") {
                                if (!progress.has(pin.flag)) {
                                    progress.set(pin.flag)
                                    audio.play(Sfx.Pin, 0.8f)
                                }
                                true
                            }
                        }
                    }
                    .pointerInput(evidence) {
                        detectTransformGestures { _, panBy, zoomBy, _ ->
                            zoom = (zoom * zoomBy).coerceIn(1f, 3.2f)
                            val limit = Offset(size.width * (zoom - 1f) / 2f, size.height * (zoom - 1f) / 2f)
                            pan = Offset((pan.x + panBy.x).coerceIn(-limit.x, limit.x), (pan.y + panBy.y).coerceIn(-limit.y, limit.y))
                        }
                    }
                    .pointerInput(evidence) {
                        detectTapGestures(
                            onDoubleTap = { p ->
                                if (zoom > 1.2f) {
                                    zoom = 1f
                                    pan = Offset.Zero
                                } else {
                                    zoom = 2.2f
                                    val c = Offset(size.width / 2f, size.height / 2f)
                                    val limit = Offset(size.width * (zoom - 1f) / 2f, size.height * (zoom - 1f) / 2f)
                                    val target = (c - p) * (zoom - 1f)
                                    pan = Offset(target.x.coerceIn(-limit.x, limit.x), target.y.coerceIn(-limit.y, limit.y))
                                }
                            },
                            onLongPress = { p -> pinAt(p, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat())) },
                        )
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin.Center
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        },
                ) {
                    EvidenceShot(evidence, Modifier.fillMaxSize().shotAspect())
                    // Sam's marker round what's been pinned
                    Canvas(Modifier.fillMaxSize()) {
                        val k = size.width / SHOT_W
                        pinnable.forEach { spot ->
                            val pin = spot.find as Find.Pin
                            if (!progress.has(pin.flag)) return@forEach
                            val r = spot.rect
                            markerRing(Rect(r.left * k, r.top * k, r.right * k, r.bottom * k), 1f, density * 0.9f)
                        }
                    }
                }
                mark?.let { m ->
                    Canvas(Modifier.fillMaxSize()) {
                        if (m.pinned) drawPin(m.at - Offset(0f, (1f - markIn.value) * 26f * density), markIn.value.coerceIn(0f, 1.2f) * 1.6f, density)
                    }
                }
            }
            mark?.let { m ->
                BasicText(
                    m.text,
                    style = hand(18.sp, weight = FontWeight.Medium, align = TextAlign.Center),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp, start = 24.dp, end = 24.dp)
                        .graphicsLayer {
                            alpha = markIn.value.coerceIn(0f, 1f)
                            translationY = ((1f - markIn.value) * 8f).dp.toPx()
                            rotationZ = -1f
                        }
                        .background(Paper.Card)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
        BasicText(
            "Pinch or double-tap to look closer. Press and hold anything worth pinning.",
            style = ui(11.5f, color = Light.Muted, align = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .background(Light.Bar)
                .padding(top = 10.dp, bottom = 22.dp, start = 18.dp, end = 18.dp),
        )
    }
}

/** How far into the growth a tool's own content shows (0..1 of the opening). */
internal fun toolContent(progress: Float) = lerp(0f, 1f, Ease.OutCubic.transform(((progress - 0.25f) / 0.75f).coerceIn(0f, 1f)))
