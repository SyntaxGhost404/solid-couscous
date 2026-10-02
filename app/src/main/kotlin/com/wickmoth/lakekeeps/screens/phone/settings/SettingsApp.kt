package com.wickmoth.lakekeeps.screens.phone.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.screens.phone.AppHeader
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.GlyphIcon
import com.wickmoth.lakekeeps.screens.phone.HeaderButton
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneOs
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.tactile
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import com.wickmoth.lakekeeps.ui.lerp as lerpFloat

private enum class Page { Main, Help, Credits }

/** The game's settings, as an app on the phones: sound, help, credits, and starting over. */
@Composable
internal fun SettingsApp(os: PhoneOs, fit: FrameFit) {
    var page by remember { mutableStateOf(Page.Main) }
    var asking by remember { mutableStateOf(false) }
    val sheet = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current

    fun open(next: Page) {
        os.feedback(Haptic.Tick)
        audio.play(Sfx.Tap, 0.5f)
        page = next
    }

    fun askToReset() {
        os.feedback(Haptic.Tick)
        audio.play(Sfx.Tap, 0.5f)
        asking = true
        scope.launch { sheet.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 380f)) }
    }

    fun dismiss() {
        scope.launch {
            sheet.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f))
            asking = false
        }
    }

    DesignScale(fit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.AppBackground),
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    // pages slide in from the right, and back out the way they came
                    if (targetState != Page.Main) {
                        (slideInHorizontally(tween(340)) { it } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(340)) { -it / 4 } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200)))
                    }
                },
                label = "page",
            ) { shown ->
                when (shown) {
                    Page.Main -> Main(os, onOpen = ::open, onReset = ::askToReset)
                    Page.Help -> Help(os) { page = Page.Main }
                    Page.Credits -> Credits(os) { page = Page.Main }
                }
            }
            if (asking) {
                ResetSheet(sheet, resetBody(os.owner.case), onCancel = ::dismiss) {
                    os.feedback(Haptic.Confirm)
                    os.state.reset()
                }
            }
        }
    }
    BackHandler(enabled = asking && !os.shadeOpen) { dismiss() }
    BackHandler(enabled = !asking && page != Page.Main && !os.shadeOpen) { page = Page.Main }
    PredictiveBackHandler(enabled = !asking && page == Page.Main && !os.shadeOpen) { gesture ->
        try {
            gesture.collect { os.peekBackFromApp(it.progress) }
            os.closeApp()
        } catch (cancelled: CancellationException) {
            os.restoreApp()
            throw cancelled
        }
    }
}

@Composable
private fun Main(os: PhoneOs, onOpen: (Page) -> Unit, onReset: () -> Unit) {
    val audio = LocalAudio.current
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() }
    Column(Modifier.fillMaxSize()) {
        AppHeader {
            PhoneText(stringResource(R.string.app_settings), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
            HeaderButton(
                Glyph.Close,
                stringResource(R.string.close),
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            ) { os.closeApp() }
        }
        PageColumn {
            TitleCard()
            Spacer(Modifier.height(24.dp))
            Section(R.string.settings_sound_section) {
                SwitchRow(
                    glyph = if (audio.muted) Glyph.SoundOff else Glyph.SoundOn,
                    title = R.string.settings_sound,
                    about = R.string.settings_sound_about,
                    checked = !audio.muted,
                ) { on ->
                    // the click is heard either way: before muting, or after unmuting
                    if (!on) audio.play(Sfx.Toggle)
                    audio.muted = !on
                    if (on) audio.play(Sfx.Toggle)
                }
                Divider()
                SwitchRow(
                    glyph = Glyph.Waves,
                    title = R.string.settings_ambience,
                    about = R.string.settings_ambience_about,
                    checked = audio.ambienceOn,
                ) { on ->
                    audio.ambienceOn = on
                    audio.play(Sfx.Toggle)
                }
            }
            Spacer(Modifier.height(20.dp))
            Section(R.string.settings_game_section) {
                LinkRow(Glyph.Help, R.string.settings_help) { onOpen(Page.Help) }
                Divider()
                LinkRow(Glyph.Credits, R.string.settings_credits) { onOpen(Page.Credits) }
                Divider()
                LinkRow(Glyph.Reset, R.string.settings_reset, PhoneColors.Alert, onTap = onReset)
            }
            if (version != null) {
                Spacer(Modifier.height(28.dp))
                PhoneText(
                    stringResource(R.string.settings_version, version),
                    phoneText(13.sp, color = PhoneColors.TextFaint),
                    Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

/** A page of the app below its header: centred, scrolling, the phone UI's width. */
@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 32.dp),
            content = content,
        )
    }
}

/** The game's title in its own type, over the night and a lantern's glow. */
@Composable
private fun TitleCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Palette.Night, Color(0xFF13283A))))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Palette.Lantern.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.84f, size.height * 0.78f),
                        radius = size.height * 0.9f,
                    ),
                    radius = size.height * 0.9f,
                    center = Offset(size.width * 0.84f, size.height * 0.78f),
                )
            }
            .border(1.dp, PhoneColors.Outline, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                stringResource(R.string.title_lead),
                style = TextStyle(fontFamily = Fonts.Fell, fontStyle = FontStyle.Italic, fontSize = 17.sp, color = Palette.TitleCream),
            )
            BasicText(
                stringResource(R.string.title_main),
                style = TextStyle(fontFamily = Fonts.Fell, fontSize = 33.sp, color = Palette.TitleCream),
            )
            Spacer(Modifier.height(6.dp))
            PhoneText(
                stringResource(R.string.settings_prototype, stringResource(R.string.studio_name)),
                phoneText(12.sp, color = PhoneColors.TextMuted),
            )
        }
    }
}

/** A labelled group of rows on one card, like the shade's notifications. */
@Composable
private fun Section(@StringRes label: Int, rows: @Composable ColumnScope.() -> Unit) {
    PhoneText(
        stringResource(label).uppercase(),
        phoneText(11.sp, FontWeight.Bold, PhoneColors.TextFaint).copy(letterSpacing = 0.12.em),
        Modifier.padding(start = 6.dp, bottom = 10.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PhoneColors.Surface)
            .border(1.dp, PhoneColors.Outline, RoundedCornerShape(18.dp)),
        content = rows,
    )
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(start = 52.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(PhoneColors.Outline),
    )
}

@Composable
private fun SwitchRow(glyph: Glyph, @StringRes title: Int, @StringRes about: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(glyph, PhoneColors.TextMuted, Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            PhoneText(stringResource(title), phoneText(16.sp, FontWeight.SemiBold))
            Spacer(Modifier.height(2.dp))
            PhoneText(stringResource(about), phoneText(12.5.sp, color = PhoneColors.TextFaint))
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked)
    }
}

/** A switch: the knob slides over as the track fills with the accent. */
@Composable
private fun Switch(checked: Boolean) {
    val on by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 520f), label = "switch")
    Canvas(Modifier.size(46.dp, 28.dp)) {
        val h = size.height
        drawRoundRect(lerp(PhoneColors.Outline, PhoneColors.Accent, on.coerceIn(0f, 1f)), cornerRadius = CornerRadius(h / 2f))
        val x = lerpFloat(h / 2f, size.width - h / 2f, on)
        drawCircle(Color.White, h / 2f - 3.dp.toPx(), Offset(x, h / 2f))
    }
}

@Composable
private fun LinkRow(glyph: Glyph, @StringRes title: Int, tint: Color = PhoneColors.Text, onTap: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .drawBehind { drawRect(PhoneColors.SurfacePressed, alpha = press.value) }
            .tactile(
                stringResource(title),
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 800f)) } },
                onTap = onTap,
            )
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(glyph, if (tint == PhoneColors.Text) PhoneColors.TextMuted else tint, Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        PhoneText(stringResource(title), phoneText(16.sp, FontWeight.SemiBold, tint), Modifier.weight(1f))
        GlyphIcon(Glyph.Chevron, PhoneColors.TextFaint, Modifier.size(18.dp))
    }
}

/** A text page reached from the settings: back to them, its title, close. */
@Composable
private fun TextPage(os: PhoneOs, @StringRes title: Int, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppHeader {
            HeaderButton(
                Glyph.Back,
                stringResource(R.string.back),
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
            ) { onBack() }
            PhoneText(stringResource(title), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
            HeaderButton(
                Glyph.Close,
                stringResource(R.string.close),
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            ) { os.closeApp() }
        }
        PageColumn(content)
    }
}

/** How the case on this phone is played, a step at a time. */
private fun helpLines(case: CaseId): List<Int> = when (case) {
    CaseId.LittleBird -> listOf(
        R.string.help_office, R.string.help_phone, R.string.help_live,
        R.string.help_sort, R.string.help_pin, R.string.help_thread, R.string.help_shade,
    )
    CaseId.Prototype -> listOf(R.string.help_board, R.string.help_phones, R.string.help_shade, R.string.help_live, R.string.help_calls)
}

/** What starting over forgets, for the case on this phone. */
@StringRes
private fun resetBody(case: CaseId): Int = when (case) {
    CaseId.LittleBird -> R.string.reset_body_case
    CaseId.Prototype -> R.string.reset_body
}

@Composable
private fun Help(os: PhoneOs, onBack: () -> Unit) = TextPage(os, R.string.settings_help, onBack) {
    helpLines(os.owner.case)
        .forEachIndexed { i, line ->
            Row(Modifier.padding(top = if (i == 0) 6.dp else 0.dp, bottom = 18.dp)) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(PhoneColors.Accent.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PhoneText("${i + 1}", phoneText(13.sp, FontWeight.Bold, PhoneColors.Accent))
                }
                Spacer(Modifier.width(14.dp))
                PhoneText(stringResource(line), phoneText(15.5.sp).copy(lineHeight = 23.sp), Modifier.weight(1f))
            }
        }
}

@Composable
private fun Credits(os: PhoneOs, onBack: () -> Unit) = TextPage(os, R.string.settings_credits, onBack) {
    TitleCard()
    Spacer(Modifier.height(24.dp))
    PhoneText(stringResource(R.string.credits_made), phoneText(15.5.sp).copy(lineHeight = 23.sp))
    Spacer(Modifier.height(22.dp))
    PhoneText(
        stringResource(R.string.credits_fonts_title).uppercase(),
        phoneText(11.sp, FontWeight.Bold, PhoneColors.TextFaint).copy(letterSpacing = 0.12.em),
    )
    Spacer(Modifier.height(8.dp))
    PhoneText(stringResource(R.string.credits_fonts), phoneText(15.5.sp).copy(lineHeight = 23.sp))
    Spacer(Modifier.height(22.dp))
    PhoneText(stringResource(R.string.credits_built), phoneText(15.5.sp, color = PhoneColors.TextMuted).copy(lineHeight = 23.sp))
}

/** "Start over?", slid up over the settings; tap outside, Cancel or back to keep playing. */
@Composable
private fun ResetSheet(progress: Animatable<Float, AnimationVector1D>, @StringRes body: Int, onCancel: () -> Unit, onReset: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = 0.6f * progress.value.coerceIn(0f, 1f)) }
                .pointerInput(Unit) { detectTapGestures { onCancel() } },
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - progress.value) * size.height }
                .background(PhoneColors.Header, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(start = 24.dp, end = 24.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 22.dp)
                    .size(36.dp, 4.dp)
                    .background(PhoneColors.Outline, CircleShape),
            )
            Column(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(),
            ) {
                PhoneText(stringResource(R.string.reset_title), phoneText(21.sp, FontWeight.Bold))
                Spacer(Modifier.height(10.dp))
                PhoneText(stringResource(body), phoneText(15.sp, color = PhoneColors.TextMuted).copy(lineHeight = 22.sp))
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SheetButton(R.string.cancel, PhoneColors.Surface, PhoneColors.Text, Modifier.weight(1f), onCancel)
                    SheetButton(R.string.reset_confirm, PhoneColors.Alert, Color.White, Modifier.weight(1f), onReset)
                }
            }
        }
    }
}

@Composable
private fun SheetButton(@StringRes label: Int, fill: Color, ink: Color, modifier: Modifier, onTap: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .height(50.dp)
            .graphicsLayer {
                val s = 1f - 0.04f * press.value
                scaleX = s
                scaleY = s
            }
            .background(fill, RoundedCornerShape(25.dp))
            .tactile(
                stringResource(label),
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 800f)) } },
                onTap = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PhoneText(stringResource(label), phoneText(15.sp, FontWeight.SemiBold, ink))
    }
}
