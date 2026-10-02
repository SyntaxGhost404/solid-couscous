package com.wickmoth.lakekeeps.screens.phone.messages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.game.messages.Face
import com.wickmoth.lakekeeps.ui.Fonts

/**
 * A contact's round picture. Board characters reuse their polaroid portrait, usually zoomed onto
 * the face; anyone else gets a placeholder: a coloured initial, or the no-photo silhouette.
 */
@Composable
fun Avatar(face: Face, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        when (face) {
            is Face.Photo -> Image(
                painter = painterResource(face.res),
                contentDescription = null,
                modifier = if (face.closeUp) {
                    // rendered larger than the circle so the face fills it, crisp at any size
                    Modifier
                        .requiredSize(size * 1.45f)
                        .offset(y = size * 0.16f)
                } else {
                    Modifier.size(size)
                },
            )
            is Face.Initial -> Box(
                Modifier
                    .size(size)
                    .background(face.tint),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = face.letter,
                    style = TextStyle(
                        fontFamily = Fonts.Quicksand,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.42f).sp,
                        color = Color.White,
                    ),
                )
            }
            Face.None -> Canvas(Modifier.size(size).background(Color(0xFF2B3540))) {
                val w = this.size.width
                drawCircle(Color(0xFF8592A0), radius = w * 0.2f, center = Offset(w / 2f, w * 0.4f))
                drawCircle(Color(0xFF8592A0), radius = w * 0.36f, center = Offset(w / 2f, w * 1.02f))
            }
        }
    }
}
