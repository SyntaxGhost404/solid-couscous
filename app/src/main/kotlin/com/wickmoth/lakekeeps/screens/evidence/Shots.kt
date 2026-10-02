package com.wickmoth.lakekeeps.screens.evidence

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.ui.Fonts
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Screenshots from Amy's phone, drawn as her phone shows them: a plain light theme, nothing like
 * Sam's. Each is laid out on a [SHOT_W] x [SHOT_H] dp page, and everything on it sits at fixed
 * coordinates so the Lab can tell what the player pressed and held.
 */
internal const val SHOT_W = 360f
internal const val SHOT_H = 780f

/** Sizes a box to a whole screenshot's proportions. */
fun Modifier.shotAspect(): Modifier = aspectRatio(SHOT_W / SHOT_H)

/** [evidence] as its sender's screenshot, filling the width of [modifier] from the top down. */
@Composable
fun EvidenceShot(evidence: Evidence, modifier: Modifier = Modifier) =
    EvidenceCrop(evidence, Rect(0f, 0f, SHOT_W, SHOT_H), modifier)

/** The [region] of [evidence]'s screenshot (in shot dp), scaled to fill the width of [modifier]. */
@Composable
fun EvidenceCrop(evidence: Evidence, region: Rect, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val density = LocalDensity.current.density
        val scale = constraints.maxWidth / (region.width * density)
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(SHOT_W.dp, SHOT_H.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = -region.left * density * scale
                    translationY = -region.top * density * scale
                },
        ) { ShotPage(evidence) }
    }
}

@Composable
private fun ShotPage(evidence: Evidence) {
    Box(
        Modifier
            .size(SHOT_W.dp, SHOT_H.dp)
            .background(Shot.Background),
    ) {
        when (evidence) {
            Evidence.Battery -> with(BatteryShot) { Page() }
            Evidence.Apps -> with(AppsShot) { Page() }
            Evidence.Permissions -> with(AppInfoShot) { Page() }
        }
    }
}

/** What pressing and holding a spot on a screenshot finds. */
sealed interface Find {
    /** Worth pinning: sets [flag], and the board shows it. [label] is how Sam names it. */
    data class Pin(val flag: String, val label: String) : Find

    /** Not worth pinning, and Sam's [note] says why. */
    data class Pass(val note: String) : Find
}

/** A spot on a screenshot, in shot dp, and what holding it finds. */
@Immutable
data class Hotspot(val rect: Rect, val find: Find)

/** The spots on [evidence] worth pressing; the first that contains the point wins. */
fun hotspots(evidence: Evidence): List<Hotspot> = when (evidence) {
    Evidence.Battery -> BatteryShot.hotspots
    Evidence.Apps -> AppsShot.hotspots
    Evidence.Permissions -> AppInfoShot.hotspots
}

/** What holding [at] (shot dp) on [evidence] finds. */
fun find(evidence: Evidence, at: Offset): Find =
    hotspots(evidence).firstOrNull { it.rect.contains(at) }?.find ?: Find.Pass(NOTHING_THERE)

internal const val NOTHING_THERE = "Nothing there worth pinning."

/** Amy's phone: a light theme with a teal accent. */
internal object Shot {
    val Background = Color(0xFFF4F5F7)
    val Surface = Color(0xFFFFFFFF)
    val Text = Color(0xFF1B1E23)
    val Muted = Color(0xFF5E6570)
    val Faint = Color(0xFF9097A1)
    val Accent = Color(0xFF2B8C7E)
    val Divider = Color(0xFFE3E6EA)
    val Band = Color(0xFFE2E6EB)
}

internal fun shotText(size: TextUnit, weight: FontWeight = FontWeight.Medium, color: Color = Shot.Text, align: TextAlign = TextAlign.Start) =
    TextStyle(fontFamily = Fonts.Quicksand, fontWeight = weight, fontSize = size, color = color, textAlign = align)

/** Text placed at ([x], [y]) on the page, [width] wide. */
@Composable
internal fun BoxScope.At(x: Float, y: Float, text: String, style: TextStyle, width: Float = SHOT_W - x - 20f) {
    BasicText(
        text,
        style = style,
        maxLines = 2,
        modifier = Modifier
            .offset(x.dp, y.dp)
            .size(width.dp, (style.fontSize.value * 2.8f).dp),
    )
}

/** The status bar along the top of a screenshot: the time, and Wi-Fi and battery at [battery] percent. */
@Composable
internal fun BoxScope.StatusBar(time: String, battery: Int) {
    At(22f, 9f, time, shotText(13.sp, FontWeight.Bold), width = 80f)
    At(266f, 9f, "$battery%", shotText(12.sp, FontWeight.SemiBold, align = TextAlign.End), width = 46f)
    Canvas(Modifier.offset(240f.dp, 11f.dp).size(98.dp, 14.dp)) {
        val dp = density
        // Wi-Fi: three arcs over a dot
        val c = Offset(10f * dp, 11f * dp)
        for (k in 1..3) {
            val r = (3f + k * 3f) * dp
            drawArc(Shot.Text, 225f, 90f, false, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(1.6f * dp, cap = StrokeCap.Round))
        }
        drawCircle(Shot.Text, 1.4f * dp, c)
        // battery
        val left = 76f * dp
        drawRoundRect(Shot.Text, Offset(left, 2f * dp), Size(18f * dp, 10f * dp), CornerRadius(2.5f * dp), style = Stroke(1.3f * dp))
        drawRoundRect(Shot.Text, Offset(left + 2f * dp, 4f * dp), Size(14f * dp * battery / 100f, 6f * dp), CornerRadius(1.2f * dp))
        drawRoundRect(Shot.Text, Offset(left + 18.6f * dp, 5f * dp), Size(1.6f * dp, 4f * dp), CornerRadius(0.8f * dp))
    }
}

/** A settings page title with its back arrow. */
@Composable
internal fun BoxScope.PageTitle(title: String) {
    Canvas(Modifier.offset(18.dp, 50.dp).size(22.dp)) {
        val dp = density
        val path = Path().apply {
            moveTo(13f * dp, 4f * dp)
            lineTo(6f * dp, 11f * dp)
            lineTo(13f * dp, 18f * dp)
        }
        drawPath(path, Shot.Text, style = Stroke(2f * dp, cap = StrokeCap.Round))
        drawLine(Shot.Text, Offset(6.5f * dp, 11f * dp), Offset(19f * dp, 11f * dp), 2f * dp, cap = StrokeCap.Round)
    }
    At(52f, 48f, title, shotText(22.sp, FontWeight.Bold), width = 280f)
}

/** A rounded tile for an app icon, drawn by [glyph] in white on the phone's own slate gradient. */
@Composable
internal fun BoxScope.SystemIcon(x: Float, y: Float, size: Float, glyph: DrawScope.(Float) -> Unit) {
    Canvas(Modifier.offset(x.dp, y.dp).size(size.dp)) {
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF5E7FA6), Color(0xFF3E5876)), Offset.Zero, Offset(this.size.width, this.size.height)),
            cornerRadius = CornerRadius(this.size.width * 0.28f),
        )
        glyph(this.size.width)
    }
}

/**
 * The impostor's icon: a flat grey square with a cog that isn't quite this phone's, not the
 * rounded slate tile every real system app wears.
 */
@Composable
internal fun BoxScope.ImpostorIcon(x: Float, y: Float, size: Float) {
    Canvas(Modifier.offset(x.dp, y.dp).size(size.dp)) {
        val w = this.size.width
        drawRect(Color(0xFFA3A8AE))
        drawCog(Offset(w * 0.52f, w * 0.5f), w * 0.3f, teeth = 6, color = Color(0xFFEDEEEF))
    }
}

/** A cog with [teeth] square teeth around a hub, centred on [c] with outer radius [r]. */
internal fun DrawScope.drawCog(c: Offset, r: Float, teeth: Int, color: Color) {
    val inner = r * 0.72f
    for (k in 0 until teeth) {
        val a = 2.0 * PI * k / teeth
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        drawLine(color, c + dir * inner * 0.8f, c + dir * r, strokeWidth = r * 0.42f)
    }
    drawCircle(color, inner, c)
    drawCircle(Color(0xFFA3A8AE), inner * 0.42f, c)
}

/** A hairline between rows. */
@Composable
internal fun BoxScope.Divider(y: Float, from: Float = 20f) {
    Box(
        Modifier
            .offset(from.dp, y.dp)
            .size((SHOT_W - from - 20f).dp, 1.dp)
            .background(Shot.Divider),
    )
}

/** A pill button on a settings page. */
@Composable
internal fun BoxScope.PillButton(x: Float, y: Float, width: Float, text: String) {
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(width.dp, 38.dp)
            .border(1.dp, Color(0xFFC9CED5), RoundedCornerShape(19.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = shotText(13.sp, FontWeight.SemiBold, Shot.Accent))
    }
}

/** A round icon with an initial, for the rows of the battery page. */
@Composable
internal fun BoxScope.LetterIcon(x: Float, y: Float, letter: String, tint: Color) {
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(32.dp)
            .background(tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(letter, style = shotText(14.sp, FontWeight.Bold, Color.White))
    }
}
