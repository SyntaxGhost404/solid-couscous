package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Plain line glyphs for the in-game phone UI, drawn on a 24-unit grid. */
enum class Glyph { Back, Close, SoundOn, SoundOff, Message }

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier, alpha: () -> Float = { 1f }) {
    Canvas(modifier) { drawGlyph(glyph, color, alpha()) }
}

fun DrawScope.drawGlyph(glyph: Glyph, color: Color, alpha: Float = 1f) {
    val u = size.minDimension / 24f
    val o = Offset((size.width - 24f * u) / 2f, (size.height - 24f * u) / 2f)
    fun p(x: Float, y: Float) = Offset(o.x + x * u, o.y + y * u)
    val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
    }
}
