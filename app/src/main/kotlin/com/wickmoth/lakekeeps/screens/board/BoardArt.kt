package com.wickmoth.lakekeeps.screens.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.wickmoth.lakekeeps.ui.Palette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Paints the slate board and the desk into a bitmap once per window size: a cool slate with
 * chalk smears, scratches and specks, and the desk front running to the bottom of the window.
 */
internal fun paintBackdrop(width: Int, height: Int, deskTop: Float, unit: Float, density: Density): ImageBitmap {
    val bitmap = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        val w = size.width
        val boardH = deskTop.coerceIn(0f, size.height)
        drawRect(Palette.Board, size = Size(w, boardH))
        drawRect(
            Brush.radialGradient(
                0f to Color(0xFF18202A), 1f to Color(0xFF0B0F14),
                center = Offset(w * 0.5f, boardH * 0.42f), radius = maxOf(w, boardH) * 0.75f,
            ),
            size = Size(w, boardH),
            alpha = 0.85f,
        )
        val rnd = Random(17)
        repeat(26) {
            val c = Offset(rnd.nextFloat() * w, rnd.nextFloat() * boardH)
            val r = (30f + rnd.nextFloat() * 90f) * unit
            drawCircle(
                Brush.radialGradient(0f to Color(0xFF8C98A6).copy(alpha = 0.035f), 1f to Color.Transparent, center = c, radius = r),
                radius = r,
                center = c,
            )
        }
        repeat(240) {
            val y = rnd.nextFloat() * boardH
            val x = rnd.nextFloat() * w
            val len = (12f + rnd.nextFloat() * 110f) * unit
            val light = rnd.nextFloat() < 0.6f
            drawLine(
                if (light) Color(0xFFB4C0CC) else Color.Black,
                Offset(x, y),
                Offset(x + len, y + (rnd.nextFloat() - 0.5f) * 3f * unit),
                strokeWidth = (0.4f + rnd.nextFloat() * 0.9f) * unit,
                alpha = if (light) 0.018f + rnd.nextFloat() * 0.03f else 0.05f + rnd.nextFloat() * 0.06f,
            )
        }
        repeat(520) {
            drawCircle(
                Color(0xFFC9D2DC),
                radius = (0.25f + rnd.nextFloat() * 0.7f) * unit,
                center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * boardH),
                alpha = 0.03f + rnd.nextFloat() * 0.09f,
            )
        }
        // the board darkens where it meets the desk
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f), startY = boardH - 26f * unit, endY = boardH),
            topLeft = Offset(0f, boardH - 26f * unit),
            size = Size(w, 26f * unit),
        )
        if (boardH < size.height) {
            drawRect(
                Brush.verticalGradient(0f to Palette.Desk, 1f to Palette.DeskShade, startY = boardH, endY = size.height),
                topLeft = Offset(0f, boardH),
                size = Size(w, size.height - boardH),
            )
            drawLine(Color(0xFF2C323A), Offset(0f, boardH + 0.6f * unit), Offset(w, boardH + 0.6f * unit), strokeWidth = 1.2f * unit)
            repeat(60) {
                val y = boardH + rnd.nextFloat() * (size.height - boardH)
                val x = rnd.nextFloat() * w
                drawLine(
                    Color.Black,
                    Offset(x, y),
                    Offset(x + (30f + rnd.nextFloat() * 140f) * unit, y),
                    strokeWidth = 0.8f * unit,
                    alpha = 0.06f + rnd.nextFloat() * 0.05f,
                )
            }
        }
    }
    return bitmap
}

/** A thread sagging under its own weight between two pins (quadratic, sag grows with span). */
internal fun threadPath(a: Offset, b: Offset): Path {
    val span = (b - a).getDistance()
    val horizontal = abs(b.x - a.x) / span.coerceAtLeast(1f)
    val mid = (a + b) / 2f + Offset(0f, span * 0.07f * (0.35f + 0.65f * horizontal))
    return Path().apply {
        moveTo(a.x, a.y)
        quadraticTo(mid.x, mid.y, b.x, b.y)
    }
}

/** Draws the first [progress] of [path], measured along its length. */
internal fun DrawScope.drawPartial(path: Path, progress: Float, color: Color, width: Float, alpha: Float = 1f, offset: Offset = Offset.Zero) {
    if (progress <= 0f) return
    val partial = if (progress >= 1f) {
        path
    } else {
        val measure = PathMeasure().apply { setPath(path, false) }
        Path().also { measure.getSegment(0f, measure.length * progress, it, true) }
    }
    if (offset != Offset.Zero) {
        partial.translate(offset)
    }
    drawPath(partial, color, alpha = alpha, style = Stroke(width = width, cap = StrokeCap.Round))
    if (offset != Offset.Zero) partial.translate(-offset)
}

/** A red thread with its shadow on the board; [lit] (0..1) brightens and thickens it. */
internal fun DrawScope.drawThread(path: Path, progress: Float, lit: Float, dp: Float) {
    drawPartial(path, progress, Color.Black, 1.7f * dp, alpha = 0.5f, offset = Offset(0.9f * dp, 1.8f * dp))
    drawPartial(path, progress, lerp(Palette.Thread, Palette.ThreadLit, lit), (1.5f + 0.8f * lit) * dp)
    if (lit > 0f) drawPartial(path, progress, Palette.ThreadLit, 5f * dp, alpha = 0.18f * lit)
}

/** A round-headed push pin seen from the front, with its cast shadow. */
internal fun DrawScope.drawPin(at: Offset, pop: Float, dp: Float, head: Color = Color(0xFFC23A36)) {
    if (pop <= 0f) return
    val r = 3.4f * dp * pop
    drawCircle(Color.Black, radius = r * 1.05f, center = at + Offset(1.3f * dp, 2.0f * dp), alpha = 0.45f)
    drawCircle(
        Brush.radialGradient(
            0f to lerp(head, Color.White, 0.35f), 0.45f to head, 1f to lerp(head, Color.Black, 0.45f),
            center = at + Offset(-r * 0.35f, -r * 0.35f), radius = r * 1.4f,
        ),
        radius = r,
        center = at,
    )
    drawCircle(Color.White, radius = r * 0.24f, center = at + Offset(-r * 0.38f, -r * 0.4f), alpha = 0.75f)
}

/** A chalked dashed line, drawn on from its start. */
internal fun DrawScope.drawDash(dash: Dash, progress: Float, dp: Float) {
    if (progress <= 0f) return
    val end = dash.from + (dash.to - dash.from) * progress
    drawLine(
        Palette.Chalk,
        dash.from,
        end,
        strokeWidth = 1.5f * dp,
        cap = StrokeCap.Round,
        alpha = 0.55f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * dp, 5f * dp)),
    )
}

/** The symbol scratched on the dock, redrawn in marker: a ringed crescent over water. */
internal object Mark {
    private val strokes = listOf(
        "M23,4 C34,4 42,12 42,23 C42,34 34,42 23,42 C12,42 4,34 4,23 C4,13 11,5.5 19.5,4.3",
        "M29,9.5 C20,10.5 15.5,19 19,25.5 C21,29.5 26,30.5 30,29",
        "M8.5,32 Q12,29 15.5,32 T22.5,32 T29.5,32 T36.5,32",
        "M11.5,37 Q15,34 18.5,37 T25.5,37 T32.5,37",
    )

    fun paths(origin: Offset, scale: Float): List<Path> = strokes.map {
        PathParser().parsePathString(it).toPath().apply {
            transform(androidx.compose.ui.graphics.Matrix().apply {
                translate(origin.x, origin.y)
                scale(scale, scale)
            })
        }
    }
}

/** Two quick, uneven loops of red marker around a card, as if circled in a hurry. */
internal fun scribblePath(center: Offset, rx: Float, ry: Float, dp: Float): Path {
    val path = Path()
    val steps = 96
    val turns = 2f * PI.toFloat() * 1.92f
    for (k in 0..steps) {
        val f = k / steps.toFloat()
        val a = -2.2f + f * turns
        val loop = if (f < 0.52f) 0f else 1f
        val jx = 3.2f * sin(a * 3f + loop * 1.4f) + loop * 4.5f
        val jy = 2.6f * cos(a * 2f + loop * 2.1f) + loop * 3f
        val x = center.x + (rx + jx) * dp * cos(a)
        val y = center.y + (ry + jy) * dp * sin(a)
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}

internal fun Offset.distanceTo(other: Offset) = hypot(x - other.x, y - other.y)
