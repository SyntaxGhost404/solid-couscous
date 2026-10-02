package com.wickmoth.lakekeeps.screens.title

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.Sequence
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.rememberFrameFit
import com.wickmoth.lakekeeps.ui.rememberSequence
import com.wickmoth.lakekeeps.ui.wave
import com.wickmoth.lakekeeps.ui.window
import kotlin.random.Random

private const val LENGTH = 7800f
private const val EXIT = 6700f

private const val ART_W = 360f
private const val ART_H = 470f
private const val HORIZON = 252f
private val Moon = Offset(246f, 108f)
private val Lamp = Offset(204.5f, 242.5f)

private val Mist = Color(0xFF9DBAC4)
private val MoonLight = Color(0xFFCFE3E0)

/** Title card: the lake fades up out of the dark, the lantern catches, and the name is set. */
@Composable
fun TitleScreen(onFinished: () -> Unit) {
    val sequence = rememberSequence(LENGTH, onEnd = onFinished)
    val fit = rememberFrameFit(Alignment.Center)
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Night)
            .pointerInput(Unit) { detectTapGestures { if (sequence.t > 600f) sequence.skipTo(EXIT) } },
    ) {
        DesignFrame(fit) {
            LakeArt(sequence)
            Lettering(sequence)
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Palette.Night, alpha = window(sequence.t, EXIT, 1000f, Ease.InOutSine)) },
        )
    }
}

@Composable
private fun LakeArt(sequence: Sequence) {
    val stars = remember {
        val rnd = Random(42)
        List(46) {
            Star(Offset(rnd.nextFloat() * ART_W, rnd.nextFloat() * 205f), 0.35f + rnd.nextFloat() * 0.75f,
                0.25f + rnd.nextFloat() * 0.6f, 1800f + rnd.nextFloat() * 2600f, rnd.nextFloat())
        }.filter { (it.at - Moon).getDistance() > 46f }
    }
    Box(
        Modifier
            .size(ART_W.dp, ART_H.dp)
            .graphicsLayer {
                val t = sequence.t
                alpha = window(t, 0f, 1800f, Ease.InOutSine)
                val push = lerp(1.06f, 1f, window(t, 0f, 7200f, Ease.OutCubic))
                scaleX = push
                scaleY = push
                transformOrigin = TransformOrigin(Lamp.x / ART_W, Lamp.y / ART_H)
                clip = true
            },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawSkyAndWater(sequence.t, stars) }
        Layer(R.drawable.title_far, FarBounds)
        Canvas(Modifier.fillMaxSize()) {
            val t = sequence.t
            drawMist(t, y = 247f, height = 22f, alpha = 0.11f, drift = 1f)
            drawLanternGlow(t, window(t, 1400f, 700f, Ease.OutCubic))
        }
        Layer(R.drawable.title_pier, PierBounds)
        Layer(R.drawable.title_boat, BoatBounds) {
            val t = sequence.t
            translationY = (wave(t, 5200f) * 1.2f).dp.toPx()
            rotationZ = wave(t, 6100f, 0.3f) * 1.1f
            transformOrigin = BoatBounds.origin(248f, 304f)
        }
        Canvas(Modifier.fillMaxSize()) { drawMist(sequence.t, y = 322f, height = 34f, alpha = 0.08f, drift = -1f) }
        Layer(R.drawable.title_reeds_left, ReedsLeftBounds) {
            rotationZ = wave(sequence.t, 5600f) * 1.3f
            transformOrigin = ReedsLeftBounds.origin(36f, ART_H)
        }
        Layer(R.drawable.title_reeds_right, ReedsRightBounds) {
            rotationZ = wave(sequence.t, 6300f, 0.4f) * 1.1f
            transformOrigin = ReedsRightBounds.origin(326f, ART_H)
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    0.55f to Color.Transparent,
                    1f to Palette.Night.copy(alpha = 0.75f),
                    center = Offset(size.width / 2f, size.height * 0.42f),
                    radius = size.width * 0.95f,
                ),
            )
            drawRect(
                Brush.verticalGradient(0f to Color.Transparent, 1f to Palette.Night, startY = size.height * 0.78f, endY = size.height),
            )
        }
    }
}

/** Where a cropped art layer sits on the 360 x 470 art canvas (matches tools/art/title.py). */
private class LayerBounds(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    fun origin(x: Float, y: Float) = TransformOrigin((x - x0) / (x1 - x0), (y - y0) / (y1 - y0))
}

private val FarBounds = LayerBounds(0f, 188f, 360f, 256f)
private val PierBounds = LayerBounds(30f, 234f, 216f, 470f)
private val BoatBounds = LayerBounds(216f, 292f, 280f, 318f)
private val ReedsLeftBounds = LayerBounds(-40f, 290f, 110f, 470f)
private val ReedsRightBounds = LayerBounds(250f, 268f, 390f, 470f)

@Composable
private fun Layer(res: Int, at: LayerBounds, layer: (GraphicsLayerScope.() -> Unit)? = null) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .offset(at.x0.dp, at.y0.dp)
            .size((at.x1 - at.x0).dp, (at.y1 - at.y0).dp)
            .then(if (layer != null) Modifier.graphicsLayer(layer) else Modifier),
    )
}

private class Star(val at: Offset, val radius: Float, val alpha: Float, val period: Float, val phase: Float)

private fun DrawScope.drawSkyAndWater(t: Float, stars: List<Star>) {
    val u = size.width / ART_W
    val horizon = HORIZON * u
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFF04070B), 0.6f to Color(0xFF0B1823), 1f to Color(0xFF1A3242),
            startY = 0f, endY = horizon,
        ),
        size = Size(size.width, horizon),
    )
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFF16303D), 0.25f to Color(0xFF0C1922), 1f to Color(0xFF05090C),
            startY = horizon, endY = size.height,
        ),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, size.height - horizon),
    )
    val rise = window(t, 500f, 2200f, Ease.InOutSine)
    val moon = Moon * u
    val breathe = 1f + 0.05f * wave(t, 5200f)
    for ((radius, alpha) in listOf(160f to 0.13f, 62f to 0.16f)) {
        val r = radius * u * breathe
        drawCircle(
            Brush.radialGradient(0f to MoonLight.copy(alpha = alpha * rise), 1f to Color.Transparent, center = moon, radius = r),
            radius = r,
            center = moon,
        )
    }
    drawCircle(Color(0xFFDDE9E6), radius = 21f * u, center = moon, alpha = 0.35f + 0.65f * rise)
    drawCircle(Color(0xFFC3D2D0), radius = 5f * u, center = moon + Offset(-6f, -5f) * u, alpha = 0.5f * rise)
    drawCircle(Color(0xFFC3D2D0), radius = 3f * u, center = moon + Offset(7f, 6f) * u, alpha = 0.45f * rise)
    for (star in stars) {
        val twinkle = 0.6f + 0.4f * wave(t, star.period, star.phase)
        drawCircle(Color(0xFFDCE6F0), radius = star.radius * u, center = star.at * u, alpha = star.alpha * twinkle * rise)
    }
    // the moon's broken path across the water
    for (i in 0 until 18) {
        val y = (HORIZON + 10f + i * 7.6f) * u
        val w = (5f + i * 1.7f) * u * (0.8f + 0.2f * wave(t, 2300f, i * 0.21f))
        val x = moon.x + wave(t, 2900f, i * 0.13f) * 2f * u
        val a = 0.24f * (1f - i / 18f) * (0.55f + 0.45f * wave(t, 1700f, i * 0.37f)) * rise
        drawRect(Color(0xFFBFD6DA), topLeft = Offset(x - w / 2f, y), size = Size(w, 1.3f * u), alpha = a)
    }
}

private fun DrawScope.drawMist(t: Float, y: Float, height: Float, alpha: Float, drift: Float) {
    val u = size.width / ART_W
    for (i in 0 until 3) {
        val cx = (60f + i * 130f + drift * 22f * wave(t, 15000f + i * 2300f, i * 0.31f)) * u
        val r = 70f * u
        val center = Offset(cx, y * u)
        scale(scaleX = 2.4f, scaleY = height / 70f, pivot = center) {
            drawCircle(
                Brush.radialGradient(0f to Mist.copy(alpha = alpha), 1f to Color.Transparent, center = center, radius = r),
                radius = r,
                center = center,
            )
        }
    }
}

private fun DrawScope.drawLanternGlow(t: Float, lit: Float) {
    if (lit <= 0f) return
    val u = size.width / ART_W
    val flicker = 0.86f + 0.08f * wave(t, 170f) + 0.06f * wave(t, 47f, 0.3f) + 0.05f * wave(t, 1300f, 0.7f)
    val lamp = Lamp * u
    for ((radius, alpha) in listOf(54f to 0.16f, 22f to 0.38f, 7f to 0.6f)) {
        val r = radius * u * (0.96f + 0.04f * flicker)
        drawCircle(
            Brush.radialGradient(0f to Palette.Lantern.copy(alpha = alpha * lit * flicker), 1f to Color.Transparent, center = lamp, radius = r),
            radius = r,
            center = lamp,
        )
    }
    for (i in 0 until 12) {
        val y = (HORIZON + 8f + i * 5.6f) * u
        val w = (2.6f + i * 0.9f) * u * (0.75f + 0.25f * wave(t, 900f, i * 0.29f))
        val x = lamp.x + wave(t, 1900f, i * 0.17f) * 1.2f * u
        val a = 0.42f * (1f - i / 12f) * lit * flicker
        drawRect(Palette.Lantern, topLeft = Offset(x - w / 2f, y), size = Size(w, 1.2f * u), alpha = a)
    }
}

@Composable
private fun Lettering(sequence: Sequence) {
    val lead = stringResource(R.string.title_lead)
    val main = stringResource(R.string.title_main)
    Column(
        Modifier
            .fillMaxWidth()
            .offset(y = 520.dp)
            .semantics(mergeDescendants = true) { heading() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TrackingText(
            text = lead,
            style = TextStyle(fontFamily = Fonts.Fell, fontStyle = FontStyle.Italic, fontSize = 27.sp, color = Palette.TitleCream),
            progress = { window(sequence.t, 2000f, 1100f, Ease.OutCubic) },
            alpha = { window(sequence.t, 2000f, 900f, Ease.InOutSine) * 0.86f },
        )
        BasicText(
            text = main,
            style = TextStyle(fontFamily = Fonts.Fell, fontSize = 62.sp, color = Palette.TitleCream, letterSpacing = 0.01.em),
            modifier = Modifier
                .graphicsLayer {
                    val t = sequence.t
                    val enter = window(t, 2400f, 1300f, Ease.OutCubic)
                    alpha = window(t, 2400f, 1100f, Ease.InOutSine)
                    translationY = ((1f - enter) * 12f).dp.toPx()
                    val s = lerp(1.04f, 1f, enter)
                    scaleX = s
                    scaleY = s
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    val p = window(sequence.t, 3800f, 1400f, Ease.InOutSine)
                    if (p > 0f && p < 1f) {
                        val band = size.width * 0.22f
                        val x = lerp(-band, size.width + band, p)
                        drawRect(
                            Brush.linearGradient(
                                0f to Color.Transparent,
                                0.5f to Color(0xFFFFF4DC).copy(alpha = 0.9f),
                                1f to Color.Transparent,
                                start = Offset(x - band, 0f),
                                end = Offset(x + band, size.height * 0.4f),
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                },
        )
    }
}

/** Text whose letters start spread apart and draw together (tracking in) as [progress] runs. */
@Composable
private fun TrackingText(text: String, style: TextStyle, progress: () -> Float, alpha: () -> Float, spread: Float = 0.45f) {
    val measurer = rememberTextMeasurer()
    val layout = remember(text, style, measurer) { measurer.measure(text, style) }
    val glyphs = remember(text, style, measurer) { text.map { measurer.measure(it.toString(), style) } }
    val size = with(LocalDensity.current) { DpSize(layout.size.width.toDp(), layout.size.height.toDp()) }
    Canvas(Modifier.size(size)) {
        val p = progress()
        val a = alpha()
        if (a <= 0f) return@Canvas
        val mid = this.size.width / 2f
        text.indices.forEach { i ->
            val left = layout.getBoundingBox(i).left
            val x = mid + (left - mid) * (1f + spread * (1f - p))
            drawText(glyphs[i], topLeft = Offset(x, (1f - p) * 6f * density), alpha = a)
        }
    }
}
