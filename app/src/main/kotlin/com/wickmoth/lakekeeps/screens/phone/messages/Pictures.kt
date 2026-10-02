package com.wickmoth.lakekeeps.screens.phone.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.screens.evidence.EvidenceShot
import com.wickmoth.lakekeeps.screens.evidence.shotAspect
import com.wickmoth.lakekeeps.screens.phone.PlaceholderImage
import com.wickmoth.lakekeeps.ui.lerp

/** A picture sent in a conversation: the sender's screenshot if it is evidence, or a placeholder. */
@Composable
internal fun AttachmentPicture(id: String, modifier: Modifier) {
    val evidence = Evidence.byId(id)
    if (evidence != null) EvidenceShot(evidence, modifier) else PlaceholderImage(id, modifier)
}

/** A picture from the conversation, whole, over black; tap anywhere (or back) to close it. */
@Composable
internal fun PictureViewer(id: String, progress: () -> Float, onClose: () -> Unit) {
    val close = stringResource(R.string.picture_close)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
            .background(Color.Black)
            .semantics {
                contentDescription = close
                onClick { onClose(); true }
            }
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        AttachmentPicture(
            id,
            Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .graphicsLayer {
                    val s = lerp(0.9f, 1f, progress())
                    scaleX = s
                    scaleY = s
                }
                .shotAspect(),
        )
    }
}
