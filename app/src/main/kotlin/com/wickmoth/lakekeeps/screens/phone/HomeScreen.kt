package com.wickmoth.lakekeeps.screens.phone

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
internal const val ICON = 64f
private const val FIRST_ROW = 136.5f
private const val ROW_STEP = 118f
private val Columns = floatArrayOf(55f, 136.5f, 218.5f, 300f)

/** Top-left of the icon tile in grid slot [index], on the design frame. */
internal fun iconTopLeft(index: Int) = Offset(Columns[index % 4] - ICON / 2f, FIRST_ROW + (index / 4) * ROW_STEP - ICON / 2f)

/** Total length of the icons' staggered entrance, in ms of [content] progress. */
internal const val ICONS_MS = 760f

/**
 * A phone's home screen at full size. [content] (0..1) drives the staggered arrival of the icons,
 * so the same screen can be shown while it is still being lifted. The Messages icon carries the
 * [unread] badge and opens the app from its tile.
 */
@Composable
fun HomeScreen(owner: Owner, content: () -> Float, unread: () -> Int, onOpenMessages: () -> Unit, modifier: Modifier = Modifier) {
    val spec = remember(owner) { phoneSpec(owner) }
    val fit = rememberFrameFit(Alignment.TopCenter)
    Box(modifier.fillMaxSize().background(Color.Black)) {
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
            spec.apps.forEachIndexed { i, app ->
                if (app.label == R.string.app_messages) {
                    AppIcon(app, i, content, badge = unread, onOpen = onOpenMessages)
                } else {
                    AppIcon(app, i, content)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.AppIcon(
    app: App,
    index: Int,
    content: () -> Float,
    badge: () -> Int = { 0 },
    onOpen: (() -> Unit)? = null,
) {
    val label = stringResource(app.label)
    val unread = badge()
    val spoken = when {
        !app.enabled -> stringResource(R.string.app_unavailable, label)
        unread > 0 -> "$label, ${pluralStringResource(R.plurals.unread, unread, unread)}"
        else -> label
    }
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
                label = spoken,
                enabled = app.enabled,
                onPress = { down ->
                    scope.launch { press.animateTo(if (down) 1f else 0f, spring(dampingRatio = if (down) 0.9f else 0.42f, stiffness = 700f)) }
                },
            ) {
                if (app.enabled) {
                    view.haptic(Haptic.Tick)
                    if (onOpen != null) onOpen() else audio.play(Sfx.Tap, 0.8f)
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
        UnreadBadge(badge, appear)
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

/** A count bubble on an icon's corner that pops in when messages arrive and bumps as more do. */
@Composable
private fun BoxScope.UnreadBadge(count: () -> Int, appear: () -> Float) {
    val n = count()
    val pop = remember { Animatable(if (n > 0) 1f else 0f) }
    var shown by remember { mutableIntStateOf(n) }
    LaunchedEffect(n) {
        if (n > 0) shown = n
        when {
            n > 0 && pop.value < 1f -> pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
            n > 0 -> {
                pop.snapTo(1.25f)
                pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
            }
            else -> pop.animateTo(0f, tween(160))
        }
    }
    if (shown <= 0) return
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .offset(x = 27.dp, y = (-6).dp)
            .size(20.dp)
            .graphicsLayer {
                val s = pop.value * appear().coerceIn(0f, 1f)
                scaleX = s
                scaleY = s
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .background(BadgeRed, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (shown > 9) "9+" else shown.toString(),
            style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White),
        )
    }
}

private val BadgeRed = Color(0xFFE5484D)
