package com.wickmoth.lakekeeps.screens.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * A stand-in for a picture whose art isn't made yet: a soft wash in one of the game's night tones,
 * picked from [seed] so the same picture always looks the same, with a plain image glyph.
 */
@Composable
fun PlaceholderImage(seed: String, modifier: Modifier = Modifier, glyphSize: Dp = 28.dp) {
    val (light, dark) = Washes[abs(seed.hashCode() % Washes.size)]
    Box(
        modifier.background(Brush.linearGradient(listOf(light, dark))),
        contentAlignment = Alignment.Center,
    ) {
        // faded as one layer, so the strokes don't brighten where they cross
        GlyphIcon(Glyph.Photo, Color.White, Modifier.size(glyphSize).graphicsLayer { alpha = 0.5f })
    }
}

private val Washes = listOf(
    Color(0xFF2E5370) to Color(0xFF14233A), // lake
    Color(0xFF6E4C72) to Color(0xFF2B1F3E), // dusk
    Color(0xFF8C5C3B) to Color(0xFF3B2519), // embers
    Color(0xFF41624D) to Color(0xFF1B2B23), // reeds
    Color(0xFF5B6673) to Color(0xFF242B33), // fog
    Color(0xFF7E4D59) to Color(0xFF331F29), // rosehip
)
