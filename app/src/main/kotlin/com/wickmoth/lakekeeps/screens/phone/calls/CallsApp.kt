package com.wickmoth.lakekeeps.screens.phone.calls

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.screens.phone.AppHeader
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.HeaderButton
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneOs
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.FrameFit
import kotlin.coroutines.cancellation.CancellationException

/** The Phone app: recent calls, or the keypad, where calls ring out. */
@Composable
internal fun CallsApp(os: PhoneOs, fit: FrameFit) {
    val dialer = os.dialer
    DesignScale(fit) {
        Column(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.AppBackground),
        ) {
            CallsHeader(dialer, onClose = os::closeApp)
            AnimatedContent(
                targetState = dialer.page,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    // the keypad rises into place; the list settles back down over it
                    if (targetState == Dialer.Page.Keypad) {
                        (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(360, easing = Ease.Emphasized)) { it / 10 }) togetherWith
                            fadeOut(tween(140))
                    } else {
                        (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(360, easing = Ease.Emphasized)) { -it / 24 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically(tween(200)) { it / 12 })
                    }
                },
                label = "page",
            ) { page ->
                when (page) {
                    Dialer.Page.Recents -> Recents(os)
                    Dialer.Page.Keypad -> Keypad(dialer)
                }
            }
        }
    }
    // Mid-call there is no leaving; otherwise back leaves the keypad, then closes the app.
    BackHandler(enabled = dialer.inCall && !os.shadeOpen) { dialer.refuse() }
    BackHandler(enabled = !dialer.inCall && dialer.page == Dialer.Page.Keypad && !os.shadeOpen) {
        dialer.show(Dialer.Page.Recents)
    }
    PredictiveBackHandler(enabled = !dialer.inCall && dialer.page == Dialer.Page.Recents && !os.shadeOpen) { gesture ->
        try {
            gesture.collect { os.peekBackFromApp(it.progress) }
            os.closeApp()
        } catch (cancelled: CancellationException) {
            os.restoreApp()
            throw cancelled
        }
    }
}

/** Title, the switch between recent calls and the keypad, and close; both buttons hide mid-call. */
@Composable
private fun CallsHeader(dialer: Dialer, onClose: () -> Unit) {
    val navigation by animateFloatAsState(if (dialer.inCall) 0f else 1f, tween(280), label = "navigation")
    AppHeader {
        Crossfade(
            targetState = dialer.page,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp),
            animationSpec = tween(200),
            label = "switch",
        ) { page ->
            val toKeypad = page == Dialer.Page.Recents
            HeaderButton(
                glyph = if (toKeypad) Glyph.Keypad else Glyph.Recents,
                label = stringResource(if (toKeypad) R.string.keypad else R.string.recent_calls),
                visible = { navigation },
            ) {
                dialer.show(if (toKeypad) Dialer.Page.Keypad else Dialer.Page.Recents)
            }
        }
        PhoneText(stringResource(R.string.app_phone), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
        HeaderButton(
            Glyph.Close,
            stringResource(R.string.close),
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp),
            visible = { navigation },
        ) { onClose() }
    }
}
