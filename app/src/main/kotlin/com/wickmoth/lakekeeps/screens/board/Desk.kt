package com.wickmoth.lakekeeps.screens.board

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.haptic
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Phone geometry shared by the desk and the pick-up transition (design dp). */
internal object PhoneShape {
    val body = Size(40f, 86f)
    const val BODY_CORNER = 7f
    const val BEZEL = 2.4f
    val screen = Size(body.width - 2 * BEZEL, body.height - 2 * BEZEL)
    const val SCREEN_CORNER = 5f
}

@Immutable
data class DeskPhone(val owner: Owner, val center: Offset, val rotation: Float, val casing: Color, val edge: Color)

internal val DeskPhones = listOf(
    DeskPhone(Owner.Mira, Offset(64f, 766f), rotation = 76f, casing = Color(0xFF2B3037), edge = Color(0xFF66707B)),
    DeskPhone(Owner.Theo, Offset(284f, 744f), rotation = 16f, casing = Color(0xFF2F7F7A), edge = Color(0xFF5BB3AA)),
)

internal fun deskPhone(owner: Owner) = DeskPhones.first { it.owner == owner }

/** The part of a prop that touches the desk and casts a shadow, in the prop's own dp. */
@Immutable
private data class Footprint(val rect: Rect, val corner: Float, val oval: Boolean = false)

@Immutable
private data class Prop(
    @DrawableRes val art: Int,
    @StringRes val label: Int,
    val topLeft: Offset,
    val size: Size,
    val rotation: Float,
    val sound: Sfx,
    val footprints: List<Footprint>,
)

private val Props = listOf(
    Prop(
        R.drawable.art_clipboard, R.string.desk_case_file, Offset(30f, 658f), Size(64f, 84f), -7f, Sfx.Paper,
        listOf(Footprint(Rect(2f, 6f, 62f, 83f), 4.5f)),
    ),
    Prop(
        R.drawable.art_newspaper, R.string.desk_newspaper, Offset(130f, 657f), Size(100f, 66f), 4f, Sfx.Paper,
        listOf(Footprint(Rect(1f, 1f, 99f, 65f), 1f)),
    ),
    Prop(
        R.drawable.art_notepad, R.string.desk_notepad, Offset(290f, 598f), Size(64f, 92f), 0f, Sfx.Paper,
        listOf(Footprint(Rect(26f, 70f, 55f, 81f), 0f, oval = true), Footprint(Rect(3f, 47f, 34f, 91f), 1f)),
    ),
    Prop(
        R.drawable.art_mug, R.string.desk_mug, Offset(155f, 734f), Size(50f, 56f), 0f, Sfx.Tap,
        listOf(Footprint(Rect(5f, 45f, 45f, 57f), 0f, oval = true)),
    ),
)

/**
 * The desk along the bottom of the board. [intro] is the board's intro clock in ms; [hidden] is the
 * phone currently lifted off the desk; [landing] reports each phone being put back down.
 */
@Composable
internal fun BoxScope.Desk(
    intro: () -> Float,
    idle: () -> Float,
    hidden: Owner?,
    landing: Landing,
    onPickUp: (Owner) -> Unit,
) {
    Props.forEachIndexed { i, prop -> DeskProp(prop, i, intro) }
    Steam(intro, idle)
    DeskPhones.forEachIndexed { i, phone ->
        PhonePiece(phone, Props.size + i, intro, hidden == phone.owner, landing, onPickUp)
    }
}

/** A soft contact shadow under [area], drawn as three widening translucent layers. */
private fun DrawScope.softShadow(area: Rect, corner: Float, lift: Float, dp: Float, oval: Boolean = false) {
    val spread = (1.5f + 3f * lift) * dp
    val drop = Offset(1.2f * dp, (2.2f + 3f * lift) * dp)
    for (k in 3 downTo 1) {
        val grow = spread * k / 3f
        val topLeft = area.topLeft + drop - Offset(grow, grow)
        val size = Size(area.width + grow * 2, area.height + grow * 2)
        val alpha = 0.16f - 0.04f * k + 0.04f * lift
        if (oval) {
            drawOval(Color.Black, topLeft = topLeft, size = size, alpha = alpha)
        } else {
            drawRoundRect(Color.Black, topLeft = topLeft, size = size, cornerRadius = CornerRadius(corner + grow), alpha = alpha)
        }
    }
}

private fun introLift(intro: Float, index: Int): Float = window(intro, 250f + index * 70f, 520f, Ease.OutCubic)

@Composable
private fun DeskProp(prop: Prop, index: Int, intro: () -> Float) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val view = LocalView.current
    Image(
        painter = painterResource(prop.art),
        contentDescription = null,
        modifier = Modifier
            .offset(prop.topLeft.x.dp, prop.topLeft.y.dp)
            .size(prop.size.width.dp, prop.size.height.dp)
            .graphicsLayer {
                val e = introLift(intro(), index)
                alpha = e
                translationY = ((1f - e) * 18f - press.value * 2f).dp.toPx()
                rotationZ = prop.rotation
                val s = 1f + 0.05f * press.value
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                prop.footprints.forEach { f ->
                    val area = Rect(f.rect.left * density, f.rect.top * density, f.rect.right * density, f.rect.bottom * density)
                    softShadow(area, f.corner * density, press.value, density, f.oval)
                }
            }
            .tactile(
                label = stringResource(prop.label),
                onPress = { down ->
                    scope.launch {
                        press.animateTo(if (down) 1f else 0f, spring(dampingRatio = if (down) 0.8f else 0.45f, stiffness = 520f))
                    }
                },
            ) {
                view.haptic(Haptic.Tick)
                audio.play(prop.sound, 0.7f)
            },
    )
}

/** Steam curling off the coffee: three soft wisps that rise, sway and thin out on staggered loops. */
@Composable
private fun BoxScope.Steam(intro: () -> Float, idle: () -> Float) {
    Canvas(
        Modifier
            .offset(152.dp, 684.dp)
            .size(56.dp, 58.dp),
    ) {
        val presence = window(intro(), 1100f, 900f, Ease.InOutSine)
        if (presence <= 0f) return@Canvas
        val t = idle()
        val dp = density
        val bottom = size.height - 4f * dp
        for (w in 0 until 3) {
            val cycle = ((t / 3800f) + w / 3f) % 1f
            val life = sin(PI.toFloat() * cycle)
            if (life <= 0.01f) continue
            val baseX = (21f + w * 7f) * dp
            val height = (36f + 8f * (w % 2)) * dp
            val path = Path()
            val steps = 20
            for (k in 0..steps) {
                val f = k / steps.toFloat()
                val y = bottom - f * height - cycle * 9f * dp
                val sway = sin(f * 5.4f - t * 0.0024f + w * 1.9f) * (1.2f + 6f * f) * dp
                if (k == 0) path.moveTo(baseX + sway, y) else path.lineTo(baseX + sway, y)
            }
            val fadeUp = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0f),
                0.55f to Color.White.copy(alpha = 0.5f),
                1f to Color.White,
                startY = bottom - height - 9f * dp,
                endY = bottom,
            )
            val a = 0.22f * life * presence
            drawPath(path, fadeUp, alpha = a * 0.35f, style = Stroke(width = 6f * dp, cap = StrokeCap.Round))
            drawPath(path, fadeUp, alpha = a, style = Stroke(width = 2.2f * dp, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun PhonePiece(phone: DeskPhone, index: Int, intro: () -> Float, lifted: Boolean, landing: Landing, onPickUp: (Owner) -> Unit) {
    val press = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(landing) {
        if (landing.owner == phone.owner && landing.count > 0) {
            settle.snapTo(1f)
            settle.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val label = stringResource(R.string.desk_phone, stringResource(if (phone.owner == Owner.Theo) R.string.phone_theo else R.string.phone_mira))
    val body = PhoneShape.body
    Canvas(
        Modifier
            .offset((phone.center.x - body.width / 2f).dp, (phone.center.y - body.height / 2f).dp)
            .size(body.width.dp, body.height.dp)
            .graphicsLayer {
                val e = introLift(intro(), index)
                alpha = if (lifted) 0f else e
                translationY = ((1f - e) * 18f).dp.toPx()
                rotationZ = phone.rotation
                val s = 1f + 0.06f * press.value + 0.05f * settle.value
                scaleX = s
                scaleY = s
            }
            .tactile(
                label = label,
                onPress = { down ->
                    scope.launch { press.animateTo(if (down) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 700f)) }
                },
                enabled = !lifted,
            ) { onPickUp(phone.owner) },
    ) {
        softShadow(Rect(Offset.Zero, size), PhoneShape.BODY_CORNER * density, press.value, density)
        drawDeskPhone(phone, density, screenGlow = press.value * 0.25f)
    }
}

/** A phone lying face up: casing, dark glass, punch-hole camera and a sliver of reflection. */
internal fun DrawScope.drawDeskPhone(phone: DeskPhone, dp: Float, screenGlow: Float = 0f) {
    val body = size
    drawRoundRect(phone.casing, size = body, cornerRadius = CornerRadius(PhoneShape.BODY_CORNER * dp))
    drawRoundRect(
        phone.edge,
        size = body,
        cornerRadius = CornerRadius(PhoneShape.BODY_CORNER * dp),
        style = Stroke(width = 0.9f * dp),
        alpha = 0.8f,
    )
    val inset = PhoneShape.BEZEL * dp
    val screen = Size(body.width - inset * 2, body.height - inset * 2)
    drawRoundRect(Color(0xFF07090B), topLeft = Offset(inset, inset), size = screen, cornerRadius = CornerRadius(PhoneShape.SCREEN_CORNER * dp))
    if (screenGlow > 0f) {
        drawRoundRect(Color(0xFF7FA6C9), topLeft = Offset(inset, inset), size = screen, cornerRadius = CornerRadius(PhoneShape.SCREEN_CORNER * dp), alpha = screenGlow * 0.35f)
    }
    drawCircle(Color(0xFF1E2329), radius = 1.5f * dp, center = Offset(body.width / 2f, inset + 3.6f * dp))
    val glare = Path().apply {
        moveTo(inset + screen.width * 0.55f, inset)
        lineTo(inset + screen.width, inset)
        lineTo(inset + screen.width, inset + screen.height * 0.32f)
        close()
    }
    drawPath(glare, Color.White, alpha = 0.07f)
    drawLine(Color(0xFF2C3238), Offset(body.width / 2f - 6f * dp, body.height - inset - 3f * dp), Offset(body.width / 2f + 6f * dp, body.height - inset - 3f * dp), strokeWidth = 1.1f * dp, cap = StrokeCap.Round)
}
