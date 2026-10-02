package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.wickmoth.lakekeeps.ui.tactile
import kotlinx.coroutines.launch

/** Height of the dark band behind the status bar and an app screen's title row. */
internal val HeaderHeight = 148.dp

/** The dark band at the top of an app screen; [content] is laid out in its title row. */
@Composable
internal fun AppHeader(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .background(PhoneColors.Header),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(76.dp),
            content = content,
        )
    }
}

@Composable
internal fun HeaderButton(glyph: Glyph, label: String, modifier: Modifier = Modifier, visible: () -> Float = { 1f }, onTap: () -> Unit) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .size(44.dp)
            .graphicsLayer {
                val v = visible()
                alpha = v
                val s = (1f - 0.12f * press.value) * (0.8f + 0.2f * v)
                scaleX = s
                scaleY = s
            }
            .tactile(label, onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 800f)) } }) {
                if (visible() > 0.5f) onTap()
            },
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, PhoneColors.Text, Modifier.size(24.dp))
    }
}

/** A quick side-to-side "no": the offset to apply, in dp, settles back to 0. */
internal suspend fun Animatable<Float, AnimationVector1D>.shake() = animateTo(
    0f,
    keyframes {
        durationMillis = 380
        -8f at 50
        8f at 120
        -5f at 190
        4f at 260
        0f at 380
    },
)
