package com.wickmoth.lakekeeps.screens.phone.calls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.phone.Ago
import com.wickmoth.lakekeeps.game.phone.Call
import com.wickmoth.lakekeeps.game.phone.CallKind
import com.wickmoth.lakekeeps.game.phone.PhoneBook
import com.wickmoth.lakekeeps.game.phone.ago
import com.wickmoth.lakekeeps.game.phone.formatNumber
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.GlyphIcon
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneOs
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch

/** The phone's call log, newest first; each call can be returned from its row. */
@Composable
internal fun Recents(os: PhoneOs) {
    val calls = os.calls.calls(os.owner)
    val now = os.messages.clock(os.owner.case)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // keyed from the oldest end, so a new call on top leaves every other row's key alone
        itemsIndexed(calls, key = { i, _ -> calls.size - i }) { i, call ->
            CallRow(
                call = call,
                now = now,
                os = os,
                appear = { window(os.appIn.value, 0.35f + i.coerceAtMost(5) * 0.05f, 0.4f, Ease.OutCubic) },
            )
        }
    }
}

@Composable
private fun CallRow(call: Call, now: Int, os: PhoneOs, appear: () -> Float) {
    val contact = PhoneBook.find(os.owner, call.number)
    val name = contact?.name ?: formatNumber(call.number)
    val label = contact?.label ?: stringResource(R.string.call_not_saved)
    val ago = when (val a = call.ago(now)) {
        Ago.Now -> stringResource(R.string.ago_now)
        is Ago.Minutes -> stringResource(R.string.ago_minutes, a.n)
        is Ago.Hours -> stringResource(R.string.ago_hours, a.n)
        is Ago.Days -> stringResource(R.string.ago_days, a.n)
    }
    val (glyph, tint, said) = when (call.kind) {
        CallKind.Outgoing -> Triple(Glyph.CallOut, PhoneColors.Accent, R.string.call_outgoing)
        CallKind.Incoming -> Triple(Glyph.CallIn, PhoneColors.Online, R.string.call_incoming)
        CallKind.Missed -> Triple(Glyph.CallMissed, PhoneColors.Alert, R.string.call_missed)
    }
    val description = stringResource(said, name, ago)
    Row(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer {
                val a = appear()
                alpha = a
                translationY = ((1f - a) * 14f).dp.toPx()
            }
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GlyphIcon(glyph, tint, Modifier.size(20.dp))
                Spacer(Modifier.height(5.dp))
                PhoneText(ago, phoneText(12.sp, color = PhoneColors.TextFaint), maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                PhoneText(name, phoneText(16.5.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                PhoneText(label, phoneText(14.sp, color = PhoneColors.TextMuted), maxLines = 1)
            }
        }
        CallBack(stringResource(R.string.call_contact, name)) { os.dialer.call(call.number) }
    }
}

/** The handset at the end of a row: rings that number again. */
@Composable
private fun CallBack(label: String, onTap: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(52.dp)
            .graphicsLayer {
                val s = 1f - 0.1f * press.value
                scaleX = s
                scaleY = s
            }
            .drawBehind { drawCircle(PhoneColors.Online, radius = size.minDimension * 0.42f, alpha = 0.16f * press.value) }
            .tactile(label, onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 800f)) } }, onTap = onTap),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(Glyph.Handset, PhoneColors.TextMuted, Modifier.size(24.dp))
    }
}
