package com.wickmoth.lakekeeps.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.screens.advisory.HeadphonesAdvisory
import com.wickmoth.lakekeeps.screens.board.CaseFile
import com.wickmoth.lakekeeps.screens.studio.StudioSplash
import com.wickmoth.lakekeeps.screens.title.TitleScreen
import com.wickmoth.lakekeeps.ui.LocalViewport
import com.wickmoth.lakekeeps.ui.Palette

@Composable
fun GameRoot(state: GameState = rememberGameState()) {
    val audio = LocalAudio.current
    LaunchedEffect(state.stage >= Stage.Title) {
        if (state.stage >= Stage.Title) audio.ambience(true)
    }
    GameSurface {
        // Each intro screen fades itself out to the shared night colour, so a plain swap is seamless.
        when (state.stage) {
            Stage.Studio -> StudioSplash(onFinished = state::next)
            Stage.Advisory -> HeadphonesAdvisory(onFinished = state::next)
            Stage.Title -> TitleScreen(onFinished = state::next)
            Stage.Board -> CaseFile(state)
        }
    }
}

/** Fills the window with the night colour and publishes the window size to the screens. */
@Composable
fun GameSurface(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Night)) {
        val viewport = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        CompositionLocalProvider(LocalViewport provides viewport, content = content)
    }
}
