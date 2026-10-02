package com.wickmoth.lakekeeps.screens.evidence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.game.littlebird.Flag

/**
 * Amy's app list with system apps shown. Every real system app wears the phone's rounded slate
 * tile and says only "System app"; one wears a flat grey square and shows a version and a data
 * bill.
 */
internal object AppsShot {
    const val ROWS_TOP = 172f
    const val ROW_H = 60f
    const val IMPOSTOR = 5

    private class Row(val name: String, val detail: String, val glyph: (DrawScope.(Float) -> Unit)?)

    private val white = Color.White

    private val rows = listOf(
        Row("Bluetooth", "System app") { w -> waves(w) },
        Row("Call Services", "System app") { w -> handset(w) },
        Row("Device Health", "System app") { w -> pulse(w) },
        Row("Keyboard", "System app") { w -> keys(w) },
        Row("Print Service", "System app") { w -> printer(w) },
        Row("System Sync Service", "Version 4.2.1 · 1.4 GB used", null),
        Row("System UI", "System app") { w -> layout(w) },
        Row("Wallpaper & Style", "System app") { w -> picture(w) },
        Row("Wi-Fi Calling", "System app") { w -> waves(w, handsetToo = true) },
        Row("Work Profile", "System app") { w -> briefcase(w) },
    )

    private fun rowTop(i: Int) = ROWS_TOP + i * ROW_H

    val impostorRow = Rect(20f, rowTop(IMPOSTOR), 340f, rowTop(IMPOSTOR + 1))

    val hotspots = listOf(
        Hotspot(impostorRow, Find.Pin(Flag.PIN_IMPOSTOR, "System Sync Service")),
        Hotspot(Rect(20f, ROWS_TOP, 340f, rowTop(rows.size)), Find.Pass("That one belongs here.")),
    )

    @Composable
    fun BoxScope.Page() {
        StatusBar("8:40", battery = 33)
        PageTitle("Apps")
        Chip(24f, 98f, 86f, "All apps", checked = false)
        Chip(118f, 98f, 168f, "System apps shown", checked = true)
        At(24f, 142f, "36 apps", shotText(13.sp, color = Shot.Muted), width = 200f)
        rows.forEachIndexed { i, row ->
            val y = rowTop(i)
            val glyph = row.glyph
            if (glyph != null) SystemIcon(24f, y + 10f, 40f, glyph) else ImpostorIcon(24f, y + 10f, 40f)
            At(78f, y + 9f, row.name, shotText(15.5.sp, FontWeight.SemiBold), width = 260f)
            At(78f, y + 31f, row.detail, shotText(12.5.sp, color = Shot.Muted), width = 260f)
        }
    }

    @Composable
    private fun BoxScope.Chip(x: Float, y: Float, width: Float, text: String, checked: Boolean) {
        Box(
            Modifier
                .offset(x.dp, y.dp)
                .size(width.dp, 30.dp)
                .background(if (checked) Color(0xFFD5ECE8) else Shot.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, if (checked) Color(0xFFB3DCD4) else Color(0xFFD3D8DE), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText((if (checked) "✓  " else "") + text, style = shotText(12.5.sp, FontWeight.SemiBold, if (checked) Color(0xFF1C6157) else Shot.Muted))
        }
    }

    // The system apps' glyphs: simple white line drawings, all in the same hand.

    private fun DrawScope.stroke(w: Float) = Stroke(w * 0.07f, cap = StrokeCap.Round)

    private fun DrawScope.waves(w: Float, handsetToo: Boolean = false) {
        val c = Offset(w * 0.5f, w * 0.68f)
        drawCircle(white, w * 0.05f, c)
        for (k in 1..2) {
            val r = w * (0.14f + k * 0.11f)
            drawArc(white, 225f, 90f, false, c - Offset(r, r), Size(r * 2, r * 2), style = stroke(w))
        }
        if (handsetToo) drawLine(white, Offset(w * 0.3f, w * 0.78f), Offset(w * 0.7f, w * 0.78f), w * 0.06f, StrokeCap.Round)
    }

    private fun DrawScope.handset(w: Float) {
        val r = w * 0.24f
        drawArc(white, 120f, 120f, false, Offset(w * 0.5f - r, w * 0.5f - r), Size(r * 2, r * 2), style = Stroke(w * 0.12f, cap = StrokeCap.Round))
    }

    private fun DrawScope.pulse(w: Float) {
        val path = Path().apply {
            moveTo(w * 0.2f, w * 0.52f)
            lineTo(w * 0.38f, w * 0.52f)
            lineTo(w * 0.46f, w * 0.32f)
            lineTo(w * 0.56f, w * 0.7f)
            lineTo(w * 0.63f, w * 0.52f)
            lineTo(w * 0.8f, w * 0.52f)
        }
        drawPath(path, white, style = stroke(w))
    }

    private fun DrawScope.keys(w: Float) {
        for (row in 0..2) {
            for (col in 0..3) {
                drawRoundRect(white, Offset(w * (0.2f + col * 0.155f), w * (0.32f + row * 0.13f)), Size(w * 0.1f, w * 0.08f), CornerRadius(w * 0.02f))
            }
        }
    }

    private fun DrawScope.printer(w: Float) {
        drawRoundRect(white, Offset(w * 0.22f, w * 0.38f), Size(w * 0.56f, w * 0.26f), CornerRadius(w * 0.05f), style = stroke(w))
        drawRect(white, Offset(w * 0.34f, w * 0.24f), Size(w * 0.32f, w * 0.12f), style = stroke(w))
        drawRect(white, Offset(w * 0.34f, w * 0.6f), Size(w * 0.32f, w * 0.16f))
    }

    private fun DrawScope.layout(w: Float) {
        drawRoundRect(white, Offset(w * 0.24f, w * 0.24f), Size(w * 0.52f, w * 0.52f), CornerRadius(w * 0.06f), style = stroke(w))
        drawLine(white, Offset(w * 0.24f, w * 0.4f), Offset(w * 0.76f, w * 0.4f), w * 0.07f)
        drawLine(white, Offset(w * 0.44f, w * 0.4f), Offset(w * 0.44f, w * 0.76f), w * 0.07f)
    }

    private fun DrawScope.picture(w: Float) {
        drawRoundRect(white, Offset(w * 0.22f, w * 0.27f), Size(w * 0.56f, w * 0.46f), CornerRadius(w * 0.06f), style = stroke(w))
        val path = Path().apply {
            moveTo(w * 0.26f, w * 0.68f)
            lineTo(w * 0.42f, w * 0.5f)
            lineTo(w * 0.53f, w * 0.6f)
            lineTo(w * 0.62f, w * 0.52f)
            lineTo(w * 0.74f, w * 0.66f)
        }
        drawPath(path, white, style = stroke(w))
    }

    private fun DrawScope.briefcase(w: Float) {
        drawRoundRect(white, Offset(w * 0.22f, w * 0.36f), Size(w * 0.56f, w * 0.38f), CornerRadius(w * 0.06f), style = stroke(w))
        drawRoundRect(white, Offset(w * 0.4f, w * 0.26f), Size(w * 0.2f, w * 0.1f), CornerRadius(w * 0.03f), style = stroke(w))
    }
}
