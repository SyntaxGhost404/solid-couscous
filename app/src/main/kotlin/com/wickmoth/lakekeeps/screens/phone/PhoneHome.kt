package com.wickmoth.lakekeeps.screens.phone

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.rememberFrameFit
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch

@Immutable
data class App(@StringRes val label: Int, @DrawableRes val icon: Int, val enabled: Boolean = true)

@Immutable
data class PhoneSpec(@StringRes val owner: Int, @DrawableRes val wallpaper: Int, val apps: List<App>)

internal fun phoneSpec(owner: Owner): PhoneSpec = when (owner) {
    Owner.Theo -> PhoneSpec(
        R.string.phone_theo,
        R.drawable.wallpaper_theo,
        listOf(
            App(R.string.app_messages, R.drawable.icon_messages),
            App(R.string.app_phone, R.drawable.icon_phone),
            App(R.string.app_calculator, R.drawable.icon_calculator),
            App(R.string.app_browser, R.drawable.icon_browser),
            App(R.string.app_notes, R.drawable.icon_notes),
            App(R.string.app_archive, R.drawable.icon_archive),
            App(R.string.app_loose_ends, R.drawable.icon_loose_ends),
            App(R.string.app_settings, R.drawable.icon_settings, enabled = false),
        ),
    )
    Owner.Mira -> PhoneSpec(
        R.string.phone_mira,
        R.drawable.wallpaper_mira,
        listOf(
            App(R.string.app_messages, R.drawable.icon_messages),
            App(R.string.app_phone, R.drawable.icon_phone),
            App(R.string.app_mail, R.drawable.icon_mail, enabled = false),
            App(R.string.app_gallery, R.drawable.icon_gallery),
            App(R.string.app_picnook, R.drawable.icon_picnook),
            App(R.string.app_diary, R.drawable.icon_diary),
            App(R.string.app_files, R.drawable.icon_files),
            App(R.string.app_mooncrush, R.drawable.icon_mooncrush),
            App(R.string.app_settings, R.drawable.icon_settings),
        ),
    )
}

/** Grid metrics on the design frame. */
private const val ICON = 64f
private const val FIRST_ROW = 136.5f
private const val ROW_STEP = 118f
private val Columns = floatArrayOf(55f, 136.5f, 218.5f, 300f)

/** Total length of the icons' staggered entrance, in ms of [content] progress. */
private const val ICONS_MS = 760f

/**
 * A phone's home screen at full size. [content] (0..1) drives the status bar and the staggered
 * arrival of the icons, so the same screen can be shown while it is still being lifted.
 */
@Composable
fun PhoneHome(owner: Owner, content: () -> Float, onBack: () -> Unit) {
    val spec = remember(owner) { phoneSpec(owner) }
    val fit = rememberFrameFit(Alignment.TopCenter)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(spec.wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = lerp(1.08f, 1f, window(content() * ICONS_MS, 0f, ICONS_MS, Ease.OutCubic))
                    scaleX = s
                    scaleY = s
                },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.2f)
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.5f), 1f to Color.Transparent)),
        )
        DesignFrame(fit) {
            StatusBar(stringResource(spec.owner), content, onBack)
            spec.apps.forEachIndexed { i, app -> AppIcon(app, i, content) }
        }
    }
}

@Composable
private fun BoxScope.StatusBar(owner: String, content: () -> Float, onBack: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val audio = LocalAudio.current
    val shown = { window(content() * ICONS_MS, 0f, 260f, Ease.OutCubic) }
    // The diamond takes the player back to the case board.
    Canvas(
        Modifier
            .offset(9.dp, 37.dp)
            .size(44.dp, 44.dp)
            .graphicsLayer {
                alpha = shown()
                val s = 1f - 0.18f * press.value
                scaleX = s
                scaleY = s
                rotationZ = 90f * press.value
            }
            .tactile(
                label = stringResource(R.string.back_to_board),
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 600f)) } },
            ) {
                view.haptic(Haptic.Tick)
                audio.play(Sfx.Tap, 0.6f)
                onBack()
            },
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val w = 6.2f * density
        val h = 8.2f * density
        val diamond = Path().apply {
            moveTo(c.x, c.y - h)
            lineTo(c.x + w, c.y)
            lineTo(c.x, c.y + h)
            lineTo(c.x - w, c.y)
            close()
        }
        drawPath(diamond, Color.White, style = Stroke(width = 1.7f * density, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawPath(diamond, Color.White, alpha = 0.35f * press.value)
    }
    BasicText(
        text = owner,
        style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.92f)),
        softWrap = false,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = 51.dp)
            .graphicsLayer {
                alpha = shown()
                translationY = ((1f - shown()) * -4f).dp.toPx()
            },
    )
    Canvas(
        Modifier
            .offset(292.dp, 50.dp)
            .size(48.dp, 18.dp)
            .graphicsLayer { alpha = shown() },
    ) {
        val dp = density
        val cy = 9f * dp
        // wifi: a dot and three arcs, each arc lighting up in turn as the screen wakes
        val wifi = Offset(10f * dp, cy + 4.6f * dp)
        drawCircle(Color.White, radius = 1.3f * dp, center = wifi)
        for (k in 1..3) {
            val r = (2.6f + k * 2.6f) * dp
            val a = window(content() * ICONS_MS, 60f + k * 60f, 140f)
            drawArc(
                Color.White,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(wifi.x - r, wifi.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 1.5f * dp, cap = StrokeCap.Round),
                alpha = a,
            )
        }
        // battery
        val left = 26f * dp
        val bw = 17f * dp
        val bh = 9.4f * dp
        val top = cy - bh / 2f
        drawRoundRect(Color.White, topLeft = Offset(left, top), size = Size(bw, bh), cornerRadius = CornerRadius(2.4f * dp), style = Stroke(width = 1.3f * dp))
        drawRoundRect(Color.White, topLeft = Offset(left + bw + 0.8f * dp, cy - 2f * dp), size = Size(1.6f * dp, 4f * dp), cornerRadius = CornerRadius(0.8f * dp))
        for (k in 0 until 3) {
            val a = window(content() * ICONS_MS, 120f + k * 70f, 120f)
            drawRoundRect(
                Color.White,
                topLeft = Offset(left + (2.4f + k * 4.2f) * dp, top + 2.2f * dp),
                size = Size(3.0f * dp, bh - 4.4f * dp),
                cornerRadius = CornerRadius(0.6f * dp),
                alpha = a,
            )
        }
    }
}

@Composable
private fun BoxScope.AppIcon(app: App, index: Int, content: () -> Float) {
    val label = stringResource(app.label)
    val press = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val audio = LocalAudio.current
    val cx = Columns[index % 4]
    val cy = FIRST_ROW + (index / 4) * ROW_STEP
    val start = 120f + index * 42f
    val appear = { window(content() * ICONS_MS, start, 420f, Ease.OutBack) }
    val fade = { window(content() * ICONS_MS, start, 220f) }

    Box(
        Modifier
            .offset((cx - 40f).dp, (cy - ICON / 2f).dp)
            .size(80.dp, 96.dp)
            .graphicsLayer { translationX = shake.value.dp.toPx() }
            .tactile(
                label = if (app.enabled) label else stringResource(R.string.app_unavailable, label),
                enabled = app.enabled,
                onPress = { down ->
                    scope.launch { press.animateTo(if (down) 1f else 0f, spring(dampingRatio = if (down) 0.9f else 0.42f, stiffness = 700f)) }
                },
            ) {
                if (app.enabled) {
                    view.haptic(Haptic.Tick)
                    audio.play(Sfx.Tap, 0.8f)
                } else {
                    view.haptic(Haptic.Reject)
                    audio.play(Sfx.Denied)
                    scope.launch {
                        shake.animateTo(0f, keyframes {
                            durationMillis = 420
                            -7f at 50
                            7f at 120
                            -5f at 190
                            5f at 260
                            -2f at 330
                            0f at 420
                        })
                    }
                }
            },
    ) {
        Image(
            painter = painterResource(app.icon),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(ICON.dp)
                .graphicsLayer {
                    val a = appear()
                    val s = lerp(0.62f, 1f, a) * (1f - 0.1f * press.value)
                    scaleX = s
                    scaleY = s
                    translationY = ((1f - a) * 12f).dp.toPx()
                    alpha = fade() * if (app.enabled) 1f else 0.5f
                },
        )
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = Fonts.Quicksand,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.2.sp,
                color = if (app.enabled) Palette.LabelOn else Palette.LabelOff,
                shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1.5f), 4f),
            ),
            softWrap = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (ICON + 9f).dp)
                .wrapContentSize(unbounded = true)
                .clearAndSetSemantics { }
                .graphicsLayer {
                    alpha = window(content() * ICONS_MS, start + 90f, 260f)
                    translationY = ((1f - window(content() * ICONS_MS, start + 90f, 260f, Ease.OutCubic)) * 4f).dp.toPx()
                },
        )
    }
}
