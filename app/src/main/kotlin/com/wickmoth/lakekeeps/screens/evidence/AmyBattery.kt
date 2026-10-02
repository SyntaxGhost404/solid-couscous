package com.wickmoth.lakekeeps.screens.evidence

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.game.littlebird.Flag

/**
 * Amy's battery page, last 24 hours, from 8 PM yesterday: ordinary use by day, the phone dead
 * from noon until she could charge it, and a steady band of use from 2 to 4 AM while the screen
 * was off. Its app list carries the one line that doesn't belong.
 */
internal object BatteryShot {
    const val CHART_LEFT = 24f
    const val CHART_TOP = 244f
    const val CHART_W = 312f
    const val CHART_H = 116f
    private const val HOUR_W = CHART_W / 24f
    const val ROWS_TOP = 456f
    const val ROW_H = 46f

    /** Use per hour (0..1), starting at 8 PM: idle overnight except 2 to 4 AM; dead at noon. */
    private val hours = floatArrayOf(
        0.38f, 0.44f, 0.30f, 0.08f, 0.02f, 0.02f, 0.46f, 0.44f, 0.03f, 0.02f, 0.05f, 0.52f,
        0.70f, 0.62f, 0.74f, 0.58f, 0f, 0f, 0.12f, 0.40f, 0.46f, 0.52f, 0.38f, 0.44f,
    )

    /** Screen off from 11 PM to 6:30 AM, in hours after 8 PM. */
    private const val SCREEN_OFF_FROM = 3f
    private const val SCREEN_OFF_TO = 10.5f

    private fun hourX(h: Float) = CHART_LEFT + h * HOUR_W

    /** The overnight band, as the board's printout crops it. */
    val chartRegion = Rect(16f, 232f, 344f, 384f)
    val band = Rect(hourX(6f) - 2f, CHART_TOP - 8f, hourX(8f) + 2f, CHART_TOP + CHART_H + 4f)

    private class Row(val name: String, val detail: String, val share: String, val letter: String?, val tint: Color)

    private val rows = listOf(
        Row("Screen", "Screen on 4 hr 51 min", "24%", "S", Color(0xFF6B7785)),
        Row("System Sync Service", "Background 2 hr 6 min", "19%", null, Color.Unspecified),
        Row("Messages", "Screen on 1 hr 10 min", "12%", "M", Color(0xFF2B8C7E)),
        Row("Camera", "Screen on 22 min", "9%", "C", Color(0xFFC08A2E)),
        Row("Browser", "Screen on 19 min", "7%", "B", Color(0xFF3E6FB8)),
        Row("Maps", "Screen on 14 min", "5%", "M", Color(0xFF4C9A5B)),
        Row("Mail", "Screen on 9 min", "3%", "M", Color(0xFFC0504D)),
    )

    private fun rowTop(i: Int) = ROWS_TOP + i * ROW_H

    val syncRow = Rect(20f, rowTop(1), 340f, rowTop(2))

    val hotspots = listOf(
        Hotspot(band, Find.Pin(Flag.PIN_BAND, "The 2 to 4 AM band")),
        Hotspot(Rect(hourX(SCREEN_OFF_FROM), CHART_TOP - 8f, hourX(SCREEN_OFF_TO), CHART_TOP + CHART_H + 4f), Find.Pass("She's asleep and the phone is face down. Look closer at the night.")),
        Hotspot(Rect(CHART_LEFT, CHART_TOP - 8f, CHART_LEFT + CHART_W, CHART_TOP + CHART_H + 4f), Find.Pass("Daytime. She uses her phone. Look at when she doesn't.")),
        Hotspot(syncRow, Find.Pin(Flag.PIN_SYNC, "System Sync Service, 19%")),
        Hotspot(Rect(20f, ROWS_TOP, 340f, rowTop(rows.size)), Find.Pass("Ordinary daytime use.")),
        Hotspot(Rect(20f, 92f, 340f, 150f), Find.Pass("Low. That's the symptom, not the cause.")),
    )

    @Composable
    fun BoxScope.Page() {
        StatusBar("8:14", battery = 34)
        PageTitle("Battery")
        At(24f, 92f, "34%", shotText(40.sp, FontWeight.Bold), width = 120f)
        At(124f, 112f, "About 3 hr 10 min left", shotText(13.sp, color = Shot.Muted), width = 200f)
        At(24f, 166f, "Battery usage", shotText(15.sp, FontWeight.SemiBold), width = 200f)
        Tab(24f, 196f, 126f, "Last 24 hours", selected = true)
        Tab(158f, 196f, 108f, "Last 7 days", selected = false)
        Chart()
        listOf("8 PM" to 0f, "2 AM" to 6f, "8 AM" to 12f, "2 PM" to 18f, "8 PM" to 24f).forEach { (label, h) ->
            At(hourX(h) - 22f, CHART_TOP + CHART_H + 6f, label, shotText(11.sp, color = Shot.Faint, align = TextAlign.Center), width = 44f)
        }
        Box(
            Modifier
                .offset(CHART_LEFT.dp, 392.dp)
                .size(12.dp)
                .background(Shot.Band, RoundedCornerShape(3.dp)),
        )
        At(CHART_LEFT + 18f, 390f, "Screen off", shotText(12.sp, color = Shot.Muted), width = 120f)
        At(24f, 424f, "App usage", shotText(15.sp, FontWeight.SemiBold), width = 200f)
        rows.forEachIndexed { i, row ->
            val y = rowTop(i)
            if (row.letter != null) LetterIcon(24f, y + 7f, row.letter, row.tint) else ImpostorIcon(24f, y + 7f, 32f)
            At(68f, y + 4f, row.name, shotText(15.sp, FontWeight.SemiBold), width = 200f)
            At(68f, y + 24f, row.detail, shotText(12.sp, color = Shot.Muted), width = 200f)
            At(270f, y + 12f, row.share, shotText(14.sp, FontWeight.SemiBold, align = TextAlign.End), width = 66f)
        }
    }

    @Composable
    private fun BoxScope.Tab(x: Float, y: Float, width: Float, text: String, selected: Boolean) {
        Box(
            Modifier
                .offset(x.dp, y.dp)
                .size(width.dp, 30.dp)
                .background(if (selected) Color(0xFFD5ECE8) else Shot.Surface, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text, style = shotText(12.5.sp, FontWeight.SemiBold, if (selected) Color(0xFF1C6157) else Shot.Muted))
        }
    }

    @Composable
    private fun BoxScope.Chart() {
        Canvas(Modifier.offset(0.dp, 0.dp).size(360.dp, 380.dp)) {
            val dp = density
            val bottom = (CHART_TOP + CHART_H) * dp
            drawRect(
                Shot.Band,
                topLeft = Offset(hourX(SCREEN_OFF_FROM) * dp, CHART_TOP * dp),
                size = Size((SCREEN_OFF_TO - SCREEN_OFF_FROM) * HOUR_W * dp, CHART_H * dp),
            )
            for (k in 0..2) {
                val y = (CHART_TOP + CHART_H * k / 2f) * dp
                drawLine(Shot.Divider, Offset(CHART_LEFT * dp, y), Offset((CHART_LEFT + CHART_W) * dp, y), 1f * dp)
            }
            hours.forEachIndexed { i, use ->
                if (use <= 0f) return@forEachIndexed
                val h = maxOf(use * CHART_H, 2f) * dp
                drawRoundRect(
                    Shot.Accent,
                    topLeft = Offset((hourX(i.toFloat()) + 2f) * dp, bottom - h),
                    size = Size((HOUR_W - 4f) * dp, h),
                    cornerRadius = CornerRadius(2f * dp),
                )
            }
            drawLine(Shot.Faint, Offset(CHART_LEFT * dp, bottom), Offset((CHART_LEFT + CHART_W) * dp, bottom), 1f * dp)
        }
    }
}
