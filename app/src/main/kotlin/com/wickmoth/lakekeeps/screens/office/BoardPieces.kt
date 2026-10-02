package com.wickmoth.lakekeeps.screens.office

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.screens.evidence.EvidenceCrop
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.Palette
import kotlin.math.abs
import kotlin.math.hypot

/** Paper and ink for things pinned to the board. Chalk and marker come from the shared [Palette]. */
internal object Paper {
    val Card = Color(0xFFF1EDE4)
    val CardRule = Color(0xFFD9817C)
    val CardLines = Color(0xFFB9C7D3)
    val Ink = Color(0xFF2A2B3A)
    val Sticky = Color(0xFFEFD98A)
    val Manila = Color(0xFFD7B98A)
    val ManilaDark = Color(0xFFB8975F)
    val PrintMargin = Color(0xFFF7F6F2)
}

internal fun hand(size: TextUnit, color: Color = Paper.Ink, weight: FontWeight = FontWeight.Normal, align: TextAlign = TextAlign.Start) =
    TextStyle(fontFamily = Fonts.Caveat, fontWeight = weight, fontSize = size, color = color, textAlign = align, lineHeight = size * 1.02f)

/** A soft shadow under paper on the board; [lift] raises it off the surface. */
internal fun DrawScope.paperShadow(size: Size, lift: Float, dp: Float) {
    val drop = Offset(1.4f * dp, (2.2f + 6f * lift) * dp)
    for (k in 3 downTo 1) {
        val grow = (1.1f + 3f * lift) * dp * k / 3f
        drawRect(
            Color.Black,
            topLeft = drop - Offset(grow, grow),
            size = Size(size.width + grow * 2, size.height + grow * 2),
            alpha = 0.16f - 0.035f * k,
        )
    }
}

/** An index card with one of Amy's symptoms in Sam's hand. */
@Composable
internal fun IndexCard(text: String, modifier: Modifier, lift: () -> Float = { 0f }, textSize: TextUnit = 15.sp) {
    Box(
        modifier.drawBehind {
            paperShadow(size, lift(), density)
            drawRect(Paper.Card)
            val dp = density
            drawLine(Paper.CardRule, Offset(0f, 9f * dp), Offset(size.width, 9f * dp), 0.8f * dp, alpha = 0.7f)
            var y = 9f * dp + 9f * dp
            while (y < size.height - 2f * dp) {
                drawLine(Paper.CardLines, Offset(0f, y), Offset(size.width, y), 0.6f * dp, alpha = 0.5f)
                y += 9f * dp
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = hand(textSize, align = TextAlign.Center), maxLines = 2, modifier = Modifier.padding(start = 5.dp, end = 5.dp, top = 6.dp))
    }
}

/** A yellow sticky note, for the smaller things Sam pins beside a printout. */
@Composable
internal fun StickyNote(text: String, modifier: Modifier) {
    Box(
        modifier.drawBehind {
            paperShadow(size, 0f, density)
            drawRect(Paper.Sticky)
            drawRect(Color.Black, topLeft = Offset(0f, 0f), size = Size(size.width, 4f * density), alpha = 0.06f)
        },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = hand(13.sp, align = TextAlign.Center), maxLines = 2, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

/**
 * A printout of part of a screenshot ([region], in shot dp), on paper with a margin. [marks] draws
 * Sam's marker over it, given the printed area's rect in the printout's own pixels and the scale
 * from shot dp to those pixels.
 */
@Composable
internal fun Printout(
    evidence: Evidence,
    region: Rect,
    modifier: Modifier,
    lift: () -> Float = { 0f },
    marks: DrawScope.(area: Rect, scale: Float) -> Unit = { _, _ -> },
) {
    Box(
        modifier
            .drawBehind {
                paperShadow(size, lift(), density)
                drawRect(Paper.PrintMargin)
            }
            .drawWithContent {
                drawContent()
                val m = MARGIN * density
                val area = Rect(m, m, size.width - m, size.height - m)
                marks(area, area.width / (region.width * density) * density)
            },
    ) {
        EvidenceCrop(
            evidence,
            region,
            Modifier
                .padding(MARGIN.dp)
                .fillMaxSize()
                .graphicsLayer { alpha = 0.92f },
        )
    }
}

internal const val MARGIN = 4f

/** Maps a rect in shot dp onto a printout's printed [area], at [scale] pixels per shot dp. */
internal fun Rect.onPrint(region: Rect, area: Rect, scale: Float) = Rect(
    area.left + (left - region.left) * scale,
    area.top + (top - region.top) * scale,
    area.left + (right - region.left) * scale,
    area.top + (bottom - region.top) * scale,
)

/** A loop of red marker around [rect], a little rough, drawn as far as [progress]. */
internal fun DrawScope.markerRing(rect: Rect, progress: Float, dp: Float) {
    if (progress <= 0f) return
    val path = Path()
    val steps = 48
    val turn = 1.12f * progress
    for (k in 0..steps) {
        val f = k / steps.toFloat() * turn
        val a = (-1.9f + f * 2f * Math.PI.toFloat())
        val wobble = 1f + 0.06f * kotlin.math.sin(a * 3f)
        val x = rect.center.x + rect.width / 2f * 1.18f * wobble * kotlin.math.cos(a)
        val y = rect.center.y + rect.height / 2f * 1.22f * wobble * kotlin.math.sin(a)
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, Palette.Marker, style = Stroke(1.7f * dp, cap = StrokeCap.Round), alpha = 0.9f)
}

/** A quick underline in red marker. */
internal fun DrawScope.markerLine(from: Offset, to: Offset, progress: Float, dp: Float) {
    if (progress <= 0f) return
    drawLine(Palette.Marker, from, from + (to - from) * progress.coerceIn(0f, 1f), 1.6f * dp, cap = StrokeCap.Round, alpha = 0.9f)
}

/**
 * Placeholder for a person's polaroid until the case's art is made: a cream frame, a plain
 * silhouette, and the name in felt-tip.
 */
@Composable
internal fun PlaceholderPolaroid(name: String, modifier: Modifier, lift: () -> Float = { 0f }) {
    Box(
        modifier.drawBehind {
            paperShadow(size, lift(), density)
            drawRect(Palette.Polaroid)
            val dp = density
            val photo = Rect(5f * dp, 5f * dp, size.width - 5f * dp, size.width - 5f * dp)
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF6F7C89), Color(0xFF4A5560)), startY = photo.top, endY = photo.bottom),
                topLeft = photo.topLeft,
                size = photo.size,
            )
            val c = Offset(photo.center.x, photo.top + photo.height * 0.42f)
            drawCircle(Color(0xFFAEB7C1), photo.width * 0.17f, c)
            drawCircle(Color(0xFFAEB7C1), photo.width * 0.32f, Offset(photo.center.x, photo.bottom + photo.width * 0.12f))
            // the frame hides the shoulders' bottom edge
            drawRect(Palette.Polaroid, topLeft = Offset(0f, photo.bottom), size = Size(size.width, size.height - photo.bottom))
        },
    ) {
        BasicText(
            name,
            style = TextStyle(fontFamily = Fonts.Marker, fontSize = 14.sp, color = Palette.PolaroidInk, textAlign = TextAlign.Center),
            softWrap = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 1.dp),
        )
    }
}

/** The back of a polaroid, turned to the board: blank, with the faint print of the paper. */
@Composable
internal fun PolaroidBack(modifier: Modifier) {
    Box(
        modifier.drawBehind {
            paperShadow(size, 0f, density)
            drawRect(Color(0xFFE6E2D8))
            drawRect(Color(0xFF8D8A82), topLeft = Offset(size.width * 0.12f, size.height * 0.1f), size = Size(size.width * 0.76f, 1f * density), alpha = 0.25f)
            drawRect(Color(0xFF8D8A82), topLeft = Offset(size.width * 0.12f, size.height * 0.16f), size = Size(size.width * 0.5f, 1f * density), alpha = 0.18f)
        },
    )
}

/**
 * Writing straight on the board (chalk, or red marker for what matters), written on line by line,
 * left to right, as [progress] runs from 0 to 1. Screen readers find it once it's begun.
 */
@Composable
internal fun BoxScope.Writing(
    text: String,
    at: Offset,
    style: TextStyle,
    width: Float,
    progress: () -> Float,
    tilt: Float = 0f,
    alpha: Float = 1f,
) {
    var lines by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    val written by rememberUpdatedState(progress)
    val begun by remember { derivedStateOf { written() > 0f } }
    BasicText(
        text,
        style = style,
        onTextLayout = { layout -> lines = (0 until layout.lineCount).map { layout.getLineTop(it) to layout.getLineBottom(it) } },
        modifier = Modifier
            .then(if (begun) Modifier else Modifier.clearAndSetSemantics { })
            .offset(at.x.dp, at.y.dp)
            .size(width.dp, (style.fontSize.value * 4.2f).dp)
            .wrapContentSize(Alignment.TopStart)
            .graphicsLayer {
                rotationZ = tilt
                this.alpha = alpha
                // faded per stroke, not through a layer the size of the text box, which would
                // clip the tops of tall letters
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .drawWithContent {
                val p = progress()
                if (p >= 1f) {
                    drawContent()
                    return@drawWithContent
                }
                if (p <= 0f || lines.isEmpty()) return@drawWithContent
                val n = lines.size
                lines.forEachIndexed { i, (top, bottom) ->
                    val line = (p * n - i).coerceIn(0f, 1f)
                    if (line > 0f) clipRect(top = top, bottom = bottom, right = size.width * line) { this@drawWithContent.drawContent() }
                }
            },
    )
}

/** The manila tag with what Sam is after right now. */
@Composable
internal fun ObjectiveTag(label: String, objective: String, modifier: Modifier) {
    Box(
        modifier.drawBehind {
            val dp = density
            paperShadow(size, 0f, dp)
            val notch = 10f * dp
            val path = Path().apply {
                moveTo(notch, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(notch, size.height)
                lineTo(0f, size.height - notch)
                lineTo(0f, notch)
                close()
            }
            drawPath(path, Paper.Manila)
            drawCircle(Paper.ManilaDark, 4.5f * dp, Offset(10f * dp, size.height / 2f), style = Stroke(1.6f * dp))
            drawCircle(Color(0xFF1A1E24), 2.8f * dp, Offset(10f * dp, size.height / 2f))
        },
    ) {
        BasicText(
            label.uppercase(),
            style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF6E5530), letterSpacing = 1.4.sp),
            modifier = Modifier.offset(24.dp, 6.dp),
        )
        BasicText(
            objective,
            style = hand(15.sp, weight = FontWeight.Medium),
            maxLines = 2,
            modifier = Modifier
                .offset(24.dp, 17.dp)
                .padding(end = 30.dp),
        )
    }
}

/** A wooden spool of red thread: tap to tie things together on the board. */
internal fun DrawScope.drawSpool(dp: Float, active: Float) {
    val w = size.width
    val h = size.height
    if (active > 0f) drawCircle(Palette.ThreadLit, w * 0.62f, Offset(w / 2f, h / 2f), alpha = 0.16f * active)
    val body = Rect(w * 0.26f, h * 0.2f, w * 0.74f, h * 0.8f)
    drawRect(Palette.Thread, body.topLeft, body.size)
    var y = body.top + 2f * dp
    while (y < body.bottom) {
        drawLine(Palette.ThreadLit, Offset(body.left, y), Offset(body.right, y), 0.7f * dp, alpha = 0.5f)
        y += 2.6f * dp
    }
    val wood = Color(0xFFB0875A)
    drawRoundRect(wood, Offset(w * 0.16f, h * 0.12f), Size(w * 0.68f, h * 0.12f), CornerRadius(2f * dp))
    drawRoundRect(wood, Offset(w * 0.16f, h * 0.76f), Size(w * 0.68f, h * 0.12f), CornerRadius(2f * dp))
    // a loose end
    val end = Path().apply {
        moveTo(body.right, h * 0.5f)
        quadraticTo(w * 0.92f, h * 0.62f, w * 0.86f, h * 0.9f)
    }
    drawPath(end, Palette.Thread, style = Stroke(1.2f * dp, cap = StrokeCap.Round))
}

/** A thread between two pins, curving out to the side so it doesn't run straight over the cards. */
internal fun linkPath(a: Offset, b: Offset, bulge: Float = 0.42f): Path {
    val d = b - a
    val span = hypot(d.x, d.y).coerceAtLeast(1f)
    // perpendicular, pointing right for a vertical thread
    var n = Offset(-d.y / span, d.x / span)
    if (n.x < 0f || (abs(n.x) < 0.01f && n.y > 0f)) n = -n
    val control = (a + b) / 2f + n * span * bulge
    return Path().apply {
        moveTo(a.x, a.y)
        quadraticTo(control.x, control.y, b.x, b.y)
    }
}

/** The middle of a [linkPath], where its label hangs. */
internal fun linkMiddle(a: Offset, b: Offset, bulge: Float = 0.42f): Offset {
    val d = b - a
    val span = hypot(d.x, d.y).coerceAtLeast(1f)
    var n = Offset(-d.y / span, d.x / span)
    if (n.x < 0f || (abs(n.x) < 0.01f && n.y > 0f)) n = -n
    val control = (a + b) / 2f + n * span * bulge
    // a quadratic's midpoint sits halfway between the chord's middle and its control point
    return ((a + b) / 2f + control) / 2f
}

/** A small paper flag with a link's verb, hung on its thread. */
@Composable
internal fun BoxScope.VerbFlag(text: String, at: Offset, appear: () -> Float) {
    BasicText(
        text,
        style = hand(13.sp, weight = FontWeight.Medium),
        softWrap = false,
        modifier = Modifier
            .offset((at.x - 30f).dp, (at.y - 10f).dp)
            .size(60.dp, 20.dp)
            .wrapContentSize()
            .graphicsLayer {
                alpha = appear()
                rotationZ = -4f
            }
            .background(Paper.Card)
            .padding(horizontal = 4.dp),
    )
}

/** A spot to draw a canvas over the whole board. */
@Composable
internal fun BoxScope.BoardCanvas(onDraw: DrawScope.() -> Unit) {
    Canvas(Modifier.fillMaxSize(), onDraw = onDraw)
}
