package com.wickmoth.lakekeeps.screens.advisory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.ui.DesignFrame
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.Palette
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.rememberFrameFit
import com.wickmoth.lakekeeps.ui.rememberSequence
import com.wickmoth.lakekeeps.ui.wave
import com.wickmoth.lakekeeps.ui.window

private const val LENGTH = 4300f
private const val EXIT = 3600f

private val LineStyle = TextStyle(
    fontFamily = Fonts.Quicksand,
    fontWeight = FontWeight.Medium,
    fontSize = 18.5.sp,
    lineHeight = 25.sp,
    color = Palette.Advisory,
    textAlign = TextAlign.Center,
)

/** "Use headphones": the headphones settle in and hover while the line fades up beneath them. */
@Composable
fun HeadphonesAdvisory(onFinished: () -> Unit) {
    val sequence = rememberSequence(LENGTH, onEnd = onFinished)
    val fit = rememberFrameFit(Alignment.Center)
    val lines = stringResource(R.string.advisory).split('\n')
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Night)
            .pointerInput(Unit) { detectTapGestures { sequence.skipTo(EXIT) } },
    ) {
        DesignFrame(fit) {
            Image(
                painter = painterResource(R.drawable.art_headphones),
                contentDescription = null,
                modifier = Modifier
                    .offset(113.dp, 292.dp)
                    .size(134.dp)
                    .graphicsLayer {
                        val t = sequence.t
                        val enter = window(t, 200f, 900f, Ease.OutCubic)
                        val exit = window(t, EXIT, 600f, Ease.InCubic)
                        val hover = window(t, 900f, 900f, Ease.InOutSine)
                        alpha = enter * (1f - exit)
                        val s = lerp(0.94f, 1f, enter)
                        scaleX = s
                        scaleY = s
                        translationY = ((1f - enter) * 10f + hover * wave(t, 3600f) * 3f - exit * 8f).dp.toPx()
                        rotationZ = hover * wave(t, 4800f, 0.25f) * 1.4f
                    },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .offset(y = 449.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                lines.forEachIndexed { i, line ->
                    BasicText(
                        text = line,
                        style = LineStyle,
                        modifier = Modifier.graphicsLayer {
                            val t = sequence.t
                            val enter = window(t, 700f + i * 160f, 700f, Ease.OutCubic)
                            val exit = window(t, EXIT + i * 60f, 500f, Ease.InCubic)
                            alpha = enter * (1f - exit)
                            translationY = ((1f - enter) * 6f).dp.toPx()
                        },
                    )
                }
            }
        }
    }
}
