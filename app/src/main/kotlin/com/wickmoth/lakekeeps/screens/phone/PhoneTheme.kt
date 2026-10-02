package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.wickmoth.lakekeeps.ui.Fonts

/**
 * Surfaces of the in-game phone UI: the game's night tones, with the Messages icon's blue as the
 * accent and the Phone icon's green for "online".
 */
internal object PhoneColors {
    val AppBackground = Color(0xFF0B131C)
    val Header = Color(0xFF070C12)
    val Surface = Color(0xFF121A23)
    val SurfacePressed = Color(0xFF1A2531)
    val Outline = Color(0xFF1F2A35)
    val Divider = Color(0xFF16212C)
    val Text = Color(0xFFE8EEF4)
    val TextMuted = Color(0xFF8E9AA7)
    val TextFaint = Color(0xFF66737F)
    val Accent = Color(0xFF2F7DE1)
    val Incoming = Color(0xFF16212C)
    val Outgoing = Color(0xFF2A5DB0)
    val OutgoingTime = Color(0xFFBCD0F0)
    val Online = Color(0xFF3FBF73)
    val Offline = Color(0xFF4A5560)
    val Shade = Color(0xFF080C11)
}

internal fun phoneText(size: TextUnit, weight: FontWeight = FontWeight.Medium, color: Color = PhoneColors.Text) =
    TextStyle(fontFamily = Fonts.Quicksand, fontWeight = weight, fontSize = size, color = color)

/** Text in the phone's type: [style] comes before [modifier], as everywhere in the phone UI. */
@Composable
internal fun PhoneText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = BasicText(text = text, modifier = modifier, style = style, overflow = overflow, maxLines = maxLines)

@Composable
internal fun PhoneText(text: AnnotatedString, style: TextStyle, modifier: Modifier = Modifier) =
    BasicText(text = text, modifier = modifier, style = style)
