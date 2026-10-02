package com.wickmoth.lakekeeps.screens.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.Sequence
import com.wickmoth.lakekeeps.ui.rememberFrameFit
import com.wickmoth.lakekeeps.ui.rememberSequence
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.sin

private const val LENGTH = 3900f
private const val IGNITE = 1250f
private const val EXIT = 2800f

private val Dim = Color(0xFF50555C)
private val Warm = Color(0xFFFFE0B2)

/** Studio ident: the wordmark is written in, the dot of its "i" catches light, then is blown out. */
@Composable
fun StudioSplash(onFinished: () -> Unit) {
    val sequence = rememberSequence(LENGTH, onEnd = onFinished)
    val audio = LocalAudio.current
    LaunchedEffect(sequence) {
        val at = snapshotFlow { sequence.t }.first { it >= IGNITE }
        if (at < IGNITE + 300f) audio.play(Sfx.Ignite)
    }
    val studio = stringResource(R.string.studio_name)
    val fit = rememberFrameFit(Alignment.Center)
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Night)
            .semantics { contentDescription = studio }
            .pointerInput(Unit) { detectTapGestures { sequence.skipTo(EXIT) } },
    ) {
        DesignFrame(fit) {
            Wordmark(studio, sequence, Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun Wordmark(word: String, sequence: Sequence, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = remember {
        TextStyle(fontFamily = Fonts.Josefin, fontWeight = FontWeight.Light, fontSize = 52.sp, letterSpacing = 0.02.em)
    }
    // The "i" is set dotless: the flame takes the place of its dot.
    val dot = word.indexOf('i')
    val set = remember(word) { word.replace('i', 'ı') }
    val layout = remember(set, measurer) { measurer.measure(set, style) }
    val glyphs = remember(set, measurer) { set.map { measurer.measure(it.toString(), style) } }
    val size = with(LocalDensity.current) { DpSize(layout.size.width.toDp(), layout.size.height.toDp()) }

    Canvas(modifier.size(size)) {
        val t = sequence.t
        val em = style.fontSize.toPx()
        val flame = window(t, IGNITE + 90f, 420f, Ease.OutBack) * (1f - window(t, EXIT + 300f, 240f, Ease.InCubic))
        val wick = if (dot >= 0) {
            Offset(layout.getBoundingBox(dot).left + 0.1135f * em, layout.firstBaseline - 0.50f * em)
        } else {
            Offset(size.width.toPx() / 2f, 0f)
        }
        for (i in set.indices) {
            val reveal = window(t, 250f + i * 70f, 650f, Ease.OutCubic)
            val exit = window(t, EXIT + i * 25f, 420f, Ease.InCubic)
            val alpha = reveal * (1f - exit)
            if (alpha <= 0f) continue
            val box = layout.getBoundingBox(i)
            val near = 1f - (abs(box.center.x - wick.x) / (em * 2.4f)).coerceIn(0f, 1f)
            val color = lerp(lerp(Dim, Palette.Wordmark, reveal), Warm, flame * near * 0.6f)
            drawText(
                glyphs[i],
                color = color,
                topLeft = Offset(box.left, (1f - reveal) * 7f * density - exit * 4f * density),
                alpha = alpha,
            )
        }
        val spark = window(t, IGNITE, 110f) * (1f - window(t, IGNITE + 110f, 220f))
        drawFlame(wick, em, t, flame, spark)
        drawSmoke(wick, em, t, window(t, EXIT + 460f, 700f))
    }
}

/** A candle flame seated on [base]: halo, body and hot core, flickering with [t]. */
internal fun DrawScope.drawFlame(base: Offset, em: Float, t: Float, amount: Float, spark: Float = 0f) {
    if (spark > 0f) {
        drawCircle(Color(0xFFFFF3D6), radius = em * 0.04f * (0.5f + spark), center = base, alpha = spark)
    }
    if (amount <= 0.001f) return
    val f1 = sin(t * 0.0137f)
    val f2 = sin(t * 0.0291f + 1.3f)
    val f3 = sin(t * 0.0067f + 0.4f)
    val h = em * 0.27f * amount * (1f + 0.06f * f1 + 0.04f * f2)
    val w = em * 0.07f * (0.6f + 0.4f * amount) * (1f + 0.05f * f2)
    val sway = em * 0.025f * f3 * amount

    val haloCenter = base + Offset(0f, -h * 0.45f)
    val halo = em * (0.95f + 0.07f * f1) * amount
    drawCircle(
        Brush.radialGradient(
            0f to Palette.Lantern.copy(alpha = 0.30f * amount),
            0.35f to Palette.Lantern.copy(alpha = 0.10f * amount),
            1f to Color.Transparent,
            center = haloCenter,
            radius = halo,
        ),
        radius = halo,
        center = haloCenter,
    )

    val tip = base + Offset(sway, -h)
    val body = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(tip.x + w * 0.25f, tip.y + h * 0.35f, base.x + w, base.y - h * 0.34f, base.x + w * 0.95f, base.y - h * 0.1f)
        cubicTo(base.x + w * 0.88f, base.y + w * 0.95f, base.x - w * 0.88f, base.y + w * 0.95f, base.x - w * 0.95f, base.y - h * 0.1f)
        cubicTo(base.x - w, base.y - h * 0.34f, tip.x - w * 0.25f, tip.y + h * 0.35f, tip.x, tip.y)
        close()
    }
    drawPath(
        body,
        Brush.verticalGradient(
            0f to Color(0x00FF6A2A),
            0.25f to Color(0xC8FF8A33),
            0.6f to Color(0xFFFFC266),
            1f to Color(0xFFFFE9B0),
            startY = tip.y,
            endY = base.y + w * 0.7f,
        ),
        alpha = amount,
    )
    scale(0.5f, pivot = base) {
        drawPath(body, Color(0xFFFFF8E8), alpha = 0.92f * amount)
    }
}

/** A thread of smoke curling up from [base] once the flame is out ([p] runs 0..1). */
private fun DrawScope.drawSmoke(base: Offset, em: Float, t: Float, p: Float) {
    if (p <= 0f || p >= 1f) return
    val rise = em * 0.75f
    val path = Path()
    val steps = 24
    for (k in 0..steps) {
        val f = k / steps.toFloat() * p
        val x = base.x + sin(f * 7f + t * 0.004f) * em * 0.05f * f * 2f
        val y = base.y - rise * f
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path,
        Color(0xFF9AA0A8),
        alpha = 0.45f * (1f - p),
        style = Stroke(width = em * 0.025f, cap = StrokeCap.Round),
    )
}
