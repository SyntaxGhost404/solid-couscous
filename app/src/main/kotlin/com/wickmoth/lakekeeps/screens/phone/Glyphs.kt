package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

/** Plain line glyphs for the in-game phone UI, drawn on a 24-unit grid. */
enum class Glyph {
    Back, Close, SoundOn, SoundOff, Message, Handset, Keypad, Recents, Backspace, CallOut, CallIn, CallMissed,
    Eye, EyeOff, Photo, Attachment, Chevron, Waves, Reset, Help, Credits,
}

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier, alpha: () -> Float = { 1f }, turn: () -> Float = { 0f }) {
    Canvas(modifier) { drawGlyph(glyph, color, alpha(), turn()) }
}

/** A handset upright, opening to the right; drawn turned 45 degrees back, as phone icons stand. */
private val UprightHandset: Path by lazy {
    val handle = Path().apply {
        // a thick arc around the left, from the earpiece down to the mouthpiece
        arcTo(Rect(Offset(15f, 12f), 9f), 120f, 120f, forceMoveTo = true)
        arcTo(Rect(Offset(15f, 12f), 5.2f), 240f, -120f, forceMoveTo = false)
        close()
    }
    val earpiece = Path().apply { addRoundRect(RoundRect(Rect(9.6f, 2.4f, 17.6f, 7.6f), CornerRadius(2.2f))) }
    val mouthpiece = Path().apply { addRoundRect(RoundRect(Rect(9.6f, 16.4f, 17.6f, 21.6f), CornerRadius(2.2f))) }
    val body = Path()
    body.op(handle, earpiece, PathOperation.Union)
    Path().apply { op(body, mouthpiece, PathOperation.Union) }
}

private val BackspaceOutline: Path by lazy {
    PathParser().parsePathString("M9 5.5H19.5Q21.5 5.5 21.5 7.5V16.5Q21.5 18.5 19.5 18.5H9L2.5 12Z").toPath()
}

private val EyeOutline: Path by lazy { PathParser().parsePathString("M2.5 12Q12 3 21.5 12Q12 21 2.5 12Z").toPath() }

private val Paperclip: Path by lazy {
    PathParser().parsePathString("M16.5 8V15.5A4.5 4.5 0 0 1 7.5 15.5V6.5A3 3 0 0 1 13.5 6.5V15A1.5 1.5 0 0 1 10.5 15V8.5").toPath()
}

private val ThreeWaves: Path by lazy {
    PathParser().parsePathString(
        "M3 7Q6 4.5 9 7T15 7T21 7M3 12Q6 9.5 9 12T15 12T21 12M3 17Q6 14.5 9 17T15 17T21 17",
    ).toPath()
}

private val QuestionMark: Path by lazy {
    PathParser().parsePathString("M9.3 9.4Q9.4 6.6 12 6.6Q14.7 6.6 14.7 9.1Q14.7 10.8 12.9 11.7Q12 12.2 12 13.6").toPath()
}

/**
 * Draws [glyph] centred in the current size. [turn] rotates it in degrees; the handset turned 135
 * degrees is the hang-up handset.
 */
fun DrawScope.drawGlyph(glyph: Glyph, color: Color, alpha: Float = 1f, turn: Float = 0f) {
    val u = size.minDimension / 24f
    val o = Offset((size.width - 24f * u) / 2f, (size.height - 24f * u) / 2f)
    fun p(x: Float, y: Float) = Offset(o.x + x * u, o.y + y * u)
    val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(vararg points: Float) {
        val path = Path().apply {
            moveTo(p(points[0], points[1]).x, p(points[0], points[1]).y)
            for (i in 2 until points.size step 2) lineTo(p(points[i], points[i + 1]).x, p(points[i], points[i + 1]).y)
        }
        drawPath(path, color, alpha = alpha, style = stroke)
    }
    // paths authored on the 24-unit grid are drawn through this, so their strokes scale with them
    fun onGrid(block: DrawScope.() -> Unit) = withTransform({
        translate(o.x, o.y)
        scale(u, u, pivot = Offset.Zero)
    }, block)
    when (glyph) {
        Glyph.Back -> {
            val path = Path().apply {
                moveTo(p(11f, 5f).x, p(11f, 5f).y); lineTo(p(4f, 12f).x, p(4f, 12f).y); lineTo(p(11f, 19f).x, p(11f, 19f).y)
                moveTo(p(4.5f, 12f).x, p(4.5f, 12f).y); lineTo(p(20f, 12f).x, p(20f, 12f).y)
            }
            drawPath(path, color, alpha = alpha, style = stroke)
        }
        Glyph.Close -> {
            drawLine(color, p(6f, 6f), p(18f, 18f), strokeWidth = 2f * u, cap = StrokeCap.Round, alpha = alpha)
            drawLine(color, p(18f, 6f), p(6f, 18f), strokeWidth = 2f * u, cap = StrokeCap.Round, alpha = alpha)
        }
        Glyph.SoundOn, Glyph.SoundOff -> {
            val body = Path().apply {
                moveTo(p(3.5f, 9.5f).x, p(3.5f, 9.5f).y); lineTo(p(7.5f, 9.5f).x, p(7.5f, 9.5f).y)
                lineTo(p(12.5f, 5f).x, p(12.5f, 5f).y); lineTo(p(12.5f, 19f).x, p(12.5f, 19f).y)
                lineTo(p(7.5f, 14.5f).x, p(7.5f, 14.5f).y); lineTo(p(3.5f, 14.5f).x, p(3.5f, 14.5f).y); close()
            }
            drawPath(body, color, alpha = alpha, style = stroke)
            if (glyph == Glyph.SoundOn) {
                drawArc(color, -45f, 90f, false, p(9.5f, 8.5f), Size(7f * u, 7f * u), alpha = alpha, style = stroke)
                drawArc(color, -50f, 100f, false, p(7f, 5f), Size(14f * u, 14f * u), alpha = alpha, style = stroke)
            } else {
                drawLine(color, p(16f, 9.5f), p(21f, 14.5f), strokeWidth = 2f * u, cap = StrokeCap.Round, alpha = alpha)
                drawLine(color, p(21f, 9.5f), p(16f, 14.5f), strokeWidth = 2f * u, cap = StrokeCap.Round, alpha = alpha)
            }
        }
        Glyph.Message -> {
            drawRoundRect(color, p(3f, 4.5f), Size(18f * u, 12.5f * u), CornerRadius(4f * u), alpha = alpha, style = stroke)
            val tail = Path().apply {
                moveTo(p(7.5f, 17f).x, p(7.5f, 17f).y); lineTo(p(6f, 20.5f).x, p(6f, 20.5f).y); lineTo(p(11f, 17f).x, p(11f, 17f).y)
            }
            drawPath(tail, color, alpha = alpha, style = stroke)
        }
        Glyph.Handset -> onGrid {
            rotate(turn - 45f, pivot = Offset(12f, 12f)) {
                drawPath(UprightHandset, color, alpha = alpha, style = Stroke(width = 1.8f, join = StrokeJoin.Round))
            }
        }
        Glyph.Keypad -> {
            for (row in 0..2) for (col in 0..2) drawCircle(color, 1.7f * u, p(6f + col * 6f, 4.5f + row * 5.5f), alpha = alpha)
            drawCircle(color, 1.7f * u, p(12f, 21f), alpha = alpha)
        }
        Glyph.Recents -> {
            drawCircle(color, 8.5f * u, p(12f, 12f), alpha = alpha, style = stroke)
            line(12f, 7.5f, 12f, 12f, 15.5f, 14f)
        }
        Glyph.Backspace -> {
            onGrid { drawPath(BackspaceOutline, color, alpha = alpha, style = Stroke(width = 2f, join = StrokeJoin.Round)) }
            line(12.5f, 9f, 17.5f, 15f)
            line(17.5f, 9f, 12.5f, 15f)
        }
        Glyph.CallOut -> {
            line(6.5f, 17.5f, 17f, 7f)
            line(9.5f, 7f, 17f, 7f, 17f, 14.5f)
        }
        Glyph.CallIn -> {
            line(17.5f, 6.5f, 7f, 17f)
            line(7f, 9.5f, 7f, 17f, 14.5f, 17f)
        }
        Glyph.CallMissed -> {
            line(3.5f, 8f, 10.5f, 15f, 20f, 5.5f)
            line(14.5f, 5.5f, 20f, 5.5f, 20f, 11f)
        }
        Glyph.Eye, Glyph.EyeOff -> {
            onGrid { drawPath(EyeOutline, color, alpha = alpha, style = Stroke(width = 2f, join = StrokeJoin.Round)) }
            drawCircle(color, 3f * u, p(12f, 12f), alpha = alpha, style = stroke)
            if (glyph == Glyph.EyeOff) line(4f, 3.5f, 20f, 20.5f)
        }
        Glyph.Photo -> {
            drawRoundRect(color, p(3f, 4.5f), Size(18f * u, 15f * u), CornerRadius(3f * u), alpha = alpha, style = stroke)
            drawCircle(color, 1.8f * u, p(8.5f, 9.5f), alpha = alpha, style = stroke)
            line(3.5f, 17.5f, 9.5f, 12f, 13f, 15f, 16f, 12.5f, 20.5f, 16.5f)
        }
        Glyph.Attachment -> onGrid {
            drawPath(Paperclip, color, alpha = alpha, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.Chevron -> line(9.5f, 5.5f, 16f, 12f, 9.5f, 18.5f)
        Glyph.Waves -> onGrid {
            drawPath(ThreeWaves, color, alpha = alpha, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.Reset -> {
            // round arrow turning back on itself
            drawArc(color, -150f, 300f, false, p(4f, 4f), Size(16f * u, 16f * u), alpha = alpha, style = stroke)
            line(3.6f, 3.8f, 5.1f, 8f, 9.4f, 7.1f)
        }
        Glyph.Help -> {
            drawCircle(color, 9f * u, p(12f, 12f), alpha = alpha, style = stroke)
            onGrid { drawPath(QuestionMark, color, alpha = alpha, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
            drawCircle(color, 1.25f * u, p(12f, 17.2f), alpha = alpha)
        }
        Glyph.Credits -> {
            line(5f, 7f, 19f, 7f)
            line(5f, 12f, 19f, 12f)
            line(5f, 17f, 13f, 17f)
        }
    }
}
